package com.poker.brazillive.shell.shell;

import java.util.*;
import java.io.*;

import com.poker.brazillive.shell.util.*;

public class ThreadMonitor extends Thread {
	
	private Properties cfg;
	private Map<Integer, Table> tables;
	private int noBtnsTimeout;
	private int noBtnsAtAllTimeout;
	
	public ThreadMonitor(Properties cfg, Map<Integer, Table> tables) {
		this.cfg = cfg;
		this.tables = tables;
		this.noBtnsTimeout = Integer.parseInt(cfg.getProperty("close.table.if.no.buttons.during.sec"));
		this.noBtnsAtAllTimeout = Integer.parseInt(cfg.getProperty("restart.client.if.no.buttons.during.sec"));
	}
	
	private long timeImgsLastCheck = 0;
	private void checkImgsFolder() throws Exception {
		if (Misc.getTime() - timeImgsLastCheck < 5*60*1000) return;
		File imgs = new File(Shell.uname+"/imgs");
		if (!imgs.exists()) {
			Log.log("WARNING! Imgs folder is not found");
			return;
		} else {
			
			long maxAge = Math.round(Misc.getPropDouble(cfg, "imgs.max.age.hours")*60*60*1000);
			
			long st = Misc.getTime();
			int fc = 0, dc = 0;
			File[] folders = FUtil.dRead(imgs.getAbsolutePath());
			for (File folder: folders) {
//				Log.log("Check folder: %s", folder.getName());
				File[] files = folder.listFiles();
				Arrays.sort(files, new Comparator<File>() {
					public int compare (File f1, File f2) {
						return f1.getName().compareTo(f2.getName());
					}
				});
				
				for (File f: files) {
//					Log.log("Check file: %s", f.getName());
					if (Misc.getTime() - f.lastModified() > maxAge) {
						boolean r = f.delete();
						if (!r) Log.log("ERROR! Cannot remove file: %s", f.getAbsolutePath());
						fc++;
					} else {
						break;
					}
				}
				if (folder.listFiles().length == 0) {
					boolean r = folder.delete();
					if (!r) Log.log("ERROR! Cannot remove empty folder: %s", folder.getAbsolutePath());
					dc++;
				}
				Thread.sleep(100);
			}
			Log.log("Imgs clean is done in %d secs. Removed %d files and %d folders", (Misc.getTime()-st)/1000, fc, dc);
			
		}
		timeImgsLastCheck = Misc.getTime();
	}

	private long timeLogsLastCheck = 0;
	private void checkLogsFolder() throws Exception {
		final int checkPeriod = 5*60*1000;
		final int oldFileAge = 12*60*60*1000;
//		final int checkPeriod = 30*1000;
//		final int oldFileAge = 30*1000;
		
		if (Misc.getTime() - timeLogsLastCheck < checkPeriod) return;
		File tlogs = new File(Shell.uname+"/tlogs");
		new File(tlogs.getAbsolutePath()+"/arch").mkdirs();
		
		if (!tlogs.exists()) {
			Log.log("WARNING! tlogs folder is not found");
			return;
		} else {
			
			File[] fs = tlogs.listFiles();
			for (File f: fs) {
				if (Misc.getTime() - f.lastModified() < oldFileAge) continue;
				File af = new File(f.getParentFile().getAbsolutePath()+"/arch/"+f.getName());
				boolean res = f.renameTo(af);
				if (!res) Log.log("WARNING! Cannot move file to arch. From: %s, to: %s", f.getAbsolutePath(), af.getAbsolutePath());
			}
		}
		timeLogsLastCheck = Misc.getTime();
	}

//	private Set<Long> unsuccJoins = new HashSet<Long>();
//	private void checkLobbyOpensWindows() throws Exception {
//		if (Shell.sleeping) return;
//		int period = 5*60*1000;
//		int count = 10;
//		
//		if (Shell.timeLastJoinClick == -1) return;
//		
//		if (Misc.getTime() - Shell.timeLastJoinClick > 10000
//				&& Shell.timeNewTableOpen < Shell.timeLastJoinClick
//				&& !this.unsuccJoins.contains(Shell.timeLastJoinClick)) {
//			this.unsuccJoins.add(Shell.timeLastJoinClick);
//			Log.log("WARNING! Unsuccessful join is detected. count=%d", unsuccJoins.size());
//		}
//		Set<Long> ujs = new HashSet<Long>(this.unsuccJoins);
//		for (long uj: ujs) if (Misc.getTime()-uj > period) {
//			Log.log("Old unsuccessful join was removed. count=%d", unsuccJoins.size());
//			this.unsuccJoins.remove(uj);
//		}
//		
//		if (this.unsuccJoins.size() > count) {
//			Log.log("ERROR! More than %d unsuccessful table joins during past %d secs. Restarting the client", count, period/1000);
//			this.unsuccJoins.clear();
//			Shell.softRestart("many unsuccessful table joins in lobby");
//		}
//	}
//	private void checkLobbyProc() throws Exception {
//		if (Shell.sleeping) return;
//		if (Shell.lobby == null) return;
//		List<Long> ets = Shell.lobby.excTimes;
//		while (ets.size() > 0 && Misc.getTime() - ets.get(0) > 60*1000)
//			ets.remove(0);
//		
//		if (ets.size() > 3) {
//			Log.log("ERROR! Exceptions in lobby procesing. Restarting. %s", ets);
//			Shell.softRestart("exceptions in lobby processing");
//		}
//	}
//	private void tmp() throws Exception {
//		if (Misc.debugFile("rest")) {
//			Log.log("Restarting the client due to debug file");
//			Shell.softRestart("debug soft restart");
//		}
//	}
	
	public void run() {
		while (true) {
			try {
//				checkNoButtonsTables();
//				checkTimeoutFold();
//				checkNoButtons();
//				checkClientDied();
				checkImgsFolder();
				checkLogsFolder();
//				checkTableLookBuild();
//				checkGameBuild();
//				checkLobbyOpensWindows();
//				checkLobbyProc();
//				tmp();
				
				Thread.sleep(1000);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
