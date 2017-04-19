package com.nuwc.interestengine.gui;

import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.map.RoutePainter;
import com.nuwc.interestengine.map.Marker;
import com.nuwc.interestengine.map.RouteChangeListener;
import com.nuwc.interestengine.map.Ship;
import com.nuwc.interestengine.map.TriMarker;
import com.nuwc.interestengine.simulation.Simulation;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.LocalResponseCache;
import org.jxmapviewer.viewer.TileFactoryInfo;

public class MainFrame extends JFrame implements KeyListener
{
    // Attributes
    private static final int MIN_WIDTH = 1200;
    private static final int MIN_HEIGHT = 800;
    private JXMapKit mapKit;
    private JPanel mapPanel;
    private JXMapKit miniMap;
    private SelectionPanel selectionPanel;
    private OptionsPanel optionsPanel;
    private List<Ship> ships;
    private RoutePainter routePainter;
    private Simulation simulation;
    private List<TriMarker> markers;

    public MainFrame()
    {
        // Calls parent class constructor
        super();

        initSettings();
        initKeyListener();
        initComponents();
    }

    private void initSettings()
    {
        // Sets window title, dimensions, and close operation
        setTitle("Contact of Interest Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        //setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
    }

    private void initComponents()
    {
        /* Initialize options panel with ship list and initial
         * simulation state of stopped.
         */
        mapPanel = getMap();
        miniMap = new JXMapKit();
        optionsPanel = new OptionsPanel(getShips(), getSimulation(),
                getMarkers(), getRoutePainter(getMap()), miniMap, this);
        selectionPanel = new SelectionPanel(getRoutePainter(getMap()), this,
                optionsPanel);
        optionsPanel.setSelectionPanel(selectionPanel);

        setLayout(new BorderLayout());
        JSplitPane sidePanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, selectionPanel, optionsPanel);
        sidePanel.setDividerLocation(0.4);
        sidePanel.setResizeWeight(0.4);
        sidePanel.setContinuousLayout(true);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapPanel, sidePanel);
        splitPane.setDividerLocation(0.8);
        splitPane.setResizeWeight(0.9);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);
    }

    private void initKeyListener()
    {
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
    }

    private JXMapKit getMap()
    {
        // If no instance of JXMapKit exists, create one
        if (mapKit == null)
        {
            mapKit = new JXMapKit();
            mapKit.setDefaultProvider(JXMapKit.DefaultProviders.OpenStreetMaps);
            mapKit.getMainMap().setOverlayPainter(getRoutePainter(mapKit));
            MapMouseListener mapMouseListener = new MapMouseListener(mapKit);
            mapKit.getMainMap().addMouseListener(mapMouseListener);
            mapKit.getMainMap().addMouseMotionListener(mapMouseListener);
            mapKit.setCenterPosition(new GeoPosition(44, 15));
            mapKit.setZoom(12);

            TileFactoryInfo info = new OSMTileFactoryInfo();
            DefaultTileFactory tileFactory = new DefaultTileFactory(info);
            tileFactory.setThreadPoolSize(8);
            mapKit.getMainMap().setTileFactory(tileFactory);

            // Setup local file cache
            String baseURL = info.getBaseURL();
            File cacheDir = new File(System.getProperty("user.home")
                    + File.separator + ".jxmapviewer2");
            LocalResponseCache.installResponseCache(baseURL, cacheDir, false);
        }

        return mapKit;
    }

    private RoutePainter getRoutePainter(JXMapKit map)
    {
        // If no instance of RoutePainter exists, create one
        if (routePainter == null)
        {
            routePainter = new RoutePainter(getShips(), getMarkers(), map, this);
        }

        return routePainter;
    }

    private List<Ship> getShips()
    {
        // If no ship list exists, create one
        if (ships == null)
        {
            ships = new ArrayList<>();
        }

        return ships;
    }

    private List<TriMarker> getMarkers()
    {
        if (markers == null)
        {
            markers = new ArrayList<>();
        }

        return markers;
    }

    private Simulation getSimulation()
    {
        // If no Simulation exists, create one
        if (simulation == null)
        {
            simulation = new Simulation(getShips());
        }

        return simulation;
    }

    public void updateMap()
    {
        getMap().repaint();
    }

    public void updateData()
    {
        optionsPanel.updateChart(routePainter);
        selectionPanel.updateInfo();
    }

    private class RouteListener implements RouteChangeListener
    {
        @Override
        public void routeChanged()
        {
            getMap().repaint();
        }

        @Override
        public void routeEnded()
        {
            getSimulation().decShips();
        }
    }

    private class MapMouseListener extends MouseAdapter
    {
        private final double SELECT_THRESH = 10;

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
//                Ship selectedShip
//                        = routePainter.getSelectedShip();
//                if (e.isShiftDown() && selectedShip != null)
//                {
//                    selectedShip.addMarker(new Marker(mapPoint));
//                }
//                else
//                {
//                    Ship newShip = new Ship();
//                    newShip.addRouteChangeListener(new RouteListener());
//                    Marker newMarker = new Marker(mapPoint);
//                    newShip.addMarker(newMarker);
//                    ships.add(newShip);
//                    routePainter.setSelected(newShip, newMarker);
//                }
//                optionsPanel.interruptSimulation();
                boolean madeSelection = false;
                for (Vessel vessel : routePainter.getVessels())
                {
                    if (vessel.contains(e.getPoint()))
                    {
                        madeSelection = true;
                        routePainter.setSelectedVessel(vessel);
                        selectionPanel.setSelectedVessel(vessel);
                        updateMap();
                        break;
                    }
                }

                if (!madeSelection && routePainter.getSelectedVessel() != null)
                {
                    routePainter.setSelectedVessel(null);
                    selectionPanel.setSelectedVessel(null);
                    updateMap();
                }
            }
            else if (SwingUtilities.isRightMouseButton(e))
            {
//                FIND:
//                for (Ship ship : ships)
//                {
//                    for (Marker marker : ship.getMarkers())
//                    {
//                        Point2D point = map.getMainMap()
//                                .convertGeoPositionToPoint(
//                                        marker.getPosition());
//                        double distance = Point.distance(point.getX(),
//                                point.getY(),
//                                clickPoint.getX(),
//                                clickPoint.getY());
//                        if (distance <= SELECT_THRESH)
//                        {
//                            routePainter.setSelected(ship, marker);
//                            break FIND;
//                        }
//                    }
//                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            last = e.getPoint();
            if (SwingUtilities.isRightMouseButton(e))
            {
                Point2D clickPoint = new Point(e.getX(), e.getY());
                FIND:
                for (Ship ship : ships)
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
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            dragging = false;
            last = null;
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            int dx = e.getX() - last.x;
            int dy = e.getY() - last.y;
            if (dragging && SwingUtilities.isRightMouseButton(e))
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
                optionsPanel.interruptSimulation();
            }
            last = e.getPoint();
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            if (!getMap().hasFocus())
            {
                MainFrame.this.requestFocus();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            Vessel focusedVessel = routePainter.getFocusedVessel();
            if (focusedVessel == null)
            {
                for (Vessel vessel : routePainter.getVessels())
                {
                    if (vessel.contains(e.getPoint()))
                    {
                        routePainter.setFocusedVessel(vessel);
                        updateMap();
                        break;
                    }
                }
            }
            else
            {
                if (!focusedVessel.contains(e.getPoint()))
                {
                    routePainter.setFocusedVessel(null);
                    updateMap();
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_DELETE || key == KeyEvent.VK_BACK_SPACE)
        {
            Marker selectedMarker = routePainter.getSelectedMarker();
            if (selectedMarker != null)
            {
                Ship selectedShip
                        = routePainter.getSelectedShip();
                selectedShip.removeMarker(selectedMarker);
                if (selectedShip.getMarkers().isEmpty())
                {
                    ships.remove(selectedShip);
                }
                optionsPanel.interruptSimulation();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }
}
