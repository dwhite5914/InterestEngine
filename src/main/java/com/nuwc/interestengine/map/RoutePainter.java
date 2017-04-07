package com.nuwc.interestengine.map;

import com.nuwc.interestengine.clustering.Cluster;
import com.nuwc.interestengine.clustering.RouteObject;
import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.data.AISPoint;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
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
    private final List<TriMarker> markers;
    private final JXMapKit map;
    private Ship selectedShip;
    private Marker selectedMarker;

    private Vessel selectedVessel = null;

    private List<Vessel> vessels = new ArrayList<>();
    private List<Cluster> clusters = new ArrayList<>();
    private List<RouteObject> routes = new ArrayList<>();
    private List<AISPoint> dataPoints = new ArrayList<>();
    private List<AISPoint> entryPoints = new ArrayList<>();
    private List<AISPoint> exitPoints = new ArrayList<>();
    private List<AISPoint> stopPoints = new ArrayList<>();

    private Color clusterColor = Color.RED;
    private Color routeColor = Color.BLUE;
    private Color dataPointColor = Color.BLACK;
    private Color entryPointColor = Color.CYAN;
    private Color exitPointColor = Color.MAGENTA;
    private Color stopPointColor = Color.GREEN;

    private boolean drawClusters = false;
    private boolean drawRoutes = true;
    private boolean drawDataPoints = false;
    private boolean drawEntryPoints = false;
    private boolean drawExitPoints = false;
    private boolean drawStopPoints = false;

    String clustersMode = "Single Color";
    String routesMode = "Rainbow";

    public RoutePainter(List<Ship> ships, List<TriMarker> markers, JXMapKit map)
    {
        this.ships = ships;
        this.markers = markers;
        this.map = map;
    }

    public void replaceVessels(Collection<Vessel> newVessels)
    {
        vessels.clear();
        for (Vessel vessel : newVessels)
        {
            vessels.add(vessel);
        }

        map.repaint();
    }

    public List<Vessel> getVessels()
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

    public void setDrawClusters(boolean drawClusters)
    {
        this.drawClusters = drawClusters;
        map.repaint();
    }

    public void setDrawRoutes(boolean drawRoutes)
    {
        this.drawRoutes = drawRoutes;
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

        if (drawDataPoints)
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

        if (drawEntryPoints)
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

        if (drawExitPoints)
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

        if (drawStopPoints)
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

        if (drawRoutes)
        {
            switch (routesMode)
            {
                case "Single Color":
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
                    break;
                case "Rainbow":
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
                    break;
            }
        }

        if (drawClusters)
        {
            switch (clustersMode)
            {
                case "Single Color":
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
                    break;
                case "Rainbow":
                    c = 0;
                    for (Cluster cluster : clusters)
                    {
                        if (cluster.getIsActive())
                        {
                            Point2D centroid = cluster.getCentroid(map);
                            paintCluster(g, c, centroid, cluster.color);
                            c++;
                        }
                    }
                    break;
                case "With Points":
                    c = 0;
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
                    break;
                case "Connected":
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
                    break;
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

        for (Vessel vessel : vessels)
        {
            if (vessel.track.size() > 0)
            {
                if (vessel == selectedVessel)
                {
                    continue;
                }
                vessel.draw(g, map, false);
            }
        }

        if (selectedVessel != null)
        {
            selectedVessel.draw(g, map, true);
        }
    }

    public Vessel getSelectedVessel()
    {
        return selectedVessel;
    }

    public void setSelectedVessel(Vessel vessel)
    {
        selectedVessel = vessel;
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
