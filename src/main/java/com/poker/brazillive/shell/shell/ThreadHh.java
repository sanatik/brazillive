package com.poker.brazillive.shell.shell;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.net.*;

import com.poker.brazillive.shell.util.*;

public class ThreadHh extends Thread {
	
	private class ThreadHhRead extends Thread {
		private Table table;
		public ThreadHhRead(Table table) {
			this.table = table;
		}
		
		public void run() {
			try {
				while (true) {
					String s = Shell.readStr(this.table.hhSocket);
					this.table.log.lo("RECIEVED to hh socket: '%s'", s.trim());
					
					byte[] bs = s.getBytes();
					boolean allZeros = true;
					for (int i = 0; i < 100; i++) if (bs[i] != 0) {
						allZeros = false;
						break;
					}
					if (allZeros) {
//						this.table.log.lo("ERROR in hh thread: a lot of zeros. Stop thread");
//						break;

						this.table.log.lo("ERROR in hh thread: a lot of zeros. Reset hh socket");
						Socket soc = this.table.hhSocket;
						soc.close();
				    	this.table.hhSocket = new Socket(soc.getInetAddress(), soc.getPort());
				    	String ss = this.table.startSessionCmd();
						Shell.sendStr(this.table.hhSocket, ss+Table.eol);
					}
					
					Thread.sleep(1000);
				}
			} catch (SocketException e) {
				if (e.getMessage().equalsIgnoreCase("Socket closed") || e.getMessage().equalsIgnoreCase("Socket is closed")) {
					this.table.log.lo("ThreadHhRead: socket is closed. Stop");
				} else {
					this.table.log.lo("Exception in ThreadHhRead:\n%s", Misc.stacktrace2Str(e));

					try {
						this.table.scheduleToCloseNow("Hh read thread is stopped");
					} catch (Exception e2) {
						this.table.log.lo("%s", Misc.stacktrace2Str(e2));
					}
				}
			} catch (Throwable e) {
				this.table.log.lo("Exception in ThreadHhRead:\n%s", Misc.stacktrace2Str(e));

				try {
					this.table.scheduleToCloseNow("Hh read thread is stopped");
				} catch (Exception e2) {
					this.table.log.lo("%s", Misc.stacktrace2Str(e2));
				}
			}
		}
	}
	
	private Table table;
	private DataOutputStream dos;
	private ThreadHhRead threadHhRead;
	public List<String> hhStrs;
	
	public ThreadHh(Table table) throws Exception {
		this.table = table;
	}

	public void run() {
		
		try {
			this.threadHhRead = new ThreadHhRead(this.table);
			this.threadHhRead.start();
		
			Map<String, FileInputStream> fs = new HashMap<String, FileInputStream>();

			while (true) {
				try {
					Thread.sleep(1000);
					
					if (this.table.tableLook == null) continue;
					String hhf = this.table.tableLook.hhFile;
					if (hhf == null) continue;
					
					File f = new File(hhf);
					if (!fs.containsKey(hhf)) {
						if (f.exists()) {
							fs.put(hhf, new FileInputStream(f));
							fs.get(hhf).read(new byte[(int)f.length()]); // skip existing content
						} else {
							fs.put(hhf, null);
						}
					}
					if (!f.exists()) continue;
					if (f.exists() && fs.get(hhf) == null) 
						fs.put(hhf, new FileInputStream(f));
					
					FileInputStream fis = fs.get(hhf);
					byte[] bs = new byte[100000];
					int len = fis.read(bs);
					if (len == -1) continue;
					byte[] tmp = new byte[len]; System.arraycopy(bs, 0, tmp, 0, len); bs = tmp;

//					this.table.hhSocket.getOutputStream().write(bs);

					String str = new String(bs);
					str = Shell.room.getHhSign()+"\r\n"+str;
					String out = "Sent to HH socket:\n";
					for (String s: str.split("[\\r\\n]+")) {
						Shell.sendStr(this.table.hhSocket, s+"\r\n");
						
						out += "\t\t\t\t"+s+"\n";
						synchronized (this.hhStrs) {
							this.hhStrs.add(s);
							while (this.hhStrs.size() > 50) this.hhStrs.remove(0);
						}
					}
					table.log.lo("%s", out);
					
				} catch (SocketException e) {
					if (e.getMessage().equalsIgnoreCase("Socket closed") || e.getMessage().equalsIgnoreCase("Socket is closed")) {
						this.table.log.lo("ThreadHh: socket is closed. Stop");
					} else {
						this.table.log.lo("Exception in ThreadHh:\n%s", Misc.stacktrace2Str(e));
					}
					break;
				}
			}
		} catch (Throwable e) {
			this.table.log.lo("ERROR! Hh thread exception: Stop\n%s", Misc.stacktrace2Str(e));
			if (!this.table.toBeMigrated) this.table.scheduleToCloseNow("Hh thread exception");
		}
	}
}
