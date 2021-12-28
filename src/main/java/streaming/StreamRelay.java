package streaming;

import master.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StreamRelay {
    private Map<String, InetAddress> nodesIPs;
    private Map<String, InetAddress> flows;
    private DatagramSocket socket;
    private Lock flowsLock;

    public StreamRelay(DatagramSocket socket){
        this.nodesIPs = new HashMap<>();
        this.flows = new HashMap<>();
        this.socket = socket;
        this.flowsLock = new ReentrantLock();
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public void setNodesIPs(Map<String, InetAddress> nodesIPs) {
        try {
            this.flowsLock.lock();
            this.nodesIPs = nodesIPs;
        }
        finally {
            this.flowsLock.unlock();
        }
    }

    public void setFlows(Map<String, InetAddress> flows) {
        try {
            this.flowsLock.lock();
            this.flows = flows;
        }
        finally {
            this.flowsLock.unlock();
        }
    }

    public Map<String, InetAddress> getNodesIPs() {
        try {
            this.flowsLock.lock();
            return nodesIPs;
        }
        finally {
            this.flowsLock.unlock();
        }
    }

    public Map<String, InetAddress> getFlows() {
        try {
            this.flowsLock.lock();
            return flows;
        }
        finally {
            this.flowsLock.unlock();
        }
    }

    public void relay(DatagramPacket packet) throws IOException {
        for(InetAddress address : this.getFlows().values()){
            packet.setAddress(address);
            packet.setPort(Constants.STREAMING_PORT);
            this.socket.send(packet);
        }
    }
}
