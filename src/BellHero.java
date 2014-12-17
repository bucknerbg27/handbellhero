import java.io.*;
import java.util.*;
import javafx.animation.*;
import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.*;
import javax.sound.midi.*;
import static java.lang.Math.random;

import org.apache.commons.io.FilenameUtils;


/**
 *  It's like Guitar Hero, but using Handbells.
 *
 *  @author Started by Ben Peter
 *  <br>Created way back on Wed Dec  3 05:23:41 PST 2014
 */
public class BellHero extends Application {

    public static final boolean DEBUG = 
        "true".equalsIgnoreCase(System.getProperty("debug"));


    /**
     * The screen size to use. Eventually it would be nice to make this
     * dynamic, based on the current size of the window.
     */
    public static final int WIDTH = 1450, HEIGHT = 700, 
           SLIDER_TOP = HEIGHT/10 + 40,
           SLIDER_BOTTOM = HEIGHT - 80;

    /**
     * The colors to use on-screen, which match the colors used by
     * the set of handbells.
     * http://www.grothmusic.com/p-41-kidsplay-8-note-diatonic-handbell-set.aspx
     */
    Color[] COLORS = new Color[] {
        Color.web("red", 0.8),          // Low C
        Color.web("orange", 0.8),       // D
        Color.web("yellow", 0.8),       // E
        Color.web("green", 0.8),        // F
        Color.web("cyan", 0.8),         // G
        Color.web("blue", 0.8),         // A
        Color.web("indigo", 0.8),       // B
        Color.web("red", 0.8)           // Upper C
    };

    /** The MIDI pitches used by each of the handbells. */
    public static int[] NOTE_PITCHES = new int[] {
        0, // Low C
        2, // D
        4, // E
        5, // F
        7, // G
        9, // A
        11, // B
        12  // C
    };

    /** Note labels, for debugging */
    private static final String[] NOTE_LABELS = new String[] {
        "Low C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B", "High C" };

    
    /** The "sliders" to display on-screen. */
    private Slider[] sliders = new Slider[8];

    /** The amount of horizontal screen space to use for each slider. */
    private int sliderWidth = WIDTH / (sliders.length +1);

    /** The timer that updates all of the notes on the sliders. */
    private Timer timer = new Timer("The Timer Thing", true);

    /** The MIDI Player thing, which has the song data, and plays it */
    private Player player;

