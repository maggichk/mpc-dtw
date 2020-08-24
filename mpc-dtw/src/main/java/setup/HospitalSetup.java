package setup;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import additive.AdditiveUtil;
import additive.ShareGenerator;
import additive.SharedSequence;
import common.db.ConnectRedis;
import common.util.Config;
import common.util.Constants;
import preparation.ExtractSequenceFromDB;
import redis.clients.jedis.Jedis;

public class HospitalSetup{
	
	private static String redisHost = Config.getSetting(Constants.CONFIG_DB_IP);
	private static int redistPort = Config.getSettingInt(Constants.CONFIG_DB_PORT);
	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);
	
	private static double time;
	
	static Logger log = Logger.getLogger(HospitalSetup.class.getName());
	
	public static void main(String[] args) {
		ShareGenerator generator = new ShareGenerator();
		
		//redis
		ConnectRedis connector = new ConnectRedis();
		Jedis jedis = connector.connectDb(redisHost, redistPort);
		
		//generate shares
		Set<String> keys = jedis.keys("*");
		Iterator<String> it = keys.iterator();
		while(it.hasNext()){
			String key = it.next();
			//System.out.println("key:"+key);
			
			if(key.split("\\|")[1].equals(String.valueOf(Integer.MAX_VALUE))) {
				System.out.println("Query or LB or UB.");
				continue;
			}
			
			SharedSequence S0 = ExtractSequenceFromDB.extract(jedis, key);		
			
			double e = System.nanoTime();
			generator.generateSharedSequence(S0);
			double s = System.nanoTime();
			time += s-e;
			
			//rebuild S0
			String[] pair0 = ExtractSequenceFromDB.buildKeyValue(S0);
			jedis.set(pair0[0], pair0[1]);
			
			//build Redis key-value pair
			SharedSequence S1 = generator.S1;
			//System.out.println("s1.sharedSeq:"+Arrays.toString(S1.getSharedSequence()));
			String[] pair1 = ExtractSequenceFromDB.buildKeyValue(S1);			
						
			//insert to Redis
			jedis.set(pair1[0], pair1[1]);
		}
		
		
		
		//SharedSequence ver0 = ExtractSequenceFromDB.extract(jedis, "true|1|1333|1");
		//SharedSequence ver1 = ExtractSequenceFromDB.extract(jedis, "true|1|1333|0");
		//long data0 = ver0.getSharedData(0)[0];
		//long sqdata0 = ver0.getSharedData(0)[1];
		//long data1 = ver1.getSharedData(0)[0];
		//long sqdata1 = ver1.getSharedData(0)[1];
		//System.out.println("verify data:"+AdditiveUtil.add(data0, data1)+" "+AdditiveUtil.add(sqdata0, sqdata1));
		
		jedis.disconnect();
		
		
		log.info("-------------------------HospitalSetup--------------------------");
		log.info("number of sequences:"+keys.size());
		log.info("total time:"+time/1e9+" seconds");
	}
	
	
	
	

}
