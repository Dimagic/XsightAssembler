package xsightassembler.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.models.Isduh;
import xsightassembler.models.MailAddress;
import xsightassembler.models.Pallet;
import xsightassembler.services.IsduhService;
import xsightassembler.services.MailAddressService;
import xsightassembler.services.PalletService;
import xsightassembler.utils.*;

import java.util.Date;

public class PalletController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Stage stage;
    private MainApp mainApp;
    private final PalletService palletService = new PalletService();
    private final IsduhService isduhService = new IsduhService();
    private ObservableList<Pallet> palletsList = FXCollections.observableArrayList();
    private ObservableList<Isduh> isduhList = FXCollections.observableArrayList();
    private Pallet currentPallet;
    private Isduh currentIsduh;

    @FXML
    private TableView<Pallet> tPallets;
    @FXML
    private TableColumn<Pallet, String> palletSnColumn;
    @FXML
    private TableColumn<Pallet, String> dateCloseColumn;
    @FXML
    private TableColumn<Pallet, String> itemsCountColumn;
    @FXML
    private TableColumn<Pallet, String> palletCommentColumn;

    @FXML
    private TableColumn<Isduh, String> isduhColumn;
    @FXML
    private TableColumn<Isduh, String> fanColumn;
    @FXML
    private TableColumn<Isduh, String> upperColumn;
    @FXML
    private TableColumn<Isduh, String> bowlColumn;

    @FXML
    private TableView<Isduh> tPallet;
    @FXML
    private AnchorPane paletPane;

    @FXML
    private Button newPalletBtn;
    @FXML
    private Button addIsduhBtn;
    @FXML
    private Button removeIsduhBtn;
    @FXML
    private Button closePalletBtn;
    @FXML
    private Button toExcelBtn;
    @FXML
    private CheckBox sendEmail;
    @FXML
    private CheckBox hideClosed;
    @FXML
    private TextField comment;

    @FXML
    private void initialize() {
        hideClosed.setSelected(true);
        paletPane.setDisable(true);
        tPallet.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tPallet.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        palletSnColumn.setCellValueFactory(cellData -> cellData.getValue().snProperty());
        dateCloseColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        itemsCountColumn.setCellValueFactory(cellData -> cellData.getValue().countProperty());
        palletCommentColumn.setCellValueFactory(cellData -> cellData.getValue().commentProperty());

        tPallets.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tPallets.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        isduhColumn.setCellValueFactory(cellData -> cellData.getValue().snProperty());
        fanColumn.setCellValueFactory(cellData -> cellData.getValue().fanProperty());
        upperColumn.setCellValueFactory(cellData -> cellData.getValue().upperProperty());
        bowlColumn.setCellValueFactory(cellData -> cellData.getValue().bowlProperty());

        tPallets.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            currentPallet = newSelection;
            paletPane.setDisable(newSelection == null);
            if (newSelection != null) {
                if (tPallets.getSelectionModel().getSelectedItems().size() > 1) {
                    tPallet.getItems().clear();
                    paletPane.setDisable(true);
                } else {
                    fillPalletTable(currentPallet);
                    if (currentPallet.isClosed()) {
                        closePalletBtn.setText("Open pallet");
                    } else {
                        closePalletBtn.setText("Close pallet");
                    }
                }
            }
        });

        tPallet.setRowFactory(t -> {
            TableRow<Isduh> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && (!row.isEmpty())) {
                    mainApp.showAllInOneAssemblerView(row.getItem());
                }
            });
            return row;
        });

        tPallet.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            currentIsduh = newSelection;
            if (newSelection != null) {
                System.out.println(newSelection);
            }
        });

        comment.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    currentPallet.setComment(comment.getText().trim());
                    palletService.saveOrUpdate(currentPallet);
                    tPallets.refresh();
                } catch (CustomException e) {
                    MsgBox.msgException(e);
                }
            }
        });

        hideClosed.selectedProperty().addListener((observable, oldValue, newValue) -> {
            fillPalletsTable();
        });

        newPalletBtn.setOnAction(e -> addNewPallet());
        addIsduhBtn.setOnAction(e -> addIsduhToPallet());
        removeIsduhBtn.setOnAction(e -> removeIsduhFromPallet());
        toExcelBtn.setOnAction(e -> exportToExcel());
        closePalletBtn.setOnAction(e -> closeOpenPallet());

        fillPalletsTable();
    }

    private void addNewPallet() {
        try {
            String n = MsgBox.msgInputString("Enter new pallet number");
            if (n == null) {
                return;
            }
            Pallet pallet = new Pallet();
            pallet.setPalletNumber(n);
            palletService.save(pallet);
            fillPalletsTable();
        } catch (CustomException e) {
            LOGGER.error("exception", e);
            MsgBox.msgException(e);
        }
    }

    private void closeOpenPallet() {
        if (currentPallet == null) {
            MsgBox.msgWarning("Pallet not selected");
            return;
        }
        if (currentPallet.isClosed()) {
            if (MsgBox.msgConfirm("Do you really want to open current pallet?")) {
                currentPallet.setCloseDate(null);
            } else {
                return;
            }
        } else {
            currentPallet.setCloseDate(new Date());
        }
        try {
            palletService.saveOrUpdate(currentPallet);
            fillPalletsTable();
        } catch (CustomException e) {
            LOGGER.error("exception", e);
            MsgBox.msgException(e);
        }
    }

    private void addIsduhToPallet() {
        try {
            if (currentPallet == null) {
                MsgBox.msgWarning("Pallet not selected");
                return;
            }
            if (currentPallet.isClosed()) {
                MsgBox.msgWarning("Pallet is closed");
                return;
            }
            String n = MsgBox.msgInputString("Enter new ISDUH number");
            if (n == null) {
                return;
            }
            n = n.trim().toUpperCase();
            Isduh isduh = isduhService.findBySn(n);
            if (isduh == null) {
                String s = String.format("System SN: %s not found", n);
                MsgBox.msgWarning(s);
                return;
            }
            if (isduh.getPallet() != null) {
                String s = String.format("System SN: %s already placed on pallet: %s",
                        n, isduh.getPallet().getPalletNumber());
                MsgBox.msgWarning(s);
                return;
            }
            if (isduh.getAssemblyStatus() != 1) {
                String s = String.format("System SN: %s assembly status is not complete.", n);
                MsgBox.msgWarning(s);
                return;
            }
            currentPallet.addIsduh(isduh);
            isduhService.saveOrUpdate(isduh);
            palletService.saveOrUpdate(currentPallet);
            tPallets.refresh();
            fillPalletTable(currentPallet);
        } catch (CustomException e) {
            LOGGER.error("exception", e);
            MsgBox.msgException(e);
        }
    }

    private void removeIsduhFromPallet() {
        try {
            if (currentPallet.isClosed()) {
                MsgBox.msgWarning("Pallet is closed");
                return;
            }
            if (currentIsduh == null) {
                MsgBox.msgWarning("System not selected");
                return;
            }
            String q = String.format("Do you really want to remove\nsystem %s from the pallet?", currentIsduh.getSn());
            if (MsgBox.msgConfirm(q)) {
                currentPallet.removeIsduh(currentIsduh);
                isduhService.saveOrUpdate(currentIsduh);
                palletService.saveOrUpdate(currentPallet);
                tPallets.refresh();
                fillPalletTable(currentPallet);
            }
        } catch (CustomException e) {
            LOGGER.error("exception", e);
            MsgBox.msgException(e);
        }
    }

    private void fillPalletsTable() {
        try {
//            tPallets.getItems().clear();
            FilteredList<Pallet> palletFilteredList = new FilteredList<>(FXCollections.observableArrayList(palletService.findAll()));
            if (hideClosed.isSelected()) {
                palletFilteredList.setPredicate(c -> !c.isClosed());
            }
            tPallets.setItems(palletFilteredList);
        } catch (CustomException e) {
            LOGGER.error("exception", e);
            MsgBox.msgException(e);
        }
    }

    private void fillPalletTable(Pallet pallet) {
        try {
            tPallet.getItems().clear();
            isduhList = FXCollections.observableArrayList(isduhService.findByPallet(pallet));
            comment.setText(pallet.getComment());
            comment.setDisable(pallet.isClosed());
            tPallet.setItems(isduhList);
        } catch (CustomException e) {
            LOGGER.error("exception", e);
            MsgBox.msgException(e);
        }
    }

    private void exportToExcel() {
        ObservableList<Pallet> selectedPallets = tPallets.getSelectionModel().getSelectedItems();
        if (selectedPallets.size() < 1) {
            MsgBox.msgInfo("Pallet not selected");
            return;
        }
        ExcelReportGenerator2 repgen = new ExcelReportGenerator2(null, null, selectedPallets);
        repgen.assemblyReportToExcell();
        if (sendEmail.isSelected() && repgen.getReportFile() != null) {
            try {
                ObservableList<MailAddress> addressList = MsgBox.msgMultiselection(
                        new MailAddressService().findAll()).orElse(null);
                if (addressList != null) {
                    Settings settings = Utils.getSettings();
                    if (settings != null) {
                        MailSender sender = new MailSender(settings);
                        StringBuffer subject = new StringBuffer();
                        subject.append("Xsihgt pallets report: ");
                        selectedPallets.forEach(p -> subject.append(String.format("%s ", p.getPalletNumber())));
                        String msg = "Report date generation: " + Utils.getFormattedDate(new Date());
                        sender.sendFile(subject.toString(), msg, addressList, repgen.getReportFile());
                        sendEmail.setSelected(false);
                    } else {
                        MsgBox.msgError("Can't get settings");
                    }
                }
            } catch (CustomException e) {
                LOGGER.error("Exception", e);
                MsgBox.msgException(e);
            }
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}
