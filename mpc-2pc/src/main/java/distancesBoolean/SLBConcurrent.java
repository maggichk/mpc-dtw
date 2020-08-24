package distancesBoolean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import additive.SharedSequence;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import distances.SSED;
import flexSC.flexsc.CompEnv;
import gadgets.SBranchGadget;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBoolean.SBranch2PC;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SLBConcurrent {
	private long[] dist1;
	private long[] dist2;
	private long[] SLB;
	private double time;
	private ShareGenerator generator;
	private int queryLength;

	public long bandwidth;

	// private int checkPort =0;
	public String host = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);

	public SLBConcurrent(int queryLength) {
		this.dist1 = new long[2];
		this.dist2 = new long[2];
		this.SLB = new long[2];
		this.generator = new ShareGenerator(true);
		this.queryLength = queryLength;
	}

	public long[] computeConcurrent(MultiplicationTriple mt, ANDTriple mt2, SharedSequence U0, SharedSequence U1,
			SharedSequence L0, SharedSequence L1, SharedSequence Y0, SharedSequence Y1) {

		// retrieve sequences
		// u0
		long[] u0set = U0.getSharedSequence();
		long[] squ0set = U0.getSharedSquareSequence();
		// U1
		long[] u1set = U1.getSharedSequence();
		long[] squ1set = U1.getSharedSquareSequence();

		// L0
		long[] l0set = L0.getSharedSequence();
		long[] sql0set = L0.getSharedSquareSequence();
		// L1
		long[] l1set = L1.getSharedSequence();
		long[] sql1set = L1.getSharedSquareSequence();

		long[] y0set = Y0.getSharedSequence();
		long[] sqy0set = Y0.getSharedSquareSequence();

		long[] y1set = Y1.getSharedSequence();
		long[] sqy1set = Y1.getSharedSquareSequence();

		final long r = 0L;

		int gcPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);// borrow to boolean gadget
		int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
		final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);

		// establish tasks
		List<Runnable> taskList = new ArrayList<Runnable>();
		int counter = 0;
		while (counter < queryLength) {
			// System.out.println("counter:" + counter);
			final int portUY = gcPort + counter;
			// System.out.println("counter:"+counter+" portUY:"+portUY);
			final int portYL = gcPort + counter + queryLength;
			// System.out.println("counter:"+counter+" portYL:"+portYL);

			final int arithmeticPortUY = arithmeticPort + counter;
			// System.out.println("counter:"+counter+" arithPortUY:"+arithmeticPortUY);
			final int arithmeticPortLY = arithmeticPort + counter + queryLength;
			// System.out.println("counter:"+counter+" arithPortLY:"+arithmeticPortLY);

			// System.out.println("counter:"+counter);
			final int portUYCli = gcPort + counter + 2 * queryLength;
			// System.out.println("counter:"+counter+" portUY:"+portUYCli);
			final int portYLCli = gcPort + counter + queryLength + 2 * queryLength;
			// System.out.println("counter:"+counter+" portYL:"+portYLCli);

			final int arithmeticPortUYCli = arithmeticPort + counter + 2 * queryLength;
			// System.out.println("counter:"+counter+" arithPortUY:"+arithmeticPortUY);
			final int arithmeticPortLYCli = arithmeticPort + counter + queryLength + 2 * queryLength;
			// System.out.println("counter:"+counter+" arithPortLY:"+arithmeticPortLY);

			// retrieve y, u, l
			final long u0 = u0set[counter];
			final long y0 = y0set[counter];
			final long u1 = u1set[counter];
			final long y1 = y1set[counter];
			final long l0 = l0set[counter];
			final long l1 = l1set[counter];

			final long squ0 = squ0set[counter];
			final long sqy0 = sqy0set[counter];
			final long squ1 = squ1set[counter];
			final long sqy1 = sqy1set[counter];
			final long sql0 = sql0set[counter];
			final long sql1 = sql1set[counter];

			Runnable taskUY = new Runnable() {

				@Override
				public void run() {

					SSED ssed = new SSED(arithmeticPortUY, arithmeticPortUYCli, hostname);
					long[] ssedUY = new long[2];

					// random w1
					long w1 = generator.generateRandom(true);
					// dist1[1] = AdditiveUtil.add(w1, dist1[1]);

					// SSED(U,Y)
					try {
						ssedUY = ssedConcurrent(ssed, mt, u0, y0, u1, y1, squ0, sqy0, squ1, sqy1);
						// System.out.println("verify ssedUY0:" + AdditiveUtil.add(ssedUY[0],
						// ssedUY[1]));
						int selectionBit = 1;
						sbranchConcurrent(portUY, portUYCli, mt2, mt, u0, y0, u1, y1, ssedUY[0], r, ssedUY[1], r, w1,
								selectionBit);

						/*
						 * if (u0 == u0set[0]) { System.out .println("verify u:" + AdditiveUtil.add(u0,
						 * u1) + " y:" + AdditiveUtil.add(y0, y1)); System.out.println("verify ssedUY0:"
						 * + AdditiveUtil.add(ssedUY[0], ssedUY[1]));
						 * System.out.println("verify branch res:" + AdditiveUtil.add(dist1[0],
						 * dist1[1]));
						 * 
						 * }
						 */

					} catch (Exception e) {

						e.printStackTrace();
					}

				}

			};
			taskList.add(taskUY);

			Runnable taskYL = new Runnable() {

				@Override
				public void run() {
					SSED ssed = new SSED(arithmeticPortLY, arithmeticPortLYCli, hostname);

					long[] ssedYL = new long[2];
					long gcRes = 0L;

					// random w2
					long w2 = generator.generateRandom(true);
					// dist2[1] = AdditiveUtil.add(w2, dist2[1]);

					// SSED
					try {
						ssedYL = ssedConcurrent(ssed, mt, y0, l0, y1, l1, sqy0, sql0, sqy1, sql1);
						int selectionBit = 2;
						sbranchConcurrent(portYL, portYLCli, mt2, mt, y0, l0, y1, l1, ssedYL[0], r, ssedYL[1], r, w2,
								selectionBit);
					} catch (Exception e) {

						e.printStackTrace();
					}

				}

			};
			taskList.add(taskYL);

			counter++;
		}

		// establish threads
		ExecutorService exec = Executors.newFixedThreadPool(queryLength * 2);
		for (int i = 0; i < taskList.size(); i++) {
			exec.submit(taskList.get(i));
		}
		// shutdown
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

		SLB[0] = AdditiveUtil.add(dist1[0], dist2[0]);

		SLB[1] = AdditiveUtil.add(dist1[1], dist2[1]);

		/*System.out.println("Shutdown threads, SLB:" + AdditiveUtil.add(SLB[0], SLB[1]) + " SLB[0]:" + SLB[0]
				+ " SLB[1]:" + SLB[1]);*/

		return SLB;
	}

	public void sbranchConcurrent(int port, int portCli, ANDTriple mt2, MultiplicationTriple mt, long m1_0, long m2_0,
			long m1_1, long m2_1, long c1_0, long c2_0, long c1_1, long c2_1, long omega, int selectionBit) {
		int checkPort = port;
		final int serverPort = checkPort;
		final int cliPort = checkPort + (portCli - port);
		//System.out.println("sbranch Connection| portServer:" + serverPort + ", portClient:" + cliPort);

		final Server sndChannel = new Server();
		final Client rcvChannel = new Client();

		ExecutorService exec = Executors.newFixedThreadPool(2);
		// Alice
		exec.execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				sndChannel.listen(serverPort);
				sndChannel.flush();

			}

		});

		// Bob
		exec.execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {

				try {
					rcvChannel.connect(host, serverPort, cliPort);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

		// shutdown threads
		// Connection should be established within 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
				// System.out.println("SLB-GC| disconnecting....serverPort:"+serverPort);

			} else {
				System.out.println("SLB-GC| somthing wrong");

			}
		} catch (InterruptedException e) {
			// Something is wrong
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		SBranch2PC sbranch = new SBranch2PC();
		long[] dist;
		try {
			dist = sbranch.compute(false, sndChannel, rcvChannel, mt2, mt, m1_0, m2_0, m1_1, m2_1, c1_0, c2_0, c1_1,
					c2_1, omega);

			if (selectionBit == 1) {
				// branch(U,Y)
				dist1[0] = AdditiveUtil.add(dist1[0], dist[0]);
				dist1[1] = AdditiveUtil.add(dist1[1], dist[1]);

				/*System.out.println("sbranch res dist1:" + AdditiveUtil.add(dist[0], dist[1]) + " omega:" + omega
						+ " m1(u):" + AdditiveUtil.add(m1_0, m1_1) + " m2(y):" + AdditiveUtil.add(m2_0, m2_1)
						+ " c1(uy):" + AdditiveUtil.add(c1_0, c1_1) + " c2(0):" + AdditiveUtil.add(c2_0, c2_1)
						+ " dist[0]:" + dist[0] + " dist[1]:" + dist[1]);
*/
			} else if (selectionBit == 2) {
				// branch(Y,L)
				dist2[0] = AdditiveUtil.add(dist2[0], dist[0]);
				dist2[1] = AdditiveUtil.add(dist2[1], dist[1]);

				/*System.out.println("sbranch res dist2:" + AdditiveUtil.add(dist[0], dist[1]) + " omega:" + omega
						+ " m1(2):" + AdditiveUtil.add(m1_0, m1_1) + " m2(l):" + AdditiveUtil.add(m2_0, m2_1)
						+ " c1(yl):" + AdditiveUtil.add(c1_0, c1_1) + " c2(0):" + AdditiveUtil.add(c2_0, c2_1)
						+ " dist[0]:" + dist[0] + " dist[1]:" + dist[1]);*/
			}

			bandwidth += sbranch.bandwidth;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

	public long[] ssedConcurrent(SSED ssed, MultiplicationTriple mt1, long u0, long y0, long u1, long y1, long squ0,
			long sqy0, long squ1, long sqy1) throws Exception {

		// ssed(u,y) -> c1
		long[] ssedUY = new long[2];
		// long u0, long y0, long u1, long y1, long squ0, long sqy0, long squ1, long
		// sqy1
		ssedUY = ssed.computeConcurrent(mt1, u0, y0, u1, y1, squ0, sqy0, squ1, sqy1);
		bandwidth += ssed.bandwidth;
		return ssedUY;

	}

	public static void main(String[] args) {
		final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		final int port = 5554;
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

		int queryLength = 128;
		// int queryLength = 1;

		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		sndChannel.disconnect();
		rcvChannel.disconnect();

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ANDTriple mt2 = new ANDTriple(boolGen);

		System.out.println("finish generating MTs");
		ShareGenerator generator = new ShareGenerator(true);
		// u
		long[] udata = new long[queryLength];
		// u0data[0] = 10;
		// u0data[1] = 10;
		long[] usqdata = new long[queryLength];
		// u0sqdata[0] = 100;
		// u0sqdata[1] = 100;
		for (int i = 0; i < queryLength; i++) {
			udata[i] = 10;
			usqdata[i] = 100;
			// u0data[i] = generator.generateRandom(true);
			// u0sqdata[i] = AdditiveUtil.mul(u0data[i], u0data[i]);
		}
		SharedSequence U0 = new SharedSequence(queryLength, 0, 0, 0, udata, usqdata);
		// u1
		generator.generateSharedSequence(U0);
		SharedSequence U1 = generator.S1;
		U0 = generator.S0;
		System.out.println(
				"u[0]:" + udata[0] + " U0[0]:" + U0.getSharedSequence()[0] + " U1[0]:" + U1.getSharedSequence()[0]);

		// l
		long[] ldata = new long[queryLength];
		// l0data[0] = 7;
		// l0data[1] = 7;
		long[] lsqdata = new long[queryLength];
		// l0sqdata[0] = 49;
		// l0sqdata[1] = 49;
		for (int i = 0; i < queryLength; i++) {
			// l0data[i] = generator.generateRandom(true);
			// l0sqdata[i] = AdditiveUtil.mul(l0data[i], l0data[i]);
			ldata[i] = 7;
			lsqdata[i] = 49;
		}
		SharedSequence L0 = new SharedSequence(queryLength, 0, 0, 0, ldata, lsqdata);
		// l1
		generator.generateSharedSequence(L0);
		SharedSequence L1 = generator.S1;
		L0 = generator.S0; // l-L1
		System.out.println(
				"l[0]:" + ldata[0] + " L0[0]:" + L0.getSharedSequence()[0] + " L1[0]:" + L1.getSharedSequence()[0]);

		// y
		long[] ydata = new long[queryLength];
		// y0data[0] = 3;
		// y0data[1] = 4;
		long[] ysqdata = new long[queryLength];
		// y0sqdata[0] = 9;
		// y0sqdata[1] = 16;
		for (int i = 0; i < queryLength; i++) {
			ydata[i] = 30;
			ysqdata[i] = 900;
			// y0data[i] = generator.generateRandom(true);
			// y0sqdata[i] = AdditiveUtil.mul(y0data[i], y0data[i]);
		}
		SharedSequence Y0 = new SharedSequence(queryLength, 0, 0, 0, ydata, ysqdata);
		// System.out.println("1) Y0:" + String.valueOf(Y0.getSharedData(0)[0]) + " " +
		// Y0.getSharedData(0)[1]);

		// y1
		generator.generateSharedSequence(Y0);
		SharedSequence Y1 = generator.S1;
		Y0 = generator.S0;

		System.out.println("y[0]:" + AdditiveUtil.add(Y0.getSharedSequence()[0], Y1.getSharedSequence()[0]));
		System.out.println("u[0]:" + AdditiveUtil.add(U0.getSharedSequence()[0], U1.getSharedSequence()[0]));
		System.out.println("l[0]:" + AdditiveUtil.add(L0.getSharedSequence()[0], L1.getSharedSequence()[0]));

		double s = System.nanoTime();
		SLBConcurrent slb = new SLBConcurrent(queryLength);
		long[] SLB = slb.computeConcurrent(mt, mt2, U0, U1, L0, L1, Y0, Y1);
		// queryLength);
		double e = System.nanoTime();
		System.out.println("SLB running time:" + (e - s) / 1e9);
		System.out.println("thread running time:" + (slb.time) / 1e9);

		System.out.println("SLB:" + AdditiveUtil.add(SLB[0], SLB[1]));

	}

}
