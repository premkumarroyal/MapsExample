package whiteelephant.com.mapsexample.api;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.POST;
import retrofit2.http.Query;
import whiteelephant.com.mapsexample.models.Directions;

/**
 * Created by prem on 29/11/2016.
 */

public interface GetDirectionInterface {


    /**
     * Generate token for forgot password...
     *
     * @param
     */
    @POST("/maps/api/directions/json?")
    Call<Directions> getDirections(@Query("origin") String origin,
                            @Query("destination") String destination,
                            @Query("key") String apiKey,
                            @Query("alternatives") boolean alternatives);

}
