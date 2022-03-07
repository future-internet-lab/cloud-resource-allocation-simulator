package fil.resource.virtual;

import java.util.Map;
import java.util.Map.Entry;

import fil.resource.substrate.SubstrateSwitch;


public class modelHP {

	public static final String PORT_IDLE = "0";
	public static final String PORT_10 = "10";
	public static final String PORT_100 = "100";
	public static final String PORT_1G = "1024";

	public static final Integer PWCORESTATIC = 39;

	public static final Integer PWPORTIDLE = 0;
	public static final double PWPORT10 = 0.42;
	public static final double PWPORT100 = 0.48;
	public static final double PWPORT1G = 0.9;

	public double getPower(String state) {
		double pw = 0;
		if (state.contains(PORT_IDLE))
			pw = PWPORTIDLE;
		if (state.contains(PORT_10))
			pw = PWPORT10;
		if (state.contains(PORT_100))
			pw = PWPORT100;
		if (state.contains(PORT_1G))
			pw = PWPORT1G;
		return pw;
	}

	public String checkState(Double demand) {

		String state = "1024";
		if (demand == 0)
			state = PORT_IDLE;
		if (demand > 0 && demand <= 10)
			state = PORT_10;
		if (demand > 10 && demand <= 100)
			state = PORT_100;
		if (demand > 100 && demand <= 10*1024)
			state = PORT_1G;
		if(demand > 10*1024)
			throw new java.lang.Error("Bandwidth port exceeds 1GB.");
		return state;
	}

	public double getPowerOfSwitch(SubstrateSwitch sSwitch) {

		double power = 0;
		double powerPort = 0;
		Map<SubstrateSwitch, Double> listBandwidth = sSwitch.getBandwidthPort();

		for (Entry<SubstrateSwitch, Double> entry : listBandwidth.entrySet()) {
			powerPort += getPower(checkState(entry.getValue()));

		}
		power = powerPort + PWCORESTATIC;
		return power;
	}
	
	public double calculateForOldPath(SubstrateSwitch sSwitch0, SubstrateSwitch sSwitch1, SubstrateSwitch sSwitch2, double band) {

		double power = 0;
		double powerPort = 0;
		Map<SubstrateSwitch, Double> listBandwidth = sSwitch1.getBandwidthPort();
		
		for (Entry<SubstrateSwitch, Double> entry : listBandwidth.entrySet()) {
			if(entry.getKey().equals(sSwitch2) ||entry.getKey().equals(sSwitch0)){
				powerPort += getPower(checkState(entry.getValue()-band));
			}
			else
			{
			powerPort += getPower(checkState(entry.getValue()));
			}

		}
//		System.out.println();
		power = powerPort + PWCORESTATIC;
		return power;
	}
	public double calculateForNewPath(SubstrateSwitch sSwitch0, SubstrateSwitch sSwitch1, SubstrateSwitch sSwitch2, double band) {

		double power = 0;
		double powerPort = 0;
		Map<SubstrateSwitch, Double> listBandwidth = sSwitch1.getBandwidthPort();
		
		for (Entry<SubstrateSwitch, Double> entry : listBandwidth.entrySet()) {
			if(entry.getKey().equals(sSwitch2)||entry.getKey().equals(sSwitch0)){
				powerPort += getPower(checkState(entry.getValue()+band));
			}
			else
			{
			powerPort += getPower(checkState(entry.getValue()));
			}

		}
//		System.out.println();
		power = powerPort + PWCORESTATIC;
		return power;
	}
}
