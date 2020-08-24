package eightBitAdditive;

import java.security.SecureRandom;
import java.util.Arrays;

public class fifteenBitSharedSequence {
	private short[] sharedSequence;// <A>_0 or <A>_1
	private short[] sharedSquareSequence;// <A^2>_0 or <A^2>_1

	private int index;//Integer.MAX indicates query, max-1 lb, max-2 ub
	private int clusterIndex;//MAX indicates query,lb,ub
	private int length;
	private int shareNo; //0,1, 2 where 2 indicates recovered sequence
	private boolean isClusterCenter;

	public fifteenBitSharedSequence() {

	}

	public fifteenBitSharedSequence(int queryLength, int index, int clusterIndex, int shareNo, short[] sharedValue, short[] sharedSquareValue) {
		this.index = index;
		this.clusterIndex = clusterIndex;
		this.length = queryLength;
		this.setShareNo(shareNo);
		this.sharedSequence = Arrays.copyOf(sharedValue, queryLength);
		this.sharedSquareSequence = Arrays.copyOf(sharedSquareValue, queryLength);
		//System.out.println("shared Value:"+sharedValue[0]+" "+this.sharedSequence[0]);
		//System.out.println("sharedSquareValue:"+sharedSquareValue[0]+" "+this.sharedSquareSequence[0]);
	}

	public fifteenBitSharedSequence(int queryLength, int index, int clusterIndex, int shareNo, boolean isCenter, short[] sharedValue, short[] sharedSquareValue) {
		this.index = index;
		this.clusterIndex = clusterIndex;
		this.length = queryLength;
		this.setShareNo(shareNo);
		this.setClusterCenter(isCenter);
		this.sharedSequence = Arrays.copyOf(sharedValue, queryLength);
		this.sharedSquareSequence = Arrays.copyOf(sharedSquareValue, queryLength);
		//System.out.println("shared Value:"+sharedValue[0]+" "+this.sharedSequence[0]);
		//System.out.println("sharedSquareValue:"+sharedSquareValue[0]+" "+this.sharedSquareSequence[0]);
	}

	public fifteenBitSharedSequence(int queryLength, int index, int clusterIndex, int shareNo) {

		this.index = index;
		this.clusterIndex = clusterIndex;
		this.length = queryLength;
		this.setShareNo(shareNo);
		this.sharedSequence = new short[queryLength];
		this.sharedSquareSequence = new short[queryLength];
	}

	public fifteenBitSharedSequence(int queryLength, int index, int clusterIndex, int shareNo, boolean isCenter, SecureRandom random) {

		this.length = queryLength;
		this.index = index;
		this.clusterIndex = clusterIndex;
		this.setShareNo(shareNo);
		this.setClusterCenter(isCenter);

		this.sharedSequence = new short[queryLength];
		this.sharedSquareSequence = new short[queryLength];
		
		for (int i = 0; i < queryLength; i++) {
			short shareData = (short) random.nextInt(32767);//[0 , 2^31-1)
			short shareSquareData = (short) random.nextInt(32767);// 0 - 2^31-1
//			short[] bytes = new byte[2];
//			random.nextBytes(bytes);
//			byte shareData = (byte) (bytes[0]  >>> 1);//[0 , 2^31-1)
//			byte shareSquareData = (byte) (bytes[1]  >>> 1);// 0 - 2^31-1
//			//byte shareData = random.nextInt(Integer.MAX_VALUE);//[0 , 2^31-1)
//			//byte shareSquareData = random.nextInt(Integer.MAX_VALUE);// 0 - 2^31-1
//			//System.out.println("shareData:"+shareData+" shareSquareData:"+shareSquareData);
			sharedSequence[i] = shareData;
			sharedSquareSequence[i] = shareSquareData;
		}
	}

	public short[] getSharedData(int dataIndex) {
		if (dataIndex > length || dataIndex < 0) {
			throw new IllegalArgumentException(
					"Specified element is out of bounds. Sequence " + index + ", Element " + dataIndex);
		} else {
			short[] shares = new short[2];
			shares[0] = this.sharedSequence[dataIndex];
			shares[1] = this.sharedSquareSequence[dataIndex];
			return shares;
		}
	}

	public void setSharedData(int dataIndex, short dataValue, short squareValue) {
		if (dataIndex > length || dataIndex < 0) {
			throw new IllegalArgumentException(
					"Specified element is out of bounds. Sequence " + index + ", Element " + dataIndex);
		} else {
			this.sharedSequence[dataIndex] = dataValue;
			this.sharedSquareSequence[dataIndex] = squareValue;
		}
	}

	public void minus(fifteenBitSharedSequence B) {

		if (B.getLength() != this.length) {
			throw new IllegalArgumentException(
					"Specified arrays are nto equal length. Sequence " + this.index + ", Sequence " + B.getIndex());
		} else {

			for (int i = 0; i < this.length; i++) {
				short[] valuesB = B.getSharedData(i);
				// shareValue
				this.sharedSequence[i] = fifteenBitAdditiveUtil.sub(this.sharedSequence[i], valuesB[0]);

				// shareSquareValue
				this.sharedSquareSequence[i] = fifteenBitAdditiveUtil.sub(this.sharedSquareSequence[i], valuesB[1]);
			}
		}
	}

	public void add(fifteenBitSharedSequence B) {
		if (B.getLength() != this.length) {
			throw new IllegalArgumentException(
					"Specified arrays are nto equal length. Sequence " + this.index + ", Sequence " + B.getIndex());
		} else {

			for (int i = 0; i < this.length; i++) {
				short[] valuesB = B.getSharedData(i);
				// shareValue
				this.sharedSequence[i] = fifteenBitAdditiveUtil.modAdditive(this.sharedSequence[i] + valuesB[0]);

				// shareSquareValue
				this.sharedSquareSequence[i] = fifteenBitAdditiveUtil.modAdditive(this.sharedSquareSequence[i] + valuesB[1]);
			}
		}
	}

	public fifteenBitSharedSequence add(fifteenBitSharedSequence A, fifteenBitSharedSequence B) {
		 fifteenBitSharedSequence res = new fifteenBitSharedSequence(A.getLength(), A.getIndex(), A.getClusterIndex(), 2, A.getSharedSequence(),
				A.getSharedSquareSequence());	
		 res.add(B);
		
		 return res;
	}

	
	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getClusterIndex() {
		return clusterIndex;
	}

	public void setClusterIndex(int clusterIndex) {
		this.clusterIndex = clusterIndex;
	}

	public short[] getSharedSequence() {
		return sharedSequence;
	}

	public void setSharedSequence(short[] sharedSequence) {
		this.sharedSequence = sharedSequence;
	}
	
	public short[] getSharedSquareSequence() {
		return sharedSquareSequence;
	}

	public void setSharedSquareSequence(short[] sharedSquareSequence) {
		this.sharedSquareSequence = sharedSquareSequence;
	}

	public int getShareNo() {
		return shareNo;
	}

	public void setShareNo(int shareNo) {
		this.shareNo = shareNo;
	}

	public boolean isClusterCenter() {
		return isClusterCenter;
	}

	public void setClusterCenter(boolean isClusterCenter) {
		this.isClusterCenter = isClusterCenter;
	}

}
