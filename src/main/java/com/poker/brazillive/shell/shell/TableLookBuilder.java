package com.poker.brazillive.shell.shell;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.poker.brazillive.shell.shell.gg.TableLookBuilderGg;
import com.poker.brazillive.shell.util.*;

public abstract class TableLookBuilder {
	protected Log log;
	public List<String> hhStrs;
	
	public TableLookBuilder(Log log) {
		this.log = log;
	}
	public abstract TableLook buildTableLook(WndData wndData, TableLook prevTableLook) throws Exception;

	protected String getNicksFromHh(String reSeat, String reDealt, Map<Integer,String> s2p) throws Exception {
		if (this.hhStrs == null) return null;
		
		List<String> hhs = null;
		synchronized (this.hhStrs) { hhs = new ArrayList<String>(this.hhStrs); }
		
		Pattern p = Pattern.compile(reSeat);
		boolean started = false;
		for (int i = hhs.size()-1; i >= 0; i--) {
			String s = hhs.get(i);
//			Log.log("s='%s'", s);
			Matcher m = p.matcher(s);
			if (!m.matches()) {
				if (started) break;
				continue;
			}
//			Log.log("MATCH!");
			started = true;
			s2p.put(Integer.parseInt(m.group(1)), m.group(2));
		}
		p = Pattern.compile(reDealt);
		for (int i = hhs.size()-1; i >= 0; i--) {
			Matcher m = p.matcher(hhs.get(i));
			if (m.matches()) return m.group(1);
		}
		return null;
	}
	
	public static void main(String[] args) throws Exception {
		String imgFile = "fgfg/imgs/20160704_123857_0x014F0308/20160704_132411.841_0x014F0308.bmp";
		
		Shell.cfg = new Properties();
		Shell.cfg.load(new FileInputStream("shell.cfg"));
		Shell.uname = "fgfg";
//		TableLookBuilder tlb = new TableLookBuilderDlr2(Log.defl, "hero", Shell.cfg);
		TableLookBuilder tlb = new TableLookBuilderGg(Log.defl, "heroNick", new ArrayList<String>());
		tlb.hhStrs = new ArrayList<String>();

		BufferedImage img = ImageIO.read(new File(imgFile));
		WndData wndData = new WndData();
		wndData.title = "Toyota 06 - $0.05 / $0.10";
		wndData.img = img;
		wndData.active = true;
		
		TableLook tl = tlb.buildTableLook(wndData, null);
		Log.log("%s", tl);
	}
}
