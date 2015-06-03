import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
/**
 * A room view that takes user input and shows output to the user.
 *
 */
public class RoomViewer extends JPanel implements ActionListener, KeyListener {
	private static class Command {
		public static String host = "/host ";
		public static String hostNoArgument = "/host";
		public static String join = "/join ";
		public static String joinNoArgument = "/join";
		public static String reconnect = "/reconnect";
		
		public static String boot = "/boot ";
		public static String bootNoArgument = "/boot";
		public static String disconnect = "/disconnect";
		public static String exit = "/exit";
		public static String quit = "/quit";
		
		public static String changeName = "/name ";
		public static String seeName = "/name";

		public static String changeColor = "/color ";
		public static String seeColor = "/color";

		public static String changeEncryptionType = "/encryption ";
		public static String seeEncryptionType = "/encryption";
		
		public static String sendFile = "/sendfile ";
		public static String sendFileNoArgument = "/sendfile";
		public static String sendFileEncrypted = "/sendfileencrypted ";
		public static String sendFileEncryptedNoArgument = "/sendfileencrypted";
		
		public static String acceptFile = "/acceptfile ";
		public static String acceptFileNoArgument = "/acceptfile";
		
		public static String rejectFile = "/acceptfile ";
		public static String rejectFileNoArgument = "/acceptfile";
		
		public static String seeHelp = "/help";
	}
	
	//initial message that will be shown if you type the help command
	private static String helpMessage = "'/host port' to create a new chatroom"
					+ "\n'/join ip:port [message]' to join a chatroom"
					+ "\n'/reconnect' to attempt to reboot the connection handler (as server)"
					+ "\n'/boot id' too boot a client (as server)"
					+ "\n'/disconnect' to leave and close the current room"
					+ "\n'/exit' or '/quit' to exit program"
					+ "\n'/name' to see name, '/name newname' to change name"
					+ "\n'/color' to see color, '/color r g b' to change color"
					+ "\n'/encryption' to see encryption type, '/encryption type' to change encryption type"
					+ "\n'/sendfile [message]' to send file, '/sendfileencrypted type [message]' to send encrypted file"
					+ "\n'/acceptfile port [message]' to accept file"
					+ "\n'/rejectfile [message]' to reject file"
					+ "\n'/help' to see help";
	
	private static Color plainColor = Color.decode("#A000A0");
	private static Color defaultReceivedMessageColor = Color.BLACK;
	
	private static final long MAX_FILE_RESPONSE_DELAY_MILLIS = 60*1000;
	
	private String name;
	private Color color;
	private String encryptionType;
	
	private String roomName;
	
	private JTextPane textPane;
	private JTextField textField;

	private List<String> previousCommands;
	private int previousCommandIndex;
	
	private RoomInterface roomInterface;
	private ChatWindow cw;
	
	private boolean isServer;
	
	private boolean connectionAccepted;
	
	private Thread fileRequestThread;
	private byte[] fileToBeSentAsBytes;
	private int fileRecipientID;
	
	private String fileToBeReceivedName;
	private int fileToBeReceivedSize;
	private String fileToBeReceivedEncryptionType;
	private String fileToBeReceivedKey;
	private int fileSenderID;
	
	{
		previousCommands = new ArrayList<String>();
		previousCommandIndex = 0;

		textPane = new JTextPane();
		textField = new JTextField();
		
		textPane.setEditable(false);

		textField.addActionListener(this);
		textField.addKeyListener(this);

		setLayout(new BorderLayout());
		add(new JScrollPane(textPane), BorderLayout.CENTER);
		add(textField, BorderLayout.SOUTH);
	}
	
