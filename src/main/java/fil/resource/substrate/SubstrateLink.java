package fil.resource.substrate;


public class SubstrateLink {
	
	private Node start;
	private Node end;
	private double usedBW;
	private double initialBW;
	
	public SubstrateLink()
	{
		setStart(new Node());
		setEnd(new Node());
	}
	
	public SubstrateLink(Node start, Node end, double bw)
	{
		this.setStart(start);
		this.setEnd(end);
		this.initialBW = bw;
	}

	public Node getStart() {
		return start;
	}

	public void setStart(Node start) {
		this.start = start;
	}

	public Node getEnd() {
		return end;
	}

	public void setEnd(Node end) {
		this.end = end;
	}
	
	public void reset() {
		this.usedBW = 0;
	}
	public double getInitialBW() {
		return initialBW;
	}

	public void setInitialBW(double initialBW) {
		this.initialBW = initialBW;
	}
	
	public double getRemainBW() {
		return (initialBW - usedBW);
	}
	public double getUsedBW() {
		return usedBW;
	}

	public void setUsedBW(double usedBW) {
		this.usedBW += usedBW;
	}
}
