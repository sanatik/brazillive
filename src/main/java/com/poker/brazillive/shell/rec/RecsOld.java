package com.poker.brazillive.shell.rec;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import com.poker.brazillive.shell.util.Log;

public class RecsOld {
	public Map<String, RecBool> recBool = new LinkedHashMap<String, RecBool>();
	public Map<String, RecText> recText = new LinkedHashMap<String, RecText>();
	
	public void load() throws Exception {
		String fromFolder = "recs/"+this.getClass().getSimpleName()+".save";
		for (RecText rec: this.recText.values()) {
			rec.load(fromFolder+"/"+rec.name+".train/"+rec.name+".save");
		}
	}
	
	public void testAll(String f) throws Exception {
		BufferedImage img = ImageIO.read(new File(f));
		
		String str = "\n";
		List<String> ks = new ArrayList<String>(this.recBool.keySet()); Collections.sort(ks);
		for (String rn: ks) {
			List<Double> ratios = new ArrayList<Double>();
			boolean[] res = this.recBool.get(rn).rec(img, ratios);
			
			if (res.length == 1) str += String.format("%-15s: %-5s %1.2f\n", rn, Arrays.toString(res), ratios.get(0));
			else {
				str += rn+":\n";
				for (int i = 0; i < res.length; i++) str += String.format("\t%3d. %5s %1.2f\n", i, res[i], ratios.get(i));
			}
		}
		ks = new ArrayList<String>(this.recText.keySet()); Collections.sort(ks);
		for (String rn: ks) {
			String[] res = this.recText.get(rn).rec(img);
			if (res.length == 1) str += String.format("%-15s: %s\n", rn, Arrays.toString(res));
			else {
				str += rn+":\n";
				for (int i = 0; i < res.length; i++) str += String.format("\t%3d. %s\n", i, res[i]);
			}
		}
		str += "\n\n";
		Log.log("%s", str);
	}
}
