import java.net.InetSocketAddress;
import java.util.*;

/**
 * A test client for a ticket reservation system
 * Accesses the backend with attempt to reserve a seat for
 * input name 10 times using input protocol
 */
public class TestClient {

    public static void main (String[] args) {
        List<InetSocketAddress> serverList = Utils.getTestServerList();


        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()) {
            String cmd = sc.nextLine();
            String[] tokens = cmd.split(" ");

            String name = tokens[0];

            for (int i=0; i < 10; i++){

                Client client = new Client(serverList);
                client.reserve(name);
            }
        }

    }
}

