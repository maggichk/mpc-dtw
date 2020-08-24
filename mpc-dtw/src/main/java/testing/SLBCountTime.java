package testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import additive.MultiplicationTriple;
import additive.SharedSequence;
import common.db.ConnectRedis;
import common.util.Config;
import common.util.Constants;
import distances.SDTW;
import distances.SLBConcurrent;
import flexSC.network.Client;
import flexSC.network.Server;
import pruning.ServicePruning;
import redis.clients.jedis.Jedis;
import setup.ServiceSetupDummy;
import utilMpc.ConnectionHelper;

public class SLBCountTime {

	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);
	private static String hostname = Config.getSetting(Constants.CONFIG_DB_IP);// LOCALHOST
	private static int redisPort = Config.getSettingInt(Constants.CONFIG_DB_PORT);// 6379

	private static int numSeqs = 10;
	//private static int numSeqs= Config.getSettingInt(Constants.CONFIG_NUM_SEQS);
	private static double time;
	private static long bandwidth;
	private static double[] timeList = new double[numSeqs];
	private static long[] bandwidthList = new long[numSeqs];
	
	static Logger log = Logger.getLogger(SLBCountTime.class.getName());

	public static void main(String[] args) {
		ConnectRedis connector = new ConnectRedis();
		Jedis jedis = connector.connectDb(hostname, redisPort);

		ServicePruning pruning = new ServicePruning(jedis);
		// load ub, lb, query
		pruning.loadQuerys(jedis);

		// variables can be visited by external class
		SharedSequence U0 = pruning.U0;
		SharedSequence U1 = pruning.U1;
		SharedSequence L0 = pruning.L0;
		SharedSequence L1 = pruning.L1;
		SharedSequence query0 = pruning.query0;
		SharedSequence query1 = pruning.query1;
		long[] thresholdShares = pruning.thresholdShares;

		// load centers
		HashMap<String, SharedSequence> centers = pruning.loadCenters(jedis);
		int centersNum = centers.size() / 2;
		System.out.println("centers num:" + centersNum);
		// int loopNum = (int) Math.ceil((double)numSeqs/centersNum);
		int loopNum = numSeqs;
		System.out.println("loopNum:" + loopNum);
		// load mts
		int mtNumLB = numSeqs * queryLength * 2;
		int mtNum = mtNumLB;
		ServiceSetupDummy mtLoader = new ServiceSetupDummy();
		ArrayList<MultiplicationTriple> mts = mtLoader.loadMT(mtNum);
		System.out.println("Number of mts:" + mts.size() + " centersNum:" + centersNum);

		Iterator<Entry<String, SharedSequence>> it = centers.entrySet().iterator();

		SharedSequence center0 = it.next().getValue();
		SharedSequence center1 = it.next().getValue();

		int counter = 0;
		while (counter < loopNum) {
			System.out.println("counter:" + counter);

			double e = System.nanoTime();
			// slb
			SLBConcurrent slb = new SLBConcurrent(queryLength);
			long[] SLB = slb.computeConcurrent(mts, U0, U1, L0, L1, center0, center1);
			double s = System.nanoTime();
			
			time += s - e;
			bandwidth += slb.bandwidth;
			
			timeList[counter] = (s-e);
			bandwidthList[counter] = slb.bandwidth;
			System.out.println("unit time:"+ (s-e)/1e9);
			System.out.println("unit bandwidth:"+slb.bandwidth/1024.0/1024.0+" MB");
			log.info("unit time:"+ (s-e)/1e9);
			log.info("unit bandwidth:"+slb.bandwidth/1024.0/1024.0+" MB");

			/*try {
				System.out.println("counter:" + counter + " sleep...");
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/

			counter++;

		}
		System.out.println("end");
		log.info("----------------------SLB------------------------------");
		log.info("num of calls:" + numSeqs);
		log.info("bandwidth:" + bandwidth / 1024.0 / 1024.0 + " MB");
		log.info("time:" + time / 1e9 + " seconds");

		System.out.println("num of calls:" + numSeqs);
		System.out.println("bandwidth:" + bandwidth / 1024.0 / 1024.0 + " MB");
		System.out.println("time:" + time / 1e9 + " seconds");
		
		log.info("unit time...");
		for(int i=0; i<numSeqs; i++) {
			log.info(timeList[i]/1e9);
			
		}

	}

}
