package scami;

public class DestinyInformation {
	private String origin;
	private String destination;
	private String distanceS;
	private String durationS;	
	private long distance;
	private long duration;
	
	public DestinyInformation(String origin, String destination, 
			String distanceS, String durationS, long distance, long duration) {
		setOrigin(origin);
		setDestination(destination);
		setDistanceS(distanceS);
		setDurationS(durationS);
		setDistance(distance);
		setDuration(duration);
	}
	
	public DestinyInformation() {
		setOrigin("None");
		setDestination("None");
		setDistanceS("Not Found");
		setDurationS("Not Found");
		setDistance(Long.MAX_VALUE);
		setDuration(Long.MAX_VALUE);
	}
	
	//sets
	public void setOrigin(String origin) {
	    this.origin = origin;
	}
	
	public void setDestination(String destination) {
	    this.destination = destination;
	}
	
	public void setDistanceS(String distanceS) {
	    this.distanceS = distanceS;
	}
	
	public void setDurationS(String durationS) {
	    this.durationS = durationS;
	}
	
	public void setDistance(long distance) {
		this.distance = distance;
	}
	
	public void setDuration(long duration) {
		this.duration = duration;
	}
	
	//gets
    public String getOrigin() {
        return origin;
    }    
    
    public String getDestination() {
        return destination;
    }    
    
    public String getDistanceS() {
        return distanceS;
    }    
    
    public String getDurationS() {
        return durationS;
    }
    
    public double getDistance() {
        return distance;
    }
    
    public double getDuration() {
        return duration;
    }
    
    public double getDurationInMinutes() {
        return (double) duration / 60;
    }
	
	@Override
	public String toString() {
		String msg = "From " + origin + " to " + destination;
		msg += " in " + duration  + " seconds (" + distance + " meters)" + " -> " + durationS  + " (" + distanceS + ")";
		return msg;
	}
	
}
