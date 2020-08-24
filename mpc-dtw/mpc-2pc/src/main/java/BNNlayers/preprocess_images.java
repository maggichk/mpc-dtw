package BNNlayers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.FileReader;
import java.util.Arrays;

import java.nio.file.Files;
import java.io.File;

public class preprocess_images {

	//inputs:
	// jason file
	//output:
	// all parameters for building models.
	//we do not consider bias in our case.
	public String modelfileName;

	public long[][][] preprocess_mnist(String layer_name) throws Exception {


		File file = new File(modelfileName);
		BufferedImage img = ImageIO.read(file);
		int width = img.getWidth();
		int height = img.getHeight();
		long[][][] imgArr = new long[1][width][height];
		Raster raster = img.getData();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				imgArr[0][j][i] = raster.getSample(i, j, 0);
			}
		}
//		BufferedImage img= ImageIO.read(new File("./resources/0.jpg"));
//		long pix[][]= new long[img.getHeight()][img.getWidth()];
//		System.out.println(pix.length);
//		System.out.println(Arrays.toString(pix[0]));
//
//		File input = new File("blackandwhite.jpg");
//
////		File fi = new File("./resources/0.jpg");
////		byte[] fileContent = Files.readAllBytes(fi.toPath());
////		System.out.println(Arrays.toString(fileContent));
//		long[][][] out = {pix,};
//
//		for(int i = 0; i < pix.length; i++){
//			System.out.println(Arrays.toString(pix[i]));
//		}


		return imgArr;

	}

	public long[][][] preprocess_cifar10(String layer_name) throws Exception {


		BufferedImage image;
		int width;
		int height;

		File input = new File(modelfileName);
		image = ImageIO.read(input);
		width = image.getWidth();
		height = image.getHeight();

		long[][][] image_input = new long[3][width][height];

		//int count = 0;

		for(int i=0; i<height; i++) {

			for(int j=0; j<width; j++) {

				//count++;
				Color c = new Color(image.getRGB(j, i));

				image_input[0][j][i] = c.getRed();
				image_input[1][j][i] = c.getGreen();
				image_input[2][j][i] = c.getBlue();
				//System.out.println("S.No: " + i+"," + j + " Red: " + c.getRed() +"  Green: " + c.getGreen() + " Blue: " + c.getBlue());
			}
		}

//		BufferedImage img= ImageIO.read(new File("./resources/0.jpg"));
//		long pix[][]= new long[img.getHeight()][img.getWidth()];
//		System.out.println(pix.length);
//		System.out.println(Arrays.toString(pix[0]));
//
////		File fi = new File("./resources/0.jpg");
////		byte[] fileContent = Files.readAllBytes(fi.toPath());
////		System.out.println(Arrays.toString(fileContent));
//		long[][][] out = {pix,};


		return image_input;

	}


	public static void main(String[] args) throws Exception {

		preprocess_images a =new preprocess_images();

		long[][][] b = a.preprocess_mnist("a");
		//System.out.println(Arrays.toString(b[0][0]));

//		long[][][] b = a.preprocess_cifa("a");
		//System.out.println(b[0].length);


	}
}

