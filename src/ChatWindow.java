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

/*
 * Main graphics output class
 */
public class ChatWindow extends JFrame implements ActionListener, KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextArea textArea;
	private JTextField textField;
	
	public static class Command {
		public static String changeName = "/name ";
		public static String changeColor = "/color ";
		public static String join = "/join ";
		public static String disconnect = "/disconnect";
		public static String exit = "/exit";
		public static String quit = "/quit";
	}
	
	private String name;
	private Color color;
	
	private Client client;
	
	private ArrayList<String> previousCommands;
	private int previousCommandIndex = 0;
	
	public ChatWindow (Client _client) {
		name = "Default Name";
		color = Color.BLACK;
		previousCommands = new ArrayList<String>();
		previousCommandIndex = 0;
		
		client = _client;
		
		textArea = new JTextArea(client.startMessage());
		textField = new JTextField();
						
		textField.addActionListener(this);
		textField.addKeyListener(this);

		add(textArea, BorderLayout.CENTER);
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
	
	public void addMessage(String message) {
		textArea.append(message + "\n");
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
			
			String errorMsg = "Something went wrong when creating a connection";
			
			if (!badInput) {
				try {
					client.addConnection(ip, port);
					addMessage("Added connection to ip " + ip);
					
				} catch (UnknownHostException e) {
                    System.out.println(errorMsg);
                    addMessage(errorMsg);
                } catch (IOException e) {
                    System.out.println(errorMsg);
                    addMessage(errorMsg);
                }
			}
		} else {
			badInput = true;
		}
		
		if (badInput) {
			addMessage("Usage: /join ip:port");
		}
	}
	
	private void changeColor(String message) {
		String newColorString = message.substring(Command.changeColor.length());
		
		//addMessage("Current color: (" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")");
		
		String[] newColorStringSplit = newColorString.split(" ");
		if (newColorStringSplit.length != 3) {
			addMessage("Usage: /color r g b\nExample: /color 255 0 0       #sets the color to red (=255,0,0)");
			return;
		}
		
		int r = Integer.parseInt(newColorStringSplit[0]);
		int g = Integer.parseInt(newColorStringSplit[1]);
		int b = Integer.parseInt(newColorStringSplit[2]);
		
		try {
			Color newColor = new Color(r,g,b);
			
			sendMessage(this.name + " changed color to (" + r + ", " + g + ", " + b + ")");
			addMessage("Changed color to (" + r + ", " + g + ", " + b + ")");
			this.color = newColor;
		} catch (NumberFormatException e) {
			addMessage("Problem setting color. Error: " + e.toString());
			return;
		}
	}
	
	private void changeName(String message) {
		String newName = message.substring(Command.changeName.length());
		
		sendMessage(name + " changed name to " + newName);
		addMessage("Changed name to " + newName);
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
			String message = textField.getText();
			textField.setText("");
			
			if (message.isEmpty())
				return;
					
			previousCommands.add(message);
			if (previousCommandIndex != previousCommands.size()) {
				previousCommandIndex += 1;
			}

			
			
			if (message.startsWith(Command.changeName)) {
				changeName(message);
			} else if (message.startsWith(Command.changeColor)) {
				changeColor(message);
			} else if (message.startsWith(Command.join)) {
				join(message);
			} else if (message.startsWith(Command.disconnect)) {
				disconnect();
			} else if (message.startsWith(Command.exit) || message.startsWith(Command.quit)) {
				disconnect();
				System.exit(0);
			} else {
				sendMessage(message);
				addMessage(message);
			}
		}
	}
}