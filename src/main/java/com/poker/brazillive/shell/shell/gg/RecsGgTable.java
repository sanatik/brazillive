package com.poker.brazillive.shell.shell.gg;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import com.poker.brazillive.shell.rec.*;
import com.poker.brazillive.shell.util.*;

public class RecsGgTable extends Recs {

	public RecBool boolCard;
	public RecBool boolCardShowed;
	public RecBool boolDealer;
	public RecBool boolButtons;
	public RecBool boolInPlay;
	public RecBool boolSeatOcc;
	public RecBool boolHeroSitout;
	public RecBool boolWin;
	public RecBool boolBlueMove;
	public RecBool boolName;
	public RecBool boolComb;
	public RecBool boolEmo;
	public RecBool boolInsur;
	public RecBool boolSitout;
	public RecBool boolServerMes;

	public RecText txtStack;
	public RecText txtStackHero;
	public RecText txtBet;
	public RecText txtCard;
	public RecText txtCardShowed;
	
	public RecsGgTable() {
		Rect[] stackRects = {
				new Rect(0, 50,547,100, 13),
				new Rect(0, 70,267,100, 13),
				new Rect(0,450,187,100, 13),
				new Rect(0,830,267,100, 13),
				new Rect(0,850,547,100, 13),
		};
		
		txtStack = new RecText(
				"stack", Rect.clones(stackRects), new Color(153, 235, 211),
				80 /*charRadius*/, 15 /*spaceRadius*/,
				4 /*splitHoriz*/, 13 /*splitVert*/, 13 /*charMaxWidth*/, 13 /*charMaxHeight*/,
				2.2 /*maxDist*/, 10 /*startClustersCount*/, null /*getRectsSign*/,
				null, new RecBool[0]);

		txtStackHero = new RecText(
				"stackHero", new Rect[]{new Rect(0,450,718,100,16)}, new Color(153, 235, 211),
				40 /*charRadius*/, 15 /*spaceRadius*/,
				4 /*splitHoriz*/, 14 /*splitVert*/, 12 /*charMaxWidth*/, 16 /*charMaxHeight*/,
				1.5 /*maxDist*/, 10 /*startClustersCount*/, null /*getRectsSign*/,
				null, new RecBool[0]);

		Rect[] betRects = {
				new Rect(0,453,534, 80,12),
				new Rect(0,179,490, 80,12),
				new Rect(0,173,312, 80,12),
				new Rect(0,433,256, 80,12),
				new Rect(0,700,303,126,12),
				new Rect(0,720,490,124,12),
		};

		txtBet = new RecText(
				"bet", Rect.clones(betRects), new Color(255,255,255),
				180 /*charRadius*/, 200 /*spaceRadius*/,
				3 /*splitHoriz*/, 12 /*splitVert*/, 10 /*charMaxWidth*/, 12 /*charMaxHeight*/,
				2 /*maxDist*/, 10 /*startClustersCount*/, null /*getRectsSign*/,
				null, new RecBool[0]){

					private static final long serialVersionUID = 1L;
					protected Rect[] getRects(BufferedImage img) throws Exception {
						Rect[] ret = Rect.clones(this.rects);
						for (int i = 0; i < ret.length; i++) {
							int c1 = 0, c2 = 0;
							for (int x = ret[i].x1; x < ret[i].x2; x++) {
								Color c = new Color(img.getRGB(x, ret[i].y1-1));
								if (Misc.dist(c, this.charColor) <= this.charRadius)
									c1++;
								
								c = new Color(img.getRGB(x, ret[i].y2));
								if (Misc.dist(c, this.charColor) <= this.charRadius)
									c2++;
							}
							if (c1 > 2 || c2 > 2) ret[i] = null;
						}
						return ret;
					}
		};

		int x1 = 296, yv = 320, ys = 342, step = 85, w = 20, h = 20;
		Rect[] cardRects = new Rect[18]; int c = 0;
		
		for (int i = 0; i < 5; i++) {
			cardRects[c++] = new Rect(0, x1+i*step, yv, w, h);
			cardRects[c++] = new Rect(0, x1+i*step, ys, w, h);
		}
		
		// Hero holes
		cardRects[c++] = new Rect(0,418,577,w,h);
		cardRects[c++] = new Rect(0,418,577+(ys-yv),w,h);
		cardRects[c++] = new Rect(0,498,577,w,h);
		cardRects[c++] = new Rect(0,498,577+(ys-yv),w,h);

		// Hero holes - 1st game is different
		cardRects[c++] = new Rect(0,423,577,w,h);
		cardRects[c++] = new Rect(0,423,577+(ys-yv),w,h);
		cardRects[c++] = new Rect(0,493,577,w,h);
		cardRects[c++] = new Rect(0,493,577+(ys-yv),w,h);
		
		Rect[] boolCardRects = new Rect[(5+2+2)*2];
		assert boolCardRects.length == cardRects.length;
		for (int i = 0; i < 5+2+2; i++) {
			Point p = new Point(cardRects[i*2].x1, cardRects[i*2].y1);
			for (int j = 0; j < 2; j++)
				boolCardRects[i*2+j] = new Rect(0, p.x, p.y-3, 75, 1);
		}
		
		boolCard = new RecBool(Rect.clones(boolCardRects), new Color(255,255,255), 20, 0.93);

		txtCard = new RecText(
				"card", Rect.clones(cardRects), new Color(255/2,0,0),
				160 /*charRadius*/, 160 /*spaceRadius*/,
				4 /*splitHoriz*/, 4 /*splitVert*/, w /*charMaxWidth*/, h /*charMaxHeight*/,
				5.5 /*maxDist*/, 13+4 /*startClustersCount*/, null /*getRectsSign*/,
				new RecBool[]{boolCard}, new RecBool[0]);
		
		
		
		
		w = 18; h = 18; int shx = 70, shy = 21;
		Point[] ps = {
				new Point( 28,427),
				new Point( 54,147),
				new Point(435, 67),
				new Point(815,148),
				new Point(840,427),
		};
		Rect[] cardShowedRects = new Rect[20];
		Rect[] boolCardShowedRects = new Rect[20];
		
		for (int i = 0; i < 5; i++) {
			Point p = ps[i];
			
			cardShowedRects[i*4+0] = new Rect(0, p.x, p.y, w ,h);
			cardShowedRects[i*4+1] = new Rect(0, p.x, p.y+shy, w ,h);
			cardShowedRects[i*4+2] = new Rect(0, p.x+shx, p.y, w ,h);
			cardShowedRects[i*4+3] = new Rect(0, p.x+shx, p.y+shy, w ,h);
			
			boolCardShowedRects[i*4+0] = new Rect(0, p.x, p.y, 60, 5);
			boolCardShowedRects[i*4+1] = boolCardShowedRects[i*4+0].clone();
			boolCardShowedRects[i*4+2] = new Rect(0, p.x+shx, p.y, 60, 5);
			boolCardShowedRects[i*4+3] = boolCardShowedRects[i*4+2].clone();
		}
		boolCardShowed = new RecBool(boolCardShowedRects, new Color(255,255,255), 20, 0.7);
		txtCardShowed = new RecText(
				"cardShowed", Rect.clones(cardShowedRects), new Color(255/2,0,0),
				160 /*charRadius*/, 160 /*spaceRadius*/,
				4 /*splitHoriz*/, 4 /*splitVert*/, w /*charMaxWidth*/, h /*charMaxHeight*/,
				5.5 /*maxDist*/, 1 /*startClustersCount*/, null /*getRectsSign*/,
				new RecBool[]{boolCardShowed}, new RecBool[0]);
		
		
		
		boolDealer = new RecBool(new Rect[]{
				new Rect(0,407,521,30,25),
				new Rect(0,159,425,30,25),
				new Rect(0,228,246,30,25),
				new Rect(0,565,222,30,25),
				new Rect(0,827,299,30,25),
				new Rect(0,781,506,30,25),
			}, new Color(234,182,106), 50, 0.30);

		boolButtons = new RecBool(new Rect[]{
				new Rect(0,604,685,20,20),
				new Rect(0,669,688,20,20),
			}, new Color(151,49,49), 80, 0.5);
		
		boolInPlay = new RecBool(new Rect[]{
				new Rect(0,  0,  0,1, 1),
				new Rect(0, 46,503,5,15),
				new Rect(0, 71,223,5,15),
				new Rect(0,451,143,5,15),
				new Rect(0,833,223,5,15),
				new Rect(0,857,503,5,15),
			}, new Color(255,255,255), 20, 0.65);
		
		boolSeatOcc = new RecBool(new Rect[]{
				new Rect(0,424,739,20,3),
				new Rect(0, 38,564,20,3),
				new Rect(0, 63,284,20,3),
				new Rect(0,563,162,2,15),
				new Rect(0,906,284,20,3),
				new Rect(0,918,564,20,3),
			}, new Color(0,0,0), 20, 0.9);

		boolHeroSitout = new RecBool(new Rect[]{new Rect(0,704,708,14,14)}, new Color(233,233,233), 20, 0.9);
		
		boolWin = new RecBool(new Rect[]{
				new Rect(0,554,653,2,20),
				new Rect(0,146,493,2,20),
				new Rect(0,171,212,2,20),
				new Rect(0,551,133,2,20),
				new Rect(0,931,213,2,20),
				new Rect(0,956,493,2,20),
			}, new Color(142,99,66), 20, 0.25);

		boolBlueMove = new RecBool(new Rect[]{
				new Rect(0,479,698,40,22),
				new Rect(0, 85,530,40,22),
				new Rect(0,112,250,40,22),
				new Rect(0,490,172,40,22),
				new Rect(0,873,250,40,22),
				new Rect(0,897,530,40,22),
			}, new Color(0,191,255), 80, 0.1);

		boolName = new RecBool(new Rect[]{
				new Rect(0,479,698,40,22),
				new Rect(0, 85,530,40,22),
				new Rect(0,112,250,40,22),
				new Rect(0,490,172,40,22),
				new Rect(0,873,250,40,22),
				new Rect(0,897,530,40,22),
			}, new Color(171,179,181), 50, 0.05);

		boolComb = new RecBool(new Rect[]{
				new Rect(0,479,698,40,22),
				new Rect(0, 85,530,40,22),
				new Rect(0,112,250,40,22),
				new Rect(0,490,172,40,22),
				new Rect(0,873,250,40,22),
				new Rect(0,897,530,40,22),
			}, new Color(255,255,255), 20, 0.05);

		Point[] mids = {new Point(498,712), new Point(97,542), new Point(121,262),
				new Point(501,182), new Point(880,262), new Point(906,542)};
		Rect[] emoRects = new Rect[6];
		for (int i = 0; i < emoRects.length; i++)
			emoRects[i] = new Rect(0, mids[i].x-20, mids[i].y-20, 40, 10);
		boolEmo = new RecBool(emoRects, new Color(212,119,18), 120, 0.15);
		
		boolInsur = new RecBool(new Rect[]{new Rect(0,294,486,20,20)}, new Color(164,65,65), 50, 0.9);

		Rect[] soRects = new Rect[6];
		for (int i = 0; i < soRects.length; i++) soRects[i] = new Rect(0, mids[i].x-40, mids[i].y+6, 80, 14);
		boolSitout = new RecBool(soRects, new Color(0,188,249), 60, 0.15);

		boolServerMes = new RecBool(new Rect(0,453,313,60,20), new Color(12,31,14), 20, 0.9);

	}

	public static void main(String[] args) throws Exception {
		ImageIO.setUseCache(false);
		RecsGgTable recs = new RecsGgTable();
//		recs.txtCardShowed.train("c:/temp/img", 10000); System.exit(0);
		
		recs.load();
		recs.testAll("fgfg/imgs/20160704_082916_0x028202D2/20160704_091209.894_0x028202D2.bmp");
	}

}
