package com.poker.brazillive.shell.game;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.poker.brazillive.shell.util.*;

public class Game implements Cloneable, Serializable {
	// Mask values
	private static final int RULES_CAN_RAISE_IF_EVERYBODY_ALLIN		= 1 << 0;
	private static final int RULES_CAN_RAISE_IF_NO_RAISE_WITH_STEP	= 1 << 1;
	private static final int RULES_RAISE_STEP_IS_TWICE_RAISE_LEVEL	= 1 << 2;

	public static final int RULES_ROOM_STARS = 0;
	public static final int RULES_ROOM_CHICO = 0;
	public static final int RULES_ROOM_DLR = /*RULES_CAN_RAISE_IF_EVERYBODY_ALLIN |*/ RULES_CAN_RAISE_IF_NO_RAISE_WITH_STEP | RULES_RAISE_STEP_IS_TWICE_RAISE_LEVEL;
	public static final int RULES_ROOM_ALT = RULES_CAN_RAISE_IF_EVERYBODY_ALLIN;
	public int rules = RULES_ROOM_STARS;

	// This numbers are used in simOneTrial and somewhere else. Change is dengerous
	public static final int STAGE_FIRST			= 1;
	
	public static final int STAGE_BLINDS		= 1;
	public static final int STAGE_PREFLOP		= 2;
	public static final int STAGE_FLOP			= 3;
	public static final int STAGE_TURN			= 4;
	public static final int STAGE_RIVER			= 5;
	public static final int STAGE_SHOWDOWN		= 6;
	public static final int STAGE_GAME_FINISHED	= 7;
	
	public static final int STAGE_LAST 			= 7;
	
	public static final String[] stage2Str = new String[STAGE_LAST+1];
	public static final String[] stage2StrLong = new String[STAGE_LAST+1];
	static {
		stage2Str[STAGE_BLINDS] = "b";
		stage2Str[STAGE_PREFLOP] = "p";
		stage2Str[STAGE_FLOP] = "f";
		stage2Str[STAGE_TURN] = "t";
		stage2Str[STAGE_RIVER] = "r";
		stage2Str[STAGE_GAME_FINISHED] = "fin";

		stage2StrLong[STAGE_BLINDS] = "blinds";
		stage2StrLong[STAGE_PREFLOP] = "pre";
		stage2StrLong[STAGE_FLOP] = "flop";
		stage2StrLong[STAGE_TURN] = "turn";
		stage2StrLong[STAGE_RIVER] = "river";
		stage2StrLong[STAGE_SHOWDOWN] = "showdown";
		stage2StrLong[STAGE_GAME_FINISHED] = "finished";
	}
	
	public String id;
	public String date;
	public String heroName;
	public Player[] players = new Player[0];
	public Player[] allPlayers = new Player[0];
	public Card[] board = {};
	public int sb, bb;
	public boolean sbPosted = false;
	public boolean bbPosted = false;
	//public int lastFoldedPutToPotInHand;
	public int curMovePlayerInd = -1;
	public int lastToMoveInStagePlayerInd;
	public int stage = STAGE_BLINDS;
	public boolean stageFinished = false;
	public int pot;
	public int guestPot;
	public int raisedVal;
	public int raisedValWithStep;
	public int raiseStep;
	public int stageRaiseCount = 0;
	public int firstRaiseVs = -1;
	public int sbFolded = 0;
	public int rcount = 0;
	public int preflopRaisedVsHowMany = -1;
	public int potPaidCount = 0;
	public double potTieSum = 0;
	public int showdownPlayersCount = 0;
	public int mcount = 0;
	
	public Game() {
	}
	
	public static int getBoardLen(int stage) {
		if (stage == Game.STAGE_BLINDS) return 0;
		if (stage == Game.STAGE_PREFLOP) return 0;
		if (stage == Game.STAGE_FLOP) return 3;
		if (stage == Game.STAGE_TURN) return 4;
		if (stage == Game.STAGE_RIVER) return 5;
		if (stage == Game.STAGE_SHOWDOWN) return 5;
		
		assert false: stage;
		return -1;
	}
	
	public Player getPlayer(String name) {
		for (Player p: this.players) if (p.name.equals(name)) return p;
		return null;
	}
	public Player getPlayerFromAll(String name) {
		for (Player p: this.allPlayers) if (p.name.equals(name)) return p;
		return null;
	}
	public int getPlayerInd(String name) {
		return Game.getElementInd(this.players, this.getPlayer(name));
	}
	public static Player[] removeElement(Player[] ar, int ind) {
		Player[] newAr = new Player[ar.length-1];
		System.arraycopy(ar, 0, newAr, 0, ind);
		System.arraycopy(ar, ind+1, newAr, ind, ar.length-ind-1);
		return newAr;
	}
	private static Player[] insertElement(Player[] ar, int ind, Player p) {
		Player[] newAr = new Player[ar.length+1];
		if (ar.length > 0) System.arraycopy(ar, 0, newAr, 0, ind);
		if (ar.length > ind) System.arraycopy(ar, ind, newAr, ind+1, ar.length-ind);
		newAr[ind] = p;
		return newAr;
	}
	public static int getElementInd(Player[] ar, Player p) {
		for (int i = 0; i < ar.length; i++) if (ar[i] == p) return i;
		return -1;
	}
	public static Player[] setFirst(Player[] ar, Player p) {
		Player[] ret = new Player[ar.length];
		int pInd = Game.getElementInd(ar, p);
		System.arraycopy(ar, pInd, ret, 0, ar.length-pInd);
		System.arraycopy(ar, 0, ret, ar.length-pInd, pInd);
		return ret;
	}
	
