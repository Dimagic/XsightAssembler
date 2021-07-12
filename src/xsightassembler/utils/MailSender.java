package xsightassembler.utils;

import java.io.*;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.Date;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;

import javax.mail.internet.*;

import com.sun.mail.smtp.*;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.models.MailAddress;

public class MailSender {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());

    private Session session;
    private final Properties prop;
    private final String username;
    private final String password;
    private final Settings settings;

    public MailSender(Settings settings) {
        this.settings = settings;
        this.prop = new Properties();
        prop.put("mail.smtp.host", settings.getMailServer());
        prop.put("mail.smtp.port", settings.getMailPort());
        prop.put("mail.smtp.auth", settings.isSslAuth());
        prop.put("mail.smtp.socketFactory.port", settings.getMailPort());
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        username = settings.getMailUser();
        password = settings.getMailPass();

    }

    public void sendFile(String subject, String textMsg, ObservableList<MailAddress> addressList, File f) {
        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
        session.setDebug(true);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(prop.getProperty("mail.smtp.host")));
            message.setRecipients( Message.RecipientType.TO, listToArray(addressList));
            message.setSubject(subject);
            message.setText(textMsg);
            message.setHeader("X-Mailer", "Tov Are's program");

            // attach file
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            Multipart multipart = new MimeMultipart();
            String file = f.getAbsolutePath();
            String fileName = f.getName();
            DataSource source = new FileDataSource(file);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(fileName);
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);

            Transport.send(message);

            MsgBox.msgInfo("Message successful");
        } catch (MessagingException e) {
            LOGGER.error("Exception", e);
            MsgBox.msgError(e.getMessage());
        }
    }

    private InternetAddress[] listToArray(ObservableList<MailAddress> mailAddresses) {
        InternetAddress newAddress;
        InternetAddress[] res = new InternetAddress[mailAddresses.size()];
        for (int i = 0; i < mailAddresses.size(); i++) {
            newAddress = new InternetAddress();
            newAddress.setAddress(mailAddresses.get(i).getEmail());
            res[i] = newAddress;
        }
        return res;
    }
}
