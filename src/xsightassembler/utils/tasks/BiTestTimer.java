package xsightassembler.utils.tasks;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.utils.Utils;
import xsightassembler.view.BiTestController;

import java.util.Date;

public class BiTestTimer extends Task<Void> {
    Logger LOGGER = LogManager.getLogger(this.getClass().getName());
    private Date startTime;
    private BiTestController btc;

    public BiTestTimer(Date startTime, BiTestController btc) {
        this.startTime = startTime;
        this.btc = btc;
    }

    @Override
    protected Void call() throws InterruptedException {
        while (!btc.getShutdown()) {
            if (startTime == null) {
                this.updateMessage("Test not started");
                break;
            } else {
                this.updateMessage(Utils.formatHMSM(new Date().getTime() - startTime.getTime()));
            }
            Thread.sleep(1000);
        }
        return null;
    }
}
