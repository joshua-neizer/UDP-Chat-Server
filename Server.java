import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Stack;

// UDP Server thread that interacts with the client
public class Server extends Thread{
    // Attributes used for socket connection
    private DatagramSocket socket;
    private DatagramPacket packet;
    private int serverPort = 7070;
    private byte[] buf = new byte[256];
    private Integer MAX_CLIENTS = 4;
    
    // Attributes used for client information
    private Stack <Integer> clientStack;
    private HashMap<String, String> clientLog;
    private HashMap<String, String> connectionMap;
    private HashMap<String, String> nameMap;
    private HashMap<String, String[]> clientInformation;
    private HashMap<String, String> messageBuffer;
    
    /**
    * Handles all sever side communication with the clients
    */ 
    Server(){
        // Starts the socket connection
        try {
            socket = new DatagramSocket (serverPort);
        } catch (SocketException e) {
            System.out.println("Error starting the server");
            System.exit(1);
        }
        
        // Instantiating all of the maps used in the program
        clientLog = new HashMap<String, String>();
        connectionMap = new HashMap<String, String>();
        nameMap = new HashMap<String, String>();
        clientInformation = new HashMap<String, String[]>();
        messageBuffer = new HashMap<String, String>();

        // Instantiating the client stack
        clientStack = new Stack <Integer>();

        for (int i=MAX_CLIENTS; i > 0; i--)
            clientStack.push(i);

        // Starts the client thread
        start();
    }

    /**
    * Receives datagram packets from a client and returns it
    *
    * @return  DatagramPacket   the datagram packet received from a client
    */ 
    public DatagramPacket receive_packet(){
        // Creates a buffer to take in data
        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);

        // Attempts to receive a packet
        try {
            socket.receive(packet);
        } catch (IOException e) {
            System.out.println("Server> ERROR receiving packet");
            return null;
        }