	public void insertPlayerBeforeCurMove(Player p) {
		int allInd = Game.getElementInd(this.allPlayers, this.getCurMovePlayer());
		this.allPlayers = Game.insertElement(this.allPlayers, allInd, p);
		
		this.players = Game.insertElement(this.players, this.curMovePlayerInd, p);
		if (this.lastToMoveInStagePlayerInd >= this.curMovePlayerInd)
			this.lastToMoveInStagePlayerInd++;
	}
	
	public void addPlayer(Player p) {
		this.players = Game.insertElement(this.players, this.players.length, p);
		this.allPlayers = Game.insertElement(this.allPlayers, this.allPlayers.length, p);
	}
	public void removePlayer(Player p) {
		assert this.hasPlayer(p);
		int ind = this.getPlayerInd(p.name);
		
		if (this.curMovePlayerInd > ind) this.curMovePlayerInd--;
		if (this.curMovePlayerInd == ind && this.players.length-1 == ind) this.curMovePlayerInd = 0;
		
		if (this.lastToMoveInStagePlayerInd > ind) this.lastToMoveInStagePlayerInd--;
		if (this.lastToMoveInStagePlayerInd == ind && this.players.length-1 == ind) this.lastToMoveInStagePlayerInd = 0;

		this.players = Game.removeElement(this.players, ind);
		
		
		ind = Game.getElementInd(this.allPlayers, p);
		this.allPlayers = Game.removeElement(this.allPlayers, ind);
	}
	
	public boolean hasPlayer(Player p) {
		for (Player pp: this.players)
			if (p == pp) return true;
		return false;
	}
	
	public void setSbPlayer(Player p) {
		if (this.players.length == 2) {
			Player p2 = null;
			if (this.players[0] == p) p2 = this.players[1];
			else p2 = this.players[0];
			this.players = Game.setFirst(this.players, p2);
			//this.allPlayers = Game.setFirst(this.allPlayers, p2);
		} else {
			this.players = Game.setFirst(this.players, p);
			//this.allPlayers = Game.setFirst(this.allPlayers, p);
		}
	}
	
	public Game clone() {
		Game g = new Game();
		
		g.players = new Player[this.players.length];
		g.allPlayers = new Player[this.allPlayers.length];
		for (int i = 0; i < this.allPlayers.length; i++) {
			Player p = this.allPlayers[i];
			Player cp = p.clone();
			g.allPlayers[i] = cp;
			for (int j = 0; j < this.players.length; j++) if (this.players[j] == p)
				g.players[j] = cp;
		}
		
		g.board = new Card[this.board.length];
		for (int i = 0; i < this.board.length; i++)
			g.board[i] = this.board[i];
		
		g.id = this.id;
		g.heroName = this.heroName;
		g.sb = this.sb;
		g.bb = this.bb;
		g.sbPosted = this.sbPosted;
		g.bbPosted = this.bbPosted;
		g.curMovePlayerInd = this.curMovePlayerInd;
		g.lastToMoveInStagePlayerInd = this.lastToMoveInStagePlayerInd;
		g.stage = this.stage;
		g.stageFinished = this.stageFinished;
		g.pot = this.pot;
		g.guestPot = this.guestPot;
		g.raisedVal = this.raisedVal;
		g.raisedValWithStep = this.raisedValWithStep;
		g.raiseStep = this.raiseStep;
		g.stageRaiseCount = this.stageRaiseCount;
		g.firstRaiseVs = this.firstRaiseVs;
		g.sbFolded = this.sbFolded;
		g.rcount = this.rcount;
		g.potPaidCount = this.potPaidCount;
		g.potTieSum = this.potTieSum;
		g.showdownPlayersCount = this.showdownPlayersCount;
		g.preflopRaisedVsHowMany = this.preflopRaisedVsHowMany;
		g.mcount = this.mcount;
		g.rules = this.rules;
		
		return g;
	}
	
	public int getPlayerPos(int ind) {
		return this.players.length - ind;
	}

	public int getPlayerPos(Player p) {
		return getPlayerPos(Game.getElementInd(this.players, p));
	}
	
	public int getCurPlayerPos() {
		return getPlayerPos(this.curMovePlayerInd);
	}

	public Player getCurMovePlayer() {
		if (this.curMovePlayerInd < 0 || this.curMovePlayerInd > this.players.length-1)
			return null;
		return this.players[this.curMovePlayerInd];
	}
	
	
	private boolean isNotInTurnBlind(Event ev) {
		if (ev.type == Event.TYPE_SB && this.sbPosted) return true;
		if (ev.type == Event.TYPE_BB && this.bbPosted) return true;
		if (ev.type == Event.TYPE_SBB) return true;
		if (ev.type == Event.TYPE_ANTE) return true;
		if (ev.type == Event.TYPE_DEAD_BLIND) return true;
		return false;
	}
	
