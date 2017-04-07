package com.nuwc.interestengine.map;

import com.nuwc.interestengine.data.AISPoint;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

public class ShipPoly
{
    private final Point2D basePoints[] =
    {
        new Point(-3, -4),
        new Point(-3, 4),
        new Point(0, 8),
        new Point(3, 4),
        new Point(3, -4),
        new Point(0, -2)
    };

    private float lat, lon, sog, cog;
    private String shipType;

    public ShipPoly(AISPoint point)
    {
        this(point.lat, point.lon, point.sog, point.cog, point.shipType);
    }

    public ShipPoly(float lat, float lon, float sog, float cog, String shipType)
    {
        this.lat = lat;
        this.lon = lon;
        this.sog = sog;
        this.cog = cog;
        this.shipType = shipType;
    }

    public void draw(Graphics g, JXMapViewer map)
    {
        GeoPosition position = new GeoPosition(lat, lon);
        Point2D center = map.convertGeoPositionToPoint(position);

        AffineTransform rotate = AffineTransform
                .getRotateInstance(Math.toRadians(cog));
        AffineTransform translate = AffineTransform
                .getTranslateInstance(center.getX(), center.getY());

        Polygon poly = new Polygon();
        for (Point2D point : basePoints)
        {
            Point2D polyPoint = rotate.transform(point, null);
            polyPoint = translate.transform(polyPoint, null);
            poly.addPoint((int) polyPoint.getX(), (int) polyPoint.getY());
        }

        Color fillColor = Color.RED;
        Color oldColor = g.getColor();
        g.setColor(fillColor);
        g.fillPolygon(poly);
        g.setColor(fillColor.darker());
        g.drawPolygon(poly);
        g.setColor(oldColor);
    }
}
