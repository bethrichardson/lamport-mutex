import org.junit.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TESTMultipleServerIntegration {
    Server server1, server2;
    List<InetSocketAddress> serverList;
    HashMap<Integer,String> expectedSeatingChart = new HashMap<>();

    public void checkCS(Server server){
        server.lmp.requestCS();
        server.lmp.releaseCriticalSection();
    }

    @Before
    public void setUp() throws Exception {
        serverList = Utils.getTestServerList();
        server1 = Utils.setupNewTestServer(serverList, 0);
        server2 = Utils.setupNewTestServer(serverList, 1);
    }
    @After
    public void tearDown() throws Exception {
        server1.shutDown();
        server2.shutDown();
        expectedSeatingChart.clear();
    }

    @Test
    public void testNewReservationReplicatedToAllServers() {
        int seat = 1;
        String name = "Beth";
        expectedSeatingChart.put(seat,name);
        server1.bookSeat(name, seat);
        checkCS(server2);

        assertEquals(server1.seatingChart, server2.seatingChart);
        assertEquals(server1.seatingChart, expectedSeatingChart);
    }

    @Test
    public void testDeleteReplicatedToAllServers() {
        int seat = 1;
        String name = "Beth";
        server1.bookSeat(name, seat);
        checkCS(server2);
        server2.delete(name);
        checkCS(server1);

        assertEquals(server1.seatingChart, server2.seatingChart);
        assertEquals(0, server1.seatingChart.size());

    }

    @Test
    public void testSimultaneousAddsReplicatedToAllServers() {
        int seat1 = 1;
        String name1 = "Beth";
        int seat2 = 2;
        String name2 = "Neel";
        expectedSeatingChart.put(seat1, name1);
        expectedSeatingChart.put(seat2, name2);

        server1.bookSeat(name1, seat1);
        server2.bookSeat(name2, seat2);
        checkCS(server1);
        checkCS(server2);

        assertEquals(server1.seatingChart, server2.seatingChart);

    }

    @Test
    public void testSimultaneousConflictingAddsBlocked() {
        int seat1 = 1;
        String name1 = "Beth";
        int seat2 = 2;
        String name2 = "Beth";
        server1.bookSeat(name1, seat1);
        server2.bookSeat(name2, seat2);
        checkCS(server1);
        checkCS(server2);

        assertEquals(1, server1.seatingChart.size());
        assertEquals(1, server2.seatingChart.size());

    }
}
