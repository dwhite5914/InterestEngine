package com.nuwc.clustering;

import com.nuwc.data.AISPoint;
import java.util.ArrayList;
import java.util.List;

public class Vessel
{
    public int mmsi;
    public List<AISPoint> track;
    public String shipType;
    public ShipStatus status;
    public float averageSpeed;

    public List<Long> wpTimestamps;
    public List<Cluster> waypoints;
    public List<RouteObject> routes;

    public Vessel(int mmsi)
    {
        this.mmsi = mmsi;
        shipType = null;
        averageSpeed = 0;
        track = new ArrayList<>();
        status = ShipStatus.SAILING;

        wpTimestamps = new ArrayList<>();
        waypoints = new ArrayList<>();
        routes = new ArrayList<>();
    }

    public AISPoint last()
    {
        return track.get(track.size() - 1);
    }
}
