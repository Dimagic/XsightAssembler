package xsightassembler.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
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
    private MenuBar menuBar;


    @FXML
    public void initialize(){

    }

    public void initMenu(boolean isAdmin) {
        if (isAdmin) {
            initAdminMenu();
        } else {
            initUserMenu();
        }
    }

    private void initAdminMenu() {
        menuBar.getMenus().addAll(getFileMenu(), getUtilsMenu(), getSettingsMenu());
    }

    private void initUserMenu() {
        menuBar.getMenus().addAll(getFileMenu());
    }

    private Menu getFileMenu() {
        Menu menu = new Menu("File");
        MenuItem menuLogout = new MenuItem("Logout");
        menuLogout.setOnAction(e -> {
            menuBar.getMenus().clear();
            logout();
        });

        MenuItem menuExit = new MenuItem("Exit");
        menuExit.setOnAction(e -> {
            System.exit(0);
        });
        menu.getItems().addAll(menuLogout, menuExit);
        return menu;
    }

    private Menu getUtilsMenu() {
        Menu menu = new Menu("Utils");
        MenuItem menuLog = new MenuItem("Open log analyzer");
        menuLog.setOnAction(e -> {
            showLogAnalyzer();
        });

        MenuItem menuAssembler = new MenuItem("Open assembler");
        menuAssembler.setOnAction(e -> {
            mainApp.showAssemblyView();
        });

        MenuItem menuRefresh = new MenuItem("Refresh test journal");
        menuRefresh.setOnAction(e -> {
            mainApp.getBiJournalController().fillTable();
        });

        menu.getItems().addAll(menuLog, menuAssembler, menuRefresh);
        return menu;
    }

    private Menu getSettingsMenu() {
        Menu menu = new Menu("Settings");
        MenuItem menuAddress = new MenuItem("Addresses");
        menuAddress.setOnAction(e -> {
            mainApp.showMailAddressView();
        });

        MenuItem menuUsers = new MenuItem("Users");
        menuUsers.setOnAction(e -> {
            mainApp.showUsersView();
        });

        MenuItem menuSettings = new MenuItem("Settings");
        menuSettings.setOnAction(e -> {
            mainApp.showSettingsDialog();
        });

        menu.getItems().addAll(menuAddress, menuUsers, menuSettings);
        return menu;
    }

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

    private void logout(){
        BiJournalController bjc = mainApp.getBiJournalController();
        if (bjc != null) {
            bjc.shutdown();
        }
        root.setCenter(null);
        mainApp.login();
    }

    public void setCurrentDbLbl(String val){
        currentDbLbl.setText(val);
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}