	public void procEvent(Event ev) throws GameException, Exception {
		procEvent(ev, true);
	}
	public void procEvent(Event ev, boolean validate) throws GameException, Exception {
		/*if (ev.fromString != null && ev.fromString.contains("rude_b0y17 posts big blind $0.05")) {
			int tmp = 0;
		}*/
		
		if (validate) {
			String v = this.validateEvent(ev);
			if (v != null) {
				//this.validateEvent(ev);
				throw new GameException(v);
			}
		}

		if (false) {
		} else if (ev.isMove() || ev.type == Event.TYPE_STRADDLE || (ev.type == Event.TYPE_SB && !this.sbPosted)
				|| ((ev.type == Event.TYPE_BB || ev.type == Event.TYPE_BB_ALLIN) && !this.bbPosted /* if sb not posted this is game with no sb*/)) {

			if (ev.type == Event.TYPE_SB) {
				this.sb = (int)ev.iVal;
				this.setSbPlayer(this.getPlayer(ev.who));
				this.curMovePlayerInd = this.getPlayerInd(ev.who);
			}
			if (ev.type == Event.TYPE_BB || ev.type == Event.TYPE_BB_ALLIN) {
//				if (this.rules == RULES_ROOM_CHICO && !this.sbPosted){
//					this.players = setFirst(this.players, this.getPlayer(ev.who));
//					this.curMovePlayerInd = this.getPlayerInd(ev.who);
//				}
				if (!this.sbPosted) { // playing without sb
					this.players = Game.setFirst(this.players, this.getPlayer(ev.who));
					this.curMovePlayerInd = this.getPlayerInd(ev.who);
				}
				this.lastToMoveInStagePlayerInd = this.getPlayerInd(ev.who);
				//this.curMovePlayerInd = this.lastToMoveInStagePlayerInd+1;
				this.bb = (int)ev.iVal;
				this.raiseStep = this.bb;
				this.raisedVal = this.bb;
				this.raisedValWithStep = this.bb;
			}
			if (ev.type == Event.TYPE_STRADDLE) {
				this.lastToMoveInStagePlayerInd = this.getPlayerInd(ev.who);
			}
			
			this.move(ev);
		} else if (ev.isBoard()) {
			this.board = Card.join(this.board, Card.mask2Cards(ev.iVal));

			this.stage++;
			this.stageRaiseCount = 0;
			this.raisedVal = 0;
			this.raisedValWithStep = 0;
			this.raiseStep = this.bb;
			for (Player pl: this.players) {
				pl.putToPotInStage = 0;
				pl.turnCountInStage = 0;
			}
			
			int allinCount = 0;
			for (Player p: this.players) if (p.stack == 0) allinCount++;
			
			if (allinCount < this.players.length-1) {
				this.curMovePlayerInd = 0;
				while (this.getCurMovePlayer().stack == 0) this.curMovePlayerInd++;
				this.lastToMoveInStagePlayerInd = this.players.length-1;
				while (this.players[this.lastToMoveInStagePlayerInd].stack == 0)
					this.lastToMoveInStagePlayerInd--;
				this.stageFinished = false;
			}
			
		} else if (ev.type == Event.TYPE_SHOWDOWN) {
			this.stage++;
		} else if (this.isNotInTurnBlind(ev)) {
			Player p = this.getPlayer(ev.who);
			if (ev.type == Event.TYPE_SBB) {
				p.putToPotInHand += this.bb;
				p.putToPotInStage += this.bb;
				this.pot += ev.iVal;
				this.guestPot += this.sb;
			} else if (ev.type == Event.TYPE_SB) {
				this.pot += ev.iVal;
				this.guestPot += this.sb;
			} else if (ev.type == Event.TYPE_BB) {
				p.putToPotInHand += this.bb;
				p.putToPotInStage += this.bb;
				this.pot += ev.iVal;
			} else if (ev.type == Event.TYPE_ANTE) {
				this.pot += ev.iVal;
				this.guestPot += ev.iVal;
			} else if (ev.type == Event.TYPE_DEAD_BLIND) {
				this.pot += ev.iVal;
				this.guestPot += ev.iVal;
			} else assert false;
			p.stack -= ev.iVal;
			
		} else if (ev.type == Event.TYPE_HOLES) {
			this.stage = Game.STAGE_PREFLOP;
			if (ev.iVal != 0) {
//				Player p = this.getPlayer(this.heroName);
				Player p = this.getPlayer(ev.who);
				if (p != null) p.holes = Card.mask2Cards(ev.iVal);
			}
		} else if (ev.type == Event.TYPE_SHOW) {
			Player p = this.getPlayerFromAll(ev.who); // From all because player can fold and show cards after game is finished
			p.holes = Card.mask2Cards(ev.iVal);
			if (this.stage == Game.STAGE_SHOWDOWN) {
				this.showdownPlayersCount++;
			}
		} else if (ev.type == Event.TYPE_MUCK) {
			Player p = this.getPlayer(ev.who);
			p.mucked = true;
			this.showdownPlayersCount++;
		} else if (ev.type == Event.TYPE_WIN_POT || ev.type == Event.TYPE_WIN_SIDE_POT) {
			boolean tie = false;
			Pot[] pots = this.getPots();
			Pot pot = pots[pots.length-1-this.potPaidCount];
			if (ev.iVal < pot.size*0.7) tie = true;
			
			if (tie) {
				this.potTieSum += ev.iVal;
				if (this.potTieSum > 0.9*pot.size) {
					this.potPaidCount++;
					this.potTieSum = 0;
				}
			} else {
				this.potPaidCount++;
			}
			Player p = this.getPlayer(ev.who);
			p.won += (int)ev.iVal;
		}
		
		if (ev.type == Event.TYPE_SB) this.sbPosted = true;
		if (ev.type == Event.TYPE_BB || ev.type == Event.TYPE_BB_ALLIN) this.bbPosted = true;
	}
	
