package xsightassembler.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.models.BiTest;
import xsightassembler.models.LogItem;


import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

public class BiLogAnalyzer extends Task<FilteredList<LogItem>> {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private BiTest biTest;
    private File[] files;
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private final ObservableList<LogItem> logData = FXCollections.observableArrayList();
    private final Settings settings = Utils.getSettings();
    private long startShift;
    private boolean isIbit = false;
    private Ssh ssh;


    public BiLogAnalyzer(BiTest biTest) {
        this.biTest = biTest;
    }

    public BiLogAnalyzer(List<File> filesList) {
        this.files = filesList.toArray(new File[0]);
    }

    @Override
    protected FilteredList<LogItem> call() throws Exception {
        try {
            if (biTest == null) {
                return parseLog(files);
            } else if (biTest.getStartDate() != null) {
                String netName = biTest.getNetNameProperty().getValue();
                if (settings.getNamePostfix() != null && !settings.getNamePostfix().isEmpty()) {
                    netName = netName + settings.getNamePostfix();
                }
                ssh = new Ssh(netName, settings.getSshUser(), settings.getSshPass());
                String filename2 = ssh.getFile(settings.getSftpFolder(), "messages.1");
                String filename = ssh.getFile(settings.getSftpFolder(), "messages");
                ssh.close();
                File[] files = filename2 != null ? new File[]{new File(filename2), new File(filename)}:
                        new File[]{new File(filename)};
                return parseLog(files);

            }
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    private FilteredList<LogItem> parseLog(File[] filesList) {
        try {
            assert settings != null;
            startShift = TimeUnit.MINUTES.toMillis(settings.getStartAnalyzeShiftInt());
        } catch (NullPointerException e) {
            startShift = 0;
        }
        try {
            List<String> allLines = new ArrayList<>();
            Arrays.sort(filesList, Comparator.comparingLong(File::lastModified));
            for (File f: filesList) {
                try {
                    allLines.addAll(Files.readAllLines(Paths.get(f.getPath())));
                } catch (MalformedInputException e) {
                    MsgBox.msgWarning("Incorrect log file found:\n" + f.getName());
                    return null;
                }

            }

            int lineNum = 1;
            Date dateLogItem;
            String sourceLogItem = "";
            Matcher m, mSour;
            for (String line : allLines) {
                String fullMsg = line;
                m = Strings.pLogDateTime.matcher(line);
                if (m.find()) {
                    // getting item date
                    dateLogItem = formatter.parse(m.group(1));
                    // if date event < start test date
                    if (biTest != null &&
                            (dateLogItem.getTime() < biTest.getStartDate().getTime() + startShift)) {
                        continue;
                    }
                    line = line.replace(m.group(1), "").trim();

                    // getting item source
                    mSour = Strings.pLogSource.matcher(line);
                    if (mSour.find()) {
                        sourceLogItem = mSour.group(1);
                        line = line.replace(sourceLogItem, "").trim();
                    }

                    String errType = null;
                    LogItem logItem;
                    if (line.contains("CSampler: Performing monthly IBIT")) {
                        isIbit = true;
                    } else if (line.contains("CSampler: Performing PBIT")) {
                        errType = "PBIT";
                    } else if (line.contains("BitReport:")) {
                        errType = "BitReport";
                        line = line.replace("BitReport:", "").trim();
                    } else if (line.toLowerCase().contains("reset")) {
                        errType = "Reset";
                    } else if (line.toLowerCase().contains("recover")) {
                        errType = "Recovery";
                    } else if (line.toLowerCase().contains("error")) {
                        errType = "Error";
                    } else if (line.toLowerCase().contains("failure")) {
                        errType = "Failure";
                    } else if (line.toLowerCase().contains("end of ibit")) {
                        isIbit = false;
                        errType = "IBIT";
                    } else {
                        // getting error type
                        Matcher mErr = Strings.pLogErrType.matcher(line);
                        if (mErr.find()) {
                            errType = mErr.group(1).trim();
                            line = line.replace(errType, "").trim();
                        }
                    }
                    if (errType == null) {
                        logItem = new LogItem(lineNum, dateLogItem, sourceLogItem.replace(":", "").trim(),
                                line.trim(), fullMsg, isIbit);
                    } else {
                        logItem = new LogItem(lineNum, dateLogItem, sourceLogItem.replace(":", "").trim(),
                                errType, line.trim(), fullMsg, isIbit);
                    }
                    logData.add(logItem);
                    lineNum++;
                } else {
                    MsgBox.msgWarning("Can't parse string\n" + line);
                    break;
                }

            }
            System.out.println("Log size: " + allLines.size() + " items count: " + logData.size());
            return new FilteredList<>(logData);
        } catch (IOException | ParseException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgError(e.getLocalizedMessage());
        }
//        finally {
//            try {
//                Thread.sleep(10000);
//                if (path != null) {
//                    Files.deleteIfExists(Paths.get(path));
//                }
//                if (path2 != null) {
//                    Files.deleteIfExists(Paths.get(path2));
//                }
//            } catch (IOException | InterruptedException e) {
//                LOGGER.error("Exception", e);
//                MsgBox.msgException(e);
//            }
//
//        }
        return null;
    }

    private static class Ssh extends AsshClient {
        public Ssh(String hostname, String username, String password) {
            super(hostname, username, password);
        }
    }

}
