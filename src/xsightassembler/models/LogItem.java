package xsightassembler.models;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

public class LogItem {
    private Date date;
    private String source;
    private String errType;
    private String message;
    private String fullMsg;
    private int lineNum;
    private boolean isIbit;
    private boolean isIgnore;

    public LogItem(String errType) {
        this.errType = errType;
    }

    public LogItem(int lineNum, Date date, String source, String message, String fullMsg, boolean isIbit) {
        this.lineNum = lineNum;
        this.date = date;
        this.source = source;
        this.errType = null;
        this.message = message;
        this.fullMsg = fullMsg;
        this.isIbit = isIbit;
    }

    public LogItem(int lineNum, Date date, String source, String errType, String message, String fullMsg, boolean isIbit) {
        this.lineNum = lineNum;
        this.date = date;
        this.source = source;
        this.errType = errType;
        this.message = message;
        this.fullMsg = fullMsg;
        this.isIbit = isIbit;
    }

    public int getLineNum() {
        return lineNum;
    }

    public Date getDate() {
        return date;
    }

    public String getSource() {
        return source;
    }

    public String getErrType() {
        return errType;
    }

    public String getMessage() {
        return message;
    }

    public String getFullMsg() {
        return fullMsg;
    }

    public boolean isIbit() {
        return isIbit;
    }

    public boolean isIgnore() {
        return isIgnore;
    }

    public void setIgnore(boolean ignore) {
        isIgnore = ignore;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
