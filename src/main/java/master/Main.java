package master;

import streaming.Streamer;

import java.net.SocketException;

public class Main {
    public static String topologyPath = "topology.json";
    public static String videoPath = "movie.Mjpeg";

    public static void main(String[] args) throws SocketException {

        if(args.length >= 1)
            topologyPath = args[0];
        if(args.length >= 2)
            videoPath = args[1];


        try {
            NodeManager nm = new NodeManager();
            new Thread(new TaskRunner(nm)).start();

            new Thread(() -> new Streamer(videoPath, nm)).start();

            /* debug to check streaming stuff
            while(true){
                Thread.sleep(2000);
                System.out.println("STREAMING CLIENTS: " + nm.getClients());
                System.out.println("ROUTING TABLE: " + nm.getRoutingTable());
            }*/

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}
