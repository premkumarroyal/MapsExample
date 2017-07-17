package whiteelephant.com.mapsexample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import whiteelephant.com.mapsexample.events.AddressFetchedEvent;
import whiteelephant.com.mapsexample.events.DirectionsEvent;
import whiteelephant.com.mapsexample.models.Directions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, LocationListener, View.OnClickListener {

    private GoogleMap _map;
    private TextInputLayout _textInputDrop;
    private TextInputLayout _textInputPickup;
    private TextInputEditText _txtEdtDrop;
    private TextInputEditText _txtEdtPickup;
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int PLAY_SERVICES_REQUEST = 101;
    private static final int REQUEST_PICKUP_LOCATION = 102;
    private static final int REQUEST_DROP_LOCATION = 103;
    private GoogleApiClient _googleApiClient;
    private Location lastLocation;
    private Handler _handler = new Handler();
    private LatLng _pickupLatLng, _dropLatLng;
    LinearLayout _bottomSheet;
    RecyclerView _bottomSheetListView;
    private BottomSheetBehavior _bottomSheetBehavior;
    DirectionsBottomsheetAdapter _adapter;
    LinearLayout _routeLay;
    LinearLayout _routes;
    TextView _noRoutes, _totalKMS, _totalTime;
    ProgressBar _progress;
    List<Polyline> _polylineList = new ArrayList<>();
    private static final int MAP_PADDING = 80;

    Marker _pickupMarker, _dropMarker;

    String TAG = MapsActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        setupUI();

        if (isLocationPermissionGranted()) {
            initiateLocationLookup();
        } else {
            requestLocationPermission();
        }

        BusProvider.getInstance().register(this);
    }

    private void setupUI() {
        _textInputPickup = findViewById(R.id.txt_input_pickup_location);
        _textInputDrop = findViewById(R.id.txt_input_drop_location);
        _txtEdtPickup = findViewById(R.id.pickup_location);
        _txtEdtDrop = findViewById(R.id.drop_location);
        _routes = findViewById(R.id.routes);
        _noRoutes = findViewById(R.id.no_routes);
        _routeLay = findViewById(R.id.route_lay);
        _progress = findViewById(R.id.progress);

        _totalKMS = findViewById(R.id.total_kms);
        _totalTime = findViewById(R.id.total_time);

        _bottomSheet = findViewById(R.id.directions_sheet);
        _bottomSheetListView = findViewById(R.id.navigation_directions_list);
        _bottomSheetListView.setLayoutManager(new LinearLayoutManager(this));

        _bottomSheetBehavior = BottomSheetBehavior.from(_bottomSheet);
        _bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        _bottomSheetBehavior.setHideable(false);
        _bottomSheetBehavior.setHideable(true);

        // set onclick listeners
        _txtEdtPickup.setOnClickListener(this);
        _txtEdtDrop.setOnClickListener(this);
        _routeLay.setOnClickListener(this);

    }

    boolean validations() {
        if (Utils.isStringEmpty(_txtEdtPickup.getText().toString())) {
            _textInputPickup.setError(getString(R.string.pick_up_empty));
            _textInputDrop.setError(null);
            return false;
        }

        if (Utils.isStringEmpty(_txtEdtDrop.getText().toString())) {
            _textInputDrop.setError(getString(R.string.drop_empty));
            _textInputPickup.setError(null);
            return false;
        }

        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        _map = googleMap;
        // checking location permission
        if (isLocationPermissionGranted()) {
            _map.setMyLocationEnabled(true);
        }
    }


    /**
     * check is location permission granted. return true if permission is grant else
     * it return false
     */
    private boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Utils.showAlert(this, "Location Access", "" +
                            "To avoid typing your pickup address, we need to access your location," +
                            "would like to give us permission to access your location ?",
                    "Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST);
                        }
                    }, "No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
            return;

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions granted
                    initiateLocationLookup();

                } else {
                    //permission denied
                    Log.d(TAG, "User denied location");
                }
                return;
            }
        }
    }

    private void initiateLocationLookup() {
        // check whether location services are disabled
        if (!Utils.isLocationServiceEnabled(getApplicationContext())) {
            Utils.showAlert(this, "Location settings", "To determine your current location," +
                            " you need to enable GPS, would you like to enable it now ?",
                    "Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    }, "No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
            return;
        }

        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();
        int result = googleApi.isGooglePlayServicesAvailable(this);
        if (result == ConnectionResult.SUCCESS) {
            buildGoogleApiClient();
        } else {
            if (googleApi.isUserResolvableError(result)) {
                googleApi.getErrorDialog(this, result, PLAY_SERVICES_REQUEST).show();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // checking location permission
        if (isLocationPermissionGranted()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(_googleApiClient,
                    mLocationRequest, this);
        } else {
            Log.d(TAG, "User denied the permissions");
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
    }

    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "Building google api client...");

        _googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        _googleApiClient.connect();
    }


    @Subscribe
    public void onAddressFetched(AddressFetchedEvent event) {
        Log.d(TAG, "onAddressFetched ");
        if (event != null && event._address != null) {
            String fullAddress = "";
            Address address = event._address;
            if (address.getSubLocality() != null) {
                fullAddress = fullAddress + address.getSubLocality();
            }
            if (address.getLocality() != null) {
                fullAddress = fullAddress + " ," + address.getLocality();
            }
            _txtEdtPickup.setText(fullAddress);

        } else {
            Log.d(TAG, "Address is null");
            Toast.makeText(this, getString(R.string.address_not_found), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        BusProvider.getInstance().unregister(this);
    }

    public void moveMarker(Double lat, Double lng) {
        Log.d(TAG, "moving marker to lat : " + lat + ", lng : " + lng);
        LatLng place = new LatLng(lat, lng);
        // remove if any previous markers
        if (_pickupMarker != null) _pickupMarker.remove();
        _pickupMarker = _map.addMarker(new MarkerOptions().position(place).title("Pickup")
                .icon(BitmapDescriptorFactory
                        .fromBitmap(getSmallerSize(R.drawable.pickup))));
        _map.animateCamera(CameraUpdateFactory.newLatLngZoom(place, 17));
    }

    @Override
    public void onLocationChanged(Location location) {

        // remove location updates if location is accurate.
        if (lastLocation != null && lastLocation.getLatitude() == location.getLatitude()
                && lastLocation.getLongitude() == location.getLongitude()) {
            // checking location permission
            if (isLocationPermissionGranted()) {
                Log.d(TAG, "Location is accurate removing the location updates");
                LocationServices.FusedLocationApi.removeLocationUpdates(_googleApiClient, this);
            }
        } else {
            lastLocation = location;
            // move marker
            moveMarker(location.getLatitude(), location.getLongitude());
            _pickupLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            // get the address
            new GeoCodeAsynTask(this).execute(lastLocation.getLatitude(),
                    lastLocation.getLongitude());
        }

    }

    @Override
    public void onClick(View view) {

        Intent placeActivity = new Intent(this, PlaceSearchActivity.class);
        switch (view.getId()) {
            case R.id.pickup_location:
                //startActivityForResult(placeActivity, REQUEST_PICKUP_LOCATION);
                findPlace(REQUEST_PICKUP_LOCATION);
                break;
            case R.id.drop_location:
                //startActivityForResult(placeActivity, REQUEST_DROP_LOCATION);
                findPlace(REQUEST_DROP_LOCATION);
                break;
            case R.id.route_lay:
                if (_bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    _bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
        }
    }


    public void findPlace(int requestId) {
        int checkGooglePlayServices = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(this);
            startActivityForResult(intent, requestId);
        } catch (GooglePlayServicesRepairableException e) {
            GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices, this, 1000).show();
        } catch (GooglePlayServicesNotAvailableException e) {
            if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
                GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices, this, 1000).show();
            }
        }
    }

    // A place has been received; use requestCode to track the request.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Places search activity result");

        if (requestCode == REQUEST_PICKUP_LOCATION || requestCode == REQUEST_DROP_LOCATION) {
            if (resultCode == RESULT_OK) {
                // get place
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.d(TAG, "Place: " + place.getName());
                LatLng latlng = place.getLatLng();


                // set marker for pickup
                if (requestCode == REQUEST_PICKUP_LOCATION) {

                    // clearing pickup marker and adding new Pickup point
                    if (_pickupMarker != null) {
                        _pickupMarker.remove();
                    }
                    _pickupMarker = _map.addMarker(new MarkerOptions().position(latlng)
                            .title("Pickup")
                            .icon(BitmapDescriptorFactory
                                    .fromBitmap(getSmallerSize(R.drawable.pickup))));

                    _txtEdtPickup.setText(place.getAddress());
                    moveMarker(latlng.latitude, latlng.longitude);
                    _pickupLatLng = latlng;
                } else {

                    // clearing drop marker and adding new drop point
                    if (_dropMarker != null) {
                        _dropMarker.remove();
                    }

                    _dropMarker = _map.addMarker(new MarkerOptions().position(latlng)
                            .title("Drop")
                            .icon(BitmapDescriptorFactory
                                    .fromBitmap(getSmallerSize(R.drawable.drop))));

                    _dropLatLng = latlng;
                    _txtEdtDrop.setText(place.getAddress());
                }

                // if both pickup and drop avalible find the Directions
                if (!Utils.isStringEmpty(_txtEdtPickup.getText().toString()) &&
                        !Utils.isStringEmpty(_txtEdtDrop.getText().toString())) {
                    Log.d(TAG, "Calling getPossible routes");

                    // remove ploylines drawn before if any
                    removeAllPolylines();
                    // show ProgressBar
                    showProgress();

                    // get Directions from server
                    GetDirectionApiService.getPossibleDirections(this, _pickupLatLng, _dropLatLng,
                            getString(R.string.google_maps_key));

                }

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.i(TAG, status.getStatusMessage());
                showNoRoutesFound();

            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "User canceled the action");
            }
        } else {
            showNoRoutesFound();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (_googleApiClient != null && _googleApiClient.isConnected()
                && isLocationPermissionGranted()) {
            // stop receiving updates
            LocationServices.FusedLocationApi.removeLocationUpdates(_googleApiClient, this);
        }
    }

    @Subscribe
    public void onDirectionsFetched(DirectionsEvent event) {
        Log.d(TAG, "onDirectionsFetched : ");
        if (event != null && event._directions != null && event._directions.routes.size() > 0) {
            for (Directions.Routes route : event._directions.routes) {
                if (route.legs.size() != 0) {
                    for (final Directions.Legs legs : route.legs) {
                        if (!Utils.isListEmpty(route.legs)) {

                            // build latlong bound to set best fit for maps
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            for (Directions.Steps step : legs.steps) {
                                List<Directions.Steps> steps = legs.steps;
                                _bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                                Log.d(TAG, "Route : " + step.htmlInstructions);
                                if (_adapter == null) {
                                    _adapter = new DirectionsBottomsheetAdapter(this, steps);
                                    _bottomSheetListView.setAdapter(_adapter);
                                } else {
                                    _adapter.swap(steps);
                                }

                                // build latlong bound to set best fit for maps
                                builder.include(step.startLocation.getLatLng());
                                builder.include(step.endLocation.getLatLng());

                                // decoding path into latlng's
                                List<LatLng> decodedPath = PolyUtil.decode(step.polyline.points);

                                // prepare poly line
                                PolylineOptions options = new PolylineOptions()
                                        .addAll(decodedPath)
                                        .width(20)
                                        .color(Color.BLACK);

                                // add polyline
                                Polyline line = _map.addPolyline(options);
                                // set steps as a tag and retrive them on click of polyline
                                line.setTag(legs);
                                line.setClickable(true);

                                // add polyline to list and remove them when map change
                                _polylineList.add(line);

                                _bottomSheet.setVisibility(View.VISIBLE);

                                // showing route time and distance
                                setDistanceAndTime(legs.distance.text, legs.duration.text);

                                _map.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
                                    @Override
                                    public void onPolylineClick(Polyline polyline) {

                                        Log.d(TAG, "Clicked on Polyline");
                                        polyline.setColor(Color.BLACK);
                                        Directions.Legs legs = (Directions.Legs) polyline.getTag();
                                        if (_adapter == null) {
                                            _adapter = new DirectionsBottomsheetAdapter(
                                                    MapsActivity.this, legs.steps);
                                        } else {
                                            _adapter.swap(legs.steps);
                                        }
                                        setDistanceAndTime(legs.distance.text, legs.duration.text);
                                        _bottomSheetListView.setAdapter(_adapter);
                                    }
                                });
                            }
                            LatLngBounds bounds = builder.build();
                            // create the camera with bounds and padding to set into map
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING);
                            _map.animateCamera(cu);
                        } else showNoRoutesFound();
                    }
                } else showNoRoutesFound();
            }
        } else showNoRoutesFound();
    }

    /**
     * Method remove all polylines from map
     */
    private void removeAllPolylines() {
        if (_polylineList != null) {
            for (Polyline line : _polylineList) {
                line.remove();
            }
            _polylineList.clear();
        }
    }

    /**
     * show the passed distanace and estimated time
     *
     * @param distance
     * @param duration
     */
    void setDistanceAndTime(String distance, String duration) {
        _routeLay.setVisibility(View.VISIBLE);
        _noRoutes.setVisibility(View.GONE);
        _progress.setVisibility(View.GONE);
        _routes.setVisibility(View.VISIBLE);

        _totalKMS.setText(distance);
        _totalTime.setText(duration);
    }

    /**
     * Show No routes found text
     */
    void showNoRoutesFound() {
        Log.d(TAG, "No routes found");
        _routeLay.setVisibility(View.VISIBLE);
        _noRoutes.setVisibility(View.VISIBLE);
        _progress.setVisibility(View.GONE);
        _routes.setVisibility(View.GONE);
    }


    /**
     * Show Progress bar
     */
    void showProgress() {
        Log.d(TAG, "showing progress");
        _routeLay.setVisibility(View.VISIBLE);
        _noRoutes.setVisibility(View.GONE);
        _progress.setVisibility(View.VISIBLE);
        _routes.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {

        // close bottom sheet if it is open
        if (_bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED ||
                _bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            _bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            _bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }

        super.onBackPressed();
    }


    public Bitmap getSmallerSize(@DrawableRes int res) {
        int height = 100;
        int width = 100;
        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(res);
        Bitmap b = bitmapdraw.getBitmap();
        return Bitmap.createScaledBitmap(b, width, height, false);
    }
}
