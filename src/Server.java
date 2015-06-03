import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Rasmus Ansin, Joakim Andersson
 * The server class acts like an extension of the Client class,
 * since the server can also be used like a client. 
 *
 */

public class Server implements RoomInterface {
	private RoomViewer roomViewer;
	private ConnectionHandler ch;
	private ArrayList<ServerSender> serverSenderList;
	private List<String> ipList;
	private int port;
	
	public Server(RoomViewer _roomViewer, int _port) {
		roomViewer = _roomViewer;
		port = _port;
		
		serverSenderList = new ArrayList<ServerSender>();
		ipList = new ArrayList<String>();
		
		createConnectionHandler();
	}
	
	public void createConnectionHandler() {
		ch = new ConnectionHandler(port, this);
		new Thread(ch).start();
	}
	
	public void connectionHandlerFailed(boolean initFailed) {
		ch = null;
		roomViewer.connectionHandlerFailed(initFailed);
	}
	
	public boolean connectionApprovedByHost(String appeal) {
		appeal += "\nID of user: " + serverSenderList.size();
		return roomViewer.connectionApprovedByHost(appeal);
	}
	
	public synchronized void addClient(ServerSender serverSender, String ip) {
		serverSenderList.add(serverSender);
		ipList.add(ip);
    }
	
	/*
	 * Sends a message to all the stored ServerSenders (but not to the sender)
	 */
	public synchronized void sendMessage(String xmlMessage, ServerSender from) throws IOException {
		try {
			Message msg = XMLParser.XMLToMsg(xmlMessage);
			if (msg.isDisconnect()) {
        		if (from != null) boot(from);
			} else if (msg.isFileRequest() || msg.isFileResponse()) {
				roomViewer.receiveMessage(xmlMessage, serverSenderList.indexOf(from));
				return;
			}
		} catch (XMLException e) {}
		
        for(ServerSender to : serverSenderList)
        	if (to != null && !to.equals(from))
        		to.sendMessage(xmlMessage);
        
        if (from != null)
        	roomViewer.receiveMessage(xmlMessage, serverSenderList.indexOf(from));
        
        System.out.println("In Server.java, sent message: " + xmlMessage);
    }
	
	public void sendMessage(String xmlMessage) throws IOException {
		sendMessage(xmlMessage, null);
	}
	
	public void sendFileMessage(String xmlMessage, int index) throws IOException {
		System.out.println("In Server.java, sent file message: " + xmlMessage);
		serverSenderList.get(index).sendMessage(xmlMessage);
	}
	
	/**
	 * Returns a list of all the id:s of serverSenders that are active
	 */
	public List<Integer> getActiveServerSenderIndexes() {
		List<Integer> indexes = new ArrayList<Integer>();
		for (int i = 0; i < serverSenderList.size(); i++)
			if (serverSenderList.get(i) != null)
				indexes.add(i);
		
		return indexes;
	}
	
	public String getIP(int id) {
		return ipList.get(id);
	}
	
	/**
	 * Takes an id and boots that id from the server
	 */
	public void boot(int id) {
		if (ch == null) return;
		ch.boot(id);
		serverSenderList.set(id, null);
		ipList.set(id, null);
	}
	
	/**
	 * Boots a certain ServerSender by finding it's id and using that with the boot(int id) method 
	 * @param userToBoot
	 */
	private void boot(ServerSender userToBoot) {
		boot(serverSenderList.indexOf(userToBoot));
	}
	
	public void disconnect() {
		if (ch != null) ch.close();
	}
}