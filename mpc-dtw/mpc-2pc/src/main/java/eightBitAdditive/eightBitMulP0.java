package eightBitAdditive;

import additive.AdditiveUtil;
import eightBitAdditive.eightBitMultiplicationTriple;
import flexSC.network.Server;

public class eightBitMulP0 {
	private Server sndChannel;
	public short z0;
	public short x0;
	public short y0;

	
	
	public void compute(boolean isDisconnect, Server sndChannel, eightBitMultiplicationTriple mt,	short x0, short y0) {
		this.x0 = x0;
		this.y0 = y0;
		this.sndChannel = sndChannel;
		
		// generate e0 f0
				short[] ef0 = this.ef0(mt, x0, y0);
				final short e0 = ef0[0];
				final short f0 = ef0[1];
				
				sendP0(e0, f0);
				short[] ef1P0 = receiveP0();
				mulP0(mt, e0, f0, ef1P0[0], ef1P0[1]);
		
	}
	
	public void sendP0(short e0, short f0) {
		//System.out.println("[P0] e0:" + e0 + " f0:" + f0+" sndChannel:"+sndChannel);
		sndChannel.writeShort(e0);
		sndChannel.writeShort(f0);
		//sndChannel.writeLong(f0);
		sndChannel.flush();

	}
	
	public short[] receiveP0() {
		short[] res = new short[2];
		//res = sndChannel.readBytes(2);
		res[0] = sndChannel.readShort();// e1
		res[1] = sndChannel.readShort();// f1
		//res[1] = sndChannel.readBytes(1)[0];// f1
		sndChannel.flush();
		// System.out.println("[P0] e1:" + res[0] + " f1:" + res[1]);
		return res;
	}
	
	public short[] ef0(eightBitMultiplicationTriple mt, short x0, short y0) {
		short tripleA0 = mt.tripleA0;
		short tripleB0 = mt.tripleB0;
		// long tripleC0 = mt.tripleC0;
		short e0 = eightBitAdditiveUtil.sub(x0, tripleA0);// e0= x0 - a0
		short f0 = eightBitAdditiveUtil.sub(y0, tripleB0);// f0 = y0 - b0
		short[] res = new short[2];
		res[0] = e0;
		res[1] = f0;
		return res;
	}

	public void mulP0(eightBitMultiplicationTriple mt, short e0, short f0, short e1, short f1) {
		/*
		 * long tripleA0 = mt.tripleA0; long tripleB0 = mt.tripleB0; long tripleC0 =
		 * mt.tripleC0; long e0 = AdditiveUtil.sub(x0, tripleA0);//e0= x0 - a0 long f0 =
		 * AdditiveUtil.sub(y0, tripleB0);//f0 = y0 - b0
		 */

		short e = eightBitAdditiveUtil.add(e0, e1);
		short f = eightBitAdditiveUtil.add(f0, f1);
		// System.out.println("[P0] e:"+e+" f:"+f);

		this.z0 = eightBitAdditiveUtil.add(eightBitAdditiveUtil.add(eightBitAdditiveUtil.mul(f, mt.tripleA0), eightBitAdditiveUtil.mul(e, mt.tripleB0)),
				mt.tripleC0);
		// System.out.println("z0:"+z0);
		// return z0;
	}
}
