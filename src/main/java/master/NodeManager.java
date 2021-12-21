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
    

    public NodeManager() throws FileNotFoundException {
        this.topology = new HashMap<>();
        this.nodesStatus = new HashMap<>();
        this.nodesIPs = new HashMap<>();
        this.countdowns = new HashMap<>();
        this.graph = new Multigraph<>(DefaultEdge.class);
        this.loadTopologyConfig();
        this.buildGraph();
    }

    public boolean isOnline(String nodeId) {
        return this.nodesStatus.containsKey(nodeId);
    }

    public void setNodeIP(String nodeId, InetAddress nodeIP) throws UnknownHostException {
        this.nodesIPs.put(nodeId, nodeIP);
    }


    public boolean changeStatus(String nodeId) {
        boolean status;

        if(this.nodesStatus.containsKey(nodeId)) {
            this.nodesStatus.remove(nodeId);
            status = false;
        } else {
            this.nodesStatus.put(nodeId, new ArrayList());
            status = true;
        }
        return status;
    }

    /*
        Sends an updated list of neighbours to the given node's neighbours
     */
    public void sendUpdatedNeighbours(String node, DatagramSocket socket){
        List<String> neighbours = getNeighbours(node);
        try {
            /*
                For each neighbour of the changed node, send an updated list of their respective neighbours
             */
            for(String n : neighbours){
                List<String> newNeighbours = getNeighbours(n);
                NeighboursPacket packet = new NeighboursPacket(newNeighbours);
                byte[] x = new byte[0];
                x = packet.toBytes();
                DatagramPacket datagramPacket = new DatagramPacket(
                        x, x.length, InetAddress.getByName("127.0.0.1") , Constants.NEIGHBOURS_PORT); //TODO: Replace 3rd parameter with "nodesIPs.get(n)"
                socket.send(datagramPacket);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO: Change to nodesStatus
    public List<String> getNeighbours(String nodeId) {
        return this.topology.get(nodeId);
    }

    private void loadTopologyConfig() throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader("./src/main/resources/topology.json"));
        HashMap<String, List<String>> data = gson.fromJson(reader, HashMap.class);
        this.topology = data;
        /*
        for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue().toString());
        }
         */
    }

    public void startCountdown(String nodeId, DatagramSocket socket) {

        if(this.countdowns.containsKey(nodeId))
            this.countdowns.get(nodeId).cancel(true);

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        this.countdowns.put(nodeId, scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                changeStatus(nodeId);
                sendUpdatedNeighbours(nodeId, socket);
                System.out.println("[MASTER] Node " + nodeId + " went offline");
            }
        }, 7, TimeUnit.SECONDS));
    }

    // TODO: Change 'topology' to 'nodesStatus'
    private void buildGraph(){
        for(String s : this.topology.keySet()){
            this.graph.addVertex(s);
        }

        for(Map.Entry<String, List<String>> e : this.topology.entrySet()){
            for(String s : e.getValue()){
                this.graph.addEdge(e.getKey(), s);
            }
        }
    }

    private List<String> getShortestPath(String from, String to){
        return DijkstraShortestPath.findPathBetween(this.graph, from, to).getVertexList();
    }
}

