package BNNlayers;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import eightBitAdditive.eightBitMultiplicationTriple;
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

public class bNNInferenceEightBit_BC3 {

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
			rd_w.modelfileName = "./resources/BC3_no_bias_no_preprocess.json";
//			byte[][] a = rd_w.get_weight_fc("fc1.weight");
			//System.out.println("I am here input");

			inputConvolutionLayer conv1 = new inputConvolutionLayer();

			long[][][][] y_conv1 = conv1.compute_conv(false, sndChannel, rcvChannel, mt, x_fc1, rd_w.get_weight_conv("features.0.weight"));

			//System.out.println("I am here conv1");

			batchNormolizationBinaryActivationLayer bn1 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn1 = bn1.compute_long_conv(false,sndChannel,rcvChannel,mt2,mt,y_conv1,rd_w.get_ep_long_output("features.1"),rd_w.get_zeta_output("features.1"));

			//System.out.println("I am here bn1");

			convolutionLayer conv2 = new convolutionLayer();

			short[][][][] y_conv2 = conv2.compute(false,sndChannel,rcvChannel,mt_15,y_bn1,rd_w.get_weight_conv("features.3.weight"));

			batchNormolizationBinaryActivationLayer bn2 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn2 = bn2.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv2,rd_w.get_ep_output("features.4"),rd_w.get_zeta_output("features.4"));

			convolutionLayer conv3 = new convolutionLayer();

			short[][][][] y_conv3 = conv3.compute(false,sndChannel,rcvChannel,mt_15,y_bn2,rd_w.get_weight_conv("features.6.weight"));

			batchNormolizationBinaryActivationLayer bn3 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn3 = bn3.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv3,rd_w.get_ep_output("features.7"),rd_w.get_zeta_output("features.7"));


			maxPoolingLayer mp1 = new maxPoolingLayer();

			byte[][][][] y_mp1 = mp1.compute(false,sndChannel,rcvChannel,mt2,y_bn3);

			//System.out.println("I am here mp1");

			convolutionLayer conv4 = new convolutionLayer();

			short[][][][] y_conv4 = conv4.compute(false,sndChannel,rcvChannel,mt_15,y_mp1,rd_w.get_weight_conv("features.10.weight"));

			//System.out.println("I am here conv2");

			batchNormolizationBinaryActivationLayer bn4 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn4 = bn4.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv4,rd_w.get_ep_output("features.11"),rd_w.get_zeta_output("features.11"));

			convolutionLayer conv5 = new convolutionLayer();

			short[][][][] y_conv5 = conv5.compute(false,sndChannel,rcvChannel,mt_15,y_bn4,rd_w.get_weight_conv("features.13.weight"));

			batchNormolizationBinaryActivationLayer bn5 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn5 = bn5.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv5,rd_w.get_ep_output("features.14"),rd_w.get_zeta_output("features.14"));

			convolutionLayer conv6 = new convolutionLayer();

			short[][][][] y_conv6 = conv6.compute(false,sndChannel,rcvChannel,mt_15,y_bn5,rd_w.get_weight_conv("features.16.weight"));

			batchNormolizationBinaryActivationLayer bn6 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn6 = bn6.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv6,rd_w.get_ep_output("features.17"),rd_w.get_zeta_output("features.17"));

			//System.out.println("I am here bn2");

			maxPoolingLayer mp2 = new maxPoolingLayer();

			byte[][][][] y_mp2 = mp2.compute(false,sndChannel,rcvChannel,mt2,y_bn6);

			//System.out.println("I am here mp2");

			convolutionLayer conv7 = new convolutionLayer();

			short[][][][] y_conv7 = conv7.compute(false,sndChannel,rcvChannel,mt_15,y_mp2,rd_w.get_weight_conv("features.20.weight"));

			batchNormolizationBinaryActivationLayer bn7 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn7 = bn7.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv7,rd_w.get_ep_output("features.21"),rd_w.get_zeta_output("features.21"));

			convolutionLayer conv8 = new convolutionLayer();

			short[][][][] y_conv8 = conv8.compute(false,sndChannel,rcvChannel,mt_15,y_bn7,rd_w.get_weight_conv("features.23.weight"));

			batchNormolizationBinaryActivationLayer bn8 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn8 = bn8.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv8,rd_w.get_ep_output("features.24"),rd_w.get_zeta_output("features.24"));

			convolutionLayer conv9 = new convolutionLayer();

			short[][][][] y_conv9 = conv9.compute(false,sndChannel,rcvChannel,mt_15,y_bn8,rd_w.get_weight_conv("features.26.weight"));

			batchNormolizationBinaryActivationLayer bn9 = new batchNormolizationBinaryActivationLayer();

			byte[][][][] y_bn9 = bn9.compute_short_conv(false,sndChannel,rcvChannel,mt2,mt_15,y_conv9,rd_w.get_ep_output("features.27"),rd_w.get_zeta_output("features.27"));

			maxPoolingLayer mp3 = new maxPoolingLayer();

			byte[][][][] y_mp3 = mp3.compute(false,sndChannel,rcvChannel,mt2,y_bn9);

			flattenLayer fl = new flattenLayer();

			byte[][] y_fl = fl.compute(false,sndChannel,rcvChannel,mt2,y_mp3);

			fullConnectedLayer fc1 = new fullConnectedLayer();

			short[][] y_fc1 = fc1.compute(false,sndChannel,rcvChannel,mt_15,y_fl,rd_w.get_weight_fc("classifier.0.weight"));


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

