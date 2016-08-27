package com.poker.brazillive.shell.shell.gg;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import com.poker.brazillive.shell.rec.*;
import com.poker.brazillive.shell.util.*;

public class RecsGgLobby extends Recs {

	public RecBool boolPls;
//	public RecBool boolGreenLine;
//	public RecBool boolPlaying;
	public RecBool boolScrCanUp;
	public RecBool boolScrCanDown;
	public RecBool boolMes;
	public RecText txtLims;
	public RecText txtBal;
	
	private static final int tx = 10, ty = 205, rh = 31, th = 848-ty;
	private static final int charRadius = 150;
	
	public Rect[] getRectsCommon(BufferedImage img) throws Exception {
		
		Rect[] ret = new Rect[100]; int c = 0;
		int cury = ty;
		
		// Skip chars cut by the table top
		while (true) {
			boolean f = false;
			for (int x = 191; x < 246; x++) {
				double d = Misc.dist(new Color(255,255,255), new Color(img.getRGB(x, cury)));
				if (d < charRadius+40) {
					f = true;
					break;
				}
			}
			if (!f) break;
			cury++;
		}
		
		while (true) {
			for (int x = 191; x < 246; x++) {
				double d = Misc.dist(new Color(255,255,255), new Color(img.getRGB(x, cury)));
				if (d < charRadius+40) {
					Rect rect = new Rect(0,150,cury,130,12);
					if (!new RecBool(rect, new Color(73,32,32), 20, 0.2).rec(img)[0])
						ret[c++] = rect;
					cury += 30;
					break;
				}
			}
			cury++;
			if (cury > ty+th-11) break;
		}
		
		Rect[] ret2 = new Rect[c];
		System.arraycopy(ret, 0, ret2, 0, c);
		
//		for (Rect rect: ret2) Log.log("%s", rect);
//		Log.log("");
		
		return ret2;
	}			
	
	public RecsGgLobby() {
		
//		int x1 = 394, y1 = 9, xStep = 16;
//		Rect[] rects = new Rect[plCount*lineCount];
//		for (int yi = 0; yi < lineCount; yi++) for (int xi = 0; xi < plCount; xi++) {
//			rects[yi*plCount+xi] = new Rect(0, x1+xStep*xi, y1+(int)Math.round(lineHeight*yi), 4, 4);
//		}
//		boolPls = new RecBool(rects, new Color(217,197,121), 150, 0.10);

		boolPls = new RecBool(new Rect[0], new Color(217,197,121), 150, 0.10) {
			protected Rect[] getRects(BufferedImage img) throws Exception {
				Rect[] rects = getRectsCommon(img);
				Rect[] ret = new Rect[rects.length*6];
				for (int i = 0; i < rects.length; i++) for (int j = 0; j < 6; j++) {
					ret[i*6+j] = new Rect(0, 394+j*16, rects[i].y1-2, 13, 13);
				}
				return ret;
			}
		};
		
//		boolGreenLine = new RecBool(new Rect[0], new Color(73,32,32), 25, 0.3) {
//			protected Rect[] getRects(BufferedImage img) throws Exception {
//				return RecsGgLobby.getRects(img, charRadius);
//			}
//		};

//		boolPlaying = new RecBool(new Rect[0], new Color(209,163,75), 20, 0.3) {
//			protected Rect[] getRects(BufferedImage img) throws Exception {
//				Rect[] rects = RecsGgLobby.getRects(img, charRadius);
//				for (Rect rect: rects) {
//					rect.x1 = tx;
//					rect.x2 = tx + 100;
//				}
//				return rects;
//			}
//		};
		boolScrCanUp = new RecBool(new Rect[]{new Rect(0,695,227,15,1)}, new Color(0,0,0), 100, 0.9);
		boolScrCanDown = new RecBool(new Rect[]{new Rect(0,695,825,15,1)}, new Color(0,0,0), 100, 0.9);
		boolMes = new RecBool(new Rect[]{new Rect(0,62,794,30,20)}, new Color(125,46,46), 20, 0.6);

		txtLims = new RecText(
				"lims", null, new Color(255,255,255),
				charRadius /*charRadius*/, charRadius /*spaceRadius*/,
				4 /*splitHoriz*/, 12 /*splitVert*/, 12 /*charMaxWidth*/, 11 /*charMaxHeight*/,
				1.5 /*maxDist*/, 5 /*startClustersCount*/, null /*getRectsSign*/,
				null, new RecBool[0]) {
			private static final long serialVersionUID = 1L;
			protected Rect[] getRects(BufferedImage img) throws Exception {
				return getRectsCommon(img);
			}
		};
		txtBal = new RecText(
				"bal", new Rect[]{
						new Rect(0,1004,248,110,13),
						new Rect(0,1004,312,110,13),
				}, new Color(189,148,76),
				70 /*charRadius*/, 70 /*spaceRadius*/,
				4 /*splitHoriz*/, 13 /*splitVert*/, 10 /*charMaxWidth*/, 13 /*charMaxHeight*/,
				2.0 /*maxDist*/, 12 /*startClustersCount*/, null /*getRectsSign*/,
				null, new RecBool[0]);
	}

	public static void main(String[] args) throws Exception {
		ImageIO.setUseCache(false);
		RecsGgLobby recs = new RecsGgLobby();
//		recs.txtBal.train("imgs_spec/lobby_many", 10000); System.exit(0);
		
		recs.load();
		recs.testAll("C:/temp/l.png");
	}

}
