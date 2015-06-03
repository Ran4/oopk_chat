import java.io.StringReader;
//import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
//import org.xml.sax.SAXException;

/**
 * This class contains methods for converting a text message in {@code Message} form into an XML {@code String}, and the
 * other way around.
 */

public class XMLParser {

	private static class Tag {
		static final String MESSAGE = "message";
		static final String TEXT = "text";
		static final String FILEREQUEST = "filerequest";
		static final String FILERESPONSE = "fileresponse";
		static final String REQUEST = "request";
		static final String DISCONNECT = "disconnect";
		static final String KEYREQUEST = "keyrequest";
		static final String ENCRYPTED = "encrypted";
	}

	private static class Attribute {
		static final String SENDER = "sender";
		static final String COLOR = "color";
		static final String NAME = "name";
		static final String SIZE = "size";
		static final String REPLY = "reply";
		static final String PORT = "port";
		static final String TYPE = "type";
		static final String KEY = "key";
	}

	/**
	 * Returns an opening XML tag with the specified tag name and attributes
	 * @param attributes an array alternating between attribute names and values
	 */
	private static String createOpeningTag(String tagName, String... attributes) {
		String tag = "<" + tagName;
		for (int i = 0; i+1 < attributes.length; i += 2)
			tag += " " + attributes[i] + "=\"" + attributes[i+1] + "\"";
		return tag + ">";
	}

	/**
	 * Returns a closing XML tag with the specified tag name
	 */
	private static String createClosingTag(String tagName) {
		return "</" + tagName + ">";
	}

	/**
	 * Converts an outbound {@code Message} to an XML {@code String} so it can be sent to the server.
	 */
	public static String msgToXML(Message msg) {
		String messageOpeningTag = createOpeningTag(Tag.MESSAGE, Attribute.SENDER, msg.sender);
		String messageClosingTag = createClosingTag(Tag.MESSAGE);

		String contentType;
		List<String> attributes = new ArrayList<String>();//new ArrayList<>();
		if (!msg.color.isEmpty()) {
			attributes.add(Attribute.COLOR);
			attributes.add(msg.color);
		}

		if (msg.isTextMessage()) contentType = Tag.TEXT;
		else if (msg.isConnectionRequest()) contentType = Tag.REQUEST;
		else if (msg.isConnectionResponse()) {
			contentType = Tag.REQUEST;
			attributes.add(Attribute.REPLY);
			attributes.add(msg.connectionReply);
		} else if (msg.isFileRequest()) {
			contentType = Tag.FILEREQUEST;
			attributes.add(Attribute.NAME);
			attributes.add(msg.fileName);
			attributes.add(Attribute.SIZE);
			attributes.add(msg.fileSize);
			if (!msg.fileEncryptionType.isEmpty()) {
				attributes.add(Attribute.TYPE);
				attributes.add(msg.fileEncryptionType);
				attributes.add(Attribute.KEY);
				attributes.add(msg.fileKey);
			}
		} else if (msg.isFileResponse()) {
			contentType = Tag.FILERESPONSE;
			attributes.add(Attribute.REPLY);
			attributes.add(msg.fileReply);
			if (msg.fileReply.equalsIgnoreCase("yes")) {
				attributes.add(Attribute.PORT);
				attributes.add(msg.port);
			}
		} else if (msg.isDisconnect()) contentType = Tag.DISCONNECT;
		else throw new IllegalArgumentException();

		String contentOpeningTag = createOpeningTag(contentType, attributes.toArray(new String[0]));
		String contentClosingTag = createClosingTag(contentType);

		String encryptionOpeningTag = "";
		String encryptionClosingTag = "";
		if (!msg.encryptionType.isEmpty()) {
			encryptionOpeningTag = createOpeningTag(Tag.ENCRYPTED, Attribute.TYPE, msg.encryptionType, Attribute.KEY, msg.key);
			encryptionClosingTag = createClosingTag(Tag.ENCRYPTED);
		}

		return messageOpeningTag + contentOpeningTag + encryptionOpeningTag + msg.text
			   + encryptionClosingTag + contentClosingTag + messageClosingTag;
	}

	/**
	 * Converts an inbound XML {@code String} message to a {@code Message}.
	 * @throws XMLException if the input string is not properly formatted XML, or something else went wrong while parsing.
	 * @throws EncryptionException if something went wrong while decrypting the message, such as a bad key.
	 * @throws UnsupportedEncryptionException if the xml contains encryption of an unsupported type.
	 */
	public static Message XMLToMsg(String xml) throws XMLException {
		DocumentBuilder builder;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new XMLException();
		}
		builder.setErrorHandler(null);

		Element root;
		try {
			root = builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
		//} catch (SAXException | IOException e) {
		} catch (Exception e) {
			throw new XMLException(e);
		}
		if (!root.getNodeName().equals(Tag.MESSAGE)) throw new XMLException();

		String sender = root.getAttribute(Attribute.SENDER);

		if (root.getElementsByTagName(Tag.DISCONNECT).getLength() != 0) return Message.createDisconnect(false, sender);
		if (root.getElementsByTagName(Tag.KEYREQUEST).getLength() != 0) return Message.createKeyRequest(sender);

		NodeList nodes = root.getChildNodes();

		List<List<String>> textList = new ArrayList<List<String>>();//new ArrayList<>();

		for (int i = 0; i < nodes.getLength(); i++) if (nodes.item(i) instanceof Element) {
			Element child = (Element)nodes.item(i);
			String tagName = child.getTagName();
			if (tagName.equals(Tag.TEXT)) {
				addText(textList, child);
				String color = child.getAttribute(Attribute.COLOR);
				return Message.createMessage(textList, color, sender);
			}
			if (tagName.equals(Tag.REQUEST)) {
				if (child.hasAttribute(Attribute.REPLY))
					return Message.createConnectionResponse(child.getAttribute(Attribute.REPLY), sender);
				addText(textList, child);
				String color = child.getAttribute(Attribute.COLOR);
				return Message.createConnectionRequest(textList, color, sender);
			}
			if (tagName.equals(Tag.FILEREQUEST)) {
				addText(textList, child);
				String color = child.getAttribute(Attribute.COLOR);
				String fileName = child.getAttribute(Attribute.NAME);
				String fileSize = child.getAttribute(Attribute.SIZE);
				String fileEncryptionType = child.getAttribute(Attribute.TYPE);
				String fileKey = child.getAttribute(Attribute.KEY);
				return Message.createFileRequest(textList, color, fileName, fileSize, fileEncryptionType, fileKey, sender);
			}
			if (tagName.equals(Tag.FILERESPONSE)) {
				addText(textList, child);
				String color = child.getAttribute(Attribute.COLOR);
				String fileReply = child.getAttribute(Attribute.REPLY);
				String port = child.getAttribute(Attribute.PORT);
				return Message.createFileResponse(textList, color, fileReply, port, sender);
			}
		}
		return Message.createMessage(textList, "", sender);
	}

	/**
	 * Adds all text content of an {@code Element} to a {@code List<List<String>>}, where each text part is represented as a
	 * list of the text and, if it's encrypted, the encryption type and key.
	 */
	private static void addText(List<List<String>> textList, Element element) {
		NodeList nodes = element.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element) {
				Element child = (Element)node;
				if (child.getTagName().equals(Tag.ENCRYPTED)) textList.add(Arrays.asList(child.getTextContent(),
						child.getAttribute(Attribute.TYPE), child.getAttribute(Attribute.KEY)));
				else addText(textList, (Element)node);
			} else textList.add(Arrays.asList(node.getTextContent()));
		}
	}
}