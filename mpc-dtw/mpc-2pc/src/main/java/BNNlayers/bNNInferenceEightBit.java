package BNNlayers;

import additive.AdditiveUtil;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import eightBitAdditive.eightBitAdditiveUtil;
import additive.MultiplicationTriple;
import eightBitAdditive.*;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBNN.SecXnorPopcountEightBit;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//import com.sun.deploy.net.MessageHeader;
//import booleanShr.BooleanShrGenerator;
//import booleanShr.BooleanUtil;
//import eightBitAdditive.eightBitAdditiveUtil;
//import eightBitAdditive.eightBitMulP0;
//import eightBitAdditive.eightBitMulP1;
//import eightBitAdditive.eightBitMultiplicationTriple;
//import flexSC.network.Client;
//import flexSC.network.Server;
//
//import utilMpc.Config2PC;
//import utilMpc.Constants2PC;

public class bNNInferenceEightBit {

	//inputs:
	// several (feature.len) inputact.row * inputact.column vector (0-255)
	// several (feature.len)  kernel.row * kernel.row matrixs (binary)
	//output:
	// several (feature.len) outputact.row * outputact.column vectors (binary)
	//parameters:
	//stride
	//padding
	//we do not consider bias in our case.

	public byte[][][] x;
	public byte[][][] w;

	public byte[][][] y;

	private Server sndChannel;
	private Client rcvChannel;

	public byte z0AND;
	public byte z1AND;

	public byte z0l = 0;
	public byte z1l = 0;

	public double bandwidth = 0.0;

	public long timeNetwork = 0;

//	public short[][][] BNN(boolean isDisconnect, Server sndChannel, Client rcvChannel,
//						   byte[][][] x) throws Exception{
//
//		int out_feature = w.length;
//		int in_feature = x.length;
//		int kenel_size = w[0].length;
//		int input_row = x[0].length;
//		int input_column = x[0][0].length;
//
//		short[][][] output_y = new short[out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1];
//		byte[][][][] total_x_for_y = new byte[out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1][(in_feature * kenel_size * kenel_size)];//x's channel * kernel_window_size^2
//		byte[][][][] total_w_for_y = new byte[out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1][(in_feature * kenel_size * kenel_size)];
//
//
//		SecXnorPopcountEightBit xnorp = new SecXnorPopcountEightBit();
//		//for each output channel
//		for(int i = 0; i<out_feature;i++){
//			//build single y matrix
//
//			//get w
//			byte[] w_flat = flaten(w[i]);
//
//			for(int i_y = 0; i_y<input_row - kenel_size + 1;i_y++){
//				for (int j_y = 0; j_y<input_column - kenel_size + 1;j_y++){
//					// caculate a single y
//
//					//append w
//					for(int j = 0; j<in_feature;j++){
//						System.arraycopy(w_flat, 0, total_w_for_y[i][i_y][j_y], j*kenel_size * kenel_size, kenel_size * kenel_size);
//
//						//append x
//						//get one value in a windows
//						byte[][] x_sub = getWindow(x[j], i_y, j_y, kenel_size);
//						byte[] x_flat = flaten(x_sub);
//						System.arraycopy(x_flat, 0, total_x_for_y[i][i_y][j_y], j*kenel_size * kenel_size, kenel_size * kenel_size);
//						}
//
//					//generate shares for a single y
//					BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
//
//					System.out.println("x:" + Arrays.toString(total_x_for_y[i][i_y][j_y]) + " w:" + Arrays.toString(total_w_for_y[i][i_y][j_y]));
//
////					byte[] x_sub = total_x_for_y[i][i_y][j_y];
////					byte[] w_sub = total_w_for_y[i][i_y][j_y];
//
//					byte[] x_0_sub_arr = new byte[total_x_for_y[i][i_y][j_y].length];
//					byte[] x_1_sub_arr = new byte[total_x_for_y[i][i_y][j_y].length];
//
//					byte[] w_0_sub_arr = new byte[total_w_for_y[i][i_y][j_y].length];
//					byte[] w_1_sub_arr = new byte[total_w_for_y[i][i_y][j_y].length];
//
//					for(int y_len=0; y_len<total_x_for_y[i][i_y][j_y].length;y_len++) {
//						boolGen.generateSharedDataPoint(total_x_for_y[i][i_y][j_y][y_len], true);
//						byte xi_sub_0 = boolGen.x0;
//						byte xi_sub_1 = boolGen.x1;
//						//System.out.println("i" + i + "  xi:" + x[i] + "  xi_0:" + xi_sub_0 + "  xi_1:" + xi_sub_1);
//						x_0_sub_arr[y_len] = xi_sub_0;
//						x_1_sub_arr[y_len] = xi_sub_1;
//
//						boolGen.generateSharedDataPoint(total_w_for_y[i][i_y][j_y][y_len], true);
//						byte wi_sub_0 = boolGen.x0;
//						byte wi_sub_1 = boolGen.x1;
//						//System.out.println("i" + i + "  wi:" + w[i] + "  wi_0:" + wi_sub_0 + "  wi_1:" + wi_sub_1);
//						w_0_sub_arr[y_len] = wi_sub_0;
//						w_1_sub_arr[y_len] = wi_sub_1;
//					}
//
//					//caculate z
//
//
//					short[] z_sub = xnorp.compute(false, sndChannel, rcvChannel, mt, x_0_sub_arr, w_0_sub_arr, x_1_sub_arr, w_1_sub_arr);
//
//					// verify
//					short z0_sub = z_sub[0];
//					short z1_sub = z_sub[1];
//
//					output_y[i][i_y][j_y] = eightBitAdditiveUtil.add(z0_sub, z1_sub);
//					System.out.println("z:" + eightBitAdditiveUtil.add(z0_sub, z1_sub) + " z0:" + z0_sub + " z1:" + z1_sub);
//					}
//				}
//
//
//		}
//  		return output_y;
//	}

	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0};

	public static void main(String[] args) throws Exception {
		final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		final int port = 5553;
		// Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
		System.out.println("Connection| hostname:port, " + hostname + ":" + port);

		final Server sndChannel = new Server();
		final Client rcvChannel = new Client();

		// Establish the connection
		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {

			@Override
			public void run() {
				sndChannel.listen(port);
				sndChannel.flush();
			}
		});

		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					rcvChannel.connect(hostname, port);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				rcvChannel.flush();
			}
		});
		// Connection should be established within 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
			}
		} catch (InterruptedException e) {
			// Something is wrong
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}


		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		eightBitMultiplicationTriple mt_8 = new eightBitMultiplicationTriple(sndChannel, rcvChannel);
		fifteenBitMultiplicationTriple mt_15 = new fifteenBitMultiplicationTriple(sndChannel,rcvChannel);
		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ANDTriple mt2 = new ANDTriple(boolGen);


		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();





