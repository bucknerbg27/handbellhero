/** Keeps track of the relevant info for a note-on event. */
class NoteOnEvent {
    long tick;
    int channel;
    int pitch;
    NoteOnEvent(long tick, int channel, int pitch) {
        this.tick = tick;
        this.channel = channel;
        this.pitch = pitch;
    }

    public String toString() {
        return "Tick: " + tick +
            ", channel: " + channel +
            ", pitch: " + pitch;
    }
}



