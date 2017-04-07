package com.nuwc.interestengine.clustering;

import com.nuwc.interestengine.Utils;
import com.nuwc.interestengine.data.AISPoint;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

public class Vessel
{
    public int mmsi;
    public List<AISPoint> track;
    public String shipType;
    public ShipStatus status;
    public float averageSpeed;

    public List<Long> wpTimestamps;
    public List<Cluster> waypoints;
    public List<RouteObject> routes;

    private Polygon lastPoly = null;

    private final Point2D basePoints[] =
    {
        new Point(-3, -4),
        new Point(-3, 4),
        new Point(0, 8),
        new Point(3, 4),
        new Point(3, -4),
        new Point(0, -2)
    };

    public Vessel(int mmsi)
    {
        this.mmsi = mmsi;
        shipType = null;
        averageSpeed = 0;
        track = new ArrayList<>();
        status = ShipStatus.SAILING;

        wpTimestamps = new ArrayList<>();
        waypoints = new ArrayList<>();
        routes = new ArrayList<>();
    }

    public AISPoint last()
    {
        return track.get(track.size() - 1);
    }

    public Point2D currentPosition(JXMapViewer map)
    {
        AISPoint current = last();
        GeoPosition position = new GeoPosition(current.lat, current.lon);
        return map.convertGeoPositionToPoint(position);
    }

    public boolean contains(Point2D point)
    {
        return lastPoly.contains(point);
    }

    public void draw(Graphics g, JXMapViewer map, boolean isSelected)
    {
        AISPoint current = last();
        GeoPosition position = new GeoPosition(current.lat, current.lon);
        Point2D center = map.convertGeoPositionToPoint(position);

        AffineTransform scale = AffineTransform
                .getScaleInstance(1.5, 1.5);
        AffineTransform rotate = AffineTransform
                .getRotateInstance(Math.toRadians(current.cog));
        AffineTransform translate = AffineTransform
                .getTranslateInstance(center.getX(), center.getY());

        Polygon poly = new Polygon();
        for (Point2D point : basePoints)
        {
            Point2D polyPoint = scale.transform(point, null);
            polyPoint = rotate.transform(polyPoint, null);
            polyPoint = translate.transform(polyPoint, null);
            poly.addPoint((int) polyPoint.getX(), (int) polyPoint.getY());
        }
        lastPoly = poly;

        Graphics2D g2 = (Graphics2D) g;
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        String shipCategory = Utils.getShipCategory(shipType);
        Color fillColor = Utils.getShipColor(shipCategory);
        g2.setColor(fillColor);
        g2.fillPolygon(poly);
        g2.setColor(fillColor.darker());
        g2.setStroke(new BasicStroke(2));
        g2.drawPolygon(poly);
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);

        if (isSelected)
        {
            int centerX = (int) center.getX();
            int centerY = (int) center.getY();
            int yBox = drawSelectionBox(g2, centerX, centerY);
            drawTextBox(g2, centerX, centerY, yBox);
        }
    }

    private int drawSelectionBox(Graphics2D g2, int centerX, int centerY)
    {
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(Color.RED);
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]
                {
                    2
                }, 0);
        g2.setStroke(dashed);
        int x = (int) centerX - 10;
        int y = (int) centerY - 10;
        int length = 20;
        g2.drawRect(x, y, length, length);
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);

        return y;
    }

    private void drawTextBox(Graphics2D g2, int centerX, int centerY, int yBox)
    {
        Color oldColor = g2.getColor();
        AISPoint current = last();
        String lines[] =
        {
            String.format("MMSI: %d", mmsi),
            String.format("Position: (%.2f, %.2f)", current.lat, current.lon),
            String.format("Speed: %.1f knots", current.sog),
            String.format("Course: %.0f %s", current.cog, Utils.DEGREE),
            String.format("Type: %s", Utils.getShipCategory(shipType))
        };
        String firstLine = lines[0];
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout layout = new TextLayout(firstLine, g2.getFont(), frc);
        Rectangle2D bounds = layout.getBounds();
        int lineHeight = (int) bounds.getHeight() + 5;
        int height = lineHeight * lines.length;
        int maxWidth = (int) bounds.getWidth();
        for (String line : lines)
        {
            layout = new TextLayout(line, g2.getFont(), frc);
            bounds = layout.getBounds();
            int width = (int) bounds.getWidth();
            if (width > maxWidth)
            {
                maxWidth = width;
            }
        }

        int x1 = centerX - maxWidth / 2;
        int y1 = yBox - height - 5;

        int rWidth = maxWidth + 10;
        int rHeight = height + 5;
        int rx1 = x1 - 5;
        int ry1 = y1;
        g2.setColor(new Color(0.9f, 0.9f, 0.9f, 0.7f));
        g2.fillRect(rx1, ry1, rWidth, rHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(rx1, ry1, rWidth, rHeight);
        for (int i = 1; i <= lines.length; i++)
        {
            g2.drawString(lines[i - 1], x1, y1 + i * lineHeight);
        }

        g2.setColor(oldColor);
    }
}
