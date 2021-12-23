package master;

import java.net.SocketException;

public class Main {
    public static void main(String[] args) throws SocketException {

        try {
            NodeManager nm = new NodeManager();
            new Thread(new TaskRunner(nm)).start();

            new Thread(){
                public void run(){
                    //new Servidor("src/main/resources/movie.Mjpeg", nm);
                }
            }.start();

            /* debug to check streaming stuff */
            while(true){
                Thread.sleep(2000);
                System.out.println("STREAMING CLIENTS: " + nm.getClients());
                System.out.println("ROUTING TABLE: " + nm.getRoutingTable());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}
