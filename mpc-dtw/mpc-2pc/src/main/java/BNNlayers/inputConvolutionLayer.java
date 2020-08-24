package BNNlayers;

import additive.ShareGenerator;
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

public class inputConvolutionLayer {

	// inputs:
	// several (feature.len) inputact.row * inputact.column vector (short)
	// several (feature.len) kernel.row * kernel.row matrixs (binary)
	// output:
	// two shares:
	// several (feature.len) outputact.row * outputact.column vectors (short)
	// parameters:
	// stride
	// padding
	// we do not consider bias in our case.

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
	public double timePure = 0.0; // timeAll - timeOTInitial - network;
	public double timeAll = 0.0;

	public int counter = 0;

	public long[][][][] compute_conv(boolean isDisconnect, Server sndChannel, Client rcvChannel,
			MultiplicationTriple mt, long[][][][] x, byte[][][][] old_w) throws Exception {

		double st0 = System.nanoTime();
		// improve w to +w and -w
		byte[][][][][] w = new byte[2][old_w.length][old_w[0].length][old_w[0][0].length][old_w[0][0][0].length];
		for (int old_w_i = 0; old_w_i < old_w.length; old_w_i++) {
			for (int old_w_j = 0; old_w_j < old_w[0].length; old_w_j++) {
				for (int old_w_k = 0; old_w_k < old_w[0][0].length; old_w_k++) {
					for (int old_w_l = 0; old_w_l < old_w[0][0].length; old_w_l++) {
						if (old_w[old_w_i][old_w_j][old_w_k][old_w_l] == 0) {
							// 0 means -1
							w[0][old_w_i][old_w_j][old_w_k][old_w_l] = 0;
							w[1][old_w_i][old_w_j][old_w_k][old_w_l] = 1;
						} else {
							// 1 means +1
							w[0][old_w_i][old_w_j][old_w_k][old_w_l] = 1;
							w[1][old_w_i][old_w_j][old_w_k][old_w_l] = 0;
						}
					}
				}
			}
		}
		double et0 = System.nanoTime();
		this.timeSharingW += et0 - st0;// time to encode w as +w,-w

		// System.out.println("w+ w- generated done!");
		int out_feature = old_w.length;
		int in_feature = x[0].length;
		int kenel_size = old_w[0][0].length;
		int input_row = x[0][0].length;
		int input_column = x[0][0][0].length;

		long[][][][] output_y = new long[2][out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1];
		long[][][][][] temp_output_y = new long[2][2][out_feature][input_row - kenel_size + 1][input_column - kenel_size
				+ 1];
		long[][][][][] total_x_for_y = new long[2][out_feature][input_row - kenel_size + 1][input_column - kenel_size
				+ 1][(in_feature * kenel_size * kenel_size)];// x's channel * kernel_window_size^2
		byte[][][][][][] total_w_for_y = new byte[2][2][out_feature][input_row - kenel_size + 1][input_column
				- kenel_size + 1][(in_feature * kenel_size * kenel_size)];

		SecIntegerBinaryVDPBatch ibvdp = new SecIntegerBinaryVDPBatch();
		// for both x_0 and x_1
		for (int sign_w = 0; sign_w < 2; sign_w++) {
			// for each output channel
			// System.out.println("For w+");
			for (int i = 0; i < out_feature; i++) {
				// build single y matrix
				// System.out.println("For out put channel" + i);

				for (int i_y = 0; i_y < input_row - kenel_size + 1; i_y++) {
					for (int j_y = 0; j_y < input_column - kenel_size + 1; j_y++) {
						// System.out.println("For a single y at " + i_y + ", " + j_y);
						// caculate a single y

						// append w
						for (int j = 0; j < in_feature; j++) {
							// System.out.println("For each input channel" + j);

							// get w

							byte[] w_flat = flaten_byte(w[sign_w][i][j]);
							BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
							byte[] w_0_sub_arr = new byte[w_flat.length];
							byte[] w_1_sub_arr = new byte[w_flat.length];
							// System.out.println(w_flat.length);
							for (int i_w = 0; i_w < w_flat.length; i_w++) {

								double st = System.nanoTime();
								boolGen.generateSharedDataPoint(w_flat[i_w], true);
								double et = System.nanoTime();
								timeSharingW += et - st;

//							byte wi_sub_0 = boolGen.x0;
//							byte wi_sub_1 = boolGen.x1;
								w_0_sub_arr[i_w] = boolGen.x0;
								w_1_sub_arr[i_w] = boolGen.x1;
							}

							System.arraycopy(w_0_sub_arr, 0, total_w_for_y[0][sign_w][i][i_y][j_y],
									j * kenel_size * kenel_size, kenel_size * kenel_size);
							System.arraycopy(w_1_sub_arr, 0, total_w_for_y[1][sign_w][i][i_y][j_y],
									j * kenel_size * kenel_size, kenel_size * kenel_size);

							// append x
							// get one value in a windows
							long[][] x_sub_0 = getWindow(x[0][j], i_y, j_y, kenel_size);
							long[] x_flat_0 = flaten(x_sub_0);
							long[][] x_sub_1 = getWindow(x[1][j], i_y, j_y, kenel_size);
							long[] x_flat_1 = flaten(x_sub_1);
							System.arraycopy(x_flat_0, 0, total_x_for_y[0][i][i_y][j_y], j * kenel_size * kenel_size,
									kenel_size * kenel_size);
							System.arraycopy(x_flat_1, 0, total_x_for_y[1][i][i_y][j_y], j * kenel_size * kenel_size,
									kenel_size * kenel_size);
						}

						double st = System.nanoTime();
						// caculate z
						long[] z = ibvdp.compute(sndChannel, rcvChannel, total_x_for_y[0][i][i_y][j_y],
								total_w_for_y[0][sign_w][i][i_y][j_y], total_x_for_y[1][i][i_y][j_y],
								total_w_for_y[1][sign_w][i][i_y][j_y]);
						double et = System.nanoTime();
						timeAll += et - st;
						counter++;
						if (counter % 100 == 0) {
							System.out.println("Progress:" + counter + " time ibvdp");
						}

						// verify output_y[i][i_y][j_y]
//						long z0_sub = z[0];
//						long z1_sub = z[1];
						temp_output_y[sign_w][0][i][i_y][j_y] = z[0];
						temp_output_y[sign_w][1][i][i_y][j_y] = z[1];

						// long z_total = AdditiveUtil.add(output_y[0][i][i_y][j_y],
						// output_y[1][i][i_y][j_y]);
						// System.out.println("z:" + z_total + " z0:" + output_y[0][i][i_y][j_y] + "
						// z1:" + output_y[1][i][i_y][j_y]);
					}
				}

			}
		}
		for (int i_out_dim = 0; i_out_dim < output_y[0].length; i_out_dim++) {
			for (int j_out_dim = 0; j_out_dim < output_y[0][0].length; j_out_dim++) {
				for (int k_out_dim = 0; k_out_dim < output_y[0][0][0].length; k_out_dim++) {
//					output_y[0][i_out_dim][j_out_dim][k_out_dim] = temp_output_y[0][0][i_out_dim][j_out_dim][k_out_dim] - temp_output_y[1][0][i_out_dim][j_out_dim][k_out_dim];
//					output_y[1][i_out_dim][j_out_dim][k_out_dim] = temp_output_y[0][1][i_out_dim][j_out_dim][k_out_dim] - temp_output_y[1][1][i_out_dim][j_out_dim][k_out_dim];
					output_y[0][i_out_dim][j_out_dim][k_out_dim] = AdditiveUtil.sub(
							temp_output_y[0][0][i_out_dim][j_out_dim][k_out_dim],
							temp_output_y[1][0][i_out_dim][j_out_dim][k_out_dim]);
					output_y[1][i_out_dim][j_out_dim][k_out_dim] = AdditiveUtil.sub(
							temp_output_y[0][1][i_out_dim][j_out_dim][k_out_dim],
							temp_output_y[1][1][i_out_dim][j_out_dim][k_out_dim]);
					// System.out.println("feature:" + i_out_dim + j_out_dim + k_out_dim + ": " +
					// (AdditiveUtil.add(output_y[0][i_out_dim][j_out_dim][k_out_dim] ,
					// output_y[1][i_out_dim][j_out_dim][k_out_dim])));
				}
			}
		}

		this.timeOTInitial = ibvdp.timeOTInitial;
		this.timeNetwork = ibvdp.timeNetwork;
		this.timePure = this.timeAll - timeOTInitial - timeNetwork;
		this.bandwidth = ibvdp.bandwidth;

		return output_y;
	}

