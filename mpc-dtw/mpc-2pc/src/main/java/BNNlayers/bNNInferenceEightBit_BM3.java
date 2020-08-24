package BNNlayers;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import eightBitAdditive.eightBitMultiplicationTriple;
import eightBitAdditive.fifteenBitAdditiveUtil;
import eightBitAdditive.fifteenBitMultiplicationTriple;
import flexSC.network.Client;
import flexSC.network.Server;
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

public class bNNInferenceEightBit_BM3 {

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


		for(int img_id = 2; img_id < 101; img_id++) {
			//int img_id = 6;
			preprocess_images pre_img = new preprocess_images();
			pre_img.modelfileName = "./resources/mnistasjpg/testSample/testSample/img_" + String.valueOf(img_id) + ".jpg";
			long[][][] x = pre_img.preprocess_mnist("a");

			int factor = 1000;//10^6

//			inputConvolutionLayer inputf = new inputConvolutionLayer();
//			long[] x_input1 = inputf.flaten(x[0]);
//
//			long[][] x_fc1 = new long[2][x_input1.length];
//
//			ShareGenerator ariGen = new ShareGenerator(true);
//
//			for (int i = 0; i < x_fc1[0].length; i++) {
//				ariGen.generateSharedDataPoint(x_input1[i], true);
//				long xi_0 = ariGen.x0;
//				long xi_1 = ariGen.x1;
//				x_fc1[0][i] = xi_0;
//				x_fc1[1][i] = xi_1;
//			}

			long[][][][] x_fc1 = new long[2][x.length][x[0].length][x[0][0].length];

			ShareGenerator ariGen = new ShareGenerator(true);

			for(int i = 0; i < x.length; i++){
				for(int j = 0; j < x[0].length; j++){
					for(int k = 0; k < x[0][0].length; k++) {
						ariGen.generateSharedDataPoint(x[i][j][k], true);
						long xi_0 = ariGen.x0;
						long xi_1 = ariGen.x1;
						x_fc1[0][i][j][k] = xi_0;
						x_fc1[1][i][j][k] = xi_1;
					}
				}
			}

			readingWeights rd_w = new readingWeights();
			rd_w.modelfileName = "./resources/BM3_no_bias_no_preprocess.json";
//			byte[][] a = rd_w.get_weight_fc("fc1.weight");
			//System.out.println("I am here input");

			inputConvolutionLayer conv1 = new inputConvolutionLayer();

			long[][][][] y_conv1 = conv1.compute_conv(false, sndChannel, rcvChannel, mt, x_fc1, rd_w.get_weight_conv("conv1.weight"));

			//System.out.println("I am here conv1");

			batchNormolizationBinaryActivationLayer bn1 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn1 = bn1.compute_long_conv(false,sndChannel,rcvChannel,mt2,mt,y_conv1,rd_w.get_ep_long_output("bn1"),rd_w.get_zeta_output("bn1"));

			//System.out.println("I am here bn1");

			maxPoolingLayer mp1 = new maxPoolingLayer();

			byte[][][][] y_mp1 = mp1.compute(false,sndChannel,rcvChannel,mt2,y_bn1);

			//System.out.println("I am here mp1");

			convolutionLayer conv2 = new convolutionLayer();

			short[][][][] y_conv2 = conv2.compute(false,sndChannel,rcvChannel,mt_15,y_mp1,rd_w.get_weight_conv("conv2.weight"));

			//System.out.println("I am here conv2");

			batchNormolizationBinaryActivationLayer bn2 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn2 = bn2.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv2,rd_w.get_ep_output("bn2"),rd_w.get_zeta_output("bn2"));

			//System.out.println("I am here bn2");

			maxPoolingLayer mp2 = new maxPoolingLayer();

			byte[][][][] y_mp2 = mp2.compute(false,sndChannel,rcvChannel,mt2,y_bn2);

			//System.out.println("I am here mp2");

			flattenLayer fl = new flattenLayer();

			byte[][] y_fl = fl.compute(false,sndChannel,rcvChannel,mt2,y_mp2);

			//System.out.println("I am here flatten layer");

			fullConnectedLayer fc1 = new fullConnectedLayer();

			short[][] y_fc1 = fc1.compute(false,sndChannel,rcvChannel,mt_15,y_fl,rd_w.get_weight_fc("classifier.0.weight"));

			//System.out.println("I am here fc1");

			batchNormolizationBinaryActivationLayer bn3 = new batchNormolizationBinaryActivationLayer();

			byte[][] y_bn3 = bn3.compute_short_fc(false,sndChannel,rcvChannel,mt2,mt_15,y_fc1,rd_w.get_ep_output("classifier.1"),rd_w.get_zeta_output("classifier.1"));

			//System.out.println("I am here bn3");

			fullConnectedLayer fc2 = new fullConnectedLayer();

			short[][] y_fc2 = fc2.compute(false,sndChannel,rcvChannel,mt_15,y_bn3,rd_w.get_weight_fc("classifier.4.weight"));

