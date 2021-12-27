package master;

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

    public TaskRunner(NodeManager nm) throws IOException {
        this.keepAliveSocket = new DatagramSocket(Constants.KEEP_ALIVE_PORT);
        this.neighboursSocket = new DatagramSocket(Constants.NEIGHBOURS_PORT);  // TODO: Change to NEIGHBOURS_PORT and uncomment line below
        this.streamingSocket = new DatagramSocket(Constants.STREAMING_PORT);
        this.nm = nm;

        // TODO: Get a more permanent solution to set the server online
        nm.setNodeIP(Constants.SERVER_ID, InetAddress.getByName("10.0.0.10"));
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
                        System.out.println("[MASTER] Node " + nodeId + " is Online");
                    }
                    else{
                        System.out.println("[MASTER] Node " + nodeId + " woke up");

                        nm.setNodeIP(nodeId, incomingPacket.getAddress());
                        nm.changeStatus(nodeId, neighboursSocket, isClient);

                        // set routing table when keep alive packets are coming
                        nm.updateRoutingTable();

                        // routing table is done, so send the neighbours (flows) to nodeId
                        Map<String, InetAddress> node_flows = new HashMap<>();
                        if(nm.getRoutingTable().containsKey(nodeId)){
                            Map<String, InetAddress> nodesIPs = this.nm.getNodesIPs();
                            for(String s : nm.getRoutingTable().get(nodeId)){
                                node_flows.put(s, nodesIPs.get(s));
                            }
                        }
                        NeighboursPacket p = new NeighboursPacket(node_flows);

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

