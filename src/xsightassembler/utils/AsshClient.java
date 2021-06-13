package xsightassembler.utils;

import com.jcraft.jsch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

abstract class AsshClient {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Session session;
    private ChannelShell channel;
    private final String username;
    private final String password;
    private final String hostname;
    private final String tmpDir = "./tmp/";

    public AsshClient(String hostname, String username, String password) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
    }

    public Session getSession() throws CustomException {
        if (session == null || !session.isConnected()) {
            session = connect(hostname, username, password);
        }
        return session;
    }

    private Channel getChannel() {
        if (channel == null || !channel.isConnected()) {
            try {
                channel = (ChannelShell) getSession().openChannel("shell");
                channel.connect();
                return channel;
            } catch (NullPointerException ignored) {
            } catch (Exception e) {
                MsgBox.msgError("Error while opening channel: " + e);
            }
        }
        return null;
    }

    private Session connect(String hostname, String username, String password) throws CustomException {
        JSch jSch = new JSch();
        try {
            session = jSch.getSession(username, hostname, 22);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(password);
            session.connect();
        } catch (Exception e) {
            if (e.getCause().toString().contains("UnknownHostException")){
                throw new CustomException("Unknown host: " + hostname);
            }else if (e.getCause().toString().contains("Connection timed out")){
                throw new CustomException("Connection timed out");
            }
            LOGGER.error("Exception", e);
            MsgBox.msgError(e.getLocalizedMessage());
            return null;
        }
        return session;

    }

    public Channel executeCommands(List<String> commands) {
        try {
            Channel channel = getChannel();
            if (channel == null) {
                return null;
            }
            sendCommands(channel, commands);
            return channel;
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    private void sendCommands(Channel channel, List<String> commands) {
        if (channel == null) {
            return;
        }
        try {
            PrintStream out = new PrintStream(channel.getOutputStream());
            for (String command : commands) {
                System.out.println(command);
                out.println(command);
                Thread.sleep(2000);
            }
            out.println("exit");
            out.flush();
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public String getFile(String remoteDir, String file) {
        try {
            File tmp = new File(tmpDir);
            if (!tmp.exists() || !tmp.isDirectory()) {
                tmp.mkdir();
            }
            session = getSession();
            Channel channel = getChannel();
            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp) channel;
            channelSftp.cd(remoteDir);
            Vector filelist = channelSftp.ls(remoteDir);
            for (Object o : filelist) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) o;
                String filename = entry.getFilename();
                if (filename.equals(file)) {
                    String dest = tmpDir + Utils.stringToHash(Integer.toString(entry.hashCode()));
                    channelSftp.get(filename, dest);
                    return dest;
                }
            }
//            MsgBox.msgInfo(String.format("File %s not found", file));
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return null;
    }

    public void downloadFiles(String remoteDir, List<String> files, String destDir) {
        try {
            Channel channel = getChannel();
            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp) channel;
            channelSftp.cd(remoteDir);
            Vector filelist = channelSftp.ls(remoteDir);
            int counter = 0;
            for (Object o : filelist) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) o;
                String filename = entry.getFilename();
                if (files.contains(filename)) {
                    channelSftp.get(filename, destDir);
                    counter++;
                }
            }
            String msg;
            if (counter > 0) {
                msg = String.format("Downloading %s files complete", counter);
            } else {
                msg = "Files for downloading not found";
            }
            MsgBox.msgInfo(msg);
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void uploadFile(String file, String destDir) {
        try {
            Channel channel = (ChannelSftp) getSession().openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp) channel;
            channelSftp.cd(destDir);
            channelSftp.put(file, destDir);

        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void close() {
        channel.disconnect();
        session.disconnect();
    }
}
