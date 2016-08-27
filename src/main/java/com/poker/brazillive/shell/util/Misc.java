package com.poker.brazillive.shell.util;

import javax.imageio.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import com.poker.brazillive.shell.util.*;

import java.util.*;
import java.util.regex.*;
import java.util.zip.CRC32;
import java.text.*;

public class Misc {

    public static Random rand = new Random();

    public static double getDist(Color col1, Color col2) {
        double dist = Math.sqrt(Math.pow(col1.getRed() - col2.getRed(), 2) + Math.pow(col1.getGreen() - col2.getGreen(), 2) + Math.pow(col1.getBlue() - col2.getBlue(), 2));
        return dist;
    }

    public static Color getBgColor(Color col) {
        int r = 0;
        if (col.getRed() < 255.0 / 2) {
            r = 255;
        }
        int g = 0;
        if (col.getGreen() < 255.0 / 2) {
            g = 255;
        }
        int b = 0;
        if (col.getBlue() < 255.0 / 2) {
            b = 255;
        }

        return new Color(r, g, b);
    }

    public static boolean compareImg(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth()) {
            return false;
        }
        if (img1.getHeight() != img2.getHeight()) {
            return false;
        }
        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static double[] joinArs(double[] ar1, double[] ar2) {
        double[] ret = new double[ar1.length + ar2.length];
        System.arraycopy(ar1, 0, ret, 0, ar1.length);
        System.arraycopy(ar2, 0, ret, ar1.length, ar2.length);
        return ret;
    }

