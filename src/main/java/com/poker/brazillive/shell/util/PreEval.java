package com.poker.brazillive.shell.util;

import com.poker.brazillive.shell.game.*;

import java.util.*;
import java.io.*;

/*drop table equity3_1opp;

CREATE TABLE `equity3_1opp` (
  `h1` tinyint DEFAULT NULL,
  `h2` tinyint DEFAULT NULL,
  `b1` tinyint DEFAULT NULL,
  `b2` tinyint DEFAULT NULL,
  `b3` tinyint DEFAULT NULL,
  `b4` tinyint DEFAULT NULL,
  `b5` tinyint DEFAULT NULL,
  `h1m` tinyint DEFAULT NULL,
  `h2m` tinyint DEFAULT NULL,
  `bm1` tinyint DEFAULT NULL,
  `bm2` tinyint DEFAULT NULL,
  `hs` decimal(4,3) DEFAULT NULL,
  `allin` decimal(4,3) DEFAULT NULL
  , KEY `i1` (`h1`,`h2`,`b1`,`b2`,`b3`,`b4`,`b5`,`h1m`,`h2m`,`bm1`,`bm2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert equity3_1opp select h1,h2,b1,b2,b3,b4,b5,h1m,h2m,bm1,bm2,
round(1.0*hs*1000)/1000.0,round(1.0*allin*1000)/1000.0
from equity3 where oppc=1;

select * from equity3_1opp;*/

/*
Looks like a bug:
board=9hKc4cQc, holes=2h2c, ret=0.31,0.37
board=9hKc4cQc, holes=2s2c, ret=0.31,0.38 - allin must be smaller then 2h2c because 2h reduces opp flush probability
board=9hKc4cQc, holes=2d2c, ret=0.31,0.37 - allin must be the same as for 2s2c
*/

public class PreEval {
	
	private static final int MAX_OPPC_FOR_DB = 5;

	private static final int ITER_COUNT_HS = 20000;
	private static final int ITER_COUNT_ALLIN = 20000;
//	private static final int ITER_COUNT_HS = 100;
//	private static final int ITER_COUNT_ALLIN = 100;
//	private static final int ITER_COUNT_HS = 10;
//	private static final int ITER_COUNT_ALLIN = 10;

	private static final int h = Deck.SUIT_HEARTS;
	private static final int d = Deck.SUIT_DIAMONDS;
	private static final int c = Deck.SUIT_CLUBS;
	private static final int s = Deck.SUIT_SPADES;

	public static final int HS_IND = 0;
	public static final int EHS_IND = 1;

	private PreEval() throws Exception {
		this.loadMemCacheFromFile();
	}
	
	private static PreEval inst = null;
	public static PreEval get() throws Exception {
		if (inst == null) inst = new PreEval();
		return inst;
	}
	
	private static long checkAddMask(long mask, Card[] cards) {
		for (Card c: cards) {
			if ((mask & c.getMask()) != 0) return -1;
			mask |= c.getMask();
		}
		return mask;
	}
	
	private static int[][][] allBoardSuits = { // [blen][snum][]
		{
			{h,h,h},
			{h,h,d},
			{h,d,h},
			{d,h,h},
			{h,d,c},
		},
		{
			{h,h,h,h},

			{h,h,h,d},
			{h,h,d,h},
			{h,d,h,h},
			{d,h,h,h},

			{h,h,d,d},
			{h,d,h,d},
			{h,d,d,h},

			{h,h,d,c},
			{h,d,h,c},
			{h,d,c,h},
			{d,h,h,c},
			{d,h,c,h},
			{d,c,h,h},

			{h,d,c,s},
		},
		{
			{h,h,h,h,h},

			{h,h,h,h,d},
			{h,h,h,d,h},
			{h,h,d,h,h},
			{h,d,h,h,h},
			{d,h,h,h,h},

			{h,h,h,d,c},
			{h,h,d,h,c},
			{h,d,h,h,c},
			{d,h,h,h,c},
			{h,h,d,c,h},
			{h,d,h,c,h},
			{d,h,h,c,h},
			{h,d,c,h,h},
			{d,h,c,h,h},
			{d,c,h,h,h},
			
			{h,d,c,s,h},
		},
	};
	
