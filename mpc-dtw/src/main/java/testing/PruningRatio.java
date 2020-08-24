
package testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import additive.AdditiveUtil;
import common.db.ConnectRedis;
import common.util.Config;
import common.util.Constants;
import pruning.ServicePruning;
import redis.clients.jedis.Jedis;
import simAnalysis.ServiceAnalysis;

public class PruningRatio {
	
	private static String hostname = Config.getSetting(Constants.CONFIG_DB_IP);// LOCALHOST
	private static int redisPort = Config.getSettingInt(Constants.CONFIG_DB_PORT);// 6379
	/*private static int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);// 5000
	private static int gcPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);// 50000
	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);// 128
	private static int band_cr = Config.getSettingInt(Constants.CONFIG_DTW_BAND_CR);// 0.05
*/	
	
	private double time;
	private long bandwidth;

	private static int threshold = Config.getSettingInt(Constants.CONFIG_DTW_THRESHOLD);
	static Logger log = Logger.getLogger(PruningRatio.class.getName());
	
	public static void main(String[] args) {
		ConnectRedis connector = new ConnectRedis();
		Jedis jedis = connector.connectDb(hostname, redisPort);
		log.info("---------------------------PruningRatio--------------------------");
		log.info("start...........");

		System.out.println("Start...");

		ServicePruning servicePruning = new ServicePruning(jedis);
		ArrayList<Integer> clusters = new ArrayList<Integer>();
		try {
			clusters = servicePruning.findCandidateCluster(jedis);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		

		
		long candidateSeqNum = 0;
		for(int i=0;i<clusters.size();i++) {
			int clusterNo = clusters.get(i);
			String key = "*|"+clusterNo+"|*|*";
			Set<String> keys = jedis.keys(key);
			candidateSeqNum += keys.size();
		}
		System.out.println("candidateSeqNum:"+candidateSeqNum);
		
		
		long allSeqNum = jedis.dbSize()-9;
		System.out.println("allSeqNum:"+allSeqNum);
		
		double candidateSeqNumDouble = candidateSeqNum;
		double allSeqNumDouble = allSeqNum;

		double candidateRatio = candidateSeqNumDouble/allSeqNumDouble;		
		
		double ratio = 1.0 - candidateRatio;
		System.out.println("candidateRatio:"+candidateRatio);
		System.out.println("ratio:"+ratio);
		log.info("---------------------------PruningRatio--------------------------");
		log.info("ratio:"+ratio);
		
		jedis.disconnect();
	}
	
	

}
