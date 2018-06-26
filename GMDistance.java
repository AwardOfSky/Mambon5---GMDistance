package scami;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.TravelMode;
import com.google.maps.model.TransitMode;
import com.google.maps.model.Unit;
import com.google.maps.model.TransitRoutingPreference;
import com.google.maps.model.DistanceMatrixElementStatus;
import com.google.maps.model.DistanceMatrix;

/**
 * <b>How to use:</b><br>
 * 
 * <br><b>All travel modes:</b>
 * <br>{@link #requestTravelModes(Imovel, ArrayList) GMDistance.requestTravelModes(Imovel, ArrayList<Indicador>);} <br>
 * <br><b>Specific travel mode:</b>
 * <br>{@link #requestTravelModes(Imovel, ArrayList, TravelMode) GMDistance.requestTravelModes(Imovel, ArrayList<Indicador>, TravelMode);} <br>
 * 
 * <br>The static method requestTravelModes 
 * will change the respective objects from type {@code DestinyInformation} in the indicators
 * given as a parameters, according to the Travel Mode(s).
 * <br>If the travel mode is omitted, a request is made to all Travel Modes.

 * <br>This method will return {@code true} or {@code false} depending on whether the request is valid or not.
 * <br><br><b>Notes:</b>
 * <br>1. Even if the request is successful, there can be errors while handling the request 
 * (i.e. Not being able to find results for element x).
 * <br>2. In both ways of using {@link #requestTravelModes(Imovel, ArrayList) requestTravelModes}, the user
 * can enter an additional {@code boolean} parameter that, if {@code true}, will print request timings.
 * 
 * @see
 * {@link #makeRequest(ArrayList, TravelMode) makeRequest} <br>
 * {@link #handleResponse(ArrayList, DistanceMatrix, TravelMode) handleResponse}
 */

public class GMDistance {
    /** Key used to build the API context */
    private static final String API_KEY = "AIzaSyD8jA6Ff_wIo8ShXf2uvo0Ju0kGxQiu3qo";

    /** 
     * Max retries upon ApiException error.<br>An ApiException error can be thrown either
     * when the daily or rate limit is reached.<br> If we are dealing with a rate limit, we want
     * the program to retry the request as other threads eventually exit and reduce the request rate.
     * <br>If the ApiException is due to a daily limit reached we don't want the program to fall into
     * a infinite retry loop, that's why we limit the requests.
     * 
     * @see {@link #context}
     */
    private static final int MAX_RETRIES = 10;

    /** We want to allow ApiExceptions to retry.
     *  @see {@link #MAX_RETRIES} */
    private static final Class<ApiException> apiException = ApiException.class;

    /** Build the {@code context} using the {@link #API_KEY} */
    private static final GeoApiContext context = new GeoApiContext
            .Builder()
            .setIfExceptionIsAllowedToRetry(apiException, false)
            .maxRetries(MAX_RETRIES) // evitar crashar  programa depois do limite diário
            .apiKey(API_KEY)
            .build();
	
    /** Specify supported Travel Modes*/
    private static final List<TravelMode> supportedTravelModes = Arrays.asList(TravelMode.TRANSIT, TravelMode.WALKING, TravelMode.DRIVING);

    /** Define number of supported Travel Modes */ 
    private static final int NUMBER_TRAVEL_MODES = supportedTravelModes.size();

    /** List of segmented blocks of destinations (in order to handle more that 25 indicators) */
    protected ArrayList<ArrayList<Indicador>> segmentedDestinations = new ArrayList<>(); // limite de 25 destinos por request

    protected ArrayList<Indicador> destinations = new ArrayList<>();
    private Imovel imovel;

    /** if {@code true} show request timings and statistics */
    private boolean benchmark;

    /** Global variable declaration to deal with the boolean value modified by each thread.
     *  @see {@link #createThreadNumber(Thread[], int, ArrayList, TravelMode) createThreadNumber} */
    private boolean finalStatus;

