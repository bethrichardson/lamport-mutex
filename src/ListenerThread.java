public abstract class ListenerThread extends Thread {
    Server server;
    int port;
    int numThreads = 0;


    /**
     * Listener thread listens for client connections
     *
     * @param server The backend server to perform requests against
     * @param port The port to listen upon
     */
    public ListenerThread(Server server, int port) {
        this.server = server;
        this.port = port;
    }

    /**
     * Listen for incoming requests and create a thread to handle
     * any incoming requests to backend.
     */
    public abstract void createSocketAndThread();

    public void run() {
        createSocketAndThread();
    }
}


