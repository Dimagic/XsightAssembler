package xsightassembler.utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.models.BiTest;
import xsightassembler.models.Isduh;
import xsightassembler.models.LogItem;
import xsightassembler.view.BiJournalController;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BiTestWorker extends Task<Void> {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());

    private final BiJournalController controller;
    final BlockingQueue<BiTest> queue = new ArrayBlockingQueue<>(1);
    private ExecutorService
            exService;
    private final Integer labNum;

    private BiTest biTest;
    private MainApp mainApp;
    private String conMsg;
    private String logMsg;
    private long logTimer = 0;
    private BiLogAnalyzer logAnalyzer;
    private FilteredList<LogItem> logItems;
    private Settings settings;
    private HashMap<String, List<String>> analyzerMap;

    public BiTestWorker(Integer labNum, BiJournalController controller) {
        this.labNum = labNum;
        this.controller = controller;
        this.settings = Utils.getSettings();
    }

    @Override
    protected Void call() throws Exception {
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
                switch (Utils.isSystemOnline(ipName)) {
                    case 1:
                        updateConnectionStatus("System online");
                        if (biTest.getStartDate() == null) {
                            startTime = 0;
                        } else {
                            startTime = biTest.getStartDate().getTime();
                        }
                        long startShift = TimeUnit.MINUTES.toMillis(settings.getStartAnalyzeShiftInt());
                        needTime = biTest.getDuration() * 3600000L + startShift;
                        durationTime = System.currentTimeMillis() - startTime;
                        this.updateProgress(durationTime, needTime);
                        logAnalyzerTask(TimeUnit.MILLISECONDS.toMinutes(durationTime));
                        Thread.sleep(10000);
                        break;
                    case 0:
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

    private void logAnalyzerTask(long minutes) {
        if (logTimer == minutes || minutes < settings.getStartAnalyzeShiftInt()) {
            return;
        } else if (logTimer != 0 && (minutes % settings.getLogCheckPeriodInt()) != 0) {
            return;
        }
        exService = Executors.newSingleThreadExecutor();
        logAnalyzer = new BiLogAnalyzer(biTest);
        logAnalyzer.setOnSucceeded(event -> {
            logItems = Utils.setIgnoreFlagInLogs(logAnalyzer.getValue());
            if (logItems != null) {
                logItems.setPredicate(s -> s.getErrType() != null);
                int errors = logItems.size();
                long ignored = logItems.stream().filter(LogItem::isIgnore).count();

                List<LogItem> pduResetList = logItems.stream().filter(s ->
                        s.getFullMsg().contains("PDU Reset Counter")).collect(Collectors.toList());
                Pattern pCounter = Pattern.compile("(?<=Counter:)(.*)(?=$)");
                Matcher m;
                Set<Integer> counterSet = new HashSet<>();
                for (LogItem l: pduResetList){
                    m = pCounter.matcher(l.getFullMsg());
                    if (m.find()) {
                        counterSet.add(Integer.parseInt(m.group(1).trim()));
                    }
                }
                pduResetList.forEach(l -> l.setIgnore(counterSet.size() == 1));

//                for (LogItem item : logItems) {
//                    boolean ifIgnore = analyzerMap.get("ignore").stream().anyMatch(s ->
//                            Pattern.compile(String.format("(.*?)%s(.*)", s)).matcher(item.getFullMsg()).matches());
//                    boolean ifIbitIgnore = analyzerMap.get("ignore_if_ibit").stream().anyMatch(s ->
//                            Pattern.compile(String.format("(.*?)%s(.*)", s)).matcher(item.getFullMsg()).matches()) && item.isIbit();
//                    if (ifIgnore || ifIbitIgnore || item.isIgnore()) {
//                        item.setIgnore(true);
//                        ignored++;
//                    }
//                }

                if (errors == 0) {
                    updateLogStatus(String.format("Errors not found. Last check: %s",
                            Utils.getFormattedTime(new Date())));
                } else {
                    updateLogStatus(String.format("%s of %s errors ignored. Last check: %s",
                            ignored, errors, Utils.getFormattedTime(new Date())));
                }

            }
            logTimer = minutes;
        });

        logAnalyzer.setOnFailed(e -> MsgBox.msgWarning("Log analyzer task failure"));
        exService.execute(logAnalyzer);
        exService.shutdown();
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
        return logItems.stream().anyMatch(t -> !t.isIgnore());
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
        if (!settings.getNamePostfix().trim().isEmpty()) {
            return new SimpleStringProperty(biTest.getNetNameProperty().getValue()
                    + settings.getNamePostfix().trim());
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

    public SimpleStringProperty getType() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        return biTest.getTypeProperty();
    }

    public ObservableValue<String> getUserLogin() {
        if (biTest == null) {
            return new SimpleStringProperty("");
        }
        return Utils.stringToProperty(biTest.getUserLogin());
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}
