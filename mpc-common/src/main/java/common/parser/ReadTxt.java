package common.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;

import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import common.model.Sample;
import common.util.UtilHelper;
import redis.clients.jedis.Jedis;

/**
 * 
 * @author maggie liu
 *
 */
public class ReadTxt {

	public ArrayList<String> readTxtFileList(String filePath, final String fileNameFormat, int fileNum) {
		ArrayList<String> lines = new ArrayList<String>();

		File dir = new File(filePath);
		File[] files = dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {

				return name.endsWith(fileNameFormat);
			}

		});

		if (files.length < fileNum) {
			System.out.println("Not enough multiplication triple files.");
			System.exit(0);
		}

		int counter = 0;
		for (File myFile : files) {

			if (counter >= fileNum) {
				break;
			}

			try {
				BufferedReader br = new BufferedReader(new FileReader(myFile));
				String line = null;

				while ((line = br.readLine()) != null) {

					if (line.length() == 0)
						continue;

					lines.add(line);
				}

				// br should be closed before deleting files
				br.close();

				if (!myFile.delete()) {
					System.out.println("unable to delete file:" + myFile);
					// System.exit(0);
				} else {
					System.out.println("file deleted:" + myFile);
					// System.exit(0);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

			counter++;
		}

		return lines;
	}

	public void bufferedReadTxtWithDeilimiterLB(HashMap<Integer, int[]> samples, String filePath, String fileName,
			char delimiter, int queryLength) {

		String directory = filePath + fileName;
		System.out.println("directory:" + directory);
		File f = new File(directory);

		// HashMap<Integer, Sample> samples = new HashMap<Integer, Sample>();
		int index = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = null;

			while ((line = br.readLine()) != null) {
				int[] atts = new int[queryLength];

				if (line.length() == 0)
					continue;

				String[] seg = line.split(String.valueOf(delimiter));
				// System.out.println("seg.size:"+seg.length+" delimiter:"+delimiter);
				for (int i = 0; i < queryLength; i++) {
					// System.out.print(seg[i]);
					// atts[i] = Integer.parseInt(seg[i].substring(0, 1)+seg[i].substring(2));
					if (!UtilHelper.isEmptyString(seg[i])) {
						atts[i] = Integer.parseInt(seg[i]);
					}

				}
				samples.put(index, atts);
				index++;
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("no of samples: " + samples.size());

	}

	public HashMap<Integer, Sample> bufferedReadTxtWithDeilimiterDP(String filePath, String fileName, char delimiter,
			int queryLength) {

		String directory = filePath + fileName;
		System.out.println("directory:" + directory);
		File f = new File(directory);

		HashMap<Integer, Sample> samples = new HashMap<Integer, Sample>();
		int index = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = null;

			while ((line = br.readLine()) != null) {
				int[] atts = new int[queryLength];
				if (line.length() == 0)
					continue;
				String[] seg = line.split(String.valueOf(delimiter));
				for (int i = 0; i < queryLength; i++) {
					// atts[i] = Integer.parseInt(seg[i].substring(0, 1)+seg[i].substring(2));
					atts[i] = Integer.parseInt(seg[i]);
				}
				samples.put(index, new Sample(atts, index));
				index++;
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("no of samples: " + samples.size());
		return samples;
	}

	/**
	 * read txt
	 * 
	 * @param filePath
	 * @param fileName
	 * @return
	 */
	public ArrayList<String> bufferedReadTxt(String filePath, String fileName) {
		ArrayList<String> dic = new ArrayList<String>();

		String directory = filePath + fileName;
		System.out.println("directory:" + directory);
		File fil = new File(directory);
		BufferedReader bf = null;
		FileReader fr = null;

		try {
			fr = new FileReader(fil);
			bf = new BufferedReader(fr);

			String nextLine;
			while ((nextLine = bf.readLine()) != null) {
				dic.add(nextLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bf.close();
				fr.close();

			} catch (IOException e) {

				e.printStackTrace();
			}

		}

		return dic;
	}

	/**
	 * read file and convert to byte
	 * 
	 * @param filePath
	 * @param fileName
	 * @param d
	 * @param idList
	 * @param isCheckAll
	 * @return
	 */
	public HashMap<Integer, byte[]> readToByteById(String filePath, String fileName, int d, ArrayList<String> idList,
			boolean isCheckAll) {

		HashMap<Integer, byte[]> map = new HashMap<Integer, byte[]>();

		String directory = filePath + fileName;

		File fil = new File(directory);
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(fil);

			int sizeB = 0;
			int divide = d / 8;
			int mod = d % 8;
			if (mod == 0) {
				sizeB = divide;
			} else {
				sizeB = divide + 1;
			}

			int idIndex = 0;

			boolean isContinue = true;
			while (isContinue) {

				byte[] singleTweet = new byte[sizeB];
				int stop = fis.read(singleTweet);

				if (stop == -1) {
					isContinue = false;
					break;
				}

				if (isCheckAll) {
					map.put(idIndex, singleTweet);
				} else {
					if (idList.contains(Integer.toString(idIndex))) {

						// add to map
						map.put(idIndex, singleTweet);

					}
				}

				idIndex++;

			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fis.close();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		return map;
	}

}
