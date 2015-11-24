import org.junit.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.Assert.*;

public class TESTIntegrationTest {
    Client client;
    Server mainServer, failoverServer;
    List<InetSocketAddress> serverList;

    @Before
    public void setUp() throws Exception {
        serverList = Utils.getTestServerList();
        client = new Client(serverList);
        mainServer = Utils.setupNewTestServer(serverList, 0);
        failoverServer = Utils.setupNewTestServer(serverList, 1);
    }

    @After
    public void tearDown() throws Exception {
        mainServer.shutDown();
        failoverServer.shutDown();

    }

    @Test
    public void testReserve() throws Exception {
        String name = "Beth";
        String seat = "1";
        String expectedResponse = client.responses.get("AssignedSeat") + seat;
        String response = reserve(name);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testBookSeat() throws Exception {
        String name = "Beth";
        String seat = "2";
        String expectedResponse = client.responses.get("AssignedSeat") + seat;
        String response = bookSeat(name, seat);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testSearch() throws Exception {
        String name = "Beth";
        String seat = "2";
        String expectedResponse = client.responses.get("Seat") + seat + client.responses.get("IsAssigned") + name;
        bookSeat(name, seat);
        String response = search(name);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testDelete() throws Exception {
        String name = "Beth";
        String seat = "2";
        String expectedResponse = client.responses.get("Deleting") + seat + client.responses.get("AssignedTo") + name;
        bookSeat(name, seat);
        String response = delete(name);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testReserveAtCapacity() throws Exception {
        String name = "Beth";
        String expectedResponse = client.responses.get("SoldOut");
        for (int i = 0; i < 5; i++){
            reserve(name + " " + Integer.toString(i));
        }
        String response = reserve(name);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testReserveDuplicate() throws Exception {
        String name = "Beth";
        String expectedResponse = client.responses.get("Existing");
        reserve(name);
        String response = reserve(name);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testBookSeatUnavailble() throws Exception {
        String name = "Beth";
        String name2 = "Neel";
        String seat = "2";
        String expectedResponse = seat + client.responses.get("NotAvailable");
        bookSeat(name, seat);
        String response = bookSeat(name2, seat);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testSearchNotFound() throws Exception {
        String name = "Beth";
        String expectedResponse = client.responses.get("NotFound") + name;
        String response = search(name);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testDeleteNotFound() throws Exception {
        String name = "Beth";
        String expectedResponse = client.responses.get("NotFound") + name;
        String response = delete(name);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testFailover() throws InterruptedException, IOException {
        mainServer.shutDown();

        String name = "Beth";
        String seat = "1";
        String expectedResponse = client.responses.get("AssignedSeat") + seat;
        String response = reserve(name);

        assertEquals(failoverServer.server, client.server);
        assertEquals(expectedResponse, response);
    }

    @Test
    public void testRecovery() throws InterruptedException, IOException {
        mainServer.shutDown();

        reserve("Beth");
        reserve("Neel");
        reserve("Jake");
        reserve("Ashley");

        mainServer = Utils.setupNewTestServer(serverList, 0);

        assertEquals(failoverServer.seatingChart, mainServer.seatingChart);
    }

    public String bookSeat(String name, String seat){
        return client.getResponse(client.bookSeat(name, seat));
    }

    public String reserve(String name){
        return client.getResponse(client.reserve(name));
    }

    public String search(String name){
        return client.getResponse(client.search(name));
    }

    public String delete(String name){
        return client.getResponse(client.delete(name));
    }

}
