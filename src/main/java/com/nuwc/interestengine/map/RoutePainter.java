package com.nuwc.interestengine.map;

import com.nuwc.interestengine.Utils;
import com.nuwc.interestengine.clustering.Cluster;
import com.nuwc.interestengine.clustering.RouteObject;
import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.data.AISPoint;
import com.nuwc.interestengine.data.Cell;
import com.nuwc.interestengine.gui.MainFrame;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.ImageIcon;
import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

public class RoutePainter implements Painter<JXMapViewer>
{
    private final Color LINE_COLOR = Color.BLACK;
    private final Color MARKER_COLOR = Color.BLUE;
    private final Color SHIP_COLOR = Color.RED;
    private final int MARKER_RADIUS = 4;

    private final List<Ship> ships;
    private final JXMapKit map;
    private Ship selectedShip;
    private Marker selectedMarker;
    private final MainFrame mainFrame;

    private Vessel focusedVessel = null;
    private Vessel selectedVessel = null;
    private boolean drawAnomalousTrack = false;

    private final ConcurrentLinkedQueue<Vessel> vessels
            = new ConcurrentLinkedQueue<>();
    private List<Cluster> clusters = new ArrayList<>();
    private List<RouteObject> routes = new ArrayList<>();
    private List<AISPoint> dataPoints = new ArrayList<>();
    private List<AISPoint> entryPoints = new ArrayList<>();
    private List<AISPoint> exitPoints = new ArrayList<>();
    private List<AISPoint> stopPoints = new ArrayList<>();
    private List<Cell> cells = new ArrayList<>();

    private Color clusterColor = Color.RED;
    private Color routeColor = Color.BLUE;
    private Color dataPointColor = Color.BLACK;
    private Color entryPointColor = Color.CYAN;
    private Color exitPointColor = Color.MAGENTA;
    private Color stopPointColor = Color.GREEN;

    private String clustersMode = "Single Color";
    private String routesMode = "Rainbow";
    private boolean drawDataPoints = false;
    private boolean drawEntryPoints = false;
    private boolean drawExitPoints = false;
    private boolean drawStopPoints = false;
    private List<String> allowedShipTypes
            = Arrays.asList(Utils.getShipTypes());

    private RouteObject bestRoute = null;

    public RoutePainter(List<Ship> ships, List<TriMarker> markers,
            JXMapKit map, MainFrame mainFrame)
    {
        this.ships = ships;
        this.map = map;
        this.mainFrame = mainFrame;
    }

    public void replaceVessels(Collection<Vessel> newVessels)
    {
        vessels.clear();
        vessels.addAll(newVessels);

        map.repaint();
        mainFrame.updateData();
    }

    public ConcurrentLinkedQueue<Vessel> getVessels()
    {
        return vessels;
    }

    public void setClusterColor(Color color)
    {
        clusterColor = color;
        map.repaint();
    }

    public void setRouteColor(Color routeColor)
    {
        this.routeColor = routeColor;
        map.repaint();
    }

    public void setDataPointColor(Color dataPointColor)
    {
        this.dataPointColor = dataPointColor;
        map.repaint();
    }

    public void setEntryPointColor(Color entryPointColor)
    {
        this.entryPointColor = entryPointColor;
        map.repaint();
    }

    public void setExitPointColor(Color exitPointColor)
    {
        this.exitPointColor = exitPointColor;
        map.repaint();
    }

    public void setStopPointColor(Color stopPointColor)
    {
        this.stopPointColor = stopPointColor;
        map.repaint();
    }

    public void setClusters(List<Cluster> clusters)
    {
        this.clusters = clusters;
    }

    public void setRoutes(List<RouteObject> routes)
    {
        this.routes = routes;
    }

    public void setDataPoints(List<AISPoint> dataPoints)
    {
        this.dataPoints = dataPoints;
        map.repaint();
    }

