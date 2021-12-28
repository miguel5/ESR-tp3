package streaming;


/* ------------------
   Cliente
   usage: java Cliente
   adaptado dos originais pela equipa docente de ESR (nenhumas garantias)
   colocar o cliente primeiro a correr que o servidor dispara logo!
---------------------- */

import master.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Client implements Runnable {

  Logger log = LogManager.getLogger(Client.class);

  //GUI
  //----
  JFrame f = new JFrame("Cliente de Testes");
  JButton setupButton = new JButton("Setup");
  JButton playButton = new JButton("Play");
  JButton pauseButton = new JButton("Pause");
  JButton tearButton = new JButton("Teardown");
  JPanel mainPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JLabel iconLabel = new JLabel();
  ImageIcon icon;


  //RTP variables:
  //----------------
  DatagramPacket rcvdp; //UDP packet received from the server (to receive)
  DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet
  static int RTP_RCV_PORT = Constants.STREAMING_PORT; //port where the client will receive the RTP packets
  StreamRelay sr;
  
  Timer cTimer; //timer used to receive data from the UDP socket
  byte[] cBuf; //buffer used to store data received from the server 
 
  //--------------------------
  //Constructor
  //--------------------------
  public Client(StreamRelay sr) {

    this.sr = sr;

    //build GUI
    //--------------------------
 
    //Frame
    f.addWindowListener(new WindowAdapter() {
       public void windowClosing(WindowEvent e) {
	 System.exit(0);
       }
    });

    //Buttons
    buttonPanel.setLayout(new GridLayout(1,0));
    buttonPanel.add(setupButton);
    buttonPanel.add(playButton);
    buttonPanel.add(pauseButton);
    buttonPanel.add(tearButton);

    // handlers... (so dois)
    playButton.addActionListener(new playButtonListener());
    tearButton.addActionListener(new tearButtonListener());

    //Image display label
    iconLabel.setIcon(null);
    
    //frame layout
    mainPanel.setLayout(null);
    mainPanel.add(iconLabel);
    mainPanel.add(buttonPanel);
    iconLabel.setBounds(0,0,380,280);
    buttonPanel.setBounds(0,280,380,50);

    f.getContentPane().add(mainPanel, BorderLayout.CENTER);
    f.setSize(new Dimension(390,370));
    f.setVisible(true);

    //init para a parte do cliente
    //--------------------------
    cTimer = new Timer(20, new clientTimerListener());
    cTimer.setInitialDelay(0);
    cTimer.setCoalesce(true);
    cBuf = new byte[15000]; //allocate enough memory for the buffer used to receive data from the server

    try {
      // socket e video
      // RTPsocket = new DatagramSocket(RTP_RCV_PORT); //init RTP socket (o mesmo para o cliente e servidor)
      RTPsocket = sr.getSocket();
      RTPsocket.setSoTimeout(5000); // setimeout to 5s
    } catch (SocketException e) {
        log.error(e);
    }
  }

  //------------------------------------
  //main
  //------------------------------------

  @Override
  public void run() {
    Client t = new Client((this.sr));
  }


  //------------------------------------
  //Handler for buttons
  //------------------------------------

  //Handler for Play button
  //-----------------------
  class playButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){
      log.trace("Play Button pressed !");
      //start the timers ...
      cTimer.start();
    }
  }

  //Handler for tear button
  //-----------------------
  class tearButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){
      log.trace("Teardown Button pressed !");
      //stop the timer
      cTimer.stop();
      //exit
      System.exit(0);
    }
  }

  //------------------------------------
  //Handler for timer (para cliente)
  //------------------------------------
  
  class clientTimerListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      
      //Construct a DatagramPacket to receive data from the UDP socket
      rcvdp = new DatagramPacket(cBuf, cBuf.length);

      try{
        //receive the DP from the socket:
        RTPsocket.receive(rcvdp);

        // Send packet to other nodes asynchronously
        new Thread(() -> {
          try {
            sr.relay(rcvdp);
          } catch (IOException ex) {
            log.error(ex);
            ex.printStackTrace();
          }
        });

        //create an RTPpacket object from the DP
        RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

        //print important header fields of the RTP packet received:
        log.info("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());

        //print header bitstream:
        //rtp_packet.printheader();

        //get the payload bitstream from the RTPpacket object
        int payload_length = rtp_packet.getpayload_length();
        byte [] payload = new byte[payload_length];
        rtp_packet.getpayload(payload);

        //get an Image object from the payload bitstream
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.createImage(payload, 0, payload_length);

        //display the image as an ImageIcon object
        icon = new ImageIcon(image);
        iconLabel.setIcon(icon);
      }
      catch (InterruptedIOException iioe){
	    log.info("Nothing to read");
      }
      catch (IOException ioe) {
	    log.error(ioe);
      }
    }
  }
}

