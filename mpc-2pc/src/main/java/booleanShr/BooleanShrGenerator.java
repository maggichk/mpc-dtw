package booleanShr;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import common.util.Constants;
import utilMpc.Constants2PC;

public class BooleanShrGenerator {


	public byte x0; // <x>_0, <x>_1
	public byte x1;

	public SecureRandom random;

	public BooleanShrGenerator() {

	}

	public BooleanShrGenerator(boolean isGenerateRandom) {
		if (isGenerateRandom) {
			try {
				this.random = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				System.out.println("Fail to get SecureRandom instance.");
				e.printStackTrace();
			}
		}

	}

	public void generateSharedDataPointSet() {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			this.generateSharedDataPointSet(random);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
		}
	}

	public void generateSharedDataPointSet(SecureRandom random) {

		this.x1 = (byte) random.nextInt(2);
		this.x0 = (byte) random.nextInt(2);

	}

	public byte generateRandom() {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			return (byte) random.nextInt(2);

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
			return 0;
		}

	}

	/**
	 * Given instance
	 * 
	 * @param isGivenRandom
	 * @return
	 */
	public long generateRandom(boolean isGivenRandom) {

		return this.random.nextInt(2);
	}

	public void generateSharedDataPoint(byte x0, boolean isGivenRandom) {

		SecureRandom random = this.random;
		this.x1 = (byte) random.nextInt(2);
		this.x0 = (byte) BooleanUtil.xor(x0, x1);

	}

	public void generateSharedDataPoint(byte x0) {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			this.x1 = (byte) random.nextInt(2);
			this.x0 = (byte) BooleanUtil.xor(x0, x1);

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
		}
	}

	
	
	

	public static void main(String[] args) {
		String queryLength = Constants.CONFIG_DTW_QUERY_LENGTH;

	}

}
