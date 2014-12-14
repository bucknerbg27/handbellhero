
import java.net.URL;

import javafx.application.Application;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

/**
 * Handy example found at 
 * http://www.java2s.com/Code/Java/JavaFX/Playmp3file.htm
 */
public class PlayMp3 extends Application {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Please specify an MP3 file on the command line.");
            return;
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String filename = getParameters().getRaw().get(0);
        final URL resource = getClass().getResource(filename);
        final Media media = new Media(resource.toString());
        final MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.play();

        primaryStage.setTitle("Audio Player 1");
        primaryStage.setWidth(200);
        primaryStage.setHeight(200);
        primaryStage.show();
    }
}

