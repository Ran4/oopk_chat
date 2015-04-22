import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	private static String startMessage = "write /join ip:port to connect\n"; 
	ChatWindow cw;
	Socket s; 
	Thread clientReaderThread;
	BufferedReader reader;
	PrintWriter writer;
	
	public Client() {
		cw = new ChatWindow(this);
	}
	
	public String startMessage() {
		return startMessage;
	}
	
	public void sendMessage(String message) {
		if (s == null || !s.isConnected())
		{
			//textArea.append("Isn't connected to any server. Write /join ip:port" + "\n");
			System.out.println("Client is talking to itself...");
		} else {
			if (writer == null) {
				System.out.println("writer is null");
				cw.addMessage("Couldn't send to server (writer is null)");
			} else {
				writer.println(message);
		        writer.flush();
		        
		        cw.addMessage("Successfully sent to server");
			}
		}
	}
	
	public void addConnection(String ip, int port)  throws UnknownHostException, IOException {
		s = new Socket(ip, port);
		
		reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
		
		Thread clientReaderThread = new Thread(new Runnable() {
		    public void run() {
		        try {
		            String message;
		            while( (message = reader.readLine()) != null) {
		                System.out.println("got message" + message);
		                cw.addMessage(message);
		            }
		        } catch (IOException e) {
		            System.out.println("Something went wrong with BufferedReader");
		        }
		    }
		});
		
		clientReaderThread.start();
		
		writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
		
		if (reader == null) {
			System.out.println("Couldn't create a reader");
		}
		if (writer == null) {
			System.out.println("Couldn't create a writer");
		}
	}
	
	@SuppressWarnings("deprecation")
	public void closeConnection() throws IOException {
		try {
			if (s != null)
				s.close();
			/*
			if (reader != null && reader.ready()) {
				//reader.close();
				
			}
			
			if (writer != null) {
				writer.close();
			}
			
			if (clientReaderThread.isAlive())
				clientReaderThread.stop();
			*/
			
			System.out.println("Done closing connection");
			
		} catch (IOException e) {
			System.out.println("Problems closing socket");
		}
	}
}