    public static Object readObj(String f) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(f)));
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    public static void writeObj(Object obj, String f) throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(f)));
        oos.writeObject(obj);
        oos.close();
    }

    public static String stacktrace2Str(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString(); // stack trace as a string
    }

    public static void sysCall(String cmd) throws Exception {
//		Log.log("cmd=%s", cmd);
        long st = Misc.getTime();
        Runtime run = Runtime.getRuntime();
        File dir = new File(new java.io.File(".").getCanonicalPath());
        Process pp = run.exec(cmd, null, dir);
        BufferedReader out = new BufferedReader(new InputStreamReader(pp.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(pp.getErrorStream()));

        String line;
        while ((line = out.readLine()) != null) {
            Log.log(line);
        }
        while ((line = err.readLine()) != null) {
            Log.log(line);
        }

        int exitVal = pp.waitFor();
        if (exitVal != 0) {
            System.out.println("Process exitValue: " + exitVal);
        }

        long t = Misc.getTime() - st;

        out.close();
        err.close();
    }

    public static void sortFiles(File[] files) {
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
    }

    public static String[] reFind(String re, String inp) throws Exception {
        Pattern p = Pattern.compile(re);
        Matcher m = p.matcher(inp);
        if (!m.find()) {
            return null;
        }
        String[] ret = new String[m.groupCount()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = m.group(i + 1);
        }
        return ret;
    }

    public static int getPropInt(Properties p, String propName) {
        return Integer.parseInt(p.getProperty(propName).trim());
    }

    public static double getPropDouble(Properties p, String propName) {
        return Double.parseDouble(p.getProperty(propName).trim());
    }

    public static boolean getPropBool(Properties p, String propName) {
        return Boolean.parseBoolean(p.getProperty(propName).trim());
    }

    public static int[] divInt(int x, int y) {
        int[] ret = new int[y + 1];
        ret[0] = 0;

        for (int i = 1; i < ret.length; i++) {
            int n = (int) Math.round(1.0 * x / y);
            ret[i] = n;
            if (i > 0) {
                ret[i] += ret[i - 1];
            }
            x -= n;
            y--;
        }
        assert y == 0;
        assert x == 0;

        return ret;
    }

    public static int[] str2Ints(String s) {
        String[] sr = s.split(",");
        int[] ret = new int[sr.length];
        for (int i = 0; i < sr.length; i++) {
            ret[i] = Integer.parseInt(sr[i].trim());
        }
        return ret;
    }

    public static boolean waitForProcess(String proc, String user, int timeout) throws Exception {
        long st = Misc.getTime();
        while (true) {
            TaskList.Task[] tasks = TaskList.getTasks(user, proc);
            if (tasks.length > 0) {
                break;
            }
            if (Misc.getTime() - st >= timeout) {
                return false;
            }
            Thread.sleep(1000);
        }
        return true;
    }

    public static long getTime() {
        return System.currentTimeMillis() /*- 1000*60*60*/;
    }

    public static boolean checkTestFile(String f) {
        if (new File(f).exists()) {
            new File(f).delete();
            return true;
        }
        return false;
    }

    public static double dist(double[] ar1, double[] ar2) {
        assert ar1.length == ar2.length;
        double sum = 0;
        for (int i = 0; i < ar1.length; i++) {
            sum += Math.pow(ar1[i] - ar2[i], 2);
        }
        sum = Math.pow(sum, 0.5);
        return sum;
    }

    public static double dist(Color c1, Color c2) {
        return Math.sqrt(
                Math.pow(c1.getRed() - c2.getRed(), 2)
                + Math.pow(c1.getGreen() - c2.getGreen(), 2)
                + Math.pow(c1.getBlue() - c2.getBlue(), 2)
        );
    }

    public static boolean debugFile(String fn) throws Exception {
        if (new File(fn).exists()) {
            Log.log("Debug file '%s' is found", fn);
            new File(fn).delete();
            return true;
        }
        return false;
    }
    // MMM dd HH:mm:ss.SSS, yyyyMMdd_HHmmss.SSS

    public static String dateFormat(long t, String f) {
        SimpleDateFormat df = new SimpleDateFormat(f);
        return df.format(t);
    }

    private static final String dfh = "MMM dd HH:mm:ss";
    private static final String dfhy = "yyyy/MM/dd HH:mm:ss";
    private static final String dfi = "yyyyMMdd_HHmmss";

    public static String dateFormatHuman(long t) {
        return dateFormat(t, dfh);
    }

    public static String dateFormatHumanY(long t) {
        return dateFormat(t, dfhy);
    }

    public static String dateFormatIntel(long t) {
        return dateFormat(t, dfi);
    }

    public static long parseDateHuman(String d) throws Exception {
        return new SimpleDateFormat(dfh).parse(d).getTime();
    }

    public static long parseDateHumanY(String d) throws Exception {
        return new SimpleDateFormat(dfhy).parse(d).getTime();
    }

    public static long parseDateIntel(String d) throws Exception {
        return new SimpleDateFormat(dfi).parse(d).getTime();
    }

    public static long getStartOfDay(long t) {
        Date date = new Date(t);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }

    public static String toLowAlphaNum(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    public static long str2crc(String s) {
        CRC32 crc = new CRC32();
        crc.update(s.getBytes());
        return crc.getValue();
    }

    public static List<Map<String, String>> parseCsv(List<String> strs) {
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        String[] hs = strs.get(0).split(",");
        for (int i = 1; i < strs.size(); i++) {
            String[] cells = strs.get(i).split(",");
            Map<String, String> l = new HashMap<String, String>();
            for (int j = 0; j < hs.length; j++) {
                String v = null;
                if (j < cells.length) {
                    v = cells[j].trim();
                    if (v.isEmpty()) {
                        v = null;
                    }
                }
                l.put(hs[j].trim(), v);
            }
            ret.add(l);
        }
        return ret;
    }

    public static List<String> reMatch(String re, String s) {
        List<String> ret = new ArrayList<String>();
        Pattern p = Pattern.compile(re);
        Matcher m = p.matcher(s);
        if (m.matches()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                ret.add(m.group(i));
            }
        }
        return ret;
    }

    public static String reMatch1(String re, String s) throws Exception {
        List<String> ms = reMatch(re, s);
        if (ms.size() == 0) {
            return null;
        }
        if (ms.size() > 1) {
            throw new Exception(String.format("More than 1 match. re='%s', str='%s'", re, s));
        }
        return ms.get(0);
    }

    public static int[] reMatchInts(String re, String s) {
        List<String> ms = reMatch(re, s);
        int[] ret = new int[ms.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = Integer.parseInt(ms.get(i));
        }
        return ret;
    }

    public static double[] reMatchDoubles(String re, String s) {
        List<String> ms = reMatch(re, s);
        double[] ret = new double[ms.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = Double.parseDouble(ms.get(i));
        }
        return ret;
    }

    public static void main(String[] args) throws Exception {
        Log.log("%s", reMatchInts("^.*maxbot=(\\d+)$", "rm maxbot=3")[0]);
    }
}
