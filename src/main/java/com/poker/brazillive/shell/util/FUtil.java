package com.poker.brazillive.shell.util;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.text.*;

public class FUtil {
	protected static void fPutAdd(String fileName, String cnt, boolean append) throws Exception {
		FileWriter fstream = new FileWriter(fileName,append);
		BufferedWriter logOut = new BufferedWriter(fstream);
		logOut.write(cnt);
		logOut.close();
	}
	public static void fPut(String fileName, String cnt) throws Exception {
		fPutAdd(fileName,cnt,false);
	}
	public static void fAdd(String fileName, String cnt) throws Exception {
		fPutAdd(fileName,cnt,true);
	}
	public static List<String> fRead(String fileName) throws Exception {
		List<String> res = new ArrayList<String>();
		
		FileInputStream fstream = new FileInputStream(fileName);
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {
			// Print the content on the console
			//System.out.println (strLine);
			res.add(strLine);
		}
		//Close the input stream
		in.close();
		return res;
	}
	public static int fCount(String fileName) throws Exception {
		int res = 0;
		
		FileInputStream fstream = new FileInputStream(fileName);
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {
			// Print the content on the console
			//System.out.println (strLine);
			res++;
		}
		//Close the input stream
		in.close();
		return res;
	}
	public static File[] dRead(String dirName) throws Exception {
		File dir = new File(dirName);
		//if (! dir.isDirectory()) return null;
		File[] files = dir.listFiles();
		
		Map<String,File> fm = new HashMap<String,File>();
		for (File f: files) fm.put(f.getName(), f);
		String[] fake = {};
		String[] fs = (String[])fm.keySet().toArray(fake);
		Arrays.sort(fs);
		File[] newFiles = new File[files.length];
		for (int i = 0; i < fs.length; i++) newFiles[i] = fm.get(fs[i]);
		
		return newFiles;
	}
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}
	
	// From here: http://stackoverflow.com/questions/106770/standard-concise-way-to-copy-a-file-in-java
	public static void copyFile(String from, String to) throws IOException, Exception {
		//Log.log("Copy from '"+from+"' to '"+to+"'");
		File sourceFile = new File(from);
		File destFile = new File(to);
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	public static List<File> searchFiles(String sf, String regex) {
		File f = new File(sf);
		List<File> ret = new ArrayList<File>();
		if (f.isDirectory()) {
			File[] fs = f.listFiles();
			for (File ff: fs) ret.addAll(searchFiles(ff.getAbsolutePath(), regex));
		} else {
			if (regex == null || f.getName().matches(regex)) ret.add(f);
		}
		return ret;
	}

	public static long getFolderSize(File f) {
		if (f.isFile()) return f.length();
	    long size = 0;
	    for (File file : f.listFiles()) size += getFolderSize(file);
	    return size;
	}

}
