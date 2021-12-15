package master;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class TaskRunner implements Runnable {
    private DatagramSocket datagramSocket;

    public TaskRunner() throws SocketException {
        this.datagramSocket = new DatagramSocket(Constants.PORT);
    }

    @Override
    public void run() {
        byte[] incomingData = new byte[Constants.MaxPacketSize];

        while (true) {
            try {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                this.datagramSocket.receive(incomingPacket);
                if(incomingPacket != null){
                    KeepAlivePacket keepAlivePacket = KeepAlivePacket.bytesToObject(incomingPacket.getData());
                    System.out.println(keepAlivePacket.getNodeId());
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
