//package com.poker.brazillive.shell.util;
//
//import java.io.*;
//import java.util.*;
//import game.*;
//
//import org.pokersource.eval.*;
//import org.pokersource.util.*;
//
//public class Calc {
//	
//
//	public static long calcEval(Card[] board, Card[] holes) {
//		Card[] cards = new Card[board.length + holes.length];
//		System.arraycopy(board, 0, cards, 0, board.length);
//		System.arraycopy(holes, 0, cards, board.length, holes.length);
//		int[] ranks = new int[cards.length];
//		int[] suits = new int[cards.length];
//		for (int i = 0; i < cards.length; i++) {
//			ranks[i] = cards[i].getRank();
//			suits[i] = cards[i].getSuit();
//		}
//		return StandardEval.EvalHigh(ranks, suits);
//	}
//
//}
