package com.poker.brazillive.shell.binimg;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.poker.brazillive.shell.util.*;

public class RecImgRucap {


	public static final String STATUS_PREFIX = "#!#! ";
	public static final String STATUS_HTTP_FAIL = STATUS_PREFIX+"http fail";
	
	public static final int TIMEOUT_SEC = 600;
	
	private static final int MAX_RECS = 30;
	private static Map<Integer,Long> recs = new ConcurrentHashMap<Integer,Long>();
	private static Count recCount = new Count();

	public static String rec(File imgFile, String rucapKey, Log log) throws Exception {
		
		int recNum = recCount.inc();

		while (true) {
			List<Integer> nums = new ArrayList<Integer>(recs.keySet());
			for (int num: nums) {
				Long t = recs.get(num);
				if (t != null && Misc.getTime() - t > TIMEOUT_SEC*1000) {
					recs.remove(num);
					log.lo("Removed rec %d by timeout", num);
				}
			}
			synchronized (recs) {
				if (recs.size() < MAX_RECS) {
					recs.put(recNum, Misc.getTime());
					break;
				}
			}
			log.lo("%d recs are working: %s. Wait", recs.size(), recs.keySet());
			Thread.sleep(3000);
		}
		
		log.lo("Started");
		
		String charset = "UTF-8";
        String requestURL = "http://rucaptcha.com/in.php";
 
        MultipartUtility multipart = new MultipartUtility(requestURL, charset);
        multipart.addHeaderField("User-Agent", "CodeJava");
        multipart.addHeaderField("Test-Header", "Header-Value");
        multipart.addFormField("method", "post");
        multipart.addFormField("key", rucapKey);
        multipart.addFilePart("file", imgFile);
        multipart.addFormField("regsense", "0");
        multipart.addFormField("language", "2");
 
        List<String> response = null;
        try {
        	response = multipart.finish();
        } catch (IOException e) {
        	recs.remove(recNum);
        	return STATUS_PREFIX+e.getMessage();
        }

        if (response.size() != 1) {
        	recs.remove(recNum);
        	return STATUS_PREFIX+"Unexpected response. It has "+response.size()+" lines";
        }
        String resp = response.get(0);
        log.lo("SERVER REPLIED: "+resp);
        
        if (resp.indexOf("OK") != 0) {
        	recs.remove(recNum);
        	return STATUS_PREFIX+resp;
        }
        
        String num = resp.split("\\|")[1];
        long timeStartWait = Misc.getTime();
        
        while (true) {
	        String url = "http://rucaptcha.com/res.php?key="+rucapKey+"&action=get&id="+num;
	        
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
	 
			log.lo("Sending 'GET' request to URL : " + url);
			int responseCode = con.getResponseCode();
			log.lo("Response Code : " + responseCode);
			
			if (responseCode != HttpURLConnection.HTTP_OK) {
	        	recs.remove(recNum);
	        	return STATUS_PREFIX+"bad response code: "+responseCode;
			}
	 
			InputStream is = con.getInputStream();
			byte[] bs = new byte[200];
			int c = is.read(bs);
			byte[] bs2 = new byte[c];
			System.arraycopy(bs, 0, bs2, 0, c);
			String resp2 = new String(bs2, Charset.forName("UTF-8"));
//					System.out.println(Arrays.toString(bs2));
			log.lo("response: "+resp2);
			
			/*
			OK|%TEXT% 	������ 	����� ������� ������������ %TEXT% - �������������� �����
			CAPCHA_NOT_READY 	��� �� ������ 	����� � ������, ��� �� ������������, ���������� ���������� ������ ����� ��������� ������
			ERROR_KEY_DOES_NOT_EXIST 	������ 	�� ������������ �������� key � �������
			ERROR_WRONG_ID_FORMAT 	������ 	�������� ������ ID �����. ID ������ ��������� ������ �����
			ERROR_CAPTCHA_UNSOLVABLE 	������ 	����� �� ������ ��������� 3 ������ ���������. ��������� �������� �� ��� ����������� ������������ ������� �� ������
			ERROR_BAD_DUPLICATES - not documented
			*/
			
			if (resp2.indexOf("OK|") == 0) {
				String r = resp2.split("\\|")[1];
				if (resp2.indexOf("OK|ERROR") == 0) {
		        	recs.remove(recNum);
					return STATUS_PREFIX+r;
				}
	        	recs.remove(recNum);
				return r.toLowerCase().replaceAll("[ _]", "");
			} else if (resp2.indexOf("ERROR_") == 0) {
	        	recs.remove(recNum);
				return STATUS_PREFIX+resp2;
			} else if (resp2.indexOf("CAPCHA_NOT_READY") == 0) {
				// Do nothing, wait
			} else {
	        	recs.remove(recNum);
				return STATUS_PREFIX+"Unknown response: '"+resp2+"'";
			}
			
			if (Misc.getTime() - timeStartWait > TIMEOUT_SEC*1000) {
	        	recs.remove(recNum);
	        	return STATUS_PREFIX+"timeout "+TIMEOUT_SEC+" sec";
			}
			
			Thread.sleep(10000);
        }
	}

	public static void main(String[] args) throws Exception {
		String f = "C:/temp/20160314_222254.855_0x00DA0B94.png";
		BufferedImage img = ImageIO.read(new File(f));
		BufferedImage simg = img.getSubimage(444,543,120,13);
		File tmpImg = new File("c:/temp/tmp.png");
		ImageIO.write(simg, "png", tmpImg);
		
//		RecImgRucap.rec(tmpImg, "a688fae7fb95529503e2a1e5b1fe9574", Log.defl);

		for (int i = 0; i < 8; i++) {
			int fi = i;
			new Thread() {
				public void run() {
					try {
						RecImgRucap.rec(tmpImg, "a688fae7fb95529503e2a1e5b1fe9574", new Log("rucap."+fi));
					} catch (Exception e) { e.printStackTrace(); }
				}
			}.start();
		}
	}
}
