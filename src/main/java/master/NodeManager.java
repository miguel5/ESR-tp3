package master;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;

public class NodeManager {
    private Map<String, List<String>> topology;
    private Map<String, List<String>> nodesStatus;
    private Set<String> playerClients;
    private Map<String, InetAddress> nodesIPs;
    private Map<String, ScheduledFuture<?>> countdowns;
    private Graph<String, DefaultEdge> graph;
    private Map<String, Set<String>> routingTable;
    private Lock statusLock, nodesIPLock;
    final static Logger log = LogManager.getLogger(NodeManager.class);

    public NodeManager() throws FileNotFoundException {
        this.topology = new HashMap<>();
        this.nodesStatus = new HashMap<>();
        this.playerClients = new HashSet<>();
        this.nodesIPs = new HashMap<>();
        this.countdowns = new HashMap<>();
        this.routingTable = new HashMap<>();
        this.graph = new Multigraph<>(DefaultEdge.class);
        this.statusLock = new ReentrantLock();
        this.nodesIPLock = new ReentrantLock();

        this.loadTopologyConfig();
    }

    public boolean isOnline(String nodeId) {
        try {
            this.statusLock.lock();
            return this.nodesStatus.containsKey(nodeId);
        }
        finally {
            this.statusLock.unlock();
        }
    }

    public Map<String, InetAddress> getNodesIPs() {
        try {
            this.nodesIPLock.lock();
            return nodesIPs;
        }
        finally {
            this.nodesIPLock.unlock();
        }
    }

    public void setNodeIP(String nodeId, InetAddress nodeIP) throws UnknownHostException {
        try {
            this.nodesIPLock.lock();
            this.nodesIPs.put(nodeId, nodeIP);
        } finally {
            this.nodesIPLock.unlock();
        }
    }


    public boolean changeStatus(String nodeId, DatagramSocket socket, Boolean isClient) throws IOException {
        boolean status;
        List<String> onlineNeighbours = new ArrayList<>();
        List<String> allNeighbours = this.topology.get(nodeId);

        try {
            this.statusLock.lock();

            // Build list of online neighbours
            for (String s : this.nodesStatus.keySet()) {
                if (allNeighbours.contains(s))
                    onlineNeighbours.add(s);
            }

            // Node going offline
            if (this.nodesStatus.containsKey(nodeId)) {
                this.nodesStatus.remove(nodeId);

                if(this.playerClients.contains(nodeId))
                    this.playerClients.remove(nodeId);

                for (String s : onlineNeighbours)
                    this.nodesStatus.get(s).remove(nodeId);

                status = false;
            }
            // Node going online
            else {
                if(isClient)
                    this.playerClients.add(nodeId);
                this.nodesStatus.put(nodeId, onlineNeighbours);
                status = true;
            }

            // Rebuild the graph
            this.buildGraph();

            // Get nodes that share the same flow with the changed node
            Set<String> sameFlowNodes = this.getNodesFromSameFlows(nodeId);

            // set routing table when keep alive packets are coming
            this.updateRoutingTable();

            /*
                Node going online
                Send to every neighbour and to the node itself
             */
            if(status){
                sendNewFlows(socket, Arrays.asList(new String[]{nodeId}));
                sendNewFlows(socket, new ArrayList<>(sameFlowNodes));
            }
            /*
                Node going offline
                Send only to its former neighbours
             */
            else
                sendNewFlows(socket, new ArrayList<>(sameFlowNodes));
        }
        finally {
            this.statusLock.unlock();
        }
        return status;
    }

