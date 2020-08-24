package gadgetBoolean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MulP0;
import additive.MulP1;
import additive.MultiplicationTriple;
import additive.SeqCompEngine;
import additive.SeqCompEngine2;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import flexSC.network.Client;
import flexSC.network.Server;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

public class SBranch2PC {

	private Server sndChannel;
	private Client rcvChannel;
	private int port;
	private String hostname;
	private int portCli;

	public double bandwidth = 0;

	private static final long MODULUS = Constants2PC.MODULUS; // 2^31

	public SBranch2PC() {

	}

	public SBranch2PC(int port, int portCli, String hostname) {
		this.port = port;
		this.hostname = hostname;
		this.portCli = portCli;
	}

	/**
	 * SBranch(m1, m2, c1,c2, omega)
	 * 
	 * @param isDisconnect
	 * @param sndChannel
	 * @param rcvChannel
	 * @param mt2
	 * @param mt
	 * @param m1_0
	 * @param m2_0
	 * @param m1_1
	 * @param m2_1
	 * @param c_0
	 * @param c_1
	 * @param omega
	 * @return
	 * @throws Exception
	 */
	public long[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			MultiplicationTriple mt, long m1_0, long m2_0, long m1_1, long m2_1, long c1_0, long c2_0, long c1_1, 
			long c2_1, long omega) throws Exception {

		long[] dist = new long[2];

		SCMP2PC scmp = new SCMP2PC();

		long[] z = scmp.compute(isDisconnect, sndChannel, rcvChannel, mt2, mt, m1_0, m2_0, m1_1, m2_1);
		//bandwidth scmp
		this.bandwidth += scmp.bandwidth;
		
		long z0 = z[0];
		long z1 = z[1];
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {
				MulP0 mulP0 = new MulP0();
				long z0s1 = AdditiveUtil.sub(0, z0);
				//SeqCompEngine(mt, z0s1, c1_0, z1s1, c1_1); z0s1 mul c1_0
				mulP0.compute(isDisconnect, sndChannel, mt, z0s1, c1_0);
				long zMUL1 = mulP0.z0;
				//SeqCompEngine(mt, z0, c2_0, z1, c2_1); z0 mul c2_0
				mulP0.compute(isDisconnect, sndChannel, mt, z0, c2_0);
				long zMUL2 = mulP0.z0;
				long dist0 = AdditiveUtil.sub( AdditiveUtil.add(zMUL1, zMUL2), omega);
				dist[0] = dist0;
				}
			});
		
		exec.execute(new Runnable() {
			@Override
			public void run(){
				MulP1 mulP1 = new MulP1();
				long z1s1 = AdditiveUtil.sub(1, z1);
				//long z1s1 = AdditiveUtil.sub(MODULUS-1, z1);
				//SeqCompEngine(mt, z0s1, c1_0, z1s1, c1_1); z1s1 mul c1_1
				mulP1.compute(isDisconnect, rcvChannel, mt, z1s1, c1_1);
				long zMUL1 = mulP1.z1;
				//SeqCompEngine(mt, z0, c2_0, z1, c2_1); z1 mul c2_1
				mulP1.compute(isDisconnect, rcvChannel, mt, z1, c2_1);
				long zMUL2 = mulP1.z1;
				long dist1 = AdditiveUtil.add( AdditiveUtil.add(zMUL1, zMUL2), omega);
				dist[1] = dist1;
			}
		});
		
		
		//SeqCompEngine engine_1 = new SeqCompEngine(isDisconnect, sndChannel, rcvChannel, mt, z0s1, c1_0, z1s1, c1_1);
		//this.bandwidth += engine_1.bandwidth;
		
		//SeqCompEngine engine_2 = new SeqCompEngine(isDisconnect, sndChannel, rcvChannel, mt, z0, c2_0, z1, c2_1);
		//this.bandwidth += engine_2.bandwidth;
		
		//long dist0 = AdditiveUtil.sub( AdditiveUtil.add(engine_1.z0, engine_2.z0), omega);
		//long dist1 = AdditiveUtil.add( AdditiveUtil.add(engine_1.z1, engine_2.z1), omega);

		exec.shutdown();		
		try {
			if (exec.awaitTermination(1, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();

				if (isDisconnect == true) {

					// System.out.println("SLB disconnecting...");
					rcvChannel.disconnectCli();
					sndChannel.disconnectServer();

				}
			}
		} catch (InterruptedException e) {
			// Something is wrong
			System.out.println("Unexpected interrupt");
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		
		this.bandwidth += (rcvChannel.cos.getByteCount() + rcvChannel.cis.getByteCount());
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		

		return dist;
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
		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		ShareGenerator generator = new ShareGenerator(true);

		// generate two shares
		long m1 = 100;//u
		long m2 = 14;//y

		long c1 = 9;
		long c2 = 0;

		long omega = generator.generateRandom();
		System.out.println("omega(random value):" + omega);

		generator.generateSharedDataPoint(m1, true);
		long m1_0 = generator.x0;
		long m1_1 = generator.x1;

		System.out.println("m1:" + m1 + " m1_0:" + m1_0 + " m1_1:" + m1_1 + " verify:" + AdditiveUtil.add(m1_0, m1_1));

		generator.generateSharedDataPoint(m2, true);
		long m2_0 = generator.x0;
		long m2_1 = generator.x1;

		System.out.println("m2:" + m2 + " m2_0:" + m2_0 + " m2_1:" + m2_1 + " verify:" + AdditiveUtil.add(m2_0, m2_1));

		generator.generateSharedDataPoint(c1, true);
		long c1_0 = generator.x0;
		long c1_1 = generator.x1;
		System.out.println("c1:" + c1 + " c1_0:" + c1_0 + " c1_1:" + c1_1 + " verify:" + AdditiveUtil.add(c1_0, c1_1));

		generator.generateSharedDataPoint(c2, true);
		long c2_0 = generator.x0;
		long c2_1 = generator.x1;
		System.out.println("c2:" + c2 + " c2_0:" + c2_0 + " c2_1:" + c2_1 + " verify:" + AdditiveUtil.add(c2_0, c2_1));

		SBranch2PC sbranch = new SBranch2PC();		
		
		int round = 1; //10^7
		double time=0.0;
		for(int i=0; i<round; i++) {
			double e = System.nanoTime();		
			long[] dist = sbranch.compute(false, sndChannel, rcvChannel, mt2, mt, m1_0, m2_0, m1_1, m2_1, c1_0,c2_0, c1_1, 
					c2_1, omega);
			double s = System.nanoTime();
			time += s-e;
			
			// verify
			long dist0 = dist[0];
			long dist1 = dist[1];
			System.out.println("dist:" + AdditiveUtil.add(dist0, dist1) + " dist0:" + dist0 + " dist1:" + dist1);

			
			if(i%1000==0) {
				System.out.println("Progress:"+i);
			}		
		}

		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + sbranch.bandwidth/1024.0/1024.0/1024.0 + " GB");
		
		
		sndChannel.disconnect();
		rcvChannel.disconnect();
	}
}
