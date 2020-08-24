package preprocess.dp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import common.model.Sample;
import redis.clients.jedis.Jedis;

public class DensityPeakCluster {
	// samples<index, sample>
	private HashMap<Integer, Sample> samples;
	/** <index, sample >Map */
	// private HashMap<Integer, Sample> sampleIndexMap;
	/** local density Map ：<index,densitycount> */
	private HashMap<Integer, Integer> densityCountMap;
	/** Sorted descending Density list */
	private ArrayList<Map.Entry<Integer, Integer>> sortedDensityList;
	/** distance from points of higher density: deltaMap:<index, delta> */
	private HashMap<Integer, Double> deltaMap;
	/** nearest neighbor：<sampleIndex, nearestNeighborIndex> */
	private HashMap<Integer, Integer> nearestNeighborMap;
	/** all-pair distance：<"index1 index2", distance> */
	//private HashMap<String, Double> pairDistanceMap;
	/** max distance */
	private double maxDistance;
	/** min distance */
	private double minDistance;
	/** cluster centers */
	private ArrayList<Integer> centerList;
	/** clusters <sampleIndex, clusterIndex> */
	private HashMap<Integer, Integer> clusterMap;
	
	public DensityPeakCluster(Jedis jedis, HashMap<Integer, Sample> samples) {
		//read to db 
		for(Map.Entry<Integer, Sample> entry: samples.entrySet()) {
			
			String key = String.valueOf(entry.getKey());
			int[] attributes = entry.getValue().getAttributes();
			String value = "";
			for(int i=0; i<attributes.length; i++) {
				if(i == 0) {
					value = String.valueOf(attributes[0]);
				}else {
					value += " "+String.valueOf(attributes[i]);
				}
			}
			
			//insert to redis
			jedis.set(key, value);
			
			//set attributes null
			attributes = null;
		}
		
		this.samples = new HashMap<Integer, Sample>(samples.size());
		this.samples.putAll(samples);
		System.out.println("new size:" + this.samples.size());
		
	}

	public DensityPeakCluster(HashMap<Integer, Sample> samples) {
		// System.out.println("1:"+samples.size());
		this.samples = new HashMap<Integer, Sample>(samples.size());
		this.samples.putAll(samples);
		System.out.println("new size:" + this.samples.size());
/*		for (Map.Entry<Integer, Sample> entry : samples.entrySet()) {
			System.out.println("index:" + entry.getKey() + " index2:" + entry.getValue().getIndex() + " attributes:"
					+ entry.getValue().getAttributes()[1]);
		}*/
//		sampleIndexMap = new HashMap<Integer, Sample>(samples.size());
//		int count = 0;
//		for(Sample s : samples) {
//			sampleIndexMap.put(count++, s);
//		}
	}

	public void clusteringTopK(int k) {
		centerList = new ArrayList<Integer>();
		clusterMap = new HashMap<Integer, Integer>();
		ArrayList<Sample> temp = new ArrayList<Sample>();
		// get centers
		for (Map.Entry<Integer, Sample> sample : samples.entrySet()) {
			int index = sample.getKey();
			double density = sample.getValue().getDensity();
			temp.add(new Sample(density, index, sample.getValue().getLocalDensity(), sample.getValue().getDelta(),
					sample.getValue().getNearestNeighbor(), sample.getValue().getPredictLabel()));
		}

		
		Collections.sort(temp, new Comparator<Sample>() {

			@Override
			public int compare(Sample o1, Sample o2) {
				if (o1.getDensity() > o2.getDensity())
					return -1;
				else if (o1.getDensity() < o2.getDensity()) {
					return 1;
				}
				return 0;
			}
		});

		for (int i = 0; i < k; i++) {
			int index = temp.get(i).getIndex();
			centerList.add(index);
			samples.get(index).setPredictLabel(i);
		}

		// sort temp by local density
		Collections.sort(temp, new Comparator<Sample>() {

			@Override
			public int compare(Sample o1, Sample o2) {
				if (o1.getLocalDensity() > o2.getLocalDensity())
					return -1;
				else if (o1.getLocalDensity() < o2.getLocalDensity()) {
					return 1;
				}
				return 0;
			}
		});
		for (int i = 0; i < temp.size(); i++) {
			Sample tempSample = temp.get(i);
			int tempIndex = tempSample.getIndex();
			int neighborIndex = tempSample.getNearestNeighbor();
			Sample neighbor = samples.get(neighborIndex);
			if (!centerList.contains(tempIndex)) {
				samples.get(tempIndex).setPredictLabel(neighbor.getPredictLabel());
			}
		}

		// calculate clusters，note: points are sorted from higher local density to lower
		// Map<index, localDensity>
		// local density
		/*
		 * for (Map.Entry<Integer, Integer> candidate : sortedDensityList) { int
		 * sampleIndex = candidate.getKey(); int neighborIndex =
		 * samples.get(sampleIndex).getNearestNeighbor();
		 * 
		 * if (!centerList.contains(candidate.getKey())) {
		 * samples.get(sampleIndex).setPredictLabel(samples.get(neighborIndex).
		 * getPredictLabel());
		 * 
		 * 
		 * // take the index of nearest neighbor as the index of the current sample if
		 * (clusterMap.containsKey(nearestNeighborMap.get(candidate.getKey()))) {
		 * clusterMap.put(candidate.getKey(),
		 * clusterMap.get(nearestNeighborMap.get(candidate.getKey()))); } else {
		 * clusterMap.put(candidate.getKey(), -1); }
		 * 
		 * } }
		 */

		/*
		 * for (int i = 0; i < samples.size(); i++) { System.out.println("Num" + i +
		 * " cluster no:" + samples.get(i).getPredictLabel() + " sample index:" +
		 * samples.get(i).getIndex());
		 * 
		 * }
		 */

	}

