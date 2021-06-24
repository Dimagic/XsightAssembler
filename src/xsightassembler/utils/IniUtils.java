package xsightassembler.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Profile;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;


public class IniUtils {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private final Wini iniFile;
    private String sectionName;

    public IniUtils(String fileName, String sectionName) throws IOException {
        this.iniFile = new Wini(new File(fileName));
        this.sectionName = sectionName;
    }

    public IniUtils(String fileName) throws IOException {
        this.iniFile = new Wini(new File(fileName));
        this.sectionName = null;
    }

    public String getString(String key) {
        return iniFile.get(sectionName, key);
    }

    public int getInteger(String key) {
        return iniFile.get(sectionName, key, int.class);
    }

    public double getDouble(String key) {
        return iniFile.get(sectionName, key, double.class);
    }

    public Pattern getPattern(String key) {
        String p = iniFile.get(sectionName, key);
        if (p != null) {
            return Pattern.compile(p);
        }
        return null;
    }

    public HashMap<String, Pattern> getPatternMapByName (String pName) {
        Profile.Section section;
        HashMap<String, Pattern> map = new HashMap<>();
        for (String s: iniFile.keySet()){
            section = iniFile.get(s);
            String val = section.get(pName);
            if (val != null) {
                Pattern p = Pattern.compile(val);
                map.put(s, p);
            }
        }
        return map;
    }

    public HashMap<String, Pattern> getPatternMap() {
        HashMap<String, Pattern> map = new HashMap<>();
        Profile.Section section = iniFile.get(sectionName);
        for (String pString: section.keySet()) {
            Pattern p = Pattern.compile(section.get(pString));
            map.put(pString, p);
        }
        return map;
    }

    public HashMap<String, String> getStringMap() {
        HashMap<String, String> map = new HashMap<>();
        Profile.Section section = iniFile.get(sectionName);
        if (section == null) {
            MsgBox.msgWarning(String.format("%s: section %s not found", iniFile.getFile().getName(), sectionName));
        } else {
            for (String pString: section.keySet()) {
                map.put(pString, section.get(pString));
            }
        }
        return map;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }
}