/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine.map;

import com.nuwc.interestengine.Utils;
import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;

/**
 *
 * @author Dan
 */
public class Route
{
    //Attributes
    private static int REFRESH_TIME = 1000;
    private List<Marker> markers;
    private double speed = 0.02;
    private RouteState state;
    private Thread simulator;
    private GeoPosition currentPosition;
    private Ship ship;
    
    public Route(Ship ship, List<Marker> markers)
    {
        //Assigns designated ship to as this shit with it's selected markers
        this.ship = ship;
        this.markers = markers;
        start();
    }
    
    private void start()
    {
        if (simulator == null)
        {
            //Creates new simulator is none exists
            simulator = new Thread(new Simulator());
        }
        //Starts simluator and changes the state
        simulator.start();
        state = RouteState.RUNNING;
    }
    
    public void stop()
    {
        //Changes state
        state = RouteState.STOPPED;
    }
    
    public void pause()
    {
        //Changes state and handles the event
        state = RouteState.PAUSED;
        ship.fireRouteStateChange();
    }
    
    public void unpause()
    {
        //Changes state and handles the event
        state = RouteState.RUNNING;
        ship.fireRouteStateChange();
    }
    
    public RouteState getState()
    {
        //Returns route state
        return state;
    }
    
    public double getSpeed()
    {
        //returns speed of ship
        return speed;
    }
    
    public void setSpeed(double speed)
    {
        //Sets new speed of ship
        this.speed = speed;
    }
    
    public GeoPosition getCurrentPosition()
    {
        //Returns Object type Geoposition containing Lat and Long
        return currentPosition;
    }
    
    public void firePositionUpdate(GeoPosition position)
    {
        //Updates ship position with a new GeoPosition position
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
        
        public Simulator() {}
        
        @Override
        public void run()
        {
            //Checks if Start and End exists
            if (markers.size() < 2)
            {
                return;
            }
            fireRouteStarted();
            for (int edge = 0; edge < markers.size() - 1; edge++)
            {
                Marker start = markers.get(edge);
                Marker end = markers.get(edge + 1);
                currentPosition = start.getPosition();
                RUN: while (state != RouteState.STOPPED &&
                        Utils.distance(currentPosition, end) > THRESH)
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
