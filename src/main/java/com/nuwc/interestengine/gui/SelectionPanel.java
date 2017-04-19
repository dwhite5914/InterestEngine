package com.nuwc.interestengine.gui;

import com.nuwc.interestengine.Utils;
import com.nuwc.interestengine.clustering.RouteObject;
import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.data.AISPoint;
import com.nuwc.interestengine.data.Cell;
import com.nuwc.interestengine.data.KernelDensityEstimator;
import com.nuwc.interestengine.data.StateVector;
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

    public SelectionPanel(RoutePainter routePainter, MainFrame mainFrame,
            OptionsPanel optionsPanel)
    {
        this.routePainter = routePainter;
        this.mainFrame = mainFrame;
        this.optionsPanel = optionsPanel;

        initComponents();
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
    }

    public void calcBestRoute()
    {
        List<RouteObject> routes = optionsPanel.getRoutes();
        StateVector vector = selectedVessel.last().toVector();
        int numVesselsTotal = getNumVesselsTotal(routes);

        List<Double> bestRouteProbs = new ArrayList<>();
        for (RouteObject route : routes)
        {
            double positionProb = route.kde.evaluatePositionConditional(vector, vector, 1);
            double velocityProb = route.kde.evaluateVelocityConditional(vector);
            if (velocityProb == 0)
            {
                velocityProb = 1E-15;
            }
            int numVesselsOfType = 0;
            for (Vessel vessel : route.vessels)
            {
                numVesselsOfType++;
            }

            double typeAndRouteProb = (double) numVesselsOfType / numVesselsTotal;
            double bestRouteProb = positionProb * velocityProb * typeAndRouteProb;
            bestRouteProbs.add(bestRouteProb);

            System.out.println("Route " + route.id + " Probabilities:");
            System.out.println("Position: " + positionProb);
            System.out.println("Velocity: " + velocityProb);
            System.out.println("Type and Route: " + typeAndRouteProb);
            System.out.println("Best Route: " + bestRouteProb);
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

        System.out.println("Best route is Route " + bestRoute.id
                + " with probability: " + highestProb);
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
            double velocityProb = route.kde.evaluateVelocityConditional(vector);
            if (velocityProb == 0)
            {
                velocityProb = 1E-15;
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
        if (drawKDECheckBox.isSelected())
        {
            gridOn();
        }
        else
        {
            gridOff();
        }
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
            formalPositionField.setText("");
            sogField.setText("");
            cogField.setText("");
            shipTypeField.setText("");
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
            formalPositionField.setText(String.format("(%.2f, %.2f)",
                    selectedVessel.last().lat, selectedVessel.last().lon));
            sogField.setText(String.format("%.1f knots",
                    selectedVessel.last().sog));
            cogField.setText(String.format("%.1f %s",
                    selectedVessel.last().cog, Utils.DEGREE));
            shipTypeField.setText((selectedVessel.shipType == null)
                    ? "Unknown" : selectedVessel.shipType);
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
        foldablePanel1 = new com.nuwc.interestengine.gui.FoldablePanel();
        jPanel2 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        mmsiField = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        shipNameField = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        positionField = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        formalPositionField = new javax.swing.JTextField();
        jTextField9 = new javax.swing.JTextField();
        sogField = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        cogField = new javax.swing.JTextField();
        jTextField13 = new javax.swing.JTextField();
        shipTypeField = new javax.swing.JTextField();
        jTextField15 = new javax.swing.JTextField();
        shipCategoryField = new javax.swing.JTextField();
        jTextField17 = new javax.swing.JTextField();
        navigationStatusField = new javax.swing.JTextField();
        jTextField19 = new javax.swing.JTextField();
        reportedDestinationField = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        drawKDECheckBox = new javax.swing.JCheckBox();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jPanel17 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();

        jScrollPane1.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(555, 473));

        foldablePanel1.setTitle("Selection");

        jPanel2.setLayout(new java.awt.GridLayout(10, 2));

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
        jTextField5.setText("Position");
        jPanel2.add(jTextField5);

        positionField.setEditable(false);
        positionField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(positionField);

        jTextField7.setEditable(false);
        jTextField7.setBackground(new java.awt.Color(255, 255, 255));
        jTextField7.setText("Position Formal");
        jTextField7.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jTextField7ActionPerformed(evt);
            }
        });
        jPanel2.add(jTextField7);

        formalPositionField.setEditable(false);
        formalPositionField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(formalPositionField);

        jTextField9.setEditable(false);
        jTextField9.setBackground(new java.awt.Color(255, 255, 255));
        jTextField9.setText("Speed Over Ground");
        jPanel2.add(jTextField9);

        sogField.setEditable(false);
        sogField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(sogField);

        jTextField11.setEditable(false);
        jTextField11.setBackground(new java.awt.Color(255, 255, 255));
        jTextField11.setText("Course Over Ground");
        jPanel2.add(jTextField11);

        cogField.setEditable(false);
        cogField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(cogField);

        jTextField13.setEditable(false);
        jTextField13.setBackground(new java.awt.Color(255, 255, 255));
        jTextField13.setText("Ship Type");
        jPanel2.add(jTextField13);

        shipTypeField.setEditable(false);
        shipTypeField.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.add(shipTypeField);

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

        foldablePanel1.add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.GridLayout(1, 4));

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/loaded_icon.png"))); // NOI18N
        jButton1.setText("Best Route");
        jButton1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton1);

        drawKDECheckBox.setText("Draw KDE");
        drawKDECheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                drawKDECheckBoxActionPerformed(evt);
            }
        });
        jPanel3.add(drawKDECheckBox);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/forecast_icon.png"))); // NOI18N
        jButton2.setText("Route Forecast");
        jButton2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton2);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/past_track_icon.png"))); // NOI18N
        jButton3.setText("Past Track");
        jButton3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton3);

        jPanel17.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel17.setLayout(new java.awt.BorderLayout());

        jLabel5.setBackground(new java.awt.Color(255, 255, 255));
        jLabel5.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Selected Vessel");
        jPanel17.add(jLabel5, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(foldablePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(foldablePanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

    private void jTextField7ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jTextField7ActionPerformed
    {//GEN-HEADEREND:event_jTextField7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField7ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
    {//GEN-HEADEREND:event_jButton1ActionPerformed
        calcBestRoute();
        RouteObject visibleRoute = new RouteObject(bestRoute.id);
        visibleRoute.points = bestRoute.points;
        visibleRoute.color = Color.BLUE;
        routePainter.setBestRoute(visibleRoute);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void drawKDECheckBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_drawKDECheckBoxActionPerformed
    {//GEN-HEADEREND:event_drawKDECheckBoxActionPerformed
        checkGrid();
    }//GEN-LAST:event_drawKDECheckBoxActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
    {//GEN-HEADEREND:event_jButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton3ActionPerformed
    {//GEN-HEADEREND:event_jButton3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField cogField;
    private javax.swing.JCheckBox drawKDECheckBox;
    private com.nuwc.interestengine.gui.FoldablePanel foldablePanel1;
    private javax.swing.JTextField formalPositionField;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField15;
    private javax.swing.JTextField jTextField17;
    private javax.swing.JTextField jTextField19;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JTextField mmsiField;
    private javax.swing.JTextField navigationStatusField;
    private javax.swing.JTextField positionField;
    private javax.swing.JTextField reportedDestinationField;
    private javax.swing.JTextField shipCategoryField;
    private javax.swing.JTextField shipNameField;
    private javax.swing.JTextField shipTypeField;
    private javax.swing.JTextField sogField;
    // End of variables declaration//GEN-END:variables
}
