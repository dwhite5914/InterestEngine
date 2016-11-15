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
public class Marker implements Waypoint
{
    private GeoPosition position;
    
    public Marker(GeoPosition position)
    {
        this.position = position;
    }
    
    public Marker(double lat, double lon)
    {
        position = new GeoPosition(lat, lon);
    }
    
    @Override
    public GeoPosition getPosition()
    {
        return new GeoPosition(position.getLatitude(), position.getLongitude());
    }
    
    public double getLatitude()
    {
        return position.getLatitude();
    }
    
    public void setLatitude(double lat)
    {
        position = new GeoPosition(lat, position.getLongitude());
    }
    
    public double getLongitude()
    {
        return position.getLongitude();
    }
    
    public void setLongitude(double lon)
    {
        position = new GeoPosition(position.getLatitude(), lon);
    }
}
