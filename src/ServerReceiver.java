import java.io.BufferedReader;
import java.io.IOException;

/**
 * 
 * @author Rasmus Ansin, Joakim Andersson
 * Receives incoming messages from a client and sends them onward if needed
 *
 */
public class ServerReceiver implements Runnable{
    BufferedReader reader;
    Server server;
    ServerSender serverSender;
    
    public ServerReceiver (BufferedReader _reader, Server _server, ServerSender _serverSender) {
        reader = _reader;
        server = _server;
        serverSender  = _serverSender;
    }

    @Override
    public void run() {
        try {
            String message = "";
            while( (message = reader.readLine()) != null) {
                if(message.length() != 0){
                    server.sendMessage(message, serverSender);
                    server.cw.addMessage(message);
                }
                
            }
        } catch (IOException e) {
            System.out.println("Something went wrong with BufferedReader");
        }
        
    }

}
