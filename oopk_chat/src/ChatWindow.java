import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 * Main graphics output class
 */
public class ChatWindow extends JFrame implements ActionListener, KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel panel;
	private JTextPane textPane;
	private JTextField textField;
	
	public static class Command {
		public static String changeName = "/name ";
		public static String seeName = "/name";
		
		public static String changeColor = "/color ";
		public static String seeColor = "/color";
		
		public static String seeHelp = "/help";
		
		public static String changeEncryptionType = "/encryption ";
		public static String seeEncryptionType = "/encryption";
		
		public static String join = "/join ";
		public static String disconnect = "/disconnect";
		
		public static String exit = "/exit";
		public static String quit = "/quit";
	}
	
	private String name;
	private Color color;
	private String encryptionType;
	
	private Client client;
	
	private ArrayList<String> previousCommands;
	private int previousCommandIndex = 0;
	
	public ChatWindow (Client _client) {
		name = "Default Name";
		color = Color.BLACK;
		encryptionType = "CAESAR";
		previousCommands = new ArrayList<String>();
		previousCommandIndex = 0;
		
		client = _client;
		
		//textArea = new JTextArea(client.startMessage());
		
		panel = new JPanel();

		textPane = new JTextPane();
        panel.add(textPane);

        appendToPane(Client.startMessage, Color.BLUE);
		
		textField = new JTextField();
						
		textField.addActionListener(this);
		textField.addKeyListener(this);

		//add(textArea, BorderLayout.CENTER);
		add(panel, BorderLayout.CENTER);
		add(textField, BorderLayout.SOUTH);
		
		addWindowListener( new WindowAdapter() {
			public void windowOpened( WindowEvent e ){
		    	textField.requestFocus();
		    }
		}); 
		
		setSize(new Dimension(500, 400));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	/*
	 * 
	 * 
	 * Found at http://stackoverflow.com/questions/9650992/how-to-change-text-color-in-the-jtextarea
	 */
	private void appendToPane(String text, Color c)
    {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = this.textPane.getDocument().getLength();
        this.textPane.setCaretPosition(len);
        this.textPane.setCharacterAttributes(aset, false);
        this.textPane.replaceSelection(text);
    }
	
	public void addMessage(String xmlMessage) {
		
		/*
		Message m = Message.fromXMLMessage(xmlMessage);
		
		String finalOutputMessage = m.from + " " + m.text;
		*/
		
		//textArea.append(xmlMessage + "\n");
		this.appendToPane(xmlMessage + "\n", Color.BLACK);
	}
	
	public void addPlainMessage(String plainText) {
		this.appendToPane(plainText, Color.decode("#800080"));
		//textArea.append(plainText + "\n");
	}
	
	private void disconnect() {
		try {
			client.closeConnection();
		} catch (IOException e) {
			
		}
	}
	
	private void join(String message) {
		String ipAndPort = message.substring(Command.join.length());
		
		String[] ipAndPortSplit = ipAndPort.split(":");
		boolean badInput = false; 
		
		if (ipAndPortSplit.length == 2) {
			String ip = ipAndPortSplit[0];
			int port = 0;
			try {
				port = Integer.parseInt(ipAndPortSplit[1]);
			} catch (NumberFormatException e) {
				badInput = true;
			}
			
			String errorMsg = "Something went wrong when creating a connection\n";
			
			if (!badInput) {
				try {
					client.addConnection(ip, port);
					addPlainMessage("Added connection to ip " + ip + "\n");
					
				} catch (UnknownHostException e) {
                    System.out.println(errorMsg);
                    
                    addPlainMessage(errorMsg);
                } catch (IOException e) {
                    System.out.println(errorMsg);
                    addPlainMessage(errorMsg);
                }
			}
		} else {
			badInput = true;
		}
		
		if (badInput) {
			addPlainMessage("Usage: /join ip:port\n");
		}
	}
	
	private void changeColor(String text) {
		String newColorString = text.substring(Command.changeColor.length());
		
		//addPlainMessage("Current color: (" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")");
		
		String[] newColorStringSplit = newColorString.split(" ");
		if (newColorStringSplit.length != 3) {
			addPlainMessage("Usage: /color r g b\n" + 
					"Example: /color 255 0 0       sets the color to red (=255,0,0)\n");
			return;
		}
		
		int r = Integer.parseInt(newColorStringSplit[0]);
		int g = Integer.parseInt(newColorStringSplit[1]);
		int b = Integer.parseInt(newColorStringSplit[2]);
		
		try {
			Color newColor = new Color(r,g,b);
			
			sendMessage(this.name + " changed color to (" + r + ", " + g + ", " + b + ")");
			addPlainMessage("Changed color to (" + r + ", " + g + ", " + b + ")\n");
			this.color = newColor;
		} catch (NumberFormatException e) {
			addPlainMessage("Problem setting color. Error: " + e.toString() + "\n");
			return;
		}
	}
	
	private void changeEncryptionType(String text) {
		String wantedEncryptionType = text.substring(Command.changeEncryptionType.length());
		this.encryptionType = wantedEncryptionType;
		addPlainMessage("Encryption type is set to " + this.encryptionType + "\n");
		return;
	}
	
	private void changeName(String text) {
		String newName = text.substring(Command.changeName.length());
		
		sendMessage(name + " changed name to " + newName);
		addPlainMessage("Changed name to " + newName + "\n");
		name = newName;
	}
	
	private void sendMessage(String message) {
		if (client instanceof Server) {
			try {
				((Server)client).sendMessage(message, null);
			} catch (IOException e) {
				System.out.println("IOException when trying to send message as a server");
			}
		} else {
			client.sendMessage(message);
		}
	}
	
	public void keyPressed(KeyEvent ke) {
		int keyCode = ke.getKeyCode();
				
		//handles scrolling up/down to see history
		if (keyCode == KeyEvent.VK_UP) {
			previousCommandIndex = Math.max(previousCommandIndex-1, 0);
			textField.setText(previousCommands.get(previousCommandIndex));
		} else if (keyCode == KeyEvent.VK_DOWN) {
			previousCommandIndex = Math.min(previousCommandIndex+1, previousCommands.size()-1);
			textField.setText(previousCommands.get(previousCommandIndex));
		}
	}
	
	public void keyReleased(KeyEvent ke) {}
	public void keyTyped(KeyEvent ke) {}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(textField)) {
			String text = textField.getText();
			textField.setText("");
			
			if (text.isEmpty())
				return;
					
			previousCommands.add(text);
			if (previousCommandIndex != previousCommands.size()) {
				previousCommandIndex += 1;
			}
			
			String ltext = text.toLowerCase();
			
			if (ltext.startsWith(Command.changeName)) {
				changeName(text);
			} else if (ltext.equals(Command.seeName)) {
				this.addPlainMessage("Your name is " + this.name + "\n");
			} else if (ltext.startsWith(Command.changeColor)) {
				changeColor(text);
			} else if (ltext.equals(Command.seeColor)) {
				String colorString = String.format("(%s, %s, %s)",
						color.getRed(), color.getGreen(), color.getBlue());
				
				this.addPlainMessage("Your color is " + colorString + "\n");
			} else if (ltext.startsWith(Command.changeEncryptionType)) {
				changeEncryptionType(text);
			} else if (ltext.equals(Command.seeEncryptionType)) {
				this.addPlainMessage("Your encryption type is " + this.encryptionType + "\n");
			} else if (ltext.startsWith(Command.seeHelp)) {
				this.addPlainMessage("Commands: /join, /disconnect, /name, /color, /encryption /help, /exit\n");
			} else if (ltext.startsWith(Command.join)) {
				join(text);
			} else if (ltext.startsWith(Command.disconnect)) {
				disconnect();
			} else if (ltext.startsWith(Command.exit) || ltext.startsWith(Command.quit)) {
				disconnect();
				System.exit(0);
			} else { //not a command, let's send the message instead
				/*
				Message m = new Message(text, this.color, this.name, this.encryptionType);
				String xmlMessage = XMLParser.msgToXML(m);
				*/
				
				String xmlMessage = text;
				
				sendMessage(xmlMessage);
				addMessage(xmlMessage);
			}
		}
	}
}