package com.nuwc.interestengine.clustering;

import com.nuwc.interestengine.data.AISPoint;
import com.nuwc.interestengine.data.KernelDensityEstimator;
import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RouteObject implements Serializable
{
    public List<Cluster> waypoints;
    public List<AISPoint> points;
    public List<Vessel> vessels;
    public Color color;
    public int id;
    public KernelDensityEstimator kde;

    public RouteObject(int id)
    {
        this.id = id;
        waypoints = new ArrayList<>();
        points = new ArrayList<>();
        vessels = new ArrayList<>();
        kde = null;
        color = Color.BLUE;
    }
}
