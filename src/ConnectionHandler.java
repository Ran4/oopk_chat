import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectionHandler implements Runnable {
	private int port;
	private Server server;
	private ServerSocket ss;
	
	private List<Socket> socketList;
	private List<BufferedReader> readerList;
	private List<PrintWriter> writerList;

	public ConnectionHandler(int _port, Server _server) {
		port = _port;
		server = _server;
		
		socketList = new ArrayList<Socket>();
		readerList = new ArrayList<BufferedReader>();
		writerList = new ArrayList<PrintWriter>();
	}
	
	public void close() {
		if (ss != null) try {
			ss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < socketList.size(); i++) boot(i);
	}
	
	public void boot(int id) {
		try {
			if (socketList.get(id) != null) socketList.get(id).close();
			if (readerList.get(id) != null) readerList.get(id).close();
			if (writerList.get(id) != null) writerList.get(id).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		socketList.set(id,  null);
		readerList.set(id,  null);
		writerList.set(id,  null);
	}

	@Override
	public void run() {
		try {
			ss = new ServerSocket(port);
		} catch (IOException e) {
			server.connectionHandlerFailed(true);
			return;
		}
		
		try {
			Socket s = null;
			while ( (s = ss.accept()) != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
				newConnection(s, reader, writer, s.getInetAddress().toString().substring(1));
			}
			ss.close();
		} catch (IOException e) {
			server.connectionHandlerFailed(false);
		}
	}
	
	public void newConnection(Socket s, BufferedReader reader, PrintWriter writer, String ip) {
		String xmlMessage;
		try {
			xmlMessage = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		if (xmlMessage == null) return;
		
		String appeal;
		Message msg = null;
		try {
			msg = XMLParser.XMLToMsg(xmlMessage);
			appeal = (msg.sender.isEmpty() ? "Some unnamed mysterious being" : msg.sender) + ", who is a "
					 + (msg.isConnectionRequest() ? "pro" : "noob") + ", is trying to connect. ";
			if (msg.decryptionFailed()) appeal += "There was a problem decrypting the message.";
			else if (msg.encryptionTypeUnsupported()) appeal += "The encryption type of the message is unsupported.";
			else appeal += "Message: " + msg.text;
		} catch (XMLException e) {
			appeal = "Someone tried to connect, but the XML couldn't be parsed: " + xmlMessage;
		}

		ServerSender serverSender = new ServerSender(writer);

		if (server.connectionApprovedByHost(appeal)) {
			socketList.add(s);
			readerList.add(reader);
			writerList.add(writer);
			server.addClient(serverSender, ip);
			(new Thread(new ServerReceiver(reader, server, serverSender))).start();
		} else try {
			Message reMsg = (msg != null && msg.isConnectionRequest()) ?
					Message.createConnectionResponse("")
					: Message.createMessage("Connection request denied", "", Color.BLACK, "");
			serverSender.sendMessage(XMLParser.msgToXML(reMsg));
		} catch (EncryptionException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (UnsupportedEncryptionTypeException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}