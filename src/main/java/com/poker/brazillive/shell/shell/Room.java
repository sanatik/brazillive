package com.poker.brazillive.shell.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.poker.brazillive.shell.shell.Shell.Hero;
import com.poker.brazillive.shell.shell.gg.RoomGg;
import com.poker.brazillive.shell.util.FUtil;
import com.poker.brazillive.shell.util.Log;
import com.poker.brazillive.shell.util.Misc;
import com.poker.brazillive.shell.util.MultipartUtility;

public abstract class Room {
	
	public boolean sendHh = false;
	public boolean clickActiveTable = false;
	public long waitNewTableDrawSec = 3;
	
	public static Room createRoom() throws Exception {
		String nw = Shell.getNetworkSkin()[0];
		return (Room)Class.forName("shell."+nw.toLowerCase()+".Room"+nw).newInstance();
	}
	
	protected Room() {
		new Thread() {
			public void run() {
				while (true) {
					try {
						Room.sendGraph();
						Thread.sleep(60*1000);
					} catch (Exception e) {
						e.printStackTrace();						
					}
				}
			}
		}.start();
	}

	public abstract String getHhSign();
	public abstract boolean isTable(WndData wndData) throws Exception;
	public abstract String[] getProtocolNetworkSkin();
	public abstract void checkIp() throws Exception;
	public abstract boolean buildGameAfterHeroFinished();
	public abstract long getMoveDelayMs(Table t);
	public abstract String getGidPrefix();
	public abstract long needSitoutPeriod() throws Exception;
	public abstract boolean isSitoutNextBbSupported() throws Exception;
	public abstract void registerTablePlay(String tid, String nick, long lifeTime) throws Exception;

	private static void sortByNameTime(List<Session> sess) {
		Collections.sort(sess, new Comparator<Session>() {
			public int compare(Session s1, Session s2) {
				int r = s1.hero.nick.compareTo(s2.hero.nick);
				if (r != 0) return r;
				return Long.compare(s1.timeStart, s2.timeStart);
			}
		});
	}
	private static void sortByTime(List<Session> sess) {
		Collections.sort(sess, new Comparator<Session>() {
			public int compare(Session s1, Session s2) {
				return Long.compare(s1.timeStartSched, s2.timeStartSched);
			}
		});
	}

	public static String ses2graph(List<Session> sess) {
		final int hourCharCount = 4;
		final int scaleLenHours = 24*5;
		final int scaleHourEach = 1;
		final int nameColWidth = 15;
		String ret = "";
		
		List<Session> ss = new ArrayList<Session>(sess);
		sortByNameTime(ss);
		
		long startTime = Misc.getTime()/1000/3600*3600*1000 - 4*3600*1000;
		long sod = Misc.getStartOfDay(startTime);
		
		ret += String.format("%"+(nameColWidth+1+4)+"s ", "|");
		for (int h = 0; h < scaleLenHours; h++) {
			long t = startTime+h*3600*1000;
			if (((t-sod)/1000/3600)%scaleHourEach == 0) ret += Misc.dateFormat(t, "HH")+String.format("%"+(hourCharCount-2)+"s", "");
			else ret += String.format("%"+hourCharCount+"s", "");
		}
		ret += "\n";
		
		long t = 0;
		Session ps = null;
		for (Session s: ss) {
			if (s.timeFinishSched <= startTime) continue;
			if (s.timeStartSched > startTime+scaleLenHours*3600*1000) continue;
			if (ps == null || !s.hero.login.equals(ps.hero.login)) {
				if (ps != null) ret += "\n";
				ret += String.format("%-"+nameColWidth+"s %2s | ", s.hero.nick, s.rsi.contains("ne")?"NE":"");
				t = startTime;
			}
			
			long noSesLen = Math.round(Math.max(0, s.timeStartSched-t)/1000.0/3600*hourCharCount);
			for (int i = 0; i < noSesLen; i++) ret += " ";
			t = Math.max(t, s.timeStartSched);
			
			long sesLen = Math.round((s.timeFinishSched - t)/1000.0/3600*hourCharCount);
			if (sesLen == 0 && s.getDurSched() > 0) sesLen = 1;
			for (int i = 0; i < sesLen; i++) ret += "*";
			t = Math.max(t, s.timeFinishSched);
			
			ps = s;
		}
		
		return ret;
	}

