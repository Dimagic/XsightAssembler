package xsightassembler.utils;


import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import javafx.application.Platform;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.windows.Win32FullScreenStrategy;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class VideoPlayer {
    private BiTestWorker btw;
    private String netAddress;
    private String source;

    public VideoPlayer(String netAddress) {
        this.netAddress = netAddress;
        this.source = String.format("rtsp://%s:554/SDUVideoHighQuality", netAddress);
    }

    public VideoPlayer(BiTestWorker btw) {
        this.btw = btw;
        this.netAddress = btw.getBiNetName().getValue();
        this.source = String.format("rtsp://%s:554/SDUVideoHighQuality",netAddress);
    }

    public void getVideo() {
        EmbeddedMediaPlayer emp;
        String vlcFolder;
        try {
            Path p = Paths.get(Objects.requireNonNull(Utils.getSettings()).getVlcFile());
            vlcFolder = p.getParent().toString();
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcFolder);
            Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
        } catch (UnsatisfiedLinkError e) {
            MsgBox.msgWarning("Unable to load library 'libvlc'.\nInstall VLC player and try again.");
            return;
        } catch (NullPointerException e) {
            MsgBox.msgWarning("VLC path not found");
            return;
        }

        JFrame f = new JFrame();
        f.setTitle(String.format("Lab#%s stream: %s", btw.getLabNum(), netAddress));
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setVisible(true);
        f.setBounds(100,100,500,300);

//        f.setIconImage(SwingFXUtils.fromFXImage(favicon, null));
        Canvas c = new Canvas();
        c.setBackground(Color.black);

        JPanel p=new JPanel();
        p.setLayout(new BorderLayout());
        p.add(c);
        f.add(p);

        MediaPlayerFactory mpf = new MediaPlayerFactory();
        emp = mpf.newEmbeddedMediaPlayer(new Win32FullScreenStrategy(f));
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                emp.stop();
                emp.release();
                mpf.release();
            }
        });
        emp.setVideoSurface(mpf.newVideoSurface(c));
        emp.setEnableMouseInputHandling(false);
        emp.setEnableKeyInputHandling(false);
        String file=String.format("rtsp://%s:554/SDUVideoHighQuality", netAddress);
        if (emp.prepareMedia(file)){
            emp.addMediaPlayerEventListener(new MediaPlayerEventAdapter(){
                @Override
                public void subItemFinished(MediaPlayer p, int i){
                    emp.release();
                    mpf.release();
                }
            });
            Platform.runLater(emp::play);
        } else{
            emp.release();
            mpf.release();
        }
//        emp.prepareMedia(file);
//        emp.play();
    }

    public void saveToFile() {
        String vlcFolder;
        try {
            Path p = Paths.get(Objects.requireNonNull(Utils.getSettings()).getVlcFile());
            vlcFolder = p.getParent().toString();
        } catch (NullPointerException e) {
            MsgBox.msgWarning("VLC path not found");
            return;
        }
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcFolder);
        Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
        MediaPlayerFactory mpf = new MediaPlayerFactory();
        MediaPlayer mp = mpf.newEmbeddedMediaPlayer();
//        String options = ":sout=#transcode{vcodec=h264,venc=x264{cfr=16},scale=1,acodec=mp4a,ab=160,channels=2,samplerate=44100}:file{dst=test.mp4}";
//        String[] options = {"sout=#duplicate{dst=std{access=file,mux=raw,dst=output-file.ext}}"};
        String[] options = {":sout=#transcode{vcodec=h264,acodec=mpga,ab=128,channels=2,samplerate=44100}:file{dst=test.mp4,no-overwrite}"};
        mp.playMedia(source, options);

        try {
            Thread.sleep(5000); // <--- sleep for 20 seconds, you should use events - but this is just a demo
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mp.stop();
        mp.release();
        mpf.release();
        System.out.println("DONE");
    }
}