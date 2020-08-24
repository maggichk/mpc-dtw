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
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class SDTWCountTime {

	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);
	private static String hostname = Config.getSetting(Constants.CONFIG_DB_IP);// LOCALHOST
	private static int redisPort = Config.getSettingInt(Constants.CONFIG_DB_PORT);// 6379
	private static int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
	
	private static double band_cr = Config.getSettingDouble(Constants.CONFIG_DTW_BAND_CR);// 0.05
	public static int cr = (int) Math.floor((double) band_cr * queryLength + 1);
	
	

	//private static int numSeqs = 10;
	private static int numSeqs= Config.getSettingInt(Constants.CONFIG_NUM_SEQS);
	private static double time;
	private static long bandwidth;
	private static double[] timeList = new double[numSeqs];
	private static long[] bandwidthList = new long[numSeqs];
	
	static Logger log = Logger.getLogger(SDTWCountTime.class.getName());

	public static void main(String[] args) {
		log.info("-----------------------SDTWCountTime----------------------");
		log.info("start....");
		
		
		ConnectRedis connector = new ConnectRedis();
		Jedis jedis = connector.connectDb(hostname, redisPort);

		ServicePruning pruning = new ServicePruning(jedis);
		// load ub, lb, query
		pruning.loadQuerys(jedis);

		// variables can be visited by external class		
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
		int mtNumDTW= numSeqs * queryLength * (2*cr+1) ;
		int mtNum = mtNumDTW;
		System.out.println("Number of mts:" + mtNum + " centersNum:" + centersNum);
		log.info("Number of mts:" + mtNum + " centersNum:" + centersNum);
		ServiceSetupDummy mtLoader = new ServiceSetupDummy();
		ArrayList<MultiplicationTriple> mts = mtLoader.loadMT(mtNum);
		

		Iterator<Entry<String, SharedSequence>> it = centers.entrySet().iterator();

		SharedSequence center0 = it.next().getValue();
		SharedSequence center1 = it.next().getValue();
		
		
		
		final Server sndChannel = new Server();
		final Client rcvChannel = new Client();
		ConnectionHelper connectHelper = new ConnectionHelper();
		connectHelper.connect(hostname, arithmeticPort, sndChannel, rcvChannel);
		System.out.println("Connection| hostname:port, " + hostname + ":" + arithmeticPort);
		log.info("Connection| hostname:port, " + hostname + ":" + arithmeticPort);
		
		SDTW sdtw = new SDTW(sndChannel, rcvChannel, queryLength, cr);
		int counter = 0;
		while (counter < loopNum) {
			System.out.println("counter:" + counter);
			log.info("counter:" + counter);

			double e = System.nanoTime();
			
			// sndChannel, rcvChannel, mts, X0, X1, Y0, Y1, queryLength
			try {
				long[] SDTW = sdtw.compute(sndChannel, rcvChannel, mts, query0, query1, center0, center1, queryLength);
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			
			double s = System.nanoTime();
			
			time += s - e;
			bandwidth += sdtw.bandwidthGC;
			
			timeList[counter] = (s-e);
			//bandwidthList[counter] = sdtw.bandwidth;
			System.out.println("unit time:"+ (s-e)/1e9);
			//System.out.println("unit bandwidth:"+sdtw.bandwidth/1024.0/1024.0+" MB");
			log.info("unit time:"+ (s-e)/1e9);
			//log.info("unit bandwidth:"+sdtw.bandwidth/1024.0/1024.0+" MB");

			/*try {
				System.out.println("counter:" + counter + " sleep...");
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/

			counter++;

		}
		
		sndChannel.disconnect();
		rcvChannel.disconnect();
		bandwidth += sdtw.bandwidthSSED;
		System.out.println("end");
		log.info("end");
		log.info("----------------------SDTW------------------------------");
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
