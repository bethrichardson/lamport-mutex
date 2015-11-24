import java.net.*; import java.io.*; import java.util.*;
public class ServerThread extends Thread {
    Server server;
    Socket theClient;
    int threadNumber;

    /**
     * Thread to access backend and respond for TCP requests to server
     * @param server Server to which to route requests
     * @param s TCP socket upon which to respond
     * @param threadNumber Count for testing/logging multi-threading
     */
    public ServerThread(Server server, Socket s, int threadNumber) {
        this.server = server;
        theClient = s;
        this.threadNumber = threadNumber;
    }

    /**
     * Read in the request from the TCP socket.
     * Access the backend with requestString and then respond on same socket
     * with responseString from server.
     */
    public void run() {
        try {
            MsgHandler.debug("Starting execution for thread " + Integer.toString(threadNumber) + " on server " +
                    "" + Integer.toString(server.serverIndex));
            Scanner sc = new Scanner(theClient.getInputStream());
            PrintWriter pout = new PrintWriter(theClient.getOutputStream());
            String command = sc.nextLine();
            String responseString = server.msg.routeMessage(command).toString();
            pout.print(responseString);

            pout.flush();
            theClient.close();
            MsgHandler.debug("Completing execution for thread " + Integer.toString(threadNumber) + " on server " +
                    "" + Integer.toString(server.serverIndex));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}


