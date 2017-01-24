/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine.map;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

/**
 *
 * @author Dan
 */
public class TriMarker implements Waypoint
{
    private GeoPosition position;
    private double cog;

    public TriMarker(double lat, double lon, double cog)
    {
        // Sets the position with the given latitude and longitude
        this.position = new GeoPosition(lat, lon);
        this.cog = cog;
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

    public double getCog()
    {
        return cog;
    }

    public void setCog(double cog)
    {
        this.cog = cog;
    }
}
