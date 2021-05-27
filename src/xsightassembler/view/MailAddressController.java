package xsightassembler.view;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.models.MailAddress;
import xsightassembler.services.MailAddressService;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.MsgBox;
import xsightassembler.utils.Strings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;


public class MailAddressController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Method method;
    private MailAddress currEmail;
    private MailAddressService service = new MailAddressService();

    @FXML
    private TableView<MailAddress> tAddresses;
    @FXML
    private TextField firstName;
    @FXML
    private TextField lastName;
    @FXML
    private TextField email;
    @FXML
    private Button saveBtn;
    @FXML
    private Button newBtn;

    @FXML
    private void initialize() {
        saveBtn.setDisable(true);
        tAddresses.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tAddresses.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tAddresses.setRowFactory(this::rowFactoryTab);
        addColumnTab("First name", "getFirstName");
        addColumnTab("Last name", "getLastName");
        addColumnTab("Email", "getEmail");

        tAddresses.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                setCurrEmail(newSelection);
            }
        });

        tAddresses.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
            if (t.getButton() == MouseButton.SECONDARY) {
                System.out.println(tAddresses.getSelectionModel().getSelectedItem());
            }
        });

        email.textProperty().addListener((observable, oldValue, newValue) -> {
            validateField();
        });
        firstName.textProperty().addListener((observable, oldValue, newValue) -> {
            validateField();
        });
        lastName.textProperty().addListener((observable, oldValue, newValue) -> {
            validateField();
        });

        saveBtn.setOnAction(e -> save());
        newBtn.setOnAction(e -> clearFields());

        fillTable();
    }

    private TableRow<MailAddress> rowFactoryTab(TableView<MailAddress> view) {
        return new TableRow<>();
    }

    private void addColumnTab(String label, String dataIndex) {
        TableColumn<MailAddress, String> column = new TableColumn<>(label);
        column.setCellValueFactory(
                (TableColumn.CellDataFeatures<MailAddress, String> param) -> {
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
        column.setCellFactory(param -> new TableCell<MailAddress, String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                final ContextMenu menu = new ContextMenu();
                MenuItem mDeleteItem = new MenuItem("Delete item");
                mDeleteItem.setOnAction((e -> deleteAddress()));
                menu.getItems().add(mDeleteItem);
                setContextMenu(menu);
                getContextMenu().setAutoHide(true);
                setText(item);
            }
        });
        tAddresses.getColumns().add(column);
    }

    private void deleteAddress() {
        try {
            MailAddress selected = tAddresses.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (MsgBox.msgConfirm("Do you really want to delete this entry?")) {
                    service.delete(selected);
                    clearFields();
                }
            }
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    private void validateField() {
        Matcher m = Strings.pEmail.matcher(email.getText().trim());
        boolean res = m.matches();
        if (res) {
            email.setStyle("-fx-background-color: white;");
        } else {
            email.setStyle("-fx-background-color: yellow;");
        }
        saveBtn.setDisable(!(res && (!firstName.getText().trim().isEmpty() || !lastName.getText().trim().isEmpty())));
    }

    private void save() {
        if (currEmail == null) {
            currEmail = new MailAddress();
        }
        currEmail.setFirstName(firstName.getText().trim());
        currEmail.setLastName(lastName.getText().trim());
        currEmail.setEmail(email.getText().trim());
        try {
            service.saveOrUpdate(currEmail);
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        clearFields();
    }

    private void setCurrEmail(MailAddress val) {
        currEmail = val;
        firstName.setText(currEmail.getFirstName());
        lastName.setText(currEmail.getLastName());
        email.setText(currEmail.getEmail());
    }

    private void clearFields() {
        currEmail = null;
        firstName.clear();
        lastName.clear();
        email.clear();
        tAddresses.getSelectionModel().clearSelection();
        fillTable();
    }

    private void fillTable() {
        try {
            tAddresses.setItems(FXCollections.observableArrayList(service.findAll()));
            tAddresses.refresh();
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }
}
