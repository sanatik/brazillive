package com.poker.brazillive.shell.binimg;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.imageio.ImageIO;

import com.poker.brazillive.shell.rec.*;
import com.poker.brazillive.shell.util.*;

public class RecBinImg implements Serializable {
	private static final long serialVersionUID = 1;

	private class ImgEnt implements Serializable {
		private static final long serialVersionUID = 1;
		public int num;
		public String answer;
		public ImgEnt(int num) {
			this.num = num;
			this.answer = ImgServer.STATUS_NOT_READY;
		}
		private File getFolder() {
			return new File(String.format("%s/%06d", rootFolder, this.num));
		}
	}
	
	private class ThreadReq extends Thread {
		
		private BufferedImage imgRec;
		private BufferedImage imgBin;
		private ImgEnt imgEnt;
		
		public ThreadReq(BufferedImage imgRec, BufferedImage imgBin, ImgEnt ent) {
			this.imgRec = imgRec;
			this.imgBin = imgBin;
			this.imgEnt = ent;
		}
		
		public void run() {
			Log l = null;
			try {
				File f = this.imgEnt.getFolder();
				f.mkdirs();
				l = new Log(f.getAbsolutePath()+"/log", true);
				l.lo("Requested img %d", this.imgEnt.num);
				
				Socket socket = new Socket(imgServerHost, imgServerPort);
				DataInputStream in = new DataInputStream(socket.getInputStream());
		        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
	
				out.writeInt(color.getRGB());
				out.writeInt(radius); // radius
				out.writeDouble(matchPrec);
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(imgRec, "png", bos);
				byte[] bmpData = bos.toByteArray();
				out.writeInt(bmpData.length);
				out.write(bmpData);
	
				bos = new ByteArrayOutputStream();
				ImageIO.write(imgBin, "png", bos);
				bmpData = bos.toByteArray();
				out.writeInt(bmpData.length);
				out.write(bmpData);
				
				int serverNum = in.readInt();
				l.lo("Got server num: %d", serverNum);
				String answer = in.readUTF();
				l.lo("Got answer: %s", answer);
				if (answer.indexOf(ImgServer.STATUS_NOT_READY) != 0) {
					this.imgEnt.answer = answer;
					FUtil.fPut(f.getAbsolutePath()+"/answer", this.imgEnt.answer);
				}
				
				socket.close();
				
//				Log.log("Requested img %d, got answer: %s", this.imgEnt.num, answer);
			} catch (Exception e) {
				String mes = String.format("Exception in RecBinImg.ThreadReq:\n%s", Misc.stacktrace2Str(e));
				l.lo("%s", mes);
				try { Log.log("%s", mes); } catch (Exception e2) { e2.printStackTrace(); }
			}
		}
	}
	
	private Map<BinImg,ImgEnt> bimg2ent = new ConcurrentHashMap<BinImg,ImgEnt>();
	private String rootFolder;
	private Color color;
	private int radius;
	private String imgServerHost;
	private int imgServerPort;
	private long timeLastSave = 0;
	private double matchPrec;
	
	public RecBinImg(String rootFolder, Color color, int radius,
			String imgServerHost, int imgServerPort, double matchPrec) throws Exception {
		
		ImageIO.setUseCache(false);
		this.rootFolder = rootFolder;
		this.color = color;
		this.radius = radius;
		this.imgServerHost = imgServerHost;
		this.imgServerPort = imgServerPort;
		this.matchPrec = matchPrec;
		
		this.load();
	}
	
	private void load() throws Exception {
		if (!new File(this.rootFolder).exists()) return;
		File[] fs = FUtil.dRead(this.rootFolder);
		ProcTimer t = new ProcTimer("Loading RecBinImg", fs.length);
		for (int i = 0; i < fs.length; i++) {
			File f = fs[i];
			ImgEnt ent = new ImgEnt(Integer.parseInt(f.getName()));
			ent.answer = FUtil.fRead(f.getAbsolutePath()+"/answer").get(0);
			BinImg bimg = (BinImg)Misc.readObj(f.getAbsolutePath()+"/img.bin");
			this.bimg2ent.put(bimg, ent);
			t.logTime(i);
		}
		//Log.log("RecBinImg: loaded. Img count = %d", this.bimg2ent.size());
		t.logFinishTime();
	}
	
	public String rec(BufferedImage imgRec, BufferedImage imgBin) throws Exception {
		BinImg bimg = new BinImg(imgBin, this.color, this.radius, this.matchPrec);
		bimg.trim();
		
		if (bimg.getSquare() == 0) return "";

		boolean newEnt = false;
		if (!this.bimg2ent.containsKey(bimg)) synchronized (this.bimg2ent) { if (!this.bimg2ent.containsKey(bimg)) {
			int num = this.bimg2ent.size();
			this.bimg2ent.put(bimg, new ImgEnt(num));
			newEnt = true;
			//Log.log("RecBinImg: new img is created, num=%d", num);
		}}
		
		ImgEnt ent = this.bimg2ent.get(bimg);
		if (newEnt) {
			File f = ent.getFolder();
			f.mkdirs();
			FUtil.fPut(f.getAbsolutePath()+"/binImg.txt", bimg.toString());
			Misc.writeObj(bimg, f.getAbsolutePath()+"/img.bin");
			FUtil.fPut(f.getAbsolutePath()+"/answer", ent.answer);

			ImageIO.write(imgRec, "png", new File(f.getAbsolutePath()+"/rec.png"));
			ImageIO.write(imgBin, "png", new File(f.getAbsolutePath()+"/bin.png"));
		}
		
		if (this.imgServerHost != null 
				&& ent.answer.equals(ImgServer.STATUS_NOT_READY))
			new ThreadReq(imgRec, imgBin, ent).start();
		
		return ent.answer;
	}
	
	public static void main(String[] args) throws Exception {
		String imgFile = "C:/temp/20160314_222254.855_0x00DA0B94.png";
		final String SERVER_HOST = "127.0.0.1";
		final int SERVER_PORT = 2000;
		
		Rect[] rects = new Rect[]{
			new Rect(0,444,543,120,13),	
			new Rect(0,47,451,120,13),	
			new Rect(0,25,267,120,13),	
			new Rect(0,437,124,120,13),	
			new Rect(0,853,267,120,13),	
			new Rect(0,841,451,120,13),	
		};
		
		RecBinImg rbi = new RecBinImg("RecBinImgNames", new Color(255,255,255), 200, SERVER_HOST, SERVER_PORT, 0.97);
	
		BufferedImage img = ImageIO.read(new File(imgFile));
		for (int i = 0; i < rects.length; i++) {
	        BufferedImage imgRec = img.getSubimage(rects[i].x1, rects[i].y1-3, rects[i].getWidth(), rects[i].getHeight()+6);
	        BufferedImage imgBin = img.getSubimage(rects[i].x1, rects[i].y1, rects[i].getWidth(), rects[i].getHeight());
	        
	        String answer = rbi.rec(imgRec, imgBin);
	        Log.log("ANSWER: %s", answer);
		}
	}

}
