package gadgetBNN;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MulP0;
import additive.MulP1;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import booleanShr.ANDP0;
import booleanShr.ANDP1;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import flexSC.network.Client;
import flexSC.network.Server;
import flexSC.util.Utils;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

public class SecXnorPopcount {

	private Server sndChannel;
	private Client rcvChannel;

	public byte z0AND;
	public byte z1AND;

	public byte z0l = 0;
	public byte z1l = 0;

	public double bandwidth = 0.0;

	public long timeNetwork = 0;

	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0 };

	public long[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple mt,
			byte[] x_0_arr,  byte[] w_0_arr, byte[] x_1_arr, byte[] w_1_arr) throws Exception {
		long z[] = new long[2];
		// long z_0 = 0L;
		// long z_1 = 0L;

		long len_n = x_0_arr.length;

		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;

		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {

				for (int i = 0; i < len_n; i++) {
					byte xi_0 = x_0_arr[i];
					byte wi_0 = w_0_arr[i];

					// XNOR(x,w)
					byte ti_0 = BooleanUtil.xor(xi_0, wi_0);
					byte pi_0 = BooleanUtil.xor(ti_0, (byte) 0);// xor(*,i)

					// convert back to Z31
					long di_0 = pi_0;
					long ei_0 = 0;
					// SeqCompEngine( mt, d_0, e_0, d_1, e_1); d_0 mul e_0
					MulP0 mulP0 = new MulP0();
					mulP0.compute(isDisconnect, sndChannel, mt, di_0, ei_0);
					long zMUL1 = mulP0.z0;

					long z0lAddi = AdditiveUtil.sub(AdditiveUtil.add(di_0, ei_0), AdditiveUtil.mul(2L, zMUL1));

					// sum
					z[0] += z0lAddi;
				}
				z[0] = AdditiveUtil.mul(2L, z[0] )- len_n;
			}

		});

		exec.execute(new Runnable() {
			@Override
			public void run() {

				for (int i = 0; i < len_n; i++) {
					byte xi_1 = x_1_arr[i];
					byte wi_1 = w_1_arr[i];

					byte ti_1 = BooleanUtil.xor(xi_1, wi_1);
					byte pi_1 = BooleanUtil.xor(ti_1, (byte) 1);// xor(*,i)

					// convert back to Z31
					long di_1 = 0;
					long ei_1 = pi_1;
					// SeqCompEngine(mt, d_0, e_0, d_1, e_1); d_1 mul e_1
					MulP1 mulP1 = new MulP1();
					mulP1.compute(isDisconnect, rcvChannel, mt, di_1, ei_1);
					long zMUL1 = mulP1.z1;
					long z1lAddi = AdditiveUtil.sub(AdditiveUtil.add(di_1, ei_1), AdditiveUtil.mul(2L, zMUL1));
					z[1] += z1lAddi;
				}
				
				z[1] = AdditiveUtil.mul(2L, z[1]);

			}
		});

		// long st4 = System.nanoTime();
		// should be done with in 1s
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
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

		this.bandwidth += rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

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

		
		

		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		
		//byte[] x = {(byte)1, (byte)1, (byte)0, (byte)0, (byte)0};
		//byte[] w = {(byte)0, (byte)1, (byte)0, (byte)0, (byte)1};
		
		//test case in XONN
		byte[] x = {(byte)1, (byte)1, (byte)0, (byte)0};
		byte[] w = {(byte)0, (byte)1, (byte)0, (byte)0};
		
		byte[] x_0_arr = new byte[x.length];
		byte[] x_1_arr = new byte[x.length];
		
		byte[] w_0_arr = new byte[w.length];
		byte[] w_1_arr = new byte[w.length];
		
		for(int i=0; i<x.length;i++) {
			boolGen.generateSharedDataPoint(x[i], true);
			byte xi_0 = boolGen.x0;
			byte xi_1 = boolGen.x1;
			x_0_arr[i] = xi_0;
			x_1_arr[i] = xi_1;
			
			boolGen.generateSharedDataPoint(w[i], true);
			byte wi_0 = boolGen.x0;
			byte wi_1 = boolGen.x1;
			w_0_arr[i] = wi_0;
			w_1_arr[i] = wi_1;			
		}

		

		SecXnorPopcount xnorp = new SecXnorPopcount();
		int round = 1;
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {			
			long[] z = xnorp.compute(false, sndChannel, rcvChannel, mt, x_0_arr, w_0_arr, x_1_arr, w_1_arr);
			// verify
			long z0 = z[0];
			long z1 = z[1];
			System.out.println("z:" + AdditiveUtil.add(z0, z1) + " z0:" + z0 + " z1:" + z1);

			if (i % 1000 == 0) {
				System.out.println("Progress:" + i);
			}
		}
		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + xnorp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		System.out.println("timeNetwork:" + xnorp.timeNetwork / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

	
	
}
