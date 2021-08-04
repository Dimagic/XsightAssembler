package xsightassembler.view;

import com.sun.javafx.stage.StageHelper;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.models.BiTest;
import xsightassembler.models.BowlModule;
import xsightassembler.models.Isduh;
import xsightassembler.services.BowlModuleService;
import xsightassembler.services.IsduhService;
import xsightassembler.services.bi.BiTestService;
import xsightassembler.utils.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BiJournalController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private final IsduhService isduhService = new IsduhService();
    private final BiTestService testService = new BiTestService();
    private MainApp mainApp;
    private Stage stage;
    private boolean isShutdown;
    private final ObservableList<BiTestWorker> runningTestList = FXCollections.observableArrayList();
    private final ObservableList<String> logList = FXCollections.observableArrayList();
    private final FilteredList<String> filteredList = new FilteredList<>(logList);
    private FilteredList<BiTest> completeFilteredList;

    private BiTestWorker btw;

    @FXML
    private TableView<BiTest> tCompleteJournal;
    @FXML
    private TextArea testLog;
    @FXML
    private DatePicker dateFrom;
    @FXML
    private DatePicker dateTo;
    @FXML
    private TableColumn<Number, Number> columnNumberComplete;
    @FXML
    private TableColumn<BiTest, String> unplugDateComplete;
    @FXML
    private TableColumn<BiTest, String> labNumComplete;
    @FXML
    private TableColumn<BiTest, String> netNameComplete;
    @FXML
    private TableColumn<BiTest, String> stageComplete;
    @FXML
    private TableColumn<BiTest, String> statusComplete;
    @FXML
    private TableColumn<BiTest, String> commentComplete;
    @FXML
    private TableColumn<BiTest, String> userComplete;
    @FXML
    private TableColumn<BiTest, String> typeComplete;
    @FXML
    private ImageView refreshImg;
    @FXML
    private TextField filterField;

    @FXML
    private TableView<BiTestWorker> tRunningTests;
    @FXML
    private TableColumn<BiTestWorker, String> labNumColumn;
    @FXML
    private TableColumn<BiTestWorker, String> netNameColumn;
    @FXML
    private TableColumn<BiTestWorker, String> stageColumn;
    @FXML
    private TableColumn<BiTestWorker, String> typeColumn;
    @FXML
    private TableColumn<BiTestWorker, String> plugDateColumn;
    @FXML
    private TableColumn<BiTestWorker, String> coolerColumn;
    @FXML
    private TableColumn<BiTestWorker, String> icrColumn;
    @FXML
    private TableColumn<BiTestWorker, String> ISDUflagColumn;
    @FXML
    private TableColumn<BiTestWorker, String> snCheckColumn;
    @FXML
    private TableColumn<BiTestWorker, String> startDateColumn;
    @FXML
    private TableColumn<BiTestWorker, Double> progressColumn;
    @FXML
    private TableColumn<BiTestWorker, String> stateColumn;
    @FXML
    private TableColumn<BiTestWorker, String> userColumn;
    @FXML
    private TabPane journalPane;
    @FXML
    private Tab generalTab;
    @FXML
    private Tab journalTab;

    @FXML
    private Label stage1PassLbl;
    @FXML
    private Label stage2PassLbl;
    @FXML
    private Label stage1FailLbl;
    @FXML
    private Label stage2FailLbl;
    @FXML
    private Label totalPassLbl;
    @FXML
    private Label totalFailLbl;
    @FXML
    private Label totalStage1Lbl;
    @FXML
    private Label totalStage2Lbl;
    @FXML
    private Label totalTotalLbl;


    @FXML
    private void initialize() {
        tRunningTests.setTableMenuButtonVisible(true);

        testLog.setEditable(false);
        ContextMenu consoleMenu = new ContextMenu();
        MenuItem mClear = new MenuItem("Clear console");
        mClear.setOnAction((event) -> logList.clear());
        consoleMenu.getItems().addAll(mClear);
        testLog.setContextMenu(consoleMenu);

        initDate();
        isShutdown = false;

        Tooltip.install(refreshImg, new Tooltip("Refresh table"));
        refreshImg.setImage(new Image(Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("refresh.png"))));
        refreshImg.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            stage.getScene().setCursor(Cursor.HAND);
            event.consume();
        });
        refreshImg.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            stage.getScene().setCursor(Cursor.DEFAULT);
            event.consume();
        });
        refreshImg.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            fillCompleteTable();
            RotateTransition rt = new RotateTransition(Duration.millis(300), refreshImg);
            rt.setByAngle(360);
            rt.setCycleCount(1);
            rt.setInterpolator(Interpolator.LINEAR);
            rt.play();
            event.consume();
        });

        tRunningTests.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tRunningTests.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tRunningTests.getColumns().forEach(c -> c.setSortable(false));

