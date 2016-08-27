package com.poker.brazillive.shell.rec;

import java.awt.Color;
import java.awt.image.*;

import javax.imageio.*;

import java.io.*;
import java.util.*;

import com.poker.brazillive.shell.util.*;

public class RecBool {

    private Rect[] rects;
    private Color col;
    private int radius;
    private double perc;
    public String name;

    public RecBool(Rect[] rects, Color col, int radius, double perc) {
        this.rects = rects;
        this.col = col;
        this.radius = radius;
        this.perc = perc;
    }

    public RecBool(Rect rect, Color col, int radius, double perc) {
        this(new Rect[]{rect}, col, radius, perc);
    }

    public RecBool(String propsFile) throws Exception {
        Properties p = new Properties();
        p.load(new FileInputStream(new File(propsFile)));

        this.rects = RecUtils.prop2Rect(p);
        this.col = RecUtils.str2Color(p.getProperty("color"));
        this.radius = Misc.getPropInt(p, "radius");
        this.perc = Misc.getPropDouble(p, "percent");
        this.name = new File(propsFile).getName().split("\\.")[0];
    }

    protected Rect[] getRects(BufferedImage img) throws Exception {
        return Rect.clones(this.rects);
    }

    public boolean[] rec(BufferedImage img) throws Exception {
        return this.rec(img, null);
    }

    public boolean[] rec(BufferedImage img, List<Double> ratios) throws Exception {
        Rect[] rects = this.getRects(img);
        boolean[] ret = new boolean[rects.length];
        for (int i = 0; i < rects.length; i++) {
            Rect rect = rects[i];

            int pixelCount = 0;
            int totalCount = 0;
            for (int x = rect.x1; x < rect.x2; x++) {
                for (int y = rect.y1; y < rect.y2; y++) {
                    double d = Misc.getDist(this.col, new Color(img.getRGB(x, y)));
                    if (d <= this.radius) {
                        pixelCount++;
                    }
                    totalCount++;
                }
            }
            double r = 1.0 * pixelCount / totalCount;
            ret[i] = r > this.perc;

            if (ratios != null) {
                ratios.add(r);
            }

//			Log.log("Rect %d, perc %1.2f %s", i, 1.0*pixelCount/totalCount, ret[i]?"*":"");
//			BufferedImage simg = img.getSubimage(rect.x1, rect.y1, rect.getWidth(), rect.getHeight());
//			ImageIO.write(simg, "bmp", new FileOutputStream(new File(i+".bmp")));
        }
        return ret;
    }

    public static void testHoles() throws Exception {
        File[] fs = FUtil.dRead("imgs_holes");
        RecBool rec = new RecBool("rec.cpa/RecIsHoles.prop");
        RecBool recB = new RecBool("rec.cpa/RecIsHolesB.prop");
        for (File f : fs) {
            BufferedImage img = ImageIO.read(f);
            Log.log("\n%s\n%s", Arrays.toString(rec.rec(img)).replace("true", "true "), Arrays.toString(recB.rec(img)).replace("true", "true "));
        }
    }

    public static void main(String[] args) throws Exception {
//		String f = "imgs/20160429_232746_0x0007084E/20160429_232811.967_0x0007084E.bmp";
//		String rn = "isLobbyText";
//		String recsClass = "rec.cpa.RecsDlr2";
//		Recs recs = (Recs)Class.forName(recsClass).newInstance();
//		BufferedImage img = ImageIO.read(new File(f));
//		RecBool rec = recs.recBool.get(rn);
//		Log.log("\n%s", Arrays.toString(rec.rec(img)));
//		System.exit(0);

        {
            String dlrNum = "2";
            //		testHoles(); System.exit(0);
            RecBool recTF = new RecBool("rec.dlr" + dlrNum + "/RecIsHolesOverBet.prop");
            BufferedImage img = ImageIO.read(new File("imgs_spec_dsa/holes_over_bet2.bmp"));
            Log.log("\n%s", Arrays.toString(recTF.rec(img)));
        }
    }

}
