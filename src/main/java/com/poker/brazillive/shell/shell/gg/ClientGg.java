package com.poker.brazillive.shell.shell.gg;

import java.io.File;

import com.poker.brazillive.shell.shell.Action;
import com.poker.brazillive.shell.shell.Client;
import com.poker.brazillive.shell.shell.Session;
import com.poker.brazillive.shell.shell.Shell;
import com.poker.brazillive.shell.util.FUtil;
import com.poker.brazillive.shell.util.Log;
import com.poker.brazillive.shell.util.Misc;
import com.poker.brazillive.shell.util.TaskList;

public class ClientGg extends Client {

	private static void waitLog(String log, int t) throws Exception {
		Log.log("%s, wait %dms", log, t);
		Thread.sleep(t);
	}
	
	public void start() throws Exception {
		File clientExe = new File(String.format("C:/Program Files (x86)/AsyaPoker_%s/launcher.exe",
				Shell.curSes.hero.login));
		
		String prName = "GGnet.exe";
		if (Misc.waitForProcess(prName, Shell.uname, 0)) {
			Log.log("Running process '%s' is found. Will not start", prName);
			return;
		}
        Log.log("Running client is not found");
        Log.log("Client exe: %s", clientExe);
        
        File tmp = new File(Shell.uname+"/tmp.bat");
        FUtil.fPut(tmp.getAbsolutePath(), "C:\ncd \""+clientExe.getParentFile()+"\"\nstart "+clientExe.getName()+"\n");
        Process p = Runtime.getRuntime().exec("cmd /C "+tmp.getAbsolutePath());

        Log.log("Waiting '%s' to load...", prName);
		if (!Misc.waitForProcess(prName, Shell.uname, 8*60*1000)) {
			Log.log("ERROR! '%s' process was not found. Client wasn't started", prName);
			return;
		}
		waitLog(prName+" is loaded", 30*1000);
        
        Action.wndActivate("AsyaPoker"); waitLog("Activated lobby", 1000);

        // Close adds
//        Action.clickNoAct(936, 160, 1, 3); waitLog("Clicked adds 1", 1000);
        Action.clickNoAct(855, 206, 1, 3); waitLog("Clicked adds 2", 1000);
//        Action.clickNoAct(856, 224, 1, 3); waitLog("Clicked adds 3", 1000);
        waitLog("Clicked all adds", 5*1000);

        Action.wndActivate("AsyaPoker"); waitLog("Activated lobby", 1000);
        Action.clickNoAct(1060, 392, 1, 3); waitLog("Clicked login", 5*1000);
        Action.sendInp(Shell.curSes.hero.login+"{Tab}"+Shell.curSes.hero.pwd+"{Enter}");
        waitLog("Enetered login/pwd = "+Shell.curSes.hero.login+"/"+Shell.curSes.hero.pwd, 15*1000);

        Action.clickNoAct(650, 150, 1, 3); waitLog("Clicked holdem", 3*1000);

		new File("tmp.bat").delete();
        Log.log("Start client is finished");
	}

	public void close() throws Exception {
		Log.log("Closing Gg client");
		String wndName = "AsyaPoker";
		Action.wndActivate(wndName);
		Thread.sleep(100);
		Action.wndClose(wndName);
		Thread.sleep(100);
		Action.wndActivate(wndName);
		Thread.sleep(100);
		Action.sendInp("{Enter}");
		Thread.sleep(100);
	}

	public void kill() throws Exception {
		Log.log("Gg client hard kill");
		
		for (String exeName: new String[]{"GGnet.exe", "launcher.exe"}) {
			TaskList.Task[] tasks = TaskList.getTasks(Shell.uname, exeName);
			if (tasks.length > 0) {
		        Process p = Runtime.getRuntime().exec("taskkill /T /F /PID "+tasks[0].id);
		        p.waitFor();
		        Thread.sleep(1000);
			}
		}
	}

	public boolean isRunning() throws Exception {
		for (String exeName: new String[]{"GGnet.exe", "launcher.exe"}) {
			TaskList.Task[] tasks = TaskList.getTasks(Shell.uname, exeName);
			if (tasks.length > 0) return true;
		}
		return false;
	}

	public static void main(String[] args) throws Exception {
		Shell.uname = "Brood";
		Shell.curSes = new Session(null, 0, 0, 0, new Shell.Hero("luminare", "Cantuccio2016", "luminare"), new int[0], 0, null, null, null, 0, 10, null);
		
		Client c = new ClientGg();
		c.start();
//		Thread.sleep(1000);
//		c.close();
	}
}
