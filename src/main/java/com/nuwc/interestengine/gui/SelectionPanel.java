package com.nuwc.interestengine.gui;

import com.nuwc.interestengine.Utils;
import com.nuwc.interestengine.clustering.RouteObject;
import com.nuwc.interestengine.clustering.RouteSegment;
import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.data.AISPoint;
import com.nuwc.interestengine.data.Cell;
import com.nuwc.interestengine.data.KernelDensityEstimator;
import com.nuwc.interestengine.data.State;
import com.nuwc.interestengine.data.StateVector;
import com.nuwc.interestengine.data.TreeNode;
import com.nuwc.interestengine.map.RoutePainter;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;

public class SelectionPanel extends javax.swing.JPanel
{
    private Vessel selectedVessel;
    private RoutePainter routePainter;
    private MainFrame mainFrame;
    private OptionsPanel optionsPanel;
    private RouteObject bestRoute;
    private VesselFrame vesselFrame;
    private TreeNode routeTree;

    private double probSoftMax = 0;
    private double probMean = 0;
    private double probStdev = 0;

    private double posProbMax = 0;
    private double velProbMax = 0;
    private double velStdev = 0;

    public SelectionPanel(RoutePainter routePainter, MainFrame mainFrame,
            OptionsPanel optionsPanel, VesselFrame vesselFrame)
    {
        this.routePainter = routePainter;
        this.mainFrame = mainFrame;
        this.optionsPanel = optionsPanel;

        this.vesselFrame = vesselFrame;

        initComponents();
    }

    public void setRouteTree(TreeNode routeTree)
    {
        this.routeTree = routeTree;
    }

    public void train()
    {
        List<RouteObject> routes = optionsPanel.getRoutes();
        for (RouteObject route : routes)
        {
            route.kde = new KernelDensityEstimator();
            route.kde.fit(route);
        }
        System.out.println("-----------Trained-----------");

        List<Double> posProbs = new ArrayList<>();
        double maxPosProb = 0;
        double meanPosProb = 0;
        double stdevPosProb = 0;
        int numPoints = 0;

        List<Double> velProbs = new ArrayList<>();
        double meanVelProb = 0;
        double maxVelProb = 0;

        for (RouteObject route : routes)
        {
            for (AISPoint point : route.points)
            {
                StateVector vector = point.toVector();
                double posProb = route.kde.evaluatePositionConditional(vector, vector, 1);
                double velProb = route.kde.evaluateJointVelocity(vector);
                posProbs.add(posProb);
                velProbs.add(velProb);
                if (posProb > maxPosProb)
                {
                    maxPosProb = posProb;
                }
                if (velProb > maxVelProb)
                {
                    maxVelProb = velProb;
                }
                numPoints++;
            }
        }

        for (double prob : posProbs)
        {
            meanPosProb += prob;
        }
        meanPosProb /= numPoints;

        for (double prob : velProbs)
        {
            meanVelProb += prob;
        }
        meanVelProb /= numPoints;

        for (double prob : posProbs)
        {
            stdevPosProb += Math.pow(prob - meanPosProb, 2);
        }
        stdevPosProb /= numPoints;

        double stdevVelProb = 0;
        for (double prob : velProbs)
        {
            stdevVelProb += Math.pow(prob - meanVelProb, 2);
        }
        stdevVelProb /= numPoints;

        posProbMax = meanPosProb;
        velProbMax = meanVelProb;
        velStdev = stdevVelProb;

        probSoftMax = meanPosProb + 3 * stdevPosProb;
        probMean = meanPosProb;
        probStdev = stdevPosProb;
    }

