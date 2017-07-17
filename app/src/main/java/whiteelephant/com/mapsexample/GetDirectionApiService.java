package whiteelephant.com.mapsexample;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import whiteelephant.com.mapsexample.api.GetDirectionInterface;
import whiteelephant.com.mapsexample.events.DirectionsEvent;
import whiteelephant.com.mapsexample.models.Directions;

/**
 * Created by prem on 15/07/2017.
 */

public class GetDirectionApiService extends IntentService {


    private static final String PARAM_FROM_LAT = "from_lat";
    private static final String PARAM_FROM_LNG = "from_lng";
    private static final String PARAM_TO_LAT = "to_lat";
    private static final String PARAM_TO_LNG = "to_lng";
    private static final String PARAM_APIKEY = "apiKey";
    static final String TAG = Utils.getLogTAG(GetDirectionApiService.class);

    @Override
    public void onCreate() {
        super.onCreate();

        BusProvider.getInstance().register(this);
    }

    public GetDirectionApiService() {
        super("GetDirectionApiService");
    }

    public static void getPossibleDirections(@NonNull Context context, @NonNull LatLng fromLatLng,
                                             @NonNull LatLng toLatLng, @NonNull String mapsAPIKey) {
        Intent intent = new Intent(context, GetDirectionApiService.class);
        intent.putExtra(PARAM_FROM_LAT, fromLatLng.latitude);
        intent.putExtra(PARAM_FROM_LNG, fromLatLng.longitude);
        intent.putExtra(PARAM_TO_LAT, toLatLng.latitude);
        intent.putExtra(PARAM_TO_LNG, toLatLng.longitude);
        intent.putExtra(PARAM_APIKEY, mapsAPIKey);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            Log.d(TAG, "Handling intent");
            Double toLat = intent.getDoubleExtra(PARAM_TO_LAT, 0.0);
            Double toLng = intent.getDoubleExtra(PARAM_TO_LNG, 0.0);
            Double fromLat = intent.getDoubleExtra(PARAM_FROM_LAT, 0.0);
            Double fromLng = intent.getDoubleExtra(PARAM_FROM_LNG, 0.0);
            String apiKey = intent.getStringExtra(PARAM_APIKEY);

            final String origin = fromLat + ", " + fromLng;
            String destination = toLat + ", " + toLng;
            Log.d(TAG, "origin =  " + origin + ", destination : " + destination + ", APIKey : " + apiKey);

            if (fromLat != 0.0 && fromLng != 0.0 && toLat != 0.0 && toLng != 0.0 && apiKey != null) {
                Retrofit retrofit = new Retrofit.Builder().baseUrl("https://maps.googleapis.com")
                        .addConverterFactory(GsonConverterFactory.create()).build();
                GetDirectionInterface apiService = retrofit.create(GetDirectionInterface.class);
                Call<Directions> call = apiService.getDirections(origin, destination, apiKey, true);
                call.enqueue(new Callback<Directions>() {
                    @Override
                    public void onResponse(Call<Directions> call, Response<Directions> response) {
                        Log.d(TAG, "Hey... Got a response");

                        DirectionsEvent directions = new DirectionsEvent(response.body());
                        BusProvider.getInstance().post(directions);
                    }

                    @Override
                    public void onFailure(Call<Directions> call, Throwable t) {

                    }
                });
            } else {
                Log.d(TAG, " some values are unexpected");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BusProvider.getInstance().unregister(this);
    }
}
