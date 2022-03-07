package fil.resource.substrate;

public class SubstrateNode {
	private String type;
	final static double CPU = 100; //100%
	private double usedCPU;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public double getRemainCPU() {
		return (CPU - this.usedCPU);
	}

	public double getUsedCPU() {
		return usedCPU;
	}

//	public void setUsedCPU(double usedCPU) {
//		this.usedCPU = usedCPU;
//	}
	
	public void setUsedCPU(double usedCPU) {
		this.usedCPU += usedCPU;
	}
	
	public void resetCPU() {
		this.usedCPU = 0.0;
	}
	
}
