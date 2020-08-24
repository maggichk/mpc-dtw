package additive;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

public class MultiplicationTriple2 {

	public long tripleA0, tripleA1;
	public long tripleB0, tripleB1;
	public long tripleC0, tripleC1; // A * B = C

	private OTSender snd;
	private OTReceiver rcv;

	private long tripleU0, tripleU1;
	private long tripleV0, tripleV1;

	private ShareGenerator2 shrGen;
	private SecureRandom random;

	private static double elapsedTimeTotal;
	// private double startTime;

	public MultiplicationTriple2() {

	}

	public MultiplicationTriple2(long tripleA0, long tripleA1, long tripleB0, long tripleB1, long tripleC0,
			long tripleC1) {
		this.tripleA0 = tripleA0;
		this.tripleA1 = tripleA1;
		this.tripleB0 = tripleB0;
		this.tripleB1 = tripleB1;
		this.tripleC0 = tripleC0;
		this.tripleC1 = tripleC1;
	}

	
	/**
	 * Dummy MT generation
	 * @param shrGen
	 */
	public MultiplicationTriple2(ShareGenerator2 shrGen) {
		this.shrGen = shrGen;
		this.random = shrGen.random;
		this.generateSharedAB(random);
		
		// get A
		long A = AdditiveUtil2.add(tripleA0, tripleA1);
		// get B
		long B = AdditiveUtil2.add(tripleB0, tripleB1);
		
		// compute C
		long C = AdditiveUtil2.mul(A,B);

		// C0, C1
		shrGen.generateSharedDataPoint(C);
		this.tripleC0 = shrGen.x0;
		this.tripleC1 = shrGen.x1;				
		
	}

