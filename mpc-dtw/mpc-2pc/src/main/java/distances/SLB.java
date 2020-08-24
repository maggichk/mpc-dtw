package distances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import additive.SharedSequence;
import common.thread.PausableExecutorService;
import common.thread.PausableThreadPoolExecutor;
import flexSC.flexsc.CompEnv;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgets.SBranchGadget;
import gadgets.SBranchGadget.Generator;

import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SLB implements Distance {

	private CompEnv<Long> envGen;
	private CompEnv<Long> envEva;
	private Server sndChannel;
	private Client rcvChannel;
	private long aliceOut;
	private long bobOut;
	private long[] dist1;
	private long[] dist2;
	private long[] SLB;
	private long[] dist;

	private double time;
	private SSED ssed;
	private ShareGenerator generator;
	
	private int queryLength;

	

	public SLB(Server sndChannel, Client rcvChannel, int queryLength) {
		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;
		this.dist1 = new long[2];
		this.dist2 = new long[2];
		this.SLB = new long[2];
		this.dist = new long[2];
		
		this.ssed = new SSED();
		this.generator = new ShareGenerator(true);
		
		this.queryLength = queryLength;
	}

	/**
	 * SharedSequence U0 = sequences[0]; SharedSequence U1 = sequences[1];
	 * SharedSequence L0 = sequences[2]; SharedSequence L1 = sequences[3];
	 * SharedSequence Y0 = sequences[4]; SharedSequence Y1 = sequences[5];
	 * 
	 * @param sndChannel
	 * @param rcvChannel
	 * @param mts
	 * @param sequences
	 * @param queryLength
	 * @return
	 */
	@Override
	public long[] compute(ArrayList<MultiplicationTriple> mts,
			SharedSequence[] sequences, int queryLength) {
		
		
		SharedSequence U0 = sequences[0];
		SharedSequence U1 = sequences[1];
		SharedSequence L0 = sequences[2];
		SharedSequence L1 = sequences[3];
		SharedSequence Y0 = sequences[4];
		SharedSequence Y1 = sequences[5];
		try {
			return this.compute(mts, U0, U1, L0, L1, Y0, Y1, queryLength);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new long[2];
	}
	


	public long[] compute(ArrayList<MultiplicationTriple> mts, SharedSequence U0,
			SharedSequence U1, SharedSequence L0, SharedSequence L1, SharedSequence Y0, SharedSequence Y1,
			int queryLength) throws Exception {

		ShareGenerator generator = new ShareGenerator(true);
		SSED ssed = new SSED();

		/*
		 * SBranchGadget.Generator<Long> gen = new SBranchGadget.Generator<Long>();
		 * SBranchGadget.Evaluator<Long> eva = new SBranchGadget.Evaluator<Long>();
		 */

		@SuppressWarnings({ "rawtypes", "unchecked" })
		final GenRunnable<Long> runGen = (GenRunnable) new SBranchGadget.Generator<Long>();
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final EvaRunnable<Long> runEva = (EvaRunnable) new SBranchGadget.Evaluator<Long>();

		// start connecting GC
		// CompEnv<Long> envGen = runGen.connect();
		// CompEnv<Long> envEva = runEva.connect();
		//System.out.println("123");
		// PausableExecutorService exec = new PausableThreadPoolExecutor(2);
		

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

		long r = 0L;

		for (int i = 0; i < queryLength; i++) {
			//System.out.println("counter:" + i);
			// mt
			MultiplicationTriple mt1 = mts.get(0);
			mts.remove(0);

			MultiplicationTriple mt2 = mts.get(0);
			mts.remove(0);
			System.runFinalization();

			// generate random omega
			long w1 = generator.generateRandom(true);
			long w2 = generator.generateRandom(true);

			dist1[1] = w1;
			dist2[1] = w2;


			// ssed(u,y) -> c1
			long[] ssedUY = new long[2];
			// long u0, long y0, long u1, long y1, long squ0, long sqy0, long squ1, long
			// sqy1
			ssedUY = ssed.compute(false, sndChannel, rcvChannel, mt1, u0set[i], y0set[i], u1set[i], y1set[i], squ0set[i],
					sqy0set[i], squ1set[i], sqy1set[i]);


			// ssed(l,y) -> c2
			long[] ssedYL = new long[2];
			ssedYL = ssed.compute(false, sndChannel, rcvChannel, mt2, l0set[i], y0set[i], l1set[i], y1set[i], sql0set[i],
					sqy0set[i], sql1set[i], sqy1set[i]);
			

			// sbranch(u,y)
			// IF U < Y, SELECT SSED; IF U > Y, SELECT R.
			final String[] argsGenUY = new String[5];
			argsGenUY[0] = String.valueOf(u0set[i]);// <u>_0, <y>_0, <c1>_0, <r>_0, w1
			argsGenUY[1] = String.valueOf(y0set[i]);
			argsGenUY[2] = String.valueOf(ssedUY[0]); // ssedUY[0]
			argsGenUY[3] = String.valueOf(r);
			argsGenUY[4] = String.valueOf(w1);

			final String[] argsEvaUY = new String[4];// <u>_1, <y>_1, <c1>_1, <r>_1
			argsEvaUY[0] = String.valueOf(u1set[i]);
			argsEvaUY[1] = String.valueOf(y1set[i]);
			argsEvaUY[2] = String.valueOf(ssedUY[1]);// ssedUY[1]
			argsEvaUY[3] = String.valueOf(r);

			//sbranch(y,l)
			// IF Y < L, SELECT SSED; IF Y >L, SELECT R.
			final String[] argsGenYL = new String[5];
			argsGenYL[0] = String.valueOf(y0set[i]);// <y>_0, <yl>_0, <c2>_0, <r>_0, w2
			argsGenYL[1] = String.valueOf(l0set[i]);
			argsGenYL[2] = String.valueOf(ssedYL[0]); // ssedyl[0]
			argsGenYL[3] = String.valueOf(r);
			argsGenYL[4] = String.valueOf(w2);

			final String[] argsEvaYL = new String[4];// <y>_1, <l>_1, <c2>_1, <r>_1
			argsEvaYL[0] = String.valueOf(y1set[i]);
			argsEvaYL[1] = String.valueOf(l1set[i]);
			argsEvaYL[2] = String.valueOf(ssedYL[1]);// ssedYL[1]
			argsEvaYL[3] = String.valueOf(r);

			
			
			ExecutorService exec = Executors.newFixedThreadPool(2);
			double start = System.nanoTime();
			exec.execute(new Runnable() {
				// long aliceOut = 0;
				@Override
				public void run() {
					
					runGen.setParameter(argsGenUY);						
					runGen.run();
					try {
						dist1[0] = runGen.getOutputAlice();						
					} catch (Exception e) {
						
						e.printStackTrace();
					}

				}
			});
			exec.execute(new Runnable() {
				@Override
				public void run() {
					runEva.setParameter(argsEvaUY);					
					runEva.run();					
					
					/*if (Flag.CountTime)
						Flag.sw.print();
					if (Flag.countIO)
						runEva.printStatistic();*/
				}
			});
			System.out.println("exexexexexexexe11111111111111");
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
			
			ExecutorService exec2 = Executors.newFixedThreadPool(2);
			
			exec2.execute(new Runnable() {
				// long aliceOut = 0;
				@Override
				public void run() {

					runGen.setParameter(argsGenYL);
					runGen.run();

					try {
						dist2[0] = runGen.getOutputAlice();
						// System.out.println("2):"+dist2[0]);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});
			exec2.execute(new Runnable() {
				@Override
				public void run() {
					
					runEva.setParameter(argsEvaYL);					
					runEva.run();			

					/*if (Flag.CountTime)
						Flag.sw.print();
					if (Flag.countIO)
						runEva.printStatistic();*/
				}
			});
			System.out.println("exexexexexexexe2222222222222222222222");
			// Connection should be established within 60s
			exec2.shutdown();
			try {
				if (exec2.awaitTermination(60, TimeUnit.SECONDS)) {
					// Execution finished
					exec2.shutdownNow();
				}
			} catch (InterruptedException e) {
				// Something is wrong
				exec2.shutdownNow();
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}

			double end = System.nanoTime();
			time += end - start;
			

			dist[0] = AdditiveUtil.add(dist1[0], dist2[0]); // c1-w1 + c2-w2
			dist[1] = AdditiveUtil.add(dist1[1], dist2[1]); // w1 + w2

			SLB[0] = AdditiveUtil.add(SLB[0], dist[0]);
			SLB[1] = AdditiveUtil.add(SLB[1], dist[1]);

			/*
			 * System.out.println("dist:"+AdditiveUtil.add(dist[0], dist[1]));
			 * System.out.println("SLB:"+AdditiveUtil.add(SLB[0], SLB[1]));
			 */

			/*
			 * System.out.println("Verify BranchRes, AliceOut:" + aliceOut + " BobOut:" +
			 * bobOut); long branchRes = aliceOut; System.out.println("ssed UY:" +
			 * AdditiveUtil.add(ssedUY[0], ssedUY[1]));
			 * //System.out.println("ssedUY0:"+ssedUY[0]+" ssedUY1:"+ssedUY[1]);
			 * System.out.println("w1:"+w1);
			 * System.out.println("verify ssed:"+AdditiveUtil.add(w1, branchRes));
			 */

		}
		return SLB;
	}

	public static void main(String[] args) throws Exception {
		final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		final int port = 5554;
		// Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
		//System.out.println("Connection| hostname:port, " + hostname + ":" + port);

		final Server sndChannel = new Server();
		final Client rcvChannel = new Client();
		
		ConnectionHelper connector = new ConnectionHelper();
		connector.connect(hostname, port, sndChannel, rcvChannel);

		/*// Establish the connection
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
		}*/

		int queryLength = 128;

		ArrayList<MultiplicationTriple> mts = new ArrayList<MultiplicationTriple>();

		int mtsNum = 2 * queryLength;
		for (int i = 0; i < mtsNum; i++) {
			MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
			mts.add(mt);
		}

		System.out.println("finish generating MTs");
		ShareGenerator generator = new ShareGenerator(true);
		// u0
		long[] u0data = new long[queryLength];
		// u0data[0] = 10;
		// u0data[1] = 10;
		long[] u0sqdata = new long[queryLength];
		// u0sqdata[0] = 100;
		// u0sqdata[1] = 100;
		for (int i = 0; i < queryLength; i++) {
			u0data[i] = generator.generateRandom(true);
			u0sqdata[i] = AdditiveUtil.mul(u0data[i], u0data[i]);
		}
		SharedSequence U0 = new SharedSequence(queryLength, 0, 0,0, u0data, u0sqdata);
		// u1
		generator.generateSharedSequence(U0);
		SharedSequence U1 = generator.S1;

		// l0
		long[] l0data = new long[queryLength];
		// l0data[0] = 7;
		// l0data[1] = 7;
		long[] l0sqdata = new long[queryLength];
		// l0sqdata[0] = 49;
		// l0sqdata[1] = 49;
		for (int i = 0; i < queryLength; i++) {
			l0data[i] = generator.generateRandom(true);
			l0sqdata[i] = AdditiveUtil.mul(l0data[i], l0data[i]);
		}
		SharedSequence L0 = new SharedSequence(queryLength, 0, 0,0, l0data, l0sqdata);
		// l1
		generator.generateSharedSequence(L0);
		SharedSequence L1 = generator.S1;

		// y0
		long[] y0data = new long[queryLength];
		 //y0data[0] = 3;
		// y0data[1] = 4;
		long[] y0sqdata = new long[queryLength];
		 //y0sqdata[0] = 9;
		 //y0sqdata[1] = 16;
		for (int i = 0; i < queryLength; i++) {
			y0data[i] = generator.generateRandom(true);
			y0sqdata[i] = AdditiveUtil.mul(y0data[i], y0data[i]);
		}
		SharedSequence Y0 = new SharedSequence(queryLength, 0, 0,0, y0data, y0sqdata);
		// System.out.println("1) Y0:" + String.valueOf(Y0.getSharedData(0)[0]) + " " +
		// Y0.getSharedData(0)[1]);

		// y1
		generator.generateSharedSequence(Y0);
		SharedSequence Y1 = generator.S1;

		System.out.println("y:" + AdditiveUtil.add(Y0.getSharedSequence()[0], Y1.getSharedSequence()[0]));
		System.out.println("u:" + AdditiveUtil.add(U0.getSharedSequence()[0], U1.getSharedSequence()[0]));
		System.out.println("l:" + AdditiveUtil.add(L0.getSharedSequence()[0], L1.getSharedSequence()[0]));

		double s = System.nanoTime();
		SLB slb = new SLB(sndChannel, rcvChannel, queryLength);

		long[] SLB = 
				//slb.computeConcurrent(sndChannel, rcvChannel, mts, U0, U1, L0, L1, Y0, Y1);
				slb.compute(mts, U0, U1, L0, L1, Y0, Y1, queryLength);
		double e = System.nanoTime();
		System.out.println("SLB running time:"+(e-s)/1e9);
		System.out.println("thread running time:"+(slb.time)/1e9);
		
		System.out.println("SLB:" + AdditiveUtil.add(SLB[0], SLB[1]));
		sndChannel.disconnect();
		rcvChannel.disconnect();
	}
}
