package additive;

import flexSC.network.Client;
import flexSC.network.Server;

public class MulP0 {
	private Server sndChannel;
	public long z0;
	public long x0;
	public long y0;

	public double time = 0.0;
	
	
	public void compute(boolean isDisconnect, Server sndChannel, MultiplicationTriple mt,	long x0, long y0) {
		this.x0 = x0;
		this.y0 = y0;
		this.sndChannel = sndChannel;
		
		// generate e0 f0
				long[] ef0 = this.ef0(mt, x0, y0);
				final long e0 = ef0[0];
				final long f0 = ef0[1];			
				
				double st = System.nanoTime();
				sendP0(e0, f0);
				long[] ef1P0 = receiveP0();
				double et = System.nanoTime();
				time = et-st;
				
				mulP0(mt, e0, f0, ef1P0[0], ef1P0[1]);
		
	}
	
	public void sendP0(long e0, long f0) {
		//System.out.println("[P0] e0:" + e0 + " f0:" + f0+" sndChannel:"+sndChannel);
		sndChannel.writeLong(e0);
		sndChannel.writeLong(f0);
		sndChannel.flush();

	}
	
	public long[] receiveP0() {
		long[] res = new long[2];
		res[0] = sndChannel.readLong();// e1
		res[1] = sndChannel.readLong();// f1
		sndChannel.flush();
		// System.out.println("[P0] e1:" + res[0] + " f1:" + res[1]);
		return res;
	}
	
	public long[] ef0(MultiplicationTriple mt, long x0, long y0) {
		long tripleA0 = mt.tripleA0;
		long tripleB0 = mt.tripleB0;
		// long tripleC0 = mt.tripleC0;
		long e0 = AdditiveUtil.sub(x0, tripleA0);// e0= x0 - a0
		long f0 = AdditiveUtil.sub(y0, tripleB0);// f0 = y0 - b0
		long[] res = new long[2];
		res[0] = e0;
		res[1] = f0;
		return res;
	}

	public void mulP0(MultiplicationTriple mt, long e0, long f0, long e1, long f1) {
		/*
		 * long tripleA0 = mt.tripleA0; long tripleB0 = mt.tripleB0; long tripleC0 =
		 * mt.tripleC0; long e0 = AdditiveUtil.sub(x0, tripleA0);//e0= x0 - a0 long f0 =
		 * AdditiveUtil.sub(y0, tripleB0);//f0 = y0 - b0
		 */

		long e = AdditiveUtil.add(e0, e1);
		long f = AdditiveUtil.add(f0, f1);
		// System.out.println("[P0] e:"+e+" f:"+f);

		this.z0 = AdditiveUtil.add(AdditiveUtil.add(AdditiveUtil.mul(f, mt.tripleA0), AdditiveUtil.mul(e, mt.tripleB0)),
				mt.tripleC0);
		// System.out.println("z0:"+z0);
		// return z0;
	}
}
