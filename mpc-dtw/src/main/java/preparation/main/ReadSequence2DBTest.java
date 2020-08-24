package preparation.main;

import common.db.ConnectRedis;
import common.util.Config;
import common.util.Constants;
import preparation.ReadSequenceToDB;
import redis.clients.jedis.Jedis;

public class ReadSequence2DBTest {

	
	private static String fileName = Config.getSetting(Constants.CONFIG_DP_FILE_NAME_OUT);
	private static String filePath = Config.getSetting(Constants.CONFIG_DP_PATH);
	private static int redisPort = Config.getSettingInt(Constants.CONFIG_DB_PORT);
	private static String redisHost = Config.getSetting(Constants.CONFIG_DB_IP);
	private static int startIndex = Config.getSettingInt(Constants.CONFIG_DP_START_INDEX);
	private static int clusterStartIndex = Config.getSettingInt(Constants.CONFIG_DP_CLUSTER_START_INDEX);
	
	public static void main(String[] args) {
		ConnectRedis cr = new ConnectRedis();
		ReadSequenceToDB reader = new ReadSequenceToDB();
		
		Jedis jedis = cr.connectDb(redisHost, redisPort);
		System.out.println("Started...");
		reader.read2DB(startIndex,clusterStartIndex, filePath, fileName, jedis);
		System.out.println("Finished.");
		
	}
}
