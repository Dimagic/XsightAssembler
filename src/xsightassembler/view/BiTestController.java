package xsightassembler.view;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.models.BiNote;
import xsightassembler.models.BiTest;
import xsightassembler.services.bi.BiNoteServise;
import xsightassembler.services.bi.BiTestService;
import xsightassembler.utils.*;
import xsightassembler.utils.tasks.BiTestTimer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipOutputStream;

public class BiTestController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private final BiTestService service = new BiTestService();
    private final BiNoteServise noteServise = new BiNoteServise();
    private BiTestTimer timerTask;
    private Thread logMonitor;
    private Stage stage;
    private MainApp mainApp;
    private boolean isShutdown;
    private BiTestWorker btw;
    private BiTest biTest;
    private boolean passFailClicked = false;
    private BiTestController controller = this;
    private BiJournalController journalController;
    private String isduhNetName;
    private Settings settings = Utils.getSettings();
    private HashMap<Integer, String> passFailMap = new HashMap<>();

    @FXML
    private TextArea logMonArea;
    @FXML
    private Label isduhLbl;
    @FXML
    private Label netNameLbl;
    @FXML
    private Label startTestLbl;
    @FXML
    private Label testTimeLbl;
    @FXML
    private Button passBtn;
    @FXML
    private Button failBtn;
    @FXML
    private Button startBtn;
    @FXML
    private Button netNameCopyBtn;
    @FXML
    private Button downloadLogsBtn;
    @FXML
    private ChoiceBox<String> stageChoice;
    @FXML
    private ChoiceBox<String> coolerChoice;
    @FXML
    private ChoiceBox<String> icrChoice;
    @FXML
    private Spinner<Integer> testDuration;
    @FXML
    private ComboBox<String> commandBox;
    @FXML
    private Button sendCommandBtn;
    @FXML
    private TreeView<BiNote> noteTree;
    @FXML
    private TextField noteField;
    @FXML
    private Button addNoteBtn;


    @FXML
    private void initialize() {
        passFailMap = Strings.getPassFailMap();
        logMonArea.setEditable(false);
        addNoteBtn.setDisable(true);
        noteTree.setShowRoot(false);

        isShutdown = false;
        stageChoice.setItems(FXCollections.observableArrayList(Strings.getStages()));

        commandBox.setItems(FXCollections.observableArrayList(Strings.getIsduhCommandsMap().values()));
        commandBox.getSelectionModel().selectFirst();

        testDuration.setValueFactory(new SpinnerValueFactory.
                IntegerSpinnerValueFactory(1, 99));

        coolerChoice.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    biTest.setCooler(Utils.mapGetKeyByValue(passFailMap, newValue));
                    saveBiTest();
                    journalController.refreshJournal();
                    setStartBtnEnable();
                });

        icrChoice.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    biTest.setIcr(Utils.mapGetKeyByValue(passFailMap, newValue));
                    saveBiTest();
                    journalController.refreshJournal();
                    setStartBtnEnable();
                });

        stageChoice.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (oldValue != null && !newValue.equals(String.valueOf(biTest.getStage()))) {
                        if (MsgBox.msgConfirm(String.format("Stage will change to %s\nAre you sure?", newValue))) {
                            addBiTestNote("Stage changed to " + newValue);
                            biTest.setStage(Integer.parseInt(newValue));
                            saveBiTest();
                            journalController.refreshJournal();
                        } else {
                            stageChoice.getSelectionModel().select(String.valueOf(biTest.getStage()));
                        }
                    }
                });

        noteTree.setCellFactory(param -> new TreeCell<BiNote>() {
            @Override
            protected void updateItem(BiNote item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item.getNote() == null) {
                        setText("");
                    } else {
                        setText(item.getNote());
                    }

                }
            }
        });

        testDuration.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                biTest.setDuration(newValue);
                saveBiTest();
            }
        });

        noteField.textProperty().addListener((observable, oldValue, newValue) -> {
            addNoteBtn.setDisable(noteField.getText().trim().isEmpty());
        });

        passBtn.setOnAction(e -> setBiTestStatus(1));
        failBtn.setOnAction(e -> setBiTestStatus(-1));
        downloadLogsBtn.setOnAction(e -> downloadLogs());
        startBtn.setOnAction(e -> startBtnClick());
        netNameCopyBtn.setOnAction(e -> setStringToClipboard(netNameLbl.getText()));
        sendCommandBtn.setOnAction(e ->
                sendCommand(Utils.mapGetKeyByValue(Strings.getIsduhCommandsMap(), commandBox.getValue()))
        );
        addNoteBtn.setOnAction(e -> addBiTestNote());
        startLogMonitorController();
    }

    public void setBiTestWorker(BiTestWorker btw) {
        this.btw = btw;
        this.biTest = btw.getBiTest();
        if (settings.getNamePostfix() != null && !settings.getNamePostfix().isEmpty()) {
            this.isduhNetName = biTest.getNetNameProperty().getValue() + settings.getNamePostfix();
        } else {
            this.isduhNetName = biTest.getNetNameProperty().getValue();
        }


        stage.setTitle(String.format("Lab #%s: %s", btw.getLabNumString().getValue(), isduhNetName));
        isduhLbl.setText(biTest.getIsduh().getSn());
        netNameLbl.setText(isduhNetName);
        initChoiceBox(coolerChoice, biTest.getCooler());
        initChoiceBox(icrChoice, biTest.getIcr());
        stageChoice.getSelectionModel().select(Integer.toString(biTest.getStage()));

        startTestLbl.setText(Utils.getFormattedDate(biTest.getStartDate()));
        fillNotes();

        if (biTest.getDuration() == 0) {
            setDefaultDuration();
        }
        testDuration.getValueFactory().setValue(biTest.getDuration());

        stage.setOnHidden(e -> {
            journalController.refreshJournal();
        });

        startTimer();

    }

    private void addBiTestNote(String s) {
        BiNote biNote = new BiNote();
        biNote.setNoteDate(new Date());
        biNote.setUser(biTest.getUser());
        biNote.setNote(s);
        biNote.setBiTest(biTest);
        try {
            noteServise.saveOrUpdate(biNote);
            biTest.addNote(biNote);
            service.saveOrUpdate(biTest);
            noteField.clear();
            fillNotes();
        } catch (CustomException e) {
            LOGGER.error("exception", e);
            MsgBox.msgError(e.getLocalizedMessage());
        }
    }

    private void addBiTestNote() {
        addBiTestNote(noteField.getText().trim());
    }

    private void fillNotes() {
        try {
            TreeItem<BiNote> rootTreeNode = new TreeItem<>(new BiNote());

            List<BiTest> testList = service.getByIsduh(biTest.getIsduh());
            for (BiTest test : testList) {
                String testName = String.format("%s Stage: %s Status: %s", Utils.getFormattedDate(test.getPlugDate()),
                        test.getStage(), Strings.getPassFailMap().get(test.getStatus()));
                BiNote note = new BiNote();
                note.setNote(testName);
                note.setNoteDate(test.getPlugDate());
                TreeItem<BiNote> testItem = new TreeItem<>(note);
                testItem.setExpanded(true);
                List<BiNote> noteList = noteServise.findByBiTest(test);
                for (BiNote noteItem : noteList) {
                    testItem.getChildren().add(new TreeItem<>(noteItem));
                }
                rootTreeNode.getChildren().add(testItem);
            }
            rootTreeNode.getChildren().sort(Comparator.comparing(t -> t.getValue().getNoteDate()));

            noteTree.setRoot(rootTreeNode);
        } catch (CustomException e) {
            LOGGER.error("Fill notes", e);
            MsgBox.msgException(e);
        }
    }

    private void downloadLogs() {
        try {
            SshClient jssh = new SshClient(isduhNetName, settings.getSshUser(),
                    settings.getSshPass(), controller);
            if (jssh.getSession() == null) {
                return;
            }
            List<String> fileList = Arrays.asList("messages", "messages.1", "messages.2.gz");
            String systemFolder = String.format("%s (Lab#%s stage#%s start#%s)", isduhNetName,
                    btw.getLabNumString().getValue(), btw.getStageString().getValue(),
                    btw.getStartDateString().getValue().replace(":", "_"));
            String destFolder = String.format("%s%s\\%s\\", Utils.getSettings().getLogFolder(),
                    Utils.getFormattedDateForFolder(new Date()), systemFolder);
            Files.createDirectories(Paths.get(destFolder));
            jssh.downloadFiles(Utils.getSettings().getSftpFolder(), fileList, destFolder);
            jssh.close();
            zipLogs(destFolder);
        } catch (IOException e) {
            LOGGER.error("runLogAnalyzer", e);
            MsgBox.msgException(e);
        }
    }

    private void zipLogs(String sourceFile) {
        try {

            String path = "smb://172.16.28.18/isduh_logs/";
            String file = "./tmp/logs.zip";
            File dir = new File("./tmp");
            if (!dir.exists()) {
                dir.mkdir();
            }
            FileOutputStream fos = new FileOutputStream(file);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFile);
            Utils.zipFile(fileToZip, fileToZip.getName(), zipOut);
            zipOut.close();
            fos.close();

//            SshClient ssh = new SshClient("172.16.28.18", "sftpuser", "XSpc$1234", this);
//            ssh.uploadFile("./tmp/logs.zip", "/samba/isduh_logs/");
//            ssh.close();
        } catch (IOException e) {
            LOGGER.error("zipLogs", e);
            MsgBox.msgException(e);
        }
    }

    private void setBiTestStatus(int status) {
        if (status == -1 && biTest.getNotes().size() == 0) {
            MsgBox.msgInfo("Please add error description.");
            return;
        }
        biTest.setStatus(status);
        biTest.setUnplugDate(new Date());
        saveBiTest();

        passFailClicked = true;
        biTest = null;
        stage.close();
        mainApp.getBiJournalController().refreshJournal();
    }

    private void setStringToClipboard(String s) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(s);
        clipboard.setContent(content);
    }

    private void setStartBtnEnable() {
        try {
            boolean isCoolerPass = Utils.mapGetKeyByValue(Strings.getPassFailMap(), coolerChoice.getSelectionModel().getSelectedItem()) == 1;
            boolean isIcrPass = Utils.mapGetKeyByValue(Strings.getPassFailMap(), icrChoice.getSelectionModel().getSelectedItem()) == 1;

            startBtn.setDisable(!isCoolerPass || !isIcrPass);
            setPassBtnEnable();
        } catch (NullPointerException e) {
            startBtn.setDisable(true);
        }

    }

    private void setPassBtnEnable() {
        passBtn.setDisable(biTest.getStartDate() == null);
    }

    private void saveBiTest() {
        try {
            service.saveOrUpdate(biTest);
        } catch (CustomException e) {
            LOGGER.error("saveBiTest", e);
            MsgBox.msgException(e);
        }
    }

    private void setDefaultDuration() {
        switch (biTest.getStage()) {
            case 1:
                biTest.setDuration(8);
                break;
            case 2:
                biTest.setDuration(2);
                break;
        }
    }

    public synchronized void addToConsole(String s) {
        Runnable update = () -> logMonArea.appendText(s + "\n");
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private void sendCommand(Integer cmdId) {
        SshClient jssh;
        switch (cmdId) {
            case 1:
                try {
                    Runtime.getRuntime().exec(String.format("%s -ssh %s@%s -pw %s",
                            settings.getPuttyFile(), settings.getSshUser(),
                            isduhNetName, settings.getSshPass()));
                } catch (IOException e) {
                    LOGGER.error("exception", e);
                    MsgBox.msgException(e);
                }
                break;
            case 2:
                try {
                    Runtime.getRuntime().exec(String.format("%s rtsp://%s:554/SDUVideoHighQuality",
                            settings.getVlcFile(), isduhNetName));
                } catch (IOException e) {
                    LOGGER.error("exception", e);
                    MsgBox.msgException(e);
                }
                break;
            case 3:
//                IsduhMenuExecutor menuExecutor = new IsduhMenuExecutor(biTest, controller);
//                menuExecutor.execMenu();

//                BiLogAnalyzer analyzerTask = new BiLogAnalyzer(biTest, this);
//                analyzerTask.setOnSucceeded(e -> System.out.println("Done"));
//                analyzerTask.run();

                VideoPlayer player = new VideoPlayer(btw);
//                player.saveToFile();
                player.getVideo();
                break;
        }
    }

    private void startTimer() {
        timerTask = new BiTestTimer(biTest.getStartDate(), this);
        testTimeLbl.textProperty().bind(timerTask.messageProperty());

        ExecutorService executorService
                = Executors.newFixedThreadPool(1);
        executorService.execute(timerTask);
        executorService.shutdown();
    }

    private void startBtnClick() {
        if (biTest.getStartDate() != null) {
            if (!MsgBox.msgConfirm("System test already run.\nDo you want restart it?")) {
                return;
            } else {
                addBiTestNote("Test was restarted at " + Utils.getFormattedDate(new Date()));
            }
        }
        // ToDo: update state on journal if test restarted
        setStartDate();
        startTimer();
    }

    private void startLogMonitorController() {
        new Thread(() -> {
            while (!isShutdown) {
                try {
                    System.out.println(">>>>>>>>>>>>> Check");
                    if (logMonitor == null || !logMonitor.isAlive()) {
                        System.out.println(">>>>>>>>>>>>> Start");
                        startLogMonitor();
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startLogMonitor() {
        logMonitor = new Thread(() -> {
            SshClient jssh = new SshClient(isduhNetName, settings.getSshUser(),
                    settings.getSshPass(), controller);
            List<String> commands = new ArrayList<String>();
            commands.add("mon_log");

            if (!jssh.executeCommands(commands)) {
                addToConsole("Can't execute command. Will retry after 15 seconds.");
                for (int i = 0; i < 15; i++) {
                    if (isShutdown) {
                        return;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        LOGGER.error("Exception", e);
                        MsgBox.msgException(e);
                    }
                }
            } else {
                jssh.close();
            }
        });
        logMonitor.start();
    }

    private void setStartDate() {
        biTest.setStartDate(new Date());
        try {
            service.saveOrUpdate(biTest);
            setBiTestWorker(btw);
            btw.setBiTest(biTest);
            journalController.refreshJournal();
        } catch (CustomException e) {
            LOGGER.error("setStartDate", e);
            MsgBox.msgException(e);
        }
    }

    private void initChoiceBox(ChoiceBox<String> choice, int val) {
        choice.getItems().add(passFailMap.get(0));
        choice.getItems().add(passFailMap.get(1));
        choice.getItems().add(passFailMap.get(-1));
        choice.getSelectionModel().select(passFailMap.get(val));
    }

    public boolean isPassFailClicked() {
        return passFailClicked;
    }

    public Boolean getShutdown() {
        return isShutdown;
    }

    public BiTest getBiTest() {
        return biTest;
    }

    public void shutdown() {
        isShutdown = true;

//        btw.setStopFlag(true);
    }

    public void setJournalController(BiJournalController journalController) {
        this.journalController = journalController;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setMinWidth(600);
        this.stage.setMinHeight(550);
        this.stage.setOnCloseRequest(event -> shutdown());
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

}
