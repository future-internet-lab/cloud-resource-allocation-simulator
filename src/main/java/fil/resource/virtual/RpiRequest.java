package fil.resource.virtual;

public class RpiRequest {
	private int id;
	private int numRequest;
	
	public RpiRequest(int id, int numRequest) {
		this.setId(id);
		this.setNumRequest(numRequest);
	}

	public int getNumRequest() {
		return numRequest;
	}

	public void setNumRequest(int numRequest) {
		this.numRequest = numRequest;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
