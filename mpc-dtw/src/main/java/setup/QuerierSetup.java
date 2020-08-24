package setup;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import additive.ShareGenerator;
import additive.SharedSequence;
import common.db.ConnectRedis;
import common.util.Config;
import common.util.Constants;
import preparation.ReadQuery;
import redis.clients.jedis.Jedis;
import testing.LowerUpper;

public class QuerierSetup {
	
	private static String filePath = Config.getSetting(Constants.CONFIG_LB_PATH);
	private static String fileName = Config.getSetting(Constants.CONFIG_LB_FILE_NAME_OUT);
	
	private static String redisHost = Config.getSetting(Constants.CONFIG_DB_IP);
	private static int redistPort = Config.getSettingInt(Constants.CONFIG_DB_PORT);
	
	protected static long[] thresholdShares = new long[2];
	protected static int threshold = Config.getSettingInt(Constants.CONFIG_DTW_THRESHOLD);
	
	static Logger log = Logger.getLogger(QuerierSetup.class.getName());
	
	public static long[] generateSharesThreshold() {
		ShareGenerator generator = new ShareGenerator(true);
		generator.generateSharedDataPoint(threshold, true);
		thresholdShares[0] = generator.x0;
		thresholdShares[1] = generator.x1;
		return thresholdShares;
	}
	
	public static void main(String[] args) {
		ConnectRedis connector = new ConnectRedis();
		Jedis jedis = connector.connectDb(redisHost, redistPort);
		ReadQuery reader = new ReadQuery();
		System.out.println("Started...");
		ArrayList<SharedSequence> sequences = reader.readToModel(filePath, fileName);
		reader.extractToDB(sequences, jedis);
		System.out.println("Finished.");
		
		log.info("---------------QuerierSetup-----------------");
		log.info("time to generate shares of Q, LB, UB:"+reader.time/1e9);
	}
	
}
