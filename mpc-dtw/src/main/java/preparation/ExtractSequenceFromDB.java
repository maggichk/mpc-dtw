package preparation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import additive.SharedSequence;
import common.db.ConnectRedis;
import common.util.Config;
import common.util.Constants;
import redis.clients.jedis.Jedis;

public class ExtractSequenceFromDB {
	
	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);
	private static String seqSeparator = Config.getSetting(Constants.CONFIG_CSV_SEPARATOR);
	private static String dpSeparator = Config.getSetting(Constants.CONFIG_DP_SEPARATOR);
	
	public static void parseKey(SharedSequence sequence, String key) {
		String[] keys = key.split("\\"+dpSeparator);
		boolean isCluster = Boolean.parseBoolean(keys[0].trim().toLowerCase());
		int clusterNo = Integer.parseInt(keys[1].trim());
		int index= Integer.parseInt(keys[2].trim());
		int shareNo = Integer.parseInt(keys[3].trim());
		
		sequence.setClusterCenter(isCluster);
		sequence.setClusterIndex(clusterNo);
		sequence.setIndex(index);
		sequence.setShareNo(shareNo);
		sequence.setLength(queryLength);
	}
	
	public static void parseValue(SharedSequence sequence, String value) {
		String[] values = value.split(seqSeparator);
		long[] seqArr = new long[queryLength];
		long[] squareArr = new long[queryLength];
		for(int i=0; i<queryLength; i++) {
			seqArr[i] = Long.parseLong(values[i]);
			squareArr[i] =  Long.parseLong(values[i+queryLength]);
		}
		
		sequence.setSharedSequence(seqArr);
		sequence.setSharedSquareSequence(squareArr);
	}
	
	public static SharedSequence extract(Jedis jedis, String key) {
		SharedSequence sequence = new SharedSequence();
		String value = jedis.get(key);
		parseValue(sequence, value);
			
		parseKey(sequence, key);		
		
		return sequence;
	}
	
	
	//build key-value pairs
	public static String[] buildKeyValue(SharedSequence sequence) {
		String[] pair = new String[2];
		// build key-value pair
		String key = String.valueOf(sequence.isClusterCenter())+dpSeparator+ sequence.getClusterIndex()+dpSeparator
				+ sequence.getIndex()+dpSeparator + sequence.getShareNo();
		String value = "";
		String sharedSeqStr = "";
		String sharedSquareStr = "";
		for (int i = 0; i < queryLength; i++) {
			sharedSeqStr += sequence.getSharedSequence()[i] + seqSeparator;
			if (i == queryLength - 1) {
				sharedSquareStr += sequence.getSharedSquareSequence()[i];
			} else {
				sharedSquareStr += sequence.getSharedSquareSequence()[i] + seqSeparator;
			}
		}
		
		value += sharedSeqStr;
		value += sharedSquareStr;
		pair[0] = key;
		pair[1] = value;
		return pair;
	}
	
		//build key
		public static String buildKey(SharedSequence sequence) {
			
			// build key-value pair
			String key = String.valueOf(sequence.isClusterCenter())+dpSeparator+ sequence.getClusterIndex()+dpSeparator
					+ sequence.getIndex()+dpSeparator + sequence.getShareNo();
			
			return key;
		}
	
	public static void main(String[] args) {
		String ip ="localhost";
		int port = 6379;
		ConnectRedis connect = new ConnectRedis();
		Jedis jedis = connect.connectDb(ip, port);
		ExtractSequenceFromDB extractor = new ExtractSequenceFromDB();
		String key = "true|0|3918|0";
		SharedSequence seq = extractor.extract(jedis, key);
		System.out.println("seq:"+seq.getIndex()+" "+seq.isClusterCenter()+" "+seq.getClusterIndex()
		+" "+Arrays.toString(seq.getSharedSequence()));
		System.out.println(Arrays.toString(seq.getSharedSquareSequence()));
		
	}
}
