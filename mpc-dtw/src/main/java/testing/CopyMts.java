package testing;

import common.parser.FileCopier;
import common.util.Config;
import common.util.Constants;

public class CopyMts {

	static String mtPath = Config.getSetting(Constants.CONFIG_MT_PATH);
	static String fileNameOutBase = Config.getSetting(Constants.CONFIG_MT_FILE_NAME_OUT);

	public static void main(String[] args) {

		int counter =0;
		if(args.length >= 1) {
			counter = Integer.parseInt(args[0]);
		}
		System.out.println("directory:"+mtPath);
		FileCopier copier = new FileCopier();
		copier.bantchCopyAll(mtPath, fileNameOutBase, counter);
		System.out.println("end");
	}

}
