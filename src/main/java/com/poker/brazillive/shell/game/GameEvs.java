package com.poker.brazillive.shell.game;

import java.io.*;

public class GameEvs implements Serializable {
	public Game game;
	public Event[] evs;
	
	public GameEvs(Game game, Event[] evs) {
		this.game = game;
		this.evs = evs;
	}
	
	public String toString() {
		return this.game.toString();
	}
}