	public long[][][][] compute_conv_padding1(boolean isDisconnect, Server sndChannel, Client rcvChannel,
			MultiplicationTriple mt, long[][][][] old_x, byte[][][][] old_w) throws Exception {

		double st0 = System.nanoTime();
		// improve w to +w and -w
		byte[][][][][] w = new byte[2][old_w.length][old_w[0].length][old_w[0][0].length][old_w[0][0][0].length];
		for (int old_w_i = 0; old_w_i < old_w.length; old_w_i++) {
			for (int old_w_j = 0; old_w_j < old_w[0].length; old_w_j++) {
				for (int old_w_k = 0; old_w_k < old_w[0][0].length; old_w_k++) {
					for (int old_w_l = 0; old_w_l < old_w[0][0].length; old_w_l++) {
						if (old_w[old_w_i][old_w_j][old_w_k][old_w_l] == 0) {
							// 0 means -1
							w[0][old_w_i][old_w_j][old_w_k][old_w_l] = 0;
							w[1][old_w_i][old_w_j][old_w_k][old_w_l] = 1;
						} else {
							// 1 means +1
							w[0][old_w_i][old_w_j][old_w_k][old_w_l] = 1;
							w[1][old_w_i][old_w_j][old_w_k][old_w_l] = 0;
						}
					}
				}
			}
		}
		double et0 = System.nanoTime();
		this.timeSharingW += et0 - st0;

		// padding and generate new x
		long[][][][] x = new long[2][old_x[0].length][old_x[0][0].length + 2][old_x[0][0][0].length + 2];
		for (int x_share = 0; x_share < 2; x_share++) {
			for (int x_channel = 0; x_channel < old_x[0].length; x_channel++) {
				for (int x_i = 0; x_i < old_x[0][0].length; x_i++) {
					for (int x_j = 0; x_j < old_x[0][0][0].length; x_j++) {
						if ((x_i == 0) || (x_i == (old_x[0][0].length - 1)) || (x_j == 0)
								|| (x_j == (old_x[0][0][0].length - 1))) {
							x[x_share][x_channel][x_i][x_j] = 0;
						} else {
							x[x_share][x_channel][x_i][x_j] = old_x[x_share][x_channel][x_i][x_j];
						}
					}
				}
			}
		}

		// System.out.println("w+ w- generated done!");
		int out_feature = old_w.length;
		int in_feature = x[0].length;
		int kenel_size = old_w[0][0].length;
		int input_row = x[0][0].length;
		int input_column = x[0][0][0].length;

		long[][][][] output_y = new long[2][out_feature][input_row - kenel_size + 1][input_column - kenel_size + 1];
		long[][][][][] temp_output_y = new long[2][2][out_feature][input_row - kenel_size + 1][input_column - kenel_size
				+ 1];
		long[][][][][] total_x_for_y = new long[2][out_feature][input_row - kenel_size + 1][input_column - kenel_size
				+ 1][(in_feature * kenel_size * kenel_size)];// x's channel * kernel_window_size^2
		byte[][][][][][] total_w_for_y = new byte[2][2][out_feature][input_row - kenel_size + 1][input_column
				- kenel_size + 1][(in_feature * kenel_size * kenel_size)];

		SecIntegerBinaryVDPBatch ibvdp = new SecIntegerBinaryVDPBatch();
		// for both x_0 and x_1
		for (int sign_w = 0; sign_w < 2; sign_w++) {
			// for each output channel
			// System.out.println("For w+");
			for (int i = 0; i < out_feature; i++) {
				// build single y matrix
				// System.out.println("For out put channel" + i);

				for (int i_y = 0; i_y < input_row - kenel_size + 1; i_y++) {
					for (int j_y = 0; j_y < input_column - kenel_size + 1; j_y++) {
						// System.out.println("For a single y at " + i_y + ", " + j_y);
						// caculate a single y

						// append w
						for (int j = 0; j < in_feature; j++) {
							// System.out.println("For each input channel" + j);

							// get w
							byte[] w_flat = flaten_byte(w[sign_w][i][j]);
							BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
							byte[] w_0_sub_arr = new byte[w_flat.length];
							byte[] w_1_sub_arr = new byte[w_flat.length];
							// System.out.println(w_flat.length);

							double st = System.nanoTime();
							for (int i_w = 0; i_w < w_flat.length; i_w++) {
								boolGen.generateSharedDataPoint(w_flat[i_w], true);
								// byte wi_sub_0 = boolGen.x0;
								// byte wi_sub_1 = boolGen.x1;
								w_0_sub_arr[i_w] = boolGen.x0;
								w_1_sub_arr[i_w] = boolGen.x1;
							}
							double et = System.nanoTime();
							this.timeSharingW += et - st;

							System.arraycopy(w_0_sub_arr, 0, total_w_for_y[0][sign_w][i][i_y][j_y],
									j * kenel_size * kenel_size, kenel_size * kenel_size);
							System.arraycopy(w_1_sub_arr, 0, total_w_for_y[1][sign_w][i][i_y][j_y],
									j * kenel_size * kenel_size, kenel_size * kenel_size);

							// append x
							// get one value in a windows
							long[][] x_sub_0 = getWindow(x[0][j], i_y, j_y, kenel_size);
							long[] x_flat_0 = flaten(x_sub_0);
							long[][] x_sub_1 = getWindow(x[1][j], i_y, j_y, kenel_size);
							long[] x_flat_1 = flaten(x_sub_1);
							System.arraycopy(x_flat_0, 0, total_x_for_y[0][i][i_y][j_y], j * kenel_size * kenel_size,
									kenel_size * kenel_size);
							System.arraycopy(x_flat_1, 0, total_x_for_y[1][i][i_y][j_y], j * kenel_size * kenel_size,
									kenel_size * kenel_size);
						}

						// caculate z
						double st = System.nanoTime();
						long[] z = ibvdp.compute(sndChannel, rcvChannel, total_x_for_y[0][i][i_y][j_y],
								total_w_for_y[0][sign_w][i][i_y][j_y], total_x_for_y[1][i][i_y][j_y],
								total_w_for_y[1][sign_w][i][i_y][j_y]);
						double et = System.nanoTime();
						this.timeAll += et - st;
						counter++;
						if (counter % 100 == 0) {
							System.out.println("Progress:" + counter + " time ibvdp");
						}

						// verify output_y[i][i_y][j_y]
						// long z0_sub = z[0];
						// long z1_sub = z[1];
						temp_output_y[sign_w][0][i][i_y][j_y] = z[0];
						temp_output_y[sign_w][1][i][i_y][j_y] = z[1];

						// long z_total = AdditiveUtil.add(output_y[0][i][i_y][j_y],
						// output_y[1][i][i_y][j_y]);
						// System.out.println("z:" + z_total + " z0:" + output_y[0][i][i_y][j_y] + "
						// z1:" + output_y[1][i][i_y][j_y]);
					}
				}

			}
		}
		for (int i_out_dim = 0; i_out_dim < output_y[0].length; i_out_dim++) {
			for (int j_out_dim = 0; j_out_dim < output_y[0][0].length; j_out_dim++) {
				for (int k_out_dim = 0; k_out_dim < output_y[0][0][0].length; k_out_dim++) {
					// output_y[0][i_out_dim][j_out_dim][k_out_dim] =
					// temp_output_y[0][0][i_out_dim][j_out_dim][k_out_dim] -
					// temp_output_y[1][0][i_out_dim][j_out_dim][k_out_dim];
					// output_y[1][i_out_dim][j_out_dim][k_out_dim] =
					// temp_output_y[0][1][i_out_dim][j_out_dim][k_out_dim] -
					// temp_output_y[1][1][i_out_dim][j_out_dim][k_out_dim];
					output_y[0][i_out_dim][j_out_dim][k_out_dim] = AdditiveUtil.sub(
							temp_output_y[0][0][i_out_dim][j_out_dim][k_out_dim],
							temp_output_y[1][0][i_out_dim][j_out_dim][k_out_dim]);
					output_y[1][i_out_dim][j_out_dim][k_out_dim] = AdditiveUtil.sub(
							temp_output_y[0][1][i_out_dim][j_out_dim][k_out_dim],
							temp_output_y[1][1][i_out_dim][j_out_dim][k_out_dim]);
					// System.out.println("feature:" + i_out_dim + j_out_dim + k_out_dim + ": " +
					// (AdditiveUtil.add(output_y[0][i_out_dim][j_out_dim][k_out_dim] ,
					// output_y[1][i_out_dim][j_out_dim][k_out_dim])));
				}
			}
		}

		this.timeOTInitial = ibvdp.timeOTInitial;
		this.timeNetwork = ibvdp.timeNetwork;
		this.timePure = this.timeAll - timeOTInitial - timeNetwork;
		this.bandwidth = ibvdp.bandwidth;

		return output_y;
	}

