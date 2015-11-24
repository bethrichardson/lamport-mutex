import java.net.*; import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TCPListenerThread extends ListenerThread {
    List<Thread> threadList = new ArrayList<>();
    ServerSocket tcpListener;
    SocketAddress socketAddress;

    /**
     * Listener thread listens for TCP client connections
     *
     * @param server The backend server to perform requests against
     * @param port   The port to listen upon
     */
    public TCPListenerThread(Server server, int port) throws IOException {
        super(server, port);
        tcpListener = new ServerSocket();
        socketAddress = new InetSocketAddress(InetAddress.getByName(server.server.getHostName()), port);
        tcpListener.bind(socketAddress);
    }

    @Override
    public void interrupt(){
        Iterator<Thread> iterator = threadList.iterator();
        while (iterator.hasNext()) {

            iterator.next().interrupt();
        }
        try {
            tcpListener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.interrupt();
    }

    /**
     * Listener thread listens for either UDP or TCP client connections
     *
     * @param server The backend server to perform requests against
     * @param tcpPort The TCP port to listen upon
     * @param udpPort The UDP port to listen upon
     * @param tcpThread Is this a TCP listener thread
     */
    /**
     * Listen for incoming TCP requests and create a ServerThread to handle
     * any incoming requests to backend. Pass off the socket for each new request to
     * a new ServerThread to read the request and send response.
     */
    @Override
    public void createSocketAndThread() {
        try {
            Socket s;
            while ( (s = tcpListener.accept()) != null) {
                numThreads++;
                Thread t = new ServerThread(server, s, threadList.size() + 1);
                threadList.add(t);
//                System.out.println("Creating TCP thread: " + Integer.toString(threadList.size()));
                t.start();
            }
            createSocketAndThread();

        } catch (IOException e) {
//            System.err.println("Server aborted in socket thread" +  Integer.toString(numThreads) + " for port:" + Integer.toString(port) + e);
        }
    }
}


