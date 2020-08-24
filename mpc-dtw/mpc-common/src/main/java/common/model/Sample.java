package common.model;

public class Sample {
	private int[] attributes;
	// private String label;
	private int predictLabel;
	private double density;
	private int index;
	private int nearestNeighbor;
	private int localDensity;
	private double delta;

	public Sample(int[] attributes, int index) {
		this.attributes = attributes;
		this.index = index;
	}

	public Sample(double density, int index, int localDensity, double delta, int nearestNeighbor, int predictLabel) {
		this.density = density;
		this.index = index;
		this.localDensity = localDensity;
		this.delta = delta;
		this.nearestNeighbor = nearestNeighbor;
		this.predictLabel = predictLabel;
	}

	public int[] getAttributes() {
		return attributes;
	}

	/*
	 * public String getLabel() { return label; }
	 */
	public int getPredictLabel() {
		return predictLabel;
	}

	public void setPredictLabel(int predictLabel) {
		this.predictLabel = predictLabel;
	}

	public double getDensity() {
		return density;
	}

	public void setDensity(double density) {
		this.density = density;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getNearestNeighbor() {
		return nearestNeighbor;
	}

	public void setNearestNeighbor(int nearestNeighbor) {
		this.nearestNeighbor = nearestNeighbor;
	}

	public int getLocalDensity() {
		return localDensity;
	}

	public void setLocalDensity(int localDensity) {
		this.localDensity = localDensity;
	}

	public double getDelta() {
		return delta;
	}

	public void setDelta(double delta) {
		this.delta = delta;
	}

}
