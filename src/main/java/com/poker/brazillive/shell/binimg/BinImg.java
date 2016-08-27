package com.poker.brazillive.shell.binimg;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.*;

import com.poker.brazillive.shell.util.*;

public class BinImg implements Serializable {

    private static final long serialVersionUID = 1;

    public int[][] data;
    public double matchMin;

    public BinImg(BufferedImage img, Color col, int radius, double matchPrec) {
        this.matchMin = matchPrec;
        data = new int[img.getWidth()][img.getHeight()];
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Color c = new Color(img.getRGB(x, y));
                if (Misc.getDist(col, c) < radius) {
                    data[x][y] = 1;
                } else {
                    data[x][y] = 0;
                }
            }
        }
    }

    private boolean columnEmpty(int x) {
        for (int y = 0; y < data[x].length; y++) {
            if (data[x][y] == 1) {
                return false;
            }
        }
        return true;
    }

    public void trim() {
        int x1 = -1, x2 = -1;
        for (int x = 0; x < data.length; x++) {
            if (!columnEmpty(x)) {
                x1 = x;
                break;
            }
        }
        for (int x = data.length - 1; x >= 0; x--) {
            if (!columnEmpty(x)) {
                x2 = x + 1;
                break;
            }
        }
        if (x1 == -1) {
            assert x2 == -1;
            this.data = new int[0][0];
        } else {
            int[][] newData = new int[x2 - x1][data[0].length];
            System.arraycopy(data, x1, newData, 0, x2 - x1);
            this.data = newData;
        }
    }

    // Deprecated, use equals
    public double match(BinImg binImg) throws Exception {
        final int maxShift = 5;

        if (Math.abs(binImg.getWidth() - this.getWidth()) > maxShift / 2 + 1) {
            return 0;
        }
        if (binImg.getWidth() == 0 && this.getWidth() == 0) {
            return 1;
        }

        double bestMatch = 0;
        int bestShift = -1;
        for (int shift = -maxShift / 2; shift < maxShift / 2; shift++) {

            int count = 0, mcount = 0;
            for (int x = 0; x < data.length; x++) {
                for (int y = 0; y < data[0].length; y++) {
                    if (x + shift >= 0 && x + shift < binImg.data.length
                            && data[x][y] == binImg.data[x + shift][y]) {
                        mcount++;
                    }
                    count++;
                }
            }

            double match = 1.0 * mcount / count;
            if (match > bestMatch) {
                bestMatch = match;
                bestShift = shift;
            }
        }
//		Log.log("Best shift: %d", bestShift);
        return bestMatch;
    }

    public int getWidth() {
        return this.data.length;
    }

    public int getHeight() {
        if (this.data.length == 0) {
            return 0;
        }
        return this.data[0].length;
    }

    public int getSquare() {
        return this.getWidth() * this.getHeight();
    }

    public String toString() {
        if (this.data.length == 0) {
            return "Empty BinImg";
        }

        String ret = "";
        for (int x = 0; x < data.length + 2; x++) {
            ret += "-";
        }
        ret += "\n";
        for (int y = 0; y < data[0].length; y++) {
            ret += "|";
            for (int x = 0; x < data.length; x++) {
                if (data[x][y] == 1) {
                    ret += "*";
                } else {
                    ret += " ";
                }
            }
            ret += "|\n";
        }
        for (int x = 0; x < data.length + 2; x++) {
            ret += "-";
        }
        ret += "\n";

        return ret;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // default serialization
        // out.defaultWriteObject();
        out.writeInt(this.getWidth());
        out.writeInt(this.getHeight());
        for (int x = 0; x < this.getWidth(); x++) {
            for (int y = 0; y < this.getHeight(); y++) {
                out.writeByte(this.data[x][y]);
            }
        }
        out.writeDouble(this.matchMin);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // default deSerialization
        // in.defaultReadObject();
        int width = in.readInt();
        int height = in.readInt();
        this.data = new int[width][height];
        for (int x = 0; x < this.getWidth(); x++) {
            for (int y = 0; y < this.getHeight(); y++) {
                this.data[x][y] = in.readByte();
            }
        }
        this.matchMin = in.readDouble();
    }

    public int hashCode() {
        return this.getWidth() * this.getHeight();
    }

    public boolean equals(Object o) {
        if (!(o instanceof BinImg)) {
            return false;
        }
        BinImg binImg = (BinImg) o;

        final int maxShift = 3;

        if (Math.abs(binImg.getWidth() - this.getWidth()) > maxShift / 2 + 1) {
            return false;
        }
        if (binImg.getWidth() == 0 && this.getWidth() == 0) {
            return true;
        }

        for (int shift = -maxShift / 2; shift < maxShift / 2; shift++) {

            int count = 0, mcount = 0;
            for (int x = 0; x < data.length; x++) {
                for (int y = 0; y < data[0].length; y++) {
                    if (x + shift >= 0 && x + shift < binImg.data.length
                            && data[x][y] == binImg.data[x + shift][y]) {
                        mcount++;
                    }
                    count++;
                }
            }

            double match = 1.0 * mcount / count;
            if (match >= this.matchMin) {
                return true;
            }
        }
        return false;
    }
}
