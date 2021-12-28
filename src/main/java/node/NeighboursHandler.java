package node;

import master.Constants;
import master.NeighboursPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import streaming.StreamRelay;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class NeighboursHandler implements Runnable {

    private DatagramSocket datagramSocket;
    private StreamRelay sr;
    final static Logger log = LogManager.getLogger(NeighboursHandler.class);

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
                    log.debug("Neighbours (NodeID : IP): " + neighboursPacket.getNeighbours().toString());
                }
            } catch(Exception e) {
                log.error(e);
            }
        }
    }
}
