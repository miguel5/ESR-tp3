package node;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {

        final String nodeId = args[0];
        final String bootstrapper = args[1];

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new KeepAliveSender(nodeId, bootstrapper), 0, 5, TimeUnit.SECONDS);

        new Thread(new NeighboursHandler()).start();
    }
}
