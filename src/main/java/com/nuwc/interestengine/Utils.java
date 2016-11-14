/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine;

import org.jxmapviewer.viewer.GeoPosition;

/**
 *
 * @author Dan
 */
public final class Utils
{
    private Utils()
    {
        // Just a function library.
    }
    
    public static double distance(GeoPosition p1, GeoPosition p2)
    {
        double latDiff = p1.getLatitude() - p2.getLatitude();
        double lonDiff = p1.getLongitude() - p2.getLongitude();
        return Math.sqrt(latDiff*latDiff + lonDiff*lonDiff);
    }
    
    public static double distance(GeoPosition p1, Marker p2)
    {
        double latDiff = p1.getLatitude() - p2.getLatitude();
        double lonDiff = p1.getLongitude() - p2.getLongitude();
        return Math.sqrt(latDiff*latDiff + lonDiff*lonDiff);
    }
    
    public static double distance(Marker p1, GeoPosition p2)
    {
        double latDiff = p1.getLatitude() - p2.getLatitude();
        double lonDiff = p1.getLongitude() - p2.getLongitude();
        return Math.sqrt(latDiff*latDiff + lonDiff*lonDiff);
    }

    public static double distance(Marker p1, Marker p2)
    {
        double latDiff = p1.getLatitude() - p2.getLatitude();
        double lonDiff = p1.getLongitude() - p2.getLongitude();
        return Math.sqrt(latDiff*latDiff + lonDiff*lonDiff);
    }
}
