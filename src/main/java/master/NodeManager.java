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
    private Map<String, InetAddress> nodesIPs;
    private Map<String, ScheduledFuture<?>> countdowns;
    private Graph<String, DefaultEdge> graph;
    private Lock statusLock;


    public NodeManager() throws FileNotFoundException {
        this.topology = new HashMap<>();
        this.nodesStatus = new HashMap<>();
        this.nodesIPs = new HashMap<>();
        this.countdowns = new HashMap<>();
        this.graph = new Multigraph<>(DefaultEdge.class);
        this.statusLock = new ReentrantLock();
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

    public void setNodeIP(String nodeId, InetAddress nodeIP) throws UnknownHostException {
        this.nodesIPs.put(nodeId, nodeIP);
    }


    public boolean changeStatus(String nodeId, DatagramSocket socket) throws IOException {
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
                List<String> newNeighbours = this.nodesStatus.get(n);
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
        JsonReader reader = new JsonReader(new FileReader("./src/main/resources/topology.json"));
        HashMap<String, List<String>> data = gson.fromJson(reader, HashMap.class);
        this.topology = data;
    }

    public void startCountdown(String nodeId, DatagramSocket socket) {

        if(this.countdowns.containsKey(nodeId))
            this.countdowns.get(nodeId).cancel(true);

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        this.countdowns.put(nodeId, scheduler.schedule(() -> {
            try {
                changeStatus(nodeId, socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
}

