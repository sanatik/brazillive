package com.poker.brazillive.shell.game;

import java.io.*;
import com.poker.brazillive.shell.util.*;

public class Card implements Serializable {
	private int index;
	private long mask = -1;
	private static Card[] cards = new Card[52];
	
	static {
		for (int i = 0; i < 52; i++) cards[i] = new Card(i);
	}
	
	public static Card getCard(int index) {
		assert index >= 0 && index <= 51;
		return cards[index];
	}
	public static Card getCard(int rank, int suit) {
		int index = rank*4+suit;
		return getCard(index);
	}
	public static Card getCard(String s) {
		return getCard(Deck.parseRank(s.substring(0,1)),Deck.parseSuit(s.substring(1,2)));
	}
	
	private Card(int index) {
		assert index >= 0 && index <= 51;
		assert cards[index] == null;
		this.index = index;
	}
//	private Card(int rank, int suit) {
//		this.index = rank*4+suit;
//	}
//	private Card(String s) {
//		this(Deck.parseRank(s.substring(0,1)),Deck.parseSuit(s.substring(1,2)));
//	}
	public int getIndex() {
		return this.index;
	}
	public int getRank() {
		return this.index/4;
	}
	public int getSuit() {
		return this.index%4;
	}
	public long getMask() {
		if (this.mask == -1)
			this.mask = Deck.createCardMask(this.getRank(), this.getSuit());
		return this.mask;
	}
	public static Card[] join(Card[] ar1, Card[] ar2) {
		Card[] ret = new Card[ar1.length+ar2.length];
		System.arraycopy(ar1, 0, ret, 0, ar1.length);
		System.arraycopy(ar2, 0, ret, ar1.length, ar2.length);
		return ret;
	}
	public static Card[] join(Card[] ar1, Card c) {
		return Card.join(ar1, new Card[]{c});
	}
	public static Card[] cloneCards(Card[] ar) {
		Card[] ret = new Card[ar.length];
		System.arraycopy(ar, 0, ret, 0, ar.length);
		return ret;
	}
	
	public static String mask2Str(long mask) {
		if (mask == 0) return "";
		return Deck.cardMaskString(mask, "");
	}
	public static Card[] mask2Cards_old(long mask) {
		return str2Cards(mask2Str(mask), false);
	}
	public static Card[] mask2Cards(long mask) {
		Card[] ret = new Card[52];
		int c = 0;
		for (int i = 0; i < 52; i++) {
			if ((mask & Card.getCard(i).getMask()) != 0) ret[c++] = Card.getCard(i);
		}
		Card[] ret2 = new Card[c];
		System.arraycopy(ret, 0, ret2, 0, c);
		return ret2;
	}
	public static String cards2Str(Card[] cards, String delim) {
		String ret = "";
		for (int i = 0; i < cards.length; i++) {
			if (i != 0) ret += delim;
			ret += cards[i].toString();
		}
		return ret;
	}
	public static String cards2Str(Card[] cards) {
		return cards2Str(cards, "");
	}
	public static String cards2StrPre(Card[] cards) {
		assert cards.length == 2;
		if (cards[0].getIndex() < cards[1].getIndex())
			cards = new Card[]{cards[1], cards[0]};
		String ret = ""+Deck.rankString(cards[0].getRank())+Deck.rankString(cards[1].getRank());
		if (cards[0].getSuit() == cards[1].getSuit()) ret += "s";
		else ret += "o";
		return ret;
	}
	
	public static int[] cards2Ranks(Card[] cards) {
		int[] ranks = new int[cards.length];
		for (int i = 0; i < cards.length; i++) ranks[i] = cards[i].getRank();
		return ranks;
	}
	
