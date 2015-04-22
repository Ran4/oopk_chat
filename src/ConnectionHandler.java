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
            	//server.cw.addMessage("Server accepted a connection");
            	System.out.println("ServerSocket accepted a connection");
            	
                PrintWriter myWriter = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
                
                ServerSender serverSender = new ServerSender(myWriter);
                
                server.addServerSender(serverSender);
                
                //System.out.println("Just added ");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                (new Thread(new ServerReceiver(reader, server, serverSender))).start();
                
                /*
                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String name = reader.readLine();
                (new Thread(new MessageReceiver(name, reader, server, sender))).start();
                */
            }
            ss.close();
        } catch (IOException e) {
            System.out.println("ServerSocket failed");
        }
    }
}