    /** Gets this party started. */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Gimme a midi file to use.");
            System.exit(0);
        }
        launch(args);
    }

    /** All of the notes that we'll be displaying. */
    private List<NoteOnEvent> noteList;

    /** The position of display within the list of notes. */
    private int noteOffset;

    /** The last tick that was processed by our timer. */
    private float lastTickProcessed;

    /** The last tick played by the player/sequencer */
    private long lastTickPlayed;
    
    /** The number of ticks to process every time the refresh timer loops. */
    private float ticksPerRefresh;

    /** The number of refresh loops per second. */
    private int refreshLoopsPerSecond = 20;

    /** The number of loops until we start the playback. */
    private int loopsUntilPlaybackStarts;

    /** The "ready" label */
    private Shape readyLabel;

    /** The background lines that are moving around */
    private List<Line> backgroundLines = new ArrayList<>();

    /** The main stage for this app */
    private Stage primaryStage;

    /**
     * Stupid hack to properly seed the tempo for certain MIDI files.
     * I couldn't seem to get the math worked out on the tempos
     * needed to synchronize the notes with the music player.
     */
    private static Properties stupidHackTempo = new Properties();
    static {
        File f = new File(MenuScreen.RESOURCES_DIR, "stupidHackTempo.props");
        if (f.exists()) {
            try (FileReader reader = new FileReader(f)) {
                stupidHackTempo.load(reader);
            } catch (Exception ex) {
                System.err.println("Unable to load " + f.getAbsolutePath());
                ex.printStackTrace();
            }
        }
    }

    /** Puts the pieces together */
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            loadAndPlay(getParameters().getRaw().get(0), primaryStage);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public void loadAndPlay(String filename, Stage primaryStage) 
        throws Exception {
        this.primaryStage = primaryStage;
        player = new Player(filename);

        noteList = player.getNotes();
        if (DEBUG) {
            System.out.println("Parsed Notes");
            noteList.stream().forEach(e -> System.out.println(e));
        }

        Group root = new Group();
        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);
        scene.setCursor(Cursor.NONE);
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.UNDECORATED);

        Image hollyImage = new Image(MenuScreen.findResourceOrFail(
                    "handbell-hero-holly-01.png"));
        ImageView hollyView = new ImageView(hollyImage);
        hollyView.setFitWidth(WIDTH-100);
        hollyView.setPreserveRatio(true);
        hollyView.setSmooth(true);
        hollyView.setX(50);
        hollyView.setY(0);
        root.getChildren().add(hollyView);
            
        
        Text t = new Text((WIDTH/2)-250, 350, "Ready");
        t.setFont(new Font("Nightmare Hero", 300));
        t.setStroke(Color.ORANGE);
        t.setStrokeWidth(8);
        //t.setEffect(new Shadow(BlurType.GAUSSIAN, Color.ORANGE, 2));
        readyLabel = t;
        //readyLabel.getStyleClass().add("ready-text");
        root.getChildren().add(readyLabel);
        
        
        for (int i=0; i < sliders.length; i++) {
            sliders[i] = new Slider(i);
            root.getChildren().add(sliders[i]);
        }

        primaryStage.show();

        // How many times whould we refresh the note's position on-screen
        loopsUntilPlaybackStarts = refreshLoopsPerSecond;

        float tempoMPQ = player.getSequencer().getTempoInMPQ();
        float tempoBPM = player.getSequencer().getTempoInBPM();

        float beatsPerSecond = tempoBPM/60;

        // The following is crappy incorrect math that just happened to 
        // work consistently on a few MIDI files I found.
        float loopMilliseconds = 1000.0f / refreshLoopsPerSecond;
        float ticksPerSecond = tempoMPQ/1000; 
        // TOTAL BS HERE - it just happens to make things work better with
        //   the set of MIDI files that we have
        ticksPerSecond = 1700;

        ticksPerRefresh = ticksPerSecond / refreshLoopsPerSecond;

        // Since I'm arrarently clueless at calculating the timing correctly,
        // here's a convenient way to correct the problem for specific files
        String basename = FilenameUtils.getBaseName(filename);
        String override = stupidHackTempo.getProperty(basename);
        if (override != null) {
            if (DEBUG) {
                System.out.println("Using ticks-per-SECOND override of " +
                        override);
            }
            ticksPerSecond = Integer.parseInt(override);
            ticksPerRefresh = ticksPerSecond / refreshLoopsPerSecond;
        }
        startTimer( (int) loopMilliseconds); 

        List<MidiEvent> tempoEventList = player.findTempoEvents();
        if (DEBUG) {
            System.out.println("tempoMPQ = " + tempoMPQ);
            System.out.println("tempoBPM = " + tempoBPM);
            System.out.println("loopMilliseconds  = " + loopMilliseconds);
            System.out.println("ticksPerRefresh = " + ticksPerRefresh);

            if (!tempoEventList.isEmpty()) {
                tempoEventList.stream().forEach(ev -> player.parseTempo(ev));
            }
        }
        
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                player.getSequencer().stop();
                timer.cancel();
            }
        });        
        
        buildBackgroundLines();
    }

    /** Builds the background lines */
    private void buildBackgroundLines() {
/*        
        Polygon top = new Polygon(new Double[] {
            0, 0,
            0, 200,
            200, 100
            400, 300
            
        for (int i=0; i < 6; i++) {
            Line line = new Line();
            line.

            line = new Line(xOffset, SLIDER_TOP, xOffset, 
                    SLIDER_BOTTOM - dropCircleRadiusY);
            line.setStrokeWidth(8);
            line.setStrokeLineCap(StrokeLineCap.ROUND);
            /*
            RadialGradient gradient = new RadialGradient(
                    0, .1, 10, 10, 10, false, CycleMethod.REPEAT, 
                    new Stop(0, Color.web("white", 0.5).darker()),
                    new Stop(1, Color.web("white", 0.5).brighter()) );
                    * /
            line.setStroke(Color.web("white", 0.8));
            Shadow lineglow = new Shadow(BlurType.GAUSSIAN, Color.WHITE, 10);
            line.setEffect(lineglow);
                
*/
    }

    /**
     * Invoked when the Frame is closed, and just kill the JVM to end the
     * MIDI player
     */
    public void stop() {
        System.exit(0);
    }
    
    /** Starts the timer (which updates the notes on the sliders) 
     * @param repeatDelay the number of milliseconds to wait between
     * updates.
     **/
    private void startTimer(int repeatDelay) {
        TimerTask timerTask = new TimerTask() {
            public void run() {

                if ( (lastTickPlayed > 0) &&
                     !player.getSequencer().isRunning() ) {
                    // the player has finished with the song
                    timer.cancel();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                    Platform.runLater(new Runnable() {
                        public void run() {
                            primaryStage.close();
                        }
                    });
                }

                loopsUntilPlaybackStarts --;
                if (loopsUntilPlaybackStarts == 0) {
                    player.startPlayback();
                }
                updateBackground();
                if (loopsUntilPlaybackStarts <= refreshLoopsPerSecond) {
                    updateSliders();
                }
            }
        };
        
        timer.scheduleAtFixedRate(timerTask, 2000, repeatDelay);
    }

    /**
     * Updates the background lines/bars
     */
    public void updateBackground() {

        if (readyLabel.isVisible()) {
            Platform.runLater(new Runnable() {
                public void run() {
                    readyLabel.setVisible(false);
                }
            });
        }
    }

    /** 
     * The task that is invoked by the timer, which updates
     * the notes on the sliders. 
     **/
    private void updateSliders() {
        long currentTickPlayer = player.getSequencer().getTickPosition();
        if (DEBUG) {
            System.out.println("PLAYER TICK = " + currentTickPlayer);
        }

//        if ( (currentTickPlayer == lastTickPlayed) &&
//                (currentTickPlayer > 0) ) {
            // the player has finished with the song
//            timer.cancel();
//            Platform.runLater(new Runnable() {
//                public void run() {
//                    primaryStage.close();
//                }
//            });
//        }

        
        // there's two ways that we calculate the future/next tick. 
        // If the player is not running (currentTickPlayer == 0), then
        //    we use math. This works at the start, but eventually the
        //    player "drifts" ahead of the math, and becomes noticable.
        // If the player is running, then calculate how many ticks the
        //    player has played since the last refresh loop, and use
        //    that quantity looking forward.

        float nextTick;
        if (currentTickPlayer == 0) {
            // the player hasn't started yet, so use math to figure out
            //    hoy many ticks ahead to process
            nextTick = (long) (lastTickProcessed + ticksPerRefresh);
        } else {
            long delta = currentTickPlayer - lastTickPlayed;
            nextTick = lastTickProcessed + delta;
            lastTickPlayed = currentTickPlayer;
        }
        if (DEBUG) {
            System.out.println("Ticks " + lastTickProcessed + " to " + nextTick);
        }
        lastTickProcessed = nextTick;

        // build a list of notes that we need to play
        int endOffset = noteList.size();
        ListIterator<NoteOnEvent> iterator = noteList.listIterator(noteOffset);
        while (iterator.hasNext()) {
            NoteOnEvent noe = iterator.next();
            // figure out where the entry BEYOND the next tick is in the list.
            if (noe.tick > nextTick) {
                endOffset = iterator.previousIndex();
                break;
            }
        }

        List<NoteOnEvent> subset = noteList.subList(noteOffset, endOffset);
        // Now, condense the list into the notes
        final boolean[] newNotes = new boolean[13];
        for (NoteOnEvent noe : subset) {
            int pitch = noe.pitch;
            int noteNumber = pitch % 12;
            if (noteNumber == 0) {
                // we need a low C and a high C
                if (pitch > 60) {
                    // treat it as a "high" c
                    noteNumber = 12;
                }
            }
            newNotes[noteNumber] = true;
        }

        if (DEBUG) {
            System.out.println("Track entries " + noteOffset +" to " + endOffset);
            System.out.println(dumpNotes(newNotes));
        }
        noteOffset = endOffset;

        Platform.runLater(new Runnable() {
            public void run() {
                for (Slider slider : sliders) {
                    boolean addNote = newNotes[slider.getPitchIndex()];
                    slider.tick(addNote);
                }
            }
        });

    }

    /**
     * Returns a line of text with the labes for the notes in the array.
     */
    private String dumpNotes(boolean[] newNotes) {
        StringBuilder sb = new StringBuilder();
        String pad = "             ";
        for (int i=0; i < newNotes.length; i++) {
            if (newNotes[i]) {
                String label = NOTE_LABELS[i];
                sb.append(label);
                sb.append(pad.substring(0, 6-label.length()));
            } else {
                sb.append(pad.substring(0, 6));
            }
            sb.append("  ");
        }
        return sb.toString();
    }


    /** 
     * Contains the "sliders" that show the notes to be played.
     */
    class Slider extends Group {
        final Color DEFAULT_FILL_COLOR = Color.web("#aaa", 0.8);
        int sliderIndex;
        int pitchIndex;

        Line line;
        int dropCircleRadiusY = 30;
        Ellipse ellipse;
        Color color;
        boolean isLitUp = true;
        Shadow colorGlow;

        List<Ellipse> slidingNotes;

        /**
         * Creates the initial view.
         */
        Slider(int sliderIndex) {
            this.sliderIndex = sliderIndex;
            this.pitchIndex = NOTE_PITCHES[sliderIndex];
            this.color = COLORS[sliderIndex];
            colorGlow = new Shadow(BlurType.GAUSSIAN, color, 5);

            int xOffset = (sliderIndex * sliderWidth) + sliderWidth;
            slidingNotes = new ArrayList<>();

            line = new Line(xOffset, SLIDER_TOP, xOffset, 
                    SLIDER_BOTTOM - dropCircleRadiusY);
            line.setStrokeWidth(8);
            line.setStrokeLineCap(StrokeLineCap.ROUND);
            /*
            RadialGradient gradient = new RadialGradient(
                    0, .1, 10, 10, 10, false, CycleMethod.REPEAT, 
                    new Stop(0, Color.web("white", 0.5).darker()),
                    new Stop(1, Color.web("white", 0.5).brighter()) );
                    */
            line.setStroke(Color.web("white", 0.8));
            Shadow lineglow = new Shadow(BlurType.GAUSSIAN, Color.WHITE, 10);
            line.setEffect(lineglow);
            //line.setStroke(Color.web("white", 0.5));
            getChildren().add(line);

            ellipse = new Ellipse();
            ellipse.setCenterX(xOffset);
            ellipse.setCenterY(SLIDER_BOTTOM);
            ellipse.setRadiusX(sliderWidth/2.2);
            ellipse.setRadiusY(dropCircleRadiusY);
            ellipse.setStrokeWidth(13);
            ellipse.setEffect(colorGlow);
            lightUp(false);

            getChildren().add(ellipse);
        }

        /**
         * Decides whether the "drop zone" ellipse at the bottom
         * should get brighter. 
         **/
        private void lightUp(boolean makeYourDayBrighter) {
            if (makeYourDayBrighter == isLitUp) {
                // already there
                return;
            }
            isLitUp = makeYourDayBrighter;

            if (makeYourDayBrighter) {
                ellipse.setFill(Color.web("white"));
                ellipse.setStroke(color.brighter());
                ellipse.setStrokeType(StrokeType.OUTSIDE);
            } else {
                ellipse.setFill(DEFAULT_FILL_COLOR);
                ellipse.setStroke(color);
                ellipse.setStrokeType(StrokeType.INSIDE);
            }

        }

        /**
         * Returns the pitch that this note represents, which will
         * be a number between 0 and 11 (the chromatic 12 notes).
         */
        public int getPitchIndex() {
            return pitchIndex;
        }

        /**
         * Increments any/all of the notes/circles present
         *
         * This must be performed on the FX thread!
         * 
         * @param addNote if true, a new note is added to this slider.
         */
        public void tick(boolean addNote) {

            boolean lightUp = false;

            double sliderIncrement = (line.getEndY() - line.getStartY()) / 
                refreshLoopsPerSecond;
            for (Ellipse disc : new ArrayList<Ellipse>(slidingNotes)) {
                double newY = disc.getCenterY() + sliderIncrement;
                if (newY > (line.getEndY() ) ) {
                    // close enough to the bottom.
                    getChildren().remove(disc);
                    slidingNotes.remove(disc);
                    lightUp = true;

                } else {
                    disc.setCenterY(newY);
                }
            }

            if (addNote) {
                Ellipse disc = new Ellipse();
                disc.setCenterX(line.getStartX());
                disc.setCenterY(line.getStartY()+50);
                disc.setRadiusX(ellipse.getRadiusX() * 0.7);
                disc.setRadiusY(ellipse.getRadiusY() * 0.7);
                disc.setStrokeWidth(7);
                disc.setStroke(color);
                disc.setFill(color.darker());
                disc.setEffect(colorGlow);
                getChildren().add(disc);
                slidingNotes.add(disc);
            }
           
            lightUp(lightUp);
        }

    }
}