	public static Card[] str2Cards(String str) {
		return str2Cards(str, true);
	}
	public static Card[] str2Cards(String str, boolean repl) {
		if (repl) str = str.replaceAll("[ ,]", "").replaceAll("10", "T");
		Card[] cards = new Card[str.length()/2];
		for (int i = 0; i < str.length()/2; i++) {
			cards[i] = getCard(str.substring(i*2, i*2+2));
		}
		return cards;
	}
	public static long cards2Mask(Card[] cards) {
		long m = 0;
		for (Card c: cards) m |= c.getMask();
		return m;
	}
	public static Card[] ar2Cards(int[] ranks, int[] suits) {
		assert ranks.length == suits.length;
		Card[] ret = new Card[ranks.length];
		for (int i = 0; i < ranks.length; i++) {
			ret[i] = getCard(ranks[i], suits[i]);
		}
		return ret;
	}
//	public static long cards2Num(Card[] cards) {
//		long ret = 0;
//		for (Card c: cards) {
//			if (ret != 0) ret *= 100;
//			ret += c.getIndex()+1; // Index can be 0 for 2h
//		}
//		return ret;
//	}
//	public static Card[] num2Cards(long num) {
//		int count = 0;
//		if (num != 0) {
//			count = (int)Math.log10(num)+1;
//			if (count % 2 == 1) count++;
//			count /= 2;
//		}
//		Card[] ret = new Card[count];
//		for (int i = 0; i < count; i++) {
//			ret[ret.length-1-i] = Card.getCard((int)(num % 100)-1);
//			num = num/100;
//		}
//		return ret;
//	}
	
	public static boolean checkNoRepeat(Card[] cards) {
		long m = 0;
		for (Card c: cards) {
			if ((m & c.getMask()) != 0) return false;
			m |= c.getMask();
		}
		return true;
	}
	public static Card[] orderByIndex(Card[] cards) {
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
	public static Card[] getRandomCards(int len, long deadMask) {
		Card[] cards = new Card[len];
		long m = 0;
		for (int i = 0; i < len; i++) {
			Card c = null;
			do {
				c = Card.getCard(Misc.rand.nextInt(52));
			} while ((m & c.getMask()) != 0 || (deadMask & c.getMask()) != 0);
			m |= c.getMask();
			cards[i] = c;
		}
		return cards;
	}
	private static Card[][][] seqLength2Seq = new Card[2][][];

	public static Card[][] getAllOrderedCardSeq(int seqLength) {
		
		assert seqLength >=0 && seqLength <= 2 : "Incorrect segLength="+seqLength;
		
		if (seqLength == 0) return new Card[0][2];
		
		if (seqLength2Seq[seqLength-1] == null) {

			int retLength = 1;
			for (int i = 0; i < seqLength; i++) retLength *= 52-i;
			Card[][] ret = new Card[retLength][seqLength];
			int curSeqCount = 0;
			
			//int rank = 0, suit = 0;
			int card = 0;
			Card[] curSeq = new Card[seqLength];
			for (int c1 = 0; c1 < 52; c1++) {
				/*rank = c1 / 4;
				suit = c1 % 4;
				card = Deck.createCardMask(rank, suit);*/
				curSeq[0] = Card.getCard(c1);
				if (seqLength == 1) {
					ret[curSeqCount++] = curSeq.clone();
					continue;
				}
				for (int c2 = 0; c2 < 52; c2++) {
					if (c2 == c1) continue;
					/*rank = c2 / 4;
					suit = c2 % 4;
					card = Deck.createCardMask(rank, suit);*/
					curSeq[1] = Card.getCard(c2);
					if (seqLength == 2) {
						ret[curSeqCount++] = curSeq.clone();
						continue;
					}
				}
			}
			seqLength2Seq[seqLength-1] = ret;
		}
		return seqLength2Seq[seqLength-1];
	}

	public String toString() {
		return Deck.cardMaskString(this.getMask());
	}
	public boolean equals(Object obj) {
		if (! (obj instanceof Card)) return false;
		Card card = (Card)obj;
		return this.index == card.index;
	}
	
	public static void main(String[] args) throws Exception {
	}
}
