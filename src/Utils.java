import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Utils {
    public static Boolean debugger = true;

    /**
     * Read in a CSV string and convert to ArrayList of Strings.
     * Used for reading client requests.
     *
     * @param str the CSV string to convert to list
     * @return the ArrayList version of the string
     */
    public static ArrayList<String> interpretStringAsList(String str){
        ArrayList<String> resultList = new ArrayList<>();
        str = str.replace("[", "").replace("]", "");
        resultList.addAll(Arrays.asList(str.split("\\s*,\\s*")));
        return resultList;
    }

    public static int convert(int[]v){
        int lamport = 0;
        for (int aV : v) {
            lamport += aV;
        }
        return lamport;
    }

    public static void badInputs(){
        System.out.println("Please re-enter your inputs.");
    }

    public static List<InetSocketAddress> getTestServerList(){
        ArrayList<InetSocketAddress> ServerList = new ArrayList<>();
        try {
            ServerList.add(new InetSocketAddress(InetAddress.getLocalHost(), 9055));
            ServerList.add(new InetSocketAddress(InetAddress.getLocalHost(), 9050));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ServerList;
    }

    public static Server setupNewTestServer(List<InetSocketAddress> ServerList, int serverIndex){
        int num_servers = 2;
        int numSeats = 5;
        return new Server(ServerList, serverIndex, num_servers, numSeats);
    }

    public static int max(int a, int b) {
        if (a > b)
            return a;
        return b;
    }

    public static void mySleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    public static void myWait(Object obj) {
        MsgHandler.debug("waiting");
        try {
            obj.wait(10);
        } catch (InterruptedException e) {
        }
    }

    public static boolean lessThan(int A[], int B[]) {
        for (int j = 0; j < A.length; j++)
            if (A[j] > B[j])
                return false;
        for (int j = 0; j < A.length; j++)
            if (A[j] < B[j])
                return true;
        return false;
    }

    public static int maxArray(int A[]) {
        int v = A[0];
        for (int i = 0; i < A.length; i++)
            if (A[i] > v)
                v = A[i];
        return v;
    }

    public static String writeArray(int A[]) {
        StringBuffer s = new StringBuffer();
        for (int j = 0; j < A.length; j++)
            s.append(String.valueOf(A[j]) + " ");
        return new String(s.toString());
    }
    public static String readLine(InputStream sin) throws IOException {
        BufferedReader inp = new BufferedReader(
                new InputStreamReader(sin));
        return inp.readLine();
    }
    public static String readLine(String fileName) throws IOException{
        BufferedReader inp = new BufferedReader(
                new FileReader(fileName));
        return inp.readLine();
    }
    public static void readArray(String s, int A[]) {
        StringTokenizer st = new StringTokenizer(s);
        for (int j = 0; j < A.length; j++)
            A[j] = Integer.parseInt(st.nextToken());
    }

    public static void readList(String s, LinkedList<Integer> q) {
        StringTokenizer st = new StringTokenizer(s);
        q.clear();
        while (st.hasMoreTokens()) {
            q.add(Integer.parseInt(st.nextToken()));
        }
    }

    public static int searchArray(int A[], int x) {
        for (int i = 0; i < A.length; i++)
            if (A[i] == x)
                return i;
        return -1;
    }

    public static void println(String s) {
        if (true) {
            System.out.println(s);
            System.out.flush();
        }
    }

    public static LinkedList<Object> getLinkedList(Object... objects) {
        LinkedList<Object> list = new LinkedList<Object>();
        for (int i = 0; i < objects.length; i += 1) {
            list.add(objects[i]);
        }
        return list;
    }
}
