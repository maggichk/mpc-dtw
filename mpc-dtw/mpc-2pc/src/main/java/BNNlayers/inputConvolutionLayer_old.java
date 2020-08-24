package BNNlayers;

import booleanShr.BooleanShrGenerator;
import additive.AdditiveUtil;
import eightBitAdditive.eightBitAdditiveUtil;
import additive.MultiplicationTriple;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBNN.SecIntegerBinaryVDPBatch;
import gadgetBNN.SecIntegerBinaryVDPBatchEightBit;
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

public class inputConvolutionLayer_old {

	//inputs:
	// several (feature.len) inputact.row * inputact.column vector (short)
	// several (feature.len)  kernel.row * kernel.row matrixs (binary)
	//output:
	// two shares:
	// several (feature.len) outputact.row * outputact.column vectors (short)
	//parameters:
	//stride
	//padding
	//we do not consider bias in our case.

	public short[][][] x;
	public byte[][][] w;

	public byte[][][] y;

	private Server sndChannel;
	private Client rcvChannel;

	public byte z0AND;
	public byte z1AND;

	public byte z0l = 0;
	public byte z1l = 0;

	public double bandwidth = 0.0;
	public double bandwidthOTInitial = 0.0;

	public double timeNetwork = 0.0;
	public double timeOTInitial = 0.0;
	public double timeSharingW = 0.0;
	public double timePure = 0.0; //timeAll - timeOTInitial - timeSharingW;
	public double timeAll = 0.0;
	
	public int counter = 0;
	

