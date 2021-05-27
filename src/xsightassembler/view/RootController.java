package xsightassembler.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import xsightassembler.MainApp;
import xsightassembler.utils.BiLogAnalyzer;
import xsightassembler.utils.Utils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RootController {
    private MainApp mainApp;

    @FXML
    private BorderPane root;
    @FXML
    private Label currentDbLbl;
    @FXML
    private Menu adminMenu;
    @FXML
    private MenuItem switchTo;

    @FXML
    public void initialize(){
        adminMenu.setVisible(false);
    }

    @FXML
    private void openSettings(){
        mainApp.showSettingsDialog();
    }

    @FXML
    private void showUsers(){
        mainApp.showUsersView();
    }

    @FXML
    private void showAddresses() {
        mainApp.showMailAddressView();
    }

    @FXML
    private void showBiJournal() {
        mainApp.showBiJournal();
    }

    @FXML
    private void showLogAnalyzer() {
        String startFolder = Utils.getSettings().getLogFolder();
        FileChooser fileChooser = new FileChooser();
        if (startFolder != null && new File(startFolder).exists()) {
            fileChooser.setInitialDirectory(new File(startFolder));
        } else {
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }

        fileChooser.setTitle("Open Log files");
        List<File> fileList = fileChooser.showOpenMultipleDialog(mainApp.getPrimaryStage());
        if (fileList != null) {
            BiLogAnalyzer logAnalyzer = new BiLogAnalyzer(fileList);
            ExecutorService exService = Executors.newSingleThreadExecutor();
            logAnalyzer.setOnSucceeded(event -> {
                mainApp.showLogView(logAnalyzer.getValue());
            });
            logAnalyzer.setOnFailed(event -> {

            });
            exService.execute(logAnalyzer);
            exService.shutdown();
        }

    }

    @FXML
    private void logout(){
        BiJournalController bjc = mainApp.getBiJournalController();
        if (bjc != null) {
            bjc.shutdown();
        }
        root.setCenter(null);
        mainApp.login();
//        System.out.println(getController(root.getCenter()));
//        adminMenu.setVisible(false);
//        mainApp.getMainController().getMainPane().setVisible(false);
//        mainApp.getPrimaryStage().setTitle(Strings.appNameWithVer);
//        mainApp.getPrimaryStage().close();
//        mainApp.initRootLayout();
    }
    
    @FXML
    private void exit(){
        System.exit(0);
    }

    public void setAdminToolsVisible(boolean val) {
        adminMenu.setVisible(val);
        switchTo.setDisable(true);
        if (val) {
            switchTo.setText("Switch to assembler");
            switchTo.setOnAction(e -> {
                BiJournalController bjc = mainApp.getBiJournalController();
                if (bjc != null) {
                    bjc.shutdown();
                }
                root.setCenter(null);
                mainApp.showMainView();
                setAdminToolsVisible(false);
            });
        } else {
            switchTo.setText("Switch to tests");
            switchTo.setOnAction(e -> {
                root.setCenter(null);
                mainApp.showBiJournal();
                setAdminToolsVisible(true);
            });
        }

    }

    public void setCurrentDbLbl(String val){
        currentDbLbl.setText(val);
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}
