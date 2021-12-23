package master;

import java.io.*;
import java.util.Set;

public class NeighboursPacket implements Serializable {
    private Set<String> flows;

    public NeighboursPacket(Set<String> flows) {
        this.flows = flows;
    }

    public static NeighboursPacket bytesToObject (byte[] incomingData) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(incomingData);
        ObjectInputStream ois = new ObjectInputStream(in);
        return (NeighboursPacket) ois.readObject();
    }

    public Set<String> getNeighbours() { return flows; }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }
}