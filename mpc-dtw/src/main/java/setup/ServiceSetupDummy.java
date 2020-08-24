package setup;

import java.util.ArrayList;

import additive.MultiplicationTriple;
import common.parser.ReadTxt;
import common.util.Config;
import common.util.Constants;

public class ServiceSetupDummy {

	public ArrayList<MultiplicationTriple> loadMT(int mtNum){
		ArrayList<MultiplicationTriple> mts = new ArrayList<MultiplicationTriple>();
		
		 String fileNameOutBase = Config.getSetting(Constants.CONFIG_MT_FILE_NAME_OUT);
		 String filePath = Config.getSetting(Constants.CONFIG_MT_PATH);
		 String separator = Config.getSetting(Constants.CONFIG_MT_SEPARATOR);
		 System.out.println("Separator: ["+separator+"]");
		 
		 int fileNum = (int) Math.ceil((double) mtNum/300);
		System.out.println("fileNum:"+fileNum+" mtNum:"+mtNum);
		 ReadTxt reader = new ReadTxt();
		 ArrayList<String> lines = reader.readTxtFileList(filePath, fileNameOutBase, fileNum);
		 for(int i=0; i<lines.size(); i++) {
			 MultiplicationTriple mt = new MultiplicationTriple();
			 String line = lines.get(i);
			 String[] lineArr = line.split("\\"+separator);
			 if(lineArr.length < 6) {
				 continue;
			 }
			 mt.tripleA0 = Long.parseLong(lineArr[0]);
			 mt.tripleA1 = Long.parseLong(lineArr[1]);
			 mt.tripleB0 = Long.parseLong(lineArr[2]);
			 mt.tripleB1 = Long.parseLong(lineArr[3]);
			 mt.tripleC0 = Long.parseLong(lineArr[4]);
			 mt.tripleC1 = Long.parseLong(lineArr[5]);
			 
			 mts.add(mt);
		 }
		 
		 
		 
		 return mts;
		
	}
}
