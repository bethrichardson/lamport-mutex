import java.util.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


public class InputReader {
    Scanner sc;

    public InputReader() {
        sc = new Scanner(System.in);
    }

    public List<InetSocketAddress> clientInput() {
        int numServers = inputNumServers();
        return gatherServerList(numServers);
    }

    public int inputNumServers() {
        System.out.println("Please enter the number of servers: ");
        String clientInput = sc.nextLine();
        int numServers = 0;

        while (numServers == 0) {
            try {
                numServers = Integer.parseInt(clientInput);
            } catch (NumberFormatException nfe) {
                Utils.badInputs();
            }

        }

        return numServers;
    }

    public ArrayList<Integer>  inputServerConfig() {
        ArrayList<Integer> serverConfig = new ArrayList<>();

        while (serverConfig.isEmpty()) {
            System.out.println("Please enter server − id, total numbers of servers," +
                    " and the total number of seats in the theater: ");

            String serverInput = sc.nextLine();
            String[] serverInputs = serverInput.split("\\s+");

            try {
                serverConfig.add(Integer.parseInt(serverInputs[0])-1);
                serverConfig.add(Integer.parseInt(serverInputs[1]));
                serverConfig.add(Integer.parseInt(serverInputs[2]));
            } catch (NumberFormatException nfe) {
                Utils.badInputs();
                serverConfig.clear();
            }
            catch (ArrayIndexOutOfBoundsException oob) {
                Utils.badInputs();
                serverConfig.clear();
            }
        }

        return serverConfig;
    }



    public List<InetSocketAddress> gatherServerList(int numServers) {
        String[] serverInput;
        String server;
        int port;

        List<InetSocketAddress> serverList = new ArrayList<>();

        while (serverList.isEmpty()) {
            try {
                for (int i = 0; i < numServers; i++) {
                    try {
                        serverInput = sc.nextLine().split(":");
                        server = serverInput[0];
                        port = Integer.parseInt(serverInput[1]);

                        serverList.add(new InetSocketAddress(InetAddress.getByName(server), port));
                    } catch (NumberFormatException | UnknownHostException e) {
                        Utils.badInputs();
                        serverList.clear();
                        break;
                    }
                }
            }
            catch (ArrayIndexOutOfBoundsException oob) {
                Utils.badInputs();
                serverList.clear();
            }
        }

        for (InetSocketAddress inetSocketAddress : serverList) {
            System.out.println("Server Added: " + inetSocketAddress.toString());
        }

        return serverList;
    }

    public void waitForClientCommands(Client client) {
        String cmd, name, seat;
        String[] tokens = null;

        while (sc.hasNextLine()) {

            while (tokens == null) {
                cmd = sc.nextLine();
                tokens = cmd.split("\\s+");

                if (tokens.length < 2) {
                    Utils.badInputs();
                    tokens = null;
                }
            }

            switch (tokens[0]) {
                case "reserve":
                    if (tokens.length > 1) {
                        name = tokens[1];
                        client.reserve(name);
                        tokens = null;
                    } else {
                        client.badParameters("reserve");
                        tokens = null;
                    }


                    break;
                case "bookSeat":
                    if (tokens.length > 2) {
                        name = tokens[1];
                        seat = tokens[2];
                        try {
                            int seatInt = Integer.parseInt(seat);
                            if (seatInt > 0) {
                                client.bookSeat(name, seat);
                                tokens = null;
                            } else {
                                client.badParameters("bookSeat");
                                tokens = null;
                            }

                        } catch (NumberFormatException nfe) {
                            client.badParameters("bookSeat");
                            tokens = null;
                        }


                    } else {
                        client.badParameters("bookSeat");
                        tokens = null;
                    }

                    break;
                case "search":
                    if (tokens.length > 1) {
                        name = tokens[1];
                        client.search(name);
                        tokens = null;
                    } else {
                        client.badParameters("search");
                        tokens = null;
                    }

                    break;
                case "delete":
                    if (tokens.length > 1) {
                        name = tokens[1];
                        client.delete(name);
                        tokens = null;
                    } else {
                        client.badParameters("delete");
                        tokens = null;
                    }

                    break;
                default:
                    System.out.println("ERROR: No such command");
            }
        }
    }
}
