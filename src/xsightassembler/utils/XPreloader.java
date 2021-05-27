package xsightassembler.utils;

import javafx.application.Preloader;
import javafx.application.Preloader.StateChangeNotification.Type;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class XPreloader extends Preloader {
    private Stage preloaderStage;
    private Image process = new Image(getClass().getResourceAsStream("pre_logo.png"));
    private static final int SPLASH_WIDTH = 1000;
    private static final int SPLASH_HEIGHT = 397;

    @Override
    public void init() throws Exception {
        Thread.sleep(5000);
    }

    @Override
    public void start(Stage primaryStage) {
        this.preloaderStage = primaryStage;

        ImageView splash = new ImageView(process);
        Pane loading = new Pane();
        loading.getChildren().add(splash);

        Scene scene = new Scene(loading, SPLASH_WIDTH, SPLASH_HEIGHT);
        scene.setFill(Color.TRANSPARENT);
        loading.setMaxWidth(SPLASH_WIDTH);
        loading.setMaxHeight(SPLASH_HEIGHT);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop(){
        preloaderStage.hide();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification stateChangeNotification) {
        if (stateChangeNotification.getType() == Type.BEFORE_START) {
//            preloaderStage.hide();
            stop();
        }
    }
}
