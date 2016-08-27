package com.poker.brazillive.shell.shell;

import com.poker.brazillive.shell.util.*;
import com.poker.brazillive.shell.rec.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;

import javax.imageio.*;

public class FindImgs {

	public static void main(String[] args) throws Exception {
		File[] fs = FUtil.dRead("imgs");
		RecBool rc = new RecBool("rec.cpa/RecIsCard.prop");
		RecBool rcb = new RecBool("rec.cpa/RecIsCardB.prop");

		for (File f: fs) {
			BufferedImage img = ImageIO.read(f);
			boolean[] rcRes = rc.rec(img);
			boolean[] rcbRes = rcb.rec(img);
//			Log.log("\n%s\n%s", Arrays.toString(rcRes), Arrays.toString(rcbRes));
			boolean c5 = true;
			for (int i = 0; i < 5; i++) if (!rcRes[i] && !rcbRes[i]) c5 = false;
			if (!c5) continue;
			
			FUtil.copyFile(f.getAbsolutePath(), "c:/temp/img/"+f.getName());
		}
	}

}
