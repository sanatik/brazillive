package com.poker.brazillive.shell.shell;

import java.util.Arrays;

import com.poker.brazillive.shell.game.*;
import com.poker.brazillive.shell.shell.*;
import com.poker.brazillive.shell.util.*;

public class TableLook {

	public long time;
	public String title;
	public boolean isActive;
	public String tableId;
	public String gameId;
	public Integer[] stacks;
	public Integer[] bets;
	public String[] pnames;
	public Card[] board;
	public Card[][] holes;
	public int dealerPos = -1;
	//public int lightPos = -1; // Rm this. Cannot always define (if blinking)
	public boolean isButtons;
	public boolean[] isInPlay;
	public boolean[] isSeatOccupied;
	public boolean[] isSeatAvailable;
	public boolean[] isSeatout;
	public boolean isHeroSitout = false;
	public boolean isHeroInPlay = false;
	public int sb = -1;
	public int bb = -1;
	public int tableMax = -1;
	public String skipReason = null;
	public boolean isRiverMovesFinished = false;
	public boolean isWaitBbPresentAndUnchecked = false;
	public String currency;
	public boolean isAutoBlindChecked = false;
	public int heroPos = -1;
	public String heroName;
	public boolean isWaitListBtn = false;
	public boolean isBuyinDialog = false;
	public boolean isBbGuest = false;
	public boolean isBbWait = false;
	public boolean isCbxPresent = false;
	public boolean isTourney = false;
	public boolean isCheckFree = false;
	public boolean isErr = false;
	public String hhFile;
	public String heroNameDetected;
	
	private static boolean cmpStr(String s1, String s2) {
		if (s1 == null && s2 == null) return true;
		if (s1 == null && s2 != null) return false;
		if (s1 != null && s2 == null) return false;
		return s1.equals(s2);
	}
	
	public boolean isSame(Object o) {
		if (o == null) return false;
		if (!(o instanceof TableLook)) return false;
		TableLook tl = (TableLook)o;

		// Most often changed
		if (!Arrays.equals(this.stacks, tl.stacks)) return false;
		if (!Arrays.equals(this.bets, tl.bets)) return false;
		if (!Arrays.equals(this.board, tl.board)) return false;
		if (!Arrays.equals(this.isInPlay, tl.isInPlay)) return false;

		if (!Arrays.equals(this.isSeatAvailable, tl.isSeatAvailable)) return false;
		if (!cmpStr(this.title, tl.title)) return false;
		if (!cmpStr(this.heroName, tl.heroName)) return false;
		if (!cmpStr(this.gameId, tl.gameId)) return false;
		if (!cmpStr(this.tableId, tl.tableId)) return false;
		if (!Arrays.equals(this.pnames, tl.pnames)) return false;
		if (!Arrays.deepEquals(this.holes, tl.holes)) return false;
		if (this.dealerPos != tl.dealerPos) return false;
		if (this.isButtons != tl.isButtons) return false;
		if (!Arrays.equals(this.isSeatOccupied, tl.isSeatOccupied)) return false;
		if (!Arrays.equals(this.isSeatout, tl.isSeatout)) return false;
		if (this.isHeroSitout != tl.isHeroSitout) return false;
		if (this.isHeroInPlay != tl.isHeroInPlay) return false;
		if (this.sb != tl.sb) return false;
		if (this.bb != tl.bb) return false;
		if (this.tableMax != tl.tableMax) return false;
		if (!cmpStr(this.skipReason, tl.skipReason)) return false;
		if (this.isRiverMovesFinished != tl.isRiverMovesFinished) return false;
		if (this.isWaitBbPresentAndUnchecked != tl.isWaitBbPresentAndUnchecked) return false;
		if (!cmpStr(this.currency, tl.currency)) return false;
		if (this.isAutoBlindChecked != tl.isAutoBlindChecked) return false;
		if (this.heroPos != tl.heroPos) return false;
		if (this.isWaitListBtn != tl.isWaitListBtn) return false;
		if (this.isBuyinDialog != tl.isBuyinDialog) return false;
		if (this.isBbGuest != tl.isBbGuest) return false;
		if (this.isBbWait != tl.isBbWait) return false;
		if (this.isCbxPresent != tl.isCbxPresent) return false;
		if (this.isTourney != tl.isTourney) return false;
		if (this.isCheckFree != tl.isCheckFree) return false;
		if (this.isErr != tl.isErr) return false;
		if (!cmpStr(this.hhFile, tl.hhFile)) return false;
		if (!cmpStr(this.heroNameDetected, tl.heroNameDetected)) return false;
		if (this.isActive != tl.isActive) return false;

		return true;
	}
	
	public String toString() {
		String ret = "";
		if (this.pnames == null) ret += "Empty table look\n";
		else {
			ret += String.format("Board: %s, gid=%s, blinds: %d/%d, title: %s, rmf: %s\n", Card.cards2Str(this.board), this.gameId, this.sb, this.bb, this.title, this.isRiverMovesFinished);
			for (int i = 0; i < this.pnames.length; i++) {
				ret += String.format("%1s%1s%1s %-15s %4s %4s %4s\n",
						this.isSeatOccupied[i]?"":"e",
						i==this.dealerPos?"D":"",
						this.isInPlay[i]?"i":"",
						this.pnames[i],
						this.holes[i] == null?"":Card.cards2Str(this.holes[i]),
						this.stacks[i] == null?"":String.format("%4d", this.stacks[i]),
						this.bets[i] == null?"":String.format("%4d", this.bets[i]));
			}
		}
		if (this.isButtons) ret += " BUTTONS";
		if (this.isHeroSitout) ret += " SITTING OUT";
		ret += "\n";
		if (this.skipReason != null) ret += " SKIP ME: "+this.skipReason+"\n";
//		ret += "HH file: "+this.hhFile;
		return ret;
	}
	public String toString(int tab) {
		String str = this.toString();
		String[] sr = str.split("[\r\n]+");
		String shift = ""; for (int i = 0; i < tab; i++) shift += "\t";
		String ret = "";
		for (String s: sr) ret += shift+s+"\n";
		return ret;
	}
}
