package node;

import master.Constants;
import master.NeighboursPacket;
import streaming.StreamRelay;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class NeighboursHandler implements Runnable {

    private DatagramSocket datagramSocket;
    private StreamRelay sr;

    public NeighboursHandler(StreamRelay sr, DatagramSocket datagramSocket) throws SocketException {
        this.datagramSocket = datagramSocket;
        this.sr = sr;
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
                    sr.setFlows(neighboursPacket.getNeighbours());
                    System.out.println("[NODE] " + neighboursPacket.getNeighbours().toString());
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
