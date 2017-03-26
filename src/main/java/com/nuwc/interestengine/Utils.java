package com.nuwc.interestengine;

import com.nuwc.interestengine.map.Marker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import org.jxmapviewer.viewer.GeoPosition;

public final class Utils
{
    private Utils()
    {
        // Just a function library.
    }
    
    public static void updateProps(Properties props)
    {
        String path = Utils.getResource("config/config.properties");
        OutputStream os = null;
        try
        {
            os = new FileOutputStream(path);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Failed to find config file.");
        }
        
        try
        {
            props.store(os, null);
        }
        catch (IOException ex)
        {
            System.out.println("Failed to update properties.");
        }
    }

    public static void delay(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            // Pass.
        }
    }

    public static double distance(GeoPosition p1, GeoPosition p2)
    {
        double latDiff = p1.getLatitude() - p2.getLatitude();
        double lonDiff = p1.getLongitude() - p2.getLongitude();
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    public static double distance(GeoPosition p1, Marker p2)
    {
        double latDiff = p1.getLatitude() - p2.getLatitude();
        double lonDiff = p1.getLongitude() - p2.getLongitude();
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    public static double distance(Marker p1, GeoPosition p2)
    {
        double latDiff = p1.getLatitude() - p2.getLatitude();
        double lonDiff = p1.getLongitude() - p2.getLongitude();
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    public static double distance(Marker p1, Marker p2)
    {
        double latDiff = p1.getLatitude() - p2.getLatitude();
        double lonDiff = p1.getLongitude() - p2.getLongitude();
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    public static String getResource(String name)
    {
        URL url = Utils.class.getResource("resources/" + name);
        File file;
        try
        {
            file = new File(url.toURI());
        }
        catch (URISyntaxException e)
        {
            file = new File(url.getPath());
        }
        return file.getAbsolutePath();
    }
}
