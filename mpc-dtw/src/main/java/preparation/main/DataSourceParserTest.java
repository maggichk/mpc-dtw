package preparation.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import common.parser.Tokenizer;
import common.parser.WriteFile;
import common.util.Config;
import common.util.Constants;
import common.util.UtilHelper;






/**
 * 
 * @author maggie liu 27/4/2017
 *
 */
public class DataSourceParserTest {

	public static void main(String[] args) {
		String fileNameIn = "";
		String fileNameOut = "";
		if (args.length >= 2) {
			fileNameIn = args[0];
			fileNameOut = args[1];
		} else {
			fileNameIn = Config.getSetting(Constants.CONFIG_DATA_FILE_NAME_IN);
			fileNameOut = Config.getSetting(Constants.CONFIG_DATA_FILE_NAME_OUT);

		}
		String filePath = Config.getSetting(Constants.CONFIG_FILE_PATH);
		String directoryIn = filePath + fileNameIn;
		String directoryOut = filePath + fileNameOut;

		char delimiter = String.valueOf(Config.getSetting(Constants.CONFIG_CSV_SEPARATOR)).charAt(0);
		System.out.println("["+String.valueOf(Config.getSetting(Constants.CONFIG_CSV_SEPARATOR))+"]");
		
		int queryLength = Integer.parseInt(Config.getSetting(Constants.CONFIG_DTW_QUERY_LENGTH));
		DataSourceParserTest readCsv = new DataSourceParserTest();
		System.out.println(directoryIn);
		ArrayList<int[]> sequenceList = readCsv.readCsvFileEcg(directoryIn, delimiter, queryLength);
		System.out.println("Number of sequences:"+sequenceList.size());
		//System.exit(0);
		WriteFile writeFile = new WriteFile();
		boolean isLastLine = false;
		int counter = 0;
		
		for(int i =0; i< sequenceList.size(); i++) {
			int[] sequence  = sequenceList.get(i);
			String sequenceStr = "";
			
			for(int j =0; j<sequence.length; j++) {
							
				sequenceStr = sequenceStr.concat(String.valueOf(sequence[j])).concat(" "); 
				//System.out.println(sequenceStr);
				counter++;
			}
//			if(i == sequenceList.size()) {
//				isLastLine=true;
//			}
			//System.out.println(sequenceStr);
			writeFile.writeFile(filePath, fileNameOut, sequenceStr, isLastLine);
			
			if(i%1000 ==0) {
				System.out.println("Progress......"+i);
			}
		}
		System.out.println("End..."+counter);
	}

	/**
	 * Read Tweets CSV file
	 * 
	 * @param directory
	 * @param delimiter
	 * @return
	 */
	public ArrayList<int[]> readCsvFileEcg(String directory, char delimiter, int queryLength) {

		ArrayList<int[]> sequenceList = new ArrayList<int[]>();
		File fil = new File(directory);
		
		FileInputStream fis = null;

		BufferedReader fr = null;
		CSVParser parser;
		CSVReader reader;
		

		
		try {

			fis = new FileInputStream(fil);
			fr = new BufferedReader(new InputStreamReader(fis));
			parser = new CSVParserBuilder().withSeparator(delimiter).withIgnoreQuotations(true).build();
			reader = new CSVReaderBuilder(fr).withSkipLines(0).withCSVParser(parser).build();
			
			String[] nextLine;
			int lineNum = 1;

			while ((nextLine = reader.readNext()) != null) {					
				System.out.println("number of points:"+nextLine.length);
				//System.exit(0);
				//queryLength or 1
				int[] sequence = new int[queryLength];
				
				int numPoint = 0;
				int counter = 0;
				for(int i = 0; i< nextLine.length; i++) {
					
					//System.out.println("["+nextLine[i]+"]");
					
					if(UtilHelper.isEmptyString(nextLine[i].trim())) {
						continue;
					}
					numPoint++;
					int point = Tokenizer.trim2Integer(nextLine[i]);
					if(counter < queryLength) {
						
						sequence[counter] = point;
						//System.out.println(counter+" "+ point);
						
						//counter++;
						
						if(counter == queryLength-1) {
							//System.out.println(counter+" 111");
							
							sequenceList.add(sequence);	
							sequence = new int[queryLength];
							//System.out.println("Add to list, counter:"+counter);
							counter = 0;
						}else {
							//sequenceList.add(sequence);
							//System.out.println(counter+" 222");
							counter++;
						}
					}			
				}
				System.out.println("Num of data points:"+numPoint);
				
				
				/*if (lineNum == Config.getSettingInt(Constants.CONFIG_CANDIDATE)) {
					break;
				}
*/
				lineNum++;
			}
			System.out.println("Num of lines:"+lineNum);
			
			return sequenceList;
			
			
		} catch (IOException e) {

			e.printStackTrace();
			return null;
		} finally {
			try {
				fr.close();
				fis.close();
				
			} catch (IOException e) {

				e.printStackTrace();
			}

		}

	}

	

}
