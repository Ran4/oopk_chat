import java.io.IOException;

public interface RoomInterface {
	public void sendMessage(String xmlMessage) throws IOException;
	public void disconnect();
}