package com.poker.brazillive.shell.game;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.nio.*;

import com.poker.brazillive.shell.util.*;

public class Event implements Cloneable, Serializable {
	
	public static final int TYPE_UNKNOWN = 0;

	public static final int TYPE_HOLES = 1;
	
	public static final int TYPE_FLOP = 2;
	public static final int TYPE_TURN = 3;
	public static final int TYPE_RIVER = 4;

	public static final int TYPE_SB = 5;
	public static final int TYPE_BB = 6;
	public static final int TYPE_SBB = 7;
	public static final int TYPE_ANTE = 8;
	
	public static final int TYPE_MOVE_FIRST = 9;

	public static final int TYPE_FOLD = 9;
	public static final int TYPE_CALL = 10;
	public static final int TYPE_CALL_ALLIN = 11;
	public static final int TYPE_RAISE = 12;
	public static final int TYPE_RAISE_ALLIN = 13;
	public static final int TYPE_BET = 14;
	public static final int TYPE_BET_ALLIN = 15;
	public static final int TYPE_CHECK = 16;

	public static final int TYPE_MOVE_LAST = 16;

	public static final int TYPE_TIME_BANK = 17;
	public static final int TYPE_RETURN_UNCALLED = 18;
	public static final int TYPE_WIN_POT = 19;
	//public static final int TYPE_TIE_POT = 20;
	public static final int TYPE_YOUR_TURN = 21;
	public static final int TYPE_NEW_HAND = 22;
	public static final int TYPE_NEW_HAND_HH = 23;
	public static final int TYPE_BUTTON_HH = 24;
	public static final int TYPE_LEAVE = 25;
	public static final int TYPE_ALLOWED_AFTER_BUTTON = 26;
	public static final int TYPE_NOT_SHOW = 27;
	public static final int TYPE_LIMITS_GO_UP = 28;
	public static final int TYPE_JOIN = 29;
	public static final int TYPE_HAS_COMB = 30;
	public static final int TYPE_SHOW = 31;
	public static final int TYPE_SHOWDOWN = 32;
	public static final int TYPE_MUCK = 33;
	public static final int TYPE_FINISHED_TOURNEY = 34;
	public static final int TYPE_SIT_OUT = 35;
	public static final int TYPE_SITS_OUT = 36;
	public static final int TYPE_NOW_HEADS_UP = 37;
	public static final int TYPE_WIN_TOURNEY = 38;
	public static final int TYPE_DISCONNECT = 39;
	public static final int TYPE_CONNECT = 40;
	public static final int TYPE_SAY = 41;
	public static final int TYPE_GAME_WILL_START_IN = 42;
	public static final int TYPE_REQUESTED_TIME = 43;
	public static final int TYPE_MISC = 44;
	public static final int TYPE_BB_ALLIN = 45;
	public static final int TYPE_HAND_CANCELED = 46;

	// From IP
	public static final int TYPE_DATE = 47;
	public static final int TYPE_DEALER = 48;
	public static final int TYPE_SEAT = 49;
	public static final int TYPE_PREFLOP = 50;
	public static final int TYPE_END_GAME = 51;
	public static final int TYPE_ALL_IN = 52;
	public static final int TYPE_WIN_BONUS = 53;
	
	public static final int TYPE_FLOP1 = 54;
	public static final int TYPE_FLOP2 = 55;
	public static final int TYPE_TURN1 = 56;
	public static final int TYPE_TURN2 = 57;
	public static final int TYPE_RIVER1 = 58;
	public static final int TYPE_RIVER2 = 59;
	public static final int TYPE_SHOWDOWN1 = 60;
	public static final int TYPE_SHOWDOWN2 = 61;

	public static final int TYPE_WIN_SIDE_POT = 62;
	public static final int TYPE_GAME_FINISHED = 63;
	public static final int TYPE_DEAD_BLIND = 64;
	public static final int TYPE_STRADDLE = 65;
	
	public int type; // = Event.TYPE_UNKNOWN;
	public String who;
	public String sVal;
	public long iVal;
	public String fromString = null;
	public double weight = Double.NaN;

	public static final int FROM_CHAT = 0;
	public static final int FROM_IP = 1;
	
	private static HashMap<Integer, HashMap<String,String>> pt = new HashMap<Integer, HashMap<String,String>>();
	private static HashMap<Integer, HashMap<Pattern,String>> ptc = new HashMap<Integer, HashMap<Pattern,String>>();

	static {
		pt.put(FROM_CHAT, new LinkedHashMap<String,String>());
		pt.put(FROM_IP, new LinkedHashMap<String,String>());
		
		
		
	} // static init
	
	public Event() {
	}
	public Event(int type, String who) {
		this.type = type;
		this.who = who;
	}
	public Event(int type, long iVal) {
		this.type = type;
		this.iVal = iVal;
	}
	public Event(int type, String who, long iVal) {
		this.type = type;
		this.iVal = iVal;
		this.who = who;
	}
	/*public Event(int type, Player player, long iVal) {
		this.type = type;
		this.iVal = iVal;
		this.who = player.name;
		this.player = player;
	}*/
	public Event(double weight, int type, String who, long iVal) {
		this(type,who,iVal);
		this.weight = weight;
	}
	public Event(double weight, int type, String who) {
		this(type,who);
		this.weight = weight;
	}
	public Event(int type, String who, String sVal) {
		this.type = type;
		this.sVal = sVal;
		this.who = who;
	}
	
