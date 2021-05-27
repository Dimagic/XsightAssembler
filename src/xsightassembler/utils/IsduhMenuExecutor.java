package xsightassembler.utils;

import javafx.util.Pair;
import xsightassembler.models.BiTest;
import xsightassembler.view.BiTestController;

import java.util.*;

public class IsduhMenuExecutor {
    private BiTest biTest;
    private BiTestController controller;
    private SortedSet<HashMap<String, String>> commands;
    private Settings settings = Utils.getSettings();

    public IsduhMenuExecutor(BiTest biTest, BiTestController controller) {
        this.biTest = biTest;
        this.controller = controller;
    }

    public void execMenu() {
//        SshClient jssh = new SshClient(biTest.getNetNameProperty().getValue(), settings.getSshUser(),
//                settings.getSshPass(), controller);
//        jssh.executeMenu(getTestCommands());
//        jssh.close();
    }

    private List<Pair<String, String>> getTestCommands() {
        List<Pair<String, String>> commands = new ArrayList<>();
        commands.add(new Pair<>("menu", "menu ver."));
        commands.add(new Pair<>("3", "Enter command ID:"));
        commands.add(new Pair<>("z", "Enter command ID:"));
        commands.add(new Pair<>("28", "PW="));
//        Collections.reverse(commands);
        return commands;
    }
}
