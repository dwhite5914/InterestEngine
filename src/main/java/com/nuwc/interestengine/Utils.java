package com.nuwc.interestengine;

import com.nuwc.interestengine.map.Marker;
import dk.tbsalling.aismessages.ais.messages.types.NavigationStatus;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import javax.swing.JTextField;
import javax.swing.border.Border;
import org.jxmapviewer.viewer.GeoPosition;

public final class Utils
{
    public static final String DEGREE = "\u00b0";

    private static final HashMap<String, String> shipCategory = new HashMap<>();
    private static final HashMap<String, Color> shipColor = new HashMap<>();
    private static final String shipTypes[] =
    {
        "Cargo Vessel",
        "Tanker",
        "Passenger Vessel",
        "High Speed Craft",
        "Tugs & Special Craft",
        "Fishing",
        "Pleasure Craft",
        "Miscellaneous",
        "Unknown"
    };

    private Utils()
    {
        // Just a function library.
    }

    public static String[] getShipTypes()
    {
        return shipTypes;
    }

    public static String getShipCategory(String type)
    {
        if (shipCategory.isEmpty())
        {
            shipCategory.put("Cargo", "Cargo Vessel");
            shipCategory.put("CargoHazardousA", "Cargo Vessel");
            shipCategory.put("CargoHazardousB", "Cargo Vessel");
            shipCategory.put("CargoHazardousC", "Cargo Vessel");
            shipCategory.put("CargoHazardousD", "Cargo Vessel");
            shipCategory.put("CargoFuture1", "Cargo Vessel");
            shipCategory.put("CargoFuture2", "Cargo Vessel");
            shipCategory.put("CargoFuture3", "Cargo Vessel");
            shipCategory.put("CargoFuture4", "Cargo Vessel");
            shipCategory.put("CargoNoAdditionalInfo", "Cargo Vessel");

            shipCategory.put("Tanker", "Tanker");
            shipCategory.put("TankerHazardousA", "Tanker");
            shipCategory.put("TankerHazardousB", "Tanker");
            shipCategory.put("TankerHazardousC", "Tanker");
            shipCategory.put("TankerHazardousD", "Tanker");
            shipCategory.put("TankerFuture1", "Tanker");
            shipCategory.put("TankerFuture2", "Tanker");
            shipCategory.put("TankerFuture3", "Tanker");
            shipCategory.put("TankerFuture4", "Tanker");
            shipCategory.put("TankerNoAdditionalInfo", "Tanker");

            shipCategory.put("Passenger", "Passenger Vessel");
            shipCategory.put("PassengerHazardousA", "Passenger Vessel");
            shipCategory.put("PassengerHazardousB", "Passenger Vessel");
            shipCategory.put("PassengerHazardousC", "Passenger Vessel");
            shipCategory.put("PassengerHazardousD", "Passenger Vessel");
            shipCategory.put("PassengerFuture1", "Passenger Vessel");
            shipCategory.put("PassengerFuture2", "Passenger Vessel");
            shipCategory.put("PassengerFuture3", "Passenger Vessel");
            shipCategory.put("PassengerFuture4", "Passenger Vessel");
            shipCategory.put("PassengerNoAdditionalInfo", "Passenger Vessel");

            shipCategory.put("HighSpeedCraft", "High Speed Craft");
            shipCategory.put("HighSpeedCraftHarzardousA", "High Speed Craft");
            shipCategory.put("HighSpeedCraftHarzardousB", "High Speed Craft");
            shipCategory.put("HighSpeedCraftHarzardousC", "High Speed Craft");
            shipCategory.put("HighSpeedCraftHarzardousD", "High Speed Craft");

            shipCategory.put("WingInGround", "Tugs & Special Craft");
            shipCategory.put("WingInGroundHazardousA", "Tugs & Special Craft");
            shipCategory.put("WingInGroundHazardousB", "Tugs & Special Craft");
            shipCategory.put("WingInGroundHazardousC", "Tugs & Special Craft");
            shipCategory.put("WingInGroundHazardousD", "Tugs & Special Craft");
            shipCategory.put("Towing", "Tugs & Special Craft");
            shipCategory.put("LargeTowing", "Tugs & Special Craft");
            shipCategory.put("DredgingOrUnderwaterOps", "Tugs & Special Craft");
            shipCategory.put("DivingOps", "Tugs & Special Craft");
            shipCategory.put("MilitaryOps", "Tugs & Special Craft");
            shipCategory.put("PilotVessel", "Tugs & Special Craft");
            shipCategory.put("SearchAndRescueVessel", "Tugs & Special Craft");
            shipCategory.put("Tug", "Tugs & Special Craft");
            shipCategory.put("PortTender", "Tugs & Special Craft");
            shipCategory.put("AntiPollutionEquipment", "Tugs & Special Craft");
            shipCategory.put("LawEnforcement", "Tugs & Special Craft");
            shipCategory.put("SpareLocalVessel1", "Tugs & Special Craft");
            shipCategory.put("SpareLocalVessel2", "Tugs & Special Craft");
            shipCategory.put("MedicalTransport", "Tugs & Special Craft");
            shipCategory.put("ShipAccordingToRRResolutionNo18", "Tugs & Special Craft");

            shipCategory.put("Fishing", "Fishing");

            shipCategory.put("PleasureCraft", "Pleasure Craft");
            shipCategory.put("Sailing", "Pleasure Craft");

            shipCategory.put("Other", "Miscellaneous");
            shipCategory.put("OtherHazardousA", "Miscellaneous");
            shipCategory.put("OtherHazardousB", "Miscellaneous");
            shipCategory.put("OtherHazardousC", "Miscellaneous");
            shipCategory.put("OtherHazardousD", "Miscellaneous");
            shipCategory.put("OtherFuture1", "Miscellaneous");
            shipCategory.put("OtherFuture2", "Miscellaneous");
            shipCategory.put("OtherFuture3", "Miscellaneous");
            shipCategory.put("OtherFuture4", "Miscellaneous");
            shipCategory.put("OtherNoAdditionalInfo", "Miscellaneous");

            shipCategory.put("NotAvailable", "Unknown");
        }

        if (type == null || !shipCategory.containsKey(type))
        {
            return "Unknown";
        }
        else
        {
            return shipCategory.get(type);
        }
    }

    public static Color getShipColor(String category)
    {
        if (shipColor.isEmpty())
        {
            shipColor.put("Cargo Vessel", Color.GREEN);
            shipColor.put("Tanker", Color.RED);
            shipColor.put("Passenger Vessel", Color.BLUE);
            shipColor.put("High Speed Craft", Color.YELLOW);
            shipColor.put("Tugs & Special Craft", Color.CYAN);
            shipColor.put("Fishing", Color.ORANGE);
            shipColor.put("Pleasure Craft", Color.MAGENTA);
            shipColor.put("Miscellaneous", Color.PINK);
            shipColor.put("Unknown", Color.GRAY);
        }

        if (category == null || !shipColor.containsKey(category))
        {
            return Color.LIGHT_GRAY;
        }
        else
        {
            return shipColor.get(category);
        }
    }

    public static boolean isAnchored(NavigationStatus status)
    {
        return (status == NavigationStatus.Aground
                || status == NavigationStatus.AtAnchor
                || status == NavigationStatus.Moored);
    }

    public static Border getTableBorder()
    {
        return new JTextField().getBorder();
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