	public boolean isChangeRaiseValue() {
		int t = this.type;
		return t==TYPE_BB || t==TYPE_BB_ALLIN || t==TYPE_STRADDLE || t==TYPE_RAISE || t==TYPE_RAISE_ALLIN || t==TYPE_BET || t==TYPE_BET_ALLIN;
	}
	public static boolean isPutToPot(int t) {
		return t==TYPE_SBB || t==TYPE_SB || t==TYPE_BB || t==TYPE_BB_ALLIN || t==TYPE_ANTE || t==TYPE_STRADDLE || t==TYPE_RAISE || t==TYPE_RAISE_ALLIN || t==TYPE_CALL || t==TYPE_CALL_ALLIN || t==TYPE_BET || t==TYPE_BET_ALLIN;
	}
	public boolean isPutToPot() {
		return Event.isPutToPot(this.type);
	}
	public boolean isReturnFromPot() {
		int t = this.type;
		return t==TYPE_RETURN_UNCALLED || t==TYPE_WIN_POT || t==TYPE_WIN_SIDE_POT;
	}
	public boolean isNotPretendOnPot() {
		int t = this.type;
		return t==TYPE_FOLD || t==TYPE_SITS_OUT || t==TYPE_SIT_OUT || t==TYPE_JOIN || t==TYPE_ALLOWED_AFTER_BUTTON;
	}
	public boolean isNotToMoveNextRound() {
		int t = this.type;
		return this.isNotPretendOnPot() || this.isAllIn();
	}
	public boolean isNewStage() {
		int t = this.type;
		return t==TYPE_FLOP || t==TYPE_TURN || t==TYPE_RIVER || t==TYPE_NEW_HAND;
	}
	public boolean isBoard() {
		int t = this.type;
		return t==TYPE_FLOP || t==TYPE_TURN || t==TYPE_RIVER;
	}
	public boolean isLeaveTable() {
		int t = this.type;
		return t==TYPE_FINISHED_TOURNEY || t==TYPE_WIN_TOURNEY || t==TYPE_LEAVE;
	}
	/*public boolean isTurn() {
		//int t = this.type;
		//if (t==TYPE_CHECK || t==TYPE_CALL || t==TYPE_CALL_ALLIN || t==TYPE_BET || t==TYPE_BET_ALLIN || t==TYPE_RAISE || t==TYPE_RAISE_ALLIN || t==TYPE_FOLD) return true;
		if (this.isMove()) return true;
		if (this.isBlind()) return true;
//		if (this.type == TYPE_SB && ! table.sbPosted) return true;
//		if ((this.type == TYPE_BB || this.type == TYPE_BB_ALLIN) && ! table.bbPosted) return true;
		return false;
	}*/
	public static boolean isMove(int t) {
		if (t >= TYPE_MOVE_FIRST && t <= TYPE_MOVE_LAST) return true;
		//if (t==TYPE_FOLD || t==TYPE_CHECK || t==TYPE_CALL || t==TYPE_BET || t==TYPE_RAISE || t==TYPE_RAISE_ALLIN || t==TYPE_CALL_ALLIN || t==TYPE_BET_ALLIN) return true;
		return false;
	}
	public boolean isMove() {
		return Event.isMove(this.type);
	}
	public boolean isFold() {
		return this.type == Event.TYPE_FOLD;
	}
	public boolean isCheck() {
		return this.type == Event.TYPE_CHECK;
	}
	public boolean isCall() {
		return this.type == Event.TYPE_CALL || this.type == Event.TYPE_CALL_ALLIN;
	}
	public static boolean isBetOrRaise(int t) {
		return isBet(t) || isRaise(t);
	}
	public boolean isBetOrRaise() {
		return isBetOrRaise(this.type);
	}
	public static boolean isBet(int t) {
		return t==TYPE_BET || t==TYPE_BET_ALLIN;
	}
	public boolean isBet() {
		return isBet(this.type);
	}
	public static boolean isRaise(int t) {
		return t==TYPE_RAISE || t==TYPE_RAISE_ALLIN;
	}
	public boolean isRaise() {
		return isRaise(this.type);
	}
	public static boolean isCallOrCheck(int t) {
		return t == TYPE_CALL || t == TYPE_CALL_ALLIN || t == TYPE_CHECK;
	}
	public boolean isCallOrCheck() {
		return isCallOrCheck(this.type);
	}
	public boolean isAllIn() {
		int t = this.type;
		return t==TYPE_RAISE_ALLIN || t==TYPE_BET_ALLIN || t==TYPE_CALL_ALLIN || t==TYPE_BB_ALLIN;
	}
	public boolean isBlind() {
		int t = this.type;
		return t==TYPE_SB || t==TYPE_BB || t==TYPE_BB_ALLIN || t==TYPE_SBB;
	}
	public boolean is2Showdowns() {
		int t = this.type;
		return t==TYPE_FLOP1 || t==TYPE_FLOP2 || t==TYPE_TURN1 || t==TYPE_TURN2
			|| t==TYPE_RIVER1 || t==TYPE_RIVER2 || t==TYPE_SHOWDOWN1 || t==TYPE_SHOWDOWN2;

	}
	public boolean isAfterRiver() {
		int t = this.type;
		return t==TYPE_SHOWDOWN || t==TYPE_HAS_COMB || t==TYPE_SHOW || t==TYPE_MUCK || t==TYPE_WIN_SIDE_POT || t==TYPE_WIN_POT;
	}
	public boolean isWin() {
		int t = this.type;
		return t==TYPE_WIN_POT || t==TYPE_WIN_SIDE_POT /*|| t==TYPE_TIE_POT*/;
	}
	public boolean isCards() {
		int t = this.type;
		return t==TYPE_HOLES || this.isBoard() || t==TYPE_SHOW || t==TYPE_MUCK;
	}
	
