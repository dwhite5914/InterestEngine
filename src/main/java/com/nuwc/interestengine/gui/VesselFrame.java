package com.nuwc.interestengine.gui;

import com.nuwc.interestengine.Utils;
import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.data.AISPoint;
import com.nuwc.interestengine.map.RoutePainter;
import java.awt.Component;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

public class VesselFrame extends javax.swing.JFrame
{
    private Vessel selectedVessel;
    private RoutePainter routePainter;
    private JFreeChart speedLineChart;
    private DefaultCategoryDataset speedLineData;

    public VesselFrame(RoutePainter routePainter)
    {
        this.routePainter = routePainter;
        initComponents();
        initTweaks();
        initCharts();
    }

    private void initTweaks()
    {
        staticDataTable.getColumn("Attribute")
                .setCellRenderer(new BoldTableCellRenderer());
        dynamicDataTable.getColumn("Attribute")
                .setCellRenderer(new BoldTableCellRenderer());
    }

    private void initCharts()
    {
        speedLineData = new DefaultCategoryDataset();
        speedLineChart = ChartFactory.createLineChart(
                "Speed vs. Time", "Time (s)",
                "Speed (kn)",
                speedLineData, PlotOrientation.VERTICAL,
                true, true, false);
        speedLineChartPanel.setChart(speedLineChart);
        CategoryPlot plot = (CategoryPlot) speedLineChart.getPlot();
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseShapesVisible(true);
        speedLineChartPanel.repaint();
    }

    public void setSelectedVessel(Vessel selectedVessel)
    {
        this.selectedVessel = selectedVessel;
        updateTables();
    }