    public boolean isAnomaly(Vessel v)
    {
        AISPoint point = v.last();
        StateVector vector = point.toVector();
        RouteObject bestRoute = calcBestRouteForPoint(point);
        int numPoints = bestRoute.points.size();
        AnomalyChecker checkers[] = new AnomalyChecker[8];
        for (int i = 0; i < 8; i++)
        {
            checkers[i] = new AnomalyChecker(i, point, bestRoute);
            checkers[i].start();
        }

        for (int i = 0; i < 8; i++)
        {
            try
            {
                checkers[i].join();
            }
            catch (InterruptedException e)
            {
                System.out.println("Thread failed to join.");
            }
        }

        double minDist = 5;
        for (int i = 0; i < 8; i++)
        {
            double dist = checkers[i].getMinDist();
            if (dist < minDist)
            {
                minDist = dist;
            }
        }

        if (minDist < 4 || Utils.isAnchored(v.navStatus) || v.last().sog < 0.12)
        {
            return false;
        }

        return true;
    }

    private class AnomalyChecker extends Thread
    {
        private int start, end;
        private AISPoint point;
        private RouteObject route;
        private double minDist;

        public AnomalyChecker(int i, AISPoint point, RouteObject route)
        {
            int numPoints = route.points.size();
            start = i * (numPoints / 8);
            end = start + (numPoints / 8);
            if (end >= numPoints)
            {
                end = numPoints - 1;
            }
            this.point = point;
            this.route = route;
        }

        public double getMinDist()
        {
            return minDist;
        }

        @Override
        public void run()
        {
            minDist = 5;
            for (int i = start; i <= end; i++)
            {
                AISPoint routePoint = route.points.get(i);
                double dist = point.distance(routePoint);
                if (dist < minDist)
                {
                    minDist = dist;
                }
            }
        }

    }