	private static class Cfg {
		public Map<String,String> fromCsv;
		public String login;
		public String pwd;
		public String nick;
		public int[] limits;
		public long[] timeDaySesStart;
		public long sesDuration;
		public long sesStartRnd;
		public long sesDurationRnd;
		public int maxTables;
		public int sitFrom;
		public int sitTo;
		double dayOffProb;
		public String rsi; // Room Specific Info
		
		
		public Cfg(Map<String,String> csv) {
			this.fromCsv = csv;
			// Login,Pwd,Nick,Limits,Sessions start,Ses start rnd,Session duration,Ses dur rnd
			this.login = csv.get("Login");
			this.pwd = csv.get("Pwd");
			this.nick = csv.get("Nick") == null ? this.login : csv.get("Nick");
			
			String[] slims = csv.get("Limits").split("\\s+");
			this.limits = new int[slims.length];
			for (int i = 0; i < slims.length; i++)
				this.limits[i] = Integer.parseInt(slims[i]);
			
			String[] sesStartStrs = csv.get("Ses start").split("\\s+");
			this.timeDaySesStart = new long[sesStartStrs.length];
			for (int i = 0; i < sesStartStrs.length; i++) {
				String[] sr2 = sesStartStrs[i].split(":");
				this.timeDaySesStart[i] = (Long.parseLong(sr2[0])*3600 + Long.parseLong(sr2[1])*60)*1000;
			}
			
			this.sesStartRnd = (long)(Double.parseDouble(csv.get("Ses start rnd"))*3600*1000);
			this.sesDuration = (long)(Double.parseDouble(csv.get("Ses dur"))*3600*1000);
			this.sesDurationRnd = (long)(Double.parseDouble(csv.get("Ses dur rnd"))*3600*1000);
			
			this.maxTables = Integer.parseInt(csv.get("Max tables"));

			String[] sr = csv.get("Sit pls").replaceAll("sp", "").split("-");
			this.sitFrom = Integer.parseInt(sr[0]);
			this.sitTo = Integer.parseInt(sr[1]);
			
			this.dayOffProb = csv.get("Day off prob") == null ? 0 : Double.parseDouble(csv.get("Day off prob"));
			this.rsi = csv.get("RSI");
		}
		
		public String toString() {
			String str = "";
			str += String.format("%s,%s,%s,", this.login, this.pwd, this.nick);
			for (int l: this.limits) str += String.format(" %d", l);
			str += ",";
			for (long st: this.timeDaySesStart) {
				long mins = st/1000/60; 
				str += String.format(" %02d:%02d", mins/60, mins%60);
			}
			str += String.format(",%1.1f,%1.1f,%1.1f", this.sesStartRnd/1000.0/3600, this.sesDuration/1000.0/3600, this.sesDurationRnd/1000.0/3600);
			return str;
		}
		
		public static List<Cfg> createCfgs(String csvFile) throws Exception {
			List<String> strs = FUtil.fRead(csvFile);
			List<Map<String,String>> csvs = Misc.parseCsv(strs);
			List<Cfg> cfgs = new ArrayList<Cfg>();
			for (Map<String,String> csv: csvs) {
				if (csv.get("Login") == null || csv.get("Login").indexOf("#") == 0) continue;
				Cfg cfg = new Cfg(csv);
				cfgs.add(cfg);
			}
			return cfgs;
		}
	}

	private static int[] str2IntAr(String str) {
		String[] strs = str.split(" ");
		int[] is = new int[strs.length];
		for (int i = 0; i < is.length; i++) is[i] = Integer.parseInt(strs[i]);
		return is;
	}
	private static String intAr2Str(int[] is) {
		String ret = "";
		for (int i = 0; i < is.length; i++) ret += " "+is[i];
		return ret.trim();
	}

