package common.parser;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileCopier {
	
	public void bantchCopyAll(String filePath, final String fileNameFormat, int counter) {
		File dir = new File(filePath);
		File[] files = dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {

				return name.endsWith(fileNameFormat);
			}

		});
		
		
		for (File myFile : files) {
			String fileName = myFile.getName();
			this.copyRename(filePath, fileName, counter+fileName);
		}
		
		
	}

	public void copyRename(String filePath, String fileName, String newName) {
		Path source = Paths.get(filePath + fileName);		
		Path target = Paths.get(filePath + newName);
		
		
		try {
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			 
			    
		} catch (IOException e) {
			System.out.println("copy file error. source:"+source.toString()+" target:"+target.toString());
			e.printStackTrace();
		}
		
	}
}
