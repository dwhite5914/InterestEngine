package com.nuwc.interestengine.gui;

import com.nuwc.interestengine.Utils;
import com.nuwc.interestengine.data.Database;
import com.nuwc.interestengine.clustering.RouteExtractor;
import com.nuwc.interestengine.clustering.RouteObject;
import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.data.AISPoint;
import com.nuwc.interestengine.map.RoutePainter;
import com.nuwc.interestengine.parser.NMEAParser;
import com.nuwc.interestengine.map.Ship;
import com.nuwc.interestengine.map.TriMarker;
import com.nuwc.interestengine.neuralnet.NeuralNet;
import com.nuwc.interestengine.simulation.Simulation;
import com.nuwc.interestengine.simulation.SimulationChangeListener;
import com.nuwc.interestengine.simulation.SimulationState;
import com.nuwc.interestengine.simulator.MessageConsumer;
import com.nuwc.interestengine.simulator.MessageProducer;
import com.nuwc.interestengine.simulator.VesselManager;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.viewer.GeoPosition;

public class OptionsPanel extends javax.swing.JPanel
{
    private final List<Ship> ships;
    private final Simulation simulation;
    private final List<TriMarker> markers;
    private final RoutePainter routePainter;
    private final MainFrame mainFrame;
    private final Database db;

    private SelectionPanel selectionPanel;
    private VesselFrame vesselFrame;

    private RouteExtractor extractor;
    private VesselManager manager;
    private MessageProducer producer;
    private MessageConsumer consumer;

    private JFreeChart pieChart;
    private DefaultPieDataset pieData;

    private Color colorClusters = null;
    private Color colorRoutes = null;
    private Color colorDataPoints = null;
    private Color colorEntryPoints = null;
    private Color colorExitPoints = null;
    private Color colorStopPoints = null;

    private List<RouteObject> routes = null;
    private List<RouteObject> oversampledRoutes = null;
    private NeuralNet network = null;

    private JXMapKit miniMap;
    private boolean modifyingMiniMap = false;

    public OptionsPanel(List<Ship> ships, Simulation simulation,
            List<TriMarker> markers, RoutePainter routePainter,
            JXMapKit miniMap, MainFrame mainFrame)
    {
        this.ships = ships;
        this.simulation = simulation;
        this.markers = markers;
        this.routePainter = routePainter;
        this.mainFrame = mainFrame;
        this.db = new Database();
        this.miniMap = miniMap;

        loadData();
        initListeners();
        initComponents();
        initTweaks();

        updateTables();
    }

    public RouteExtractor getExtractor()
    {
        return extractor;
    }

    public SelectionPanel getSelectionPanel()
    {
        return selectionPanel;
    }

    public void setSelectionPanel(SelectionPanel selectionPanel)
    {
        this.selectionPanel = selectionPanel;
    }

    public VesselFrame getVesselFrame()
    {
        return vesselFrame;
    }

    public void setVesselFrame(VesselFrame vesselFrame)
    {
        this.vesselFrame = vesselFrame;
    }

    public NeuralNet getNeuralNet()
    {
        return network;
    }

    public List<RouteObject> getRoutes()
    {
        return routes;
    }

    private void loadData()
    {

    }

    public float getMinLat()
    {
        return ((Double) minLatSpinner.getValue()).floatValue();
    }

    public float getMaxLat()
    {
        return ((Double) maxLatSpinner.getValue()).floatValue();
    }

    public float getMinLon()
    {
        return ((Double) minLonSpinner.getValue()).floatValue();
    }

    public float getMaxLon()
    {
        return ((Double) maxLonSpinner.getValue()).floatValue();
    }

    private void initListeners()
    {
        simulation.addSimulationChangeListener(new StateChangeListener());
    }

    private void initTweaks()
    {
        pieData = new DefaultPieDataset();

        pieChart = ChartFactory.createPieChart(
                "Ship Types",
                pieData,
                true,
                false,
                false
        );
        pieChart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        pieChart.setBackgroundPaint(new Color(240, 240, 240));

        PiePlot plot = (PiePlot) pieChart.getPlot();
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.setNoDataMessage("No data available");
        plot.setCircular(true);
        plot.setLabelGap(0.02);
        plot.setBackgroundPaint(new Color(240, 240, 240));
        plot.setOutlineVisible(false);
        plot.setLabelGenerator(null);

        shipTypesChart.setChart(pieChart);
        shipTypesChart.repaint();

        updateMiniMap();
    }

    private void updateMiniMap()
    {
        float minLat = ((Double) minLatSpinner.getValue()).floatValue();
        float maxLat = ((Double) maxLatSpinner.getValue()).floatValue();
        float minLon = ((Double) minLonSpinner.getValue()).floatValue();
        float maxLon = ((Double) maxLonSpinner.getValue()).floatValue();
        GeoPosition start = new GeoPosition(maxLat, minLon);
        GeoPosition end = new GeoPosition(minLat, maxLon);
        miniMapPanel.setRegion(start, end);
    }

    public boolean isModifyingMiniMap()
    {
        return modifyingMiniMap;
    }

    public void setModifyingMiniMap(boolean modifyingMiniMap)
    {
        this.modifyingMiniMap = modifyingMiniMap;
    }

    private ImageIcon getPlayIcon()
    {
        String path = "/com/nuwc/interestengine/resources/gui/route-play.png";
        return new ImageIcon(getClass().getResource(path));
    }

    private ImageIcon getPauseIcon()
    {
        String path = "/com/nuwc/interestengine/resources/gui/route-pause.png";
        return new ImageIcon(getClass().getResource(path));
    }

    private ImageIcon getStopIcon()
    {
        String path = "/com/nuwc/interestengine/resources/gui/route-stop.png";
        return new ImageIcon(getClass().getResource(path));
    }

    public void updateChart(RoutePainter painter)
    {
        ConcurrentLinkedQueue<Vessel> vessels = painter.getVessels();
        numActiveVesselsField.setText("" + vessels.size());

        for (Vessel vessel : vessels)
        {
            String shipCategory = Utils.getShipCategory(vessel.shipType);
            if (pieData.getIndex(shipCategory) == -1)
            {
                pieData.setValue(shipCategory, 1);
                PiePlot plot = (PiePlot) pieChart.getPlot();
                plot.setSectionPaint(shipCategory,
                        Utils.getShipColor(shipCategory));
            }
            else
            {
                pieData.setValue(shipCategory,
                        pieData.getValue(shipCategory).intValue() + 1);
            }
        }
        shipTypesChart.repaint();
    }

