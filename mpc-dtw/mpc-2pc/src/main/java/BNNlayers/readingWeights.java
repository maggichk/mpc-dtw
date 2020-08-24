package BNNlayers;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import eightBitAdditive.eightBitMultiplicationTriple;
import eightBitAdditive.fifteenBitAdditiveUtil;
import eightBitAdditive.fifteenBitMultiplicationTriple;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBNN.SecBinaryActivation2PC;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class readingWeights {

	//inputs:
	// jason file
	//output:
	// all parameters for building models.
	//we do not consider bias in our case.
	public String modelfileName;

//	public static void main(String[] args)
//	{
//		//JSON parser object to parse read file
//		JSONParser jsonParser = new JSONParser();
//
//		try (FileReader reader = new FileReader("./resources/BM3_no_bias.json"))
//		{
//			//Read JSON file
//			Object obj = jsonParser.parse(reader);
//
//			JSONObject class_parameters = (JSONObject) obj;
//
//			JSONArray weights_Array = (JSONArray) class_parameters.get("conv1.weight");
//			System.out.println(weights_Array.size());
//			JSONArray get_length_1 = (JSONArray) weights_Array.get(0);
//			System.out.println(get_length_1.size());
//			JSONArray get_length_2 = (JSONArray) get_length_1.get(0);
//			JSONArray get_width = (JSONArray) get_length_2.get(0);
//
//			byte[][][] weights = new byte[weights_Array.size()][get_length_1.size()][get_width.size()];
//			for (int i = 0; i < weights_Array.size(); i++) {
//				JSONArray i_channel = (JSONArray) weights_Array.get(i);
//				for (int j = 0; j < i_channel.size(); j++){
//					JSONArray j_row_1 = (JSONArray) i_channel.get(0);
//					JSONArray j_row = (JSONArray) j_row_1.get(j);
//
//					for(int k = 0; k < j_row.size(); k++){
//						System.out.println("This is:  " + j_row.get(k));
//						if(((Double) j_row.get(k)) == 1){
//							weights[i][j][k] = (byte) 1;
//						}
//						else{
//							weights[i][j][k] = (byte) 0;
//						}
//					}
//				}
//
//			}
//			System.out.println(Arrays.toString(weights[0][0]));
//
//
//
//			//JSONArray class_parameters = (JSONArray) obj;
////			System.out.println(weights_Array);
//
//			//Iterate over employee array
//			//class_parameters.forEach( emp -> classObject( (JSONObject) emp ) );
//
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ParseException e) {
//			e.printStackTrace();
//		}
//	}

	public byte[][][][] get_weight_conv(String layer_name) throws Exception {

		//JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();

		try (FileReader reader = new FileReader(modelfileName))
		{
			//Read JSON file
			Object obj = jsonParser.parse(reader);

			JSONObject class_parameters = (JSONObject) obj;

			JSONArray feature_Array = (JSONArray) class_parameters.get(layer_name);
			//System.out.println(feature_Array.size());
			JSONArray channel_Array_test = (JSONArray) feature_Array.get(0);
			//System.out.println(channel_Array_test.size());
			JSONArray length_Array_test = (JSONArray) channel_Array_test.get(0);
			JSONArray width_Array_test = (JSONArray) length_Array_test.get(0);

			byte[][][][] weights = new byte[feature_Array.size()][channel_Array_test.size()][length_Array_test.size()][width_Array_test.size()];
			for (int i = 0; i < feature_Array.size(); i++) {
				JSONArray channel_Array = (JSONArray) feature_Array.get(i);
				for (int j = 0; j < channel_Array.size(); j++){
					JSONArray length_Array = (JSONArray) channel_Array.get(j);
					for(int k = 0; k < length_Array.size(); k++){
					JSONArray width_Array = (JSONArray) length_Array.get(k);

						for(int l = 0; l < width_Array.size(); l++) {
							//System.out.println("This is:  " + width_Array.get(l));
							if (((Double) width_Array.get(l)) == 1) {
								weights[i][j][k][l] = (byte) 1;
							} else {
								weights[i][j][k][l] = (byte) 0;
							}
						}
					}
				}

			}
			//System.out.println(Arrays.toString(weights[0][0]));

			return weights;
		}
	}

	public byte[][] get_weight_fc(String layer_name) throws Exception {
		//JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();

		try (FileReader reader = new FileReader(modelfileName))
		{
			//Read JSON file
			Object obj = jsonParser.parse(reader);

			JSONObject class_parameters = (JSONObject) obj;

			JSONArray weights_Array = (JSONArray) class_parameters.get(layer_name);
			JSONArray get_length_1 = (JSONArray) weights_Array.get(0);
			//System.out.println(weights_Array.size());

			byte[][] weights = new byte[weights_Array.size()][get_length_1.size()];
			for (int i = 0; i < weights_Array.size(); i++) {
				//System.out.println("This is:  " + weights_Array.get(i));
				JSONArray get_length = (JSONArray) weights_Array.get(i);
				for (int j = 0; j < get_length.size(); j++) {
					if (((Double) get_length.get(j)) == 1) {
						weights[i][j] = (byte) 1;
					} else {
						weights[i][j] = (byte) 0;
					}
				}
			}
			//System.out.println(Arrays.toString(weights));

			return weights;
	}
	}

	public short[] get_ep_output(String layer_name) throws Exception {

		//ep = ep2/ep1
		//JSON parser object to parse read file

		float[] ep1 = this.get_ep1_output(layer_name);
		float[] ep2 = this.get_ep2_output(layer_name);
		float[] ep_debug = new float[ep1.length];
		short[] ep = new short[ep1.length];
		//double[] vars = new double[var_Array.size()];
		for (int i = 0; i < ep1.length; i++) {
			//System.out.println("This is:  " + weights_Array.get(i));
			ep_debug[i] = ep2[i]/ep1[i];
			if(ep_debug[i] >= 16384){
				ep[i] = 16384 - 1;
			}
			else if(ep_debug[i] < -16384){
				ep[i] = -16384;
			}
			else{
				ep[i] = fifteenBitAdditiveUtil.modAdditive((short) (ep2[i]/ep1[i]));
			}
			//ep[i] = (short) (ep2[i]/ep1[i]);
			//vars[i] = (Double) var_Array.get(i);

		}
		//System.out.println(Arrays.toString(ep));
		//System.out.println(Arrays.toString(ep_debug));
		return ep;
		}

	public long[] get_ep_long_output(String layer_name) throws Exception {

		//ep = ep2/ep1
		//JSON parser object to parse read file

		float[] ep1 = this.get_ep1_output(layer_name);
		float[] ep2 = this.get_ep2_output(layer_name);
		float[] ep_debug = new float[ep1.length];
		long[] ep = new long[ep1.length];
		//double[] vars = new double[var_Array.size()];
		for (int i = 0; i < ep1.length; i++) {
			//System.out.println("This is:  " + weights_Array.get(i));
			ep_debug[i] = ep2[i]/ep1[i];
			if(ep_debug[i] >= 536870912){
				ep[i] = 536870912 - 1;
			}
			else if(ep_debug[i] < -536870912){
				ep[i] = -536870912;
			}
			else{
				ep[i] = AdditiveUtil.modAdditive((long) (ep2[i]/ep1[i]));
			}

			//vars[i] = (Double) var_Array.get(i);

		}
		//System.out.println(Arrays.toString(ep));
		//System.out.println(Arrays.toString(ep_debug));
		return ep;
	}

	public byte[] get_zeta_output(String layer_name) throws Exception {

		//zeta = sign(ep1)
		//JSON parser object to parse read file

		float[] ep1 = this.get_ep1_output(layer_name);

		byte[] zeta = new byte[ep1.length];
		//double[] vars = new double[var_Array.size()];
		for (int i = 0; i < ep1.length; i++) {
			//System.out.println("This is:  " + weights_Array.get(i));
			if(ep1[i] < 0){
				zeta[i] = (byte) 0;
			}
			else{
				zeta[i] = (byte) 1;
			}
			//zeta[i] = (short) (ep2[i]/ep1[i]);
			//vars[i] = (Double) var_Array.get(i);

		}
		//System.out.println(Arrays.toString(zeta));
		//System.out.println(Arrays.toString(ep_debug));
		return zeta;
	}


	public float[] get_ep1_output(String layer_name) throws Exception {

		//ep1 = weight/var
		//JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();

		try (FileReader reader = new FileReader(modelfileName))
		{
			//Read JSON file
			Object obj = jsonParser.parse(reader);

			JSONObject class_parameters = (JSONObject) obj;

			JSONArray weights_Array = (JSONArray) class_parameters.get(layer_name + ".weight");
			JSONArray var_Array = (JSONArray) class_parameters.get(layer_name+ ".running_var");
			//System.out.println(weights_Array.size());

			float[] ep1 = new float[weights_Array.size()];
			//double[] vars = new double[var_Array.size()];
			for (int i = 0; i < weights_Array.size(); i++) {
				//System.out.println("This is:  " + weights_Array.get(i));
				ep1[i] = (float) (((Double) (weights_Array.get(i)))/(Math.sqrt((Double) (var_Array.get(i)))));
				//vars[i] = (Double) var_Array.get(i);

			}

			//System.out.println(Arrays.toString(weights));

			return ep1;
		}
	}

	public float[] get_ep2_output(String layer_name) throws Exception {
		//ep2 = bias - weight*mean/var


		//JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();

		try (FileReader reader = new FileReader(modelfileName))
		{
			//Read JSON file
			Object obj = jsonParser.parse(reader);

			JSONObject class_parameters = (JSONObject) obj;

			JSONArray weights_Array = (JSONArray) class_parameters.get(layer_name + ".weight");
			JSONArray mean_Array = (JSONArray) class_parameters.get(layer_name+ ".running_mean");
			JSONArray var_Array = (JSONArray) class_parameters.get(layer_name+ ".running_var");
			JSONArray bias_Array = (JSONArray) class_parameters.get(layer_name+ ".bias");
			//System.out.println(weights_Array.size());

			float[] ep2 = new float[weights_Array.size()];
			//double[] vars = new double[var_Array.size()];
			for (int i = 0; i < weights_Array.size(); i++) {
				//System.out.println("This is:  " + weights_Array.get(i));
				ep2[i] = (float) (((Double) (bias_Array.get(i))) - (((Double) (weights_Array.get(i))) * ((Double) (mean_Array.get(i)))/(Math.sqrt((Double)(var_Array.get(i))))));
				//vars[i] = (Double) var_Array.get(i);

			}
			//System.out.println(Arrays.toString(weights));

			return ep2;
		}
	}


	public static void main(String[] args) throws Exception {

		readingWeights rw = new readingWeights();
		rw.modelfileName = "./resources/BM1_no_bias.json";

		long[] ep1 = rw.get_ep_long_output("bn1");
		System.out.println(ep1.length);

//		byte[] ep1 = rw.get_zeta_output("classifier.1");
//		System.out.println(Arrays.toString(ep1));


//		byte[][][][] weights_conv = rw.get_weight_conv("conv1.weight");
//
//		System.out.println(Arrays.toString(weights_conv[0][0][0]));
//
//		byte[][] weights_fc = rw.get_weight_fc("classifier.4.weight");
//		System.out.println(Arrays.toString(weights_fc[0]));
//		System.out.println(weights_fc.length);
//		System.out.println(weights_fc[0].length);


	}
}

