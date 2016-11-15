/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine;

import java.util.ArrayList;
import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;

/**
 *
 * @author Dan
 */
public class Ship
{
    private List<Marker> markers;
    private List<RouteChangeListener> routeChangeListeners;
    private GeoPosition currentPosition;
    private Route route;
    private double speed = 0.00001;
    
    public Ship()
    {
        markers = new ArrayList<>();
        routeChangeListeners = new ArrayList<>();
    }
    
    public void addMarker(Marker marker)
    {
        markers.add(marker);
        fire(RouteEvent.CHANGE);
    }
    
    public void removeMarker(Marker marker)
    {
        if (markers.remove(marker))
        {
            fire(RouteEvent.CHANGE);
        }
    }
    
    public void clearMarkers()
    {
        markers.clear();
        fire(RouteEvent.CHANGE);
    }
    
    public List<Marker> getMarkers()
    {
        return markers;
    }
    
    public boolean isStartMarker(Marker marker)
    {
        return (!markers.isEmpty() &&
                    markers.get(0).equals(marker));
    }
    
    public boolean isEndMarker(Marker marker)
    {
        return (!markers.isEmpty() &&
                    markers.get(markers.size() - 1).equals(marker));
    }
    
    public boolean isMarker(Marker marker)
    {
        return !markers.isEmpty();
    }
    
    public GeoPosition getCurrentPosition()
    {
        return currentPosition;
    }
    
    public double getSpeed()
    {
        return speed;
    }
    
    public void setSpeed(double speed)
    {
        this.speed = speed;
        if (route != null)
        {
            route.setSpeed(speed);
            fire(RouteEvent.CHANGE);
        }
    }
    
    public Route getRoute()
    {
        return route;
    }
    
    public Route startRoute()
    {
        return new Route(this, markers);
    }
    
    public void fireRouteStarted(Route route)
    {
        this.route = route;
        fire(RouteEvent.CHANGE);
    }
    
    public void firePositionUpdate(GeoPosition position)
    {
        currentPosition = position;
        fire(RouteEvent.CHANGE);
    }
    
    public void fireRouteEnded()
    {
        route = null;
        currentPosition = null;
        fire(RouteEvent.CHANGE);
        fire(RouteEvent.ENDED);
    }
    
    public void fireRouteStateChange()
    {
        fire(RouteEvent.CHANGE);
    }
    
    public void addRouteChangeListener(RouteChangeListener listener) {
        routeChangeListeners.add(listener);
    }
    
    public void removeRouteChangeListener(RouteChangeListener listener)
    {
        routeChangeListeners.remove(listener);
    }
    
    private void fire(RouteEvent e)
    {
        if (e == RouteEvent.CHANGE)
        {
            for (RouteChangeListener listener : routeChangeListeners)
            {
                listener.routeChanged();
            }
        }
        else if (e == RouteEvent.ENDED)
        {
            for (RouteChangeListener listener : routeChangeListeners)
            {
                listener.routeEnded();
            }
        }
    }
    
    private enum RouteEvent
    {
        CHANGE,
        ENDED
    }
}
