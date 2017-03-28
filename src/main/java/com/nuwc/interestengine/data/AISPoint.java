package com.nuwc.interestengine.data;

import com.nuwc.interestengine.clustering.Cluster;
import java.util.ArrayList;
import java.util.List;

public class AISPoint
{
    public int mmsi;
    public float lat;
    public float lon;
    public float sog;
    public float cog;
    public String shipType;
    public long timestamp;
    public boolean isVisited;
    public PointType type;
    public List<AISPoint> neighbors;
    public Cluster assignedCluster;

    public AISPoint()
    {
        this(0, 0, 0, 0, 0);
    }

    public AISPoint(float lat, float lon, float sog, float cog, long timestamp)
    {
        this.lat = lat;
        this.lon = lon;
        this.sog = sog;
        this.cog = cog;
        this.timestamp = timestamp;
        this.isVisited = false;
        this.type = PointType.UNCLASSIFIED;
        this.neighbors = new ArrayList<>();
    }

    public float distance(AISPoint point)
    {
        // Return Haversine distance between two points.
        final int R = 6371;  // Earth radius in km.
        double lat1 = this.lat;
        double lon1 = this.lon;
        double lat2 = point.getLat();
        double lon2 = point.getLon();
        double latDelta = Math.toRadians(lat2 - lat1);
        double lonDelta = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;  // Distance in km.
        return (float) Math.abs(d / 1.852);  // Convert distance to nautical miles.
    }

    public float getLat()
    {
        return lat;
    }

    public float getLon()
    {
        return lon;
    }

    public int getMmsi()
    {
        return mmsi;
    }

    public float getSog()
    {
        return sog;
    }

    public float getCog()
    {
        return cog;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public boolean isVisited()
    {
        return isVisited;
    }

    public PointType getType(int minPoints)
    {
        if (neighbors.size() >= minPoints)
        {
            type = PointType.CORE;
        }

        return type;
    }

    public void setMmsi(int mmsi)
    {
        this.mmsi = mmsi;
    }

    public void setLat(float lat)
    {
        this.lat = lat;
    }

    public void setLon(float lon)
    {
        this.lon = lon;
    }

    public void setSog(float sog)
    {
        this.sog = sog;
    }

    public void setCog(float cog)
    {
        this.cog = cog;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }

    public void setIsVisited(boolean isVisited)
    {
        this.isVisited = isVisited;
    }

    public void setType(PointType type)
    {
        this.type = type;
    }

    @Override
    public String toString()
    {
        return String.format(
                "lat = %f, lon = %f, sog = %f, cog = %f, timestamp = %d",
                lat, lon, sog, cog, timestamp);
    }
}
