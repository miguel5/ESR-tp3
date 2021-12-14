package Master;

import java.io.*;

public class KeepAlivePacket implements Serializable {
    private String nodeId;

    public KeepAlivePacket(String nodeId) {
        this.nodeId = nodeId;
    }

    public static KeepAlivePacket  bytesToObject (byte[] incomingData) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(incomingData);
        ObjectInputStream ois = new ObjectInputStream(in);
        return (KeepAlivePacket) ois.readObject();
    }

    public String getNodeId() {
        return nodeId;
    }


    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }
}
