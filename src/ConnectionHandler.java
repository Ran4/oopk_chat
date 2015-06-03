import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionHandler implements Runnable {
    int port;
    Server server;

    public ConnectionHandler(int _port, Server _server) {
        port = _port;
        server = _server;
    }
    
    @Override
    public void run(){
    	
    	System.out.println("In ConnectionHandler.java, run() method");
    	
        try {
            ServerSocket ss = new ServerSocket(port);
            Socket s = null;
            while ( (s = ss.accept()) != null) {
            	System.out.println("ServerSocket accepted a connection");
            	
            	BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            	
            	try {
                    String xmlMessage = "";
                    while ( (xmlMessage = reader.readLine()) != null && !xmlMessage.isEmpty()) {
                    	Message msg = null;
                    	try {
                    		msg = XMLParser.XMLToMsg(xmlMessage);
                    	} catch (XMLException e) {
                    		throw new Error("Shitty xml!");
                    	} catch (UnsupportedEncryptionTypeException e) {
                    		throw new Error("Encryption isn't implemented yet!");
                    	} catch (EncryptionException e) {
                    		throw new Error("Encryption isn't implemented yet!");
                    	}
                    	
                    	String sender = msg.sender.isEmpty() ? "Some unnamed thing" : msg.sender;
                    	String appeal = sender + " is trying to connect. It is a " + (msg.isConnectionRequest() ? "pro" : "noob")
                    			+ ". Message: " + msg.text;
                    	
                    	PrintWriter myWriter = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
                    	ServerSender serverSender = new ServerSender(myWriter);
                    	
                    	if (server.askHostToAcceptAppeal(appeal)) {
                            server.addServerSender(serverSender);
                            (new Thread(new ServerReceiver(reader, server, serverSender))).start();
                    		
                    	} else try {
                    			serverSender.sendMessage(XMLParser.msgToXML(Message.createConnectionResponse(true, "")));
                    	} catch (Exception e) {
                    	}
                    	break;
                    }
                } catch (IOException e) {
                    System.out.println("Something went wrong with BufferedReader");
                }
            }
            ss.close();
        } catch (IOException e) {
            System.out.println("ServerSocket failed");
        }
    }
}
