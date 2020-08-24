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

public class SecIntegerBinaryVDPBatchOT1 {

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

	public SecIntegerBinaryVDPBatchOT1() {
		this.shrGen = new ShareGenerator(true);
		this.random = shrGen.random;
	}

	public long[] compute(Server sndChannel, Client rcvChannel, long[] x_0_arr, byte[] w_0_arr, long[] x_1_arr,
			byte[] w_1_arr) throws Exception {
		len_n = x_0_arr.length;

		this.x_0_arr = x_0_arr;
		this.w_0_arr = w_0_arr;
		this.x_1_arr = x_1_arr;
		this.w_1_arr = w_1_arr;

		long[] z = new long[2];

		// COT-f (x_0) multi (w_0 xor w_1)
		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {
				// P0 acts as a sender
				double s = System.nanoTime();
				setSnd(sndChannel);// P0 acts as a sender
				double e = System.nanoTime();
				System.out.println("initialize OT time (s):" + (e - s) / 1e9);
				timeOTInitial += e - s;

				double s1 = System.nanoTime();
				sndChannel.flush();
				double e1 = System.nanoTime();
				timeNetwork += e1 - s1;

				SecIntegerBinaryVDPBatchP0 p0 = new SecIntegerBinaryVDPBatchP0();
				try {
					U0 = p0.computeP0Sender(snd, rcv, x_0_arr, w_0_arr, x_1_arr, w_1_arr);
				} catch (Exception e4) {
					// TODO Auto-generated catch block
					e4.printStackTrace();
				}

				// P0 acts as a receiver
				double s2 = System.nanoTime();
				// P0 acts as a receiver
				setRcv(sndChannel);
				double e2 = System.nanoTime();
				timeOTInitial += e2 - s2;

				try {
					V0 = p0.computeP0Receiver(snd, rcv, x_0_arr, w_0_arr, x_1_arr, w_1_arr);
				} catch (Exception e4) {
					// TODO Auto-generated catch block
					e4.printStackTrace();
				}

				double s3 = System.nanoTime();
				sndChannel.flush();
				double e3 = System.nanoTime();
				timeNetwork += e3 - s3;

			}
		});
		
		
		exec.execute(new Runnable() {

			@Override
			public void run() {

				setRcv(rcvChannel);// P1 acts as a receiver
				
				SecIntegerBinaryVDPBatchP1 p1 = new SecIntegerBinaryVDPBatchP1();
				try {
					U1 = p1.computeP1Receiver(snd, rcv, x_0_arr, w_0_arr, x_1_arr, w_1_arr);
				} catch (Exception e4) {
					// TODO Auto-generated catch block
					e4.printStackTrace();
				}
				
				rcvChannel.flush();

				setSnd(rcvChannel);// P1 acts as a sender
				try {
					V1 = p1.computeP1Sender(snd, rcv, x_0_arr, w_0_arr, x_1_arr, w_1_arr);
				} catch (Exception e4) {
					// TODO Auto-generated catch block
					e4.printStackTrace();
				}
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

		double st1 = System.nanoTime();
		this.bandwidth += rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		double et1 = System.nanoTime();
		timeNetwork += et1 - st1;

		return z;
		

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
		ShareGenerator ariGen = new ShareGenerator(true);

		long[] x = { 255, 255, 255, 255, 255, 255, 255, 255, 255 };// 123456789
		byte[] w = { (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1 };// 9*255

		long[] x_0_arr = new long[x.length];
		long[] x_1_arr = new long[x.length];

		byte[] w_0_arr = new byte[w.length];
		byte[] w_1_arr = new byte[w.length];

		for (int i = 0; i < x.length; i++) {
			ariGen.generateSharedDataPoint(x[i], true);
			long xi_0 = ariGen.x0;
			long xi_1 = ariGen.x1;
			x_0_arr[i] = xi_0;
			x_1_arr[i] = xi_1;

			boolGen.generateSharedDataPoint(w[i], true);
			byte wi_0 = boolGen.x0;
			byte wi_1 = boolGen.x1;
			w_0_arr[i] = wi_0;
			w_1_arr[i] = wi_1;

			// System.out.println("i:" + i + " xi_0:" + xi_0 + " xi_1:" + xi_1 + " wi_0:" +
			// wi_0 + " wi_1:" + wi_1);
		}

		// MNIST counter=784*128/10
		int counter = 1;

		System.out.println("Starting Integer Binary VDP COT...");

		double elapsedTimeTotal = 0.0;
		Flag.sw.startTotal();
		double s = System.nanoTime();
		SecIntegerBinaryVDPBatchOT1 ibvdp = new SecIntegerBinaryVDPBatchOT1();
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

		System.out.println("bandwidth:" + ibvdp.bandwidth / 1024.0 + " KB");
		System.out.println("time overall:" + elapsedTimeTotal / 1e9 + " s");
		System.out.println("network time:" + ibvdp.timeNetwork / 1e9 + " s");
		System.out.println("time no network:" + (elapsedTimeTotal - ibvdp.timeNetwork) / 1e9 + " s");
		System.out.println("ot initialize time:" + ibvdp.timeOTInitial / 1e9 + " s");

		// verify
		System.out.println("z:" + AdditiveUtil.add(ibvdp.z0, ibvdp.z1) + " z0:" + ibvdp.z0 + " z1:" + ibvdp.z1);

		rcvChannel.disconnect();
		sndChannel.disconnect();

		/*
		 * Flag.sw.stopTotal(); // double e = System.nanoTime();
		 * System.out.println("Gen running time(second):" + elapsedTimeTotal / 1e9);
		 * System.out.println("Gen running time(mu second):" + elapsedTimeTotal / 1e3);
		 * 
		 * if (Flag.CountTime) Flag.sw.print(); if (Flag.countIO)
		 * rcvChannel.printStatistic();
		 */

	}

}
