/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine.gui;

import java.io.File;
import javax.swing.JPanel;
import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.LocalResponseCache;
import org.jxmapviewer.viewer.TileFactoryInfo;

/**
 *
 * @author User
 */
public class MainMapPanel extends JPanel
{
    JXMapKit mapKit = null;
    
    public MainMapPanel()
    {
        super();
        add(getMap());
    }

    private JXMapKit getMap()
    {
        // If no instance of JXMapKit exists, create one
        if (mapKit == null)
        {
            mapKit = new JXMapKit();
            mapKit.setDefaultProvider(JXMapKit.DefaultProviders.OpenStreetMaps);
            //mapKit.getMainMap().setOverlayPainter(getRoutePainter(mapKit));
           // MainFrame.MapMouseListener mapMouseListener = new MainFrame.MapMouseListener(mapKit);
            //mapKit.getMainMap().addMouseListener(mapMouseListener);
            //mapKit.getMainMap().addMouseMotionListener(mapMouseListener);
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
}
