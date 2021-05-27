package xsightassembler.utils;

import com.jcraft.jsch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.view.BiTestController;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class SshClient {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Session session;
    private ChannelShell channel;
    private final String username;
    private final String password;
    private final String hostname;
    private BiTestController btc;

    public SshClient(String hostname, String username, String password, BiTestController btc) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.btc = btc;
    }

    public Session getSession() {
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

    private Session connect(String hostname, String username, String password) {
        JSch jSch = new JSch();
        try {
            session = jSch.getSession(username, hostname, 22);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(password);

            btc.addToConsole("Connecting SSH to " + hostname + " - Please wait for few seconds... ");
            session.connect();
            btc.addToConsole("Connected!");
        } catch (Exception e) {
            if (e.getCause().toString().contains("UnknownHostException")){
                btc.addToConsole("Unknown host: " + hostname);
                return null;
            }else if (e.getCause().toString().contains("Connection timed out")){
                btc.addToConsole("Connection lost. Retry after 30 seconds.");
                try {
                    Thread.sleep(30000);
                    if (!btc.getShutdown()) {
                        return connect(hostname, username, password);
                    }
                } catch (InterruptedException ex) {
                    LOGGER.error("Thread sleep", ex);
                    MsgBox.msgException(ex);
                }
                return null;
            }
            LOGGER.error("Exception", e);
            MsgBox.msgError(e.getLocalizedMessage());
            return null;
        }

        return session;

    }

    public boolean executeCommands(List<String> commands) {
        try {
            Channel channel = getChannel();
            if (channel == null) {
                return false;
            }
            btc.addToConsole("Sending commands...");
            sendCommands(channel, commands);

            readChannelOutput(channel);
            btc.addToConsole("Finished sending commands!");
            return true;
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
        return false;
    }

    private void sendCommands(Channel channel, List<String> commands) {
        if (channel == null) {
            return;
        }
        try {
            PrintStream out = new PrintStream(channel.getOutputStream());

            out.println("#!/bin/bash");
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

    private void readChannelOutput(Channel channel) {
        if (channel == null) {
            return;
        }

        byte[] buffer = new byte[1024];

        try {
            InputStream in = channel.getInputStream();
            String line = "";
            while (!btc.getShutdown()) {
                while (in.available() > 0) {
                    System.out.println(in.available());
                    if (btc.getShutdown()) {
                        break;
                    }
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    line = new String(buffer, 0, i);
                    btc.addToConsole(line.trim());
                }

                if (line.contains("logout")) {
                    break;
                }

                if (channel.isClosed() || channel.isEOF()) {
                    break;
                }
                if (Utils.isSystemOnline(hostname) != 1) {
                    break;
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public String getFile(String remoteDir, String file) {
        try {
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
                if (filename.equals(file)){
                    String dest = "./tmp/" + Utils.stringToHash(Integer.toString(entry.hashCode()));
                    channelSftp.get(filename, dest);
                    return dest;
                }
            }
            MsgBox.msgInfo(String.format("File %s not found", file));
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
        btc.addToConsole("Disconnected channel and session");
    }
}
