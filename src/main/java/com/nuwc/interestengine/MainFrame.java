/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.viewer.GeoPosition;

/**
 *
 * @author Dan
 */
public class MainFrame extends JFrame implements KeyListener
{
    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 600;
    private JXMapKit mapKit;
    private JPanel mapPanel;
    private JPanel optionsPanel;
    private List<Ship> ships;
    private RoutePainter routePainter;
    
    public MainFrame()
    {
        super();
        
        setTitle("Contact of Interest Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        
        initKeyListener();
        
        optionsPanel = new OptionsPanel();
        mapPanel = getMap();
        setLayout(new BorderLayout());
        add(mapPanel, BorderLayout.CENTER);
        add(optionsPanel, BorderLayout.EAST);
    }
    
    private void initKeyListener()
    {
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
    }
    
    private JXMapKit getMap()
    {
        if (mapKit == null)
        {
            mapKit = new JXMapKit();
            mapKit.setDefaultProvider(JXMapKit.DefaultProviders.OpenStreetMaps);
            mapKit.getMainMap().setOverlayPainter(getRoutePainter());
            MapMouseListener mapMouseListener = new MapMouseListener(mapKit);
            mapKit.getMainMap().addMouseListener(mapMouseListener);
            mapKit.getMainMap().addMouseMotionListener(mapMouseListener);
        }
        
        return mapKit;
    }
    
    private RoutePainter getRoutePainter()
    {
        if (routePainter == null)
        {
            routePainter = new RoutePainter(getShips());
        }
        
        return routePainter;
    }
    
    private List<Ship> getShips()
    {
        if (ships == null)
        {
            ships = new ArrayList<>();
        }
        
        return ships;
    }
    
    private class RouteListener implements RouteChangeListener
    {
        @Override
        public void routeChanged()
        {
            getMap().repaint();
        }
    }
    
    private class MapMouseListener extends MouseAdapter
    {
        private final double SELECT_THRESH = 8;
        
        private JXMapKit map;
        private JPopupMenu popup;
        private Point last;
        private boolean dragging;
        
        public MapMouseListener(JXMapKit map)
        {
            this.map = map;
            this.dragging = false;
        }
        
        @Override
        public void mouseClicked(MouseEvent e)
        {
            Point2D clickPoint = new Point(e.getX(), e.getY());
            GeoPosition mapPoint = map.getMainMap().
                                        convertPointToGeoPosition(clickPoint);
            if (SwingUtilities.isLeftMouseButton(e))
            {
                Ship selectedShip =
                        routePainter.getSelectedShip();
                if (e.isShiftDown() && selectedShip != null)
                {
                    selectedShip.addMarker(new Marker(mapPoint));
                }
                else
                {
                    Ship newBuilder = new Ship();
                    newBuilder.addRouteChangeListener(new RouteListener());
                    Marker newMarker = new Marker(mapPoint);
                    newBuilder.addMarker(newMarker);
                    ships.add(newBuilder);
                    routePainter.setSelected(newBuilder, newMarker);
                }
            }
            else if (SwingUtilities.isRightMouseButton(e))
            {
                FIND: for (Ship ship : ships)
                {
                    for (Marker marker : ship.getMarkers())
                    {
                        Point2D point = map.getMainMap()
                                            .convertGeoPositionToPoint(
                                            marker.getPosition());
                        double distance = Point.distance(point.getX(),
                                                            point.getY(),
                                                            clickPoint.getX(),
                                                            clickPoint.getY());
                        if (distance <= SELECT_THRESH)
                        {
                            routePainter.setSelected(ship, marker);
                            break FIND;
                        }
                    }
                }
            }
            
            // Print out the # of nodes for each Route Builder.
            for (Ship builder : ships)
            {
                System.out.print(builder.getMarkers().size() + " ");
            }
            System.out.println();
        }
        
        @Override
        public void mousePressed(MouseEvent e)
        {
            last = e.getPoint();
            Point2D clickPoint = new Point(e.getX(), e.getY());
            FIND: for (Ship ship : ships)
            {
                for (Marker marker : ship.getMarkers())
                {
                    Point2D point = map.getMainMap()
                                        .convertGeoPositionToPoint(
                                        marker.getPosition());
                    double distance = Point.distance(point.getX(),
                                                        point.getY(),
                                                        clickPoint.getX(),
                                                        clickPoint.getY());
                    if (distance <= SELECT_THRESH)
                    {
                        routePainter.setSelected(ship, marker);
                        dragging = true;
                        break FIND;
                    }
                }
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e)
        {
            dragging = false;
            last = null;
        }
        

        @Override
        public void mouseDragged(MouseEvent e) {
            int dx = e.getX() - last.x;
            int dy = e.getY() - last.y;
            if (dragging)
            {
                Marker selectedMarker = routePainter.getSelectedMarker();
                Point2D point = map.getMainMap()
                                    .convertGeoPositionToPoint(
                                    selectedMarker.getPosition());
                point.setLocation(point.getX() + dx, point.getY() + dy);
                point.setLocation(Math.max(5, point.getX()),
                                    Math.max(5, point.getY()));
                point.setLocation(Math.min(point.getX(), mapKit.getWidth() - 5),
                                    Math.min(point.getY(), mapKit.getHeight() - 5));
                GeoPosition mapPoint = map.getMainMap().convertPointToGeoPosition(point);
                selectedMarker.setLatitude(mapPoint.getLatitude());
                selectedMarker.setLongitude(mapPoint.getLongitude());
                routePainter.getSelectedShip().fireRouteStateChange();
            }
            last = e.getPoint();
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        if(key == KeyEvent.VK_DELETE || key == KeyEvent.VK_BACK_SPACE)
        {
            Marker selectedMarker = routePainter.getSelectedMarker();
            if (selectedMarker != null)
            {
                Ship selectedShip =
                        routePainter.getSelectedShip();
                selectedShip.removeMarker(selectedMarker);
                if (selectedShip.getMarkers().isEmpty())
                {
                    ships.remove(selectedShip);
                }
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {}
    
    @Override
    public void keyTyped(KeyEvent e) {}
}
