package com.poker.brazillive.shell.game;

import java.io.*;
import java.nio.*;
import java.util.*;
import com.poker.brazillive.shell.util.*;

public class Player implements Cloneable, Serializable {
	public String name;
	public int stack;
	public int won = 0;
	public int putToPotInStage;
	public int putToPotInHand;
	//public int putGuest;
	public Card[] holes = null;
	public int prevAction = -1;
	public int prevPrevAction = -1;
	public int turnCountInStage = 0;
	public int putCount = 0;
	public boolean mucked = false;
	
	public int[] betOrRaiseCount = new int[Game.STAGE_LAST];
	public int[] raiseCount = new int[Game.STAGE_LAST];
	public int[] called = new int[Game.STAGE_LAST];
	public double[] calledEq = new double[Game.STAGE_LAST];
	public int[] raised = new int[Game.STAGE_LAST];
	public double[] raisedPot = new double[Game.STAGE_LAST];
	
	public Player() {}
	
	public Player(String name,int stack) {
		this.name = name;
		this.stack = stack;
	}
	private Player(
			String name,
			int stack,
			int won,
			int putToPotInStage,
			int putToPotInHand,
			Card[] holes,
			int prevAction,
			int prevPrevAction,
			int turnCountInStage,
			int putCount,
			boolean mucked
		) {
		this(name,stack);
		this.putToPotInStage = putToPotInStage;
		this.putToPotInHand = putToPotInHand;
		this.holes = (holes == null ? null : holes.clone());
		this.prevAction = prevAction;
		this.prevPrevAction = prevPrevAction;
		this.turnCountInStage = turnCountInStage;
		this.putCount = putCount;
		this.mucked = mucked;
	}
	public Player clone() {
		Player p = new Player(this.name,this.stack,this.won,this.putToPotInStage,this.putToPotInHand, this.holes, this.prevAction, this.prevPrevAction, this.turnCountInStage, this.putCount, this.mucked);

		System.arraycopy(this.betOrRaiseCount, 0, p.betOrRaiseCount, 0, this.betOrRaiseCount.length);
		System.arraycopy(this.raiseCount, 0, p.raiseCount, 0, this.raiseCount.length);
		System.arraycopy(this.called, 0, p.called, 0, this.called.length);
		System.arraycopy(this.calledEq, 0, p.calledEq, 0, this.calledEq.length);
		System.arraycopy(this.raised, 0, p.raised, 0, this.raised.length);
		System.arraycopy(this.raisedPot, 0, p.raisedPot, 0, this.raisedPot.length);

		return p;
	}
	
	public String toString() {
		return this.name+" "+this.stack;
	}
}
