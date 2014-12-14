import java.io.*;
import java.util.*;
import javax.sound.midi.*;

/**
 *  Loads and plays a MIDI file.  Built with lots of help from
 *  http://stackoverflow.com/questions/6038917/how-to-play-a-midi-file-in-a-new-thread-in-java
 *
 *  @author Started by Ben Peter
 *  <br>Created way back on Wed Dec  3 06:09:46 PST 2014
 */
public class Player {

    Sequence sequence;  // the song data
    Sequencer sequencer;  // the thing that plays the song data

    public Player(String midifile) throws Exception {

        MidiDevice receivingDevice = getReceivingDevice();
        if (receivingDevice == null) {
            throw new Exception("No midi devices available.");
        }
        receivingDevice.open();

        File f = new File(midifile);

        sequence = MidiSystem.getSequence(f.toURI().toURL());
        //dumpInfo(sequence);

        sequencer = MidiSystem.getSequencer(false);
        Transmitter tx1 = sequencer.getTransmitter();
        Receiver rx1 = receivingDevice.getReceiver();
        tx1.setReceiver(rx1);

        sequencer.open();
        sequencer.setSequence(sequence);
/*
sequencer1.addMetaEventListener(new MetaEventListener() {
    public void meta(MetaMessage meta) {
        System.out.println("meta: " + meta);
    }
});

        sequencer.addControllerEventListener(new ControllerEventListener() {
            public void controlChange(ShortMessage event) {
                    System.out.println(event);
            }
        }, new int[] {144});
*/


        //sequencer1.setTrackSolo(0, true);
    }

    /** Tells the player/sequencer to start playing the sequence/song. */
    public void startPlayback() {
        sequencer.start();
    }

    /** Returns the Sequencer being used */
    public Sequencer getSequencer() {
        return sequencer;
    }

    /**
     * Figure out which device to use for sending/playing the
     * MIDI info.
     */
    private MidiDevice getReceivingDevice()
        throws MidiUnavailableException {

        // Currently biased towards "Microsoft GS Wavetable Synth",
        // but "gervill" would probably work too.
        String preferredDevice = "wavetable";
        preferredDevice = "gervill";
        
        for (MidiDevice.Info mdi: MidiSystem.getMidiDeviceInfo()) {
            MidiDevice dev = MidiSystem.getMidiDevice(mdi);
            if (dev.getMaxReceivers() != 0 && mdi.getName() != null) {
                if (BellHero.DEBUG) {
                    System.out.println("Found MIDI device: " + mdi);
                }
                String lcName = mdi.getName().toLowerCase();
                if (lcName.contains( preferredDevice)) {
                    return dev;
                }
            }
        }
        return null;
    }

    /** Prints info about the sequence to Standard Out */
    public void dumpInfo(Sequence seq) {

        Track[] tracks = seq.getTracks();
        System.out.println("# of tracks: " + tracks.length);
        Arrays.stream(tracks).forEach(t -> dumpTrack(t));
    }

    /** Prints info about the track to Standard Out */
    public void dumpTrack(Track track) {
        long ticks = track.ticks();
        System.out.println("Track ticks = " + ticks);
        long size = track.size();
        System.out.println("Track entries = " + size);
        for (int i=0; i < size; i++) {
            MidiEvent midiEvent = track.get(i);
            System.out.println(i +": " + dumpMidiEvent(midiEvent));
            
            //String parsedNote = parseMidiEvent(midiEvent);
            //if (parsedNote != null) {
            //    System.out.println(i +": " + parsedNote);
            //}

        }
    }

    /** Prints info about the MidiEvent to Standard Out */
    public String dumpMidiEvent(MidiEvent midiEvent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tick: " + midiEvent.getTick());
        MidiMessage msg = midiEvent.getMessage();
        int status = msg.getStatus();
        if (status < 0) status += 256;
        sb.append("   status: " + status);
        sb.append("   bytes: ");
        sb.append(Arrays.toString(msg.getMessage()));
        return sb.toString();
    }


    public String parseMidiEvent(MidiEvent midiEvent) {
        MidiMessage msg = midiEvent.getMessage();
        int status = msg.getStatus();
        if (status < 0) status += 256;

        
        if ( (status & 0b10010000) > 0) {
            StringBuilder sb = new StringBuilder();
            // it's a NOTE ON event:  1001xxxx
            sb.append("Tick: " + midiEvent.getTick());
            // the low-nibble contains the channel number.
            int channel = status & 0b1111;
            sb.append("  Chan: " + channel);
            sb.append("  Note: " + msg.getMessage()[1]);
            return sb.toString();
        }

        return null;
    }

