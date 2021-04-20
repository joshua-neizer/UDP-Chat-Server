import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

// UDP Client process that interacts with the server and other clients
public class Client {
    // Attributes used for socket connection
    private DatagramSocket socket;
    private DatagramPacket packet;
    private int serverPort = 7070;
    private String serverName = "localhost";
    private boolean active;
    private byte[] buf = new byte[256];
    private InetAddress address;
    private String receivedData;
    private Scanner dataInput;
    private int TIMER = 10*1000;

    // Attributes used for client information
    String[] options;
    String name = "You";
    String friend;
    String server_response = "";
    String prev_message_in = "";
    String message_in = "";
    String message_out = "";
    int seq;
    Boolean send_error = false;
    Boolean initiator = false;
    Boolean activeConnection;

    /**
    * Handles all client side communication with the server and other clients
    */ 
    Client(){
        // Starts the socket connection
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName(serverName);
            socket.setSoTimeout(TIMER);
        } catch (Exception e) {
            //TODO: handle exception
        }

        // Sets the flag that connection 
        active = true;

        // Starts the scanner
        dataInput = new Scanner(System.in);
        
        // Runs the client instance
        run();
    }


    /**
    * Stalls the instance for the user
    *
    * @param  wait   how long the program will stall for
    */ 
    public void stall(int wait){
        try {
            TimeUnit.SECONDS.sleep(wait);
        } catch (Exception e) {}
    }


    /**
    * Receives datagram packets from a client and returns it
    *
    * @return  DatagramPacket   the datagram packet received from a client
    */ 
    public String receivePacket(){
        // Creates a buffer to take in data
        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);

        // Attempts to receive a packet
        try {
            socket.receive(packet);
        } catch (IOException e) {
            send_error = true;
            return "connection_failure";
        }

        // Processes the packet as string
        receivedData = new String(packet.getData(), 0, packet.getLength());

        // Processes the received data and returns it
        return request_check(receivedData);
    }



    /**
    * Sends datagram packets from to a client
    *
    * @param  data      the data to be sent to a client
    */ 
    public void sendPacket(String data){
        // Creates a buffer to write out to the client
        buf = new byte[256];
        buf = data.getBytes();
        packet = new DatagramPacket(buf, buf.length, address, serverPort);

        // Attempts to send the packet to the destination client
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Server> ERROR sending packet");
        }    
    }


    /**
    * Gets input from the user
    *
    * @return  the user input
    */ 
    public String get_input(){
        System.out.print("\n" + name + "> ");
        return dataInput.nextLine();
    }

    /**
    * Clears the terminal screen
    */ 
    public static void clear_screen() {  
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();  
            } else {
                new ProcessBuilder("/bin/bash", "-c", "clear").inheritIO().start().waitFor();  
            }
        } catch (final Exception e) {
            System.out.printf("\n\n\n\n");
        } 
    } 


    /**
    * Setup for the user to connect to the server
    */ 
    public void setup(){
        String response;
        Boolean notConnected = true;

        // Attempts to connect to the server
        System.out.println("Connecting to the server...");
        while (notConnected){
            sendPacket("connection_request");
            response = receivePacket();

            // If connections is not made, attempts another connection in 10 seconds
            if (response.equals("connection_granted")){
                notConnected = false;
            } else {
                System.out.println("\nServer is busy, attempting to reconnect in 10 seconds...");
                stall(10);
            }            
        }

        System.out.println("\nConnection successful!");

        // Stalls so the user can see the connection was successful
        stall(1);

        clear_screen();

        // Sets the nickname for the user, and sends it to the server
        System.out.println("Welcome to UPD Server");
        System.out.println("What display name do you want?");
        name = get_input().split("\\s+") [0];
        if (name.equals("")){
            name = "You";
            sendPacket("nickname _null");
        } else {
            sendPacket("nickname " + name);
        }
            
        
        receivePacket();
    }


    /**
    * Request list of active clients from the server
    *
    * @return the list of online clients
    */ 
    public String fetch_online_list(){
        System.out.println("Clients Online:");
        sendPacket("online_request");
        String response = receivePacket();

        // Ensures nobody is attempting to connect with client
        if (!response.equals("_chat")){
            // Saves all of online clients into a list
            options = response.split("\\r?\\n");
            System.out.println(response);
        }
        
        return response;
    };
    

    /**
    * Allows the user to connect with online clients in the server
    *
    * @return the user's decision within the connection room
    */ 
    public String connect_room(){
        String input;
        int index;
        String response = "connection_failure";

        // Iterates until either a connection is made or the user goes back to the menu
        while(response.equals("connection_failure")){
            clear_screen();
            System.out.println("Welcome to the Connect Room!");
            System.out.println("Enter '_back' to exit to the menu");

            // Gets online list
            response = fetch_online_list();

            // Iterates until either a client is online or the user goes back to the menu
            while (response.equals("Nobody Is Online")){
                // Checks to see if a connection request to the client has been made
                if (response.equals("_chat"))
                    return response;

                System.out.println("\nPress enter to refresh the list");
                input = get_input();
                
                // Allows the user to return to the menu
                if (input.equals("_back")){
                    return input;
                }
                
                clear_screen();
                System.out.println("Welcome to the Connect Room!");
                System.out.println("Enter '_back' to exit to the menu");
                response = fetch_online_list();
            }

            // Checks to see if a connection request to the client has been made
            if (response.equals("_chat")){
                receivePacket();
                return response;
            }
                

            response = "connection_failure";
        
            System.out.println("\nWho would you like to connect to?");
            input = get_input();

            // Allows the user to return to the menu
            if (input.equals("_back"))
                return input;
            
            // Error check to see if the user entered an integer
            try {
                index = Integer.parseInt(input) - 1;
            } catch (Exception e) {
                System.out.println("Invalid Input: please enter a valid integer");
                stall(2);
                continue;
            }

            // Error check to see if the user entered a valid user
            if (index < 0 || index >= options.length ){
                System.out.println("Invalid Input: please enter a valid integer");
                stall(2);
                continue;
            }

            // Attempts to make a connection request to the desired client
            response = connect_request(options[index].split("\\s+") [1]);

            // Checks to see if a connection request to the client has been made
            if (response.equals("_chat"))
                return response;
            else if (response.equals("connection_failure")){
                // Notifies the user the connection failed 
                System.out.println("\nRequest Denied");
                stall(3);
            }
        }

        // Sets the friend attribute
        friend = response;

        // Allows the client to know they initiated the connection
        initiator = true;

        System.out.println("\nConnection successful!");
        stall(2);

        return "_chat";
    }


    /**
    * Allows the user to wait to be connected to
    *
    * @return the user's decision within the connection room
    */ 
    public String waiting_room(){
        String input;
        String response;
       
        clear_screen();
        System.out.println("Welcome to the Waiting Room!");
        System.out.println("Enter '_back' to exit to the menu");

        // Iterates until the user chooses to leave by going back or accepting a connection
        while (true){
            System.out.println("\nWaiting to be connected with...");
            response = receivePacket();

            // Checks to see if a connection request to the client has been made
            if (response.equals("_chat")){
                return response;
            } else if (response.equals("_denied")){
                // Allows the user to see that they denied a connection
                System.out.println("\nConnection Denied");
                System.out.println("Waiting to be connected with...");
            }

            // Checks to see if a connection request to the client has been made
            response = receivePacket();
            if (response.equals("_chat"))
                return response;

            // The user is given an option to return the menu if they input '_back'
            System.out.println("\nType '_back' to return to menu or press enter to continue waiting");
            input = get_input();
            if (input.equals("_back"))
                return input;
        }
    }


    /**
    * Menu that allows the user to connect interact with the software
    */ 
    public void menu(){
        String input = "";
        String route;
        Boolean validInput = false;
        
        // Iterates until the user has made a decision
        while (true){
            System.out.println("Type '_exit' to exit the program");
            System.out.println("\nDo you want to be placed in a waiting room or connect room?");
            System.out.printf("Enter 1 for 'connect room', 0 for 'wait room'\n");
            
            // Error checks user input
            while (!validInput){
                input = get_input();

                // Checks to see if the user wants to exit the program
                if (input.equals("_exit")){
                    sendPacket(input);
                    active = false;
                    return;
                }

                // Restricts the inputs to be either '0' or '1'
                if (input.equals("0") || input.equals("1")){
                    validInput = true;
                } else {
                    System.out.println("Invalid Input: Enter 1 for 'connect room', 0 for 'wait room'");
                }
            }

            // Routes the connection depending on the user input
            route = input.equals("1") ? connect_room() : waiting_room();
            
            // Returns to the chat if the user has initiated a chat
            if (route.equals("_chat"))
                return;

            // Resets variables if the user chooses to come back to the menu
            validInput = false;
            clear_screen();
            System.out.println("Welcome Back!\n");
        }
    }


    /**
    * Attempts a connection with desired client
    *
    * @param  desiredID   the desired clientID
    * @return the server response to the connection
    */ 
    public String connect_request(String desiredID){
        String response;

        // Sends the connection request of the user to the server
        sendPacket("client_request " + desiredID);
        System.out.println("\nWaiting for their response...");

        // Server waits for the response, the connection is terminated if the
        // user doesn't respond in 10 seconds
        response = receivePacket();
        if (response.equals("connection_failure")){
            return response;
        }
        
        return receivePacket();
    }

    
    /**
    * Handles request attempts made to the client
    *
    * @param  request   the incoming client request
    * @return the server response to the connection
    */ 
    public String request_check(String request){
        String input = "0";
        Boolean validInput = false;
        String[] response = request.split("\\s+");
        long stop;

        // If the incoming server data isn't a client request, it is returned
        if (!(response [0]).equals("client_ack"))
            return request;

        // Sets a timer for user response
        stop = System.currentTimeMillis() + TIMER;

        System.out.printf("\n\n%s (%s) is requesting to connect with you\n", response [1], response [2]);
        System.out.printf("Do you accept the request? \n1 for 'accept', 0 for 'reject'\n");
        
        // Validates user input to be one of the menu choices
        while (!validInput){
            input = get_input();

            if (input.equals("0") || input.equals("1")){
                // If the user doesn't respond within 10 seconds of receiving the
                // request, the connection is terminated.
                if (System.currentTimeMillis() > stop){
                    System.out.println("\nRequest timed out");
                    return "_denied";
                }

                // Sends a message to the server of user's response
                sendPacket("client_ack_response " + input);
                validInput = true;
            } else {
                System.out.println("Invalid Input: please 1 for accept, or 0 for reject");
            }
        }
        
        // Returns a denied flag if the user denies the connection
        if (input.equals("0")){
            receivePacket();
            return "_denied";
        }

        // Saves the connected client's information if they are connected
        friend = response [1];
        receivePacket();
        System.out.println("\nConnection successful!");
        stall(2);
        return "_chat";
    }


    /**
    * Handles all messages sent to the server during chat communications
    */ 
    public void sendChat(){
        String response;

        // Gets user input
        message_out = get_input();

        // Iterates until the sent message is acknowledged
        while (true){
            // Sends message to the server
            sendPacket("message_ack " + String.valueOf(seq) + " " + message_out);
            
            // Receives acknowledgement from the serer
            response = receivePacket();

            // Receives client response to message request from the server
            response = receivePacket();
            
            // Exits loop and displays client's response if the message is
            // acknowledged within 10 seconds
            if (!response.equals("connection_failure")){
                // Allows the user to know whether their message was accepted
                if (response.equals("ack_received")){
                    System.out.println("Server> Message Received");

                    // If the messaged was accepted, checks to see if the user requested to exit
                    if (message_out.equals("_exit")){
                        sendPacket("_exit");
                        activeConnection = false;
                    }
                }
                    
                else
                    System.out.println("Server> Message Denied");

                break;
            }        
        }
    }


    /**
    * Handles all messages received from the server during chat communications
    */ 
    public void receiveChat(){
        String input;
        message_in = receivePacket();

        // Iterates until a proper message is received from the server
        while(message_in.equals("connection_failure"))
            message_in = receivePacket();
        
        // Discards message if it is a duplicate
        if (!message_in.equals(prev_message_in)){
            
            // Checks to see if the message incoming is a message request
            if (message_in.split("\\s+") [0].equals("message_request")){
                prev_message_in = message_in;
                
                System.out.println("\nYou have just received a message request!");

                // If exit message is received, notice is sent to the user
                if (message_in.split("\\s+") [1].equals("_exit"))
                    System.out.println("NOTE: Client is requesting connection termination");

                // Iterates until the user inputs a valid response
                while (true){
                    System.out.println("\nEnter 1 to 'accept', 0 for 'reject'");
                    System.out.print("Response> ");
                    input = dataInput.nextLine();

                    if (input.equals("0") || input.equals("1")){
                        break;
                    } else {
                        System.out.println("Invalid Input: Enter 1 to 'accept', 0 for 'reject'");
                    }
                }

                // Sends user response ot the server
                sendPacket(input);
                message_in = receivePacket();

                // Iterates until all duplicate messages are discarded
                while(message_in.equals(prev_message_in))
                    message_in = receivePacket();

                // Displays the message if the user accepts the message
                if (input.equals("1")){
                    // Terminates chat if the user accepts exit message
                    if (message_in.split(" ", 2) [1].equals("_exit")){
                        System.out.println("Server> Connection termination");
                        activeConnection = false;
                        stall(3);
                    }

                    // Displays the message to the user
                    System.out.printf("\n%s> %s\n", friend, message_in.split(" ", 2) [1]);

                    // Increments the sequence number based on the message
                    seq = Integer.parseInt(message_in.split("\\s+") [0]) + 1;
                }

                // Resets the previous message
                prev_message_in = message_in;
            }
        }
    }
    

    /**
    * Runs the client instance
    */ 
    public void run(){
        // Sets up the connection with server
        setup();
        
        // Iterates until the user exits the program through the menu
        while (active){
            // Goes to the menu for the user
            clear_screen();
            menu();

            if (!active){
                break;
            }

            clear_screen();

            // Shows the chat room for the user
            System.out.printf("___Chatroom with %s___\n", friend);
            System.out.println("Type '_exit' to terminate connection");

            seq = 0;
            prev_message_in = "";

            activeConnection = true;
            // Starts the client that initiated the connection as the first sender
            if (initiator)
                sendChat();

            // Iterates until a exit request is acknowledged and accepted by both users
            while(activeConnection){
                receiveChat();

                if (!activeConnection)
                    break;

                sendChat();
            }

            initiator = false;
        }
    }
    

    /**
    * Client main function
    */ 
    public static void main(String[] args) {
        new Client();
    }
}
