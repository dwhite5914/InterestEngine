package com.nuwc.interestengine.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

public class Region
{
    GeoPosition corner1, corner2;
    Color borderColor = Color.RED;
    Color fillColor = new Color(1f, 1f, 1f, 0.5f);

    public Region(GeoPosition corner1, GeoPosition corner2,
            Color fillColor, Color borderColor)
    {
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
    }
    
    public GeoPosition getCenter()
    {
        double centerLat = (corner1.getLatitude() + corner2.getLatitude()) / 2;
        double centerLon = (corner1.getLongitude() + corner2.getLongitude()) / 2;
        
        return new GeoPosition(centerLat, centerLon);
    }

    public void draw(Graphics g, JXMapViewer map)
    {
        Graphics2D g2 = (Graphics2D) g;
        Point2D point1 = map.convertGeoPositionToPoint(corner1);
        Point2D point2 = map.convertGeoPositionToPoint(corner2);
        int x = (int) point1.getX();
        int y = (int) point1.getY();
        int width = (int) Math.abs(point2.getX() - point1.getX());
        int height = (int) Math.abs(point2.getY() - point1.getY());
        g2.setColor(fillColor);
        g2.fillRect(x, y, width, height);
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(x, y, width, height);
    }
}
