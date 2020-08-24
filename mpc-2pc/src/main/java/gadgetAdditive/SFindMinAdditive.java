package gadgetAdditive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.MultiplicationTriple2;
import additive.ShareGenerator;
import flexSC.network.Client;
import flexSC.network.Server;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

public class SFindMinAdditive {

	private Server sndChannel;
	private Client rcvChannel;
	private int port;
	private String hostname;
	private int portCli;

	public double bandwidth = 0;

	public SFindMinAdditive() {

	}

	public SFindMinAdditive(int port, int portCli, String hostname) {
		this.port = port;
		this.hostname = hostname;
		this.portCli = portCli;
	}

	public long[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple2 mt2,
			MultiplicationTriple mt, long a_0, long b_0, long c_0, long a_1, long b_1, long c_1, long omega1,
			long omega2) throws Exception {
		long[] dmin = new long[2];

		SBranchAdditive sbranch = new SBranchAdditive();

		long[] d1 = sbranch.compute(isDisconnect, sndChannel, rcvChannel, mt2, mt, a_0, b_0, a_1, b_1, a_0, b_0, a_1,
				b_1, omega1);
		long d1_0 = d1[0];
		long d1_1 = d1[1];

		dmin = sbranch.compute(isDisconnect, sndChannel, rcvChannel, mt2, mt, d1_0, c_0, d1_1, c_1, d1_0, c_0, d1_1,
				c_1, omega2);

		this.bandwidth += sbranch.bandwidth;

		return dmin;
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

		MultiplicationTriple2 mt2 = new MultiplicationTriple2(sndChannel, rcvChannel);
		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		ShareGenerator generator = new ShareGenerator(true);

		// generate three shares
		long a = 70;// a
		long b = 14;// b
		long c = 30;// c

		long omega1 = generator.generateRandom();
		System.out.println("omega1 (random value):" + omega1);

		long omega2 = generator.generateRandom();
		System.out.println("omega2 (random value):" + omega2);

		generator.generateSharedDataPoint(a, true);
		long a_0 = generator.x0;
		long a_1 = generator.x1;

		System.out.println("a:" + a + " a_0:" + a_0 + " a_1:" + a_1 + " verify:" + AdditiveUtil.add(a_0, a_1));

		generator.generateSharedDataPoint(b, true);
		long b_0 = generator.x0;
		long b_1 = generator.x1;

		System.out.println("b:" + b + " b_0:" + b_0 + " b_1:" + b_1 + " verify:" + AdditiveUtil.add(b_0, b_1));

		generator.generateSharedDataPoint(c, true);
		long c_0 = generator.x0;
		long c_1 = generator.x1;
		System.out.println("c:" + c + " c_0:" + c_0 + " c_1:" + c_1 + " verify:" + AdditiveUtil.add(c_0, c_1));

		SFindMinAdditive sfindmin = new SFindMinAdditive();
		
		int round = 1000000; //10^6
		double time=0.0;
		for(int i=0; i<round; i++) {
			double e = System.nanoTime();				
			long[] dmin = sfindmin.compute(false, sndChannel, rcvChannel, mt2, mt, a_0, b_0, c_0, a_1, b_1, c_1, omega1,
				omega2);
			double s = System.nanoTime();
			time += s-e;
			if(i%1000==0) {
				System.out.println("Progress:"+i);
			}		
		}
		//System.out.println("bandwidth:" + sfindmin.bandwidth + " B");
		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + sfindmin.bandwidth/1024.0/1024.0/1024.0 + " GB");

		// verify
		//long dmin0 = dmin[0];
		//long dmin1 = dmin[1];
		//System.out.println("dmin:" + AdditiveUtil.add(dmin0, dmin1) + " dmin0:" + dmin0 + " dmin1:" + dmin1);

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

}
