package com.poker.brazillive.shell.shell;

import java.util.*;
import java.util.regex.*;

import com.poker.brazillive.shell.util.*;

public class Checker {
	
	private static void checkNoButtonsTables() throws Exception {
		
		long noBtnsTimeout = Misc.getPropInt(Shell.cfg, "close.table.if.no.buttons.during.sec");
		long neverBtnsTimeout = Misc.getPropInt(Shell.cfg, "close.table.if.never.showed.buttons.during.sec");

		synchronized (Shell.tables) {
			for (Table table: Shell.tables.values()) {
				long waitTime = noBtnsTimeout*1000;
				if (table.timeButtonsShowed != -1 && Misc.getTime() - table.timeButtonsShowed > waitTime)
					table.scheduleToCloseNow("Table doesn't shows buttons");
				if (table.timeButtonsShowed == -1 && Misc.getTime() - table.timeBorn > neverBtnsTimeout*1000)
					table.scheduleToCloseNow("Table doesn't shows buttons");
				if (table.timeLastEvents != -1 && Misc.getTime() - table.timeLastEvents > waitTime)
					table.scheduleToCloseNow("Table doesn't send events");
				
				if (table.timeAnswerObtained != -1 && Misc.getTime() - table.timeAnswerObtained > waitTime)
					table.scheduleToCloseNow("Table didn't obtain answer >"+waitTime/1000+" secs");
				if (table.timeAnswerObtained == -1
						&& table.timeFirstButtonsShowed != -1
						&& Misc.getTime() - table.timeFirstButtonsShowed > waitTime)
					table.scheduleToCloseNow("Table didn't obtain any answer >"+waitTime/1000+" secs");
			}
		}
	}

	private static void checkNoButtons() throws Exception {
		int noBtnsAtAllTimeout = Misc.getPropInt(Shell.cfg, "restart.client.if.no.buttons.during.sec");

		long t = Misc.getTime();
		if (t-Shell.timeLastButtons > noBtnsAtAllTimeout*1000) {
			Log.log("WARNING! No buttons during %d secs. Restarting client", noBtnsAtAllTimeout);
			Shell.playSoftRestart(String.format("no buttons at all during %d secs", noBtnsAtAllTimeout));
			return;
		}
		if (Shell.timeLastServerAnswer != 0 && t-Shell.timeLastServerAnswer > noBtnsAtAllTimeout*1000) {
			Log.log("WARNING! No server answers during %d secs. Restarting client", noBtnsAtAllTimeout);
			Shell.playSoftRestart(String.format("no answers at all during %d secs", noBtnsAtAllTimeout));
			return;
		}
	}
	
	private static void checkTableLookBuild() throws Exception {
		int shellTimeoutSec = 5*60;
		int tableTimeoutSec = 40;
		if (Shell.timeLastTableLookBuildSucc != -1 && Shell.tables.size() > 0
				&& Misc.getTime() - Shell.timeLastTableLookBuildSucc > shellTimeoutSec*1000) {
			Log.log("ERROR! No table with successful look build for the past %d secs over all the tables. Restarting client", shellTimeoutSec);
			Shell.playSoftRestart(String.format("no successful look build during %d secs", shellTimeoutSec));
			return;
		}
		
		synchronized (Shell.tables) {
			for (Table table: Shell.tables.values()) {
				if (table.timeLastTableLookBuildSucc != -1
						&& Misc.getTime() - table.timeLastTableLookBuildSucc > tableTimeoutSec*1000) {
					table.log.lo("Time: %d, last table look build: %d", Misc.getTime(), table.timeLastTableLookBuildSucc);
					table.scheduleToCloseNow("ERROR! No successful table look build for the past "+tableTimeoutSec+" secs");
				}
			}
		}
	}

