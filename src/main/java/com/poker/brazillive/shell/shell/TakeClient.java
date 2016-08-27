package com.poker.brazillive.shell.shell;

import java.net.*;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import com.poker.brazillive.shell.rec.*;
import com.poker.brazillive.shell.util.*;

public class TakeClient {

    public static final String SERVER_HOST = "127.0.0.1";
    public static final int SERVER_PORT = 1111;

    private static boolean recsLoaded = false;
    private static RecText[] recs;

    public static void testRecs(WndData wndData) throws Exception {
        String[] recNames = {
            "RecStack",
            "RecBet",
            "RecCardsSuit",
            "RecCardsSuitB",
            "RecCardsVal",
            "RecCardsValB",
            "RecGameId",
            "RecHolesSuit",
            "RecHolesSuitB",
            "RecHolesVal",
            "RecHolesValB",};

        if (!recsLoaded) {
            recs = new RecText[recNames.length];
            for (int i = 0; i < recs.length; i++) {
                recs[i] = new RecText("rec.cpa/" + recNames[i] + ".prop");
                recs[i].load("rec.cpa/" + recNames[i] + ".train/" + recNames[i] + ".save");
            }
        }

        Log.log("\n\nImage: %s", wndData.getImgFileName());
        for (int i = 0; i < recs.length; i++) {
            RecText rec = recs[i];
            String[] recRes = rec.rec(wndData.getImg());
            Log.log("%s: %s", recNames[i], Arrays.toString(recRes));
        }
    }

    public static void main(String[] args) throws Exception {

        ImageIO.setUseCache(false);
        Shell.cfg = new Properties();
        Shell.cfg.load(new FileInputStream("shell.cfg"));
        Shell.uname = "Brood";

        InetAddress ipAddress = InetAddress.getByName(SERVER_HOST);
        Socket socket = new Socket(ipAddress, SERVER_PORT);
        System.out.println("Client socket created");
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

//        while (true) {
//        	System.out.print(in.readByte()+" ");
//        	System.out.print(in.readUTF());
//        }
        Map<Integer, Long> lastWrite = new HashMap<Integer, Long>();
        Map<Integer, Integer> count = new HashMap<Integer, Integer>();

//        RecTrueFalse recButtons = new RecTrueFalse("rec.cpa/RecButtons.prop");
        Map<Integer, Boolean> prevButtons = new HashMap<Integer, Boolean>();

//        RecText recCardVals = new RecText("rec.cpa/RecCardsVal.prop");
        int c = 0;

        while (true) {

            int wndCount = in.readByte();
//			Log.log("wndCount=%d", wndCount);

            for (int i = 0; i < wndCount; i++) {
                WndData wndData = WndData.readSocket(in, Log.defl);
                Log.log("Title: '%s'", wndData.title);
                if (wndData.title == null) {
                    continue;
                }
                if (lastWrite.get(wndData.hwnd) == null) {
                    lastWrite.put(wndData.hwnd, Misc.getTime());
                    String ahkExe = "\"" + Action.AHK_PATH + "\" \"" + Shell.cfg.getProperty("ahk.control") + "\" move 0x" + String.format("%08X", wndData.hwnd) + " " + 100 + " " + 0;
//		    		synchronized (Shell.clickSync) {
//		    			Misc.sysCall(ahkExe);
//		    		}
//		    		String ahkExe = "\""+Action.AHK_PATH+"\" \""+Shell.cfg.getProperty("ahk.control")+"\" resize 0x"+String.format("%08X", wndData.hwnd)+" "+1015+" "+1200;
//		    		synchronized (Shell.clickSync) {
//		    			Misc.sysCall(ahkExe);
//		    		}
                }
                if (Misc.getTime() - lastWrite.get(wndData.hwnd) >= 300) {
                    wndData.saveImg();
                    lastWrite.put(wndData.hwnd, Misc.getTime());
//		        	testRecs(wndData);
                }
            }
        }

        //socket.close();
    }
}
