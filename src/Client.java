import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class Client implements RoomInterface {
	private RoomViewer roomViewer;
	private Socket s;
	private BufferedReader reader;
	private PrintWriter writer;
	public String ip;
		
	public Client(RoomViewer _roomViewer, String _ip, int port) throws IOException {
		roomViewer = _roomViewer;
		ip = _ip;
		
		s = new Socket(ip, port);
		
		reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
		beginReceiverThread();
		
		writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
				
		if (reader == null) throw new IOException("Couldn't creater a reader");
		if (writer == null) throw new IOException("Couldn't create a writer");
	}
	
	private void beginReceiverThread() {
		new Thread(new Runnable() {
		    public void run() {
		        try {
		            String xmlMessage;
		            while( (xmlMessage = reader.readLine()) != null) {
		                roomViewer.receiveMessage(xmlMessage);
		            }
		        } catch (SocketException e) {
		        	if (!e.getMessage().equals("Socket closed")) e.printStackTrace();
		        } catch (IOException e) {
		        	e.printStackTrace();
		        }
		    }
		}).start();
	}
	
	public void sendMessage(String xmlMessage) {
		if (s == null || !s.isConnected()) {
			System.out.println("Client is talking to itself...");
		} else {
			if (writer == null) {
				System.out.println("writer is null");
				roomViewer.addPlainMessage("Couldn't send to server (writer is null)");
			} else {
				writer.println(xmlMessage);
		        writer.flush();
		        
		        System.out.println("In Client.java, sent message: " + xmlMessage);
			}
		}
	}

	public void disconnect() {
		try {
			if (s != null) s.close();
			if (reader != null) reader.close();
			if (writer != null) writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}