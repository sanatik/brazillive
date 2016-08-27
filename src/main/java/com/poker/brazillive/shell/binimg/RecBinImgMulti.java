package com.poker.brazillive.shell.binimg;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;


import com.poker.brazillive.shell.rec.*;
import com.poker.brazillive.shell.shell.Shell;
import com.poker.brazillive.shell.util.*;
import com.poker.brazillive.shell.util.Rect;

public class RecBinImgMulti {

	private static RecBinImgMulti instForNames;
	synchronized public static RecBinImgMulti getForNames(Properties cfg) throws Exception {
		if (instForNames == null) {
			instForNames = new RecBinImgMulti(Shell.uname+"/RecBinImg_names",
					new Rect[]{
							new Rect(0,444,543,120,13),	
							new Rect(0,47,451,120,13),	
							new Rect(0,25,267,120,13),	
							new Rect(0,437,124,120,13),	
							new Rect(0,853,267,120,13),	
							new Rect(0,841,451,120,13),	
						},
					2, 2,
					new Color(255,255,255), 200,
					new RecBool[]{
						new RecBool("rec.dlr/RecIsName.prop"),
					},
					new RecBool[]{
						new RecBool("rec.dlr/RecIsSeatAvailable.prop"),
					},
					cfg.getProperty("captcha.server").split(":")[0],
					Integer.parseInt(cfg.getProperty("captcha.server").split(":")[1]),
					0.97
				);
		}
		return instForNames;
	}
	synchronized public static RecBinImgMulti getForNames2(Properties cfg) throws Exception {
		if (instForNames == null) {
			instForNames = new RecBinImgMulti(Shell.uname+"/RecBinImg_names2.2",
					new Rect[]{
//							new Rect(0,450,536,110,10),
//							new Rect(0,63,456,110,10),
//							new Rect(0,82,254,110,10),
//							new Rect(0,450,156,110,10),
//							new Rect(0,821,254,110,10),
//							new Rect(0,836,456,110,10),
							
//							new Rect(0,452,536,94,10),
							new Rect(0,0,0,0,0), // Zero rect - not to send to img server
//							new Rect(0,65,456,94,10), // repeated 1 and 2 because we take hero name from cfg
							new Rect(0,65,456,94,10),
							new Rect(0,84,254,94,10),
							new Rect(0,452,156,94,10),
							new Rect(0,820,254,94,10),
							new Rect(0,838,456,94,10),
						},
					2, 2,
					new Color(255,255,255), 200,
					new RecBool[]{
						new RecBool("rec.dlr2/RecIsName.prop"),
					},
					new RecBool[]{
						new RecBool("rec.dlr2/RecIsSeatAvailable.prop"),
					},
					cfg.getProperty("captcha.server").split(":")[0],
					Integer.parseInt(cfg.getProperty("captcha.server").split(":")[1]),
					0.97
				);
		}
		return instForNames;
	}
	
	private int gapHoriz;
	private int gapVert;
	private Rect[] rects;
	private RecBool[] recsUse;
	private RecBool[] recsNotUse;
	
	private RecBinImg recBinImg;
	
	public RecBinImgMulti(String rootFolder, Rect[] rects, int gapHoriz, int gapVert, Color col, int radius, RecBool[] recsUse, RecBool[] recsNotUse, String imgServerHost, int imgServerPort, double matchPrec) throws Exception {
		this.gapHoriz = gapHoriz;
		this.gapVert = gapVert;
		this.rects = rects;
		this.recsUse = recsUse;
		this.recsNotUse = recsNotUse;
		
		this.recBinImg = new RecBinImg(rootFolder, col, radius, imgServerHost, imgServerPort, matchPrec);
	}
	
	protected Rect[] getRects(BufferedImage img) throws Exception {
		return Rect.clones(this.rects);
	}
	
	public String[] rec(BufferedImage img) throws Exception {
		Rect[] rects = this.getRects(img);
		String[] ret = new String[rects.length];
		
		boolean[][] use = null;
		if (this.recsUse != null) use = new boolean[this.recsUse.length][];
		boolean[][] notUse = null;
		if (this.recsNotUse != null) notUse = new boolean[this.recsNotUse.length][];
		
		if (use != null) for (int i = 0; i < this.recsUse.length; i++) {
			use[i] = this.recsUse[i].rec(img);
			assert use[i].length == ret.length;
		}
		if (notUse != null) for (int i = 0; i < this.recsNotUse.length; i++) {
			notUse[i] = this.recsNotUse[i].rec(img);
			assert notUse[i].length == ret.length;
		}
		
		for (int i = 0; i < rects.length; i++) {
			Rect r = rects[i];
			boolean skip = false;
			if (r == null) skip = true;
			if (use != null) for (int j = 0; j < use.length; j++) if (!use[j][i]) skip = true;
			if (notUse != null) for (int j = 0; j < notUse.length; j++) if (notUse[j][i]) skip = true;
			if (skip) continue;
			
			
			if (r.getWidth()*r.getHeight() == 0) {
				ret[i] = "";
			} else {
				
				BufferedImage imgRec = img.getSubimage(
						Math.max(r.x1-this.gapHoriz, 0),
						Math.max(r.y1-this.gapVert, 0),
						Math.min(r.getWidth()+2*this.gapHoriz, img.getWidth()-1),
						Math.min(r.getHeight()+2*this.gapVert, img.getHeight()-1)
					);
				BufferedImage imgBin = img.getSubimage(r.x1, r.y1, r.getWidth(), r.getHeight());
				
				ret[i] = this.recBinImg.rec(imgRec, imgBin);
			}
		}
		
		return ret;
	}

	public static void main(String[] args) throws Exception {
		String f = "imgs_spec2/bet/0.png";
		Properties cfg = new Properties();
		cfg.load(new FileInputStream("shell.cfg"));

		RecBinImgMulti rec = RecBinImgMulti.getForNames2(cfg);
		while (true) {
			String[] res = rec.rec(ImageIO.read(new File(f)));
			Log.log("Result:\n- %s", Arrays.toString(res).replaceAll(", ", "\n- ").replaceAll("[\\[\\]]", ""));
			Thread.sleep(2000);
		}
	}

}
