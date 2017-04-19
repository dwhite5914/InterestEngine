package com.nuwc.interestengine.clustering;

import com.nuwc.interestengine.Utils;
import com.nuwc.interestengine.data.PointType;
import com.nuwc.interestengine.data.DataPoint;
import com.nuwc.interestengine.data.Database;
import com.nuwc.interestengine.gui.MainFrame;
import com.nuwc.interestengine.data.AISPoint;
import com.nuwc.interestengine.map.RoutePainter;
import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class RouteExtractor
{
    private final Database db;
    private final RoutePainter painter;
    private final MainFrame mainFrame;
    private final float lostTime;
    private final float minSpeed;
    private final float entryEpsilon;
    private final int entryMinPoints;
    private final float exitEpsilon;
    private final int exitMinPoints;
    private final float stopEpsilon;
    private final int stopMinPoints;

    private final HashMap<Integer, Vessel> vessels;
    private final HashMap<Integer, Integer> vesselWaypointCounts;
    private final List<AISPoint> entryPoints;
    private final List<AISPoint> exitPoints;
    private final List<AISPoint> stopPoints;
    private final List<AISPoint> aisPoints;

    private HashMap<Point, RouteObject> routes;
    private int id = 0;
    private String routesPath;

    private final List<Cluster> clusterList;
    private final int minPoints;
    private final double epsilon;
    private int clustersCount;

    private int entryIndex;
    private int exitIndex;
    private int stopIndex;

    public RouteExtractor(Database db, RoutePainter painter, MainFrame mainFrame,
            float lostTime, float minSpeed,
            float entryEpsilon, int entryMinPoints,
            float exitEpsilon, int exitMinPoints,
            float stopEpsilon, int stopMinPoints)
    {
        this.db = db;
        this.painter = painter;
        this.mainFrame = mainFrame;
        this.lostTime = lostTime;
        this.minSpeed = minSpeed;
        this.entryEpsilon = entryEpsilon;
        this.entryMinPoints = entryMinPoints;
        this.exitEpsilon = exitEpsilon;
        this.exitMinPoints = exitMinPoints;
        this.stopEpsilon = stopEpsilon;
        this.stopMinPoints = stopMinPoints;

        vessels = new HashMap<>();
        vesselWaypointCounts = new HashMap<>();
        entryPoints = new ArrayList<>();
        exitPoints = new ArrayList<>();
        stopPoints = new ArrayList<>();
        aisPoints = new ArrayList<>();

        routes = new HashMap<>();
        init();

        clusterList = new ArrayList<>();
        clustersCount = 0;
        epsilon = entryEpsilon;
        minPoints = entryMinPoints;

        entryIndex = 0;
        exitIndex = 0;
        stopIndex = 0;
    }

    private void init()
    {
        File routesDir = new File(Utils.getResource("") + "/routes");
        if (!routesDir.exists())
        {
            routesDir.mkdir();
        }

        routesPath = routesDir.getAbsolutePath() + "/routes.ser";
    }

    public void saveRoutes()
    {
        ObjectOutputStream objectOutStream = null;
        try
        {
            FileOutputStream outStream = new FileOutputStream(routesPath);
            objectOutStream = new ObjectOutputStream(outStream);
            objectOutStream.writeObject(routes);
        }
        catch (IOException e)
        {
            System.out.println("Failed to write routes to file.");
        }
        finally
        {
            try
            {
                objectOutStream.close();
            }
            catch (IOException e)
            {
                System.out.println("Failed to close routes output stream.");
            }
        }
    }

    public void loadRoutes()
    {
        ObjectInputStream objectInStream = null;
        try
        {
            FileInputStream inStream = new FileInputStream(routesPath);
            objectInStream = new ObjectInputStream(inStream);
            routes = (HashMap<Point, RouteObject>) objectInStream.readObject();
        }
        catch (IOException | ClassNotFoundException ex)
        {
            System.out.println("Failed to read routes from file.");
        }
        finally
        {
            try
            {
                objectInStream.close();
            }
            catch (IOException ex)
            {
                System.out.println("Failed to close routes input stream.");
            }
        }
    }

    public List<RouteObject> getOversampledRoutes()
    {
        List<RouteObject> allRoutes = new ArrayList<>();
        for (RouteObject route : routes.values())
        {
            allRoutes.add(route);
        }

        List<RouteObject> validRoutes = new ArrayList<>();
        for (RouteObject route : allRoutes)
        {
            if (route.points.size() >= 100)
            {
                validRoutes.add(route);
            }
        }

        int maxPoints = validRoutes.get(0).points.size();
        for (RouteObject route : validRoutes)
        {
            int numPoints = route.points.size();
            if (numPoints > maxPoints)
            {
                maxPoints = numPoints;
            }
        }

        List<RouteObject> oversampledRoutes = new ArrayList<>();
        for (RouteObject route : validRoutes)
        {
            List<AISPoint> oversampledPoints = new ArrayList<>(route.points);
            int deficit = maxPoints - route.points.size();
            System.out.println("deficit " + deficit);
            Random rand = new Random();
            for (int i = 0; i < deficit; i++)
            {
                int randIndex = rand.nextInt(route.points.size());
                AISPoint randPoint = route.points.get(randIndex);
                AISPoint newPoint = new AISPoint();
                newPoint.lat = randPoint.lat;
                newPoint.lon = randPoint.lon;
                newPoint.sog = randPoint.sog;
                newPoint.cog = randPoint.cog;
                newPoint.shipType = randPoint.shipType;
                oversampledPoints.add(newPoint);
            }
            Collections.shuffle(oversampledPoints);
            RouteObject oversampledRoute = new RouteObject(route.id);
            oversampledRoute.points = oversampledPoints;
            oversampledRoutes.add(oversampledRoute);
        }

        return oversampledRoutes;
    }

    public int getNumberOfClusters()
    {
        int count = 0;
        for (Cluster cluster : clusterList)
        {
            if (cluster.getIsActive())
            {
                count++;
            }
        }

        return count;
    }

    public int getNumberOfRoutes()
    {
        return routes.size();
    }

    public List<RouteObject> run()
    {
        // Load data from database.
        System.out.println("Loading from Database...");
        List<DataPoint> points = db.getVesselData();
        System.out.println("Loading from Database Ended.");

        System.out.println("Route Extraction 2 Started...");

        // Run route extraction algorithm.
        for (int i = 0; i < points.size(); i++)
        {
            DataPoint point = points.get(i);
            AISPoint aisPoint = new AISPoint(point.lat, point.lon,
                    point.sog, point.cog, point.timestamp);
            aisPoint.shipType = point.shipType;
            aisPoints.add(aisPoint);

            extractRoutes(point.mmsi, aisPoint, point.shipType, points, i);
        }

        noiseLabel(aisPoints);

        System.out.println("Route Extraction 2 Ended.");

        for (Point routeKey : routes.keySet())
        {
            System.out.println("(" + routeKey.x + ", " + routeKey.y + "): "
                    + routes.get(routeKey).points.size());
        }

        Random rand = new Random();

        for (Cluster cluster : clusterList)
        {
            float r = rand.nextFloat();
            float g = rand.nextFloat();
            float b = rand.nextFloat();
            Color randColor = new Color(r, g, b);
            cluster.color = randColor;
        }

        for (RouteObject route : routes.values())
        {
            float r = rand.nextFloat();
            float g = rand.nextFloat();
            float b = rand.nextFloat();
            Color randColor = new Color(r, g, b);
            route.color = randColor;
        }

        int k = 0;
        List<RouteObject> validRoutes = new ArrayList<>();
        for (RouteObject route : routes.values())
        {
            if (route.points.size() >= 100)
            {
                route.id = k++;
                validRoutes.add(route);
            }
        }

        painter.setRoutes(validRoutes);
        painter.setClusters(clusterList);
        painter.setDataPoints(aisPoints);
        painter.setEntryPoints(entryPoints);
        painter.setExitPoints(exitPoints);
        painter.setStopPoints(stopPoints);

        System.out.println("Route Extraction 2 Complete.");

        return validRoutes;
    }

    private void extractRoutes(int mmsi, AISPoint point, String shipType,
            List<DataPoint> data, int index)
    {
        if (!vessels.containsKey(mmsi))
        {
            // Vessel does not exist -> add to vessel list, set 'sailing'.
            Vessel vessel = new Vessel(mmsi);
            vessel.shipType = shipType;
            vessel.status = ShipStatus.SAILING;
            vessel.track.add(point);
            vessels.put(mmsi, vessel);

            updateClusters(vessel, entryPoints);
            entryPoints.add(point);
            updateRoutes(vessel);
        }
        else
        {
            // Vessel exists -> update and test parameters.
            Vessel vessel = vessels.get(mmsi);
            vessel.track.add(point);
            vessel.averageSpeed = point.sog;

            if (vessel.averageSpeed < minSpeed
                    && vessel.status == ShipStatus.SAILING)
            {
                //System.out.println("SPEED");
                vessel.status = ShipStatus.STATIONARY;
                updateClusters(vessel, stopPoints);
                stopPoints.add(point);
                updateRoutes(vessel);
            }
            else if (vessel.status == ShipStatus.LOST)
            {
                vessel.status = ShipStatus.SAILING;
                updateClusters(vessel, entryPoints);
                entryPoints.add(point);
                updateRoutes(vessel);
            }
        }

        if (index % (data.size() / 3) == 0 || index == data.size() - 1)
        {
            for (Vessel v : vessels.values())
            {
                long lastTimestamp = v.last().timestamp;
                long timestamp = point.timestamp;
                long timeDelta = Math.abs(timestamp - lastTimestamp);

                if (v.status != ShipStatus.LOST && timeDelta > lostTime)
                {
                    v.status = ShipStatus.LOST;
                    updateClusters(v, exitPoints);
                    exitPoints.add(point);
                    updateRoutes(v);
                }
            }
        }
    }

    private void updateRoutes(Vessel v)
    {
        int numWaypoints = v.waypoints.size();
        if (v.waypoints.size() > 1)
        {
            vesselWaypointCounts.put(v.mmsi, numWaypoints);
            int x = v.waypoints.size() - 2;
            int y = v.waypoints.size() - 1;
            Cluster waypointX = v.waypoints.get(x);
            Cluster waypointY = v.waypoints.get(y);
            Point routeKey = new Point();
            routeKey.x = waypointX.getID();
            routeKey.y = waypointY.getID();
            if (!routes.containsKey(routeKey))
            {
                routes.put(routeKey, new RouteObject(id++));
            }

            long timestampX = v.wpTimestamps.get(x);
            long timestampY = v.wpTimestamps.get(y);

            RouteObject route = routes.get(routeKey);
            for (AISPoint point : v.track)
            {
                if (timestampX <= point.timestamp
                        && point.timestamp <= timestampY)
                {
                    route.points.add(point);
                }
            }
            route.vessels.add(v);

            route.waypoints.add(waypointX);
            route.waypoints.add(waypointY);

            v.routes.add(route);
        }
        else
        {
            vesselWaypointCounts.put(v.mmsi, 0);
        }
    }

    private void updateClusters(Vessel v, List<AISPoint> dataset)
    {
        AISPoint point = v.last();
        List<AISPoint> updateSeeds = getUpdateSeeds(point, dataset);
        if (updateSeeds.isEmpty())
        {
            System.out.println("NOISE");
            markAsNoise(point);
            System.out.println("NOISE_FINISHED");
        }
        else if (updateSeedContainsCorePointsWithNoCluster(updateSeeds))
        {
            System.out.println("CREATE");
            createCluster(point, v, updateSeeds);
            System.out.println("CREATE_FINISHED");
        }
        else if (updateSeedContainsCorePointsFromOneCluster(updateSeeds))
        {
            System.out.println("EXPAND");
            expandCluster(point, v, updateSeeds);
            System.out.println("EXPAND_FINISHED");
        }
        else
        {
            System.out.println("MERGE");
            mergeClusters(point, v, updateSeeds);
            System.out.println("MERGE_FINISHED");
        }
        point.setIsVisited(true);
    }

    private List<AISPoint> getUpdateSeeds(AISPoint point, List<AISPoint> dataset)
    {
        List<AISPoint> updateSeeds = new ArrayList<>();
        for (AISPoint p : dataset)
        {
            if (!p.isVisited())
            {
                break;
            }
            double distance = point.distance(p);
            if (distance > this.epsilon)
            {
                continue;
            }
            point.neighbors.add(p);
            p.neighbors.add(point);
            if (p.getType(minPoints) == PointType.CORE)
            {
                updateSeeds.add(p);
            }
        }

        return updateSeeds;
    }

    private boolean updateSeedContainsCorePointsWithNoCluster(
            List<AISPoint> updateSeeds)
    {
        for (AISPoint p : updateSeeds)
        {
            if (p.assignedCluster != null)
            {
                return false;
            }
        }

        return true;
    }

    private boolean updateSeedContainsCorePointsFromOneCluster(
            List<AISPoint> updateSeeds)
    {
        Cluster c = updateSeeds.get(0).assignedCluster;
        for (int i = 1; i < updateSeeds.size(); i++)
        {
            AISPoint p = updateSeeds.get(i);
            if (p.assignedCluster != c)
            {
                return false;
            }
        }

        return true;
    }

    private void markAsNoise(AISPoint point)
    {
        point.setType(PointType.NOISE);
    }

    private void createCluster(AISPoint point, Vessel v, List<AISPoint> updateSeeds)
    {
        Cluster c = new Cluster(this.clustersCount);
        clustersCount++;

        point.assignedCluster = c;
        c.points.add(point);
        for (AISPoint p : updateSeeds)
        {
            p.assignedCluster = c;
            c.points.add(p);
        }
        clusterList.add(c);

        v.waypoints.add(c);
        v.wpTimestamps.add(point.timestamp);
        c.mmsiList.add(v.mmsi);
    }

    private void expandCluster(AISPoint point, Vessel v, List<AISPoint> updateSeeds)
    {
        Cluster c = updateSeeds.get(0).assignedCluster;
        point.assignedCluster = c;
        c.points.add(point);

        v.waypoints.add(c);
        v.wpTimestamps.add(point.timestamp);
        c.mmsiList.add(v.mmsi);
    }

    private void mergeClusters(AISPoint point, Vessel v, List<AISPoint> updateSeeds)
    {
        List<Cluster> clusters = getPointClusters(updateSeeds);
        Cluster masterCluster = clusters.get(0);
        point.assignedCluster = masterCluster;
        masterCluster.points.add(point);
        for (int i = 1; i < clusters.size(); i++)
        {
            Cluster c = clusters.get(i);
            c.setActive(false);
            List<AISPoint> cPoints = c.points;

            // Assign each cluster's points to master cluster.
            AISPoint[] cPointsArray = new AISPoint[cPoints.size()];
            cPoints.toArray(cPointsArray);
            for (AISPoint p : cPointsArray)
            {
                p.assignedCluster = masterCluster;
                masterCluster.points.add(p);
            }

            // Update each vessel's waypoints to observe the master cluster.
            for (Integer mmsi : c.mmsiList)
            {
                Vessel v0 = vessels.get(mmsi);
                for (int j = 0; j < v0.waypoints.size(); j++)
                {
                    Cluster wp = v0.waypoints.get(j);
                    if (c == wp)
                    {
                        v0.waypoints.set(j, masterCluster);
                    }
                }
            }

            // Add each cluster's mmsi list to the master cluster's mmsi list.
            masterCluster.mmsiList.addAll(c.mmsiList);
        }

        v.waypoints.add(masterCluster);
        v.wpTimestamps.add(point.timestamp);
        masterCluster.mmsiList.add(v.mmsi);
    }

    private List<Cluster> getPointClusters(List<AISPoint> updateSeeds)
    {
        List<Cluster> clusters = new ArrayList<>();
        for (AISPoint p : updateSeeds)
        {
            Cluster c = p.assignedCluster;
            if (c == null)
            {
                continue;
            }
            if (!clusters.contains(c))
            {
                clusters.add(c);
            }
        }

        return clusters;
    }

    private void noiseLabel(List<AISPoint> dataset)
    {
        for (AISPoint p : dataset)
        {
            if (p.getType(minPoints) != PointType.NOISE)
            {
                continue;
            }
            for (AISPoint neighbor : p.neighbors)
            {
                if (neighbor.assignedCluster == null
                        || neighbor.getType(minPoints) != PointType.CORE)
                {
                    continue;
                }
                p.assignedCluster = neighbor.assignedCluster;
                Cluster c = p.assignedCluster;
                c.points.add(p);
                break;
            }
        }
    }
}
