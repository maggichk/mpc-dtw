package BNNlayers;

import gadgetBNN.SecXnorPopcountEightBit;

//import com.sun.deploy.net.MessageHeader;
import eightBitAdditive.eightBitAdditiveUtil;
import eightBitAdditive.eightBitMulP0;
import eightBitAdditive.eightBitMulP1;
import eightBitAdditive.eightBitMultiplicationTriple;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import flexSC.network.Client;
import flexSC.network.Server;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

//import booleanShr.BooleanShrGenerator;
//import booleanShr.BooleanUtil;
//import eightBitAdditive.eightBitAdditiveUtil;
//import eightBitAdditive.eightBitMulP0;
//import eightBitAdditive.eightBitMulP1;
//import eightBitAdditive.eightBitMultiplicationTriple;
//import flexSC.network.Client;
//import flexSC.network.Server;
//
//import utilMpc.Config2PC;
//import utilMpc.Constants2PC;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class fullConnectedLayerEightBit {

	//inputs:
	// a 1*inputact.len vector (binary)
	// a inputact.len(column number) * outputact.len(row number) matrix (binary)
	//output:
	// a 1*outputact.len vector (short)
	//we do not consider bias in our case.

	public byte[] x;
	public byte[][] w;

	public byte[] y;

	private Server sndChannel;
	private Client rcvChannel;

	public byte z0AND;
	public byte z1AND;

	public byte z0l = 0;
	public byte z1l = 0;

	public double bandwidth = 0.0;

	public long timeNetwork = 0;

	public short[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel,
						   eightBitMultiplicationTriple mt, byte[] x, byte[][] w) throws Exception{

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);

		// define two shares
		byte[] x_0 = new byte[x.length];
		byte[] x_1 = new byte[x.length];
		byte[][] w_0 = new byte[w.length][x.length];
		byte[][] w_1 = new byte[w.length][x.length];
		short[] y = new short[w.length];

//		byte[] w_0_single_neural = new byte[w[0].length];
//		byte[] w_1_single_neural = new byte[w[0].length];

		//seperate x

		for(int i = 0; i<x.length;i++){
			boolGen.generateSharedDataPoint(x[i], true);
			byte xi_0 = boolGen.x0;
			byte xi_1 = boolGen.x1;
			//System.out.println("i" + i + "  xi:" + x[i] + "  xi_0:" + xi_0 + "  xi_1:" + xi_1);
			x_0[i] = xi_0;
			x_1[i] = xi_1;
		}

		//seperate y

		SecXnorPopcountEightBit xnorp = new SecXnorPopcountEightBit();

  		for(int j = 0; j<w.length;j++){

  			//for each bit
			for(byte i=0; i<x.length;i++) {

				boolGen.generateSharedDataPoint(w[j][i], true);
				byte wi_0 = boolGen.x0;
				byte wi_1 = boolGen.x1;
				//System.out.println("i" + i + "  wi:" + w[i] + "  wi_0:" + wi_0 + "  wi_1:" + wi_1);
				w_0[j][i] = wi_0;
				w_1[j][i] = wi_1;
			}

			//comput each output

			short[] z = xnorp.compute(false, sndChannel, rcvChannel, mt, x_0, w_0[j], x_1, w_1[j]);
			// verify
			short z0 = z[0];
			short z1 = z[1];
			y[j] = eightBitAdditiveUtil.add(z0, z1);

			//System.out.println("z:" + (z0 + z1) + " z0:" + z0 + " z1:" + z1);
			System.out.println("feature:" + j);
			System.out.println("x:" + Arrays.toString(x) + " x0:" + Arrays.toString(x_0) + " x1:" + Arrays.toString(x_1));
			System.out.println("w:" + Arrays.toString(w[j])+ " w0:" + Arrays.toString(w_0[j]) + " w1:" + Arrays.toString(w_1[j]));
			System.out.println("z:" + y[j] + " z0:" + z0 + " z1:" + z1);

//			sndChannel.disconnect();
//			rcvChannel.disconnect();
		}

  		return y;
	}


	//private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	//		0 };
	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0};

