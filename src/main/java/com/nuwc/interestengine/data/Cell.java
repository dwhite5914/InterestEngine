package com.nuwc.interestengine.data;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

public class Cell
{
    private float minLat, maxLat, minLon, maxLon;
    private Color color;

    public Cell(float minLat, float maxLat, float minLon, float maxLon)
    {
        this(minLat, maxLat, minLon, maxLon, Color.BLACK);
    }

    public Cell(float minLat, float maxLat, float minLon, float maxLon,
            Color color)
    {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.color = color;
    }

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        this.color = color;
    }

    public GeoPosition getCenter()
    {
        float centerLat = (minLat + maxLat) / 2;
        float centerLon = (minLon + maxLon) / 2;

        return new GeoPosition(centerLat, centerLon);
    }

    public void draw(Graphics g, JXMapViewer map)
    {
        Point2D lowerLeft = getMapPoint(map, new GeoPosition(maxLat, minLon));
        Point2D upperRight = getMapPoint(map, new GeoPosition(minLat, maxLon));
        int xLow = (int) lowerLeft.getX();
        int yLow = (int) lowerLeft.getY();
        int width = (int) (upperRight.getX() - lowerLeft.getX());
        int height = (int) (upperRight.getY() - lowerLeft.getY());

        Color oldColor = g.getColor();
        g.setColor(color);
        g.fillRect(xLow, yLow, width, height);
        g.setColor(oldColor);
    }

    private Point2D getMapPoint(JXMapViewer map, GeoPosition geo)
    {
        return map.convertGeoPositionToPoint(geo);
    }
}
