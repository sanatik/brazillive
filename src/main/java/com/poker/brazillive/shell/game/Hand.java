package com.poker.brazillive.shell.game;

import java.util.*;
import com.poker.brazillive.shell.util.*;

public class Hand {
	
	private static Hand[][] allHands = new Hand[52][52];

	private static short[][] hand2ind = new short[52][52];
	private static Hand[] ind2hand = new Hand[1326];
	
	static {
		short ind = 0;
		for (int i1 = 0; i1 < 52; i1++) for (int i2 = 0; i2 < i1; i2++) {
			allHands[i1][i2] = new Hand(Card.getCard(i1), Card.getCard(i2));
			hand2ind[i1][i2] = ind;
			ind2hand[ind] = Hand.getHand(i1, i2);
			ind++;
		}
	}
	
	private Card c1, c2; // Must not be public. Hand is just one and it's cards must not be changed
	
	private Hand(Card c1, Card c2) {
		assert allHands[c1.getIndex()][c2.getIndex()] == null;
		if (c1.getIndex() > c2.getIndex()) {
			this.c1 = c1;
			this.c2 = c2;
		} else {
			this.c1 = c2;
			this.c2 = c1;
		}
	}
	
	public static Hand getHand(int i1, int i2) {
		if (i1 < i2) { int tmp = i1; i1 = i2; i2 = tmp; }
		return allHands[i1][i2];
	}
	public static Hand getHand(Card c1, Card c2) {
		return getHand(c1.getIndex(), c2.getIndex());
	}
	public static Hand getHand(Card[] cards) {
		return getHand(cards[0].getIndex(), cards[1].getIndex());
	}
	public static Hand getHand(short ind) {
		return Hand.ind2hand[ind];
	}
	public Card[] getCards() {
		return new Card[]{c1, c2};
	}
	public short getInd() {
		return hand2ind[this.c1.getIndex()][this.c2.getIndex()];
	}
	public long getMask() {
		return this.c1.getMask() | this.c2.getMask();
	}
	public int r1() {
		return this.c1.getRank();
	}
	public int r2() {
		return this.c2.getRank();
	}
	public int s1() {
		return this.c1.getSuit();
	}
	public int s2() {
		return this.c2.getSuit();
	}
	public boolean isPair() {
		return this.r1() == this.r2();
	}
	public boolean isConn() {
		return Math.abs(this.r1() - this.r2()) == 1;
	}
	public boolean isHalfConn() {
		return Math.abs(this.r1() - this.r2()) == 2;
	}
	public boolean isSuited() {
		return this.s1() == this.s2();
	}
	public boolean isSuitedConn() {
		return this.isConn() && this.isSuited();
	}
	public boolean isSuitedHalfConn() {
		return this.isConn() && this.isSuited();
	}
	
	private abstract static class HandsFilter {
		public abstract boolean takeHand(Hand h);
	}
	
	public static List<Hand> getHands(HandsFilter f) {
		List<Hand> ret = new ArrayList<Hand>();
		for (int i = ind2hand.length-1; i >= 0; i--)
			if (f.takeHand(ind2hand[i])) ret.add(ind2hand[i]);
		return ret;
	}
	
	public static Hand getRandomHand() {
		return ind2hand[Misc.rand.nextInt(1326)];
	}
	
//	public static List<Hand> getPairs(int rankFrom, int rankTo) {
//		return getHands(new HandsFilter() {
//			public boolean takeHand(Hand h) {
//				return h.isPair() && h.r1() >= rankFrom && h.r1() <= rankTo;
//			}
//		});
//	}
//	public static List<Hand> getConn(int majorRankFrom, int majorRankTo) {
//		return getHands(new HandsFilter() {
//			public boolean takeHand(Hand h) {
//				return h.isConn() && h.r1() >= majorRankFrom && h.r1() <= majorRankTo;
//			}
//		});
//	}
//	public static List<Hand> getConnSuited(int majorRankFrom, int majorRankTo) {
//		return getHands(new HandsFilter() {
//			public boolean takeHand(Hand h) {
//				return h.isSuitedConn() && h.r1() >= majorRankFrom && h.r1() <= majorRankTo;
//			}
//		});
//	}
//	public static List<Hand> getHalfConn(int majorRankFrom, int majorRankTo) {
//		return getHands(new HandsFilter() {
//			public boolean takeHand(Hand h) {
//				return h.isHalfConn() && h.r1() >= majorRankFrom && h.r1() <= majorRankTo;
//			}
//		});
//	}
//	public static List<Hand> getHalfConnSuited(int majorRankFrom, int majorRankTo) {
//		return getHands(new HandsFilter() {
//			public boolean takeHand(Hand h) {
//				return h.isSuitedHalfConn() && h.r1() >= majorRankFrom && h.r1() <= majorRankTo;
//			}
//		});
//	}
//	public static List<Hand> getBroadway() {
//		return getHands(new HandsFilter() {
//			public boolean takeHand(Hand h) {
//				return h.r2() >= Deck.RANK_TEN;
//			}
//		});
//	}
//	public static List<Hand> getAx(int rankFrom, int rankTo) {
//		return getHands(new HandsFilter() {
//			public boolean takeHand(Hand h) {
//				return h.r1() == Deck.RANK_ACE && h.r2() >= rankFrom && h.r2() <= rankTo;
//			}
//		});
//	}
//	public static List<Hand> getAxSuited(int rankFrom, int rankTo) {
//		return filterSuited(getAx(rankFrom, rankTo));
//	}
//	public static List<Hand> getBigger(int r) {
//		return getHands(new HandsFilter() {
//			public boolean takeHand(Hand h) {
//				return h.r2() >= r;
//			}
//		});
//	}
//	
//	public static List<Hand> filterSuited(List<Hand> hs) {
//		List<Hand> ret = new ArrayList<Hand>();
//		for (Hand h: hs) if (h.isSuited()) ret.add(h);
//		return ret;
//	}
//	
//	public static List<Hand> getXx(int majorRank, int lowerRankFrom, int lowerRankTo) {
//		return getHands(new HandsFilter() {
//			public boolean takeHand(Hand h) {
//				return h.r1() == majorRank && h.r2() >= lowerRankFrom && h.r2() <= lowerRankTo;
//			}
//		});
//	}
	
