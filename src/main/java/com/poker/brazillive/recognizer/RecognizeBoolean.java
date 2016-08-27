/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.recognizer;

import com.poker.brazillive.shell.rec.RecUtils;
import com.poker.brazillive.shell.util.Misc;
import com.poker.brazillive.shell.util.Rect;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author boson
 */
public class RecognizeBoolean {

    private static final Logger LOGGER = Logger.getLogger(RecognizeBoolean.class.getName());

    private final Rect[] rectangles;
    private final Color color;
    private final int radius;
    private final double percent;
    public String name;

    public RecognizeBoolean(String propsFileName) throws FileNotFoundException, IOException, Exception {
        Properties p = new Properties();
        p.load(new FileInputStream(new File(propsFileName)));
        this.rectangles = RecUtils.prop2Rect(p);
        this.color = RecUtils.str2Color(p.getProperty("color"));
        this.radius = Misc.getPropInt(p, "radius");
        this.percent = Misc.getPropDouble(p, "percent");
        this.name = new File(propsFileName).getName().split("\\.")[0];
    }

    public boolean[] recognize(BufferedImage img, List<Double> ratios) throws IOException {
        Rect[] rects = this.getRects(img);
        boolean[] ret = new boolean[rects.length];
        for (int i = 0; i < rects.length; i++) {
            Rect rect = rects[i];

            int pixelCount = 0;
            int totalCount = 0;
            for (int x = rect.x1; x < rect.x2; x++) {
                for (int y = rect.y1; y < rect.y2; y++) {
                    double d = Misc.getDist(this.color, new Color(img.getRGB(x, y)));
                    if (d <= this.radius) {
                        pixelCount++;
                    }
                    totalCount++;
                }
            }
            double r = 1.0 * pixelCount / totalCount;
            ret[i] = r > this.percent;

            LOGGER.log(Level.INFO, String.format("Rect %d, perc %1.2f %s", i, 1.0 * pixelCount / totalCount, ret[i] ? "*" : ""));
            BufferedImage simg = img.getSubimage(rect.x1, rect.y1, rect.getWidth(), rect.getHeight());
            ImageIO.write(simg, "bmp", new FileOutputStream(new File(i + ".bmp")));

            if (ratios != null) {
                ratios.add(r);
            }
        }
        return ret;
    }

    private Rect[] getRects(BufferedImage img) {
        return Rect.clones(this.rectangles);
    }
}
