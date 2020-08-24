package preparation.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import common.db.ConnectRedis;
import common.model.Sample;
import common.parser.ReadTxt;
import common.parser.WriteFile;
import common.util.Config;
import common.util.Constants;
import preprocess.dp.DensityPeakCluster;
import redis.clients.jedis.Jedis;

public class DensityPeakClusteringTest {
	private static double time;
	private static String hostname = Config.getSetting(Constants.CONFIG_DB_IP);// LOCALHOST
	private static int redisPort = Config.getSettingInt(Constants.CONFIG_DB_PORT);// 6379
	static Logger log = Logger.getLogger(DensityPeakClusteringTest.class.getName());
	
	public static void main(String[] args) {
		//initialize Redis
		ConnectRedis connector = new ConnectRedis();
		Jedis jedis = connector.connectDb(hostname, redisPort);		
		
		
		ReadTxt reader = new ReadTxt();
		String filePath = Config.getSetting(Constants.CONFIG_DP_PATH);
		
		String fileNameIn = Config.getSetting(Constants.CONFIG_DP_FILE_NAME_IN);
		String dpNameOut = Config.getSetting(Constants.CONFIG_DP_FILE_NAME_OUT);
		
		char delimiter = Config.getSettingChar(Constants.CONFIG_CSV_SEPARATOR);//[\space]
		
		char delimiterDp = Config.getSettingChar(Constants.CONFIG_DP_SEPARATOR);//[|]
		
		int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);
		
		int topK = Config.getSettingInt(Constants.CONFIG_DP_CLUSTER_NUM);
		
		
		
		HashMap<Integer,Sample> samples = reader.bufferedReadTxtWithDeilimiterDP(filePath, fileNameIn, delimiter, queryLength);
		//HashMap<Integer, Sample> samples = reader.bufferedReadTxtWithDeilimiter("", "./resources/iris.data", ',', 4);
		log.info("num of points:"+samples.size());
		
		double e = System.nanoTime();
		DensityPeakCluster cluster = new DensityPeakCluster(jedis, samples);
		//all-pair distance Map<index1+index2, SED>
		log.info("Start calculating all-pair distance...");
		cluster.calPairDistance(jedis);
		log.info("Finish all-pair distance.");
		//cutoff distance dc
		double dc = cluster.findDC(jedis);
		log.info("Finish calculate cutoff distance: "+dc);
		//local density
		cluster.calRho(dc, jedis);
		log.info("Finish calculate local density.");
		//Distance from points with higher local density
		cluster.calDelta(jedis);
		log.info("Finish calculate Delta.");
		//cluster.clustering(0.3, 1);
		cluster.clusteringTopK(topK);
		ArrayList<Integer> centers = cluster.getCenterList();
		double s = System.nanoTime();
		time = s-e;
		
		log.info("-----------------DensityPeakClustering---------------------");
		log.info("total time:"+time/1e9+" seconds.");
		log.info("number of sequences:"+samples.size());
		log.info("number of clusters:"+centers.size());
		log.info("cluster center index list is "+centers);
		System.out.println("total time:"+time/1e9+" seconds.");
		System.out.println("cluster center index list is "+centers);
		
		//System.exit(0);
		log.info("start writing to file...");
		WriteFile writeFile = new WriteFile();
		boolean isLastLine = false;
		int counter = 0;
		int i=0;
		for(Map.Entry<Integer, Sample> entry : samples.entrySet()) {
			String[] seqStr = jedis.get(String.valueOf(entry.getKey())).split(" ");
			int[] sequence  = new int[128];
			for(int j=0; j<seqStr.length; j++) {
				sequence[j] = Integer.parseInt(seqStr[j].trim());
			}
			
					
			int index = entry.getValue().getIndex();
			int label = entry.getValue().getPredictLabel();
			
			String sequenceStr = "";
			
			for(int j =0; j<sequence.length; j++) {
							
				sequenceStr = sequenceStr.concat(String.valueOf(sequence[j])).concat(" "); 
				//System.out.println(sequenceStr);
				counter++;
			}
			String prefix = index+String.valueOf(delimiterDp)+label+String.valueOf(delimiterDp);
			sequenceStr = prefix.concat(sequenceStr);
			//System.out.println(sequenceStr);
			
			if(i == 0) {
				String starter = "";
				for(int j=0; j<centers.size();j++) {
					if(j==0) {
						starter = starter + centers.get(j);
					}else {
					starter = starter+delimiterDp +centers.get(j);
					}
				}
				writeFile.writeFile(filePath, dpNameOut, starter, isLastLine);
			}
			
			writeFile.writeFile(filePath, dpNameOut, sequenceStr, isLastLine);
			
			if(i%1000 ==0) {
				log.info("Progress......"+i);
			}
			i++;
			}
		log.info("End..."+" no of points: "+counter+" number of sequences: "+i);
		/*for(int i =0; i<samples.size();i++) {
			System.out.println("cluster no:"+samples.get(i).getPredictLabel()+" sample no:"+samples.get(i).getIndex());
		}*/
		//cluster.predictLabel();
		jedis.flushAll();
		jedis.disconnect();
		
	}
}