//        running test table
        labNumColumn.setCellValueFactory(cellData -> cellData.getValue().getLabNumString());
        netNameColumn.setCellValueFactory(cellData -> cellData.getValue().getBiNetName());
        stageColumn.setCellValueFactory(cellData -> cellData.getValue().getStageString());
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().getType());
        plugDateColumn.setCellValueFactory(cellData -> cellData.getValue().getPlugDateString());
        coolerColumn.setCellValueFactory(cellData -> cellData.getValue().getCoolerStatus());
        icrColumn.setCellValueFactory(cellData -> cellData.getValue().getIcrStatus());
        ISDUflagColumn.setCellValueFactory(cellData -> cellData.getValue().getISDUFlag());
        snCheckColumn.setCellValueFactory(cellData -> cellData.getValue().getSnCheckStatus());
        startDateColumn.setCellValueFactory(cellData -> cellData.getValue().getStartDateString());
        userColumn.setCellValueFactory(cellDate -> cellDate.getValue().getUserLogin());

        stateColumn.setCellValueFactory(new PropertyValueFactory<>("message"));

        setPassFailCellFactory(coolerColumn);
        setPassFailCellFactory(icrColumn);
        setPassFailCellFactory(ISDUflagColumn);
        setPassFailCellFactory(snCheckColumn);

        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));
        progressColumn.setCellFactory(param -> new TableCell<BiTestWorker, Double>() {
            final ProgressBar bar = new ProgressBar();

            public void updateItem(Double progress, boolean empty) {
                if (!empty) {
                    try {
                        BiTestWorker btw = (BiTestWorker) this.getTableRow().getItem();
                        if (progress == -1) {
                            bar.setStyle("-fx-accent: #0096C9");
                        } else if (progress == 1.0) {
                            if (btw.getStartDate() == null) {
                                bar.setStyle("-fx-accent: yellow");
                            } else {
                                if (btw.isTestFail() || btw.isIbitCountError()) {
                                    bar.setStyle("-fx-accent: red");
                                } else {
                                    bar.setStyle("-fx-accent: palegreen");
                                }
                            }
                        } else if (btw.isTestFail() || btw.isIbitCountError()) {
                            bar.setStyle("-fx-accent: orange");
                        }
                    } catch (NullPointerException ignored) {
                    }
                    bar.setProgress(progress);
                    setGraphic(bar);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        labNumComplete.setCellValueFactory(cellData -> cellData.getValue().getLabNumProperty());
        typeComplete.setCellValueFactory(cellData -> cellData.getValue().getTypeProperty());
        unplugDateComplete.setCellValueFactory(cellData -> cellData.getValue().getUnplugDateProperty());
        netNameComplete.setCellValueFactory(cellData -> cellData.getValue().getNetNameProperty());
        stageComplete.setCellValueFactory(cellData -> cellData.getValue().getStageProperty());
        statusComplete.setCellValueFactory(cellData -> cellData.getValue().getStatusProperty());
        commentComplete.setCellValueFactory(cellData -> cellData.getValue().getCommentProperty());
        userComplete.setCellValueFactory(cellDate -> cellDate.getValue().getUserProperty());

        columnNumberComplete.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(tCompleteJournal.
                getItems().indexOf(column.getValue()) + 1));

        //        complete test table
        tCompleteJournal.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tCompleteJournal.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        filterField.textProperty().addListener((observable, oldValue, newValue) -> filter(newValue));

        // Log console init
        tRunningTests.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            btw = newSelection;
            if (btw != null && newSelection.getBiTest() != null) {
                filteredList.setPredicate(c -> c.contains(newSelection.getBiNetName().getValue()));
            } else {
                logList.removeIf(s -> s.contains(String.format("Lab#%s", newSelection.getLabNum())));
                testLog.clear();
            }
        });

        filteredList.addListener((ListChangeListener<String>) change -> {
            testLog.clear();
            filteredList.forEach(c -> testLog.appendText(c));
        });

        tCompleteJournal.setRowFactory(tv -> {
            TableRow<BiTest> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem mi1 = new MenuItem("Add to lab station");
            mi1.setOnAction((event) -> {
                List<String> labNums = new ArrayList<>();
                tRunningTests.getItems().forEach(e -> labNums.add(Integer.toString(e.getLabNum())));
                String res = MsgBox.msgChoice("Select lab number", "", labNums);
                if (res != null) {
                    addBiTest(Integer.parseInt(res), row.getItem().getIsduh());
                }
            });
            cm.getItems().addAll(mi1);
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    cm.show(tRunningTests, event.getScreenX(), event.getScreenY());
                    cm.setHideOnEscape(true);
                    cm.setAutoHide(true);
                } else if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
                    cm.hide();
                }
            });
            return row;
        });

        tRunningTests.setRowFactory(tv -> {
            TableRow<BiTestWorker> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem mi1 = new MenuItem("Log analyzer");
            mi1.setOnAction((event) -> {
                if (row.getItem().isOnline()) {
                    mainApp.showLogView(row.getItem());
                }
            });
            MenuItem mi2 = new MenuItem("Update logs");
            mi2.setOnAction((event) -> {
                if (row.getItem().isOnline()) {
                    row.getItem().forceLogAnalyzerTask();
                }
            });
            MenuItem mi3 = new MenuItem("Delete item");
            mi3.setOnAction((event) -> {
                BiTestWorker btw = row.getItem();
                String netName = btw.getBiTest().getNetNameProperty().getValue();
                if (btw.getBiTest() != null) {
                    if (btw.getStartDate() != null) {
                        MsgBox.msgInfo("You can't delete started test");
                    } else {
                        if (MsgBox.msgConfirm(String.format("Delete system %s from lab station?", netName))) {
                            try {
                                // close stage if open
                                for (Stage stage : StageHelper.getStages()) {
                                    if (stage.getTitle().contains(netName)) {
                                        stage.hide();
                                        break;
                                    }
                                }
                                testService.delete(btw.getBiTest());
                                btw.setBiTest(null);
                                refreshJournal();
                            } catch (CustomException e) {
                                LOGGER.error("exception", e);
                                MsgBox.msgException(e);
                            }
                        }
                    }
                }
            });
            MenuItem mi4 = new MenuItem("Open assembler");
            mi4.setOnAction((event) -> {
                Isduh isduh = row.getItem().getIsduh();
                if (isduh != null) {
                    mainApp.showAllInOneAssemblerView(isduh);
                }
            });

            cm.getItems().addAll(mi1, mi2, mi3, mi4);
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty() && row.getItem().getIsduh() != null) {
                    cm.show(tRunningTests, event.getScreenX(), event.getScreenY());
                    cm.setHideOnEscape(true);
                    cm.setAutoHide(true);
                } else if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && (!row.isEmpty())) {
                    BiTestWorker rowData = row.getItem();
                    if (rowData.getIsduh() != null) {
                        mainApp.showBiTestView(rowData);
                    } else {
                        addBiTest(row.getItem().getLabNum());
                    }
                } else if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
                    cm.hide();
                }

            });
            return row;
        });

        journalPane.getSelectionModel().selectedItemProperty().addListener(
                (ov, t, t1) -> {
                    if (t1 == journalTab) {
                        fillCompleteTable();
                    } else {
                        refreshJournal();
                    }
                }
        );