    public synchronized void updateTables()
    {
        if (selectedVessel == null)
        {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) staticDataTable.getModel();
        String mmsi = "" + selectedVessel.mmsi;
        String shipName = (selectedVessel.shipName == null)
                ? "Not Available"
                : selectedVessel.shipName;
        String callSign = (selectedVessel.callSign == null)
                ? "Not Available"
                : selectedVessel.callSign;
        String imo = (selectedVessel.imo == null)
                ? "Not Available"
                : "" + selectedVessel.imo;
        String shipType = (selectedVessel.callSign == null)
                ? "Not Available"
                : selectedVessel.shipType;
        String shipCategory = Utils.getShipCategory(selectedVessel.shipType);
        String length = (selectedVessel.shipLength == null
                || selectedVessel.shipLength == 0)
                        ? "Not Avaiable"
                        : String.format("%dm", selectedVessel.shipLength);
        String width = (selectedVessel.shipWidth == null
                || selectedVessel.shipWidth == 0)
                        ? "Not Available"
                        : String.format("%dm", selectedVessel.shipWidth);
        String toBow = (selectedVessel.toBow == null
                || selectedVessel.toBow == 0)
                        ? "--"
                        : String.format("%dm", selectedVessel.toBow);
        String toStern = (selectedVessel.toStern == null
                || selectedVessel.toStern == 0)
                        ? "--"
                        : String.format("%dm", selectedVessel.toStern);
        String bowStern = String.format("%s, %s", toBow, toStern);
        String toStarboard = (selectedVessel.toStarboard == null
                || selectedVessel.toStarboard == 0)
                        ? "--"
                        : String.format("%dm", selectedVessel.toStarboard);
        String toPort = (selectedVessel.toPort == null
                || selectedVessel.toPort == 0)
                        ? "--"
                        : String.format("%dm", selectedVessel.toPort);
        String starboardPort = String.format("%s, %s", toStarboard, toPort);

        Object staticValues[] =
        {
            mmsi,
            shipName,
            callSign,
            imo,
            shipType,
            shipCategory,
            length,
            width,
            bowStern,
            starboardPort
        };
        for (int i = 0; i < model.getRowCount(); i++)
        {
            model.setValueAt(staticValues[i], i, 1);
        }
        staticDataTable.repaint();

        model = (DefaultTableModel) dynamicDataTable.getModel();
        Date datetime = new Date(selectedVessel.last().timestamp * 1000);
        SimpleDateFormat dateFormat
                = new SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Object timestamp = dateFormat.format(datetime);
        Object navStatus = (selectedVessel.navStatus == null)
                ? "Not Available"
                : selectedVessel.navStatus;
        Object destination = (selectedVessel.destination == null
                || selectedVessel.destination.equals(""))
                ? "Not Available"
                : selectedVessel.destination;
        Object eta = (selectedVessel.eta == null)
                ? "Not Available"
                : selectedVessel.eta;
        Object lat = String.format("%.4f%s", selectedVessel.last().lat, Utils.DEGREE);
        Object lon = String.format("%.4f%s", selectedVessel.last().lon, Utils.DEGREE);
        Object sog = String.format("%.2fkn", selectedVessel.last().sog);
        Object cog = String.format("%.2f%s", selectedVessel.last().cog, Utils.DEGREE);
        Object draught = String.format("%.2fm", selectedVessel.draught);
        Object isAnomalous = selectedVessel.last().anomalous;
        Object dynamicValues[] =
        {
            timestamp,
            navStatus,
            destination,
            eta,
            lat,
            lon,
            sog,
            cog,
            draught,
            isAnomalous
        };
        for (int i = 0; i < model.getRowCount(); i++)
        {
            model.setValueAt(dynamicValues[i], i, 1);
        }
        dynamicDataTable.repaint();

        model = (DefaultTableModel) latestPositionsTable.getModel();
        removeAllRows(model);
        for (AISPoint point : selectedVessel.track)
        {
            datetime = new Date(point.timestamp * 1000);
            dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss z");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Object row[] =
            {
                point.lat,
                point.lon,
                point.sog,
                point.cog,
                dateFormat.format(datetime)
            };
            model.addRow(row);
        }
        latestPositionsTable.repaint();

        model = (DefaultTableModel) nearbyVesselsTable.getModel();
        removeAllRows(model);
        ConcurrentLinkedQueue<Vessel> vessels = routePainter.getVessels();
        List<Object[]> rows = new ArrayList<>();
        for (Vessel vessel : vessels)
        {
            float distanceTo = selectedVessel.distanceTo(vessel);
            System.out.println(distanceTo);
            if (selectedVessel.mmsi != vessel.mmsi && distanceTo < 20)
            {
                AISPoint point = vessel.last();
                Object row[] =
                {
                    vessel.mmsi,
                    vessel.shipName,
                    point.lat,
                    point.lon,
                    point.sog,
                    point.cog,
                    distanceTo,
                    "Possible Collision"
                };
                rows.add(row);
            }
        }
        rows.sort(new Comparator()
        {
            @Override
            public int compare(Object o1, Object o2)
            {
                Object row1[] = (Object[]) o1;
                Object row2[] = (Object[]) o2;
                Float distanceTo1 = (Float) row1[6];
                Float distanceTo2 = (Float) row2[6];

                return distanceTo1.compareTo(distanceTo2);
            }
        });
        for (Object row[] : rows)
        {
            model.addRow(row);
        }
        nearbyVesselsTable.repaint();

        speedLineData.clear();
        int numPoints = selectedVessel.track.size();
        int start = Math.max(0, numPoints - 30);
        for (int i = start; i < numPoints; i++)
        {
            AISPoint point = selectedVessel.track.get(i);
            speedLineData.addValue(point.sog, "Vessel",
                    new Double(point.timestamp));
        }
        speedLineChartPanel.repaint();
    }

    private void removeAllRows(DefaultTableModel model)
    {
        for (int i = model.getRowCount() - 1; i >= 0; i--)
        {
            model.removeRow(i);
        }
    }

    private class BoldTableCellRenderer extends DefaultTableCellRenderer
    {
        public BoldTableCellRenderer()
        {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable tblData,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column)
        {
            super.getTableCellRendererComponent(tblData, value, isSelected,
                    hasFocus, row, column);
            Component cellComponent = super.getTableCellRendererComponent(
                    tblData, value, isSelected, hasFocus, row, column);
            cellComponent.setFont(cellComponent.getFont().deriveFont(Font.BOLD));
            return cellComponent;
        }
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

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane6 = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        staticDataTable = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        dynamicDataTable = new javax.swing.JTable();
        jTabbedPane3 = new javax.swing.JTabbedPane();
        speedLineChartPanel = new com.nuwc.interestengine.gui.JChartPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        latestPositionsTable = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        nearbyVesselsTable = new javax.swing.JTable();

