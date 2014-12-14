import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javafx.animation.*;
import javafx.application.*;
import javafx.collections.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.*;

/**
 *  Displays the splash/menu screen.
 *  @author Started by Ben Peter
 *  <br>Created way back on Fri Dec  5 07:22:37 PST 2014
 */
public class MenuScreen extends Application {

    /** The directory of resources needed by this app. */
    public static final String RESOURCES_DIR_NAME = "resources";

    /** The directory of resources needed by this app. */
    public static final File RESOURCES_DIR = new File(RESOURCES_DIR_NAME);


    /** The awesome splash screen image */
    private static final String SPLASH_IMAGE = "handbell-hero-01.png";

    /** Where to find MIDI files */
    private static final String MIDI_DIR = "midi";

    /** The sound effect file to play when selecting stuff. */
    private static final String SELECT_SOUND_EFFECT = "Hit_Metal_Hollow.mp3";

    /** The CSS file to use */
    
    /** Plays the "thunk" sound when something is selected. */
    private MediaPlayer itemSelectedAudioEffectPlayer;

    public static void main(String[] args) {
        launch(args);
    }

    public static String findResourceOrFail(String filename) {

        File f = new File(RESOURCES_DIR, filename);
        if (!f.canRead()) {
            System.err.println("Unable to find/use the resource file "
                    + f.getAbsolutePath());
            System.exit(1);
        }

        try {
            String uriString = f.toURI().toURL().toExternalForm();
            return uriString;
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            System.exit(2);
            return null; // gotta help the compiler
        }
    }

    @Override
    public void start(Stage primaryStage) {
try {
        Media media = new Media(findResourceOrFail(SELECT_SOUND_EFFECT));
        itemSelectedAudioEffectPlayer = new MediaPlayer(media);

        primaryStage.setTitle("Handbell Hero!");
        primaryStage.setFullScreen(true);


        Label headText = new Label("HelpSystems Development Presents");
        headText.getStyleClass().add("splash-text");


        Image img = new Image(findResourceOrFail(SPLASH_IMAGE));
        ImageView imgView = new ImageView(img);
        imgView.setFitWidth(600);
        imgView.setPreserveRatio(true);
        imgView.setSmooth(true);
        

        Label listLabel = new Label("Select a song!");
        listLabel.getStyleClass().add("song-list-header");

        ListView<String> list = new ListView<>();
        list.getStyleClass().add("song-list");
        File dir = new File(MIDI_DIR);
        String[] fileArray = dir.list();
        //fileArray = (String[]) Arrays.stream(fileArray).map(s -> s.split(Pattern.quote("."))[0]).toArray();
        for (int i=0; i < fileArray.length; i++) {
            fileArray[i] = fileArray[i].split(Pattern.quote("."))[0];
        }
        list.getItems().addAll(Arrays.asList(fileArray));

        list.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<String>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends String> event) {
                
                playItemSelectedSound();
            }
        });

        list.setOnKeyPressed(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent event) {
                if (KeyCode.ENTER.equals(event.getCode())) {
                    launchPlayer(list.getSelectionModel().getSelectedItems().get(0));
                }
            }
        });
 

        VBox vbox = new VBox();
        vbox.getStyleClass().add("song-container");
        VBox.setVgrow(list, Priority.ALWAYS);
        vbox.getChildren().addAll(listLabel, list);

        
        BorderPane pane = new BorderPane();
        pane.setTop(headText);
        BorderPane.setAlignment(headText, Pos.CENTER);
        pane.setLeft(imgView);
        BorderPane.setMargin(vbox, new Insets(0,50,50,50));
        pane.setCenter(vbox);

        Scene scene = new Scene(pane);
        scene.setCursor(Cursor.NONE);
        scene.getStylesheets().add(findResourceOrFail("bellhero.css"));
        primaryStage.setScene(scene);
        primaryStage.show();
        list.requestFocus();
    } catch (Throwable t) {
        t.printStackTrace();
        System.exit(0);
    }
    }

    /**
     * Stops the player if necessary, and then restarts playback from the
     * beginning of the sample.
     */
    private void playItemSelectedSound() {
        itemSelectedAudioEffectPlayer.stop();
        itemSelectedAudioEffectPlayer.play();
    }

    private void launchPlayer(String filename) {
        BellHero app = new BellHero();
        try {
            app.loadAndPlay(MIDI_DIR +'/' + filename +".mid", new Stage());
        } catch (Exception ex) {
            displayError("Unable to load/play the song file", ex);
        }
    }

    public static void displayError(String what, Throwable t) {
        /*
        Dialogs.create().title("Error")
            .message(what)
            .style(DialogStyle.NATIVE)
            .showExceptionInNewWindow(t);
        */
        System.err.println(what);
        t.printStackTrace();
    }

    public void stop() {
        System.exit(0);
    }
    
}

