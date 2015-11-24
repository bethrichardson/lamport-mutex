import java.util.*;
import java.util.regex.*;
import java.net.*; import java.io.*;

/**
 * A client for a ticket reservation system for a movie.
 * The system functions with TCP connections.
 * There is a single server, but multiple clients may access the server concurrently.
 * A person can reserve only one seat at any given time.
 */
public class Client {
    Scanner din;
    PrintStream pout;
    Map<String, String> responses = new HashMap<>();
    List<InetSocketAddress> serverList;
    int serverIndex;
    InetSocketAddress server;
    Socket tcpserver;

    /**
     *
     * @param serverList List of servers with hostnames and ports organized by proximity to client
     */
    public Client(List<InetSocketAddress> serverList){
        this.serverList = serverList;
        this.serverList = serverList;
        this.serverIndex = 0;
        this.server = this.serverList.get(serverIndex);
        System.out.println("Client server initialized to " + this.server.toString());
        initializeResponseSet();
    }


    /**
     * Sends request to server using TCP
     *
     * @param request request to send to the server
     */
    private ArrayList<String> tcpRequest(String request){
        String retstring = "[]";
        try {
            retstring =  makeTCPRequest(request);
        } catch (Exception e) {
            System.err.println("Server aborted:" + e);
        }
//        System.out.println("Received from Server:" + retstring);
        return Utils.interpretStringAsList(retstring);
    }


    /**
     * Outputs appropriate parameters if bad parameters are entered for a method
     *
     * @param method method requested
     */
    public void badParameters(String method){
        System.out.println("Bad parameters entered for " + method + "!");
        System.out.println("Please use the following command format:");
        switch(method){
            case ("reserve"):
                System.out.println("reserve <name>");
                break;

            case ("bookSeat"):
                System.out.println("bookSeat <name> <seat>");
                break;

            case ("search"):
                System.out.println("search <name>");
                break;

            case ("delete"):
                System.out.println("delete <name>");
                break;
        }
    }

    private void initializeResponseSet (){
        responses.put("NoResponse", "No server response.");
        responses.put("SoldOut", "Sold out - No seat available.");
        responses.put("Existing", "Seat already booked against the name provided");
        responses.put("NotAvailable", " is not available");
        responses.put("NotFound", "No reservation found for ");
        responses.put("AssignedSeat", "Seat assigned to you is ");
        responses.put("Seat", "Seat ");
        responses.put("IsAssigned", " is assigned to ");
        responses.put("Deleting", "Deleting seat ");
        responses.put("AssignedTo", " assigned to ");

    }

    /**
     * Interprets server responses and outputs relevant information to user
     * Response is composed of the following values:
     * 0: "True/False", Is the theater sold out?
     * 1: "True/False", Does a reservation already exist for the user?
     * 2: "True/False" Is the requested seat already booked,
     * 3: "True/False/None/Delete" Was search successful? If Delete, perform search and then delete reservation
     * 4: String seatNumber
     * 5: String name
     *
     * @param response response to interpret
     */
    public String getResponse(ArrayList<String> response){
        String result;
        if (response.size()==1){
            result = responses.get("NoResponse");
        }
        else {
            String soldOut = response.get(0);
            String existingReservation = response.get(1);
            String booked = response.get(2);
            String searchStatus = response.get(3);
            String seat = response.get(4);
            String name = response.get(5);
            name = name.substring(0, name.length());

            if (soldOut.equals("True")) {
                result = responses.get("SoldOut");
            } else if (existingReservation.equals("True")) {
                result = responses.get("Existing");
            } else if (booked.equals("True")) {
                result = seat + responses.get("NotAvailable");
            } else if (searchStatus.equals("False")) {
                result = responses.get("NotFound") + name;
            } else if (searchStatus.equals("True")) {
                result = responses.get("Seat") + seat + responses.get("IsAssigned") + name;
            } else if (searchStatus.equals("Delete")) {
                result = responses.get("Deleting") + seat + responses.get("AssignedTo") + name;
            }
            else {
                result = responses.get("AssignedSeat") + seat;
            }
        }
        return result;
    }

