package BNNlayers;

import additive.MultiplicationTriple;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import eightBitAdditive.fifteenBitMultiplicationTriple;
import eightBitAdditive.fifteenBitShareGenerator;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBNN.SecBinaryActivation2PC;
import gadgetBNN.SecBinaryActivation2PCEightBit;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class batchNormolizationBinaryActivationLayer {

	// input:
	public short[][][] x;

	// output:
	public byte[][][] y;
	// parameter:
	public int stride = 2;

	private Server sndChannel;
	private Client rcvChannel;
	public byte z0AND;
	public byte z1AND;

	public byte a0 = 0;
	public byte a1 = 0;

	public double bandwidth = 0.0;
	public double timeNetwork = 0.0;
	public double timePure = 0.0; // timeAll - timeNetwork ;
	public double timeAll = 0.0;
	public double timeSharing = 0.0;
	
	public int counter = 0;
	

	// output = sign(ep1) xnor sign(x+ ep2/ep1) = byte[zeta] xnor byte[MSB(x+ep)]
	// input: zeta byte
	// ep short(long)
	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0 };

	public byte[][] compute_long_fc(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			MultiplicationTriple mt, long[][] x, long[] ep, byte[] zeta) throws Exception {

		// BN:
		// input:
		// x[shares][channel][length][width]
		// output:
		// y[shares][channel][length][width]
		int channel = x[0].length;
		// int length = x[0][0].length;
		// int width = x[0][0][0].length;

		byte[][] out_y = new byte[2][channel];
		long[][] ep_share = new long[2][channel];
		byte[][] zeta_share = new byte[2][channel];

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ShareGenerator ariGen = new ShareGenerator(true);
		SecBinaryActivation2PC bn_ba = new SecBinaryActivation2PC();
		for (int i = 0; i < channel; i++) {

			
			double st = System.nanoTime();
			// generate share
			ariGen.generateSharedDataPoint(ep[i], true);
			long epi_0 = ariGen.x0;
			long epi_1 = ariGen.x1;
			// System.out.println("i" + i + " wi:" + w[i] + " wi_0:" + wi_0 + " wi_1:" +
			// wi_1);
			ep_share[0][i] = epi_0;
			ep_share[1][i] = epi_1;

			boolGen.generateSharedDataPoint(zeta[i], true);
			byte zetai_0 = boolGen.x0;
			byte zetai_1 = boolGen.x1;
			// System.out.println("i" + i + " wi:" + w[i] + " wi_0:" + wi_0 + " wi_1:" +
			// wi_1);
			zeta_share[0][i] = zetai_0;
			zeta_share[1][i] = zetai_1;
			double et = System.nanoTime();
			this.timeSharing += et-st;
			
			
			

			// compute
			double st1 = System.nanoTime();
			byte[] y = bn_ba.compute(false, sndChannel, rcvChannel, mt2, mt, x[0][i], x[1][i], ep_share[0][i],
					ep_share[1][i], zeta_share[0][i], zeta_share[1][i]);
			double et1 = System.nanoTime();
			timeAll += et1 -st1;
			
			
			
			out_y[0][i] = y[0];
			out_y[1][i] = y[1];
		}
		
		this.timeNetwork = bn_ba.timeNetwork;
		this.bandwidth = bn_ba.bandwidth;		
		this.timePure = this.timeAll - this.timeNetwork;
		
				
		return out_y;
	}

	public byte[][] compute_short_fc(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			fifteenBitMultiplicationTriple mt, short[][] x, short[] ep, byte[] zeta) throws Exception {

		// BN:
		// input:
		// x[shares][channel][length][width]
		// output:
		// y[shares][channel][length][width]
		int channel = x[0].length;
		// int length = x[0][0].length;
		// int width = x[0][0][0].length;

		byte[][] out_y = new byte[2][channel];
		short[][] ep_share = new short[2][channel];
		byte[][] zeta_share = new byte[2][channel];

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		fifteenBitShareGenerator ariGen = new fifteenBitShareGenerator(true);
		SecBinaryActivation2PCEightBit bn_ba = new SecBinaryActivation2PCEightBit();
		for (int i = 0; i < channel; i++) {

			
			double st = System.nanoTime();
			// generate share
			ariGen.generateSharedDataPoint(ep[i], true);
			short epi_0 = ariGen.x0;
			short epi_1 = ariGen.x1;
			// System.out.println("i" + i + " wi:" + w[i] + " wi_0:" + wi_0 + " wi_1:" +
			// wi_1);
			ep_share[0][i] = epi_0;
			ep_share[1][i] = epi_1;

			boolGen.generateSharedDataPoint(zeta[i], true);
			byte zetai_0 = boolGen.x0;
			byte zetai_1 = boolGen.x1;
			// System.out.println("i" + i + " wi:" + w[i] + " wi_0:" + wi_0 + " wi_1:" +
			// wi_1);
			zeta_share[0][i] = zetai_0;
			zeta_share[1][i] = zetai_1;
			double et = System.nanoTime();
			this.timeSharing += et-st;

			double st1 = System.nanoTime();
			byte[] y = bn_ba.compute(false, sndChannel, rcvChannel, mt2, mt, x[0][i], x[1][i], ep_share[0][i],
					ep_share[1][i], zeta_share[0][i], zeta_share[1][i]);
			double et1 = System.nanoTime();
			this.timeAll += et1 - st1;
			
			out_y[0][i] = y[0];
			out_y[1][i] = y[1];
		}
		
		this.timeNetwork = bn_ba.timeNetwork;
		this.bandwidth = bn_ba.bandwidth;		
		this.timePure = this.timeAll - this.timeNetwork;
		
		
		return out_y;
	}

	public byte[][][][] compute_long_conv(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			MultiplicationTriple mt, long[][][][] x, long[] ep, byte[] zeta) throws Exception {

		// BN:
		// input:
		// x[shares][channel][length][width]
		// output:
		// y[shares][channel][length][width]
		int channel = x[0].length;
		int length = x[0][0].length;
		int width = x[0][0][0].length;

		byte[][][][] out_y = new byte[2][channel][length][width];
		long[][] ep_share = new long[2][channel];
		byte[][] zeta_share = new byte[2][channel];

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ShareGenerator ariGen = new ShareGenerator(true);
		SecBinaryActivation2PC bn_ba = new SecBinaryActivation2PC();
		for (int i = 0; i < channel; i++) {
			
			
			double st = System.nanoTime();
			// generate share
			ariGen.generateSharedDataPoint(ep[i], true);
			long epi_0 = ariGen.x0;
			long epi_1 = ariGen.x1;
			// System.out.println("i" + i + " epi:" + ep[i] + " epi_0:" + epi_0 + " wi_1:" +
			// epi_1);
			ep_share[0][i] = epi_0;
			ep_share[1][i] = epi_1;

			boolGen.generateSharedDataPoint(zeta[i], true);
			byte zetai_0 = boolGen.x0;
			byte zetai_1 = boolGen.x1;
			// System.out.println("i" + i + " zetai:" + zeta[i] + " zetai_0:" + zetai_0 + "
			// wi_1:" + zetai_1);
			zeta_share[0][i] = zetai_0;
			zeta_share[1][i] = zetai_1;
			double et = System.nanoTime();
			timeSharing += et-st;

			for (int j = 0; j < length; j++) {
				for (int k = 0; k < width; k++) {

					double st1 = System.nanoTime();
					byte[] y = bn_ba.compute(false, sndChannel, rcvChannel, mt2, mt, x[0][i][j][k], x[1][i][j][k],
							ep_share[0][i], ep_share[1][i], zeta_share[0][i], zeta_share[1][i]);
					double et1 = System.nanoTime();
					this.timeAll += et1 - st1;
					
					out_y[0][i][j][k] = y[0];
					out_y[1][i][j][k] = y[1];
				}
			}
		}
		
		this.timeNetwork = bn_ba.timeNetwork;
		this.bandwidth = bn_ba.bandwidth;		
		this.timePure = this.timeAll - this.timeNetwork;
		
		
		return out_y;
	}

	public byte[][][][] compute_short_conv(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			fifteenBitMultiplicationTriple mt, short[][][][] x, short[] ep, byte[] zeta) throws Exception {

		// BN:
		// input:
		// x[shares][channel][length][width]
		// output:
		// y[shares][channel][length][width]
		int channel = x[0].length;
		int length = x[0][0].length;
		int width = x[0][0][0].length;

		byte[][][][] out_y = new byte[2][channel][length][width];
		short[][] ep_share = new short[2][channel];
		byte[][] zeta_share = new byte[2][channel];

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
//		fifteenBitMultiplicationTriple mt_15 = fifteenBitMultiplicationTriple()
		fifteenBitShareGenerator ariGen = new fifteenBitShareGenerator(true);
		SecBinaryActivation2PCEightBit bn_ba = new SecBinaryActivation2PCEightBit();
		for (int i = 0; i < channel; i++) {
			
			
			double st = System.nanoTime();
			// generate share
			ariGen.generateSharedDataPoint(ep[i], true);
			short epi_0 = ariGen.x0;
			short epi_1 = ariGen.x1;
			// System.out.println("i" + i + " wi:" + w[i] + " wi_0:" + wi_0 + " wi_1:" +
			// wi_1);
			ep_share[0][i] = epi_0;
			ep_share[1][i] = epi_1;

			boolGen.generateSharedDataPoint(zeta[i], true);
			byte zetai_0 = boolGen.x0;
			byte zetai_1 = boolGen.x1;
			// System.out.println("i" + i + " wi:" + w[i] + " wi_0:" + wi_0 + " wi_1:" +
			// wi_1);
			zeta_share[0][i] = zetai_0;
			zeta_share[1][i] = zetai_1;
			double et = System.nanoTime();
			this.timeSharing += et-st;
			
			for (int j = 0; j < length; j++) {
				for (int k = 0; k < width; k++) {

//					x[0][i][j][k] = fifteenBitAdditiveUtil.sub(fifteenBitAdditiveUtil.add(x[0][i][j][k] , (short) 128) , (short) 128);
//					x[1][i][j][k] = fifteenBitAdditiveUtil.sub(fifteenBitAdditiveUtil.add(x[1][i][j][k] , (short) 128) , (short) 128);
//					
					
					//compute
					double st1 = System.nanoTime();
					byte[] y = bn_ba.compute(false, sndChannel, rcvChannel, mt2, mt, x[0][i][j][k], x[1][i][j][k],
							ep_share[0][i], ep_share[1][i], zeta_share[0][i], zeta_share[1][i]);
					double et1 = System.nanoTime();
					this.timeAll += et1-st1;
					
					
					out_y[0][i][j][k] = y[0];
					out_y[1][i][j][k] = y[1];
				}
			}
		}
		
		this.timeNetwork = bn_ba.timeNetwork;
		this.bandwidth = bn_ba.bandwidth;		
		this.timePure = this.timeAll - this.timeNetwork;
		
		
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

		// MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		// test case in XONN
		byte[][][] x = {
				{ { (byte) 1, (byte) 1, (byte) 0, (byte) 0 }, { (byte) 1, (byte) 1, (byte) 1, (byte) 0 },
						{ (byte) 0, (byte) 0, (byte) 0, (byte) 0 }, { (byte) 0, (byte) 0, (byte) 0, (byte) 0 } },
				{ { (byte) 1, (byte) 1, (byte) 0, (byte) 0 }, { (byte) 1, (byte) 1, (byte) 0, (byte) 0 },
						{ (byte) 1, (byte) 1, (byte) 1, (byte) 0 }, { (byte) 1, (byte) 1, (byte) 0, (byte) 0 } }, };

		batchNormolizationBinaryActivationLayer secmp = new batchNormolizationBinaryActivationLayer();// y, u
		int round = 1;
		long time = 0;
		// double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {
			// extract MSB of a
			// byte[][][] z = secmp.compute(false, sndChannel, rcvChannel, mt2, x);
			// verify
			// System.out.println("z:" + Arrays.toString(z));

		}
		long s = System.nanoTime();
		time += s - e;
		// bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + secmp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		System.out.println("timeNetwork:" + secmp.timeNetwork / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

}
