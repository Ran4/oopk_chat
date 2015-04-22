import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author Rasmus Ansin, Joakim Andersson
 * The server class acts like an extension of the Client class,
 * since the server can also be used like a client. 
 *
 */

public class Server {
	
	ConnectionHandler ch;
	//messageReceiverHandler mrh;
	private ArrayList<ServerSender> serverSenderList;
	private Client host;
	
	public Server(Client _host, int port) {
		host = _host;
		serverSenderList = new ArrayList<ServerSender>();
		
		ch = new ConnectionHandler(port, this);
		Thread t = new Thread(ch);
		t.start();
	}
	
	public boolean askHostToAcceptAppeal(String appeal) {
		return (host.askHostToAcceptAppeal(appeal));
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