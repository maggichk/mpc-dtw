package BNNlayers;

import booleanShr.BooleanShrGenerator;
import eightBitAdditive.eightBitAdditiveUtil;
import eightBitAdditive.eightBitMultiplicationTriple;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBNN.SecXnorPopcountEightBit;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//import com.sun.deploy.net.MessageHeader;
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

public class convolutionLayerEightBit {

	//inputs:
	// several (feature.len) inputact.row * inputact.column vector (binary)
	// several (feature.len)  kernel.row * kernel.row matrixs (binary)
	//output:
	// several (feature.len) outputact.row * outputact.column vectors (short)
	//parameters:
	//stride
	//padding
	//we do not consider bias in our case.

	public byte[][][] x;
	public byte[][][] w;

	public byte[][][] y;

	private Server sndChannel;
	private Client rcvChannel;

	public byte z0AND;
	public byte z1AND;

	public byte z0l = 0;
	public byte z1l = 0;

	public double bandwidth = 0.0;

	public long timeNetwork = 0;

	public short[][][] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel,
						   eightBitMultiplicationTriple mt, byte[][][] x, byte[][][] w) throws Exception{

		int out_feature = w.length;
		int in_feature = x.length;
		int kenel_size = w[0].length;
		int input_row = x[0].length;
		int input_column = x[0][0].length;

		short[][][] output_y = new short[out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1];
		byte[][][][] total_x_for_y = new byte[out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1][(in_feature * kenel_size * kenel_size)];//x's channel * kernel_window_size^2
		byte[][][][] total_w_for_y = new byte[out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1][(in_feature * kenel_size * kenel_size)];


		SecXnorPopcountEightBit xnorp = new SecXnorPopcountEightBit();
		//for each output channel
		for(int i = 0; i<out_feature;i++){
			//build single y matrix

			//get w
			byte[] w_flat = flaten(w[i]);

			for(int i_y = 0; i_y<input_row - kenel_size + 1;i_y++){
				for (int j_y = 0; j_y<input_column - kenel_size + 1;j_y++){
					// caculate a single y

					//append w
					for(int j = 0; j<in_feature;j++){
						System.arraycopy(w_flat, 0, total_w_for_y[i][i_y][j_y], j*kenel_size * kenel_size, kenel_size * kenel_size);

						//append x
						//get one value in a windows
						byte[][] x_sub = getWindow(x[j], i_y, j_y, kenel_size);
						byte[] x_flat = flaten(x_sub);
						System.arraycopy(x_flat, 0, total_x_for_y[i][i_y][j_y], j*kenel_size * kenel_size, kenel_size * kenel_size);
						}

					//generate shares for a single y
					BooleanShrGenerator boolGen = new BooleanShrGenerator(true);

					System.out.println("x:" + Arrays.toString(total_x_for_y[i][i_y][j_y]) + " w:" + Arrays.toString(total_w_for_y[i][i_y][j_y]));

//					byte[] x_sub = total_x_for_y[i][i_y][j_y];
//					byte[] w_sub = total_w_for_y[i][i_y][j_y];

					byte[] x_0_sub_arr = new byte[total_x_for_y[i][i_y][j_y].length];
					byte[] x_1_sub_arr = new byte[total_x_for_y[i][i_y][j_y].length];

					byte[] w_0_sub_arr = new byte[total_w_for_y[i][i_y][j_y].length];
					byte[] w_1_sub_arr = new byte[total_w_for_y[i][i_y][j_y].length];

					for(int y_len=0; y_len<total_x_for_y[i][i_y][j_y].length;y_len++) {
						boolGen.generateSharedDataPoint(total_x_for_y[i][i_y][j_y][y_len], true);
						byte xi_sub_0 = boolGen.x0;
						byte xi_sub_1 = boolGen.x1;
						//System.out.println("i" + i + "  xi:" + x[i] + "  xi_0:" + xi_sub_0 + "  xi_1:" + xi_sub_1);
						x_0_sub_arr[y_len] = xi_sub_0;
						x_1_sub_arr[y_len] = xi_sub_1;

						boolGen.generateSharedDataPoint(total_w_for_y[i][i_y][j_y][y_len], true);
						byte wi_sub_0 = boolGen.x0;
						byte wi_sub_1 = boolGen.x1;
						//System.out.println("i" + i + "  wi:" + w[i] + "  wi_0:" + wi_sub_0 + "  wi_1:" + wi_sub_1);
						w_0_sub_arr[y_len] = wi_sub_0;
						w_1_sub_arr[y_len] = wi_sub_1;
					}

					//caculate z


					short[] z_sub = xnorp.compute(false, sndChannel, rcvChannel, mt, x_0_sub_arr, w_0_sub_arr, x_1_sub_arr, w_1_sub_arr);

					// verify
					short z0_sub = z_sub[0];
					short z1_sub = z_sub[1];

					output_y[i][i_y][j_y] = eightBitAdditiveUtil.add(z0_sub, z1_sub);
					System.out.println("z:" + eightBitAdditiveUtil.add(z0_sub, z1_sub) + " z0:" + z0_sub + " z1:" + z1_sub);
					}
				}


		}
  		return output_y;
	}

	public byte[] flaten(byte[][] x) throws Exception{
		//make the matrix to array
		byte[] flat_matrix = new byte[x.length * x[0].length];
		for(int i = 0; i < x.length; i++){
			for(int j = 0; j < x[0].length; j++) {
				flat_matrix [i * x.length + j] = x[i][j];
				}
			}
		//System.out.println("Original array" + Arrays.toString(x) );
		//System.out.println("flaten array" + Arrays.toString(flat_matrix) );
		return flat_matrix;
		}

	public byte[][] getWindow(byte[][] x, int i, int j, int windowSize) throws Exception{
		//This is for get a window in x at (i,j)
		byte[][] sub_x = new byte[windowSize][windowSize];
		for(int p = 0; p < windowSize; p++){
			for(int q = 0; q < windowSize; q++){
				sub_x[p][q] = x[p+i][q+j];
			}
		}
		return sub_x;
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
		byte[][][] x = {
				{{(byte)1, (byte)1, (byte)0},
				 {(byte)1, (byte)1, (byte)0},
				 {(byte)1, (byte)1, (byte)0},
				},
				{{(byte)1, (byte)1, (byte)0},
				 {(byte)1, (byte)1, (byte)0},
				 {(byte)1, (byte)1, (byte)0},
				}
		};
		byte[][][] w = {
				{{(byte)1, (byte)1},
				 {(byte)1, (byte)1}
				},
				{{(byte)1, (byte)1},
				 {(byte)1, (byte)1}
				},
				{{(byte)1, (byte)1},
				 {(byte)1, (byte)1}
				},
		};
		//short[] y = new short[5];

		convolutionLayerEightBit conV = new convolutionLayerEightBit();
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		short[][][] y = conV.compute(false, sndChannel, rcvChannel, mt, x, w);
		//boolean isDisconnect, Server sndChannel, Client rcvChannel,
		//						   eightBitMultiplicationTriple mt, byte[][][] x, byte[][][] w

		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
//		System.out.println("bandwidth:" + xnorp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
//		System.out.println("timeNetwork:" + xnorp.timeNetwork / 1e9 + " seconds");
//		System.out.println("Output:" + Arrays.toString(y));

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

	
	
}
