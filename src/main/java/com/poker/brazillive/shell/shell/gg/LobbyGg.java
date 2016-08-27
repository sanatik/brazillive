package com.poker.brazillive.shell.shell.gg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import com.poker.brazillive.shell.shell.Action;
import com.poker.brazillive.shell.shell.Lobby;
import com.poker.brazillive.shell.shell.Shell;
import com.poker.brazillive.shell.shell.WndData;
import com.poker.brazillive.shell.util.Log;
import com.poker.brazillive.shell.util.Misc;
import com.poker.brazillive.shell.util.Rect;

public class LobbyGg extends Lobby {

	private long timeProc;
	private RecsGgLobby recs;
	private boolean clickedJoin = false;
	
	private String scrDir = "down";
	private int scrTopCount = 0;
	private int scrBottomCount = 0;
	
	public LobbyGg() throws Exception {
		recs = new RecsGgLobby();
		recs.load();
	}
	
	public void procLook(List<WndData> look) throws Exception {
		
		if (Misc.getTime() - this.timeProc < 5000) return;
		this.timeProc = Misc.getTime();
		
		for (WndData wd: look) {
			if (wd.broken) continue;
			if (wd.title.indexOf("AsyaPoker") != -1) {
				this.procLobby(wd);
			}
			//if (Shell.room.isTable(wd)) continue;
			
			if (wd.title.contains("Alert") || wd.title.contains("Error")) {
				Shell.threadSaveImg.addImg(wd, "dialog to be closed");
				Action.wndClose(wd.hwnd);
				Log.log("Closed window title='%s', img=%s", wd.title, wd.getImgFileName());
				Thread.sleep(100);
			}
		}
	}

	private static boolean test = false;
	
