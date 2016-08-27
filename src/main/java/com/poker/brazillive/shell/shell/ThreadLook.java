package com.poker.brazillive.shell.shell;

import java.io.*;
import java.util.*;

import com.poker.brazillive.shell.util.*;

public class ThreadLook extends Thread {
	
	public static Object syncLook = new Object();
	private DataInputStream in;
	private Log log;
	
	public ThreadLook(DataInputStream in, Log log) throws Exception {
		this.in = in;
		this.setPriority(Thread.MAX_PRIORITY);
		this.log = log;
	}
	
	public void run() {
		int lookNum = 0;
		while (true) {
			try {

				List<WndData> newLook = new ArrayList<WndData>();
				long t = Misc.getTime();
				int wndCount = in.readByte();
				log.lo("Look num %d, Got wnd count: %d, waited %dms", lookNum++, wndCount, Misc.getTime()-t);
//				log.log("wndCount=%d", wndCount);
				
				for (int i = 0; i < wndCount; i++) {
//					Log.log("Wait for wnd data %d", i);
					WndData wndData = WndData.readSocket(in, log);
//					Log.log("Read new WndData: %s", wndData.getInfo());
					newLook.add(wndData);
				}
				
				long period = 0;
				synchronized (syncLook) {
					Shell.look = newLook;
					Shell.lookNum++;
					t = Misc.getTime();
					period = t - Shell.lookTime;
					Shell.lookTime = t;
				}
//				log.lo("Put look num %d, wnd count %d. Prev look was %dms ago", Shell.lookNum, Shell.look.size(), period);
				
				Thread.sleep(10);
			} catch (Throwable e) {
				e.printStackTrace();
				try { Thread.sleep(1000); } catch (Exception e2) { e2.printStackTrace(); }
			}
		}
	}
}
