package scami;

import java.util.ArrayList;
import java.util.Arrays;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;

public class DistanceTest {
	private static final String API_KEY = "AIzaSyDJB3LjfI0tTVznT-J2GCtT6Bki0LSvwy8";

	private static final GeoApiContext context = new GeoApiContext.Builder().apiKey(API_KEY).build();
	private String[] destinations; // o tipo de dados que a API usa
	protected ArrayList<String[]> segmentedDestinations = new ArrayList<>(); // limite de 25 destinos por request
	private LatLng origin;
	protected ArrayList<DestinyInformation> destinationsInfoCar = new ArrayList<>();
	protected ArrayList<DestinyInformation> destinationsInfoBicycle = new ArrayList<>();
	protected ArrayList<DestinyInformation> destinationsInfoWalk = new ArrayList<>();
	
	public DistanceTest(LatLng origin, String[] destinations) {
		setDestinations(destinations);
		setOrigin(origin);
	}

	public DistanceMatrix makeRequest(String[] destinations, TravelMode travelMode) {
    	if(travelMode != TravelMode.DRIVING && travelMode != TravelMode.BICYCLING && travelMode != TravelMode.WALKING) {
    		travelMode = TravelMode.DRIVING;
    	}
	    try {
	    	//mesmo servi�o, limite de 2500 requests por dia
	        DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
	        DistanceMatrix result = req.origins(origin)
	                .destinations(destinations)
	                .mode(travelMode)
	                .language("pt-PT")
	                .units(Unit.METRIC)
	                .await();
	        return result;
	    } catch (ApiException e) {
	        System.out.println(e.getMessage());
	    } catch (Exception e) {
	        System.out.println(e.getMessage());
	    }
	    return null;
	}
	
	public void handleResponse(ArrayList<DestinyInformation> destinationsInfo, DistanceMatrix response) {
		if(response != null) {
			for(int i = 0; i < response.rows[0].elements.length; i++) {
				if(response.rows[0].elements[i].status.equals("OK")) {
					destinationsInfo.add(new DestinyInformation(
							response.originAddresses[0],
							response.destinationAddresses[i],
							response.rows[0].elements[i].distance.humanReadable,
							response.rows[0].elements[i].duration.humanReadable,
							response.rows[0].elements[i].distance.inMeters,
							response.rows[0].elements[i].duration.inSeconds));
				} else {
					System.out.println("Error handling response of element " + i +
							", error was: " + response.rows[0].elements[i].status);
				}
			}
		} else {
			System.out.println("Error: Invalid request");
		}
	}
	
	public void requestAllTravelModes()  {
		for(String[] element : segmentedDestinations) {
			handleResponse(destinationsInfoCar, makeRequest(element, TravelMode.DRIVING));
			handleResponse(destinationsInfoBicycle, makeRequest(element, TravelMode.BICYCLING));
			handleResponse(destinationsInfoWalk, makeRequest(element, TravelMode.WALKING));
		}
	}
	
    //sets
    public void setDestinations(String[] destinations) {
        this.destinations = destinations;
        //temos de segmentar os strings dos destinos
        int i = 0;
        String[] segment = new String[25];
        Arrays.fill(segment, null);
        while(i < destinations.length) {
        	segment[i] = destinations[i];
        	if(i % 25 == 0 && i > 0) {	
        		segmentedDestinations.add(segment);
        		segment = new String [25];
        		Arrays.fill(segment, null);
        	}
        	if(i == destinations.length - 1) { // o �ltimo resquest n�o poder� ter destinos a null
        		int j = 0;
        		while(segment[j] != null && j < 25) { j++; }
                String[] lastSegment = new String[j];
                for(int k = 0; k < j; k++) {
                	lastSegment[k] = segment[k];
                }
                segmentedDestinations.add(lastSegment);
        	}
        	i++;
        }
    }

    public void setOrigin(LatLng origin) {
        this.origin = origin;
    }
    
    //gets
    public String[] getDestinations() {
        return destinations;
    }

    public LatLng getOrigin() {
        return this.origin;
    }
	
	@Override
	public String toString() {
		String msg  = "Requests by car:";
		for(DestinyInformation element : destinationsInfoCar) {
			msg += element.toString() + "\n";
		}
		msg  += "Requests by car:\n";
		for(DestinyInformation element : destinationsInfoBicycle) {
			msg += element.toString() + "\n";
		}
		msg  += "Requests on foot:\n";
		for(DestinyInformation element : destinationsInfoWalk) {
			msg += element.toString() + "\n";
		}
		return msg;
	}
}

