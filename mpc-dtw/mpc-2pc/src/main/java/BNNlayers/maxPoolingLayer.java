package BNNlayers;

import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBNN.SecMaxPooling;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class maxPoolingLayer {

	//input:
	public byte[][][] x;

	//output:
	public byte[][][] y;
	//parameter:
	public int stride = 2;

	private Server sndChannel;
	private Client rcvChannel;
	public byte z0AND;
	public byte z1AND;

	public byte a0 = 0;
	public byte a1 = 0;

	public double bandwidth = 0.0;
	public double timeNetwork = 0.0;
	public double timePure = 0.0;
	public double timeAll = 0.0;

	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0 };

	public byte[][][][] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			byte[][][][] x) throws Exception {

		//maxpool:
		//input:
		//x[channel][length][width]
		//output:
		//y[channel][length/2][width/2]
		int channel = x[0].length;
		int length = x[0][0].length;
		int width = x[0][0][0].length;

		byte[][][][] out_y = new byte[2][channel][(int) Math.ceil(length/2)][(int) Math.ceil(width/2)];
		//byte[][][][] in_x = new byte[channel][(int) Math.ceil(length/2)][(int) Math.ceil(width/2)][stride*stride];

		SecMaxPooling maxp = new SecMaxPooling();
		//each channel
		for(int i=0; i<channel; i++){

			for(int i_y=0; i_y<(length/2); i_y++){
				for(int j_y=0; j_y<(width/2); j_y++){
					//each y
					byte[][] x_sub_0 = getWindow(x[0][i],i_y*2,j_y*2,stride);
					byte[] x_flat_0 = flaten(x_sub_0);

					byte[][] x_sub_1 = getWindow(x[1][i],i_y*2,j_y*2,stride);
					byte[] x_flat_1 = flaten(x_sub_1);

					double st = System.nanoTime();
					byte[] z_sub = maxp.computeSingleVector(false, sndChannel, rcvChannel, mt2,  x_flat_0,  x_flat_1);
					double et = System.nanoTime();
					timeAll += et-st;
					
					
					out_y[0][i][i_y][j_y] = z_sub[0];
					out_y[1][i][i_y][j_y] = z_sub[1];

					//out_y[i][i_y][j_y] = BooleanUtil.xor(z_sub[0], z_sub[1]);
					//System.out.println("z:" + BooleanUtil.xor(z_sub[0], z_sub[1]) + " z0:" + z_sub[0] + " z1:" + z_sub[1]);
				}
			}
		}
		
		this.bandwidth = maxp.bandwidth;
		this.timeNetwork = maxp.timeNetwork;
		this.timePure = this.timeAll - this.timeNetwork;
		
		return out_y;
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
		byte[][][] x = {
				{{(byte)1, (byte)1, (byte)0, (byte)0},
				 {(byte)1, (byte)1, (byte)1, (byte)0},
				 {(byte)0, (byte)0, (byte)0, (byte)0},
				 {(byte)0, (byte)0, (byte)0, (byte)0}
				},
				{{(byte)1, (byte)1, (byte)0, (byte)0},
				 {(byte)1, (byte)1, (byte)0, (byte)0},
				 {(byte)1, (byte)1, (byte)1, (byte)0},
				 {(byte)1, (byte)1, (byte)0, (byte)0}
				},
		};

		maxPoolingLayer secmp = new maxPoolingLayer();// y, u
		int round = 1;
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {			
			//extract MSB of a
			//byte[][][] z = secmp.compute(false, sndChannel, rcvChannel, mt2,  x);
			// verify
			//System.out.println("z:" + Arrays.toString(z));

		}
		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + secmp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		System.out.println("timeNetwork:" + secmp.timeNetwork / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

	
	
}
