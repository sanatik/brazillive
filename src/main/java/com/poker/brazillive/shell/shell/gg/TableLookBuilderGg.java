package com.poker.brazillive.shell.shell.gg;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import com.poker.brazillive.shell.binimg.BinImg;
import com.poker.brazillive.shell.binimg.ImgServer;
import com.poker.brazillive.shell.binimg.RecBinImgMulti;
import com.poker.brazillive.shell.game.*;
import com.poker.brazillive.shell.rec.*;
import com.poker.brazillive.shell.shell.*;
import com.poker.brazillive.shell.util.*;

public class TableLookBuilderGg extends TableLookBuilder {

	private static final int HERO_LOC = 0;

	private RecsGgTable recs = new RecsGgTable();
	private String heroName;
	private static RecBinImgMulti recNames;
	
	public TableLookBuilderGg(Log log, String heroName, List<String> hhStrs) throws Exception {
		super(log);
		recs.load();
		this.heroName = heroName;
		this.hhStrs = hhStrs;
	}
	
	private RecBinImgMulti getRecNames() throws Exception {
		synchronized (this.getClass()) {
			if (recNames != null) return recNames;
			
			recNames = new RecBinImgMulti(Shell.uname+"/RecBinImg_gg_names",
						new Rect[]{
	//						new Rect(0,435,693,122,19),	
							new Rect(0,0,0,0,0), // Zero rect - not to send to img server
							new Rect(0, 36,526,122,19),	
							new Rect(0, 61,246,122,19),	
							new Rect(0,441,166,122,19),	
							new Rect(0,821,246,122,19),	
							new Rect(0,846,526,122,19),	
						},
						2, 2,
						new Color(173,179,179), 200,
						new RecBool[]{recs.boolName},
						new RecBool[]{},
						Shell.cfg.getProperty("captcha.server").split(":")[0],
						Integer.parseInt(Shell.cfg.getProperty("captcha.server").split(":")[1]),
						0.97
					);
			return recNames;
		}
	}

