package xsightassembler.view;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import xsightassembler.MainApp;
import xsightassembler.models.History;
import xsightassembler.models.User;
import xsightassembler.services.HistoryService;
import xsightassembler.utils.IniUtils;
import xsightassembler.utils.MsgBox;
import xsightassembler.utils.Strings;
import xsightassembler.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import static xsightassembler.utils.Utils.getAllNodesInParent;
import static xsightassembler.utils.Utils.setFirstCharToUpper;

public class ModuleAssemblerController implements Initializable {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());

    private Stage stage;
    private MainApp mainApp;
    private Method method;
    private Object service;
    private Object currentModule;
    private Class typeModule;
    private GridPane grid;
    private IniUtils iniUtilsISDU;
    private IniUtils iniUtilsISDUH;
    private HashMap<String, Pattern> patternMapISDU;
    private HashMap<String, Pattern> patternMapISDUH;
    private final ValidationSupport support = new ValidationSupport();


    @FXML
    private SplitPane splitPane;
    @FXML
    private TableView<Object> tModule;
    @FXML
    private AnchorPane leftPane;
    @FXML
    private Button saveBtn;
    @FXML
    private Button newBtn;
    @FXML
    private Button historyBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            iniUtilsISDU = new IniUtils("strings.ini", "isdu");
            patternMapISDU = iniUtilsISDU.getPatternMap();
            iniUtilsISDUH = new IniUtils("strings.ini", "isduh");
            patternMapISDUH = iniUtilsISDUH.getPatternMap();
        } catch (IOException e) {
            LOGGER.error("IniUtils", e);
            MsgBox.msgException(e);
            return;
        }
        historyBtn.setVisible(false);
        tModule.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tModule.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        tModule.setRowFactory(this::rowFactoryTab);
        addColumnTab("Date", "getFormattedDate");
        addColumnTab("Module", "getModule");

        tModule.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillFields(newSelection);
            }
        });

        saveBtn.setOnAction(e -> saveModule());
        newBtn.setOnAction(e -> clearFields());
        historyBtn.setOnAction(e -> mainApp.showHistoryView(currentModule, stage));
        support.invalidProperty().addListener((obs, wasInvalid, isInvalid) -> {
            saveBtn.setDisable(isInvalid);
        });
    }

    private TableRow<Object> rowFactoryTab(TableView<Object> view) {
        return new TableRow<>();
    }

    private void addColumnTab(String label, String dataIndex) {
        TableColumn<Object, String> column = new TableColumn<>(setFirstCharToUpper(label));
        column.setCellValueFactory(
                (TableColumn.CellDataFeatures<Object, String> param) -> {
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
        column.setCellFactory(param -> new TableCell<Object, String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                setText(item);
            }
        });
        tModule.getColumns().add(column);
    }

    private void addGridWithFields(final Class<?> type) {
        HashMap<String, TextField> fieldMap = getFieldMap();
        Label label;
        Label commentLbl = new Label();
        TextArea commentArea = new TextArea();
        String fieldName;
        grid = new GridPane();
        int n = 0;
        for (Field field : type.getDeclaredFields()) {
            if (field.getType() == String.class) {
                fieldName = field.getName();
                if (fieldName.equalsIgnoreCase("comment")) {
                    commentLbl.setText("Comment:");
                    commentLbl.setStyle("-fx-font-weight: bold");
                    commentLbl.setPadding(new Insets(10, 10, 0, 10));
                    commentArea.setId(fieldName);
                    commentArea.setPrefWidth(180);
                    commentArea.setPrefHeight(100);
                    commentArea.setText(fieldMap.get(fieldName).getText());
                    continue;
                }
                label = new Label();
                label.setStyle("-fx-font-weight: bold");
                label.setText(setFirstCharToUpper(fieldName) + ":");
                grid.add(label, 0, n);
                TextField currField = fieldMap.get(field.getName());
                try {
                    if (fieldName.equalsIgnoreCase("module")) {
                        currField.focusedProperty().addListener((arg0, oldValue, newValue) -> {
                            if (!newValue && !currField.getText().trim().isEmpty()) {
                                Object o = Utils.findByInnerModuleSn(typeModule, currField.getText().trim().toUpperCase());
                                if (o != null) {
                                    fillFields(o);
                                }
                            }
                        });
                    } else {
                        addColumnTab(fieldName, "get" + setFirstCharToUpper(fieldName));
                    }
                    currField.setId(fieldName);

                    /*
                     * add regex validators for field
                     */
                    currField.textProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            currField.setText(newValue.toUpperCase().trim());
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Exception", e);
                    MsgBox.msgException(e);
                }
                grid.add(currField, 1, n);
                n++;
            }
        }
        addColumnTab("User", "getUserLogin");

//        grid.setMinSize(280, 150);
        grid.setPadding(new Insets(10, 10, 0, 10));
        grid.setVgap(5);
        grid.setHgap(5);
        grid.getColumnConstraints().addAll(new ColumnConstraints(95),
                new ColumnConstraints(181));
        Double d = (n + 1) * 28.0;

        AnchorPane.setTopAnchor(grid, 10.0);
        AnchorPane.setLeftAnchor(grid, 10.0);
        AnchorPane.setRightAnchor(grid, 10.0);

        AnchorPane.setTopAnchor(commentLbl, d);
        AnchorPane.setLeftAnchor(commentLbl, 10.0);

        AnchorPane.setTopAnchor(commentArea, d);
        AnchorPane.setRightAnchor(commentArea, 20.0);
        AnchorPane.setLeftAnchor(commentArea, 119.0);


        leftPane.getChildren().addAll(grid, commentLbl, commentArea);
    }

    /*
     * all modules fields
     */
    private HashMap<String, TextField> getFieldMap() {
        HashMap<String, TextField> fieldMap = new HashMap<>();
        TextField module = new TextField();
        TextField comment = new TextField();
        TextField comEx = new TextField();
        TextField breakable = new TextField();
        TextField carrier = new TextField();
        TextField cameraHouse = new TextField();
        TextField camera = new TextField();
        TextField mcu = new TextField();
        TextField board = new TextField();
        TextField top = new TextField();
        TextField cooler = new TextField();

        fieldMap.put("module", module);
        fieldMap.put("comment", comment);
        fieldMap.put("comEx", comEx);
        fieldMap.put("breakable", breakable);
        fieldMap.put("carrier", carrier);
        fieldMap.put("cameraHouse", cameraHouse);
        fieldMap.put("camera", camera);
        fieldMap.put("mcu", mcu);
        fieldMap.put("board", board);
        fieldMap.put("top", top);
        fieldMap.put("cooler", cooler);

        return fieldMap;
    }

    /*
     * function fill fields on gui
     */
    private void fillFields(Object module) {
        currentModule = module;
        System.out.println(currentModule);

        try {
            Set<History> tmp = (Set<History>) currentModule.getClass().getMethod("getHistorySet").invoke(currentModule);
            historyBtn.setVisible(tmp.size() > 0);
            HashMap<String, String> valuesMap = (HashMap<String, String>) module.getClass().
                    getMethod("getValuesMap").invoke(module);
            for (Node node : getAllNodesInParent(leftPane)) {
                if (node instanceof TextField) {
                    if (node.getId().equalsIgnoreCase("module")) {
//                        node.setDisable(true);
                        ((TextField) node).setEditable(false);
                    }
                    ((TextField) node).setText(valuesMap.get(node.getId()));
                }
                if (node instanceof TextArea) {
                    ((TextArea) node).setText(valuesMap.get(node.getId()));
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    private void fillTable(Class<?> typeModule) {
        try {
            Object moduleList = Utils.class.getMethod("getList" + typeModule.getSimpleName()).invoke(this);
            tModule.setItems((ObservableList<Object>) moduleList);
            tModule.refresh();
            for (Node n: Utils.getAllNodesInParent(grid)) {
                if (n instanceof TextField) {
                    textFieldRegexValidator(typeModule, (TextField) n);
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    /*
     * function clear fields on gui
     */
    private void clearFields() {
        currentModule = null;
        historyBtn.setVisible(false);
        tModule.getSelectionModel().clearSelection();
        for (Node node : Utils.getAllNodesInParent(leftPane)) {
            if (node instanceof TextField) {
                ((TextField) node).setText("");
                ((TextField) node).setEditable(true);
            }
            if (node instanceof TextArea) {
                ((TextArea) node).setText("");
            }
        }
//        saveBtn.setDisable(true);
    }

    private void saveModule() {
        try {
            String msg;
            Object module;
            if (currentModule != null) {
                msg = Strings.updateComplete;
                module = currentModule;
            } else {
                msg = Strings.saveComplete;
                Class<?> c = Class.forName(typeModule.getName());
                module = c.newInstance();
            }
            /*
             * set module values
             */
            for (Node node : getAllNodesInParent(leftPane)) {
                if (node instanceof TextField) {
                    String value = ((TextField) node).getText().trim().isEmpty() ? null : ((TextField) node).getText().trim();
                    /*
                     * add history to module
                     */
                    if (module.getClass().getMethod("getId").invoke(module) != null) {
                        method = module.getClass().getMethod("get" + setFirstCharToUpper(node.getId()));
                        if (!value.equals(method.invoke(module))) {
                            History history = new History();
                            history.setFieldChange(setFirstCharToUpper(node.getId()));
                            history.setOldValue((String) method.invoke(module));
                            history.setNewValue(value);
                            history.setUser(mainApp.getCurrentUser());
                            HistoryService historyService = new HistoryService();
                            historyService.save(history);
                            module.getClass().getMethod("addHistory", History.class).invoke(module, history);
                        }
                    }
                    method = module.getClass().getMethod("set" + setFirstCharToUpper(node.getId()), String.class);
                    method.invoke(module, value);

                }
                if (node instanceof TextArea) {
                    method = module.getClass().getMethod("set" + setFirstCharToUpper(node.getId()), String.class);
                    method.invoke(module, ((TextArea) node).getText());
                }
            }
            /*
             * set user if new module
             */
            if (module.getClass().getMethod("getId").invoke(module) == null){
                method = module.getClass().getMethod("setUser", User.class);
                method.invoke(module, mainApp.getCurrentUser());
            }

            if (!Utils.saveModule(typeModule, module)) {
                return;
            }
            fillTable(module.getClass());
            clearFields();
            MsgBox.msgInfo(msg);
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }


    private void textFieldRegexValidator(Class<?> typeModule, TextField currField) {
        String pName;
        boolean isModule = currField.getId().equalsIgnoreCase("module");
        if (isModule) {
            pName = "pManuf" + (typeModule.getSimpleName().replace("Module", ""));
        } else {
            pName = "p" + Utils.setFirstCharToUpper(currField.getId());
        }
        Validator<String> validator = (control, value) -> {
            Pattern p1 = patternMapISDU.get(pName);
            Pattern p2 = patternMapISDUH.get(pName);
            Pattern p = Pattern.compile(String.format("%s|%s", p1, p2));
            boolean condition;
            try {
                condition = value.isEmpty() || !value.matches(p.pattern());
                if (!condition) {
                    condition = isSnAlreadyUse(currField);
                }

                return ValidationResult.fromMessageIf(control, "Incorrect", Severity.ERROR, condition);
            } catch (NullPointerException e) {
                MsgBox.msgWarning(String.format("RegEx for field %s not found", currField.getId()));
            }
            return null;
        };
        support.initInitialDecoration();
        support.registerValidator(currField, true, validator);
    }

    /*
     * search by inner module
     */
    private boolean isSnAlreadyUse(TextField currField) {
        boolean res = false;
        Object o = Utils.findByInnerModuleSn(typeModule, currField.getText().trim().toUpperCase());
        String sn = Utils.getModuleSnInObject(o);
        if (sn != null && !sn.equalsIgnoreCase(Utils.getModuleSnInObject(currentModule))) {
//                                    currField.setStyle("-fx-background-color: yellow;");
            String msg = String.format("Module with\nSN: %s\nalready use in system\nSN: %s",
                    currField.getText(), sn);
            MsgBox.msgWarning(msg);
            currField.setTooltip(new Tooltip(msg));
            res = true;
        } else {
            currField.setTooltip(null);
        }
        return res;
    }

    public void setCurrentModule(Object currentModule) {
        this.currentModule = currentModule;
        fillFields(currentModule);
    }

    public void setTypeModule(Class<?> typeModule) {
        this.typeModule = typeModule;
        addGridWithFields(typeModule);
        fillTable(typeModule);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle(typeModule.getSimpleName());
        stage.setMinHeight(600);
        stage.setMinWidth(800);
        stage.setMaximized(true);
        stage.setMaximized(false);
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;

    }


}
