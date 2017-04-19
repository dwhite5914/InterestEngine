package com.nuwc.interestengine.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

public class MapPanel extends JPanel
{
    private JXMapKit miniMap;
    int xStart, yStart, xEnd, yEnd;
    int width, height;
    boolean drawing = false;
    Color fillColor = new Color(1f, 1f, 1f, 0.5f);
    Color borderColor = Color.RED;
    Region region = null;
    Region oldRegion = null;

    JSpinner minLatSpinner, minLonSpinner, maxLatSpinner, maxLonSpinner;
    OptionsPanel optionsPanel;

    public MapPanel()
    {
        super();
        setLayout(new BorderLayout());
    }

    public void setRegion(GeoPosition start, GeoPosition end)
    {
        region = new Region(start, end, fillColor, borderColor);
        miniMap.getMainMap().setCenterPosition(region.getCenter());
        miniMap.setZoom(14);
        repaint();
    }

    public void setMap(JXMapKit map, JSpinner minLatSpinner,
            JSpinner minLonSpinner, JSpinner maxLatSpinner,
            JSpinner maxLonSpinner, OptionsPanel optionsPanel)
    {
        this.minLatSpinner = minLatSpinner;
        this.minLonSpinner = minLonSpinner;
        this.maxLatSpinner = maxLatSpinner;
        this.maxLonSpinner = maxLonSpinner;
        this.optionsPanel = optionsPanel;

        map.setDefaultProvider(JXMapKit.DefaultProviders.OpenStreetMaps);
        map.getMainMap().setOverlayPainter(new Painter<JXMapViewer>()
        {
            @Override
            public void paint(Graphics2D g, JXMapViewer map, int i, int i1)
            {
                if (region != null)
                {
                    region.draw(g, map);
                }

                if (drawing)
                {
                    g.setColor(borderColor);
                    g.setStroke(new BasicStroke(3));
                    g.drawRect(xStart, yStart, width, height);
                }
            }
        });
        MouseAdapter mapMouseListener = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    drawing = true;
                    xStart = e.getX();
                    yStart = e.getY();
                    width = 0;
                    height = 0;
                    GeoPosition geo = map.getMainMap()
                            .convertPointToGeoPosition(new Point(xStart, yStart));
                    optionsPanel.setModifyingMiniMap(true);
                    maxLatSpinner.setValue(geo.getLatitude());
                    minLonSpinner.setValue(geo.getLongitude());
                    maxLatSpinner.repaint();
                    minLonSpinner.repaint();
                    optionsPanel.setModifyingMiniMap(false);
                    oldRegion = region;
                    region = null;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    xEnd = e.getX();
                    yEnd = e.getY();
                    width = Math.abs(xEnd - xStart);
                    height = Math.abs(yEnd - yStart);
                    repaint();
                    GeoPosition geo = map.getMainMap()
                            .convertPointToGeoPosition(new Point(xEnd, yEnd));
                    optionsPanel.setModifyingMiniMap(true);
                    minLatSpinner.setValue(geo.getLatitude());
                    maxLonSpinner.setValue(geo.getLongitude());
                    minLatSpinner.repaint();
                    maxLonSpinner.repaint();
                    optionsPanel.setModifyingMiniMap(false);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    drawing = false;
                    xEnd = e.getX();
                    yEnd = e.getY();
                    width = Math.abs(xEnd - xStart);
                    height = Math.abs(yEnd - yStart);
                    if (width != 0 && height != 0)
                    {
                        Point2D start = new Point(xStart, yStart);
                        Point2D end = new Point(xEnd, yEnd);
                        GeoPosition startPoint = map.getMainMap().convertPointToGeoPosition(start);
                        GeoPosition endPoint = map.getMainMap().convertPointToGeoPosition(end);
                        region = new Region(startPoint, endPoint,
                                fillColor, borderColor);
                        GeoPosition geo = map.getMainMap()
                                .convertPointToGeoPosition(new Point(xEnd, yEnd));
                        optionsPanel.setModifyingMiniMap(true);
                        minLatSpinner.setValue(geo.getLatitude());
                        maxLonSpinner.setValue(geo.getLongitude());
                        minLatSpinner.repaint();
                        maxLonSpinner.repaint();
                        optionsPanel.setModifyingMiniMap(false);
                        oldRegion = null;
                    }
                    else if (oldRegion != null)
                    {
                        region = oldRegion;
                    }
                    repaint();
                }
            }
        };
        map.getMainMap().addMouseListener(mapMouseListener);
        map.getMainMap().addMouseMotionListener(mapMouseListener);
        map.setCenterPosition(new GeoPosition(0, 0));
        map.setMiniMapVisible(false);
        map.setZoomSliderVisible(false);
        map.setZoomButtonsVisible(false);

        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(8);
        map.getMainMap().setTileFactory(tileFactory);
        map.setZoom(info.getMaximumZoomLevel());

        add(map, BorderLayout.CENTER);

        miniMap = map;
    }
}