	public long[][] compute_fc(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple mt,
			long[][] x, byte[][] old_w) throws Exception {

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);

		
		double st0 = System.nanoTime();
		// improve w to +w and -w
		byte[][][] w = new byte[2][old_w.length][x[0].length];
		for (int old_w_i = 0; old_w_i < old_w.length; old_w_i++) {
			for (int old_w_j = 0; old_w_j < old_w[0].length; old_w_j++) {
				if (old_w[old_w_i][old_w_j] == 0) {
					// 0 means -1
					w[0][old_w_i][old_w_j] = 0;
					w[1][old_w_i][old_w_j] = 1;
				} else {
					// 1 means +1
					w[0][old_w_i][old_w_j] = 1;
					w[1][old_w_i][old_w_j] = 0;
				}
			}
		}
		double et0 = System.nanoTime();
		this.timeSharingW += et0-st0;
		

		// define two shares
		byte[][][] w_0 = new byte[2][w[0].length][x[0].length];
		byte[][][] w_1 = new byte[2][w[0].length][x[0].length];
		long[][] y = new long[2][w[0].length];
		long[][][] y_temp = new long[2][2][w[0].length]; // 2 is 2 sign, 2 is 2 shares, w[0].length is the output
															// dimension

//		byte[] w_0_single_neural = new byte[w[0].length];
//		byte[] w_1_single_neural = new byte[w[0].length];

