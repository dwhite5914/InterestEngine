/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
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
    private final Color SELECTED_LINE_COLOR = Color.RED;
    private final Color MARKER_COLOR = Color.BLUE;
    private final Color SHIP_COLOR = Color.RED;
    private final Color START_COLOR = Color.YELLOW;
    private final Color END_COLOR = Color.GREEN;
    
    private final int MARKER_RADIUS = 5;
    private final int SHIP_RADIUS = 8;
    
    private List<Ship> ships;
    private Ship selectedShip;
    private Marker selectedMarker;
    
    public RoutePainter(List<Ship> builders)
    {
        ships = builders;
    }
    
    public Ship getSelectedShip()
    {
        return selectedShip;
    }
    
    public Marker getSelectedMarker()
    {
        return selectedMarker;
    }
    
    public void setSelected(Ship builder, Marker marker)
    {
        selectedShip = builder;
        selectedMarker = marker;
        builder.fireRouteStateChange();
    }
    
    @Override
    public void paint(Graphics2D g, JXMapViewer map, int i, int i1)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
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
                        paintMarker(g, mapPoint, START_COLOR);
                        if (marker == selectedMarker)
                        {
                            paintMarkerBorder(g, mapPoint, Color.RED);
                        }
                    }
                    else if (ship.isEndMarker(marker))
                    {
                        paintMarker(g, mapPoint, END_COLOR);
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
                System.out.println("CURRPOS");
                Point2D shipPosition = map.convertGeoPositionToPoint(currentPosition);
                if (g.getClip().contains(shipPosition))
                {
                    paintMarker(g, shipPosition, SHIP_COLOR);
                }
            }
        }
    }
    
    private void paintMarker(Graphics2D g, Point2D point, Color color)
    {
        int x = (int) (point.getX() - MARKER_RADIUS);
        int y = (int) (point.getY() - MARKER_RADIUS);
        int diameter = MARKER_RADIUS * 2;
        g.setPaint(color);
        g.fillOval(x, y, diameter, diameter);
    }
    
    private void paintMarkerBorder(Graphics2D g, Point2D point, Color color)
    {
        int x = (int) (point.getX() - MARKER_RADIUS);
        int y = (int) (point.getY() - MARKER_RADIUS);
        int diameter = MARKER_RADIUS * 2;
        g.setPaint(color);
        g.drawRect(x, y, diameter, diameter);
    }
}
