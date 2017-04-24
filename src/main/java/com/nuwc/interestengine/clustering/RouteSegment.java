package com.nuwc.interestengine.clustering;

import com.nuwc.interestengine.data.AISPoint;
import java.util.ArrayList;
import java.util.List;

public class RouteSegment
{
    public List<Cluster> waypoints;
    public List<AISPoint> points;
    public List<Vessel> vessels;
    public int id;

    public RouteSegment(int id)
    {
        waypoints = new ArrayList<>();
        points = new ArrayList<>();
        vessels = new ArrayList<>();

        this.id = id;
    }
}
