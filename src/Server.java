import java.io.IOException;
import java.util.ArrayList;


public class Server extends Client {
	private static String startMessage = "You are a server\n";
	
	ConnectionHandler ch;
	private ArrayList<ServerSender> serverSenderList;
	
	public Server() {
		serverSenderList = new ArrayList<ServerSender>();
		
		ch = new ConnectionHandler(8080, this);
		Thread t = new Thread(ch);
		t.start();
	}
	
	@Override
	public String startMessage() {
		return startMessage;
	}
	
	public synchronized void addServerSender(ServerSender serverSender) {
		serverSenderList.add(serverSender);
    }
	
	public synchronized void sendMessage(String msg, ServerSender from) throws IOException{
        System.out.println(msg);
        for(ServerSender to : serverSenderList) {
    		if (from == null || !to.equals(from)) {
    			to.sendMessage(msg);
    		}
        }
        
        //this.cw.addMessage("In server.java, outward message:" + msg);
        System.out.println("In server.java, sent message: " + msg);
    }
}