package fil.resource.virtual;

public class Density extends Service {
	public Density(int sfcID, int piBelong) {
		this.setSfcID(sfcID);
		this.setPiBelong(piBelong);
		this.setServiceType("density");
		this.setCpu_pi(13.6); //CPU usage when running on Pi
		this.setCpu_server(6.5);; //CPU usage when running on server
		this.setBelongToEdge(false);
		this.setBandwidth(0.6);
		this.setPower(0.13);
		this.setServiceID(3);

	}
	public Density() {
		this.setServiceType("density");
		this.setCpu_pi(13.6); //CPU usage when running on Pi

		this.setCpu_server(6.5);; //CPU usage when running on server
		this.setBelongToEdge(false);
		this.setBandwidth(0.6);
		this.setPower(0.13);
		this.setServiceID(3);

	}
}
