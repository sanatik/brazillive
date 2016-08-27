package com.poker.brazillive.shell.util;

import java.io.*;

public class Rect implements Serializable {
	private static final long serialVersionUID = 1;

	public int x1,y1,x2,y2;
	public Rect(int x1,int y1,int x2,int y2) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}
	public Rect(int fake, int x1,int y1,int w,int h) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x1+w;
		this.y2 = y1+h;
	}
	public int getWidth() {
		return x2-x1;
	}
	public int getHeight() {
		return y2-y1;
	}
	public Rect clone() {
		return new Rect(this.x1, this.y1, this.x2, this.y2);
		
	}
	public String toString() {
		return "("+x1+","+y1+")-("+x2+","+y2+") "+(x2-x1)+"x"+(y2-y1);
	}
	
	public static Rect[] clones(Rect[] rects) {
		Rect[] ret = new Rect[rects.length];
		for (int i = 0; i < ret.length; i++)
			ret[i] = rects[i].clone();
		return ret;
	}
	public static Rect[] join(Rect[]... rs) {
		int len = 0; for (Rect[] r: rs) len += r.length;
		Rect[] ret = new Rect[len];
		int pos = 0;
		for (Rect[] r: rs) {
			System.arraycopy(r, 0, ret, pos, r.length);
			pos += r.length;
		}
		return ret;
	}
	public static final int DIR_HORIZ = 0;
	public static final int DIR_VERT = 1;
	public static Rect[] period(Rect br, int n, int step, int dir) {
		Rect[] ret = new Rect[n];
		for (int i = 0; i < n; i++) {
			Rect r = br.clone();
			if (dir == DIR_HORIZ) { r.x1 += i*step; r.x2 += i*step; }
			else if (dir == DIR_VERT) { r.y1 += i*step; r.y2 += i*step; }
			else assert false;
			ret[i] = r;
		}
		return ret;
	}
}
