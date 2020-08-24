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
import flexSC.gc.GCSignal;
import flexSC.network.Client;
import flexSC.network.Network;
import flexSC.network.Server;
import flexSC.ot.OTExtReceiver;
import flexSC.ot.OTExtSender;
import flexSC.ot.OTReceiver;
import flexSC.ot.OTSender;
import flexSC.util.Utils;

@Deprecated
public class SecIntegerBinaryVDPPairwise {

	// set OT channel based on FlexSC
	private OTSender snd;
	private OTReceiver rcv;

	public double bandwidth = 0.0;
	public long timeNetwork = 0;

	public int len_n;

	public long U0, U1;
	public long V0, V1;
	
	private long x_0, x_1;
	private byte w_0, w_1; 

	private ShareGenerator shrGen;
	private SecureRandom random;

	public SecIntegerBinaryVDPPairwise() {
		this.shrGen = new ShareGenerator(true);
		this.random = shrGen.random;
	}

	// boolean shares of binary vector <w0,w1,w2,..,w_n-1>: w_0_arr, w_1_arr
	// arithmetic shares of integer vector <a0, a1, .., a_n-1>: a_0_arr, a_1_arr
	public long[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple mt,
			long[] x_0_arr, byte[] w_0_arr, long[] x_1_arr, byte[] w_1_arr) throws Exception {
		len_n = x_0_arr.length;
		
		return null;
	}

	public long[] computePairwise(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple mt,
			long x_0, byte w_0, long x_1, byte w_1) throws Exception {
		this.x_0  = x_0;
		this.w_0 = w_0;
		this.x_1 = x_1;
		this.w_1 = w_1;
		
		long[] z = new long[2];
		long z_0, z_1 = 0L;

		// COT-f (x_0) multi (w_0 xor w_1)
		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {

				setSnd(sndChannel);

				generateU0();// multi(x_0, w) U0=r
				sndChannel.flush();
				//generateV0();
				//sndChannel.flush();

			}
		});

		exec.execute(new Runnable() {

			@Override
			public void run() {

				setRcv(rcvChannel);

				generateU1();
				rcvChannel.flush();
				//generateV1();
				//rcvChannel.flush();

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

		return z;
	}

	/**
	 * Initialize OT sender - 32 bits label
	 * 
	 * @param sndChannel
	 */
	private void setSnd(Network sndChannel) {
		if (sndChannel != null) {
			System.out.println("Initialize OTExtSender");
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
			System.out.println("Initialize OTExtReceiver");
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
	 * Generate <U>_0, <U> = [w] * <x>_0
	 */
	private void generateU0() {
		// send A0
		this.U0 = this.generateP0Share(this.x_0);
	}

	private long generateP0Share(long x_0) {
		long U0 = 0L;
		GCSignal[] pair = new GCSignal[2];
		
			long r = random.nextInt(Integer.MAX_VALUE);
			long si0 = AdditiveUtil.add(r, AdditiveUtil.mul(this.w_0, this.x_0));
			long si1 =  AdditiveUtil.add(r, AdditiveUtil.mul( (1-this.w_0), this.x_0));
			

			pair = generatePairs(si0, si1);// si0, si1
		
		try {
			snd.send(pair);

		} catch (IOException e) {
			e.printStackTrace();
		}

		U0 = AdditiveUtil.modAdditive(-r);

		return U0;
	}
	
	/**
	 * Generate <U>_1, <U> = [w] * <x>_0
	 */
	private void generateU1() {
		// receive w1
		this.U1 = this.generateP1Share(this.w_1);
	}
	//selection bit b=[w]_1
	private long generateP1Share(byte w_1) {
		long U1 = 0L;
		boolean inputB1 = (w_1==0)? false : true;
		try {
			GCSignal res = rcv.receive(inputB1);// select bits between si0, si1 based on value B1
			U1 = AdditiveUtil.modAdditive(new BigInteger(res.bytes).longValue());
			

		} catch (IOException e) {
			e.printStackTrace();
		}

		return U1;
	}

}
