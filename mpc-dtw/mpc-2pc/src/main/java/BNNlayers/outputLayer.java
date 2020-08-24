package BNNlayers;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import eightBitAdditive.eightBitShareGenerator;
import eightBitAdditive.eightBitAdditiveUtil;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBNN.SBN2PCOutput;
import gadgetBNN.SecMaxPooling;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class outputLayer {

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
	public double timePure = 0.0;
	public double timeAll = 0.0;
	public double timeNetwork = 0.0;
	public double timeSharing = 0.0;

	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0 };

	public long[][] compute_short_fc(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple mt,
									  short[][] x,  float[] ep1_float, float[] ep2_float, int factor) throws Exception {

		//BN:
		//input:
		//x[shares][channel][length][width]
		//output:
		//y[shares][channel][length][width]
		int channel = x[0].length;
		//int length = x[0][0].length;
		//int width = x[0][0][0].length;
		long[][] out_y = new long[2][channel];


		ShareGenerator generator = new ShareGenerator(true);

		SBN2PCOutput bn = new SBN2PCOutput();

		for(int i = 0; i < channel; i++){

			// generate two shares
			long ep1 = (long) (ep1_float[i]*factor);
			long ep2 = (long) (ep2_float[i]*factor);
			
			
			double st0 = System.nanoTime();
			generator.generateSharedDataPoint(ep1, true);
			long ep1_0 = generator.x0;
			long ep1_1 = generator.x1;

			//System.out.println("ep1:" + ep1 + " ep1_0:" + ep1_0 + " ep1_1:" + ep1_1 + " verify:" + AdditiveUtil.add(ep1_0,ep1_1 ));

			generator.generateSharedDataPoint(ep2, true);
			long ep2_0 = generator.x0;
			long ep2_1 = generator.x1;
			double et0 = System.nanoTime();
			timeSharing += et0-st0;

			//System.out.println("ep2:" + ep2 + " ep2_0:" + ep2_0 + " ep2_1:" + ep2_1 + " verify:" + AdditiveUtil.add(ep2_0, ep2_1));


			//System.out.println("x:" + x + " x0:" + x[0][i] + " x_1:" + x[1][i] + " verify:" + AdditiveUtil.add(x[0][i], x[1][i]));

			double st = System.nanoTime();
			long[] z = bn.compute(false, sndChannel, rcvChannel, mt, x[0][i],ep1_0,ep2_0,x[1][i],ep1_1,ep2_1,factor);
			double et = System.nanoTime();
			timeAll += et-st;
			
			out_y[0][i] = z[0];
			out_y[1][i] = z[1];
		}
		
		this.timeNetwork = bn.timeNetwork;
		this.timePure = this.timeAll  - this.timeNetwork;
		this.bandwidth = bn.bandwidth;

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

		outputLayer secmp = new outputLayer();// y, u
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