    public RouteObject getBestRoute()
    {
        List<RouteObject> routes = optionsPanel.getRoutes();
        StateVector vector = selectedVessel.last().toVector();
        List<RouteSegment> segments = new ArrayList<>();
        for (RouteObject route : routes)
        {
            for (AISPoint point : route.points)
            {
                for (TreeNode node : routeTree.children)
                {
                    Cell majorTile = (Cell) node.value;
                    if (majorTile.contains(point))
                    {
                        for (TreeNode child : node.children)
                        {
                            Cell minorTile = (Cell) node.value;
                            if (minorTile.contains(point))
                            {
                                for (TreeNode segNode : child.children)
                                {
                                    RouteSegment segment
                                            = (RouteSegment) segNode.value;
                                    segments.add(segment);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        List<Double> probs = new ArrayList<>();
        for (RouteSegment segment : segments)
        {
            State state = new State(selectedVessel.last(), 10, segment);
            double posProb = state.evaluatePosition(vector);
            double velProb = state.evaluateVelocity(vector);
            double jointProb = posProb * velProb;
            probs.add(jointProb);
            System.out.println("pos: " + posProb);
            System.out.println("vel: " + velProb);
        }

        double bestProb = 0;
        int bestID = -1;
        for (int i = 0; i < segments.size(); i++)
        {
            double prob = probs.get(i);
            if (prob > bestProb)
            {
                bestProb = prob;
                bestID = segments.get(i).id;
            }
        }

        return routes.get(bestID);
    }

    public void calcBestRoute()
    {
        List<RouteObject> routes = optionsPanel.getRoutes();
        StateVector vector = selectedVessel.last().toVector();
        int numVesselsTotal = getNumVesselsTotal(routes);

        List<Double> bestRouteProbs = new ArrayList<>();
        List<Double> posProbs = new ArrayList<>();
        List<Double> velProbs = new ArrayList<>();
        for (RouteObject route : routes)
        {
            double positionProb = route.kde.evaluatePositionConditional(vector, vector, 1);
            double velocityProb = route.kde.evaluateJointVelocity(vector);
            int numVesselsOfType = 0;
            for (Vessel vessel : route.vessels)
            {
                numVesselsOfType++;
            }

            double typeAndRouteProb = (double) numVesselsOfType / numVesselsTotal;
            double bestRouteProb = positionProb * velocityProb * typeAndRouteProb;
            bestRouteProbs.add(bestRouteProb);
            posProbs.add(positionProb);
            velProbs.add(velocityProb);

            System.out.println("Route " + route.id + " Probabilities:");
            System.out.println("Position: " + positionProb);
            System.out.println("Velocity: " + velocityProb);
            System.out.println("Type and Route: " + typeAndRouteProb);
            System.out.println("Best Route: " + bestRouteProb);
        }

        for (int i = 0; i < routes.size(); i++)
        {
            double posProb = posProbs.get(i);
            double velProb = velProbs.get(i);
            posProbs.set(i, normalize(posProb, posProbMax));
            velProbs.set(i, normalize(velProb, velProbMax));
        }

        double highestPosProb = 0;
        double highestVelProb = 0;
        bestRoute = null;
        for (int i = 0; i < routes.size(); i++)
        {
            double posProb = posProbs.get(i);
            double velProb = velProbs.get(i);
            if (posProb > highestPosProb)
            {
                highestPosProb = posProb;
                highestVelProb = velProb;
                bestRoute = routes.get(i);
            }
        }

        for (int i = 0; i < routes.size(); i++)
        {
            RouteObject route = routes.get(i);
            double posProb = posProbs.get(i);
            double velProb = velProbs.get(i);
            System.out.println("Route " + route.id + " Probabilities:");
            System.out.println("Position: " + posProb);
            System.out.println("Velocity: " + velProb);
        }

//        double highestProb = 0;
//        bestRoute = null;
//        for (int i = 0; i < routes.size(); i++)
//        {
//            double prob = bestRouteProbs.get(i);
//            if (prob > highestProb)
//            {
//                highestProb = prob;
//                bestRoute = routes.get(i);
//            }
//        }
        System.out.println("Best route is Route " + bestRoute.id
                + " with probability: " + highestPosProb + ", " + highestVelProb);
    }

    public double normalize(double originalProb, double maxProb)
    {
        double newProb = originalProb / maxProb;
        return newProb;
    }

    public RouteObject calcBestRouteForPoint(AISPoint point)
    {
        List<RouteObject> routes = optionsPanel.getRoutes();
        StateVector vector = point.toVector();
        int numVesselsTotal = getNumVesselsTotal(routes);

        List<Double> bestRouteProbs = new ArrayList<>();
        for (RouteObject route : routes)
        {
            double positionProb = route.kde.evaluatePositionConditional(vector, vector, 1);
            double velocityProb = route.kde.evaluateJointVelocity(vector);
            if (velocityProb == 0)
            {
                velocityProb = 1E-20;
            }
            int numVesselsOfType = 0;
            for (Vessel vessel : route.vessels)
            {
                numVesselsOfType++;
            }

            double typeAndRouteProb = (double) numVesselsOfType / numVesselsTotal;
            double bestRouteProb = positionProb * velocityProb * typeAndRouteProb;
            bestRouteProbs.add(bestRouteProb);
        }

        double highestProb = 0;
        bestRoute = null;
        for (int i = 0; i < routes.size(); i++)
        {
            double prob = bestRouteProbs.get(i);
            if (prob > highestProb)
            {
                highestProb = prob;
                bestRoute = routes.get(i);
            }
        }

        return bestRoute;
    }

    public void evaluateAccuracy()
    {
        List<RouteObject> routes = optionsPanel.getRoutes();
        int minPoints = routes.get(0).points.size();
        for (RouteObject route : routes)
        {
            int numPoints = route.points.size();
            if (numPoints < minPoints)
            {
                minPoints = numPoints;
            }
        }

        int numCorrect = 0;
        int numTotal = 0;
        for (RouteObject route : routes)
        {
            int numPoints = route.points.size();
            int excess = numPoints - minPoints;
            List<AISPoint> points = route.points;
            Collections.shuffle(points);
            List<AISPoint> downsampled = points.subList(0, numPoints - excess - 1);
            for (AISPoint point : downsampled)
            {
                RouteObject best = calcBestRouteForPoint(point);
                if (best.id == route.id)
                {
                    numCorrect++;
                }
                numTotal++;
            }
        }

        float accuracy = (float) numCorrect / numTotal;
        System.out.println("Num Correct: " + numCorrect);
        System.out.println("Total: " + numTotal);
        System.out.printf("Accuracy: %.2f%%", accuracy * 100);
    }

    public int getNumVesselsTotal(List<RouteObject> routes)
    {
        int numVesselsTotal = 0;
        for (RouteObject route : routes)
        {
            numVesselsTotal += route.vessels.size();
        }

        return numVesselsTotal;
    }

    public void gridOn()
    {
        if (bestRoute == null)
        {
            return;
        }

        float minLat = optionsPanel.getMinLat();
        float maxLat = optionsPanel.getMaxLat();
        float minLon = optionsPanel.getMinLon();
        float maxLon = optionsPanel.getMaxLon();

        List<Cell> grid = getGrid(minLat, maxLat, minLon, maxLon, 200, 200);
        List<AISPoint> gridPoints = new ArrayList<>();
        for (Cell cell : grid)
        {
            GeoPosition center = cell.getCenter();
            AISPoint point = new AISPoint();
            point.lat = (float) center.getLatitude();
            point.lon = (float) center.getLongitude();
            gridPoints.add(point);
        }

        Color colors[] = bestRoute.kde.evaluatePositionColors(
                selectedVessel.last().toVector(), gridPoints, 1);
        for (int i = 0; i < grid.size(); i++)
        {
            grid.get(i).setColor(colors[i]);
        }

        routePainter.setCells(grid);
    }

    public void gridOff()
    {
        routePainter.setCells(null);
    }

    public void checkGrid()
    {
//        if (drawKDECheckBox.isSelected())
//        {
//            gridOn();
//        }
//        else
//        {
//            gridOff();
//        }
    }

//    public RouteObject getBestRoute()
//    {
//        List<RouteObject> routes = optionsPanel.getRoutes();
//        StateVector vector = selectedVessel.last().toVector();
//        int numVesselsTotal = 0;
//        for (RouteObject route : routes)
//        {
//            numVesselsTotal += route.vessels.size();
//        }
//
//        List<Double> bestRouteProbs = new ArrayList<>();
//        for (RouteObject route : routes)
//        {
//            State currentState = new State(vector.x, vector.y, 2, route);
//            double positionProb = currentState.evaluatePosition(vector);
//            double velocityProb = currentState.evaluatePosition(vector);
//            int numVesselsOfType = 0;
//            for (Vessel vessel : route.vessels)
//            {
//                numVesselsOfType++;
//            }
//            double typeAndRouteProb = (double) numVesselsOfType / numVesselsTotal;
//            double bestRouteProb = positionProb * velocityProb * typeAndRouteProb;
//            bestRouteProbs.add(bestRouteProb);
//
//            System.out.println("Route " + route.id + " Probabilities:");
//            System.out.println("Position: " + positionProb);
//            System.out.println("Velocity: " + velocityProb);
//            System.out.println("Type and Route: " + typeAndRouteProb);
//            System.out.println("Best Route: " + bestRouteProb);
//        }
//
//        double highestProb = 0;
//        RouteObject bestRoute = null;
//        for (int i = 0; i < routes.size(); i++)
//        {
//            double prob = bestRouteProbs.get(i);
//            if (prob > highestProb)
//            {
//                highestProb = prob;
//                bestRoute = routes.get(i);
//            }
//        }
//
//        KernelDensityEstimator kde = new KernelDensityEstimator();
//        kde.fit(bestRoute);
//        double prob = kde.evaluatePosition(selectedVessel.last().toVector());
//        double condProb = kde.evaluatePositionCond(
//                selectedVessel.last().toVector(), selectedVessel.last().toVector(), 2);
//
//        System.out.println("BEST ROUTE: " + highestProb);
//        System.out.println("WHOLE ROUTE: " + prob);
//        System.out.println("STATE COND: " + condProb);
//
//        float minLat = optionsPanel.getMinLat();
//        float maxLat = optionsPanel.getMaxLat();
//        float minLon = optionsPanel.getMinLon();
//        float maxLon = optionsPanel.getMaxLon();
//        State bestState = new State(vector.x, vector.y, 2, bestRoute);
//        //bestState.plotDist(minLat, maxLat, minLon, maxLon);
//
//        //kde.plotDist(minLat, maxLat, minLon, maxLon);
//        List<Cell> grid = getGrid(minLat, maxLat, minLon, maxLon, 250, 500);
//        List<AISPoint> gridPoints = new ArrayList<>();
//        for (Cell cell : grid)
//        {
//            GeoPosition center = cell.getCenter();
//            AISPoint point = new AISPoint();
//            point.lat = (float) center.getLatitude();
//            point.lon = (float) center.getLongitude();
//            gridPoints.add(point);
//        }
//
//        //Color colors[] = kde.evaluatePositionColors(selectedVessel.last().toVector(), gridPoints, 1);
//        Color colors[] = bestState.evaluatePositionColors(selectedVessel.last().toVector(), gridPoints, 1);
//        for (int i = 0; i < grid.size(); i++)
//        {
//            grid.get(i).setColor(colors[i]);
//        }
//
//        routePainter.setCells(grid);
//
//        return bestRoute;
//    }
    private List<Cell> getGrid(float minLat, float maxLat, float minLon,
            float maxLon, int xCells, int yCells)
    {
        float latDiff = maxLat - minLat;
        float lonDiff = maxLon - minLon;
        float latDelta = latDiff / yCells;
        float lonDelta = lonDiff / xCells;

        List<Cell> cells = new ArrayList<>();

        for (int i = 0; i < xCells; i++)
        {
            for (int j = 0; j < yCells; j++)
            {
                float maxLatCell = maxLat - j * latDelta;
                float minLatCell = maxLatCell - latDelta;
                float minLonCell = minLon + i * lonDelta;
                float maxLonCell = minLonCell + lonDelta;
                Cell cell = new Cell(minLatCell, maxLatCell,
                        minLonCell, maxLonCell);
                cells.add(cell);
            }
        }

        return cells;
    }

    public Vessel getSelectedVessel()
    {
        return selectedVessel;
    }

    public void setSelectedVessel(Vessel selectedVessel)
    {
        this.selectedVessel = selectedVessel;
        updateInfo();
    }

    public void updateInfo()
    {
        if (selectedVessel == null)
        {
            mmsiField.setText("");
            shipNameField.setText("");
            positionField.setText("");
            shipCategoryField.setText("");
            navigationStatusField.setText("");
            reportedDestinationField.setText("");
        }
        else
        {
            mmsiField.setText("" + selectedVessel.mmsi);
            shipNameField.setText((selectedVessel.shipName == null)
                    ? "Unknown" : selectedVessel.shipName);
            positionField.setText(String.format("(%.2f, %.2f)",
                    selectedVessel.last().lat, selectedVessel.last().lon));
            shipCategoryField.setText(
                    Utils.getShipCategory(selectedVessel.shipType));
            navigationStatusField.setText((selectedVessel.navStatus == null)
                    ? "Undefined" : "" + selectedVessel.navStatus);
            reportedDestinationField.setText((selectedVessel.destination == null)
                    ? "Unreported" : selectedVessel.destination);
        }
        repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jPanel17 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        mmsiField = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        shipNameField = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        positionField = new javax.swing.JTextField();
        jTextField15 = new javax.swing.JTextField();
        shipCategoryField = new javax.swing.JTextField();
        jTextField17 = new javax.swing.JTextField();
        navigationStatusField = new javax.swing.JTextField();
        jTextField19 = new javax.swing.JTextField();
        reportedDestinationField = new javax.swing.JTextField();

        jScrollPane1.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(555, 473));

        jPanel3.setLayout(new java.awt.GridLayout(1, 4));

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/loaded_icon.png"))); // NOI18N
        jButton2.setText("Add to Fleet");
        jButton2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton2);

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/best_route_icon.png"))); // NOI18N
        jButton4.setText("Vessel Details");
        jButton4.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton4);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/forecast_icon.png"))); // NOI18N
        jButton1.setText("Best Route");
        jButton1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton1);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/past_track_icon.png"))); // NOI18N
        jButton5.setText("Past Track");
        jButton5.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton5ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton5);

        jPanel17.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel17.setLayout(new java.awt.BorderLayout());

        jLabel5.setBackground(new java.awt.Color(255, 255, 255));
        jLabel5.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Selected Vessel");
        jPanel17.add(jLabel5, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.GridLayout(6, 2));

        jTextField1.setEditable(false);
        jTextField1.setBackground(new java.awt.Color(255, 255, 255));
        jTextField1.setText("MMSI");
        jPanel2.add(jTextField1);

        mmsiField.setEditable(false);
        mmsiField.setBackground(new java.awt.Color(255, 255, 255));
        mmsiField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mmsiFieldActionPerformed(evt);
            }
        });
        jPanel2.add(mmsiField);

