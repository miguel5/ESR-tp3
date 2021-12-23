package node;

import streaming.Cliente;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {
        Boolean isClient = Boolean.FALSE;
        final String nodeId = args[0];
        final String bootstrapper = args[1];

        if(args.length == 3)
            if(args[2].equals("-c"))
                isClient = Boolean.TRUE;

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new KeepAliveSender(nodeId, bootstrapper, isClient), 0, 5, TimeUnit.SECONDS);

        new Thread(new NeighboursHandler()).start();

        if(isClient)
            /* TODO: i'm a client, so I'll receive and forward the stream */
            new Cliente();
        else {
            /* TODO: i'm not a client, so I'll just forward stuff */
        }
    }
}
