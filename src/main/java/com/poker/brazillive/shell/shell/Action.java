package com.poker.brazillive.shell.shell;

import com.poker.brazillive.shell.util.Misc;

public class Action {
	
	public static final String AHK_PATH = "lib/AutoHotkey.exe";
	
	private static String ahkExeCommon() {
		return "\""+AHK_PATH+"\" \"ahk/common.ahk\"";
	}

	public static void wndMove(int hwnd, int x, int y) throws Exception {
		String cmd = String.format("%s move 0x%08X %d %d", ahkExeCommon(), hwnd, x, y);
		Misc.sysCall(cmd);
	}

	public static void wndActivate(String name) throws Exception {
		String cmd = String.format("%s win_act_by_name \"%s\"", ahkExeCommon(), name);
		Misc.sysCall(cmd);
	}
	public static void wndActivate(int hwnd) throws Exception {
		String cmd = String.format("%s win_act_by_hwnd 0x%08X", ahkExeCommon(), hwnd);
		Misc.sysCall(cmd);
	}
	public static void wndClose(int hwnd) throws Exception {
		String cmd = String.format("%s close 0x%08X", ahkExeCommon(), hwnd);
		Misc.sysCall(cmd);
	}
	public static void wndClose(String name) throws Exception {
		String cmd = String.format("%s close_by_name \"%s\"", ahkExeCommon(), name);
		Misc.sysCall(cmd);
	}
	public static void wndResize(String name, int w, int h) throws Exception {
		String cmd = String.format("%s resize_by_name \"%s\" %d %d", ahkExeCommon(), name, w, h);
		Misc.sysCall(cmd);
	}
	public static void wndMove(String name, int x, int y) throws Exception {
		String cmd = String.format("%s move_by_name \"%s\" %d %d", ahkExeCommon(), name, x, y);
		Misc.sysCall(cmd);
	}
	public static void sendInp(String inp) throws Exception {
		String cmd = String.format("%s send_inp %s", ahkExeCommon(), inp);
		Misc.sysCall(cmd);
	}

	public static void click(int hwnd, int x, int y, int count, int rnd) throws Exception {
		String ahkExe = String.format("\"%s\" \"%s\" click 0x%08X %d %d %d %d",
				AHK_PATH, "ahk/common.ahk",
				hwnd, x, y, count, rnd);
		
		synchronized (Shell.clickSync) {
			Misc.sysCall(ahkExe);
			Thread.sleep(100);
		}
	}
	public static void clickNoAct(int x, int y, int count, int rnd) throws Exception {
		String ahkExe = String.format("\"%s\" \"%s\" click_no_act %d %d %d %d",
				AHK_PATH, "ahk/common.ahk",
				x, y, count, rnd);
		
		synchronized (Shell.clickSync) {
			Misc.sysCall(ahkExe);
			Thread.sleep(100);
		}
	}
	public static void clickHold(int hwnd, int x, int y, int delay, int rnd) throws Exception {
		String ahkExe = String.format("\"%s\" \"%s\" click_hold 0x%08X %d %d %d %d",
				AHK_PATH, "ahk/common.ahk",
				hwnd, x, y, delay, rnd);
		
		synchronized (Shell.clickSync) {
			Misc.sysCall(ahkExe);
			Thread.sleep(100);
		}
	}
}
