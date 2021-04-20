# UDP-Chat-Server
**Author:** Joshua Neizer
## Project Overview
A UDP socket chat server I created for a computer networking class

## Project Description
A  simple  client-server  communication application (a chat-like service). Detailed requirements are found below. 

## Project Components
<ol>
    <li>When started, the application server must create a UDP socket that listens on port 7070. The server must be able to handle multiple clients concurrently.</li>
    <li>Upon starting, any client must establish a UDP socket and send its port number to the application server at “localhost:7070”. The list of clients that are active on the application and their reachable port numbers must be automatically displayed upon any change (any joining or leaving client) on the Server’s command line (same as the list shown in Figure 1).</li>
    <li>For any client (e.g., Client 1) to establish a connection to another client (e.g., Client 2), it must follow the following procedure:
        <ol type="a">
            <li>User running Client 1 uses the command line to prompt the sending of a message to the server at “localhost:7070” asking about the UDP port number of Client 2.</li>
            <li>If Client 2 is active (the server has a UDP port number for it), the server returns the port number to Client 1. If not, the server returns a “Client 2 Inactive” message to Client 1.</li>
            <li>If Client 1 receives a port number for Client 2, it sends a “Connection Request” message, sets a 10-second timer, and waits for a response. The “Connection Request” message must have a numerical identifier for Client 1 (say no. 1). If no “Connection Accept” nor “Connection Reject” is received from Client 2 within 10 seconds, Client 1 re-sends the “Connection Request” message again and repeats the procedure until it gets one of these two messages.</li>
            <li>Once Client 2 receives the “Connection Request” message, it displays a message on the command line for the user to accept or reject the connection.   
                <ol type="i">
                    <li>If accepted. Client 2 responds with a “Connection Accept” message.</li>
                    <li>If rejected, Client 2 responds with a “Connection Reject” message.</li>
                </ol>
            In either case, the message must have a numerical identifier for Client 2 (say no. 2).
            </li>
        </ol>
    </li>
    <li>When a connection is established between two clients, they can send short messages to one another
    through their command lines, which should be displayed on each other’s command lines. The
    messages sent by each client should include the client’s identifier. The messages must be sequenced incrementally and each of them must be acknowledged by the other party (using its sequence number) within 10 seconds, or else sent again. Duplicate messages (detected by duplicate sequence numbers) must be discarded and not displayed again.</li>
    <li>Either clients can send a “Connection Termination” message to the other, which if acknowledged,
    must result in connection termination (again displayed in the command line).</li>
    <li>To quit the application, a client must send a “Client Leave” message to the server. Once received the server erases the UDP port number from its table and sends an acknowledgement to the client. Only then, the client executes a socket close command. </li>
</ol>