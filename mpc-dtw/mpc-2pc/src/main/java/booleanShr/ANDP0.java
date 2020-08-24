package booleanShr;

import flexSC.network.Client;
import flexSC.network.Server;

public class ANDP0 {

	private Server sndChannel;
	public byte z0;	
	public byte x0;	
	public byte y0;
	
	public double time=0.0;
	

	public void compute(boolean isDisconnect, Server sndChannel, final ANDTriple mt2, byte x0, byte y0) {
		this.x0 = x0;
		this.y0 = y0;
		this.sndChannel = sndChannel;
		
		// generate e0 f0
		byte[] ef0 = ef0(mt2, x0, y0);
		final byte e0 = ef0[0];
		final byte f0 = ef0[1];
		
		double st1 = System.nanoTime();
		sendP0(e0, f0);
		byte[] ef1P0 = receiveP0();
		
		double et1 = System.nanoTime();
		time = et1-st1;
		
		mulP0(mt2, e0, f0, ef1P0[0], ef1P0[1]);
	}
	public byte[] ef0(ANDTriple mt2, byte x0, byte y0) {
		byte tripleA0 = mt2.tripleA0;
		byte tripleB0 = mt2.tripleB0;
		// long tripleC0 = mt.tripleC0;
		byte e0 = BooleanUtil.xor(x0, tripleA0);// e0= x0 - a0
		byte f0 = BooleanUtil.xor(y0, tripleB0);// f0 = y0 - b0
		byte[] res = new byte[2];
		res[0] = e0;
		res[1] = f0;
		return res;
	}
	

	public void mulP0(ANDTriple mt, byte e0, byte f0, byte e1, byte f1) {
		/*
		 * long tripleA0 = mt.tripleA0; long tripleB0 = mt.tripleB0; long tripleC0 =
		 * mt.tripleC0; long e0 = AdditiveUtil.sub(x0, tripleA0);//e0= x0 - a0 long f0 =
		 * AdditiveUtil.sub(y0, tripleB0);//f0 = y0 - b0
		 */

		byte e = BooleanUtil.xor(e0, e1);//e0+e1
		byte f = BooleanUtil.xor(f0, f1);//f0+f1
		// System.out.println("[P0] e:"+e+" f:"+f);

		this.z0 =  BooleanUtil.xor(BooleanUtil.xor(BooleanUtil.and(f, mt.tripleA0), BooleanUtil.and(e, mt.tripleB0)),
				mt.tripleC0);

	}
	
	public void sendP0(byte e0, byte f0) {
		// System.out.println("[P0] e0:" + e0 + " f0:" + f0+" sndChannel:"+sndChannel);
		sndChannel.writeByte(e0);
		//sndChannel.flush();
		sndChannel.writeByte(f0);
		sndChannel.flush();

	}

	public byte[] receiveP0() {
		byte[] res = new byte[2];
		// res[0] = sndChannel.readBytes(1)[0];// e1
		// res[1] = sndChannel.readBytes(1)[0];// f1

		res = sndChannel.readBytes(2);
		sndChannel.flush();
		// System.out.println("size teste1:"+teste1.length+" testf1:"+testf1.length);
		 //System.out.println("[P0] e1:" + res[0] + " f1:" + res[1]);
		return res;
	}

}
