package BNNlayers;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import eightBitAdditive.*;
//		eightBitMultiplicationTriple;
//import eightBitAdditive.fifteenBitMultiplicationTriple;
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

public class bNNInferenceEightBit_BC2 {

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


		for(int img_id = 2; img_id < 3; img_id++) {
			//int img_id = 6;
			preprocess_images pre_img = new preprocess_images();
			//pre_img.modelfileName = "./resources/mnistasjpg/testSample/testSample/img_" + String.valueOf(img_id) + ".jpg";
			pre_img.modelfileName = "./resources/6.png";
			long[][][] x = pre_img.preprocess_cifar10("a");

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
			rd_w.modelfileName = "./resources/BC2_no_bias_no_preprocess.json";
//			byte[][] a = rd_w.get_weight_fc("fc1.weight");
			//System.out.println("I am here input");

			inputConvolutionLayer conv1 = new inputConvolutionLayer();

			long[][][][] y_conv1 = conv1.compute_conv_padding1(false, sndChannel, rcvChannel, mt, x_fc1, rd_w.get_weight_conv("features.0.weight"));

			System.out.println("I am here conv1");
			long[] show_conv1 = new long[y_conv1[0][0][0].length];
			for(int i = 0; i < y_conv1[0][0][0].length; i++){
				show_conv1[i] = AdditiveUtil.add(y_conv1[0][0][0][i],y_conv1[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_conv1));

			batchNormolizationBinaryActivationLayer bn1 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn1 = bn1.compute_long_conv(false,sndChannel,rcvChannel,mt2,mt,y_conv1,rd_w.get_ep_long_output("features.1"),rd_w.get_zeta_output("features.1"));

			System.out.println("I am here bn1");
			byte[] show_bn1 = new byte[y_bn1[0][0][0].length];
			for(int i = 0; i < y_bn1[0][0][0].length; i++){
				show_bn1[i] = BooleanUtil.xor(y_bn1[0][0][0][i],y_bn1[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_bn1));

			convolutionLayer conv2 = new convolutionLayer();

			short[][][][] y_conv2 = conv2.compute_padding1(false,sndChannel,rcvChannel,mt_15,y_bn1,rd_w.get_weight_conv("features.3.weight"));

			System.out.println("I am here conv2");
			short[] show_conv2 = new short[y_conv2[0][0][0].length];
			for(int i = 0; i < y_conv2[0][0][0].length; i++){
				show_conv2[i] = fifteenBitAdditiveUtil.add(y_conv2[0][0][0][i],y_conv2[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_conv2));

			batchNormolizationBinaryActivationLayer bn2 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn2 = bn2.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv2,rd_w.get_ep_output("features.4"),rd_w.get_zeta_output("features.4"));

			System.out.println("I am here bn2");
			byte[] show_bn2 = new byte[y_bn2[0][0][0].length];
			for(int i = 0; i < y_bn2[0][0][0].length; i++){
				show_bn2[i] = BooleanUtil.xor(y_bn2[0][0][0][i],y_bn2[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_bn2));

			convolutionLayer conv3 = new convolutionLayer();

			short[][][][] y_conv3 = conv3.compute_padding1(false,sndChannel,rcvChannel,mt_15,y_bn2,rd_w.get_weight_conv("features.6.weight"));

			System.out.println("I am here conv3");
			short[] show_conv3 = new short[y_conv3[0][0][0].length];
			for(int i = 0; i < y_conv3[0][0][0].length; i++){
				show_conv3[i] = fifteenBitAdditiveUtil.add(y_conv3[0][0][0][i],y_conv3[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_conv3));

			batchNormolizationBinaryActivationLayer bn3 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn3 = bn3.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv3,rd_w.get_ep_output("features.7"),rd_w.get_zeta_output("features.7"));

			System.out.println("I am here bn3");
			byte[] show_bn3 = new byte[y_bn3[0][0][0].length];
			for(int i = 0; i < y_bn3[0][0][0].length; i++){
				show_bn3[i] = BooleanUtil.xor(y_bn3[0][0][0][i],y_bn3[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_bn3));

			maxPoolingLayer mp1 = new maxPoolingLayer();

			byte[][][][] y_mp1 = mp1.compute(false,sndChannel,rcvChannel,mt2,y_bn3);

			System.out.println("I am here mp1");
			byte[] show_mp1 = new byte[y_mp1[0][0][0].length];
			for(int i = 0; i < y_mp1[0][0][0].length; i++){
				show_mp1[i] = BooleanUtil.xor(y_mp1[0][0][0][i],y_mp1[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_mp1));

			convolutionLayer conv4 = new convolutionLayer();

			short[][][][] y_conv4 = conv4.compute_padding1(false,sndChannel,rcvChannel,mt_15,y_mp1,rd_w.get_weight_conv("features.10.weight"));

			System.out.println("I am here conv4");
			short[] show_conv4 = new short[y_conv4[0][0][0].length];
			for(int i = 0; i < y_conv4[0][0][0].length; i++){
				show_conv4[i] = fifteenBitAdditiveUtil.add(y_conv4[0][0][0][i],y_conv4[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_conv4));

			batchNormolizationBinaryActivationLayer bn4 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn4 = bn4.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv4,rd_w.get_ep_output("features.11"),rd_w.get_zeta_output("features.11"));

			System.out.println("I am here bn4");
			byte[] show_bn4 = new byte[y_bn4[0][0][0].length];
			for(int i = 0; i < y_bn4[0][0][0].length; i++){
				show_bn4[i] = BooleanUtil.xor(y_bn4[0][0][0][i],y_bn4[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_bn4));

			convolutionLayer conv5 = new convolutionLayer();

			short[][][][] y_conv5 = conv5.compute_padding1(false,sndChannel,rcvChannel,mt_15,y_bn4,rd_w.get_weight_conv("features.13.weight"));

			System.out.println("I am here conv5");
			short[] show_conv5 = new short[y_conv5[0][0][0].length];
			for(int i = 0; i < y_conv5[0][0][0].length; i++){
				show_conv5[i] = fifteenBitAdditiveUtil.add(y_conv5[0][0][0][i],y_conv5[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_conv5));

			batchNormolizationBinaryActivationLayer bn5 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn5 = bn5.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv5,rd_w.get_ep_output("features.14"),rd_w.get_zeta_output("features.14"));

			System.out.println("I am here bn5");
			byte[] show_bn5 = new byte[y_bn5[0][0][0].length];
			for(int i = 0; i < y_bn5[0][0][0].length; i++){
				show_bn5[i] = BooleanUtil.xor(y_bn5[0][0][0][i],y_bn5[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_bn5));

			convolutionLayer conv6 = new convolutionLayer();

			short[][][][] y_conv6 = conv6.compute_padding1(false,sndChannel,rcvChannel,mt_15,y_bn5,rd_w.get_weight_conv("features.16.weight"));

			System.out.println("I am here conv6");
			short[] show_conv6 = new short[y_conv6[0][0][0].length];
			for(int i = 0; i < y_conv6[0][0][0].length; i++){
				show_conv6[i] = fifteenBitAdditiveUtil.add(y_conv6[0][0][0][i],y_conv6[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_conv6));

			batchNormolizationBinaryActivationLayer bn6 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn6 = bn6.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv6,rd_w.get_ep_output("features.17"),rd_w.get_zeta_output("features.17"));

			System.out.println("I am here bn6");
			byte[] show_bn6 = new byte[y_bn6[0][0][0].length];
			for(int i = 0; i < y_bn6[0][0][0].length; i++){
				show_bn6[i] = BooleanUtil.xor(y_bn6[0][0][0][i],y_bn6[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_bn6));

			maxPoolingLayer mp2 = new maxPoolingLayer();

			byte[][][][] y_mp2 = mp2.compute(false,sndChannel,rcvChannel,mt2,y_bn6);

			System.out.println("I am here mp2");
			byte[] show_mp2 = new byte[y_mp2[0][0][0].length];
			for(int i = 0; i < y_mp2[0][0][0].length; i++){
				show_mp2[i] = BooleanUtil.xor(y_mp2[0][0][0][i],y_mp2[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_mp2));


			convolutionLayer conv7 = new convolutionLayer();

			short[][][][] y_conv7 = conv7.compute(false,sndChannel,rcvChannel,mt_15,y_mp2,rd_w.get_weight_conv("features.20.weight"));

			System.out.println("I am here conv7");
			short[] show_conv7 = new short[y_conv7[0][0][0].length];
			for(int i = 0; i < y_conv7[0][0][0].length; i++){
				show_conv7[i] = fifteenBitAdditiveUtil.add(y_conv7[0][0][0][i],y_conv7[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_conv7));

			batchNormolizationBinaryActivationLayer bn7 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn7 = bn7.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv7,rd_w.get_ep_output("features.21"),rd_w.get_zeta_output("features.21"));

			System.out.println("I am here bn7");
			byte[] show_bn7 = new byte[y_bn7[0][0][0].length];
			for(int i = 0; i < y_bn7[0][0][0].length; i++){
				show_bn7[i] = BooleanUtil.xor(y_bn7[0][0][0][i],y_bn7[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_bn7));

			convolutionLayer conv8 = new convolutionLayer();

			short[][][][] y_conv8 = conv8.compute(false,sndChannel,rcvChannel,mt_15,y_bn7,rd_w.get_weight_conv("features.23.weight"));

			System.out.println("I am here conv8");
			short[] show_conv8 = new short[y_conv8[0][0][0].length];
			for(int i = 0; i < y_conv8[0][0][0].length; i++){
				show_conv8[i] = fifteenBitAdditiveUtil.add(y_conv8[0][0][0][i],y_conv8[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_conv8));

			batchNormolizationBinaryActivationLayer bn8 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn8 = bn8.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv8,rd_w.get_ep_output("features.24"),rd_w.get_zeta_output("features.24"));

			System.out.println("I am here bn8");
			byte[] show_bn8 = new byte[y_bn8[0][0][0].length];
			for(int i = 0; i < y_bn8[0][0][0].length; i++){
				show_bn8[i] = BooleanUtil.xor(y_bn8[0][0][0][i],y_bn8[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_bn8));

			convolutionLayer conv9 = new convolutionLayer();

			short[][][][] y_conv9 = conv9.compute(false,sndChannel,rcvChannel,mt_15,y_bn8,rd_w.get_weight_conv("features.26.weight"));

			System.out.println("I am here conv9");
			short[] show_conv9 = new short[y_conv9[0][0][0].length];
			for(int i = 0; i < y_conv9[0][0][0].length; i++){
				show_conv9[i] = fifteenBitAdditiveUtil.add(y_conv9[0][0][0][i],y_conv9[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_conv9));

			batchNormolizationBinaryActivationLayer bn9 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn9 = bn9.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv9,rd_w.get_ep_output("features.27"),rd_w.get_zeta_output("features.27"));

			System.out.println("I am here bn9");
			byte[] show_bn9 = new byte[y_bn9[0][0][0].length];
			for(int i = 0; i < y_bn9[0][0][0].length; i++){
				show_bn9[i] = BooleanUtil.xor(y_bn9[0][0][0][i],y_bn9[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_bn9));

			maxPoolingLayer mp3 = new maxPoolingLayer();

			byte[][][][] y_mp3 = mp3.compute(false,sndChannel,rcvChannel,mt2,y_bn9);

			System.out.println("I am here mp3");
			byte[] show_mp3 = new byte[y_mp3[0][0][0].length];
			for(int i = 0; i < y_mp3[0][0][0].length; i++){
				show_mp3[i] = BooleanUtil.xor(y_mp3[0][0][0][i],y_mp3[1][0][0][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_mp3));


			flattenLayer fl = new flattenLayer();

			byte[][] y_fl = fl.compute_1_flatten(false,sndChannel,rcvChannel,mt2,y_mp3);

			System.out.println("I am here flatten");
			byte[] show_fl = new byte[y_fl[0].length];
			for(int i = 0; i < y_fl[0].length; i++){
				show_fl[i] = BooleanUtil.xor(y_fl[0][i],y_fl[1][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_fl));

			fullConnectedLayer fc1 = new fullConnectedLayer();

			short[][] y_fc1 = fc1.compute(false,sndChannel,rcvChannel,mt_15,y_fl,rd_w.get_weight_fc("classifier.0.weight"));


			System.out.println("I am here fc1");
			short[] show_fc1 = new short[y_fc1[0].length];
			for(int i = 0; i < y_fc1[0].length; i++){
				show_fc1[i] = eightBitAdditiveUtil.add(y_fc1[0][i],y_fc1[1][i]);
			}
			System.out.println("first line:" + Arrays.toString(show_fl));
			//System.out.println("I am here flatten layer");

//			short[][] y_fc2 = {
//					{1,2,3,4,5,6,7,8,9,10},
//					{1,2,3,4,5,6,7,8,9,10}
//			};

			outputLayer outlayer = new outputLayer();

			long[][] y_outlayer = outlayer.compute_short_fc(false,sndChannel,rcvChannel,mt,y_fc1,rd_w.get_ep1_output("classifier.1"),rd_w.get_ep2_output("classifier.1"),1000);

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

