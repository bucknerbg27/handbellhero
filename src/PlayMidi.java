import java.io.*;
import java.util.*;
import javax.sound.midi.*;

/**
 *
 * http://stackoverflow.com/questions/6038917/how-to-play-a-midi-file-in-a-new-thread-in-java
 *
 */
public class PlayMidi {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Gimme a MIDI file to play");
            return;
        }

        MidiDevice receivingDevice = getReceivingDevice();
        if (receivingDevice == null) {
            System.err.println("No midi devices available.");
            return;
        }
        receivingDevice.open();

        File f = new File(args[0]);

        Sequence sequence1 = MidiSystem.getSequence(f.toURI().toURL());
        dumpInfo(sequence1);
        
        Sequencer sequencer1 = MidiSystem.getSequencer(false);
        Transmitter tx1 = sequencer1.getTransmitter();
        Receiver rx1 = receivingDevice.getReceiver();
        tx1.setReceiver(rx1);

        sequencer1.open();
        sequencer1.setSequence(sequence1);
System.out.println("bpm = " + sequencer1.getTempoInBPM());
System.out.println("MPQ = " + sequencer1.getTempoInMPQ());
/*
sequencer1.addMetaEventListener(new MetaEventListener() {
    public void meta(MetaMessage meta) {
        System.out.println("meta: " + meta);
    }
});

sequencer1. 	addControllerEventListener(new ControllerEventListener() {
    public void controlChange(ShortMessage event) {
            System.out.println(event);
    }
}, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 127});
*/


        //sequencer1.setTrackSolo(0, true);
        sequencer1.start();
    }


    private static MidiDevice getReceivingDevice()
        throws MidiUnavailableException {

        for (MidiDevice.Info mdi: MidiSystem.getMidiDeviceInfo()) {
            MidiDevice dev = MidiSystem.getMidiDevice(mdi);
            if (dev.getMaxReceivers() != 0 && mdi.getName() != null) {
                System.out.println(mdi);
                String lcName = mdi.getName().toLowerCase();
                if (lcName.contains( "wavetable")) {
                    return dev;
                }
            }
        }
        return null;
    }


    public static void dumpInfo(Sequence seq) {

        Track[] tracks = seq.getTracks();
        System.out.println("# of tracks: " + tracks.length);
        Arrays.stream(tracks).forEach(t -> dumpTrack(t));
    }

    public static void dumpTrack(Track track) {
        long ticks = track.ticks();
        System.out.println("ticks = " + ticks);
        long size = track.size();
        System.out.println("size = " + size);
        for (int i=0; i < size; i++) {
            MidiEvent midiEvent = track.get(i);
            //System.out.println(i +": " + dumpMidiEvent(midiEvent));
            
            String parsedNote = parseMidiEvent(midiEvent);
            if (parsedNote != null) {
                System.out.println(i +": " + parsedNote);
            }

        }
    }

    public static String dumpMidiEvent(MidiEvent midiEvent) {
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


    public static String parseMidiEvent(MidiEvent midiEvent) {
        MidiMessage msg = midiEvent.getMessage();
        int status = msg.getStatus();
        if (status < 0) status += 256;

        
        if ( (status & 0b10010000) > 0) {
            StringBuilder sb = new StringBuilder();
            // it's a NOTE ON event:  1001xxxx
            sb.append("Tick: " + midiEvent.getTick());
            int channel = status & 0b1111;
            sb.append("  Chan: " + channel);
            sb.append("  Note: " + msg.getMessage()[1]);
            return sb.toString();
        }

        return null;
    }

}

