package xsightassembler.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.RangeSlider;
import xsightassembler.MainApp;
import xsightassembler.models.LogItem;
import xsightassembler.utils.MsgBox;
import xsightassembler.utils.Utils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class LogViewController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private MainApp mainApp;
    private Stage stage;
    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private FilteredList<LogItem> itemList;
    private List<LogItem> searchResultList = new ArrayList<>();
    private LogItem selectedLogItem;
    private RangeSlider durationSlider;
    private Label durationLbl = new Label();
    private Date startDate;
    private Date stopDate;
    private long duration;
    private final Image failImg = new Image(getClass().getClassLoader().getResourceAsStream("fail_32x32.png"));


    @FXML
    private ListView<LogItem> listView;
    @FXML
    private TreeView<LogItem> logTree;
    @FXML
    private VBox sliderBox;
    @FXML
    private Label startLbl;
    @FXML
    private Label stopLbl;
    @FXML
    private Label searchResultLbl;
    @FXML
    private Label clipboardLbl;
    @FXML
    private TextField selectedItemField;
    @FXML
    private Button upBtn;
    @FXML
    private Button downBtn;
    @FXML
    private Button filterBtn;
    @FXML
    private CheckBox isIgnoreCase;


    @FXML
    private void initialize() {
        Tooltip.install(upBtn, new Tooltip("Search up"));
        Tooltip.install(downBtn, new Tooltip("Search down"));
        Tooltip.install(filterBtn, new Tooltip("Filter by value"));

        upBtn.setDisable(true);
        downBtn.setDisable(true);
        filterBtn.setDisable(true);

        logTree.setShowRoot(false);
        listView.setCellFactory(param -> new ListCell<LogItem>() {
            @Override
            protected void updateItem(LogItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getFullMsg() == null) {
                    setText(null);
                    setStyle("-fx-control-inner-background: derive(palegreen, 50%)");
                } else {
                    setText(item.getFullMsg());
                    if (item != null && item.isIbit()) {
                        setStyle("-fx-control-inner-background: derive(palegreen, 50%)");
                    } else {
                        setStyle("-fx-control-inner-background: derive(-fx-base, 80%)");
                    }
                }
            }
        });

        listView.setOnMouseClicked(event -> {
            selectedLogItem = listView.getSelectionModel().getSelectedItem();
            if (selectedLogItem == null) {
                selectedItemField.setText("");
            } else {
                selectedItemField.setText(selectedLogItem.getFullMsg());
            }
        });

        selectedItemField.setOnMouseReleased(e -> {
            clipboard.clear();
            selectedItemField.copy();
            if (clipboard.hasString()) {
                searchResultList = searchByString(clipboard.getString());
                searchResultLbl.setText(String.format("Found %s items", searchResultList.size()));
                upBtn.setDisable(searchResultList.isEmpty());
                downBtn.setDisable(searchResultList.isEmpty());
            }
        });

        selectedItemField.textProperty().addListener((observable, oldValue, newValue) -> {
            clipboardLbl.setText("");
            if (newValue.isEmpty()) {
                searchResultList.clear();
                selectedItemField.clear();
                searchResultLbl.setText("");
                upBtn.setDisable(true);
                downBtn.setDisable(true);
            } else {
                try {
                    searchResultList = searchByPattern(newValue);
                    if (searchResultList.isEmpty()) {
                        searchResultList = searchByString(newValue);
                    }
                    searchResultLbl.setText(String.format("Found %s items", searchResultList.size()));
                    upBtn.setDisable(searchResultList.isEmpty());
                    downBtn.setDisable(searchResultList.isEmpty());
                } catch (PatternSyntaxException e) {
                    clipboardLbl.setText("Incorrect pattern, looking for string");
                }
            }
        });

        logTree.setCellFactory(param -> new TreeCell<LogItem>() {
            @Override
            protected void updateItem(LogItem item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(
                        "  -fx-base: #fdfdfd ;\n" +
                                "  -fx-control-inner-background: derive(-fx-base,20%);\n" +
                                "  -fx-control-inner-background-alt: derive(-fx-control-inner-background,-10%);\n" +
                                "  -fx-accent: #006689;\n" +
                                "  -fx-focus-color: #036e83;\n" +
                                "  -fx-faint-focus-color: #036e83;");
                if (empty || item == null) {
                    setText(null);
//                    setStyle("-fx-background-color: derive(-fx-base, 80%)");
                } else {
                    if (item.getFullMsg() == null) {
                        setText(item.getErrType());
                    } else {
                        setText(String.format("%s -> %s", Utils.getFormattedTime(item.getDate()), item.getMessage()));
                        if (!item.isIgnore()) {
                            setStyle("-fx-background-color: derive(red, 50%)");
                        }
//                        if (item.isIgnore()) {
//                            setStyle("-fx-background-color: derive(#28ee28, 50%)");
//                        } else {
//
//                        }
                    }
                }
            }
        });

        logTree.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if (newValue != null) {
                listView.scrollTo(newValue.getValue());
                listView.getSelectionModel().select(newValue.getValue());
                selectedItemField.clear();
            }
        });

        EventHandler<MouseEvent> mouseEventHandle = this::handleMouseClicked;


        logTree.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEventHandle);

        downBtn.setOnMouseClicked(e -> {
            int i = searchResultList.indexOf(selectedLogItem);
            searchResultLbl.setText(String.format("Selected %s of  %s items", i + 1, searchResultList.size()));
            try {
                selectedLogItem = searchResultList.get(i + 1);
                if (selectedLogItem != null) {
                    listView.scrollTo(selectedLogItem);
                    listView.getSelectionModel().select(selectedLogItem);
                }
            } catch (IndexOutOfBoundsException ex) {
                MsgBox.msgInfo("Nothing found");
            }
        });

        upBtn.setOnMouseClicked(e -> {
            try {
                int i = searchResultList.indexOf(selectedLogItem);
                searchResultLbl.setText(String.format("Selected %s of  %s items", i + 1, searchResultList.size()));
                selectedLogItem = searchResultList.get(i - 1);
                if (selectedLogItem != null) {
                    listView.scrollTo(selectedLogItem);
                    listView.getSelectionModel().select(selectedLogItem);
                }
            } catch (IndexOutOfBoundsException ex) {
                MsgBox.msgInfo("Nothing found");
            }
        });

        isIgnoreCase.selectedProperty().addListener((observable, oldValue, newValue) -> {
            searchResultList.clear();
            if (!selectedItemField.getText().isEmpty()) {
                try {
                    searchResultList = searchByPattern(selectedItemField.getText());
                } catch (PatternSyntaxException e) {
                    searchResultList = searchByString(selectedItemField.getText());
                }
            }
            upBtn.setDisable(searchResultList.isEmpty());
            downBtn.setDisable(searchResultList.isEmpty());
            searchResultLbl.setText(String.format("Found %s items", searchResultList.size()));
        });
    }

    public void setItemList(FilteredList<LogItem> itemList) {
        this.itemList = Utils.setIgnoreFlagInLogs(itemList);
        this.itemList.setPredicate(s -> true);
        if (itemList.size() > 0) {
            startDate = itemList.get(0).getDate();
            stopDate = itemList.get(itemList.size() - 1).getDate();
            startLbl.setText(Utils.getFormattedDate(startDate));
            stopLbl.setText(Utils.getFormattedDate(stopDate));
        }

        setSlider();
        fillLogView();
    }

    private void fillLogView() {
        listView.getItems().clear();
        logTree.setRoot(null);
        itemList.setPredicate(s -> s.getDate().getTime() >= durationSlider.lowValueProperty().longValue() &&
                s.getDate().getTime() <= durationSlider.highValueProperty().longValue());

//        itemList.setPredicate(s -> s.getDate().getTime() < durationSlider.lowValueProperty().longValue() ||
//                s.getDate().getTime() > durationSlider.highValueProperty().longValue());
        for (LogItem i : itemList) {
            listView.getItems().add(i);
        }
        listView.refresh();
        fillLogTree();
    }

    private void fillLogTree() {
        itemList.setPredicate(s -> s.getErrType() != null &&
                s.getDate().getTime() >= durationSlider.lowValueProperty().longValue() &&
                s.getDate().getTime() <= durationSlider.highValueProperty().longValue());
        Set<String> errorsSet = new HashSet<>();
        itemList.forEach(i -> errorsSet.add(i.getErrType()));

        TreeItem<LogItem> rootItem = new TreeItem<>(new LogItem("Root"));
        TreeItem<LogItem> errorCategory;

        for (String val : errorsSet) {
            itemList.setPredicate(s -> s.getErrType() != null &&
                    s.getErrType().equalsIgnoreCase(val) &&
                    s.getDate().getTime() >= durationSlider.lowValueProperty().longValue() &&
                    s.getDate().getTime() <= durationSlider.highValueProperty().longValue());
            errorCategory = new TreeItem<>(new LogItem(String.format("%s [%s of %s ignored]", val,
                    itemList.stream().filter(LogItem::isIgnore).count(), itemList.size())));
            for (LogItem l : itemList) {
                TreeItem<LogItem> t = new TreeItem<>(l, new ImageView(failImg));
                errorCategory.getChildren().add(t);
            }
            rootItem.getChildren().add(errorCategory);
        }
        rootItem.getChildren().sort(Comparator.comparing(t -> t.getValue().getErrType()));
        logTree.setRoot(rootItem);
    }

    private void setSlider() {
        durationSlider = new RangeSlider(startDate.getTime(), stopDate.getTime(), startDate.getTime(), stopDate.getTime());
        setDuration();
        sliderBox.getChildren().addAll(durationSlider, durationLbl);
        sliderBox.setAlignment(Pos.BASELINE_CENTER);

        durationSlider.lowValueProperty().addListener((observable, oldValue, newValue) -> {
            startLbl.setText(Utils.getFormattedDate(new Date(newValue.longValue())));
            setDuration();
        });

        durationSlider.highValueProperty().addListener((observable, oldValue, newValue) -> {
            stopLbl.setText(Utils.getFormattedDate(new Date(newValue.longValue())));
            setDuration();
        });

        durationSlider.setOnMouseReleased(e -> fillLogView());
    }

    private void setDuration() {
        duration = durationSlider.highValueProperty().longValue() - durationSlider.lowValueProperty().longValue();
        durationLbl.setText(Utils.formatHMSM(duration));

        searchResultList.clear();
        selectedItemField.clear();
        searchResultLbl.setText("");
        upBtn.setDisable(true);
        downBtn.setDisable(true);
    }

    private void handleMouseClicked(MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        // Accept clicks only on node cells, and not on empty spaces of the TreeView
        if (node instanceof Text || (node instanceof TreeCell && ((TreeCell) node).getText() != null)) {
            LogItem treeItem = (LogItem) ((TreeItem) logTree.getSelectionModel().getSelectedItem()).getValue();
            if (!treeItem.equals(listView.getSelectionModel().getSelectedItem())) {
                listView.scrollTo(treeItem);
                listView.getSelectionModel().select(treeItem);
                selectedItemField.clear();
            }
        }
    }

    private List<LogItem> searchByString(String val) {
        clipboardLbl.setText(String.format("String for search: '%s'", val));
        if (isIgnoreCase.isSelected()) {
            return listView.getItems().stream().filter(c ->
                    c.getFullMsg().toLowerCase().
                            contains(val.toLowerCase())).collect(Collectors.toList());
        }
        return listView.getItems().stream().filter(c ->
                c.getFullMsg().contains(val)).collect(Collectors.toList());

    }

    private List<LogItem> searchByPattern(String val) throws PatternSyntaxException {
        Pattern pattern;
        clipboardLbl.setText(String.format("Pattern for search: %s", val));
        if (isIgnoreCase.isSelected()) {
            pattern = Pattern.compile(val.toLowerCase());
            return listView.getItems().stream().filter(c ->
                    pattern.matcher(c.getFullMsg().toLowerCase()).find()).collect(Collectors.toList());
        }
        pattern = Pattern.compile(val);
        return listView.getItems().stream().filter(c ->
                pattern.matcher(c.getFullMsg()).find()).collect(Collectors.toList());
    }

    @FXML
    private void clearSearchField() {
        selectedItemField.clear();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setMinHeight(400);
        stage.setMinWidth(800);
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}
