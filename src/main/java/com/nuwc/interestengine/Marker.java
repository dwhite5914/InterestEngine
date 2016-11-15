/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

/**
 *
 * @author Dan
 */
public class Marker implements Waypoint
{
    private GeoPosition position;
    
    //Constructors 
    public Marker(GeoPosition position)
    {
        //Sets this position with given position object
        this.position = position;
    }
    
    public Marker(double lat, double lon)
    {
        //Sets position with given Latitude and Longitude
        position = new GeoPosition(lat, lon);
    }
    
 
    @Override
    public GeoPosition getPosition()
    {
        //Retrieves geoprahic position of marker 
        return new GeoPosition(position.getLatitude(), position.getLongitude());
    }
    
    public double getLatitude()
    {
        //Returns just the latitude of Marker
        return position.getLatitude();
    }
    
    public void setLatitude(double lat)
    {
        //Sets new Latitude of Marker with previous Longitude
        position = new GeoPosition(lat, position.getLongitude());
    }
    
    public double getLongitude()
    {
        //Retrieves just the Longitude of Marker
        return position.getLongitude();
    }
    
    public void setLongitude(double lon)
    {
        //Sets new Longitude with previous Latitude
        position = new GeoPosition(position.getLatitude(), lon);
    }
}
