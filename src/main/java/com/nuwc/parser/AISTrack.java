package com.nuwc.parser;

import java.util.ArrayList;
import java.util.List;

public class AISTrack
{
    int mmsi;
    float length;
    String type;
    String destName;
    List<AISPoint> points;

    public AISTrack(int mmsi)
    {
        this.mmsi = mmsi;
        points = new ArrayList<>();
    }

    public AISPoint findClosestPoint(long timestamp)
    {
        if (points.size() < 1)
        {
            return null;
        }

        long minDelta = Math.abs(points.get(0).timestamp - timestamp);
        AISPoint closestPoint = points.get(0);
        for (AISPoint point : points)
        {
            long delta = Math.abs(point.timestamp - timestamp);
            if (delta < minDelta)
            {
                minDelta = delta;
                closestPoint = point;
            }
        }

        AISPoint copy = new AISPoint();
        copy.lat = closestPoint.lat;
        copy.lon = closestPoint.lon;
        copy.sog = closestPoint.sog;
        copy.cog = closestPoint.cog;
        copy.timestamp = closestPoint.timestamp;

        return copy;
    }
}
