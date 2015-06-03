import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Main {
	public static void main(String[] args) {
		Object[] options = {"Server", "Client"};
		int choice = JOptionPane.showOptionDialog(new JFrame(), "Do you want to run a server or a client?",
				"Server/Client choice",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null, //no custom icon 
				options, options[0]);
				
		if (choice == JOptionPane.YES_OPTION) {
			System.out.println("User chose to become a Server");
			//new Server();
		} else if (choice == JOptionPane.NO_OPTION) {
			System.out.println("User chose to become a Client");
			//new Client();
		} else {
			System.out.println("User didn't want to become either a server or a client");
		}
	}
}