	protected void move(Event ev) {
		assert ev.isMove() || ev.isBlind() || ev.type == Event.TYPE_STRADDLE;
		
		// Stats
		Player p = this.getPlayer(ev.who);
		if (ev.isMove()) {
			this.mcount++;
			if (!ev.isBlind()) p.turnCountInStage++;
			p.prevPrevAction = p.prevAction;
			p.prevAction = ev.type;
			if (ev.isBetOrRaise()) {
				if (this.stage == Game.STAGE_PREFLOP && this.rcount == 0) this.preflopRaisedVsHowMany = this.players.length-1;
				int call = this.getCall();
				int rval = (int)ev.iVal - p.putToPotInStage;
				this.rcount++;
				p.betOrRaiseCount[this.stage]++;
				if (this.getCall() > 0) p.raiseCount[this.stage]++;
				assert ev.iVal >= 0;
				p.raised[this.stage] += rval;
				p.raisedPot[this.stage] = Math.max(p.raisedPot[this.stage], 1.0*(rval-call)/(this.pot+call));
				
				if (this.stageRaiseCount == 0) p.putCount++;
				else p.putCount += 2;
			} else if (ev.isCallOrCheck()) {
				p.called[this.stage] += ev.iVal;
				p.calledEq[this.stage] = Math.max(p.calledEq[this.stage], this.getCallEq());
				if (ev.type == Event.TYPE_CALL || ev.type == Event.TYPE_CALL_ALLIN) p.putCount++;
			}
		}
		
		boolean lastMoveInStage = ev.isMove() && this.curMovePlayerInd == this.lastToMoveInStagePlayerInd
				&& this.curMovePlayerInd != -1 && this.bbPosted;
		
		if (ev.type != Event.TYPE_FOLD) {
			// raise or call or check
			if (ev.isPutToPot()) {
				// raise or call or blind
				int put = -1;
				if (ev.isBetOrRaise() || ev.isBlind() || ev.type == Event.TYPE_STRADDLE) {
					// raise
					if (ev.type == Event.TYPE_STRADDLE || ev.isBlind()
							|| (int)ev.iVal >= this.raisedValWithStep + this.raiseStep)
						this.raisedValWithStep = (int)ev.iVal;
					
					if ((this.rules & RULES_RAISE_STEP_IS_TWICE_RAISE_LEVEL) == 0) 
						this.raiseStep = Math.max(this.raiseStep, (int)ev.iVal - this.raisedVal);
					else 
						this.raiseStep = Math.max(this.raiseStep, (int)ev.iVal);
					
					this.raisedVal = Math.max(this.raisedVal, (int)ev.iVal);
					
					if (!ev.isBlind() && ev.type != Event.TYPE_STRADDLE)
						this.lastToMoveInStagePlayerInd = this.curMovePlayerInd - 1;
					if (this.lastToMoveInStagePlayerInd < 0) this.lastToMoveInStagePlayerInd = this.players.length-1;
					
					if (ev.isBetOrRaise()) {
						this.stageRaiseCount++;
						if (this.firstRaiseVs == -1) this.firstRaiseVs = this.players.length-1;
						lastMoveInStage = false;
					}
					put = (int)(ev.iVal-p.putToPotInStage);
				} else if (ev.type == Event.TYPE_CALL || ev.type == Event.TYPE_CALL_ALLIN) {
					put = (int)ev.iVal;
				} else assert false;
				assert put != -1;

				this.pot += put;
				p.stack -= put;
				p.putToPotInStage += put;
				p.putToPotInHand += put;
			}

		} else if (ev.type == Event.TYPE_FOLD) {
			//this.lastFoldedPutToPotInHand = this.players[this.curMovePlayerInd].putToPotInHand;
			if (this.sbFolded == 0 && this.curMovePlayerInd == 0) this.sbFolded = 1;
			this.players = Game.removeElement(this.players, this.curMovePlayerInd);
		}
		if (ev.isMove() || ev.isBlind() || ev.type == Event.TYPE_STRADDLE) {
			if (ev.type == Event.TYPE_FOLD) {
				if (this.lastToMoveInStagePlayerInd > this.curMovePlayerInd) this.lastToMoveInStagePlayerInd--;
				if (this.curMovePlayerInd >= this.players.length) this.curMovePlayerInd = 0;
			} else {
				this.curMovePlayerInd++;
				if (this.curMovePlayerInd >= this.players.length) this.curMovePlayerInd = 0;
			}
		}
		
		if (lastMoveInStage
				// Everybody but button folded
				|| (this.stage == Game.STAGE_PREFLOP && this.players.length == 1 && this.raisedVal == this.bb))
			this.stageFinished = true;
		
		// Move further in case of all-in player
		if (!this.stageFinished && this.stage >= Game.STAGE_PREFLOP && this.stage <= Game.STAGE_RIVER) {

			int allinCount = 0;
			Player notAllinPlayer = null;
			for (Player cp: this.players) {
				if (cp.stack == 0) allinCount++;
				else notAllinPlayer = cp;
			}
			
			int c = 0;
			while ((this.getCurMovePlayer().stack == 0 && this.getCurMovePlayer().putToPotInHand > 0)
					// The case when p1 is all-in and p2 is on bb which is bigger the raise
					|| (this.players.length - allinCount == 1 && notAllinPlayer.putToPotInStage >= this.raisedVal)) {
				
				if (this.curMovePlayerInd == this.lastToMoveInStagePlayerInd) {
					this.stageFinished = true;
					break;
				}
				this.curMovePlayerInd++;
				if (this.curMovePlayerInd >= this.players.length) this.curMovePlayerInd = 0;
				c++;
				if (c > 20) break; // Just in case, not to get inf loop
			}
		}
		if (this.stageFinished) {
			this.curMovePlayerInd = -1;
			this.lastToMoveInStagePlayerInd = -1;
			this.returnUncalled();
			for (Player cp: this.allPlayers) cp.putToPotInStage = 0;
		}
	}
	
	private void returnUncalled() {
		assert this.stageFinished;
		Player[] spls = this.allPlayers.clone();
		Arrays.sort(spls, new Comparator<Player>() {
			public int compare(Player p1, Player p2) {
				return -Integer.compare(p1.putToPotInStage, p2.putToPotInStage);
			}
		});
		int ret = (spls[0].putToPotInStage) - (spls[1].putToPotInStage);
		assert ret >= 0;
		Player p = spls[0];
		p.stack += ret;
		p.putToPotInHand -= ret;
		p.putToPotInStage -= ret;
		this.pot -= ret;
	}
	
	public class Pot {
		public int size;
		public int raiseLevel;
		public Player[] players = new Player[0];
		public void addPlayer(Player p) {
			Player[] pls = new Player[this.players.length+1];
			System.arraycopy(this.players, 0, pls, 0, this.players.length);
			pls[pls.length-1] = p;
			this.players = pls;
		}
		public String toString() {
			String ret = String.format("Pot size=%d raise=%d ", this.size, this.raiseLevel);
			for (Player p: this.players) ret += p.name+" ";
			return ret;
		}
	}
	
