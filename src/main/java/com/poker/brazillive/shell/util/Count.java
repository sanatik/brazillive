package com.poker.brazillive.shell.util;

import java.io.*;

public class Count implements Serializable {
	private static final long serialVersionUID = 1;
	private int c = 0;
	
	public Count() {
		this(0);
	}
	public Count(int initVal) {
		this.c = initVal;
	}
	
	synchronized public void add(int a) {
		this.c += a;
	}
	synchronized public int inc() {
		this.c++;
		return this.c;
	}
	synchronized public void dec() {
		this.c--;
	}
	public int getVal() {
		return this.c;
	}
	public String toString() {
		return ""+this.c;
	}
}