	private static void ses2File(Session s, File f) throws Exception {
		Properties ps = new Properties();
		ps.setProperty("fromFile", s.fromFile == null ? "null" : s.fromFile.getAbsolutePath());
		ps.setProperty("start", Misc.dateFormatHumanY(s.timeStartSched));
		ps.setProperty("finish", Misc.dateFormatHumanY(s.timeFinishSched));
		ps.setProperty("duration", String.format("%02d:%02d", s.getDurSched()/1000/60/60, s.getDurSched()/1000/60%60));
		ps.setProperty("login", s.hero.login);
		ps.setProperty("pwd", s.hero.pwd);
		ps.setProperty("nick", s.hero.nick);
		ps.setProperty("limits", intAr2Str(s.limits));
		ps.setProperty("max.tables", ""+s.maxTables);
		ps.setProperty("rsi", ""+s.rsi);
		ps.setProperty("proxy", s.proxy);
		ps.setProperty("network", s.network);
		ps.setProperty("skin", s.skin);
		ps.setProperty("sit.from", ""+s.sitFrom);
		ps.setProperty("sit.to", ""+s.sitTo);
		
		FileOutputStream fos = new FileOutputStream(f);
		ps.store(fos, "");
		fos.close();
	}
	private static Session file2Ses(File f) throws Exception {
		Properties ps = new Properties();
		FileInputStream fis = new FileInputStream(f);
		ps.load(fis);
		fis.close();
//		String ff = ps.getProperty("from.file");
		
		return new Session(
				f,
				Misc.parseDateHumanY(ps.getProperty("start")),
				Misc.parseDateHumanY(ps.getProperty("start")),
				Misc.parseDateHumanY(ps.getProperty("finish")),
				new Shell.Hero(
						ps.getProperty("login"),
						ps.getProperty("pwd"),
						ps.getProperty("nick")
					),
				str2IntAr(ps.getProperty("limits")),
				Misc.getPropInt(ps, "max.tables"),
				ps.getProperty("network"),
				ps.getProperty("skin"),
				ps.getProperty("proxy"),
				Misc.getPropInt(ps, "sit.from"),
				Misc.getPropInt(ps, "sit.to"),
				ps.getProperty("rsi")
			);
	}

	private static String getCfgFile() {
		return "session.csv";
	}
	
	private static String getSessFolder() {
//		String cfgDate = Misc.dateFormat(new File(getCfgFile()).lastModified(), "yyyyMMdd_HHmmss");
//		return SES_PREF+cfgDate;
		return "sessions";
	}
	
	private List<Session> createSessions1(Cfg cfg, Cfg oldCfg) throws Exception {
		List<Session> ret = new ArrayList<Session>();

		if (oldCfg != null && cfg.fromCsv.equals(oldCfg.fromCsv)) return ret;
		if (oldCfg == null || !cfg.fromCsv.equals(oldCfg.fromCsv)) {
			// Remove sessions for changed Cfg
			File[] fs = new File(getSessFolder()).listFiles(new Filter("^[^_].*$"));
			int c = 0;
			if (fs != null) for (File f: fs) {
				if (f.getName().equals("arch")) continue;
				Session s = file2Ses(f);
				if (s.hero.login.equals(cfg.login)) {
					if (!f.delete()) Log.log("ERROR! Cannot remove file: '%s'", f.getAbsolutePath());
					c++;
				}
			}
			if (c > 0) {
				Log.log("Removed %d files for changed acc %s", c, cfg.login);
				Thread.sleep(500);
			}
		}
		
		final int periodDays = 30*3;
		final int minGapMins = 20;
		Random rand = new Random();
		long todayStart = Misc.getStartOfDay(Misc.getTime());

		for (int d = 0; d < periodDays; d++) {
			if (rand.nextDouble() <= cfg.dayOffProb) continue;
			for (long ss: cfg.timeDaySesStart) {
				long ds = todayStart+d*24*3600*1000L; // L - to avoid int overflow
				int startRnd = rand.nextInt((int)cfg.sesStartRnd+1);
				long start = ds+ss-cfg.sesStartRnd/2+startRnd;
				long dur = cfg.sesDuration-cfg.sesDurationRnd/2+rand.nextInt((int)cfg.sesDurationRnd+1);
				String[] nws = Shell.getNetworkSkin();
				Session s = new Session(null, start, start, start+dur,
						new Hero(cfg.login,cfg.pwd,cfg.nick), cfg.limits, cfg.maxTables,
						nws[0], nws[1], "-", cfg.sitFrom, cfg.sitTo, cfg.rsi);
				ret.add(s);
			}
		}
		
		// Create min gaps
		sortByNameTime(ret);
		Session ps = null;
		long mg = minGapMins*60*1000;
		Set<Session> rm = new HashSet<Session>();
		for (Session s: ret) {
			if (ps != null && s.timeStartSched - ps.timeFinishSched < minGapMins*60*1000) {
				long mid = (s.timeStartSched + ps.timeFinishSched)/2;
				ps.timeFinishSched = mid - mg/2;
				s.timeStartSched = mid + mg/2;
			}
			ps = s;
		}
		
		for (Session s: ret) {
			if (s.getDurSched() <= 0) rm.add(s);
			if (s.timeFinishSched <= Misc.getTime()) rm.add(s);
		}
		for (Session s: rm) ret.remove(s);
		
		Log.log("Created %d sessions for acc %s", ret.size(), cfg.login);
		return ret;
	}
	