	public Pot[] getPots() {
		Player[] spls = this.allPlayers.clone();
		Arrays.sort(spls, new Comparator<Player>() {
			public int compare(Player p1, Player p2) {
				return Integer.compare(p1.putToPotInHand, p2.putToPotInHand);
			}
		});
		Pot[] pots = new Pot[10];
		int pc = 0;
		Pot prevPot = null;
		for (int i = 0; i < spls.length; i++) {
			if (spls[i].stack == 0 || i == spls.length-1) {
				Pot pot = new Pot();
				pot.raiseLevel = spls[i].putToPotInHand;
				for (int j = 0; j < spls.length; j++) {
					int potAdd = pot.raiseLevel;
					if (potAdd > spls[j].putToPotInHand)
						potAdd = spls[j].putToPotInHand;
					if (prevPot != null) potAdd -= prevPot.raiseLevel;
					if (potAdd < 0) potAdd = 0;
					pot.size += potAdd;
					if (spls[j].putToPotInHand >= pot.raiseLevel
							&& this.hasPlayer(spls[j]))
						pot.addPlayer(spls[j]);
				}
				if (pc == 0) pot.size += this.guestPot;
				if (pot.size != 0) {
					pots[pc++] = pot;
					prevPot = pot;
				}
			}
		}
		Pot[] ret = new Pot[pc];
		System.arraycopy(pots, 0, ret, 0, pc);
		return ret;
	}
	public int getPotForPlayer(Player p) {
		Pot[] pots = this.getPots();
		int ret = 0;
		for (Pot pot: pots) {
			if (Game.getElementInd(pot.players, p) != -1) ret += pot.size;
		}
		return ret;
	}
	
	public int getCall() {
		Player p = this.getCurMovePlayer();
		return Math.min(this.raisedVal-p.putToPotInStage, p.stack);
	}
	public double getCallEq() {
		int call = this.getCall();
		Player p = this.getCurMovePlayer();
		int epot = 0;
		
		Pot[] pots = this.getPots();
		for (int i = 0; i < pots.length-1; i++) epot += pots[i].size;
		Pot lastPot = pots[pots.length-1];
		
		if (call < p.stack)	{
			epot += call + lastPot.size;
		} else {
			assert call == p.stack;
			
			epot += lastPot.size + call;
			for (Player cp: this.players) {
				if (cp == p) continue;
				epot -= Math.max(0, cp.putToPotInHand - (p.putToPotInHand + call));
			}
		}
		double ret = 1d*call/epot;
		assert ret <= 0.5;
		return ret;
	}
	
	public int getMinRaise() {
		boolean everybodyAllin = true;
		boolean smbAllin = false;
		for (Player p: this.players) {
			if (p == this.getCurMovePlayer()) continue;
			if (p.stack > 0) everybodyAllin = false;
			if (p.stack == 0) smbAllin = true;
		}
		if (everybodyAllin && (this.rules & RULES_CAN_RAISE_IF_EVERYBODY_ALLIN) == 0) return -1;
		
		Player p = this.getCurMovePlayer();
		boolean blind = this.stage == Game.STAGE_PREFLOP && p.turnCountInStage == 0 && p.putToPotInHand > 0 && p.putToPotInHand <= this.bb;
		boolean checked = this.stage >= Game.STAGE_FLOP && p.putToPotInStage == 0;
		if ((this.rules & RULES_CAN_RAISE_IF_NO_RAISE_WITH_STEP) == 0) {
			if (this.getCall() > 0 && this.getCall() < this.raiseStep && !blind && !checked) return -1; // Player already raised and there was no re-raise with step
		}
//		if (this.getCall() > 0 && this.getCall() < this.raiseStep && p.turnCountInStage > 0) return -1; // Player already raised and there was no re-raise with step
		
//		if (this.getCurMovePlayer().betOrRaiseCount[this.stage] > 0 && this.raisedVal > this.raisedValWithStep) {
//			assert smbAllin;
//			boolean hasPlayerNotAllinAndPutBigger = false; // If true - there is player who did re-raise with step, means hero can re-raise
//			for (Player p: this.players) {
//				if (p == this.getCurMovePlayer()) continue;
//				if (p.stack == 0) continue;
//				if (p.putToPotInStage > this.getCurMovePlayer().putToPotInStage) hasPlayerNotAllinAndPutBigger = true;
//			}
//			if (!hasPlayerNotAllinAndPutBigger) return -1; // Player raised and there were no re-raise with step - raise is not possible
//		}
		
		int call = this.getCall();
		//int minRaise = call + this.raiseStep;
		//if (call < this.raiseStep) minRaise -= call; // Raise less the raise step all-in, then re-raise. Example: bb 5, raise 6 all-in, re-raise 10
		if (p.stack <= call) return -1;
		int minRaise = this.raisedValWithStep + this.raiseStep - p.putToPotInStage;
		if (p.stack < minRaise) return p.stack;
		else return minRaise;
	}
	
	public int getMaxEffectiveRaiseLevel() {
		assert this.getCurMovePlayer() != null;
		if (this.getMinRaise() == -1) return -1;
		Player p = this.getCurMovePlayer();
		int merl = 0;
		for (Player cp: this.players) {
			if (cp == p) continue;
			if (cp.stack+cp.putToPotInStage > merl) merl = cp.stack+cp.putToPotInStage;
		}
		if (p.stack+p.putToPotInStage < merl) merl = p.stack+p.putToPotInStage;
		return merl;
	}
	
