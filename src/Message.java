import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

/**
 * This is a container class for all the information that can be sent between clients.
 */

public class Message {
	private static final String no = "no"; // no

	/**
	 * Escapes special characters in the input string and returns the result.
	 */
	public static String makeEscape(String s) {
		return s.replace("&","&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("'","&apos;").replace("\"", "&quot;");
	}

	/**
	 * Adds # to the beginning of the input unless it already starts with it or is empty. If empty, simply return
	 * input. Otherwise checks if the resulting string is properly formatted as #RRGGBB. If the check fails, prints
	 * information to the console and returns the empty string. Otherwise returns the properly formatted color string.
	 */
	public static String fixColor(String s) {
		if (s.isEmpty()) return s;
		if (!s.startsWith("#")) s = "#" + s;
		try {
			if (s.length() != 7) throw new NumberFormatException();
			Color.decode(s);
		} catch (NumberFormatException e) {
			System.out.println("String not of form #RRGGBB: " + s);
			s = "";
		}
		return s;
	}

	//The message text
	public final String text;
	//The encryption type
	public final String encryptionType;
	//The encryption key
	public final String key;
	//The color of the message, in #RRGGBB format
	public final String color;
	//The name of the file to be sent
	public final String fileName;
	//The size of the file to be sent
	public final String fileSize;
	//The encryption type to be used on the file to be sent
	public final String fileEncryptionType;
	//The encryption key for the file to be sent
	public final String fileKey;
	//The reply to the file request
	public final String fileReply;
	//The port to send the file through
	public final String port;
	//The reply to the connection request
	public final String connectionReply;
	//The sender's name
	public final String sender;
	//0 if normal message, 1 if connection request, 2 if connection response, 3 if file request, 4 if file response,
	//5 if disconnect, 6 if key request
	public final int messageType;
	//true iff the decryption of the inbound message failed
	public final boolean decryptionFailed;
	//true iff the encryption type of the inbound message is unsupported
	public final boolean encryptionTypeUnsupported;
	//true iff the file size of the proposed inbound file transfer is not a number or is negative
	public final boolean fileSizeInvalid;
	//true iff the encryption type of the proposed inbound file transfer is unsupported
	public final boolean fileEncryptionTypeUnsupported;
	//true iff port of the accepted outbound file transfer is not a number or is negative
	public final boolean portInvalid;

	/**
	 * Constructor for all outbound messages.
	 */
	private Message(String _text, String _encryptionType, Color _color, String _fileName, String _fileSize,
					String _fileEncryptionType, String _fileReply, String _port, String _connectionReply,
					String _sender, int _messageType)
			throws EncryptionException, UnsupportedEncryptionTypeException {
		text = _encryptionType.isEmpty() ? makeEscape(_text) : Crypto.encrypt(_text, _encryptionType);
		encryptionType = makeEscape(_encryptionType);
		key = _encryptionType.isEmpty() ? "" : Crypto.getKey(_encryptionType);
		color = _color == null ? "" : "#" + String.format("%06x", _color.getRGB() & 0x00FFFFFF);
		fileName = makeEscape(_fileName);
		fileSize = _fileSize;
		fileEncryptionType = makeEscape(_fileEncryptionType);
		fileKey = _fileEncryptionType.isEmpty() ? "" : Crypto.getKey(_fileEncryptionType);
		fileReply = _fileReply;
		port = _port;
		connectionReply = _connectionReply;
		sender = makeEscape(_sender);
		messageType = _messageType;
		decryptionFailed = false;
		encryptionTypeUnsupported = false;
		if (!_fileSize.isEmpty() && Integer.parseInt(_fileSize) < 0) throw new NumberFormatException();
		fileSizeInvalid = false;
		if (!_fileEncryptionType.isEmpty() && !Crypto.isSupported(_fileEncryptionType))
			throw new UnsupportedEncryptionTypeException(true);
		fileEncryptionTypeUnsupported = false;
		if (!_port.isEmpty() && Integer.parseInt(_port) < 0) throw new NumberFormatException();
		portInvalid = false;
	}

	/**
	 * Constructor for all inbound messages.
	 * @param textList a list of lists, each of which represents a part of the message text. Each member list consists
	 *				   of a text string and, if it's encrypted, the encryption type and key.
	 */
	private Message(List<List<String>> textList, String _color, String _fileName, String _fileSize,
					String _fileEncryptionType, String _fileKey, String _fileReply, String _port,
					String _connectionReply, String _sender, int _messageType) {
		String tempText;
		boolean tempDecryptionFailed;
		boolean tempEncryptionTypeUnsupported;
		try {
			StringBuilder textBuilder = new StringBuilder();
			for (List<String> textPart : textList) {
				String t = textPart.get(0);
				if (textPart.size() != 1) {
					String type = textPart.get(1);
					String key = textPart.get(2);
					textBuilder.append(Crypto.decrypt(t, key, type));
				} else textBuilder.append(t);
			}
			tempText = textBuilder.toString();
			tempDecryptionFailed = false;
			tempEncryptionTypeUnsupported = false;
		} catch (EncryptionException e) {
			tempText = "";
			tempDecryptionFailed = true;
			tempEncryptionTypeUnsupported = false;
		} catch (UnsupportedEncryptionTypeException e) {
			tempText = "";
			tempDecryptionFailed = false;
			tempEncryptionTypeUnsupported = true;
		}
		text = tempText;
		decryptionFailed = tempDecryptionFailed;
		encryptionTypeUnsupported = tempEncryptionTypeUnsupported;

		encryptionType = "";
		key = "";
		color = fixColor(_color);
		fileName = _fileName;
		fileSize = _fileSize;
		fileEncryptionType = _fileEncryptionType;
		fileKey = _fileKey;
		fileReply = _fileReply;
		port = _port;
		connectionReply = _connectionReply;
		sender = _sender;
		messageType = _messageType;
		
		boolean tempFileSizeInvalid;
		try {
			if (!_fileSize.isEmpty() && Integer.parseInt(_fileSize) < 1) throw new NumberFormatException();
			tempFileSizeInvalid = false;
		} catch (NumberFormatException e) {
			tempFileSizeInvalid = true;
		}
		fileSizeInvalid = tempFileSizeInvalid;
		
		fileEncryptionTypeUnsupported = fileEncryptionType.isEmpty() ? false : !Crypto.isSupported(fileEncryptionType);
		
		boolean tempPortInvalid;
		try {
			if (!_port.isEmpty() && Integer.parseInt(_port) < 1) throw new NumberFormatException();
			tempPortInvalid = false;
		} catch (NumberFormatException e) {
			tempPortInvalid = true;
		}
		portInvalid = tempPortInvalid;
	}

	/**
	 * Returns true iff the message is of text message type.
	 */
	public boolean isTextMessage() {
		return messageType == 0;
	}

	/**
	 * Returns true iff the message is of connection request type.
	 */
	public boolean isConnectionRequest() {
		return messageType == 1;
	}

	/**
	 * Returns true iff the message is of connection response type.
	 */
	public boolean isConnectionResponse() {
		return messageType == 2;
	}

	/**
	 * Returns true iff the message is of file request type.
	 */
	public boolean isFileRequest() {
		return messageType == 3;
	}

	/**
	 * Returns true iff the message is of file response type.
	 */
	public boolean isFileResponse() {
		return messageType == 4;
	}

	/**
	 * Returns true iff the message is of disconnect type.
	 */
	public boolean isDisconnect() {
		return messageType == 5;
	}

	/**
	 * Returns true iff the message is of key request type.
	 */
	public boolean isKeyRequest() {
		return messageType == 6;
	}

	/**
	 * Returns true iff the decryption of the inbound message failed.
	 */
	public boolean decryptionFailed() {
		return decryptionFailed;
	}

	/**
	 * Returns true iff the encryption type of the inbound message is unsupported.
	 */
	public boolean encryptionTypeUnsupported() {
		return encryptionTypeUnsupported;
	}
	
	/**
	 * Returns true iff the file size of the proposed inbound file transfer is invalid.
	 */
	public boolean fileSizeInvalid() {
		return fileSizeInvalid;
	}

	/**
	 * Returns true iff the encryption type of the proposed inbound file transfer is unsupported.
	 */
	public boolean fileEncryptionTypeUnsupported() {
		return fileEncryptionTypeUnsupported;
	}
	
	/**
	 * Returns true iff port of the accepted outbound file transfer is not a number or is negative
	 */
	public boolean portInvalid() {
		return portInvalid;
	}

	/**
	 * Factory method for outbound text messages.
	 */
	public static Message createMessage(String text, String encryptionType, Color color, String sender)
			throws EncryptionException, UnsupportedEncryptionTypeException {
		return new Message(text, encryptionType, color, "", "", "", "", "", "", sender, 0);
	}

	/**
	 * Factory method for inbound text messages.
	 */
	public static Message createMessage(List<List<String>> textList, String color, String sender) {
		return new Message(textList, color, "", "", "", "", "", "", "", sender, 0);
	}

	/**
	 * Factory method for outbound connection request messages.
	 */
	public static Message createConnectionRequest(String text, Color color, String sender) {
		try {
			return new Message(text, "", color, "", "", "", "", "", "", sender, 1);
		//} catch (EncryptionException | UnsupportedEncryptionTypeException e) {
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
			throw null;
		}
	}

	/**
	 * Factory method for inbound connection request messages.
	 */
	public static Message createConnectionRequest(List<List<String>> textList, String color, String sender) {
		return new Message(textList, color, "", "", "", "", "", "", "", sender, 1);
	}

	/**
	 * Factory method for outbound connection response messages.
	 */
	public static Message createConnectionResponse(String sender) {
		try {
			return new Message("", "", null, "", "", "", "", "", no, sender, 2);
		//} catch (EncryptionException | UnsupportedEncryptionTypeException e) {
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
			throw null;
		}
	}
	
	/**
	 * Factory method for inbound connection response messages.
	 */
	public static Message createConnectionResponse(String reply, String sender) {
		return new Message(new ArrayList<List<String>>(), "", "", "", "", "", "", "", reply, sender, 2);
	}

	/**
	 * Factory method for outbound file request messages.
	 */
	public static Message createFileRequest(String text, String encryptionType, Color color, String fileName,
											String fileSize, String fileEncryptionType, String sender)
			throws EncryptionException, UnsupportedEncryptionTypeException {
		return new Message(text, encryptionType, color, fileName, fileSize, fileEncryptionType, "", "", "", sender, 3);
	}

	/**
	 * Factory method for inbound file request messages.
	 */
	public static Message createFileRequest(List<List<String>> textList, String color, String fileName,
											String fileSize, String fileEncryptionType, String fileKey, String sender) {
		return new Message(textList, color, fileName, fileSize, fileEncryptionType, fileKey, "", "", "", sender, 3);
	}

	/**
	 * Factory method for outbound file response messages.
	 */
	public static Message createFileResponse(String text, String encryptionType, Color color, String fileReply,
											 String port, String sender)
			throws EncryptionException, UnsupportedEncryptionTypeException {
		return new Message(text, encryptionType, color, "", "", "", fileReply, port, "", sender, 4);
	}

	/**
	 * Factory method for inbound file response messages.
	 */
	public static Message createFileResponse(List<List<String>> textList, String color, String fileReply, String port,
											 String sender) {
		return new Message(textList, color, "", "", "", "", fileReply, port, "", sender, 4);
	}

	/**
	 * Factory method for disconnect messages.
	 * @param outbound indicates whether the message is being sent or received
	 */
	public static Message createDisconnect(boolean outbound, String sender) {
		if (outbound) try {
			return new Message("", "", null, "", "", "", "", "", "", sender, 5);
		//} catch (EncryptionException | UnsupportedEncryptionTypeException e) {
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return new Message(new ArrayList<List<String>>(), "", "", "", "", "", "", "", "", sender, 5);
	}

	/**
	 * Factory method for key request messages (always inbound).
	 */
	public static Message createKeyRequest(String sender) {
		return new Message(new ArrayList<List<String>>(), "", "", "", "", "", "", "", "", sender, 6);
	}
}