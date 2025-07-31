package com.gg.maiotproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.util.BoundingBox;

import java.util.ArrayList;
import java.util.List; // Import List

public class SummaryActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView tvDistance;
    private TextView tvMaxPositiveAngle;
    private TextView tvMaxNegativeAngle;
    private Button bttRestart;

    private static final int WINDOW_SIZE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        mapView = findViewById(R.id.mapView);
        tvDistance = findViewById(R.id.tvDistance);
        tvMaxPositiveAngle = findViewById(R.id.tvMaxPositiveAngle);
        tvMaxNegativeAngle = findViewById(R.id.tvMaxNegativeAngle);
        bttRestart = findViewById(R.id.bttRestart);

        // Initialize the map
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue("MyApp/1.0");
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Get the data from the intent
        Intent intent = getIntent();
        ArrayList<GeoPoint> rawGeoPoints = intent.getParcelableArrayListExtra("geoPoints"); // Renamed to rawGeoPoints
        double distance = intent.getDoubleExtra("distance", 0);
        double maxPositiveRoll = intent.getDoubleExtra("maxPositiveAngle", 0);
        double maxNegativeRoll = intent.getDoubleExtra("maxNegativeAngle", 0);

        // Apply smoothing filter to the raw geoPoints before displaying them
        List<GeoPoint> displayedGeoPoints = new ArrayList<>();
        if (rawGeoPoints != null && !rawGeoPoints.isEmpty()) {
            displayedGeoPoints = applyMovingAverageFilter(rawGeoPoints, WINDOW_SIZE);
        }

        // Display the itinerary on the map
        Polyline polyline = new Polyline();
        polyline.setPoints(displayedGeoPoints); // Use the filtered points here
        mapView.getOverlays().add(polyline);

        // Add start and end markers and adjust map view
        if (!displayedGeoPoints.isEmpty()) { // Check against the displayed (filtered) points
            GeoPoint startPoint = displayedGeoPoints.get(0);
            GeoPoint endPoint = displayedGeoPoints.get(displayedGeoPoints.size() - 1);

            Marker startMarker = new Marker(mapView);
            startMarker.setPosition(startPoint);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setTitle("Start Point");
            mapView.getOverlays().add(startMarker);

            Marker endMarker = new Marker(mapView);
            endMarker.setPosition(endPoint);
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            endMarker.setTitle("End Point");
            mapView.getOverlays().add(endMarker);

            // Calculate the initial bounding box for all displayedGeoPoints
            BoundingBox initialBoundingBox = BoundingBox.fromGeoPoints(displayedGeoPoints);

            // Add a small margin to the bounding box to prevent points from being exactly on the edge
            double latMargin = initialBoundingBox.getLatitudeSpan() * 0.1;
            double lonMargin = initialBoundingBox.getLongitudeSpan() * 0.1;

            // Ensure there's a minimum span for very short lines or single points
            if (latMargin == 0) latMargin = 0.0005;
            if (lonMargin == 0) lonMargin = 0.0005;

            // Create a NEW BoundingBox instance with the adjusted margins
            final BoundingBox finalBoundingBox = new BoundingBox(
                    initialBoundingBox.getLatNorth() + latMargin,
                    initialBoundingBox.getLonEast() + lonMargin,
                    initialBoundingBox.getLatSouth() - latMargin,
                    initialBoundingBox.getLonWest() - lonMargin
            );

            // Use a post() to ensure the map has been laid out before attempting to zoom
            mapView.post(() -> {
                mapView.zoomToBoundingBox(finalBoundingBox, true);

                // Optional: Set a maximum zoom level to prevent over-zooming on very short paths.
                if (mapView.getZoomLevelDouble() > 18.0) {
                    mapView.getController().setZoom(18.0);
                }
            });

        } else {
            // If no geoPoints are available, set a default center and zoom
            mapView.getController().setZoom(10.0);
            Toast.makeText(this, "No path recorded for this journey.", Toast.LENGTH_LONG).show();
        }

        // Display the distance and angles
        tvDistance.setText(String.format("Distance: %.2f km", distance));
        tvMaxPositiveAngle.setText(String.format("Bank Angle LEFT: %.2f°", maxPositiveRoll));
        tvMaxNegativeAngle.setText(String.format("Bank Angle RIGHT: %.2f°", maxNegativeRoll));

        // Set up the restart button
        bttRestart.setOnClickListener(v -> {
            Intent restartIntent = new Intent(SummaryActivity.this, MainActivity.class);
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(restartIntent);
            Toast.makeText(getApplicationContext(), "Journey Cleared", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // Filters the GeoPoints to display a smoothed path
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
}