	private int getRake(int pot) {
		if (this.stage == Game.STAGE_PREFLOP) return 0;
		
		int irake = -1;
		if (this.bb == 2) {
		} else if (this.bb == 5) {
			double drake = pot*0.0415;
			irake = (int)Math.round(drake);
			if (this.allPlayers.length <= 4 && irake > 50) irake = 50;
			else if (irake > 100) irake = 100;
		} else assert false;
		assert irake >= 0;
		return irake;
	}
	
	private static String sf(String fmt, Object... args) throws Exception {
		return String.format(fmt, args);
	}
	private long getExistingCardsMask(Player excPl) {
		long existingCards = 0;
		existingCards |= Card.cards2Mask(this.board);
		for (Player p: this.allPlayers) {
			if (p.holes == null) continue;
			if (p == excPl) continue;
			existingCards |= Card.cards2Mask(p.holes);
		}
		return existingCards;
	}
	public String validateEvent(Event ev) throws Exception {
		
		if (ev.type == Event.TYPE_UNKNOWN)
			return sf("Unknown event. From string: '%s'", ev.fromString);
		
		if (ev.isMove() || ev.type == Event.TYPE_STRADDLE
				|| ((ev.type == Event.TYPE_BB || ev.type == Event.TYPE_BB_ALLIN)
						&& !this.bbPosted && this.sbPosted)) {
			
			if (this.players.length < 2)
				return sf("Not enough players in game: %s", this.players.length);
			
			if (this.board.length < Game.getBoardLen(this.stage))
				return sf("Incorrect board length. Expected %d, really %d. Board event wasn't processed?", Game.getBoardLen(this.stage));

			if (this.stageFinished)
				return sf("Game stage is finished, but player moved. Expected board event. Current stage: ", Game.stage2StrLong[this.stage]);

			if (curMovePlayerInd < 0)
				return sf("Player to move number is incorrect, less then zero: %d", curMovePlayerInd);

			if (curMovePlayerInd >= this.players.length)
				return sf("Player to move number is incorrect, bigger then players count: %d", curMovePlayerInd);

			if (!this.players[curMovePlayerInd].name.equals(ev.who))
				return sf("Incorrect player moved. Expected: '%s', Actual: '%s'", this.players[curMovePlayerInd].name, ev.who);

			int put = this.players[this.curMovePlayerInd].putToPotInStage;
			int stack = this.players[this.curMovePlayerInd].stack;

			if (stack <= 0)
				return sf("Incorrect player '%s' stack: %d", this.getCurMovePlayer().name, this.getCurMovePlayer().stack);
			
			int addToPot = (int)ev.iVal;
			if (ev.isBetOrRaise()) addToPot -= this.getCurMovePlayer().putToPotInStage;

			if (addToPot < 0)
				return sf("Incorrect put to pot money amount: %d", addToPot);
			
			int mr = this.getMinRaise();
			if (ev.isBetOrRaise()) {
				if (mr == -1)
					return sf("Player '%s' raised while raise is not possible", this.getCurMovePlayer().name);
				if (addToPot < mr)
					return sf("Player '%s' raised %d that is less than minimum raise %d", this.getCurMovePlayer().name, addToPot, mr);
			}
			if (ev.isBet() && this.raisedVal > 0) return sf("Player '%s' bet, but only raise is possible", this.getCurMovePlayer().name);
			if (ev.isRaise() && this.raisedVal == 0) return sf("Player '%s' raised, but only bet is possible", this.getCurMovePlayer().name);
			
			// Cannot put to pot bigger then stack
			if (stack < addToPot)
				return sf("Player '%s' put to pot %d, but this is more than his stack %d", this.getCurMovePlayer().name, addToPot, this.getCurMovePlayer().stack);
			
			if (stack > addToPot) { // This is not all-in
				// Cannot put less then call
				if (ev.isCallOrCheck()) {
					if (put+addToPot < this.raisedVal)
						return sf("Player '%s' call or check is incorrect. Expected call size: %d, actual call size: %d, player stack: %d", this.getCurMovePlayer().name, this.raisedVal-put, addToPot, this.getCurMovePlayer().stack);
				}
				if (ev.isAllIn())
					return sf("Player '%s' put to pot %d that is less then his stack %d, this cannot be all-in", this.getCurMovePlayer().name, addToPot, this.getCurMovePlayer().stack);
			} else if (stack == addToPot) { // This is all-in
				if (!ev.isAllIn())
					return sf("Player '%s' put to pot all his stack %d. This must be all-in", this.getCurMovePlayer().name, this.getCurMovePlayer().stack);
			}

			if (ev.isMove() && (this.stage < Game.STAGE_PREFLOP || this.stage > Game.STAGE_RIVER))
				return sf("Incorrect stage: %s", Game.stage2StrLong[this.stage]);
			if (ev.isCallOrCheck()) {
				if (this.getCall() != ev.iVal)
					return sf("Incorrect call amount: %d, expected: %d", ev.iVal, this.getCall());
			}

//		} else if (ev.type == Event.TYPE_ANTE || ev.type == Event.TYPE_STRADDLE) {
//			if (this.stage != Game.STAGE_BLINDS) return sf("Incorrect stage %d for ante", this.stage);
//			if (this.getPlayer(ev.who) == null) return sf("Not existing player '%s' posted the ante", ev.who);
			
		} else if (ev.isBoard()) {
		
			if (! this.stageFinished)
				return sf("Unexpected board event. Game stage is not finished");
			
			if (this.players.length < 2)
				return sf("Incorrect amount of players: %d", this.players.length);
			
			Card[] addCards = Card.mask2Cards(ev.iVal);
			if (Game.getBoardLen(this.stage+1) != this.board.length + addCards.length)
				return sf("Incorrect board length after board is delt. Expected: %d, Actual: %d", Game.getBoardLen(this.stage+1), this.board.length + addCards.length);
			
			long ec = this.getExistingCardsMask(null);
			if ((ec & Card.cards2Mask(addCards)) != 0)
				return sf("Repeated cards are delt to board. Delt: %s, existing: %s", Card.cards2Str(addCards), Card.mask2Str(ec));
			
			if (this.board.length + addCards.length != Game.getBoardLen(this.stage+1))
				return sf("Incorrect board cards count. Board: %s, Dealt: %s", Card.cards2Str(this.board), Card.cards2Str(addCards));

		} else if (ev.isBlind() || ev.type == Event.TYPE_ANTE || ev.type == Event.TYPE_STRADDLE) {
			if (this.stage != Game.STAGE_BLINDS)
				return sf("Incorrect stage for this event. Expected: %s, actual: %s", Game.stage2StrLong[Game.STAGE_BLINDS], Game.stage2StrLong[this.stage]);
			if (this.board.length != 0)
				return sf("Expected empty board for this event. Actual board: %s", Card.cards2Str(this.board));
			if (this.getPlayer(ev.who) == null)
				return sf("Player '%s' doesn't exist", ev.who);
		} else if (ev.type == Event.TYPE_HOLES) {
			if (this.board.length != 0)
				return sf("Expected empty board when dealing holes. Actual board: %s", Card.cards2Str(this.board));
			
			long ec = this.getExistingCardsMask(this.getPlayer(ev.who));
			if ((ec & ev.iVal) != 0) return sf("Repeated delt holes cards. Delt: %s, existing: %s", Card.mask2Str(ev.iVal), Card.mask2Str(ec));
			
			if (ev.iVal != 0) {
				Card[] cs = Card.mask2Cards(ev.iVal);
				if (cs.length != 2) return sf("Incorrect amount of holes delt: %d, expected: %d", cs.length, 2);
			}
		} else if (ev.type == Event.TYPE_SHOWDOWN) {
			if (this.stage != Game.STAGE_RIVER)
				return sf("Incorrect stage for showdown. Expected: %s, actual: %s", Game.stage2StrLong[Game.STAGE_RIVER], Game.stage2StrLong[this.stage]);
			if (!this.stageFinished)
				return sf("Unexpected showdown when stage is not finished");
		} else if (ev.type == Event.TYPE_SHOW) {
			Player p = this.getPlayerFromAll(ev.who); // From all because player can fold and show holes after game finished
			if (p == null)
				return sf("Player '%s' showed cards but he wasn't part of the game", ev.who);
			if (p.holes != null && p.holes.length == 2 && Card.cards2Mask(p.holes) != ev.iVal)
				return sf("Showed cards do not match player cards. Showed: %s, has: %s", Card.mask2Str(ev.iVal), Card.cards2Str(p.holes));
			
			long ec = this.getExistingCardsMask(this.getPlayer(ev.who));
			if ((ec & ev.iVal) != 0) return sf("Repeated showed cards. Showed: %s, existing: %s", Card.mask2Str(ev.iVal), Card.mask2Str(ec));
		} else if (ev.type == Event.TYPE_MUCK) {
			if (this.stage != Game.STAGE_SHOWDOWN)
				return sf("Incorrect stage. Expected: %s, actual: %s", Game.stage2StrLong[Game.STAGE_SHOWDOWN], Game.stage2StrLong[this.stage]);
		} else if (ev.isCards() && ev.iVal != 0) {
			long m = Card.cards2Mask(this.board);
			for (Player p: this.allPlayers) if (p.holes != null) m |= Card.cards2Mask(p.holes);
			if ((m & ev.iVal) != 0) return sf("Repeated cards: %s", Card.mask2Str(ev.iVal));
		} else if (ev.type == Event.TYPE_WIN_POT || ev.type == Event.TYPE_WIN_SIDE_POT) {

			Pot[] pots = this.getPots();
			
			if (pots.length == 1 && this.pot != pots[0].size)
				return sf("Total pot doesn't match all pots. Total pot: %d, all pots: %d", this.pot, pots[0].size);
			
			Pot pot = pots[pots.length-1-this.potPaidCount];
			Player p = this.getPlayer(ev.who);
			int ind = Game.getElementInd(pot.players, p);
			if (ind == -1)
				return sf("Player '%s' is not involved to the pot number %d to be paied", ev.who, pots.length-1-this.potPaidCount);
			if (p.mucked)
				return sf("Player '%s' mucked. He cannot win any pot", ev.who);
			
			if (this.players.length >= 2 && pot.players.length != this.showdownPlayersCount)
				return sf("Amount of players involved to the current pot (%d) doesn't match amount of players who reached showdown (%d)", pot.players.length, this.showdownPlayersCount);

//			if (ev.type == Event.TYPE_WIN_POT || ev.type == Event.TYPE_WIN_SIDE_POT) {
//				boolean tie = false;
//				if (ev.iVal < 0.7*pot.size) tie = true;
//				
//				if (tie) {
//					
//					long eval = Calc.calcEval(this.board, p.holes);
//					int tieCount = 1;
//					for (Player cp: pot.players) {
//						if (cp == p) continue;
//						if (cp.mucked) continue;
//						long cpEval = Calc.calcEval(this.board, cp.holes);
//						if (eval < cpEval)
//							return sf("Player '%s' who tied the pot has weaker hand (%s) then player '%s' (%s)", p.name, Card.cards2Str(p.holes), cp.name, Card.cards2Str(cp.holes));
//						if (cpEval == eval) tieCount++;
//					}
//					if (tieCount < 2)
//						return sf("Amount of players who tied the pot cannot be less then 2. Actual amount: %d", tieCount);
//					//int val = (pot.size-this.getRake(pot.size))/tieCount;
//					int val = pot.size/tieCount;
//					//if (Math.abs(ev.iVal-val) > 1) return 31;
//					if (ev.iVal > val+1 || ev.iVal < 0.94*val-1)
//						return sf("Incorrect tied value %d. Expected from %1.1f to %d", ev.iVal, 0.94*val-1, val+1);
//
//				} else {
//				
//					if (this.board.length == 0 && ev.iVal != pot.size) // Preflop pots are not raked
//						return sf("Incorrect won amount %d. Expected: %d", ev.iVal, pot.size);
//					//else if (Math.abs(ev.iVal - (pot.size-this.getRake(pot.size))) > 1)
//					else if (ev.iVal > pot.size+1 || ev.iVal < 0.94*pot.size-1)
//						return sf("Incorrect won amount %d. Expected from %1.1f to %d", ev.iVal, 0.94*pot.size-1, pot.size+1);
//					
//					if (this.players.length >= 2) {
//						long eval = Calc.calcEval(this.board, p.holes);
//						for (Player cp: pot.players) {
//							if (cp == p) continue;
//							if (cp.mucked) continue;
//							long cpEval = Calc.calcEval(this.board, cp.holes);
//							if (eval <= cpEval)
//								return sf("Won player '%s' has weaker cards %s then player '%s' who has %s", p.name, Card.cards2Str(p.holes), cp.name, Card.cards2Str(cp.holes));
//						}
//					}
//				}
//			}
			
			if (ev.type == Event.TYPE_WIN_POT) {
				if (pots.length - this.potPaidCount != 1)
					return sf("Player '%s' won main pot, but there are not paied side pots", ev.who);
			} else if (ev.type == Event.TYPE_WIN_SIDE_POT) {
				if (pots.length - this.potPaidCount <= 1)
					return sf("Player '%s' won side pot, but there is only main pot", ev.who);
			}
			
		} else if (ev.type == Event.TYPE_GAME_FINISHED) {
			if (this.potPaidCount != this.getPots().length)
				return sf("Game is finished but not all the pots are paied. Paied: %d, Total: %d", this.potPaidCount, this.getPots().length);
			if (this.players.length >= 2 && this.showdownPlayersCount != this.players.length)
				return sf("Amount of players who reached showdown (%d) doesn't match amount of players in game (%d)", this.showdownPlayersCount, this.players.length);
		} else if (ev.is2Showdowns()) {
			return sf("2 showdowns are not supported");
		}
		return null;
	}
	
