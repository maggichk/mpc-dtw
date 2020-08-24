package gadgetBoolean;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
//import additive.AdditiveUtil2;
import additive.MultiplicationTriple;
//import additive.MultiplicationTriple2;
import additive.SeqCompEngine;
//import additive.SeqCompEngine2;
import additive.ShareGenerator;
import additive.SharedSequence;
import booleanShr.ANDTriple;
import booleanShr.BooleanANDEngine;
import booleanShr.BooleanANDEngineBatch;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import common.util.Converter;
import flexSC.network.Client;
import flexSC.network.Server;
import flexSC.util.Utils;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class SCMPBoolean {

	private Server sndChannel;
	private Client rcvChannel;
	private int port;
	private String hostname;
	private int portCli;

	public double bandwidth = 0;
	public long timeNetwork = 0;

	//public final long modulus = 2;
	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 
							 0, 0, 0, 0, 0, 0, 0, 0, 
							 0, 0, 0, 0, 0, 0, 0, 0, 
							 0, 0, 0, 0, 0, 0, 0 };


	
	public SCMPBoolean() {

	}

	public SCMPBoolean(int port, int portCli, String hostname) {
		this.port = port;
		this.hostname = hostname;
		this.portCli = portCli;

	}

	public long[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			MultiplicationTriple mt, long x0, long y0, long x1, long y1) throws Exception {

		long z[] = new long[2];
		long z0 = AdditiveUtil.sub(y0, x0);
		long z1 = AdditiveUtil.sub(y1, x1);
		byte[] z0BitArr = Utils.fromLong2byteRightmost(z0, 31);
		byte[] z1BitArr = Utils.fromLong2byteRightmost(z1, 31);


		BooleanANDEngineBatch engineBatch = new BooleanANDEngineBatch(isDisconnect, sndChannel, rcvChannel,
				mt2, z0BitArr, ARR_0, ARR_0, z1BitArr);
		byte[] d0BitArr = engineBatch.z0;
		byte[] d1BitArr = engineBatch.z1;
		bandwidth += engineBatch.bandwidth;
		timeNetwork += engineBatch.time;


		byte[] c0BitArr = new byte[31]; 
		byte[] c1BitArr = new byte[31];
		byte z0l = 0;
		byte z1l = 0;
		for (int i = 0; i < 31; i++) {

			// System.out.print(AdditiveUtil2.add(z0i, z1i));
			// System.out.print(AdditiveUtil2.add(w0i, w1i));
			byte w0i = z0BitArr[i];
			byte w1i = z1BitArr[i];

			if (i == 0) {
				c0BitArr[0] = d0BitArr[0];
				c1BitArr[0] = d1BitArr[0];
				// System.out.println("round 0 verify c:" + AdditiveUtil2.add(c0i, c1i)+" verify
				// w:"+AdditiveUtil2.add(w0i, w1i));

				// mts.remove(0);
			} else if (i == 30) {

				z0l = BooleanUtil.xor(w0i, c0BitArr[i - 1]);// Arr[29]
				z1l = BooleanUtil.xor(w1i, c1BitArr[i - 1]);
				

			} else {
				

				d0BitArr[i] = BooleanUtil.xor(d0BitArr[i], (byte) 1);
				byte d0i = d0BitArr[i];
				byte d1i = d1BitArr[i];
				

				byte c0is1 = c0BitArr[i - 1];
				byte c1is1 = c1BitArr[i - 1];
				BooleanANDEngine engine1 = new BooleanANDEngine(isDisconnect, sndChannel, rcvChannel, mt2, w0i, c0is1,
						w1i, c1is1);
				// bandwidth += engine1.bandwidth / 32.0;
				bandwidth += engine1.bandwidth;
				timeNetwork += engine1.time;

				byte e0i = BooleanUtil.xor(engine1.z0, (byte) 1);
				byte e1i = engine1.z1;
				BooleanANDEngine engine2 = new BooleanANDEngine(isDisconnect, sndChannel, rcvChannel, mt2, e0i, d0i,
						e1i, d1i);
				// bandwidth += engine2.bandwidth / 32.0;
				bandwidth += engine2.bandwidth;
				timeNetwork += engine2.time;

				byte c0i = BooleanUtil.xor(engine2.z0, (byte) 1);
				byte c1i = engine2.z1;

				c0BitArr[i] = c0i;
				c1BitArr[i] = c1i;

			}

		}

		// convert back to Z31
		long t1_0 = z0l;
		long t1_1 = 0;
		long t2_0 = 0;
		long t2_1 = z1l;
		SeqCompEngine engine3 = new SeqCompEngine(isDisconnect, sndChannel, rcvChannel, mt, t1_0, t2_0, t1_1, t2_1);
		bandwidth += engine3.bandwidth;

		long z0lAddi = AdditiveUtil.sub(AdditiveUtil.add(t1_0, t2_0), AdditiveUtil.mul(2L, engine3.z0));
		long z1lAddi = AdditiveUtil.sub(AdditiveUtil.add(t1_1, t2_1), AdditiveUtil.mul(2L, engine3.z1));

		z[0] = z0lAddi;
		z[1] = z1lAddi;

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
		System.out.println("mt2 A0:" + mt2.tripleA0 + " A1:" + mt2.tripleA1 + " B0:" + mt2.tripleB0 + " B1:"
				+ mt2.tripleB1 + " C0:" + mt2.tripleC0 + " C1:" + mt2.tripleC1);

		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		ShareGenerator generator = new ShareGenerator(true);

		// generate two shares
		long a = 40;
		long b = 7;

		generator.generateSharedDataPoint(a, true);
		long a0 = generator.x0;
		long a1 = generator.x1;

		System.out.println("a:" + a + " a0:" + a0 + " a1:" + a1 + " verify:" + AdditiveUtil.add(a0, a1));

		generator.generateSharedDataPoint(b, true);
		long b0 = generator.x0;
		long b1 = generator.x1;

		System.out.println("b:" + b + " b0:" + b0 + " b1:" + b1 + " verify:" + AdditiveUtil.add(b0, b1));

		SCMPBoolean scmp = new SCMPBoolean();// y, u

		int round = 1;
		long time = 0;
		for (int i = 0; i < round; i++) {
			long e = System.nanoTime();
			long[] z = scmp.compute(false, sndChannel, rcvChannel, mt2, mt, a0, b0, a1, b1);
			long s = System.nanoTime();
			time += s - e;

			// verify
			long z0 = z[0]; long z1 = z[1]; System.out.println("z:" +
					  AdditiveUtil.add(z0, z1) + " z0:" + z0 + " z1:" + z1);
			

			if (i % 1000 == 0) {
				System.out.println("Progress:" + i);
			}
		}

		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + scmp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		System.out.println("timeNetwork:" + scmp.timeNetwork / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

}