	protected byte[] memCache = null; //new byte[10][];
	
	private static byte[] int2bytes(int value) {
		return new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
	}

	protected byte[] getMemKey(int h1, int h2, int b1, int b2, int b3, int b4, int b5, int h1m, int h2m, int bm1, int bm2) {
		int[] fs4b = {h1,h2,b1,b2,b3,b4,b5};
		int[] fs6b = {h1m,h2m,bm1,bm2};

		int key1 = 0;
		for (int f4b: fs4b) {
			if (f4b == -1) f4b = 15;
			//Log.log(f4b+"="+Integer.toBinaryString(keyPart));
			if (key1 != 0) key1 = key1 << 4;
			assert (key1 & f4b) == 0;
			key1 |= f4b;
		}
		int key2 = 0;
		for (int f6b: fs6b) {
			//Log.log(f4b+"="+Integer.toBinaryString(keyPart));
			if (key2 != 0) key2 = key2 << 6;
			assert (key2 & f6b) == 0;
			key2 |= f6b;
		}
		
		//return new int[]{key1, key2};
		
		byte[] ret = new byte[keyLen];
		System.arraycopy(int2bytes(key1), 0, ret, 0, 4);
		System.arraycopy(int2bytes(key2), 0, ret, 4, 4);
		return ret;
	}
	protected byte[] getMemKey(Card[] board, Card[] holes) {
		Card[] ob = orderByIndex(board);
		Card[] oh = orderByIndex(holes);
		int[] mkey = PreEval.getKey(ob, oh);
		
		return this.getMemKey(oh[0].getRank(), oh[1].getRank(),
				ob[0].getRank(), ob[1].getRank(), ob[2].getRank(),
				ob.length > 3 ? ob[3].getRank() : -1,
				ob.length > 4 ? ob[4].getRank() : -1,
				mkey[0], mkey[1], mkey[2], mkey[3]);
	}
	
	public void loadMemCacheFromFile() throws Exception {
		ProcTimer t = new ProcTimer("Loading PreEval mem cache from file",0);

		t.logStartTime();
		
		File file = new File("hs_cache.bin");
		memCache = new byte[(int) file.length()];
		FileInputStream fis = new FileInputStream(file);
		fis.read(memCache);
		fis.close();

	    t.logFinishTime();
	}
	
	class MemKeyComparator implements Comparator<byte[]> {
	    public int compare(byte[] a, byte[] b) {
	    	int c = 0;
	    	for (int i = 0; i < keyLen; i++) {
		        c = Byte.compare(a[i], b[i]);
		        if (c != 0) return c;
	    	}
	    	return c;
	    }
	}
	
