package com.poker.brazillive.shell.shell.gg;

import java.awt.image.BufferedImage;
import com.poker.brazillive.shell.shell.Room;
import com.poker.brazillive.shell.shell.Table;
import com.poker.brazillive.shell.shell.WndData;
import com.poker.brazillive.shell.util.Log;
import com.poker.brazillive.shell.util.Misc;

public class RoomGg extends Room {

	
	public RoomGg() throws Exception {
		this.sendHh = false;
		this.clickActiveTable = true;
		this.waitNewTableDrawSec = 15;
	}
	
	public String getHhSign() {
		return "";
	}

	public boolean isTable(WndData wndData) throws Exception {
		BufferedImage img = wndData.getImg();
		return img.getWidth() == TableGg.width && img.getHeight() == TableGg.height;
	}

	public String[] getProtocolNetworkSkin() {
		return new String[]{"GG", "AsyaPoker"};
	}

	public void checkIp() throws Exception {
		// Do nothing - cannot check
	}

	public boolean buildGameAfterHeroFinished() {
		return true;
	}

	public long getMoveDelayMs(Table t) {
		return 0;
		// ANSWER F 0 28 Normal
		// ANSWER R 0.08 305 Normal
//		if (t == null || t.answer == null) return 3000;
//		return Integer.parseInt(t.answer.split(" ")[3])*5;
//		return 3000+Misc.rand.nextInt(7000);
	}
	public String getGidPrefix() {
		return "";
	}

	public long needSitoutPeriod() throws Exception {
		return 0;
	}

	public boolean isSitoutNextBbSupported() throws Exception {
		return false;
	}

	public void registerTablePlay(String tid, String nick, long lifeTime) throws Exception {
		// Do nothing
	}
}
