/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

/**
 *
 * @author Dan
 */
public class RoutePainter implements Painter<JXMapViewer>
{
    private final Color LINE_COLOR = Color.BLACK;
    private final Color MARKER_COLOR = Color.BLUE;
    private final Color SHIP_COLOR = Color.RED;

    private final int MARKER_RADIUS = 4;

    private List<Ship> ships;
    private Ship selectedShip;
    private Marker selectedMarker;
    private List<TriMarker> markers;

    public RoutePainter(List<Ship> ships, List<TriMarker> markers)
    {
        this.ships = ships;
        this.markers = markers;
    }

    public Ship getSelectedShip()
    {
        return selectedShip;
    }

    public Marker getSelectedMarker()
    {
        return selectedMarker;
    }

    public void setSelected(Ship ship, Marker marker)
    {
        selectedShip = ship;
        selectedMarker = marker;
        ship.fireRouteStateChange();
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int i, int i1)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        for (TriMarker marker : markers)
        {
            GeoPosition position = marker.getPosition();
            Point2D mapPoint = map.convertGeoPositionToPoint(position);
            if (g.getClip().contains(mapPoint))
            {
                paintTriMarker(g, mapPoint, marker.getCog(), getTriMarkerBI());
            }
        }
        for (Ship ship : ships)
        {
            List<Point2D> mapPoints = new ArrayList<>();
            for (Marker marker : ship.getMarkers())
            {
                GeoPosition position = marker.getPosition();
                Point2D mapPoint = map.convertGeoPositionToPoint(position);
                mapPoints.add(mapPoint);
                if (g.getClip().contains(mapPoint))
                {
                    if (ship.isStartMarker(marker))
                    {
                        paintSpecial(g, mapPoint,
                                getStartMarkerIcon().getImage());
                        if (marker == selectedMarker)
                        {
                            paintMarkerBorder(g, mapPoint, Color.RED);
                        }
                    }
                    else if (ship.isEndMarker(marker))
                    {
                        paintSpecial(g, mapPoint,
                                getEndMarkerIcon().getImage());
                        if (marker == selectedMarker)
                        {
                            paintMarkerBorder(g, mapPoint, Color.RED);
                        }
                    }
                    else
                    {
                        paintMarker(g, mapPoint, MARKER_COLOR);
                        if (marker == selectedMarker)
                        {
                            paintMarkerBorder(g, mapPoint, Color.RED);
                        }
                    }
                }
            }

            if (mapPoints.size() > 1)
            {
                g.setPaint(LINE_COLOR);
                if (ship == selectedShip)
                {
                    g.setStroke(new BasicStroke(2));
                }
                else
                {
                    g.setStroke(new BasicStroke(1));
                }
                Point2D start = mapPoints.remove(0);
                while (!mapPoints.isEmpty())
                {
                    Point2D end = mapPoints.remove(0);
                    int x1 = (int) start.getX();
                    int y1 = (int) start.getY();
                    int x2 = (int) end.getX();
                    int y2 = (int) end.getY();
                    g.drawLine(x1, y1, x2, y2);
                    start = end;
                }
            }

            GeoPosition currentPosition = ship.getCurrentPosition();
            if (currentPosition != null)
            {
                Point2D shipPosition = map.convertGeoPositionToPoint(currentPosition);
                if (g.getClip().contains(shipPosition))
                {
                    paintShip(g, shipPosition, SHIP_COLOR);
                }
            }
        }
    }

    private ImageIcon getStartMarkerIcon()
    {
        String path = "/com/nuwc/interestengine/resources/map/marker-start.png";
        return new ImageIcon(getClass().getResource(path));
    }

    private ImageIcon getEndMarkerIcon()
    {
        String path = "/com/nuwc/interestengine/resources/map/marker-end.png";
        return new ImageIcon(getClass().getResource(path));
    }

    private BufferedImage getTriMarkerBI()
    {
        String path = "/com/nuwc/interestengine/resources/map/marker-tri.png";
        try
        {
            return ImageIO.read(new File(getClass().getResource(path).getPath()));
        }
        catch (IOException ex)
        {
            return null;
        }
    }

    private void paintSpecial(final Graphics2D g, final Point2D point,
            Image icon)
    {
        int x = (int) point.getX();
        int y = (int) point.getY();
        int width = icon.getWidth(null);
        int height = icon.getHeight(null);
        g.drawImage(icon, x - width / 2, y - height, null);
    }

    private void paintMarker(Graphics2D g, Point2D point, Color color)
    {
        int x = (int) (point.getX() - MARKER_RADIUS);
        int y = (int) (point.getY() - MARKER_RADIUS);
        int diameter = MARKER_RADIUS * 2;
        g.setPaint(color);
        g.fillOval(x, y, diameter, diameter);
    }

    private void paintTriMarker(Graphics2D g, Point2D point, double cog, BufferedImage bi)
    {
        int x = (int) point.getX();
        int y = (int) point.getY();
        int width = bi.getWidth();
        int height = bi.getHeight();
        double rotation = Math.toRadians(cog);
        AffineTransform originalTransform = g.getTransform();
        AffineTransform transform = new AffineTransform();
        transform.rotate(rotation, x, y);
        g.setTransform(transform);
        g.drawImage(bi, x - width / 2, y - height / 2, null);
        g.setTransform(originalTransform);
    }

    private void paintShip(Graphics2D g, Point2D point, Color color)
    {
        int x = (int) (point.getX() - MARKER_RADIUS);
        int y = (int) (point.getY() - MARKER_RADIUS);
        int diameter = MARKER_RADIUS * 2;
        g.setPaint(color);
        g.fillRect(x, y, diameter, diameter);
        g.setStroke(new BasicStroke(1));
        g.setPaint(Color.BLACK);
        g.drawRect(x, y, diameter, diameter);
    }

    private void paintMarkerBorder(Graphics2D g, Point2D point, Color color)
    {
        int x = (int) (point.getX() - MARKER_RADIUS * 2);
        int y = (int) (point.getY() - MARKER_RADIUS * 2);
        int diameter = MARKER_RADIUS * 4;
        g.setPaint(color);
        g.setStroke(new BasicStroke(2));
        g.drawRect(x, y, diameter, diameter);
        g.setStroke(new BasicStroke(1));
    }
}
