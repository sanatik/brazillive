/*
 * Concept:
 * - Firstly build table look, then compare it with the current game stage
 * 		and build game and game events
 * - Any table can be scheduled to be closed now or after the game anywhere. Call of
 * 		clicks, window closes are not allowed from anywhere in the code
 * - All the clicks and other user control actions are in one place: Shell.playSession,
 * 		after all the table looks are built
 */

package com.poker.brazillive.shell.shell;

import java.util.*;
import java.util.concurrent.*;

import javax.imageio.ImageIO;


import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.text.*;


import com.poker.brazillive.shell.util.*;

public class Shell {

	public static final String TAKE_HOST = "127.0.0.1";
	public static final int TAKE_PORT = 1111;
	
	public static Object clickSync = new Object();
	
	public static Properties cfg = new Properties();
	public static long timeLastServerAnswer = 0;
	public static long timeLastButtons = Misc.getTime();
	public static long timeNoConnection = 0;
	public static long timeRestarted = 0;
	
	public static long timeLastTableLookBuild = -1;
	public static long timeLastTableLookBuildSucc = -1;
	public static long timeLastGameBuild = -1;
	public static long timeLastGameBuildSucc = -1;
	
	public static long timeLastJoinClick = -1;
	public static long timeNewTableOpen = 0;
	public static Map<Integer, Table> tables = new ConcurrentHashMap<Integer, Table>();
	
	public static List<Long> timesSitinClicked = new ArrayList<Long>();
	public static List<Long> timesNotBotMove = new ArrayList<Long>();
	
	public static void resetTimes() {
		timeLastServerAnswer = 0;
		timeLastButtons = Misc.getTime();
		timeNoConnection = 0;
		timeRestarted = 0;
		timeLastTableLookBuild = -1;
		timeLastTableLookBuildSucc = -1;
		timeLastGameBuild = -1;
		timeLastGameBuildSucc = -1;
		timeLastJoinClick = -1;
		timeNewTableOpen = 0;
	}
	
	public static void wheelScroll(int hwnd, int x, int y, int count, String dir) throws Exception {
		String ahkExe = String.format("\"%s\" \"%s\" wheel 0x%08X %d %d %d %s",
				Action.AHK_PATH, "common.ahk",
				hwnd, x, y, count, dir);
		
		synchronized (Shell.clickSync) {
			Misc.sysCall(ahkExe);
			Thread.sleep(100);
		}
	}
	
	
	public static Lobby lobby;
	
	public static String firefoxExe() {
		return "C:/Program Files (x86)/Mozilla Firefox/firefox_"+Shell.uname+".exe";
	}
	public static String chromeExe() {
		return "C:/Program Files (x86)/Google/Chrome/Application/chrome_"+Shell.uname+".exe";
	}

	
	public static List<WndData> look = new ArrayList<WndData>();
	public static int lookNum = 0;
	public static long lookTime = 0;
	
	public static ThreadSaveImg threadSaveImg;

	private static Log lookLog;
	private static ThreadLook thl;
	
	// Table id -> time it was closed
	public static Map<String, Long> closedTables = new ConcurrentHashMap<String, Long>();
	
	public static class Hero implements Serializable {
		private static final long serialVersionUID = 1;
		
		public String login; public String pwd; public String nick;
		public Hero(String name, String pwd, String nick) {
			this.login = name;
			this.pwd = pwd;
			this.nick = nick;
		}
		public String toString() { return this.login; }
	}
	
	public static String uname;
	public static String host;
	public static String version;
	public static Client client;
	public static Room room;
	
	private static void init() throws Exception {
		Shell.version = FUtil.fRead("ver").get(0);
		Shell.uname = System.getProperty("user.name");
		
		Log.log("Shell version: %s", Shell.version);

		cfg.load(new FileInputStream(new File("shell.cfg")));
		String checkRes = checkCfg(Shell.cfg);
		if (!checkRes.equals("")) {
			Log.log("Incorrect config:\n%s", checkRes);
			System.exit(0);
		}
		
		Shell.host = Shell.cfg.getProperty("server.name");
		if (host.isEmpty()) {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface ni: Collections.list(nets)) {
				Enumeration<InetAddress> addrs = ni.getInetAddresses();
				for (InetAddress addr : Collections.list(addrs)) {
					String ip = addr.getHostAddress();
					if (!ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) continue;
					if (ip.matches("192\\.168.*")) continue;
					if (ip.equals("127.0.0.1")) continue;
					host = ip;
				}
			}
		}