    /**
     * Constructor needed to access the non-static methods
     * 
     * @param imovel - origin
     * @param destinations - list of destinations (can be more that 25)
     * @param benchmark - print request Timings if {@code true}
     */
    public GMDistance(Imovel imovel, ArrayList<Indicador> destinations, boolean benchmark) {
        setDestinations(destinations);
        setImovel(imovel);
        setBenchmark(benchmark);
    }
	
    /**
     * Method used to build the request to the server, using the context defined by {@code GeoApiContext}.<br>
     * This request consists of a matrix of coordinates that can't have more that 25 rows or columns.<br>
     * That's why we need to segment the {@code ArrayList} of indicators passed to {@link #requestTravelModes
     * (Imovel, ArrayList) requestTravelModes}.
     * 
     * <br><br><b>Notes:</b>
     * <br> 1. If the selected Travel Mode is {@code TravelMode.TRANSIT},
     * we will set the preferred transportation vehicle to bus in order to walk the least.
     * <br> 2. If the specified Travel Mode is not supported, the default is {@code TravelMode.DRIVING}.
     * <br> 3. The request is handled synchronously.
     * <br> 4. The language response from the server is set to Portuguese.
     * <br> 5. All metrics use the Metric System of Units.
     * <br> 6. {@code TravelMode.BICYCLING} is not a valid Travel Mode as it is only supported in the USA.
     * <br> 7. The {@code destinations} array will is already a segmented list of {@code String} coordinates,
     * with a size no larger than 25 elements. 
     * 
     * @param destinations - list of destiny indicators
     * @param travelMode - specifies the travel mode
     * @return <b>{@code true}</b> if valid request <br> <b>{@code false}</b> if any API errors occur
     * @see
     * {@link #handleResponse(ArrayList, DistanceMatrix, TravelMode) handleResponse} <br>
     * {@link #getStringsFromArrayList(ArrayList) getStringsFromArrayList} <br>
     * {@link #checkIfValidTravelMode(TravelMode) checkIfValidTravelMode}
     */
    public boolean makeRequest(ArrayList<Indicador> destinations, TravelMode travelMode) {
        if(!checkIfValidTravelMode(travelMode)) {
            travelMode = supportedTravelModes.get(0);
        }
    	String[] destinationStrings = getStringsFromArrayList(destinations);
        try {
            //mesmo serviço, limite de 2500 requests por dia
            DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
            DistanceMatrix result = req.origins(imovel.getLatLon().toUrlValue())
                    .destinations(destinationStrings)
                    .mode(travelMode)
                    .transitModes(TransitMode.BUS)
                    .language("pt-PT")
                    .units(Unit.METRIC)
                    .transitRoutingPreference(TransitRoutingPreference.LESS_WALKING)
                    .await();
            handleResponse(destinations, result, travelMode);
            return true;
        } catch (ApiException e) {
            System.out.println("API exception: " + e.getMessage());	
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
	
    /**
     * This method will analyse the status of the metrics returned by the server for the
     * request made in {@link #makeRequest(ArrayList, TravelMode) makeRequest}.<br>
     * An object from the type {@code DestinyInformation} will be filled with relevant metrics accordingly<br>
     * to the specified Travel Mode. This will be made for each Indicator in the destinations list.
     * 
     * <br><br><b>Notes:</b>
     * <br> 1. The {@code destinations} array won't be grater that 25 elements.
     * <br> 2. An "OK" status message means no error were found, "ZERO_RESULTS" for no results and "NOT FOUND"
     * <br>if there is no way of accessing that indicator location with the specified Travel Mode.
     * 
     * @param destinations - list of destiny indicators
     * @param response - data structure returned by the server
     * @param travelMode - specified the travel mode
     * @return <b>{@code true}</b> if valid request <br> <b>{@code false}</b> if any API errors occurs
     * @see
     * {@link #makeRequest(ArrayList, TravelMode) makeRequest} <br>
     * {@link #buildDestinyInformation(DistanceMatrix, int) buildDestinyInformation}
     */
    public void handleResponse(ArrayList<Indicador> destinations, DistanceMatrix response, TravelMode travelMode) {
        ArrayList<DestinyInformation> destinationsInfo = new ArrayList<>();
        if(response != null) {
            for(int i = 0; i < response.rows[0].elements.length; i++) {

                DistanceMatrixElementStatus status = response.rows[0].elements[i].status;

                if(status.equals(DistanceMatrixElementStatus.OK)) {
                    destinationsInfo.add(buildDestinyInformation(response, i));
                } else if (status.equals(DistanceMatrixElementStatus.ZERO_RESULTS)) {
                    System.out.println("Error: Zero results found for element " + i + 
                            " for the preferred travel mode: " + travelMode.toString());
                    destinationsInfo.add(new DestinyInformation(response.originAddresses[0],
                            response.destinationAddresses[i], "Not Found", "Not found", Long.MAX_VALUE, Long.MAX_VALUE));
                } else if (status.equals(DistanceMatrixElementStatus.NOT_FOUND))  {
                    System.out.println("Error: element " + i + " for the preferred travel mode: " + 
                            travelMode.toString() + " was not found.");
                    destinationsInfo.add(null);
                } else {
                    System.out.println("Error handling response of element " + i + ", error unknown: " + 
                            response.rows[0].elements[i].status);
                    destinationsInfo.add(null);
                }
            }
            for(int i = 0; i < destinationsInfo.size(); i++) {
                destinations.get(i).setDestinyInformationByTravelMode(travelMode, destinationsInfo.get(i));
            }
        } else {
            System.out.println("Error: Invalid request");
        }
    }
	
    /**
     * This method will create a {@code DetinyInformation} object type that will be used in the 
     * {@link #handleResponse(ArrayList, DistanceMatrix, TravelMode) handling} of the request.<br>
     * The object created will contain metrics corresponding to the i-th row of the response data structure. 
     * 
     * @param response - data returned from Google Maps
     * @param i - index to build the object from
     * @return <b>{@code DestinyInformation}</b>
     */
    public DestinyInformation buildDestinyInformation(DistanceMatrix response, int i) {
        return new DestinyInformation(
                response.originAddresses[0],
                response.destinationAddresses[i],
                response.rows[0].elements[i].distance.humanReadable,
                response.rows[0].elements[i].duration.humanReadable,
                response.rows[0].elements[i].distance.inMeters,
                response.rows[0].elements[i].duration.inSeconds);
    }
	
    /**
     * In this method, a thread is created to handle each segment for each Travel Mode.
     * Each thread created calls {@link #makeRequest(ArrayList, TravelMode) makeRequest}, decreasing the amount of
     * time taken to answer all requests.
     * <br>The current supported Travel Modes are: <b>{@code TravelMode.DRIVING}</b>, <b>{@code TravelMode.TRANSIT}</b> 
     * and <b>{@code TravelMode.WALKING}</b>
     * 
     * <br><br><b>Note:</b> The {@code destinations} array won't be grater that 25 elements.
     * 
     * @param destinations
     * @return <b>{@code true}</b> if all request were valid <br> <b>{@code false}</b> if any API errors occurs
     * @see
     * {@link #makeRequest(ArrayList, TravelMode) makeRequest}
     * {@link #createThreadNumber(Thread[], int, ArrayList, TravelMode) createThreadNumer} <br>
     * {@link #checkIfValidTravelMode(TravelMode) checkIfValidTravelMode}
     */
    public boolean requestAllTravelModes(ArrayList<Indicador> destinations)  {
        finalStatus = true;
        int numberOfSegments = this.segmentedDestinations.size();
        Thread[] worker = new Thread[numberOfSegments * NUMBER_TRAVEL_MODES];

        long startTime = System.currentTimeMillis();
        int i;
        for(i = 0; i < numberOfSegments; i++) {
            ArrayList<Indicador> element = segmentedDestinations.get(i);
            for(int j = 0; j < NUMBER_TRAVEL_MODES; j++) {
                createThreadNumber(worker, i * NUMBER_TRAVEL_MODES + j, element, supportedTravelModes.get(j));
            }
        }
        waitThreadPool(worker);
        long endTime = System.currentTimeMillis() - startTime;

        System.out.println("Time taken: " + endTime + " ms");
        printTimings(endTime, true);
        return finalStatus;
    }
	
    /**
     * In this method, a thread is created to handle each segment of indicators.<br>Each thread created calls 
     * {@link #makeRequest(ArrayList, TravelMode) makeRequest}, decreasing the amount of time taken to answer all requests.
     * <br>The current supported Travel Modes are: <b>{@code TravelMode.DRIVING}</b>, <b>{@code TravelMode.TRANSIT}</b> 
     * and <b>{@code TravelMode.WALKING}</b>
     * 
     * <br><br><b>Note:</b> The {@code destinations} array won't be grater that 25 elements.
     * 
     * @param destinations
     * @return <b>{@code true}</b> if all request were valid <br> <b>{@code false}</b> if any API errors occurs or invalid Travel Mode
     * @see
     * {@link #makeRequest(ArrayList, TravelMode) makeRequest} <br>
     * {@link #createThreadNumber(Thread[], int, ArrayList, TravelMode) createThreadNumer} <br>
     * {@link #checkIfValidTravelMode(TravelMode) checkIfValidTravelMode}
     */
    public boolean requestSpecificTravelMode(ArrayList<Indicador> destinations, TravelMode travelMode) {
        if(checkIfValidTravelMode(travelMode)) {
            finalStatus = true;
            int numberOfSegments = this.segmentedDestinations.size();
            Thread[] worker = new Thread[numberOfSegments];

            long startTime = System.currentTimeMillis();
            for(int i = 0; i < numberOfSegments; i++) {

                ArrayList<Indicador> element = segmentedDestinations.get(i);
                createThreadNumber(worker, i, element, travelMode);

            }
            waitThreadPool(worker);
            long endTime = System.currentTimeMillis() - startTime;

            System.out.println("Time taken: " + endTime + " ms");
            printTimings(endTime, false);
            return finalStatus;
        } else {
            System.out.println("Error: Invalid Travel Mode.");
            return false;
        }
    }
	
    /**
     * Creates a thread (anonymously) in a specific thread pool.<br>
     * Assign the thread execution to call {@link #makeRequest(ArrayList, TravelMode) makeRequest} according
     * to a segment of indicators and a specific TravelMode <br> Start thread execution
     * 
     * <br><br><b>Note:</b> We need to synchronize the assignment of the boolean variable returned by the
     * request, that's why we have a global {@code finalStatus}.
     * 
     * @param worker - Thread pool
     * @param i - Place to put the created thread in the pool
     * @param element - Segment of indicators to be processed
     * @param travelMode - Travel Mode of the request
     * @see {@link #makeRequest(ArrayList, TravelMode) makeRequest}
     */
    public void createThreadNumber(Thread[] worker, int i, final ArrayList<Indicador>  element,final TravelMode travelMode) {
        worker[i] = new Thread() {
            @Override
            public void run(){
                System.out.println("Thread Running");
                boolean result = makeRequest(element, travelMode);
                synchronized(this) {
                    finalStatus = finalStatus && result;
                }
                System.out.println("Thread Exiting");
            }
        };
        worker[i].start();
    }
	
    /**
     * Awaits the termination of the given thread pool. This method is called after creating
     * and assigning all threads in and {@link #requestAllTravelModes(ArrayList) requestAllTravelModes}
     * and {@link #requestSpecificTravelMode(ArrayList, TravelMode) requestSpecificTravelMode}
     * 
     * @param worker - Thread pool to wait for
     * @see {@link #createThreadNumber(Thread[], int, ArrayList, TravelMode) createThreadNumer}
     */
    public void waitThreadPool(Thread[] worker) {
        try {
            for(int i = 0; i < worker.length; i++) {
                worker[i].join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Error: Thread Interrupted: " + e.getMessage());
        }
    }
	
    /**
     * Perform requests on all Travel Modes, while not showing any benchmarking results.
     * 
     * This static method creates a "dummy" non-static context in order to handle requests.
     * This way, the requests are performed with a single call to the class without the need
     * to externally declare and initialize the object.
     * 
     * @param imovel - matrix origin
     * @param destinations - matrix destinations
     * @return <b>{@code true}</b> if all request were valid <br> <b>{@code false}</b> if any API errors occurs
     * @see {@link #requestAllTravelModes(ArrayList) requestAllTravelModes}
     */
    public static boolean requestTravelModes(Imovel imovel, ArrayList<Indicador> destinations) {
        GMDistance dummy = new GMDistance(imovel, destinations, false);
        return dummy.requestAllTravelModes(destinations);
    }
	
    /**
     * Perform requests on a specific Travel Mode, while not showing any benchmarking results.
     * 
     * @return <b>{@code true}</b> if all request were valid <br> <b>{@code false}</b> if any API errors 
     * occurs or invalid Travel Mode
     * @see 
     * {@link #requestTravelModes(Imovel, ArrayList) requestTravelModes} <b>(method overflow)</b><br>
     * {@link #requestSpecificTravelMode(ArrayList, TravelMode) requestSpecificTravelMode}
     */
    public static boolean requestTravelModes(Imovel imovel, ArrayList<Indicador> destinations, TravelMode travelMode) {
        GMDistance dummy = new GMDistance(imovel, destinations, false);
        return dummy.requestSpecificTravelMode(destinations, travelMode);
    }
	
    /**
     * Perform requests on all Travel Modes, while allowing benchmark results.
     * 
     * @return <b>{@code true}</b> if all request were valid <br> <b>{@code false}</b> if any API errors occurs
     * @see 
     * {@link #requestTravelModes(Imovel, ArrayList) requestTravelModes} <b>(method overflow)</b><br>
     * {@link #requestAllTravelModes(ArrayList) requestAllTravelModes}
     */
    public static boolean requestTravelModes(Imovel imovel, ArrayList<Indicador> destinations, boolean benchmark) {
        GMDistance dummy = new GMDistance(imovel, destinations, benchmark);
        return dummy.requestAllTravelModes(destinations);
    }
	
    /**
     * Perform requests on a specific Travel Mode, while allowing benchmark results.
     * 
     * @return <b>{@code true}</b> if all request were valid <br> <b>{@code false}</b> if any API errors 
     * occurs or invalid Travel Mode
     * @see
     * {@link #requestTravelModes(Imovel, ArrayList) requestTravelModes} <b>(method overflow)</b><br>
     * {@link #requestSpecificTravelMode(ArrayList, TravelMode) requestSpecificTravelMode}
     */
    public static boolean requestTravelModes(Imovel imovel, ArrayList<Indicador> destinations, TravelMode travelMode, boolean benchmark) {
        GMDistance dummy = new GMDistance(imovel, destinations, benchmark);
        return dummy.requestSpecificTravelMode(destinations, travelMode);
    }
	
    /**
     * The currently supported Travel Modes are: <b>{@code TravelMode.DRIVING}</b>, <b>{@code TravelMode.TRANSIT}</b> 
     * and <b>{@code TravelMode.WALKING}.</b><br>
     * {@code TravelMode.BICYCLING} is not a valid Travel Mode as it is only supported in USA by the Google Maps API.
     * 
     * @param travelMode - specified Travel Mode
     * @return <b>{@code true}</b> if the Travel Mode is valid <br> <b>{@code false}</b> otherwise
     */
    public boolean checkIfValidTravelMode(TravelMode travelMode) {
        return supportedTravelModes.contains(travelMode);
    } // TravelMode.BICYCLING é também um modo, no entanto retorna falso porque só é suportado nos EUA
	
    
    //setters
    /**
     * This method not only sets the {@code destinations} array as an attribute of the class (for later use),
     * but also segments the {@code destinations} in blocks to be able to handle more than 25 indicators
     * ({@code DistanceMatrixApi} API hard limit) at a time.
     * 
     * @param destinations - list containing all the indicators given to the class constructor
     */
    public void setDestinations(ArrayList<Indicador> destinations) {
    	//definir destinos
        this.destinations = destinations;
        
        //segmentar a ArrayList de Indicadores para a segmentedDestinations (por causa dos limites do GM)
        if(destinations != null) {
            ArrayList<Indicador> segment = new ArrayList<>();
            int i = 0;
            while(i < destinations.size()) {
                segment.add(destinations.get(i));
                if((i+1) % 25 == 0 && i > 0) {
                    segmentedDestinations.add(segment);
                    segment = new ArrayList<>();
                }
                i++;
            }
            if(!segment.isEmpty()) {
                segmentedDestinations.add(segment);
            }
        }
    }
    
    public void setImovel(Imovel imovel) {
        this.imovel = imovel;
    }
    
    public void setBenchmark(boolean bench) {
        this.benchmark = bench;
    }
    
    
    //getters
    public ArrayList<Indicador> getDestinations() {
        return this.destinations;
    }

    public Imovel getImovel() {
        return this.imovel;
    }

    public boolean getBenchmark() {
        return this.benchmark;
    }
    
    /**
     * This method returns a list of {@code Strings} containing the Latitude and Longitude
     * of the indicators given as parameters.<br>The indicator {@code ArrayList} is already
     * in it's segmented form, meaning it is only a block of the total indicators/destinations
     * requested
     * 
     * @param indicadores - Destinations to be converted to {@code String}
     * @return <b>{@code String[]}</b> - Coordinates of indicators in {@code String} form
     */
    public String[] getStringsFromArrayList(ArrayList<Indicador> indicadores) {
    	int j = indicadores.size();
    	if(j > 25) {
            System.out.println("Error: Indicator array greater than 25");
    	}
    	String[] segment = new String[j];
    	Arrays.fill(segment, null);
    	for(int i = 0; i < j; i++) {
            segment[i] = indicadores.get(i).getLatLon().toUrlValue();
    	}
    	return segment;
    }
    
    /**
     * This method will print the timings and statistics related to making the necessary requests. <br>
     * Currently the timings refer to the whole process of making a request, but as it
     * was demonstrated in a test, an average of 99.52% of the time is spent obtaining the response
     * from the server.
     * 
     * <br><br><b>Note:</b> Taking out request restrictions doesn't seem to make any difference on time
     * taken to obtain a response.
     */
    public void printTimings(long sum, boolean allTravelModes) {
    	if(this.benchmark) {
            int numberOfRequests = this.segmentedDestinations.size();
            if(allTravelModes) {
                numberOfRequests *= NUMBER_TRAVEL_MODES;
            }
            System.out.println("\n------------------------Request Statistics------------------------");
            System.out.println("Number of requests made: " + numberOfRequests);
            System.out.println("Indicators processed: " + this.destinations.size());
            System.out.println("All TravelModes: " + allTravelModes);
            System.out.println("Time taken to make all requests: " + sum + " ms");
            if (numberOfRequests == 0) {
                System.out.println("Number of requests is zero.");
            } else {
                System.out.println("Average Time Taken per request: " + sum / numberOfRequests + " ms");
            }
            System.out.println("------------------------------------------------------------------\n");
        }
    }
    
}