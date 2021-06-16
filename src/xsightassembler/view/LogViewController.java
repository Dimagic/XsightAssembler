package xsightassembler.view;

import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.RangeSlider;
import xsightassembler.MainApp;
import xsightassembler.dao.bi.BiNoteDao;
import xsightassembler.models.BiNote;
import xsightassembler.models.LogItem;
import xsightassembler.services.bi.BiNoteServise;
import xsightassembler.services.bi.BiTestService;
import xsightassembler.utils.BiTestWorker;
import xsightassembler.utils.CustomException;
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
    private BiTestWorker btw;
    private FilteredList<LogItem> itemList;
    private List<LogItem> searchResultList = new ArrayList<>();
    private LogItem selectedLogItem;
    private RangeSlider durationSlider;
    private final Label durationLbl = new Label();
    private Date startDate;
    private Date stopDate;
    private final RadioButton allBtn = new RadioButton("all");
    private final RadioButton correctBtn = new RadioButton("correct range");
    private final RadioButton incorrectBtn = new RadioButton("incorrect range");
    private HBox radioBox;

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

        allBtn.setSelected(true);

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
                    if (item.isIbit()) {
                        setStyle("-fx-control-inner-background: derive(palegreen, 50%)");
                        setTooltip(new Tooltip("BIT test running"));
                    } else if (item.isIncorrectDateRange()) {
                        setStyle("-fx-control-inner-background: derive(yellow, 50%)");
                        setTooltip(new Tooltip("Incorrect date range"));
                    } else {
                        setStyle("-fx-control-inner-background: derive(-fx-base, 80%)");
                        setTooltip(null);
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
                if (event.getButton() == MouseButton.SECONDARY) {
                    StringBuilder sb = new StringBuilder();
                    if (btw != null) {
                        BiNoteServise noteServise = new BiNoteServise();
                        BiTestService biTestService = new BiTestService();
                        try {
                            List<BiNote> notes = noteServise.findByBiTest(btw.getBiTest());
                            if (notes.size() == 0) {
                                sb.append("No comments yet");
                            } else {
                                sb.append("Comments for current test:\n");
                                notes.forEach(n -> sb.append(n.getNote()).append("\n"));
                            }
                            String comment = MsgBox.msgInputString("Add comment", sb.toString(),
                                    "comment", selectedLogItem.getMessage());
                            if (comment != null && !comment.isEmpty()) {
                                comment = comment.trim();
                                BiNote biNote = new BiNote();
                                biNote.setNoteDate(new Date());
                                biNote.setUser(mainApp.getCurrentUser());
                                biNote.setBiTest(btw.getBiTest());
                                biNote.setNote(comment.trim());
                                noteServise.save(biNote);
                                btw.getBiTest().addNote(biNote);
                                biTestService.saveOrUpdate(btw.getBiTest());
                                MsgBox.msgInfo(String.format("Comment:\n%s\nadded successfully.", comment));
                            }
                        } catch (CustomException e) {
                            LOGGER.error("Exception", e);
                            MsgBox.msgException(e);
                        }
                    }

                }
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
            filteredItemsNavigate(1);
        });

        upBtn.setOnMouseClicked(e -> {
            filteredItemsNavigate(-1);
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
        checkDateRangeCorrect();
        initSliderBox();
        setItemListPredicate(true);
        listView.setItems(itemList);
        fillLogTree();
        ifIncorrectRangePreset();
    }

    private void fillLogTree() {
        Set<String> errorsSet = new HashSet<>();
        FilteredList<LogItem> tmp = new FilteredList<>(listView.getItems());
        listView.getItems().forEach(i -> {
            if (i.getErrType() != null) {
                errorsSet.add(i.getErrType());
            }
        });

        TreeItem<LogItem> rootItem = new TreeItem<>(new LogItem("Root"));
        TreeItem<LogItem> errorCategory;

        for (String val : errorsSet) {
            tmp.setPredicate(s -> s.getErrType() != null &&
                    s.getErrType().equalsIgnoreCase(val) &&
                    s.getDate().getTime() >= durationSlider.lowValueProperty().longValue() &&
                    s.getDate().getTime() <= durationSlider.highValueProperty().longValue());
            errorCategory = new TreeItem<>(new LogItem(String.format("%s [%s of %s ignored]", val,
                    tmp.stream().filter(LogItem::isIgnore).count(), tmp.size())));
            for (LogItem l : tmp) {
                TreeItem<LogItem> t = new TreeItem<>(l);
                errorCategory.getChildren().add(t);
            }
            rootItem.getChildren().add(errorCategory);
        }
        rootItem.getChildren().sort(Comparator.comparing(t -> t.getValue().getErrType()));
        logTree.setRoot(rootItem);
        searchResultLbl.setText(String.format("Found %s items", itemList.size()));
    }

    private void initSliderBox() {
        durationSlider = new RangeSlider(0, 0, 0, 0);

        ToggleGroup group = new ToggleGroup();
        Label rangeLbl = new Label("Show:");
        allBtn.setToggleGroup(group);
        correctBtn.setToggleGroup(group);
        incorrectBtn.setToggleGroup(group);
        radioBox = new HBox();
        radioBox.setPadding(new Insets(10));
        radioBox.setSpacing(5);
        radioBox.getChildren().addAll(rangeLbl, allBtn, correctBtn, incorrectBtn);
        sliderBox.setAlignment(Pos.BASELINE_CENTER);
        sliderBox.getChildren().addAll(durationSlider, durationLbl, radioBox);

        group.selectedToggleProperty().addListener((ov, old_toggle, new_toggle) -> {
            setItemListPredicate(true);
        });

        durationSlider.lowValueProperty().addListener((observable, oldValue, newValue) -> {
            startLbl.setText(Utils.getFormattedDate(new Date(newValue.longValue())));
            setDuration();
        });

        durationSlider.highValueProperty().addListener((observable, oldValue, newValue) -> {
            stopLbl.setText(Utils.getFormattedDate(new Date(newValue.longValue())));
            setDuration();
        });

        durationSlider.setOnMouseReleased(e -> {
            setItemListPredicate();
            searchResultList = runSearch();
            searchResultLbl.setText(String.format("Found %s items", searchResultList.size()));
        });
    }

    private void setDuration() {
        long duration = durationSlider.highValueProperty().longValue() - durationSlider.lowValueProperty().longValue();
        durationLbl.setText(Utils.formatHMSM(duration));
    }

    private void ifIncorrectRangePreset() {
        boolean isIncorrectRangePresent = listView.getItems().stream().anyMatch(LogItem::isIncorrectDateRange);
        boolean isAllItemsInIncorrectRange = listView.getItems().stream().allMatch(LogItem::isIncorrectDateRange);
        if (isIncorrectRangePresent && !isAllItemsInIncorrectRange) {
            durationLbl.setText("Found incorrect date range");
        }
    }

    private void checkDateRangeCorrect() {
        long oldDate = 0;
        for (LogItem item : itemList) {
            if (oldDate == 0) {
                oldDate = item.getDate().getTime();
                continue;
            }
            if (oldDate > item.getDate().getTime()) {
//                ignore logs where time difference < 2 seconds
                if ((oldDate - item.getDate().getTime()) > 2000) {
                    item.setIncorrectDateRange(true);
                }
            } else {
                oldDate = item.getDate().getTime();
            }
        }
    }

    private void setItemListPredicate() {
        setItemListPredicate(false);
    }

    private void setItemListPredicate(boolean isResetLimits) {
        if (isResetLimits) {
            itemList.setPredicate(s -> {
                if (correctBtn.isSelected()) {
                    return !s.isIncorrectDateRange();
                }
                if (incorrectBtn.isSelected()) {
                    return s.isIncorrectDateRange();
                }
                return true;
            });
            setMinMaxDate();
            durationSlider.setMin(startDate.getTime());
            durationSlider.setMax(stopDate.getTime());
            durationSlider.setLowValue(startDate.getTime());
            durationSlider.setHighValue(stopDate.getTime());
        } else {
            itemList.setPredicate(s -> {
                if (correctBtn.isSelected()) {
                    return s.getDate().getTime() >= durationSlider.lowValueProperty().longValue() &&
                            s.getDate().getTime() <= durationSlider.highValueProperty().longValue() &&
                            !s.isIncorrectDateRange();
                }
                if (incorrectBtn.isSelected()) {
                    return s.getDate().getTime() >= durationSlider.lowValueProperty().longValue() &&
                            s.getDate().getTime() <= durationSlider.highValueProperty().longValue() &&
                            s.isIncorrectDateRange();
                }
                return s.getDate().getTime() >= durationSlider.lowValueProperty().longValue() &&
                        s.getDate().getTime() <= durationSlider.highValueProperty().longValue();
            });
        }
        fillLogTree();
        setDuration();
        ifIncorrectRangePreset();
    }

    private void setMinMaxDate() {
        if (itemList.size() == 1) {
            startDate = stopDate = itemList.get(0).getDate();
        } else if (!itemList.isEmpty()) {
            startDate = itemList.parallelStream().min(
                    Comparator.comparingLong(s -> s.getDate().getTime())).get().getDate();
            stopDate = itemList.parallelStream().max(
                    Comparator.comparingLong(s -> s.getDate().getTime())).get().getDate();
        } else {
            startDate = stopDate = new Date();
        }
    }

    @FXML
    private void clearSearchField() {
        selectedItemField.clear();
    }

    private List<LogItem> runSearch() {
        String val;
        if (clipboard.hasString() && selectedItemField.getText().contains(clipboard.getString())) {
            val = clipboard.getString();
        } else {
            val = selectedItemField.getText();
        }
        try {
            return searchByPattern(val);
        } catch (PatternSyntaxException e) {
            return searchByString(val);
        }
    }

    private List<LogItem> searchByString(String val) {
        List<LogItem> tmp;
        clipboardLbl.setText(String.format("String for search: '%s'", val));
        if (isIgnoreCase.isSelected()) {
            tmp = listView.getItems().stream().filter(c ->
                    c.getFullMsg().toLowerCase().
                            contains(val.toLowerCase())).collect(Collectors.toList());
        } else {
            tmp = listView.getItems().stream().filter(c ->
                    c.getFullMsg().contains(val)).collect(Collectors.toList());
        }
        return tmp;
    }

    private List<LogItem> searchByPattern(String val) throws PatternSyntaxException {
        Pattern pattern;
        List<LogItem> tmp;
        clipboardLbl.setText(String.format("Pattern for search: %s", val));
        if (isIgnoreCase.isSelected()) {
            pattern = Pattern.compile(val.toLowerCase());
            tmp = listView.getItems().stream().filter(c ->
                    pattern.matcher(c.getFullMsg().toLowerCase()).find()).collect(Collectors.toList());
        } else {
            pattern = Pattern.compile(val);
            tmp = listView.getItems().stream().filter(c ->
                    pattern.matcher(c.getFullMsg()).find()).collect(Collectors.toList());
        }
        return tmp;
    }

    private void handleMouseClicked(MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        if (node instanceof Text || (node instanceof TreeCell && ((TreeCell) node).getText() != null)) {
            LogItem treeItem = (LogItem) ((TreeItem) logTree.getSelectionModel().getSelectedItem()).getValue();
            if (!treeItem.equals(listView.getSelectionModel().getSelectedItem())) {
                listView.scrollTo(treeItem);
                listView.getSelectionModel().select(treeItem);
                selectedItemField.clear();
            }
        }
    }

    // direct may be 1 or -1
    private void filteredItemsNavigate (int direct){
        int i = 0;
        try {
            i = searchResultList.indexOf(selectedLogItem);
            selectedLogItem = searchResultList.get(i + direct);
            if (selectedLogItem != null) {
                listView.scrollTo(selectedLogItem);
                listView.getSelectionModel().select(selectedLogItem);
                i += direct;
            }
        } catch (IndexOutOfBoundsException ignored) {}
        searchResultLbl.setText(String.format("Selected item %s of  %s", i + 1, searchResultList.size()));
    }

    public void setBtw(BiTestWorker btw) {
        this.btw = btw;
        setItemList(btw.getLogItems());
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
