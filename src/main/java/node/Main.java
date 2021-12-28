package node;

import master.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import streaming.Client;
import streaming.StreamRelay;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {
        Boolean isClient = Boolean.FALSE;
        final String nodeId = args[0];
        final String bootstrapper = args[1];
        DatagramSocket neighboursSocket = new DatagramSocket(Constants.NEIGHBOURS_PORT);
        DatagramSocket streamingSocket = new DatagramSocket(Constants.STREAMING_PORT);
        StreamRelay sr = new StreamRelay(streamingSocket);
        Logger log = LogManager.getLogger(Main.class);

        if(args.length == 3)
            if(args[2].equals("-c"))
                isClient = Boolean.TRUE;

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new KeepAliveSender(nodeId, bootstrapper, isClient), 0, 5, TimeUnit.SECONDS);

        new Thread(new NeighboursHandler(sr, neighboursSocket)).start();

        if(isClient)
            /* I'm a client, so I'll receive and forward the stream */
            new Thread(new Client(sr));
        else {
            /* I'm not a client, so I'll just forward stuff */

            scheduler.scheduleAtFixedRate((Runnable) () -> {

                byte[] cBuf = new byte[15000];

                //Construct a DatagramPacket to receive data from the UDP socket
                DatagramPacket rcvdp = new DatagramPacket(cBuf, cBuf.length);

                try{
                    //receive the DP from the socket:
                    streamingSocket.receive(rcvdp);

                    sr.relay(rcvdp);
                }
                catch (InterruptedIOException iioe){
                    log.info("Nothing to read");
                }
                catch (IOException ioe) {
                    log.error(ioe);
                }
            }, 0, 20, TimeUnit.MILLISECONDS);
        }
    }
}