    public void checkAnomaly(Vessel vessel)
    {
        AISPoint point = vessel.last();
        point.anomalous = selectionPanel.isAnomaly(vessel);
    }

    public void interruptSimulation()
    {
        if (simulation.getState() != SimulationState.STOPPED)
        {
            simulation.stop();
        }

        updatePanel();
    }

    public synchronized void updatePanel()
    {
//        SimulationState state = simulation.getState();
//        if (ships.isEmpty())
//        {
//            playPauseButton.setIcon(getPlayIcon());
//            playPauseButton.setText("Play");
//            stopButton.setEnabled(false);
//            playPauseButton.setEnabled(false);
//        }
//        else if (state == SimulationState.STOPPED)
//        {
//            playPauseButton.setIcon(getPlayIcon());
//            playPauseButton.setText("Play");
//            stopButton.setEnabled(false);
//            playPauseButton.setEnabled(true);
//        }
//        else if (state == SimulationState.PAUSED)
//        {
//            playPauseButton.setIcon(getPlayIcon());
//            playPauseButton.setText("Play");
//            stopButton.setEnabled(true);
//            playPauseButton.setEnabled(true);
//        }
//        else
//        {
//            playPauseButton.setIcon(getPauseIcon());
//            playPauseButton.setText("Pause");
//            stopButton.setEnabled(true);
//            playPauseButton.setEnabled(true);
//        }
    }

    public void updateInfo()
    {
        vesselFrame.updateTables();
    }

    private void updateTables()
    {
        if (db.exists())
        {
            DefaultTableModel model;
            model = (DefaultTableModel) vesselDataTable.getModel();
            deleteRows(model);
            List<Object[]> points = db.getTableData();
            System.out.println(db.getTableData().size());
            for (Object[] point : points)
            {
                model.addRow(point);
            }

            numAISPointsField.setText("" + points.size());
            HashMap<Integer, Boolean> vessels = new HashMap<>();
            for (Object[] point : points)
            {
                Integer mmsi = (Integer) point[1];
                if (!vessels.containsKey(mmsi))
                {
                    vessels.put(mmsi, true);
                }
            }
            numVesselsField.setText("" + vessels.size());
        }
    }

    private void deleteRows(DefaultTableModel model)
    {
        for (int i = model.getRowCount() - 1; i >= 0; i--)
        {
            model.removeRow(i);
        }
    }

    private void colorDialog(Color color, Color defaultColor, JPanel colorBox,
            String name)
    {
        Color c;
        if (color == null)
        {
            c = defaultColor;
        }
        else
        {
            c = color;
        }
        Color newColor = JColorChooser.showDialog(this, "Pick a Color", c);

        if (newColor != null && newColor != color)
        {
            color = newColor;
            colorBox.setBackground(color);
            switch (name)
            {
                case "clusters":
                    routePainter.setClusterColor(color);
                    break;
                case "routes":
                    routePainter.setRouteColor(color);
                    break;
                case "dataPoints":
                    routePainter.setDataPointColor(color);
                    break;
                case "entryPoints":
                    routePainter.setEntryPointColor(color);
                    break;
                case "exitPoints":
                    routePainter.setExitPointColor(color);
                    break;
                case "stopPoints":
                    routePainter.setStopPointColor(color);
                    break;
            }
        }
    }

    private class StateChangeListener implements SimulationChangeListener
    {
        public StateChangeListener()
        {
        }

        @Override
        public void stateChanged()
        {
            updatePanel();
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
        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        databaseAccordion = new com.nuwc.interestengine.gui.FoldablePanel();
        jPanel4 = new javax.swing.JPanel();
        loadDataButton = new javax.swing.JButton();
        numAISPointsLabel = new javax.swing.JLabel();
        numAISPointsField = new javax.swing.JTextField();
        numVesselsLabel = new javax.swing.JLabel();
        numVesselsField = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jTextField17 = new javax.swing.JTextField();
        minLatSpinner = new javax.swing.JSpinner();
        jTextField18 = new javax.swing.JTextField();
        maxLatSpinner = new javax.swing.JSpinner();
        jTextField19 = new javax.swing.JTextField();
        minLonSpinner = new javax.swing.JSpinner();
        jTextField20 = new javax.swing.JTextField();
        maxLonSpinner = new javax.swing.JSpinner();
        miniMapPanel = new com.nuwc.interestengine.gui.MapPanel();
        foldablePanel1 = new com.nuwc.interestengine.gui.FoldablePanel();
        jPanel10 = new javax.swing.JPanel();
        vesselDataScroller = new javax.swing.JScrollPane();
        vesselDataTable = new javax.swing.JTable();
        jPanel13 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel5 = new javax.swing.JPanel();
        foldablePanel3 = new com.nuwc.interestengine.gui.FoldablePanel();
        jPanel7 = new javax.swing.JPanel();
        trainNetworkButton = new javax.swing.JButton();
        AnalysisSettingsPanel2 = new javax.swing.JPanel();
        jTextField10 = new javax.swing.JTextField();
        epochsField = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        batchSizeField = new javax.swing.JTextField();
        jTextField12 = new javax.swing.JTextField();
        learningRateField = new javax.swing.JTextField();
        jTextField13 = new javax.swing.JTextField();
        jTextField14 = new javax.swing.JTextField();
        foldablePanel5 = new com.nuwc.interestengine.gui.FoldablePanel();
        analysisPanel = new javax.swing.JPanel();
        extractRoutesButton = new javax.swing.JButton();
        numClustersLabel = new javax.swing.JLabel();
        numClustersField = new javax.swing.JTextField();
        numRoutesLabel = new javax.swing.JLabel();
        numRoutesField = new javax.swing.JTextField();
        AnalysisSettingsPanel = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        minSpeedField = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        lostTimeField = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        entryEpsilonField = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        entryMinPointsField = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        stopEpsilonField = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        stopMinPointsField = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        exitEpsilonField = new javax.swing.JTextField();
        jTextField8 = new javax.swing.JTextField();
        exitMinPointsField = new javax.swing.JTextField();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0));
        jPanel14 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jPanel12 = new javax.swing.JPanel();
        foldablePanel4 = new com.nuwc.interestengine.gui.FoldablePanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jTextField9 = new javax.swing.JTextField();
        jComboBox3 = new javax.swing.JComboBox<>();
        jColorButton5 = new com.nuwc.interestengine.gui.JColorButton();
        jTextField22 = new javax.swing.JTextField();
        jComboBox4 = new javax.swing.JComboBox<>();
        jColorButton2 = new com.nuwc.interestengine.gui.JColorButton();
        jTextField23 = new javax.swing.JTextField();
        jCheckBox4 = new javax.swing.JCheckBox();
        jColorButton6 = new com.nuwc.interestengine.gui.JColorButton();
        jTextField25 = new javax.swing.JTextField();
        jCheckBox5 = new javax.swing.JCheckBox();
        jColorButton7 = new com.nuwc.interestengine.gui.JColorButton();
        jTextField27 = new javax.swing.JTextField();
        jCheckBox6 = new javax.swing.JCheckBox();
        jColorButton1 = new com.nuwc.interestengine.gui.JColorButton();
        jTextField29 = new javax.swing.JTextField();
        jCheckBox7 = new javax.swing.JCheckBox();
        jColorButton3 = new com.nuwc.interestengine.gui.JColorButton();
        foldablePanel2 = new com.nuwc.interestengine.gui.FoldablePanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        shipTypesCheckBoxTree = new com.nuwc.interestengine.gui.JCheckBoxTree();
        jPanel15 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jPanel6 = new javax.swing.JPanel();
        aisSimulationAccordion = new com.nuwc.interestengine.gui.FoldablePanel();
        jPanel2 = new javax.swing.JPanel();
        runSimulationButton = new javax.swing.JButton();
        numActiveVesselsLabel = new javax.swing.JLabel();
        numActiveVesselsField = new javax.swing.JTextField();
        simulationRateSlider = new javax.swing.JSlider();
        simulationRateValueLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        shipTypesChart = new com.nuwc.interestengine.gui.JChartPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jPanel11 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();