        jTextField3.setEditable(false);
        jTextField3.setBackground(new java.awt.Color(255, 255, 255));
        jTextField3.setText("Name");
        jTextField3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jTextField3ActionPerformed(evt);
            }
        });
        jPanel2.add(jTextField3);

        shipNameField.setEditable(false);
        shipNameField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(shipNameField);

        jTextField5.setEditable(false);
        jTextField5.setBackground(new java.awt.Color(255, 255, 255));
        jTextField5.setText("Last Position");
        jPanel2.add(jTextField5);

        positionField.setEditable(false);
        positionField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(positionField);

        jTextField15.setEditable(false);
        jTextField15.setBackground(new java.awt.Color(255, 255, 255));
        jTextField15.setText("Ship Category");
        jPanel2.add(jTextField15);

        shipCategoryField.setEditable(false);
        shipCategoryField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(shipCategoryField);

        jTextField17.setEditable(false);
        jTextField17.setBackground(new java.awt.Color(255, 255, 255));
        jTextField17.setText("Navigation Status");
        jPanel2.add(jTextField17);

        navigationStatusField.setEditable(false);
        navigationStatusField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(navigationStatusField);

        jTextField19.setEditable(false);
        jTextField19.setBackground(new java.awt.Color(255, 255, 255));
        jTextField19.setText("Reported Destination");
        jPanel2.add(jTextField19);

        reportedDestinationField.setEditable(false);
        reportedDestinationField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(reportedDestinationField);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE)
                    .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(117, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void mmsiFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mmsiFieldActionPerformed
    {//GEN-HEADEREND:event_mmsiFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_mmsiFieldActionPerformed

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jTextField3ActionPerformed
    {//GEN-HEADEREND:event_jTextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField3ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
    {//GEN-HEADEREND:event_jButton1ActionPerformed
        calcBestRoute();
        RouteObject visibleRoute = new RouteObject(bestRoute.id);
        visibleRoute.points = bestRoute.points;
        visibleRoute.color = Color.BLUE;
        routePainter.setBestRoute(visibleRoute);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton4ActionPerformed
    {//GEN-HEADEREND:event_jButton4ActionPerformed
        vesselFrame.setVisible(true);
        vesselFrame.setSelectedVessel(selectedVessel);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton5ActionPerformed
    {//GEN-HEADEREND:event_jButton5ActionPerformed
        routePainter.setDrawAnomalousTrack(true);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
    {//GEN-HEADEREND:event_jButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField15;
    private javax.swing.JTextField jTextField17;
    private javax.swing.JTextField jTextField19;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField mmsiField;
    private javax.swing.JTextField navigationStatusField;
    private javax.swing.JTextField positionField;
    private javax.swing.JTextField reportedDestinationField;
    private javax.swing.JTextField shipCategoryField;
    private javax.swing.JTextField shipNameField;
    // End of variables declaration//GEN-END:variables
}
