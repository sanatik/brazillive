package com.poker.brazillive.shell.shell;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.poker.brazillive.shell.util.FUtil;
import com.poker.brazillive.shell.util.Misc;

public class Proxyfier {

	public static class Host {
		
		public String host;
		public String login;
		public String pwd;
		public String[] prgs;
		
		public Host(String host, String login, String pwd, String[] prgs) {
			this.host = host;
			this.login = login;
			this.pwd = pwd;
			this.prgs = prgs;
		}
		
		public boolean equals(Object o) {
			if (!(o instanceof Host)) return false;
			Host h = (Host)o;
			boolean r = this.host.equals(h.host)
					&& this.login.equals(h.login)
					&& this.pwd.equals(h.pwd);
			if (!r) return false;
			if (this.prgs.length != h.prgs.length) return false;
			for (int i = 0; i < this.prgs.length; i++)
				if (!this.prgs[i].equals(h.prgs[i])) return false;
			return true;
		}
		
		public String toString() {
			return this.host+" "+this.login+" "+this.pwd+" "+Arrays.toString(this.prgs);
		}
	}

	private static Document getDoc(String f) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
//		Document doc = db.parse(new File(f));
		
		List<String> strs = FUtil.fRead(f);
		String s = "";
		for (String str: strs) s += str;
		s = s.replaceAll("[\\r\\n]", "").replaceAll(">\\s*<", "><");
		Document doc = db.parse(new ByteArrayInputStream(s.getBytes()));
		
		return doc;
	}
	
