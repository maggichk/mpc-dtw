package preparation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import additive.AdditiveUtil;
import additive.SharedSequence;
import common.util.Config;
import common.util.Constants;
import common.util.UtilHelper;
import redis.clients.jedis.Jedis;

public class ReadSequenceToDB {

	private final String dpSeparator = Config.getSetting(Constants.CONFIG_DP_SEPARATOR); // [|]
	private final String seqSeparator = Config.getSetting(Constants.CONFIG_CSV_SEPARATOR);// [\space]
	private final int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);

	private int arrLen;
	private int[] centerIndex;
	private int shareNo = 2; // all share no. are specified as 2 when reading

	// read from file
	public void read2DB(int startIndex, int clusterStartIndex, String filePath, String fileName, Jedis jedis) {

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
				if (counter == 0) {
					SharedSequence sequence = this.extract2Model(nextLine, counter);
					counter++;
					continue;
				}

				SharedSequence sequence = this.extract2Model(nextLine, counter);
				//System.out.println("startindex:" + startIndex);
				//System.out.println("seq index:" + sequence.getIndex());
				sequence.setIndex(sequence.getIndex() + startIndex);
				sequence.setClusterIndex(sequence.getClusterIndex()+clusterStartIndex);

				if (null != sequence) {
					// build key-value pair
					String key = String.valueOf(sequence.isClusterCenter()) + dpSeparator + sequence.getClusterIndex()
							+ dpSeparator + sequence.getIndex() + dpSeparator + sequence.getShareNo();
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

					// insert to DB
					jedis.set(key, value);

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
	}

	// extract info to SharedSequence
	public SharedSequence extract2Model(String line, int lineNo) {
		SharedSequence sequence = null;

		String[] lineArr = line.split("\\" + dpSeparator);
		//System.out.println("line:" + line + " dpSeparator:" + dpSeparator);
		if (lineNo == 0) {

			arrLen = lineArr.length;
			centerIndex = new int[arrLen];
			for (int i = 0; i < arrLen; i++) {

				centerIndex[i] = Integer.parseInt(lineArr[i]);
			}

		} else {

			int index = Integer.parseInt(lineArr[0]);
			int clusterNo = Integer.parseInt(lineArr[1]);
			int shareNo = this.shareNo;
			boolean isCenter = false;
			if (UtilHelper.isArrayContainInt(this.centerIndex, index)) {
				isCenter = true;
			}

			String[] seqStr = lineArr[2].split(seqSeparator);
			long[] sharedSequence = new long[queryLength];
			long[] sharedSquareSequence = new long[queryLength];
			for (int i = 0; i < queryLength; i++) {
				if (!UtilHelper.isEmptyString(seqStr[i].trim())) {
					sharedSequence[i] = Long.parseLong(seqStr[i]);
					sharedSquareSequence[i] = AdditiveUtil.mul(sharedSequence[i], sharedSequence[i]);
				}
			}
			//System.out.println("index:" + index);
			sequence = new SharedSequence(queryLength, index, clusterNo, shareNo, sharedSequence, sharedSquareSequence);
			sequence.setClusterCenter(isCenter);

		}

		return sequence;

	}

}
