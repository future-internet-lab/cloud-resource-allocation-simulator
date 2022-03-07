package fil.resource.virtual;

public class Receive extends Service {
	public Receive(int sfcID, int piBelong) {
		this.setSfcID(sfcID);
		this.setPiBelong(piBelong);
		this.setServiceType("receive");
		this.setCpu_pi(0); // CPU of capture in pi
		this.setCpu_server(5);
		this.setBelongToEdge(false);
		this.setServiceID(4);

	}
	public Receive() {
		this.setServiceType("receive");
		this.setCpu_pi(0); // CPU of capture in pi
		this.setCpu_server(5);
		this.setBelongToEdge(false);
		this.setServiceID(4);

	}
}
