package master;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class NodeManager {
    private HashMap<String, ArrayList<String>> topology;
    private HashMap<String, Set<String>> nodesStatus;

    public NodeManager() throws FileNotFoundException {
        this.topology = new HashMap<>();
        this.nodesStatus = new HashMap<>();
        this.loadTopologyConfig();
    }

    public boolean isOnline(String nodeId) {
        return this.nodesStatus.containsKey(nodeId);
    }

    public boolean changeStatus(String nodeId) {
        boolean status;
        if(this.nodesStatus.containsKey(nodeId)) {
            this.nodesStatus.remove(nodeId);
            status = false;
        } else {
            this.nodesStatus.put(nodeId, new HashSet<>());
            status = true;
        }
        return status;
    }

    public Set<String> getNeighbours(String nodeId) {
        return this.nodesStatus.get(nodeId);
    }

    private void loadTopologyConfig() throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader("./src/main/resources/topology.json"));
        HashMap<String, ArrayList<String>> data = gson.fromJson(reader, HashMap.class);
        this.topology = data;
        /*
        for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue().toString());
        }
         */
    }
}

