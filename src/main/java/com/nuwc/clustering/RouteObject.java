package com.nuwc.clustering;

import com.nuwc.data.AISPoint;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class RouteObject
{
    public List<Cluster> waypoints;
    public List<AISPoint> points;
    public Color color;

    public RouteObject()
    {
        waypoints = new ArrayList<>();
        points = new ArrayList<>();
        color = Color.BLUE;
    }
}
