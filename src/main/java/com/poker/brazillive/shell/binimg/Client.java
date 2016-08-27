package com.poker.brazillive.shell.binimg;

import java.net.*;
import java.io.*;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;

import com.poker.brazillive.shell.util.*;

public class Client {

	public static void main(String[] args) throws Exception {
		String imgFile = "C:/temp/20160310_172528.268_0x0018084E.png";
		final String SERVER_HOST = "188.213.172.20";
		final int SERVER_PORT = 2000;
		
		Rect[] rects = new Rect[]{
			new Rect(0,444,543,120,13),	
			new Rect(0,47,451,120,13),	
			new Rect(0,25,267,120,13),	
			new Rect(0,437,124,120,13),	
			new Rect(0,853,267,120,13),	
			new Rect(0,841,451,120,13),	
		};

		ImageIO.setUseCache(false);
		InetAddress ipAddress = InetAddress.getByName(SERVER_HOST);
	
		BufferedImage img = ImageIO.read(new File(imgFile));
		for (int i = 0; i < rects.length; i++) {
			Socket socket = new Socket(ipAddress, SERVER_PORT);
			DataInputStream in = new DataInputStream(socket.getInputStream());
	        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

	        BufferedImage imgRec = img.getSubimage(rects[i].x1, rects[i].y1-3, rects[i].getWidth(), rects[i].getHeight()+6);
	        BufferedImage imgBin = img.getSubimage(rects[i].x1, rects[i].y1, rects[i].getWidth(), rects[i].getHeight());
			out.writeInt(new Color(255,255,255).getRGB());
			out.writeInt(200); // radius
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(imgRec, "png", bos);
			byte[] bmpData = bos.toByteArray();
			out.writeInt(bmpData.length);
			out.write(bmpData);

			bos = new ByteArrayOutputStream();
			ImageIO.write(imgBin, "png", bos);
			bmpData = bos.toByteArray();
			out.writeInt(bmpData.length);
			out.write(bmpData);
			
			Log.log("ANSWER: %s %s", in.readInt(), in.readUTF());
			socket.close();
//			break;
		}
	}

}
