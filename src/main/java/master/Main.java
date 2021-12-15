package master;

import java.io.FileNotFoundException;
import java.net.SocketException;

public class Main {
    public static void main(String[] args) throws SocketException {
        new Thread(new TaskRunner()).start();
        try {
            NodeManager nm = new NodeManager();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}