		// seperate y

		SecIntegerBinaryVDPBatch ibvdp = new SecIntegerBinaryVDPBatch(); // y_temp[sign_w]

		for (int sign_w = 0; sign_w < 2; sign_w++) {

			for (int j = 0; j < w[0].length; j++) {

				// for each bit
				for (int i = 0; i < x[0].length; i++) {

					// System.out.println(sign_w +" " + j +" " + i);

					double st = System.nanoTime();
					boolGen.generateSharedDataPoint(w[sign_w][j][i], true);
					double et = System.nanoTime();
					timeSharingW += et - st;

					byte wi_0 = boolGen.x0;
					byte wi_1 = boolGen.x1;
					// System.out.println("i" + i + " wi:" + w[i] + " wi_0:" + wi_0 + " wi_1:" +
					// wi_1);
					w_0[sign_w][j][i] = wi_0;
					w_1[sign_w][j][i] = wi_1;
				}
				// comput each output

//			System.out.println("j:" + j);
//			System.out.println("x0:" + Arrays.toString(x[0]) + "x1:" + Arrays.toString(x[1]));
//			System.out.println("w0:" + Arrays.toString(w_0[j]) + "w1:" + Arrays.toString(w_1[j]));

				double st = System.nanoTime();
				long[] z = ibvdp.compute(sndChannel, rcvChannel, x[0], w_0[sign_w][j], x[1], w_1[sign_w][j]);
				double et = System.nanoTime();
				timeAll += et - st;
				counter++;
				if (counter % 100 == 0) {
					System.out.println("Progress:" + counter + " time ibvdp");
				}

				// verify
//			long z0 = z[0];
//			long z1 = z[1];
				y_temp[sign_w][0][j] = z[0];
				y_temp[sign_w][1][j] = z[1];
				// y[j] = eightBitAdditiveUtil.add(z0, z1);

//			System.out.println("z:" + (z0 + z1) + " z0:" + z0 + " z1:" + z1);
//				System.out.println("feature:" + j);
//				System.out.println("x:" + Arrays.toString(x) + " x0:" + Arrays.toString(x[0]) + " x1:" + Arrays.toString(x[1]));
//				System.out.println("w:" + Arrays.toString(w[j])+ " w0:" + Arrays.toString(w_0[j]) + " w1:" + Arrays.toString(w_1[j]));
//				System.out.println("z:" + AdditiveUtil.add(z[0] , z[1]) + " z0:" + z[0] + " z1:" + z[1]);
//			sndChannel.disconnect();
//			rcvChannel.disconnect();

			}
		}