    public void setEntryPoints(List<AISPoint> entryPoints)
    {
        this.entryPoints = entryPoints;
        map.repaint();
    }

    public void setExitPoints(List<AISPoint> exitPoints)
    {
        this.exitPoints = exitPoints;
        map.repaint();
    }

    public void setStopPoints(List<AISPoint> stopPoints)
    {
        this.stopPoints = stopPoints;
        map.repaint();
    }

    public void setDrawDataPoints(boolean drawDataPoints)
    {
        this.drawDataPoints = drawDataPoints;
        map.repaint();
    }

    public void setDrawEntryPoints(boolean drawEntryPoints)
    {
        this.drawEntryPoints = drawEntryPoints;
        map.repaint();
    }

    public void setDrawExitPoints(boolean drawExitPoints)
    {
        this.drawExitPoints = drawExitPoints;
        map.repaint();
    }

    public void setDrawStopPoints(boolean drawStopPoints)
    {
        this.drawStopPoints = drawStopPoints;
        map.repaint();
    }

    public void setClustersMode(String clustersMode)
    {
        this.clustersMode = clustersMode;
        map.repaint();
    }

    public void setRoutesMode(String routesMode)
    {
        this.routesMode = routesMode;
        map.repaint();
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

    public RouteObject getBestRoute()
    {
        return bestRoute;
    }

    public void setBestRoute(RouteObject bestRoute)
    {
        this.bestRoute = bestRoute;
        map.repaint();
    }

    public List<String> getAllowedShipTypes()
    {
        return allowedShipTypes;
    }

    public void setAllowedShipTypes(List<String> allowedShipTypes)
    {
        for (String type : allowedShipTypes)
        {
            System.out.println(type);
        }
        this.allowedShipTypes = allowedShipTypes;
        map.repaint();
        for (String type : allowedShipTypes)
        {
            System.out.println(type);
        }
    }

    public void drawDataPoints(Graphics2D g, JXMapViewer map)
    {
        for (AISPoint point : dataPoints)
        {
            GeoPosition position = new GeoPosition(point.lat, point.lon);
            Point2D mapPoint = map.convertGeoPositionToPoint(position);
            if (g.getClip().contains(mapPoint))
            {
                paintMarker(g, mapPoint, dataPointColor);
            }
        }
    }

    public void drawEntryPoints(Graphics2D g, JXMapViewer map)
    {
        for (AISPoint point : entryPoints)
        {
            GeoPosition position = new GeoPosition(point.lat, point.lon);
            Point2D mapPoint = map.convertGeoPositionToPoint(position);
            if (g.getClip().contains(mapPoint))
            {
                paintMarker(g, mapPoint, entryPointColor);
            }
        }
    }

    public void drawExitPoints(Graphics2D g, JXMapViewer map)
    {
        for (AISPoint point : exitPoints)
        {
            GeoPosition position = new GeoPosition(point.lat, point.lon);
            Point2D mapPoint = map.convertGeoPositionToPoint(position);
            if (g.getClip().contains(mapPoint))
            {
                paintMarker(g, mapPoint, exitPointColor);
            }
        }
    }

    public void drawStopPoints(Graphics2D g, JXMapViewer map)
    {
        for (AISPoint point : stopPoints)
        {
            GeoPosition position = new GeoPosition(point.lat, point.lon);
            Point2D mapPoint = map.convertGeoPositionToPoint(position);
            if (g.getClip().contains(mapPoint))
            {
                paintMarker(g, mapPoint, stopPointColor);
            }
        }
    }

    public void drawSingleColorRoutes(Graphics2D g, JXMapViewer map)
    {
        for (RouteObject route : routes)
        {
            for (AISPoint point : route.points)
            {
                GeoPosition position = new GeoPosition(point.lat, point.lon);
                Point2D mapPoint = map.convertGeoPositionToPoint(position);
                if (g.getClip().contains(mapPoint))
                {
                    paintMarker(g, mapPoint, routeColor);
                }
            }
        }
    }

    public void drawRainbowColoredRoutes(Graphics2D g, JXMapViewer map)
    {
        for (RouteObject route : routes)
        {
            for (AISPoint point : route.points)
            {
                GeoPosition position = new GeoPosition(point.lat, point.lon);
                Point2D mapPoint = map.convertGeoPositionToPoint(position);
                if (g.getClip().contains(mapPoint))
                {
                    paintMarker(g, mapPoint, route.color);
                }
            }
        }
    }

    public void drawSingleColorClusters(Graphics2D g, JXMapViewer map)
    {
        int c = 0;
        for (Cluster cluster : clusters)
        {
            if (cluster.getIsActive())
            {
                Point2D centroid = cluster.getCentroid(map);
                paintCluster(g, c, centroid, clusterColor);
                c++;
            }
        }
    }

    public void drawRainbowColoredClusters(Graphics2D g, JXMapViewer map)
    {
        int c = 0;
        for (Cluster cluster : clusters)
        {
            if (cluster.getIsActive())
            {
                Point2D centroid = cluster.getCentroid(map);
                paintCluster(g, c, centroid, cluster.color);
                c++;
            }
        }
    }

    public void drawClustersWithPoints(Graphics2D g, JXMapViewer map)
    {
        int c = 0;
        for (Cluster cluster : clusters)
        {
            if (cluster.getIsActive())
            {
                for (AISPoint point : cluster.points)
                {
                    GeoPosition position = new GeoPosition(point.lat, point.lon);
                    Point2D mapPoint = map.convertGeoPositionToPoint(position);
                    if (g.getClip().contains(mapPoint))
                    {
                        paintMarker(g, mapPoint, cluster.color);
                    }
                }
                Point2D centroid = cluster.getCentroid(map);
                paintCluster(g, c, centroid, cluster.color);
                c++;
            }
        }
    }

    public void drawConnectedClusters(Graphics2D g, JXMapViewer map)
    {
        int rid = 0;
        for (RouteObject route : routes)
        {
            for (int j = 0; j < route.waypoints.size(); j++)
            {
                Cluster cluster = route.waypoints.get(j);
                Point2D centroid = cluster.getCentroid(map);
                paintCluster(g, rid, centroid, route.color);
                if (j + 1 < route.waypoints.size())
                {
                    Cluster cluster2 = route.waypoints.get(j + 1);
                    Point2D centroid2 = cluster2.getCentroid(map);
                    Color oldColor = g.getColor();
                    g.setColor(route.color);
                    g.drawLine((int) centroid.getX(),
                            (int) centroid.getY(),
                            (int) centroid2.getX(),
                            (int) centroid2.getY());
                    g.setColor(oldColor);

                }
            }
            rid++;
        }
    }

    private void drawFakeShips(Graphics2D g, JXMapViewer map)
    {
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

    private void drawSimulatedVessels(Graphics2D g, JXMapViewer map)
    {
        for (Vessel vessel : vessels)
        {
            String shipType = vessel.shipType;
            if (shipType == null)
            {
                shipType = "NotAvailable";
            }
            if (vessel.track.size() > 0
                    && vessel != focusedVessel
                    && vessel != selectedVessel
                    && Utils.isAnchored(vessel.navStatus)
                    && allowedShipTypes.contains(shipType))
            {
                vessel.draw(g, map, false, false);
            }
        }

        for (Vessel vessel : vessels)
        {
            String shipType = vessel.shipType;
            if (shipType == null)
            {
                shipType = "Unknown";
            }
            if (vessel.track.size() > 0
                    && vessel != focusedVessel
                    && vessel != selectedVessel
                    && !Utils.isAnchored(vessel.navStatus)
                    && allowedShipTypes.contains(shipType))
            {
                vessel.draw(g, map, false, false);
            }
        }

        if (selectedVessel != null)
        {
            String shipType = selectedVessel.shipType;
            if (shipType == null)
            {
                shipType = "Unknown";
            }
            if (!allowedShipTypes.contains(shipType))
            {
                selectedVessel = null;
            }
        }

        if (focusedVessel != null)
        {
            String shipType = focusedVessel.shipType;
            if (shipType == null)
            {
                shipType = "Unknown";
            }
            if (!allowedShipTypes.contains(shipType))
            {
                focusedVessel = null;
            }
        }

        if (selectedVessel != null)
        {
            if (drawAnomalousTrack)
            {
                for (AISPoint point : selectedVessel.track)
                {
                    GeoPosition position = new GeoPosition(point.lat, point.lon);
                    Point2D mapPoint = map.convertGeoPositionToPoint(position);
                    Color color = point.anomalous ? Color.RED : Color.BLUE;
                    paintMarker(g, mapPoint, color);
                }
            }
            selectedVessel.draw(g, map, false, true);
        }

        if (focusedVessel != null)
        {
            focusedVessel.draw(g, map, true, false);
        }
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int i, int i1)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (drawDataPoints)
        {
            drawDataPoints(g, map);
        }

        if (drawEntryPoints)
        {
            drawEntryPoints(g, map);
        }

        if (drawExitPoints)
        {
            drawExitPoints(g, map);
        }

        if (drawStopPoints)
        {
            drawStopPoints(g, map);
        }

        switch (routesMode)
        {
            case "Single Color":
                drawSingleColorRoutes(g, map);
                break;
            case "Rainbow":
                drawRainbowColoredRoutes(g, map);
                break;
            default:
                break;
        }

        switch (clustersMode)
        {
            case "Single Color":
                drawSingleColorClusters(g, map);
                break;
            case "Rainbow":
                drawRainbowColoredClusters(g, map);
                break;
            case "With Points":
                drawClustersWithPoints(g, map);
                break;
            case "Connected":
                drawConnectedClusters(g, map);
                break;
            default:
                break;
        }

        if (bestRoute != null)
        {
            for (AISPoint point : bestRoute.points)
            {
                GeoPosition position = new GeoPosition(point.lat, point.lon);
                Point2D mapPoint = map.convertGeoPositionToPoint(position);
                if (g.getClip().contains(mapPoint))
                {
                    paintMarker(g, mapPoint, bestRoute.color);
                }
            }
        }

        drawFakeShips(g, map);

        drawSimulatedVessels(g, map);

        if (cells != null)
        {
            for (Cell cell : cells)
            {
                cell.draw(g, map);
            }
        }
    }

    public void setCells(List<Cell> cells)
    {
        this.cells = cells;
        map.repaint();
    }

    public Vessel getFocusedVessel()
    {
        return focusedVessel;
    }

    public void setFocusedVessel(Vessel focusedVessel)
    {
        this.focusedVessel = focusedVessel;
    }

    public Vessel getSelectedVessel()
    {
        return selectedVessel;
    }

    public void setSelectedVessel(Vessel selectedVessel)
    {
        this.selectedVessel = selectedVessel;
    }

    public boolean isDrawAnomalousTrack()
    {
        return drawAnomalousTrack;
    }

    public void setDrawAnomalousTrack(boolean drawAnomalousTrack)
    {
        this.drawAnomalousTrack = drawAnomalousTrack;
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

    private void paintCluster(Graphics2D g, int id, Point2D point, Color color)
    {
        int x = (int) (point.getX() - MARKER_RADIUS);
        int y = (int) (point.getY() - MARKER_RADIUS);
        int diameter = MARKER_RADIUS * 2 * 2;
        int radius = diameter / 2;
        g.setPaint(color);
        g.drawOval(x - radius, y - radius, diameter, diameter);
        Font oldFont = g.getFont();
        g.setFont(new Font("Calibri", Font.PLAIN, 20));
        g.drawString("" + id, x - radius, y - radius);
        g.setFont(oldFont);
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