	public int countPlayersToMoveInStage() {
		if (this.curMovePlayerInd <= this.lastToMoveInStagePlayerInd) {
			return this.lastToMoveInStagePlayerInd - this.curMovePlayerInd;
		} else {
			return this.lastToMoveInStagePlayerInd + (this.players.length - this.curMovePlayerInd);
		}
	}
	
//	public void payWins() throws GameException {
//		
//		if (this.players.length == 1) {
//			this.players[0].won += this.pot;
//		} else if (this.stage == Game.STAGE_RIVER && this.stageFinished) {
//			
//			for (Game.Pot pot: this.getPots()) {
//				long maxEval = -1;
//				List<Player> winPlayers = new ArrayList<Player>();
//				for (Player p: pot.players) {
//					long eval = -1;
//					if (p.holes != null && p.holes.length == 2) {
//						if ((Card.cards2Mask(p.holes) & Card.cards2Mask(this.board)) != 0)
//							throw new GameException("Incorrect cards for paying wins");
//						eval = Calc.calcEval(this.board, p.holes);
//					}
//					if (eval > maxEval) {
//						winPlayers.clear();
//						winPlayers.add(p);
//						maxEval = eval;
//					} else if (eval == maxEval) {
//						winPlayers.add(p);
//					}
//				}
//				
//				for (Player p: winPlayers)
//					p.won += pot.size/winPlayers.size(); 
//			}
//		} else throw new GameException("Incorrect stage for paying wins");
//		
//	}