		// caculate output
		for (int out_dim = 0; out_dim < w[0].length; out_dim++) {
			y[0][out_dim] = AdditiveUtil.sub(y_temp[0][0][out_dim], y_temp[1][0][out_dim]);
			y[1][out_dim] = AdditiveUtil.sub(y_temp[0][1][out_dim], y_temp[1][1][out_dim]);
			// System.out.println("feature:" + (AdditiveUtil.add(y[0][out_dim] ,
			// y[1][out_dim])));
		}

		// System.out.println("feature:" + Arrays.toString(y[0]));
		// System.out.println("feature:" + Arrays.toString(y[1]));

		double st1 = System.nanoTime();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		double et1 = System.nanoTime();
		timeNetwork += et1 - st1;

		this.timeOTInitial = ibvdp.timeOTInitial;
		this.timeNetwork = ibvdp.timeNetwork;
		this.timePure = this.timeAll - timeOTInitial - timeNetwork;
		this.bandwidth = ibvdp.bandwidth;

		System.out.println("input fc timePure:" + timePure / 1e9 + " s");
		System.out.println("timeAll:" + timeAll / 1e9 + " s");
		System.out.println("timeOT:" + timeOTInitial / 1e9 + " s");
		System.out.println("timeNetwork:" + timeNetwork / 1e9 + " s");

