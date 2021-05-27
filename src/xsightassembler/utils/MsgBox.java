package xsightassembler.utils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Pair;
import xsightassembler.MainApp;
import xsightassembler.models.MailAddress;
import xsightassembler.models.Pallet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

public interface MsgBox {
    Image favicon = new Image(Objects.requireNonNull(
            MainApp.class.getClassLoader().getResourceAsStream("logo.png")));

    static String msgInputString(String content) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Strings.input);
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);
        dialog.setHeaderText(null);
        dialog.setContentText(content);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    static double msgInputDouble(String content) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Strings.input);
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);
        dialog.setHeaderText(null);
        dialog.setContentText(content);

        Optional<String> result = dialog.showAndWait();
        String resultString = result.orElse(null);
        double res = 0.0;
        if (resultString != null){
            try {
                res = Double.parseDouble(resultString) ;
            } catch (NumberFormatException e){
                e.printStackTrace();
                msgWarning(Strings.incorrInput);
                return msgInputDouble(content);
            }
        }
        return res;
    }

    static String msgInputPassword(String content) {
        ImageView passImg = new ImageView(new Image(Objects.requireNonNull(
                MainApp.class.getClassLoader().getResourceAsStream("password.png")),
                64, 64, false, false));
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(Strings.inpPassw);
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);
        dialog.setHeaderText(null);
        dialog.setContentText(content);
        dialog.setGraphic(passImg);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField pwd = new PasswordField();
        HBox cont = new HBox();
        cont.setAlignment(Pos.CENTER_LEFT);
        cont.setSpacing(10);
        cont.getChildren().addAll(new Label(Strings.password), pwd);
        dialog.getDialogPane().setContent(cont);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return pwd.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    static boolean msgConfirm(String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Strings.appName);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.filter(buttonType -> (buttonType == ButtonType.OK)).isPresent();
    }

    static void msgException(Exception e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            System.out.println(e.getLocalizedMessage());
            alert.setTitle(Strings.exception);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(favicon);
            alert.setHeaderText(e.getMessage());
            alert.setContentText(e.toString());

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();

            Label label = new Label(Strings.exceptTrace);

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);

            alert.showAndWait();
        });
    }

    static void msgException(String s, RuntimeException e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            System.out.println(e.getLocalizedMessage());
            alert.setTitle(Strings.exception);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(favicon);
            alert.setHeaderText(e.getMessage());
            alert.setContentText(e.toString());

            // Create expandable Exception.
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();

            Label label = new Label(Strings.exceptTrace);

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);

            // Set expandable Exception into the dialog pane.
            alert.getDialogPane().setExpandableContent(expContent);

            alert.showAndWait();
        });
    }

    static void msgError(String content){
        msgError(Strings.appName, content);
    }

    static void msgError(String title, String content) {
        msgError(title, null, content);
    }

    static void msgError(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(favicon);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    static void msgInfo(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(favicon);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    static void msgInfo(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(favicon);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    static void msgInfo(String content) {
        msgInfo(Strings.appName, content);
    }

    static void msgWarning(String content){
        msgWarning(Strings.appName, content);
    }

    static void msgWarning(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(favicon);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    static String msgChoice(String header, String content, List<String> choices){
        ChoiceDialog<String> dialog = new ChoiceDialog<>(null, choices);
        dialog.setTitle(Strings.appName);
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    static String msgChoice(String header, String content, List<String> choices, String selectedItem){
        ChoiceDialog<String> dialog = new ChoiceDialog<>(null, choices);
        dialog.setTitle(Strings.appName);
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        dialog.setSelectedItem(selectedItem);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    static Optional<String> msgInputStringWithConfirm(String msg) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Confirm");
        dialog.setHeaderText(msg);
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);
        ButtonType confirmButtonType = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField string1 = new TextField();
        string1.setPromptText("Enter your email");
        TextField string2 = new TextField();
        string2.setPromptText("Confirm your email");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(string1, 1, 0);
        grid.add(new Label("Confirm:"), 0, 1);
        grid.add(string2, 1, 1);

        Node confirmButton = dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setDisable(true);

        string1.textProperty().addListener((observable, oldValue, newValue) -> {
            Matcher m = Strings.pEmail.matcher(newValue.trim());
            confirmButton.setDisable(newValue.trim().isEmpty() || !m.find());
        });

        string2.textProperty().addListener((observable, oldValue, newValue) -> {
            Matcher m = Strings.pEmail.matcher(newValue.trim());
            confirmButton.setDisable(newValue.trim().isEmpty() || !m.find());
        });

        string1.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                confirmButton.setDisable(!string1.getText().trim().equalsIgnoreCase(string2.getText().trim()));
            }
        });

        string2.focusedProperty().addListener((arg0, oldValue, newValue) -> {
            if (!newValue) {
                confirmButton.setDisable(!string1.getText().trim().equalsIgnoreCase(string2.getText().trim()));
            }
        });

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(string1::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return string1.getText().trim();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        return result;
    }

    static Optional<Pair<String, String>> msgLogin(){
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText(String.format("%s login", Strings.appName));
        ImageView passImg = new ImageView(new Image(Objects.requireNonNull(
                MainApp.class.getClassLoader().getResourceAsStream("pre_logo_300.png"))));

        dialog.setGraphic(passImg);
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty() || password.getText().trim().isEmpty());
        });

        password.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty() || username.getText().trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

