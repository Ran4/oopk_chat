import java.io.PrintWriter;
import java.io.IOException;

/**
 * 
 * @author Rasmus Ansin, Joakim Andersson
 * Helper class used when sending messages
 *
 */
public class ServerSender{
    private PrintWriter writer;
    
    public ServerSender(PrintWriter _writer) {
        writer = _writer;
    }
    public void sendMessage(String msg) throws IOException{
        writer.println(msg);
        writer.flush();
    }
}