    /**
     * reserve <name> T|U – inputs the name of a person and reserves a seat against this name.
     * The client sends this command to the server using TCP protocol.
     * If the theater does not have enough seats(completely booked),
     * no seat is assigned and the command responds with message: ‘Sold out - No seat available’.
     * If a reservation has already been made under that name,
     * then the command responds with message: ‘Seat already booked against the name provided’.
     * Otherwise, a seat is reserved against the name provided
     * and the client is relayed a message: ‘Seat assigned to you is <seat-number>’.
     *
     * @param name the name for which to book the reservation
     */
    public ArrayList<String> reserve(String name) {
        String request = "reserve," + name;

        System.out.println("Attempting to reserve seat for " + name + ".");

        return sendRequest(request);
    }

    /**
     * bookSeat <name> <seatNum> T|U – behaves similar to reserve command
     * but imposes an additional constraint that a seat is reserved
     * if and only if there is no existing reservation against name
     * and the seat having the number <seatNum> is available.
     * If there is no existing reservation but <seatNum> is not available,
     * the response is: ‘<seatNum> is not available’.
     *
     * @param name the name for which to book the reservation
     * @param seat the seat number to book
     *  **/
    public ArrayList<String> bookSeat(String name, String seat) {
        String request = "bookSeat," + name + "," + seat;

        System.out.println("Attempting to book seat " + seat + " for " + name + ".");

        return sendRequest(request);
    }

    /**
     * search <name> T|U – returns the seat number reserved for name.
     * If no reservation is found for name the system responds with a message:
     * ‘No reservation found for <name>’.
     *
     * @param name the name for which to search
     */
    public ArrayList<String> search(String name) {
        String request = "search," + name;

        System.out.println("Searching for seat reservation for " + name + ".");

        return sendRequest(request);
    }

    /**
     * delete <name> T|U – frees up the seat allocated to that person.
     * The command returns the seat number that was released.
     * If no existing reservation was found, responds with:
     * ‘No reservation found for <name>’.
     *
     * @param name the name for which to delete the reservation
     */
    public ArrayList<String> delete(String name) {
        String request = "delete," + name;

        System.out.println("Delete seat reservation for " + name + ".");

        return sendRequest(request);
    }

    /**
     * Send the request to the server using TCP protocol
     *
     * @param request the request string formatted for server
     */
    private ArrayList<String> sendRequest(String request){
        ArrayList<String>response;

        response = tcpRequest(request);
        MsgHandler.debug(getResponse(response));
        return response;
    }

    /**
     * Access a new TCP socket
     *
     * @throws IOException
     */
    public void getSocket() throws IOException {
        while (true) {
            try {
            this.tcpserver = new Socket();
            tcpserver.connect(server, 100);
            din = new Scanner(tcpserver.getInputStream());
            pout = new PrintStream(tcpserver.getOutputStream());
            } catch (SocketTimeoutException | ConnectException e) {
                updateServer();
                continue;
            }

            break;
        }
    }

    /**
     * Make a TCP request using a new socket
     *
     * @param request The request string formatted for the server
     * @return the response from the server
     * @throws IOException
     */
    public String makeTCPRequest(String request)
            throws IOException {
        getSocket();
        pout.println(request);
        pout.flush();
        String retValue = din.nextLine();
        tcpserver.close();
        return retValue;
    }

    public void updateServer(){
        serverIndex = (serverIndex + 1) % serverList.size();
        server = serverList.get(serverIndex);
        System.out.println("Attempting access to server at " + server.toString());
    }

    public static void main (String[] args) {
        InputReader reader = new InputReader();
        List<InetSocketAddress> serverList = reader.clientInput();

        Client client = new Client(serverList);
        reader.waitForClientCommands(client);
    }
}