import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MsgHandler {
    int numServers, serverIndex;
    List<InetSocketAddress> serverList;
    Server server;

    public MsgHandler(Server server, int numServers, int serverIndex, List<InetSocketAddress> serverList){
        this.server = server;
        this.numServers = numServers;
        this.serverIndex = serverIndex;
        this.serverList = serverList;
    }

    public InetSocketAddress getServer(int serverIndex){
        return serverList.get(serverIndex);
    }

    public void broadCastChange(String request, DirectClock v) {
        MsgHandler.debug("Broadcasting: " + request);
        for (int i = 0; i < numServers; i++) {
            if (i != serverIndex) {
                sendMsg(request, i);
                v.sendAction();
            }
        }
    }

    public static void debug(String log){
        if (Utils.debugger){
            System.out.println("DEBUG: " + log);
        }
    }

    public void sendMsg(String request, int serverId){
        InetSocketAddress server = getServer(serverId);
        try {
            makeServerRequest(server, serverId, request, false);
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    /**
     * Make a TCP request using a new socket
     *
     * @param server The destination of the request
     * @param request The request string formatted for the server
     * @param expectResponse Does the requester need to wait for a response?
     * @return the response from the server
     * @throws IOException
     */
    public String makeServerRequest(InetSocketAddress server, int serverId, String request, boolean expectResponse) throws IOException {
        Socket socket;
        Scanner din;
        PrintStream pout;
        String retValue = null;

        try {
            socket = new Socket();
            socket.connect(server, 100);
            socket.setReuseAddress(true);
        } catch (SocketTimeoutException | ConnectException e) {
            this.server.lmp.v.setValue(serverId, Integer.MAX_VALUE);
            return null;
        }
            din = new Scanner(socket.getInputStream());

            pout = new PrintStream(socket.getOutputStream());
            pout.println(request);
            pout.flush();

            if (expectResponse) retValue = din.nextLine();

            socket.close();

            pout.close();

        return retValue;
    }

    public ArrayList<String> routeMessage(String request){
        ArrayList<String> requestList =  Utils.interpretStringAsList(request);
        ArrayList<String> response = new ArrayList<>();
        String method = requestList.get(0);

        MsgHandler.debug("Accessing server with request: " + request);

        switch(method){
            case("externalSync"):
                response = server.supplyExternalSync();
                break;
            case("externalPut"):
                response = server.queueExternalRequest(request);
                break;
            case("externalDelete"):
                response = server.queueExternalRequest(request);
                break;
            case("request"):
                response.add("done");
                server.lmp.interpretMessage(request);
                break;
            case("release"):
                response.add("done");
                server.lmp.interpretMessage(request);
                break;
            case("ack"):
//                response = server.queueExternalRequest(request);
                server.lmp.interpretMessage(request);
                break;
            default:
                response = server.accessBackend(request);
        }

        return response;
    }
}
