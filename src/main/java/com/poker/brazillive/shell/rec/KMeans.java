package com.poker.brazillive.shell.rec;

import java.util.*;
import com.poker.brazillive.shell.util.*;

public class KMeans {

	private static double[] avg(List<double[]> ars) {
		double[] avg = new double[ars.get(0).length];
		for (double[] ar: ars) {
			assert ar.length == avg.length;
			for (int i = 0; i < ar.length; i++) avg[i] += ar[i];
		}
		for (int i = 0; i < avg.length; i++) avg[i] /= ars.size();
		
		return avg;
	}
	
	public static class Res {
		List<double[]> cents;
		List<List<Integer>> clusters;
		double cost;
		public Res(List<double[]> cents, List<List<Integer>> clusters, double cost) {
			this.cents = cents;
			this.clusters = clusters;
			this.cost = cost;
		}
	}
	
	public static Res kMeans(List<double[]> points, List<double[]> startCents) throws Exception {
		double prevCost = Double.POSITIVE_INFINITY;
		List<List<Integer>> clusters = null;
		double cost = 0;
		List<double[]> cents = new ArrayList<double[]>(startCents);

		while (true) {
			
			// Distribute points over current cluster centroids
			clusters = new ArrayList<List<Integer>>();
			for (int i = 0; i < cents.size(); i++) clusters.add(new ArrayList<Integer>());
			
			for (int pn = 0; pn < points.size(); pn++) {
				double[] point = points.get(pn);
				int closestCent = -1;
				double minDist = Double.POSITIVE_INFINITY;
				for (int cn = 0; cn < cents.size(); cn++) {
					double[] cent = cents.get(cn);
					if (cent == null) continue;
					double dist = Misc.dist(cent, point);
					if (dist < minDist) {
						minDist = dist;
						closestCent = cn;
					}
				}
				/*Log.log("minDist=%1.3f", minDist);
				if (minDist > 5) {
					clusters.add(new ArrayList<Integer>());
					clusters.get(clusters.size()-1).add(pn);
				}
				else*/ clusters.get(closestCent).add(pn);
//				Log.log("Point %3d goes to cluster %2d", pn, closestCent);
			}
			
			// Calc cost function
			cost = 0;
			for (int cn = 0; cn < cents.size(); cn++) for (int pn: clusters.get(cn))
				cost += Misc.dist(points.get(pn), cents.get(cn));
			cost /= points.size();
			Log.log("cost=%1.4f, prev cost=%1.4f", cost, prevCost);
			if (prevCost == cost) break;
			prevCost = cost;
	
			// Define new cluster centroids
			for (int cn = 0; cn < cents.size(); cn++) {
				List<double[]> cPoints = new ArrayList<double[]>();
				for (int pn: clusters.get(cn)) cPoints.add(points.get(pn));
				
				if (cPoints.size() == 0) {
					cents.set(cn, null);
					Log.log("WARNING! Zero cluster");
				} else cents.set(cn, avg(cPoints));
			}
		}
		
		return new Res(cents, clusters, cost);
	}

	public static Res kMeansRnd(List<double[]> points, int cc) throws Exception {
		// Randomly select centroids
		List<double[]> cents = new ArrayList<double[]>(cc);
		List<Integer> rnd = new ArrayList<Integer>();
		for (int i = 0; i < points.size(); i++) rnd.add(i);
		Collections.shuffle(rnd);
		for (int i = 0; i < cc; i++) cents.add(points.get(rnd.get(i)));
//		for (int cn = 0; cn < cc; cn++) Log.log("Cluster %2d, centroid point %3d: %s", cn, rnd.get(cn), Arrays.toString(points.get(rnd.get(cn))));
		
		return kMeans(points, cents);
	}
	
	public static Res kMeansN(List<double[]> points, int cc, int itCount) throws Exception {
		Res bestRes = null;
		double minCost = Double.POSITIVE_INFINITY;
		for (int i = 0; i < itCount; i++) {
			Log.log("Try %d", i);
			Res res = kMeansRnd(points, cc);
			if (res.cost < minCost) {
				minCost = res.cost;
				bestRes = res;
			}
		}
		Log.log("Best cost: %1.4f", minCost);
		return bestRes;
	}

	public static Res kMeansMaxDist(List<double[]> points, double maxDist, int startClNum) throws Exception {
		Res res = null;
		List<double[]> cents = null;
		
		while (true) {
			if (cents == null) res = kMeansRnd(points, startClNum);
			else res = kMeans(points, cents);
			
			double maxd = Double.NEGATIVE_INFINITY;
			int maxdpn = -1;
			for (int cn = 0; cn < res.clusters.size(); cn++) {
				for (int pn: res.clusters.get(cn)) {
					double d = Misc.dist(points.get(pn), res.cents.get(cn));
					if (d > maxd) {
						maxd = d;
						maxdpn = pn;
					}
				}
			}
			if (maxd < maxDist) break;
			
			cents = res.cents;
			cents.add(points.get(maxdpn));
			
			// Remove empty clusters
			int i = 0;
			while (i < cents.size()) {
				if (cents.get(i) == null) cents.remove(i);
				else i++;
			}
			
			Log.log("Max dist %1.4f > needed %1.4f, clustering for %d clusters", maxd, maxDist, cents.size());
		}
		
		return res;
	}
	
	public static void main(String[] args) throws Exception {
		List<double[]> points = new ArrayList<double[]>();
		
		points.add(new double[]{0,1});	
		points.add(new double[]{1,1});	
		points.add(new double[]{1,0});

		points.add(new double[]{10,11});
		points.add(new double[]{11,11});
		points.add(new double[]{11,10});
		
//		Res res = kMeansRnd(points, 3);
//		Res res = kMeansN(points, 3);
		Res res = kMeansMaxDist(points, 0.5, 1);
		
		for (int cn = 0; cn < res.cents.size(); cn++)
			Log.log("Cluster %d: %s", cn, Arrays.toString(res.cents.get(cn)));
	}
}
