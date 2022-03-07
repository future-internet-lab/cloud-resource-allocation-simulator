package fil.resource.virtual;


/**
 * Builds Virtual Machine
 * 
 * @author Van Huynh Nguyen
 *
 */
public class VirtualMachine {
	private String nameVirtualMachine;
	private double cpu;
	private double memory;
	private int vdcID;

	/**
	 * Constructs virtual machine
	 */
	public VirtualMachine() {
		this.nameVirtualMachine = "";
		this.memory = 0;
		this.cpu = 0;
		this.vdcID =0;
	}

	/**
	 * Constructs virtual machine
	 * 
	 * @param nameVM
	 *            Name of virtual machine
	 * @param CPU
	 *            CPU capacity of virtual machine
	 * @param memory
	 *            Memory capacity of virtual machine
	 */
	public VirtualMachine(String nameVM, double CPU, double memory, int vdcID) {
		this.nameVirtualMachine = nameVM;
		this.cpu = CPU;
		this.memory = memory;
		this.vdcID = vdcID;
	}

	public String getNameVM() {
		return nameVirtualMachine;
	}

	public void setNameVM(String nameVM) {
		this.nameVirtualMachine = nameVM;
	}

	public double getCPU() {
		return cpu;
	}

	public void setCPU(double cPU) {
		cpu = cPU;
	}

	public double getMemory() {
		return memory;
	}

	public void setMemory(double memory) {
		this.memory = memory;
	}

	public int getVdcID() {
		return vdcID;
	}

	public void setVdcID(int vdcID) {
		this.vdcID = vdcID;
	}
}
