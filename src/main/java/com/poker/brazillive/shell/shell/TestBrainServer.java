package com.poker.brazillive.shell.shell;

import java.net.*;
import java.util.*;

import com.poker.brazillive.shell.util.FUtil;
import com.poker.brazillive.shell.util.Log;

import java.io.*;

public class TestBrainServer {

	private static final String ip = "192.168.229.1";
	private static final int port = 1031;
	
	public static void readSocket(Socket s) throws Exception {
    	byte[] bs = new byte[5000];
    	int c = s.getInputStream().read(bs);
    	if (c == -1) Log.log("RECEIVED: -1");	
    	else Log.log("RECEIVED: %s", new String(Arrays.copyOfRange(bs, 0, c)).trim());
	}
	public static void writeSocket(Socket s, String str) throws Exception {
		s.getOutputStream().write((str.trim()+"\r\n").getBytes("UTF-8"));
//		s.getOutputStream().flush();
		Log.log("SENT: %s", str);
	}
	
	public static void testSimple() throws Exception {
		while (true) {
	    	InetAddress addr = InetAddress.getByName(ip);
	    	Socket s = new Socket(addr, port);
//			System.out.println("Hit ENTER\n"); System.in.read(); System.in.read();
	    	s.getOutputStream().write("PING\r\n".getBytes());
			System.out.println("SENT PING");
	    	byte[] bs = new byte[20];
	    	int c = s.getInputStream().read(bs);
			String ans = new String(Arrays.copyOfRange(bs, 0, c));
//	    	String ans = new DataInputStream(s.getInputStream()).readUTF();
			System.out.println("ANSWER: '"+ans+"'");
	    	s.close();
	    	Thread.sleep(1000);
		}
	}
	public static void testGame() throws Exception {
    	InetAddress addr = InetAddress.getByName(ip);
    	Socket s = new Socket(addr, port);
//		writeSocket(s, "PING");
//		readSocket(s);
    	
    	Thread at = new Thread() {
    		public void run() {
    			try {
	    			while (true) {
	    				readSocket(s);
	    				Thread.sleep(100);
	    			}
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    	};
    	at.start();
    	
    	List<String> strs = FUtil.fRead("cl.in");
    	for (String str: strs) {
    		if (str.indexOf("#") == 0) continue;
    		if (str.isEmpty()) continue;
    		writeSocket(s, str);
//        	if (str.indexOf("BUTTONS") != -1) readSocket(s);
    	}
    	at.join();
	}
	
	public static void main(String[] args) throws Exception {
//		testSimple(); System.exit(0);
		testGame(); System.exit(0);
	}

}
