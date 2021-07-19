package xsightassembler.utils;

import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.view.BiTestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import static com.sun.xml.fastinfoset.stax.events.XMLConstants.ENCODING;

public class SshClient {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Session session;
    private Channel channel;
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

    private Channel getChannel(String channelType) {
        if (channel != null && channel.isConnected()) {
            return channel;
        }

        try {
            return getSession().openChannel(channelType);
        } catch (JSchException e) {
            e.printStackTrace();
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

            writeBtcConsole("Connecting SSH to " + hostname + " - Please wait for few seconds... ");
            session.connect(3000);
            writeBtcConsole("Connected!");
        } catch (Exception e) {
            if (e.getCause().toString().contains("UnknownHostException")){
                writeBtcConsole("Unknown host: " + hostname);
                return null;
            }else if (e.getCause().toString().contains("Connection timed out")){
                writeBtcConsole("Connection lost. Retry after 30 seconds.");
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

    public boolean executeCommands(List<String> commands) throws CustomException {
        try {
            channel = getChannel("shell");
            if (channel == null) {
                return false;
            }
            channel.connect();
            writeBtcConsole("Sending commands...");
            sendCommands(channel, commands);
            readChannelOutput(channel);
            writeBtcConsole("Finished sending commands!");
            return true;
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }

    private void sendCommands(Channel channel, List<String> commands) {
        if (channel == null) {
            return;
        }
        try {
            PrintStream out = new PrintStream(channel.getOutputStream());

            out.println("#!/bin/bash");
            for (String command : commands) {
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
                    if (btc.getShutdown()) {
                        break;
                    }
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    line = new String(buffer, 0, i);
                    writeBtcConsole(line.trim());
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
                Thread.sleep(500);
            }
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

    public String execSingleCommand(String command) throws IOException, JSchException {
        ChannelExec channelExec = (ChannelExec) getSession().openChannel("exec");
        InputStream in = channelExec.getInputStream();
        channelExec.setCommand(command);
        channelExec.setErrStream(System.err);
        channelExec.connect();
        String result = IOUtils.toString(in, ENCODING);
        channelExec.disconnect();
        return result;
    }

    public String getComExSn() {
        try {
            return execSingleCommand("cat /sys/class/dmi/id/board_serial").trim();
        } catch (IOException | JSchException e) {
            e.printStackTrace();
            MsgBox.msgError(e.getLocalizedMessage());
        }
        return null;
    }

    public String getFlashMemorySn() {
        try {
            String name = "ID_SERIAL_SHORT";
            String tmp = execSingleCommand(String.format("udevadm info --query=all --name=/dev/sda | grep %s", name));
            return tmp.substring(tmp.indexOf(name) + name.length() + 1).trim().toUpperCase();
        } catch (IOException | JSchException e) {
            MsgBox.msgError(e.getLocalizedMessage());
        }
        return null;
    }

    public String getMacAddress() {
        try {
            return execSingleCommand("cat /sys/class/net/eth0/address").trim().toUpperCase();
        } catch (IOException | JSchException e) {
            MsgBox.msgError(e.getLocalizedMessage());
        }
        return null;
    }

    public String getPDUEep() {
        try {
            return execSingleCommand("/opt/Xsight/Fodetect/app/bin/PDUEep -p").trim();
        } catch (IOException | JSchException e) {
            MsgBox.msgError(e.getLocalizedMessage());
        }
        return null;
    }

    public void setISduFlag(int flag) {
        try {
            execSingleCommand(String.format("/opt/Xsight/Fodetect/app/bin/PDUEep -w ISduFlag -d %s", flag));
        } catch (IOException | JSchException e) {
            MsgBox.msgError(e.getLocalizedMessage());
        }
    }

    public String getMcuMonitorStatus() {
//        try {
//            return execSingleCommand("/opt/Xsight/Fodetect/app/bin/MCUMonitor /dev/ttyUSB0 Status").trim();
//        } catch (IOException | JSchException e) {
//            MsgBox.msgError(e.getLocalizedMessage());
//        }
//        return null;
        return "===> Decoded RX message | Type: MPU, ID: Status, Cnt: 4, Ack: 1 | Responce time: 0 ms \n" +
                "Discrete: Radar on\tCamera on\tLaser off\tNIR off\n" +
                "\t  Pump off\tFan on\t\tMCU Rst off\tRDRHUB.Rst off\n" +
                "\t  Acc.Tst off\tMAN. O/R off\tEye Safety on\n" +
                "\n" +
                "Temperature:\tLocal 35c\tRemote -64c\tRunway -70.000000\n" +
                "\n" +
                "PWM:\t\tHeater 000%\tWiper 000%\tDoor 000%\tIR Filt. 000%\n" +
                "\n" +
                "Input Voltage: 11.956[Volt]\n" +
                "Current: Wiper  : avg   0[mA]      max   0[mA]      cutoff 299[mA]\n" +
                "\t Door   : avg   2[mA]      max 213[mA]      cutoff 149[mA]\n" +
                "\t IR Filt: avg   5[mA]      max 118[mA]      cutoff  79[mA]\n" +
                "\t NIR    : avg   0[mA]      max   0[mA]      cutoff 4500[mA]\n" +
                "\n" +
                "Status: Wiper: unmounted\tWiper plus pump Error: 0 \n" +
                "\tDoor:  open \n" +
                "\tIRF:   mounted - out \n" +
                "\tUART2 Mux Channel: 0\n" +
                "\n" +
                "BIT results: 0x0";
    }

    public void downloadLogFiles(String remoteDir, String destDir) {
        try {
            channel = getChannel("sftp");
            if (channel == null) {
                return;
            }
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp) channel;
            channelSftp.cd(remoteDir);
            Vector filelist = channelSftp.ls(remoteDir);
            List<ChannelSftp.LsEntry> tmp = new ArrayList<>();
            for (Object o : filelist) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) o;
                if (entry.getFilename().matches("^(messages)+($|.[\\d]$)")) {
                    tmp.add(entry);
                    channelSftp.get(entry.getFilename(), destDir);
                }
            }
            String msg = tmp.size() > 0 ? String.format("Downloading %s files complete", tmp.size()):
                    "Files for downloading not found";
            MsgBox.msgInfo(msg);
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            MsgBox.msgException(e);
        }
    }

    public void downloadFiles(String remoteDir, List<String> files, String destDir) {
        try {
            channel = getChannel("sftp");
            if (channel == null) {
                return;
            }
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

    private void writeBtcConsole(String s) {
        if (btc != null) {
            btc.addToConsole(s);
        }
    }

    public void close() {
        channel.disconnect();
        session.disconnect();
        writeBtcConsole("Disconnected channel and session");
    }
}
