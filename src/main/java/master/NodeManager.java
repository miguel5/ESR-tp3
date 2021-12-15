package Master;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NodeManager {
    private HashMap<String, Set<String>> topology;
    private HashMap<String, Set<String>> nodesStatus;

    public NodeManager() {
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

    private void loadTopologyConfig() {
    }
}