			//System.out.println("I am here fc2");

//			short[][] y_fc2 = {
//					{1,2,3,4,5,6,7,8,9,10},
//					{1,2,3,4,5,6,7,8,9,10}
//			};

			outputLayer outlayer = new outputLayer();

			long[][] y_outlayer = outlayer.compute_short_fc(false,sndChannel,rcvChannel,mt,y_fc2,rd_w.get_ep1_output("classifier.5"),rd_w.get_ep2_output("classifier.5"),1000);


			//System.out.println("I am here output");


//			//System.out.println("Fc1 length" + y_fc1[0].length);
//			//System.out.println("I am hereFC1");
////			long[] yy_fc1 = new long[y_fc1[0].length];
////			for (int i = 0; i < y_fc1[0].length; i++) {
////				//System.out.println("y" + i + ": ");
////				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
////				yy_fc1[i] = AdditiveUtil.add(y_fc1[0][i], y_fc1[1][i]);
////			}
//			//System.out.println("FC1 output" + Arrays.toString(yy_fc1));
//
//
//			//System.out.println("Before creat BN1");
//			batchNormolizationBinaryActivationLayer bn1 = new batchNormolizationBinaryActivationLayer();
//			//System.out.println("After creat BN1");
//			byte[][] y_bn1 = bn1.compute_long_fc(false, sndChannel, rcvChannel, mt2, mt, y_fc1, rd_w.get_ep_long_output("bn1"), rd_w.get_zeta_output("bn1"));
//
//			//System.out.println(y_fc1[0].length);
//			//System.out.println("I am hereBN1");
//			byte[] yy_bn1 = new byte[y_bn1[0].length];
//			for (int i = 0; i < y_bn1[0].length; i++) {
//				//System.out.println("y" + i + ": ");
//				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
//				yy_bn1[i] = BooleanUtil.xor(y_bn1[0][i], y_bn1[1][i]);
//			}
//			//System.out.println("BNBA1 output" + Arrays.toString(yy_bn1));
//
//			fullConnectedLayer fc2 = new fullConnectedLayer();
//			short[][] y_fc2 = fc2.compute(false, sndChannel, rcvChannel, mt_15, y_bn1, rd_w.get_weight_fc("fc2.weight"));
//
//			//System.out.println("I am hereFC2");
//			short[] yy_fc2 = new short[y_fc2[0].length];
//			for (int i = 0; i < y_fc2[0].length; i++) {
//				//System.out.println("y" + i + ": ");
//				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
//				yy_fc2[i] = fifteenBitAdditiveUtil.add(y_fc2[0][i], y_fc2[1][i]);
//			}
//			//System.out.println(Arrays.toString(yy_fc2));
//
//			batchNormolizationBinaryActivationLayer bn2 = new batchNormolizationBinaryActivationLayer();
//			byte[][] y_bn2 = bn2.compute_short_fc(false, sndChannel, rcvChannel, mt2, mt_15, y_fc2, rd_w.get_ep_output("bn2"), rd_w.get_zeta_output("bn2"));
//
//			//System.out.println("I am hereBN2");
//			byte[] yy_bn2 = new byte[y_bn2[0].length];
//			for (int i = 0; i < y_bn2[0].length; i++) {
//				//System.out.println("y" + i + ": ");
//				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
//				yy_bn2[i] = BooleanUtil.xor(y_bn2[0][i], y_bn2[1][i]);
//			}
//			//System.out.println(Arrays.toString(yy_bn2));
//
//			fullConnectedLayer fc3 = new fullConnectedLayer();
//			short[][] y_fc3 = fc3.compute(false, sndChannel, rcvChannel, mt_15, y_bn2, rd_w.get_weight_fc("fc4.weight"));
//
//			//System.out.println("I am here");
//			short[] yy_fc3 = new short[y_fc3[0].length];
//			for (int i = 0; i < y_fc3[0].length; i++) {
//				//System.out.println("y" + i + ": ");
//				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
//				yy_fc3[i] = fifteenBitAdditiveUtil.add(y_fc3[0][i], y_fc3[1][i]);
//			}
//			//System.out.println(Arrays.toString(yy_fc3));
//
//			outputLayer out_l = new outputLayer();
//			long[][] y_out_1 = out_l.compute_short_fc(false, sndChannel, rcvChannel, mt, y_fc3, rd_w.get_ep1_output("bn4"), rd_w.get_ep2_output("bn4"), factor);


			//recover
			//System.out.println("I am here");
			long[] y = new long[y_outlayer[0].length];
			for (int i = 0; i < y_outlayer[0].length; i++) {
				//System.out.println("y" + i + ": ");
				//y[i][j][k] = AdditiveUtil.add(y_conV1[0][i][j][k], y_conV1[1][i][j][k]);
				y[i] = AdditiveUtil.add(y_outlayer[0][i], y_outlayer[1][i]);
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

