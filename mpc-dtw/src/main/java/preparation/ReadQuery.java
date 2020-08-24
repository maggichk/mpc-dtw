package preparation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import additive.AdditiveUtil;
import additive.ShareGenerator;
import additive.SharedSequence;
import common.util.Config;
import common.util.Constants;
import redis.clients.jedis.Jedis;

public class ReadQuery {

	private static String lbSeparator = Config.getSetting(Constants.CONFIG_LB_SEPARATOR); // [|]
	private static String seqSeparator = Config.getSetting(Constants.CONFIG_CSV_SEPARATOR); // [\space]
	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);

	public double time;
	
	public ArrayList<SharedSequence> readToModel(String filePath, String fileName) {
		ArrayList<SharedSequence> arr = new ArrayList<SharedSequence>();

		String directory = filePath + fileName;
		System.out.println("directory:" + directory);
		File fil = new File(directory);
		BufferedReader bf = null;
		FileReader fr = null;
		try {
			fr = new FileReader(fil);
			bf = new BufferedReader(fr);
			String nextLine;
			int counter = 0;
			while ((nextLine = bf.readLine()) != null) {
				// Integer.MAX indicates query, max-1 lb, max-2 ub

				if (counter == 0) {
					ShareGenerator generator = new ShareGenerator(true);
					SharedSequence query;
					String[] seqStrArr = nextLine.split(seqSeparator);
					long[] seqArr = new long[queryLength];
					long[] squareArr = new long[queryLength];
					for (int i = 0; i < queryLength; i++) {
						seqArr[i] = Long.parseLong(seqStrArr[i]);
						squareArr[i] = AdditiveUtil.mul(seqArr[i], seqArr[i]);
					}
					query = new SharedSequence(queryLength, Integer.MAX_VALUE, Integer.MAX_VALUE, 2, seqArr, squareArr);
					query.setClusterCenter(true);
					arr.add(query);

					double e = System.nanoTime();					
					generator.generate2SharedSequences(query, true);
					SharedSequence S0 = generator.S0;
					SharedSequence S1 = generator.S1;
					double s = System.nanoTime();
					time += s-e;
					
					arr.add(S0);
					arr.add(S1);

				} else {
					ShareGenerator generator = new ShareGenerator();
					SharedSequence b;

					String[] seqStrArr = nextLine.split("\\"+lbSeparator)[1].split(seqSeparator);
					long[] seqArr = new long[queryLength];
					long[] squareArr = new long[queryLength];
					for (int i = 0; i < queryLength; i++) {
						seqArr[i] = Long.parseLong(seqStrArr[i]);
						squareArr[i] = AdditiveUtil.mul(seqArr[i], seqArr[i]);
					}
					if (counter == 1) {

						b = new SharedSequence(queryLength, Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 2, seqArr,
								squareArr);
					} else {
						b = new SharedSequence(queryLength, Integer.MAX_VALUE - 2, Integer.MAX_VALUE, 2, seqArr,
								squareArr);
					}
					b.setClusterCenter(true);
					arr.add(b);

					double e = System.nanoTime();
					generator.generate2SharedSequences(b);
					SharedSequence S0 = generator.S0;
					SharedSequence S1 = generator.S1;
					double s = System.nanoTime();
					time += s-e;
					
					arr.add(S0);
					arr.add(S1);

				}

				counter++;
			}

		} catch (FileNotFoundException e) {
			System.out.println("File not found. Directory:" + directory);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Buffered Reader wrong.");
			e.printStackTrace();
		} finally {
			try {
				bf.close();
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return arr;
	}
	
	public void extractToDB(ArrayList<SharedSequence> sequences, Jedis jedis) {
		for(int i= 0; i<sequences.size(); i++) {
			String[] pair = ExtractSequenceFromDB.buildKeyValue(sequences.get(i));
			jedis.set(pair[0], pair[1]);
		}
		
		
	}

}