	public static List<Hand> fromStrPre(String str) {
		assert str.length() >= 2 && str.length() <= 3;
		int r1 = Deck.parseRank(str.substring(0,1));
		int r2 = Deck.parseRank(str.substring(1,2));
		boolean suited = str.length() == 3 && str.substring(2,3).equals("s");
		if (r1 == r2) assert !suited;
		return getHands(new HandsFilter() {
			public boolean takeHand(Hand h) {
				return h.r1() == r1 && h.r2() == r2 && h.isSuited() == suited;
			}
		});
	}
	public static List<Hand> fromStrAbdul(String str) {
		if (str.length() <= 3) return fromStrPre(str);
		String s1 = str.split("-")[0];
		String s2 = str.split("-")[1];
		assert s1.length() >= 2 && s1.length() <= 3;
		assert s2.length() >= 2 && s2.length() <= 3;
		assert s1.length() == s2.length(); 
		int r1 = Deck.parseRank(s1.substring(0,1));
		int r2 = Deck.parseRank(s1.substring(1,2));
		if (s1.length() == 3) assert s1.substring(2,3).equals("s") || s1.substring(2,3).equals("o");
		if (s2.length() == 3) assert s2.substring(2,3).equals("s") || s2.substring(2,3).equals("o");
		boolean suited = s1.length() == 3 && s1.substring(2,3).equals("s");
		if (r1 == r2) assert !suited;
		if (suited) assert s2.substring(2,3).equals("s");
		boolean notSuited = s1.length() == 3 && s1.substring(2,3).equals("o");
		if (notSuited) assert s2.substring(2,3).equals("o");
		int r3 = Deck.parseRank(s2.substring(0,1));
		int r4 = Deck.parseRank(s2.substring(1,2));
		if (r1 == r2) assert r3 == r4 && r1 != r3;
		else assert r1 == r3;
		if (r1 != r2) assert r2 < r1 && r4 < r3;

		if (r2 < r4) {
			int tmp = r1; r1 = r3; r3 = tmp;
			tmp = r2; r2 = r4; r4 = tmp;
		}
		
		List<Hand> ret = null;
		if (r1 == r2) {
			int rf = r3, rt = r1;
			ret = getHands(new HandsFilter() {
				public boolean takeHand(Hand h) {
					return h.isPair() && h.r1() >= rf && h.r1() <= rt;
				}
			});
		} else {
			int rm = r1, rf = r4, rt = r2;
			ret = getHands(new HandsFilter() {
				public boolean takeHand(Hand h) {
					if (suited && !h.isSuited()) return false;
					if (notSuited && h.isSuited()) return false;
					return h.r1() == rm && h.r2() >= rf && h.r2() <= rt;
				}
			});
		}
		Collections.sort(ret, new Comparator<Hand>() {
			public int compare(Hand h1, Hand h2) {
				return -Integer.compare(h1.r1()+h1.r2(), h2.r1()+h2.r2());
			}
		});
		return ret;
	}
	
	public static Hand[] getInd2Hands() {
		return Hand.ind2hand.clone();
	}
	
	public String toString() {
		return ""+this.c1+this.c2;
	}
	public String toStringPre() {
		return Deck.rankString(this.r1())+Deck.rankString(this.r2())+(this.s1() == this.s2() ? "s" : "o");
	}
	
	public static void testAbdul() {
		for (String abdulStr: new String[]{"AKs", "AKs-ATs", "ATs-AKs", "KQ-KT"})
			System.out.println(Hand.fromStrAbdul(abdulStr));
	}
	public static void main(String[] args) {
		testAbdul();
	}
}
