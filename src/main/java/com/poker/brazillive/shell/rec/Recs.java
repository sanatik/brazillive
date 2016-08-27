package com.poker.brazillive.shell.rec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import javax.imageio.ImageIO;
import com.poker.brazillive.shell.util.*;

public class Recs {
	public void load() throws Exception {
		String fromFolder = "recs/"+this.getClass().getSimpleName()+".save";
		for (Field field: this.getClass().getFields()) {
			if (field.getName().indexOf("txt") == -1) continue;
			String name = field.getName().replaceAll("^txt", "");
			name = name.substring(0, 1).toLowerCase()+name.substring(1);
			RecText rec = (RecText)field.get(this);
			if (rec == null) continue;
			rec.load(fromFolder+"/"+name+".train/"+name+".save");
		}
	}
	
	public void testAll(String f) throws Exception {
		BufferedImage img = ImageIO.read(new File(f));
		String str = "\n";
		
		for (Field field: this.getClass().getFields()) {
			if (field.getName().indexOf("bool") == -1) continue;
			RecBool rec = (RecBool)field.get(this);
			List<Double> ratios = new ArrayList<Double>();
			boolean[] res = rec.rec(img, ratios);
			if (res.length == 1) str += String.format("%-15s: %-5s %1.2f\n", field.getName(), Arrays.toString(res), ratios.get(0));
			else {
				str += field.getName()+":\n";
				for (int i = 0; i < res.length; i++) str += String.format("\t%3d. %5s %1.2f\n", i, res[i], ratios.get(i));
			}
		}

		for (Field field: this.getClass().getFields()) {
			if (field.getName().indexOf("txt") == -1) continue;
			RecText rec = (RecText)field.get(this);
			if (rec == null) continue;
			String[] res = rec.rec(img);
			if (res.length == 1) str += String.format("%-15s: %s\n", field.getName(), Arrays.toString(res));
			else {
				str += field.getName()+":\n";
				for (int i = 0; i < res.length; i++) str += String.format("\t%3d. %s\n", i, res[i]);
			}
		}

		Log.log("%s", str);
	}

	private static Rect[] rectRepeat(int x, int y, int step, String dir, int count, int w, int h) {
		Rect[] rects = new Rect[count];
		for (int i = 0; i < rects.length; i++) {
			rects[i] = new Rect(0, x, y, w, h);
			if (dir.equals("horiz")) x += step;
			else if (dir.equals("vert")) y += step;
			else assert false;
		}
		return rects;
	}
}
