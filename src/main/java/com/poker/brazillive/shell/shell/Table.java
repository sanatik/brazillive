package com.poker.brazillive.shell.shell;

import com.poker.brazillive.shell.game.*;
import com.poker.brazillive.shell.shell.gg.TableGg;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Constructor;
import java.text.*;
import java.util.*;
import java.net.*;

import javax.imageio.ImageIO;

import com.poker.brazillive.shell.util.*;

public abstract class Table {
	
	public static class ImgsToSave {
		private int maxSize;
		public List<WndData> q = new LinkedList<WndData>();
		
		public ImgsToSave(int maxSize) {
			this.maxSize = maxSize;
		}
		synchronized public void add(WndData wndData) {
			if (maxSize == 0) return;
			if (q.contains(wndData)) return;
			q.add(wndData);
			while (q.size() > this.maxSize) q.remove(0);
		}
		synchronized public void addAll(List<WndData> wndDatas) {
			for (WndData wndData: wndDatas) this.add(wndData);
		}
		synchronized public List<WndData> getAllAndRemove() {
			List<WndData> ret = new ArrayList<WndData>(this.q);
			this.q.clear();
			return ret;
		}
		synchronized public List<WndData> getLast(int n) {
			return this.q.subList(Math.max(this.q.size()-n, 0), this.q.size());
		}
	}
	
	public final static String eol = "\r\n";

	public int hwnd;
	public Socket evSocket;
	public Socket hhSocket;
	public Log log;
	public ThreadAnswer thAnswer;
	public ThreadHh thHh;
	public long timeLastImg = -1;
	public int x = -1, y = -1;
	public boolean toBeClosedNextBb = false;
	public boolean toBeClosedAfterGame = false;
	public boolean toBeClosedNow = false;
	public boolean toBeMigrated = false;
	
	public long timeButtonsShowed = -1;
	public long timeButtonsSent = -1;
	public long timeFirstButtonsShowed = -1;
	public String answer;
	public long timeAnswerObtained = -1;
	public long timeAnswerApplied = 0;
	public long timeTriedToClose = 0;
	
	public long timeLastTableLookBuild = -1;
	public long timeLastTableLookBuildSucc = -1;
	public long timeLastGameBuild = -1;
	public long timeLastGameBuildSucc = -1;
	
	public long timeClickedFreeCheck = -1;
	public long timeResized = -1;
	
	public ImgsToSave imgsToSave;
	
	public WndData wndData;
	public TableLook tableLook;
	protected Game game;
	private GameBuilder gameBuilder;
	private int gameCount = 0;
	public long timeBorn;
	public boolean resized = false;
	public List<String> hhStrs;
	
	public int targetWidth;
	public int targetHeight;
	
	public boolean needSitout = false; 
	
	public static Table create(int hwnd) throws Exception {
		String nw = Shell.getNetworkSkin()[0];
		Class<?> c = Class.forName("shell."+nw.toLowerCase()+".Table"+nw);
		Constructor<?> constr = c.getDeclaredConstructor(new Class<?>[]{Integer.TYPE});
		Table t = (Table)constr.newInstance(hwnd);
		return t;
	}
	
	public Table(int hwnd) throws Exception {
		this.hwnd = hwnd;
		this.timeBorn = Misc.getTime();
		
		this.imgsToSave = new ImgsToSave(Misc.getPropInt(Shell.cfg, "img.save.table.max"));
		
		File tlFolder = new File(Shell.uname+"/tlogs");
		tlFolder.mkdirs();
		this.log = new Log(tlFolder.getAbsolutePath()+"/"+(new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(Misc.getTime())))+".0x"+String.format("%08X", hwnd)+".log");
		
		this.hhStrs = new ArrayList<String>();
		
		this.gameBuilder = new GameBuilder(0, this.log);
		this.log.lo("Created table, hwnd=%08X", this.hwnd);
	
