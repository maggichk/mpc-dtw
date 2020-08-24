package flexSC.gc.regular;

import flexSC.gc.GCSignal;

import java.security.SecureRandom;



public class TestGarbler {
	SecureRandom rnd = new SecureRandom();
	GCSignal a = GCSignal.freshLabel(rnd);
	GCSignal b = GCSignal.freshLabel(rnd);
	GCSignal m = GCSignal.freshLabel(rnd);
	GCSignal ret = GCSignal.freshLabel(rnd);
	Garbler gb = new Garbler();
	
	public void test() {
		gb.enc(a, b, 0, m, ret);
		
//		Assert.assertTrue(m.equals(gb.dec(a, b, 0L, gb.enc(a, b, 0L, m))));
	}


	public void test1000() {
		for(int i = 0; i<10000; i++)
			test();
	}
}
