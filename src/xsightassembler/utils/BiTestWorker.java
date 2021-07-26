package xsightassembler.utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.models.*;
import xsightassembler.services.BowlModuleService;
import xsightassembler.services.IsduhService;
import xsightassembler.services.UpperSensorModuleService;
import xsightassembler.view.BiJournalController;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BiTestWorker extends Task<Void> {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());

    private final BiJournalController controller;
    final BlockingQueue<BiTest> queue = new ArrayBlockingQueue<>(1);
    private ExecutorService exService;
    private final Integer labNum;

    private BiTest biTest;
    private MainApp mainApp;
    private String conMsg;
    private String logMsg;
    private long logTimer = 0;
    private BiLogAnalyzer logAnalyzer;
    private FilteredList<LogItem> logItems;
    private final Settings settings;
    private HashMap<String, List<String>> analyzerMap;
    private SshClient sshClient;
    private boolean isSerialsChecked = false;
    private boolean isISduFlag = false;
    private int onlineStatus;
    private IniUtils iniCmd;
    private Map<String, Boolean> checkSerialsMap = new HashMap<>();

    public BiTestWorker(Integer labNum, BiJournalController controller) {
        this.labNum = labNum;
        this.controller = controller;
        this.settings = Utils.getSettings();
        try {
            this.iniCmd = new IniUtils("./strings.ini", "mcu_cmd");
        } catch (IOException e) {
            MsgBox.msgException(e);
        }
    }

    @Override
    protected Void call() throws Exception {
        if (iniCmd == null) {
            return null;
        }
        analyzerMap = Utils.getStringMapFromFile("./analyzer.ini");
        long durationTime;
        long startTime;
        long needTime;
        String ipName = null;

        while (!controller.getShutdown()) {
            System.out.println(Thread.currentThread().getName() + " -> " + Thread.activeCount());
            biTest = controller.getBiTestByLabNum(labNum);
            if (biTest != null) {
                if (ipName == null) {
                    ipName = biTest.getNetNameProperty().getValue();
                    if (settings.getNamePostfix() != null && !settings.getNamePostfix().isEmpty()) {
                        ipName = ipName + settings.getNamePostfix();
                    }
                }
                // update progressbar
                startTime = biTest.getStartDate() == null ? 0 : biTest.getStartDate().getTime();
                long startShift = TimeUnit.MINUTES.toMillis(settings.getStartAnalyzeShiftInt());
                needTime = biTest.getDuration() * 3600000L + startShift;
                durationTime = System.currentTimeMillis() - startTime;
                this.updateProgress(durationTime, needTime);

                onlineStatus = Utils.isSystemOnline(ipName);
                if (onlineStatus != 1) {
                    isSerialsChecked = false;
                    isISduFlag = false;
                    checkSerialsMap.clear();
                    controller.refreshJournal();
                }
                switch (onlineStatus) {
                    case 1:
                        updateConnectionStatus("System online");
                        if (biTest.getStartDate() != null) {
                            logAnalyzerTask(TimeUnit.MILLISECONDS.toMinutes(durationTime));
                        }
                        if (!isSerialsChecked || !isISduFlag) {
                            isSerialsChecked = checkSerials();
                            Thread.sleep(1000);
                            controller.refreshJournal();
                        }
                        Thread.sleep(10000);
                        break;
                    case 0:
                        isSerialsChecked = false;
                        clearMsg();
                        int n = 10;
                        while (n >= 0) {
                            if (biTest == null) {
                                clearMsg();
                                break;
                            }
                            updateConnectionStatus("Host is not reachable. Retry after " + n);
                            Thread.sleep(1000);
                            n--;
                        }
                        updateConnectionStatus("Sending ping request to " + ipName);
                        break;
                    case -1:
                        isSerialsChecked = false;
                        clearMsg();
                        Utils.sendCmd(new String[]{"ipconfig", "/flushdns"});
                        int m = 20;
                        while (m >= 0) {
                            if (biTest == null) {
                                clearMsg();
                                break;
                            }
                            this.updateMessage(
                                    String.format("Unknown host %s. Doing flushdns. Retry after %s", ipName, m));
                            Thread.sleep(1000);
                            m--;
                        }
                        if (biTest != null) {
                            updateConnectionStatus("Sending ping request to " + ipName);
                        }
                        break;
                }
            } else {
                ipName = null;
                clearMsg();
                queue.take();
            }
        }
        return null;
    }

    public void forceLogAnalyzerTask() {
        logTimer = 0;
        logAnalyzerTask(TimeUnit.MILLISECONDS.toMinutes(
                System.currentTimeMillis() - biTest.getStartDate().getTime()));
    }

    private void logAnalyzerTask(long minutes) {
        if (logTimer == minutes || minutes < settings.getStartAnalyzeShiftInt()) {
            return;
        } else if (logTimer != 0 && (minutes % settings.getLogCheckPeriodInt()) != 0) {
            return;
        }
        writeConsole("Start log analyzer task");
        exService = Executors.newSingleThreadExecutor();
        logAnalyzer = new BiLogAnalyzer(biTest, this);
        logAnalyzer.setOnSucceeded(event -> {
            logItems = logAnalyzer.getValue();
            if (logItems != null) {
//                logItems.setPredicate(s -> s.getMessage().contains("UpperSensorSerialNumber"));
//                System.out.println(logItems.size());
                logItems.setPredicate(s -> s.getErrType() != null);
                int errors = logItems.size();
                int ignored = 0;

                List<LogItem> pduResetList = logItems.stream().filter(s ->
                        s.getFullMsg().contains("PDU Reset Counter")).collect(Collectors.toList());
                Pattern pCounter = Pattern.compile("(?<=Counter:)(.*)(?=$)");
                Matcher m;
                Set<Integer> counterSet = new HashSet<>();
                for (LogItem l : pduResetList) {
                    m = pCounter.matcher(l.getFullMsg());
                    if (m.find()) {
                        counterSet.add(Integer.parseInt(m.group(1).trim()));
                    }
                }
                pduResetList.forEach(l -> l.setIgnore(counterSet.size() == 1));

                for (LogItem item : logItems) {
                    boolean ifIgnore = analyzerMap.get("ignore").stream().anyMatch(s ->
                            Pattern.compile(String.format("(.*?)%s(.*)", s)).matcher(item.getFullMsg()).matches());
                    boolean ifIbitIgnore = analyzerMap.get("ignore_if_ibit").stream().anyMatch(s ->
                            Pattern.compile(String.format("(.*?)%s(.*)", s)).matcher(item.getFullMsg()).matches()) && item.isIbit();
                    if (ifIgnore || ifIbitIgnore || item.isIgnore()) {
                        item.setIgnore(true);
                        ignored++;
                    }
                }

                if (errors == 0) {
                    updateLogStatus(String.format("Errors not found. Last check: %s",
                            Utils.getFormattedTime(new Date())));
                } else {
                    updateLogStatus(String.format("%s of %s errors ignored. Last check: %s",
                            ignored, errors, Utils.getFormattedTime(new Date())));
                }

            }
            logTimer = minutes;
            writeConsole("Complete log analyzer task");
        });

        logAnalyzer.setOnFailed(e -> writeConsole("Log analyzer task failure"));
        exService.execute(logAnalyzer);
        exService.shutdown();
    }

    private boolean isBowlAssemblySnPresent(BowlModuleService service, BowlModule bowlModule, String name, String sn) throws CustomException {
        BowlModule tmp = service.findByInnerModuleSn(sn);
        if (tmp != null && !tmp.getId().equals(bowlModule.getId())) {
            String msg = String.format("%s: %s\nalready use on bowl module SN: %s", name, sn, tmp.getModule());
            MsgBox.msgWarning(msg);
            writeConsole(msg);
            return true;
        }
        return false;
    }

    private void clearMsg() {
        this.updateMessage("");
        this.updateProgress(-1, 0);
    }

    private synchronized void updateCurrentMessage(String msg) {
        Platform.runLater(() -> {
            if (getIsduh() == null) {
                this.updateMessage("");
            } else {
                this.updateMessage(msg);
            }
        });
    }

    private synchronized void updateCurrentMessage() {
        String msg;
        if (logMsg == null) {
            msg = (conMsg);
        } else {
            msg = String.format("%s | %s", logMsg, conMsg);
        }
        updateCurrentMessage(msg);
    }

    private void updateConnectionStatus(String s) {
        this.conMsg = s;
        updateCurrentMessage();
    }

    private void updateLogStatus(String s) {
        this.logMsg = s;
        updateCurrentMessage();
    }

    public Integer getLabNum() {
        return labNum;
    }

    public BiTest getBiTest() {
        return biTest;
    }


    public void setBiTest(BiTest biTest) {
        this.isSerialsChecked = false;
        this.isISduFlag = false;
        this.biTest = biTest;
        if (biTest != null && queue.isEmpty()) {
            queue.add(biTest);
        }
        logItems = null;
        logMsg = null;
        conMsg = null;
        updateCurrentMessage();
        controller.refreshJournal();
    }

    public boolean isTestFail() {
        return logItems.stream().anyMatch(t -> t.getErrType() != null && !t.isIgnore());
    }

    public Isduh getIsduh() {
        if (biTest == null) {
            return null;
        }
        return biTest.getIsduh();
    }

    public ObservableValue<String> getLabNumString() {
        return new SimpleStringProperty(Integer.toString(labNum));
    }

    public ObservableValue<String> getStartDateString() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        return Utils.stringToProperty(Utils.getFormattedDate(biTest.getStartDate()));
    }

    public ObservableValue<String> getStageString() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        return new SimpleStringProperty(Integer.toString(biTest.getStage()));
    }

    public ObservableValue<String> getBiNetName() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        try {
            if (!settings.getNamePostfix().trim().isEmpty()) {
                return new SimpleStringProperty(biTest.getNetNameProperty().getValue()
                        + settings.getNamePostfix().trim());
            }
        } catch (NullPointerException ignored) {
        }
        return biTest.getNetNameProperty();
    }

    public ObservableValue<String> getPlugDateString() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        return Utils.stringToProperty(Utils.getFormattedDate(biTest.getPlugDate()));
    }

    public Date getStartDate() {
        return biTest.getStartDate();
    }

    public FilteredList<LogItem> getLogItems() {
        return logItems;
    }

    public SimpleStringProperty getCoolerStatus() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        return new SimpleStringProperty(Strings.getPassFailMap().get(biTest.getCooler()));
    }

    public SimpleStringProperty getIcrStatus() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        return new SimpleStringProperty(Strings.getPassFailMap().get(biTest.getIcr()));
    }

    public SimpleStringProperty getSnCheckStatus() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        String res = isSerialsChecked ? "Yes" : "No";
        return new SimpleStringProperty(res);
    }

    public SimpleStringProperty getISDUFlag() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        String res = isISduFlag ? "Yes" : "No";
        return new SimpleStringProperty(res);
    }

    public SimpleStringProperty getType() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        return biTest.getTypeProperty();
    }

    public void writeConsole(String value) {
        controller.writeTestLog(this, value);
    }

    public ObservableValue<String> getUserLogin() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        return Utils.stringToProperty(biTest.getUserLogin());
    }

    private boolean checkSerials() {
        // check uptime
        String sUptime = sendSshCommand("getUptimeMs");
        long uptime = 0;
        if (sUptime != null && !sUptime.isEmpty()) {
            uptime = Long.parseLong(sUptime);
        } else {
            writeConsole("Can't get system uptime");
            return false;
        }
        if (uptime < 30000) {
            writeConsole(String.format("Uptime is: %s. Waiting for 3 minutes", Utils.formatHMSM(uptime)));
            return false;
        }
        writeConsole("Uptime is: " + Utils.formatHMSM(uptime));
        checkISduFlag();
        return writeComExSN() && writeMac() && writeFlashSn() && writeUpperSn();
    }

    private boolean checkISduFlag() {
        if (checkSerialsMap.get("ISduFlag") != null && checkSerialsMap.get("ISduFlag")) {
            return true;
        }
        String flag = sendSshCommand("getIsduFlag");
        if (flag != null && !flag.isEmpty()) {
            isISduFlag = flag.trim().equals("1");
            writeConsole(String.format("%-10s: %s","ISduFlag", isISduFlag));
        } else {
            writeConsole("ISduFlag not found");
        }
        checkSerialsMap.put("ISduFlag", isISduFlag);
        return false;
    }

    private boolean writeComExSN() {
        if (checkSerialsMap.get("ComExSn") != null && checkSerialsMap.get("ComExSn")) {
            return true;
        }
        String sn = sendSshCommand("getComExSn");
        if (sn != null && !sn.isEmpty()) {
            try {
                sn = Long.valueOf(sn).toString();
                BowlModule bowlModule = getIsduh().getBowlModule();
                bowlModule.setComEx(sn);
                boolean res = new BowlModuleService().saveOrUpdate(bowlModule);
                if (res) {
                    writeConsole(String.format("%-10s: %s", "ComEx SN", sn));
                } else {
                    writeConsole("Can't save bowl module");
                }
                checkSerialsMap.put("ComExSn", res);
                return res;
            } catch (CustomException e) {
                LOGGER.error("writeComExSN", e);
                MsgBox.msgWarning(e.getMessage());
            }
        }
        writeConsole("ComEx SN: can't get serial");
        return false;
    }

    private boolean writeFlashSn() {
        if (checkSerialsMap.get("FlashSn") != null && checkSerialsMap.get("FlashSn")) {
            return true;
        }
        String sn = null;
        String tmp = sendSshCommand("getFlashMemorySn");
        if (tmp != null) {
            sn = tmp.split("=")[1];
        }
        if (sn != null && !sn.isEmpty()) {
            try {
                BowlModule bowlModule = getIsduh().getBowlModule();
                bowlModule.setFlash(sn);
                boolean res = new BowlModuleService().saveOrUpdate(bowlModule);
                if (res) {
                    writeConsole(String.format("%-10s: %s", "Flash SN", sn));
                } else {
                    writeConsole("Can't save bowl module");
                }
                checkSerialsMap.put("FlashSn", res);
                return res;
            } catch (CustomException e) {
                LOGGER.error("writeFlashSn", e);
                MsgBox.msgWarning(e.getMessage());
            }
        }
       return false;
    }

    private boolean writeMac() {
        if (checkSerialsMap.get("MacAddress") != null && checkSerialsMap.get("MacAddress")) {
            return true;
        }
        String mac = sendSshCommand("getMacAddress");
        if (mac != null && !mac.isEmpty()) {
            try {
                mac = mac.toUpperCase().trim();
                BowlModule bowlModule = getIsduh().getBowlModule();
                bowlModule.setMac(mac);
                boolean res = new BowlModuleService().saveOrUpdate(bowlModule);
                if (res) {
                    writeConsole(String.format("%-10s: %s", "MAC", mac));
                } else {
                    writeConsole("Can't save bowl module");
                }
                checkSerialsMap.put("MacAddress", res);
                return res;
            } catch (CustomException e) {
                LOGGER.error("writeFlashSn", e);
                MsgBox.msgWarning(e.getMessage());
            }
        }
        return false;
    }

    private boolean writeUpperSn() {
        if (checkSerialsMap.get("UpperSn") != null && checkSerialsMap.get("UpperSn")) {
            return true;
        }
        String sn = sendSshCommand("getUpperSensorSn");
        if (sn != null && !sn.isEmpty()) {
            Pattern p = Pattern.compile("(?<=UpperUnitSerialNumber=)(\\d){10}$");
            Matcher m = p.matcher(sn);
            if (m.find()) {
                sn = String.format("AM%s", m.group(0));
                try {
                    Isduh currentIsduh = getIsduh();
                    UpperSensorModuleService upperService = new UpperSensorModuleService();
                    IsduhService isduhService = new IsduhService();
                    UpperSensorModule upperSensorModule = upperService.findBySn(sn);
                    if (upperSensorModule != null) {
                        if (upperSensorModule.getModule().equals(currentIsduh.getUpperSensorModuleSn())) {
                            writeConsole(String.format("%-10s: %s", "Upper SN", sn));
                            return true;
                        }
                        Isduh tmpIsduh = isduhService.findByUpperSensorModule(sn);
                        if (tmpIsduh != null) {
                            MsgBox.msgInfo(String.format("Upper sensor SN: %s\nalready use in system SN: %s\n" +
                                    "Will replace to this system.", sn, tmpIsduh.getSn()));
                            tmpIsduh.setUpperSensorModule(null);
                            currentIsduh.setUpperSensorModule(upperSensorModule);
                            List<Object> forSave = new ArrayList<>();
                            forSave.add(tmpIsduh);
                            forSave.add(currentIsduh);
                            isduhService.saveOrUpdate(forSave);
                            writeConsole(String.format("Upper sensor SN: %s moved from system %s to %s",
                                    sn, tmpIsduh.getSn(), currentIsduh.getSn()));
                            return true;
                         }
                    }

                    upperSensorModule = new UpperSensorModule();
                    upperSensorModule.setModule(sn);
                    upperSensorModule.setUser(getCurrentUser());
                    upperService.saveOrUpdate(upperSensorModule);
                    currentIsduh.setUpperSensorModule(upperSensorModule);
                    isduhService.saveOrUpdate(currentIsduh);
                    writeConsole(String.format("%1$-15s: s", "Upper SN", sn));
                    checkSerialsMap.put("UpperSn", true);
                    return true;
                } catch (CustomException e) {
                    LOGGER.error("writeUpperSn", e);
                    MsgBox.msgWarning(e.getMessage());
                }
            }
        }
        return false;
    }

    private String sendSshCommand(String key) {
        String res = "";
        String cmd = iniCmd.getString(key);
        if (cmd == null) {
            MsgBox.msgWarning("Command by key %s not found.\n" +
                    "Please check strings.ini file and try again");
            return null;
        }
        if (sshClient == null) {
            sshClient = new SshClient(getBiNetName().getValue(), settings.getSshUser(), settings.getSshPass(), null);
        }

        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<Object> task = () -> sshClient.execSingleCommand(cmd);
        Future<Object> future = executor.submit(task);
        try {
            res = (String) future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            return null;
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("sendSshCommand", e);
            MsgBox.msgException(e);
        } finally {
            future.cancel(true);
        }
        executor.shutdown();
        return res.trim();
    }

    public boolean isOnline() {
        return onlineStatus == 1;
    }

    private User getCurrentUser() {
        return controller.getMainApp().getCurrentUser();
    }

    private static class Ssh extends AsshClient {
        public Ssh(String hostname, String username, String password) {
            super(hostname, username, password);
        }
    }
}
