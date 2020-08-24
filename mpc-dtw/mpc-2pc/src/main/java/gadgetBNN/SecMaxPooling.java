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
import booleanShr.BooleanANDEngineBatch;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import flexSC.network.Client;
import flexSC.network.Server;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

public class SecMaxPooling {

	private Server sndChannel;
	private Client rcvChannel;
	public byte z0AND;
	public byte z1AND;

	public byte a0 = 0;
	public byte a1 = 0;

	public double bandwidth = 0.0;
	public double timeNetwork = 0.0;

	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0 };

	public byte[] computeSingleVector(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			byte[] a_0_arr,  byte[] a_1_arr) throws Exception {
		
		
		byte a[] = new byte[2];
		
		//length of the vector is the size of pooling window.
		int len_n = a_0_arr.length;
		
		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;


		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {
				
				for(int i=0; i<len_n; i++) {
					a_0_arr[i] = BooleanUtil.xor(a_0_arr[i], (byte) 0);
				}
				
				//Boolean AND operation
				ANDP0 andP0 = new ANDP0();
				
				byte c_0 = a_0_arr[0];
				
				for (int i = 1; i < len_n; i++) {
					//System.out.println("c_0:"+c_0+" a_0_arr[i]:"+a_0_arr[i]);
					andP0.compute(isDisconnect, sndChannel, mt2, c_0, a_0_arr[i]);
					c_0 = andP0.z0;
					//System.out.println("c_0:"+c_0);
					//c_0 = BooleanUtil.and(c_0, a_0_arr[i]);
					timeNetwork += andP0.time;
				}
				
				//Boolean NOT(c_0)
				a0 = BooleanUtil.xor(c_0, (byte) 0); 
				a[0] = a0;

			}

		});

		exec.execute(new Runnable() {
			@Override
			public void run() {
				
				byte[] not_a_1_arr = new byte[4];
				for(int i=0; i<len_n; i++) {
					
					//a_1_arr[i] = BooleanUtil.xor(a_1_arr[i], (byte) 1);
					
					
					not_a_1_arr[i] = BooleanUtil.xor(a_1_arr[i], (byte) 1);					
					
					//System.out.println("[P1] not ai:"+not_a_1_arr[i] );
				}
								
				//Boolean AND operation
				ANDP1 andP1 = new ANDP1();
				
				//byte c_1 = a_1_arr[0];
				byte c_1 = not_a_1_arr[0];
				//System.out.println("[P1] x0:"+c_1);
				for (int i = 1; i < len_n; i++) {
					//System.out.println("[P1] xi:"+a_1_arr[i]+" c_1:"+c_1);
					//andP1.compute(isDisconnect, rcvChannel, mt2, c_1, a_1_arr[i]);
					andP1.compute(isDisconnect, rcvChannel, mt2, c_1, not_a_1_arr[i]);
					c_1 = andP1.z1;
					//System.out.println("[P1] c and a:"+c_1);
					//c_1 = BooleanUtil.and(c_1, a_1_arr[i]);
				}
				
				//Boolean NOT(c_1)
				a1 = BooleanUtil.xor(c_1, (byte) 1); 
				a[1] = a1;

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
		
		
		double st1 = System.nanoTime();
		this.bandwidth += rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		double et1 = System.nanoTime();
		timeNetwork += et1-st1;
		
		
		return a;
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

		//MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		//test case in XONN
				byte[] x = {(byte)0, (byte)0, (byte)0, (byte)0};
				//byte[] w = {(byte)0, (byte)1, (byte)0, (byte)0};
				
				byte[] x_0_arr = new byte[x.length];
				byte[] x_1_arr = new byte[x.length];
				
				//byte[] w_0_arr = new byte[w.length];
				//byte[] w_1_arr = new byte[w.length];
				
				for(int i=0; i<x.length;i++) {
					boolGen.generateSharedDataPoint(x[i], true);
					byte xi_0 = boolGen.x0;
					byte xi_1 = boolGen.x1;
					x_0_arr[i] = xi_0;
					x_1_arr[i] = xi_1;
					
					System.out.println("xi:"+xi_0+","+xi_1);
					
					/*boolGen.generateSharedDataPoint(w[i], true);
					byte wi_0 = boolGen.x0;
					byte wi_1 = boolGen.x1;
					w_0_arr[i] = wi_0;
					w_1_arr[i] = wi_1;		*/	
				}

				

		
		int round = 1001;
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {	
			SecMaxPooling secmp = new SecMaxPooling();// y, u
			//extract MSB of a
			byte[] z = secmp.computeSingleVector(false, sndChannel, rcvChannel, mt2,  x_0_arr,  x_1_arr);
			// verify
			byte z0 = z[0];
			byte z1 = z[1];
			byte zVer = BooleanUtil.xor(z0, z1);
			System.out.println("z:" + zVer + " z0:" + z0 + " z1:" + z1);
			if(zVer == 1) {
				break;
			}

			if (i % 1000 == 0) {
				System.out.println("Progress:" + i);
			}
		}
		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
		//System.out.println("bandwidth:" + secmp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		//System.out.println("timeNetwork:" + secmp.timeNetwork / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

	
	
}
