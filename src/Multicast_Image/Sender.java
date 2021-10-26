package Multicast_Image;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public class Sender {
	public static String IP_ADDRESS =  "225.4.5.6";
	public static int PORT = 4444;
	
	public static int HEADER_SIZE = 8;
	public static int SESSION_START = 128;
	public static int SESSION_END = 64;
	public static int DATAGRAM_MAX_SIZE = 65507 - HEADER_SIZE;
	
	public static byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, format, baos);
		return baos.toByteArray();
	}
	
	public static BufferedImage scale(BufferedImage source, int w, int h) {
		Image image = source.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
		BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = result.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return result;
	}
	
	public static BufferedImage shrink(BufferedImage source, double factor) {
		int w = (int) (source.getWidth() * factor);
		int h = (int) (source.getHeight() * factor);
		return scale(source, w, h);
	}
	
	private boolean sendImage(byte[] imageData, String multicastAddress, int port) {
		InetAddress inetAdress;
		boolean ret = false;
		int TTL = 2;

		try {
			inetAdress = InetAddress.getByName(multicastAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return ret;
		}

		MulticastSocket multicastSocket = null;

		try {
			multicastSocket = new MulticastSocket();
			multicastSocket.setTimeToLive(TTL);
			DatagramPacket dataPacket = new DatagramPacket(imageData, imageData.length,	inetAdress, port);
			multicastSocket.send(dataPacket);
			ret = true;
		} catch (IOException e) {
			e.printStackTrace();
			ret = false;
		} finally {
			if (multicastSocket != null) {
				multicastSocket.close();
			}
		}
		return ret;
	}
	
	public static void main(String[] args) {
		Sender sender = new Sender();
		final File[] file =new File[1];
		
		JFrame mainFrame = new JFrame();
        mainFrame.setSize(480, 480);
        mainFrame.getContentPane().setLayout(new GridLayout(3, 1));
        ImageIcon icon = new ImageIcon("D:\\background.jpg","image");
        JLabel headerLabel = new JLabel("",icon, JLabel.CENTER);
        headerLabel.setForeground(new Color(255, 255, 255));
        headerLabel.setBackground(new Color(210, 105, 30));
        headerLabel.setFont(new Font("Tahoma", Font.BOLD, 27));
        JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setSize(480, 50);
        JLabel msglabel = new JLabel("", JLabel.CENTER);
        msglabel.setFont(new Font("Tahoma", Font.PLAIN, 18));
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        mainFrame.getContentPane().add(headerLabel);
        mainFrame.getContentPane().add(msglabel);
        mainFrame.getContentPane().add(statusLabel);
        mainFrame.setVisible(true);
        mainFrame.setTitle("Multicast Server");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		String memberName="<html><h1>Group 3</h1>"+
                "Cao Minh Phat - 1720169"+
                "<br>Vo Phung Quang - 1720188" +
                "<br>Nguyen Van Son - 1720203" +
                "</html>";
        headerLabel.setText("Multicast Sending Image");
        headerLabel.setOpaque(true);
        msglabel.setText(memberName);
        
        JPanel panel = new JPanel();
        panel.setBackground(new Color(245, 245, 245));
        panel.setSize(480, 160);
        GridLayout layout = new GridLayout(1, 2);
        layout.setHgap(5);
        layout.setVgap(5);
 
        panel.setLayout(layout);
        JButton jbChooseImage = new JButton();
        jbChooseImage.setIcon(new ImageIcon("D:\\folder.png"));
        panel.add(jbChooseImage);
    
        JButton jbSendImage = new JButton();
        jbSendImage.setIcon(new ImageIcon("D:\\muiten.png"));
        panel.add(jbSendImage);
        statusLabel.add(panel);
        mainFrame.setVisible(true);
		
		jbChooseImage.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jFileChooser = new JFileChooser();
				jFileChooser.setDialogTitle("Choose a image to send.");
				if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					 file[0] = jFileChooser.getSelectedFile();
					 System.out.println(file[0]);
				 }
			}
		});
		
		jbSendImage.addActionListener(new ActionListener() {
			int sessionNumber = 0;
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					BufferedImage image =ImageIO.read(file[0]);
					image = shrink(image, 0.5);
					byte[] imageByteArray = bufferedImageToByteArray(image, "jpg");
					int packets = (int) Math.ceil(imageByteArray.length / (float)DATAGRAM_MAX_SIZE);
					System.out.println(packets);
					if(packets > 255) {
						System.out.println("Image is too large to be transmitted!");
					}
					for(int i = 0; i <= packets; i++) {
						int flags = 0;
						flags = i == 0 ? flags | SESSION_START: flags;
						flags = (i + 1) * DATAGRAM_MAX_SIZE > imageByteArray.length ? flags | SESSION_END : flags;

						int size = (flags & SESSION_END) != SESSION_END ? DATAGRAM_MAX_SIZE : imageByteArray.length - i * DATAGRAM_MAX_SIZE;

						/* Set additional header */
						byte[] data = new byte[HEADER_SIZE + size];
						data[0] = (byte)flags;
						data[1] = (byte)sessionNumber;
						data[2] = (byte)packets;
						data[3] = (byte)(DATAGRAM_MAX_SIZE >> 8);
						data[4] = (byte)DATAGRAM_MAX_SIZE;
						data[5] = (byte)i;
						data[6] = (byte)(size >> 8);
						data[7] = (byte)size;

						System.arraycopy(imageByteArray, i * DATAGRAM_MAX_SIZE, data, HEADER_SIZE, size);

						sender.sendImage(data, IP_ADDRESS, PORT);

						if((flags & SESSION_END) == SESSION_END) break;
					}
					Thread.sleep(2000);
					sessionNumber = sessionNumber < 255 ? ++sessionNumber : 0 ;
				} catch(Exception ex) {
					ex.printStackTrace();
				}	
			}
		});
		
	}
	
}