//	public short[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, eightBitMultiplicationTriple mt,
//			byte[] x_0_arr,  byte[] w_0_arr, byte[] x_1_arr, byte[] w_1_arr) throws Exception {
//		short z[] = new short[2];
//		// long z_0 = 0L;
//		// long z_1 = 0L;
//
//		int len_n = x_0_arr.length;
//
//		this.sndChannel = sndChannel;
//		this.rcvChannel = rcvChannel;
//
//		ExecutorService exec = Executors.newFixedThreadPool(2);
//		exec.execute(new Runnable() {
//			@Override
//			public void run() {
//
//				for (int i = 0; i < len_n; i++) {
//					byte xi_0 = x_0_arr[i];
//					byte wi_0 = w_0_arr[i];
//
//					// XNOR(x,w)
//					byte ti_0 = BooleanUtil.xor(xi_0, wi_0);
//					byte pi_0 = BooleanUtil.xor(ti_0, (byte) 0);// xor(*,i)
//
//					// convert back to Z31
//					short di_0 = pi_0;
//					short ei_0 = 0;
//					// SeqCompEngine( mt, d_0, e_0, d_1, e_1); d_0 mul e_0
//					eightBitMulP0 mulP0 = new eightBitMulP0();
//
//
//
//					short a = eightBitAdditiveUtil.add(mt.tripleA0, mt.tripleA1);
//					short b = eightBitAdditiveUtil.add(mt.tripleB0, mt.tripleB1);
//					short c = eightBitAdditiveUtil.add(mt.tripleC0, mt.tripleC1);
//
//					System.out.print("MT A:" + a); System.out.print(" MT B:" + b);
//					System.out.println(" MT C:" + c);
//
//
//					// verify
//					short cVer = eightBitAdditiveUtil.mul((byte) a, (byte) b);
//
//					System.out.println("verify c:" + cVer +" u:"+eightBitAdditiveUtil.mul(mt.tripleA0,
//							mt.tripleB1)+" v:"+eightBitAdditiveUtil.mul(mt.tripleA1, mt.tripleB0));
////					byte mta0 = mt.tripleA0 ;
////					byte mta1 = mt.tripleA1 ;
////					byte mtb0 = mt.tripleB0 ;
////					byte mtb1 = mt.tripleB1 ;
////					byte mtc0 = mt.tripleC0 ;
////					byte mtc1 = mt.tripleC1 ;
////					System.out.println("  mta0:" + mta0 + "  mta1:" + mta1 + "  mtb0:" + mtb0 + "  mtb1:" + mtb1 + "  mtc0:" + mtc0 + "  mtc1:" + mtc1);
////					System.out.println("  ab:" + eightBitAdditiveUtil.mul(eightBitAdditiveUtil.sub(mta0 , mta1) , eightBitAdditiveUtil.sub(mtb0 , mtb1)) + "  c:" + eightBitAdditiveUtil.sub(mtc0 , mtc1));
//
//					mulP0.compute(isDisconnect, sndChannel, mt, di_0, ei_0);
//					short zMUL1 = mulP0.z0;
//					System.out.println("z0:" + zMUL1);
//
//					//short z0lAddi = di_0 + ei_0 -  (2*zMUL1);
//					short z0lAddi = eightBitAdditiveUtil.sub(eightBitAdditiveUtil.add(di_0, ei_0), eightBitAdditiveUtil.mul((short) 2, zMUL1));
//
//					// sum
//					z[0] += z0lAddi;
//					//z[0] = eightBitAdditiveUtil.sub(z[0] , z0lAddi);
//				}
//
//				//z[0] = 2 * z[0] -  len_n;
//				z[0] = (short) ((eightBitAdditiveUtil.mul((short) 2, z[0] ) ) -  len_n);
//			}
//
//		});
//
//		exec.execute(new Runnable() {
//			@Override
//			public void run() {
//
//				for (int i = 0; i < len_n; i++) {
//					byte xi_1 = x_1_arr[i];
//					byte wi_1 = w_1_arr[i];
//
//					byte ti_1 = BooleanUtil.xor(xi_1, wi_1);
//					byte pi_1 = BooleanUtil.xor(ti_1, (byte) 1);// xor(*,i)
//
//					// convert back to Z31
//					short di_1 = 0;
//					short ei_1 = pi_1;
//					// SeqCompEngine(mt, d_0, e_0, d_1, e_1); d_1 mul e_1
//					eightBitMulP1 mulP1 = new eightBitMulP1();
//					mulP1.compute(isDisconnect, rcvChannel, mt, di_1, ei_1);
//					short zMUL1 = mulP1.z1;
//
//					//int z1lAddi = di_1 + ei_1 -  (2*zMUL1);
//					short z1lAddi = eightBitAdditiveUtil.sub(eightBitAdditiveUtil.add(di_1, ei_1), eightBitAdditiveUtil.mul((short) 2, zMUL1));
//					z[1] += z1lAddi;
//				}
//
//				//z[1] = 2 * z[1];
//				z[1] = eightBitAdditiveUtil.mul((short) 2, z[1]);
//
//			}
//		});
//
//		// long st4 = System.nanoTime();
//		// should be done with in 1s
//		exec.shutdown();
//		try {
//			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
//				// Execution finished
//				exec.shutdownNow();
//
//				if (isDisconnect == true) {
//
//					// System.out.println("SLB disconnecting...");
//					rcvChannel.disconnectCli();
//					sndChannel.disconnectServer();
//
//				}
//			}
//		} catch (InterruptedException e) {
//			// Something is wrong
//			System.out.println("Unexpected interrupt");
//			exec.shutdownNow();
//			Thread.currentThread().interrupt();
//			throw new RuntimeException(e);
//		}
//
//		this.bandwidth += rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
//		rcvChannel.cis.resetByteCount();
//		rcvChannel.cos.resetByteCount();
//
//		return z;
//	}
	
	
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




		eightBitMultiplicationTriple mt = new eightBitMultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
//		eightBitMultiplicationTriple mt = new eightBitMultiplicationTriple(sndChannel, rcvChannel);
//		// long bandwidthMT =
//		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
//		rcvChannel.cis.resetByteCount();
//		rcvChannel.cos.resetByteCount();
//
//		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		
		//byte[] x = {(byte)1, (byte)1, (byte)0, (byte)0, (byte)0};
		//byte[] w = {(byte)0, (byte)1, (byte)0, (byte)0, (byte)1};
		
		//test case in XONN
		byte[] x = {(byte)1, (byte)1, (byte)0, (byte)0};
		byte[][] w = {{(byte)0, (byte)1, (byte)0, (byte)0},
				{(byte)1, (byte)1, (byte)1, (byte)1},
				{(byte)0, (byte)0, (byte)0, (byte)0},
				{(byte)1, (byte)1, (byte)0, (byte)0},
				{(byte)0, (byte)0, (byte)1, (byte)1}
		};
		//short[] y = new short[5];

		fullConnectedLayerEightBit fullC = new fullConnectedLayerEightBit();
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		short[] y = fullC.compute(false, sndChannel, rcvChannel, mt, x, w);

		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
//		System.out.println("bandwidth:" + xnorp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
//		System.out.println("timeNetwork:" + xnorp.timeNetwork / 1e9 + " seconds");
		System.out.println("Output:" + Arrays.toString(y));

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

	
	
}