//	private static List<Host> getHostList(String xmlFile) throws Exception {
//		Document doc = getDoc(xmlFile);
//		
//		NodeList pns = doc.getElementsByTagName("Proxy");
//		List<Host> ret = new ArrayList<Host>();
//
//		for (int i = 0; i < pns.getLength(); i++) {
//			Element pe = (Element)pns.item(i);
//			
//			String id = pe.getAttribute("id");
//			String addr = pe.getElementsByTagName("Address").item(0).getTextContent();
//			String login = pe.getElementsByTagName("Username").item(0).getTextContent();
//			String pwd = pe.getElementsByTagName("Password").item(0).getTextContent();
//			
//			NodeList rns = doc.getElementsByTagName("Rule");
//			for (int j = 0; j < rns.getLength(); j++) {
//				Element re = (Element)rns.item(j);
//				String proxyId = re.getElementsByTagName("Action").item(0).getTextContent();
//				if (!proxyId.equals(id)) continue;
//				String apps = re.getElementsByTagName("Applications").item(0).getTextContent();
//				String[] as = apps.split(";\\s*");
//				
//				ret.add(new Host(addr, login, pwd, as));
//			}
//		}
//		return ret;
//	}
	
	private static void saveDoc(Document doc, String f) throws Exception {
		Transformer t = TransformerFactory.newInstance().newTransformer();
	    
		t.setOutputProperty(OutputKeys.METHOD, "xml");
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//		t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		
		Source input = new DOMSource(doc);
		Result output = new StreamResult(new File(f));
		t.transform(input, output);
	}
	
	private static Element addProxyIfNeeded(String xmlFile, String addr, String login, String pwd) throws Exception {
		Document doc = getDoc(xmlFile);
		NodeList ps = doc.getElementsByTagName("Proxy");
		int maxId = 0;
		for (int i = 0; i < ps.getLength(); i++) {
			Element pe = (Element)ps.item(i);
			String a = pe.getElementsByTagName("Address").item(0).getTextContent();
			String id = pe.getAttribute("id");
			if (a.equals(addr)) return pe;
			if (Integer.parseInt(id) > maxId) maxId = Integer.parseInt(id);
		}
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document pdoc = db.parse(new ByteArrayInputStream((
			    "<Proxy id=\""+(maxId+1)+"\" type=\"SOCKS5\">"+
			    "<Address>"+addr+"</Address>"+
			    "<Port>61336</Port>"+
			    "<Options>48</Options>"+
			    "<Authentication enabled=\"true\">"+
			    "<Username>"+login+"</Username>"+
			    "<Password>"+pwd+"</Password>"+
			    "</Authentication>"+
			    "</Proxy>"
			).getBytes()));
		Element pe = pdoc.getDocumentElement();
		
		pe = (Element)doc.importNode(pe, true);
		
		Node pln = doc.getElementsByTagName("ProxyList").item(0);
		NodeList pl = ((Element)pln).getElementsByTagName("Proxy");
		Node p1 = pl.getLength() == 0 ? null : pl.item(0);
		pln.insertBefore(pe, p1);

		saveDoc(doc, xmlFile);
		
		return pe;
	}
	
	private static boolean addRuleIfNeeded(String xmlFile, Element pe, String prg) throws Exception {
		Document doc = getDoc(xmlFile);
		String proxyId = pe.getAttribute("id");
		String proxyHost = pe.getElementsByTagName("Address").item(0).getTextContent();
		NodeList rs = doc.getElementsByTagName("Rule");
		Element tre = null;
		
		for (int i = 0; i < rs.getLength(); i++) {
			Element re = (Element)rs.item(i);
			Element ae = (Element)re.getElementsByTagName("Action").item(0);
			if (!ae.getAttribute("type").equals("Proxy")) continue;
			if (!ae.getTextContent().equals(proxyId)) continue;
			tre = re;
			break;
		}
		
		if (tre == null) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document rdoc = db.parse(new ByteArrayInputStream((
			        "<Rule enabled=\"true\">"+
		            "<Name>"+proxyHost+"</Name>"+
		            "<Applications></Applications>"+
		            "<Action type=\"Proxy\">"+proxyId+"</Action>"+
		            "</Rule>"
				).getBytes()));
			tre = rdoc.getDocumentElement();
			tre = (Element)doc.importNode(tre, true);
			
			Node rln = doc.getElementsByTagName("RuleList").item(0);
			NodeList rns = ((Element)rln).getElementsByTagName("Rule");
			Node r1 = rns.getLength() == 0 ? null : rns.item(0);
			rln.insertBefore(tre, r1);
		}

		Element ae = (Element)tre.getElementsByTagName("Applications").item(0);
		String prgs = ae.getTextContent();
		String[] sr = prgs.split(";");
		for (String p: sr) if (p.trim().equals(prg)) return false;
		if (!prgs.trim().equals("")) prgs += "; ";
		prgs += prg;
		ae.setTextContent(prgs);
		
		saveDoc(doc, xmlFile);
		return true;
	}
	
	private static void addProxyRuleIfNeeded(String xmlFile, String host, String login, String pwd, String prg) throws Exception {
		
		FileLock lock = null;
		File lockFile = null;
		RandomAccessFile rFile = null;
		FileChannel lockCh = null;
		
		try {
			lockFile = new File(xmlFile+".lock");
			rFile = new RandomAccessFile(lockFile, "rw");
			lockCh = rFile.getChannel();
			lock = lockCh.lock();
	
			Element pe = addProxyIfNeeded(xmlFile, host, login, pwd);
			boolean added = addRuleIfNeeded(xmlFile, pe, prg);
			
			if (added) {
				String pexe = "C:/Program Files (x86)/Proxifier/Proxifier.exe "+xmlFile+" silent-load";
		        Misc.sysCall(pexe);
				Thread.sleep(1000);
			}
		} finally {
			lockFile.delete();
			lockCh.close();
			rFile.close();
			lock.release();
		}
	}
	
	public static void main(String[] args) throws Exception {
//		List<Host> hs = getHostList("C:/temp/ppp.ppx");
//		Log.log("%s", hs);
//		Element pe = addProxyIfNeeded("C:/temp/p.xml", "123.123.123.126", "login", "pwd");
//		Log.log("id=%s", pe.getAttribute("id"));

//		BUSY "80.93.28.133"        "61336"        "matumbo2013"        "rXhW8pu8"
//		BUSY "62.149.223.231"        "61336"        "matumbo2013"        "rXhW8pu8"

		addProxyRuleIfNeeded("proxy.xml", "80.93.28.133", "matumbo2013", "rXhW8pu8", "chrome1.exe");
		addProxyRuleIfNeeded("proxy.xml", "62.149.223.231", "matumbo2013", "rXhW8pu8", "chrome2.exe");
	}
}
