import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

public class FileTransfer {
	//public static final File outputPath = new File("D:\\Users\\Joakim\\Downloads");//new File(new File(System.getProperty("user.home")), "Downloads");
	public static final File outputPath = new File(new File(System.getProperty("user.home")), "Downloads");
	private static int numBytesPerChunk = 1;
	
	@SuppressWarnings("serial")
	private static class FileProgressBar extends JProgressBar {
		public FileProgressBar(int len) {
			super(0,len);
			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.add(this);
			frame.setSize(new Dimension(400,60));
			frame.setVisible(true);
		}
	}
	
	public static void sendFile(final RoomViewer rv, final byte[] fileToBeSentAsBytes, final int port, final String fileRecipientIP) {
		//Assuming that when the other user sent their OK, they also started waiting for files
		new Thread(new Runnable() { public void run() {
			Socket socket = null;
			DataOutputStream dos = null;
			try {
				//Get socket connection
				try {
					socket = new Socket(fileRecipientIP, port);
					dos = new DataOutputStream(socket.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
					rv.addPlainMessage("Problem initiating file sending connection");
					return;
				}
				
				int len = fileToBeSentAsBytes.length;
				int from = 0;
				int to = numBytesPerChunk;
				
				FileProgressBar progressBar = new FileProgressBar(len);
				
				byte[] chunk;
				if (to >= len) { //we can send the entire file as one chunk
					System.out.println("to>=len, sends entire file as one chunk");
					chunk = fileToBeSentAsBytes;
					try {
						dos.write(chunk);
					} catch (IOException e) {
						e.printStackTrace();
						rv.addPlainMessage("Failure when writing file");
						return;
					}
				} else while (to <= len) {
					if (to > len) to = len;
					
					//System.out.println("from=" + String.valueOf(from) + ", to=" + String.valueOf(to));
					chunk = Arrays.copyOfRange(fileToBeSentAsBytes, from, to);
					try {
						dos.write(chunk);
						progressBar.setValue(to);
						
					} catch (IOException e) {
						e.printStackTrace();
						rv.addPlainMessage("Failure when writing file");
						return;
					}
					
					from += numBytesPerChunk;
					to += numBytesPerChunk;
				}
								
				rv.addPlainMessage("File transfer complete.");
			} finally {
				try {
					if (socket != null) socket.close();
					if (dos != null) dos.close();
				} catch (IOException e) {
					e.printStackTrace();
					rv.addPlainMessage("Problem closing connection after transferring file");
				}
			}
		}}).start();
	}
	
	public static void receiveFile(final RoomViewer rv, final String fileToBeReceivedName, final int fileToBeReceivedSize,
			final String fileToBeReceivedEncryptionType, final String fileToBeReceivedKey, final int port) {
		new Thread(new Runnable() { public void run() {
			byte[] data = null;
			
			ServerSocket ss = null;
			Socket s = null;
			DataInputStream dis = null;
			try {
				try {
					ss = new ServerSocket(port);
				} catch (IOException e) {
					e.printStackTrace();
					rv.addPlainMessage("Problem starting a ServerSocket when trying to read file.");
					return;
				}
				
				rv.addPlainMessage("Awaiting file transfer.");
	
				try {
					s = ss.accept();
				    dis = new DataInputStream(s.getInputStream());
				} catch (IOException e) {
					e.printStackTrace();
					rv.addPlainMessage("Problem accepting new socket connection.");
					return;
				}
				
				rv.addPlainMessage("File transfer initiated.");
				
				FileProgressBar progressBar = new FileProgressBar(fileToBeReceivedSize);
				
				data = new byte[fileToBeReceivedSize];
				
				System.out.println("Data is of length:" + String.valueOf(data.length));
				
				int numBytesLeftToRead = fileToBeReceivedSize;
				int off = 0;
				int numBytesRead = 0;
				while (numBytesLeftToRead > 0 && off < fileToBeReceivedSize) {
					try {
						//System.out.println("Trying to dis.read(data, off=" + String.valueOf(off) + ", numBytesPerChunk=" + String.valueOf(numBytesPerChunk)+")");
						
						/*
						if (numBytesLeftToRead < numBytesPerChunk) {
							numBytesRead = dis.read(data, off, numBytesLeftToRead);
						} else {
							numBytesRead = dis.read(data, off, numBytesPerChunk);
						}*/
						
						data[off] = dis.readByte();
						
						//System.out.println("byte " + off + " = " + data[off]);
						
						/*
						System.out.println("trying to write buffer of size " + String.valueOf(buffer.length) + " to data");
						for (int i = 0; i < buffer.length; i++) {
							if ((off+i) > data.length) {
								break;
							}
							data[off+i] = buffer[i];
						}
						buffer = new byte[numBytesPerChunk];
						
						System.out.println("buffer was written to data");
						*/
						
						//numBytesRead = in.read(data, off, numBytesPerChunk);
					} catch (IOException e) {
						e.printStackTrace();
						rv.addPlainMessage("Something went wrong trying to read " + numBytesPerChunk + " bytes.");
						return;
					}
					
					//off += numBytesRead;
					off += numBytesPerChunk;
					numBytesLeftToRead -= numBytesRead;
					progressBar.setValue(off);
				}
			} finally {
				try {
					if (ss != null) ss.close();
					if (s != null) s.close();
					if (dis != null) dis.close();
				} catch (IOException e) {
					e.printStackTrace();
					rv.addPlainMessage("Problem closing connection after file transfer");
				}
			}
						
			rv.addPlainMessage("File transfer complete.");
									
			if (!fileToBeReceivedEncryptionType.isEmpty()) try {
				data = Crypto.decrypt(data, fileToBeReceivedKey, fileToBeReceivedEncryptionType);
			} catch (EncryptionException e) {
				rv.addPlainMessage("File decryption failed");
				return;
			} catch (UnsupportedEncryptionTypeException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			File file = new File(outputPath, fileToBeReceivedName);
			int index = fileToBeReceivedName.lastIndexOf('.');
			String nameWithoutExt = index == -1 ? fileToBeReceivedName : fileToBeReceivedName.substring(0,index);
			String ext = index == -1 ? "" : fileToBeReceivedName.substring(index);
			for (int i = 1; file.exists() && i < 1000; i++)
				file = new File(outputPath, nameWithoutExt + " (" + i + ")" + ext);
			if (file.exists()) {
				rv.addPlainMessage("Couldn't write to file (all names taken)");
				return;
			}
			
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				rv.addPlainMessage("Failed to write to file (FileNotFoundException");
				return;
			}
			try {
				fos.write(data);
			} catch (IOException e) {
				e.printStackTrace();
				rv.addPlainMessage("Failed to write to file (IOException");
			} finally {
				try {
					fos.close();
					rv.addPlainMessage("File saved.");
				} catch (IOException e) {
					e.printStackTrace();
					rv.addPlainMessage("Failed to close file output stream (IOException");
				}
			}
		}}).start();
	}
}