/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine;
/*
import java.awt.BorderLayout;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.event.MouseInputListener;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.LocalResponseCache;
import org.jxmapviewer.viewer.TileFactoryInfo;
/*
/**
 *
 * @author Dan
 */
public class InterestEngine
{
    public static void main(String args[])
    {
        /*
        JXMapViewer mapViewer = new JXMapViewer();
        
        // Create a TileFactoryInfo for OpenStreetMap
        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(8);
        mapViewer.setTileFactory(tileFactory);

        // Setup local file cache
        String baseURL = info.getBaseURL();
        File cacheDir = new File(System.getProperty("user.home")
                + File.separator + ".jxmapviewer2");
        LocalResponseCache.installResponseCache(baseURL, cacheDir, false);

        // Add interactions
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        Painter painter = new CompoundPainter<>();
        mapViewer.setOverlayPainter(painter);

        GeoPosition center = new GeoPosition(0, 0);

        // Set the focus
        mapViewer.setZoom(info.getMinimumZoomLevel());
        mapViewer.setAddressLocation(center);

        JFrame frame = new JFrame("JXMapviewer2 Example 1");
        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(mapViewer);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //frame.setVisible(true);
        */
        
        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);
    }
}
