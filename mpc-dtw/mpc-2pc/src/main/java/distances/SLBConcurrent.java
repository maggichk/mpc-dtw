package distances;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import additive.SharedSequence;
import flexSC.flexsc.CompEnv;
import gadgets.SBranchGadget;
import flexSC.network.Client;
import flexSC.network.Server;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SLBConcurrent implements Distance{
	private long[] dist1;
	private long[] dist2;
	private long[] SLB;
	private double time;
	private ShareGenerator generator;	
	private int queryLength;
	
	public long bandwidth;
	
	//private int checkPort =0;
	
	
	public SLBConcurrent(int queryLength) {		
		this.dist1 = new long[2];
		this.dist2 = new long[2];
		this.SLB = new long[2];		
		this.generator = new ShareGenerator(true);		
		this.queryLength = queryLength;
	}

	
	
	
	@Override
	public long[] compute(ArrayList<MultiplicationTriple> mts,
			SharedSequence[] sequences, int queryLength) {
		SharedSequence U0 = sequences[0];
		SharedSequence U1 = sequences[1];
		SharedSequence L0 = sequences[2];
		SharedSequence L1 = sequences[3];
		SharedSequence Y0 = sequences[4];
		SharedSequence Y1 = sequences[5];
		return this.computeConcurrent(mts, U0, U1, L0, L1, Y0, Y1);
	}
	
	public long[] computeConcurrent(ArrayList<MultiplicationTriple> mts, SharedSequence U0,
			SharedSequence U1, SharedSequence L0, SharedSequence L1, SharedSequence Y0, SharedSequence Y1) {		
		
		//retrieve sequences
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
		
		int gcPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);
		int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
		final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		
		
		//establish tasks
		List<Runnable> taskList = new ArrayList<Runnable>();
		int counter = 0;
		while(counter < queryLength) {
			//System.out.println("counter:"+counter);
			final int portUY = gcPort+counter;	
			//System.out.println("counter:"+counter+" portUY:"+portUY);
			final int portYL = gcPort+counter+queryLength;
			//System.out.println("counter:"+counter+" portYL:"+portYL);
			
			final int arithmeticPortUY = arithmeticPort + counter;
			//System.out.println("counter:"+counter+" arithPortUY:"+arithmeticPortUY);
			final int arithmeticPortLY = arithmeticPort + counter + queryLength;
			//System.out.println("counter:"+counter+" arithPortLY:"+arithmeticPortLY);
			
			//System.out.println("counter:"+counter);
			final int portUYCli = gcPort+counter+2*queryLength;	
			//System.out.println("counter:"+counter+" portUY:"+portUYCli);
			final int portYLCli = gcPort+counter+queryLength+2*queryLength;
			//System.out.println("counter:"+counter+" portYL:"+portYLCli);
			
			final int arithmeticPortUYCli = arithmeticPort + counter+2*queryLength;
			//System.out.println("counter:"+counter+" arithPortUY:"+arithmeticPortUY);
			final int arithmeticPortLYCli = arithmeticPort + counter + queryLength+2*queryLength;
			//System.out.println("counter:"+counter+" arithPortLY:"+arithmeticPortLY);
			
			
			// mt
			final MultiplicationTriple mt1 = mts.get(0);
			mts.remove(0);

			final MultiplicationTriple mt2 = mts.get(0);
			mts.remove(0);
			
			
			//retrieve y, u, l
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
					
					//random w1
					long w1 = generator.generateRandom(true);
					dist1[1] = AdditiveUtil.add(w1, dist1[1]);
					
					
					
					//SSED
					try {
						ssedUY = ssedConcurrent(ssed, mt1,  u0,  y0,  u1,  y1,  squ0, sqy0,  squ1,  sqy1);
					} catch (Exception e) {
						
						e.printStackTrace();
					}
					
					//System.out.println("verify:"+AdditiveUtil.add(ssedUY[0], ssedUY[1]));
					
					//GC
					//GC args
					// sbranch(y, u)
					// IF U < Y, SELECT SSED; IF U > Y, SELECT R.
					String[] argsGenUY = new String[5];
					argsGenUY[0] = String.valueOf(u0);// <u>_0, <y>_0, <c1>_0, <r>_0, w1
					argsGenUY[1] = String.valueOf(y0);
					argsGenUY[2] = String.valueOf(ssedUY[0]); // ssedUY[0]
					argsGenUY[3] = String.valueOf(r);
					argsGenUY[4] = String.valueOf(w1);

					String[] argsEvaUY = new String[4];// <u>_1, <y>_1, <c1>_1, <r>_1
					argsEvaUY[0] = String.valueOf(u1);
					argsEvaUY[1] = String.valueOf(y1);
					argsEvaUY[2] = String.valueOf(ssedUY[1]);// ssedUY[1]
					argsEvaUY[3] = String.valueOf(r);
					
					int selectionBit =1;
					gcConcurrent(portUY, portUYCli, argsGenUY, argsEvaUY, selectionBit);
					
				}
				
			};
			taskList.add(taskUY);
			
			Runnable taskYL = new Runnable() {									
				
				@Override
				public void run() {
					SSED ssed = new SSED(arithmeticPortLY,arithmeticPortLYCli, hostname);
					
					
					long[] ssedYL = new long[2];
					long gcRes = 0L;
					
					//random w2
					long w2 = generator.generateRandom(true);
					dist2[1] = AdditiveUtil.add(w2, dist2[1]);
					
					//SSED
					try {
						ssedYL = ssedConcurrent(ssed, mt2,  y0,  l0,  y1,  l1,  sqy0, sql0,  sqy1,  sql1);
					} catch (Exception e) {
						
						e.printStackTrace();
					}
					//System.out.println("verify ssedYL:"+AdditiveUtil.add(ssedYL[0], ssedYL[1]));
					//GC
					//sbranch(y,l)
					// IF Y < L, SELECT SSED; IF Y >L, SELECT R.
					final String[] argsGenYL = new String[5];
					argsGenYL[0] = String.valueOf(y0);// <y>_0, <yl>_0, <c2>_0, <r>_0, w2
					argsGenYL[1] = String.valueOf(l0);
					argsGenYL[2] = String.valueOf(ssedYL[0]); // ssedyl[0]
					argsGenYL[3] = String.valueOf(r);
					argsGenYL[4] = String.valueOf(w2);

					final String[] argsEvaYL = new String[4];// <y>_1, <l>_1, <c2>_1, <r>_1
					argsEvaYL[0] = String.valueOf(y1);
					argsEvaYL[1] = String.valueOf(l1);
					argsEvaYL[2] = String.valueOf(ssedYL[1]);// ssedYL[1]
					argsEvaYL[3] = String.valueOf(r);
					
					int selectionBit =2; 
					gcConcurrent(portYL, portYLCli, argsGenYL, argsEvaYL, selectionBit);
					
					
				}
				
			};
			taskList.add(taskYL);
			
			
			counter++;
		}
		
		
		//establish threads
		ExecutorService exec = Executors.newFixedThreadPool(queryLength*2);
		for(int i = 0; i<taskList.size(); i++) {
			exec.submit(taskList.get(i));
		}
		//shutdown
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
		
		return SLB;
	}
	
	public void gcConcurrent( int port,  int portCli, final String[] argsGen, final String[] argsEva, final int selectionBit) {
		int checkPort = port;
		
		/*int backupPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_BACKUP_PORT);//20000	
		
		
		if (!ConnectionHelper.available(port)) {
			for (int i = 0; i < 128; i++) {
				if (ConnectionHelper.available(backupPort + i)) {
					checkPort = backupPort+i;					
					break;
				}
			}
		}
		
		if(!ConnectionHelper.available(checkPort)) {			
			System.out.println("1) backup port in use:"+checkPort);
			System.exit(0);
		}*/
		final int serverPort = checkPort;
		final int cliPort = checkPort + (portCli - port);
		//System.out.println("GC Connection| portServer:"+serverPort+", portClient:"+cliPort);
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		//initialize GC		
		@SuppressWarnings("rawtypes")
		final GenRunnable runGen = (GenRunnable) new SBranchGadget.Generator<Long>();		
		@SuppressWarnings("rawtypes")
		final EvaRunnable runEva = (EvaRunnable) new SBranchGadget.Evaluator<Long>();
		
		//Alice
		exec.execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				
				runGen.setInput(argsGen);
				runGen.setConnection(serverPort);					
				CompEnv<Long> env = runGen.connect();
				runGen.run(env);
				
				try {
					if(selectionBit == 1) {
						//branch(U,Y)
						dist1[0] = AdditiveUtil.add(dist1[0], runGen.getOutputAlice());
					}else if(selectionBit == 2) {
						//branch(Y,L)
						dist2[0] = AdditiveUtil.add(dist2[0], runGen.getOutputAlice());
					}
					
				} catch (Exception e) {					
					e.printStackTrace();
				}
				
			}
			
		});
		
		//Bob
		exec.execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				runEva.setInput(argsEva);
				runEva.setConnection(cliPort);
				//System.out.println("serverPort:"+serverPort+" cliPort:"+cliPort);
				CompEnv<Long> env = runEva.connect(serverPort, cliPort);
				runEva.run(env);
							
			}			
		});
		
		//shutdown threads
		// Connection should be established within 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
				//System.out.println("SLB-GC| disconnecting....serverPort:"+serverPort);
				runEva.disconnection();	
				runGen.disconnection();
			}else {
				System.out.println("SLB-GC| somthing wrong");
				
			}
		} catch (InterruptedException e) {
			// Something is wrong
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		/*try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		bandwidth += (runEva.cos.getByteCount()+runEva.cis.getByteCount());
	}
	
	public long[] ssedConcurrent(SSED ssed, MultiplicationTriple mt1, long u0, long y0, long u1, long y1, long squ0,
			long sqy0, long squ1, long sqy1) throws Exception {	
				
		
		// ssed(u,y) -> c1
		long[] ssedUY = new long[2];
		// long u0, long y0, long u1, long y1, long squ0, long sqy0, long squ1, long
		// sqy1
		ssedUY = ssed.computeConcurrent( mt1, u0, y0, u1, y1, squ0,
				sqy0, squ1, sqy1);
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

		ArrayList<MultiplicationTriple> mts = new ArrayList<MultiplicationTriple>();

		int mtsNum = 2 * queryLength;
		for (int i = 0; i < mtsNum; i++) {
			MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
			mts.add(mt);
		}
		sndChannel.disconnect();
		rcvChannel.disconnect();

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
		// y0data[0] = 3;
		// y0data[1] = 4;
		long[] y0sqdata = new long[queryLength];
		// y0sqdata[0] = 9;
		// y0sqdata[1] = 16;
		for (int i = 0; i < queryLength; i++) {
			y0data[i] = generator.generateRandom(true);
			y0sqdata[i] = AdditiveUtil.mul(y0data[i], y0data[i]);
		}
		SharedSequence Y0 = new SharedSequence(queryLength, 0, 0, 0,y0data, y0sqdata);
		// System.out.println("1) Y0:" + String.valueOf(Y0.getSharedData(0)[0]) + " " +
		// Y0.getSharedData(0)[1]);

		// y1
		generator.generateSharedSequence(Y0);
		SharedSequence Y1 = generator.S1;

		System.out.println("y:" + AdditiveUtil.add(Y0.getSharedSequence()[0], Y1.getSharedSequence()[0]));
		System.out.println("u:" + AdditiveUtil.add(U0.getSharedSequence()[0], U1.getSharedSequence()[0]));
		System.out.println("l:" + AdditiveUtil.add(L0.getSharedSequence()[0], L1.getSharedSequence()[0]));

		double s = System.nanoTime();
		SLBConcurrent slb = new SLBConcurrent(queryLength);

		long[] SLB = 
				slb.computeConcurrent(mts, U0, U1, L0, L1, Y0, Y1);
				//slb.compute(sndChannel, rcvChannel, mts, U0, U1, L0, L1, Y0, Y1, queryLength);
		double e = System.nanoTime();
		System.out.println("SLB running time:"+(e-s)/1e9);
		System.out.println("thread running time:"+(slb.time)/1e9);
		
		System.out.println("SLB:" + AdditiveUtil.add(SLB[0], SLB[1]));
		
	}

	

}
