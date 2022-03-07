package fil.resource.virtual;

public class Capture extends Service {
	public Capture(int sfcID, int piBelong) {
		this.setSfcID(sfcID);
		this.setPiBelong(piBelong);
		this.setServiceType("capture");
		this.setCpu_pi(3); 
		this.setCpu_server(0);
		this.setBelongToEdge(true);
		this.setBandwidth(47.35); //47.35
		this.setPower(0.49);
		this.setServiceID(1);
	}
	public Capture() {
		this.setServiceType("capture");
		this.setCpu_pi(3); // CPU of capture in pi
		this.setCpu_server(0);
		this.setBelongToEdge(true);
		this.setBandwidth(47.35); //47.35
		this.setPower(0.49);
		this.setServiceID(1);
	}
}
