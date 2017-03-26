package com.nuwc.interestengine.map;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

public class Marker implements Waypoint
{
    private GeoPosition position;

    // Constructors
    public Marker(GeoPosition position)
    {
        // Sets the position with the given position object
        this.position = position;
    }

    public Marker(double lat, double lon)
    {
        // Sets the position with the given latitude and longitude
        position = new GeoPosition(lat, lon);
    }

    @Override
    public GeoPosition getPosition()
    {
        // Returns the geographic position of the marker
        return new GeoPosition(position.getLatitude(), position.getLongitude());
    }

    public double getLatitude()
    {
        // Returns the latitude of the marker
        return position.getLatitude();
    }

    public void setLatitude(double lat)
    {
        // Sets just the latitude of the marker
        position = new GeoPosition(lat, position.getLongitude());
    }

    public double getLongitude()
    {
        // Returns the longitude or the marker
        return position.getLongitude();
    }

    public void setLongitude(double lon)
    {
        // Sets just the longitude of the marker
        position = new GeoPosition(position.getLatitude(), lon);
    }
}