	public boolean isFinished() {
		if (!this.bbPosted) return false;
		if (this.players.length == 1) return true;
		if (this.stage == Game.STAGE_RIVER && this.stageFinished) return true;
		if (this.stage > Game.STAGE_RIVER) return true;
		return false;
	}

	public String toString() {
		String ret = "";
		String title = "---------------- Game #"+this.id+" -----------------";
		ret += title+"\n";
//		int bb = this.bb;
		int bb = 1;
		if (bb == 0) bb = 1; // to avoid division by zero
		
		String potStr = "";
		Pot[] pots = this.getPots();
		for (int i = 0; i < pots.length; i++) {
			if (!potStr.equals("")) potStr += " ";
			potStr += String.format("pot%d: %d (%d)", i+1, pots[i].size/bb, pots[i].players.length);
		}
		
		ret += potStr/*+", stage: "+Game.stage2StrLong[this.stage]*/
			+", board: "+Card.cards2Str(this.board)+", raised: "+this.raisedVal/bb
			+", rStep: "+this.raiseStep/bb+", bb="+this.bb;
		if (this.raisedVal != this.raisedValWithStep)
			ret += "("+this.raisedValWithStep+")";
		ret += "\n";
		for (int i = 0; i < title.length(); i++) ret += "-";
		ret += "\n";
		for (int i = 0; i < this.players.length; i++) {
			Player p = this.players[i];
			
			ret += String.format("%1s%1s%-20s", 
				(this.curMovePlayerInd == i?"*":" "),
				(this.lastToMoveInStagePlayerInd == i?"-":" "),
				p.name + (p.holes != null?"["+p.holes[0]+p.holes[1]+"]":""));
			
			ret += String.format("%5d %3d %3d", p.stack/bb, p.putToPotInHand/bb, p.putToPotInStage/bb);
			
			ret += "\n";
		}
		return ret;
	}

	public boolean equals(Object obj) {
		assert false;
		return false;
	}
	
	public static void main(String[] args) throws Exception {
//		testGame();
		//playGame(113286216442l);
		//playGame2();
		//playFile();
	}
}
