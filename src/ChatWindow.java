import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Main graphics display class, handles tab behavior.
 */
public class ChatWindow extends JFrame implements ChangeListener, WindowListener {
	private static final long serialVersionUID = 1L;
		
	private static int defaultPort = 8080;
	
	private static Color defaultColor = Color.BLACK;
	private static String defaultName = "Unnamed";
	private static String defaultEncryptionType = "AES";
	
	private JTabbedPane tabbedPane;

	public ChatWindow () {
		tabbedPane = new JTabbedPane();
		tabbedPane.setTabPlacement(JTabbedPane.LEFT);
		
		tabbedPane.addChangeListener(this);
		
		host(defaultPort, defaultName, defaultColor, defaultEncryptionType);

		add(tabbedPane);

		setSize(new Dimension(500, 400));
		addWindowListener(this);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		
		stateChanged(null); // sets focus
	}	

	public void closeTab(RoomViewer rv) {
		if (tabbedPane.getTabCount() == 1) {
			setVisible(false);
			dispose();
		} else tabbedPane.remove(rv);
	}
	
	public void stateChanged(ChangeEvent ce) {
		((RoomViewer)tabbedPane.getSelectedComponent()).requestFocus();
	}
	
	public void host(int port, String name, Color color, String encryptionType) {
		String roomName = UUID.randomUUID().toString().substring(0,8);
		tabbedPane.addTab(roomName, RoomViewer.hostRoom(this, port, name, color, encryptionType, roomName));
	}

	public void join(String ip, int port, String message, String name, Color color, String encryptionType) throws IOException {
		String roomName = UUID.randomUUID().toString().substring(0, 8); //get a random name for the room...
		tabbedPane.addTab(roomName, RoomViewer.joinRoom(this, ip, port, message, name, color, encryptionType, roomName));
	}
	
	@Override
	public void windowClosing(WindowEvent e) {
		System.out.println("HEY");
		for (int i = tabbedPane.getTabCount()-1; i >= 0; i--) {
			((RoomViewer)tabbedPane.getComponentAt(i)).close();
		}
	}
	
	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
}