	public long[][][][] compute_conv(boolean isDisconnect, Server sndChannel, Client rcvChannel,
						   MultiplicationTriple mt, long[][][][] x, byte[][][] old_w) throws Exception{

		//improve w to +w and -w
		byte[][][][] w = new byte[2][old_w.length][old_w[0].length][old_w[0][0].length];
		for(int old_w_i = 0; old_w_i<old_w.length;old_w_i++){
			for(int old_w_j = 0; old_w_j<old_w[0].length;old_w_j++){
				for(int old_w_k = 0; old_w_k<old_w[0][0].length;old_w_k++){
					if (old_w[old_w_i][old_w_j][old_w_k] == 0){
						//0 means -1
						w[0][old_w_i][old_w_j][old_w_k] = 0;
						w[1][old_w_i][old_w_j][old_w_k] = 1;
					}
					else{
						//1 means +1
						w[0][old_w_i][old_w_j][old_w_k] = 1;
						w[1][old_w_i][old_w_j][old_w_k] = 0;
					}
				}
			}
		}

		int out_feature = old_w.length;
		int in_feature = x[0].length;
		int kenel_size = old_w[0].length;
		int input_row = x[0][0].length;
		int input_column = x[0][0][0].length;

		long[][][][] output_y = new long[2][out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1];
		long[][][][][] temp_output_y = new long[2][2][out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1];
		long[][][][][] total_x_for_y = new long[2][out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1][(in_feature * kenel_size * kenel_size)];//x's channel * kernel_window_size^2
		byte[][][][][] total_w_for_y = new byte[2][out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1][(in_feature * kenel_size * kenel_size)];


		SecIntegerBinaryVDPBatch ibvdp = new SecIntegerBinaryVDPBatch();
		//for both x_0 and x_1
		for(int sign_w = 0; sign_w<2;sign_w++){
			//for each output channel
			for(int i = 0; i<out_feature;i++){
				//build single y matrix

				//get w
				byte[] w_flat = flaten_byte(w[sign_w][i]);

				for(int i_y = 0; i_y<input_row - kenel_size + 1;i_y++){
					for (int j_y = 0; j_y<input_column - kenel_size + 1;j_y++){
						// caculate a single y

						//append w
						for(int j = 0; j<in_feature;j++){
							System.arraycopy(w_flat, 0, total_w_for_y[sign_w][i][i_y][j_y], j*kenel_size * kenel_size, kenel_size * kenel_size);

							//append x
							//get one value in a windows
							long[][] x_sub_0 = getWindow(x[0][j], i_y, j_y, kenel_size);
							long[] x_flat_0 = flaten(x_sub_0);
							long[][] x_sub_1 = getWindow(x[1][j], i_y, j_y, kenel_size);
							long[] x_flat_1 = flaten(x_sub_1);
							System.arraycopy(x_flat_0, 0, total_x_for_y[0][i][i_y][j_y], j*kenel_size * kenel_size, kenel_size * kenel_size);
							System.arraycopy(x_flat_1, 0, total_x_for_y[1][i][i_y][j_y], j*kenel_size * kenel_size, kenel_size * kenel_size);
						}

						

						//System.out.println("x:" + Arrays.toString(total_x_for_y[i][i_y][j_y]) + " w:" + Arrays.toString(total_w_for_y[i][i_y][j_y]));

//					byte[] x_sub = total_x_for_y[i][i_y][j_y];
//					byte[] w_sub = total_w_for_y[i][i_y][j_y];

						//long[] x_0_sub_arr = new long[total_x_for_y[i_shares][i][i_y][j_y].length];
						//long[] x_1_sub_arr = new long[total_x_for_y[i_shares][i][i_y][j_y].length];

						byte[] w_0_sub_arr = new byte[total_w_for_y[sign_w][i][i_y][j_y].length];
						byte[] w_1_sub_arr = new byte[total_w_for_y[sign_w][i][i_y][j_y].length];
						//generate shares for a single y
						BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
						for(int y_len=0; y_len<total_x_for_y[0][i][i_y][j_y].length;y_len++) {
							//boolGen.generateSharedDataPoint(total_x_for_y[i][i_y][j_y][y_len], true);
							//long xi_sub_0 = boolGen.x0;
							//long xi_sub_1 = boolGen.x1;
							//System.out.println("i" + i + "  xi:" + x[i] + "  xi_0:" + xi_sub_0 + "  xi_1:" + xi_sub_1);
							//x_0_sub_arr[y_len] = xi_sub_0;
							//x_1_sub_arr[y_len] = xi_sub_1;
							
							double st = System.nanoTime();
							boolGen.generateSharedDataPoint(total_w_for_y[sign_w][i][i_y][j_y][y_len], true);
							double et = System.nanoTime();
							timeSharingW += et-st;
							
							byte wi_sub_0 = boolGen.x0;
							byte wi_sub_1 = boolGen.x1;
							//System.out.println("i" + i + "  wi:" + w[i] + "  wi_0:" + wi_sub_0 + "  wi_1:" + wi_sub_1);
							w_0_sub_arr[y_len] = wi_sub_0;
							w_1_sub_arr[y_len] = wi_sub_1;
						}

						double st = System.nanoTime();
						//caculate z
						long[] z = ibvdp.compute(sndChannel, rcvChannel, total_x_for_y[0][i][i_y][j_y], w_0_sub_arr, total_x_for_y[1][i][i_y][j_y], w_1_sub_arr);
						double et = System.nanoTime();
						timeAll += et-st;
						counter++;
						if(counter%100 == 0) {
							System.out.println("Progress:"+counter+" time ibvdp");
						}
						// verify output_y[i][i_y][j_y]
//						long z0_sub = z[0];
//						long z1_sub = z[1];
						temp_output_y[sign_w][0][i][i_y][j_y] = z[0];
						temp_output_y[sign_w][1][i][i_y][j_y] = z[1];

						//long z_total = AdditiveUtil.add(output_y[0][i][i_y][j_y], output_y[1][i][i_y][j_y]);
						//System.out.println("z:" + z_total + " z0:" + output_y[0][i][i_y][j_y] + " z1:" + output_y[1][i][i_y][j_y]);
					}
				}


			}
		}
		for(int i_out_dim = 0; i_out_dim<w[0].length;i_out_dim++){
			for(int j_out_dim = 0; j_out_dim<w[0][0].length;j_out_dim++){
				for(int k_out_dim = 0; k_out_dim<w[0][0][0].length;k_out_dim++){
					output_y[0][i_out_dim][j_out_dim][k_out_dim] = AdditiveUtil.sub(temp_output_y[0][0][i_out_dim][j_out_dim][k_out_dim], temp_output_y[1][0][i_out_dim][j_out_dim][k_out_dim]);
					output_y[1][i_out_dim][j_out_dim][k_out_dim] = AdditiveUtil.sub(temp_output_y[0][1][i_out_dim][j_out_dim][k_out_dim], temp_output_y[1][1][i_out_dim][j_out_dim][k_out_dim]);
					System.out.println("feature:" + (AdditiveUtil.add(output_y[0][i_out_dim][j_out_dim][k_out_dim] , output_y[1][i_out_dim][j_out_dim][k_out_dim])));
				}
			}
		}

		this.timeOTInitial = ibvdp.timeOTInitial;
		this.timeNetwork = ibvdp.timeNetwork;
		this.timePure = this.timeAll - timeOTInitial - timeNetwork;
		this.bandwidth = ibvdp.bandwidth;
  		return output_y;
	}


