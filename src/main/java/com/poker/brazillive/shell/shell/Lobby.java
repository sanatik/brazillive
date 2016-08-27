package com.poker.brazillive.shell.shell;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import com.poker.brazillive.shell.util.Log;
import com.poker.brazillive.shell.util.Misc;

public abstract class Lobby {
	
	public List<Long> excTimes = new ArrayList<Long>();
	protected Log log;

	public static Lobby create() throws Exception {
		String nw = Shell.getNetworkSkin()[0];
		return (Lobby)Class.forName("shell."+nw.toLowerCase()+".Lobby"+nw).newInstance();
	}

	public Lobby() throws Exception {
		File tlFolder = new File(Shell.uname+"/tlogs");
		tlFolder.mkdirs();
		this.log = new Log(tlFolder.getAbsolutePath()+"/"+(new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(Misc.getTime())))+".lobby.log");
	}
	
	public abstract void procLook(List<WndData> look) throws Exception;
}
