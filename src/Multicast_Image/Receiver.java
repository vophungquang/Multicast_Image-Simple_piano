package Multicast_Image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Receiver {
	public static String IP_ADDRESS = "225.4.5.6";

	public static int PORT = 4444;
	
	public static int HEADER_SIZE = 8;

	public static int SESSION_START = 128;

	public static int SESSION_END = 64;
	
	private static int DATAGRAM_MAX_SIZE = 65507;

	JFrame frame;
	
	private void receiveImages(String multicastAddress, int port) {
		boolean debug = true;
		InetAddress inetAddress = null;
		MulticastSocket multicastSocket = null;
		
		JLabel labelImage = new JLabel();
		frame = new JFrame("Multicast Image Receiver");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(labelImage);
		frame.setSize(300, 300);
		frame.setVisible(true);
		try {
			inetAddress = InetAddress.getByName(multicastAddress);
			multicastSocket = new MulticastSocket(port);
			multicastSocket.joinGroup(inetAddress);
			
			int currentSession = -1;
			int slicesStored = 0;
			int[] slicesCol = null;
			byte[] imageData = null;
			boolean sessionAvailable = false;
			
			byte[] buffer = new byte[DATAGRAM_MAX_SIZE];
			while (true) {
				DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
				multicastSocket.receive(datagramPacket);
				byte[] data = datagramPacket.getData();
				
				short session = (short) (data[1] & 0xff);
				short slices = (short) (data[2] & 0xff);
				int maxPacketSize = (int) ((data[3] & 0xff) << 8 | (data[4] & 0xff));
				short slice = (short) (data[5] & 0xff);
				int size = (int) ((data[6] & 0xff) << 8 | (data[7] & 0xff));
				if (debug) {
					System.out.println("------------- PACKET -------------");
					System.out.println("SESSION_START = " + ((data[0] & SESSION_START) == SESSION_START));
					System.out.println("SSESSION_END = " + ((data[0] & SESSION_END) == SESSION_END));
					System.out.println("SESSION NR = " + session);
					System.out.println("SLICES = " + slices);
					System.out.println("MAX PACKET SIZE = " + maxPacketSize);
					System.out.println("SLICE NR = " + slice);
					System.out.println("SIZE = " + size);
					System.out.println("------------- PACKET -------------\n");
				}
				
				if ((data[0] & SESSION_START) == SESSION_START) {
					if (session != currentSession) {
						currentSession = session;
						slicesStored = 0;
						imageData = new byte[slices * maxPacketSize];
						slicesCol = new int[slices];
						sessionAvailable = true;
					}
				}
				if (sessionAvailable && session == currentSession) {
					if (slicesCol != null && slicesCol[slice] == 0) {
						slicesCol[slice] = 1;
						System.arraycopy(data, HEADER_SIZE, imageData, slice
								* maxPacketSize, size);
						slicesStored++;
					}
				}
				
				if (slicesStored == slices) {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							imageData);
					BufferedImage image = ImageIO.read(bis);
					labelImage.setIcon(new ImageIcon(image));

					frame.pack();
				}
				if (debug) {
					System.out.println("STORED SLICES: " + slicesStored);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(multicastSocket != null) {
				try {
					multicastSocket.leaveGroup(inetAddress);
					multicastSocket.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static void main(String[] args) {
		Receiver receiver = new Receiver();
		receiver.receiveImages(IP_ADDRESS, PORT);
	}
}
