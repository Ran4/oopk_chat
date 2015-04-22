import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author Rasmus Ansin, Joakim Andersson
 * The server class acts like an extension of the Client class,
 * since the server can also be used like a client. 
 *
 */
public class Server extends Client {
	static {
		startMessage = "You are a server\n";
	}
	
	ConnectionHandler ch;
	private ArrayList<ServerSender> serverSenderList;
	
	public Server() {
		serverSenderList = new ArrayList<ServerSender>();
		
		ch = new ConnectionHandler(8080, this);
		Thread t = new Thread(ch);
		t.start();
	}
	
	public synchronized void addServerSender(ServerSender serverSender) {
		serverSenderList.add(serverSender);
    }
	
	/*
	 * Sends a message to all the stored ServerSenders (but not to the sender)
	 */
	public synchronized void sendMessage(String xmlMessage, ServerSender from) throws IOException{
        //System.out.println(xmlMessage);
        for(ServerSender to : serverSenderList) {
    		if (from == null || !to.equals(from)) {
    			to.sendMessage(xmlMessage);
    		}
        }
        
        //this.cw.addMessage("In server.java, outward message:" + xmlMessage);
        System.out.println("In server.java, sent message: " + xmlMessage);
    }
}