		Shell.room = Room.createRoom();
		rmTlogs();
		Shell.room.checkIp();
		
		new File(Shell.uname+"/imgs").mkdirs();
		
//		Shell.sendCfg_rmme();
		
		ThreadMonitor th = new ThreadMonitor(cfg, tables);
		th.start();

		threadSaveImg = new ThreadSaveImg();
		threadSaveImg.start();
		
		Shell.startSendStatusThread();

		InetAddress ipAddress = InetAddress.getByName(TAKE_HOST);
//		Socket socket = new Socket(ipAddress, Integer.parseInt(cfg.getProperty("take.port")));
		int un = 0;
		try {
			un = Integer.parseInt(Shell.uname.replaceAll("[^\\d]", ""));
		} catch (Exception e) {}
		Socket socket = new Socket(ipAddress, TAKE_PORT+un);
		DataInputStream in = new DataInputStream(socket.getInputStream());
		Log.log("Take socket created");
		
		new File(Shell.uname).mkdirs();
		lookLog = new Log(Shell.uname+"/thread.look."+Misc.dateFormat(Misc.getTime(), "yyyyMMdd_HHmmss")+".out");
		thl = new ThreadLook(new DataInputStream(socket.getInputStream()), lookLog);
		thl.start();
		
		new Thread() {
			public void run() {
				while (true) {
					try {
						Log.log("Proc images thread is started");
						Shell.procImgs();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		
		ImageIO.setUseCache(false);
		Shell.client = Client.create();
	}
	
	public static String[] getNetworkSkin() {
		return Shell.cfg.getProperty("u."+Shell.uname+".network.skin").split("/");
	}
	
	private static void rmTlogs() throws Exception {
		final long OLD_FILE_DAYS = 10;
		File arch = new File(Shell.uname+"/tlogs/arch");
		if (!arch.exists()) {
			Log.log("WARNING! Tlogs arch folder is not found: %s", arch.getAbsolutePath());
			return;
		}
		Log.log("Removing old tlogs");
		int c = 0;
		File[] tls = arch.listFiles();
		for (File tl: tls) {
			if (Misc.getTime() - tl.lastModified() < 1000*3600*24*OLD_FILE_DAYS) continue;
			if (!tl.delete()) Log.log("ERROR! Cannot delete old tlog: %s", tl.getAbsolutePath());
			c++;
		}
		Log.log("Removed %d old tlogs", c);
	}
	
	private static void sendStatus() {
//		if (Misc.getTime() > 0) return;
		
		try {
			if (Shell.curSes == null) return;
			if (Shell.cfg.getProperty("admin.url").isEmpty()) return;
//			if (Shell.curSes.timeStart == 0) return; // This should be removed. Need to send status to get stop/play command from admin
			
			String surl = Shell.cfg.getProperty("admin.url")+"/public_status.php";
			if (surl.isEmpty()) return;
			surl += "?"+Shell.curSes.getUrlParsAndFlushCounts();
//			Log.log("Sending status, URL=%s", surl);
			URL url = new URL(surl);
			URLConnection conn = url.openConnection();
	
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	
			List<String> resp = new ArrayList<String>();
			String str;
			while ((str = br.readLine()) != null) resp.add(str);
			br.close();

			String r = (resp.size() >= 1 ? resp.get(0) : "");
			boolean ok = resp.size() == 1 && (1 ==2
					|| r.equals("OK")
					|| r.equals("play")
					|| r.equals("stop")
					);
			if (resp.size() > 1 || !ok) for (String s: resp) Log.log("STATUS ANSWER: '%s'", s);

			if (r.equals("play")) {
//				Log.log("Got play cmd from admin");
				Shell.sesPauseReason = null;
			} else if (r.equals("stop")) {
//				Log.log("Got stop cmd from admin");
				Shell.sesPauseReason = "stop command from admin";
			}
			
			Log.log("Status is sent. Answer: %s. URL: %s", r, surl);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void sendCfg_rmme() {
//		if (Misc.getTime() > 0) return;
		
		try {
			String surl = Shell.cfg.getProperty("admin.url");
			if (surl.isEmpty()) return;

			String charset = "UTF-8";
	        String requestURL = surl+"/public_cfg.php";
	 
	        MultipartUtility multipart = new MultipartUtility(requestURL, charset);
	        multipart.addHeaderField("User-Agent", "CodeJava");
	        multipart.addHeaderField("Test-Header", "Header-Value");
	        multipart.addFormField("method", "post");
	        multipart.addFormField("ip", Shell.host);
	        multipart.addFormField("winacc", Shell.uname);
	        multipart.addFormField("version", Shell.version);
	        multipart.addFilePart("file", new File("shell.cfg"));
	 
	        List<String> resp = multipart.finish();
	        
	        if (resp.size() != 1 || !resp.get(0).equals("OK")) {
	        	String s = ""; for (String s1: resp) s += s1+"\n";
	        	Log.log("ERROR! Response from cfg.php: %s", s);
	        }
	        
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void procImgs() throws Exception {
		
		long itTime = Misc.getTime();
		
		while (true) {
			try { 
				
				itTime = Misc.getTime();
				
		        List<WndData> curLook = null;
		        int lookNum = -1;
				synchronized (ThreadLook.syncLook) {
					curLook = Shell.look;
					lookNum = Shell.lookNum;
					Shell.look = null;
//					lookLog.lo("Took look num %d", lookNum);
//					for (WndData wndData: curLook) lookLog.lo("\n%s", wndData.getInfo());
				}
				
				if (curLook == null) {
		        	Thread.sleep(10);
		        	continue;
				}
				

				Set<Integer> existingWnds = new HashSet<Integer>();
				for (WndData wndData: curLook) {
					if (wndData.broken) {
						Log.log("WARNING! Broken WndData hwnd=0x%08X", wndData.hwnd);
						if (tables.containsKey(wndData.hwnd))
							tables.get(wndData.hwnd).log.lo("WARNING! Broken WndData hwnd=0x%08X", wndData.hwnd);
						continue;
					}

					if (!Shell.room.isTable(wndData)) continue;
					
					Table table = null;
					existingWnds.add(wndData.hwnd);
					
					synchronized (tables) {
				        if (tables.get(wndData.hwnd) == null) {
				        	timeNewTableOpen = Misc.getTime();
				        	table = Table.create(wndData.hwnd);
			        		tables.put(wndData.hwnd, table);
			        		continue; // Don't proc first img
				        }
				        table = tables.get(wndData.hwnd);
					}
					long st = Misc.getTime();
			        table.procImg(wndData);
			        if (Misc.getTime()-st > 2000)
			        	table.log.lo("ERROR! Processed img %s is too slow", wndData.getImgFileName());
				}
				
				// Remove tables that do not exist in look
				synchronized (tables) {
					Set<Integer> hwnds = new HashSet<Integer>(tables.keySet());
					for (int hwnd: hwnds) if (! existingWnds.contains(hwnd)) {
						tables.get(hwnd).log.lo("This table doesn't send images. Disconnect and remove");
						tables.get(hwnd).disconnect();
						tables.remove(hwnd);
					}
				}
				int pc = 0;
				int bbSum = 0; int bbCount = 0;
				for (Table t: tables.values()) {
					if (Misc.getTime() - t.timeButtonsShowed < 5*30*1000) pc++;
					if (t.tableLook != null && t.tableLook.bb != 0) {
						bbSum += t.tableLook.bb;
						bbCount++;
					}
				}
				if (Shell.curSes != null) {
					Shell.curSes.countTable = tables.size();
					Shell.curSes.countTablePlaying = pc;
					if (bbCount != 0) {
						Shell.curSes.avgBb = 1.0*bbSum/bbCount;
//						Log.log("Avg bb: %d/%d = %1.1f", bbSum, bbCount, Shell.curSes.avgBb);
					}
				}
				
				// Below is the only place where clicks and other user control actions are possible
				
				for (Table table: tables.values()) {
					if (table.toBeClosedNow && Misc.getTime() - table.timeTriedToClose > 5*1000) {
						table.log.lo("Closing table because toBeClosedNow == true");
						table.close();
						continue;
					}
					if (table.toBeClosedAfterGame
							&& table.tableLook != null
							&& !table.tableLook.isHeroInPlay
//							&& table.tableLook.heroPos == 0
							&& Misc.getTime() - table.timeTriedToClose > 5*1000) {
						table.log.lo("Closing table because toBeClosedAfterGame == true and hero is not in play");
						table.close();
						continue;
					}
					if (table.toBeClosedNextBb
							&& table.tableLook != null
							&& table.tableLook.isHeroSitout
							&& Misc.getTime() - table.timeTriedToClose > 5*1000) {
						table.log.lo("Closing table because toBeClosedNextBb == true and hero is sitting out");
						table.close();
						continue;
					}
				}

				Table activeButtonsTable = null;
				
				List<Table> tables2Click = new ArrayList<Table>();
				if (Shell.room.clickActiveTable) {
					for (Table table: tables.values())
						if (table.tableLook != null && table.tableLook.isActive
								&& table.tableLook.isButtons
								&& Misc.getTime() - table.timeButtonsShowed < 10*1000 // Aoid situation when active buttons table blocks the game
							)
						activeButtonsTable = table;
				}
				if (activeButtonsTable == null) tables2Click.addAll(tables.values());
				else tables2Click.add(activeButtonsTable);
				
//				if (activeButtonsTable == null) Log.log("Processing %d NOT active buttons tables", tables2Click.size());
//				else Log.log("Processing ONE active buttons table");
				
				for (Table table: tables2Click) {

					table.posIfNeeded();
					table.resizeIfNeeded();

					if (table.tableLook == null) continue;
					
					int waitTime = Integer.parseInt(cfg.getProperty("wait.for.server.answer"));
					if (table.tableLook.isButtons
							&& table.timeButtonsSent != -1
							&& table.timeAnswerObtained < table.timeButtonsSent
							&& Misc.getTime() - table.timeButtonsSent > waitTime) {
					
						table.log.lo("ERROR! Server didn't answer in %d secs", waitTime/1000);
						table.scheduleToCloseNow("No answer from server within "+waitTime+"ms");
					}

					// This is removed because tables to click create queue and clicks goes to that tables one by one. This can cause a delay
//					if (table.tableLook.isButtons
//							&& table.timeButtonsShowed != -1
//							&& Misc.getTime() - table.timeButtonsShowed >= waitTime) {
//
//						table.log.lo("ERROR! Wasn't able to apply answer in %d secs", waitTime/1000);
//						synchronized (table) {
//							table.answer = "ANSWER F 0 0 Normal";
//							table.timeAnswerObtained = Misc.getTime();
//						}
//					}

					if (table.tableLook.isButtons
							&& table.timeAnswerObtained >= table.timeButtonsShowed
							&& Misc.getTime() - table.timeAnswerApplied > 3000

							// Do not do action immediately
							&& Misc.getTime() - table.timeButtonsShowed > Shell.room.getMoveDelayMs(table)
						) {

						if (table.timeAnswerApplied < table.timeButtonsShowed) {
							table.applyAnswer();
						} else if (Misc.getTime() - table.timeAnswerApplied > 4000
								// Check table has fresh table look. This is to avoid situation when table looks were skipped and we see old table look with buttons
								&& table.tableLook != null
								&& Misc.getTime() - table.tableLook.time < 1000
							) {
							table.log.lo("WARNING! Apply answer once again");
							table.applyAnswer();
							//Shell.threadSaveImg.queue.add(table.imgsToSave.q.get(table.imgsToSave.q.size()-1));
						}
						if (activeButtonsTable != null) Thread.sleep(100);
						
//							if (activeButtonsTable == null) Log.log("Clicked other table");
//							else Log.log("Clicked active buttons table");
					}

					table.doActions();
				}
				
				if (activeButtonsTable == null && Shell.sesPlaying && !Shell.sesSitout) {
					if (Shell.lobby == null) Shell.lobby = Lobby.create();
					Shell.lobby.procLook(curLook);
				}
				
				// End of place for clicks
		        
		        Thread.sleep(10);
		        
			} catch (Throwable e) {
				e.printStackTrace();
				Thread.sleep(1000);
			}
		}
	}

	public static String readStr(Socket s) throws Exception {
		if (cfg.getProperty("str.read.method").equals("bytes")) {
			InputStream in = s.getInputStream();
			int c = 0;
			byte[] bs = new byte[10000];
			while ((c < 2 || !(bs[c-2] == 13 && bs[c-1] == 10)) && c < bs.length) {
				byte[] b = new byte[1];
				in.read(b);
				bs[c++] = b[0];
				//System.out.print(b[0]+" ");
			}
			byte[] bs2 = new byte[c];
			System.arraycopy(bs, 0, bs2, 0, c);
			return new String(bs2);
			
		} else if (cfg.getProperty("str.read.method").equals("readUTF")) {
			return new DataInputStream(s.getInputStream()).readUTF();
		} else assert false;
		return null;
	}
	public static void sendStr(Socket s, String str) throws Exception {
		if (cfg.getProperty("str.read.method").equals("bytes")) {
			OutputStream out = s.getOutputStream();
			long t = Misc.getTime();
//			Log.log("Sending...");
			out.write(str.getBytes());
//			Log.log("Send took %d ms", Misc.getTime()-t);
		} else if (cfg.getProperty("str.read.method").equals("readUTF")) {
			new DataOutputStream(s.getOutputStream()).writeUTF(str);
		} else assert false;
	}
	
	public static String checkCfg_old(Properties cfg) throws Exception {
		Properties ecfg = new Properties();
		ecfg.load(new FileInputStream("shell.etalon.cfg"));

		String errs = "";
		
		List<String> us = new ArrayList<String>();
		for (Object k: cfg.keySet()) if (k.toString().matches("^.*\\.hero$"))
			us.add(k.toString().split("\\.")[0]);
		
		for (Object k: ecfg.keySet()) {
			List<String> ks = new ArrayList<String>();
			if (k.toString().indexOf("u1.") == 0) {
				for (String u: us) ks.add(k.toString().replaceAll("u1.", u+"."));
			} else {
				ks.add(k.toString());
			}
			for (String k1: ks) if (!cfg.containsKey(k1))
				errs += String.format("Property '%s' is not found in shell.cfg\n", k1);
		}

//		for (Object k: cfg.keySet()) {
//			String k1 = k.toString();
//			if (k.toString().matches("u\\d+\\..*"))
//				k1 = k.toString().replaceAll("^u\\d+", "u1");
////			Log.log("k=%s, k1=%s", k, k1);
//			if (!ecfg.containsKey(k1) && !k1.matches("brain.server\\d"))
//				errs += String.format("Property '%s' is not needed in shell.cfg\n", k);
//		}
		
		return errs;
	}
	public static String checkCfg(Properties cfg) throws Exception {
		Properties ecfg = new Properties();
		ecfg.load(new FileInputStream("shell.etalon.cfg"));
		String errs = "";
		
		Set<Object> eks = new HashSet<Object>(ecfg.keySet());
		String uk = null, bsk = null;
		for (Object k: cfg.keySet()) {
			if (((String)k).indexOf("brain.server") == 0) {
				k = ((String)k).replaceAll("\\d", "1");
				bsk = (String)k;
			}
			if (((String)k).indexOf("u.") == 0) {
//				Log.log("k before = %s", k);
				k = ((String)k).replaceAll("^u\\..*?\\.", "u.u1.");
//				Log.log("k after = %s", k);
				uk = (String)k;
			}

			if (!eks.contains(k)) errs += String.format("Property '%s' is not needed in shell.cfg\n", k);
			if (((String)k).indexOf("u.") != 0 && ((String)k).indexOf("brain.server") != 0)
				eks.remove(k);
		}
		eks.remove(uk);
		eks.remove(bsk);
		for (Object k: eks) {
			errs += String.format("Property '%s' is not found in shell.cfg\n", k);
		}
		
		return errs;
	}
	
	public static void startSendStatusThread() throws Exception {
		new Thread() {
			public void run() {
				while (true) {
					try {
						Shell.sendStatus();
						Thread.sleep(20*1000);
					} catch (Exception e) {
						e.printStackTrace();						
					}
				}
			}
		}.start();
	}
	
	public static void playStart(String reason) throws Exception {
		Log.log("Starting playing: %s", reason);
		Shell.client.start();
		Shell.resetTimes();
		sesPlaying = true;
	}
	public static void playSoftShutdown(String reason) throws Exception {
		Log.log("Soft shutdown: %s", reason);
		Log.log("Using sitout next %s", Shell.room.isSitoutNextBbSupported()?"BB":"game");
		sesPlaying = false;
		for (Table t: Shell.tables.values()) {
			if (Shell.room.isSitoutNextBbSupported()) 
				t.scheduleToCloseNextBb(reason);
			else t.scheduleToCloseAfterGame(reason);
		}
		long st = Misc.getTime();
		int waitTimeMin = 3;
		if (Shell.room.isSitoutNextBbSupported()) waitTimeMin = 10;
			
		Log.log("Waiting %d mins to close all the tables", waitTimeMin);
		while (Shell.tables.size() > 0) {
			if (Misc.getTime() - st > waitTimeMin*60*1000) {
				Log.log("ERROR! Wasn't able to close all the tables in %d mins", waitTimeMin);
				break;
			}
			Log.log("Tables left: %d", Shell.tables.size());
			Thread.sleep(1000*5);
		}
		Shell.lobby = null;
		Shell.client.closeOrKill();
	}
	public static void playSoftRestart(String reason) throws Exception {
		Log.log("Soft restart: %s", reason);
		playSoftShutdown(reason);
		playStart(reason);
	}
	
	public static void playSitoutStart(String reason) throws Exception {
		Log.log("Sitout all the tables: %s", reason);
		sesSitout = true;
		for (Table t: Shell.tables.values()) t.scheduleToSitout(reason);
	}
	public static void playSitoutFinish(String reason) throws Exception {
		Log.log("Finish sitout for all the tables: %s", reason);
		sesSitout = false;
		for (Table t: Shell.tables.values()) t.needSitout = false;
		Shell.resetTimes();
	}
	
	public static Session curSes;
	public static boolean sesPlaying = false;
	public static boolean sesSitout = false;
	public static String sesPauseReason = null;
	public static long timeSitoutFinish = 0;
	public static long timeSesCreate = 0;
	
	private static void newMain() throws Exception {
		cfg.load(new FileInputStream(new File("shell.cfg")));
		boolean loop1 = true;
		Shell.room.createSessionsIfNeeded();
		
		while (true) {
			try {
				if (!loop1) Thread.sleep(5*1000); loop1 = false;
				
				if (new File("stopall").exists()) {
					Log.log("Stopall file is found. Exit");
					System.exit(0);
				}
				
				if (curSes == null || (Misc.getTime() > curSes.timeFinishSched && !sesPlaying)) {
					boolean wasNull = (curSes == null);
					Session newSes = Shell.room.getNextSession();
					if (newSes == null) {
						Log.log("No session for now");
						Thread.sleep(3*60*1000);
					} else {
						curSes = newSes;
						Log.log("New session is taken: %s", curSes);
						if (wasNull) Shell.sendStatus(); // To get play/stop command from admin
					}
				}
	
				if (Misc.getTime() - timeSesCreate > 5*1000) {
					Shell.room.createSessionsIfNeeded();
					if (curSes != null && !curSes.fromFile.exists()) {
						playSoftShutdown("session file was deleted");
						curSes = null;
						continue;
					}
					timeSesCreate = Misc.getTime();
				}
				
				if (curSes == null) continue;
	
				if (Misc.getTime() > curSes.timeFinishSched && sesPlaying) {
					playSoftShutdown("session is over");
					Shell.curSes.timeFinish = Misc.getTime();
					continue;
				}
				if (Misc.getTime() > curSes.timeStartSched
						&& Misc.getTime() < curSes.timeFinishSched
						&& !sesPlaying && sesPauseReason == null) {
	
					if (Shell.curSes.timeStart == 0) Shell.curSes.timeStart = Misc.getTime();
					playStart("session time to play");
					continue;
				}
				if (sesPlaying && sesPauseReason != null) {
					playSoftShutdown(sesPauseReason);
					continue;
				}
				
				if (Misc.getTime() < curSes.timeStartSched && !sesPlaying) {
					long w = (curSes.timeStartSched-Misc.getTime())/1000;
					Log.log("Next session will start in %s. Session period: %s - %s",
							String.format("%02d:%02d:%02d", w/3600, (w % 3600) / 60, (w % 60)),
							Misc.dateFormatHuman(curSes.timeStartSched),
							Misc.dateFormatHuman(curSes.timeFinishSched));
					
					Thread.sleep(60*1000);
					continue;
				}
				
				long sitoutPeriod = Shell.room.needSitoutPeriod();
				if (sitoutPeriod != 0 && !Shell.sesSitout) {
					Shell.timeSitoutFinish = Misc.getTime()+sitoutPeriod;
					Shell.playSitoutStart(String.format("sitout cmd from room. Period: %1.2f mins, till %s", sitoutPeriod/1000.0/60, Misc.dateFormatHuman(Shell.timeSitoutFinish)));
				}
				if (Shell.sesSitout && Misc.getTime() > Shell.timeSitoutFinish) {
					Shell.playSitoutFinish("sitout period is over");
				}
				
				if (sesPlaying && sesPauseReason == null && !Shell.sesSitout) Checker.checkAll();
				
				if (Misc.debugFile("ppp")) sesPauseReason = "ppp";
				if (Misc.debugFile("ggg")) sesPauseReason = null;
				if (Misc.debugFile("soStart")) Shell.playSitoutStart("soStart debug file");
				if (Misc.debugFile("soStop")) Shell.playSitoutFinish("soStop debug file");
			} catch (Throwable e) {
				e.printStackTrace();
				Thread.sleep(5*1000);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		init();
		newMain();
		System.exit(0);
	}

}
