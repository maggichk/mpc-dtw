package protocol.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import additive.AdditiveUtil;
import common.db.ConnectRedis;
import common.util.Config;
import common.util.Constants;
import pruning.ServicePruning;
import redis.clients.jedis.Jedis;
import simAnalysis.ServiceAnalysis;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

public class Protocol {

	private static String hostname = Config.getSetting(Constants.CONFIG_DB_IP);// LOCALHOST
	private static int redisPort = Config.getSettingInt(Constants.CONFIG_DB_PORT);// 6379
	/*private static int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);// 5000
	private static int gcPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);// 50000
	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);// 128
	private static int band_cr = Config.getSettingInt(Constants.CONFIG_DTW_BAND_CR);// 0.05
*/	private static double time;

	private static int threshold = Config.getSettingInt(Constants.CONFIG_DTW_THRESHOLD);
	
	static Logger log = Logger.getLogger(Protocol.class.getName());

	public static void main(String[] args) {
		ConnectRedis connector = new ConnectRedis();
		Jedis jedis = connector.connectDb(hostname, redisPort);
		
		Map<Integer, Long> resMap = new HashMap<Integer, Long>();

		System.out.println("Start...");
		log.info("-----------------Protocol--------------------");
		log.info("start....");
		
		log.info("Start find candidate clusters....");
		ServicePruning servicePruning = new ServicePruning(jedis);
		ArrayList<Integer> clusters = new ArrayList<Integer>();
		try {
			clusters = servicePruning.findCandidateCluster(jedis);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		System.out.println("pruning time:"+servicePruning.time);
		log.info("---------------------pruning time:"+servicePruning.time/1e9);
		time += servicePruning.time;
		
		
		

		System.out.println("Start service analysis...");
		log.info("Start service analysis...");
		ServiceAnalysis analysis = new ServiceAnalysis();
		HashMap<Integer, long[]> mapDTW = new HashMap<Integer, long[]>();
		try {
			mapDTW = analysis.getDTWs(jedis, clusters, servicePruning);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("analysis time:"+analysis.time);
		log.info("---------------------analysis time:"+analysis.time/1e9);
		time += analysis.time;
		
		
		System.out.println("Finished.");
		log.info("Finished.");
		log.info("overall time:"+time/1e9);
		
		double candidateSeqNum = analysis.numCandidateSeq;
		double allSeqNum = jedis.dbSize()-9;
		double ratio = 1- candidateSeqNum/allSeqNum;
		System.out.println("ratio:"+ratio);
		log.info("ratio:"+ratio);
		log.info("candidate seq num:"+candidateSeqNum);
		log.info("total num:"+allSeqNum);
		

		for (Map.Entry<Integer, long[]> entry : mapDTW.entrySet()) {
			long[] resShare = entry.getValue();
			long res = AdditiveUtil.add(resShare[0],resShare[1]);
			if(res < threshold) {
				resMap.put(entry.getKey(), res);
			}
		}
		
		System.out.println("Total number of sequences similar to query:"+resMap.size());
		log.info("Total number of sequences similar to query:"+resMap.size());
		
		System.out.println("index \t DTW");
		for(Map.Entry<Integer, Long> entryDTW: resMap.entrySet() ) {
				System.out.println(entryDTW.getKey()+" \t "+entryDTW.getValue());
		}

		jedis.disconnect();
	}
	
	

}