	public long[][] compute_fc(boolean isDisconnect, Server sndChannel, Client rcvChannel,
						   MultiplicationTriple mt, long[][] x, byte[][] old_w) throws Exception{

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);

		//improve w to +w and -w  -----encoding
		byte[][][] w = new byte[2][old_w.length][x[0].length];
		for(int old_w_i = 0; old_w_i<old_w.length;old_w_i++){
			for(int old_w_j = 0; old_w_j<old_w[0].length;old_w_j++){
				if (old_w[old_w_i][old_w_j] == 0){
					//0 means -1
					w[0][old_w_i][old_w_j] = 0;
					w[1][old_w_i][old_w_j] = 1;
				}
				else{
					//1 means +1
					w[0][old_w_i][old_w_j] = 1;
					w[1][old_w_i][old_w_j] = 0;
				}
			}
		}

		// define two shares
		byte[][][] w_0 = new byte[2][w[0].length][x[0].length];
		byte[][][] w_1 = new byte[2][w[0].length][x[0].length];
		long[][] y = new long[2][w[0].length];
		long[][][] y_temp = new long[2][2][w[0].length]; //2 is 2 sign, 2 is 2 shares, w[0].length is the output dimension

//		byte[] w_0_single_neural = new byte[w[0].length];
//		byte[] w_1_single_neural = new byte[w[0].length];

		//seperate y

		SecIntegerBinaryVDPBatch ibvdp = new SecIntegerBinaryVDPBatch(); //y_temp[sign_w]

		for(int sign_w = 0; sign_w<2;sign_w++){

			for(int j = 0; j<w[0].length;j++){

				//for each bit
				for(int i=0; i<x[0].length;i++) {
					
					double st = System.nanoTime();
					//System.out.println("sign_w:"+sign_w +" j:"+j+" i:"+i);
					boolGen.generateSharedDataPoint(w[sign_w][j][i], true);
					
					double et = System.nanoTime();
					timeSharingW += et-st;
					
					byte wi_0 = boolGen.x0;
					byte wi_1 = boolGen.x1;
					//System.out.println("i" + i + "  wi:" + w[i] + "  wi_0:" + wi_0 + "  wi_1:" + wi_1);
					w_0[sign_w][j][i] = wi_0;
					w_1[sign_w][j][i] = wi_1;
				}
				//comput each output

//			System.out.println("j:" + j);
//			System.out.println("x0:" + Arrays.toString(x[0]) + "x1:" + Arrays.toString(x[1]));
//			System.out.println("w0:" + Arrays.toString(w_0[j]) + "w1:" + Arrays.toString(w_1[j]));
				
				double st = System.nanoTime();
				long[] z = ibvdp.compute( sndChannel, rcvChannel, x[0], w_0[sign_w][j], x[1], w_1[sign_w][j]);
				double et = System.nanoTime();
				timeAll += et-st;
				counter++;
				if(counter%100 == 0) {
					System.out.println("Progress:"+counter+" time ibvdp");
				}
				
				// verify
//			long z0 = z[0];
//			long z1 = z[1];
				y_temp[sign_w][0][j] = z[0];
				y_temp[sign_w][1][j] = z[1];
				//y[j] = eightBitAdditiveUtil.add(z0, z1);

//			System.out.println("z:" + (z0 + z1) + " z0:" + z0 + " z1:" + z1);
//				System.out.println("feature:" + j);
//				System.out.println("x:" + Arrays.toString(x) + " x0:" + Arrays.toString(x[0]) + " x1:" + Arrays.toString(x[1]));
//				System.out.println("w:" + Arrays.toString(w[j])+ " w0:" + Arrays.toString(w_0[j]) + " w1:" + Arrays.toString(w_1[j]));
//				System.out.println("z:" + AdditiveUtil.add(z[0] , z[1]) + " z0:" + z[0] + " z1:" + z[1]);
//			sndChannel.disconnect();
//			rcvChannel.disconnect();

			}
		}

