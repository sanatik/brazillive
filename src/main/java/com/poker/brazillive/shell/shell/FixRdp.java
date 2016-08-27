package com.poker.brazillive.shell.shell;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import com.poker.brazillive.shell.util.FUtil;
import com.poker.brazillive.shell.util.Log;
import com.poker.brazillive.shell.util.Misc;

public class FixRdp {

	private static final int PERIOD_SEC = 10*60; ///
	private static final int MAX_MEM_MB = 400; ///
//	private static final int PERIOD_SEC = 30; ///
//	private static final int MAX_MEM_MB = 400; ///
	
	
	private static Properties cfg = new Properties();
	private static int maxUn = -1;

	private static Map<Integer, Integer> u2mem() throws Exception {
		Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
	    Process p = Runtime.getRuntime().exec("tasklist /V /FO CSV");
	    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line;
	    while ((line = input.readLine()) != null) {
	    	if (!line.contains("mstsc.exe")) continue;
	    	String[] sr = line.split("\",\"");
	    	String uns = sr[sr.length-1].split(" ")[0].replaceAll("\"", "").replaceAll("[^\\d]", "");
	    	String mems = sr[4].replaceAll("[^\\d]", "");
	    	try {
	    		ret.put(Integer.parseInt(uns), Integer.parseInt(mems));
	    	} catch (NumberFormatException e) {
	    		continue;
	    	}
	    }
	    return ret;
	}
	
	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	private static void fRepl(File fromFile, File toFile, String find, String repl) throws Exception {
		FileInputStream fis = new FileInputStream(fromFile);
		byte[] bs = new byte[(int)fromFile.length()];
		fis.read(bs);
		fis.close();
		
		String str = new String(bs, Charset.forName("UTF-16LE"));
		str = str.replaceAll(find, repl);
		FileOutputStream fos = new FileOutputStream(toFile);
		fos.write(str.getBytes(Charset.forName("UTF-16LE")));
		fos.close();
	}
	private static String getRdpTitle(int un) throws Exception {
		return "u"+un+" - localhost - Remote Desktop Connection";
	}
	private static void rdpClose(int un) throws Exception {
		Action.wndClose(getRdpTitle(un));
		Thread.sleep(1000);
	}
	private static void rdpOpen(int un) throws Exception {
        File rdpExe = new File("mstsc.exe");
        File bat = new File("tmp.bat");
        File rdpFrom = new File("u.RDP");
        File rdp = new File("u"+un+".RDP");
        fRepl(rdpFrom, rdp, "username:s:u1", "username:s:u"+un);

		FUtil.fPut(bat.getAbsolutePath(), String.format("start %s %s\n", rdpExe.getName(), rdp.getAbsolutePath()));
        Process p = Runtime.getRuntime().exec("cmd /C "+bat.getAbsolutePath());
		p.waitFor();
		Thread.sleep(2000);

		Action.sendInp("medaka{!}{!}"+un+"{Enter}");
		Thread.sleep(2000);
		int w = 230;
		Action.wndResize(getRdpTitle(un), w, 35);
		Thread.sleep(1000);
		Action.wndMove(getRdpTitle(un), 1920-w, (un-1)*40+(un > maxUn/2?30:0));
		Thread.sleep(1000);
		
		bat.delete();
		rdp.delete();
	}

	private static int getMaxUn() throws Exception {
		int maxUn = 0;
		for (Object ko: cfg.keySet()) {
			String k = (String)ko;
			if (!k.matches("^u\\d+\\..*")) continue;
			int un = Integer.parseInt(k.split("\\.")[0].replaceAll("[^\\d]", ""));
			if (un > maxUn) maxUn = un;
		}
		return maxUn; 
	}
	
	private static void openRdps(Map<Integer, Integer> u2mem) throws Exception {
		for (int un = 1; un <= maxUn; un++) {
			if (u2mem.containsKey(un)) continue;
			rdpOpen(un);
			//break;
		}
	}

	public static void main(String[] args) throws Exception {
		cfg.load(new FileInputStream("shell.cfg"));
//		maxUn = getMaxUn();
		maxUn = Integer.parseInt(args[0]);
		Log.log("Max user num: %d", maxUn);
		long timeLastRestart = 0;

//		for (int un = 1; un <= maxUn; un++) rdpClose(un); // Tries to close not existing rdps and takes too much time
		
        while (true) {
        	Map<Integer, Integer> u2mem = u2mem();
       		openRdps(u2mem);
        
       		if (Misc.getTime() - timeLastRestart > 1000*PERIOD_SEC) {
            	Log.log("Check mem and restart if needed");
            	Log.log("%s", u2mem);
	        	for (Integer un: u2mem.keySet()) {
	        		if (u2mem.get(un)/1024 < MAX_MEM_MB) continue;
	        		Log.log("Restarted RDP for u%d", un);
	        		rdpClose(un);
	        		rdpOpen(un);
	        	}
	        	timeLastRestart = Misc.getTime();
       		}
			Thread.sleep(5*1000);
        }
	}

}
