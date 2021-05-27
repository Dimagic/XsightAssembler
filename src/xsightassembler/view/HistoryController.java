package xsightassembler.view;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.models.History;
import xsightassembler.utils.MsgBox;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Set;

public class HistoryController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Method method;
    private Stage stage;
    private Object module;
    private Set<History> historySet;

    @FXML
    private TableView<History> tHistory;

    @FXML
    private void initialize() {
        tHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tHistory.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        tHistory.setRowFactory(this::rowFactoryTab);
        addHistoryColumnTab("Date", "getStringDate");
        addHistoryColumnTab("Field", "getFieldChange");
        addHistoryColumnTab("Old value", "getOldValue");
        addHistoryColumnTab("New value", "getNewValue");
        addHistoryColumnTab("User", "getUserLogin");
    }

    private TableRow<History> rowFactoryTab(TableView<History> view) {
        return new TableRow<>();
    }

    private void addHistoryColumnTab(String label, String dataIndex) {
        TableColumn<History, String> column = new TableColumn<>(label);
        column.prefWidthProperty().bind(tHistory.widthProperty().divide(4));
        column.setCellValueFactory(
                (TableColumn.CellDataFeatures<History, String> param) -> {
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
        column.setCellFactory(param -> new TableCell<History, String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                History history = (History) getTableRow().getItem();
                setText(item);
            }
        });
        tHistory.getColumns().add(column);
    }

    private void fillTable() {
        ObservableList<History> res = FXCollections.observableArrayList(historySet);;
        FXCollections.sort(res, Comparator.comparingLong(History::getDateMilliseconds));
        tHistory.setItems(FXCollections.observableArrayList(res));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setModule(Object module) {
        this.module = module;
        try {
            String sn;
            if (module.getClass().getSimpleName().equals("Isduh")) {
                sn = (String) module.getClass().getMethod("getSn").invoke(module);
            } else {
                sn = (String) module.getClass().getMethod("getModule").invoke(module);
            }

            this.stage.setTitle(String.format("History of %s SN: %s", module.getClass().getSimpleName(), sn));
            this.historySet = (Set<History>) module.getClass().getMethod("getHistorySet").invoke(module);
            fillTable();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }
}