	private RoomViewer(ChatWindow _cw, String ip, int port, String message, String _name, Color _color, String _encryptionType, String _roomName)
			throws IOException {
		cw = _cw;
		name = _name;
		color = _color;
		encryptionType = _encryptionType;
		roomName = _roomName;
		if (ip == null) { //ip is not provided, so RoomViewer should create a Server
			isServer = true;
			connectionAccepted = true;
			roomInterface = new Server(this, port);
			addPlainMessage("Room created, port " + port);
		} else {
			isServer = false;
			connectionAccepted = false;
			roomInterface = new Client(this, ip, port);
			sendMessage(Message.createConnectionRequest(message, color, name));
			addPlainMessage("Room entered, ip " + ip + ", port " + port + ". Connection request sent:");
			appendToPane(message, color);
		}
	}
	
	/**
	 * Helper method that starts a new server and returns a new RoomViewer
	 */
	public static RoomViewer hostRoom(ChatWindow cw, int port, String name, Color color, String encryptionType, String roomName) {
		try {
			return new RoomViewer(cw, null, port, null, name, color, encryptionType, roomName);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
			throw null;
		}
	}
	
	/**
	 * Helper method that creates a RoomViewer and sends a join request
	 */
	public static RoomViewer joinRoom(ChatWindow cw, String ip, int port, String message, String name, Color color, String encryptionType, String roomName)
			throws IOException {
		return new RoomViewer(cw, ip, port, message, name, color, encryptionType, roomName);
	}

