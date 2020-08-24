package BNNlayers;

import additive.AdditiveUtil;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import eightBitAdditive.eightBitShareGenerator;
import eightBitAdditive.eightBitAdditiveUtil;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBNN.SecMaxPooling;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class batchNormolizationLayer {

	//input:
	public short[][][] x;

	//output:
	public byte[][][] y;
	//parameter:
	public int stride = 2;

	private Server sndChannel;
	private Client rcvChannel;
	public byte z0AND;
	public byte z1AND;

	public byte a0 = 0;
	public byte a1 = 0;

	public double bandwidth = 0.0;

	public long timeNetwork = 0;

	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0 };

	public long[][] compute_long_fc(long[][] x, float[] w, float[] b) throws Exception {

		//BN:
		//input:
		//x[shares][channel][length][width]
		//output:
		//y[shares][channel][length][width]
		int channel = x[0].length;
		//int length = x[0][0].length;
		//int width = x[0][0][0].length;

		long[] plaint_y = new long[channel];
		double[] temp_plaint_y = new double[channel];
		long[][] out_y = new long[2][channel];

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ShareGenerator ariGen = new ShareGenerator(true);
		for (int i = 0; i < channel; i++) {
			//recover to plaintext
			plaint_y[i] = AdditiveUtil.add(x[0][i], x[1][i]);

			//BN
			temp_plaint_y[i] =  (eightBitAdditiveUtil.add(((short) (plaint_y[i])), (short) 128) - 128) * w[i] + b[i];
			//generate shares
			for (byte j = 0; j < temp_plaint_y.length; j++) {
				if(temp_plaint_y[i] < 0){
					ariGen.generateSharedDataPoint( -1,true);
				}
				else{
					ariGen.generateSharedDataPoint( 1,true);
				}
				//ariGen.generateSharedDataPoint(temp_plaint_y[j], true);
				long xi_0 = ariGen.x0;
				long xi_1 = ariGen.x1;
				//System.out.println("i" + i + "  xi:" + x[i] + "  xi_0:" + xi_0 + "  xi_1:" + xi_1);
				out_y[0][i] = xi_0;
				out_y[1][i] = xi_1;
			}

		}
		return out_y;
	}

	public short[][] compute_short_fc(short[][] x, float[] w, float[] b) throws Exception {

		//BN:
		//input:
		//x[shares][channel][length][width]
		//output:
		//y[shares][channel][length][width]
		int channel = x[0].length;
		//int length = x[0][0].length;
		//int width = x[0][0][0].length;

		short[] plaint_y = new short[channel];
		double[] temp_plaint_y = new double[channel];
		short[][] out_y = new short[2][channel];

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		eightBitShareGenerator ariGen = new eightBitShareGenerator(true);
		for (int i = 0; i < channel; i++) {
			//recover to plaintext
			plaint_y[i] = eightBitAdditiveUtil.add(x[0][i], x[1][i]);
			//BN
			System.out.println("before sign:" + plaint_y[i]);
			System.out.println("before BA:" + (eightBitAdditiveUtil.add((plaint_y[i]), (short) 128) - 128));
			System.out.println("after BA:" + ((eightBitAdditiveUtil.add((plaint_y[i]), (short) 128) - 128) * w[i] + b[i]));
			temp_plaint_y[i] = (eightBitAdditiveUtil.add((plaint_y[i]), (short) 128) - 128) * w[i] + b[i];
			System.out.println("after short:" + temp_plaint_y[i]);
			//temp_plaint_y[i] = (short) (plaint_y[i] * w[i] + b[i]);
			//generate shares
			for (byte j = 0; j < temp_plaint_y.length; j++) {
				if(temp_plaint_y[i] < 0){
					ariGen.generateSharedDataPoint((short) 255,true);
				}
				else{
					ariGen.generateSharedDataPoint((short) 1,true);
				}
//				ariGen.generateSharedDataPoint(temp_plaint_y[j], true);
				short xi_0 = ariGen.x0;
				short xi_1 = ariGen.x1;
				//System.out.println("i" + i + "  xi:" + x[i] + "  xi_0:" + xi_0 + "  xi_1:" + xi_1);
				out_y[0][i] = xi_0;
				out_y[1][i] = xi_1;
			}

		}
		return out_y;
	}

	public long[][][][] compute_long_conv(long[][][][] x, float[] w, float[] b) throws Exception {

		//BN:
		//input:
		//x[shares][channel][length][width]
		//output:
		//y[shares][channel][length][width]
		int channel = x[0].length;
		int length = x[0][0].length;
		int width = x[0][0][0].length;

		long[][][] plaint_y = new long[channel][length][width];
		double[][][] temp_plaint_y = new double[channel][length][width];
		long[][][][] out_y = new long[2][channel][length][width];

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ShareGenerator ariGen = new ShareGenerator(true);
		for(int i = 0; i < channel; i++) {
			for (int j = 0; j < length; j++) {
				for (int k = 0; k < width; k++) {
					//recover to plaintext
					plaint_y[i][j][k] = AdditiveUtil.add(x[0][i][j][k], x[1][i][j][k]);
					//BN
					//temp_plaint_y[i][j][k] = (long) ((eightBitAdditiveUtil.add(((short) (plaint_y[i][j][k])), (short) 128) - 128) * w[i] + b[i]);
					temp_plaint_y[i][j][k] = plaint_y[i][j][k] * w[i] + b[i];
					System.out.println("float:  " + (plaint_y[i][j][k] * w[i] + b[i]) + "long:  " + temp_plaint_y[i][j][k]);
					//generate shares
					for (int i_share = 0; i_share < temp_plaint_y.length; i_share++) {
						if(temp_plaint_y[i][j][k] < 0){
							ariGen.generateSharedDataPoint( -1,true);
						}
						else{
							ariGen.generateSharedDataPoint( 1,true);
						}
//				ariGen.generateSharedDataPoint(temp_plaint_y[j], true);
//						short xi_0 = boolGen.x0;
//						short xi_1 = boolGen.x1;
//						ariGen.generateSharedDataPoint(temp_plaint_y[i][j][k], true);
						long xi_0 = ariGen.x0;
						long xi_1 = ariGen.x1;
						//System.out.println("i" + i + "  xi:" + x[i] + "  xi_0:" + xi_0 + "  xi_1:" + xi_1);
						out_y[0][i][j][k] = xi_0;
						out_y[1][i][j][k] = xi_1;
					}
				}
			}
		}
			return out_y;
		}

	public short[][][][] compute_short_conv(short[][][][] x, float[] w, float[] b) throws Exception {

		//BN:
		//input:
		//x[shares][channel][length][width]
		//output:
		//y[shares][channel][length][width]
		int channel = x[0].length;
		int length = x[0][0].length;
		int width = x[0][0][0].length;

		short[][][] plaint_y = new short[channel][length][width];
		double[][][] temp_plaint_y = new double[channel][length][width];
		short[][][][] out_y = new short[2][channel][length][width];

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		eightBitShareGenerator ariGen = new eightBitShareGenerator(true);
		for(int i = 0; i < channel; i++) {
			for (int j = 0; j < length; j++) {
				for (int k = 0; k < width; k++) {
					//recover to plaintext
					plaint_y[i][j][k] = eightBitAdditiveUtil.add(x[0][i][j][k], x[1][i][j][k]);
					//BN
					temp_plaint_y[i][j][k] = (eightBitAdditiveUtil.add((plaint_y[i][j][k]), (short) 128) - 128) * w[i] + b[i];
//					temp_plaint_y[i][j][k] = (short) (plaint_y[i][j][k] * w[i] + b[i]);
					//generate shares
					for (int i_share = 0; i_share < temp_plaint_y.length; i_share++) {
						if(temp_plaint_y[i][j][k] < 0){
							ariGen.generateSharedDataPoint((short) 255,true);
						}
						else{
							ariGen.generateSharedDataPoint((short) 1,true);
						}
						//ariGen.generateSharedDataPoint(temp_plaint_y[i][j][k], true);
						short xi_0 = ariGen.x0;
						short xi_1 = ariGen.x1;
						//System.out.println("i" + i + "  xi:" + x[i] + "  xi_0:" + xi_0 + "  xi_1:" + xi_1);
						out_y[0][i][j][k] = xi_0;
						out_y[1][i][j][k] = xi_1;
					}
				}
			}
		}
		return out_y;
	}

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

		// ArrayList<MultiplicationTriple> mts = new ArrayList<MultiplicationTriple>();

		/*
		 * for (int i = 0; i < 4; i++) { MultiplicationTriple mt = new
		 * MultiplicationTriple(sndChannel, rcvChannel); mts.add(mt); }
		 */

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ANDTriple mt2 = new ANDTriple(boolGen);
		System.out.println("mt2 A0:" + mt2.tripleA0 + " A1:" + mt2.tripleA1 + " B0:" + mt2.tripleB0 + " B1:"
				+ mt2.tripleB1 + " C0:" + mt2.tripleC0 + " C1:" + mt2.tripleC1);

		//MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		//test case in XONN
		byte[][][] x = {
				{{(byte)1, (byte)1, (byte)0, (byte)0},
				 {(byte)1, (byte)1, (byte)1, (byte)0},
				 {(byte)0, (byte)0, (byte)0, (byte)0},
				 {(byte)0, (byte)0, (byte)0, (byte)0}
				},
				{{(byte)1, (byte)1, (byte)0, (byte)0},
				 {(byte)1, (byte)1, (byte)0, (byte)0},
				 {(byte)1, (byte)1, (byte)1, (byte)0},
				 {(byte)1, (byte)1, (byte)0, (byte)0}
				},
		};

		batchNormolizationLayer secmp = new batchNormolizationLayer();// y, u
		int round = 1;
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {			
			//extract MSB of a
			//byte[][][] z = secmp.compute(false, sndChannel, rcvChannel, mt2,  x);
			// verify
			//System.out.println("z:" + Arrays.toString(z));

		}
		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + secmp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		System.out.println("timeNetwork:" + secmp.timeNetwork / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

	
}
