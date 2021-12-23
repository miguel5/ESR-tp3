package node;

import master.Constants;
import streaming.Cliente;
import streaming.StreamRelay;

import java.io.IOException;
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

        if(args.length == 3)
            if(args[2].equals("-c"))
                isClient = Boolean.TRUE;

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new KeepAliveSender(nodeId, bootstrapper, isClient), 0, 5, TimeUnit.SECONDS);

        new Thread(new NeighboursHandler(sr, neighboursSocket)).start();

        if(isClient)
            /* I'm a client, so I'll receive and forward the stream */
            new Thread(new Cliente(sr));
        else {
            /* I'm not a client, so I'll just forward stuff */
        }
    }
}
