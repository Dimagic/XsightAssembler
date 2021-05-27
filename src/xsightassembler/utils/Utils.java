package xsightassembler.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import xsightassembler.models.LogItem;
import xsightassembler.services.*;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utils {
    private static Logger LOGGER = LogManager.getLogger(Utils.class.getName());
    private static CameraModuleService cameraModuleService = new CameraModuleService();
    private static BowlModuleService bowlModuleService = new BowlModuleService();
    private static HashMap<String, String> stringMap = new HashMap<>();

    public final static long ONE_MILLISECOND = 1;
    public final static long MILLISECONDS_IN_A_SECOND = 1000;

    public final static long ONE_SECOND = 1000;
    public final static long SECONDS_IN_A_MINUTE = 60;

    public final static long ONE_MINUTE = ONE_SECOND * 60;
    public final static long MINUTES_IN_AN_HOUR = 60;

    public final static long ONE_HOUR = ONE_MINUTE * 60;
    public final static long HOURS_IN_A_DAY = 24;
    public final static long ONE_DAY = ONE_HOUR * 24;
    public final static long DAYS_IN_A_YEAR = 365;

    public static Settings getSettings() {
        try {
            File file = new File("./settings");
            if (!file.exists()) {
                MsgBox.msgWarning("Load settings", "Settings file not found.\n" +
                        "Will create a new settings file.\nPlease fill settings form.");
                if (file.createNewFile()) {
                    return getSettings();
                }
            } else if (file.length() != 0) {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                String str = new String(data, StandardCharsets.UTF_8);
                return new ObjectMapper().readValue(decodeString(str), Settings.class);
            }
        } catch (IllegalArgumentException ignore) {
        } catch (Exception e) {
            LOGGER.error("Open settings file exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static HashMap<String, String> getModulesMap(String systemType) {
        HashMap<String, String> modulesMap = new HashMap<>();
        modulesMap.put("Azimut", Strings.manufAzimutModule);
        modulesMap.put("Bowl", Strings.manufBowlModule);
        modulesMap.put("Camera", Strings.manufCameraModule);
        modulesMap.put("Fan", Strings.manufFanModule);
        modulesMap.put("Nose", Strings.manufNoseModule);
        modulesMap.put("Radar", Strings.manufRadarModule);
        modulesMap.put("UpperSensor", Strings.manufUpperSensorModule);
        return modulesMap;
    }

    public static String stringToHash(String str) {
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashInBytes = md.digest(str.getBytes(StandardCharsets.UTF_8));

            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }

        } catch (NoSuchAlgorithmException e) {
            MsgBox.msgException(e);
        }
        return sb.toString();
    }

    public static String getFormattedDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dt.format(date);
    }

    public static String getFormattedTime(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat dt = new SimpleDateFormat("HH:mm:ss");
        return dt.format(date);
    }

    public static String getFormattedDateForFolder(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat dt = new SimpleDateFormat("yyyy.MM.dd");
        return dt.format(date);
    }

    public static String localDateToString(LocalDate date) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return date.format(fmt);
    }

    public static Date stringToDate(String formatter, String s) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(formatter);
            return df.parse(s);
        } catch (java.text.ParseException e) {
            MsgBox.msgWarning(String.format("Can't parse String: %s\nwith formatter: %s", s, formatter));
        }
        return null;
    }

    public static LocalDateTime dateToLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static String formatHMSM(Number n) {
        String res = "";
        if (n != null) {
            long duration = n.longValue();

            duration /= ONE_MILLISECOND;
            int milliseconds = (int) (duration % MILLISECONDS_IN_A_SECOND);
            duration /= ONE_SECOND;
            int seconds = (int) (duration % SECONDS_IN_A_MINUTE);
            duration /= SECONDS_IN_A_MINUTE;
            int minutes = (int) (duration % MINUTES_IN_AN_HOUR);
            duration /= MINUTES_IN_AN_HOUR;
            int hours = (int) (duration % HOURS_IN_A_DAY);
            duration /= HOURS_IN_A_DAY;
            int days = (int) (duration % DAYS_IN_A_YEAR);
            duration /= DAYS_IN_A_YEAR;
            int years = (int) (duration);

            if (days == 0) {
                res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                res = String.format("%d days %02d:%02d:%02d", days, hours, minutes, seconds);
            }
        }
        return res;
    }

    public static SimpleStringProperty stringToProperty(String s) {
        if (s == null) {
            return new SimpleStringProperty("");
        }
        return new SimpleStringProperty(s);
    }

    public static boolean fieldValidator(TextField field, Pattern p) {
        field.setText(field.getText().trim().toUpperCase());
        Matcher m = p.matcher(field.getText());
        boolean res = m.matches();
        if (res) {
            field.setStyle("-fx-background-color: white;");
            field.setTooltip(null);
        } else if (field.getText().isEmpty()) {
            field.setStyle("-fx-background-color: white;");
        } else {
            field.setStyle("-fx-background-color: yellow;");
            field.setTooltip(new Tooltip("Incorrect serial number"));
        }
        return res;
    }


    public static List<Node> getAllNodesInParent(Parent parent) {
        List<Node> ret = new ArrayList<>();
        for (Node child : parent.getChildrenUnmodifiable()) {
            ret.add(child);
            if (child instanceof Parent) {
                ret.addAll(getAllNodesInParent((Parent) child));
            }
        }
        return ret;
    }

    public static Node getNodeByRowColumnIndex(GridPane grid, final int row, final int column) {
        Node result = null;
        List<Node> ret = getAllNodesInParent(grid);
        for (Node node : ret) {
            try {
                if (grid.getRowIndex(node) == row && grid.getColumnIndex(node) == column) {
                    result = node;
                    break;
                }
            } catch (NullPointerException ignore) {

            }
        }
        return result;
    }

    public static String setFirstCharToUpper(String s) {
        return s.replaceFirst(s.substring(0, 1), s.substring(0, 1).toUpperCase());
    }

    public static ObservableList<Object> getListCameraModule() {
        try {
            return FXCollections.observableArrayList(cameraModuleService.findAll());
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static ObservableList<Object> getListBowlModule() {
        try {
            return FXCollections.observableArrayList(bowlModuleService.findAll());
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static ObservableList<Object> getListAzimutModule() {
        try {
            return FXCollections.observableArrayList(new AzimutModuleService().findAll());
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static ObservableList<Object> getListFanModule() {
        try {
            return FXCollections.observableArrayList(new FanModuleService().findAll());
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static ObservableList<Object> getListNoseModule() {
        try {
            return FXCollections.observableArrayList(new NoseModuleService().findAll());
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static ObservableList<Object> getListRadarModule() {
        try {
            return FXCollections.observableArrayList(new RadarModuleService().findAll());
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static ObservableList<Object> getListUpperSensorModule() {
        try {
            return FXCollections.observableArrayList(new UpperSensorModuleService().findAll());
        } catch (CustomException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static boolean saveModule(Class<?> typeModule, Object module) {
        try {
            Class<?> service = Class.forName("xsightassembler.services." + typeModule.getSimpleName() + "Service");
            Method method = service.getMethod("saveOrUpdate", Object.class);
            method.invoke(service.newInstance(), module);
            return true;
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgWarning(e.getCause().getLocalizedMessage());
        }
        return false;
    }

    public static Object findByInnerModuleSn(Class<?> typeModule, String sn) {
        try {
            for (String s : Strings.sheets) {
                Class<?> service = Class.forName("xsightassembler.services." + s + "Service");
                Method method = service.getMethod("findByInnerModuleSn", String.class);
                System.out.println(service);
                Object o = method.invoke(service.newInstance(), sn);
                if (o != null) {
                    return o;
                }
            }
//            Class<?> service = Class.forName("xsightassembler.services." + typeModule.getSimpleName() + "Service");
//            Method method = service.getMethod("findByInnerModuleSn", String.class);
//            Object o = method.invoke(service.newInstance(), sn);
//            return o;
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgWarning(e.getLocalizedMessage());
        }
        return null;
    }

    public static <K, V> K mapGetKeyByValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void initDatePicker(DatePicker dateFrom, DatePicker dateTo) {
        final String pattern = "yyyy-MM-dd";
        dateFrom.setShowWeekNumbers(true);
        dateFrom.setConverter(new StringConverter<LocalDate>() {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);

            {
                dateFrom.setPromptText(pattern.toLowerCase());
            }

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });

        dateTo.setShowWeekNumbers(true);
        dateTo.setConverter(new StringConverter<LocalDate>() {
            final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);

            {
                dateTo.setPromptText(pattern.toLowerCase());
            }

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date input = cal.getTime();
        LocalDate curDate = input.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate start = curDate.withDayOfMonth(1);
        LocalDate stop = curDate.withDayOfMonth(curDate.lengthOfMonth());
        dateFrom.setValue(start);
        dateTo.setValue(stop);
    }

    public static HashMap<String, Field> getStringsValuesHashMap() {
        HashMap<String, Field> stringsHash = new HashMap<>();
        for (Field field : Strings.class.getFields()) {
            stringsHash.put(field.getName(), field);
        }
        return stringsHash;
    }

    public static String getModuleSnInObject(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return (String) o.getClass().getMethod("getModule").invoke(o);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static JSONObject jsonToObject(String val) throws ParseException {
        try {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(val);
        } catch (ParseException e) {
            throw e;
        }
    }

    public static String getComputerName() {
        Map<String, String> env = getSystemEnvironment();
        if (env.containsKey("COMPUTERNAME"))
            return env.get("COMPUTERNAME");
        else return env.getOrDefault("HOSTNAME", "Unknown Computer");
    }

    private static Map<String, String> getSystemEnvironment() {
        return System.getenv();
    }

    public static String encodeString(String s) {
        try {
            DESKeySpec keySpec = new DESKeySpec("aBcD12345678".getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);
            sun.misc.BASE64Encoder base64encoder = new BASE64Encoder();
            byte[] cleartext = s.getBytes("UTF8");
            Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return base64encoder.encode(cipher.doFinal(cleartext));
        } catch (InvalidKeyException | UnsupportedEncodingException
                | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static String decodeString(String s) {
        try {
            DESKeySpec keySpec = new DESKeySpec("aBcD12345678".getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);
            sun.misc.BASE64Decoder base64decoder = new BASE64Decoder();
            byte[] encrypedPwdBytes = base64decoder.decodeBuffer(s);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plainTextPwdBytes = (cipher.doFinal(encrypedPwdBytes));
            return new String(plainTextPwdBytes);
        } catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException
                | BadPaddingException | IOException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        } catch (IllegalBlockSizeException e) {
            MsgBox.msgWarning("The settings file is damaged");
        }
        return null;
    }

    public static String getRandomString() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }

    public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    public static void uploadToSmb(String destinationPath, File localFile){
        localFile = new File("./tmp/logs.zip");
        String username = "smbuser";
        String password = "XSpc$1234";
        String user = username + ":" + password;
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);
//        byte[] BUFFER = new byte[10 * 8024];
//        ByteArrayInputStream inputStream = null;
//        SmbFileOutputStream sfos = null;
        try {
            SmbFile remoteFile =  new SmbFile("smb://172.16.28.18", auth);
            SmbFileOutputStream out = new SmbFileOutputStream(remoteFile);
            FileInputStream fis = new FileInputStream(localFile);
            out.write(IOUtils.toByteArray(fis));
            out.close();
//            String user = username + ":" + password;
//            byte[] data = FileUtils.readFileToByteArray(localFile);
//            inputStream = new ByteArrayInputStream(data);
//            String path = (destinationPath + localFile.getName());
//            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);
//            SmbFile remoteFile = new SmbFile(path, auth);
//            sfos = new SmbFileOutputStream(remoteFile);
//            int count = 0;
//            while ((count = inputStream.read(BUFFER)) > 0) {
//                sfos.write(BUFFER,0,count);
//            }
//            sfos.flush();
//            inputStream.close();
//            sfos.close();

        } catch (Exception e) {
            LOGGER.error("uploadToSmb", e);
            MsgBox.msgWarning(e.getLocalizedMessage());
        }
    }



    public static HashMap<String, Pattern> getPatternMap(String sectionName) {
        try {
            IniUtils iniUtils = new IniUtils("strings.ini", sectionName);
            return iniUtils.getPatternMap();
        } catch (IOException e) {
            LOGGER.error("getPatternMap", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static HashMap<String, String> getStringMap(String sectionName) {
        try {
            IniUtils iniUtils = new IniUtils("strings.ini", sectionName);
            return iniUtils.getStringMap();
        } catch (IOException e) {
            LOGGER.error("getStringMap", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static HashMap<String, Pattern> getPatternMapByName(String pName) {
        try {
            IniUtils iniUtils = new IniUtils("strings.ini", pName);
            return iniUtils.getPatternMapByName(pName);
        } catch (IOException e) {
            LOGGER.error("getPatternMapByName", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public static Thread getThreadByName(String name){
        for (Thread t: Thread.getAllStackTraces().keySet()){
            if (t.getName().equals(name)) return t;
        }
        return null;
    }

//    return
//    1 if online
//    0 if host is not reachable
//    -1 if UnknownHost
    public static int isSystemOnline(String ipName) {
        try {
            InetAddress inet = InetAddress.getByName(ipName);
            if (inet.isReachable(5000)) {
                return 1;
            }
        } catch (UnknownHostException e) {
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean sendCmd(String[] args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "866"));
            String line;
            while ((line = reader.readLine()) != null) {
                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                System.out.println(new String(bytes, StandardCharsets.UTF_8));
            }
            process.destroy();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static HashMap<String, List<String>> getStringMapFromFile(String fileName) {
        HashMap<String, List<String>> res = new HashMap<>();
        BufferedReader reader;
        Pattern pGroup = Pattern.compile("(?<=\\[)(.*?)(?=\\])");
        Matcher m;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String line = reader.readLine();
            String key = null;
            List<String> value = new ArrayList<>();
            while (line != null) {
                if (line.trim().isEmpty()){
                    line = reader.readLine();
                    continue;
                }
                m = pGroup.matcher(line);
                if (m.find()) {
                    if (key == null) {
                        key = m.group(1);
                    } else {
                        res.put(key, value);
                        key = m.group(1);
                        value = new ArrayList<>();
                    }
                } else {
                    value.add(line.trim());
                }
                // read next line
                line = reader.readLine();
            }
            if (value.size() > 0) {
                res.put(key, value);
            }
            reader.close();
        } catch (IOException e) {
            LOGGER.error("getStringMapFromFile", e);
            MsgBox.msgException(e);
        }
        return res;
    }

    public static FilteredList<LogItem> setIgnoreFlagInLogs(FilteredList<LogItem> logItems) {
        HashMap<String, List<String>> analyzerMap = Utils.getStringMapFromFile("./analyzer.ini");
        for (LogItem item : logItems) {
            boolean ifIgnore = analyzerMap.get("ignore").stream().anyMatch(s ->
                    Pattern.compile(String.format("(.*?)%s(.*)", s)).matcher(item.getFullMsg()).matches());
            boolean ifIbitIgnore = analyzerMap.get("ignore_if_ibit").stream().anyMatch(s ->
                    Pattern.compile(String.format("(.*?)%s(.*)", s)).matcher(item.getFullMsg()).matches()) && item.isIbit();
            if (ifIgnore || ifIbitIgnore || item.isIgnore()) {
                item.setIgnore(true);
            }
        }
        return logItems;
    }

    public static void setImgViewEvents(Stage stage, ImageView img) {
        img.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            stage.getScene().setCursor(Cursor.HAND);
            event.consume();
        });
        img.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            stage.getScene().setCursor(Cursor.DEFAULT);
            event.consume();
        });
    }
}
