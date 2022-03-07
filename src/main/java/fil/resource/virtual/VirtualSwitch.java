package fil.resource.virtual;

/**
 * Builds virtual switch
 * 
 * @author Van Huynh Nguyen
 *
 */
public class VirtualSwitch {
	private String nameVirtualSwitch;
	private double cpu;

	/**
	 * Constructs virtual switch
	 */
	public VirtualSwitch() {
		this.nameVirtualSwitch = "";
		this.cpu = 0;
	}

	/**
	 * Constructs virtual switch
	 * 
	 * @param name
	 *            Name of virtual machine
	 * @param cpu
	 *            CPU capacity of virtual machine
	 */
	public VirtualSwitch(String name, double cpu) {
		this.nameVirtualSwitch = name;
		this.cpu = cpu;
	}

	public String getNameVirtualSwitch() {
		return nameVirtualSwitch;
	}

	public void setNameVirtualSwitch(String nameVirtualSwitch) {
		this.nameVirtualSwitch = nameVirtualSwitch;
	}

	public double getCpu() {
		return cpu;
	}

	public void setCpu(double cpu) {
		this.cpu = cpu;
	}
}
