package node;

import streaming.Cliente;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {
        Boolean is_client = Boolean.FALSE;
        final String nodeId = args[0];
        final String bootstrapper = args[1];

        if(args[1] != null) /* TODO: specific argument or scanner to change this */
            is_client = Boolean.TRUE;

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new KeepAliveSender(nodeId, bootstrapper, is_client), 0, 5, TimeUnit.SECONDS);

        new Thread(new NeighboursHandler()).start();

        if(is_client)
            /* TODO: i'm a client, so I'll receive and forward the stream */
            new Cliente();
        else {
            /* TODO: i'm not a client, so I'll just forward stuff */
        }
    }
}
