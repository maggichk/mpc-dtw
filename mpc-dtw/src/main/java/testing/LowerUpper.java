package testing;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;

import additive.ShareGenerator;
import additive.SharedSequence;
import common.parser.ReadTxt;
import common.parser.WriteFile;
import common.util.Config;
import common.util.Constants;
import common.util.UtilHelper;
import preprocess.lb.LowerUpperBoundGen;

public class LowerUpper {
	
	/*private static String filePath = Config.getSetting(Constants.CONFIG_LB_PATH);
	private static String fileIn = Config.getSetting(Constants.CONFIG_LB_FILE_NAME_IN);//QueryInteger.txt
	private static String fileOut = Config.getSetting(Constants.CONFIG_LB_FILE_NAME_OUT);//QueryInteger.txt
	private static String lbSeperator = Config.getSetting(Constants.CONFIG_LB_SEPARATOR);//[|]
	private static char delimiter = Config.getSettingChar(Constants.CONFIG_CSV_SEPARATOR);//[\space]
*/	
	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);//128
	private static double cr = Config.getSettingDouble(Constants.CONFIG_DTW_BAND_CR);//0.05
	
	private static double time;
	private static double timeShr;
	private static int numSeqs = Config.getSettingInt(Constants.CONFIG_NUM_SEQS);
	
	
	static Logger log = Logger.getLogger(LowerUpper.class.getName());
	
	
	public static void main(String[] args) {
		
		
		/*ReadTxt reader = new ReadTxt();
		HashMap<Integer, int[]> querys = new HashMap<Integer, int[]>();
		reader.bufferedReadTxtWithDeilimiterLB(querys, filePath, fileIn, delimiter, queryLength);*/
		ShareGenerator generator = new ShareGenerator(true);
		int counter =0;
		while(counter < numSeqs) {
			int[] query = new int[queryLength];
			long[] qLong = new long[queryLength];
			for(int i=0; i<queryLength; i++) {
				qLong[i] = generator.generateRandom(true); 
				query[i] = (int) qLong[i];
			}
			
			int[] lb = new int[queryLength];
			int[] ub = new int[queryLength];
			LowerUpperBoundGen boundsGen = new LowerUpperBoundGen();
			double e = System.nanoTime();
			boundsGen.generateBounds(queryLength, query, lb, ub, cr);
			double s = System.nanoTime();
			
			time += s-e;
			
			//generate share of query
			SharedSequence S = new SharedSequence(queryLength, Integer.MAX_VALUE, Integer.MAX_VALUE, 2, qLong, qLong);
			S.setClusterCenter(true);
			double e2 = System.nanoTime();
			generator.generate2SharedSequences(S, true);
			double s2 = System.nanoTime();
			timeShr += s2-e2;
			
			
			System.out.println("counter:"+counter);
			counter++;
		}
		
		log.info("--------------LowerUpper------------------");
		log.info("Number of sequences:"+counter);
		log.info("Total time:"+time/1e9);
		
		
		log.info("--------------QueryShare------------------");
		log.info("Number of sequences:"+counter);
		log.info("Total time:"+timeShr/1e9);
		
		
		
		
	}
}