        // If attempt is successful, returns the packet
        return packet;
    }

    /**
    * Sends datagram packets from to a client
    *
    * @param  data      the data to be sent to a client
    * @param  address   the address of the destination client
    * @param  port      the port of the destination client
    */ 
    public void send_packet(String data, InetAddress address, int port){
        System.out.printf("Outgoing: %s> %s\n", get_ID(address, port), data);

        // Creates a buffer to write out to the client
        buf = new byte[256];
        buf = data.getBytes();
        packet = new DatagramPacket(buf, buf.length, address, port);

        // Attempts to send the packet to the destination client
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Server> ERROR sending packet");
        }
        
    }

    /**
    * Process incoming datagram packets from clients
    *
    * @param  input     the datagram packet coming in from a client
    */ 
    public void process_input(DatagramPacket input){
        // Converts the datagram packet to a string
        String receivedData = new String(input.getData(), 0, input.getLength());

        // Gets the address and port of where the packet came from
        InetAddress address = input.getAddress();
        int port = input.getPort();

        // Attempts to retrieve which established client the packet is from
        // as well as it's potential established connection
        String clientID = get_ID(address, port);  
        String destID = connectionMap.get(clientID);

        System.out.printf("\nIncoming: %s> %s\n", clientID, receivedData);
        

        // Checks to see if an established connection as not been setup
        if (destID == null || connectionMap.get(destID) == null){
            // Exits the program if the exit message is sent
            if (receivedData.equals("_exit")){
                exit_request(address, port);
            } else {
                // Sets up connections between clients
                setup_input(receivedData, address, port, clientID);
            }
        } else {
            // Terminates the connection if an exit message is received
            if (receivedData.equals("_exit"))
                exit_chat(clientID);
            else {
                // Default is to route the message to the client
                route_message(receivedData, clientID);
            }
        }
    }

    /**
    * Protocols for a client before a connection has been established
    *
    * @param  receivedData  the received string data from the client
    * @param  address       the address of the destination client
    * @param  port          the port of the destination client
    * @param  clientID      the client identification for the received data
    */ 
    public void setup_input(String receivedData, InetAddress address, int port, String clientID){
        // Different protocol depending on what message is sent
        switch (receivedData.split("\\s+") [0]) {
            // Server saves the information for a client who wants to establish a connection
            case "connection_request":
                // Denies the connection if there are too many established client connections
                if (clientStack.empty()){
                    send_packet("connection_denied", address, port);
                    return;
                }

                // Adds clients to established clients
                add_client(address, port);
                send_packet("connection_granted", address, port);
                break;
            
            // Sends a list of all active connections on the server
            case "online_request":
                send_packet(get_online_list(clientID), address, port);
                break;

            // Attempts to establish a connection from the source client to a desired client
            case "client_request":
                String requestID = receivedData.split("\\s+") [1];
                
                // Notifies the client if the connection can be made
                if (connection_request(requestID, clientID)){
                    send_packet("waiting...", address, port);
                } else {                    
                    send_packet("connection_failure", address, port);
                }
                break;

            // Notifies a client that a connection is being attempted
            case "client_ack_response":
                // Notifies the client whether the connection setup was successful
                if (connection_setup(receivedData.split("\\s+") [1], clientID)){
                    send_packet("connection_success", address, port);
                } else {
                    send_packet("connection_failure", address, port);
                }
                break;

            // Sets a nickname for a client
            case "nickname":
                String name = receivedData.split("\\s+") [1];
                set_nickname(name, clientID);
                send_packet("success", address, port);
                break;
        }
    }


    /**
    * Retrieves the clientID given an address and port
    *
    * @param  address       the address of the destination client
    * @param  port          the port of the destination client
    * @return the identification of the client with the given address and port
    */ 
    public String get_ID(InetAddress address, int port){
        return clientLog.get(address.toString() + ":" + String.valueOf(port));
    }


    /**
    * Adds the client sever storage
    *
    * @param  address       the address of the destination client
    * @param  port          the port of the destination client
    */ 
    public void add_client(InetAddress address, int port){
        // Gets client information
        String strAddress = address.toString();
        String strPort = String.valueOf(port);
        String clientAP = strAddress + ":" + strPort;

        // Gets the clientID through the most available 
        String clientID = "Client_" + String.valueOf(clientStack.pop());

        // Saves client information through the use of maps
        clientLog.put(clientAP, clientID);
        clientInformation.put(clientID, new String[] {strAddress, strPort});
    }


    /**
    * Closes the client connection with the server
    *
    * @param  address       the address of the destination client
    * @param  port          the port of the destination client
    */ 
    public void exit_request(InetAddress address, int port){
        // Gets client information
        String clientID = address.toString() + ":" + String.valueOf(port);
        String sourceID = get_ID(address, port);

        // Puts the current client position back in available stack
        clientStack.push(Integer.parseInt(sourceID.split("_", 2) [1]));

        // Removes the connection information from the server
        nameMap.remove(sourceID);
        clientLog.remove(clientID);
    }


    /**
    * Closes the client connection with the server
    *
    * @param  sourceID   the client's identification string
    */ 
    public void exit_chat(String sourceID){
        // Gets the ID of the user the client is connected to
        String connectedID = connectionMap.get(sourceID);

        // Removes the associated ID's
        connectionMap.remove(sourceID);
        connectionMap.remove(connectedID);

        // Resets the message buffer
        messageBuffer.remove(sourceID);
        messageBuffer.remove(connectedID);
    }


    /**
    * Returns a list of all of the currently online clients
    *
    * @param  sourceID   the client's identification string
    * @return the list of online clients
    */ 
    public String get_online_list(String sourceID){
        // Instantiates variables
        String onlineList = "";
        int i = 0;

        // Iterates over all online clients adds them to an ongoing string
        for (String client : clientLog.values()){
            if (sourceID != client){
                i++; 

                onlineList += "[" + String.valueOf(i) + "] " + client + " ==> " + 
                nameMap.get(client) +"\n";
            }   
        }

        // Returns a notifier if there are no clients online
        if (i == 0){
            return "Nobody Is Online";
        }

        return onlineList;
    }


    /**
    * Attempts a connection request from the source client to the request client
    *
    * @param  requestID the request client's identification string
    * @param  sourceID  the source client's identification string
    * @return a boolean of whether the connection request was successful
    */ 
    public boolean connection_request(String requestID, String sourceID){ 
        // Denies request if the requested client doesn't exist
        if (clientInformation.get(requestID) == null)
            return false;

        // Denies request if the requested client is already connected to another client
        // Or if the source client connected to another client
        if (connectionMap.get(requestID) != null || connectionMap.get(sourceID) != null){
            if (!connectionMap.get(requestID).equals(sourceID))
                return false;
        }

        // Associates the requested client with the source client
        connectionMap.put(requestID, sourceID);

        // Sends a client acknowledgement message to the requested client
        send_message("client_ack " + nameMap.get(sourceID) + " " +  sourceID, requestID);

        return true;
    }


    /**
    * Attempts to setup the connection between two clients
    *
    * @param  ack        the requested client's response to the connection
    * @param  requestID  the request client's identification string
    * @return a boolean of whether the connection was successful
    */ 
    public boolean connection_setup(String ack, String requestID){
        // Gets the ID of the client who made the request
        String sourceID = connectionMap.get(requestID);

        // Removes the connection if the request client denies the connection
        if (!(ack.equals("1"))){
            // Sends a failure message to the source client's ID
            send_message("connection_failure", sourceID);
            connectionMap.remove(requestID);
            return false;
        }

        // Sets up the connection if the request client accepts the connection
        connectionMap.put(sourceID, requestID);

        // Sends the nickname of the requested client
        send_message(nameMap.get(requestID), sourceID);
        
        return true;
    }

    
    /**
    * Attempts to setup the connection between two clients
    *
    * @param  name      the requested name for the client
    * @param  sourceID  the source client's identification string
    */ 
    public void set_nickname(String name, String sourceID){ 
        // If the user sets a nickname, it is associated with their ID 
        if (name.equals("_null")){            
            nameMap.put(sourceID, sourceID);
        } else {
            nameMap.put(sourceID, name);
        }
    }


    /**
    * Routes the message from the source client to the connected ID
    *
    * @param  message   the message being sent
    * @param  sourceID  the source client's identification string
    */ 
    public void route_message(String message, String sourceID){
        // Gets the ID of the source client's connected client
        String destID = connectionMap.get(sourceID);
        
        // Gets the information of the destination ID.
        String[] destInformation = clientInformation.get(destID);
        InetAddress destAddress = null;

        try {
            destAddress = InetAddress.getByName(destInformation [0].replace("/", ""));
        } catch (Exception e) {
            System.out.println("Error");
            e.printStackTrace();
        }
        
        int destPort = Integer.parseInt(destInformation [1]);

        // Buffers the message if acknowledgement has not been sent
        message = ack_message(message, sourceID, destID);
        
        // Sends the message
        send_packet(message, destAddress, destPort);
    }


    /**
    * Sends the message to the desired Client
    *
    * @param  message   the message being sent
    * @param  destID    the desired client's identification string
    */ 
    public void send_message(String message, String destID){
        // Gets the information of the destination ID.
        String[] destInformation = clientInformation.get(destID);
        InetAddress destAddress = null;

        try {
            destAddress = InetAddress.getByName(destInformation [0].replace("/", ""));
        } catch (Exception e) {
            System.out.println("Error");
            e.printStackTrace();
        }

        int destPort = Integer.parseInt(destInformation [1]);

        // Sends the message
        send_packet(message, destAddress, destPort);
    }


    /**
    * Allows a message to be acknowledged before sent
    *
    * @param  message   the message being sent
    * @param  sourceID  the source client's identification string
    * @param  destID    the desired client's identification string
    * @return the processed message response
    */ 
    public String ack_message(String message, String sourceID, String destID){
        // Checks what type of message is being sent
        if (message.split("\\s+") [0].equals("message_ack")){
            // Buffers the message to send
            messageBuffer.put(sourceID, message.split(" ", 2) [1]); 

            // Sends confirmation message to the client that sent the message
            send_message("ack_sent", sourceID);

            // Checks to see if the current message is an exit message
            if (messageBuffer.get(sourceID).split(" ", 2) [1].equals("_exit"))
                return "message_request _exit";
                
            return "message_request basic";
        } else {
            // If the message is not a message request, it is a message response
            // Sends the buffered message if the message is accept, else it is denied
            if (message.equals("1")){
                send_message(messageBuffer.get(destID), sourceID);
                messageBuffer.put(destID, null); 
                return "ack_received";
            } else {
                send_message("reset", sourceID);
                return "ack_denied";
            }
        }
    }


    /**
    * The main of the thread
    */ 
    public void run(){
        System.out.println("\nStarting the server...");

        // Continuously receives and processes packets
        while (true){
            process_input(receive_packet());
        }
    }


    /**
    * Server main function
    */ 
    public static void main(String[] args) {
        new Server();
    }
    
}
