package node;

import master.Constants;
import master.KeepAlivePacket;

import java.io.IOException;
import java.net.*;

public class KeepAliveSender implements Runnable{

    private DatagramSocket datagramSocket;
    private String nodeId;

    public KeepAliveSender(String nodeId) throws SocketException {
        this.datagramSocket = new DatagramSocket();
        this.nodeId = nodeId;
    }

    @Override
    public void run(){
        InetAddress address = null;
        try {
            address = InetAddress.getByName("localhost");

            KeepAlivePacket p = new KeepAlivePacket(nodeId);
            byte[] x = new byte[0];
            x = p.toBytes();

            DatagramPacket packet = new DatagramPacket(x,x.length, address, Constants.KEEP_ALIVE_PORT);
            this.datagramSocket.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