	public void clustering(double deltaThreshold, double rhoThreshold) {
		centerList = new ArrayList<Integer>();
		clusterMap = new HashMap<Integer, Integer>();
		// get centers
		for (Map.Entry<Integer, Double> deltaEntry : deltaMap.entrySet()) {
			if (deltaEntry.getValue() >= deltaThreshold && densityCountMap.get(deltaEntry.getKey()) >= rhoThreshold) {
				centerList.add(deltaEntry.getKey());
				clusterMap.put(deltaEntry.getKey(), deltaEntry.getKey());
			}
		}
		// calculate clusters，note: points are sorted from higher local density to lower
		// local density
		for (Map.Entry<Integer, Integer> candidate : sortedDensityList) {
			if (!centerList.contains(candidate.getKey())) {
				// take the index of nearest neighbor as the index of the current sample
				if (clusterMap.containsKey(nearestNeighborMap.get(candidate.getKey()))) {
					clusterMap.put(candidate.getKey(), clusterMap.get(nearestNeighborMap.get(candidate.getKey())));
				} else {
					clusterMap.put(candidate.getKey(), -1);
				}
			}
		}

	}

	public void calDelta(Jedis jedis) {
		// sort local density descendingly
		sortedDensityList = new ArrayList<Map.Entry<Integer, Integer>>(densityCountMap.entrySet());
		Collections.sort(sortedDensityList, new Comparator<Map.Entry<Integer, Integer>>() {

			@Override
			public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
				if (o1.getValue() > o2.getValue())
					return -1;
				else if (o1.getValue() < o2.getValue()) {
					return 1;
				}
				return 0;
			}
		});
		nearestNeighborMap = new HashMap<Integer, Integer>(samples.size());
		deltaMap = new HashMap<Integer, Double>(samples.size());
		for (int i = 0; i < sortedDensityList.size(); i++) {
			if (i == 0) {
				nearestNeighborMap.put(sortedDensityList.get(i).getKey(), -1);
				deltaMap.put(sortedDensityList.get(i).getKey(), maxDistance);
			} else {
				double minDij = Double.MAX_VALUE;
				int index = 0;
				for (int j = 0; j < i; j++) {
					double dis = getDistanceFromIndex(sortedDensityList.get(i).getKey(),
							sortedDensityList.get(j).getKey(), jedis);
					if (dis < minDij) {
						index = j;
						minDij = dis;
					}
				}
				nearestNeighborMap.put(sortedDensityList.get(i).getKey(), sortedDensityList.get(index).getKey());
				deltaMap.put(sortedDensityList.get(i).getKey(), minDij);
			}
		}

		System.out.println("Output index of sample, local density, index of nearest neighbor, distance delta");
		for (Map.Entry<Integer, Integer> entry : sortedDensityList) {
			/*System.out.println(entry.getKey() + " " + entry.getValue() + " " + nearestNeighborMap.get(entry.getKey())
					+ " " + deltaMap.get(entry.getKey()));*/

			double delta = deltaMap.get(entry.getKey());
			int rho = entry.getValue();

			samples.get(entry.getKey()).setDensity(delta * rho);
			samples.get(entry.getKey()).setLocalDensity(rho);
			samples.get(entry.getKey()).setDelta(delta);
			// samples.get(entry.getKey()).setIndex(entry.getKey());

			if (nearestNeighborMap.get(entry.getKey()) == -1) {
				nearestNeighborMap.put(entry.getKey(), entry.getKey());
			}
			samples.get(entry.getKey()).setNearestNeighbor(nearestNeighborMap.get(entry.getKey()));
		}

