package node;

import master.Constants;
import master.KeepAlivePacket;

import java.io.IOException;
import java.net.*;

public class KeepAliveSender implements Runnable{

    private DatagramSocket datagramSocket;
    private String nodeId;
    private String bootstrapper;
    private Boolean is_client;

    public KeepAliveSender(String nodeId, String bootstrapper, Boolean isClient) throws SocketException {
        this.datagramSocket = new DatagramSocket();
        this.nodeId = nodeId;
        this.bootstrapper = bootstrapper;
        this.is_client = isClient;
    }

    @Override
    public void run(){
        InetAddress address = null;
        try {
            address = InetAddress.getByName(this.bootstrapper);

            KeepAlivePacket p = new KeepAlivePacket(nodeId, is_client);
            byte[] x = new byte[0];
            x = p.toBytes();

            DatagramPacket packet = new DatagramPacket(x,x.length, address, Constants.KEEP_ALIVE_PORT);

            for(int i = 0; i < 3; i++)
                this.datagramSocket.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