		return y;
	}

	public long[] flaten(long[][] x) throws Exception {
		// make the matrix to array
		long[] flat_matrix = new long[x.length * x[0].length];
		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[0].length; j++) {
				flat_matrix[i * x.length + j] = x[i][j];
			}
		}
		// System.out.println("Original array" + Arrays.toString(x) );
		// System.out.println("flaten array" + Arrays.toString(flat_matrix) );
		return flat_matrix;
	}

	public byte[] flaten_byte(byte[][] x) throws Exception {
		// make the matrix to array
		byte[] flat_matrix = new byte[x.length * x[0].length];
		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[0].length; j++) {
				flat_matrix[i * x.length + j] = x[i][j];
			}
		}
		// System.out.println("Original array" + Arrays.toString(x) );
		// System.out.println("flaten array" + Arrays.toString(flat_matrix) );
		return flat_matrix;
	}

	public long[][] getWindow(long[][] x, int i, int j, int windowSize) throws Exception {
		// This is for get a window in x at (i,j)
		long[][] sub_x = new long[windowSize][windowSize];
		for (int p = 0; p < windowSize; p++) {
			for (int q = 0; q < windowSize; q++) {
				sub_x[p][q] = x[p + i][q + j];
			}
		}
		return sub_x;
	}

	// private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	// 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	// 0 };
	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0 };

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

		// byte[] x = {(byte)1, (byte)1, (byte)0, (byte)0, (byte)0};
		// byte[] w = {(byte)0, (byte)1, (byte)0, (byte)0, (byte)1};

		// test case in XONN
		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ShareGenerator ariGen = new ShareGenerator(true);

		// long[] x = { 255, 255, 255, 255, 255, 255, 255, 255, 255,255, 255, 255, 255,
		// 255, 255, 255, 255, 255, 255,255 };//{ 1, 2, 3, 4, 5, 6, 7, 8, 9,10, 1, 2, 3,
		// 4, 5, 6, 7, 8, 9,10 };// 123456789
		long[] x = { 86, 91, 80, 89, 100, 82, 87, 85, 97, 79, 94, 96, 88, 82, 92, 82, 92, 91, 92, 93, 64, 94, 103, 80,
				86, 92, 94, 0, 97, 82, 103, 97, 68, 111, 82, 94, 103, 0, 3, 0, 0, 5, 84, 101, 93, 63, 102, 94, 9, 23,
				57, 11, 3, 6, 12, 0, 99, 78, 0, 0, 10, 0, 2, 0, 0, 4, 85, 81, 79, 89, 108, 74, 0, 18, 0, 0, 0, 0, 3, 0,
				0, 2, 0, 0, 81, 93, 5, 10, 0, 80, 4, 10, 6, 79, 97, 15, 8, 84, 72, 92, 10, 0, 0, 13, 7, 85, 104, 79, 15,
				10, 4, 0, 92, 0, 2, 6, 80, 91, 103, 70, 0, 6, 2, 0, 23, 6, 1, 7, 0, 78, 4, 79, 1, 1, 92, 0, 0, 0, 22, 0,
				92, 80, 0, 0, 103, 4, 0, 14, 86, 87, 93, 110, 0, 0, 0, 89, 101, 9, 0, 19, 0, 100, 82, 1, 14, 18, 74, 5,
				5, 6, 91, 1, 0, 0, 13, 0, 11, 79, 81, 84, 98, 0, 105, 0, 80, 88, 96, 82, 95, 84, 0, 13, 77, 91, 83, 0,
				83, 98, 0, 1, 0, 4, 4, 88, 55, 74, 151, 0, 4, 73, 4, 0, 92, 91, 82, 92, 86, 3, 3, 0, 93, 79, 15, 0, 94,
				0, 0, 4, 0, 0, 136, 255, 167, 166, 153, 162, 248, 245, 255, 105, 112, 108, 255, 104, 75, 9, 0, 97, 82,
				9, 0, 0, 88, 93, 0, 3, 92, 108, 143, 212, 0, 26, 85, 134, 164, 255, 243, 170, 153, 251, 255, 255, 163,
				50, 0, 1, 2, 94, 92, 0, 0, 0, 13, 0, 86, 90, 18, 0, 5, 80, 85, 0, 168, 95, 163, 153, 168, 131, 135, 246,
				157, 30, 5, 1, 79, 99, 86, 11, 94, 0, 12, 25, 82, 84, 78, 97, 87, 100, 87, 17, 0, 88, 94, 87, 0, 0, 0,
				173, 123, 93, 118, 72, 94, 10, 0, 1, 86, 106, 0, 0, 6, 77, 102, 83, 84, 83, 8, 0, 0, 31, 0, 93, 96, 115,
				151, 155, 169, 88, 75, 100, 3, 0, 3, 0, 78, 96, 10, 4, 6, 0, 0, 5, 94, 0, 0, 102, 107, 75, 89, 90, 85,
				199, 168, 248, 146, 1, 0, 7, 4, 0, 12, 1, 100, 68, 97, 0, 0, 12, 76, 90, 100, 87, 78, 0, 83, 107, 74,
				95, 146, 173, 174, 147, 76, 0, 18, 0, 18, 0, 6, 0, 94, 81, 90, 14, 0, 7, 92, 92, 0, 82, 98, 85, 89, 81,
				88, 91, 222, 157, 97, 86, 95, 2, 0, 0, 0, 97, 97, 0, 0, 4, 0, 18, 0, 87, 100, 75, 6, 0, 3, 99, 84, 80,
				111, 0, 119, 152, 146, 2, 13, 0, 10, 89, 86, 92, 94, 0, 98, 84, 0, 0, 1, 0, 8, 90, 0, 9, 88, 93, 79, 92,
				0, 45, 162, 111, 0, 9, 0, 0, 92, 88, 5, 0, 5, 0, 73, 108, 88, 4, 0, 8, 0, 101, 21, 0, 102, 78, 95, 18,
				0, 153, 138, 9, 0, 0, 10, 0, 79, 97, 0, 0, 19, 0, 97, 82, 10, 0, 0, 8, 0, 0, 0, 89, 81, 101, 83, 0, 124,
				171, 255, 93, 9, 76, 99, 0, 1, 92, 0, 8, 71, 0, 5, 0, 0, 3, 0, 86, 14, 0, 13, 97, 83, 5, 0, 108, 156,
				255, 118, 85, 75, 20, 87, 8, 0, 0, 5, 0, 104, 13, 91, 104, 0, 1, 4, 90, 74, 10, 0, 20, 0, 3, 0, 175,
				167, 162, 83, 85, 23, 0, 0, 86, 9, 10, 2, 0, 82, 0, 81, 74, 10, 2, 0, 15, 2, 0, 13, 0, 81, 0, 134, 168,
				202, 0, 0, 104, 69, 12, 0, 94, 101, 76, 86, 91, 96, 9, 100, 91, 1, 0, 10, 82, 105, 0, 0, 99, 83, 230,
				255, 248, 146, 0, 93, 81, 86, 6, 0, 92, 0, 102, 0, 14, 0, 0, 1, 0, 7, 0, 0, 13, 77, 97, 3, 69, 154, 255,
				166, 152, 15, 0, 96, 0, 4, 0, 3, 91, 0, 2, 6, 0, 20, 0, 2, 0, 6, 4, 1, 0, 11, 0, 0, 97, 200, 255, 158,
				122, 0, 0, 0, 2, 29, 62, 7, 11, 0, 0, 0, 22, 77, 2, 4, 0, 0, 1, 2, 0, 21, 2, 89, 84, 236, 227, 255, 113,
				72, 99, 0, 2, 0, 25, 0, 0, 19, 4, 24, 54, 92, 13, 0, 0, 1, 0, 0, 6, 79, 89, 89, 89, 75, 99, 80, 91, 3,
				90, 104, 92, 83, 82, 4, 2, 0, 0, 5, 0, 19, 0 };