	public TableLook buildTableLook(WndData wndData, TableLook prevTableLook) throws Exception {
		BufferedImage img = wndData.getImg();
		
		TableLook ret = new TableLook();

		if (img.getWidth() < 200 && img.getHeight() < 100)
			throw new Exception(String.format("Small image size: %dx%d. Most likely window is minimized", img.getWidth(), img.getHeight()));
		if (img.getWidth() != 1000 || img.getHeight() != 750)
			throw new Exception(String.format("Incorrect table image size: %dx%d", img.getWidth(), img.getHeight()));
		
		ret.time = wndData.time;
		ret.isActive = wndData.active;
		ret.title = wndData.title;
		ret.currency = "USD"; if (ret.title.indexOf(" NE ") != -1) ret.currency = "PLAYMONEY";
		ret.heroName = heroName;
		
		// Toyota 05 - $0.05 / $0.10
		String[] sr = wndData.title.split(" - ")[1].replaceAll("[^\\d./]", "").split("/");
		ret.sb = (int)Math.round(Double.parseDouble(sr[0])*100);
		ret.bb = (int)Math.round(Double.parseDouble(sr[1])*100);
		
		ret.gameId = null;
		ret.tableMax = 6;
		
		if (recs.boolInsur.rec(img)[0]) ret.skipReason = "All-in insurance is detected";
		if (recs.boolServerMes.rec(img)[0]) ret.skipReason = "Server message is detected";
		
		String[] st = recs.txtStack.rec(img);
		String[] sth = recs.txtStackHero.rec(img);
		ret.stacks = new Integer[ret.tableMax];
		for (int i = 0; i < ret.tableMax; i++) {
			
			String s;
			if (i == 0) s = sth[0];
			else s = st[i-1];
			
			if (s.indexOf("A") != -1) {
				ret.stacks[i] = 0;
			} else {
				String sn = s.replaceAll("[^\\d.]", "");
				if (sn.equals("")) continue;
				ret.stacks[i] = (int)Math.round((Double.parseDouble(sn)*100));
			}
		}

		String[] bt = recs.txtBet.rec(img);
		ret.bets = new Integer[ret.tableMax];
		for (int i = 0; i < ret.tableMax; i++) if (!bt[i].equals("")) {
			String b = bt[i].replaceAll("[^\\d.]", "");
//			String b = bt[i].replaceAll("[^\\d]", "");
			if (b.equals("")) continue;
//			ret.bets[i] = (int)Math.round((Double.parseDouble(b)*100));
			try {
				ret.bets[i] = (int)Math.round(Double.parseDouble(b)*100);
			} catch (NumberFormatException e) {
				if (ret.skipReason == null) throw e; // All-in insurance or server message can be detected above
			}
		}

		
		// ********************** Cards ****************************
		ret.holes = new Card[ret.tableMax][];
		String[] css = recs.txtCard.rec(img);

		int bl = -1;
		for (int i = 0; i < 5; i++) if (css[i*2].equals("")) { bl = i; break; }
		if (bl == -1) bl = 5; if (bl < 3) bl = 0;
		
		try {
			ret.board = new Card[bl];
			for (int i = 0; i < bl; i++) ret.board[i] = Card.str2Cards(css[i*2]+css[i*2+1])[0];
		} catch (IllegalArgumentException e) {
			ret.skipReason = "Cannot recognize cards: "+Arrays.toString(css);
			ret.board = new Card[0];
		}

			Color c = new Color(img.getRGB(418,600));
			int n = 0;
			if (c.getRed() != 255 || c.getGreen() != 255 || c.getBlue() != 255) {
				// Bug in client: in the first game hero holes have different position
				n = (5+2)*2;
			} else {
				n = (5+0)*2;
			}
		
			if (!css[n].equals("")) {
				try {
					ret.holes[HERO_LOC] = Card.str2Cards(css[n]+css[n+1]+css[n+2]+css[n+3]);
				} catch (IllegalArgumentException e) {
					ret.skipReason = "Cannot recognize cards: "+Arrays.toString(css);
					ret.holes[HERO_LOC] = null;
				}
			}
			
			css = recs.txtCardShowed.rec(img);
			for (int i = 0; i < 5; i++) {
				if (css[i*4].equals("")) continue;
				try {
					ret.holes[1+i] = Card.str2Cards(css[i*4]+css[i*4+1]+css[i*4+2]+css[i*4+3]);
				} catch (IllegalArgumentException e) {
					ret.skipReason = "Cannot recognize cards: "+Arrays.toString(css);
					ret.holes[1+i] = null;
				}
			}
		
		// **********************************************************
		
		ret.isSeatOccupied = recs.boolSeatOcc.rec(img);
		ret.isButtons = recs.boolButtons.rec(img)[0];
		
		Map<Integer, String> s2p = new HashMap<Integer, String>();
		ret.heroNameDetected = this.getNicksFromHh(
				"^Seat (\\d+): (.*?) \\(.* in chips\\).*", "^Dealt to (.*?) \\[(.*?)\\].*", s2p);
//		log.lo("s2p: %s", s2p);
		
		int heroSeat = -1;
		for (int seat: s2p.keySet()) if (s2p.get(seat).equals(this.heroName)) heroSeat = seat;

		// ********************** Names *****************************
		boolean[] blueMoves = recs.boolBlueMove.rec(img);
		boolean isBlueMove = false; for (boolean b: blueMoves) if (b) isBlueMove = true;
		
		if (ret.board.length == 0 && ret.isButtons && isBlueMove)
			ret.skipReason = "Blue move and first buttons";
			
		// Player names - initial values
		boolean[] isName = recs.boolName.rec(img);
		if (prevTableLook != null) {
			ret.pnames = prevTableLook.pnames;
			for (int i = 0; i < ret.pnames.length; i++) {
				if (!ret.isSeatOccupied[i]) ret.pnames[i] = null; // The case when player left table
				if (ret.isSeatOccupied[i] && ret.pnames[i] == null) ret.pnames[i] = "<"+i+">"; // The case when seat was empty in ptl
			}
		} else {
			ret.pnames = new String[ret.tableMax];
			for (int i = 0; i < ret.pnames.length; i++)
				if (isName[i]) ret.pnames[i] = "<"+i+">";
		}

		// Player names
		// Rec them only if tl is not skipped, otherwise animation can interfere
		// Rec them only once when 1st btns are displayed
		if (ret.board.length == 0 && ret.isButtons && !isBlueMove) {
//			String[] recNames = new String[TABLE_MAX];
//			String[] recNames = this.recNames.rec(img, Log.defl);
			String[] recNames = this.getRecNames().rec(img);
			ret.heroNameDetected = null;

			for (int i = 0; i < recNames.length; i++) {
				if (i == 0) ret.pnames[i] = this.heroName;
				else if (recNames[i] != null && recNames[i].indexOf(ImgServer.STATUS_PREFIX) != 0)
					ret.pnames[i] = recNames[i];
				else if (recNames[i] != null && recNames[i].indexOf(ImgServer.STATUS_PREFIX) == 0)
					ret.pnames[i] = "<"+i+">";
				else if (recNames[i] == null && isName[i])
					ret.pnames[i] = "<"+i+">";
				else if (recNames[i] == null) {/* do nothing, take from ptl */}
				else assert false;
			}
		}
		// **********************************************************

		boolean[] wins = recs.boolWin.rec(img);
		for (boolean w: wins) if (w) ret.isRiverMovesFinished = true;
		boolean[] combs = recs.boolComb.rec(img);

		ret.isInPlay = recs.boolInPlay.rec(img);
		if (ret.holes[HERO_LOC] != null) ret.isInPlay[HERO_LOC] = true;
		else ret.isInPlay[HERO_LOC] = false;
		for (int i = 0; i < ret.isInPlay.length; i++) {
			if (wins[i]) ret.isInPlay[i] = true;
			if (combs[i]) ret.isInPlay[i] = true;
		}
		
		
		ret.isHeroInPlay = ret.isInPlay[HERO_LOC];
		
		boolean[] dp = recs.boolDealer.rec(img);
		for (int i = 0; i < ret.tableMax; i++) if (dp[i]) ret.dealerPos = i;
		
		ret.isSeatout = new boolean[ret.tableMax];

		ret.isHeroSitout = recs.boolHeroSitout.rec(img)[0];
		ret.isSeatout = recs.boolSitout.rec(img);
		ret.isSeatout[HERO_LOC] = ret.isHeroSitout; 
		
		ret.heroPos = HERO_LOC;

		boolean[] emos = recs.boolEmo.rec(img);
		for (boolean emo: emos) if (emo) ret.skipReason = "Emo is detected";
		
		return ret;
	}
	
	public static void main(String[] args) throws Exception {
//		{
//			BufferedImage img = ImageIO.read(new File("fgfg/RecBinImg_names/000002/bin.png"));
//			BinImg bi = new BinImg(img, new Color(173,179,179), 80, 0.97);
//			bi.trim();
//			Log.log("\n%s", bi);
//			System.exit(0);
//		}
		
		
		BufferedImage img = ImageIO.read(new File("imgs_gg/btns/0.png"));

		Shell.cfg = new Properties();
		Shell.cfg.load(new FileInputStream("shell.cfg"));
		Shell.uname = "fgfg";
		TableLookBuilderGg tlb = new TableLookBuilderGg(Log.defl, "heroName", new ArrayList<String>());
		String[] ns = tlb.getRecNames().rec(img);
		Log.log("\n%s", Arrays.toString(ns).replaceAll(", ", "\n").replaceAll("[\\[\\]]", ""));
	}
}
