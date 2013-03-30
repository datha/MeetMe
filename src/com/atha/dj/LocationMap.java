package com.atha.dj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LocationMap extends Activity implements LocationListener,
		OnMarkerDragListener {

	private GoogleMap map;
	private LocationManager locationManager;
	private String provider;
	private Button sendLocationButton;
	private Marker currentLocation;
	private String phoneNumber;
	private CameraUpdate cameraUpdate;
	private Criteria criteria;
	private MarkerOptions markerOptions;
	private double lat;
	private double lng;
	private TextView addressTV1;
	private Button backButton;
	private final static String BASE_MAP_URL = "http://maps.googleapis.com/maps/api/staticmap";
	private static final long minTime = 20000; // Update time 20 seconds
	private static final float minDistance = 5f;

	protected void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setContentView(R.layout.map);

		phoneNumber = getIntent().getExtras().getString("PhoneNumber");
		phoneNumber = cleanPhoneNumber(phoneNumber);

		addressTV1 = (TextView) findViewById(R.id.address1TextView);

		initMapView();
		initLocationManager();
		initSendButton();
		initBackButton();

	}

	private void initBackButton() {
		backButton = (Button) findViewById(R.id.backButton);
		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void initMapView() {
		map = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.locationMap)).getMap();
		map.setOnMarkerDragListener(this);

		markerOptions = new MarkerOptions();
	}

	private void initLocationManager() {
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		criteria = new Criteria();
		provider = locationManager.getBestProvider(criteria, true); // Only
																	// return a
																	// provider
																	// if it's
																	// enabled
																	// will
																	// return
																	// one with
																	// best
																	// accuracy
		locationManager.requestLocationUpdates(provider, minTime, minDistance,
				this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		provider = locationManager.getBestProvider(criteria, true);
		locationManager.requestLocationUpdates(provider, minTime, minDistance,
				this);
	}

	@Override
	protected void onPause() {
		locationManager.removeUpdates(this);
		super.onPause();

	}

	@Override
	protected void onDestroy() {
		locationManager.removeUpdates(this);
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		locationManager.removeUpdates(this); // <8>
		super.onStop();

	}

	private String cleanPhoneNumber(String phoneNumber) {
		phoneNumber = phoneNumber.replaceAll("[^0-9]+", "");
		return phoneNumber;
	}

	private void initSendButton() {
		sendLocationButton = (Button) findViewById(R.id.sendLocationButton);
		sendLocationButton.setOnClickListener(new OnClickListener() {

			private AsyncTask<String, Void, String> shortUriRunner;

			@Override
			public void onClick(View v) {
				LatLng latLng = currentLocation.getPosition();
				Uri uri = buildMapUri(latLng);
				shortUriRunner = new GetShortURL();
				shortUriRunner.execute(uri.toString());
				String string = null;
				try {
					string = shortUriRunner.get();
				} catch (Exception e) {
					CharSequence text = e.getMessage();
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(LocationMap.this, text,
							duration);
					toast.show();
				}

				String addressText = getAddress(latLng);
				sendSMSMessage(phoneNumber, string);
				sendSMSMessage(phoneNumber, addressText);
				showAlert("Message Sent");
			}
		});
	}

	protected String getAddress(LatLng latLng) {
		Geocoder gcd = new Geocoder(LocationMap.this, Locale.getDefault());
		List<Address> addresses = null;
		String addressText = null;
		try {
			double lat = latLng.latitude;
			double lng = latLng.longitude;
			addresses = gcd.getFromLocation(lat, lng, 1);
			addressText = addresses.get(0).getAddressLine(0) +", " + addresses.get(0).getLocality(); //Get the first address and city returned,
		} catch (IOException e) {
			CharSequence text = "Could not get determine address";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(LocationMap.this, text, duration);
			toast.show();
		}

		return addressText;

	}

	protected void showAlert(String message) {
		final ConfirmContactDialog alert = new ConfirmContactDialog(
				LocationMap.this);
		alert.setMessageTop(message);
		alert.getOkButton().setText("Back");
		alert.getOkButton().setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				alert.dismiss();
				finish();
			}
		});

		alert.getCancelButton().setVisibility(View.INVISIBLE);
		alert.show();
	}

	protected void sendSMSMessage(String phoneNumber, String message) {
		SmsManager sms = SmsManager.getDefault();
		if (message != null) {
			sms.sendTextMessage(phoneNumber, null, message, null, null);
		}

	}

	protected Uri buildMapUri(LatLng position) {
		double lng = position.longitude;
		double lat = position.latitude;
		String lngLatString = (Double.toString(lat) + "," + Double
				.toString(lng));
		Builder uriBuilder = Uri.parse(BASE_MAP_URL).buildUpon();
		uriBuilder.appendQueryParameter("center", lngLatString);
		uriBuilder.appendQueryParameter("markers", lngLatString);
		uriBuilder.appendQueryParameter("size", "500x300");
		uriBuilder.appendQueryParameter("zoom", "15");
		uriBuilder.appendQueryParameter("sensor", "true"); //true because I am using a sensor (network or gps) to determine position
		uriBuilder.appendQueryParameter("key", getString(R.string.map_api_key));

		return uriBuilder.build();

	}

	public void displayLocationOnMap(Location location) {

		if (location != null) {
			map.clear(); // Clear old marker

			LatLng latLng = new LatLng(lat, lng);
			markerOptions.position(latLng);
			currentLocation = map.addMarker(markerOptions);
			currentLocation.setDraggable(true);
			cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
			map.animateCamera(cameraUpdate);
		} else {
			CharSequence text = "Could not get location.";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(LocationMap.this, text, duration);
			toast.show();
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location != null) {
			lat = location.getLatitude();
			lng = location.getLongitude();
			displayLocationOnMap(location);
			upateAddressView(getAddress(new LatLng(lat, lng)));
		}

	}

	@Override
	public void onProviderDisabled(String arg0) {

	}

	@Override
	public void onProviderEnabled(String provider) {
		locationManager.requestLocationUpdates(provider, minTime, minDistance,
				this);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	private class GetShortURL extends AsyncTask<String, Void, String> {
		private ProgressDialog progressDialog;

		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(LocationMap.this);
			progressDialog.setMessage("Sending...");
			progressDialog.show();
		}

		protected String doInBackground(String... uris) {
			String tinyUrl = null;
			try {

				HttpClient client = new DefaultHttpClient();
				String uri = "http://tinyurl.com/api-create.php?url=" + uris[0];
				HttpGet request = new HttpGet(uri);
				HttpResponse response = client.execute(request);
				HttpEntity entity = response.getEntity();
				InputStream in = entity.getContent();
				try {
					StatusLine statusLine = response.getStatusLine();
					int statusCode = statusLine.getStatusCode();
					//make sure response is ok
					if (statusCode == HttpStatus.SC_OK) {
						String enc = "utf-8";
						Reader reader = new InputStreamReader(in, enc);
						BufferedReader bufferedReader = new BufferedReader(
								reader);
						tinyUrl = bufferedReader.readLine();
						//if response is empty
						if (tinyUrl != null) {
						} else {
							throw new IOException("empty response");
						}
						//if bad response throw error to show in toast
					} else {
						String msg = "Unexpected Response"
								+ Integer.toString(statusCode);
						throw new IOException(msg);
					}
					//close input stream
				} finally {
					in.close();
				}
			} catch (IOException e) {
				tinyUrl = "ERROR";
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(LocationMap.this, e.getMessage(),
						duration);
				toast.show();
			}

			return tinyUrl;

		}

		protected void onPostExecute(String result) {
			progressDialog.dismiss();
		}

	}

	@Override
	public void onMarkerDrag(Marker marker) {

	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		markerOptions.position(marker.getPosition());
		upateAddressView(getAddress(currentLocation.getPosition()));
		locationManager.removeUpdates(this); // stop updating when user drags
												// icon so it doesn't change
												// locations

	}

	private void upateAddressView(String address) {
		addressTV1.setText(address);
	}

	@Override
	public void onMarkerDragStart(Marker marker) {

	}

}
