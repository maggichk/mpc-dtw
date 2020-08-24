package additive;

import java.security.SecureRandom;
import java.util.Arrays;
import utilMpc.Constants2PC;

public class SharedSequence {
	private long[] sharedSequence;// <A>_0 or <A>_1
	private long[] sharedSquareSequence;// <A^2>_0 or <A^2>_1	

	private int index;//Integer.MAX indicates query, max-1 lb, max-2 ub
	private int clusterIndex;//MAX indicates query,lb,ub
	private int length;
	private int shareNo; //0,1, 2 where 2 indicates recovered sequence
	private boolean isClusterCenter;
	
	public SharedSequence() {
		
	}

	public SharedSequence(int queryLength, int index, int clusterIndex, int shareNo, long[] sharedValue, long[] sharedSquareValue) {
		this.index = index;
		this.clusterIndex = clusterIndex;
		this.length = queryLength;
		this.setShareNo(shareNo);
		this.sharedSequence = Arrays.copyOf(sharedValue, queryLength);
		this.sharedSquareSequence = Arrays.copyOf(sharedSquareValue, queryLength);
		//System.out.println("shared Value:"+sharedValue[0]+" "+this.sharedSequence[0]);
		//System.out.println("sharedSquareValue:"+sharedSquareValue[0]+" "+this.sharedSquareSequence[0]);
	}

	public SharedSequence(int queryLength, int index, int clusterIndex, int shareNo, boolean isCenter, long[] sharedValue, long[] sharedSquareValue) {
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
	
	public SharedSequence(int queryLength, int index, int clusterIndex,int shareNo) {

		this.index = index;
		this.clusterIndex = clusterIndex;
		this.length = queryLength;
		this.setShareNo(shareNo);
		this.sharedSequence = new long[queryLength];
		this.sharedSquareSequence = new long[queryLength];
	}

	public SharedSequence(int queryLength, int index, int clusterIndex, int shareNo, boolean isCenter, SecureRandom random) {

		this.length = queryLength;
		this.index = index;
		this.clusterIndex = clusterIndex;
		this.setShareNo(shareNo);
		this.setClusterCenter(isCenter);

		this.sharedSequence = new long[queryLength];
		this.sharedSquareSequence = new long[queryLength];
		
		for (int i = 0; i < queryLength; i++) {
			long shareData = random.nextInt(Integer.MAX_VALUE);//[0 , 2^31-1)
			long shareSquareData = random.nextInt(Integer.MAX_VALUE);// 0 - 2^31-1
			//System.out.println("shareData:"+shareData+" shareSquareData:"+shareSquareData);
			sharedSequence[i] = shareData;
			sharedSquareSequence[i] = shareSquareData;
		}
	}

	public long[] getSharedData(int dataIndex) {
		if (dataIndex > length || dataIndex < 0) {
			throw new IllegalArgumentException(
					"Specified element is out of bounds. Sequence " + index + ", Element " + dataIndex);
		} else {
			long[] shares = new long[2];
			shares[0] = this.sharedSequence[dataIndex];
			shares[1] = this.sharedSquareSequence[dataIndex];
			return shares;
		}
	}

	public void setSharedData(int dataIndex, long dataValue, long squareValue) {
		if (dataIndex > length || dataIndex < 0) {
			throw new IllegalArgumentException(
					"Specified element is out of bounds. Sequence " + index + ", Element " + dataIndex);
		} else {
			this.sharedSequence[dataIndex] = dataValue;
			this.sharedSquareSequence[dataIndex] = squareValue;
		}
	}

	public void minus(SharedSequence B) {

		if (B.getLength() != this.length) {
			throw new IllegalArgumentException(
					"Specified arrays are nto equal length. Sequence " + this.index + ", Sequence " + B.getIndex());
		} else {

			for (int i = 0; i < this.length; i++) {
				long[] valuesB = B.getSharedData(i);
				// shareValue
				this.sharedSequence[i] = AdditiveUtil.sub(this.sharedSequence[i], valuesB[0]);

				// shareSquareValue
				this.sharedSquareSequence[i] = AdditiveUtil.sub(this.sharedSquareSequence[i], valuesB[1]);
			}
		}
	}

	public void add(SharedSequence B) {
		if (B.getLength() != this.length) {
			throw new IllegalArgumentException(
					"Specified arrays are nto equal length. Sequence " + this.index + ", Sequence " + B.getIndex());
		} else {

			for (int i = 0; i < this.length; i++) {
				long[] valuesB = B.getSharedData(i);
				// shareValue
				this.sharedSequence[i] = AdditiveUtil.modAdditive(this.sharedSequence[i] + valuesB[0]);

				// shareSquareValue
				this.sharedSquareSequence[i] = AdditiveUtil.modAdditive(this.sharedSquareSequence[i] + valuesB[1]);
			}
		}
	}

	public SharedSequence add(SharedSequence A, SharedSequence B) {
		 SharedSequence res = new SharedSequence(A.getLength(), A.getIndex(), A.getClusterIndex(), 2, A.getSharedSequence(),
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

	public long[] getSharedSequence() {
		return sharedSequence;
	}

	public void setSharedSequence(long[] sharedSequence) {
		this.sharedSequence = sharedSequence;
	}
	
	public long[] getSharedSquareSequence() {
		return sharedSquareSequence;
	}

	public void setSharedSquareSequence(long[] sharedSquareSequence) {
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
