import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;

/**
 *
 * @author Rasmus Ansin, Joakim Andersson
 * Receives incoming messages from a client and sends them onward if needed
 *
 */
public class ServerReceiver implements Runnable{
    private BufferedReader reader;
    private Server server;
    private ServerSender serverSender;

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
                }
            }
        } catch (SocketException e) {
        	if (!e.getMessage().equals("Socket closed")) e.printStackTrace();
        } catch (IOException e) {
        	if (!e.getMessage().equals("Stream closed")) e.printStackTrace();
        }
    }
}