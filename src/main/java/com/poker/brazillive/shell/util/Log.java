package com.poker.brazillive.shell.util;

import java.util.*;
import java.io.*;
import java.text.*;


public class Log {
	public static boolean ADD_DATE = true;
	public static boolean SHIFT = true;

	public static Log defl;
	public Map<String,PrintStream> pss = new HashMap<String,PrintStream>();
	
	static {
		defl = new Log();
		PrintStream ps = new PrintStream(System.out);
		defl.pss.put("stdout", ps);
	}
	
	public Log() {
	}
	public Log(String logf) throws Exception {
		this(logf, false);
	}
	public Log(String logf, boolean app) throws Exception {
		this.addLogFile(logf, logf, app);
	}

	public void addStdOut() throws Exception {
		PrintStream ps = new PrintStream(System.out);
		pss.put("stdout", ps);
	}
	public void addLogFile(String key, String file, boolean app) throws Exception {
		PrintStream ps = new PrintStream(new FileOutputStream(new File(file), app));
		pss.put(key, ps);
	}
	public void removeLogFile(String file) {
		pss.remove(file);
	}
	
	synchronized public void lo(String fmt, Object... args) {
		String mes = String.format(fmt+"\n", args);

		if (SHIFT) {
			int sd = Thread.currentThread().getStackTrace().length-4;
			for (int i = 0; i < sd; i++) mes = "    "+mes;
		}
		if (ADD_DATE) {
			Date date = new Date(Misc.getTime());

			SimpleDateFormat formatter = new SimpleDateFormat("MMM dd HH:mm:ss.SSS"); //.SSS
			mes = formatter.format(date)+" "+mes;
		}
		
		for (PrintStream ps: pss.values()) {
			ps.print(mes);
			ps.flush();
		}
	}
	public void close() {
		for (PrintStream ps: pss.values()) ps.close();
	}
	
	public static void log(String fmt, Object... args) throws Exception {
		defl.lo(fmt, args);
	}
}
