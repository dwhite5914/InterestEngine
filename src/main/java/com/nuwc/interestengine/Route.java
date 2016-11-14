/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine;

import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;

/**
 *
 * @author Dan
 */
public class Route
{
    private static int REFRESH_TIME = 1000;
    private List<Marker> markers;
    private double speed = 0.00001;
    private RouteState state;
    private Thread simulator;
    private GeoPosition currentPosition;
    private Ship ship;
    
    public Route(Ship ship, List<Marker> markers)
    {
        this.markers = markers;
        state = RouteState.STOPPED;
    }
    
    public void start()
    {
        if (state == RouteState.STOPPED)
        {
            simulator.start();
            state = RouteState.RUNNING;
            ship.fireRouteStateChange();
        }
    }
    
    public void stop()
    {
        state = RouteState.STOPPED;
        ship.fireRouteStateChange();
    }
    
    public void pause()
    {
        state = RouteState.PAUSED;
        ship.fireRouteStateChange();
    }
    
    public void unpause()
    {
        state = RouteState.RUNNING;
        ship.fireRouteStateChange();
    }
    
    public RouteState getState()
    {
        return state;
    }
    
    public double getSpeed()
    {
        return speed;
    }
    
    public void setSpeed(double speed)
    {
        this.speed = speed;
    }
    
    public GeoPosition getCurrentPosition()
    {
        return currentPosition;
    }
    
    public void firePositionUpdate(GeoPosition position)
    {
        ship.firePositionUpdate(position);
    }

    public void fireRouteStarted()
    {
        ship.fireRouteStarted(this);
    }

    public void fireRouteEnded()
    {
        ship.fireRouteEnded();
    }
    
    private class Simulator implements Runnable
    {
        private final double THRESH = 2 * speed;
        
        @Override
        public void run()
        {
            fireRouteStarted();
            if (markers.size() < 2)
            {
                return;
            }
            
            for (int edge = 0; edge < markers.size(); edge++)
            {
                Marker start = markers.get(edge);
                Marker end = markers.get(edge + 1);
                currentPosition = start.getPosition();
                RUN: while (state != RouteState.STOPPED &&
                        Utils.distance(start, currentPosition) > THRESH)
                {
                    firePositionUpdate(currentPosition);
                    updatePosition(start, end);
                    try
                    {
                        do
                        {
                            Thread.sleep(REFRESH_TIME);
                        }
                        while (state == RouteState.PAUSED);
                    }
                    catch (InterruptedException e)
                    {
                        // Pass
                    }
                }
            }
            
            fireRouteEnded();
        }
        
        private void updatePosition(Marker start, Marker end)
        {
            double latDiff = end.getLatitude() - start.getLatitude();
            double lonDiff = end.getLongitude() - start.getLongitude();
            double length = Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
            double latDelta = latDiff / length * speed;
            double lonDelta = lonDiff / length * speed;
            double latNew = currentPosition.getLatitude() + latDelta;
            double lonNew = currentPosition.getLongitude() + lonDelta;
            currentPosition = new GeoPosition(latNew, lonNew);
        }
    }
}
