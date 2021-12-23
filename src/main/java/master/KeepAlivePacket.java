package master;

import java.io.*;

public class KeepAlivePacket implements Serializable {
    private String nodeId;
    private Boolean is_client;

    public KeepAlivePacket(String nodeId, Boolean isClient) {
        this.is_client = isClient;
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

    public Boolean node_isClient(){ return is_client; }


    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }
}
