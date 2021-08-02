package xsightassembler.view;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import xsightassembler.MainApp;
import xsightassembler.models.*;
import xsightassembler.services.*;
import xsightassembler.utils.CustomException;
import xsightassembler.utils.IniUtils;
import xsightassembler.utils.MsgBox;
import xsightassembler.utils.Utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xsightassembler.utils.Utils.getAllNodesInParent;

public class AllInOneAssemblerController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Method method;
    private MainApp mainApp;
    private Stage stage;
    private AssemblyJournalController assemblyJournalController;
    private final IsduhService isduhService = new IsduhService();
    private final UpperSensorModuleService upperService = new UpperSensorModuleService();
    private final AzimutModuleService azimutService = new AzimutModuleService();
    private final CameraModuleService cameraService = new CameraModuleService();
    private final BowlModuleService bowlService = new BowlModuleService();
    private final RadarModuleService radarService = new RadarModuleService();
    private final NoseModuleService noseModuleService = new NoseModuleService();
    private final FanModuleService fanService = new FanModuleService();
    private HashMap<String, Pattern> patternMap = new HashMap<>();
    private HashMap<String, Pattern> pSysTypeMap = new HashMap<>();
    private HashMap<String, String> manufNumberMap = new HashMap<>();
    private Set<TextField> fieldHistorySet = new HashSet<>();
    private Map<Object, TextField[]> moduleFieldMap = new HashMap<>();

    private TextField[] isduhFields;
    private TextField[] fanFields;
    private TextField[] upperFields;
    private TextField[] azimutFields;
    private TextField[] cameraFields;
    private TextField[] bowlFields;

    private Isduh isduh;
    private UpperSensorModule upperSensorModule;
    private AzimutModule azimutModule;
    private CameraModule cameraModule;
    private BowlModule bowlModule;
    private RadarModule radarModule;
    private NoseModule noseModule;
    private FanModule fanModule;

    @FXML
    private AnchorPane pane;
    @FXML
    private TitledPane isduhPane;
    @FXML
    private TitledPane upperPane;
    @FXML
    private TitledPane azimutPane;
    @FXML
    private TitledPane cameraPane;
    @FXML
    private TitledPane bowlPane;

    @FXML
    private TextField isduhSystemSn;
    @FXML
    private TextField fanModuleSn;
    @FXML
    private TextField upperSensorModuleSn;
    @FXML
    private TextField coolerSn;
    @FXML
    private TextField radarModuleSn;
    @FXML
    private TextField azimutModuleSn;
    @FXML
    private TextField topSn;
    @FXML
    private TextField boardSn;
    @FXML
    private TextField cameraModuleSn;
    @FXML
    private TextField mcuSn;
    @FXML
    private TextField cameraSn;
    @FXML
    private TextField houseSn;
    @FXML
    private TextField noseModuleSn;
    @FXML
    private TextField bowlModuleSn;
    @FXML
    private TextField comExSn;
    @FXML
    private TextField carrierSn;
    @FXML
    private TextField breakableSn;
    @FXML
    private Label coolerLbl;
    @FXML
    private Label fanLbl;
    @FXML
    private Button saveBtn;
    @FXML
    private Button historyBtn;


    @FXML
    private void initialize() {
        historyBtn.setDisable(true);
        try {
            pSysTypeMap = new IniUtils("strings.ini").getPatternMapByName("pManufIsduh");
            if (pSysTypeMap.isEmpty()) {
                MsgBox.msgWarning("Can't get system type pattern");
                return;
            }
        } catch (IOException e) {
            MsgBox.msgException(e);
        }

        isduhPane.setText("Undefine type system");
        isduhFields = new TextField[]{isduhSystemSn, fanModuleSn, upperSensorModuleSn, bowlModuleSn};
        fanFields = new TextField[]{fanModuleSn};
        upperFields = new TextField[]{upperSensorModuleSn, coolerSn, radarModuleSn,
                noseModuleSn, azimutModuleSn, cameraModuleSn};
        azimutFields = new TextField[]{azimutModuleSn, topSn, boardSn};
        cameraFields = new TextField[]{cameraModuleSn, mcuSn, cameraSn, houseSn};
        bowlFields = new TextField[]{bowlModuleSn, comExSn, carrierSn, breakableSn};

        isduhPane.setCollapsible(false);
        upperPane.setCollapsible(false);
        azimutPane.setCollapsible(false);
        cameraPane.setCollapsible(false);
        bowlPane.setCollapsible(false);

        addFieldValidator(upperSensorModuleSn, "pManufUpperSensor");
        addFieldValidator(cameraModuleSn, "pManufCamera");
        addFieldValidator(azimutModuleSn, "pManufAzimut");
        addFieldValidator(radarModuleSn, "pRadar");
        addFieldValidator(noseModuleSn, "pManufNose");
        addFieldValidator(bowlModuleSn, "pManufBowl");
        addFieldValidator(isduhSystemSn, "pManufIsduh");

        addFieldValidator(fanModuleSn, "pFan");
        addFieldValidator(coolerSn, "pCooler");
        addFieldValidator(topSn, "pTop");
        addFieldValidator(boardSn, "pBoard");
        addFieldValidator(mcuSn, "pMcu");
        addFieldValidator(cameraSn, "pCamera");
        addFieldValidator(houseSn, "pHouse");
        addFieldValidator(comExSn, "pComEx");
        addFieldValidator(carrierSn, "pCarrier");
        addFieldValidator(breakableSn, "pBreakable");

        clearFillUpperModule();
        clearFillAzimutModule();
        clearFillCameraModule();
        clearFillBowlModule();
        clearFillIsduhSystem();

        upperSensorModuleSn.textProperty().addListener((observable, oldValue, newValue) -> clearFillUpperModule());
        azimutModuleSn.textProperty().addListener((observable, oldValue, newValue) -> clearFillAzimutModule());
        cameraModuleSn.textProperty().addListener((observable, oldValue, newValue) -> clearFillCameraModule());
        bowlModuleSn.textProperty().addListener((observable, oldValue, newValue) -> clearFillBowlModule());
        isduhSystemSn.textProperty().addListener((observable, oldValue, newValue) -> clearFillIsduhSystem());

        saveBtn.setOnAction(e -> save());
        historyBtn.setOnAction(e -> getHistory());
    }

    private void save() {
        if (!isAllFilledDataValid()) {
            MsgBox.msgWarning("Not all data valid. Please check and try again.");
            return;
        }
        if (isDoubleDataPresent()) {
            MsgBox.msgWarning("Not unique serial numbers found.\nPlease check and try again.");
            return;
        }
        if (isduh.getPallet() != null) {
            String msg = String.format("System already placed on pallet: %s" +
                    "\nDo you want continue?", isduh.getPallet().getPalletNumber());
            if (!MsgBox.msgConfirm(msg)) {
                return;
            }
        }
        List<Object> saveList = new ArrayList<>();
        String sn;
        try {
            sn = fanModuleSn.getText().trim();
            if (!sn.isEmpty() && !fanModuleSn.getStyle().contains("yellow")) {
                fanModule = fanService.findBySn(sn);
                if (fanModule == null) {
                    fanModule = new FanModule();
                    fanModule.setUser(mainApp.getCurrentUser());
                    fanModule.setModule(sn);
                }
                saveList.add(fanModule);
            }
            sn = upperSensorModuleSn.getText().trim();
            if (!sn.isEmpty() && !upperSensorModuleSn.getStyle().contains("yellow")) {
                upperSensorModule = upperService.findBySn(sn);
                if (upperSensorModule == null) {
                    upperSensorModule = new UpperSensorModule();
                    upperSensorModule.setUser(mainApp.getCurrentUser());
                    upperSensorModule.setModule(sn);
                }
                sn = azimutModuleSn.getText().trim();
                if (!sn.isEmpty() && !azimutModuleSn.getStyle().contains("yellow")) {
                    azimutModule = azimutService.findBySn(sn);
                    if (azimutModule == null) {
                        azimutModule = new AzimutModule();
                        azimutModule.setUser(mainApp.getCurrentUser());
                        azimutModule.setModule(sn);
                    }
                    azimutModule.setTop(topSn.getText().trim().isEmpty() ? null : topSn.getText());
                    azimutModule.setBoard(boardSn.getText().trim().isEmpty() ? null : boardSn.getText());
                    saveList.add(azimutModule);
                    upperSensorModule.setAzimutModule(azimutModule);
                } else {
                    upperSensorModule.setAzimutModule();
                }
                sn = cameraModuleSn.getText().trim();
                if (!sn.isEmpty() && !cameraModuleSn.getStyle().contains("yellow")) {
                    cameraModule = cameraService.findBySn(sn);
                    if (cameraModule == null) {
                        cameraModule = new CameraModule();
                        cameraModule.setUser(mainApp.getCurrentUser());
                        cameraModule.setModule(sn);
                    }
                    cameraModule.setMcu(mcuSn.getText().trim().isEmpty() ? null : mcuSn.getText());
                    cameraModule.setCamera(cameraSn.getText().trim().isEmpty() ? null : cameraSn.getText());
                    cameraModule.setCameraHouse(houseSn.getText().trim().isEmpty() ? null : houseSn.getText());
                    saveList.add(cameraModule);
                    upperSensorModule.setCameraModule(cameraModule);
                } else {
                    upperSensorModule.setCameraModule();
                }
                sn = radarModuleSn.getText().trim();
                if (!sn.isEmpty() && !radarModuleSn.getStyle().contains("yellow")) {
                    radarModule = radarService.findBySn(sn);
                    if (radarModule == null) {
                        radarModule = new RadarModule();
                        radarModule.setUser(mainApp.getCurrentUser());
                        radarModule.setModule(sn);
                    }
                    saveList.add(radarModule);
                    upperSensorModule.setRadarModule(radarModule);
                } else {
                    upperSensorModule.setRadarModule();
                }
                sn = noseModuleSn.getText().trim();
                if (!sn.isEmpty() && !noseModuleSn.getStyle().contains("yellow")) {
                    noseModule = noseModuleService.findBySn(sn);
                    if (noseModule == null) {
                        noseModule = new NoseModule();
                        noseModule.setUser(mainApp.getCurrentUser());
                        noseModule.setModule(sn);
                    }
                    saveList.add(noseModule);
                    upperSensorModule.setNoseModule(noseModule);
                } else {
                    upperSensorModule.setNoseModule();
                }
                if (coolerSn.isVisible()) {
                    upperSensorModule.setCooler(coolerSn.getText());
                }
                saveList.add(upperSensorModule);
            }
            sn = bowlModuleSn.getText().trim();
            if (!sn.isEmpty() && !bowlModuleSn.getStyle().contains("yellow")) {
                bowlModule = bowlService.findBySn(sn);
                if (bowlModule == null) {
                    bowlModule = new BowlModule();
                    bowlModule.setUser(mainApp.getCurrentUser());
                    bowlModule.setModule(sn);
                }
                bowlModule.setComEx(comExSn.getText().trim().isEmpty() ? null : comExSn.getText());
                bowlModule.setCarrier(carrierSn.getText().trim().isEmpty() ? null : carrierSn.getText());
                bowlModule.setBreakable(breakableSn.getText().trim().isEmpty() ? null : breakableSn.getText());
                HashMap<TextField, History> h = getHistoryMap(fieldHistorySet);
                for (TextField f: h.keySet()) {
                    History history = h.get(f);
                    saveList.add(history);
                    bowlModule.addHistory(history);
                }
                saveList.add(bowlModule);
            }
            if (fanModuleSn.getText().trim().isEmpty()) {
                isduh.setFanModule();
            } else {
                isduh.setFanModule(fanModule);
            }
            if (upperSensorModuleSn.getText().trim().isEmpty()) {
                isduh.setUpperSensorModule();
            } else {
                isduh.setUpperSensorModule(upperSensorModule);
            }
            if (bowlModuleSn.getText().trim().isEmpty()) {
                isduh.setBowlModule();
            } else {
                isduh.setBowlModule(bowlModule);
            }
            isduh.setAssemblyStatus(isAllDataFilled() ? 1 : 0);
            saveList.add(isduh);
            isduhService.saveOrUpdate(saveList);
            stage.close();
            assemblyJournalController.fillTable();
        } catch (CustomException e) {
            String msg = getCause(e).getLocalizedMessage();
            String key = StringUtils.substringBetween(msg, "Key (", ")=(");
            String val = StringUtils.substringBetween(msg, ")=(", ") ");
//            MsgBox.msgWarning(getCause(e).getLocalizedMessage());
            if (val != null && !val.isEmpty()) {
                try {
                    Isduh i;
                    if (key.contains("_id")) {
                        i = isduhService.findByModuleNameAndId(key, Long.parseLong(val));
                        if (i == null) {
                            i = isduhService.findByModuleInUpper(key, Long.parseLong(val));
                        }
                        if (i != null) {
                            msg = String.format("%s already exist in system\nSN: %s",
                                    key.replace("_id", "").toUpperCase(), i.getSn());
                        }
                    } else {
                        HashMap<String, JSONObject> res = isduhService.globalSearchString(val);
                        System.out.println(res);
                        for (String k : res.keySet()) {
                            if (!k.contains("_module")) {
                                continue;
                            }
                            msg = String.format("%s SN: %s\nalready exist in %s\nSN: %s",
                                    key.toUpperCase(), val, k.toUpperCase(), res.get(k).get("module"));
                        }
                    }
                    MsgBox.msgWarning(msg);
                } catch (CustomException ex) {
                    LOGGER.error("exception", ex);
                    MsgBox.msgException(ex);
                }
            }

        }
    }

    private void addFieldValidator(TextField field, String pName) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (field.isVisible() && newValue != null) {
                field.setText(newValue.trim().toUpperCase());
                if (patternMap.isEmpty()) {
                    field.setText(newValue.trim().toUpperCase());
                    for (String type : pSysTypeMap.keySet()) {
                        Matcher m = pSysTypeMap.get(type).matcher(field.getText());
                        if (!m.matches()) {
                            field.setStyle("-fx-background-color: yellow;");
                            patternMap.clear();
                            manufNumberMap.clear();
                        } else {
                            field.setStyle("-fx-background-color: white;");
                            isduhPane.setText(type.toUpperCase() + " system");
                            patternMap = Utils.getPatternMap(type);
                            manufNumberMap = Utils.getStringMap(type + "_manuf");
                            break;
                        }
                    }
                } else {
                    field.setText(newValue.trim().toUpperCase());
                    if (!newValue.trim().isEmpty()) {
                        Pattern p = patternMap.get(pName);
                        Matcher m = p.matcher(field.getText());
                        if (!m.matches()) {
                            field.setStyle("-fx-background-color: yellow;");
                        } else {
                            field.setStyle("-fx-background-color: white;");
                        }
                    } else {
                        field.setStyle("-fx-background-color: white;");
                    }
                }
                if (field == isduhSystemSn && !field.getText().isEmpty() && isduhSystemSn.getStyle().contains("yellow")) {
                    patternMap.clear();
                    manufNumberMap.clear();
                    isduhPane.setText("Undefine type system");
                }
                setTooltipAndPrompt();
            }
        });

        // add history if need
        field.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
            if (!newPropertyValue && isduh != null) {
                try {
                    String newValue = field.getText();
                    method = isduh.getClass().getMethod("get" + Utils.setFirstCharToUpper(field.getId()));
                    String oldValue = (String) method.invoke(isduh);
                    if (!oldValue.isEmpty()) {
                        if (!oldValue.equals(newValue)) {
                            fieldHistorySet.add(field);
                        } else {
                            fieldHistorySet.remove(field);
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private HashMap<TextField, History> getHistoryMap(Set<TextField> fieldSet) {
        try {
            System.out.println(fieldSet);
            HashMap<TextField, History> historyMap = new HashMap<>();

            for (TextField textField: fieldSet) {
                String fieldName = Utils.setFirstCharToUpper(textField.getId()).replaceAll("Sn$", "");
                String oldValue = (String) isduh.getClass()
                        .getMethod(String.format("get%s", Utils.setFirstCharToUpper(textField.getId()))).invoke(isduh);
                History history = new History();
                history.setFieldChange(fieldName);
                history.setOldValue(oldValue);
                history.setNewValue(textField.getText());
                history.setUser(mainApp.getCurrentUser());
                historyMap.put(textField, history);
            }
            return historyMap;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void disableFieldsByType() {
        boolean isIsdu = isduh.getSystemType().equalsIgnoreCase("isdu");
        fanModuleSn.setVisible(!isIsdu);
        fanLbl.setVisible(!isIsdu);
        coolerSn.setVisible(!isIsdu);
        coolerLbl.setVisible(!isIsdu);
    }

    private void clearFillIsduhSystem() {
        String sn = isduhSystemSn.getText().trim();
        if (!sn.isEmpty()) {
            if (isduhSystemSn.getStyle().contains("yellow")) {
                clearAndBlockField(isduhFields, true);
            } else {
                try {
                    isduh = isduhService.findBySn(sn);
                } catch (CustomException e) {
                    LOGGER.error("exception", e);
                    MsgBox.msgException(e);
                }
                clearAndBlockField(isduhFields, false);
                if (isduh == null) {
                    isduh = new Isduh();
                    isduh.setSn(sn);
                    isduh.setUser(mainApp.getCurrentUser());
                    disableFieldsByType();
                } else {
                    setIsduhSystem(isduh);
                }
            }
        } else {
            clearAndBlockField(isduhFields, true);
        }
    }

    private void clearFillUpperModule() {
        String sn = upperSensorModuleSn.getText().trim();
        if (!sn.isEmpty()) {
            if (upperSensorModuleSn.getStyle().contains("yellow")) {
                clearAndBlockField(upperFields, true);
            } else {
                clearAndBlockField(upperFields, false);
                try {
                    upperSensorModule = upperService.findBySn(sn);
                    if (upperSensorModule != null) {
                        upperSensorModuleSn.setText(upperSensorModule.getModule());
                        coolerSn.setText(upperSensorModule.getCooler());
                        noseModuleSn.setText(upperSensorModule.getNoseModuleSn());
                        radarModuleSn.setText(upperSensorModule.getRadarModuleSn());
                        azimutModuleSn.setText(upperSensorModule.getAzimutModuleSn());
                        cameraModuleSn.setText(upperSensorModule.getCameraModuleSn());
                    }
                } catch (CustomException e) {
                    e.printStackTrace();
                }
            }
        } else {
            clearAndBlockField(upperFields, true);
        }
    }

    private void clearFillAzimutModule() {
        String sn = azimutModuleSn.getText().trim();
        if (!sn.isEmpty()) {
            if (azimutModuleSn.getStyle().contains("yellow")) {
                clearAndBlockField(azimutFields, true);
            } else {
                clearAndBlockField(azimutFields, false);
                try {
                    azimutModule = azimutService.findBySn(sn);
                    if (azimutModule != null) {
                        azimutModuleSn.setText(azimutModule.getModule());
                        topSn.setText(azimutModule.getTop());
                        boardSn.setText(azimutModule.getBoard());
                    }
                } catch (CustomException e) {
                    e.printStackTrace();
                }
            }
        } else {
            clearAndBlockField(azimutFields, true);
        }
    }

    private void clearFillCameraModule() {
        String sn = cameraModuleSn.getText().trim();
        if (!sn.isEmpty()) {
            if (cameraModuleSn.getStyle().contains("yellow")) {
                clearAndBlockField(cameraFields, true);
            } else {
                clearAndBlockField(cameraFields, false);
                try {
                    cameraModule = cameraService.findBySn(sn);
                    if (cameraModule != null) {
                        cameraModuleSn.setText(cameraModule.getModule());
                        mcuSn.setText(cameraModule.getMcu());
                        cameraSn.setText(cameraModule.getCamera());
                        houseSn.setText(cameraModule.getCameraHouse());
                        if (noseModule == null) {
                            noseModule = new NoseModule();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("exception", e);
                    MsgBox.msgException(e);
                }
            }
        } else {
            clearAndBlockField(cameraFields, true);
        }
    }

    private void clearFillBowlModule() {
        String sn = bowlModuleSn.getText().trim();
        if (!sn.isEmpty()) {
            if (bowlModuleSn.getStyle().contains("yellow")) {
                clearAndBlockField(bowlFields, true);
            } else {
                clearAndBlockField(bowlFields, false);
                try {
                    bowlModule = bowlService.findBySn(sn);
                    if (bowlModule != null) {
                        bowlModuleSn.setText(bowlModule.getModule());
                        comExSn.setText(bowlModule.getComEx());
                        carrierSn.setText(bowlModule.getCarrier());
                        breakableSn.setText(bowlModule.getBreakable());

                    }
                } catch (Exception e) {
                    LOGGER.error("exception", e);
                    MsgBox.msgException(e);
                }
            }
        } else {
            clearAndBlockField(bowlFields, true);
        }
    }

    private void clearAndBlockField(TextField[] fields, boolean isBlock) {
        for (int i = 1; i < fields.length; i++) {
            if (isBlock) {
                fields[i].clear();
                fields[i].setDisable(true);
            } else {
                fields[i].setDisable(false);
            }
        }
        setTooltipAndPrompt();
    }

    public void setIsduhSystem(Isduh isduh) {
        this.isduh = isduh;
        historyBtn.setDisable(!isduh.isHistoryPresent());
        stage.setTitle("Assembly system SN: " + isduh.getSn());
        disableFieldsByType();
        // getting patterns by system type
        isduhPane.setText(isduh.getTypeString().toUpperCase() + " system");
        manufNumberMap = Utils.getStringMap(isduh.getSystemType() + "_manuf");
        patternMap = Utils.getPatternMap(isduh.getSystemType());
        setTooltipAndPrompt();

        isduhSystemSn.setEditable(false);
        isduhSystemSn.setText(isduh.getSn());

        if (isduh.getFanModule() != null) {
            setFanModule(isduh.getFanModule());
        }
        if (isduh.getUpperSensorModule() != null) {
            setUpperModule(isduh.getUpperSensorModule());
            if (upperSensorModule.getAzimutModule() != null) {
                setAzimutModule(upperSensorModule.getAzimutModule());
            }
            if (upperSensorModule.getCameraModule() != null) {
                setCameraModule(upperSensorModule.getCameraModule());
            }
            if (upperSensorModule.getNoseModule() != null) {
                setNoseModule(upperSensorModule.getNoseModule());
            }
        }
        if (isduh.getBowlModule() != null) {
            setBowlModule(isduh.getBowlModule());
        }
        if (isduh.getPallet() != null && isduh.getPallet().isClosed()) {
            MsgBox.msgInfo("This system is in a closed pallet");
            saveBtn.setDisable(true);
        }
    }

    private void setFanModule(FanModule fanModule) {
        this.fanModule = fanModule;
        fanModuleSn.setText(fanModule.getModule());
    }

    private void setUpperModule(UpperSensorModule upperSensorModule) {
        this.upperSensorModule = upperSensorModule;
        upperSensorModuleSn.setText(upperSensorModule.getModule());
        coolerSn.setText(upperSensorModule.getCooler());
        if (upperSensorModule.getRadarModule() != null) {
            setRadarModule(upperSensorModule.getRadarModule());
        }
    }

    private void setAzimutModule(AzimutModule azimutModule) {
        this.azimutModule = azimutModule;
        azimutModuleSn.setText(azimutModule.getModule());
        topSn.setText(azimutModule.getTop());
        boardSn.setText(azimutModule.getBoard());
    }

    private void setCameraModule(CameraModule cameraModule) {
        this.cameraModule = cameraModule;
        cameraModuleSn.setText(cameraModule.getModule());
        mcuSn.setText(cameraModule.getMcu());
        cameraSn.setText(cameraModule.getCamera());
        houseSn.setText(cameraModule.getCameraHouse());
    }

    private void setRadarModule(RadarModule radarModule) {
        this.radarModule = radarModule;
        radarModuleSn.setText(upperSensorModule.getRadarModuleSn());
    }

    private void setNoseModule(NoseModule noseModule) {
        this.noseModule = noseModule;
        noseModuleSn.setText(upperSensorModule.getNoseModuleSn());
    }

    private void setBowlModule(BowlModule bowlModule) {
        this.bowlModule = bowlModule;
        bowlModuleSn.setText(bowlModule.getModule());
        comExSn.setText(bowlModule.getComEx());
        carrierSn.setText(bowlModule.getCarrier());
        breakableSn.setText(bowlModule.getBreakable());
    }

    private void setTooltipAndPrompt() {
        setToltipAndPromptToField(isduhSystemSn, "manufISDUHModule");
        setToltipAndPromptToField(fanModuleSn, "manufFanModule");
        setToltipAndPromptToField(upperSensorModuleSn, "manufUpperSensorModule");
        setToltipAndPromptToField(coolerSn, "manufCooler");
        setToltipAndPromptToField(radarModuleSn, "manufRadarModule");
        setToltipAndPromptToField(azimutModuleSn, "manufAzimutModule");
        setToltipAndPromptToField(topSn, "manufTop");
        setToltipAndPromptToField(boardSn, "manufBoard");
        setToltipAndPromptToField(cameraModuleSn, "manufCameraModule");
        setToltipAndPromptToField(mcuSn, "manufMcu");
        setToltipAndPromptToField(cameraSn, "manufCamera");
        setToltipAndPromptToField(houseSn, "manufCameraHouse");
        setToltipAndPromptToField(noseModuleSn, "manufNoseModule");
        setToltipAndPromptToField(bowlModuleSn, "manufBowlModule");
        setToltipAndPromptToField(comExSn, "manufComEx");
        setToltipAndPromptToField(carrierSn, "manufCarrier");
        setToltipAndPromptToField(breakableSn, "manufBreakable");
    }

    private void setToltipAndPromptToField(TextField field, String val) {
        field.setTooltip(new Tooltip(manufNumberMap.get(val)));
        field.setPromptText(manufNumberMap.get(val));
    }


    private boolean isAllFilledDataValid() {
        for (Node node : getAllNodesInParent(isduhPane)) {
            if (node instanceof TextField) {
                if (node.getStyle().contains("yellow")) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isAllDataFilled() {
        for (Node node : getAllNodesInParent(isduhPane)) {
            if (node instanceof TextField) {
                if (!node.isVisible()) {
                    continue;
                }
                if (((TextField) node).getText().trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isDoubleDataPresent() {
        HashMap<String, Integer> dataMap = new HashMap<>();
        for (Node node : getAllNodesInParent(isduhPane)) {
            if (node instanceof TextField && ((TextField) node).getText() == null) {
                ((TextField) node).setText("");
            }
            if (node instanceof TextField && node.isVisible() && !((TextField) node).getText().isEmpty()) {
                String data = ((TextField) node).getText().trim();
                if (!data.isEmpty()) {
                    int count = dataMap.get(data) == null ? 1 : dataMap.get(data) + 1;
                    dataMap.put(((TextField) node).getText().trim(), count);
                }
            }
        }
        for (Integer i : dataMap.values()) {
            if (i > 1) return true;
        }
        return false;
    }

    private void getHistory() {
        mainApp.showHistoryView(isduh, stage);
    }

    public void setMainController(AssemblyJournalController assemblyJournalController) {
        this.assemblyJournalController = assemblyJournalController;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle("New system assembly");
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    private Throwable getCause(Throwable e) {
        Throwable cause = null;
        Throwable result = e;

        while (null != (cause = result.getCause()) && (result != cause)) {
            result = cause;
        }
        return result;
    }
}
