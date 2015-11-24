import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    public int totalSeats = 5;
    public int numServers = 1;
    public int serverIndex;
    List<InetSocketAddress> serverList;
    InetSocketAddress server;
    TCPListenerThread tcpThread;
    MsgHandler msg;
    ArrayList<String> criticalSectionUpdates = new ArrayList<>();
    LamportMutex lmp;

    public HashMap<Integer,String> seatingChart;

    /**
     * Server for a ticket reservation system for a movie.
     * The system functions with TCP connections.
     * There can be multiple server, and multiple clients may access each server concurrently.
     * A person can reserve only one seat at any given time.
     *
     * @param serverList list of all servers with hosts and ports
     * @param serverIndex Index in server list for current server
     * @param numServers total number of servers
     * @param totalSeats total number of seats in theater
     */
    public Server(List<InetSocketAddress> serverList, int serverIndex, int numServers, int totalSeats){
        this.serverList = serverList;
        this.totalSeats = totalSeats;
        this.numServers = numServers;
        this.serverIndex = serverIndex;
        this.seatingChart = new HashMap<>();
        this.msg = new MsgHandler(this, numServers, serverIndex, serverList);
        lmp = new LamportMutex(msg, this);


        this.server = msg.getServer(serverIndex);
        MsgHandler.debug("Server configured at " + server.toString());

        requestExternalSync();
        //Set up listener thread for TCP
        try {
            tcpThread = new TCPListenerThread(this, server.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        tcpThread.start();
    }

    public void shutDown() throws IOException {
        tcpThread.interrupt();
    }

    public void clearSeatingChart() {
        seatingChart.clear();
    }

    public void makeAllSynchedUpdates(){
        MsgHandler.debug("Making all synched updates in list size(" + Integer.toString(criticalSectionUpdates.size()) + ")");
        String request;
        synchronized(criticalSectionUpdates) {
            Iterator<String> it=criticalSectionUpdates.iterator();
            while(it.hasNext()) {
                request = it.next();
                MsgHandler.debug("Making external update: " + request);
                        externalRequest(request);
            }
            criticalSectionUpdates.clear();
        }
    }

    private void externalRequest(String request){
        MsgHandler.debug("Processing request: " + request);
        ArrayList<String> requestList =  Utils.interpretStringAsList(request);
        String method = requestList.get(0);
        int seat = Integer.parseInt(requestList.get(1));
        switch(method) {
            case ("externalPut"):
                String name = requestList.get(2);
                externalReserve(name, seat);
                break;
            case ("externalDelete"):
                externalDelete(seat);
                break;
            case("request"):
                lmp.interpretMessage(request);
                break;
            case("release"):
                lmp.interpretMessage(request);
                break;
            case("ack"):
                MsgHandler.debug("Gettin Acks.");
                lmp.interpretMessage(request);
                break;

        }

    }

    private void externalDelete(int seat){
        seatingChart.remove(seat);
    }

    private void externalReserve(String name, int seat){
        seatingChart.put(seat, name);
    }

    public ArrayList<String> queueExternalRequest(String request){
        ArrayList<String> empty = new ArrayList<>();
        criticalSectionUpdates.add(request);
        return empty;
    }

    public void addReservation(int seat, String name){
        seatingChart.put(seat, name);
        String request = "externalPut," + seat + "," + name;
        msg.broadCastChange(request, lmp.v);
    }

    public void removeReservation(int seat){
        seatingChart.remove(seat);
        String request = "externalDelete," + seat;
        msg.broadCastChange(request, lmp.v);
    }

    public void requestExternalSync() {
        String request = "externalSync";
        int syncServerIndex = getClosestServerIndex();

        try {
            applyExternalSync(msg.makeServerRequest(serverList.get(syncServerIndex), syncServerIndex, request, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> supplyExternalSync() {
        ArrayList<String> response = new ArrayList<>();
        for (Integer seat : seatingChart.keySet()) {
            response.add(seat + "|" + seatingChart.get(seat));
        }

        return response;
    }

    private void applyExternalSync(String response) {
        if (response == null || response.equals("[]")) return;

        ArrayList<String> entries =  Utils.interpretStringAsList(response);

        for (String entry : entries) {
            String[] pair = entry.split("\\|");
            seatingChart.put(Integer.valueOf(pair[0]), pair[1]);
        }
    }

    private int getClosestServerIndex() {
        return (serverIndex + 1) % serverList.size();
    }

    /**
     * Find and hold the next available seat in the seating chart.
     *
     * @return The next seat number (by index) available.
     *      Returns -1 if no seats available.
     */
    private int getNextSeat(String name){
        for (int seat = 1; seat < totalSeats + 1; seat++){
            if (! seatAlreadyReserved(seat)){
                addReservation(seat, name);
                return seat;
            }
        }
        return -1;
    }

    /**
     * Search for a seat reservation based on an input name
     *
     * @param name The string to search for in seating chart
     * @return The seat number (by index) for the input name.
     *      Returns -1 if not found.
     */
    private int search(String name){
        if (seatingChart.containsValue(name)){
           for (int i = 1; i < totalSeats + 1; i++){
               if (seatingChart.containsKey(i)){
                   if (seatingChart.get(i).equals(name)){
                       lmp.releaseCriticalSection();
                       return i;
                   }
               }
           }
        }
        return -1;
    }

    private boolean soldOut(){
        boolean soldOut = seatingChart.size() == totalSeats;
        return soldOut;
    }

    private boolean existingReservation(String name){
        int seat = search(name);
        return seat != -1;
    }


    private boolean seatGreaterThanCapacity(int seat){
        return seat > totalSeats;
    }

    private boolean seatAlreadyReserved(int seat){
        lmp.requestCS();
        boolean seatReserved = seatingChart.containsKey(seat);
        lmp.releaseCriticalSection();
        return seatReserved;
    }

    private ArrayList<String> soldOutResponse(String name){
        ArrayList<String> response = new ArrayList<>();
        response.add("True");
        response.add("False");
        response.add("False");
        response.add("None");
        response.add("-1");
        response.add(name);
        return response;
    }

    private ArrayList<String> existingReservationResponse(String name){
        ArrayList<String> response = new ArrayList<>();
        response.add("False");
        response.add("True");
        response.add("False");
        response.add("None");
        response.add("-1");
        response.add(name);
        return response;
    }

    private ArrayList<String> reserveNextSeatResponse(int seat, String name){
        ArrayList<String> response = new ArrayList<>();
        response.add("False");
        response.add("False");
        response.add("False");
        response.add("None");
        response.add(Integer.toString(seat));
        response.add(name);
        return response;
    }


    private ArrayList<String> seatUnavailableResponse(int seat, String name){
        ArrayList<String> response = new ArrayList<>();
        response.add("False");
        response.add("False");
        response.add("True");
        response.add("None");
        response.add(Integer.toString(seat));
        response.add(name);
        return response;
    }


    private ArrayList<String> bookSeatResponse(int seat, String name){
        ArrayList<String> response = new ArrayList<>();
        response.add("False");
        response.add("False");
        response.add("False");
        response.add("None");
        response.add(Integer.toString(seat));
        response.add(name);
        return response;
    }

    private ArrayList<String> searchResponse(boolean existingReservation, int seat, String name){
        ArrayList<String> response = new ArrayList<>();
        response.add("False");
        response.add("False");
        response.add("False");
        if (existingReservation){
            response.add("True");
        }
        else{
            response.add("False");
        }
        response.add(Integer.toString(seat));
        response.add(name);

        return response;
    }

    private ArrayList<String> deleteResponse(boolean existingReservation, int seat, String name){
        ArrayList<String> response = new ArrayList<>();

        response.add("False");
        response.add("False");
        response.add("False");
        if (existingReservation){
            response.add("Delete");
        }
        else{
            response.add("False");
        }
        response.add(Integer.toString(seat));
        response.add(name);

        return response;
    }

    /**
     * Procedural logic for reserving a seat and formulating the appropriate response.
     * 1. Check for sold out.
     * 2. Check for existing reservation
     * 3. Get the next available seat
     *
     * @param name The name for which to reserve a seat
     * @return The formatted response ArrayList
     */
    public ArrayList<String> reserve(String name){
        lmp.requestCS();
        if (soldOut()){
            lmp.releaseCriticalSection();
            return soldOutResponse(name);
        }
        else {
            if(existingReservation(name)){
                lmp.releaseCriticalSection();
                return existingReservationResponse(name);
            }
            else {
                int seat = getNextSeat(name);
                lmp.releaseCriticalSection();
                return reserveNextSeatResponse(seat, name);
            }
        }
    }


    /**
     * Procedural logic for booking a seat and formulating the appropriate response.
     * 1. Check for sold out.
     * 2. Check for existing reservation
     * 3. Check for seat number out of bounds (0 and negative integers removed at Client)
     * 4. Check is seat is available and if so, book it.
     *
     * @param name The name for which to reserve a seat
     * @return The formatted response ArrayList
     */
    public ArrayList<String> bookSeat(String name, int seat){
        lmp.requestCS();
        if (soldOut()){
            lmp.releaseCriticalSection();
            return soldOutResponse(name);
        }
        else {
            if(existingReservation(name)){
                lmp.releaseCriticalSection();
                return existingReservationResponse(name);
            }
            else if (seatGreaterThanCapacity(seat) || seatAlreadyReserved(seat)){
                lmp.releaseCriticalSection();
                return seatUnavailableResponse(seat, name);
            }
            else {
                addReservation(seat, name);
                lmp.releaseCriticalSection();
                return bookSeatResponse(seat, name);
            }
        }
    }

    /**
     * Procedural logic for searching for a reservation and formulating the appropriate response.
     * Was a reservation found? If so, return seat number
     *
     * @param name The name for which to search
     * @return The formatted response ArrayList
     */
    public ArrayList<String> searchReservations(String name){
        lmp.requestCS();
        ArrayList<String> response =  searchResponse(existingReservation(name), search(name), name);
        lmp.releaseCriticalSection();
        return response;
    }

    /**
     * Procedural logic for deleting a reservation and formulating the appropriate response.
     * Was a reservation found? If so, delete reservation
     *
     * @param name The name for which to delete
     * @return The formatted response ArrayList
     */
    public ArrayList<String> delete(String name){
        lmp.requestCS();
        int seat = search(name);
        boolean existing = existingReservation(name);
        if (existing){
            removeReservation(seat);
        }
        lmp.releaseCriticalSection();
        return deleteResponse(existing, seat, name);
    }

    /**
     * Creates server responses for the Server class
     *
     * @param request server request to act upon.
     * @return Formatted ArrayList response to send back to client
     */
    public synchronized ArrayList<String> accessBackend(String request){
        ArrayList<String> requestList =  Utils.interpretStringAsList(request);
        ArrayList<String> response = new ArrayList<>();
        String method = requestList.get(0);
        String name = requestList.get(1);
        String seat;

        MsgHandler.debug("Accessing server with request: " + request);

        switch(method){
            case("bookSeat"):
                seat = requestList.get(2);
                response = bookSeat(name, Integer.parseInt(seat));
                break;

            case("reserve"):
                response = reserve(name);
                break;

            case("search"):
                response = searchReservations(name);
                break;

            case("delete"):
                response = delete(name);
                break;
        }

        return response;
    }


    public static void main (String[] args) {
        InputReader reader = new InputReader();

        ArrayList<Integer> serverConfig = reader.inputServerConfig();

        int serverIndex = serverConfig.get(0);
        int numServers = serverConfig.get(1);
        int numSeats = serverConfig.get(2);

        List<InetSocketAddress> serverList = reader.gatherServerList(numServers);

        //Set up server backend to accept requests from worker threads
        new Server(serverList, serverIndex, numServers, numSeats);
    }
}