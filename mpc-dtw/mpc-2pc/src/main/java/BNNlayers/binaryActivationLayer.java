package BNNlayers;

import booleanShr.*;
import eightBitAdditive.*;
import additive.*;
import flexSC.network.Client;
import flexSC.network.Server;
import flexSC.util.Utils;
import gadgetBNN.SecBinaryActivation2PC;
import gadgetBNN.SecBinaryActivation2PCEightBit;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class binaryActivationLayer {

	//input:
	//CONV
	// x = [shares][channel][length][width] short
	//FC
	// x = [shares][channel] short

	//output:
	//CONV
	// y = [shares][channel][length][width] binary
	//FC
	// y = [shares][channel] binary

	private Server sndChannel;
	private Client rcvChannel;
	public byte z0AND;
	public byte z1AND;

	public byte z0l = 0;
	public byte z1l = 0;

	public double bandwidth = 0.0;

	public long timeNetwork = 0;

	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0};

	public byte[][] compute_fc_short(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
						   eightBitMultiplicationTriple mt, short[][] x) throws Exception {

		byte[][] z = new byte[2][x[0].length];
		//long z0 = AdditiveUtil.sub(y0, x0);
		//long z1 = AdditiveUtil.sub(y1, x1);


		/*for(int j = 0; j < x[0].length; j++){
			SecBinaryActivation2PCEightBit sign = new SecBinaryActivation2PCEightBit();
			byte[] z_temp = sign.compute(false, sndChannel, rcvChannel, mt2, mt, x[0][j], x[1][j]);
			// verify
			z[0][j] = z_temp[0];
			z[1][j] = z_temp[1];
		}*/
		return z;
	}

	public byte[][] compute_fc_long(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
									 MultiplicationTriple mt, long[][] x) throws Exception {

		byte[][] z = new byte[2][x[0].length];
		//long z0 = AdditiveUtil.sub(y0, x0);
		//long z1 = AdditiveUtil.sub(y1, x1);


		/*for(int j = 0; j < x[0].length; j++){
			SecBinaryActivation2PC sign = new SecBinaryActivation2PC();
			byte[] z_temp = sign.compute(false, sndChannel, rcvChannel, mt2, mt, x[0][j], x[1][j]);
			// verify
			z[0][j] = z_temp[0];
			z[1][j] = z_temp[1];
		}*/
		return z;
	}

	public byte[][][][] compute_conv_short(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
									 eightBitMultiplicationTriple mt, short[][][][] x) throws Exception {

		byte[][][][] z = new byte[2][x[0].length][x[0][0].length][x[0][0][0].length];
		//long z0 = AdditiveUtil.sub(y0, x0);
		//long z1 = AdditiveUtil.sub(y1, x1);


	/*	for(int i = 0; i < x[0].length; i++){
			for(int j = 0; j < x[0][0].length; j++){
				for(int k = 0; k < x[0][0][0].length; k++){
					SecBinaryActivation2PCEightBit sign = new SecBinaryActivation2PCEightBit();
					byte[] z_temp = sign.compute(false, sndChannel, rcvChannel, mt2, mt, x[0][i][j][k], x[1][i][j][k]);
					// verify
					z[0][i][j][k] = z_temp[0];
					z[1][i][j][k] = z_temp[1];
					System.out.println("z:" + (z_temp[0]+z_temp[1]) + "z0:  " + z_temp[0] + "z1:  " + z_temp[1]);
				}
			}
		}*/

		return z;
	}

	public byte[][][][] compute_conv_long(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
										   MultiplicationTriple mt, long[][][][] x) throws Exception {

		byte[][][][] z = new byte[2][x[0].length][x[0][0].length][x[0][0][0].length];
		//long z0 = AdditiveUtil.sub(y0, x0);
		//long z1 = AdditiveUtil.sub(y1, x1);


		/*for(int i = 0; i < x[0].length; i++){
			for(int j = 0; j < x[0][0].length; j++){
				for(int k = 0; k < x[0][0][0].length; k++){
					SecBinaryActivation2PC sign = new SecBinaryActivation2PC();
					byte[] z_temp = sign.compute(false, sndChannel, rcvChannel, mt2, mt, x[0][i][j][k], x[1][i][j][k]);
					// verify
					z[0][i][j][k] = z_temp[0];
					z[1][i][j][k] = z_temp[1];
				}
			}
		}*/

		return z;
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
//		System.out.println("mt2 A0:" + mt2.tripleA0 + " A1:" + mt2.tripleA1 + " B0:" + mt2.tripleB0 + " B1:"
//				+ mt2.tripleB1 + " C0:" + mt2.tripleC0 + " C1:" + mt2.tripleC1);

		eightBitMultiplicationTriple mt = new eightBitMultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		eightBitShareGenerator generator = new eightBitShareGenerator(true);

		// generate two shares
		short a = -2;//sign(2)=1; sign(-2)=0.
		short b = 10;
		short[][][] x = {
				{
						{1, 1, -1},
						{1, 1, -1},
						{1, 1, -1},
				},
				{
						{1, 1, -1},
						{1, 1, -1},
						{1, 1, -1},
				},
		};

		short [][][][] x_shares = new short[2][x.length][x[0].length][x[0][0].length];
		for(int i = 0; i < x.length; i++){
			for(int j = 0; j < x[0].length; j++){
				for(int k = 0; k < x[0][0].length; k++){
					generator.generateSharedDataPoint(x[i][j][k], true);
					x_shares[0][i][j][k] = generator.x0;
					x_shares[1][i][j][k] = generator.x1;
				}
			}
		}
//
//
//
//		generator.generateSharedDataPoint(a, true);
//		short a0 = generator.x0;
//		short a1 = generator.x1;
//
//		System.out.println("a:" + a + " a0:" + a0 + " a1:" + a1 + " verify:" + eightBitAdditiveUtil.add(a0, a1));
//
//		generator.generateSharedDataPoint(b, true);
//		short b0 = generator.x0;
//		short b1 = generator.x1;
//
//		System.out.println("b:" + b + " b0:" + b0 + " b1:" + b1 + " verify:" + eightBitAdditiveUtil.add(b0, b1));

		binaryActivationLayer sign = new binaryActivationLayer();// y, u

		int round = 1;
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {			
			//extract MSB of a
			byte[][][][] z = sign.compute_conv_short(false, sndChannel, rcvChannel, mt2, mt, x_shares);
			// verify
//			byte z0 = z[0];
//			byte z1 = z[1];
//			System.out.println("z:" + eightBitAdditiveUtil.add(z0, z1) + " z0:" + z0 + " z1:" + z1);

			if (i % 1000 == 0) {
				System.out.println("Progress:" + i);
			}
		}
		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + sign.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		System.out.println("timeNetwork:" + sign.timeNetwork / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

}
