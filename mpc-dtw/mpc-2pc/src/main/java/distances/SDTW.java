package distances;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.util.Arrays;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import additive.SharedSequence;
import common.util.Config;
import common.util.Constants;
import flexSC.flexsc.CompEnv;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgets.SFindMinGadget;

import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SDTW implements Distance {
	private long[] SDTW;
	private long[] a;
	private long[] b;
	private long[] c;
	private long[] min;

	private long[] SSED;
	private long[][] cost;
	private long[][] costPrev;
	private Server sndChannel;
	private Client rcvChannel;
	private final int queryLength;
	private final int cr; // flooring(percent * queryLength)
	private final int lenArr;
	private long aliceOut;
	private long bobOut;
	
	
	public double time;
	public long bandwidthGC;
	public long bandwidthSSED;
	
	private static int gcPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);// 50000
	private static int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);//

	public SDTW(Server sndChannel, Client rcvChannel, int queryLength, int cr) {
		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;
		this.queryLength = queryLength;
		this.cr = cr;

		this.SDTW = new long[2];
		this.a = new long[2];
		this.b = new long[2];
		this.c = new long[2];
		this.min = new long[2];

		this.SSED = new long[2];

		lenArr = 2 * cr + 1;//15
		this.cost = new long[2][lenArr];
		this.costPrev = new long[2][lenArr];

		// initialize a, b, c with Integer.MAX_VALUE
		a[0] = Integer.MAX_VALUE; //infinite
		a[1] = 0L;
		b[0] = Integer.MAX_VALUE;
		b[1] = 0L;
		c[0] = Integer.MAX_VALUE;
		c[1] = 0L;
		
		for (int i = 0; i < lenArr; i++) {
			cost[0][i] = Integer.MAX_VALUE;
			cost[1][i] = 0L;
			costPrev[0][i] = Integer.MAX_VALUE;
			costPrev[1][i] = 0L;
		}

	}

	@Override
	public long[] compute( ArrayList<MultiplicationTriple> mts,
			SharedSequence[] sequences, int queryLength) {
		SharedSequence X0 = sequences[0];
		SharedSequence X1 = sequences[1];
		SharedSequence Y0 = sequences[2];
		SharedSequence Y1 = sequences[3];

		try {
			return this.compute(sndChannel, rcvChannel, mts, X0, X1, Y0, Y1, queryLength);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		return new long[2];
	}

	public long[] compute(Server sndChannel, Client rcvChannel, ArrayList<MultiplicationTriple> mts, SharedSequence X0,
			SharedSequence X1, SharedSequence Y0, SharedSequence Y1, int queryLength) throws Exception {
		
		// verify
		if (queryLength != this.queryLength) {
			System.out.println("Wrong sequence length.");
			System.exit(0);
		}

		// SSED
		SSED ssed = new SSED();

		// Random Generator with given random instance
		ShareGenerator generator = new ShareGenerator(true);


		// retrieve data
		long[] x0set = X0.getSharedSequence();
		long[] sqx0set = X0.getSharedSquareSequence();

		long[] x1set = X1.getSharedSequence();
		long[] sqx1set = X1.getSharedSquareSequence();

		long[] y0set = Y0.getSharedSequence();
		long[] sqy0set = Y0.getSharedSquareSequence();

		long[] y1set = Y1.getSharedSequence();
		long[] sqy1set = Y1.getSharedSquareSequence();

		int k = 0;
		for (int i = 0; i < queryLength; i++) {
			// choose k
			k = Integer.max(0, cr - i);
			
			for (int j = Integer.max(0, i - cr); j <= Integer.min(queryLength - 1, i + cr); j++,k++) {
				
				if (ConnectionHelper.available(arithmeticPort) == false) {
					System.out.println("aithmeticPort in use:" + arithmeticPort);
				}
				if (ConnectionHelper.available(gcPort) == false) {
					System.out.println("gcPort in use:" + gcPort);
				}
				
				// retrive mt
				MultiplicationTriple mt = mts.get(0);
				mts.remove(0);
				//System.out.println("compute SSED of (i,j) :"+i+","+j);
				// Initialize all row and column
				if (i == 0 && j == 0) {
					
					SSED = ssed.compute(false, sndChannel, rcvChannel, mt, x0set[0], y0set[0], x1set[0], y1set[0], sqx0set[0],
							sqy0set[0], sqx1set[0], sqy1set[0]);
					// store at cost[][k]
					cost[0][k] = SSED[0];
					cost[1][k] = SSED[1];
					//System.out.println("k:"+k+" (i,j):" + i + "," + j + " ssed:" + AdditiveUtil.add(SSED[0], SSED[1]));
					bandwidthSSED = ssed.bandwidth;

					continue;
				}
				
				if (j >= 1 && k >= 1) {
					b[0] = cost[0][k - 1];
					b[1] = cost[1][k - 1];
					
				}else {
					b[0] = Integer.MAX_VALUE;//infinite
					b[1] = 0L;
					
				}
				
				if (i >= 1 && k + 1 <= 2 * cr) {
					a[0] = costPrev[0][k + 1];
					a[1] = costPrev[1][k + 1];
				}else {
					a[0] = Integer.MAX_VALUE;
					a[1] = 0L;
					
				}
				
				
				if (i >= 1 && j >= 1) {
					c[0] = costPrev[0][k];
					c[1] = costPrev[1][k];
				}else {
					c[0] = Integer.MAX_VALUE;
					c[1] = 0L;
				}

				//-----------------------------SSED-------------------------------
				// ssed(xi, yj)
				SSED = ssed.compute(false, sndChannel, rcvChannel, mt, x0set[i], y0set[j], x1set[i], y1set[j], sqx0set[i],
						sqy0set[j], sqx1set[i], sqy1set[j]);
				//System.out.println("(i,j):" + i + "," + j + " ssed:" + AdditiveUtil.add(SSED[0], SSED[1]));
				
				bandwidthSSED = ssed.bandwidth;
				
				
				
				// SFindMin(a, b, c)
				long r = generator.generateRandom(true); // random r \in [0, 2^31-1] for masking min

				// prepare parameters for GC
				// Alice - generator
				final String[] argsGen = new String[4];// <A>_0, <B>_0, <C>_0, r
				argsGen[0] = String.valueOf(a[0]);
				argsGen[1] = String.valueOf(b[0]);
				argsGen[2] = String.valueOf(c[0]);
				argsGen[3] = String.valueOf(r);

				// Bob - evaluator
				final String[] argsEva = new String[3];// <A>_1, <B>_1, <C>_1
				argsEva[0] = String.valueOf(a[1]);
				argsEva[1] = String.valueOf(b[1]);
				argsEva[2] = String.valueOf(c[1]);

 
				
				//-----------------GC-----------------------
				//final int port = gcPort + (i+1)*(j+1);
				//Thread.sleep(5000);
				GenRunnable<Long> runGen = (GenRunnable) new SFindMinGadget.Generator<Long>();
				EvaRunnable<Long> runEva = (EvaRunnable) new SFindMinGadget.Evaluator<Long>();
				
				// Start two threads for GC
				ExecutorService exec = Executors.newFixedThreadPool(2);
				exec.execute(new Runnable() {

					@Override
					public void run() {
						// generator						
						/*GenRunnable<Long> runGen = (GenRunnable) new SFindMinGadget.Generator<Long>();
						runGen.setParameter(argsGen);
						runGen.run();*/
						runGen.setInput(argsGen);
						runGen.setConnection(gcPort);						
						CompEnv<Long> env = runGen.connect();
						runGen.run(env);

						// get res = min - r
						try {
							aliceOut = runGen.getOutputAlice();
						} catch (Exception e) {
							System.out.println("[SFindMin Gadget] get alice's output wrong.");
							e.printStackTrace();
						}
						

					}

				});

				exec.execute(new Runnable() {

					@Override
					public void run() {
						// evaluator
						/*EvaRunnable<Long> runEva = (EvaRunnable) new SFindMinGadget.Evaluator<Long>();
						runEva.setParameter(argsEva);
						runEva.run();*/
						
						runEva.setInput(argsEva);
						runEva.setConnection(gcPort);
						CompEnv<Long> env = runEva.connect();
						runEva.run(env);
						
						// get bob's output to verify
						try {
							bobOut = runEva.getOutputBob();
						} catch (Exception e) {
							System.out.println("[SFindMin Gadget] get bob's output wrong.");
							e.printStackTrace();
						}

					}

				});

				// prepare shutdown threads
				exec.shutdown();
				// Connection should be established within 60s
				try {
					if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
						// Execution finished
						//System.out.println("disconnecting..."+i+j);
						exec.shutdownNow();
						runEva.disconnection();
						runGen.disconnection();
						
					}else {
						System.out.println("SDTW| Something wrong");
						System.exit(0);
					}
				} catch (InterruptedException e) {
					// Something is wrong
					exec.shutdownNow();
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				
				bandwidthGC += (runEva.cos.getByteCount() + runEva.cis.getByteCount());

				// verify if aliceOut == bobOut
				if (aliceOut != bobOut) {
					System.out.println("[SFindMin Gadget] GC output wrong.");
					System.exit(0);
				}

				// set min = GC's res, random
				min[0] = aliceOut;
				min[1] = r;
				
				

				cost[0][k] = AdditiveUtil.add(min[0], SSED[0]);
				cost[1][k] = AdditiveUtil.add(min[1], SSED[1]);
				
				/*System.out.println("a:" + AdditiveUtil.add(a[0], a[1]) + " b:" + AdditiveUtil.add(b[0], b[1]) + " c:"
						+ AdditiveUtil.add(c[0], c[1]) + " min:" + AdditiveUtil.add(min[0], min[1])+" D:"+AdditiveUtil.add(cost[0][k], cost[1][k]));
				*/
				
			}

			// move cost to costPrev
			long[][] temp = new long[2][lenArr];
			temp[0] = Arrays.clone(cost[0]);
			temp[1] = Arrays.clone(cost[1]);

			cost[0] = Arrays.clone(costPrev[0]);
			cost[1] = Arrays.clone(costPrev[1]);

			costPrev[0] = Arrays.clone(temp[0]);
			costPrev[1] = Arrays.clone(temp[1]);

		}

		k--;
		SDTW[0] = costPrev[0][k];
		SDTW[1] = costPrev[1][k];

		return SDTW;
	}

	public static void main(String[] args) throws Exception {
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
		int cr = 25;

		ShareGenerator generator = new ShareGenerator(true);
		
		ArrayList<MultiplicationTriple> mts = new ArrayList<MultiplicationTriple>();

		int mtsNum = (2 * cr + 1) * queryLength;
		for (int i = 0; i < mtsNum; i++) {
			MultiplicationTriple mt = new MultiplicationTriple(generator, sndChannel, rcvChannel);
			mts.add(mt);
			System.out.println("mt:"+i);
		}

		System.out.println("finish generating MTs");
		
		// x0
		long[] x0data = new long[queryLength];
		/*x0data[0] = 1;
		x0data[1] = 1;
		x0data[2] = 1;
		x0data[3] = 2;
		x0data[4] = 3;*/
		long[] x0sqdata = new long[queryLength];
		/*x0sqdata[0] = 1;
		x0sqdata[1] = 1;
		x0sqdata[2] = 1;
		x0sqdata[3] = 4;
		x0sqdata[4] = 9;*/

		
		  for (int i = 0; i < queryLength; i++) { x0data[i] =
		  generator.generateRandom(true); x0sqdata[i] = AdditiveUtil.mul(x0data[i],
		  x0data[i]); }
		 
		SharedSequence X0 = new SharedSequence(queryLength, 0, 0, 0, x0data, x0sqdata);
		// u1
		generator.generateSharedSequence(X0);
		SharedSequence X1 = generator.S1;

		// y0
		long[] y0data = new long[queryLength];
		/*y0data[0] = 4;
		y0data[1] = 4;
		y0data[2] = 5;
		y0data[3] = 6;
		y0data[4] = 7;*/

		long[] y0sqdata = new long[queryLength];
		/*y0sqdata[0] = 16;
		y0sqdata[1] = 16;
		y0sqdata[2] = 25;
		y0sqdata[3] = 36;
		y0sqdata[4] = 49;*/

		
		  for (int i = 0; i < queryLength; i++) { y0data[i] =
		  generator.generateRandom(true); y0sqdata[i] = AdditiveUtil.mul(y0data[i],
		  y0data[i]); }
		 
		SharedSequence Y0 = new SharedSequence(queryLength, 0, 0,0, y0data, y0sqdata);
		// System.out.println("1) Y0:" + String.valueOf(Y0.getSharedData(0)[0]) + " " +
		// Y0.getSharedData(0)[1]);

		// y1
		generator.generateSharedSequence(Y0);
		SharedSequence Y1 = generator.S1;

		System.out.println("y:" + AdditiveUtil.add(Y0.getSharedSequence()[0], Y1.getSharedSequence()[0]));
		System.out.println("x:" + AdditiveUtil.add(X0.getSharedSequence()[0], X1.getSharedSequence()[0]));

		double s = System.nanoTime();
		SDTW sdtw = new SDTW(sndChannel, rcvChannel, queryLength, cr);

		// sndChannel, rcvChannel, mts, X0, X1, Y0, Y1, queryLength
		long[] SDTW = sdtw.compute(sndChannel, rcvChannel, mts, X0, X1, Y0, Y1, queryLength);
		double e = System.nanoTime();
		System.out.println("SDTW running time:" + (e - s) / 1e9);
		System.out.println("thread running time:" + (sdtw.time) / 1e9);

		System.out.println("SDTW:" + AdditiveUtil.add(SDTW[0], SDTW[1]));
		sndChannel.disconnect();
		rcvChannel.disconnect();
	}
}
