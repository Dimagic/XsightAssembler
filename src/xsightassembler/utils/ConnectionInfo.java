package xsightassembler.utils;

import org.hibernate.jdbc.Work;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionInfo implements Work {

    private String dataBaseUrl;
    private String dataBaseProductName;
    private String driverName;
    private String username;

    @Override
    public void execute(Connection connection) throws SQLException {
        dataBaseUrl = connection.getMetaData().getURL();
        dataBaseProductName = connection.getMetaData().getDatabaseProductName();
        driverName = connection.getMetaData().getDriverName();
        username = connection.getMetaData().getUserName();

    }

    public String getDataBaseProductName() {
        return dataBaseProductName;
    }

    public void setDataBaseProductName(String dataBaseProductName) {
        this.dataBaseProductName = dataBaseProductName;
    }

    public String getDataBaseUrl() {
        return dataBaseUrl;
    }

    public void setDataBaseUrl(String dataBaseUrl) {
        this.dataBaseUrl = dataBaseUrl;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
