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
        this.buildGraph();
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

            this.sendUpdatedNeighbours(socket, onlineNeighbours);
        }
        finally {
            this.statusLock.unlock();
        }

        return status;
    }

    /*
        Sends an updated list of neighbours to the given node's neighbours
     */
    private void sendUpdatedNeighbours(DatagramSocket socket, List<String> neighbours) throws IOException {
            /*
                For each neighbour of the changed node, send an updated list of their respective neighbours
            */
            //neighbours.add("O7");
            //this.setNodeIP("O7", InetAddress.getByName("localhost"));
            for (String n : neighbours) {
                List<String> newNeighboursList = this.nodesStatus.get(n);
                Map<String, InetAddress> newNeighbours = new HashMap<>();
                for(String s : newNeighboursList){
                    newNeighbours.put(s, this.nodesIPs.get(s));
                }
                NeighboursPacket packet = new NeighboursPacket(newNeighbours);
                byte[] x = new byte[0];
                x = packet.toBytes();
                DatagramPacket datagramPacket = new DatagramPacket(
                        x, x.length, nodesIPs.get(n), Constants.NEIGHBOURS_PORT);
                socket.send(datagramPacket);
            }
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
                e.printStackTrace();
            }
            setRoutingTable();
            System.out.println("[MASTER] Node " + nodeId + " went offline");
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
        return DijkstraShortestPath.findPathBetween(this.graph, from, to).getVertexList();
    }

    /*
    public List<String> get_clients(){
        List<String> client_list = new ArrayList<>();
        for(Map.Entry<String, Pair<Boolean, List<String>>> entry : nodesStatus.entrySet()){
            // if it's a streaming client, add it
            if(entry.getValue().getFirst()) client_list.add(entry.getKey());
        }
        return client_list;
    }
     */

    public List<String> getClients(){
        return new ArrayList<>(this.playerClients);
    }

    public Map<String, List<String>> getPaths(){
        Map<String, List<String>> all_paths = new HashMap<>();
        List<String> activeClients = getClients();
        if(activeClients.size() > 0)
            for(String client : activeClients){
                List<String> client_path = getShortestPath(Constants.SERVER_ID, client);
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

    public void setRoutingTable(){
       this.routingTable = buildFlows(getPaths());
    }

    public Map<String, Set<String>> getRoutingTable(){
        return this.routingTable;
    }
}

