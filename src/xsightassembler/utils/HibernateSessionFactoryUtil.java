package xsightassembler.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;
import xsightassembler.models.*;


import java.util.HashMap;

public class HibernateSessionFactoryUtil implements MsgBox {
    private static final Logger LOGGER = LogManager.getLogger(HibernateSessionFactoryUtil.class.getName());
    private static SessionFactory sessionFactory;

    private HibernateSessionFactoryUtil() {
    }

    public static SessionFactory getSessionFactory() throws CustomException {
        Settings settings = Utils.getSettings();
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration()
                        .addAnnotatedClass(CameraModule.class)
                        .addAnnotatedClass(BowlModule.class)
                        .addAnnotatedClass(AzimutModule.class)
                        .addAnnotatedClass(UpperSensorModule.class)
                        .addAnnotatedClass(RadarModule.class)
                        .addAnnotatedClass(FanModule.class)
                        .addAnnotatedClass(NoseModule.class)
                        .addAnnotatedClass(Isduh.class)
                        .addAnnotatedClass(User.class)
                        .addAnnotatedClass(History.class)
                        .addAnnotatedClass(MailAddress.class)
                        .addAnnotatedClass(Component.class)
                        .addAnnotatedClass(BiTest.class)
                        .addAnnotatedClass(BiNote.class)
                        .addAnnotatedClass(Pallet.class)
                        .setProperty("hibernate.connection.url", String.format("jdbc:postgresql://%s:%s/%s",
                                settings.getDbAddress(), settings.getDbPort(), settings.getDbName()))
                        .setProperty("hibernate.connection.username", settings.getDbUser())
                        .setProperty("hibernate.connection.password", settings.getDbPass())
                        .configure();

                StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
                sessionFactory = configuration.buildSessionFactory(builder.build());
                Statistics stats = sessionFactory.getStatistics();
                stats.setStatisticsEnabled(true);
            } catch (NullPointerException ignore) {
                return null;
            } catch (Exception e) {
                throw new CustomException(e.getCause().getCause().getMessage());
            }
        }
        return sessionFactory;
    }

    public static SessionFactory restartSessionFactory() throws CustomException {
        sessionFactory = null;
        return getSessionFactory();
    }

    public static HashMap<String, String> getConnectionInfo() throws CustomException {
        HashMap<String, String> connInfoMap = new HashMap<>();
        SessionFactory sessionFactory = getSessionFactory();
        if (sessionFactory == null){
            return null;
        }
        Session session = sessionFactory.openSession();
        ConnectionInfo connectionInfo = new ConnectionInfo();
        session.doWork(connectionInfo);
        connInfoMap.put("DataBaseProductName", connectionInfo.getDataBaseProductName());
        connInfoMap.put("DataBaseUrl", connectionInfo.getDataBaseUrl());
        connInfoMap.put("DriverName", connectionInfo.getDriverName());
        connInfoMap.put("Username", connectionInfo.getUsername());
        for (String c : connInfoMap.keySet()) {
            LOGGER.info(String.format("%s : %s", c, connInfoMap.get(c)));
        }
        session.close();
        return connInfoMap;
    }
}