	public String toStringNoWeight() {
		if (this.type == Event.TYPE_UNKNOWN) {
			String ret = "UNKNOWN EVENT";
			if (this.fromString != null) ret += ": "+this.fromString;
			return ret;
		}
		if (this.fromString != null && !this.fromString.contains("No value"))
			return this.fromString;
		String type = new Integer(this.type).toString();
		String ret = "";

		if (false) ;
		else if (this.type == Event.TYPE_HOLES) ret += "holes";

		else if (this.type == Event.TYPE_FLOP) ret += "flop";
		else if (this.type == Event.TYPE_TURN) ret += "turn";
		else if (this.type == Event.TYPE_RIVER) ret += "river";

		else if (this.type == Event.TYPE_SB) ret += "sb";
		else if (this.type == Event.TYPE_SBB) ret += "sbb";
		else if (this.type == Event.TYPE_BB || this.type == Event.TYPE_BB_ALLIN) ret += "bb";
		
		else if (this.type == Event.TYPE_FOLD) ret += "fold";
		else if (this.type == Event.TYPE_CHECK) ret += "check";
		else if (this.type == Event.TYPE_BET || this.type == Event.TYPE_BET_ALLIN) ret += "bet";
		else if (this.type == Event.TYPE_RAISE || this.type == Event.TYPE_RAISE_ALLIN) ret += "raise";
		else if (this.type == Event.TYPE_CALL || this.type == Event.TYPE_CALL_ALLIN) ret += "call";

		else if (this.type == Event.TYPE_SHOW) ret += "show "+Card.mask2Str(this.iVal);
		else if (this.type == Event.TYPE_MUCK) ret += "muck "+Card.mask2Str(this.iVal);
		
		else ret += "event type="+this.type;

		if (this.isAllIn()) ret += " all-in";
		
		if (this.who != null) ret = this.who+" "+ret;
		if (this.isPutToPot()) ret += " "+iVal;

		if (this.isNewStage()) ret += " " + this.sVal;
		return ret;
	}
	public String toString() {
		String ret = this.toStringNoWeight();
		if (!Double.isNaN(this.weight)) ret += String.format(" (%1.1f%%)", this.weight*100);
		return ret;
	}
	public String toStringShort() {
		String ret = "";
		if (this.type == Event.TYPE_FOLD) ret += "fold";
		else if (this.type == Event.TYPE_CHECK) ret += "check";
		else if (this.isCallOrCheck() && !this.isAllIn()) ret += "call "+this.iVal;
		else if (this.isCallOrCheck() && this.isAllIn()) ret += "call all-in "+this.iVal;
		else if (this.isBet() && !this.isAllIn()) ret += "bet "+this.iVal;
		else if (this.isRaise() && !this.isAllIn()) ret += "raise "+this.iVal;
		else if (this.isBet() && this.isAllIn()) ret += "bet all-in "+this.iVal;
		else if (this.isRaise() && this.isAllIn()) ret += "raise all-in "+this.iVal;
		else if (this.isBoard()) ret += "board "+Card.mask2Str(this.iVal);
		else ret += "ev type "+this.type;
		return ret;
	}
	public String toStringVeryShort() {
		String ret = "";
		if (this.type == Event.TYPE_FOLD) ret += "f";
		else if (this.type == Event.TYPE_CHECK) ret += "c";
		else if (this.isCallOrCheck() && this.isAllIn()) ret += "ca"+this.iVal;
		else if (this.isCallOrCheck()) ret += "c"+this.iVal;
		else if (this.isBet() && !this.isAllIn()) ret += "b"+this.iVal;
		else if (this.isRaise() && !this.isAllIn()) ret += "r"+this.iVal;
		else if (this.isBet() && this.isAllIn()) ret += "ba"+this.iVal;
		else if (this.isRaise() && this.isAllIn()) ret += "ra"+this.iVal;
		//else if (this.isBoard()) ret += "b"+Card.mask2Str(this.iVal);
		else ret += "e"+this.type;
		return ret;
	}
}

	