package com.poker.brazillive.shell.util;

import java.io.*;

public class ProcTimer {
	private final int INTERVAL_SEC = 5;
	
	private long startTime;
	private String label;
	private long total;
	private long prevLogTime = 0;
	private int prevDoneCount = 0;
	//private Writer writer = null;
	
	public ProcTimer(String label, long total) throws Exception {
		this.label = label;
		this.total = total;
		this.startTime = Misc.getTime();
		this.prevLogTime = this.startTime;
		//Log.log(label+": started...");
	}
//	public ProcTimer(String label, long total, Writer writer) throws Exception {
//		this.label = label;
//		this.total = total;
//		this.startTime = Misc.getTime();
//		this.prevLogTime = this.startTime;
//		this.writer = writer;
//		//Log.log(label+": started...");
//	}
	
//	private void log(String mes) throws Exception {
//		if (this.writer != null) this.writer.write(mes+"\n");
//		else Log.log(mes);
//	}
	
	private static String timeFormat(long mls) {
		if (mls/1000 < 120)	return String.format("%d sec", mls/1000);
		else return String.format("%1.1f min", mls/1000.0/60);
	}
	
	public void logStartTime() throws Exception {
		Log.log(label+": started...");
	}
	public void logTime(int doneCount) throws Exception {
		long t = Misc.getTime();
		
		if (t - prevLogTime > INTERVAL_SEC*1000) {
		
			String out = "";
			//if (prevLogTime == startTime) out += "\n";
			
			long spent = t - startTime; 
			double left = Double.MIN_VALUE;
			if (doneCount != 0 && spent != 0 && total != 0)
				left = 1.0*(total-doneCount)/(1.0*doneCount/spent);
				//left = 1.0*(total-doneCount)/(1.0*(doneCount-prevDoneCount)/(t - prevLogTime));
			
			if (total == 0)
				out += String.format("Done %d. ", doneCount);
			else
				out += String.format("Done %d out of %d (%d%%). ", doneCount, total, Math.round(100.0*doneCount/total));
			
			if (left != Double.MIN_VALUE)
				out += String.format("Spent "+timeFormat(spent)+", left "+timeFormat(Math.round(left)));

			Log.log("%s",out);
			prevLogTime = t;
			prevDoneCount = doneCount;
		}
	}
	public void logFinishTime() throws Exception {
		long spent = Misc.getTime()-startTime;
		if (prevLogTime == this.startTime) Log.log(String.format(label+": done in %d sec.",spent/1000));
		else {
			if (spent/1000 < 120) Log.log(String.format(label + " is done in %d sec",spent/1000));
			else Log.log(String.format(label+" is done in %1.1f mins", spent/1000.0/60));
		}
	}
}