//		long[][][] x = {
//				{{255,255,255,255},
//						{255,255,255,255},
//						{255,255,255,255},
//						{255,255,255,255},
//				},
//				{{255,255,255,255},
//						{255,255,255,255},
//						{255,255,255,255},
//						{255,255,255,255},
//				}
//		};

		
		int counter = 2; //101
		for(int img_id = 1; img_id < counter; img_id++) {
			//int img_id = 6;
			preprocess_images pre_img = new preprocess_images();
			pre_img.modelfileName = "./resources/mnistasjpg/testSample/testSample/img_" + String.valueOf(img_id) + ".jpg";
			long[][][] x = pre_img.preprocess_mnist("a");

			int factor = 1000;//10^6

			inputConvolutionLayer inputf = new inputConvolutionLayer();
			long[] x_input1 = inputf.flaten(x[0]);

			long[][] x_fc1 = new long[2][x_input1.length];

			ShareGenerator ariGen = new ShareGenerator(true);

			for (int i = 0; i < x_fc1[0].length; i++) {
				ariGen.generateSharedDataPoint(x_input1[i], true);
				long xi_0 = ariGen.x0;
				long xi_1 = ariGen.x1;
				x_fc1[0][i] = xi_0;
				x_fc1[1][i] = xi_1;
			}

			readingWeights rd_w = new readingWeights();
			rd_w.modelfileName = "./resources/BM1_no_bias_no_preprocess.json";
			byte[][] a = rd_w.get_weight_fc("fc1.weight");
//		System.out.println(Arrays.toString(a[0]));
//		System.out.println(a.length);
//		System.out.println(a[0].length);
//		System.out.println(x_fc1[0].length);
			//System.out.println("I am here:before input!");
//		long[] yy_fc1 = new long[x_input1[0].length];
//		for(int i = 0; i < x_input1[0].length; i++) {
//			System.out.println("y" + i + ": ");
//			//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
//			yy_fc1[i] = AdditiveUtil.add(x_input1[0][i], x_input1[1][i]);
//		}
			//System.out.println(Arrays.toString(x_input1));
			//System.out.println(Arrays.toString(a[0]));


			inputConvolutionLayer fc1 = new inputConvolutionLayer();

			long[][] y_fc1 = fc1.compute_fc(false, sndChannel, rcvChannel, mt, x_fc1, rd_w.get_weight_fc("fc1.weight"));//

			//System.out.println("Fc1 length" + y_fc1[0].length);
			//System.out.println("I am hereFC1");
			long[] yy_fc1 = new long[y_fc1[0].length];
			for (int i = 0; i < y_fc1[0].length; i++) {
				//System.out.println("y" + i + ": ");
				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
				yy_fc1[i] = AdditiveUtil.add(y_fc1[0][i], y_fc1[1][i]);
			}
			//System.out.println("FC1 output" + Arrays.toString(yy_fc1));


			//System.out.println("Before creat BN1");
			batchNormolizationBinaryActivationLayer bn1 = new batchNormolizationBinaryActivationLayer();
			//System.out.println("After creat BN1");
			byte[][] y_bn1 = bn1.compute_long_fc(false, sndChannel, rcvChannel, mt2, mt, y_fc1, rd_w.get_ep_long_output("bn1"), rd_w.get_zeta_output("bn1"));

			//System.out.println(y_fc1[0].length);
			//System.out.println("I am hereBN1");
			byte[] yy_bn1 = new byte[y_bn1[0].length];
			for (int i = 0; i < y_bn1[0].length; i++) {
				//System.out.println("y" + i + ": ");
				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
				yy_bn1[i] = BooleanUtil.xor(y_bn1[0][i], y_bn1[1][i]);
			}
			//System.out.println("BNBA1 output" + Arrays.toString(yy_bn1));

			fullConnectedLayer fc2 = new fullConnectedLayer();
			short[][] y_fc2 = fc2.compute(false, sndChannel, rcvChannel, mt_15, y_bn1, rd_w.get_weight_fc("fc2.weight"));

			//System.out.println("I am hereFC2");
			short[] yy_fc2 = new short[y_fc2[0].length];
			for (int i = 0; i < y_fc2[0].length; i++) {
				//System.out.println("y" + i + ": ");
				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
				yy_fc2[i] = fifteenBitAdditiveUtil.add(y_fc2[0][i], y_fc2[1][i]);
			}
			//System.out.println(Arrays.toString(yy_fc2));

			batchNormolizationBinaryActivationLayer bn2 = new batchNormolizationBinaryActivationLayer();
			byte[][] y_bn2 = bn2.compute_short_fc(false, sndChannel, rcvChannel, mt2, mt_15, y_fc2, rd_w.get_ep_output("bn2"), rd_w.get_zeta_output("bn2"));

			//System.out.println("I am hereBN2");
			byte[] yy_bn2 = new byte[y_bn2[0].length];
			for (int i = 0; i < y_bn2[0].length; i++) {
				//System.out.println("y" + i + ": ");
				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
				yy_bn2[i] = BooleanUtil.xor(y_bn2[0][i], y_bn2[1][i]);
			}
			//System.out.println(Arrays.toString(yy_bn2));

			fullConnectedLayer fc3 = new fullConnectedLayer();
			short[][] y_fc3 = fc3.compute(false, sndChannel, rcvChannel, mt_15, y_bn2, rd_w.get_weight_fc("fc4.weight"));

			//System.out.println("I am here");
			short[] yy_fc3 = new short[y_fc3[0].length];
			for (int i = 0; i < y_fc3[0].length; i++) {
				//System.out.println("y" + i + ": ");
				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
				yy_fc3[i] = fifteenBitAdditiveUtil.add(y_fc3[0][i], y_fc3[1][i]);
			}
			//System.out.println(Arrays.toString(yy_fc3));

			outputLayer out_l = new outputLayer();
			long[][] y_out_1 = out_l.compute_short_fc(false, sndChannel, rcvChannel, mt, y_fc3, rd_w.get_ep1_output("bn4"), rd_w.get_ep2_output("bn4"), factor);


			//recover
			//System.out.println("I am here");
			long[] y = new long[y_out_1[0].length];
			for (int i = 0; i < y_out_1[0].length; i++) {
				//System.out.println("y" + i + ": ");
				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
				y[i] = AdditiveUtil.add(y_out_1[0][i], y_out_1[1][i]);
			}
			System.out.println(Arrays.toString(y));
			long max = 0;
			int maxid = 0;

			for (int m_i = 0; m_i < y.length; m_i++) {

				if(y[m_i] > 1073741823){
					y[m_i] = y[m_i] - 21474836478L;
				}
				if (y[m_i] > max) {
					max = y[m_i];
					maxid = m_i;
				}
			}
			System.out.println("The image index is:   " + img_id);
			System.out.println(maxid);
		}



		sndChannel.disconnect();
		rcvChannel.disconnect();
	}



	}

