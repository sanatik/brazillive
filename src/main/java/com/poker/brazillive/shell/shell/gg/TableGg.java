package com.poker.brazillive.shell.shell.gg;

import com.poker.brazillive.shell.game.Game;
import com.poker.brazillive.shell.game.Player;
import com.poker.brazillive.shell.shell.*;
import com.poker.brazillive.shell.util.Misc;
import com.poker.brazillive.shell.util.Point;

public class TableGg extends Table {

	public static final int width = 1000;
	public static final int height = 750;
	
	public TableGg(int hwnd) throws Exception {
		super(hwnd);
		this.tableLookBuilder = new TableLookBuilderGg(this.log, Shell.curSes.hero.nick, this.hhStrs);

		this.targetWidth = width;
		this.targetHeight = height;
		this.resized = true;
	}

	private void ggClick(int x, int y, int rnd) throws Exception {
		Action.wndActivate(this.hwnd);
		Action.clickNoAct(x, y, 1, rnd);
	}
	
	public void actionClose() throws Exception {
		Action.wndClose(this.hwnd);
		Thread.sleep(100);
		Action.sendInp("{Tab}{Tab}{Enter}");
	}
	public void actionSitin() throws Exception {
		ggClick(900, 715, 15);
	}
	public void actionTakeEmptySeat() throws Exception {
		// Do nothing
	}
	
	public void actionJoinWaitList() throws Exception {
		ggClick(860, 705, 15);
	}

	public void actionClickSitout() throws Exception {
		ggClick(156, 715, 3);
	}

	public void doActions() throws Exception {
		clickSitinIfNeeded();
		clickWaitListBtnIfNeeded();
		posIfNeeded();
	}
	
	public void applyAnswerAction() throws Exception {
		// ANSWER <action> <amount> <delay> <quality>
		// Example: ANSWER C 1.00 1201 Normal
		String[] sr = answer.split(" +");
		
		String act = sr[1];
		double val = 0;
		if (! sr[2].equals("-")) val = Double.parseDouble(sr[2]);
		
		Game g = this.game;
		int mr = g.getMinRaise();
		Player p = g.getCurMovePlayer();
		boolean raiseMeans = false; // false: raise means nothing because all the players can just call
		
		for (Player cp: g.players) {
			if (cp == g.getCurMovePlayer()) continue;
			if (cp.putToPotInStage + cp.stack > g.raisedVal) raiseMeans = true;
		}
		
		if (act.equals("F")) {
			ggClick(670, 715, 15);
			
		} else if (act.equals("K") || act.equals("C")
				|| (act.equals("A") && g.getMinRaise() == -1
				|| !raiseMeans
				/*val <= this.recTable.game.getCall()/100.0*/)) {
			ggClick(800, 715, 15);
			
		} else if (act.equals("B") || act.equals("R")
				|| (act.equals("A") && g.getMinRaise() != -1
				/*val > this.recTable.game.getCall()/100.0*/)) {
			double rLevel = g.getCurMovePlayer().putToPotInStage/100.0 + val;
			String ahkExe = String.format("\"%s\" \"ahk/control.gg.ahk\" raise 0x%08X %1.2f",
					Action.AHK_PATH, this.hwnd, rLevel);
			Misc.sysCall(ahkExe);
		
		} else assert false;
	}

	public String getId() {
		// Toyota 02 - $0.05 / $0.10
		if (this.tableLook == null) return null;
		return this.tableLook.title.split(" NL ")[0];
	}
	public void resizeIfNeeded(int tw, int th) throws Exception {
		// Do nothing
	}
}
