
public class DirectClock {
    public int[] clock;
    int myId;

    public DirectClock(int numProc, int id) {
        myId = id;
        clock = new int[numProc];
        for (int i = 0; i < numProc; i++) clock[i] = 0;
        clock[myId] = 1;
    }

    public int getValue(int i) {
        return clock[i];
    }

    public void setValue(int index, int value) {
        clock[index] = value;
    }

    public void tick() {
        clock[myId]++;
    }

    public void sendAction() {
        // sentValue = clock[myId];
        tick();
    }

    public void receiveAction(int sender, int sentValue) {
        MsgHandler.debug("LAMPORT ACK RECEIVED from " + Integer.toString(sender) + " value:" + Integer.toString(sentValue));
        clock[sender] = Utils.max(clock[sender], sentValue);
        clock[myId] = Utils.max(clock[myId], sentValue) + 1;
        MsgHandler.debug("NEW CLOCK VALUE for me (" + myId + "): " + Integer.toString(clock[myId]) + ", " +
                "Them(" + sender + "):" + Integer.toString(clock[sender]));
    }
}
