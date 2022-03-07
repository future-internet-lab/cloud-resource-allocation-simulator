package fil.resource.virtual;

public class Decode extends Service {
	public Decode(int sfcID, int piBelong) {
		this.setSfcID(sfcID);
		this.setPiBelong(piBelong);
		this.setServiceType("decode");
		this.setCpu_pi(8); //8
		this.setCpu_server(1.5);
		this.setBelongToEdge(false);
		this.setBandwidth(16.32);
		this.setPower(0.1);
		this.setServiceID(2);

	}
	public Decode() {
		this.setServiceType("decode");
		this.setCpu_pi(8); //8
		this.setCpu_server(1.5);
		this.setBelongToEdge(false);
		this.setBandwidth(16.32); //16.32
		this.setPower(0.1);
		this.setServiceID(2);

	}
}