	private static class Filter implements FilenameFilter {
		private String re;
		public Filter(String re) {
			this.re = re;
		}
		public boolean accept(File f, String name) {
			return name.matches(re);
		}
	}
	
	public void createSessionsIfNeeded() throws Exception {
		try {
			sesLockGet();
			
			File[] ocs = new File(getSessFolder()).listFiles(new Filter("^_cfg\\..*$"));
			if (ocs == null) ocs = new File[0];
			sortFiles(ocs);
			
			List<Cfg> oldCfgs = new ArrayList<Cfg>();
			if (ocs.length > 0) {
				File oc = ocs[ocs.length-1];
				long ocd = Misc.parseDateIntel(oc.getName().split("\\.")[1]);
				if (ocd == new File(getCfgFile()).lastModified()/1000*1000) return;
				oldCfgs = Cfg.createCfgs(oc.getAbsolutePath());
			}
			
			Map<String, Cfg> oldCfgsMap = new HashMap<String, Cfg>();
			for (Cfg cfg: oldCfgs) oldCfgsMap.put(cfg.login, cfg);

			List<Cfg> cfgs = Cfg.createCfgs(getCfgFile());
			
			List<Session> newSess = new ArrayList<Session>();
			for (Cfg cfg: cfgs)
				newSess.addAll(this.createSessions1(cfg, oldCfgsMap.remove(cfg.login)));

			
			{
				// Remove all sessions for accs which were removed from cfg
				// oldCfgsMap now contains removed accs only
				File[] fs = new File(getSessFolder()).listFiles(new Filter("^[^_].*$"));
				int c = 0;
					
				if (fs != null) for (File f: fs) {
					if (f.getName().equals("arch")) continue;
					if (!f.exists()) continue; // Some problem with the file system?
					Session s = file2Ses(f);
					if (oldCfgsMap.containsKey(s.hero.login)) {
						if (!f.delete()) Log.log("ERROR! Cannot remove file: '%s'", f.getAbsolutePath());
						c++;
					}
				}
				if (c > 0) {
					Log.log("Removed %d ses files for accs which were removed from cfg: %s", c, oldCfgsMap.keySet());
					Thread.sleep(500);
				}
			}
	
			sortByTime(newSess);
			
			new File(getSessFolder()).mkdir();
			for (Session s: newSess) {
				ses2File(s, new File(getSessFolder()+"/"+Misc.dateFormatIntel(s.timeStartSched)+"."+s.hero.login));
			}

			File[] fs = new File(getSessFolder()).listFiles(new Filter("^[^_].*$"));
			List<Session> sess = new ArrayList<Session>();
			for (File f: fs) {
				if (f.getName().equals("arch")) continue;
				if (!f.exists()) continue; // Some problem with the file system
				sess.add(file2Ses(f));
			}
			
			for (File f: ocs) f.delete();
			FUtil.copyFile(getCfgFile(), getSessFolder()+"/_cfg."+Misc.dateFormatIntel(new File(getCfgFile()).lastModified()));
			FUtil.fPut("ses.graph", ses2graph(sess));
			
		} finally {
			sesLockRelease();
		}
		
	}

	private static File sesLockFile = new File("ses.lock");
	private static RandomAccessFile rFile;
	private static FileChannel sesLockCh;
	private static FileLock sesLock;
	
	private static void sesLockGet() throws Exception {
		rFile = new RandomAccessFile(sesLockFile, "rw");
		sesLockCh = rFile.getChannel();
		sesLock = sesLockCh.lock();
	}
	private static void sesLockRelease() throws Exception {
		sesLock.release();
		rFile.close();
		sesLockCh.close();
		sesLockFile.delete();
	}
	
	private static Comparator<File> fCmp = new Comparator<File>() {
		public int compare(File f1, File f2) {
			return f1.getName().compareTo(f2.getName());
		}
	};
	
	private static void sortFiles(List<File> fs) {
		Collections.sort(fs, fCmp);
	}
	private static void sortFiles(File[] fs) {
		Arrays.sort(fs, fCmp);
	}
	
//	private static void rename(String from, String to) throws Exception {
//		if (!new File(from).renameTo(new File(to)))
//			Log.log("ERROR! Cannot rename '%s' -> '%s'", from, to);
//	}
	private static void rename(String from, String to) throws Exception {
		FUtil.copyFile(from, to);
		
//		Files.delete(Paths.get(from));
		if (!new File(from).delete())
			Log.log("Error! Cannot delete file '%s'", from);
	}
	private static void move2Arch(File sf, String reason) throws Exception {
		Log.log("Moving %s session to arch. Reason: %s", sf.getName(), reason);
		rename(sf.getAbsolutePath(), getSessFolder()+"/arch/"+sf.getName());
	}
	
