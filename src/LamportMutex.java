import java.util.ArrayList;

public class LamportMutex  {
    public DirectClock v;
    public int[] q; // request queue
    public int n; //numServers
    public int myId;
    MsgHandler initComm;
    Server server;
    public LamportMutex(MsgHandler initComm, Server server) {
        this.initComm = initComm;
        this.server = server;
        n = initComm.numServers;
        myId = initComm.serverIndex;
        v = new DirectClock(n, myId);
        q = new int[n];
        for (int j = 0; j < n; j++)
            q[j] = Integer.MAX_VALUE; // infinity
    }
    public synchronized void requestCS() {
        v.tick();
        q[myId] = v.getValue(myId);
        MsgHandler.debug("Current value: " + q[myId]);
        MsgHandler.debug("Broadcasting change.");
        initComm.broadCastChange("request," + q[myId] + ',' + myId, v);
        server.makeAllSynchedUpdates();
        while (!okayCS())
            waitForCriticalSection();
    }
    public synchronized void waitForCriticalSection(){
        MsgHandler.debug("Waiting in critical Section: " + myId);
        Utils.myWait(this);
        server.makeAllSynchedUpdates();
    }

    public void interpretMessage(String request){
        ArrayList<String> requestList =  Utils.interpretStringAsList(request);
        handleMsg(Integer.parseInt(requestList.get(1)), Integer.parseInt(requestList.get(2)), requestList.get(0));
    }

    public synchronized void releaseCriticalSection() {
        q[myId] =  Integer.MAX_VALUE; // infinity
        initComm.broadCastChange("release," + v.getValue(myId) + ',' + myId, v);
    }
    public Boolean okayCS() {
        for (int j = 0; j < n; j++){
            if (isGreater(q[myId], myId, q[j], j)) {
                MsgHandler.debug("They have a message ahead of me:" + q[j] + ". Mine is " + q[myId]);
                return false;
            }
            if (isGreater(q[myId], myId, v.getValue(j), j)) {
                MsgHandler.debug("I haven't heard back from them: " + "" +
                        "Me:" + Integer.toString(myId) + ":" + Integer.toString(q[myId]) +
                                "Him:" + Integer.toString(j) + ":" + Integer.toString(myId)
                );
                return false;
            }
        }
        MsgHandler.debug("OKAY CS:" + myId);
        return true;
    }
    boolean isGreater(int entry1, int pid1, int entry2, int pid2) {
        return ((entry1 > entry2)
                || ((entry1 == entry2) && (pid1 > pid2)));
    }
    public synchronized void handleMsg(int timeStamp, int src, String tag) {
        v.receiveAction(src, timeStamp);
        if (tag.equals("request")) {
            q[src] = timeStamp;
            initComm.sendMsg("ack," + v.getValue(myId) + ',' + myId, src);
            v.sendAction();
            MsgHandler.debug("LAMPORT ACK SENT TO: " + src);
        } else if (tag.equals("release")) {
            q[src] = Integer.MAX_VALUE;
            MsgHandler.debug("LAMPORT RELEASE RECEIVED from: " + src + ". Setting q[src]=" + q[src]);
        }
        else{
            MsgHandler.debug("LAMPORT ACK RECEIVED from: " + src + ". New v[j]=" + v.getValue(src));
        }
    }
}