package xsightassembler.view;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;
import xsightassembler.utils.MsgBox;
import xsightassembler.utils.Settings;
import xsightassembler.utils.Utils;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SettingsController implements MsgBox {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private MainApp mainApp;
    private Settings currSettings;
    private Stage dialogStage;

    @FXML
    private TextField dbAddress;
    @FXML
    private TextField dbPort;
    @FXML
    private TextField dbName;
    @FXML
    private TextField dbUser;
    @FXML
    private PasswordField dbPass;
    @FXML
    private TextField mailServer;
    @FXML
    private TextField mailPort;
    @FXML
    private TextField mailUser;
    @FXML
    private PasswordField mailPass;
    @FXML
    private ComboBox<String> printerCombo;
    @FXML
    private CheckBox enableIncAssembly;
    @FXML
    private CheckBox sslAuth;
    @FXML
    private Spinner<Integer> labCount;
    @FXML
    private TextField sshUser;
    @FXML
    private PasswordField sshPass;
    @FXML
    private TextField sftpFolder;
    @FXML
    private TextField logFolder;
    @FXML
    private TextField namePostfix;
    @FXML
    private Spinner<Integer> logCheckPeriod;
    @FXML
    private Spinner<Integer> startAnalyzeShift;
    @FXML
    private TextField puttyFile;
    @FXML
    private TextField vlcFile;
    @FXML
    private TextArea templateArea;
    @FXML
    private Button saveBtn;
    @FXML
    private Button testConnBtn;

    @FXML
    private void initialize() {
        fillPrinterBox();

    }

    @FXML
    private void testConnection() {

    }

    public void fillSettings() {
        try {
            Settings currSet = Utils.getSettings();
            dbAddress.setText(currSet.getDbAddress());
            dbPort.setText(currSet.getDbPort());
            dbName.setText(currSet.getDbName());
            dbUser.setText(currSet.getDbUser());
            dbPass.setText(currSet.getDbPass());
            mailServer.setText(currSet.getMailServer());
            mailPort.setText(currSet.getMailPort());
            mailUser.setText(currSet.getMailUser());
            mailPass.setText(currSet.getMailPass());
            printerCombo.setValue(currSet.getPrinterCombo());
            enableIncAssembly.setSelected(currSet.isEnableIncAssembly());
            sslAuth.setSelected(currSet.isSslAuth());
            labCount.setValueFactory(new SpinnerValueFactory.
                    IntegerSpinnerValueFactory(0, 25, currSet.getLabCountInt()));
            sshUser.setText(currSet.getSshUser());
            sshPass.setText(currSet.getSshPass());
            sftpFolder.setText(currSet.getSftpFolder());

            logFolder.setText(currSet.getLogFolder());
            puttyFile.setText(currSet.getPuttyFile());
            namePostfix.setText(currSet.getNamePostfix());
            logCheckPeriod.setValueFactory(new SpinnerValueFactory.
                    IntegerSpinnerValueFactory(5, 60, currSet.getLogCheckPeriodInt()));
            startAnalyzeShift.setValueFactory(new SpinnerValueFactory.
                    IntegerSpinnerValueFactory(0, 60, currSet.getStartAnalyzeShiftInt()));
            templateArea.setText(currSet.getTemplateArea());
            vlcFile.setText(currSet.getVlcFile());
        } catch (NullPointerException ignored) {
        }
    }

    @FXML
    private boolean saveSettings() {
        HashMap<String, String> sett = new HashMap<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (field.getType() == TextField.class) {
                    TextField textField = (TextField) field.get(this);
                    sett.put(field.getName(), textField.getText());
                } else if (field.getType() == PasswordField.class) {
                    PasswordField passwordField = (PasswordField) field.get(this);
                    sett.put(field.getName(), passwordField.getText());
                } else if (field.getType() == ComboBox.class) {
                    ComboBox<String> comboField = (ComboBox<String>) field.get(this);
                    sett.put(field.getName(), comboField.getValue());
                } else if (field.getType() == CheckBox.class) {
                    CheckBox checkField = (CheckBox) field.get(this);
                    sett.put(field.getName(), String.valueOf(checkField.isSelected()));
                } else if(field.getType() == Spinner.class) {
                    Spinner<Integer> spinnerField = (Spinner<Integer>) field.get(this);
                    sett.put(field.getName(), Integer.toString(spinnerField.getValue()));
                } else if (field.getType() == TextArea.class) {
                    TextArea textArea = (TextArea) field.get(this);
                    sett.put(field.getName(), textArea.getText());
                }
                System.out.println(field.getName() + " >>> " + field.toString());
            } catch (NullPointerException e) {
                MsgBox.msgError(String.format("Field %s not found", field.getName()));
                continue;
            } catch (IllegalArgumentException | IllegalAccessException e) {
                MsgBox.msgException(e);
            }
        }
        setCurrSettings(new Settings(sett));
        if (!currSettings.validate()) {
            MsgBox.msgWarning("Not all fields are filled");
            return false;
        }

        File f = new File("./settings");
        try {
            // clear file
            PrintWriter writer = new PrintWriter(f);
            writer.print("");
            writer.close();
            // write file
            FileWriter fw = new FileWriter(f, true);
            System.out.println(">>> " + currSettings);

            fw.write(Utils.encodeString(currSettings.toString()));
            fw.close();
            MsgBox.msgInfo("Save settings", "Save settings successfully complete.");
//			dialogStage.close();
            return true;
        } catch (Exception e) {
            LOGGER.error("Save settings, Marshaller", e);
            MsgBox.msgException(e);
            return false;
        }
    }

    private void fillPrinterBox() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        List<String> prntList = new ArrayList<>();
        for (PrintService printer : printServices) {
            prntList.add(printer.getName());
        }
        printerCombo.setItems(FXCollections.observableArrayList(prntList));
    }

    @FXML
    private void selectLogFolder() {
        DirectoryChooser dir_chooser = new DirectoryChooser();
        File file = dir_chooser.showDialog(dialogStage);
        if (file != null) {
            logFolder.setText(file.getAbsolutePath() + "\\");
        }
    }

    @FXML
    private void selectPuttyFile() {
        FileChooser file_chooser = new FileChooser();
        File file = file_chooser.showOpenDialog(dialogStage);
        if (file != null) {
            puttyFile.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void selectVlcFile() {
        FileChooser file_chooser = new FileChooser();
        File file = file_chooser.showOpenDialog(dialogStage);
        if (file != null) {
            vlcFile.setText(file.getAbsolutePath());
        }
    }

    private void setCurrSettings(Settings currSettings) {
        this.currSettings = currSettings;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}
