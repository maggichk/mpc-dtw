package gadgetBNN;

//import com.sun.deploy.net.MessageHeader;

import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import eightBitAdditive.*;
import eightBitAdditive.eightBitAdditiveUtil;
import eightBitAdditive.eightBitMulP0;
import eightBitAdditive.eightBitMulP1;
import eightBitAdditive.eightBitMultiplicationTriple;
import flexSC.network.Client;
import flexSC.network.Server;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SecXnorPopcountFifteenBit {

	private Server sndChannel;
	private Client rcvChannel;

	public byte z0AND;
	public byte z1AND;

	public byte z0l = 0;
	public byte z1l = 0;

	public double bandwidth = 0.0;
	public double timeNetwork = 0.0;

	//private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	//		0 };
	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0};

	public short[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, fifteenBitMultiplicationTriple mt,
			byte[] x_0_arr,  byte[] w_0_arr, byte[] x_1_arr, byte[] w_1_arr) throws Exception {
		short z[] = new short[2];
		// long z_0 = 0L;
		// long z_1 = 0L;

		int len_n = x_0_arr.length;

		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;

		//System.out.println("Inside: x0:" + Arrays.toString(x_0_arr) + " x1:" + Arrays.toString(x_1_arr));
		//System.out.println("Inside: w0:" + Arrays.toString(w_0_arr) + " w1:" + Arrays.toString(w_1_arr));


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
					short di_0 = pi_0;
					short ei_0 = 0;
					// SeqCompEngine( mt, d_0, e_0, d_1, e_1); d_0 mul e_0
					fifteenBitMulP0 mulP0 = new fifteenBitMulP0();



//					short a = fifteenBitAdditiveUtil.add(mt.tripleA0, mt.tripleA1);
//					short b = fifteenBitAdditiveUtil.add(mt.tripleB0, mt.tripleB1);
//					short c = fifteenBitAdditiveUtil.add(mt.tripleC0, mt.tripleC1);

//					System.out.print("MT A:" + a); System.out.print(" MT B:" + b);
//					System.out.println(" MT C:" + c);


//					// verify
//					short cVer = fifteenBitAdditiveUtil.mul((byte) a, (byte) b);
//
////					System.out.println("verify c:" + cVer +" u:"+fifteenBitAdditiveUtil.mul(mt.tripleA0,
////							mt.tripleB1)+" v:"+fifteenBitAdditiveUtil.mul(mt.tripleA1, mt.tripleB0));
//					short mta0 = mt.tripleA0 ;
//					short mta1 = mt.tripleA1 ;
//					short mtb0 = mt.tripleB0 ;
//					short mtb1 = mt.tripleB1 ;
//					short mtc0 = mt.tripleC0 ;
//					short mtc1 = mt.tripleC1 ;
//					System.out.println("  mta0:" + mta0 + "  mta1:" + mta1 + "  mtb0:" + mtb0 + "  mtb1:" + mtb1 + "  mtc0:" + mtc0 + "  mtc1:" + mtc1);
//					System.out.println("  ab:" + fifteenBitAdditiveUtil.mul(fifteenBitAdditiveUtil.add(mta0 , mta1) , fifteenBitAdditiveUtil.add(mtb0 , mtb1)) + "  c:" + fifteenBitAdditiveUtil.add(mtc0 , mtc1));

					mulP0.compute(isDisconnect, sndChannel, mt, di_0, ei_0);
					short zMUL1 = mulP0.z0;
					timeNetwork += mulP0.time;
					
					//System.out.println("z0:" + zMUL1);

					//short z0lAddi = di_0 + ei_0 -  (2*zMUL1);
					short z0lAddi = fifteenBitAdditiveUtil.sub(fifteenBitAdditiveUtil.add(di_0, ei_0), fifteenBitAdditiveUtil.mul((short) 2, zMUL1));

					// sum
					z[0] += z0lAddi;
					//z[0] = fifteenBitAdditiveUtil.sub(z[0] , z0lAddi);
				}

				//z[0] = 2 * z[0] -  len_n;
				z[0] = (short) ((fifteenBitAdditiveUtil.mul((short) 2, z[0] ) ) -  len_n);
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
					short di_1 = 0;
					short ei_1 = pi_1;
					// SeqCompEngine(mt, d_0, e_0, d_1, e_1); d_1 mul e_1
					fifteenBitMulP1 mulP1 = new fifteenBitMulP1();
					mulP1.compute(isDisconnect, rcvChannel, mt, di_1, ei_1);
					short zMUL1 = mulP1.z1;

					//int z1lAddi = di_1 + ei_1 -  (2*zMUL1);
					short z1lAddi = fifteenBitAdditiveUtil.sub(fifteenBitAdditiveUtil.add(di_1, ei_1), fifteenBitAdditiveUtil.mul((short) 2, zMUL1));
					z[1] += z1lAddi;
				}

				//z[1] = 2 * z[1];
				z[1] = fifteenBitAdditiveUtil.mul((short) 2, z[1]);

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

		
		double st = System.nanoTime();
		this.bandwidth += rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		double et = System.nanoTime();
		timeNetwork += et-st;

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




		fifteenBitMultiplicationTriple mt = new fifteenBitMultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		
		//byte[] x = {(byte)1, (byte)1, (byte)0, (byte)0, (byte)0};
		//byte[] w = {(byte)0, (byte)1, (byte)0, (byte)0, (byte)1};
		
		//test case in XONN
		byte[] x = {(byte)0, (byte)1, (byte)0};
		byte[] w = {(byte)0, (byte)0, (byte)0};
		
		byte[] x_0_arr = new byte[x.length];
		byte[] x_1_arr = new byte[x.length];
		
		byte[] w_0_arr = new byte[w.length];
		byte[] w_1_arr = new byte[w.length];
		
		for(byte i=0; i<x.length;i++) {
			boolGen.generateSharedDataPoint(x[i], true);
			byte xi_0 = boolGen.x0;
			byte xi_1 = boolGen.x1;
			//System.out.println("i" + i + "  xi:" + x[i] + "  xi_0:" + xi_0 + "  xi_1:" + xi_1);
			x_0_arr[i] = xi_0;
			x_1_arr[i] = xi_1;
			
			boolGen.generateSharedDataPoint(w[i], true);
			byte wi_0 = boolGen.x0;
			byte wi_1 = boolGen.x1;
			//System.out.println("i" + i + "  wi:" + w[i] + "  wi_0:" + wi_0 + "  wi_1:" + wi_1);
			w_0_arr[i] = wi_0;
			w_1_arr[i] = wi_1;			
		}
		//System.out.println("  x:" + Arrays.toString(x) + "  x_0:" + Arrays.toString(x_0_arr) + "  x_1:" + Arrays.toString(x_1_arr));
		//System.out.println("  w:" + Arrays.toString(w) + "  w_0:" + Arrays.toString(w_0_arr) + "  w_1:" + Arrays.toString(w_1_arr));

		

		SecXnorPopcountFifteenBit xnorp = new SecXnorPopcountFifteenBit();
		int round = 1;
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {			
			short[] z = xnorp.compute(false, sndChannel, rcvChannel, mt, x_0_arr, w_0_arr, x_1_arr, w_1_arr);
			// verify
			short z0 = z[0];
			short z1 = z[1];

			System.out.println("z:" + (z0 + z1) + " z0:" + z0 + " z1:" + z1);
			System.out.println("z:" + fifteenBitAdditiveUtil.add(z0, z1) + " z0:" + z0 + " z1:" + z1);

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
