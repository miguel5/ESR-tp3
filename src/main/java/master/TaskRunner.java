package master;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class TaskRunner implements Runnable {
    private DatagramSocket keepAliveSocket;
    private DatagramSocket neighboursSocket;
    private DatagramSocket streamingSocket;
    private NodeManager nm;
    final static Logger log = LogManager.getLogger(NodeManager.class);

    public TaskRunner(NodeManager nm) throws IOException {
        this.keepAliveSocket = new DatagramSocket(Constants.KEEP_ALIVE_PORT);
        this.neighboursSocket = new DatagramSocket(Constants.NEIGHBOURS_PORT);
        this.streamingSocket = new DatagramSocket(Constants.STREAMING_PORT);
        this.nm = nm;

        // TODO: Get a more permanent solution to set the server online
        nm.setNodeIP(Constants.SERVER_ID, InetAddress.getByName("localhost"));
        this.nm.changeStatus(Constants.SERVER_ID, neighboursSocket, false);
    }

    @Override
    public void run() {
        byte[] incomingData = new byte[Constants.MAX_PACKET_SIZE];

        while (true) {
            try {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                this.keepAliveSocket.receive(incomingPacket);
                if(incomingPacket != null){
                    KeepAlivePacket keepAlivePacket = KeepAlivePacket.bytesToObject(incomingPacket.getData());
                    String nodeId = keepAlivePacket.getNodeId();
                    Boolean isClient = keepAlivePacket.node_isClient();

                    if (nm.isOnline(nodeId)){
                        // reset timer
                        this.nm.startCountdown(nodeId, neighboursSocket, isClient);
                        log.info("[MASTER] Node " + nodeId + " is Online");
                    }
                    else{
                        log.info("[MASTER] Node " + nodeId + " woke up");

                        nm.setNodeIP(nodeId, incomingPacket.getAddress());
                        nm.changeStatus(nodeId, neighboursSocket, isClient);
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
                log.error(e);
            }
        }
    }
}

