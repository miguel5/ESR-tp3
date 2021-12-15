package master;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class NeighboursPacket implements Serializable {
    private List<String> neighbours;

    public NeighboursPacket(List<String> neighbours) {
        this.neighbours = neighbours;
    }

    public static NeighboursPacket bytesToObject (byte[] incomingData) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(incomingData);
        ObjectInputStream ois = new ObjectInputStream(in);
        return (NeighboursPacket) ois.readObject();
    }

    public List<String> getNeighbours() {
        return neighbours;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }
}