package xsightassembler;

import javafx.application.Application;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.models.Isduh;
import xsightassembler.models.LogItem;
import xsightassembler.models.User;
import xsightassembler.services.UserService;
import xsightassembler.utils.*;
import xsightassembler.view.*;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class MainApp extends Application {
    Logger LOGGER = LogManager.getLogger(MainApp.class.getName());

    private final Image favicon = new Image(Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream("logo.png")));
    private final UserService userService = new UserService();

    private Stage primaryStage;
    private Stage biJournalStage;
    private Stage palletStage;
    private Stage assemblyStage;
    private BorderPane rootView;
    private RootController rootController;
    private AssemblyJournalController assemblyJournalController;
    private BiJournalController biJournalController;
    private User currentUser;
    private final HashMap<String, Stage> testViewMap = new HashMap<>();
    private final HashMap<String, Stage> assemblyViewMap = new HashMap<>();


    @Override
    public void init() {

    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(Strings.appNameWithVer);
        this.primaryStage.getIcons().add(favicon);
        primaryStage.setOnHidden(e -> {
            if (biJournalController != null) {
                biJournalController.shutdown();
            }
            try {
                FileUtils.cleanDirectory(new File("./tmp"));
            } catch (NullPointerException | IllegalArgumentException ignored) {
            } catch (IOException ioException) {
                LOGGER.error("clear logs", ioException);
                MsgBox.msgException(ioException);
            }
            System.exit(0);
//            Platform.exit();
        });

        initRootLayout();

    }

    public void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("RootView.fxml"));
            rootView = loader.load();
            Scene scene = new Scene(rootView);
            primaryStage.setScene(scene);
            primaryStage.setMinHeight(700);
            primaryStage.setMinWidth(900);
            rootController = loader.getController();
            rootController.setMainApp(this);
            primaryStage.show();
            if (checkDbConnection()) {
                login();
            } else {
                showSettingsDialog();
            }
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("AssemblyJournalView.fxml"));
            AnchorPane mainPage = loader.load();
            rootView.setCenter(mainPage);
            assemblyJournalController = loader.getController();
            assemblyJournalController.setMainApp(this);
            assemblyJournalController.setStage(primaryStage);
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showAssemblyView() {
        try {
            if (assemblyStage != null) {
                assemblyStage.setMaximized(true);
                assemblyStage.requestFocus();
                return;
            }
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("AssemblyJournalView.fxml"));
            AnchorPane page = loader.load();
            assemblyStage = new Stage();
            assemblyStage.setTitle("Assembler");
            assemblyStage.getIcons().add(favicon);
            assemblyStage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            assemblyStage.setScene(scene);
            assemblyJournalController = loader.getController();
            assemblyJournalController.setMainApp(this);
            assemblyJournalController.setStage(assemblyStage);
            assemblyStage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
                assemblyStage = null;
            });
            assemblyStage.showAndWait();
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("SettingsView.fxml"));
            AnchorPane page = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.getIcons().add(favicon);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            stage.setScene(scene);
            stage.setResizable(false);
            SettingsController settingsViewController = loader.getController();
            settingsViewController.setMainApp(this);
            settingsViewController.fillSettings();
            settingsViewController.setDialogStage(stage);
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showMailAddressView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("MailAddressView.fxml"));
            AnchorPane page = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Addresses");
            stage.getIcons().add(favicon);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            stage.setScene(scene);
            stage.setResizable(false);
            MailAddressController mailAddressController = loader.getController();

            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showAllInOneAssemblerView(Isduh isduh) {
        try {
            if (isduh != null && assemblyViewMap.get(isduh.getSn()) != null) {
                assemblyViewMap.get(isduh.getSn()).requestFocus();
            } else {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getClassLoader().getResource("AllInOneAssemblerView.fxml"));
                AnchorPane page = loader.load();
                Stage stage = new Stage();
                stage.getIcons().add(favicon);
                stage.initModality(Modality.WINDOW_MODAL);
                stage.initOwner(assemblyStage);
                Scene scene = new Scene(page);
                stage.setScene(scene);
                stage.setResizable(false);
                AllInOneAssemblerController controller = loader.getController();
                controller.setMainApp(this);
                controller.setStage(stage);
                controller.setMainController(assemblyJournalController);
                if (isduh != null) {
                    controller.setIsduhSystem(isduh);
                    stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                        assemblyViewMap.put(isduh.getSn(), stage);
                    });
                    stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> {
                        assemblyViewMap.remove(isduh.getSn());
                    });
                }
                stage.showAndWait();
            }
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showBiJournal() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("BiJournal.fxml"));
            AnchorPane page = loader.load();
            biJournalStage = new Stage();
            biJournalStage.setTitle("BI journal");
            biJournalStage.getIcons().add(favicon);
            biJournalStage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            biJournalStage.setScene(scene);
            biJournalController = loader.getController();
            biJournalStage.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> {
                biJournalController.shutdown();
            });
            biJournalController.setMainApp(this);
            biJournalController.setStage(biJournalStage);

            biJournalStage.showAndWait();
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    private void showBiJournal2() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("BiJournal.fxml"));
            AnchorPane mainPage = loader.load();
            rootView.setCenter(mainPage);
            biJournalController = loader.getController();
            biJournalController.setMainApp(this);
            biJournalController.setStage(primaryStage);
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showPalletView() {
        try {
            if (palletStage != null) {
                palletStage.requestFocus();
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("PalletView.fxml"));
                AnchorPane page = loader.load();
                palletStage = new Stage();
                palletStage.setTitle("Pallet journal");
                palletStage.getIcons().add(favicon);
                palletStage.initModality(Modality.WINDOW_MODAL);
                Scene scene = new Scene(page);
                palletStage.setScene(scene);
                PalletController controller = loader.getController();
                controller.setMainApp(this);
                controller.setStage(palletStage);
                palletStage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
                    palletStage = null;
                });
                palletStage.showAndWait();
            }

        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showLogView(BiTestWorker btw) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("LogView.fxml"));
            SplitPane page = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Log analyze: " + btw.getIsduh().getSn());
            stage.getIcons().add(favicon);
            stage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            stage.setScene(scene);
            LogViewController controller = loader.getController();
            controller.setMainApp(this);
            controller.setStage(stage);
            controller.setBtw(btw);
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showLogView(FilteredList<LogItem> itemList) {
        if (itemList == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("LogView.fxml"));
            SplitPane page = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Analysis of selected logs");
            stage.getIcons().add(favicon);
            stage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            stage.setScene(scene);
            LogViewController controller = loader.getController();
            controller.setMainApp(this);
            controller.setItemList(itemList);
            controller.setStage(stage);
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public boolean showBiTestView(BiTestWorker btw) {
        try {
            if (testViewMap.get(btw.getBiNetName().getValue()) != null) {
                testViewMap.get(btw.getBiNetName().getValue()).requestFocus();
            } else {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getClassLoader().getResource("BiTestView.fxml"));
                AnchorPane page = loader.load();
                Stage stage = new Stage();
                stage.getIcons().add(favicon);
                stage.initModality(Modality.WINDOW_MODAL);
                Scene scene = new Scene(page);
                stage.setScene(scene);
                BiTestController controller = loader.getController();
                stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                    testViewMap.put(btw.getBiNetName().getValue(), stage);
                });
                stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> {
                    testViewMap.remove(btw.getBiNetName().getValue());
                    controller.shutdown();
                });
                controller.setMainApp(this);
                controller.setStage(stage);
                controller.setJournalController(biJournalController);
                controller.setBiTestWorker(btw);
                stage.showAndWait();
                return controller.isPassFailClicked();
            }
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return false;
    }

    public void showModuleAssemblerView(Class<?> c) {
        showModuleAssemblerView(c, primaryStage);
    }

    public void showModuleAssemblerView(Class<?> c, Stage parentStage) {
        showModuleAssemblerView(c, parentStage, null);
    }

    public void showModuleAssemblerView(Class<?> c, Stage parentStage, Object o) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("ModuleAssemblerView.fxml"));
            SplitPane page = loader.load();
            ModuleAssemblerController controller = loader.getController();
            Stage stage = new Stage();
            stage.getIcons().add(favicon);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(assemblyStage);
            Scene scene = new Scene(page);
            stage.setScene(scene);
            controller.setMainApp(this);
            if (o != null) {
                controller.setTypeModule(o.getClass());
                controller.setCurrentModule(o);
            } else {
                controller.setTypeModule(c);
            }
            controller.setStage(stage);

            stage.showAndWait();
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showHistoryView(Object module, Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("HistoryView.fxml"));
            AnchorPane page = loader.load();
            Stage stage = new Stage();
            stage.getIcons().add(favicon);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            Scene scene = new Scene(page);
            stage.setScene(scene);
            stage.setResizable(false);
            HistoryController controller = loader.getController();
            controller.setStage(stage);
            controller.setModule(module);
            stage.show();
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showIsduhEditorView(Isduh isduh) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("IsduhEditorView.fxml"));
            AnchorPane page = loader.load();
            IsduhEditorController controller = loader.getController();
            Stage stage = new Stage();
            stage.getIcons().add(favicon);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(primaryStage);
            stage.setResizable(false);
            Scene scene = new Scene(page);
            stage.setScene(scene);
            stage.setOnHiding(e -> {
                assemblyJournalController.fillTable();
            });

            controller.setMainApp(this);
            if (isduh != null) {
                controller.setIsduh(isduh);
                stage.setTitle(isduh.getSn());
            } else {
                stage.setTitle("New device");
            }
            controller.setStage(stage);

            stage.showAndWait();

        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void showUsersView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("UsersView.fxml"));
            AnchorPane page = loader.load();
            UsersController controller = loader.getController();
            Stage stage = new Stage();
            stage.getIcons().add(favicon);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            stage.setScene(scene);

            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    private boolean checkDbConnection() {
        Map<String, String> connectionInfo = null;
        try {
            connectionInfo = HibernateSessionFactoryUtil.getConnectionInfo();
            if (connectionInfo != null) {
                rootController.setCurrentDbLbl(connectionInfo.get("DataBaseUrl"));
            } else {
                rootController.setCurrentDbLbl("No DB connection");
            }
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            rootController.setCurrentDbLbl("No DB connection");
            MsgBox.msgError(e.getLocalizedMessage());
        }
        return connectionInfo != null;
    }

    public User login() {
        User user = null;
        try {
            if (userService.findAll().size() == 0) {
                if (MsgBox.msgConfirm("Users not found. You have to add admin user")) {
                    String pass1 = MsgBox.msgInputPassword("Enter password for user Admin");
                    String pass2 = MsgBox.msgInputPassword("Retry password for user Admin");
                    if (!pass1.equals(pass2)) {
                        MsgBox.msgWarning("Passwords not equals");
                        return login();
                    }
                    String email = MsgBox.msgInputStringWithConfirm("Enter email for password recovery").orElse(null);
                    if (email == null) {
                        System.exit(0);
                    }
                    User admin = new User();
                    admin.setLogin("admin");
                    admin.setFirstName("admin");
                    admin.setLastName("admin");
                    admin.setPassword(pass1);
                    admin.setUserRole(1);
                    admin.setUserStatus(1);
                    admin.setEmail(email);
                    if (userService.save(admin)) {
                        MsgBox.msgInfo("Admin register complete");
                        return login();
                    }
                } else {
                    System.exit(0);
                }

            }
            Pair<String, String> login = MsgBox.msgLogin().orElse(null);
            if (login == null) {
                System.exit(0);
            }
            user = userService.findByLogin(login.getKey());
            if (user == null || !user.getPassword().equals(Utils.stringToHash(login.getValue()))) {
                MsgBox.msgWarning("Combination User/Password not found");
                return login();
            } else {
                if (user.getUserStatus() == 0) {
                    MsgBox.msgInfo("User disabled");
                    return login();
                }
                if (user.getPassword().equals(Utils.stringToHash(user.getLogin()))) {
                    if (!MsgBox.msgConfirm(Strings.firstEntrance)) {
                        System.exit(0);
                    } else {
                        String pass1 = MsgBox.msgInputPassword("Enter new password");
                        String pass2 = MsgBox.msgInputPassword("Confirm password");
                        if ((pass1 != null && pass2 != null) && pass1.equals(pass2)) {
                            user.setPassword(pass1);
                            userService.update(user);
                            if (Utils.stringToHash(pass1).equals(user.getPassword())) {
                                MsgBox.msgInfo("Change password complete.");
                                return login();
                            } else {
                                MsgBox.msgInfo("Something wrong");
                                return null;
                            }

                        } else {
                            MsgBox.msgWarning("Passwords not equals");
                            return login();
                        }
                    }
                }

                user.setLastLogin(new Date());
                userService.update(user);
                currentUser = user;

                rootController.initMenu(user.getUserRole() == 1);
                this.primaryStage.setTitle(Strings.appNameWithVer + " User: " + user.getLogin());
                if (assemblyJournalController != null) {
                    assemblyJournalController.getMainPane().setVisible(true);
                    assemblyJournalController.setMainApp(this);
                }
                int userRole = user.getUserRole();
                if (userRole == 1 || userRole == 2) {
                    showBiJournal2();
                } else {
                    showMainView();
                }

                return user;
            }
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }

        return user;
    }

    public BiJournalController getBiJournalController() {
        return biJournalController;
    }

    public AssemblyJournalController getMainController() {
        return assemblyJournalController;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public RootController getRootController() {
        return rootController;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Image getFavicon() {
        return favicon;
    }

    public static void main(String[] args) {
        launch(args);
//        LauncherImpl.launchApplication(MainApp.class, XPreloader.class, args);
    }
}
