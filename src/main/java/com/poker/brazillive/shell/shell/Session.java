package com.poker.brazillive.shell.shell;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import com.poker.brazillive.shell.util.*;

public class Session implements Serializable {
	private static final long serialVersionUID = 1;
	
	public File fromFile;

	public String host;
	public String winAcc;
	public String shellVer;
	
	public String network;
	public String skin;
	public String proxy;
	public String rsi;
	public int sitFrom;
	public int sitTo;

	public long timeBase;
	public long timeStartSched;
	public long timeFinishSched;
	public long timeStart;
	public long timeFinish;
	public long timeLastHand;
	public Shell.Hero hero;
	public int[] limits;
	public int maxTables;
	
	private Count countHands;
	public int countTable;
	public int countTablePlaying;
	private List<Long> handTimes;
	private Integer bankroll;
	private Integer bankrollStart;
	public Double avgBb;
	public Count countErr = new Count();
	
	public Session(File fromFile, long timeBase, long timeStart, long timeFinish, Shell.Hero hero,
			int[] limits, int maxTables, String network, String skin, String proxy,
			int sitFrom, int sitTo, String rsi) throws Exception {
		
		this.fromFile = fromFile;
		
		this.timeBase = timeBase;
		this.timeStartSched = timeStart;
		this.timeFinishSched = timeFinish;
		this.hero = hero;
		
		this.network = network;
		this.skin = skin;
		this.proxy = proxy;
		
		this.limits = limits;
		Arrays.sort(this.limits);
		for (int i = 0; i < this.limits.length/2; i++) {
			int t = this.limits[i];
			this.limits[i] = this.limits[this.limits.length-1-i];
			this.limits[this.limits.length-1-i] = t;
		}
		
		this.maxTables = maxTables;
		this.host = Shell.host;

//		this.host = InetAddress.getLocalHost().getHostAddress();
		this.winAcc = Shell.uname;
		this.countHands = new Count();
		this.countTable = 0;
		this.handTimes = new ArrayList<Long>();
		
		this.shellVer = Shell.version;
		this.sitFrom = sitFrom;
		this.sitTo = sitTo;
		this.rsi = rsi;
	}

	public long getDurSched() {
		return this.timeFinishSched - this.timeStartSched;
	}
	
	synchronized public void addHand() {
		this.handTimes.add(Misc.getTime());
		while (Misc.getTime() - this.handTimes.get(0) > 60*60*1000) this.handTimes.remove(0);
		this.timeLastHand = Misc.getTime();
		this.countHands.inc();
	}
	synchronized public int getLastHandCount(int period) {
		for (int i = 0; i < this.handTimes.size(); i++)
			if (Misc.getTime() - this.handTimes.get(i) <= period)
				return this.handTimes.size() - i;
		return 0;
	}
	public void setBr(int br) {
		this.bankroll = br;
		if (this.bankrollStart == null) this.bankrollStart = br;
	}
	
	public static String df(long t) {
		if (t == 0) return "null";
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        return df.format(t);
	}
	public static String nf(Double n) {
		if (n == null) return "null";
		return n.toString();
	}
	public static String nf(Integer n) {
		if (n == null) return "null";
		return n.toString();
	}
	
	public String getUrlParsAndFlushCounts() throws Exception {
		
		Map<String, String> pars = new HashMap<String, String>();
		
		pars.put("host", this.host);
		pars.put("winAcc", this.winAcc);
		pars.put("nick", this.hero.nick);
		pars.put("shellVer", this.shellVer);

		pars.put("network", this.network);
		pars.put("skin", this.skin);
		pars.put("proxy", this.proxy);
		
		pars.put("timeBase", df(this.timeBase));
		
		pars.put("timeStartSched", df(this.timeStartSched));
		pars.put("timeFinishSched", df(this.timeFinishSched));
		
		pars.put("timeStart", df(this.timeStart));
		pars.put("timeFinish", df(this.timeFinish));

		pars.put("timeLastHand", df(this.timeLastHand));

		synchronized (this) {
			pars.put("countHand", ""+this.countHands.getVal());
			pars.put("countErr", ""+this.countErr.getVal());
			
			this.countHands = new Count();
			this.countErr = new Count();
		}
		
		pars.put("countTable", ""+this.countTable);
		pars.put("countTablePlaying", ""+this.countTablePlaying);
		pars.put("countLast5minHand", ""+this.getLastHandCount(5*60*1000));
		pars.put("bankroll", nf(this.bankroll));
		pars.put("bankrollStart", nf(this.bankrollStart));
		pars.put("avgBb", nf(this.avgBb));
		
		pars.put("limits", Arrays.toString(this.limits).replaceAll(", ", " ").replaceAll("[\\[\\]]", ""));
		
		String ret = "";
		for (String k: pars.keySet()) ret += "&"+k+"="+URLEncoder.encode(pars.get(k), "UTF-8");
		return ret;
	}
    
	public String toString() {
		return String.format("%-15s %s %s", this.hero.login,
			Misc.dateFormat(this.timeStartSched, "MMM dd HH:mm:ss"),
			String.format("%02d:%02d", this.getDurSched()/1000/60/60, this.getDurSched()/1000/60%60),
			Arrays.toString(this.limits)
		);
	}
	
	public static void main(String[] args) throws Exception {
		Log.log("%s", Arrays.toString(TimeZone.getAvailableIDs()).replaceAll(", ", "\n"));
	}
}