// Request focus on the username field by default.
        Platform.runLater(() -> username.requestFocus());

// Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

//        result.ifPresent(usernamePassword -> {
//            System.out.println("Username=" + usernamePassword.getKey() + ", Password=" + usernamePassword.getValue());
//        });
        return result;
    }

    static Optional<ObservableList<MailAddress>> msgMultiselection(List<MailAddress> mailList){
        Dialog<ObservableList<MailAddress>> dialog = new Dialog<>();
        dialog.setTitle("Selection");
        dialog.setHeaderText("Select recipient addresses");
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);

        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);


        ListView<MailAddress> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(mailList));
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(param -> new ListCell<MailAddress>() {
            @Override
            protected void updateItem(MailAddress item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item.getString());
                }
            }
        });

        dialog.getDialogPane().setContent(listView);
        Node selectButton= dialog.getDialogPane().lookupButton(selectButtonType);
        selectButton.setDisable(true);

        listView.setOnMouseClicked((EventHandler<Event>) event -> {
            ObservableList<MailAddress> selectedItems =  listView.getSelectionModel().getSelectedItems();
            selectButton.setDisable(selectedItems.size() < 1);
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return listView.getSelectionModel().getSelectedItems();
            }
            return null;
        });
        Optional<ObservableList<MailAddress>> result = dialog.showAndWait();
        return result;
    }

    public static Optional<ObservableList<Pallet>> msgPalletsSelection(List<Pallet> paletsList){
        Dialog<ObservableList<Pallet>> dialog = new Dialog<>();
        dialog.setTitle("Selection");
        dialog.setHeaderText("Select pallets for report");
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(favicon);

        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);


        ListView<Pallet> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(paletsList));
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(param -> new ListCell<Pallet>() {
            @Override
            protected void updateItem(Pallet item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item.getPalletNumber());
                }
            }
        });

        dialog.getDialogPane().setContent(listView);
        Node selectButton= dialog.getDialogPane().lookupButton(selectButtonType);
        selectButton.setDisable(true);

        listView.setOnMouseClicked((EventHandler<Event>) event -> {
            ObservableList<Pallet> selectedItems =  listView.getSelectionModel().getSelectedItems();
            selectButton.setDisable(selectedItems.size() < 1);
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return listView.getSelectionModel().getSelectedItems();
            }
            return null;
        });
        Optional<ObservableList<Pallet>> result = dialog.showAndWait();
        return result;
    }
}
