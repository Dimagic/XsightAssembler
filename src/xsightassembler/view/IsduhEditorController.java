package xsightassembler.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.models.*;
import xsightassembler.services.HistoryService;
import xsightassembler.services.IsduhService;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.MsgBox;
import xsightassembler.utils.Strings;
import xsightassembler.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.regex.Pattern;

import static xsightassembler.utils.Utils.*;
import static xsightassembler.utils.Utils.setFirstCharToUpper;

public class IsduhEditorController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Isduh isduh;
    private MainApp mainApp;
    private Method method;
    private Stage stage;
    private IsduhService service = new IsduhService();
    private FilteredList<Isduh> filteredIsduh;
    private ObservableList<Isduh> allIsduh;
    private HashMap<String, Pattern> patternMap;
    private HashMap<String, String> stringMap;

    @FXML
    private GridPane grid;
    @FXML
    private TextField serial;
    @FXML
    private TextField azimut;
    @FXML
    private TextField bowl;
    @FXML
    private TextField camera;
    @FXML
    private TextField fan;
    @FXML
    private TextField nose;
    @FXML
    private TextField radar;
    @FXML
    private TextField upperSensor;

    @FXML
    private Button saveBtn;
    @FXML
    private Button historyBtn;


    @FXML
    private void initialize() {
        historyBtn.setVisible(false);
        isduh = new Isduh();
        serial.setPromptText(Strings.manufISDUHModule);
        azimut.setPromptText(Strings.manufAzimutModule);
        bowl.setPromptText(Strings.manufBowlModule);
        camera.setPromptText(Strings.manufCameraModule);
        fan.setPromptText(Strings.manufFanModule);
        nose.setPromptText(Strings.manufNoseModule);
        radar.setPromptText(Strings.manufRadarModule);
        upperSensor.setPromptText(Strings.manufUpperSensorModule);

        serial.textProperty().addListener((observable, oldValue, newValue) -> {
            if (fieldValidator(serial, patternMap.get("pManufSerial"))) {
                isduh.setSn(newValue);
            }
            isAllFieldsFill();
        });

        initModuleFieldValidator();

        saveBtn.setDisable(true);
        saveBtn.setOnAction(e -> save());
        historyBtn.setOnAction(e -> mainApp.showHistoryView(isduh, stage));
    }

    private void save() {
        try {
            if (isduh.getId() == null) {
                isduh.setUser(mainApp.getCurrentUser());
            } else {
                /*
                 * add history to module
                 */
                Isduh tmp = service.findBySn(serial.getText().trim());
                for (Node node : getAllNodesInParent(grid)) {
                    if (node instanceof TextField) {
                        if (!((TextField) node).getId().equalsIgnoreCase("serial")) {
                            String newValue = ((TextField) node).getText().trim();
                            String oldValue = (String) tmp.getClass().
                                    getMethod("get" + setFirstCharToUpper(node.getId()) + "ModuleSn").invoke(tmp);
                            if (!newValue.equalsIgnoreCase(oldValue)) {
                                History history = new History();
                                history.setFieldChange(setFirstCharToUpper(node.getId()));
                                history.setOldValue(oldValue);
                                history.setNewValue(newValue);
                                history.setUser(mainApp.getCurrentUser());
                                HistoryService historyService = new HistoryService();
                                historyService.save(history);
                                isduh.addHistory(history);
                            }
                        }
                    }
                }
            }
            service.saveOrUpdate(isduh);
            MsgBox.msgInfo("Assembly saved successfully.");
            stage.close();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    private void initModuleFieldValidator() {
        serial.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue && !serial.getText().trim().isEmpty()) {
                try {
                    Isduh res = service.findBySn(serial.getText().trim());
                    if (res != null) {
                        serial.setStyle("-fx-background-color: yellow;");
                        String msg = String.format("System SN: %s already exist", serial.getText());
                        MsgBox.msgWarning(msg);
                        serial.setTooltip(new Tooltip(msg));
                    }
                } catch (CustomException e) {
                    LOGGER.error("Exception", e);
                    MsgBox.msgError(e.getLocalizedMessage());
                }
            }
        });

        azimut.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                moduleValidator(new AzimutModule(), azimut);
            }
        });

        bowl.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                moduleValidator(new BowlModule(), bowl);
            }
        });

        camera.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                moduleValidator(new CameraModule(), camera);
            }
        });

        fan.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                moduleValidator(new FanModule(), fan);
            }
        });

        nose.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                moduleValidator(new NoseModule(), nose);
            }
        });

        radar.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                moduleValidator(new RadarModule(), radar);
            }
        });

        upperSensor.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                moduleValidator(new UpperSensorModule(), upperSensor);
            }
        });
    }

    private void addModuleToIsduh (Object typeModule, TextField field) {
        Isduh presentSystem;
        try {
            if (field.getText().trim().isEmpty()){
                // clear module
                isduh.getClass().getMethod("set" + typeModule.getClass().getSimpleName()).invoke(isduh);
                field.setStyle("-fx-background-color: white;");
                field.setTooltip(null);
                return;
            }
            Class<?> c = Class.forName("xsightassembler.services." + typeModule.getClass().getSimpleName() + "Service");
            Object service = c.newInstance();
            method = service.getClass().getMethod("findBySn", String.class);
            Object module = method.invoke(service, field.getText());
            // if module present
            if (module == null){
                String msg = String.format("%s\nSN: %s\nnot found",
                        typeModule.getClass().getSimpleName(), field.getText());
                MsgBox.msgWarning(msg);
                field.setStyle("-fx-background-color: yellow;");
                field.setTooltip(new Tooltip(msg));
                return;
            }
            // if module already assembled
            presentSystem = this.service.findByModule(module);
            if (presentSystem != null) {
                if (!presentSystem.getSn().equalsIgnoreCase(isduh.getSn())){
                    String msg = String.format("Module with\nSN: %s\nalready use in system\nSN: %s",
                            field.getText(), presentSystem.getSn());
                    MsgBox.msgWarning(msg);
                    field.setStyle("-fx-background-color: yellow;");
                    field.setTooltip(new Tooltip(msg));
                    return;
                }
            }
            // Add module to system
            method = isduh.getClass().getMethod("set" + module.getClass().getSimpleName(), module.getClass());
            method.invoke(isduh, module);
            field.setStyle("-fx-background-color: white;");
            field.setTooltip(null);
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    private void moduleValidator(Object typeModule, TextField field) {
        addModuleToIsduh(typeModule, field);
        isAllFieldsFill();
    }

    /*
     * fields validator (enable/disable save button)
     */
    private void isAllFieldsFill() {
        boolean isValidate = true;
        boolean isAllFill = true;

        for (Node node : getAllNodesInParent(grid)) {
            if (node instanceof TextField) {
                if (node.getStyle().contains("yellow")) {
                    isValidate = false;
                    break;
                }
            }
        }
        for (Node node : getAllNodesInParent(grid)) {
            if (node instanceof TextField) {
                if (((TextField) node).getText().trim().isEmpty()) {
                    isAllFill = false;
                    break;
                }
            }
        }
        isduh.setAssemblyStatus(isAllFill ? 1 : 0);
        saveBtn.setDisable(!isValidate);
    }

    public void setIsduh(Isduh isduh) {
        if (isduh == null) {
            this.isduh = new Isduh();
            serial.setDisable(false);
        } else {
            this.isduh = isduh;
            historyBtn.setVisible(isduh.getHistorySet().size() > 0);
            serial.setText(isduh.getSn());
            azimut.setText(isduh.getAzimutModuleSn());
            bowl.setText(isduh.getBowlModuleSn());
            camera.setText(isduh.getCameraModuleSn());
            fan.setText(isduh.getFanModuleSn());
            nose.setText(isduh.getNoseModuleSn());
            radar.setText(isduh.getRadarModuleSn());
            upperSensor.setText(isduh.getUpperSensorModuleSn());
            serial.setDisable(true);
        }
        initViewBtn();
        isAllFieldsFill();
    }

    private FilteredList<Isduh> getFilteredByFieldList(String s) {
        filteredIsduh.setPredicate(v ->
                v.getSn().trim().equalsIgnoreCase(s.trim()) ||
                        v.getUpperSensorModule().getAzimutModuleSn().trim().equalsIgnoreCase(s.trim()) ||
                        v.getBowlModuleSn().trim().equalsIgnoreCase(s.trim()) ||
                        v.getUpperSensorModule().getCameraModuleSn().trim().equalsIgnoreCase(s.trim()) ||
                        v.getFanModuleSn().trim().equalsIgnoreCase(s.trim()) ||
                        v.getUpperSensorModule().getNoseModuleSn().trim().equalsIgnoreCase(s.trim()) ||
                        v.getUpperSensorModule().getRadarModuleSn().trim().equalsIgnoreCase(s.trim()) ||
                        v.getUpperSensorModuleSn().trim().equalsIgnoreCase(s.trim())
        );
        return filteredIsduh;
    }

    private void initViewBtn() {
        for (Node node : Utils.getAllNodesInParent(grid)) {
            if (node instanceof Button) {
                int rowIndex = grid.getRowIndex(node);
                String value = ((TextField) Utils.getNodeByRowColumnIndex(grid, rowIndex, 1)).getText();
                Object o = isduh.getModulesMap().get(value);
                if (o != null) {
                    ((Button) node).setDisable(false);
                    ((Button) node).setOnAction(e -> mainApp.showAllModuleView(o.getClass(), stage, o));
                } else {
                    ((Button) node).setDisable(true);
                }
            }
        }

    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        try {
            /*
             * create list for search by fields
             */
            allIsduh = FXCollections.observableArrayList(service.findAll());
            filteredIsduh = new FilteredList<>(allIsduh, t -> true);
            patternMap = Utils.getPatternMap("isduh");

        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgError(e.getLocalizedMessage());
        }
    }
}
