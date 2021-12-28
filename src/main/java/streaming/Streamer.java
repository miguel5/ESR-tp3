package streaming;

/* ------------------
   Servidor
   usage: java Servidor [Video file]
   adaptado dos originais pela equipa docente de ESR (nenhumas garantias)
   colocar primeiro o cliente a correr, porque este dispara logo
---------------------- */

import master.Constants;
import master.NodeManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.Timer;


public class Streamer extends JFrame implements ActionListener, Runnable {
    NodeManager nm;

    Logger log = LogManager.getLogger(streaming.Streamer.class);

    //GUI:
    //----------------
    JLabel label;

    //RTP variables:
    //----------------
    DatagramPacket senddp; //UDP packet containing the video frames (to send)A
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet
    int RTP_dest_port = Constants.STREAMING_PORT; //destination port for RTP packets

    static String VideoFileName; //video file to request to the server

    //Video constants:
    //------------------
    int imagenb = 0;               //image nb of the image currently transmitted
    VideoStream video;             //VideoStream object used to access video frames
    static int MJPEG_TYPE = 26;    //RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; //length of the video in frames

    Timer sTimer; //timer used to send the images at the video frame rate
    byte[] sBuf; //buffer used to store the images to send to the client

    //--------------------------
    //Constructor
    //--------------------------
    public Streamer(String videoFileName, NodeManager nm) {
        this.nm = nm;

        //init Frame
        new JFrame("Server");

        this.VideoFileName = videoFileName;

        // init para a parte do servidor
        sTimer = new Timer(FRAME_PERIOD, this); //init Timer para servidor
        sTimer.setInitialDelay(0);
        sTimer.setCoalesce(true);
        sBuf = new byte[15000]; //allocate memory for the sending buffer

        try {
            RTPsocket = new DatagramSocket(); //init RTP socket
            video = new VideoStream(VideoFileName); //init the VideoStream object:
            log.info("Sending file: " + VideoFileName);

        } catch (SocketException e) {
            log.error(e);
        } catch (Exception e) {
            log.error(e);
        }

        //Handler to close the main window
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //stop the timer and exit
                sTimer.stop();
                System.exit(0);
            }
        });

        //GUI:
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

        sTimer.start();
    }



    //------------------------
    //Handler for timer
    //------------------------
    public void actionPerformed(ActionEvent e) {

        //if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH)
        {
            //update current imagenb
            imagenb++;

            try {
                //get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(sBuf);

                //Builds an RTPpacket object containing the frame
                Map<String, Set<String>> flows = nm.getRoutingTable(); // get the flows for the packet
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, sBuf, image_length);

                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);

                //send the packet as a DatagramPacket over the UDP socket, to all server neighbours
                if(!flows.isEmpty())
                    for(String destination : flows.get(Constants.SERVER_ID)){
                        InetAddress clientIP = nm.getNodesIPs().get(destination);
                        senddp = new DatagramPacket(packet_bits, packet_length, clientIP, RTP_dest_port);
                        RTPsocket.send(senddp);
                        log.debug("Send frame #" + imagenb);
                        //print the header bitstream
                        //rtp_packet.printheader();

                    }


                //update GUI
                //label.setText("Send frame #" + imagenb);
            }
            catch(Exception ex)
            {
                log.fatal(ex);
                System.exit(0);
            }
        }
        else
        {
            //if we have reached the end of the video file, stop the timer
            sTimer.stop();
        }
    }

    @Override
    //------------------------------------
    //main
    //------------------------------------
    public void run() {
        //get video filename to request:
        //VideoFileName = "src/main/resources/movie.Mjpeg";

        File f = new File(VideoFileName);
        if (f.exists()) {
            Streamer s = new Streamer(VideoFileName, nm);
            //show GUI: (opcional!)
            //s.pack();
            //s.setVisible(true);
        } else
            log.error("Video file not found: " + VideoFileName);
    }
}