    /*
        Sends the new flows for the nodes given in the 'nodes' list
     */
    private void sendNewFlows(DatagramSocket socket, List<String> nodes) throws IOException{

        for (String n : nodes) {
            // routing table is done, so send the neighbours (flows) to nodeId
            Map<String, InetAddress> node_flows = new HashMap<>();
            if(this.getRoutingTable().containsKey(n)){
                for(String s : this.getRoutingTable().get(n)){
                    node_flows.put(s, this.nodesIPs.get(s));
                }
            }
            NeighboursPacket p = new NeighboursPacket(node_flows);

            byte[] x = new byte[0];
            x = p.toBytes();

            DatagramPacket packet = new DatagramPacket(x,x.length, this.nodesIPs.get(n), Constants.NEIGHBOURS_PORT);
            socket.send(packet);
        }

    }

    // Returns a set of nodes that are in the same flow(s) of the given node
    private Set<String> getNodesFromSameFlows(String nodeId){
        Set<String> nodes = new HashSet<>();

        if(this.routingTable.containsKey(nodeId))
            nodes.addAll(this.routingTable.get(nodeId));

        for(Set<String> flow : this.routingTable.values()){
            if(flow.contains(nodeId))
                nodes.addAll(flow);
        }

        return this.nodesStatus.keySet();
    }

    public List<String> getNeighbours(String nodeId) {
        try {
            this.statusLock.lock();
            return this.nodesStatus.get(nodeId);
        }
        finally {
            this.statusLock.unlock();
        }

    }

    private void loadTopologyConfig() throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(Main.topologyPath));
        HashMap<String, List<String>> data = gson.fromJson(reader, HashMap.class);
        this.topology = data;
    }

    public void startCountdown(String nodeId, DatagramSocket socket, Boolean isClient) {

        if(this.countdowns.containsKey(nodeId))
            this.countdowns.get(nodeId).cancel(true);

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        this.countdowns.put(nodeId, scheduler.schedule(() -> {
            try {
                changeStatus(nodeId, socket, isClient);
            } catch (IOException e) {
                log.error(e);
            }
            //updateRoutingTable();
            log.info("Node " + nodeId + " went offline");
        }, 7, TimeUnit.SECONDS));
    }


    private void buildGraph(){
        for(String s : this.nodesStatus.keySet()){
            this.graph.addVertex(s);
        }

        for(Map.Entry<String, List<String>> e : this.nodesStatus.entrySet()){
            for(String s : e.getValue()){
                this.graph.addEdge(e.getKey(), s);
            }
        }
    }

    private List<String> getShortestPath(String from, String to){
        List<String> path = DijkstraShortestPath.findPathBetween(this.graph, from, to).getVertexList();

        if(path == null){
            return new ArrayList<>();

        }
        return DijkstraShortestPath.findPathBetween(this.graph, from, to).getVertexList();
    }

    public List<String> getClients(){
        return new ArrayList<>(this.playerClients);
    }

    public Map<String, List<String>> getPaths(){
        Map<String, List<String>> all_paths = new HashMap<>();
        List<String> activeClients = getClients();
        if(activeClients.size() > 0)
            for(String client : activeClients){
                List<String> client_path = this.getShortestPath(Constants.SERVER_ID, client);
                all_paths.put(client, client_path);
            }
        return all_paths;
    }

    public Map<String, Set<String>> buildFlows(Map<String, List<String>> all_paths){
        // key: node; value: destinations
        Map<String, Set<String>> all_flows = new HashMap<>();
        if(all_paths.size() > 0)
            for (Map.Entry<String, List<String>> entry : all_paths.entrySet()) {
                int list_size = entry.getValue().size();
                for(int i = 0; i < list_size - 1; i++){
                    String father_node = entry.getValue().get(i);
                    String next_node = entry.getValue().get(i+1);
                    if(all_flows.containsKey(entry.getValue().get(i))){
                        all_flows.get(father_node).add(next_node);
                    }else{
                        Set<String> new_set = new HashSet<>();
                        new_set.add(next_node);
                        all_flows.put(father_node, new_set);
                    }
                }
            }
        return all_flows;
    }

    public void updateRoutingTable(){
       this.routingTable = buildFlows(getPaths());
    }

    public Map<String, Set<String>> getRoutingTable(){
        return this.routingTable;
    }
}

