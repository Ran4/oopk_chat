import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Client {
	ChatWindow cw;
	Socket s;
	Thread clientReaderThread;
	BufferedReader reader;
	PrintWriter writer;
	
	protected static String startMessage;
	static {
		startMessage = "write /join ip:port to connect\n";
	}
	
	public Client() {
		cw = new ChatWindow(this);
	}
	
	public boolean askHostToAcceptAppeal(String appeal) {
		Object[] options = {"Accept", "Deny"};
		int choice = JOptionPane.showOptionDialog(cw, appeal,
				"Connection request",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null, //no custom icon 
				options, options[0]);
				
		if (choice == JOptionPane.YES_OPTION) {
			System.out.println("Host accepted connection");
			return true;
		} else if (choice == JOptionPane.NO_OPTION) {
			System.out.println("Host didn't accept connection");
			return false;
		} else {
			System.out.println("Host canceled connection request, so no connection was done");
			return false;
		}
	}
	
	public String startMessage() {
		return startMessage;
	}
	
	public void sendMessage(String xmlMessage) {
		if (s == null || !s.isConnected())
		{
			//textArea.append("Isn't connected to any server. Write /join ip:port" + "\n");
			System.out.println("Client is talking to itself...");
		} else {
			if (writer == null) {
				System.out.println("writer is null");
				cw.addMessage("Couldn't send to server (writer is null)");
			} else {
				writer.println(xmlMessage);
		        writer.flush();
		        
		        cw.addMessage("Successfully sent to server");
			}
		}
	}
	
	public void addConnection(String ip, int port) throws UnknownHostException, IOException {
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
