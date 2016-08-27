package com.poker.brazillive.shell.shell;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import com.poker.brazillive.shell.util.*;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

public class WndData {
	public static final String IMG_TYPE = "bmp";
	public static long lastTime;
	private static Map<Integer,Long> hwnd2bornTime = new ConcurrentHashMap<Integer,Long>();
	
	public boolean active;
	public int hwnd;
	public String title;
	public BufferedImage img;
	private byte[] bmpData;
	public long time;
	public boolean broken;
	
	public static WndData readSocket(DataInputStream in, Log log) throws Exception {
		WndData wndData = new WndData();
		
		wndData.hwnd = in.readInt();
//		log.lo("hwnd=0x%08X", wndData.hwnd);

		if (!hwnd2bornTime.containsKey(wndData.hwnd))
        	hwnd2bornTime.put(wndData.hwnd, Misc.getTime());
		
		wndData.broken = in.readByte() > 0;
//		log.lo("broken="+wndData.broken);
		if (wndData.broken) Log.log("BROKEN wndData in readSocket!");
		if (wndData.broken) return wndData;
		
		wndData.active = in.readByte() > 0;
//		log.lo("active="+wndData.active);
		wndData.title = in.readUTF();
//		log.lo("title=%s", wndData.title);
//        Log.log("%08X, '%s', %s", wndData.hwnd, wndData.title, wndData.active?"ACTIVE":"");
        int bmpLen = in.readInt();
//		log.lo("bmpLen=%d", bmpLen);
        byte[] bmpData = null;
       	bmpData = new byte[bmpLen];
        int pos = 0;
        while (pos < bmpLen) {
        	int rl = in.read(bmpData, pos, bmpData.length-pos);
        	pos += rl;
        }
        
//        wndData.img = ImageIO.read(new BufferedInputStream(new ByteArrayInputStream(bmpData)));
        wndData.bmpData = bmpData;
        
        wndData.time = Misc.getTime();
        if (wndData.time <= lastTime) wndData.time = lastTime+1;
        lastTime = wndData.time;
        
        return wndData;
	}
	
	public BufferedImage getImg() throws Exception {
		if (img == null) {
			img = ImageIO.read(new BufferedInputStream(new ByteArrayInputStream(bmpData)));
			bmpData = null; // Free mem
		}
		return img;
	}
	public void setImg(BufferedImage img) throws Exception {
		this.img = img;
	}
	
	public String getImgFileName() {
		return this.getImgFileName(IMG_TYPE);
	}
	private String getImgFileName(String format) {
		return (new SimpleDateFormat("yyyyMMdd_HHmmss.SSS").format(new Date(this.time)))+"_0x"+String.format("%08X", this.hwnd)+"."+format;
	}
	public void saveImg() throws Exception {
		this.saveImg(IMG_TYPE);
	}
	private void saveImg(String format) throws Exception {
//		File imgFolder = new File(String.format("%s/0x%08X", Shell.cfg.getProperty("imgs.folder"), this.hwnd));
//		imgFolder.mkdirs();
		File imgFolder = new File(String.format("%s", Shell.uname+"/imgs"));
		imgFolder.mkdirs();
		
		long wndTime = hwnd2bornTime.get(this.hwnd);
		String df = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(wndTime));
		File wndFolder = new File(imgFolder.getAbsolutePath()+"/"+String.format("%s_0x%08X", df, this.hwnd));
		wndFolder.mkdirs();
		File imgFile = new File(wndFolder.getAbsolutePath()+"/"+this.getImgFileName(format));
		ImageIO.write(this.getImg(), format, imgFile);
	}
	
	public String getInfo() {
		String ret = "";
		ret += String.format("WndData info: %s hwnd=0x%08X bmplen=%d", this.broken?"BROKEN":"", this.hwnd, this.bmpData == null ? 0:this.bmpData.length);
		return ret;
	}
}
