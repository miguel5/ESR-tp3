package node;

import master.Constants;
import master.NeighboursPacket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class NeighboursHandler implements Runnable {

    private DatagramSocket datagramSocket;

    public NeighboursHandler() throws SocketException {
        this.datagramSocket = new DatagramSocket(Constants.NEIGHBOURS_PORT);
    }

    @Override
    public void run() {
        byte[] incomingData = new byte[Constants.MAX_PACKET_SIZE];

        while(true){
            try {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                this.datagramSocket.receive(incomingPacket);
                if(incomingPacket != null){
                    NeighboursPacket neighboursPacket = NeighboursPacket.bytesToObject(incomingPacket.getData());
                    System.out.println("[NODE] " + neighboursPacket.getNeighbours().toString());
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
