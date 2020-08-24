package gadgetAdditive;

import additive.AdditiveUtil;
import additive.AdditiveUtil2;
import booleanShr.BooleanUtil;
import common.util.Converter;
import flexSC.util.Utils;

public class tester {

	public static void main(String[] args) {

		long a = 4;
		int[] aArr = Utils.fromLong2int(a, 31);

		for (int i = 0; i < 31; i++) {

			System.out.print(aArr[i]);
		}
		System.out.println();
		int[] inverse = Utils.copyArrInverse(aArr, 31);

		for (int i = 0; i < 31; i++) {

			System.out.print(inverse[i]);
		}
		System.out.println();
		int[] rightmost = Utils.fromLong2intRightmost(a, 31);
		for (int i = 0; i < 31; i++) {

			System.out.print(rightmost[i]);
		}
		System.out.println();
		
		byte[] long2byte = Utils.fromLong2byteRightmost(a,31);
		System.out.println("size:"+long2byte.length);
		for (int i = 0; i < long2byte.length; i++) {
			System.out.print(long2byte[i]);
		}
		System.out.println("---------");
		int[] res = new int[31];
		/*
		 * for (int i = 31-1; i >=0; i--) { res[i] = (((a >> i) & 1) == 0) ? 0 : 1;
		 * System.out.println(a>>i); }
		 */

		for (int i = 0; i < 31; i++) {
			res[30 - i] = (((a >> i) & 1) == 0) ? 0 : 1;
			// System.out.println(((a>>i)&1));
		}
		System.out.println();
		for (int i = 0; i < 31; i++) {
			System.out.print(res[i]);
		}
		System.out.println();
		System.out.println(2 % 2);
		
		int x0=10; int x1=5;
		int y=2;
		
		
		byte x0b = 1;
		byte x1b = 1;
		System.out.println("xor:"+BooleanUtil.xor(x0b, x1b)+" and:"+BooleanUtil.and(x0b, x1b));
		long s1 = System.nanoTime();
		System.out.println(BooleanUtil.and(x0b, x1b));
		long e1 = System.nanoTime();
		System.out.println(e1-s1);
		
		long s2 = System.nanoTime();
		System.out.println(x0b & x1b);
		long e2 = System.nanoTime();
		System.out.println(e2-s2);
		
		
		
		
	}
}
