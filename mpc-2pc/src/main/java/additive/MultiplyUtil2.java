package additive;

import flexSC.network.Client;
import flexSC.network.Server;

public class MultiplyUtil2 {
	
	private Server sndChannel;
	private Client rcvChannel;
	public long z0;
	public long z1;
	
	public MultiplyUtil2(Server sndChannel, Client rcvChannel) {
		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;
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

	public long[] ef0(MultiplicationTriple2 mt, long x0, long y0) {
		long tripleA0 = mt.tripleA0;
		long tripleB0 = mt.tripleB0;
		// long tripleC0 = mt.tripleC0;
		long e0 = AdditiveUtil2.sub(x0, tripleA0);// e0= x0 - a0
		long f0 = AdditiveUtil2.sub(y0, tripleB0);// f0 = y0 - b0
		long[] res = new long[2];
		res[0] = e0;
		res[1] = f0;
		return res;
	}

	public void mulP0(MultiplicationTriple2 mt, long e0, long f0, long e1, long f1) {
		/*
		 * long tripleA0 = mt.tripleA0; long tripleB0 = mt.tripleB0; long tripleC0 =
		 * mt.tripleC0; long e0 = AdditiveUtil.sub(x0, tripleA0);//e0= x0 - a0 long f0 =
		 * AdditiveUtil.sub(y0, tripleB0);//f0 = y0 - b0
		 */

		long e = AdditiveUtil2.add(e0, e1);
		long f = AdditiveUtil2.add(f0, f1);
		// System.out.println("[P0] e:"+e+" f:"+f);

		this.z0 = AdditiveUtil2.add(AdditiveUtil2.add(AdditiveUtil2.mul(f, mt.tripleA0), AdditiveUtil2.mul(e, mt.tripleB0)),
				mt.tripleC0);
		// System.out.println("z0:"+z0);
		// return z0;
	}

	public void sendP1(long e1, long f1) {
		//System.out.println("[P1] e1:" + e1 + " f1:" + f1 + " rcvChannel:" + rcvChannel);
		rcvChannel.writeLong(e1);
		rcvChannel.writeLong(f1);
		rcvChannel.flush();

	}

	public long[] receiveP1() {
		long[] res = new long[2];
		res[0] = rcvChannel.readLong();// e0
		res[1] = rcvChannel.readLong();// f0
		rcvChannel.flush();
		// System.out.println("[P1] e0:" + res[0] + " f0:" + res[1]);
		return res;
	}

	public long[] ef1(MultiplicationTriple2 mt, long x1, long y1) {
		long tripleA1 = mt.tripleA1;
		long tripleB1 = mt.tripleB1;

		long e1 = AdditiveUtil2.sub(x1, tripleA1);
		long f1 = AdditiveUtil2.sub(y1, tripleB1);
		long[] res = new long[2];
		res[0] = e1;
		res[1] = f1;
		return res;
	}

	public void mulP1(MultiplicationTriple2 mt, long e1, long f1, long e0, long f0) {
		/*
		 * long tripleA1 = mt.tripleA1; long tripleB1 = mt.tripleB1;
		 * 
		 * long e1 = AdditiveUtil.sub(x1, tripleA1); long f1 = AdditiveUtil.sub(y1,
		 * tripleB1);
		 */

		/*
		 * long e0 = sndChannel.readLong(); long f0 = sndChannel.readLong();
		 * sndChannel.flush(); System.out.println("e0:"+e0+" f0:"+f0);
		 */
		long tripleC1 = mt.tripleC1;
		long e = AdditiveUtil2.add(e0, e1);
		long f = AdditiveUtil2.add(f0, f1);
		// System.out.println("[P1] e:"+e+" f:"+f);
		long ief = AdditiveUtil2.mul(e, f);

		this.z1 = AdditiveUtil2.add(ief, AdditiveUtil2
				.add(AdditiveUtil2.add(AdditiveUtil2.mul(f, mt.tripleA1), AdditiveUtil2.mul(e, mt.tripleB1)), tripleC1));
		// System.out.println("z1:"+z1);
		// return z1;
	}

}
