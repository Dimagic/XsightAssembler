package xsightassembler.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class Strings {
    static Logger LOGGER = LogManager.getLogger(Strings.class);
    public static String appName = "Xsight-assembler";
    public static String VERSION = "0.0.4.3";
    public static String appNameWithVer = String.format("%s v%s %s", appName, VERSION, Utils.getComputerName());

    public static String manufISDUHModule = "XSTXT0010000500";
    public static String manufAzimutModule = "XSTXT0020000213";
    public static String manufBowlModule = "XSTXT0030000500";
    public static String manufCameraModule = "XSTXT0020000394";
    public static String manufFanModule = "XSTXT8020000268";
    public static String manufNoseModule = "XSTXT0020000393";
    public static String manufRadarModule = "XSTXT0070000029";
    public static String manufUpperSensorModule = "XSTXT0020000500";

    public static String manufMcu = "XSTXT1012000098";
    public static String manufCamera = "XSTXT1018000013";
    public static String manufCameraHouse = "XSTXT0020000373";
    public static String manufCooler = "XSTXT1018000039";
    public static String manufBoard = "XSTXT1012000060";
    public static String manufTop = "XSTXT1012000034";
    public static String manufCarrier = "XSTXT1012000171";
    public static String manufBreakable = "XSTXT2011001010";
    public static String manufComEx = "XSTXT0030000096";


    public static String input = "Input";
    public static String incorrInput = "Incorrect input";

    public static String password = "Password";
    public static String inpPassw = "Input password:";
    public static String firstEntrance = "First entrance.\nYou need to change your password.";

    public static String exception = "Exception";
    public static String exceptTrace = "The exception stacktrace was:";

    public static String saveComplete = "Save module complete";
    public static String updateComplete = "Update module complete";

    public static String regexFileName = "strings.ini";

    public static Pattern pModuleGetter = Pattern.compile("^(get)[\\w]+(Module)$");
    public static Pattern pEmail = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static Pattern pLogDateTime = Pattern.compile("(\\d{4}/\\d{2}/\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d{3})");
    public static Pattern pLogSource = Pattern.compile("^([a-zA-Z]+(:\\s))");
    public static Pattern pLogErrType = Pattern.compile("([\\w\\s]+(failure|error))", Pattern.CASE_INSENSITIVE);

    public static String[] sheets = {"AzimutModule", "BowlModule", "CameraModule", "FanModule", "NoseModule", "RadarModule", "UpperSensorModule"};
    public static String[] isduhColumns = {"No", "Date", "ISDUH", "Azimut", "Bowl", "Camera", "Fan", "Nose",
            "Radar", "UpperSensor", "User", "History"};
    public static String[] azimutColumns = {"No", "Date", "Module", "Top", "Board", "User", "History"};
    public static String[] bowlColumns = {"No", "Date", "Module", "ComEx", "Breakable", "Carrier", "User", "History"};
    public static String[] cameraColumns = {"No", "Date", "Module", "CameraHouse", "Camera", "Mcu", "User", "History"};
    public static String[] fanColumns = {"No", "Date", "Module", "User", "History"};
    public static String[] noseColumns = {"No", "Date", "Module", "User", "History"};
    public static String[] radarColumns = {"No", "Date", "Module", "User", "History"};
    public static String[] upperSensorColumns = {"No", "Date", "Module", "Cooler", "User", "History"};

    public static String[] assemblyStatuses = {"Incomplete", "Done"};
    public static String[] statusesForAssemblyJournal = {"All", "Done", "Incomplete"};

    public static HashMap<Integer, String> getPassFailMap(){
        HashMap<Integer, String> passFailMap = new HashMap<>();
        passFailMap.put(0, "-");
        passFailMap.put(1, "Pass");
        passFailMap.put(-1, "Fail");
        return passFailMap;
    }

    public static String uptimeCmd = "echo $(uptime) | sed 's/^.\\+up\\ \\+\\([^,]*\\).*/\\1/g'";

    public static HashMap<Integer, String> getUserRoleMap() {
        HashMap<Integer, String> userRoleMap = new HashMap<>();
        userRoleMap.put(0, "User");
        userRoleMap.put(1, "Administrator");
        userRoleMap.put(2, "Technician");
        userRoleMap.put(3, "User + send report");
        return userRoleMap;
    }

    public static HashMap<Integer, String> getIsduhCommandsMap() {
        HashMap<Integer, String> commands = new HashMap<>();
        commands.put(1, "Open PuTTY");
        commands.put(2, "Video stream (VLC)");
        commands.put(3, "Video stream (custom)");
        return commands;
    }

    public static List<String> getStages() {
        return Arrays.asList("1", "2");
    }

//    public static Pattern getPatternFromFile(String key) {
//        try {
//            Wini ini = new Wini(new File("strings.ini"));
//            String regex = ini.get("regex", key);
//            if (regex == null) {
//                MsgBox.msgWarning(String.format("Regular expression %s not found", key));
//                return null;
//            }
//            return Pattern.compile(regex);
//        } catch (Exception e) {
//            LOGGER.error("Exception", e);
//            MsgBox.msgException(e);
//        }
//        return null;
//
//    }
}
