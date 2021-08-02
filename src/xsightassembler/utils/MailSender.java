package xsightassembler.utils;

import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.models.MailAddress;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Properties;

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
        prop.put("mail.smtp.ssl.trust", settings.getMailServer());
        prop.put("mail.smtp.port", settings.getMailPort());
        prop.put("mail.smtp.starttls.enable", settings.isSslAuth());
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.socketFactory.port", settings.getMailPort());
        prop.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        prop.put("mail.smtp.socketFactory.fallback", "false");

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
