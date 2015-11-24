
import java.util.List;
import java.net.InetSocketAddress;
import java.util.Random;
import static org.junit.Assert.*;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class TESTMultipleClientIntegration {
    Server mainServer, failoverServer;
    List<InetSocketAddress> serverList;

    @BeforeClass
    public void setUp() throws Exception {
        serverList = Utils.getTestServerList();
        mainServer = Utils.setupNewTestServer(serverList, 0);
        failoverServer = Utils.setupNewTestServer(serverList, 1);
    }

    @AfterClass
    public void tearDown() throws Exception {
        mainServer.shutDown();
        failoverServer.shutDown();

    }

    @org.testng.annotations.Test(threadPoolSize = 5, invocationCount = 5,  timeOut = 100)
    public void BookTheater() {
        Random rand = new Random();
        int n = rand.nextInt(1000);
        Client client = new Client(serverList);

        String name = "Beth" + "-" + Integer.toString(n);
        String expectedResponse = client.responses.get("AssignedSeat");
        String response = reserve(client, name);
        response = response.substring(0, response.length()-1);

        assertEquals(expectedResponse, response);
    }

    public String reserve(Client client, String name){
        return client.getResponse(client.reserve(name));
    }
}
