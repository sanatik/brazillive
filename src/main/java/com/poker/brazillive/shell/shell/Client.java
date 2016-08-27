package com.poker.brazillive.shell.shell;

public abstract class Client {
	
	public static Client create() throws Exception {
		String nw = Shell.getNetworkSkin()[0];
		return (Client)Class.forName("shell."+nw.toLowerCase()+".Client"+nw).newInstance();
	}
	
	public abstract void start() throws Exception;
	public abstract void close() throws Exception;
	public abstract void kill() throws Exception;
	public abstract boolean isRunning() throws Exception;

	public void closeOrKill() throws Exception {
		this.close();
		Thread.sleep(3000);
		if (this.isRunning()) this.kill();
	}
}
