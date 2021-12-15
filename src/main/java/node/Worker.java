package Node;

import Master.Constants;
import Master.KeepAlivePacket;

import java.io.IOException;
import java.net.*;

public class Worker implements Runnable{

    private DatagramSocket datagramSocket;

    public Worker() throws SocketException {
        this.datagramSocket = new DatagramSocket();
    }

    @Override
    public void run(){
        InetAddress address = null;
        try {
            address = InetAddress.getByName("localhost");

        KeepAlivePacket p = new KeepAlivePacket("1");
        byte[] x = new byte[0];
        x = p.toBytes();

        DatagramPacket packet = new DatagramPacket(x,x.length, address, 12345);
        this.datagramSocket.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
