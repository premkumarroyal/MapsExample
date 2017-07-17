package whiteelephant.com.mapsexample;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import whiteelephant.com.mapsexample.events.AddressFetchedEvent;

/**
 * Created by prem on 15/07/2017.
 */

public class GeoCodeAsynTask extends AsyncTask<Double, Void, Address> {

    private final static int MAX_ADDRESS_LIST_SIZE = 1;
    private Context _context;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        BusProvider.getInstance().register(this);
    }

    public GeoCodeAsynTask(Context context) {
        _context = context;
    }

    @Override
    protected Address doInBackground(Double... doubles) {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(_context, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(doubles[0],doubles[1], MAX_ADDRESS_LIST_SIZE);
            return addresses.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Address address) {
        super.onPostExecute(address);
        BusProvider.getInstance().post(new AddressFetchedEvent(address));
        BusProvider.getInstance().unregister(this);
    }
}