//		long[] x = {1,2,3};
//		byte[][] w = {
//				{1,1,1},
//				{1,0,1},
//				{0,1,0},
//				{0,0,1}
//		};

//		byte[][] w = {
//				{ (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte)0,(byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte)1 },
//				{ (byte) 1, (byte) 0, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte)1,(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte)0 }
//		};// 45
		readingWeights rd_w = new readingWeights();
		rd_w.modelfileName = "./resources/BM1_no_bias.json";
		byte[][] a = rd_w.get_weight_fc("fc1.weight");
		byte[][] w = new byte[1][a[0].length];
		w[0] = a[0];
		// ShareGenerator ariGen = new ShareGenerator(true);
		long[] x_0_arr = new long[x.length];
		long[] x_1_arr = new long[x.length];
		long[][] x_share = new long[2][x.length];

		for (int i = 0; i < x.length; i++) {
			ariGen.generateSharedDataPoint(x[i], true);
			long xi_0 = ariGen.x0;
			long xi_1 = ariGen.x1;
			x_0_arr[i] = xi_0;
			x_1_arr[i] = xi_1;
		}
		x_share[0] = x_0_arr;
		x_share[1] = x_1_arr;
		System.out.println(Arrays.toString(x));
		System.out.println(Arrays.toString(a[0]));

		inputConvolutionLayer fc1 = new inputConvolutionLayer();
		long[][] out = fc1.compute_fc(false, sndChannel, rcvChannel, mt, x_share, w);