	private void procLobby(WndData wd) throws Exception {
		try {
			BufferedImage img = wd.getImg();
			
			if (!test) {
				Shell.threadSaveImg.addImg(wd, "lobby img");
				log.lo("Proc lobby img: %s", wd.getImgFileName());
				log.lo("Opened tables %d/%d", Shell.tables.size(), Shell.curSes.maxTables);
				Log.log("Opened tables %d/%d", Shell.tables.size(), Shell.curSes.maxTables);
			}

			if (img.getWidth() < 200 && img.getHeight() < 100) {
				log.lo("WARNING! Looks like lobby window is minimized. Title: %s", wd.title);
				return;
			}
			int tw = 1125, th = 888;
			if (img.getWidth() < tw && img.getHeight() < th) {
				log.lo("WARNING! Incorrect lobby size. Expected: %dx%d, actual: %dx%d", tw, th, img.getWidth(), img.getHeight());
				return;
			}
			if (!test && !Shell.sesPlaying) {
				log.lo("Skip img because Shell.sesPlaying=%s", Shell.sesPlaying);
				return;
			}
			if (recs.boolMes.rec(img)[0]) {
				log.lo("Lobby message is detected. Skip img");
				return;
			}
			
			String[] brs = recs.txtBal.rec(img);
			int br = Integer.parseInt(brs[0].replaceAll("[^\\d]", ""));
			Shell.curSes.setBr(br);
			log.lo("br=%1.2f", br/100.0);

			if (!test && Shell.tables.size() >= Shell.curSes.maxTables) {
				log.lo("Enough tables");
				return;
			}
			
			int waitSetup = 20;
			long maxt = Math.max(Shell.timeLastJoinClick, Shell.timeNewTableOpen);
			if (Misc.getTime()-maxt < waitSetup*1000) {
				log.lo("Waiting for new table to set up (left %d secs)", waitSetup-(Misc.getTime()-maxt)/1000);
				return;
			}
			
			if (!this.clickedJoin) {
				Rect[] rects = this.recs.getRectsCommon(img);
				String[] slims = this.recs.txtLims.rec(img);
				boolean[] pls = this.recs.boolPls.rec(img);

				assert rects.length == slims.length;
				assert pls.length%6 == 0;
				assert rects.length == pls.length/6;
				
				class Row {
					int lim;
					int pc;
					int ptc;
					int y;
					
					public Row(int lim, int pc, int ptc, int y) {
						this.lim = lim;
						this.pc = pc;
						this.ptc = ptc;
						this.y = y;
					}
					public String toString() {
						return String.format("nl%d%s %d/%d y=%d", this.lim, this.lim<9?"  ":(this.lim<99?" ":""), this.pc, this.ptc, this.y);
					}
				}
		
				Row[] rows = new Row[rects.length];
				for (int i = 0; i < rects.length; i++) {
					int lim = (int)Math.round(Double.parseDouble(slims[i].split("/\\$")[1])*100);
					int pc = 0;
					for (int j = 0; j < 6; j++) if (pls[i*6+j]) pc++;
					int ptc = 6;
					rows[i] = new Row(lim, pc, ptc, rects[i].y1);
				}
				
//				String str = "";
//				for (int i = 0; i < count; i++) str += String.format("%s\n", rows[i]);
//				log.lo("\n%s", str);
		
				List<Integer> freePlaceRows = new ArrayList<Integer>();
				List<Integer> otherRows = new ArrayList<Integer>();
				
//				log.lo("Session lims: %s", Arrays.toString(Shell.curSes.limits));
				Set<Integer> lims = new HashSet<Integer>();
				for (int lim: Shell.curSes.limits) lims.add(lim);
				
//				String[] sr = Shell.cfg.getProperty("players.amount.required.to.sit").split("-");
//				int pcFrom = Integer.parseInt(sr[0]);
//				int pcTo = Integer.parseInt(sr[1]);

				int pcFrom = Shell.curSes.sitFrom;
				int pcTo = Shell.curSes.sitTo;
				
				String s = "";
				for (int i = 0; i < rects.length; i++) {
					if (rows[i] == null) s += "null row";
					else s += rows[i];
					
					if (rows[i] == null) {
						s += " SKIPPED because its' null\n";
						continue;
					}
					if (rows[i].ptc != 6) {
						s += " SKIPPED because total players != 6\n";
						continue;
					}
					if (rows[i].pc < pcFrom) {
						s += String.format(" SKIPPED because pc < pcFrom (%d < %d)\n", rows[i].pc, pcFrom);
						continue;
					}
					if (rows[i].pc > pcTo) {
						s += String.format(" SKIPPED because pc > pcTo (%d > %d)\n", rows[i].pc, pcTo);
						continue;
					}
					if (!lims.contains(rows[i].lim)) {
						s += String.format(" SKIPPED because limit is not appropriate. lim=%d, allowed=%s\n", rows[i].lim, lims.toString());
						continue;
					}
					
					s += " can be clicked\n";
					if (rows[i].pc < rows[i].ptc) freePlaceRows.add(i);
					else otherRows.add(i);
				}
				log.lo("\n%s", s);
				log.lo("Found free place rows: %s", freePlaceRows);
				log.lo("Found other rows: %s", otherRows);
				
				List<Integer> rowsToClick = freePlaceRows;
				if (rowsToClick.size() == 0) rowsToClick = otherRows;
				
				if (rowsToClick.size() > 0) {
					int i = rowsToClick.get(Misc.rand.nextInt(rowsToClick.size()));
					int x = 650, y = rows[i].y;
					Action.wndActivate(wd.title);
					Action.clickNoAct(x, y, 1, 3);
					this.clickedJoin = true;
					log.lo("Clicked join row %d: (%d,%d)", i, x, y);
					
					return;
				}
			}
			this.clickedJoin = false;
			
			boolean scrCanUp = recs.boolScrCanUp.rec(img)[0];
			boolean scrCanDown = recs.boolScrCanDown.rec(img)[0];
			
			if (true) {
				if (!scrCanUp) {
					this.scrTopCount++;
					this.scrDir = "down";
					if (this.scrTopCount < 2) return;
				}
				if (!scrCanDown) {
					this.scrBottomCount++;
					this.scrDir = "up";
					if (this.scrBottomCount < 2) return;
				}
				if (this.scrDir.equals("down")) {
					log.lo("Scroll down");
					Action.clickHold(wd.hwnd, 702, 840, 2000, 0);
				}
				if (this.scrDir.equals("up")) {
					log.lo("Scroll up");
					Action.clickHold(wd.hwnd, 702, 212, 2000, 0);
				}
				
				this.scrTopCount = 0;
				this.scrBottomCount = 0;
			}
		} catch (Exception e) {
			log.lo("Error during processing of img %s:\n%s", wd.getImgFileName(), Misc.stacktrace2Str(e));
			Log.log("Error in lobby processing");
			this.excTimes.add(Misc.getTime());
		}
	}

	public static void main(String[] args) throws Exception {
		String f = "imgs_gg/lobby/scr_top.png";
		BufferedImage img = ImageIO.read(new File(f));
		WndData wd = new WndData();
		wd.img = img;
		Shell.cfg.load(new FileInputStream(new File("shell.cfg")));
		Shell.uname = "Brood";
		
		test = true;
		LobbyGg l = new LobbyGg();
		l.procLobby(wd);
	}
}
