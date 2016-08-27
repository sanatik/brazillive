package com.poker.brazillive.shell.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TaskList {

	public static class Task {
		public int id;
		public String img;
		public String uname;
		
		public Task(int id, String img, String uname) {
			this.id = id;
			this.img = img;
			this.uname = uname;
		}
		public Task(String csv) {
			String[] sr = csv.substring(1, csv.length()-1).split("\",\"");
			// "Image Name","PID","Session Name","Session#","Mem Usage","Status","User Name","CPU Time","Window Title"
			this.id = Integer.parseInt(sr[1]);
			this.img = sr[0];
			String[] sr2 = sr[6].split("\\\\");
			this.uname = sr2[sr2.length-1];
		}
		
		public String toString() {
			return String.format("%4d %s %s", this.id, this.img, this.uname);
		}
	}
	
	public static Task[] getTasks(String uname, String img) throws Exception {
		String userFilter = "", imgFilter = "";
		if (uname != null) userFilter = String.format("/FI \"USERNAME eq %s\"", uname);
		if (img != null) imgFilter = String.format("/FI \"IMAGENAME eq %s\"", img);
		
		String cmd = String.format("tasklist %s %s /FO CSV /NH /V", userFilter, imgFilter);
		
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        Task[] ret = new Task[1000]; int c = 0;
        String line;
        while ((line = input.readLine()) != null) {
        	if (line.indexOf("INFO: No tasks") != -1) break;
        	ret[c++] = new Task(line);
        }
        input.close();
        
        Task[] ret2 = new Task[c];
        System.arraycopy(ret, 0, ret2, 0, c);
        return ret2;
	}
	
	public static void main(String[] args) throws Exception {
		Task[] ts = getTasks(null, "chrome.exe");
		String s = "";
		for (Task t: ts) s += String.format("%s\n", t);
		Log.log("\n%s", s);
	}

}