	/**
	 * Handles what happens when user types enter into the input field
	 */
	public void actionPerformed(ActionEvent ae) {
		String input = textField.getText();
		textField.setText("");

		if (input.isEmpty()) return;

		previousCommands.add(input);
		if (previousCommandIndex != previousCommands.size()) {
			previousCommandIndex += 1;
		}

		String inputLCase = input.toLowerCase(); //ignore the case when comparing

		if (inputLCase.startsWith(Command.host)) {
			host(input.substring(Command.host.length()));
		} else if (inputLCase.equals(Command.hostNoArgument)) {
			addPlainMessage("Usage: /host port");
		} else if (inputLCase.startsWith(Command.join)) {
			join(input.substring(Command.join.length()));
		} else if (inputLCase.equals(Command.joinNoArgument)) {
			addPlainMessage("Usage: /join ip:port [message]");
		} else if (inputLCase.startsWith(Command.reconnect)) {
			reconnect();
		} else if (inputLCase.startsWith(Command.boot)) {
			boot(input.substring(Command.boot.length()));
		} else if (inputLCase.startsWith(Command.bootNoArgument)) {
			addPlainMessage("Usage: /boot id");
		} else if (inputLCase.startsWith(Command.disconnect)) {
			close();
		} else if (inputLCase.startsWith(Command.exit) || inputLCase.startsWith(Command.quit)) {
			System.exit(0);
		} else if (inputLCase.startsWith(Command.changeName)) {
			changeName(input.substring(Command.changeName.length()));
		} else if (inputLCase.equals(Command.seeName)) {
			addPlainMessage("Your name is " + name);
		} else if (inputLCase.startsWith(Command.changeColor)) {
			changeColor(input.substring(Command.changeColor.length()));
		} else if (inputLCase.equals(Command.seeColor)) {
			String colorString = String.format("(%s, %s, %s)",
					color.getRed(), color.getGreen(), color.getBlue());
			addPlainMessage("Your color is " + colorString);
		} else if (inputLCase.startsWith(Command.changeEncryptionType)) {
			changeEncryptionType(input.substring(Command.changeEncryptionType.length()));
		} else if (inputLCase.equals(Command.seeEncryptionType)) {
			addPlainMessage("Your encryption type is " + encryptionType);
		} else if (inputLCase.startsWith(Command.sendFile)) {
			sendFileRequest(input.substring(Command.sendFile.length()), "");
		} else if (inputLCase.equals(Command.sendFileNoArgument)) {
			sendFileRequest("", "");
		} else if (inputLCase.startsWith(Command.sendFileEncrypted)) {
			sendFileRequestEncrypted(input.substring(Command.sendFileEncrypted.length()));
		} else if (inputLCase.equals(Command.sendFileEncryptedNoArgument)) {
			addPlainMessage("Usage: /sendfileencrypted type [message]");
		} else if (inputLCase.startsWith(Command.acceptFile)) {
			acceptFile(input.substring(Command.acceptFile.length()));
		} else if (inputLCase.equals(Command.acceptFileNoArgument)) {
			addPlainMessage("Usage: /acceptfile port [message]");
		} else if (inputLCase.startsWith(Command.rejectFile)) {
			rejectFile(input.substring(Command.rejectFile.length()));
		} else if (inputLCase.equals(Command.rejectFileNoArgument)) {
			rejectFile("");
		} else if (inputLCase.startsWith(Command.seeHelp)) {
			addPlainMessage(helpMessage);
		} else { //not a command, let's send the message instead
			sendTextMessage(input);
			addTextMessage(input, color, "You");
			return;
		}
		appendToPane(input, color);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods to handle commands
	//////////////
	
	public void host(String portStr) {
		try {
			int port = Integer.parseInt(portStr);
			cw.host(port, name, color, encryptionType);
		} catch (NumberFormatException e) {
			addPlainMessage("Usage: /host port");
		}
	}
	
	/**
	 * Class that parses a join command, either showing help or connecting to a server.
	 * @param input
	 */
	public void join(String input) {
		int index1 = input.indexOf(':');
		String ip = index1 == -1 ? "localhost" : input.substring(0,index1);
		if (index1 != -1) input = input.substring(index1+1);
		
		int index2 = input.indexOf(' ');
		String portString = index2 == -1 ? input : input.substring(0,index2);
		String text = index2 == -1 ? "" : input.substring(index2+1);
		
		int port;
		try {
			port = Integer.parseInt(portString);
		} catch (NumberFormatException e) {
			addPlainMessage("Usage: /join ip:port message");
			return;
		}

		try {
			cw.join(ip, port, text, name, color, encryptionType);
        } catch (IOException e) {
            if (e.getMessage().equals("Connection refused")) System.out.println("Connection refused");
            else e.printStackTrace();
            addPlainMessage("something went wrong when creating a connection");
        }
	}
	
	public void reconnect() {
		if (isServer) ((Server)roomInterface).createConnectionHandler();
		else addPlainMessage("You're not a server host");
	}
	
	/**
	 * Allows the server owner to kick a user
	 */
	private void boot(String idStr) {
		if (!isServer) {
			addPlainMessage("You're not a server!");
			return;
		}
		try {
			int id = Integer.parseInt(idStr);
			((Server)roomInterface).boot(id);
		} catch (NumberFormatException e) {
			addPlainMessage("Usage: /boot id");
		} catch (IndexOutOfBoundsException e) {
			addPlainMessage("User ID out of bounds");
		}
	}
	
	/**
	 * Disconnects and removes itself from it's parent
	 */
	public void close() {
		System.out.println("HEHEYHYHEHEYHEY");
		sendMessage(Message.createDisconnect(true, name));
		roomInterface.disconnect();
		cw.closeTab(this);
	}
	
	private void changeName(String newName) {
		sendTextMessage("I changed name to " + newName);
		addPlainMessage("Changed name to " + newName);
		name = newName;
	}
	
	private void changeColor(String input) {
		String[] newColorArray = input.split(" ");
		if (newColorArray.length != 3) {
			addPlainMessage("Usage: /color r g b\n" +
					"Example: /color 255 0 0       sets the color to red (=255,0,0)");
			return;
		}

		int r = Integer.parseInt(newColorArray[0]);
		int g = Integer.parseInt(newColorArray[1]);
		int b = Integer.parseInt(newColorArray[2]);

		try {
			Color newColor = new Color(r,g,b);
			sendTextMessage("I changed color to (" + r + ", " + g + ", " + b + ")");
			addPlainMessage("Changed color to (" + r + ", " + g + ", " + b + ")");
			color = newColor;
		} catch (IllegalArgumentException e) {
			addPlainMessage("Accepted values for r, g and b are 0-255");
		}
	}

	private void changeEncryptionType(String newEncryptionType) {
		encryptionType = newEncryptionType;
		addPlainMessage("Encryption type is set to " + encryptionType);
	}
	
	private void sendFileRequestEncrypted(String message) {
		int index = message.indexOf(' ');
		String fileEncryptionType = index == -1 ? message : message.substring(0,index);
		String text = index == -1 ? "" : message.substring(fileEncryptionType.length()+1);
		sendFileRequest(text, fileEncryptionType);
	}
	
	private synchronized void sendFileRequest(final String text, final String fileEncryptionType) {
		fileRequestThread = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(MAX_FILE_RESPONSE_DELAY_MILLIS);
				} catch (InterruptedException e) {
					return;
				}

				//If we got here, then we weren't interrupted
				fileToBeSentAsBytes = null;
				fileRecipientID = -1;
				
				addPlainMessage("File request timed out");
			}
		});
		
		new Thread(new Runnable() { //File selection thread
			public void run() {
				//Selects file recipient
				if (isServer) {
					List<Integer> indexes = ((Server)roomInterface).getActiveServerSenderIndexes();
					if (indexes.isEmpty()) {
						addPlainMessage("No recipients available");
						return;
					}
					if (indexes.size() == 1) fileRecipientID = indexes.get(0);
					else {
						String[] buttons = new String[indexes.size()];
						for (int i = 0; i < buttons.length; i++) buttons[i] = String.valueOf(indexes.get(i));
						int selection = JOptionPane.showOptionDialog(null, "Select recipient by ID", "",
						        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttons, null);
						if (selection == -1) return;
						fileRecipientID = indexes.get(selection);
					}
				} else fileRecipientID = -1;
								
				JFileChooser jc = new JFileChooser();
				jc.showOpenDialog(cw); //Blocking: waits for file selection
				File file = jc.getSelectedFile();
				
				if (file == null) return;
				String fileName = file.getName();
				
				//Reads file into byte array
				try {
					RandomAccessFile rf = new RandomAccessFile(file, "r");
					fileToBeSentAsBytes = new byte[(int)rf.length()];
					rf.readFully(fileToBeSentAsBytes);
					rf.close();
				} catch (FileNotFoundException e) {
					addPlainMessage("Failed to send file (file not found)");
					return;
				} catch (IOException e) {
					addPlainMessage("Failed to send file (IOException reading file)");
					return;
				}
				
				//Encrypts file
				if (!fileEncryptionType.isEmpty()) try {
					fileToBeSentAsBytes = Crypto.encrypt(fileToBeSentAsBytes, fileEncryptionType);
				} catch (EncryptionException e) {
					addPlainMessage("Failed to send file (failed to encrypt file)");
					return;
				} catch (UnsupportedEncryptionTypeException e) {
					addPlainMessage("Failed to send file (file encryption type unsupported)");
					return;
				}
				
				String fileSize = String.valueOf(fileToBeSentAsBytes.length); //AES changes size

				//Creates file request
				Message msg;
				try {
					msg = Message.createFileRequest(text, encryptionType, color, fileName,
							fileSize, fileEncryptionType, name);
				} catch (EncryptionException e) {
					addPlainMessage("Failed to send file (failed to encrypt message");
					return;
				} catch (UnsupportedEncryptionTypeException e) {
					addPlainMessage("Failed to send file (" + (e.fromOutboundFileRequest ? "file" : "message") + " encryption type unsupported)");
					return;
				}
				
				//Sends file request to file receiver
				sendFileMessage(msg, fileRecipientID);
				addPlainMessage("File request sent");
				
				fileRequestThread.start();
			}
		}).start();
	}
	
	public synchronized void acceptFile(String message) {
		int index = message.indexOf(' ');
		String portStr = index == -1 ? message : message.substring(0,index);
		String text = index == -1 ? "" : message.substring(index+1);
		Message msg = null;
		try {
			msg = Message.createFileResponse(text, encryptionType, color, "yes", portStr, name);
		} catch (EncryptionException e) {
			addPlainMessage("Failed to send file response (failed to encrypt)");
		} catch (UnsupportedEncryptionTypeException e) {
			addPlainMessage("Failed to send file response (encryption type unsupported)");
		} catch (NumberFormatException e) {
			addPlainMessage("Failed to send file response (port invalid");
		}
		if (msg == null) return;
		
		FileTransfer.receiveFile(this, fileToBeReceivedName, fileToBeReceivedSize, fileToBeReceivedEncryptionType,
				fileToBeReceivedKey, Integer.parseInt(portStr));
		fileToBeReceivedName = null;
		fileToBeReceivedSize = 0;
		fileToBeReceivedEncryptionType = null;
		fileToBeReceivedKey = null;
		fileRecipientID = -1;
		
		sendFileMessage(msg, fileSenderID);
	}
	
	public synchronized void rejectFile(String message) {
		try {
			sendFileMessage(Message.createFileResponse(message, encryptionType, color, "no", "", name), fileSenderID);
		} catch (EncryptionException e) {
			addPlainMessage("Failed to send file response (failed to encrypt)");
		} catch (UnsupportedEncryptionTypeException e) {
			addPlainMessage("Failed to send file response (encryption type unsupported)");
		}
		fileToBeReceivedName = null;
		fileToBeReceivedSize = 0;
		fileToBeReceivedEncryptionType = null;
		fileToBeReceivedKey = null;
		fileRecipientID = -1;
	}
	
	
	//////////////
	// End of methods to handle commands
	//////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * When a client tries to connect to the server, the server owner
	 * is asked (with a JOptionPane) if s/he wants to accept the user or not 
	 * @param appeal
	 * @return
	 */
	public boolean connectionApprovedByHost(String appeal) {
		Object[] options = {"Accept", "Deny"};
		int choice = JOptionPane.showOptionDialog(cw, appeal,
				"Connection request in " + roomName,
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null, //no custom icon 
				options, options[0]);
				
		if (choice == JOptionPane.YES_OPTION) {
			addPlainMessage("Connection accepted.");
			return true;
		} else if (choice == JOptionPane.NO_OPTION) {
			addPlainMessage("Connection rejected.");
			return false;
		} else {
			addPlainMessage("Connection request canceled. Connection rejected.");
			return false;
		}
	}
	
	/**
	 * Makes sure that the text focus gains focus when we select a room
	 */
	@Override
	public void requestFocus() {
		textField.requestFocus();
	}

	/**
	 * Handles scrolling up/down to see history
	 */
	public void keyPressed(KeyEvent ke) {
		int keyCode = ke.getKeyCode();
		if (keyCode != KeyEvent.VK_UP && keyCode != KeyEvent.VK_DOWN) return;
		if (keyCode == KeyEvent.VK_UP) previousCommandIndex = Math.max(previousCommandIndex-1, 0);
		else previousCommandIndex = Math.min(previousCommandIndex+1, previousCommands.size()-1);
		textField.setText(previousCommands.get(previousCommandIndex));
	}
	public void keyReleased(KeyEvent ke) {}
	public void keyTyped(KeyEvent ke) {}
	
	public void connectionHandlerFailed(boolean initFailed) {
		if (initFailed) addPlainMessage("server socket failed to initialize. Type \"/reconnect\" to open again.");
		else addPlainMessage("connection handler crashed while handling a connection. Type \"/reconnect\" to open again.");
	}
	
	private void sendTextMessage(String message) {
		try {
			sendMessage(Message.createMessage(message, encryptionType, color, name));
		} catch (EncryptionException e) {
			addPlainMessage("Failed to send message (failed to encrypt)");
		} catch (UnsupportedEncryptionTypeException e) {
			addPlainMessage("Failed to send message (encryption type unsupported)");
		}
	}
	
	private void sendMessage(Message msg) {
		try {
			roomInterface.sendMessage(XMLParser.msgToXML(msg));
		} catch (IOException e) {
			e.printStackTrace();
			addPlainMessage("Failed to send message (IOException)");
		}
	}
	
	private void sendFileMessage(Message msg, int id) {
		if (isServer) {
			Server server = (Server)roomInterface;
			try {
				server.sendFileMessage(XMLParser.msgToXML(msg), id);
			} catch (IOException e) {
				e.printStackTrace();
				addPlainMessage("Failed to send file related message (IOException)");
			}
		} else {
			sendMessage(msg);
		}
	}
	
	public void receiveMessage(String xml) {
		receiveMessage(xml, -1); //id = -1 
	}
	
	/**
	 * Receives a message as an xml string, turns it into a Message,
	 * then handle it differently depending on which type of message it is
	 * (e.g. if it's a connection response, a disconnect message, a key request and so on)
	 * @param xml Message as an XML String.
	 * @param id Used if we're sending a file and to identify
	 * which id we received a message from if we are host (used for e.g. booting users)
	 */
	public synchronized void receiveMessage(String xml, int id) {
		Message msg;
		try {
			msg = XMLParser.XMLToMsg(xml);
		} catch (XMLException e) {
			addPlainMessage("Failed to parse received XML: " + xml);
			return;
		}
		
		String sender = msg.sender;
		if (id != -1) sender += " (" + id + ")";
		Color msgColor = msg.color.isEmpty() ? defaultReceivedMessageColor : Color.decode(msg.color);
		
		if (msg.isConnectionResponse()) {
			String output;
			if (!connectionAccepted) {
				connectionAccepted = !msg.connectionReply.equals("no");
				output = "Connection response: " + msg.connectionReply;
				if (!connectionAccepted) output += "\nEnter /disconnect to close tab";
			} else {
				output = sender + " sent unexpected connection response: " + msg.connectionReply;
			}
			addPlainMessage(output);
			return;
		}
		//if we got to this point we have an accepted connection
		connectionAccepted = true;
		
		if (msg.isDisconnect()) {
			addPlainMessage(sender + " has disconnected");
		} else if (msg.isKeyRequest()) {
			addPlainMessage(sender + " sent key request");
		} else if (msg.encryptionTypeUnsupported() || msg.decryptionFailed()) {
			String output = (msg.decryptionFailed() ? "Failed to decrypt " : "Encryption type unsupported for ")
					+ "a message from " + sender + " of type ";
			if (msg.isTextMessage()) output += "text message.";
			else if (msg.isConnectionRequest()) output += "connection request.";
			else if (msg.isFileRequest()) {
				output += "file request. ";
				try {
					sendFileMessage(Message.createFileResponse(
							msg.decryptionFailed() ? "Failed to decrypt file request message" : "Encryption type unsupported for file request message",
							encryptionType, color, "no", "", name), id);
				} catch (EncryptionException e) {
					addPlainMessage("Failed to send file response (failed to encrypt).");
				} catch (UnsupportedEncryptionTypeException e) {
					addPlainMessage("Failed to send file response (encryption type unsupported).");
				}
			} else if (msg.isFileResponse()) {
				if (fileRequestThread != null) fileRequestThread.interrupt();
				output += "file response" + (fileToBeSentAsBytes == null ? " (unexpected)" : "") + ". Reply: " + msg.fileReply + ". ";
				if (fileToBeSentAsBytes == null) ;
				else if (msg.fileReply.equals("yes")) try {
					String fileRecipientIP = isServer ? ((Server)roomInterface).getIP(fileRecipientID) : ((Client)roomInterface).ip;
					FileTransfer.sendFile(this, fileToBeSentAsBytes, Integer.parseInt(msg.port), fileRecipientIP);
					output += "File transfer initiated.";
				} catch (NumberFormatException e) {
					output += "Recipient provided invalid port. File transfer aborted.";
				} else {
					output += "File transfer aborted.";
				}
				fileToBeSentAsBytes = null;
				fileRecipientID = -1;
			}
			addPlainMessage(output);
		} else if (msg.isTextMessage()) {
			addTextMessage(msg.text, Color.decode(msg.color), sender);
		} else if (msg.isConnectionRequest()) {
			addPlainMessage(sender + " sent unexpected connection request.");
		} else if (msg.isFileRequest()) {
			addPlainMessage(sender + " sent file request. File name: " + msg.fileName + ". File size: " + msg.fileSize + ". File encryption type: "
					+ msg.fileEncryptionType + ". Message:");
			appendToPane(msg.text, msgColor);

			if (fileToBeReceivedName != null || msg.fileSizeInvalid() || msg.fileEncryptionTypeUnsupported()) {
				String reason;
				if (fileToBeReceivedName != null) reason = "File request already pending.";
				else if (msg.fileSizeInvalid()) reason = "File size invalid.";
				else reason = "File encryption type unsupported.";
				addPlainMessage(reason + " File request denied.");
				try {
					sendFileMessage(Message.createFileResponse(reason, encryptionType, color, "no", "", name), id);
				} catch (EncryptionException e) {
					addPlainMessage("Failed to send file response (failed to encrypt).");
				} catch (UnsupportedEncryptionTypeException e) {
					addPlainMessage("Failed to send file response (encryption type unsupported).");
				}
			} else {
				addPlainMessage("Enter '/rejectfile [message]' or '/acceptfile port [message]' to respond.");
				fileToBeReceivedName = msg.fileName;
				fileToBeReceivedSize = Integer.parseInt(msg.fileSize);
				fileToBeReceivedEncryptionType = msg.fileEncryptionType;
				fileToBeReceivedKey = msg.fileKey;
				fileSenderID = id;
			}
		} else if (msg.isFileResponse()) {
			if (fileRequestThread != null) fileRequestThread.interrupt();
			addPlainMessage(sender + " sent " + (fileToBeSentAsBytes == null ? "unexpected " : "") + "file response. Reply: " + msg.fileReply + ". Message:");
			appendToPane(msg.text, msgColor);
			if (fileToBeSentAsBytes == null) ;
			else if (msg.fileReply.equals("yes")) try {
				String fileRecipientIP = isServer ? ((Server)roomInterface).getIP(fileRecipientID) : ((Client)roomInterface).ip;
				FileTransfer.sendFile(this, fileToBeSentAsBytes, Integer.parseInt(msg.port), fileRecipientIP);
				addPlainMessage("File transfer initiated.");
			} catch (NumberFormatException e) {
				addPlainMessage("Recipient provided invalid port. File transfer aborted.");
			} else {
				addPlainMessage("File transfer aborted");
			}
			fileToBeSentAsBytes = null;
			fileRecipientID = -1;
		}
	}
	
	/**
	 * Helper function to add a String to the textPane without any formatting
	 * @param plainText
	 */
	public void addPlainMessage(String plainText) {
		appendToPane(plainText, plainColor);
	}
	
	/**
	 * Helper function to add a String to the textPane with a certain color, from a certain sender name.
	 * @param text
	 * @param c
	 * @param sender
	 */
	public void addTextMessage(String text, Color c, String sender) {
		appendToPane(sender + ": " + text, c);
	}

	//Found at http://stackoverflow.com/questions/9650992/how-to-change-text-color-in-the-jtextarea
	private synchronized void appendToPane(String text, Color c) {
		textPane.setEditable(true);
		
		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

		aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
		aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

		int len = textPane.getDocument().getLength();
		if (len != 0) text = "\n" + text;
		
		textPane.setCaretPosition(len);
		textPane.setCharacterAttributes(aset, false);
		textPane.replaceSelection(text);
		
		textPane.setEditable(false);
    }
}