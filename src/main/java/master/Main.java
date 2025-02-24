package master;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import streaming.Streamer;

import java.net.SocketException;

public class Main {
    public static String topologyPath = "topology.json";
    public static String videoPath = "movie.Mjpeg";
    final static Logger log = LogManager.getLogger(master.Main.class);

    public static void main(String[] args) throws SocketException {

        if(args.length >= 1)
            topologyPath = args[0];
        if(args.length >= 2)
            videoPath = args[1];


        try {
            NodeManager nm = new NodeManager();
            new Thread(new TaskRunner(nm)).start();

            new Thread(() -> new Streamer(videoPath, nm)).start();

        } catch (Exception e) {
            log.fatal(e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}
