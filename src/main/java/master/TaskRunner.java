package master;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class TaskRunner implements Runnable {
    private DatagramSocket keepAliveSocket;
    private DatagramSocket neighboursSocket;
    private DatagramSocket streamingSocket;
    private NodeManager nm;

    public TaskRunner(NodeManager nm) throws SocketException {
        this.keepAliveSocket = new DatagramSocket(Constants.KEEP_ALIVE_PORT);
        this.neighboursSocket = new DatagramSocket(Constants.STREAMING_PORT);  // TODO: Change to NEIGHBOURS_PORT and uncomment line below
        //this.streamingSocket = new DatagramSocket(Constants.STREAMING_PORT);
        this.nm = nm;
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

                    if (nm.isOnline(nodeId)){
                        // reset timer
                        this.nm.startCountdown(nodeId, neighboursSocket);
                        System.out.println("[MASTER] Node " + nodeId + " is Online");
                    }
                    else{
                        System.out.println("[MASTER] Node " + nodeId + " woke up");

                        nm.setNodeIP(nodeId, incomingPacket.getAddress());
                        nm.changeStatus(nodeId, neighboursSocket);
                        //nm.sendUpdatedNeighbours(nodeId, neighboursSocket);


                        NeighboursPacket p = new NeighboursPacket(nm.getNeighbours(nodeId));
                        byte[] x = new byte[0];
                        x = p.toBytes();

                        DatagramPacket packet = new DatagramPacket(x,x.length, incomingPacket.getAddress(), Constants.NEIGHBOURS_PORT);
                        this.neighboursSocket.send(packet);

                    }
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

