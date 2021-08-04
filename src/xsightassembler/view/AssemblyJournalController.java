package xsightassembler.view;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.models.Isduh;
import xsightassembler.models.MailAddress;
import xsightassembler.models.Pallet;
import xsightassembler.services.IsduhService;
import xsightassembler.services.MailAddressService;
import xsightassembler.services.PalletService;
import xsightassembler.services.bi.BiNoteServise;
import xsightassembler.services.bi.BiTestService;
import xsightassembler.utils.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AssemblyJournalController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private MainApp mainApp;
    private Method method;
    private Stage stage;
    private HashMap<String, String> modulesMap = Utils.getModulesMap("isduh");
    private IsduhService service = new IsduhService();
    private FilteredList<Isduh> filteredList;
    private ObservableList<Isduh> allList;
    private ObservableList<? extends Isduh> selectedItems;

    @FXML
    private AnchorPane mainPane;
    @FXML
    private ComboBox<String> journalCombo;
    @FXML
    private TableView<Isduh> tIsduh;
    @FXML
    private HBox iconsBar;
    @FXML
    private ComboBox<String> moduleNameCombo;
    @FXML
    private DatePicker dateFrom;
    @FXML
    private DatePicker dateTo;
    @FXML
    private ImageView refreshImg;
    @FXML
    private TextField filterField;
    @FXML
    private Button allInOneBtn;
    @FXML
    private Button palletBtn;
    @FXML
    private Button addToPalletBtn;

    private CheckBox sendMail;
    private ImageView exportImg;
    private ImageView debugImg;

    @FXML
    private void initialize() {
        tIsduh.setTableMenuButtonVisible(true);

        Tooltip.install(refreshImg, new Tooltip("Refresh table"));
        refreshImg.setImage(new Image(Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("refresh.png"))));
        refreshImg.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            refreshJournal();
            RotateTransition rt = new RotateTransition(Duration.millis(300), refreshImg);
            rt.setByAngle(360);
            rt.setCycleCount(1);
            rt.setInterpolator(Interpolator.LINEAR);
            rt.play();
            event.consume();
        });

        addToPalletBtn.setDisable(true);
        addToPalletBtn.setTooltip(new Tooltip("Add to pallet"));

        TreeSet<String> statusSet = new TreeSet<>(Arrays.asList(Strings.statusesForAssemblyJournal));
        journalCombo.setItems(FXCollections.observableArrayList(statusSet));
        journalCombo.getSelectionModel().selectFirst();
        journalCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            fillTable();
        });

        TreeSet<String> keySet = new TreeSet<>(modulesMap.keySet());
        moduleNameCombo.setItems(FXCollections.observableArrayList(keySet));
        moduleNameCombo.getSelectionModel().selectFirst();

        tIsduh.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tIsduh.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        tIsduh.setRowFactory(this::rowFactoryTab);
        TableColumn numberCol = new TableColumn("#");
        numberCol.setMinWidth(20);
        numberCol.setMaxWidth(40);
        numberCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Isduh, Isduh>, ObservableValue<Isduh>>() {
            @Override
            public ObservableValue<Isduh> call(TableColumn.CellDataFeatures<Isduh, Isduh> p) {
                return new ReadOnlyObjectWrapper(p.getValue());
            }
        });

        numberCol.setCellFactory(new Callback<TableColumn<Isduh, Isduh>, TableCell<Isduh, Isduh>>() {
            @Override
            public TableCell<Isduh, Isduh> call(TableColumn<Isduh, Isduh> param) {
                return new TableCell<Isduh, Isduh>() {
                    @Override
                    protected void updateItem(Isduh item, boolean empty) {
                        super.updateItem(item, empty);

                        if (this.getTableRow() != null && item != null) {
                            setText(this.getTableRow().getIndex() + 1 + "");
                        } else {
                            setText("");
                        }
                    }
                };
            }
        });
        numberCol.setSortable(false);

        numberCol.setSortable(false);
        tIsduh.getColumns().add(numberCol);
        addColumnTab("Date", null, "getFormattedDate");
        addColumnTab("Status", null, "getAssemblyStatusString");
        addColumnTab("Type", null, "getTypeString");
        addColumnTab("SN", "XSTXT0010000500", "getSn");
        addColumnTab("Azimut", "XSTXT0020000213", "getAzimutModuleSn");
        addColumnTab("Bowl", "XSTXT0030000500", "getBowlModuleSn");
        addColumnTab("Camera", "XSTXT0020000394", "getCameraModuleSn");
        addColumnTab("Fan", "XSTXT8020000268", "getFanModuleSn");
        addColumnTab("Nose", "XSTXT0020000393", "getNoseModuleSn");
        addColumnTab("Radar", "XSTXT0070000029", "getRadarModuleSn");
        addColumnTab("Upper sensor", "XSTXT0020000500", "getUpperSensorModuleSn");
        addColumnTab("Pallet", null, "getPalletSn");
        addColumnTab("User", null, "getUserLogin");

        ContextMenu rowMenu = new ContextMenu();
        MenuItem addToPalletItem = new MenuItem("Add to pallet");
        MenuItem removeFromPalletItem = new MenuItem("Remove from pallet");

        tIsduh.setRowFactory(tv -> {
            TableRow<Isduh> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && (!row.isEmpty())) {
                    mainApp.showAllInOneAssemblerView(row.getItem());
                }
//                if (event.getButton() == MouseButton.SECONDARY && (!row.isEmpty())) {
//                    Isduh item = tIsduh.getSelectionModel().getSelectedItem();
//                    System.out.println(item);
//                    if (item != null) {
//                        if (item.getPallet() == null){
//                            rowMenu.getItems().add(addToPalletItem);
//                        } else {
//                            rowMenu.getItems().add(removeFromPalletItem);
//                        }
//                    }

////                    rowMenu.getItems().add(addToPalletItem);
//                }
//                tIsduh.setContextMenu(rowMenu);
            });

            return row;
        });

        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            getFilteredByFieldList(newValue);
        });

        tIsduh.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Isduh>) changed -> {
            selectedItems = changed.getList();
            boolean isAvailable = selectedItems.stream().allMatch(c -> c.getPallet() == null
                    && c.getAssemblyStatus() == 1);
            addToPalletBtn.setDisable(!isAvailable);
        });

        Utils.initDatePicker(dateFrom, dateTo);
        allInOneBtn.setOnAction(e -> mainApp.showAllInOneAssemblerView(null));
        palletBtn.setOnAction(e -> mainApp.showPalletView());
        addToPalletBtn.setOnAction(e -> addItemsToPallet());
    }

    private TableRow<Isduh> rowFactoryTab(TableView<Isduh> view) {
        return new TableRow<>();
    }

    private void addColumnTab(String label, String manuf, String dataIndex) {
        TableColumn<Isduh, String> column = new TableColumn<>(label);
//        if (manuf != null){
//            TableColumn<Isduh, String> manufColumn = new TableColumn<>(manuf);
//            manufColumn.setStyle("-fx-font-size:10px;");
//            column.getColumns().addAll(manufColumn);
//        }

        column.setCellValueFactory(
                (TableColumn.CellDataFeatures<Isduh, String> param) -> {
                    ObservableValue<String> result = new ReadOnlyStringWrapper("");
                    if (param.getValue() != null) {
                        try {
                            method = param.getValue().getClass().getMethod(dataIndex);
                            result = new ReadOnlyStringWrapper("" + method.invoke(param.getValue()));
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            return result;
                        }
                    }
                    return result;
                }
        );
        column.setCellFactory(param -> new TableCell<Isduh, String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                final ContextMenu menu = new ContextMenu();
                if (mainApp.getCurrentUser().getUserRole() == 1) {
                    MenuItem mDeleteItem = new MenuItem("Delete item");
                    mDeleteItem.setOnAction((e -> deleteAssembly()));
                    menu.getItems().add(mDeleteItem);
                    setContextMenu(menu);
                    getContextMenu().setAutoHide(true);
                }

                setText(item);
            }
        });
        tIsduh.getColumns().add(column);
    }

    private void refreshJournal() {
        filterField.clear();
        fillTable();
    }

    @FXML
    public void fillTable() {
        try {
            int status = IntStream.range(0, Strings.assemblyStatuses.length)
                    .filter(i -> journalCombo.getSelectionModel().getSelectedItem().equals(Strings.assemblyStatuses[i]))
                    .findFirst()
                    .orElse(-1);
            allList = FXCollections.observableArrayList(service.getAllBetweenDate(
                    java.sql.Date.valueOf(dateFrom.getValue()),
                    java.sql.Date.valueOf(dateTo.getValue())));

            String filter = filterField.getText().toUpperCase().trim();
            if (filter.isEmpty()) {
                filteredList = new FilteredList<>(allList, t -> true);
            } else {
                filteredList = getFilteredByFieldList(filter);
            }

            if (status == -1) {
                tIsduh.setItems(filteredList);
            } else {
                tIsduh.setItems(getFilteredByStatus(status));
            }

            tIsduh.refresh();
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgError(e.getLocalizedMessage());
        }
    }

    @FXML
    private void showModuleAssembler() {
        try {
            Class<?> c = Class.forName(String.format("xsightassembler.models.%sModule",
                    moduleNameCombo.getSelectionModel().getSelectedItem()));
            mainApp.showModuleAssemblerView(c);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    private FilteredList<Isduh> getFilteredByFieldList(String s) {
        filteredList.setPredicate(v -> s == null || s.isEmpty() ||
                v.getSn().toUpperCase().trim().contains(s.toUpperCase().trim()) ||
                v.getAzimutModuleSn().toUpperCase().trim().contains(s.toUpperCase().trim()) ||
                v.getBowlModuleSn().toUpperCase().trim().contains(s.toUpperCase().trim()) ||
                v.getCameraModuleSn().toUpperCase().trim().contains(s.toUpperCase().trim()) ||
                v.getFanModuleSn().toUpperCase().trim().contains(s.toUpperCase().trim()) ||
                v.getNoseModuleSn().toUpperCase().trim().contains(s.toUpperCase().trim()) ||
                v.getRadarModuleSn().toUpperCase().trim().contains(s.toUpperCase().trim()) ||
                v.getUpperSensorModuleSn().toUpperCase().trim().contains(s.toUpperCase().trim()) ||
                v.getPalletSn().toUpperCase().trim().contains(s.toUpperCase().trim())
        );
        return filteredList;
    }

    private FilteredList<Isduh> getFilteredByStatus(int status) {
        filteredList.setPredicate(v -> v.getAssemblyStatus() == status);
        return filteredList;
    }

    @FXML
    private void generateReport() {
        // generate only Done status
//        FilteredList<Isduh> tmpList = getFilteredByStatus(1);
        FilteredList<Isduh> tmpList = filteredList;
        if (tmpList.size() > 0) {
            ExcelReportGenerator repgen = new ExcelReportGenerator(dateFrom.getValue(), dateTo.getValue(), tmpList);
            repgen.assemblyReportToExcell();
            if (sendMail.isSelected() && repgen.getReportFile() != null) {
                try {
                    ObservableList<MailAddress> addressList = MsgBox.msgMultiselection(
                            new MailAddressService().findAll()).orElse(null);
                    if (addressList != null) {
                        Settings settings = Utils.getSettings();
                        if (settings != null) {
                            MailSender sender = new MailSender(settings);
                            String msg = "Report date generation: " + Utils.getFormattedDate(new Date());
                            sender.sendFile("Xsihgt report", msg, addressList, repgen.getReportFile());
                            sendMail.setSelected(false);
                        } else {
                            MsgBox.msgError("Can't get settings");
                        }
                    }
                } catch (CustomException e) {
                    LOGGER.error("Exception", e);
                    MsgBox.msgException(e);
                }
            }
        } else {
            MsgBox.msgInfo("There are no items for report");
            fillTable();
        }
    }

    private void deleteAssembly() {
        if (!MsgBox.msgConfirm("Delete selected assembly? Are you sure?")) {
            return;
        }
        try {
            BiTestService testService = new BiTestService();
            if (testService.getLastRunningTest(tIsduh.getSelectionModel().getSelectedItem()) != null) {
                MsgBox.msgInfo(String.format("Can't delete the system that is in test lab"));
                return;
            }
            String pass = MsgBox.msgInputPassword("Enter password for delete");
            if (pass != null) {
                if (pass.equals("Xsight2020")) {
                    BiNoteServise noteServise = new BiNoteServise();
                    for (Isduh assembly : tIsduh.getSelectionModel().getSelectedItems()) {
                        assembly.getTestSet().forEach(c -> c.getNotes().forEach(n -> {
                            try {
                                noteServise.delete(n);
                            } catch (CustomException e) {
                                e.printStackTrace();
                            }
                        }));
                        service.delete(assembly);
                    }
                    fillTable();
                    MsgBox.msgInfo("Delete complete");

                } else {
                    MsgBox.msgWarning("Incorrect password");
                }
            }
        } catch (CustomException ex) {
            MsgBox.msgWarning("Delete failure");
        }
    }

    public AnchorPane getMainPane() {
        return mainPane;
    }


    private void showIsduhEditorView(Isduh isduh) {
        mainApp.showIsduhEditorView(isduh);
    }

    private void showNewIsduhEditorView() {
        mainApp.showIsduhEditorView(null);
    }

    private void addItemsToPallet() {
        try {
            Set<String> types = new HashSet<>();
            selectedItems.forEach(c -> types.add(c.getSystemType()));
            if (types.size() > 1) {
                if (!MsgBox.msgConfirm("Selected system of different types.\n" +
                        "Are you sure want to continue?")) {
                    return;
                }
            }
            PalletService palletService = new PalletService();
            List<Pallet> palletList = palletService.findAll().stream()
                    .filter(c -> !c.isClosed()).collect(Collectors.toList());
            if (palletList.size() == 0) {
                MsgBox.msgInfo("Available pallets not found");
                return;
            }
            List<String> palletNumbers = palletList.stream()
                    .map(Pallet::getPalletNumber).collect(Collectors.toList());
            String selectedPalletNumber = MsgBox.msgChoice("Choose pallet", "Pallet", palletNumbers);
            if (selectedPalletNumber == null) {
                return;
            }
            Pallet selectedPallet = palletList.stream()
                    .filter(customer -> selectedPalletNumber.equals(customer.getPalletNumber()))
                    .findAny()
                    .orElse(null);
            if (selectedPallet == null) {
                return;
            }
            if (selectedPallet.getIsduhList().stream().anyMatch(c -> selectedItems.contains(c))) {
                MsgBox.msgInfo("One or more system already placed in pallet #" + selectedPalletNumber);
                return;
            }
            List<Object> forSave = new ArrayList<>();
            for (Isduh isduh: selectedItems) {
                selectedPallet.addIsduh(isduh);
                forSave.add(isduh);
            }
            forSave.add(selectedPallet);

            if (service.saveOrUpdate(forSave)) {
                MsgBox.msgInfo(String.format("%s systems has been\n" +
                        "successfully added to the pallet %s", selectedItems.size(), selectedPalletNumber));
            } else {
                MsgBox.msgWarning("Something went wrong");
            }
            tIsduh.refresh();
        } catch (CustomException e) {
            e.printStackTrace();
        }

    }

    public void setStage(Stage stage) {
        this.stage = stage;
        Utils.setImgViewEvents(stage, refreshImg);
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        setToolBar(mainApp.getCurrentUser().getUserRole());
        fillTable();
    }

    private void setToolBar(int userRole) {
        exportImg = new ImageView(new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(
                "to_excel.png")), 25, 25, false, false));
        Tooltip.install(exportImg, new Tooltip("Export to Excel"));
        exportImg.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            stage.getScene().setCursor(Cursor.HAND);
            event.consume();
        });
        exportImg.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            stage.getScene().setCursor(Cursor.DEFAULT);
            event.consume();
        });
        exportImg.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            generateReport();
        });


        debugImg = new ImageView(new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(
                "debug_32x32.png")), 25, 25, false, false));
        Tooltip.install(debugImg, new Tooltip("Debug journal"));

        sendMail = new CheckBox();
        Tooltip.install(sendMail, new Tooltip("send file via email"));
        sendMail.setPrefHeight(25);
        sendMail.setText("send mail");

        HBox.setMargin(exportImg, new Insets(15, 10, 0, 0));
        HBox.setMargin(sendMail, new Insets(15, 10, 0, 0));
        HBox.setMargin(debugImg, new Insets(15, 10, 0, 0));

        iconsBar.getChildren().add(exportImg);
        iconsBar.getChildren().add(sendMail);
        if (userRole == 1) {
            iconsBar.getChildren().add(debugImg);
        }
    }
}
