package pruning;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import java.util.Set;

import org.apache.log4j.Logger;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.SharedSequence;

import common.util.Config;
import common.util.Constants;
import distances.SDTW;
import distances.SLBConcurrent;
import gadgets.SCMPRankRunnable;
import flexSC.network.Client;
import flexSC.network.Server;
import preparation.ExtractSequenceFromDB;
import redis.clients.jedis.Jedis;
import setup.QuerierSetup;
import setup.ServiceSetupDummy;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class ServicePruning {
	public double time;

	private static int gcPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);// 50000
	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);// 128
	private static double band_cr = Config.getSettingDouble(Constants.CONFIG_DTW_BAND_CR);// 0.05
	private static String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);// localhost
	private static int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);//

	public static int cr = (int) Math.floor((double) band_cr * queryLength + 1);

	static Logger log = Logger.getLogger(ServicePruning.class.getName());
	// variables can be visited by external class
	public SharedSequence U0;
	public SharedSequence U1;
	public SharedSequence L0;
	public SharedSequence L1;
	public SharedSequence query0;
	public SharedSequence query1;
	public long[] thresholdShares;

	public ServicePruning(Jedis jedis) {
		this.loadQuerys(jedis);
		thresholdShares = QuerierSetup.generateSharesThreshold();
	}

	public ArrayList<Integer> findCandidateCluster(Jedis jedis) throws Exception {
		log.info("-----------------findCandidateCluster--------------------");
		ArrayList<Integer> clusters = new ArrayList<Integer>();
		ConnectionHelper connector = new ConnectionHelper();

		// ConnectRedis connect = new ConnectRedis();
		// Jedis jedis = connect.connectDb(hostname, redisPort);

		// load ub, lb, query
		this.loadQuerys(jedis);

		// load centers
		HashMap<String, SharedSequence> centers = this.loadCenters(jedis);
		

		// load mts
		int centersNum = centers.size();
		int mtNumLB = centersNum/2 * queryLength * 2;
		int mtNumDTW = centersNum/2 * (queryLength * (2*cr+1));
		int mtNum = mtNumLB + mtNumDTW;
		ServiceSetupDummy mtLoader = new ServiceSetupDummy();
		
		ArrayList<MultiplicationTriple> mts = mtLoader.loadMT(mtNum);
		System.out.println("Number of mts:"+mts.size()+" centersNum:"+centersNum);
		//Thread.sleep(5000);

		SCMPRankRunnable scmp = new SCMPRankRunnable();

		if (centersNum % 2 != 0) {
			System.out.println("ERROR| Center numbers are not even number.");
			System.exit(0);
		}

		Set<String> keys = centers.keySet();
		for (int i = 0; i < centersNum / 2; i++) {
			System.out.println("counter:"+i);
			log.info("counter:"+i);
			
			
			Iterator<String> it = keys.iterator();// new iterator when remove each key
			String centerKey0 = it.next();
			String centerKey1 = "";
			if (centerKey0.endsWith("0")) {
				centerKey1 = centerKey0.substring(0, centerKey0.length() - 1).concat("1");
			}
			else if (centerKey0.endsWith("1")) {
				centerKey1 = centerKey0.substring(0, centerKey0.length() - 1).concat("0");
			} else {
				System.out.println("ERROR| center key share error. key:" + centerKey0);
			}

			// System.out.println("centerKey0:"+centerKey0+" centerKey1:"+centerKey1);

			SharedSequence center0 = centers.get(centerKey0);
			SharedSequence center1 = centers.get(centerKey1);

			// System.out.println("center0:"+Arrays.toString(center0.getSharedSequence()));
			// compare LB
			if (ConnectionHelper.available(arithmeticPort) == false) {
				System.out.println("aithmeticPort in use:" + arithmeticPort);
			}
			if (ConnectionHelper.available(gcPort) == false) {
				System.out.println("gcPort in use:" + gcPort);
			}
			
			double e = System.nanoTime();
			SLBConcurrent slb = new SLBConcurrent(queryLength);
			long[] SLB = slb.computeConcurrent(mts, U0, U1, L0, L1, center0, center1);
			double s = System.nanoTime();
			time += s-e;
			
			System.out.println("Finish SLB calculation.");
			log.info("Finish SLB calculation.");
			
			System.out.println("sleeping...");
			Thread.sleep(60000);
			
			// prepare inputs
			String[][] args = scmp.prepareArgs(SLB[0], thresholdShares[0], SLB[1], thresholdShares[1]);
			String[] argsGen = args[0];
			String[] argsEva = args[1];

			System.out.println("Start compare SLB with threshold");
			
			if (ConnectionHelper.available(arithmeticPort) == false) {
				System.out.println("aithmeticPort in use:" + arithmeticPort);
			}
			if (ConnectionHelper.available(gcPort) == false) {
				System.out.println("gcPort in use:" + gcPort);
			}
			
			double e2 = System.nanoTime();
			scmp.run(gcPort, argsGen, argsEva);
			double s2 = System.nanoTime();
			time += s2-e2;
			
			long rank = scmp.rank; // rank = 0, X<Y -> SLB<threshold
			System.out.println("rank:" + rank);			
			System.out.println("SLB:"+AdditiveUtil.add(SLB[0], SLB[1]));
			System.out.println("threshold:"+AdditiveUtil.add(thresholdShares[0], thresholdShares[1]));
			log.info("rank:" + rank);
			log.info("SLB:"+AdditiveUtil.add(SLB[0], SLB[1]));
			log.info("threshold:"+AdditiveUtil.add(thresholdShares[0], thresholdShares[1]));
			
			if (rank == 0) {
				
				
				System.out.println("sleeping...");
				Thread.sleep(60000);
				
				// DTW(center, query)
				SharedSequence[] sequences = new SharedSequence[4];
				sequences[0] = center0; // x0
				sequences[1] = center1; // x1
				sequences[2] = query0; // y0
				sequences[3] = query1; // y1
				System.out.println("Start calculate DTW");
				// Thread.sleep(10000); //wait for everything close
				// DTW
				Server sndChannelDTW = new Server();
				Client rcvChannelDTW = new Client();
				if (ConnectionHelper.available(arithmeticPort) == false) {
					System.out.println("aithmeticPort in use:" + arithmeticPort);
				}
				if (ConnectionHelper.available(gcPort) == false) {
					System.out.println("gcPort in use:" + gcPort);
				}
				connector.connect(hostname, arithmeticPort, sndChannelDTW, rcvChannelDTW);
				
				double e3 = System.nanoTime();
				SDTW sdtw = new SDTW(sndChannelDTW, rcvChannelDTW, queryLength, cr);
				long[] SDTW = sdtw.compute(mts, sequences, queryLength);
				double s3 = System.nanoTime();
				time += s3-e3;
				
				System.out.println("Finish calculate DTW.");
				rcvChannelDTW.disconnect();
				sndChannelDTW.disconnect();
				
				
				System.out.println("sleeping...");
				Thread.sleep(60000);
				
				// SCMP RANK
				String[][] args2 = scmp.prepareArgs(SDTW[0], thresholdShares[0], SDTW[1], thresholdShares[1]);
				String[] argsGen2 = args2[0];
				String[] argsEva2 = args2[1];
				if (ConnectionHelper.available(arithmeticPort) == false) {
					System.out.println("aithmeticPort in use:" + arithmeticPort);
				}
				if (ConnectionHelper.available(gcPort) == false) {
					System.out.println("gcPort in use:" + gcPort);
				}
				
				double e4 = System.nanoTime();
				scmp.run(gcPort, argsGen2, argsEva2);
				double s4 = System.nanoTime();
				time += s4-e4;
				
				long rank2 = scmp.rank;
				System.out.println("rank2:" + rank2);
				log.info("rank2:" + rank2);
				log.info("SLB:"+AdditiveUtil.add(SLB[0], SLB[1]));
				log.info("SDTW:"+AdditiveUtil.add(SDTW[0], SDTW[1]));
				log.info("threshold:"+AdditiveUtil.add(thresholdShares[0], thresholdShares[1]));
				
				System.out.println("rank2:" + rank2);
				System.out.println("SLB:"+AdditiveUtil.add(SLB[0], SLB[1]));
				System.out.println("SDTW:"+AdditiveUtil.add(SDTW[0], SDTW[1]));
				System.out.println("threshold:"+AdditiveUtil.add(thresholdShares[0], thresholdShares[1]));
				
				if (rank2 == 0) {
					
					clusters.add(center0.getClusterIndex());
					System.out.println("add cluster index into list:"+center0.getClusterIndex());
					log.info("add cluster index into list:"+center0.getClusterIndex());
				}
			}

			keys.remove(centerKey0);
			keys.remove(centerKey1);
			
			
			/*System.out.println("sleeping...");
			Thread.sleep(10000);*/
		}

		// jedis.disconnect();
		return clusters;
	}

	public HashMap<String, SharedSequence> loadCenters(Jedis jedis) {
		HashMap<String, SharedSequence> centers = new HashMap<String, SharedSequence>();
		String keyFormat = "true|*";
		Set<String> keys = jedis.keys(keyFormat);
		Iterator<String> it = keys.iterator();
		// System.out.println("no of keys:"+keys.size());
		while (it.hasNext()) {
			String key = it.next();
			String[] keyArr = key.split("\\|");
			// System.out.println("size of keyArr:"+keyArr.length);
			if (Integer.parseInt(keyArr[1]) == Integer.MAX_VALUE || Integer.parseInt(keyArr[3]) == 2) {
				continue;
			}

			SharedSequence seq = ExtractSequenceFromDB.extract(jedis, key);
			centers.put(key, seq);
		}

		return centers;
	}

	public void loadQuerys(Jedis jedis) {
		// retrieve upperbound, lowerbound, query from redis
		U0 = new SharedSequence(queryLength, Integer.MAX_VALUE - 2, Integer.MAX_VALUE, 0);
		U0.setClusterCenter(true);
		String ub0Key = ExtractSequenceFromDB.buildKey(U0);
		U0 = ExtractSequenceFromDB.extract(jedis, ub0Key);

		U1 = new SharedSequence(queryLength, Integer.MAX_VALUE - 2, Integer.MAX_VALUE, 1);
		U1.setClusterCenter(true);
		String ub1Key = ExtractSequenceFromDB.buildKey(U1);
		U1 = ExtractSequenceFromDB.extract(jedis, ub1Key);

		L0 = new SharedSequence(queryLength, Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 0);
		L0.setClusterCenter(true);
		String lb0Key = ExtractSequenceFromDB.buildKey(L0);
		L0 = ExtractSequenceFromDB.extract(jedis, lb0Key);

		L1 = new SharedSequence(queryLength, Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 1);
		L1.setClusterCenter(true);
		String lb1Key = ExtractSequenceFromDB.buildKey(L1);
		L1 = ExtractSequenceFromDB.extract(jedis, lb1Key);

		query0 = new SharedSequence(queryLength, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
		query0.setClusterCenter(true);
		String query0Key = ExtractSequenceFromDB.buildKey(query0);
		query0 = ExtractSequenceFromDB.extract(jedis, query0Key);

		query1 = new SharedSequence(queryLength, Integer.MAX_VALUE, Integer.MAX_VALUE, 1);
		query1.setClusterCenter(true);
		String query1Key = ExtractSequenceFromDB.buildKey(query1);
		query1 = ExtractSequenceFromDB.extract(jedis, query1Key);

	}

	
}