	private static void checkGameBuild() throws Exception {
		int shellTimeoutSec = 5*60;
		int tableTimeoutSec = 3*60;
		if (Shell.timeLastGameBuild != -1 && Shell.tables.size() > 0
				&& Misc.getTime() - Shell.timeLastGameBuildSucc > shellTimeoutSec*1000) {
			Log.log("ERROR! No successful game build for the past %d secs over all the tables. Restarting client", shellTimeoutSec);
			Shell.playSoftRestart(String.format("no successful game build at all during %d secs", shellTimeoutSec));
			return;
		}
		
		synchronized (Shell.tables) {
			for (Table table: Shell.tables.values()) {
				if (table.timeLastGameBuild != -1
						&& Misc.getTime() - table.timeLastGameBuildSucc > tableTimeoutSec*1000) {
					table.scheduleToCloseNow("ERROR! No successful game build for the past "+tableTimeoutSec+" secs");
				}
			}
		}
	}

	private static Set<Long> unsuccJoins = new HashSet<Long>();
	private static void checkLobbyOpensWindows() throws Exception {
		int period = 5*60*1000;
		int count = 5;
		
		if (Shell.timeLastJoinClick == -1) return;
		
		if (Misc.getTime() - Shell.timeLastJoinClick > 10*1000
				&& Shell.timeNewTableOpen < Shell.timeLastJoinClick
				&& !unsuccJoins.contains(Shell.timeLastJoinClick)) {
			unsuccJoins.add(Shell.timeLastJoinClick);
			Log.log("WARNING! Unsuccessful join is detected. count=%d", unsuccJoins.size());
		}
		Set<Long> ujs = new HashSet<Long>(unsuccJoins);
		for (long uj: ujs) if (Misc.getTime()-uj > period) {
			Log.log("Old unsuccessful join was removed. count=%d", unsuccJoins.size());
			unsuccJoins.remove(uj);
		}
		
		if (unsuccJoins.size() > count) {
			Log.log("ERROR! More than %d unsuccessful table joins during past %d secs. Restarting the client", count, period/1000);
			unsuccJoins.clear();
			Shell.playSoftRestart(String.format("many unsuccessful table joins in lobby"));
		}
	}
	private static void checkLobbyProc() throws Exception {
		if (Shell.lobby == null) return;
		List<Long> ets = Shell.lobby.excTimes; // exception times
		while (ets.size() > 0 && Misc.getTime() - ets.get(0) > 60*1000)
			ets.remove(0);
		
		if (ets.size() > 3) {
			Log.log("ERROR! Exceptions in lobby procesing. Restarting. %s", ets);
			Shell.playSoftRestart(String.format("exceptions in lobby processing"));
		}
	}
	
	private static long timeHeroChecked;
	private static Pattern pat = Pattern.compile("^Dealt to (.*) \\[.*\\]\\s*$");
	private static void checkHeroNick() throws Exception {
		if (Shell.curSes.hero.nick == null) return;
		if (Misc.getTime() - timeHeroChecked < 10*60*1000) return;
		
		List<Table> ts = null;
		synchronized (Shell.tables) {
			ts = new ArrayList<Table>(Shell.tables.values());
		}
		for (Table t: ts) {
			if (t.hhStrs == null) continue;
			List<String> hhs = null;
			synchronized (t.hhStrs) { hhs = new ArrayList<String>(t.hhStrs); }
			for (String s: hhs) {
				// Dealt to blackmirror0 [As 9s]
				if (s.indexOf("Dealt to ") != 0) continue;
				Matcher m = pat.matcher(s);
				m.find();
				String hhHero = m.group(1);
				if (hhHero.equals(Shell.curSes.hero.nick)) {
//					Log.log("Hero check is passed: %s", hhHero);
					continue;
				}
				Log.log("ERROR! Playing hero '%s' doesn't match hh hero '%s'. Exit shell", Shell.curSes.hero.nick, hhHero);
				System.exit(0);
			}
		}
		timeHeroChecked = Misc.getTime();
	}

	public static void checkAll() throws Exception {
		checkNoButtonsTables();
		checkNoButtons();
		checkTableLookBuild();
		checkGameBuild();
		checkLobbyOpensWindows();
		checkLobbyProc();
		checkHeroNick();
	}
	
	public static void main(String[] args) throws Exception {
		Pattern pat = Pattern.compile("^Dealt to (.*) \\[.*\\]\\s*$");		
		Matcher m = pat.matcher("Dealt to blackmirror0 [As 9s]");
		m.find();
		Log.log("Hero: %s", m.group(1));
	}
}
