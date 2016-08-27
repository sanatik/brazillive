/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.shell;

import com.poker.brazillive.config.AhkConfig;
import com.poker.brazillive.util.Misc;

/**
 *
 * @author boson
 */
public class Shell {

    public final static Object SYNC = new Object();
    private final AhkConfig ahkConfig;

    public Shell(AhkConfig ahkConfig) {
        this.ahkConfig = ahkConfig;
    }

    private String ahk() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(ahkConfig.getPath()).append("\" ");
        sb.append("\"").append(ahkConfig.getScript()).append("\" ");
        return sb.toString();
    }

    public void click(int hwnd, int x, int y, int count, int rnd) throws Exception {

        String ahkExe = String.format(ahk() + "click 0x%08X %d %d %d %d",
                hwnd, x, y, count, rnd);

        synchronized (Shell.SYNC) {
            Misc.sysCall(ahkExe);
            Thread.sleep(100);
        }
    }

    public void resize(int hwnd, int width, int height) throws Exception {
        StringBuilder sb = new StringBuilder(ahk());
        sb.append("resize ").append(hwnd).append(" ");
        sb.append(width).append(" ").append(height);
        synchronized (Shell.SYNC) {
            Misc.sysCall(sb.toString());
            Thread.sleep(500);
        }
    }

    public void activate(int hwnd) throws Exception {
        StringBuilder sb = new StringBuilder(ahk());
        sb.append("resize ").append(hwnd);
        synchronized (Shell.SYNC) {
            Misc.sysCall(sb.toString());
            Thread.sleep(500);
        }
    }

    public void raise(int hwnd, int x, int y, String value) throws Exception {
        StringBuilder sb = new StringBuilder(ahk());
        sb.append("raise ").append(hwnd).append(" ")
                .append(x).append(" ")
                .append(y).append(" ")
                .append(value);
        synchronized (Shell.SYNC) {
            Misc.sysCall(sb.toString());
            Thread.sleep(1000);
        }
    }
}