//        runningTestList.addListener(new ListChangeListener<BiTestWorker>() {
//            @Override
//            public void onChanged(Change<? extends BiTestWorker> change) {
//                System.out.println("Selection changed: " + change.getList());
//            }
//        });

        tRunningTests.setItems(runningTestList);
        fillTable();
    }

    private void setPassFailCellFactory(TableColumn<BiTestWorker, String> column) {
        column.setCellFactory(param -> new TableCell<BiTestWorker, String>() {
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!isEmpty()) {
                    this.setAlignment(Pos.CENTER);
                    if (item.contains("Yes") || item.contains("Pass")) {
                        this.setTextFill(Color.GREEN);
                    } else {
                        this.setTextFill(Color.RED);
                    }
                    setText(item);
                }
            }
        });
    }

    public void fillTable() {
        tRunningTests.getItems().clear();
        try {
            BiTest biTest;
            BiTestWorker biTestWorker;
            for (int i = 1; i <= Utils.getSettings().getLabCountInt(); i++) {
                biTestWorker = getBtwByLabNum(i);
                biTest = testService.getRunningTestByLabNum(i);
                if (biTest != null) {
                    biTestWorker.setBiTest(biTest);
                }
                runningTestList.add(biTestWorker);
            }
        } catch (CustomException e) {
            LOGGER.error("Fill running table", e);
            MsgBox.msgError(e.getLocalizedMessage());
        }
        runningTestList.forEach(e -> {
            if (getThreadByName(e.getLabNumString().getValue()) == null) {
                Thread t = new Thread(e);
                t.setName(e.getLabNumString().getValue());
                t.setDaemon(true);
                t.start();
            }
        });
        tRunningTests.setItems(runningTestList);
        refreshJournal();
    }

    public void refreshJournal() {
        for (BiTestWorker btw : runningTestList) {
            if (btw.getBiTest() != null && btw.getBiTest().getUnplugDate() != null) {
                btw.setBiTest(null);
            }
        }
        tRunningTests.refresh();
    }

    @FXML
    private void fillCompleteTable() {
        try {
            ObservableList<BiTest> completeTestList = FXCollections.observableArrayList(testService.getCompleteTestBetweenDates(
                    java.sql.Date.valueOf(dateFrom.getValue()),
                    java.sql.Date.valueOf(dateTo.getValue())
            ));
            completeFilteredList = new FilteredList<>(completeTestList);
            if (!filterField.getText().trim().isEmpty()) {
                filter(filterField.getText().trim().toUpperCase());
            }
            tCompleteJournal.setItems(completeFilteredList);
            fillStatistics();
        } catch (CustomException e) {
            LOGGER.error("Fill complete table", e);
            MsgBox.msgError(e.getLocalizedMessage());
        }
    }

    private void filter(String s) {
        completeFilteredList.setPredicate(biTest -> biTest.getNetNameProperty().getValue().contains(s.toUpperCase().trim()));
        fillStatistics();
    }

    private void fillStatistics() {
        FilteredList<BiTest> filteredStatList = new FilteredList<>(tCompleteJournal.getItems());

        filteredStatList.setPredicate(biTest -> biTest.getStatus() == 1 && biTest.getStage() == 1);
        stage1PassLbl.setText(Integer.toString(filteredStatList.size()));

        filteredStatList.setPredicate(biTest -> biTest.getStatus() == -1 && biTest.getStage() == 1);
        stage1FailLbl.setText(Integer.toString(filteredStatList.size()));

        filteredStatList.setPredicate(biTest -> biTest.getStatus() == 1 && biTest.getStage() == 2);
        stage2PassLbl.setText(Integer.toString(filteredStatList.size()));

        filteredStatList.setPredicate(biTest -> biTest.getStatus() == -1 && biTest.getStage() == 2);
        stage2FailLbl.setText(Integer.toString(filteredStatList.size()));

        filteredStatList.setPredicate(biTest -> biTest.getStage() == 1);
        totalStage1Lbl.setText(Integer.toString(filteredStatList.size()));

        filteredStatList.setPredicate(biTest -> biTest.getStage() == 2);
        totalStage2Lbl.setText(Integer.toString(filteredStatList.size()));

        filteredStatList.setPredicate(biTest -> biTest.getStatus() == 1);
        totalPassLbl.setText(Integer.toString(filteredStatList.size()));

        filteredStatList.setPredicate(biTest -> biTest.getStatus() == -1);
        totalFailLbl.setText(Integer.toString(filteredStatList.size()));

        filteredStatList.setPredicate(biTest -> true);
        totalTotalLbl.setText(Integer.toString(filteredStatList.size()));
    }

    private void addBiTest(int labNum) {
        try {
            String sn = MsgBox.msgInputString("Scan or input ISDUH SN");
            if (sn == null || sn.trim().length() == 0) {
                return;
            }
            sn = sn.toUpperCase().trim();

            HashMap<String, Pattern> pSysTypeMap = Utils.getPatternMapByName("pManufIsduh");
            if (pSysTypeMap == null || pSysTypeMap.isEmpty()) {
                MsgBox.msgWarning("Can't get system type pattern");
                return;
            }
            boolean isSnCorrect = false;
            for (String type : pSysTypeMap.keySet()) {
                Matcher m = pSysTypeMap.get(type).matcher(sn.trim().toUpperCase());
                if (m.find()) {
                    isSnCorrect = true;
                    break;
                }
            }
            if (!isSnCorrect) {
                MsgBox.msgWarning("Incorrect ISDUH serial number");
                return;
            }

            Isduh isduh = isduhService.findBySn(sn);
            if (isduh == null) {
                if (MsgBox.msgConfirm(String.format("ISDUH with SN: %s not found.\n" +
                        "Will be add system. Do you want continue?", sn))) {
                    isduh = new Isduh();
                    isduh.setSn(sn);
                    isduh.setUser(mainApp.getCurrentUser());
                    isduhService.save(isduh);
                }
            }
            addBiTest(labNum, isduh);
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgError(e.getLocalizedMessage());
        }
    }

    private void addBiTest(int labNum, Isduh isduh) {
        try {
            if (!checkPlugIsduh(isduh, labNum)) {
                return;
            }

            // Check if bowl module present
            if (isduh.getBowlModule() == null) {
                IniUtils iniUtils = new IniUtils("strings.ini", isduh.getSystemType());
                Pattern p = iniUtils.getPattern("pManufBowl");
                Optional<String> bowlSn = MsgBox.msgInputStringWithValidator("Bowl module not found in assembly.\n" +
                        "Please enter bowl serial number", p);
                if (bowlSn.isPresent()) {
                    BowlModuleService bowlModuleService = new BowlModuleService();
                    BowlModule bowlModule = bowlModuleService.findBySn(bowlSn.get());
                    if (bowlModule == null) {
                        bowlModule = new BowlModule();
                        bowlModule.setModule(bowlSn.get());
                        bowlModule.setDate(new Date());
                        bowlModule.setUser(mainApp.getCurrentUser());
                        bowlModuleService.save(bowlModule);
                    }
                    isduh.setBowlModule(bowlModule);
                    isduhService.saveOrUpdate(isduh);
                }
            }

            BiTest lastTest = testService.getLastTest(isduh);
            String stage;
            // if last test found, stage == 1 and passed -> select stage 2
            if (lastTest != null && ((lastTest.getStatus() == 1 && lastTest.getStage() == 1) ||
                    (lastTest.getStage() == 2))) {
                stage = MsgBox.msgChoice("Please select stage", "Stage",
                        Strings.getStages(), "2");
            } else {
                stage = MsgBox.msgChoice("Please select stage", "Stage", Strings.getStages());
            }
            // if stage not selected
            if (stage == null) {
                return;
            }
            BiTest biTest = new BiTest();
            biTest.setLabNum(labNum);
            biTest.setPlugDate(new Date());
            biTest.setIsduh(isduh);
            biTest.setStage(Integer.parseInt(stage));
            biTest.setUser(mainApp.getCurrentUser());
            testService.save(biTest);
            runningTestList.stream().filter(e -> e.getLabNum() == labNum).findFirst().get().setBiTest(biTest);
//            getBtwByLabNum(labNum).setBiTest(biTest);
            tRunningTests.refresh();
        } catch (CustomException | IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgError(e.getLocalizedMessage());
        }
    }

    private boolean checkPlugIsduh(Isduh isduh, int labNum) {
        for (BiTestWorker btw : runningTestList) {
            if (btw.getBiTest() != null) {
                if (btw.getBiTest().getLabNum() == labNum) {
                    MsgBox.msgWarning(String.format("Lab #%s not empty", labNum));
                    return false;
                }
                if (btw.getBiTest().getIsduh().getSn().equals(isduh.getSn())) {
                    MsgBox.msgWarning(String.format("System SN: %s already placed in Lab#%s",
                            isduh.getSn(), btw.getBiTest().getLabNum()));
                    return false;
                }
            }
        }
        return true;
    }

    private void initDate() {
        final String pattern = "yyyy-MM-dd";
        dateFrom.setShowWeekNumbers(true);
        dateFrom.setConverter(new StringConverter<LocalDate>() {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);

            {
                dateFrom.setPromptText(pattern.toLowerCase());
            }

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });

        dateTo.setShowWeekNumbers(true);
        dateTo.setConverter(new StringConverter<LocalDate>() {
            final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);

            {
                dateTo.setPromptText(pattern.toLowerCase());
            }

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date input = cal.getTime();
        LocalDate curDate = input.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate start = curDate.withDayOfMonth(1);
        LocalDate stop = curDate.withDayOfMonth(curDate.lengthOfMonth());
        dateFrom.setValue(start);
        dateTo.setValue(stop);
    }

    public BiTest getBiTestByLabNum(int labNum) {
        for (BiTestWorker btw : runningTestList) {
            if (btw.getLabNum() == labNum) return btw.getBiTest();
        }
        return null;
    }

    private BiTestWorker getBtwByLabNum(int labNum) {
        for (BiTestWorker btw : runningTestList) {
            if (btw.getLabNum() == labNum) return btw;
        }
        BiTestWorker btw = new BiTestWorker(labNum, this);
        return btw;
    }

    private Thread getThreadByName(String threadName) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName)) return t;
        }
        return null;
    }

    public synchronized void writeTestLog(BiTestWorker btw, String value) {
        String s = String.format("%s Lab#%s %s: %s", Utils.getFormattedTime(new Date()), btw.getLabNum(),
                btw.getBiNetName().getValue(), value.trim());
        Runnable update = () -> {
//            testLog.appendText(s + "\n");
            logList.add(s + "\n");
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    public boolean getShutdown() {
        return isShutdown;
    }

    public void shutdown() {
        runningTestList.clear();
        isShutdown = true;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public MainApp getMainApp() {
        return mainApp;
    }

    public void setStage(Stage stage) {
        this.stage = stage;

    }
}
