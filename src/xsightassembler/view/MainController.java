package xsightassembler.view;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.models.Isduh;
import xsightassembler.models.MailAddress;
import xsightassembler.services.IsduhService;
import xsightassembler.services.MailAddressService;
import xsightassembler.utils.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.IntStream;

public class MainController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private MainApp mainApp;
    private Method method;
    private Stage stage;
    private HashMap<String, String> modulesMap = Utils.getModulesMap("isduh");
    private IsduhService service = new IsduhService();
    private FilteredList<Isduh> filteredList;
    private ObservableList<Isduh> allList;

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
//    @FXML
//    private Button newBtn;
    @FXML
    private Button allInOneBtn;
    @FXML
    private Button palletBtn;

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
            filterField.clear();
            fillTable();
            RotateTransition rt = new RotateTransition(Duration.millis(300), refreshImg);
            rt.setByAngle(360);
            rt.setCycleCount(1);
            rt.setInterpolator(Interpolator.LINEAR);
            rt.play();
            event.consume();
        });

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
        tIsduh.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        tIsduh.setRowFactory(this::rowFactoryTab);
        addColumnTab("Date",null, "getFormattedDate");
        addColumnTab("Status",null, "getAssemblyStatusString");
        addColumnTab("Type", null, "getTypeString");
        addColumnTab("SN","XSTXT0010000500", "getSn");
        addColumnTab("Azimut","XSTXT0020000213", "getAzimutModuleSn");
        addColumnTab("Bowl","XSTXT0030000500", "getBowlModuleSn");
        addColumnTab("Camera","XSTXT0020000394", "getCameraModuleSn");
        addColumnTab("Fan","XSTXT8020000268", "getFanModuleSn");
        addColumnTab("Nose", "XSTXT0020000393","getNoseModuleSn");
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

        Utils.initDatePicker(dateFrom, dateTo);
//        newBtn.setOnAction(e -> showNewIsduhEditorView());
        allInOneBtn.setOnAction(e -> mainApp.showAllInOneAssemblerView(null));
        palletBtn.setOnAction(e -> mainApp.showPalletView());
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
            if (filter.isEmpty()){
                filteredList = new FilteredList<>(allList, t -> true);
            } else {
                filteredList = getFilteredByFieldList(filter);
            }

            if (status == -1){
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
    private void showAssembler() {
        try {
            Class<?> c = Class.forName(String.format("xsightassembler.models.%sModule",
                    moduleNameCombo.getSelectionModel().getSelectedItem()));
            mainApp.showAllModuleView(c);
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
        String pass = MsgBox.msgInputPassword("Enter password for delete");
        if (pass != null) {
            if (pass.equals("Xsight2020")) {
                try {
                    for (Isduh assembly : tIsduh.getSelectionModel().getSelectedItems()) {
                        service.delete(assembly);
                    }
                    fillTable();
                    MsgBox.msgInfo("Delete complete");
                } catch (CustomException ex) {
                    MsgBox.msgWarning("Delete failure");
                }
            } else {
                MsgBox.msgWarning("Incorrect password");
            }
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
