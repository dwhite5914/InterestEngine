package com.nuwc.interestengine.clustering;

import com.nuwc.interestengine.data.AISPoint;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

public class Cluster
{
    private final int ID;
    private boolean isActive;
    public Color color;
    public List<AISPoint> points;
    public final List<Integer> mmsiList;

    public Cluster(int id)
    {
        this.ID = id;
        this.isActive = true;
        this.color = Color.BLUE;
        this.mmsiList = new ArrayList<>();
        this.points = new ArrayList<>();
    }

    public Point2D getCentroid(JXMapViewer map)
    {
        float latSum = 0;
        float lonSum = 0;
        for (AISPoint point : points)
        {
            latSum += point.getLat();
            lonSum += point.getLon();
        }
        float latAve = latSum / points.size();
        float lonAve = lonSum / points.size();

        GeoPosition position = new GeoPosition(latAve, lonAve);
        Point2D mapPoint = map.convertGeoPositionToPoint(position);
        return mapPoint;
    }

    public int getID()
    {
        return ID;
    }

    public void setActive(boolean isActive)
    {
        this.isActive = isActive;
    }

    public boolean getIsActive()
    {
        return this.isActive;
    }
}