        setMinimumSize(new java.awt.Dimension(500, 300));
        setPreferredSize(new java.awt.Dimension(500, 800));

        jTabbedPane1.setPreferredSize(new java.awt.Dimension(720, 1080));

        jScrollPane2.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setPreferredSize(new java.awt.Dimension(600, 1837));

        jPanel3.setPreferredSize(new java.awt.Dimension(500, 2073));

        databaseAccordion.setTitle("Database");
        databaseAccordion.setTitle("Target Region");

        loadDataButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/add_icon.png"))); // NOI18N
        loadDataButton.setText("Load Data");
        loadDataButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                loadDataButtonActionPerformed(evt);
            }
        });

        numAISPointsLabel.setText("Number of Messages:");

        numAISPointsField.setEditable(false);

        numVesselsLabel.setText("Number of Vessels:");

        numVesselsField.setEditable(false);
        numVesselsField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                numVesselsFieldActionPerformed(evt);
            }
        });

        jPanel1.setLayout(new java.awt.GridLayout(4, 4));

        jTextField17.setEditable(false);
        jTextField17.setBackground(new java.awt.Color(255, 255, 255));
        jTextField17.setText("Min Lat.");
        jPanel1.add(jTextField17);

        minLatSpinner.setModel(new SpinnerNumberModel(42, -180, 180, 0.5));
        minLatSpinner.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                minLatSpinnerStateChanged(evt);
            }
        });
        jPanel1.add(minLatSpinner);

        jTextField18.setEditable(false);
        jTextField18.setBackground(new java.awt.Color(255, 255, 255));
        jTextField18.setText("Max Lat.");
        jPanel1.add(jTextField18);

        maxLatSpinner.setModel(new SpinnerNumberModel(47, -180, 180, 0.5));
        maxLatSpinner.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                maxLatSpinnerStateChanged(evt);
            }
        });
        jPanel1.add(maxLatSpinner);

        jTextField19.setEditable(false);
        jTextField19.setBackground(new java.awt.Color(255, 255, 255));
        jTextField19.setText("Min Lon.");
        jPanel1.add(jTextField19);

        minLonSpinner.setModel(new SpinnerNumberModel(10, -180, 180, 0.5));
        minLonSpinner.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                minLonSpinnerStateChanged(evt);
            }
        });
        jPanel1.add(minLonSpinner);

        jTextField20.setEditable(false);
        jTextField20.setBackground(new java.awt.Color(255, 255, 255));
        jTextField20.setText("Max Lon.");
        jPanel1.add(jTextField20);

        maxLonSpinner.setModel(new SpinnerNumberModel(18, -180, 180, 0.5));
        maxLonSpinner.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                maxLonSpinnerStateChanged(evt);
            }
        });
        jPanel1.add(maxLonSpinner);

        miniMapPanel.setMap(miniMap, minLatSpinner, minLonSpinner,
            maxLatSpinner, maxLonSpinner, this);
        miniMapPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(loadDataButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 306, Short.MAX_VALUE)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numAISPointsLabel)
                            .addComponent(numVesselsLabel))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numAISPointsField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(numVesselsField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(miniMapPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(numAISPointsLabel)
                            .addComponent(numAISPointsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(numVesselsLabel)
                            .addComponent(numVesselsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(loadDataButton, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(miniMapPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        databaseAccordion.add(jPanel4, java.awt.BorderLayout.CENTER);

        foldablePanel1.setTitle("Parsed Data");

        vesselDataTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {

            },
            new String []
            {
                "ID", "MMSI", "Lat", "Lon", "SOG", "COG", "Ship Type", "Timestamp"
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
        vesselDataTable.setFillsViewportHeight(true);
        vesselDataScroller.setViewportView(vesselDataTable);

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(vesselDataScroller, javax.swing.GroupLayout.DEFAULT_SIZE, 658, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(vesselDataScroller, javax.swing.GroupLayout.DEFAULT_SIZE, 290, Short.MAX_VALUE)
                .addContainerGap())
        );

        foldablePanel1.add(jPanel10, java.awt.BorderLayout.CENTER);

        jPanel13.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel13.setLayout(new java.awt.BorderLayout());

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Database");
        jPanel13.add(jLabel1, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(foldablePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(databaseAccordion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(databaseAccordion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(foldablePanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 332, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1242, Short.MAX_VALUE))
        );

        jScrollPane2.setViewportView(jPanel3);

        jTabbedPane1.addTab("", new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/2017-04-17_02h30_26.png")), jScrollPane2, "Database"); // NOI18N

        jScrollPane1.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(600, 2058));

        jPanel5.setPreferredSize(new java.awt.Dimension(500, 2056));

        foldablePanel3.setTitle("Kernel Density Estimation");

        trainNetworkButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/convert_icon.png"))); // NOI18N
        trainNetworkButton.setText("Train Estimator");
        trainNetworkButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                trainNetworkButtonActionPerformed(evt);
            }
        });

        AnalysisSettingsPanel2.setLayout(new java.awt.GridLayout(4, 2));

        jTextField10.setEditable(false);
        jTextField10.setBackground(new java.awt.Color(255, 255, 255));
        jTextField10.setText("Forgetting Factor");
        AnalysisSettingsPanel2.add(jTextField10);

        epochsField.setText("1");
        epochsField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                epochsFieldActionPerformed(evt);
            }
        });
        AnalysisSettingsPanel2.add(epochsField);

        jTextField11.setEditable(false);
        jTextField11.setBackground(new java.awt.Color(255, 255, 255));
        jTextField11.setText("Compression Threshold");
        AnalysisSettingsPanel2.add(jTextField11);

        batchSizeField.setText("0.02");
        batchSizeField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                batchSizeFieldActionPerformed(evt);
            }
        });
        AnalysisSettingsPanel2.add(batchSizeField);

        jTextField12.setEditable(false);
        jTextField12.setBackground(new java.awt.Color(255, 255, 255));
        jTextField12.setText("Number of Samples");
        AnalysisSettingsPanel2.add(jTextField12);

        learningRateField.setText("100");
        AnalysisSettingsPanel2.add(learningRateField);

        jTextField13.setEditable(false);
        jTextField13.setBackground(new java.awt.Color(255, 255, 255));
        jTextField13.setText("Neighborhood Radius");
        AnalysisSettingsPanel2.add(jTextField13);

        jTextField14.setEditable(false);
        jTextField14.setBackground(new java.awt.Color(255, 255, 255));
        jTextField14.setText("1.5");
        jTextField14.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jTextField14ActionPerformed(evt);
            }
        });
        AnalysisSettingsPanel2.add(jTextField14);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(trainNetworkButton)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(AnalysisSettingsPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 658, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(trainNetworkButton, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(AnalysisSettingsPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        foldablePanel3.add(jPanel7, java.awt.BorderLayout.CENTER);

        foldablePanel5.setTitle("Route Extraction");

        extractRoutesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/output_icon.png"))); // NOI18N
        extractRoutesButton.setText("Extract Routes");
        extractRoutesButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                extractRoutesButtonActionPerformed(evt);
            }
        });

        numClustersLabel.setText("Number of Clusters:");

        numClustersField.setEditable(false);

        numRoutesLabel.setText("Number of Routes:");

        numRoutesField.setEditable(false);

        AnalysisSettingsPanel.setLayout(new java.awt.GridLayout(8, 2));

        jTextField1.setEditable(false);
        jTextField1.setBackground(new java.awt.Color(255, 255, 255));
        jTextField1.setText("Min Speed");
        AnalysisSettingsPanel.add(jTextField1);

        minSpeedField.setText("1");
        AnalysisSettingsPanel.add(minSpeedField);

        jTextField2.setEditable(false);
        jTextField2.setBackground(new java.awt.Color(255, 255, 255));
        jTextField2.setText("Lost Time");
        AnalysisSettingsPanel.add(jTextField2);

        lostTimeField.setText("100");
        AnalysisSettingsPanel.add(lostTimeField);

        jTextField3.setEditable(false);
        jTextField3.setBackground(new java.awt.Color(255, 255, 255));
        jTextField3.setText("Entry Epsilon");
        AnalysisSettingsPanel.add(jTextField3);

        entryEpsilonField.setText("3");
        entryEpsilonField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                entryEpsilonFieldActionPerformed(evt);
            }
        });
        AnalysisSettingsPanel.add(entryEpsilonField);

        jTextField4.setEditable(false);
        jTextField4.setBackground(new java.awt.Color(255, 255, 255));
        jTextField4.setText("Entry Min Points");
        AnalysisSettingsPanel.add(jTextField4);

        entryMinPointsField.setText("5");
        AnalysisSettingsPanel.add(entryMinPointsField);

        jTextField5.setEditable(false);
        jTextField5.setBackground(new java.awt.Color(255, 255, 255));
        jTextField5.setText("Stop Epsilon");
        AnalysisSettingsPanel.add(jTextField5);

        stopEpsilonField.setText("3");
        AnalysisSettingsPanel.add(stopEpsilonField);

        jTextField6.setEditable(false);
        jTextField6.setBackground(new java.awt.Color(255, 255, 255));
        jTextField6.setText("Stop Min Points");
        AnalysisSettingsPanel.add(jTextField6);

        stopMinPointsField.setText("5");
        AnalysisSettingsPanel.add(stopMinPointsField);

        jTextField7.setEditable(false);
        jTextField7.setBackground(new java.awt.Color(255, 255, 255));
        jTextField7.setText("Exit Epsilon");
        AnalysisSettingsPanel.add(jTextField7);

        exitEpsilonField.setText("3");
        AnalysisSettingsPanel.add(exitEpsilonField);

        jTextField8.setEditable(false);
        jTextField8.setBackground(new java.awt.Color(255, 255, 255));
        jTextField8.setText("Exit Min Points");
        AnalysisSettingsPanel.add(jTextField8);

        exitMinPointsField.setText("5");
        AnalysisSettingsPanel.add(exitMinPointsField);

        javax.swing.GroupLayout analysisPanelLayout = new javax.swing.GroupLayout(analysisPanel);
        analysisPanel.setLayout(analysisPanelLayout);
        analysisPanelLayout.setHorizontalGroup(
            analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(analysisPanelLayout.createSequentialGroup()
                        .addComponent(extractRoutesButton)
                        .addGap(18, 317, Short.MAX_VALUE)
                        .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numClustersLabel)
                            .addComponent(numRoutesLabel))
                        .addGap(18, 18, 18)
                        .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numClustersField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(numRoutesField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(AnalysisSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        analysisPanelLayout.setVerticalGroup(
            analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(analysisPanelLayout.createSequentialGroup()
                        .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(numClustersLabel)
                            .addComponent(numClustersField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(numRoutesLabel)
                            .addComponent(numRoutesField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(extractRoutesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(AnalysisSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        foldablePanel5.add(analysisPanel, java.awt.BorderLayout.CENTER);

        jPanel14.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel14.setLayout(new java.awt.BorderLayout());

        jLabel2.setBackground(new java.awt.Color(255, 255, 255));
        jLabel2.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Analysis");
        jPanel14.add(jLabel2, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(foldablePanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(foldablePanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(foldablePanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(foldablePanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1543, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel5);

        jTabbedPane1.addTab("", new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/2017-04-17_02h31_01.png")), jScrollPane1, "Analysis"); // NOI18N

        jScrollPane5.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane5.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane5.setPreferredSize(new java.awt.Dimension(600, 2058));

        jPanel12.setPreferredSize(new java.awt.Dimension(500, 2056));

        foldablePanel4.setTitle("Display Filters");

        jPanel9.setLayout(new java.awt.GridLayout(6, 3));

        jTextField9.setText("Clusters");
        jPanel9.add(jTextField9);

        jComboBox3.setBorder(Utils.getTableBorder());
        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "Single Color", "Rainbow", "With Points", "Connected" }));
        jComboBox3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jComboBox3ActionPerformed(evt);
            }
        });
        jPanel9.add(jComboBox3);

        jColorButton5.setRoutePainter(routePainter);
        jColorButton5.setFillColor(new java.awt.Color(255, 0, 0));
        jColorButton5.setObjectName(com.nuwc.interestengine.gui.MapObject.Clusters);
        jPanel9.add(jColorButton5);

        jTextField22.setText("Routes");
        jPanel9.add(jTextField22);

        jComboBox4.setBorder(Utils.getTableBorder());
        jComboBox4.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "Rainbow", "Single Color" }));
        jComboBox4.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jComboBox4ActionPerformed(evt);
            }
        });
        jPanel9.add(jComboBox4);

        jColorButton2.setRoutePainter(routePainter);
        jColorButton2.setFillColor(new java.awt.Color(0, 0, 255));
        jColorButton2.setObjectName(com.nuwc.interestengine.gui.MapObject.Routes);
        jPanel9.add(jColorButton2);

        jTextField23.setText("Data Points");
        jPanel9.add(jTextField23);

        jCheckBox4.setBorder(Utils.getTableBorder());
        jCheckBox4.setBackground(new java.awt.Color(255, 255, 255));
        jCheckBox4.setBorderPainted(true);
        jCheckBox4.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jCheckBox4ActionPerformed(evt);
            }
        });
        jPanel9.add(jCheckBox4);

        jColorButton6.setRoutePainter(routePainter);
        jColorButton6.setObjectName(com.nuwc.interestengine.gui.MapObject.DataPoints);
        jPanel9.add(jColorButton6);

        jTextField25.setText("Entry Points");
        jPanel9.add(jTextField25);

        jCheckBox5.setBorder(Utils.getTableBorder());
        jCheckBox5.setBackground(new java.awt.Color(255, 255, 255));
        jCheckBox5.setBorderPainted(true);
        jCheckBox5.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jCheckBox5ActionPerformed(evt);
            }
        });
        jPanel9.add(jCheckBox5);

        jColorButton7.setRoutePainter(routePainter);
        jColorButton7.setFillColor(new java.awt.Color(0, 255, 255));
        jColorButton7.setObjectName(com.nuwc.interestengine.gui.MapObject.EntryPoints);
        jPanel9.add(jColorButton7);

        jTextField27.setText("Exit Points");
        jPanel9.add(jTextField27);

        jCheckBox6.setBorder(Utils.getTableBorder());
        jCheckBox6.setBackground(new java.awt.Color(255, 255, 255));
        jCheckBox6.setBorderPainted(true);
        jCheckBox6.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jCheckBox6ActionPerformed(evt);
            }
        });
        jPanel9.add(jCheckBox6);

        jColorButton1.setRoutePainter(routePainter);
        jColorButton1.setFillColor(new java.awt.Color(255, 0, 255));
        jColorButton1.setObjectName(com.nuwc.interestengine.gui.MapObject.ExitPoints);
        jPanel9.add(jColorButton1);

        jTextField29.setText("Stop Points");
        jPanel9.add(jTextField29);

        jCheckBox7.setBorder(Utils.getTableBorder());
        jCheckBox7.setBackground(new java.awt.Color(255, 255, 255));
        jCheckBox7.setBorderPainted(true);
        jCheckBox7.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jCheckBox7ActionPerformed(evt);
            }
        });
        jPanel9.add(jCheckBox7);

        jColorButton3.setRoutePainter(routePainter);
        jColorButton3.setFillColor(new java.awt.Color(0, 255, 0));
        jColorButton3.setObjectName(com.nuwc.interestengine.gui.MapObject.StopPoints);
        jPanel9.add(jColorButton3);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        foldablePanel4.add(jPanel8, java.awt.BorderLayout.CENTER);

        foldablePanel2.setTitle("Ship Type");

        shipTypesCheckBoxTree.setBackground(new java.awt.Color(240, 240, 240));
        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Cargo Vessel");
        javax.swing.tree.DefaultMutableTreeNode treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Cargo Hazardous A");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Cargo Hazardous B");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Cargo Hazardous C");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Cargo Hazardous D");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Cargo Future 1");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Cargo Future 2");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Cargo Future 3");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Cargo Future 4");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Cargo No Additional Info");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Tanker");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tanker Hazardous A");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tanker Hazardous B");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tanker Hazardous C");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tanker Hazardous D");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tanker Future 1");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tanker Future 2");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tanker Future 3");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tanker Future 4");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tanker No Additional Info");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Passenger Vessel");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Passenger Hazardous A");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Passenger Hazardous B");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Passenger Hazardous C");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Passenger Hazardous D");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Passenger Future 1");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Passenger Future 2");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Passenger Future 3");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Passenger Future 4");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Passenger No Additional Info");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("High Speed Craft");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("High Speed Craft Hazardous A");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("High Speed Craft Hazardous B");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("High Speed Craft Hazardous C");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("High Speed Craft Hazardous D");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Tugs & Special Craft");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Wing In Ground");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Wing In Ground Hazardous A");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Wing In Ground Hazardous B");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Wing In Ground Hazardous C");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Wing In Ground Hazardous D");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Towing");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Large Towing");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Dredging Or Underwater Ops");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Diving Ops");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Military Ops");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Pilot Vessel");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Search And Rescue Vessel");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Tug");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Port Tender");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Anti Pollution Equipment");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Law Enforcement");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Spare Local Vessel 1");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Spare Local Vessel 2");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Medical Transport");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Ship According To RR Resolution No 18");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Fishing");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Pleasure Craft");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Sailing");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Miscellaneous");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other Hazardous A");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other Hazardous B");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other Hazardous C");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other Hazardous D");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other Future 1");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other Future 2");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other Future 3");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other Future 4");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other No Additional Info");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Other");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Unknown");
        treeNode1.add(treeNode2);
        shipTypesCheckBoxTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        shipTypesCheckBoxTree.setRootVisible(false);
        shipTypesCheckBoxTree.setScrollsOnExpand(false);
        shipTypesCheckBoxTree.addCheckChangeEventListener(new com.nuwc.interestengine.gui.JCheckBoxTree.CheckChangeEventListener()
        {
            public void checkStateChanged(com.nuwc.interestengine.gui.JCheckBoxTree.CheckChangeEvent evt)
            {
                shipTypesCheckBoxTreeCheckStateChanged(evt);
            }
        });
        jScrollPane6.setViewportView(shipTypesCheckBoxTree);

        foldablePanel2.add(jScrollPane6, java.awt.BorderLayout.CENTER);

        jPanel15.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel15.setLayout(new java.awt.BorderLayout());

        jLabel3.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Display");
        jPanel15.add(jLabel3, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(foldablePanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(foldablePanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(foldablePanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(foldablePanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1522, Short.MAX_VALUE))
        );

        jScrollPane5.setViewportView(jPanel12);

        jTabbedPane1.addTab("", new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/2017-04-17_02h31_39.png")), jScrollPane5, "Filters"); // NOI18N

        jScrollPane3.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane3.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane3.setPreferredSize(new java.awt.Dimension(600, 2058));

        jPanel6.setPreferredSize(new java.awt.Dimension(500, 2056));

        aisSimulationAccordion.setTitle("AIS Simulation");
        aisSimulationAccordion.setTitle("Vessel Data");

        runSimulationButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/route-play.png"))); // NOI18N
        runSimulationButton.setText("Run Simulation");
        runSimulationButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                runSimulationButtonActionPerformed(evt);
            }
        });

        numActiveVesselsLabel.setText("Number of Active Vessels:");

        numActiveVesselsField.setEditable(false);

        simulationRateSlider.setMajorTickSpacing(100);
        simulationRateSlider.setMinimum(1);
        simulationRateSlider.setMinorTickSpacing(1);
        simulationRateSlider.setSnapToTicks(true);
        simulationRateSlider.setValue(1);
        simulationRateSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                simulationRateSliderStateChanged(evt);
            }
        });

        simulationRateValueLabel.setText("1x");

        jLabel7.setText("Simulation Rate");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(runSimulationButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 80, Short.MAX_VALUE)
                        .addComponent(numActiveVesselsLabel)
                        .addGap(18, 18, 18)
                        .addComponent(numActiveVesselsField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(simulationRateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(simulationRateValueLabel))
                            .addComponent(jLabel7))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(runSimulationButton, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numActiveVesselsLabel)
                    .addComponent(numActiveVesselsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel7)
                .addGap(12, 12, 12)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(simulationRateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(simulationRateValueLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        aisSimulationAccordion.add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel16.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel16.setLayout(new java.awt.BorderLayout());

        jLabel4.setBackground(new java.awt.Color(255, 255, 255));
        jLabel4.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Simulation");
        jPanel16.add(jLabel4, java.awt.BorderLayout.CENTER);

        shipTypesChart.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout shipTypesChartLayout = new javax.swing.GroupLayout(shipTypesChart);
        shipTypesChart.setLayout(shipTypesChartLayout);
        shipTypesChartLayout.setHorizontalGroup(
            shipTypesChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        shipTypesChartLayout.setVerticalGroup(
            shipTypesChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 221, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(aisSimulationAccordion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(shipTypesChart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(aisSimulationAccordion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(shipTypesChart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1600, Short.MAX_VALUE))
        );

        jScrollPane3.setViewportView(jPanel6);

        jTabbedPane1.addTab("", new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/2017-04-17_02h31_44.png")), jScrollPane3, "Simulation"); // NOI18N

        jScrollPane4.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane4.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane4.setPreferredSize(new java.awt.Dimension(600, 2075));

        jPanel11.setPreferredSize(new java.awt.Dimension(500, 2056));

        jPanel17.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel17.setLayout(new java.awt.BorderLayout());

        jLabel5.setBackground(new java.awt.Color(255, 255, 255));
        jLabel5.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Fleets");
        jPanel17.add(jLabel5, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, 676, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(2007, Short.MAX_VALUE))
        );

        jScrollPane4.setViewportView(jPanel11);

        jTabbedPane1.addTab("", new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/2017-04-17_02h41_27.png")), jScrollPane4, "Fleets"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void entryEpsilonFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_entryEpsilonFieldActionPerformed
    {//GEN-HEADEREND:event_entryEpsilonFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_entryEpsilonFieldActionPerformed

    private void extractRoutesButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_extractRoutesButtonActionPerformed
    {//GEN-HEADEREND:event_extractRoutesButtonActionPerformed
        if (db.exists())
        {
            float minSpeed = Float.parseFloat(minSpeedField.getText());
            float lostTime = Float.parseFloat(lostTimeField.getText());
            float entryEpsilon = Float.parseFloat(entryEpsilonField.getText());
            float exitEpsilon = Float.parseFloat(exitEpsilonField.getText());
            float stopEpsilon = Float.parseFloat(stopEpsilonField.getText());
            int entryMinPoints = Integer.parseInt(entryMinPointsField.getText());
            int exitMinPoints = Integer.parseInt(exitMinPointsField.getText());
            int stopMinPoints = Integer.parseInt(stopMinPointsField.getText());
            float minLat = ((Double) minLatSpinner.getValue()).floatValue();
            float maxLat = ((Double) maxLatSpinner.getValue()).floatValue();
            float minLon = ((Double) minLonSpinner.getValue()).floatValue();
            float maxLon = ((Double) maxLonSpinner.getValue()).floatValue();
            extractor = new RouteExtractor(db, routePainter,
                    mainFrame, lostTime, minSpeed, entryEpsilon, entryMinPoints,
                    exitEpsilon, exitMinPoints, stopEpsilon, stopMinPoints,
                    minLat, minLon, maxLat, maxLon);
            routes = extractor.run();

            System.out.println("Actual Routes: " + routes.size());
            int i = 1;
            int numPoints = 0;
            for (RouteObject route : routes)
            {
                numPoints += route.points.size();
                System.out.println("Route " + i + ": " + route.points.size());
                i++;
            }
            System.out.println("Total points: " + numPoints);

            selectionPanel.setRouteTree(extractor.getRouteTree());
            selectionPanel.train();
            selectionPanel.evaluateAccuracy();
        }
        else
        {
            JOptionPane.showMessageDialog(this, "Database not populated!",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_extractRoutesButtonActionPerformed

    private void runSimulationButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_runSimulationButtonActionPerformed
    {//GEN-HEADEREND:event_runSimulationButtonActionPerformed
        JFileChooser fileChooser = new JFileChooser("C:\\Users\\User\\Desktop");
        fileChooser.setFileFilter(new FileNameExtensionFilter("LOG file", "log"));
        fileChooser.setMultiSelectionEnabled(true);
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            File[] files = fileChooser.getSelectedFiles();
            Arrays.sort(files, (File file1, File file2)
                    ->
            {
                String path1 = file1.getAbsolutePath();
                String path2 = file2.getAbsolutePath();
                return path1.compareTo(path2);
            });
            float minLat = ((Double) minLatSpinner.getValue()).floatValue();
            float maxLat = ((Double) maxLatSpinner.getValue()).floatValue();
            float minLon = ((Double) minLonSpinner.getValue()).floatValue();
            float maxLon = ((Double) maxLonSpinner.getValue()).floatValue();

            ConcurrentLinkedQueue messageQueue = new ConcurrentLinkedQueue();
            manager = new VesselManager(minLat, maxLat, minLon, maxLon, this, routePainter);
            producer = new MessageProducer(files, messageQueue);
            producer.setSimRate(simulationRateSlider.getValue());
            consumer = new MessageConsumer(messageQueue, manager);
            Thread producerThread = new Thread(producer);
            Thread consumerThread = new Thread(consumer);
            producerThread.start();
            consumerThread.start();
        }
    }//GEN-LAST:event_runSimulationButtonActionPerformed

    private void trainNetworkButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_trainNetworkButtonActionPerformed
    {//GEN-HEADEREND:event_trainNetworkButtonActionPerformed
        if (routes != null)
        {
            if (true)
            {
                int returnValue = JOptionPane.showConfirmDialog(this,
                        "Overwrite Neural Network?", "Confirm",
                        JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.CANCEL_OPTION
                        || returnValue == JOptionPane.CLOSED_OPTION)
                {
                    return;
                }
            }
        }
        else
        {
            JOptionPane.showMessageDialog(this, "Extract routes first!",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_trainNetworkButtonActionPerformed

    private void loadDataButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_loadDataButtonActionPerformed
    {//GEN-HEADEREND:event_loadDataButtonActionPerformed
        loadDataButton.setEnabled(false);
        extractRoutesButton.setEnabled(false);

        JFileChooser fileChooser = new JFileChooser("C:\\Users\\User\\Desktop");
        fileChooser.setFileFilter(new FileNameExtensionFilter("LOG file", "log"));
        fileChooser.setMultiSelectionEnabled(true);
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            if (db.exists())
            {
                returnValue = JOptionPane.showConfirmDialog(this,
                        "Overwrite database?", "Confirm",
                        JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.CANCEL_OPTION
                        || returnValue == JOptionPane.CLOSED_OPTION)
                {
                    loadDataButton.setEnabled(true);
                    extractRoutesButton.setEnabled(true);
                    return;
                }
            }

            File[] files = fileChooser.getSelectedFiles();
            Arrays.sort(files, (File file1, File file2)
                    ->
            {
                String path1 = file1.getAbsolutePath();
                String path2 = file2.getAbsolutePath();
                return path1.compareTo(path2);
            });
            float minLat = ((Double) minLatSpinner.getValue()).floatValue();
            float maxLat = ((Double) maxLatSpinner.getValue()).floatValue();
            float minLon = ((Double) minLonSpinner.getValue()).floatValue();
            float maxLon = ((Double) maxLonSpinner.getValue()).floatValue();
            NMEAParser parser = new NMEAParser(files, markers, db, minLat,
                    maxLat, minLon, maxLon);
            parser.parse();
            mainFrame.updateMap();
            updateTables();
        }

        loadDataButton.setEnabled(true);
        extractRoutesButton.setEnabled(true);
    }//GEN-LAST:event_loadDataButtonActionPerformed

    private void batchSizeFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_batchSizeFieldActionPerformed
    {//GEN-HEADEREND:event_batchSizeFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_batchSizeFieldActionPerformed

    private void jCheckBox4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jCheckBox4ActionPerformed
    {//GEN-HEADEREND:event_jCheckBox4ActionPerformed
        routePainter.setDrawDataPoints(jCheckBox4.isSelected());
    }//GEN-LAST:event_jCheckBox4ActionPerformed

    private void jCheckBox5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jCheckBox5ActionPerformed
    {//GEN-HEADEREND:event_jCheckBox5ActionPerformed
        routePainter.setDrawEntryPoints(jCheckBox5.isSelected());
    }//GEN-LAST:event_jCheckBox5ActionPerformed

    private void jCheckBox6ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jCheckBox6ActionPerformed
    {//GEN-HEADEREND:event_jCheckBox6ActionPerformed
        routePainter.setDrawExitPoints(jCheckBox6.isSelected());
    }//GEN-LAST:event_jCheckBox6ActionPerformed

    private void jCheckBox7ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jCheckBox7ActionPerformed
    {//GEN-HEADEREND:event_jCheckBox7ActionPerformed
        routePainter.setDrawStopPoints(jCheckBox7.isSelected());
    }//GEN-LAST:event_jCheckBox7ActionPerformed

    private void jComboBox4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jComboBox4ActionPerformed
    {//GEN-HEADEREND:event_jComboBox4ActionPerformed
        routePainter.setRoutesMode((String) jComboBox4.getSelectedItem());
    }//GEN-LAST:event_jComboBox4ActionPerformed

    private void jComboBox3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jComboBox3ActionPerformed
    {//GEN-HEADEREND:event_jComboBox3ActionPerformed
        routePainter.setClustersMode((String) jComboBox3.getSelectedItem());
    }//GEN-LAST:event_jComboBox3ActionPerformed

    private void numVesselsFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_numVesselsFieldActionPerformed
    {//GEN-HEADEREND:event_numVesselsFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_numVesselsFieldActionPerformed

    private void minLatSpinnerStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_minLatSpinnerStateChanged
    {//GEN-HEADEREND:event_minLatSpinnerStateChanged
        if (!modifyingMiniMap)
        {
            updateMiniMap();
        }
    }//GEN-LAST:event_minLatSpinnerStateChanged

    private void maxLatSpinnerStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_maxLatSpinnerStateChanged
    {//GEN-HEADEREND:event_maxLatSpinnerStateChanged
        if (!modifyingMiniMap)
        {
            updateMiniMap();
        }
    }//GEN-LAST:event_maxLatSpinnerStateChanged

    private void minLonSpinnerStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_minLonSpinnerStateChanged
    {//GEN-HEADEREND:event_minLonSpinnerStateChanged
        if (!modifyingMiniMap)
        {
            updateMiniMap();
        }
    }//GEN-LAST:event_minLonSpinnerStateChanged

    private void maxLonSpinnerStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_maxLonSpinnerStateChanged
    {//GEN-HEADEREND:event_maxLonSpinnerStateChanged
        if (!modifyingMiniMap)
        {
            updateMiniMap();
        }
    }//GEN-LAST:event_maxLonSpinnerStateChanged

    private void epochsFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_epochsFieldActionPerformed
    {//GEN-HEADEREND:event_epochsFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_epochsFieldActionPerformed

    private void jTextField14ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jTextField14ActionPerformed
    {//GEN-HEADEREND:event_jTextField14ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField14ActionPerformed

    private void simulationRateSliderStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_simulationRateSliderStateChanged
    {//GEN-HEADEREND:event_simulationRateSliderStateChanged
        int simRate = simulationRateSlider.getValue();
        producer.setSimRate(simRate);
        simulationRateValueLabel.setText(String.format("%dx", simRate));
    }//GEN-LAST:event_simulationRateSliderStateChanged

    private void shipTypesCheckBoxTreeCheckStateChanged(com.nuwc.interestengine.gui.JCheckBoxTree.CheckChangeEvent evt)//GEN-FIRST:event_shipTypesCheckBoxTreeCheckStateChanged
    {//GEN-HEADEREND:event_shipTypesCheckBoxTreeCheckStateChanged
        List<String> shipTypes = new ArrayList<>();
        for (TreePath treePath : shipTypesCheckBoxTree.getCheckedPaths())
        {
            Object components[] = treePath.getPath();
            if (components.length == 3)
            {
                String shipCategory = components[1].toString();
                String shipType = components[2].toString();
                if (shipType.equals("Other")
                        && !shipCategory.equals("Miscellaneous"))
                {
                    shipType = shipCategory;
                }
                shipTypes.add(shipType.replaceAll(" ", ""));
            }
            else if (components.length == 2)
            {
                String shipType = components[1].toString();
                if (shipType.equals("Unknown"))
                {
                    shipTypes.add("NotAvailable");
                }
                shipTypes.add(shipType.replaceAll(" ", ""));
            }
        }

        routePainter.setAllowedShipTypes(shipTypes);
    }//GEN-LAST:event_shipTypesCheckBoxTreeCheckStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AnalysisSettingsPanel;
    private javax.swing.JPanel AnalysisSettingsPanel2;
    private com.nuwc.interestengine.gui.FoldablePanel aisSimulationAccordion;
    private javax.swing.JPanel analysisPanel;
    private javax.swing.JTextField batchSizeField;
    private com.nuwc.interestengine.gui.FoldablePanel databaseAccordion;
    private javax.swing.JTextField entryEpsilonField;
    private javax.swing.JTextField entryMinPointsField;
    private javax.swing.JTextField epochsField;
    private javax.swing.JTextField exitEpsilonField;
    private javax.swing.JTextField exitMinPointsField;
    private javax.swing.JButton extractRoutesButton;
    private javax.swing.Box.Filler filler1;
    private com.nuwc.interestengine.gui.FoldablePanel foldablePanel1;
    private com.nuwc.interestengine.gui.FoldablePanel foldablePanel2;
    private com.nuwc.interestengine.gui.FoldablePanel foldablePanel3;
    private com.nuwc.interestengine.gui.FoldablePanel foldablePanel4;
    private com.nuwc.interestengine.gui.FoldablePanel foldablePanel5;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JCheckBox jCheckBox7;
    private com.nuwc.interestengine.gui.JColorButton jColorButton1;
    private com.nuwc.interestengine.gui.JColorButton jColorButton2;
    private com.nuwc.interestengine.gui.JColorButton jColorButton3;
    private com.nuwc.interestengine.gui.JColorButton jColorButton5;
    private com.nuwc.interestengine.gui.JColorButton jColorButton6;
    private com.nuwc.interestengine.gui.JColorButton jColorButton7;
    private javax.swing.JComboBox<String> jComboBox3;
    private javax.swing.JComboBox<String> jComboBox4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField14;
    private javax.swing.JTextField jTextField17;
    private javax.swing.JTextField jTextField18;
    private javax.swing.JTextField jTextField19;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField20;
    private javax.swing.JTextField jTextField22;
    private javax.swing.JTextField jTextField23;
    private javax.swing.JTextField jTextField25;
    private javax.swing.JTextField jTextField27;
    private javax.swing.JTextField jTextField29;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JTextField learningRateField;
    private javax.swing.JButton loadDataButton;
    private javax.swing.JTextField lostTimeField;
    private javax.swing.JSpinner maxLatSpinner;
    private javax.swing.JSpinner maxLonSpinner;
    private javax.swing.JSpinner minLatSpinner;
    private javax.swing.JSpinner minLonSpinner;
    private javax.swing.JTextField minSpeedField;
    private com.nuwc.interestengine.gui.MapPanel miniMapPanel;
    private javax.swing.JTextField numAISPointsField;
    private javax.swing.JLabel numAISPointsLabel;
    private javax.swing.JTextField numActiveVesselsField;
    private javax.swing.JLabel numActiveVesselsLabel;
    private javax.swing.JTextField numClustersField;
    private javax.swing.JLabel numClustersLabel;
    private javax.swing.JTextField numRoutesField;
    private javax.swing.JLabel numRoutesLabel;
    private javax.swing.JTextField numVesselsField;
    private javax.swing.JLabel numVesselsLabel;
    private javax.swing.JButton runSimulationButton;
    private com.nuwc.interestengine.gui.JChartPanel shipTypesChart;
    private com.nuwc.interestengine.gui.JCheckBoxTree shipTypesCheckBoxTree;
    private javax.swing.JSlider simulationRateSlider;
    private javax.swing.JLabel simulationRateValueLabel;
    private javax.swing.JTextField stopEpsilonField;
    private javax.swing.JTextField stopMinPointsField;
    private javax.swing.JButton trainNetworkButton;
    private javax.swing.JScrollPane vesselDataScroller;
    private javax.swing.JTable vesselDataTable;
    // End of variables declaration//GEN-END:variables
}
