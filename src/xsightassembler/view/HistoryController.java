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
import xsightassembler.models.Isduh;
import xsightassembler.utils.MsgBox;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class HistoryController {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Method method;
    private Stage stage;
    private Object module;
    private Set<History> historySet = new HashSet<>();

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
        addHistoryColumnTab("Comment", "getComment");
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
        ObservableList<History> res = FXCollections.observableArrayList(this.historySet);
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
            String typeModule;
            if (module.getClass().getSimpleName().equals("Isduh")) {
                sn = (String) module.getClass().getMethod("getSn").invoke(module);
                typeModule = "system";
                List<Object> tmp = (List<Object>) module.getClass().getMethod("getModulesList").invoke(module);
                Isduh isduh = (Isduh) module;
                tmp.addAll(isduh.getHistorySet());
                for (Object o: tmp) {
                    if (o != null) {
                        if (o instanceof History) {
                            this.historySet.add((History) o);
                        } else {
                            Set<History> hSet = (Set<History>) o.getClass().getMethod("getHistorySet").invoke(o);
                            if (hSet != null && hSet.size() > 0) {
                                this.historySet.addAll(hSet);
                            }
                        }

                    }
                }
            } else {
                sn = (String) module.getClass().getMethod("getModule").invoke(module);
                typeModule = module.getClass().getSimpleName();
                this.historySet = (Set<History>) module.getClass().getMethod("getHistorySet").invoke(module);
            }

            this.stage.setTitle(String.format("History of %s SN: %s", typeModule, sn));
            fillTable();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }

    }
}
