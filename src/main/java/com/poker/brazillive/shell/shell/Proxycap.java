package com.poker.brazillive.shell.shell;

import java.io.*;
import java.util.*;
import com.poker.brazillive.shell.util.*;

public class Proxycap {

	private static final String ahk = "lib/AutoHotkey.exe ahk/common.ahk";
//	private static final double slowKoef = 1.0;
	
	
	private static String rep(String s, int n) {
		String ret = "";
		for (int i = 0; i < n; i++) ret += s;
		return ret;
	}
	
	private static void activate(String title) throws Exception {
		Misc.sysCall(ahk+" win_act_by_name "+title);
		Thread.sleep(500);
	}
	private static void click(int x, int y) throws Exception {
		Misc.sysCall(ahk+" click_no_act "+x+" "+y+" 1 0");
		Thread.sleep(500);
	}
	private static void inp(String fmt, Object... args) throws Exception {
		Misc.sysCall(ahk+" send_inp "+String.format(fmt, args));
		Thread.sleep(500);
	}
	
	private static void createProxy(String name, String ip, String login, String pwd) throws Exception {
		Log.log("Creating proxy: %s %s %s %s", name, ip, login, pwd);

		activate("ProxyCap Configuration");
		click(82, 72);
		click(656, 90);
		inp("%s{Tab}{Tab}%s{Tab}%s{Tab}{Space}{Tab}%s{Tab}%s{Enter}",
				name.replaceAll(" ", "{Space}"), ip, "61336", login, pwd);
	}
	private static void createRule(String name, int proxyNum) throws Exception {
		Log.log("Creating rule: %s %d", name, proxyNum);
		
		activate("ProxyCap Configuration");
		click(76, 107);
		click(637, 90);
		inp("%s%s%s{Enter}",
				name.replaceAll(" ", "{Space}"), rep("{Tab}", 5), rep("{Down}", proxyNum+1));
		
//		for (String prg: prgs) {
//			inp("{Enter}");
//			Thread.sleep(500);
//			inp("%s{Enter}", prg.replaceAll(" ", "{Space}").replaceAll("\\/", "\\\\"));
//		}
//		inp("{Tab}{Enter}");
	}
	private static void ruleAddPrgs(int ruleNum, String[] prgs) throws Exception {
		Log.log("Adding prgs to rule %d: %s", ruleNum, Arrays.toString(prgs));
		activate("ProxyCap Configuration");
		click(76, 107);
		click(250, 140);
		inp("%s", rep("{Down}", ruleNum));
		click(700, 90);
		click(100, 50);
		click(30, 108);
		
		for (String prg: prgs) {
			click(560, 143);
			inp("%s{Enter}", prg.replaceAll(" ", "{Space}").replaceAll("\\/", "\\\\"));
		}
		inp("{Enter}");
	}
	
	public static void createFromCfg(String[] args) throws Exception {
		
//		proxy luminare 62.149.223.231 matumbo2013 rXhW8pu8
//			C:/Program Files (x86)/AsyaPoker_luminare/launcher.exe
//			C:/Program Files (x86)/AsyaPoker_luminare/GGnet.exe
		
		List<String> strs = FUtil.fRead(args[0]);
		List<String> proxyNames = new ArrayList<String>();
		List<List<String>> prgs = new ArrayList<List<String>>();
		List<String> curPrgs = null;
		
		for (String s: strs) {
			s = s.trim(); if (s.isEmpty() || s.indexOf("#") == 0) continue;
			if (s.indexOf("proxy") == 0) {
				String[] sr = s.split(" +");
				createProxy(sr[1], sr[2], sr[3], sr[4]);
				proxyNames.add(sr[1]);
				if (curPrgs != null) prgs.add(curPrgs);
				curPrgs = new ArrayList<String>();
			} else {
				curPrgs.add(s);
			}
		}
		if (curPrgs != null) prgs.add(curPrgs);
		assert proxyNames.size() == prgs.size();
		
		for (int pn = 0; pn < proxyNames.size(); pn++) {
			createRule(proxyNames.get(pn), pn);
		}
		for (int pn = 0; pn < proxyNames.size(); pn++) {
			ruleAddPrgs(pn, prgs.get(pn).toArray(new String[0]));
		}

		inp("{Enter}");
	}
	
	public static void main(String[] args) throws Exception {
//		ruleAddPrgs(1, new String[]{"C:/Program Files (x86)/AsyaPoker_luminare/launcher.exe", "C:/Program Files (x86)/AsyaPoker_luminare/GGnet.exe"});
//		System.exit(0);
		
		createFromCfg(args); System.exit(0);
		
//		createProxy("tmp", "1.2.3.4", "lll", "ppp");
//		createRule("rule tmp", 2, new String[]{
//				"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
//				"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome_Brood.exe",
//		});
		
		Properties cfg = new Properties();
		cfg.load(new FileInputStream("shell.cfg"));
		
		List<String> heros = new ArrayList<String>();
		for (Object ko: cfg.keySet()) {
			String k = (String)ko;
			if (k.indexOf(".hero.") == -1) continue;
			heros.add(k.split("\\.")[0]);
		}
		Collections.sort(heros);
		
		for (String hero: heros) {
			String[] sr = cfg.getProperty(hero+".my.ip").split("/");
			createProxy(hero, sr[0], sr[1], sr[2]);
		}
		for (int i = 0; i < heros.size(); i++) {
			String hero = heros.get(i);
			final String[] prgs = {
//					"C:\\Program Files (x86)\\Mozilla Firefox\\firefox_"+hero+".exe",
//					"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome_"+hero+".exe",
				};
//			createRule(hero, i+1, prgs);
		}

		inp("{Enter}");
	}
	
}
