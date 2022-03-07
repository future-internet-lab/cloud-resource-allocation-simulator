package fil.resource.virtual;

public class Event implements Comparable<Event> {
	private String type;
	private SFC sfc;
	private double time;
	private int piID;
	
	public Event() {
		type = "None";
		time = 0;
		piID = 0;
	}
	
	public Event(String type, SFC sfc, double time, int piID) {
		this.type = type;
		this.time = time;
		this.piID = piID;
		this.sfc = sfc;
	}
	
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public int getPiID() {
		return piID;
	}
	public void setPiID(int piID) {
		this.piID = piID;
	}

	public SFC getSfc() {
		return sfc;
	}

	public void setSfc(SFC sfc) {
		this.sfc = sfc;
	}
	
	@Override
	public int compareTo(Event compareEv) {
		// TODO Auto-generated method stub
		double compareTime = compareEv.getTime();
		if(compareTime > this.time)
			return -1;
		else
			return 1;
        /* For Ascending order*/
//        return (int) (this.time - compareTime);
	}

	
}
