package gadgetBNN;

import additive.ShareGenerator;
import booleanShr.BooleanShrGenerator;
import eightBitAdditive.*;
import eightBitAdditive.eightBitAdditiveUtil;
import eightBitAdditive.eightBitShareGenerator;
import flexSC.flexsc.Flag;
import flexSC.gc.GCSignal;
import flexSC.network.Client;
import flexSC.network.Network;
import flexSC.network.Server;
import flexSC.ot.OTExtReceiver;
import flexSC.ot.OTExtSender;
import flexSC.ot.OTReceiver;
import flexSC.ot.OTSender;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SecIntegerBinaryVDPBatchFifteenBit {

	// set OT channel based on FlexSC
	private OTSender snd;
	private OTReceiver rcv;

	public double bandwidth = 0.0;
	public double timeNetwork = 0;

	public int len_n;

	public short U0, U1;
	public short V0, V1;

	public short z0, z1;

	private short[] x_0_arr, x_1_arr;
	private byte[] w_0_arr, w_1_arr;

	private ShareGenerator shrGen;
	private SecureRandom random;

	public SecIntegerBinaryVDPBatchFifteenBit() {
		this.shrGen = new ShareGenerator(true);
		this.random = shrGen.random;
	}

	// boolean shares of binary vector <w0,w1,w2,..,w_n-1>: w_0_arr, w_1_arr
	// arithmetic shares of integer vector <a0, a1, .., a_n-1>: a_0_arr, a_1_arr
	public short[] compute(Server sndChannel, Client rcvChannel, short[] x_0_arr, byte[] w_0_arr, short[] x_1_arr,
			byte[] w_1_arr) throws Exception {
		len_n = x_0_arr.length;

		this.x_0_arr = x_0_arr;
		this.w_0_arr = w_0_arr;
		this.x_1_arr = x_1_arr;
		this.w_1_arr = w_1_arr;

		short[] z = new short[2];

		// COT-f (x_0) multi (w_0 xor w_1)
		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {

				double s = System.nanoTime();
				setSnd(sndChannel);// P0 acts as a sender
				double e = System.nanoTime();
				System.out.println("initialize OT time (ms):"+(e-s)/1e6);
				
				double s1 = System.nanoTime();
				generateU0();// multi(x_0, w) U0=r
				double e1 = System.nanoTime();
				System.out.println("generateU0 time (ms):"+(e1-s1)/1e6);
				
				sndChannel.flush();

				setRcv(sndChannel);// P0 acts as a receiver
				generateV0();
				sndChannel.flush();

			}
		});

		exec.execute(new Runnable() {

			@Override
			public void run() {

				setRcv(rcvChannel);// P1 acts as a receiver
				generateU1();// multi(x_0, w) U1+= si0 or si1
				rcvChannel.flush();

				setSnd(rcvChannel);// P1 acts as a sender
				generateV1();
				rcvChannel.flush();

			}

		});

		// Create OT instance
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

		generateZ();
		z[0] = this.z0;
		z[1] = this.z1;
		return z;
	}

	// z0+z1 should be Sigma(w*x)
	private void generateZ() {
		// z0 = U0 + V0
		this.z0 = fifteenBitAdditiveUtil.add(this.U0, this.V0);
		//this.z0 = (short) (this.U0 + this.V0);
		// z1 = U1+V1
		this.z1 = fifteenBitAdditiveUtil.add(this.U1, this.V1);
		//this.z1 = (short) (this.U1 + this.V1);

	}

	/**
	 * Initialize OT sender - 32 bits label
	 * 
	 * @param sndChannel
	 */
	private void setSnd(Network sndChannel) {
		if (sndChannel != null) {
			//System.out.println("Initialize OTExtSender");
			snd = new OTExtSender(32, sndChannel);
		}
	}

	/**
	 * Initialize OT receiver
	 * 
	 * @param rcvChannel
	 */
	private void setRcv(Network rcvChannel) {
		if (rcvChannel != null) {
			//System.out.println("Initialize OTExtReceiver");
			rcv = new OTExtReceiver(rcvChannel);
		}
	}

	private GCSignal[] generatePairs(short si0, short si1) {
		GCSignal[] label = new GCSignal[2];

		label[0] = GCSignal.newInstance(BigInteger.valueOf(si0).toByteArray());

		label[1] = GCSignal.newInstance(BigInteger.valueOf(si1).toByteArray());
		return label;
	}

	/**
	 * Generate <U>_0, <U> = [w_arr] * <x_arr>_0
	 */
	private void generateU0() {
		// send A0
		this.U0 = this.generateSenderShare(this.x_0_arr, this.w_0_arr);
	}

	private short generateSenderShare(short[] x_01_arr, byte[] w_01_arr) {
		short U0 = 0;

		GCSignal[][] pair = new GCSignal[len_n][2];
		for (int i = 0; i < len_n; i++) {

			// generate a fresh randomness each iteration
			short r = (short) random.nextInt(256);

			// generate si0, si1 where 0/1 is the choice bit
			//short si0 = (short) (r + fifteenBitAdditiveUtil.mul(w_01_arr[i], x_01_arr[i]));
			//short si1 = (short) (r + fifteenBitAdditiveUtil.mul((short) (1 - w_01_arr[i]), x_01_arr[i]));
			short si0 = fifteenBitAdditiveUtil.add(r, fifteenBitAdditiveUtil.mul(w_01_arr[i], x_01_arr[i]));
			short si1 = fifteenBitAdditiveUtil.add(r, fifteenBitAdditiveUtil.mul((short) (1 - w_01_arr[i]), x_01_arr[i]));

			// prepare GC labels for the generated messages
			pair[i] = generatePairs(si0, si1);// si0, si1

			// U0 = Sigma{-r}
			U0 = fifteenBitAdditiveUtil.add(U0, fifteenBitAdditiveUtil.modAdditive(-r));
			//U0 = fifteenBitAdditiveUtil.add(U0, fifteenBitAdditiveUtil.modAdditive(-r));
		}

		try {
			snd.send(pair);

		} catch (IOException e) {
			e.printStackTrace();
		}

		// U0 = U0 (mod Z_2^l )
		U0 = fifteenBitAdditiveUtil.modAdditive(U0);

		return U0;
	}

	/**
	 * Generate <U>_1, <U> = [w_arr] * <x_arr>_0
	 */
	private void generateU1() {
		// receive w1
		this.U1 = this.generateReceiverShare(this.w_1_arr);
	}

	// selection bit b=[w]_1 or 0
	private short generateReceiverShare(byte[] w_10_arr) {
		short U1 = 0;
		boolean[] inputB1 = new boolean[len_n];
		for (int i = 0; i < len_n; i++) {
			inputB1[i] = (w_10_arr[i] == 0) ? false : true;
		}
		try {
			GCSignal[] res = rcv.receive(inputB1);// select bits between si0, si1 based on value B1

			for (int i = 0; i < len_n; i++) {
				//U1 = (short) (U1+ new BigInteger(res[i].bytes).shortValue());
				U1 = fifteenBitAdditiveUtil.add(U1, fifteenBitAdditiveUtil.modAdditive(new BigInteger(res[i].bytes).shortValue()));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		//U1 = U1;
		U1 = fifteenBitAdditiveUtil.modAdditive(U1);

		return U1;
	}

	/**
	 * Generate <V>_1, <V> = <x>_1 * w; P1 acts as the sender
	 */
	private void generateV1() {
		// send x1
		this.V1 = this.generateSenderShare(this.x_1_arr, this.w_1_arr);// P1
	}

	/**
	 * Generate <V>_0, <V> = <x>_1 * w; w_0 is the choice bit; P0 acts as the
	 * receiver
	 */
	private void generateV0() {
		// choose msg based on w_0
		this.V0 = this.generateReceiverShare(this.w_0_arr);
	}

	public static void main(String[] args) {

		final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		final int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
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

		// test case
		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		fifteenBitShareGenerator ariGen = new fifteenBitShareGenerator(true);

		short[] x = { 1, 2, 3, 4, 5, 6, 7, 8, 9,10, 1, 2, 3, 4, 5, 6, 7, 8, 9,10 };
		//short[] x = { 255, 255, 255, 255, 255, 255, 255, 255, 255,255, 255, 255, 255, 255, 255, 255, 255, 255, 255,255 };// 123456789
		byte[] w = { (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte)1,(byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte)1 };// 45

		short[] x_0_arr = new short[x.length];
		short[] x_1_arr = new short[x.length];

		byte[] w_0_arr = new byte[w.length];
		byte[] w_1_arr = new byte[w.length];

		for (int i = 0; i < x.length; i++) {
			ariGen.generateSharedDataPoint(x[i], true);
			short xi_0 = ariGen.x0;
			short xi_1 = ariGen.x1;
			x_0_arr[i] = xi_0;
			x_1_arr[i] = xi_1;

			boolGen.generateSharedDataPoint(w[i], true);
			byte wi_0 = boolGen.x0;
			byte wi_1 = boolGen.x1;
			w_0_arr[i] = wi_0;
			w_1_arr[i] = wi_1;

			System.out.println("i:" + i + " xi_0:" + xi_0 + " xi_1:" + xi_1 + " wi_0:" + wi_0 + " wi_1:" + wi_1);
		}

		// MNIST counter=784*128/10
		int counter = 1;

		System.out.println("Starting Integer Binary VDP COT...");

		double elapsedTimeTotal = 0.0;
		Flag.sw.startTotal();
		double s = System.nanoTime();
		SecIntegerBinaryVDPBatchFifteenBit ibvdp = new SecIntegerBinaryVDPBatchFifteenBit();
		try {
			for (int i = 0; i < counter; i++) {
				ibvdp.compute(sndChannel, rcvChannel, x_0_arr, w_0_arr, x_1_arr, w_1_arr);
				
				if (i % 1000 == 0) {
					System.out.println("Progress:" + i);
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		double e = System.nanoTime();
		elapsedTimeTotal += e - s;

		
		// Shutdown channel in test program
		

		// verify
		System.out.println("z:" + (ibvdp.z0 + ibvdp.z1) + " z0:" + ibvdp.z0 + " z1:" + ibvdp.z1);

		Flag.sw.stopTotal();
		// double e = System.nanoTime();
		System.out.println("Gen running time(second):" + elapsedTimeTotal / 1e9);
		System.out.println("Gen running time(mu second):" + elapsedTimeTotal / 1e3);
		rcvChannel.disconnect();
		sndChannel.disconnect();
		if (Flag.CountTime)
			Flag.sw.print();
		if (Flag.countIO)
			rcvChannel.printStatistic();

	}

}