        setPreferredSize(new java.awt.Dimension(970, 720));

        jTabbedPane1.setPreferredSize(new java.awt.Dimension(950, 700));

        jPanel3.setPreferredSize(new java.awt.Dimension(900, 625));
        jPanel3.setLayout(new java.awt.GridLayout(2, 1));

        jPanel2.setLayout(new java.awt.GridLayout(1, 2));

        jPanel15.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel15.setLayout(new java.awt.BorderLayout());

        jLabel3.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Static Data");
        jPanel15.add(jLabel3, java.awt.BorderLayout.CENTER);

        staticDataTable.setTableHeader(null);
        staticDataTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {"MMSI", null},
                {"Ship Name", null},
                {"Call Sign", null},
                {"IMO", null},
                {"Ship Type", null},
                {"Ship Category", null},
                {"Length", null},
                {"Width", null},
                {"Bow, Stern", null},
                {"Starboard, Port", null}
            },
            new String []
            {
                "Attribute", "Value"
            }
        )
        {
            boolean[] canEdit = new boolean []
            {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        staticDataTable.setFillsViewportHeight(true);
        staticDataTable.setRowHeight(20);
        staticDataTable.setRowSelectionAllowed(false);
        jScrollPane4.setViewportView(staticDataTable);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 461, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel2.add(jPanel4);

        jPanel16.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel16.setLayout(new java.awt.BorderLayout());

        jLabel4.setBackground(new java.awt.Color(255, 255, 255));
        jLabel4.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Dynamic Data");
        jPanel16.add(jLabel4, java.awt.BorderLayout.CENTER);

        dynamicDataTable.setTableHeader(null);
        dynamicDataTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {"Last Timestamp", null},
                {"Navigation Status", null},
                {"Reported Destination", null},
                {"ETA", null},
                {"Latitude", null},
                {"Longitude", null},
                {"Speed", null},
                {"Course", null},
                {"Draught", null},
                {"Anomalous", null}
            },
            new String []
            {
                "Attribute", "Value"
            }
        )
        {
            boolean[] canEdit = new boolean []
            {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        dynamicDataTable.setFillsViewportHeight(true);
        dynamicDataTable.setRowHeight(20);
        dynamicDataTable.setRowSelectionAllowed(false);
        jScrollPane5.setViewportView(dynamicDataTable);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 461, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel2.add(jPanel5);

        jPanel3.add(jPanel2);

        javax.swing.GroupLayout speedLineChartPanelLayout = new javax.swing.GroupLayout(speedLineChartPanel);
        speedLineChartPanel.setLayout(speedLineChartPanelLayout);
        speedLineChartPanelLayout.setHorizontalGroup(
            speedLineChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 958, Short.MAX_VALUE)
        );
        speedLineChartPanelLayout.setVerticalGroup(
            speedLineChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 317, Short.MAX_VALUE)
        );

        jTabbedPane3.addTab("Speed", speedLineChartPanel);

        jPanel3.add(jTabbedPane3);

        jScrollPane6.setViewportView(jPanel3);

        jTabbedPane1.addTab("Vessel Details", jScrollPane6);

        latestPositionsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {

            },
            new String []
            {
                "Latitude", "Longitude", "Speed", "Course", "Timestamp"
            }
        ));
        latestPositionsTable.setFillsViewportHeight(true);
        jScrollPane2.setViewportView(latestPositionsTable);

        jTabbedPane1.addTab("Lastest Positions", jScrollPane2);

        nearbyVesselsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {

            },
            new String []
            {
                "MMSI", "Name", "Latitude", "Longitude", "Speed", "Course (\u00b0)", "Distance To (NM)", "Course Intersection In..."
            }
        )
        {
            boolean[] canEdit = new boolean []
            {
                false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        nearbyVesselsTable.setFillsViewportHeight(true);
        jScrollPane3.setViewportView(nearbyVesselsTable);

        jTabbedPane1.addTab("Nearby Vessels", jScrollPane3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable dynamicDataTable;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane3;
    private javax.swing.JTable latestPositionsTable;
    private javax.swing.JTable nearbyVesselsTable;
    private com.nuwc.interestengine.gui.JChartPanel speedLineChartPanel;
    private javax.swing.JTable staticDataTable;
    // End of variables declaration//GEN-END:variables
}
