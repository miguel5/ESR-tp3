package master;

import java.io.FileNotFoundException;
import java.net.SocketException;

public class Main {
    public static void main(String[] args) throws SocketException {

        try {
            NodeManager nm = new NodeManager();
            new Thread(new TaskRunner(nm)).start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}
