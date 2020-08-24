package preparation.main;

import java.util.Arrays;
import java.util.HashMap;

import common.parser.ReadTxt;
import common.parser.WriteFile;
import common.util.Config;
import common.util.Constants;
import common.util.UtilHelper;
import preprocess.lb.LowerUpperBoundGen;

public class LowerUpperTest {
	
	private static String filePath = Config.getSetting(Constants.CONFIG_LB_PATH);
	private static String fileIn = Config.getSetting(Constants.CONFIG_LB_FILE_NAME_IN);//QueryInteger.txt
	private static String fileOut = Config.getSetting(Constants.CONFIG_LB_FILE_NAME_OUT);//QueryInteger.txt
	private static String lbSeperator = Config.getSetting(Constants.CONFIG_LB_SEPARATOR);//[|]
	private static char delimiter = Config.getSettingChar(Constants.CONFIG_CSV_SEPARATOR);//[\space]
	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);//128
	private static double cr = Config.getSettingDouble(Constants.CONFIG_DTW_BAND_CR);//0.05

	public static void main(String[] args) {
		
		
		ReadTxt reader = new ReadTxt();
		HashMap<Integer, int[]> querys = new HashMap<Integer, int[]>();
		reader.bufferedReadTxtWithDeilimiterLB(querys, filePath, fileIn, delimiter, queryLength);
		int[] query = querys.get(0);
		
		
		int[] lb = new int[queryLength];
		int[] ub = new int[queryLength];
		LowerUpperBoundGen boundsGen = new LowerUpperBoundGen();
		boundsGen.generateBounds(queryLength, query, lb, ub, cr);
		
/*		for(int i =0; i<queryLength; i++){
			System.out.print(query[i] + " ");
		}
		System.out.println("");
		for(int i =0; i<queryLength; i++){
			System.out.print(lb[i] + " ");
		}
		System.out.println("");
		for(int i =0; i<queryLength; i++){
			System.out.print(ub[i] + " ");
		}*/
		
		String lbStr = "lb"+lbSeperator;
		String ubStr = "ub"+lbSeperator;
		int counter =0;
		for(int j =0; j<queryLength; j++) {
						
			lbStr = lbStr.concat(String.valueOf(lb[j])).concat(" "); 
			
			ubStr = ubStr.concat(String.valueOf(ub[j])).concat(" "); 
			//System.out.println(sequenceStr);
			counter++;
		}
		
		WriteFile writeFile = new WriteFile();
		writeFile.writeFile(filePath, fileOut, lbStr, false);
		writeFile.writeFile(filePath, fileOut, ubStr, false);
		
		
		System.out.println("End..."+" no of points: "+counter);
		
		
	}
}