	public Session getNextSession() throws Exception {

		try {
			sesLockGet();
			
			File[] tmp = new File(getSessFolder()).listFiles();
			List<File> sfs = new ArrayList<File>();
			for (File f: tmp) if (f.getName().matches(".*-"+Shell.uname)) sfs.add(f);
			sortFiles(sfs);
			
			if (sfs.size() > 0) {
//				Log.log("Found %d sessions in user folder", sfs.size());
				
				String arch = getSessFolder()+"/arch";
				new File(arch).mkdirs();
				for (int i = 0; i < sfs.size()-1; i++)
					move2Arch(sfs.get(i), "there are older sessions for this user");
				
				File usf = sfs.get(sfs.size()-1);
				Session s = file2Ses(usf);
				
				if (s.timeFinishSched < Misc.getTime()) {
					move2Arch(usf, "depricated");
				} else {
					Log.log("Found session '%s'", usf.getName());
					return s;
				}
			}
			
			File[] fs = new File(getSessFolder()).listFiles();
			if (fs.length == 0) {
				Log.log("WARNING! No Sessions found in '%s' folder", getSessFolder());
				return null;
			}
			sortFiles(fs);
			
			for (File f: fs) {
				if (f.getName().indexOf("_cfg") == 0 || f.getName().equals("arch")) continue;
				if (f.getName().indexOf("-") != -1) continue; // Some user has already took this session
				Session s = file2Ses(f);
				if (s.timeStartSched <= Misc.getTime() && s.timeFinishSched > Misc.getTime()) {
					String newName = f.getAbsolutePath()+"-"+Shell.uname;
					rename(f.getAbsolutePath(), newName);
					Log.log("Found new session '%s' in sessions queue", f.getName());
					return file2Ses(new File(newName));
				}
				if (s.timeFinishSched < Misc.getTime())
					move2Arch(f, "wasn't played by any acc and depricated");
			}
			
			return null;

		} finally {
			sesLockRelease();
//			Log.log("Lock is released");
		}
	}
	private static long timeGraphSent;
	private static long graphCrc;
	
	private static void sendGraph() {
		
		try {
			String surl = Shell.cfg.getProperty("admin.url");
			if (surl.isEmpty()) return;
//			Log.log("surl='%s'", surl);
			
			// Renew graph
			List<Session> ss = new ArrayList<Session>();
			File[] fs = new File(getSessFolder()).listFiles();
			if (fs == null) {
				Log.log("WARNING! Sessions are not found in sessions folder '%s'", getSessFolder());
			} else {
				for (File f: fs) {
					if (f.getName().indexOf("_cfg") == 0 || f.getName().equals("arch")) continue;
					ss.add(file2Ses(f));
				}
			}
			String graph = ses2graph(ss);
			long crc = Misc.str2crc(graph);
			FUtil.fPut("ses.graph", graph);

			if (graphCrc == crc && Misc.getTime() - timeGraphSent < 3600*1000) return;
			graphCrc = crc;
			
			Log.log("Sending %s file", "ses.graph");
			
			if (surl.isEmpty()) return;

			String charset = "UTF-8";
	        String requestURL = surl+"/public_graph.php";
	 
	        MultipartUtility multipart = new MultipartUtility(requestURL, charset);
	        multipart.addHeaderField("User-Agent", "CodeJava");
	        multipart.addHeaderField("Test-Header", "Header-Value");
	        multipart.addFormField("method", "post");
	        multipart.addFormField("ip", Shell.host);
	        multipart.addFilePart("graph", new File("ses.graph"));
	 
	        List<String> resp = multipart.finish();
	        
	        if (resp.size() != 1 || !resp.get(0).equals("OK")) {
	        	String s = ""; for (String s1: resp) s += s1+"\n";
	        	Log.log("ERROR! Response from public_graph.php: %s", s);
	        }
	        
	        timeGraphSent = Misc.getTime();
	        
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		Shell.uname = "Brood";
//		tmp(); System.exit(0);
		
		Room r = new RoomGg();
		r.createSessionsIfNeeded();
//		Log.log("\n%s", sched2Str(sess));
		
		for (int i = 0; i < 10; i++) {
//			Log.log("Press ENTER..."); System.in.read(); System.in.read();
//			Log.log("Got ses: %s", r.getNextSession());
		}
	}
}
