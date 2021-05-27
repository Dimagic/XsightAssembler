package xsightassembler.utils;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.xml.bind.annotation.XmlRootElement;
import java.lang.reflect.Field;
import java.util.HashMap;

@XmlRootElement(name = "settings")
public class Settings {
    private String dbAddress;
    private String dbPort;
    private String dbName;
    private String dbUser;
    private String dbPass;
    private String mailServer;
    private String mailPort;
    private String mailUser;
    private String mailPass;
    private String printerCombo;
    private boolean enableIncAssembly;
    private boolean sslAuth;
    private String labCount;
    private String sshUser;
    private String sshPass;
    private String sftpFolder;
    private String logFolder;
    private String puttyFile;
    private String logCheckPeriod;
    private String startAnalyzeShift;
    private String namePostfix;
    private String templateArea;
    private String vlcFile;

    public Settings(){
    }

    public Settings(HashMap<String, String> args) {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            for(Field field: fields) {
                String val = args.get(field.getName());
                field.setAccessible(true);
                if (val != null && (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false"))){
                    field.set(this, Boolean.parseBoolean(val));
                } else {
                    field.set(this, val);
                }
            }
        } catch (SecurityException
                | IllegalArgumentException
                | IllegalAccessException e) {
            MsgBox.msgException(e);
        }
    }

    public String getDbAddress() {
        return dbAddress;
    }

    public void setDbAddress(String dbAddress) {
        this.dbAddress = dbAddress;
    }

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPass() {
        return dbPass;
    }

    public void setDbPass(String dbPass) {
        this.dbPass = dbPass;
    }

    public String getMailServer() {
        return mailServer;
    }

    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    public String getMailPort() {
        return mailPort;
    }

    public void setMailPort(String mailPort) {
        this.mailPort = mailPort;
    }

    public String getMailUser() {
        return mailUser;
    }

    public void setMailUser(String mailUser) {
        this.mailUser = mailUser;
    }

    public String getMailPass() {
        return mailPass;
    }

    public void setMailPass(String mailPass) {
        this.mailPass = mailPass;
    }

    public String getPrinterCombo() {
        return printerCombo;
    }

    public void setPrinterCombo(String printerCombo) {
        this.printerCombo = printerCombo;
    }

    public boolean isEnableIncAssembly() {
        return enableIncAssembly;
    }

    public void setEnableIncAssembly(boolean enableIncAssembly) {
        this.enableIncAssembly = enableIncAssembly;
    }

    public boolean isSslAuth() {
        return sslAuth;
    }

    public void setSslAuth(boolean sslAuth) {
        this.sslAuth = sslAuth;
    }

    public boolean validate(){
        return !dbAddress.isEmpty() && !dbPort.isEmpty() && !dbName.isEmpty() && !dbUser.isEmpty() && !dbPass.isEmpty();
    }

    public String getLabCount() {
        return labCount;
    }

    public Integer getLabCountInt() {
        try {
            return Integer.parseInt(getLabCount());
        } catch (Exception e) {
            return 0;
        }
    }

    public void setLabCount(String labCount) {
        this.labCount = labCount;
    }

    public String getSshUser() {
        return sshUser;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    public String getSshPass() {
        return sshPass;
    }

    public void setSshPass(String sshPass) {
        this.sshPass = sshPass;
    }

    public String getSftpFolder() {
        return sftpFolder;
    }

    public void setSftpFolder(String sftpFolder) {
        this.sftpFolder = sftpFolder;
    }

    public String getLogFolder() {
        return logFolder;
    }

    public void setLogFolder(String logFolder) {
        this.logFolder = logFolder;
    }

    public String getPuttyFile() {
        return puttyFile;
    }

    public void setPuttyFile(String puttyFile) {
        this.puttyFile = puttyFile;
    }

    public String getLogCheckPeriod() {
        return logCheckPeriod;
    }

    public Integer getLogCheckPeriodInt() {
        try {
            return Integer.parseInt(getLogCheckPeriod());
        } catch (Exception e) {
            return 0;
        }
    }

    public void setLogCheckPeriod(String logCheckPeriod) {
        this.logCheckPeriod = logCheckPeriod;
    }

    public String getStartAnalyzeShift() {
        return startAnalyzeShift;
    }

    public Integer getStartAnalyzeShiftInt() {
        try {
            return Integer.parseInt(getStartAnalyzeShift());
        } catch (Exception e) {
            return 0;
        }
    }

    public void setStartAnalyzeShift(String startAnalyzeShift) {
        this.startAnalyzeShift = startAnalyzeShift;
    }

    public String getNamePostfix() {
        return namePostfix;
    }

    public void setNamePostfix(String namePostfix) {
        this.namePostfix = namePostfix;
    }

    public String getTemplateArea() {
        return templateArea;
    }

    public void setTemplateArea(String templateArea) {
        this.templateArea = templateArea;
    }

    public String getVlcFile() {
        return vlcFile;
    }

    public void setVlcFile(String vlcFile) {
        this.vlcFile = vlcFile;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
