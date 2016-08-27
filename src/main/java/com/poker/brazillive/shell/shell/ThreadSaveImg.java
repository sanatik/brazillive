package com.poker.brazillive.shell.shell;

import java.util.*;
import java.util.concurrent.*;

import com.poker.brazillive.shell.util.Log;
import com.poker.brazillive.shell.util.Misc;

public class ThreadSaveImg extends Thread {
	
	private Queue<WndData> queue = new ConcurrentLinkedQueue<WndData>();
	private Map<WndData, Integer> uniq = new ConcurrentHashMap<WndData, Integer>();
	private long timeLog = 0;
	private int maxQueueSize;
	
	public ThreadSaveImg() {
		this.maxQueueSize = Misc.getPropInt(Shell.cfg, "img.save.queue.max.size");
	}
	
	public void addImg(WndData wd, String reason) throws Exception {
//		Log.log("ThreadSaveImg: adding image %s to save. Reason: %s. Queue size: %d", wd.getImgFileName(), reason, this.queue.size());
		
		if (maxQueueSize == 0) return;
		
		if (uniq.containsKey(wd)) {
//			Log.log("ThreadSaveImg: this image is already in the queue");
			return;
		}
		this.queue.add(wd);
		this.uniq.put(wd, 1);
		
		int c = 0, s = queue.size();
		while (queue.size() > maxQueueSize) {
			WndData w = queue.poll();
			this.uniq.remove(w);
			c++;
		}
		if (c > 0) Log.log("ThreadSaveImg: WARNING! Removed %d imgs from queue because it was too big: %d", c, s);
	}
	public void addImgs(List<WndData> wds, String reason) throws Exception {
		for (WndData wd: wds) this.addImg(wd, reason);
	}
	
	public void run() {
		try {
			while (true) {
				if (queue.isEmpty()) {
					Thread.sleep(100);
					continue;
				}
				
				WndData wd = queue.poll();
				this.uniq.remove(wd);
				wd.saveImg();
				if (queue.size() > 10 && Misc.getTime() - this.timeLog > 5*1000) {
					Log.log("ThreadSaveImg: img save queue size: %d", queue.size());
					this.timeLog = Misc.getTime();
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
