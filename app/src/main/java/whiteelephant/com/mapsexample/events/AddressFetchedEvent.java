package whiteelephant.com.mapsexample.events;

import android.location.Address;

/**
 * Created by prem on 15/07/2017.
 */

public class AddressFetchedEvent {
    public Address _address;
    public boolean isError = false;

    public AddressFetchedEvent(Address address){
        _address =address;
    }
}
