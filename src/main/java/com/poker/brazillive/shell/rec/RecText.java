package com.poker.brazillive.shell.rec;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.*;

import com.poker.brazillive.shell.shell.Shell;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;
import com.poker.brazillive.shell.util.*;

public class RecText implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String unrecChar = "~";
    public static final double trainShare = 1;

    public Rect[] rects;
    protected Color charColor;
    protected int charRadius;
    private int spaceRadius;
    private int splitHoriz;
    private int splitVert;
    protected int charMinWidth = 0;
    private int charMaxWidth;
    protected int charMaxHeight;
    public String name;
    private double maxDist;
    private int startClustersCount;
    private RecBool[] useRect;
    private RecBool[] dontUseRect;
    private String getRectsSign;

    public RecText(String name, Rect[] rects, Color charColor, int charRadius, int spaceRadius,
            int splitHoriz, int splitVert, int charMaxWidth, int charMaxHeight,
            double maxDist, int startClustersCount, String getRectsSign,
            RecBool[] useRects, RecBool[] dontUseRect) {
        this.name = name;
        this.rects = rects;
        this.charColor = charColor;
        this.charRadius = charRadius;
        this.spaceRadius = spaceRadius;
        this.splitHoriz = splitHoriz;
        this.splitVert = splitVert;
        this.charMaxWidth = charMaxWidth;
        this.charMaxHeight = charMaxHeight;
        this.maxDist = maxDist;
        this.startClustersCount = startClustersCount;
        this.getRectsSign = getRectsSign;

        this.useRect = useRects;
        this.dontUseRect = dontUseRect;
    }

    public RecText(String propFile) throws Exception {
        Properties p = new Properties();
        p.load(new FileInputStream(new File(propFile)));

        this.rects = RecUtils.prop2Rect(p);
        this.charColor = RecUtils.str2Color(p.getProperty("char.color"));
        this.charRadius = Misc.getPropInt(p, "char.radius");
        this.spaceRadius = Misc.getPropInt(p, "space.radius");
        this.splitHoriz = Misc.getPropInt(p, "split.horiz");
        this.splitVert = Misc.getPropInt(p, "split.vert");
        this.charMaxWidth = Misc.getPropInt(p, "char.max.width");
        this.charMaxHeight = Misc.getPropInt(p, "char.max.height");
        this.maxDist = Misc.getPropDouble(p, "max.dist");
        this.startClustersCount = Misc.getPropInt(p, "start.clusters.count");

        this.name = new File(propFile).getName().split("\\.")[0];
        this.getRectsSign = p.getProperty("get.rects.sign");

        if (p.containsKey("use.rect")) {
            String[] sr = p.getProperty("use.rect").split(";");
            this.useRect = new RecBool[sr.length];
            for (int i = 0; i < sr.length; i++) {
                this.useRect[i] = new RecBool(sr[i]);
            }
        }
        if (p.containsKey("dont.use.rect")) {
            String[] sr = p.getProperty("dont.use.rect").split(";");
            this.dontUseRect = new RecBool[sr.length];
            for (int i = 0; i < sr.length; i++) {
                this.dontUseRect[i] = new RecBool(sr[i]);
            }
        }
        if (p.containsKey("char.min.width")) {
            this.charMinWidth = Misc.getPropInt(p, "char.min.width");
        }
    }

    protected Rect[] getRects(BufferedImage img) throws Exception {
        if (this.getRectsSign == null) {
            return this.getRectsCfg(img);
        } else if (this.getRectsSign.equals("dlr2.holes")) {
            return this.getRectsDlr2Holes(img);
        } else if (this.getRectsSign.equals("dlr2.stack")) {
            return this.getRectsDlr2Stack(img);
        }
        assert false;
        return null;
    }

    protected Rect[] getRectsCfg(BufferedImage img) throws Exception {
        Rect[] ret = Rect.clones(this.rects);
        boolean[] use = this.getUse(img);
        assert ret.length == use.length;
        for (int i = 0; i < ret.length; i++) {
            if (!use[i]) {
                ret[i] = null;
            }
        }
        return ret;
    }
    private static Color cardColor = new Color(230, 255, 255);
    private static int cardColorRadius = 30;

    protected Rect[] getRectsDlr2Holes(BufferedImage img) throws Exception {
        Rect[] ret = this.getRectsCfg(img);

        for (int i = 0; i < ret.length; i++) {
            Rect rect = ret[i];
            if (rect == null) {
                ret[i] = null;
                continue;
            }
            int topEdgeY = -1;
            for (int y = rect.y1; y < rect.y2; y++) {
                Color col = null;
                boolean sameCol = true;
                for (int x = rect.x1 + 2; x < rect.x2 - 2; x++) {
                    Color pc = new Color(img.getRGB(x, y));
                    if (col == null) {
                        col = pc;
                    }
                    if (col.getRGB() != pc.getRGB()) {
                        sameCol = false;
                        break;
                    }
                }
                if (sameCol && Misc.dist(col, cardColor) <= cardColorRadius) {
                    topEdgeY = y;
                    break;
                }
            }
            if (topEdgeY == -1) {
                continue;
            }

            for (int y = topEdgeY + 3; y < topEdgeY + 10; y++) {
                boolean found = false;
                for (int x = rect.x1; x < rect.x1 + 18; x++) {
                    Color pc = new Color(img.getRGB(x, y));
                    if (Misc.dist(pc, this.charColor) <= this.charRadius) {
                        rect.y1 = y;
                        rect.y2 = y + this.charMaxHeight;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }

        return ret;
    }

    protected Rect[] getRectsDlr2Holes_old(BufferedImage img) throws Exception {
        Rect[] ret = this.getRectsCfg(img);

        for (int i = 0; i < ret.length; i++) {
            Rect rect = ret[i];
            if (rect == null) {
                ret[i] = null;
                continue;
            }
            int topEdgeY = -1;
            for (int y = rect.y1; y < rect.y2; y++) {
                Color pc = new Color(img.getRGB((rect.x1 + rect.x2) / 2, y));
                if (Misc.dist(pc, cardColor) <= cardColorRadius) {
                    topEdgeY = y;
                    break;
                }
            }
            if (topEdgeY == -1) {
                continue;
            }

            for (int y = topEdgeY + 5; y < topEdgeY + 10; y++) {
                boolean found = false;
                for (int x = rect.x1; x < rect.x1 + 18; x++) {
                    Color pc = new Color(img.getRGB(x, y));
                    if (Misc.dist(pc, this.charColor) <= this.charRadius) {
                        rect.y1 = y;
                        rect.y2 = y + this.charMaxHeight;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }

        return ret;
    }

    protected Rect[] getRectsDlr2Stack(BufferedImage img) throws Exception {
        Rect[] ret = Rect.clones(this.rects);

        boolean[] use = this.getUse(img);
        /*RecTrueFalse recLight = this.dontUseRect[0];
		assert recLight.name.equals("RecIsLight");
		if (!recLight.rec(img)[0])*/ use[0] = true;
        for (int i = 0; i < ret.length; i++) {
            if (!use[i]) {
                ret[i] = null;
            }
        }
        if (ret[0] == null) {
            return ret;
        }

        int y1 = -1, y2 = -1;
        for (int y = ret[0].y1 - 25; y < ret[0].y2 + 5; y++) {
            int xc = ret[0].x1 + ret[0].getWidth() / 2;
            for (int x = xc - 8; x < xc + 8; x++) {
                Color pc = new Color(img.getRGB(x, y));
                if (Misc.dist(pc, this.charColor) <= this.charRadius) {
                    if (y1 == -1) {
                        y1 = y;
                    }
                    y2 = y;
                }
            }
        }

        if (y1 == -1 || y2 == -1) {
            ret[0] = null;
        } else if (y2 - y1 + 1 != ret[0].getHeight()) {
            ret[0] = null;
        } else {
            ret[0].y1 = y1;
            ret[0].y2 = y2 + 1;
        }

        return ret;
    }

    private Rect[] splitChars(BufferedImage img, Rect rect) throws Exception {
        Rect[] ret = new Rect[2];
        int xFrom = rect.getWidth() / 2 - 1 + rect.x1;
        int xTo = rect.getWidth() % 2 == 0 ? xFrom + 2 : xFrom + 1;

        int minX = -1;
        int minC = Integer.MAX_VALUE;
        for (int x = xFrom; x < xTo; x++) {
            int c = 0;
            for (int y = rect.y1; y < rect.y2; y++) {
                Color curCol = new Color(img.getRGB(x, y));
                double dist = Misc.getDist(this.charColor, curCol);
                if (dist < this.charRadius) {
                    c++;
                }
            }
            if (c < minC) {
                minC = c;
                minX = x;
            }
        }

        ret[0] = new Rect(rect.x1, rect.y1, minX, rect.y2);
        ret[1] = new Rect(minX + 1, rect.y1, rect.x2, rect.y2);

        return ret;
    }

    private Rect[] sepChars(BufferedImage img, Rect rect) throws Exception {
        Rect[] ret = new Rect[100];
        int c = 0;
        int x = rect.x1;

        Rect curRect = null;
        while (x < rect.x2) {
            double minDist = Double.POSITIVE_INFINITY;
            for (int y = rect.y1; y < rect.y2; y++) {
                Color curCol = new Color(img.getRGB(x, y));
                double dist = Misc.getDist(this.charColor, curCol);
                if (minDist > dist) {
                    minDist = dist;
                }
            }
//			Log.logf("x=%3d, minDist = %5.1f", x, minDist);

            if (minDist > this.spaceRadius && curRect != null && x - curRect.x1 >= this.charMinWidth) {
                curRect.x2 = x;
                if (curRect.getWidth() > this.charMaxWidth) {
                    Rect[] split = this.splitChars(img, curRect);
                    ret[c++] = split[0];
                    ret[c++] = split[1];
                } else {
                    ret[c++] = curRect;
                }
                curRect = null;
            } else if (minDist < this.spaceRadius) {
                if (curRect == null) {
                    curRect = new Rect(x, rect.y1, 0, rect.y2);
                }
            }
            x++;
        }
        if (curRect != null) {
            curRect.x2 = x;
            ret[c++] = curRect;
        }

        Rect[] ret2 = new Rect[c];
        System.arraycopy(ret, 0, ret2, 0, c);
        return ret2;
    }

    private void trim(BufferedImage img, Rect rect) {
        // Cut left blank space
        int x = rect.x1;
        do {
            int cc = 0;
            for (int y = rect.y1; y < rect.y2; y++) {
                Color curCol = new Color(img.getRGB(x, y));
                double d = Misc.getDist(curCol, this.charColor);
                if (d < this.charRadius) {
                    cc++;
                }
            }
            if (cc != 0) {
                break;
            }
            x++;
        } while (x < rect.x2);
        rect.x1 = x;

        // Cut right blank space
        x = rect.x2 - 1;
        do {
            int cc = 0;
            for (int y = rect.y1; y < rect.y2; y++) {
                Color curCol = new Color(img.getRGB(x, y));
                double d = Misc.getDist(curCol, this.charColor);
                if (d < this.charRadius) {
                    cc++;
                }
            }
            if (cc != 0) {
                break;
            }
            x--;
        } while (x >= 0);
        rect.x2 = x + 1;
    }

    private boolean[] getUse(BufferedImage img) throws Exception {
        boolean[] use = new boolean[this.rects.length];
        Arrays.fill(use, true);
        if (this.useRect != null) {
            for (int i = 0; i < use.length; i++) {
                boolean u = false;
                for (int j = 0; j < this.useRect.length; j++) {
                    if (this.useRect[j].rec(img)[i]) {
                        u = true;
                    }
                }
                use[i] = u;
            }
        }
        if (this.dontUseRect != null) {
            for (int i = 0; i < use.length; i++) {
                boolean du = false;
                for (int j = 0; j < this.dontUseRect.length; j++) {
                    if (this.dontUseRect[j].rec(img)[i]) {
                        du = true;
                    }
                }
                if (du) {
                    use[i] = false;
                }
            }
        }
        assert use.length == this.rects.length;
        return use;
    }

    private List<double[]> cents;
    private List<String> out;

    public void train(String ssFolder, int maxFiles) throws Exception {
        String trFolder = this.getTrainFolder();

        if (new File(trFolder + "/out.txt").exists()) {
            List<String> outStrs = FUtil.fRead(trFolder + "/out.txt");
            String outStr = "";
            this.out = new ArrayList<String>();
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(this.getTrainFolder() + "/cents.obj")));
            this.cents = (List<double[]>) ois.readObject();
            ois.close();

            for (String s : outStrs) {
                outStr += s.replaceAll("\\s+", "");
            }
            for (int i = 0; i < outStr.length(); i++) {
                String ch = outStr.substring(i, i + 1);
                out.add(ch);
            }
            assert out.size() == cents.size();
            this.save(this.getTrainFolder() + "/" + this.name + ".save");

            return;
        }

        List<File> imgFiles = FUtil.searchFiles(ssFolder, null);
        Collections.sort(imgFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        List<BufferedImage> charImgs = new ArrayList<BufferedImage>();
        List<String> picNames = new ArrayList<String>();
        List<double[]> points = new ArrayList<double[]>();

        (new File(trFolder + "/char_img")).mkdirs();

        String mainFolder = String.format("%s/mains", trFolder);
        new File(mainFolder).mkdirs();
        new File(trFolder + "/charImgs").mkdirs();

        int fc = 0, cc = 0;
        ProcTimer t = new ProcTimer("Separating chars", imgFiles.size() < maxFiles ? imgFiles.size() : maxFiles);
        t.logStartTime();

        long lt = Misc.getTime();
        StringBuilder charStr = new StringBuilder("");

        for (File imgFile : imgFiles) {
            t.logTime(fc);
            if (fc > maxFiles) {
                break;
            }

            BufferedImage img = null;
            try {
                img = ImageIO.read(new FileInputStream(imgFile));
            } catch (Exception e) {
                Log.log("WARNING! Cannot read image: %s", imgFile.getName());
                continue;
            }
            //if (img.getWidth() != 1000 || img.getHeight() != 750) continue; // Do not commit this
            fc++;

            Rect[] rects = this.getRects(img);

            for (int i = 0; i < rects.length; i++) {
                if (rects[i] == null) {
                    continue;
                }
                Rect[] chars = this.sepChars(img, rects[i]);

                for (int j = 0; j < chars.length; j++) {
                    BufferedImage charImg = img.getSubimage(chars[j].x1, chars[j].y1, chars[j].getWidth(), chars[j].getHeight());
                    charStr.append(String.format("%s, rect %d, char %d\n%s", imgFile.getName(), i, j, img2str(charImg, this.charColor, this.charRadius)));
                    double[] point = this.charImg2Inp(charImg);

                    boolean exist = false;
//					for (BufferedImage im: charImgs) {
//						if (Misc.compareImg(im, charImg)) {
                    for (double[] p : points) {
                        if (Arrays.equals(p, point)) {
                            exist = true;
                            break;
                        }
                    }

                    if (exist) {
                        continue;
                    }
                    charImgs.add(charImg);

                    if (Misc.getTime() - lt > 5000) {
                        lt = Misc.getTime();
                        Log.log("Char imgs count: %d", charImgs.size());
                    }

                    String charPicName = String.format("%04d", cc) + "." + imgFile.getName() + ".p" + i + ".ch" + j + ".png";

//					ImageIO.write(charImg, "png", new File(String.format("%s/charImgs/%06d.png", trFolder, points.size())));
                    picNames.add(charPicName);
                    points.add(point);
                    cc++;
                }
            }
        }
        t.logFinishTime();
        FUtil.fPut(trFolder + "/chars.txt", charStr.toString());

        KMeans.Res kres = KMeans.kMeansMaxDist(points, this.maxDist, this.startClustersCount);

        List<double[]> wrPoints = new ArrayList<double[]>();
        for (int cn = 0; cn < kres.clusters.size(); cn++) {
            if (kres.clusters.get(cn).size() == 0) {
                continue;
            }

//			Files.copy(Paths.get(String.format("%s/charImgs/%06d.png", trFolder, kres.clusters.get(cn).get(0))),
//					Paths.get(String.format("%s/%03d.png", mainFolder, cn)), REPLACE_EXISTING);
            BufferedImage mainImg = charImgs.get(kres.clusters.get(cn).get(0));
            ImageIO.write(mainImg, "png", new File(String.format("%s/%03d.png", mainFolder, cn)));

            for (int pn : kres.clusters.get(cn)) {
                String charPicName = picNames.get(pn);
                BufferedImage charImg = charImgs.get(pn);
                String folder = String.format("%s/char_img/c.%03d", trFolder, cn);
                new File(folder).mkdirs();

                double[] point = points.get(pn);
                boolean exists = false;
                for (double[] p : wrPoints) {
                    if (Arrays.equals(p, point)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
//					Files.move(Paths.get(String.format("%s/charImgs/%06d.png", trFolder, pn)),
//							Paths.get(folder+"/"+charPicName), REPLACE_EXISTING);
                    ImageIO.write(charImg, "png", new File(folder + "/" + charPicName));
                    wrPoints.add(point);
                }
            }
        }

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(trFolder + "/cents.obj")));
        oos.writeObject(kres.cents);
        oos.close();

        t.logFinishTime();
    }

    public double[] charImg2Inp(BufferedImage charImg, int splitHoriz, int splitVert, int charRadius) throws Exception {

        Color bgCol = Misc.getBgColor(this.charColor);
        int w = this.charMaxWidth, h = this.charMaxHeight;

        int[] xGrid = Misc.divInt(w, splitHoriz);
        int[] yGrid = Misc.divInt(h, splitVert);
        //Log.logf("xGrid=%s yGrid=%s", Arrays.toString(xGrid), Arrays.toString(yGrid));
        double[] ret = new double[(xGrid.length - 1) * (yGrid.length - 1)];

        int c = 0;
        for (int i = 1; i < xGrid.length; i++) {
            for (int j = 1; j < yGrid.length; j++) {
                int xFrom = xGrid[i - 1], xTo = xGrid[i];
                int yFrom = yGrid[j - 1], yTo = yGrid[j];

                int dc = 0;
                double distSum = 0;
                int colCount = 0;
                for (int x = xFrom; x < xTo; x++) {
                    for (int y = yFrom; y < yTo; y++) {
                        Color pixCol = null;
                        if (x < charImg.getWidth() && y < charImg.getHeight()) {
                            pixCol = new Color(charImg.getRGB(x, y));
                        } else {
                            pixCol = bgCol;
                        }

                        double dist = Misc.getDist(this.charColor, pixCol);
                        distSum += dist;
                        dc++;
                        if (dist < charRadius) {
                            colCount++;
                        }
                    }
                }
                //ret[c++] = distSum/dc;
                ret[c++] = colCount;

            }
        }
        assert c == ret.length;

//		printImg(charImg, this.charColor, this.charRadius);
        return ret;
    }

    public double[] charImg2Inp(BufferedImage charImg) throws Exception {
        return this.charImg2Inp(charImg, this.splitHoriz, this.splitVert, this.charRadius);
    }

    private String getTrainFolder() {
        return this.name + ".train";
    }

    private String recChar(BufferedImage charImg) throws Exception {
        double[] imgPoint = this.charImg2Inp(charImg);

        double minDist = Double.POSITIVE_INFINITY;
        int minDistNum = -1;
        for (int i = 0; i < this.cents.size(); i++) {
            double d = Misc.dist(this.cents.get(i), imgPoint);
            if (d < minDist) {
                minDist = d;
                minDistNum = i;
            }
        }
        return this.out.get(minDistNum);
    }

    public String[] rec(BufferedImage img) throws Exception {

        Rect[] rects = this.getRects(img);
        String[] ret = new String[rects.length];

        for (int i = 0; i < ret.length; i++) {

            Rect rect = rects[i];
            if (rect == null) {
                ret[i] = "";
                continue;
            }
            Rect[] charRects = this.sepChars(img, rect);
            String str = "";
            for (int j = 0; j < charRects.length; j++) {
                Rect charRect = charRects[j];
                BufferedImage charImg = img.getSubimage(charRect.x1, charRect.y1, charRect.getWidth(), charRect.getHeight());
                String pred = this.recChar(charImg);
                str += pred;
//				printImg(charImg, this.charColor, this.charRadius);
//				ImageIO.write(img.getSubimage(charRect.x1, charRect.y1, charRect.getWidth(), charRect.getHeight()), "bmp", new FileOutputStream(new File(String.format("%d.%02d.bmp", i, j))));
            }
            ret[i] = str;
        }
        return ret;
    }

    public static String img2str(BufferedImage img, Color col, int radius) {
        String ret = "";
        Color bgCol = Misc.getBgColor(col);

        for (int y = 0; y < img.getHeight(); y++) {
            ret += "|";
            for (int x = 0; x < img.getWidth(); x++) {
                Color pixCol = null;
                if (x < img.getWidth() && y < img.getHeight()) {
                    pixCol = new Color(img.getRGB(x, y));
                } else {
                    pixCol = bgCol;
                }
                double dist = Misc.getDist(col, pixCol);
                if (dist < radius) {
                    ret += "*";
                } else {
                    ret += " ";
                }
            }
            ret += "|\n";
        }
        ret += "\n";
        return ret;
    }

    public static void printImg(BufferedImage img, Color col, int radius) {
        System.out.print(img2str(img, col, radius));
    }

    public void save(String toFile) throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(toFile)));
        oos.writeObject(this.cents);
        oos.writeObject(this.out);
        oos.close();
    }

    public void load(String fromFile) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(fromFile)));
        this.cents = (List<double[]>) ois.readObject();
        this.out = (List<String>) ois.readObject();
        ois.close();
    }

    public static void testHoles() throws Exception {
        File[] fs = FUtil.dRead("imgs_holes");

        RecText recVal = new RecText("rec.cpa/RecHolesVal.prop");
        recVal.load("rec.cpa/RecHolesVal.train/RecHolesVal.save");
        RecText recValB = new RecText("rec.cpa/RecHolesValB.prop");
        recValB.load("rec.cpa/RecHolesValB.train/RecHolesValB.save");

        RecText recSuit = new RecText("rec.cpa/RecHolesSuit.prop");
        recSuit.load("rec.cpa/RecHolesSuit.train/RecHolesSuit.save");
        RecText recSuitB = new RecText("rec.cpa/RecHolesSuitB.prop");
        recSuitB.load("rec.cpa/RecHolesSuitB.train/RecHolesSuitB.save");

        for (File f : fs) {
            BufferedImage img = ImageIO.read(f);

            String[] recValRes = recVal.rec(img);
            String[] recValBRes = recValB.rec(img);
            String[] recSuitRes = recSuit.rec(img);
            String[] recSuitBRes = recSuitB.rec(img);

            String hs = "";
            for (int i = 0; i < 6; i++) {
                String v1 = recValRes[i * 2];
                if (v1.equals("")) {
                    v1 = recValBRes[i * 2];
                }
                String v2 = recValRes[i * 2 + 1];
                if (v2.equals("")) {
                    v2 = recValBRes[i * 2 + 1];
                }

                String s1 = recSuitRes[i * 2];
                if (s1.equals("")) {
                    s1 = recSuitBRes[i * 2];
                }
                String s2 = recSuitRes[i * 2 + 1];
                if (s2.equals("")) {
                    s2 = recSuitBRes[i * 2 + 1];
                }

                hs += String.format("%d:%4s ", i, v1 + s1 + v2 + s2);
            }
            Log.log("%s\n%s", f.getName(), hs);
        }
    }

    public static void test() throws Exception {
        String recName = "RecStack";
        RecText rec = new RecText("rec/props/RecBet.prop");
        rec.load("RecBet.train/RecBet.save");
        BufferedImage img = ImageIO.read(new File("test/imgs/20160802/220145_0x008706D8.bmp"));

        Log.log("%s", Arrays.toString(rec.rec(img)));
    }

    public static void train(String[] args) throws Exception {
        String recsClass = args[0];
        String recName = args[1];
        String imgFolder = args[2];
        int maxFiles = Integer.MAX_VALUE;
        if (args.length > 3) {
            maxFiles = Integer.parseInt(args[3]);
        }

        Shell.cfg = new Properties();
        Shell.cfg.load(new FileInputStream(new File("shell.cfg")));

        RecsOld recs = (RecsOld) Class.forName(recsClass).newInstance();
        RecText rec = recs.recText.get(recName);
        rec.train(imgFolder, maxFiles);
    }

    public static void main(String[] args) throws Exception {
//		train(args); System.exit(0);
//		test(); System.exit(0);
//		testHoles(); System.exit(0);

        String propFile = "rec/props/RecBet.prop";
        String act = "train";
        String imgFolder = "test/imgs/20160802";
        int maxFiles = 100;

        RecText rec = new RecText(propFile);

        if (act.equals("train")) {
            rec.train(imgFolder, maxFiles);
        } else {
            assert false;
        }
    }
}
