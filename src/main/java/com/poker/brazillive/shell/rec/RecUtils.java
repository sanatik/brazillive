package com.poker.brazillive.shell.rec;

import com.poker.brazillive.shell.util.*;
import java.util.*;
import java.awt.Color;

public class RecUtils {

    public static Color str2Color(String s) {
        String[] sr = s.split(",");
        assert sr.length == 3;
        int[] ret = new int[sr.length];
        for (int i = 0; i < sr.length; i++) {
            ret[i] = Integer.parseInt(sr[i].trim());
        }
        return new Color(ret[0], ret[1], ret[2]);
    }

    public static Rect[] prop2Rect(Properties p) throws Exception {
        Rect[] rects = new Rect[200];
        int maxPos = -1;
        for (String k : p.stringPropertyNames()) {
            String[] mr = Misc.reFind("^rect.(\\d+)$", k);
            if (mr == null) {
                continue;
            }
            int[] pars = Misc.str2Ints(p.getProperty(k));
            Rect rect = null;
            if (pars.length == 4) {
                rect = new Rect(pars[0], pars[1], pars[2], pars[3]);
            } else if (pars.length == 5) {
                rect = new Rect(pars[0], pars[1], pars[2], pars[3], pars[4]);
            }
            int pos = Integer.parseInt(mr[0]);
            rects[pos] = rect;
            if (maxPos < pos) {
                maxPos = pos;
            }
        }
        Rect[] ret = new Rect[maxPos + 1];
        System.arraycopy(rects, 0, ret, 0, maxPos + 1);
        return ret;
    }
}