	protected void loadMemCacheFromDB() throws Exception {
		ProcTimer t = null;
		
		List<byte[]> cache = new ArrayList<byte[]>();
		
		File[] fs = FUtil.dRead(".");
		for (File f: fs) {
			if (!f.getName().contains(".sql")) continue;
			t = new ProcTimer("Loading big cache from "+f.getName(), 0);
			t.logStartTime();
			
			BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
			
			String str;
			int c = 0;
			while ((str = br.readLine()) != null) {
				String[] ts = str.split(",");
				if (ts.length < 10) continue;
				
				int[] nums = new int[11];
				for (int i = 0; i < 11; i++)
					nums[i] = Integer.parseInt(ts[i]);
				
				byte[] keys = this.getMemKey(nums[0],nums[1],nums[2],nums[3],nums[4],nums[5],nums[6],nums[7],nums[8],nums[9],nums[10]);
				assert keys.length == keyLen;
	
				double hs = Double.parseDouble(ts[12]);
				double allin = Double.parseDouble(ts[13]);
				
				byte bhs = (byte)Math.round(hs*100);
				byte ballin = (byte)Math.round(allin*100);
				
				byte[] row = new byte[keys.length+2];
				System.arraycopy(keys, 0, row, 0, keys.length);
				row[row.length-2] = bhs;
				row[row.length-1] = ballin;
				
				cache.add(row);
				
				t.logTime(c++);
			}
			br.close();
			t.logFinishTime();
			//assert c == size;
		}
		
		t = new ProcTimer("Sorting",0);
		t.logStartTime();
		Collections.sort(cache, new MemKeyComparator());
		t.logFinishTime();
		
		t = new ProcTimer("Saving to file",0);
		t.logStartTime();
		
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File("hs_cache.bin"))));
		//out.writeInt(memCache.length);
		for (byte[] row: cache) {
			assert row.length == recLen;
			out.write(row);
//			for (byte b: row) System.out.print(b+" ");
//			System.out.println("");
		}
		out.close();
		t.logFinishTime();
	}
	
	private int compareKeys(byte[] data, int keyInd, byte[] searchKey) {
		int c = 0;
		for (int i = 0; i < keyLen; i++) {
	        c = Byte.compare(data[keyInd*recLen+i], searchKey[i]);
	        if (c != 0) return c;
		}
		return c;
	}
	
	private int recLen = 10;
	private int keyLen = 8;

	public double[] getMemCache(Card[] board, Card[] holes) throws Exception {
		assert board.length >= 3 && board.length <= 5: "Board "+Card.cards2Str(board);
		assert holes.length == 2: "Holes: "+Card.cards2Str(holes);
		byte[] key = this.getMemKey(board, holes);
//		Log.log("Finding key: "+b2s(key,0,key.length));
		
		int leftInd = 0;
		assert this.memCache.length % recLen == 0;
		int rightInd = this.memCache.length/recLen-1;
		int foundInd = -1;
		
		while (rightInd - leftInd > 1) {
			int centerInd = (leftInd+rightInd)/2;
//			Log.log("left="+leftInd+", center="+centerInd+", right="+rightInd);
			int comp = compareKeys(memCache,centerInd, key);
			if (comp < 0) leftInd = centerInd;
			else if (comp > 0) rightInd = centerInd;
			else if (comp == 0) {
				foundInd = centerInd;
				break;
			}
		}
//		Log.log("Finished: left="+leftInd+", right="+rightInd);
//		Log.log("Left key: "+b2s(memCache, leftInd*recLen, leftInd*recLen+keyLen));
//		Log.log("Right key: "+b2s(memCache, rightInd*recLen, rightInd*recLen+keyLen));
		if (foundInd == -1) {
			if (compareKeys(memCache,leftInd, key) == 0) foundInd = leftInd;
			else if (compareKeys(memCache,rightInd, key) == 0) foundInd = rightInd;
			else assert false;
		}
		
//		Log.log("Found key: "+b2s(memCache, foundInd*recLen, foundInd*recLen+keyLen));
//		Log.log("foundInd="+foundInd);
		
		byte ihs = memCache[foundInd*recLen+keyLen];
		byte iallin = memCache[foundInd*recLen+keyLen+1];
		
		double[] ret = {ihs/100.0, iallin/100.0};
		//Log.log("board="+Card.cards2Str(board)+", holes="+Card.cards2Str(holes)+", key="+key[0]+","+key[1]+", ind="+ind+", val="+val+", ret="+ret[0]+","+ret[1]);
		return ret;
	}
	
	
	private static int[] pows = {1,2,4,6,8,10,12,14,16,20,25,40};
	private static String getRow(int oppc, Card[] board, Card[] holes, int[] iKey, double hsEq, double allinEq) {
		String ret = "";
		//ret += ""+oppc;
		for (Card c: holes) ret += c.getRank()+",";
		for (int i = 0; i < 5; i++) {
			if (i < board.length) ret += board[i].getRank()+",";
			else ret += "-1,";
		}
		for (int i = 0; i < 4; i++) ret += iKey[i]+",";
		ret += "'"+Card.cards2Str(board)+" "+Card.cards2Str(holes)+"',";
		
		ret +=  ((int)(hsEq*10000))/10000.0+",";

		for (int pow: pows) {
			double p = Math.pow(allinEq, pow);
			ret += ((int)(p*10000))/10000.0+",";
		}
		
		return ret;
	}
	
	private static void logf(String fmt, Object... args) throws Exception {
		System.out.println(String.format(fmt, args));
	}
	
