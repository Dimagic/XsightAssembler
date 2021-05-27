package xsightassembler.view;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.models.User;
import xsightassembler.services.UserService;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.MsgBox;
import xsightassembler.utils.Strings;
import xsightassembler.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;


public class UsersController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private UserService service = new UserService();
    private HashMap<Integer, String> userRoleMap = Strings.getUserRoleMap();
    private User currentUser = null;
    private MainApp mainApp;
    private Method method;

    @FXML
    private TableView<User> tUsers;
    @FXML
    private TextField loginField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private ComboBox<String> roleBox;
    @FXML
    private CheckBox isEnabled;
    @FXML
    private Button saveBtn;
    @FXML
    private Button newBtn;
    @FXML
    private Button dropPassBtn;

    @FXML
    public void initialize (){
        saveBtn.setDisable(true);
        dropPassBtn.setDisable(true);
        tUsers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tUsers.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        tUsers.setRowFactory(this::rowFactoryTab);
        addColumnTab("Login", "getLogin");
        addColumnTab("First name", "getFirstName");
        addColumnTab("Last name", "getLastName");
        addColumnTab("Role", "getUserRoleName");
        addColumnTab("Status", "getUserStatusName");
        addColumnTab("Last login", "getLastLoginString");

        roleBox.setItems(FXCollections.observableArrayList(userRoleMap.values()));
        roleBox.getSelectionModel().selectFirst();

        tUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillUserFields(newSelection);
            }
        });

        loginField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveBtn.setDisable(loginField.getText().trim().isEmpty());
        });

        newBtn.setOnAction(e -> clearUserFields());
        saveBtn.setOnAction(e -> saveUser());
        dropPassBtn.setOnAction(e -> dropPassword());

        fillTable();
    }

    private TableRow<User> rowFactoryTab(TableView<User> view) {
        return new TableRow<>();
    }

    private void addColumnTab(String label, String dataIndex) {
        TableColumn<User, String> column = new TableColumn<>(label);
        column.setCellValueFactory(
                (TableColumn.CellDataFeatures<User, String> param) -> {
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
        column.setCellFactory(param -> new TableCell<User, String>() {
            @Override
            public void updateItem(String item, boolean empty) {

                setText(item);
            }
        });
        tUsers.getColumns().add(column);
    }

    private void fillUserFields(User user){
        currentUser = user;
        loginField.setText(user.getLogin());
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        roleBox.setValue(userRoleMap.get(user.getUserRole()));
        isEnabled.setSelected(user.getUserStatus() == 1);

        loginField.setDisable(true);
        roleBox.setDisable(user.getLogin().equalsIgnoreCase("admin"));
        isEnabled.setDisable(user.getLogin().equalsIgnoreCase("admin"));
        dropPassBtn.setDisable(user.getId() == null);
    }

    private void clearUserFields(){
        currentUser = null;
        loginField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        roleBox.getSelectionModel().selectFirst();
        isEnabled.setSelected(false);
        tUsers.getSelectionModel().clearSelection();
        loginField.setDisable(false);
        roleBox.setDisable(false);
        isEnabled.setDisable(false);
        dropPassBtn.setDisable(true);
    }

    private User getUserFromFields(){
        User user;
        if (currentUser != null){
            user = currentUser;
        } else {
            user = new User();
        }
        user.setLogin(loginField.getText());
        user.setPassword(loginField.getText());
        user.setFirstName(firstNameField.getText());
        user.setLastName(lastNameField.getText());
        user.setUserRole(Utils.mapGetKeyByValue(userRoleMap, roleBox.getSelectionModel().getSelectedItem()));
        user.setUserStatus(isEnabled.isSelected() ? 1: 0);
        return user;
    }

    private void saveUser(){
        try {
            User user = getUserFromFields();
            service.saveOrUpdate(user);
            fillTable();
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgWarning(e.getCause().getLocalizedMessage());
        }
    }

    private void dropPassword() {
        String msg = String.format("Password for user: %s will drop.\nAre you sure?", currentUser.getLogin());
        if (MsgBox.msgConfirm(msg)) {
            try {
                currentUser.setPassword(currentUser.getLogin());
                service.update(currentUser);
                MsgBox.msgInfo("Changing password complete.\nCurrent password equals user login.");
            } catch (CustomException e) {
                LOGGER.error("Exception", e);
                MsgBox.msgWarning(e.getLocalizedMessage());
            }
        }
    }

    private void fillTable() {
        try {
            ObservableList<User> allUsers = FXCollections.observableArrayList(service.findAll());
            tUsers.setItems(allUsers);
            tUsers.refresh();
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}