	public MultiplicationTriple2(ShareGenerator2 shrGen, final Network sndChannel, final Network rcvChannel) {
		this.shrGen = shrGen;
		this.random = shrGen.random;
		this.generateSharedAB(random);

		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {

				setSnd(sndChannel);

				generateU0();
				sndChannel.flush();
				generateV0();
				sndChannel.flush();

			}
		});

		exec.execute(new Runnable() {

			@Override
			public void run() {

				setRcv(rcvChannel);

				generateU1();
				rcvChannel.flush();
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

		generateTripleC();
	}

	public MultiplicationTriple2(boolean isCloseSocket, ShareGenerator2 shrGen, final Server sndChannel,
			final Client rcvChannel) {
		this.shrGen = shrGen;
		this.random = shrGen.random;
		this.generateSharedAB(random);

		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {

				setSnd(sndChannel);
				generateU0();
				sndChannel.flush();
				generateV0();
				sndChannel.flush();

			}
		});

		exec.execute(new Runnable() {

			@Override
			public void run() {

				setRcv(rcvChannel);

				generateU1();
				rcvChannel.flush();
				generateV1();
				rcvChannel.flush();

			}

		});

		/*
		 * try { exec.wait(Long.MAX_VALUE); } catch (InterruptedException e1) { // TODO
		 * Auto-generated catch block e1.printStackTrace(); }
		 */

		// Create OT instance
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();

				generateTripleC();

				rcvChannel.disconnect();
				sndChannel.disconnect();
				// System.out.println("6) disconnect");
			}
		} catch (InterruptedException e) {
			// Something is wrong
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

	}

	public MultiplicationTriple2(final Network sndChannel, final Network rcvChannel) {

		this.shrGen = new ShareGenerator2(true);
		this.random = shrGen.random;
		// generate shares of <A>, <B>
		this.generateSharedAB(random);
		// System.out.println("a0:" + this.tripleA0 + " a1:" + this.tripleA1 + " b0:" +
		// this.tripleB0 + " b1:" + this.tripleB1);

		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {

				setSnd(sndChannel);
				generateU0();
				sndChannel.flush();
				generateV0();
				sndChannel.flush();

			}
		});

		exec.execute(new Runnable() {

			@Override
			public void run() {

				setRcv(rcvChannel);
				generateU1();
				rcvChannel.flush();
				generateV1();
				rcvChannel.flush();

			}

		});

		/*
		 * exec.execute(new Runnable() {
		 * 
		 * @Override public void run() { setSnd(sndChannel);
		 * 
		 * generateV0(); sndChannel.flush(); } });
		 * 
		 * 
		 * exec.execute(new Runnable() {
		 * 
		 * @Override public void run() { setRcv(rcvChannel);
		 * 
		 * generateV1(); rcvChannel.flush(); }
		 * 
		 * });
		 */

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

		generateTripleC();
	}

	private void generateTripleC() {

		// recover <U>
		long tripleU = AdditiveUtil2.add(tripleU0, tripleU1);

		// recover <V>
		long tripleV = AdditiveUtil2.add(tripleV0, tripleV1);

		// get A0*B0
		long A0B0 = AdditiveUtil2.mul(tripleA0, tripleB0);
		// get A1*B1
		long A1B1 = AdditiveUtil2.mul(tripleA1, tripleB1);
		// System.out.println("U0:"+tripleU0+" U1:"+tripleU1+" V0:"+tripleV0+"
		// V1:"+tripleV1);
		// System.out.println("A0B0:"+A0B0+" A1B1:"+A1B1+" A0B1(U):"+tripleU+"
		// A1B0(V):"+tripleV);

		// compute C
		long C = AdditiveUtil2.add(AdditiveUtil2.add(AdditiveUtil2.add(A0B0, A1B1), tripleU), tripleV);

		// C0, C1
		shrGen.generateSharedDataPoint(C);
		this.tripleC0 = shrGen.x0;
		this.tripleC1 = shrGen.x1;

	}

	private void setSnd(Network sndChannel) {
		if (sndChannel != null) {
			// System.out.println("Initialize OTExtSender");
			snd = new OTExtSender(32, sndChannel);
		}
	}

	private void setRcv(Network rcvChannel) {
		if (rcvChannel != null) {
			rcv = new OTExtReceiver(rcvChannel);
		}
	}

	private void generateSharedAB(SecureRandom random) {
		shrGen.generateSharedDataPointSet(random);
		this.tripleA0 = shrGen.x0;
		this.tripleA1 = shrGen.x1;

		shrGen.generateSharedDataPointSet(random);
		this.tripleB0 = shrGen.x0;
		this.tripleB1 = shrGen.x1;

	}

	private GCSignal[] generatePairs(long si0, long si1) {
		GCSignal[] label = new GCSignal[2];

		label[0] = GCSignal.newInstance(BigInteger.valueOf(si0).toByteArray());

		label[1] = GCSignal.newInstance(BigInteger.valueOf(si1).toByteArray());
		return label;
	}

	/**
	 * Generate <V>_0, <V> = <A>_1 * <B>_0
	 */
	private void generateV0() {
		// send B0
		this.tripleV0 = this.generateP0Share(this.tripleB0);
	}

	/**
	 * Generate <U>_0, <U> = <A>_0 * <B>_1
	 */
	private void generateU0() {
		// send A0
		this.tripleU0 = this.generateP0Share(this.tripleA0);
	}

	/**
	 * Generate <U> = <A>_0 * <B>_1
	 */
	private void generateU1() {
		// receive B1
		this.tripleU1 = this.generateP1Share(this.tripleB1);
	}

	/**
	 * Generate <V>_1, <V> = <A>_1 * <B>_0
	 */
	private void generateV1() {
		// receive A1
		this.tripleV1 = this.generateP1Share(this.tripleA1);
	}

	private long generateP0Share(long A0) {
		long U0 = 0L;
		GCSignal[][] pair = new GCSignal[1][2];
		for (int i = 0; i < 1; i++) {
			long si0 = random.nextInt(Integer.MAX_VALUE);
			long si1 = AdditiveUtil.sub(AdditiveUtil.modAdditive(A0 << i), si0);
			U0 = AdditiveUtil.add(si0, U0);

			pair[i] = generatePairs(AdditiveUtil.modAdditive(-si0), si1);// si0, si1
		}
		try {
			snd.send(pair);

		} catch (IOException e) {
			e.printStackTrace();
		}

		U0 = AdditiveUtil.modAdditive(U0);

		return U0;
	}

	private long generateP1Share(long B1) {
		long U1 = 0L;
		boolean[] inputB1 = Utils.fromInt((int) B1, 1);
		try {
			GCSignal[] res = rcv.receive(inputB1);// select bits between si0, si1 based on value B1
			for (int i = 0; i < 1; i++) {
				U1 += AdditiveUtil.modAdditive(new BigInteger(res[i].bytes).longValue());
			}
			U1 = AdditiveUtil.modAdditive(U1);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return U1;
	}

	public long getTripleA0() {
		return tripleA0;
	}

	public void setTripleA0(long tripleA0) {
		this.tripleA0 = tripleA0;
	}

	public long getTripleA1() {
		return tripleA1;
	}

	public void setTripleA1(long tripleA1) {
		this.tripleA1 = tripleA1;
	}

	public long getTripleB0() {
		return tripleB0;
	}

	public void setTripleB0(long tripleB0) {
		this.tripleB0 = tripleB0;
	}

	public long getTripleB1() {
		return tripleB1;
	}

	public void setTripleB1(long tripleB1) {
		this.tripleB1 = tripleB1;
	}

	public long getTripleC0() {
		return tripleC0;
	}

	public void setTripleC0(long tripleC0) {
		this.tripleC0 = tripleC0;
	}

	public long getTripleC1() {
		return tripleC1;
	}

	public void setTripleC1(long tripleC1) {
		this.tripleC1 = tripleC1;
	}

	public long getTripleU0() {
		return tripleU0;
	}

	public void setTripleU0(long tripleU0) {
		this.tripleU0 = tripleU0;
	}

	public long getTripleU1() {
		return tripleU1;
	}

	public void setTripleU1(long tripleU1) {
		this.tripleU1 = tripleU1;
	}

	public long getTripleV0() {
		return tripleV0;
	}

	public void setTripleV0(long tripleV0) {
		this.tripleV0 = tripleV0;
	}

	public long getTripleV1() {
		return tripleV1;
	}

	public void setTripleV1(long tripleV1) {
		this.tripleV1 = tripleV1;
	}

	public static void main(String[] args) {
		// int counter = Config.getSettingInt(Constants.CONFIG_MT_NUM);
		int counter = 1;

		// String fileNameIn = Config.getSetting(Constants.CONFIG_MT_FILE_NAME_IN);
		String fileNameOut = Config.getSetting(Constants.CONFIG_MT_FILE_NAME_OUT);
		String filePath = Config.getSetting(Constants.CONFIG_MT_PATH);
		String separator = Config.getSetting(Constants.CONFIG_MT_SEPARATOR);
		System.out.println("Separator: [" + separator + "]");

		WriteFile writeFile = new WriteFile();
		boolean isLastLine = false;
		// int queryLength =
		// Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);//128

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

		System.out.println("Starting Multiplication Triple generation...");

		Flag.sw.startTotal();
		for (int i = 0; i < counter; i++) {
			if (i == counter - 1) {
				isLastLine = true;
			}
			String line = "";

			double s = System.nanoTime();
			MultiplicationTriple2 mt = new MultiplicationTriple2(sndChannel, rcvChannel);
			double e = System.nanoTime();
			elapsedTimeTotal += e - s;

			// write to file
			line = mt.tripleA0 + separator + mt.tripleA1 + separator + mt.tripleB0 + separator + mt.tripleB1 + separator
					+ mt.tripleC0 + separator + mt.tripleC1;
			writeFile.writeFile(filePath, fileNameOut, line, isLastLine);

			System.out.println("A0:" + mt.tripleA0 + " A1:" + mt.tripleA1 + " B0:" + mt.tripleB0 + "B1:" + mt.tripleB1
					+ " C0:" + mt.tripleC0 + " C1:" + mt.tripleC1);

			/*
			 * long a = AdditiveUtil.add(mt.tripleA0, mt.tripleA1); long b =
			 * AdditiveUtil.add(mt.tripleB0, mt.tripleB1); long c =
			 * AdditiveUtil.add(mt.tripleC0, mt.tripleC1);
			 * 
			 * System.out.print("MT A:" + a); System.out.print(" MT B:" + b);
			 * System.out.println(" MT C:" + c);
			 * 
			 * 
			 * // verify long cVer = AdditiveUtil.mul(a, b);
			 * 
			 * System.out.println("verify c:" + cVer +" u:"+AdditiveUtil.mul(mt.tripleA0,
			 * mt.tripleB1)+" v:"+AdditiveUtil.mul(mt.tripleA1, mt.tripleB0));
			 */

			// Shutdown channel in test program

			if (i % 1000 == 0) {
				System.out.println("Progress:" + i);
			}
		}
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
