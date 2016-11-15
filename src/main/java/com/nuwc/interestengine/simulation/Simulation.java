/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuwc.interestengine.simulation;

import com.nuwc.interestengine.map.Route;
import com.nuwc.interestengine.map.Ship;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dan
 */
public class Simulation
{
    private volatile int runningShips;
    private SimulationState state;
    private List<Ship> ships;
    private List<SimulationChangeListener> simulationChangeListeners;
    
    // Constructor
    public Simulation(List<Ship> ships)
    {
        // Start in a stopped state
        this.runningShips = 0;
        this.state = SimulationState.STOPPED;
        this.ships = ships;
        this.simulationChangeListeners = new ArrayList<>();
    }
    
    public synchronized void start()
    {
        // For each ship, start its route
        for (Ship ship : ships)
        {
            if (ship.getRoute() == null)
            {
                ship.startRoute();
            }
        }
        runningShips = ships.size();
        state = SimulationState.RUNNING;
        fire(SimulationEvent.STATE_CHANGED);
    }
    
    public synchronized void pause()
    {
        // For each ship, pause its route
        for (Ship ship : ships)
        {
            Route route = ship.getRoute();
            if (route != null)
            {
                route.pause();
            }
        }
        state = SimulationState.PAUSED;
        fire(SimulationEvent.STATE_CHANGED);
    }
    
    public synchronized void unpause()
    {
        // For each ship in a paused state, resume
        for (Ship ship : ships)
        {
            Route route = ship.getRoute();
            if (route != null)
            {
                route.unpause();
            }
        }
        state = SimulationState.RUNNING;
        fire(SimulationEvent.STATE_CHANGED);
    }
    
    public synchronized void stop()
    {
        // For each ship in a running state, stop
        for (Ship ship : ships)
        {
            Route route = ship.getRoute();
            if (route != null)
            {
                route.stop();
            }
        }
        // Resets number of running ships to zero
        runningShips = 0;
        state = SimulationState.STOPPED;
        fire(SimulationEvent.STATE_CHANGED);
    }
    
    public synchronized void decShips()
    {
        // Decrease number of running ships by one
        runningShips--;
        if (runningShips == 0)
        {
            state = SimulationState.STOPPED;
            fire(SimulationEvent.STATE_CHANGED);
        }
    }
    
    public SimulationState getState()
    {
        return state;
    }
    
    public void addSimulationChangeListener(SimulationChangeListener listener)
    {
        simulationChangeListeners.add(listener);
    }
    
    public void removeSimulationChangeListener(SimulationChangeListener listener)
    {
        simulationChangeListeners.remove(listener);
    }
    
    private void fire(SimulationEvent e)
    {
        if (e == SimulationEvent.STATE_CHANGED)
        {
            for (SimulationChangeListener listener : simulationChangeListeners)
            {
                listener.stateChanged();
            }
        }
    }
    
    private enum SimulationEvent
    {
        STATE_CHANGED
    }
}
