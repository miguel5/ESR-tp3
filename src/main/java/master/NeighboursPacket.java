package master;

import java.io.*;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

public class NeighboursPacket implements Serializable {
    private Map<String, InetAddress> flows;

    public NeighboursPacket(Map<String, InetAddress> flows) {
        this.flows = flows;
    }

    public static NeighboursPacket bytesToObject (byte[] incomingData) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(incomingData);
        ObjectInputStream ois = new ObjectInputStream(in);
        return (NeighboursPacket) ois.readObject();
    }

    public Map<String, InetAddress> getNeighbours() { return flows; }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }
}