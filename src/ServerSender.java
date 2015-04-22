import java.io.PrintWriter;
import java.io.IOException;


public class ServerSender{
    private PrintWriter writer;
    
    public ServerSender(PrintWriter _writer) {
        writer = _writer;
        //System.out.println("In ServerSender constructor, has just set writer to writer");
    }
    public void sendMessage(String msg) throws IOException{
        writer.println(msg);
        writer.flush();
    }
}
