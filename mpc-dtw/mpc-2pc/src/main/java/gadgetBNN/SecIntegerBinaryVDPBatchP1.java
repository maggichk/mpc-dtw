package gadgetBNN;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import booleanShr.BooleanShrGenerator;
import common.parser.WriteFile;
import common.util.Config;
import common.util.Constants;
import flexSC.flexsc.Flag;
import flexSC.gc.GCSignal;
import flexSC.network.Client;
import flexSC.network.Network;
import flexSC.network.Server;
import flexSC.ot.OTExtReceiver;
import flexSC.ot.OTExtSender;
import flexSC.ot.OTReceiver;
import flexSC.ot.OTSender;
import flexSC.util.Utils;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

public class SecIntegerBinaryVDPBatchP1 {

	// set OT channel based on FlexSC
	private OTSender snd;
	private OTReceiver rcv;

	public double bandwidth = 0.0;
	public double timeNetwork = 0.0;
	public double timeOTInitial = 0.0;

	public int len_n;

	public long U0, U1;
	public long V0, V1;

	public long z0, z1;

	private long[] x_0_arr, x_1_arr;
	private byte[] w_0_arr, w_1_arr;

	private ShareGenerator shrGen;
	private SecureRandom random;

	public SecIntegerBinaryVDPBatchP1() {
		this.shrGen = new ShareGenerator(true);
		this.random = shrGen.random;
	}

	// boolean shares of binary vector <w0,w1,w2,..,w_n-1>: w_0_arr, w_1_arr
	// arithmetic shares of integer vector <a0, a1, .., a_n-1>: a_0_arr, a_1_arr
	public long computeP1Receiver(OTSender snd, OTReceiver rcv, long[] x_0_arr, byte[] w_0_arr, long[] x_1_arr,
			byte[] w_1_arr) throws Exception {
		this.snd = snd;
		this.rcv = rcv;
		
		len_n = x_0_arr.length;

		this.x_0_arr = x_0_arr;
		this.w_0_arr = w_0_arr;
		this.x_1_arr = x_1_arr;
		this.w_1_arr = w_1_arr;

		generateU1();// multi(x_0, w) U1+= si0 or si1

		return this.U1;
	}

	public long computeP1Sender(OTSender snd, OTReceiver rcv, long[] x_0_arr, byte[] w_0_arr, long[] x_1_arr,
			byte[] w_1_arr) throws Exception {
		this.snd = snd;
		this.rcv = rcv;
		
		
		len_n = x_0_arr.length;

		this.x_0_arr = x_0_arr;
		this.w_0_arr = w_0_arr;
		this.x_1_arr = x_1_arr;
		this.w_1_arr = w_1_arr;

		// P1 acts as a sender
		generateV1();

		return this.V1;
	}

	// z0+z1 should be Sigma(w*x)
	private void generateZ() {
		// z0 = U0 + V0
		this.z0 = AdditiveUtil.add(this.U0, this.V0);
		// z1 = U1+V1
		this.z1 = AdditiveUtil.add(this.U1, this.V1);

	}

	/**
	 * Initialize OT sender - 32 bits msg
	 * 
	 * @param sndChannel
	 */
	private void setSnd(Network sndChannel) {
		if (sndChannel != null) {
			// System.out.println("Initialize OTExtSender");
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
			// System.out.println("Initialize OTExtReceiver");
			rcv = new OTExtReceiver(rcvChannel);
		}
	}

	private GCSignal[] generatePairs(long si0, long si1) {
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

	private long generateSenderShare(long[] x_01_arr, byte[] w_01_arr) {
		long U0 = 0L;

		GCSignal[][] pair = new GCSignal[len_n][2];
		for (int i = 0; i < len_n; i++) {

			// generate a fresh randomness each iteration
			long r = random.nextInt(Integer.MAX_VALUE);

			// generate si0, si1 where 0/1 is the choice bit
			long si0 = AdditiveUtil.add(r, AdditiveUtil.mul(w_01_arr[i], x_01_arr[i]));
			long si1 = AdditiveUtil.add(r, AdditiveUtil.mul((1 - w_01_arr[i]), x_01_arr[i]));

			// prepare GC labels for the generated messages
			pair[i] = generatePairs(si0, si1);// si0, si1

			// U0 = Sigma{-r}
			U0 = AdditiveUtil.add(U0, AdditiveUtil.modAdditive(-r));
		}

		try {

			double s = System.nanoTime();
			snd.send(pair);
			double e = System.nanoTime();
			timeNetwork += (e - s) / 2;

		} catch (IOException e) {
			e.printStackTrace();
		}

		// U0 = U0 (mod Z_2^l )
		U0 = AdditiveUtil.modAdditive(U0);

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
	private long generateReceiverShare(byte[] w_10_arr) {
		long U1 = 0L;
		boolean[] inputB1 = new boolean[len_n];
		for (int i = 0; i < len_n; i++) {
			inputB1[i] = (w_10_arr[i] == 0) ? false : true;
		}
		try {

			double s = System.nanoTime();
			GCSignal[] res = rcv.receive(inputB1);// select bits between si0, si1 based on value B1
			double e = System.nanoTime();
			timeNetwork += (e - s) / 2;

			for (int i = 0; i < len_n; i++) {
				U1 = AdditiveUtil.add(U1, AdditiveUtil.modAdditive(new BigInteger(res[i].bytes).longValue()));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		U1 = AdditiveUtil.modAdditive(U1);

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

}