		/*for (int i = 0; i < samples.size(); i++) {
			if(samples.get(i).getDensity() >= 50000) {
				System.out.println("Sample index:" + samples.get(i).getIndex() + " density:" + samples.get(i).getDensity()
						+ " neighbor:" + samples.get(i).getNearestNeighbor());
			}
			
		}*/
	}

	/**
	 * get distance according to index
	 * 
	 * @param index1
	 * @param index2
	 * @return
	 */
	private double getDistanceFromIndex(int index1, int index2, Jedis jedis) {
		
		String key ="";
		if(index1 < index2) {
			key = index1+"|"+index2;
		}else {
			key = index2+"|"+index1;
		}
				
		//System.out.println("key:"+key);
		if(null != jedis.get(key)) {
			double value = Double.parseDouble(jedis.get(key).trim());
			return value;
		}else {
			return 0;
		}
		
		
		/*if (pairDistanceMap.containsKey(index1 + " " + index2)) {
			return pairDistanceMap.get(index1 + " " + index2);
		} else {
			return pairDistanceMap.get(index2 + " " + index1);
		}*/
	}

	/**
	 * calculate local density rho
	 */
	public void calRho(double dcThreshold, Jedis jedis) {
		densityCountMap = new HashMap<Integer, Integer>(samples.size());
		// initialize with 0
		for (int i = 0; i < samples.size(); i++) {
			densityCountMap.put(i, 0);
		}
		
		
		Set<String> keys = jedis.keys("*|*");
		Iterator<String> itKeys = keys.iterator();
		while(itKeys.hasNext()) {
			String key = itKeys.next();
			double diss = Double.parseDouble(jedis.get(key));
			
			if (diss < dcThreshold) {
				String[] segs = key.split("\\|");
				int[] indexs = new int[2];
				indexs[0] = Integer.parseInt(segs[0]);
				indexs[1] = Integer.parseInt(segs[1]);
				for (int i = 0; i < indexs.length; i++) {
					densityCountMap.put(indexs[i], densityCountMap.get(indexs[i]) + 1);
				}
			}
		}
		keys.clear();
		
		
			
	}

	/**
	 * calculate all pair distance
	 */
	public void calPairDistance(Jedis jedis) {
		//pairDistanceMap = new HashMap<String, Double>();
		maxDistance = Double.MIN_VALUE;
		minDistance = Double.MAX_VALUE;
		for (int i = 0; i < samples.size() - 1; i++) {
			for (int j = i + 1; j < samples.size(); j++) {
				
				double dis = twoSampleDistance(jedis, samples.get(i), samples.get(j));
				String key = i + "|" + j;
				String value = String.valueOf(dis);
				jedis.set(key, value);
				
				
				if (dis > maxDistance)
					maxDistance = dis;
				if (dis < minDistance)
					minDistance = dis;
			}
		}
		System.out.println("maxDistance:" + maxDistance + " minDistance:" + minDistance);
	}

	/**
	 * calculate cutoff distance
	 * 
	 * @return
	 */
	public double findDC(Jedis jedis) {
		double tmpMax = maxDistance;
		double tmpMin = minDistance;
		double dc = 0.5 * (tmpMax + tmpMin);
		for (int iteration = 0; iteration < 100; iteration++) {
			int neighbourNum = 0;
			
			Set<String> keys = jedis.keys("*|*");
			Iterator<String> itKeys = keys.iterator();
			while(itKeys.hasNext()) {
				String key = itKeys.next();
				double dis = Double.parseDouble(jedis.get(key));
				
				if (dis < dc)
					neighbourNum += 2;
			}
			keys.clear();
			
			double neighborPercentage = neighbourNum / Math.pow(samples.size(), 2);
			if (neighborPercentage >= 0.01 && neighborPercentage <= 0.02)
				break;
			if (neighborPercentage > 0.02) {
				tmpMax = dc;
				dc = 0.5 * (tmpMax + tmpMin);
			}
			if (neighborPercentage < 0.01) {
				tmpMin = dc;
				dc = 0.5 * (tmpMax + tmpMin);
			}

		}
		return dc;
	}

	/**
	 * calculate squared Euclidean distance
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	private double twoSampleDistance(Jedis jedis, Sample a, Sample b) {
		
		//int[] aData = a.getAttributes();
		//int[] bData = b.getAttributes();
		String[] aDataStr = jedis.get(String.valueOf(a.getIndex())).split(" ");
		String[] bDataStr = jedis.get(String.valueOf(b.getIndex())).split(" ");	
		
		
		int distance = 0;
		for (int i = 0; i < aDataStr.length; i++) {
			int aData = Integer.parseInt(aDataStr[i].trim()); 
			int bData = Integer.parseInt(bDataStr[i].trim());
			
			distance += Math.pow(aData - bData, 2);
		}
		/*System.out.println(
				"Euclidean distance between sample " + a.getIndex() + " and " + b.getIndex() + ": " + distance);*/
		return distance;
	}

	public ArrayList<Integer> getCenterList() {
		return centerList;
	}

	/*
	 * public void predictLabel() { for(int i = 0; i < samples.size(); i++) {
	 * //System.out.println(clusterMap.get(i)); if(clusterMap.get(i) != -1)
	 * samples.get(i).setPredictLabel(samples.get(clusterMap.get(i)).getLabel()); }
	 * }
	 */
	/*
	 * public static void main(String[] args) { DataReader reader = new
	 * DataReader(); reader.readData(); ArrayList<Sample> samples =
	 * reader.getSamples(); DensityPeakCluster cluster = new
	 * DensityPeakCluster(samples); cluster.calPairDistance(); double dc =
	 * cluster.findDC(); System.out.println(dc); cluster.calRho(dc);
	 * cluster.calDelta(); cluster.clustering(0.38, 1);
	 * System.out.println(cluster.getCenterList()); }
	 */
}