//	private static void train(int from, int to) throws Exception {
//		int insEach = 1000;
//		long st = System.currentTimeMillis();
//		long lt = -1; int lastInsCount = -1;
//		
//		Log.log("# Starting preeval with counts: "+ITER_COUNT_HS+" "+ITER_COUNT_ALLIN);
//		
//		String lastInsFile = "lastins."+from+"-"+to;
//		
//		if ((new File(lastInsFile)).exists()) {
//			from = Integer.parseInt(FUtil.fRead(lastInsFile).get(0))+1;
//			Log.log("From is changed to: "+from);
//		}
//
//		
//		ProcTimer t = new ProcTimer("#Calc", to-from);
//		t.logStartTime();
//		long lastStopCheck = System.currentTimeMillis();
//		int count = 0;
//		
//		for (int b1 = Deck.RANK_2; b1 <= Deck.RANK_ACE; b1++) {
//			logf("\n# b1="+b1+"\n");
//		for (int b2 = b1; b2 <= Deck.RANK_ACE; b2++) {
//			logf("\n# b2="+b2+"\n");
//		for (int b3 = b2; b3 <= Deck.RANK_ACE; b3++) {
//			boolean[] newBoard = {true,true,true};
//		for (int b4 = b3; b4 <= Deck.RANK_ACE; b4++) {
//			newBoard[1] = true; newBoard[2] = true;
//		for (int b5 = b4; b5 <= Deck.RANK_ACE; b5++) {
//			newBoard[2] = true;
//			int[] allBoardRanks = {b1,b2,b3,b4,b5};
//			//int[] allBoardRanks = {3,4,5,6,7};
//					
//			for (int blen = 3; blen <= 5; blen++) {
//			//for (int blen = 5; blen <= 5; blen++) {
//				if (! newBoard[blen-3]) continue;
//				newBoard[blen-3] = false;
//				
//				for (int h1 = Deck.RANK_2; h1 <= Deck.RANK_ACE; h1++) {
//				for (int h2 = h1; h2 <= Deck.RANK_ACE; h2++) {
//						
//					for (int oppc = 1; oppc <= 1; oppc++) {
//	
//						count++; // oppc=1: 770133, oppc=1-3: 2310399, oppc=4-5: 1540266
//						t.logTime(count-from);
//						logf("# count = "+count);
//						//if (true) continue;
//						if (count < from) continue;
//						if (count > to) continue;
//
//						int[] holesRanks = {h1,h2};
//						//int[] holesRanks = {6,10};
//						
//						int[] boardRanks = new int[blen];
//						System.arraycopy(allBoardRanks, 0, boardRanks, 0, blen);
//
//						String sql = train(oppc,holesRanks,boardRanks);
//						logf(sql);
//
//						long ct = System.currentTimeMillis();
//						if (ct - lastStopCheck > 5000) {
//							if ((new File("stopcalc")).exists()) {
//								FUtil.fPut(lastInsFile, ""+count);
//								return;
//							}
//							lastStopCheck = ct;
//						}
//					}
//				}}
//			}
//			//System.exit(0);
//		}}}}}
//		
//		FUtil.fPut(lastInsFile, ""+count);
//		
//		t.logFinishTime();
//	}
//	
//	private static BVec flatBVec = BVec.getFlatBVec();
//	
//	private static String train(int oppc, int[] holesRanks, int[] boardRanks) throws Exception {
//
//		Calc calc = new Calc();
//		int blen = boardRanks.length;
//		BVecSimple[] hands = new BVecSimple[oppc];
//		for (int i = 0; i < oppc; i++) hands[i] = flatBVec.getSimpleBVec();
//		//BVecSimple[] hands = {BVecSimple.get1HandBVec(new Card("3h"), new Card("3d"))};
//		int[][] boardSuits = allBoardSuits[blen-3];
//		
//		String sql ="";
//		int allCount = 0;
//		
//		for (int[] bss: boardSuits) {
//			Card[] board = new Card[blen];
//			for (int i = 0; i < board.length; i++) board[i] = Card.getCard(boardRanks[i],bss[i]);
//			//Log.log("Board: "+Card.cards2Str(board)+"\n");
//			long boardMask = checkAddMask(0,board);
//			if (boardMask == -1) continue;
//
//			Map<String,Integer> readyHolesSuits = new HashMap<String,Integer>();
//			//int[][] holesSuits = getMeaningHolesSuits(bss);
//			for (int s1 = 0; s1 < 4; s1++) for (int s2 = 0; s2 < 4; s2++) { 
//			//for (int[] hss: holesSuits) {
//				Card[] holes = new Card[2];
//				holes[0] = Card.getCard(holesRanks[0],s1);
//				holes[1] = Card.getCard(holesRanks[1],s2);
//				//Log.log("\tHoles: "+Card.cards2Str(holes)+"\n");
//				//for (int i = 0; i < holes.length; i++) holes[i] = new Card(holesRanks[i],hss[i]);
//				
//				long holesMask = checkAddMask(0,holes);
//				if (holesMask == -1) continue;
//				if ((holesMask & boardMask) != 0) continue;
//				
//				int[] iKey = getKey(board,holes);
//				String key = "";
//				for (int ik: iKey) key += ik+","; //key += Integer.toBinaryString(ik)+",";
//				if (readyHolesSuits.containsKey(key)) continue;
//				readyHolesSuits.put(key, 1);
//				//Log.log("\tEVAL\n");
//				
//				allCount++;
//				/*//if (true) continue;
//				if (count < from) continue;
//				if (count > to) System.exit(0);*/
//
//				double hsEq = calc.calcEquity(board, holes, hands, false,ITER_COUNT_HS,false);
//				double allinEq = -1;
//				if (board.length < 5) allinEq = calc.calcEquity(board, holes, hands, true,ITER_COUNT_ALLIN,false);
//				else allinEq = hsEq;
//				
//	//				if (!sql.equals("")) sql += ",\n";
//	//				sql += "("+getSql2(oppc,board,holes,iKey,hsEq,allinEq)+")";
//				sql += getRow(oppc,board,holes,iKey,hsEq,allinEq)+"\n";
//				
//				/*String holesStr = "";
//				for (Card c: holes) holesStr += ""+c;
//				String boardStr = "";
//				for (Card c: board) boardStr += ""+c;
//				Log.log("%s %s hs=%1.3f allin=%1.3f\n", holesStr, boardStr, hsEq, allinEq);*/
//				
//				//if (count > 10) System.exit(0);
//			}
//		}
//		return sql;
//	}

	private static Card[] orderByIndex(Card[] cards) {
		Card[] ret = new Card[cards.length];
		System.arraycopy(cards, 0, ret, 0, cards.length);
		for (int i = 0; i < cards.length-1; i++) {
			for (int j = 1; j < cards.length; j++) {
				if (ret[j].getIndex() < ret[j-1].getIndex()) {
					Card tmp = ret[j];
					ret[j] = ret[j-1];
					ret[j-1] = tmp;
				}
			}
		}
		return ret;
	}
	
	public static int getCount = 0;
	public double[] get(Card[] board, Card[] holes, int oppc) throws Exception {
		getCount++;
		assert oppc == 1;
		return getMemCache(board, holes);
	}
	
	private static int countBits(int mask) {
		// From here - http://stackoverflow.com/questions/109023/best-algorithm-to-count-the-number-of-set-bits-in-a-32-bit-integer
	    mask = mask - ((mask >> 1) & 0x55555555);
	    mask = (mask & 0x33333333) + ((mask >> 2) & 0x33333333);
	    return (((mask + (mask >> 4)) & 0x0F0F0F0F) * 0x01010101) >> 24;
	}
	
	
	private static int[] getKey(Card[] board, Card[] holes) {
		int h1Match = 0; int h2Match = 0;
		int[] bMatches = new int[5]; 
		int bMatchesCount = 0;

		for (int i = 0; i < board.length; i++) {
			int bs = board[i].getSuit();
			if (holes[0].getSuit() == bs) h1Match |= 1 << i;
			if (holes[1].getSuit() == bs) h2Match |= 1 << i;
			
			//if (holes[0].getSuit() == bs || holes[1].getSuit() == bs) continue;
			
			boolean suitIsInBoardMasks = false;
			for (int j = 0; j < bMatchesCount; j++) {
				if ((bMatches[j] & 1 << i) != 0) {
					suitIsInBoardMasks = true;
					break;
				}
			}
			if (suitIsInBoardMasks) continue;
			int newBoardMask = 1 << i;
			for (int j = i+1; j < board.length; j++) {
				if (bs == board[j].getSuit()) newBoardMask |= 1 << j;
			}
			bMatches[bMatchesCount++] = newBoardMask;
		}
		
		int[] mm = new int[7]; // Meaning matches
		int mmc = 0;
		if (board.length == 3) {
			if (holes[0].getSuit() == holes[1].getSuit()) {
				if (countBits(h1Match) >= 1) { mm[mmc++] = h1Match; mm[mmc++] = h2Match; }
				else mmc += 2;
			} else {
				if (countBits(h1Match) >= 2) mm[mmc++] = h1Match; else mmc++;
				if (countBits(h2Match) >= 2) mm[mmc++] = h2Match; else mmc++;
			}
			for (int bMatch: bMatches) if (countBits(bMatch) >= 2) mm[mmc++] = bMatch;

		} else if (board.length == 4) {
			if (holes[0].getSuit() == holes[1].getSuit()) {
				if (countBits(h1Match) >= 2) { mm[mmc++] = h1Match; mm[mmc++] = h2Match; }
				else mmc += 2;
			} else {
				if (countBits(h1Match) >= 3) mm[mmc++] = h1Match; else mmc++;
				if (countBits(h2Match) >= 3) mm[mmc++] = h2Match; else mmc++;
			}
			for (int bMatch: bMatches) if (countBits(bMatch) >= 2) mm[mmc++] = bMatch;

		} else if (board.length == 5) {
			if (holes[0].getSuit() == holes[1].getSuit()) {
				if (countBits(h1Match) >= 3) { mm[mmc++] = h1Match; mm[mmc++] = h2Match; }
				else mmc += 2;
			} else {
				// 6sQh2d5h6h AhTc: Ah doesn't allow some flushes so the allin equity is different then AsTc
				if (countBits(h1Match) >= 3) mm[mmc++] = h1Match; else mmc++;
				if (countBits(h2Match) >= 3) mm[mmc++] = h2Match; else mmc++;
			}
			for (int bMatch: bMatches) if (countBits(bMatch) >= 3) mm[mmc++] = bMatch;
		}
		
		return mm;
	}
	
	private static void testGetPerf() throws Exception {
		PreEval preEval = new PreEval();
		
		int M = 5;
		int time = 5; // secs
		int blen = 3;
		int oppc = 1;
		
		//Log.log("# Hit ENTER..."); System.in.read();
		for (int m = 0; m < M; m++) {
			double sum = 0;
			long st = System.currentTimeMillis();
			int c = 0;
			while (System.currentTimeMillis() - st < time*1000) {
				blen = Misc.rand.nextInt(3)+3;
				Card[] board = Card.getRandomCards(blen,0);
				Card[] holes = Card.getRandomCards(2,Card.cards2Mask(board));
				//oppc = Misc.rand.nextInt(5)+1;
				
				double[] res = preEval.get(board,holes,oppc);
				sum += res[0]+res[1];
				c++;
			}
			Log.log("%1.8f msecs/get, %d gets/sec; ", 1000.0*time/c, c/time);
		}
	}
	
	private static void experBoard() throws Exception {
		Card[] board = Card.str2Cards("9h8hKs");
		Card[] holes = Card.str2Cards("Jd3d");
		
		PreEval.get();
		long dead = Card.cards2Mask(board) | Card.cards2Mask(holes);
		Map<Integer,List<Card[]>> stat = new TreeMap<Integer,List<Card[]>>();
		
		for (int i1 = 0; i1 < 52; i1++) for (int i2 = 0; i2 < 52; i2++) {
			if (i1 == i2) continue;
			Card b4 = Card.getCard(i1);
			Card b5 = Card.getCard(i2);
			if ((b4.getMask() & dead) != 0) continue;
			if ((b5.getMask() & dead) != 0) continue;
			
			Card[] cb = new Card[]{board[0], board[1], board[2], b4, b5};
			int hs = (int)Math.round(PreEval.get().get(cb, holes, 1)[0]*100);
			
			List<Card[]> l = stat.get(hs);
			if (l == null) l = new ArrayList<Card[]>();
			l.add(new Card[]{b4,b5});
			stat.put(hs, l);
		}
		for (Integer hs: stat.keySet()) {
			String str = "";
			List<Card[]> l = stat.get(hs);
			str += String.format("hs=%d%% (%2d): ",hs, l.size());
			for (Card[] bp: l) str += Card.cards2Str(bp)+" ";
			Log.log(str);
		}
		Log.log("Total %d", stat.size());

		Map<Card, List<Card>> tree = new HashMap<Card, List<Card>>();
		for (Integer hs: stat.keySet()) {
			List<Card[]> l = stat.get(hs);
			Card c1 = l.get(0)[0];
			Card c2 = l.get(0)[1];
			
			if (tree.get(c1) == null) tree.put(c1, new ArrayList<Card>());
			tree.get(c1).add(c2);
		}
		
		int c = 0;
		List<Card> ks = new ArrayList<Card>(tree.keySet());
		Collections.sort(ks, new Comparator<Card>() {
			public int compare(Card c1, Card c2) {
				return Integer.compare(c1.getIndex(), c2.getIndex());
			}
		});
		for (Card c1: ks) {
			Log.log(""+c1);
			for (Card c2: tree.get(c1)) {
				Log.log("\t"+c2);
				c++;
			}
		}
		Log.log("Total: %d", c);
	}
	
	public static void main(String[] args) throws Exception {
		//Log.log("# Hit ENTER...\n"); System.in.read();
		
		//testEval();
		//testTrain();
		//testGetPerf();
		//testGetPerfBoard();
		//testGetSimple();
		//testGetBVec();
		//testGetDealPerf();
		//testBoardGet();
		//testBoardNorm();
		//testCommon();
		//testNormAndEval();
		//testHalf();
		//exper();
		//testGetNew();
		//testGetMemCache();
		//experCat();
		//experBoard();
		//testOne();
		
		PreEval preEval = new PreEval();
//		preEval.loadMemCacheFromDB();
//		PreEval.testGetMemCache();
		System.exit(0);
	}
}
