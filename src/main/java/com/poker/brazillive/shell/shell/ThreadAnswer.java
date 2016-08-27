package com.poker.brazillive.shell.shell;

import java.io.*;
import java.net.*;
import java.util.*;

import com.poker.brazillive.shell.util.*;

public class ThreadAnswer extends Thread {
	
	private Table table;
	
	public ThreadAnswer(Table table) throws Exception {
		this.table = table;
	}

	public void run() {
		while (true) {
			try {
				String str = Shell.readStr(table.evSocket);
				

				byte[] bs = str.getBytes();
				boolean allZeros = true;
				for (int i = 0; i < 100; i++) if (bs[i] != 0) {
					allZeros = false;
					break;
				}
				if (allZeros) {
					this.table.log.lo("ERROR! Got a lot of zeros. Stop answer thread");
					break;
				}

				if (new File("test").exists()) {
					new File("test").delete();
					str += "\r\nMIGRATE\r\n";
				}
				String[] strs = str.split("[\r\n]+");
				for (String s: strs) {
					table.log.lo("RECEIVED: %s", s);
					
//					if (s.indexOf("AnswerIsNotApplied.WrongAction") != -1) {
//						this.table.log.lo("Server reported WrongAction. Table is to be closed now");
//						this.table.toBeClosedNow = true;
//					}
					
					if (s.indexOf("ANSWER") == 0) {
						synchronized (Shell.class) {
							Shell.timeLastServerAnswer = Misc.getTime();
						}
						synchronized (table) {
							this.table.answer = s;
							table.timeAnswerObtained = Misc.getTime();
						}
					} else if (s.indexOf("ERROR") == 0) {
						if (1 == 1
								&& s.indexOf("{Warning}") == -1
								&& s.indexOf("{MissedPreButtons}") == -1
								&& s.indexOf("Cannot distribute pots because there are players to act") == -1
//								&& s.indexOf("WrongBetSize") == -1
								)
							if (Shell.curSes != null) Shell.curSes.countErr.inc();
					} else if (s.indexOf("CLOSE") == 0) {
						if (Misc.getTime()-table.timeFirstButtonsShowed < 5*60*1000) {
							table.log.lo("Ignore CLOSE command because this table is just started");
						} else {
							table.scheduleToCloseAfterGame("got CLOSE command from server");
							Shell.closedTables.put(table.getId(), Misc.getTime());
						}
					} else if (s.indexOf("MIGRATE") == 0) {
						table.toBeMigrated = true;
					} else if (Misc.debugFile("migr")) {
						table.toBeMigrated = true;
					} else if (s.indexOf("PING") == 0) {
					} else {
						table.log.lo("WARNING! Unknown server message: %s", s);
					}
				}
				Thread.sleep(10);
			} catch (SocketException e) {
				if (e.getMessage().equalsIgnoreCase("Socket closed") || e.getMessage().equalsIgnoreCase("Socket is closed")) {
					this.table.log.lo("ThreadAnswer: socket is closed. Stop");
				} else {
					this.table.log.lo("Exception in ThreadAnswer:\n%s", Misc.stacktrace2Str(e));
				}
				break;
			} catch (Throwable e) {
				table.log.lo("%s", Misc.stacktrace2Str(e));
				try { Thread.sleep(1000); } catch (Exception ex) { ex.printStackTrace(); }
			}
		}
		try {
			if (!this.table.toBeMigrated) this.table.scheduleToCloseNow("Answer thread is stopped");
		} catch (Exception e) {
			this.table.log.lo("%s", Misc.stacktrace2Str(e));
		}
	}
}
