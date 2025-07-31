package com.gg.maiotproject.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapHandler {
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private WeakReference<Context> contextRef;
    private List<GeoPoint> geoPoints;
    private Polyline polyline;
    private LocationCallback locationCallback;
    private Marker currentLocationMarker;

    private static final double DISTANCE_THRESHOLD_METERS = 3.0; // Movement threshold
    private static final int WINDOW_SIZE = 2;

    // The constructor initializes the MapHandler with the application context and MapView instance from the layout
    public MapHandler(Context context, MapView mapView) {
        this.contextRef = new WeakReference<>(context);
        this.mapView = mapView;

        // Loads osmdroid's configuration and sets the user agent
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue("MyApp/1.0");

        // Configures the MapView to use MAPNIK as tile source and set initial zoom level
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(20.0);

        // Instantiates Google's recommended API for location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        // Initializes an array to store GeoPoints (latitude/longitude) and creates a polyline obj to draw the path
        geoPoints = new ArrayList<>();
        polyline = new Polyline();
        mapView.getOverlays().add(polyline);

        // Initialize a marker to display the user's current position on the map
        currentLocationMarker = new Marker(mapView);
        currentLocationMarker.setTitle("Current Location");
        mapView.getOverlays().add(currentLocationMarker);
    }

    // Calculates distance of journey
    // It iterates through the geoPoints calculating distance between consecutive point using distanceToDouble()
    public double getDistance() {
        double distance = 0.0;
        for (int i = 1; i < geoPoints.size(); i++) {
            distance += geoPoints.get(i - 1).distanceToAsDouble(geoPoints.get(i));
        }
        return distance / 1000; // Convert from meters to kilometers
    }

    // Returns the recorded path (crucial for passing to SummaryActivity
    public List<GeoPoint> getGeoPoints() {
        return geoPoints;
    }

    // Implements a moving average filter to smooth out fluctuations in GPS readings
    private List<GeoPoint> applyMovingAverageFilter(List<GeoPoint> points, int WINDOW_SIZE) {
        List<GeoPoint> smoothedPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            double sumLat = 0.0;
            double sumLon = 0.0;
            int count = 0;
            for (int j = Math.max(0, i - WINDOW_SIZE); j <= Math.min(points.size() - 1, i + WINDOW_SIZE); j++) {
                sumLat += points.get(j).getLatitude();
                sumLon += points.get(j).getLongitude();
                count++;
            }
            smoothedPoints.add(new GeoPoint(sumLat / count, sumLon / count));
        }
        return smoothedPoints;
    }

    @SuppressLint("MissingPermission")
    public void startTracking() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 250)
                .setMinUpdateIntervalMillis(250)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude()); // When new location is received a GeoPoint is created
                    if (!geoPoints.isEmpty()) {
                        GeoPoint lastPoint = geoPoints.get(geoPoints.size() - 1);
                        double distance = lastPoint.distanceToAsDouble(point);
                        if (distance < DISTANCE_THRESHOLD_METERS) {
                            continue; // Skip this point if the distance is below the threshold
                        }
                    }
                    geoPoints.add(point);
                    List<GeoPoint> smoothedPoints = applyMovingAverageFilter(geoPoints, WINDOW_SIZE); // moving average method called to smooth out points when drawing
                    polyline.setPoints(smoothedPoints);
                    mapView.getController().setCenter(point);

                    // Update the marker position
                    currentLocationMarker.setPosition(point);
                    mapView.invalidate();
                }
            }
        };
        // Starts receiving location updates on the main Looper thread
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @SuppressLint("MissingPermission")
    public void displayCurrentLocation() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Used to get most recently location
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().setCenter(point);
                    currentLocationMarker.setPosition(point);
                    mapView.invalidate();
                } else {
                    // Handle the case where location is null after the delay
                    Toast.makeText(contextRef.get(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        }, 500); // Delay
    }

    // Cleans up tracking resources
    public void stopTracking() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);    // Stops receiving location updates
        }

        polyline.setPoints(geoPoints);
        mapView.invalidate();
    }
}