//		long[][][][] x = {
//				{
//						{{(byte)1, (byte)1, (byte)0},
//								{(byte)1, (byte)1, (byte)0},
//								{(byte)1, (byte)1, (byte)0},
//						},
//						{{(byte)1, (byte)1, (byte)0},
//								{(byte)1, (byte)1, (byte)0},
//								{(byte)1, (byte)1, (byte)0},
//						}
//				},
//				{
//						{{(byte)1, (byte)1, (byte)0},
//								{(byte)1, (byte)1, (byte)0},
//								{(byte)1, (byte)1, (byte)0},
//						},
//						{{(byte)1, (byte)1, (byte)0},
//								{(byte)1, (byte)1, (byte)0},
//								{(byte)1, (byte)1, (byte)0},
//						}
//				}
//		};
//		byte[][][][] w = {{
//				{{(byte)1, (byte)1},
//				 {(byte)1, (byte)1}
//				},
//				{{(byte)1, (byte)1},
//				 {(byte)1, (byte)1}
//				},
//				{{(byte)1, (byte)1},
//				 {(byte)1, (byte)1}
//				},
//		},
//		{
//			{{(byte)0, (byte)0},
//				{(byte)0, (byte)0}
//			},
//			{{(byte)0, (byte)0},
//				{(byte)0, (byte)0}
//			},
//			{{(byte)0, (byte)0},
//				{(byte)0, (byte)0}
//			},
//		}};
//		//short[] y = new short[5];
//
//		inputConvolutionLayer conV = new inputConvolutionLayer();
//		long time = 0;
//		//double bandwidth = 0.0;
//		long e = System.nanoTime();
//		long[][][][] y = conV.compute_conv(false, sndChannel, rcvChannel, mt, x, w);
//		//boolean isDisconnect, Server sndChannel, Client rcvChannel,
//		//						   eightBitMultiplicationTriple mt, byte[][][] x, byte[][][] w
//
//		long s = System.nanoTime();
//		time += s - e;
//		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
//		System.out.println("time:" + time / 1e9 + " seconds");
////		System.out.println("bandwidth:" + xnorp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
////		System.out.println("timeNetwork:" + xnorp.timeNetwork / 1e9 + " seconds");
////		System.out.println("Output:" + Arrays.toString(y));

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

}
