package xsightassembler.view;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import xsightassembler.utils.Utils;

import java.util.TreeSet;

public class AssemblerController {

    @FXML
    private Accordion modules;

    @FXML
    private void initialize() {
//        ToDo: Utils.getModulesMap(nulluser)
        TreeSet<String> keySet = new TreeSet<>(Utils.getModulesMap(null).keySet());
        keySet.forEach(c -> {
            GridPane grid = getGridForm();
            TitledPane pane = new TitledPane(c, grid);
            pane.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
                if (newPropertyValue)
                {
//                    modules.setExpandedPane(pane);
//                    try {
//                        while (!pane.isExpanded()){
//                            Thread.sleep(500);
//                        }
//                        ((TextField) getNodeByColumnRowIndex(1, 0, (GridPane) pane.getContent())).setText("test");
//                        Robot r = new Robot();
//                        r.keyPress(KeyEvent.VK_TAB);
//                    } catch (AWTException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
            });
            modules.getPanes().add(pane);
        });
    }

    private Node getNodeByColumnRowIndex(final int column, final int row, GridPane gridPane) {
        Node result = null;
        ObservableList<Node> childrens = gridPane.getChildren();
        for(Node node : childrens) {
            if(gridPane.getRowIndex(node) == row && gridPane.getColumnIndex(node) == column) {
                result = node;
                break;
            }
        }
        return result;
    }

    private GridPane getGridForm(){
        GridPane grid = new GridPane();
        grid.setVgap(4);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.add(new Label("First Name: "), 0, 0);
        grid.add(new TextField(), 1, 0);
        grid.add(new Label("Last Name: "), 0, 1);
        grid.add(new TextField(), 1, 1);
        grid.add(new Label("Email: "), 0, 2);
        grid.add(new TextField(), 1, 2);
        return grid;
    }
}