   		this.connectToServer();
    	
//    	this.posIfNeeded();
//    	this.resizeIfNeeded();
	}
	
	public abstract void doActions() throws Exception;
	public abstract void applyAnswerAction() throws Exception;
	public abstract String getId(); // Returns uniq id of the table in the lobby
	
	public abstract void actionSitin() throws Exception;
	public abstract void actionTakeEmptySeat() throws Exception;
	public abstract void actionJoinWaitList() throws Exception;
	public abstract void actionClose() throws Exception;
	
	private long timeWaitListClicked = 0;
	synchronized public void clickWaitListBtnIfNeeded() throws Exception {
		if (this.tableLook == null) return;
		if (this.tableLook.heroPos != -1) return; // To avoid btns click
		if (this.tableLook.isButtons) return; // just in case, not to click on btn
		if (!this.tableLook.isWaitListBtn) return;
		if (Misc.getTime() - this.timeWaitListClicked < 5000) return;

		this.actionJoinWaitList();
		
		this.timeWaitListClicked = Misc.getTime();
		this.log.lo("Clicked wait list");
	}

	protected long timeEmptySeatClicked = 0;
	synchronized public void clickEmptySeatIfNeeded() throws Exception {
		if (this.tableLook == null) return;
		if (this.tableLook.heroPos != -1) return;
		if (this.tableLook.isWaitListBtn) return;
		if (Misc.getTime() - this.timeEmptySeatClicked < 5000) return;
		boolean hasEmptySeat = false;
		for (boolean b: this.tableLook.isSeatOccupied) if (!b) hasEmptySeat = true;
		if (!hasEmptySeat) return;
		
		this.actionTakeEmptySeat();
		
		this.timeEmptySeatClicked = Misc.getTime();
		this.log.lo("Clicked empty seat");
	}

	protected long timeSitInClicked = 0;
	synchronized public void clickSitinIfNeeded() throws Exception {
		if (this.tableLook == null) return;
		if (this.tableLook.isButtons) return; // just in case, not to click on btn
		if (!this.tableLook.isHeroSitout) return;
		if (this.tableLook.heroPos == -1) return; // No check boxes if hero is not sitting
		if (this.needSitout) return;
		if (this.toBeClosedNextBb) return;
		if (Misc.getTime() - this.timeSitInClicked < 5000) return;
		this.actionSitin();
		this.timeSitInClicked = Misc.getTime();
		this.log.lo("Sitin is clicked");
	}

	public void posIfNeeded() throws Exception {
		if (this.x != -1) return;
		
		Properties cfg = Shell.cfg;
		int rows = Integer.parseInt(cfg.getProperty("table.layout.rows"));
		int cols = Integer.parseInt(cfg.getProperty("table.layout.cols"));
		int gapl = Integer.parseInt(cfg.getProperty("table.layout.left.gap"));
		int gapr = Integer.parseInt(cfg.getProperty("table.layout.right.gap"));
		
		int gapt = 30; // gap top
		int gapb = 0; // gap bottom
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int dispWidth = (int)screenSize.getWidth();
		int dispHeight = (int)screenSize.getHeight();
		this.log.lo("Display resolution: %dx%d, table: %dx%d", dispWidth, dispHeight, this.targetWidth, this.targetHeight);
		
		int[] xs = new int[cols];
		int[] ys = new int[rows];
		int xStep = (dispWidth-gapl-gapr-this.targetWidth)/(cols-1);
		int yStep = (dispHeight-gapt-gapb-this.targetHeight)/(rows-1);
		for (int i = 0; i < xs.length; i++) xs[i] = gapl+xStep*i;
		for (int i = 0; i < ys.length; i++) ys[i] = gapt+yStep*i;
		
		Point[] ps = new Point[rows*cols]; int c = 0;
		for (int j = 0; j < ys.length; j++) for (int i = 0; i < xs.length; i++)
			ps[c++] = new Point(xs[i], ys[j]);

    	for (int i = 0; i < ps.length; i++) {
    		boolean busy = false;
    		for (Table t: Shell.tables.values()) if (t.x == ps[i].x && t.y == ps[i].y) busy = true;
    		if (busy) continue;
    		Action.wndMove(this.hwnd, ps[i].x, ps[i].y);
    		this.x = ps[i].x; this.y = ps[i].y;
        	this.log.lo("Window is moved to (%d,%d)", ps[i].x, ps[i].y);
    		return;
    	}
		Action.wndMove(this.hwnd, ps[0].x, ps[0].y);
		this.x = ps[0].x; this.y = ps[0].y;
    	this.log.lo("ERROR! Cannot position the window to free place. Moved to (%d,%d)", ps[0].x, ps[0].y);
	}
	
	public void resizeIfNeeded() throws Exception {
		if (this.resized) return;
		if (this.wndData == null) return;
		int w = this.wndData.getImg().getWidth(), h = this.wndData.getImg().getHeight();
		if (this.targetWidth != w || this.targetHeight != h) this.actionResize(this.targetWidth, this.targetHeight);
		this.resized = true;
		this.timeResized = Misc.getTime();
		this.log.lo("Table is resized: %dx%d", this.targetWidth, this.targetHeight);
	}

	public String startSessionCmd() {
    	String nick = Shell.curSes.hero.nick;
    	if (!Shell.room.sendHh) nick = nick.toLowerCase().replaceAll("[ _]", "");

    	String pref = (Shell.room.sendHh?"":"IG-");
    	String ss = "START "+pref+"SESSION "+Misc.getTime()+" 6 "+Shell.curSes.hero.login+" '"+nick.replace("'", "''")+"'";
    	if (Shell.room.sendHh) ss += new SimpleDateFormat(" Z").format(new Date());
    	
    	return ss;
	}
	
	public synchronized void connectToServer() throws Exception {
		// brain.server1 = 192.168.229.1:1031:1032
		this.log.lo("Connecting to server");

		String str = "\n";
		String[] minLoadServ = null;
		int minLoad = Integer.MAX_VALUE;
		for (Object o: Shell.cfg.keySet()) {
			String prop = (String)o;
			if (prop.indexOf("brain.server") != 0) continue;
			str += Shell.cfg.getProperty(prop)+":";
			String[] sr = Shell.cfg.getProperty(prop).split(":");

			this.log.lo("PING server %s:%s", sr[0], sr[1]);
        	InetAddress ip = InetAddress.getByName(sr[0]);
        	Socket s = null;
        	try {
//        		s = new Socket(ip, Integer.parseInt(sr[1]));
        		s = new Socket();
        		s.connect(new InetSocketAddress(ip, Integer.parseInt(sr[1])), 2000);
        	} catch (Throwable e) {
        		str += " "+e.getMessage()+"\n";
        		continue;
        	}
        	this.log.lo("Socket is created");
        	String ans = null;
        	try {
        		Shell.sendStr(s, "PING\r\n");
            	ans = Shell.readStr(s);
        	} catch (Throwable e) {
        		str += " "+e.getMessage()+"\n";
        		continue;
        	}
        	int load = Integer.parseInt(ans.split(" ")[1].replaceAll("[\\r\\n]+", ""));
        	str += " loading "+load+"\n";
        	
        	if (minLoad > load) {
        		minLoad = load;
        		minLoadServ = sr;
        	}
        	s.close();
		}
		this.log.lo(str);

		if (minLoadServ != null) {
			this.log.lo("Selected server: "+Arrays.toString(minLoadServ));
	    	InetAddress ip = InetAddress.getByName(minLoadServ[0]);
	    	this.evSocket = new Socket(ip, Integer.parseInt(minLoadServ[1]));
	    	if (Shell.room.sendHh) this.hhSocket = new Socket(ip, Integer.parseInt(minLoadServ[2]));

	    	String ss = startSessionCmd();
	    	this.send(ss);
			if (Shell.room.sendHh) Shell.sendStr(this.hhSocket, ss+eol);

			this.thAnswer = new ThreadAnswer(this);
    		this.thAnswer.start();
    	
    		if (Shell.room.sendHh) {
	    		this.thHh = new ThreadHh(this);
	    		this.thHh.hhStrs = this.hhStrs;
    			this.thHh.start();
    		}
    		Shell.timeNoConnection = 0;
		} else {
			if (Shell.timeNoConnection == 0) Shell.timeNoConnection = Misc.getTime();
			this.scheduleToCloseNow("Cannot connect to server");
			Log.log("Cannot connect to AI server. Exit");
		}
	}
	
	public void send(String str) throws Exception {
        /*DataOutputStream out = new DataOutputStream(evSocket.getOutputStream());
        out.writeUTF(str);*/
		Shell.sendStr(this.evSocket, str+eol);
		log.lo("SEND: \"%s\"", str);
	}
	
	public static String ahkExeCommon() {
		return "\""+Action.AHK_PATH+"\" \"ahk/common.ahk\" ";
	}
	protected String ahkWinId() {
		return "0x"+String.format("%08X", this.hwnd)+" ";
	}

	synchronized public void actionMove(int x, int y) throws Exception {
		synchronized (Shell.clickSync) {
			String cmd = ahkExeCommon()+"move "+this.ahkWinId()+x+" "+y;
			Misc.sysCall(ahkExeCommon()+"move "+this.ahkWinId()+x+" "+y);
			Thread.sleep(500);
		}
		this.x = x; this.y = y;
	}
	synchronized public void actionResize(int w, int h) throws Exception {
		synchronized (Shell.clickSync) {
			Misc.sysCall(ahkExeCommon()+"resize "+this.ahkWinId()+w+" "+h);
			Thread.sleep(500);
		}
	}
	
	synchronized public void disconnect() throws Exception {
		try {
			if (this.evSocket != null && !this.evSocket.isClosed()) {
				this.send("END GAME");
				this.send("END SESSION");
				this.evSocket.close();
			}
		} catch (Exception e) {
			this.log.lo("Tried to close ev socket but got:\n%s", Misc.stacktrace2Str(e));
		}
		try {
			if (this.hhSocket != null && !this.hhSocket.isClosed()) {
//				Shell.sendStr(this.hhSocket, "END SESSION");
				this.hhSocket.close();
			}
		} catch (Exception e) {
			this.log.lo("Tried to close hh socket but got:\n%s", Misc.stacktrace2Str(e));
		}
	}
	
	public boolean closed = false;
	private boolean imgsSaved = false;
	synchronized public void close() throws Exception {
		if (Misc.getTime() - this.timeTriedToClose < 5*1000) return;

		this.log.lo("Closing table 0x%08X...", this.hwnd);
		this.disconnect();
		this.actionClose();
		this.log.lo("Close action is applied");
		this.timeTriedToClose = Misc.getTime();

		if (!imgsSaved) {
			if (this.imgsToSave.q.size() > 0)
				this.log.lo("Save %d last imgs. Last img: %s", this.imgsToSave.q.size(), this.imgsToSave.q.get(this.imgsToSave.q.size()-1).getImgFileName());
			else this.log.lo("No imgs to save");
			Shell.threadSaveImg.addImgs(this.imgsToSave.q, String.format("closing table 0x%08X, %d imgs are added", this.hwnd, this.imgsToSave.q.size()));
			this.imgsToSave.q.clear(); // free mem
//			new ThreadSave(this.savedImgs, this.log).start();
			imgsSaved = true;
		}
	}
	
	
	private static int pn2seat(Game g, String pn) {
		for (int i = 0; i < g.allPlayers.length; i++)
			if (g.allPlayers[i].name.equals(pn)) return i+1; 
		return -1;
	}
	
	public List<String> ev2Server(Game g, Event[] evs) throws Exception {
		List<String> ret = new ArrayList<String>();
		g = g.clone();
		
		if (!g.bbPosted) {
			// New game
			/*START GAME BM 100000546 - L EUR 2.00
			DATE 2009.01.01 00:00:45 +0300
			TABLE Sample1
			SEAT (1) 186.25 'Mank''ament'
			SEAT (2) 520.81 'jbarrasa'
			SEAT (3) 22.00 'gsus11111'
			SEAT (4) 232.00 'ana2136'
			SEAT (5) 100.00 'SamplePlayer'
			DEALER (3)
			BLIND SB 1.00 - (4) 'ana2136'
			BLIND BB 2.00 - (5) 'SamplePlayer'
			PREFLOP
			DEALT [4d,2h] (5) 'SamplePlayer'
			FOLD - (1) 'Mank''ament'
			CALL 2.00 (2) 'jbarrasa'
			FOLD - (3) 'gsus11111'		*/	
			
			int bb = -1;
			String sbName = null, bbName = null;
			for (Event ev: evs) {
				if (ev.type == Event.TYPE_BB || ev.type == Event.TYPE_BB_ALLIN) {
					bb = (int)ev.iVal;
					bbName = ev.who;
				}
				if (ev.type == Event.TYPE_SB) sbName = ev.who;
			}
			if (this.gameCount > 1) ret.add("END GAME");
			String[] ns = Shell.room.getProtocolNetworkSkin();
			ret.add("START GAME "+ns[1]+" "+ns[0]+" "+Shell.room.getGidPrefix()+g.id+" - NL "+this.tableLook.currency+" "+String.format("%1.2f", bb/100.0));
			
			ret.add("DATE "+new SimpleDateFormat("yyyy.MM.dd HH:mm:ss Z", Locale.US).format(new Date(Misc.getTime())));
			ret.add("TABLE "+this.tableLook.title);
			
			List<String> seats = new ArrayList<String>();
			for (Player p: g.players)
				seats.add("SEAT ("+pn2seat(g, p.name)+") "+String.format("%1.2f", p.stack/100.0)+" '"+p.name.replace("'", "''")+"'");
			Collections.sort(seats);
			ret.addAll(seats);

			int dealerSeat = -1;
			if (g.players.length == 2) {
				// Game can be without sb, so take !bbName instead of sbName
				String notBbName = g.players[0].name.equals(bbName) ? g.players[1].name : g.players[0].name;
				dealerSeat = pn2seat(g, notBbName);
			} else {
				String afterDealer = sbName == null ? bbName : sbName;
				for (int i = 0; i < g.players.length; i++) {
					if (g.players[i].name.equals(afterDealer)) dealerSeat = pn2seat(g, g.players[i-1 < 0 ? g.players.length-1 : i-1].name);
				}
			}
			if (dealerSeat == -1) this.log.lo("ERROR! Dealer seat is %d", dealerSeat);
			ret.add("DEALER ("+dealerSeat+")");
			
		} 
		
		for (Event ev: evs) {
			Player p = g.getCurMovePlayer();
			String spn = null;
			if (ev.who != null) spn = "("+pn2seat(g, ev.who)+") '"+ev.who.replace("'", "''")+"'";
			if (ev.type == Event.TYPE_SB) ret.add("BLIND SB "+ev.iVal/100.0+" - "+spn);
			else if ((ev.type == Event.TYPE_BB || ev.type == Event.TYPE_BB_ALLIN) && !g.bbPosted) {
				ret.add("BLIND BB "+ev.iVal/100.0+" - "+spn);
			} else if ((ev.type == Event.TYPE_BB || ev.type == Event.TYPE_BB_ALLIN) && g.bbPosted) {
				ret.add("BLIND POST "+ev.iVal/100.0+" - "+spn);
			} else if (ev.type == Event.TYPE_HOLES) {
				ret.add("PREFLOP");
				if (ev.iVal != 0) {
					Card[] h = Card.mask2Cards(ev.iVal);
					String hs = Card.cards2Str(new Card[]{h[0]})+","+Card.cards2Str(new Card[]{h[1]});
					ret.add("DEALT ["+hs+"] ("+pn2seat(g, g.heroName)+") '"+g.heroName.replace("'", "''")+"'");
				}
			} else if (ev.isBoard()) {
				Card[] b = Card.mask2Cards(ev.iVal);
				if (ev.type == Event.TYPE_FLOP) ret.add("FLOP ["+Card.cards2Str(b,",")+"]");
				else if (ev.type == Event.TYPE_TURN) ret.add("TURN ["+Card.cards2Str(g.board,",")+"] ["+Card.cards2Str(b,",")+"]");
				else if (ev.type == Event.TYPE_RIVER) ret.add("RIVER ["+g.board[0]+","+g.board[1]+","+g.board[2]+"] ["+g.board[3]+"] ["+Card.cards2Str(b,",")+"]");
			} else if (ev.isAllIn()) {
				double put = ev.iVal/100.0;
				if (ev.isRaise()) put = (ev.iVal-p.putToPotInStage)/100.0;
				ret.add("ALL-IN "+put+" "+spn);
			} else if (ev.isFold()) ret.add("FOLD - "+spn);
			else if (ev.type == Event.TYPE_CHECK) ret.add("CHECK - "+spn);
			else if (ev.isCallOrCheck()) ret.add("CALL "+ev.iVal/100.0+" "+spn);
			else if (ev.isBet()) ret.add("BET "+ev.iVal/100.0+" "+spn);
			else if (ev.isRaise()) ret.add("RAISE "+(ev.iVal-p.putToPotInStage)/100.0+" "+spn);
			else if (ev.type == Event.TYPE_SHOWDOWN) ret.add("SHOW DOWN");
			else if (ev.type == Event.TYPE_SHOW) {
				Card[] h = Card.mask2Cards(ev.iVal);
				if (h.length == 2) ret.add("SHOW PAIR ["+h[0]+","+h[1]+"] "+spn);
				else if (h.length == 1) ret.add("SHOW CARDS ["+h[0]+"] "+spn);
			}
			
			g.procEvent(ev);
		}
		
		return ret;
	}

	public void applyAnswer() throws Exception {
		// ANSWER F 0 28 Normal
		// ANSWER R 0.08 305 Normal
		assert answer.indexOf("ANSWER") == 0;
		
		if (this.game == null) {
			this.log.lo("ERROR! Cannot apply answer because game is null");
			this.timeAnswerApplied = Misc.getTime();
			return;
		}
		Game g = this.game;
		Player p = g.getCurMovePlayer();
		if (p == null) {
			this.log.lo("ERROR! Cannot apply answer because cur move player is null");
			this.timeAnswerApplied = Misc.getTime();
			return;
		}
	
		this.applyAnswerAction();
		
		this.timeAnswerApplied = Misc.getTime();
		this.log.lo("Applied answer: %s", this.answer);
		
		String[] sr = answer.split(" +");
		if (sr[1].equals("F") && this.toBeMigrated) {
			try {
				this.log.lo("Migrating...");
				this.disconnect();
				this.connectToServer();
			} finally {
				this.toBeMigrated = false;
			}
		}
	}
	
	public void scheduleToCloseNow(String reason) {
		if (this.toBeClosedNow) return;
		log.lo("Table scheduled to be closed now. Reason: %s", reason);
		this.toBeClosedNow = true;
	}
	public void scheduleToCloseAfterGame(String reason) {
		if (this.toBeClosedAfterGame) return;
		log.lo("Table scheduled to be closed after game. Reason: %s", reason);
		this.toBeClosedAfterGame = true;
	}
	public void scheduleToCloseNextBb(String reason) {
		if (this.toBeClosedNextBb) return;
		log.lo("Table scheduled to be closed next BB. Reason: %s", reason);
		this.toBeClosedNextBb = true;
	}
	public void scheduleToSitout(String reason) {
		if (this.needSitout) return;
		log.lo("Table is set to sitout. Reason: %s", reason);
		this.needSitout = true;
	}
	
	public long timeLastEvents = -1; //Misc.getTime();
	private int buttonsSentForGameState = -1;
	private String prevGid = "";
	protected TableLookBuilder tableLookBuilder;
	private long timeLastImageObtained = -1;
	private long timeLastNotSkippedImg = -1;
	private TableLook prevTableLook;
	public int heroStack = 0;
	private boolean skipPauseIsReported = false;
	private long timeLastTableRegister = 0;
	private int tlBuildFailCount = 0;
	
	synchronized public void procImg(WndData wndData) throws Exception {
		
		try {

//			long imgt = 5*1000;
//			if (this.timeLastImageObtained != -1
//					&& Misc.getTime() - this.timeLastImageObtained > imgt)
//				this.log.lo("ERROR! Too big pause between imgs: %d secs", imgt/1000);
				
			this.timeLastImageObtained = Misc.getTime();
			
//			if (!this.skipPauseIsReported && this.timeLastEvents != -1 && this.timeLastNotSkippedImg != -1
//					&& Misc.getTime() - this.timeLastNotSkippedImg > imgt) {
//				this.log.lo("ERROR! Time period when skipping imgs is too big: %d secs", (Misc.getTime() - this.timeLastNotSkippedImg)/1000);
//				this.skipPauseIsReported = true;
//			}
			
			if (this.toBeClosedNow) return;

			// Do not proc imgs more often than once a second
//			boolean twoPls = false;
//			if (this.game != null && this.game.players.length <= 2) twoPls = true;
//			if (this.timeLastImageProcessed != -1
//					&& Misc.getTime()-this.timeLastImageProcessed < (twoPls?0:1000)) return;

			this.log.lo("Processing file: %s", wndData.getImgFileName());		
			this.imgsToSave.add(wndData);
			
			if (this.timeResized != -1 && Misc.getTime() - this.timeResized < 5000) {
				this.log.lo("Waiting for resize to finish");
				return;
			}
			if (Misc.getTime() - this.timeBorn < Shell.room.waitNewTableDrawSec*1000) {
				this.log.lo("Waiting new window to draw well");
				return;
			}
		
			long t = Misc.getTime();
	        this.timeLastImg = t;
	        this.wndData = wndData;
	        TableLook tl = null;
	        
	        // Build table look
	        try {
	        	this.timeLastTableLookBuild = Misc.getTime();
	        	Shell.timeLastTableLookBuild = this.timeLastTableLookBuild;
	        	tl = this.tableLookBuilder.buildTableLook(wndData, this.prevTableLook);
	        } catch (Exception e) {
	        	this.tlBuildFailCount++;
	        	String pref = "ERROR"; if (this.tlBuildFailCount < 3) pref = "WARNING";
	        	log.lo("%s! Cannot build table look:\n%s", pref, Misc.stacktrace2Str(e));
		        Shell.threadSaveImg.addImgs(this.imgsToSave.getLast(5), String.format("0x%08X tl build fail, adding %d imgs", this.hwnd, 5));
	        	return;
	        }
	        this.tlBuildFailCount = 0;
        	this.timeLastTableLookBuildSucc = Misc.getTime();
	        Shell.timeLastTableLookBuildSucc = this.timeLastTableLookBuildSucc;
//	        log.lo("Table look successfully built. Timestamp %d", this.timeLastSuccTableLookBuild);
	        
			if (Misc.getTime() - this.timeLastTableRegister > 30*1000) {
				Shell.room.registerTablePlay(tl.tableId, Shell.curSes.hero.nick, 60*1000);
				this.timeLastTableRegister = Misc.getTime();
			}

			if (tl.heroPos != -1 && tl.stacks[tl.heroPos] != null) {
				this.heroStack = tl.stacks[tl.heroPos];
				if (tl.bets[tl.heroPos] != null) this.heroStack += tl.bets[tl.heroPos];
			}
	        
	        /*if (this.game != null
	        		&& !this.game.isFinished()
	        		&& !tl.gameId.equals(this.game.id)
	        		&& this.prevTableLook != null
	        		&& !tl.gameId.equals(this.prevTableLook.gameId)) {
	        	Log.log("WARNING! Not finished game is detected:\n%s", this.game);
		        Shell.threadSaveImg.queue.addAll(this.imgsToSave.getAllAndRemove());
	        }*/
	        
	        // Save img
	        if (!tl.isSame(this.prevTableLook)) {
		        log.lo("New table look is built:\n%s", tl.toString(5));
//	        	log.lo("Added img to save queue: %s", wndData.getImgFileName());
//	        	Shell.threadSaveImg.addImg(wndData, "new look");
	        }
	        this.prevTableLook = tl; // this.prevTableLook is used for buildTableLook only
	        
	        if (tl.skipReason != null) {
	        	log.lo("WARNING! Table look is skipped due to: %s", tl.skipReason);
	        	if (this.timeLastNotSkippedImg != -1 && Misc.getTime() - this.timeLastNotSkippedImg > 5000)
			        Shell.threadSaveImg.addImgs(this.imgsToSave.getLast(1), String.format("Too many skipped imgs"));
	        	return;
	        }
	        
	        this.skipPauseIsReported = false;
	        TableLook prevLook = this.tableLook;
	        this.tableLook = tl; // Assign new tl only if it's successfully built and not skipped
	        this.timeLastNotSkippedImg = Misc.getTime();

//	        // This should be in GameBuilder but we do not want to break game build and GameBuilder doesn't have log to report the issue
//	        if (!Shell.dsa && prevLook != null && this.tableLook.board.length >= prevLook.board.length)
//	        	for (int i = 0; i < prevLook.board.length; i++)
//	        		if (this.tableLook.board[i] != prevLook.board[i]) {
//	        			log.lo("ERROR! Board cards were changed: %s -> %s", Card.cards2Str(prevLook.board), Card.cards2Str(this.tableLook.board));
//	    		        Shell.threadSaveImg.addImgs(this.imgsToSave.getAllAndRemove(), "board cards were changed");
//	        		}
	        		
	        
	        // Check equal names at the table
	        Set<String> ns = new HashSet<String>();
	        for (String n: this.tableLook.pnames) {
	        	if (n == null) continue;
	        	if (ns.contains(n)) {
	        		this.scheduleToCloseNow("ERROR! Equal names at the table: "+n);
	        		return;
	        	}
	        	ns.add(n);
	        }
			if (this.tableLook.isErr)
				this.scheduleToCloseNow("Error message is detected at the table");
			
			if (this.tableLook.heroNameDetected != null
					&& !Shell.curSes.hero.nick.equals(this.tableLook.heroNameDetected)) {
				Log.log("ERROR! Incorrect hero nick. Exit. Configured: %s, detected: %s", Shell.curSes.hero.nick, this.tableLook.heroNameDetected);
				System.exit(0);
			}

        	if (this.tableLook.isButtons && (prevLook == null || !prevLook.isButtons
        			|| prevLook.board.length != this.tableLook.board.length
        			|| !Arrays.equals(prevLook.bets, this.tableLook.bets))) {
        		// New buttons are showed, save img
	        	this.timeButtonsShowed = Misc.getTime();
	        	Shell.timeLastButtons = this.timeButtonsShowed;
	        	if (this.timeFirstButtonsShowed == -1) this.timeFirstButtonsShowed = this.timeButtonsShowed;
	        	Shell.threadSaveImg.addImg(wndData, "new buttons");
        	}
        	
        	// Save img with new hero holes
        	long prevHoles = 0;
        	if (prevLook != null && prevLook.heroPos != -1 && prevLook.holes[prevLook.heroPos] != null) prevHoles = Card.cards2Mask(prevLook.holes[prevLook.heroPos]);
        	long curHoles = 0;
        	if (this.tableLook.heroPos != -1 && this.tableLook.holes[this.tableLook.heroPos] != null) curHoles = Card.cards2Mask(this.tableLook.holes[this.tableLook.heroPos]);
        	if (prevHoles != curHoles) Shell.threadSaveImg.addImg(wndData, "new hero holes");

	        // Build game
			GameEvs ge = null;
			int inpc = 0; for (boolean i: this.tableLook.isInPlay) inpc += (i?1:0);
			if (Shell.room.buildGameAfterHeroFinished()
					|| (this.tableLook.isHeroInPlay && inpc > 1)) {
				try {
					ge = this.gameBuilder.procTableLook(this.game, this.tableLook);
				} catch (Exception e) {
		        	log.lo("ERROR! Cannot build game:\n%s\nTableLook which caused the exception:\n%s", Misc.stacktrace2Str(e), this.tableLook);
		        	int s = this.imgsToSave.q.size();
			        Shell.threadSaveImg.addImgs(this.imgsToSave.getAllAndRemove(), String.format("0x%08X tl proc fail, %d imgs are added", this.hwnd, s));
		        	return;
				}
				this.timeLastGameBuildSucc = Misc.getTime();
				Shell.timeLastGameBuildSucc = this.timeLastGameBuildSucc;
			
				if (ge.evs.length > 0) {
//					this.log.lo("TableLook which produced %d events:\n%s", ge.evs.length, this.tableLook);
					Shell.threadSaveImg.addImg(wndData, String.format("%08X tl produced %d events", this.hwnd, ge.evs.length));
				}
				
				if (ge.game != null) {
					this.gameCount++;
					if (Shell.curSes != null) Shell.curSes.addHand();
				}
	
				// Send new events to the server
				if (ge.game != null) this.game = ge.game; // new game is started
				
	        	if (ge.evs.length > 0) {
	        		timeLastEvents = Misc.getTime();
		        	
	        		int mbb = 0; for (int bb: Shell.curSes.limits) mbb = Math.max(mbb, bb);
					if (this.game.bb > mbb) {
						this.scheduleToCloseNow("Table bb "+this.game.bb+" is bigger then maximum allowed bb "+mbb);
						return; // Just in case
					}
					if (Misc.checkTestFile("close")) this.scheduleToCloseAfterGame("Found 'close' file");
					if (Misc.checkTestFile("closenow")) this.scheduleToCloseNow("Found 'closenow' file");
	        		
		        	List<String> toSend = ev2Server(this.game, ge.evs);
//		        	this.log.lo("evs.length=%d, toSend.size()=%d", ge.evs.length, toSend.size());
//		        	this.log.lo("Evs:\n%s", Arrays.toString(ge.evs));
//		        	this.log.lo("toSend:\n", toSend);
		        	for (String s: toSend) {
		        		this.send(s);
		        	}
		        }
	
				for (Event ev: ge.evs) {
					this.game.procEvent(ev);
					this.log.lo("%s\n%s", ev, this.game);
				}
			}

        	Game g = this.game;
        	if (g != null) {
        		if (!this.prevGid.equals(g.id)) this.buttonsSentForGameState = -1;
        		this.prevGid = g.id;
        	}
        	
           	// Send buttons
        	if (this.tableLook.isButtons && g != null
        			&& g.getCurMovePlayer() != null
        			&& g.getCurMovePlayer().name.equals(g.heroName)
        			&& this.buttonsSentForGameState != g.mcount) {

	        	Player p = g.getCurMovePlayer();
	        	String btn = "BUTTONS F";
	        	if (g.getCall() == 0) btn += "K";
	        	else if (g.getCall() >= p.stack) btn += "A";
	        	else btn += "C";
	        	boolean canRaise = g.getMinRaise() != -1;
	        	if (canRaise) assert g.getMinRaise() <= p.stack;
	    		if (canRaise && g.raisedVal == 0) btn += "B";
	    		else if (canRaise && g.raisedVal > 0 && g.getMinRaise() < p.stack) btn += "R";
	    		else if (canRaise && g.getMinRaise() == p.stack) btn += "A";
	
	        	btn += " ? ? ? 10000 ("+pn2seat(g, g.heroName)+") '"+g.heroName.replace("'", "''")+"'";
	        	this.send(btn);
	        	this.buttonsSentForGameState = g.mcount;
	        	this.timeButtonsSent = Misc.getTime();
        	}

        	// Check enough players
        	if (this.resized && this.tableLook.heroPos != -1) {
	        	int minPls = Integer.parseInt(Shell.cfg.getProperty("players.amount.required.to.play"));
	        	int sc = 0; for (boolean so: this.tableLook.isSeatOccupied) sc += so?1:0;
	        	if (sc < minPls)
	        		this.scheduleToCloseAfterGame(String.format("table %s %08X doesn't have enough players to play. Seating players=%d minOpps=%d", this.tableLook.title, this.hwnd, sc, minPls));
        	}

		} catch (Throwable e) {
			this.scheduleToCloseNow("Error during processing of img: "+wndData.getImgFileName());
	        Shell.threadSaveImg.addImgs(this.imgsToSave.getAllAndRemove(), String.format("0x%08X exception in procImg", this.hwnd));
			this.log.lo("%s", Misc.stacktrace2Str(e));
		}
	}
	
	public static void main(String[] args) throws Exception {
//		File[] fs = FUtil.dRead("imgs");
		File[] fs = FUtil.dRead("c:/temp/imgs");
		
		Properties cfg = new Properties();
		cfg.load(new FileReader("shell.cfg"));
//		CpaTable cpaTable = new CpaTable(cfg.getProperty("hero.name"), cfg, Log.defl);
		Table table = new TableGg(0);
		for (File f: fs) {
			if (f.getName().compareTo("20150921_194436.570_0x00080892.png") < 0) continue;
			Log.log("File: %s", f.getName());

			BufferedImage img = ImageIO.read(f);
			if (f.getName().contains("20150102_143427.734_0x0690B62.png")) {
				int tmp = 0;
			}
			WndData wndData = new WndData();
			wndData.img = img;
			wndData.title = "#5622 Hum XI 0.01/0.02 NL - Google Chrome";
			table.procImg(wndData);
		}
	}
}