    /**
     * Grabs all of the note-on events. This method looks at the
     * tracks contained within the song/sequence, and picks the
     * track which contains the most ticks.
     *
     * Returns a whole bunch of NoteOnEvent objects, generally
     * in the order of the tick in which they occur.
     */
    public List<NoteOnEvent> getNotes() {
        Track biggestTrack = findBiggestTrack();
        //dumpTrack(biggestTrack);
        return getNotes(biggestTrack);
    }

    /** 
     * Figures out which track is the "biggest" one.
     * Basically, a lame way of identifying which track to use
     * for the on-screen display of notes.
     */
    public Track findBiggestTrack() {
        Track[] tracks = sequence.getTracks();
        Track biggestTrack = tracks[0];
        for (Track aTrack : tracks) {
            if (aTrack.size() > biggestTrack.size()) {
                biggestTrack = aTrack;
            }
        }
        return biggestTrack;
    }

    /**
     * Returns the Note-On events for the given track.
     */
    private List<NoteOnEvent> getNotes(Track track) {
        List<NoteOnEvent> list = new ArrayList<>();

        int size = track.size();
        // Wish I could Stream the MidiEvents from a Track ...
        for (int i=0; i < size; i++) {
            MidiEvent midiEvent = track.get(i);
            NoteOnEvent noe = parseNoteOn(midiEvent);
            if (noe != null) {
                list.add(noe);
            }
        }

        return list;
    }

    /**
     * Returns a NoteOnEvent object if thte specified
     * MidiEvent contains a Note-On event.
     * Otherwise, returns null.
     */
    private NoteOnEvent parseNoteOn(MidiEvent midiEvent) {
        MidiMessage msg = midiEvent.getMessage();
        int status = msg.getStatus();
        if (status < 0) status += 256;
        
        int statusNibble = status >> 4;
        if ( statusNibble == 0b1001 ) {
            StringBuilder sb = new StringBuilder();
            // it's a NOTE ON event:  1001xxxx
            long tick = midiEvent.getTick();
            // the low-nibble contains the channel number.
            int channel = status & 0b1111;
            int pitch = msg.getMessage()[1];
            return new NoteOnEvent(tick, channel, pitch);
        }

        return null;
    }

    /**
     * Searches the MIDI stream to find a Tempo event and 
     * returns its value.
     * Returns -1 if it's not found.
     */
    public int findTempoEvent() {
        Track track = findBiggestTrack();
        int size = track.size();

        for (int i=0; i < size; i++) {
            MidiEvent midiEvent = track.get(i);
            if (midiEvent.getTick() != 0) {
                return -1;
            }
            MidiMessage message = midiEvent.getMessage();
            byte[] buf = message.getMessage();
            if (buf.length > 3 &&
                buf[0] == (byte) 0xff &&
                buf[1] == (byte) 0x51 &&
                buf[2] == (byte) 0x03) {
                // looks like a tempo event!
                int tempo = parseTempo(midiEvent);
                return tempo;
            }
        }

        return -1;
    }

    /**
     * Attempts to parse the tempo from a MidiEvent,
     * using the MIDI file format info here:
     * http://cs.fit.edu/~ryan/cse4051/projects/midi/midi.html
     *
     */
    public int parseTempo(MidiEvent ev) {
        MidiMessage msg = ev.getMessage();
        int status = msg.getStatus();
        if (status < 0) status += 256;
        if (status != 255) {
            throw new RuntimeException("Status was " + status +", expected 0xff (255d)");
        }

        byte[] message = msg.getMessage();
        if (message.length < 4) {
            throw new RuntimeException("Message length was type " + message.length +", expected 4 or more bytes. ");
        }

        if (message[1] != 81 ||
            message[2] != 3 ) {
            throw new RuntimeException("message type was " + message[1] +" + " + message[2] + ", expected 0x51 (81d) + 0x03");
        }

        if (BellHero.DEBUG) {
            System.out.println("Parsing tempo message: " + Arrays.toString(message));
        }
        int tempo = 0;
        for (int i=3; i < message.length; i++) {
            int lame = message[i];
            if (lame < 0) lame += 256;
            tempo = tempo << 8 | lame;
        }

        if (BellHero.DEBUG) {
            System.out.println("Looks like the tempo is " + tempo);
        }
        return tempo;
    }
}