		//caculate output
		for(int out_dim = 0; out_dim<w[0].length;out_dim++){
			y[0][out_dim] = AdditiveUtil.sub(y_temp[0][0][out_dim], y_temp[1][0][out_dim]);
			y[1][out_dim] = AdditiveUtil.sub(y_temp[0][1][out_dim], y_temp[1][1][out_dim]);
			//System.out.println("feature:" + (AdditiveUtil.add(y[0][out_dim] , y[1][out_dim])));
		}

		//System.out.println("feature:" + Arrays.toString(y[0]));
		//System.out.println("feature:" + Arrays.toString(y[1]));


		this.timeOTInitial = ibvdp.timeOTInitial;
		this.timeNetwork = ibvdp.timeNetwork;
		this.timePure = this.timeAll - timeOTInitial - timeNetwork;
		this.bandwidth = ibvdp.bandwidth;


		return y;
	}

	public long[] flaten(long[][] x) throws Exception{
		//make the matrix to array
		long[] flat_matrix = new long[x.length * x[0].length];
		for(int i = 0; i < x.length; i++){
			for(int j = 0; j < x[0].length; j++) {
				flat_matrix [i * x.length + j] = x[i][j];
				}
			}
		//System.out.println("Original array" + Arrays.toString(x) );
		//System.out.println("flaten array" + Arrays.toString(flat_matrix) );
		return flat_matrix;
		}

	public byte[] flaten_byte(byte[][] x) throws Exception{
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

	public long[][] getWindow(long[][] x, int i, int j, int windowSize) throws Exception{
		//This is for get a window in x at (i,j)
		long[][] sub_x = new long[windowSize][windowSize];
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




		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
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
		
		//test case in XONN - 2 channels 3x3 image
		long[][][][] x = {
				//share 0
				{		
						//for output channel 1 - 3x3
						{{(byte)1, (byte)1, (byte)0},
								{(byte)1, (byte)1, (byte)0},
								{(byte)1, (byte)1, (byte)0},
						},
						//for output channel 2 - 3x3
						{{(byte)1, (byte)1, (byte)0},
								{(byte)1, (byte)1, (byte)0},
								{(byte)1, (byte)1, (byte)0},
						}
				},
				//share 1
				{
						{{(byte)1, (byte)1, (byte)0},
								{(byte)1, (byte)1, (byte)0},
								{(byte)1, (byte)1, (byte)0},
						},
						{{(byte)1, (byte)1, (byte)0},
								{(byte)1, (byte)1, (byte)0},
								{(byte)1, (byte)1, (byte)0},
						}
				}
		};
		
		//weight 3 kernels 2x2 sliding window
		byte[][][] w = {
				//kernel 1
				{{(byte)1, (byte)1},
				 {(byte)1, (byte)1}
				},
				//kernel 2
				{{(byte)1, (byte)1},
				 {(byte)1, (byte)1}
				},
				//kernel 3
				{{(byte)1, (byte)1},
				 {(byte)1, (byte)1}
				},
		};
		
		// output 3 channels 2x2 feature each
		
		//short[] y = new short[5];

		inputConvolutionLayer_old conV = new inputConvolutionLayer_old();
		
		long[][][][] y = conV.compute_conv(false, sndChannel, rcvChannel, mt, x, w);
		//boolean isDisconnect, Server sndChannel, Client rcvChannel,
		//						   eightBitMultiplicationTriple mt, byte[][][] x, byte[][][] w

		
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + conV.timePure / 1e9 + " seconds");
	    System.out.println("bandwidth:" + conV.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		System.out.println("timeNetwork:" + conV.timeNetwork / 1e9 + " seconds");
//		System.out.println("Output:" + Arrays.toString(y));

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

	
	
}
