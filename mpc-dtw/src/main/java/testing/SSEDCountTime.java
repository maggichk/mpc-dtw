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
import distances.SSED;
import flexSC.network.Client;
import flexSC.network.Server;
import pruning.ServicePruning;
import redis.clients.jedis.Jedis;
import setup.ServiceSetupDummy;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class SSEDCountTime {

	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);
	private static String hostname = Config.getSetting(Constants.CONFIG_DB_IP);// LOCALHOST
	private static int redisPort = Config.getSettingInt(Constants.CONFIG_DB_PORT);// 6379
	private static int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);

	private static double band_cr = Config.getSettingDouble(Constants.CONFIG_DTW_BAND_CR);// 0.05
	public static int cr = (int) Math.floor((double) band_cr * queryLength + 1);

	// private static int numSeqs = 10;
	private static int numSeqs = 78;
	//= Config.getSettingInt(Constants.CONFIG_NUM_SEQS);
	private static double time;
	private static long bandwidth;

	private static double[] timeList = new double[numSeqs * queryLength];

	private static long[] bandwidthList = new long[numSeqs * queryLength];

	static Logger log = Logger.getLogger(SSEDCountTime.class.getName());

	public static void main(String[] args) throws Exception {
		log.info("-----------------------SSEDCountTime----------------------");
		log.info("start....");

		// database connector
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
		//int loopNum = numSeqs * (2*cr+1);
		int loopNum = numSeqs;
		System.out.println("loopNum:" + loopNum);

		// load mts
		int mtNumDTW = numSeqs*queryLength;
		int mtNum = mtNumDTW;
		ServiceSetupDummy mtLoader = new ServiceSetupDummy();
		ArrayList<MultiplicationTriple> mts = mtLoader.loadMT(mtNum);
		System.out.println("Number of mts:" + mts.size() + " centersNum:" + centersNum);
		log.info("Number of mts:" + mts.size() + " centersNum:" + centersNum);

		Iterator<Entry<String, SharedSequence>> it = centers.entrySet().iterator();

		SharedSequence center0 = it.next().getValue();
		SharedSequence center1 = it.next().getValue();

		// arithmetic connector
		final Server sndChannel = new Server();
		final Client rcvChannel = new Client();
		ConnectionHelper connectHelper = new ConnectionHelper();
		connectHelper.connect(hostname, arithmeticPort, sndChannel, rcvChannel);
		System.out.println("Connection| hostname:port, " + hostname + ":" + arithmeticPort);
		log.info("Connection| hostname:port, " + hostname + ":" + arithmeticPort);

		

		long[] x0set = query0.getSharedSequence();
		long[] x0sqset = query0.getSharedSquareSequence();
		long[] x1set = query1.getSharedSequence();
		long[] x1sqset = query1.getSharedSquareSequence();
		long[] y0set = center0.getSharedSequence();
		long[] y0sqset = center0.getSharedSquareSequence();
		long[] y1set = center1.getSharedSequence();
		long[] y1sqset = center1.getSharedSquareSequence();
		SSED ssed = new SSED();
		
		
		int counter = 0;
		while (counter < loopNum) {
			System.out.println("sequence counter:" + counter);
			log.info("sequence counter:" + counter);

			for (int i = 0; i < queryLength; i++) {
				
				MultiplicationTriple mt = mts.get(0);
				mts.remove(0);
				System.out.println("data point:"+i);
				double e = System.nanoTime();
				ssed.compute(false, sndChannel, rcvChannel, mt, x0set[i], y0set[i], x1set[i], y1set[i], x0sqset[i],
						y0sqset[i], x1sqset[i], y1sqset[i]);
				double s = System.nanoTime();

				time += s - e;
				bandwidth = ssed.bandwidth;

				timeList[counter*queryLength+i] = (s - e);
				bandwidthList[counter*queryLength+i] = ssed.bandwidth;
				System.out.println("unit time:" + (s - e) / 1e6+" milliseconds");
				System.out.println("unit bandwidth:" + ssed.bandwidth + " B");
				log.info("unit time:" + (s - e) / 1e6+" milliseconds");
				log.info("unit bandwidth:" + ssed.bandwidth  + " B");

				/*try {
					System.out.println("counter:" + counter + " sleep...");
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}*/

			}

			counter++;

		}

		sndChannel.disconnect();
		rcvChannel.disconnect();
		bandwidth= ssed.bandwidth;

		System.out.println("end");
		log.info("end");
		log.info("----------------------SSEDCountTime------------------------------");
		log.info("num of calls:" + loopNum*queryLength);
		log.info("bandwidth:" +  bandwidth /1024.0/1024.0 + " MB");
		log.info("time:" + time / 1e9 + " seconds");

		System.out.println("num of calls:" + loopNum*queryLength);
		System.out.println("bandwidth:" + bandwidth / 1024.0/1024.0 + " MB");
		System.out.println("time:" + time / 1e9 + " seconds");

		log.info("unit time...");
		for (int i = 0; i < loopNum*queryLength; i++) {
			log.info(timeList[i] / 1e6);

		}
		
		log.info("unit bandwidth...");
		for (int i = 0; i < loopNum*queryLength; i++) {
			log.info(bandwidthList[i] / 1024/1024); //MB

		}

	}

}
