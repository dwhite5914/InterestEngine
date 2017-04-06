package com.nuwc.interestengine.gui;

import com.nuwc.interestengine.data.Database;
import com.nuwc.interestengine.clustering.RouteExtractor;
import com.nuwc.interestengine.clustering.RouteObject;
import com.nuwc.interestengine.map.RoutePainter;
import com.nuwc.interestengine.parser.NMEAParser;
import com.nuwc.interestengine.map.Ship;
import com.nuwc.interestengine.map.TriMarker;
import com.nuwc.interestengine.neuralnet.NeuralNet;
import com.nuwc.interestengine.simulation.Simulation;
import com.nuwc.interestengine.simulation.SimulationChangeListener;
import com.nuwc.interestengine.simulation.SimulationState;
import java.awt.Color;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

public class OptionsPanel extends javax.swing.JPanel
{
    private final List<Ship> ships;
    private final Simulation simulation;
    private final List<TriMarker> markers;
    private final RoutePainter painter;
    private final MainFrame mainFrame;
    private final Database db;

    private final Color defaultColorClusters = Color.RED;
    private final Color defaultColorRoutes = Color.BLUE;
    private final Color defaultColorDataPoints = Color.BLACK;
    private final Color defaultColorEntryPoints = Color.CYAN;
    private final Color defaultColorExitPoints = Color.MAGENTA;
    private final Color defaultColorStopPoints = Color.GREEN;

    private Color colorClusters = null;
    private Color colorRoutes = null;
    private Color colorDataPoints = null;
    private Color colorEntryPoints = null;
    private Color colorExitPoints = null;
    private Color colorStopPoints = null;

    private List<RouteObject> routes = null;
    private NeuralNet network = null;

    public OptionsPanel(List<Ship> ships, Simulation simulation,
            List<TriMarker> markers, RoutePainter painter, MainFrame mainFrame)
    {
        this.ships = ships;
        this.simulation = simulation;
        this.markers = markers;
        this.painter = painter;
        this.mainFrame = mainFrame;
        this.db = new Database();

        initListeners();
        initComponents();
        initTweaks();

        updateTables();
    }

    private void initListeners()
    {
        simulation.addSimulationChangeListener(new StateChangeListener());
    }

    private void initTweaks()
    {
        colorClustersBox.setBackground(defaultColorClusters);
        colorRoutesBox.setBackground(defaultColorRoutes);
        colorDataPointsBox.setBackground(defaultColorDataPoints);
        colorEntryPointsBox.setBackground(defaultColorEntryPoints);
        colorExitPointsBox.setBackground(defaultColorExitPoints);
        colorStopPointsBox.setBackground(defaultColorStopPoints);
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
        SimulationState state = simulation.getState();
        if (ships.isEmpty())
        {
            playPauseButton.setIcon(getPlayIcon());
            playPauseButton.setText("Play");
            stopButton.setEnabled(false);
            playPauseButton.setEnabled(false);
        }
        else if (state == SimulationState.STOPPED)
        {
            playPauseButton.setIcon(getPlayIcon());
            playPauseButton.setText("Play");
            stopButton.setEnabled(false);
            playPauseButton.setEnabled(true);
        }
        else if (state == SimulationState.PAUSED)
        {
            playPauseButton.setIcon(getPlayIcon());
            playPauseButton.setText("Play");
            stopButton.setEnabled(true);
            playPauseButton.setEnabled(true);
        }
        else
        {
            playPauseButton.setIcon(getPauseIcon());
            playPauseButton.setText("Pause");
            stopButton.setEnabled(true);
            playPauseButton.setEnabled(true);
        }
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

            numberAISPointsField.setText("" + points.size());
            HashMap<Integer, Boolean> vessels = new HashMap<>();
            for (Object[] point : points)
            {
                Integer mmsi = (Integer) point[1];
                if (!vessels.containsKey(mmsi))
                {
                    vessels.put(mmsi, true);
                }
            }
            numberVesselsField.setText("" + vessels.size());
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
                    painter.setClusterColor(color);
                    break;
                case "routes":
                    painter.setRouteColor(color);
                    break;
                case "dataPoints":
                    painter.setDataPointColor(color);
                    break;
                case "entryPoints":
                    painter.setEntryPointColor(color);
                    break;
                case "exitPoints":
                    painter.setExitPointColor(color);
                    break;
                case "stopPoints":
                    painter.setStopPointColor(color);
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

        mainScroller = new javax.swing.JScrollPane();
        mainScrollerPanel = new javax.swing.JPanel();
        databasePanel = new javax.swing.JPanel();
        loadDataButton = new javax.swing.JButton();
        databaseTabs = new javax.swing.JTabbedPane();
        vesselDataScroller = new javax.swing.JScrollPane();
        vesselDataTable = new javax.swing.JTable();
        numberAISPointsLabel = new javax.swing.JLabel();
        numberVesselsLabel = new javax.swing.JLabel();
        numberAISPointsField = new javax.swing.JTextField();
        numberVesselsField = new javax.swing.JTextField();
        DatabaseSettingsPanel = new javax.swing.JPanel();
        minLatLabel = new javax.swing.JLabel();
        maxLatLabel = new javax.swing.JLabel();
        minLatSpinner = new javax.swing.JSpinner();
        maxLatSpinner = new javax.swing.JSpinner();
        minLonLabel = new javax.swing.JLabel();
        minLonSpinner = new javax.swing.JSpinner();
        maxLonLabel = new javax.swing.JLabel();
        maxLonSpinner = new javax.swing.JSpinner();
        simulationPanel = new javax.swing.JPanel();
        stopButton = new javax.swing.JButton();
        playPauseButton = new javax.swing.JButton();
        analysisPanel = new javax.swing.JPanel();
        extractRoutesButton = new javax.swing.JButton();
        numberClustersLabel = new javax.swing.JLabel();
        numberClustersField = new javax.swing.JTextField();
        numberRoutesLabel = new javax.swing.JLabel();
        numberRoutesField = new javax.swing.JTextField();
        AnalysisSettingsPanel = new javax.swing.JPanel();
        minSpeedLabel = new javax.swing.JLabel();
        entryEpsilonLabel = new javax.swing.JLabel();
        lostTimeLabel = new javax.swing.JLabel();
        entryMinPointsLabel = new javax.swing.JLabel();
        stopEpsilonLabel = new javax.swing.JLabel();
        exitEpsilonLabel = new javax.swing.JLabel();
        stopMinPointsLabel = new javax.swing.JLabel();
        exitMinPointsLabel = new javax.swing.JLabel();
        minSpeedField = new javax.swing.JTextField();
        entryEpsilonField = new javax.swing.JTextField();
        exitEpsilonField = new javax.swing.JTextField();
        stopEpsilonField = new javax.swing.JTextField();
        lostTimeField = new javax.swing.JTextField();
        entryMinPointsField = new javax.swing.JTextField();
        exitMinPointsField = new javax.swing.JTextField();
        stopMinPointsField = new javax.swing.JTextField();
        displayPanel = new javax.swing.JPanel();
        drawClustersBox = new javax.swing.JCheckBox();
        drawRoutesBox = new javax.swing.JCheckBox();
        drawEntryPointsBox = new javax.swing.JCheckBox();
        drawExitPointsBox = new javax.swing.JCheckBox();
        drawStopPointsBox = new javax.swing.JCheckBox();
        drawDataPointsBox = new javax.swing.JCheckBox();
        colorDataPointsButton = new javax.swing.JButton();
        colorRoutesButton = new javax.swing.JButton();
        colorClustersButton = new javax.swing.JButton();
        colorEntryPointsButton = new javax.swing.JButton();
        colorExitPointsButton = new javax.swing.JButton();
        colorStopPointsButton = new javax.swing.JButton();
        colorClustersBox = new javax.swing.JPanel();
        colorRoutesBox = new javax.swing.JPanel();
        colorDataPointsBox = new javax.swing.JPanel();
        colorEntryPointsBox = new javax.swing.JPanel();
        colorExitPointsBox = new javax.swing.JPanel();
        colorStopPointsBox = new javax.swing.JPanel();
        clustersModeCombo = new javax.swing.JComboBox<>();
        routesModeCombo = new javax.swing.JComboBox<>();
        jPanel1 = new javax.swing.JPanel();
        trainNetworkButton = new javax.swing.JButton();
        trainNetworkButton1 = new javax.swing.JButton();

        setPreferredSize(new java.awt.Dimension(500, 800));

        mainScroller.getVerticalScrollBar().setUnitIncrement(16);
        mainScroller.setPreferredSize(new java.awt.Dimension(400, 2000));

        mainScrollerPanel.setPreferredSize(new java.awt.Dimension(400, 1200));

        databasePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Database"));

        loadDataButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/add_icon.png"))); // NOI18N
        loadDataButton.setText("Load Data");
        loadDataButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                loadDataButtonActionPerformed(evt);
            }
        });

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

        databaseTabs.addTab("Vessel Data", vesselDataScroller);

        numberAISPointsLabel.setText("Number of AIS Points:");

        numberVesselsLabel.setText("Number of Vessels:");

        numberAISPointsField.setEditable(false);

        numberVesselsField.setEditable(false);

        DatabaseSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));

        minLatLabel.setText("Min Lat:");

        maxLatLabel.setText("Max Lat:");

        minLatSpinner.setModel(new SpinnerNumberModel(42, -180, 180, 0.5));

        maxLatSpinner.setModel(new SpinnerNumberModel(47, -180, 180, 0.5));

        minLonLabel.setText("Min Lon:");

        minLonSpinner.setModel(new SpinnerNumberModel(10, -180, 180, 0.5));

        maxLonLabel.setText("Max Lon:");

        maxLonSpinner.setModel(new SpinnerNumberModel(18, -180, 180, 0.5));

        javax.swing.GroupLayout DatabaseSettingsPanelLayout = new javax.swing.GroupLayout(DatabaseSettingsPanel);
        DatabaseSettingsPanel.setLayout(DatabaseSettingsPanelLayout);
        DatabaseSettingsPanelLayout.setHorizontalGroup(
            DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DatabaseSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(maxLatLabel)
                    .addComponent(minLatLabel))
                .addGap(18, 18, 18)
                .addGroup(DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(minLatSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE)
                    .addComponent(maxLatSpinner))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(maxLonLabel)
                    .addComponent(minLonLabel))
                .addGap(18, 18, 18)
                .addGroup(DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(minLonSpinner)
                    .addComponent(maxLonSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        DatabaseSettingsPanelLayout.setVerticalGroup(
            DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DatabaseSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(DatabaseSettingsPanelLayout.createSequentialGroup()
                        .addGroup(DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(minLonLabel)
                            .addComponent(minLonSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(maxLonLabel)
                            .addComponent(maxLonSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(DatabaseSettingsPanelLayout.createSequentialGroup()
                        .addGroup(DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(minLatLabel)
                            .addComponent(minLatSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(DatabaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(maxLatLabel)
                            .addComponent(maxLatSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout databasePanelLayout = new javax.swing.GroupLayout(databasePanel);
        databasePanel.setLayout(databasePanelLayout);
        databasePanelLayout.setHorizontalGroup(
            databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(databasePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(DatabaseSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, databasePanelLayout.createSequentialGroup()
                        .addComponent(loadDataButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numberAISPointsLabel)
                            .addComponent(numberVesselsLabel))
                        .addGap(18, 18, 18)
                        .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numberAISPointsField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(numberVesselsField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(databaseTabs, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        databasePanelLayout.setVerticalGroup(
            databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(databasePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(loadDataButton, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(databasePanelLayout.createSequentialGroup()
                        .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(numberAISPointsLabel)
                            .addComponent(numberAISPointsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(numberVesselsLabel)
                            .addComponent(numberVesselsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(DatabaseSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(databaseTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                .addContainerGap())
        );

        simulationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Simulation"));

        stopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/route-stop.png"))); // NOI18N
        stopButton.setText("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                stopButtonActionPerformed(evt);
            }
        });

        playPauseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/route-play.png"))); // NOI18N
        playPauseButton.setText("Play");
        playPauseButton.setEnabled(false);
        playPauseButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                playPauseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout simulationPanelLayout = new javax.swing.GroupLayout(simulationPanel);
        simulationPanel.setLayout(simulationPanelLayout);
        simulationPanelLayout.setHorizontalGroup(
            simulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(simulationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(playPauseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        simulationPanelLayout.setVerticalGroup(
            simulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(simulationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(simulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(playPauseButton, javax.swing.GroupLayout.DEFAULT_SIZE, 41, Short.MAX_VALUE)
                    .addComponent(stopButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        analysisPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Analysis"));

        extractRoutesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/output_icon.png"))); // NOI18N
        extractRoutesButton.setText("Extract Routes");
        extractRoutesButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                extractRoutesButtonActionPerformed(evt);
            }
        });

        numberClustersLabel.setText("Number of Clusters:");

        numberClustersField.setEditable(false);

        numberRoutesLabel.setText("Number of Routes:");

        numberRoutesField.setEditable(false);

        AnalysisSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));

        minSpeedLabel.setText("Min Speed:");

        entryEpsilonLabel.setText("Entry Eps:");

        lostTimeLabel.setText("Lost Time:");

        entryMinPointsLabel.setText("Entry Min Pts:");

        stopEpsilonLabel.setText("Stop Eps:");

        exitEpsilonLabel.setText("Exit Eps:");

        stopMinPointsLabel.setText("Stop Min Pts:");

        exitMinPointsLabel.setText("Exit Min Pts:");

        minSpeedField.setText("1");

        entryEpsilonField.setText("3");
        entryEpsilonField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                entryEpsilonFieldActionPerformed(evt);
            }
        });

        exitEpsilonField.setText("3");

        stopEpsilonField.setText("3");

        lostTimeField.setText("100");

        entryMinPointsField.setText("5");

        exitMinPointsField.setText("5");

        stopMinPointsField.setText("5");

        javax.swing.GroupLayout AnalysisSettingsPanelLayout = new javax.swing.GroupLayout(AnalysisSettingsPanel);
        AnalysisSettingsPanel.setLayout(AnalysisSettingsPanelLayout);
        AnalysisSettingsPanelLayout.setHorizontalGroup(
            AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AnalysisSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(AnalysisSettingsPanelLayout.createSequentialGroup()
                        .addComponent(stopEpsilonLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
                        .addComponent(stopEpsilonField, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(AnalysisSettingsPanelLayout.createSequentialGroup()
                        .addComponent(exitEpsilonLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(exitEpsilonField, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(AnalysisSettingsPanelLayout.createSequentialGroup()
                        .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(entryEpsilonLabel)
                            .addComponent(minSpeedLabel))
                        .addGap(18, 18, 18)
                        .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(entryEpsilonField, javax.swing.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE)
                            .addComponent(minSpeedField))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, AnalysisSettingsPanelLayout.createSequentialGroup()
                        .addComponent(lostTimeLabel)
                        .addGap(36, 36, 36))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, AnalysisSettingsPanelLayout.createSequentialGroup()
                        .addComponent(entryMinPointsLabel)
                        .addGap(18, 18, 18))
                    .addGroup(AnalysisSettingsPanelLayout.createSequentialGroup()
                        .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(exitMinPointsLabel)
                            .addComponent(stopMinPointsLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(stopMinPointsField, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exitMinPointsField, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(lostTimeField, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(entryMinPointsField, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        AnalysisSettingsPanelLayout.setVerticalGroup(
            AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AnalysisSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lostTimeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lostTimeLabel))
                    .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(minSpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(minSpeedLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(AnalysisSettingsPanelLayout.createSequentialGroup()
                        .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(entryMinPointsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(entryMinPointsLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(exitMinPointsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(exitMinPointsLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(stopMinPointsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(stopMinPointsLabel)))
                    .addGroup(AnalysisSettingsPanelLayout.createSequentialGroup()
                        .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(entryEpsilonLabel)
                            .addComponent(entryEpsilonField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(exitEpsilonLabel)
                            .addComponent(exitEpsilonField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(AnalysisSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(stopEpsilonLabel)
                            .addComponent(stopEpsilonField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout analysisPanelLayout = new javax.swing.GroupLayout(analysisPanel);
        analysisPanel.setLayout(analysisPanelLayout);
        analysisPanelLayout.setHorizontalGroup(
            analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(analysisPanelLayout.createSequentialGroup()
                        .addComponent(extractRoutesButton)
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numberClustersLabel)
                            .addComponent(numberRoutesLabel))
                        .addGap(18, 18, 18)
                        .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numberClustersField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(numberRoutesField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                            .addComponent(numberClustersLabel)
                            .addComponent(numberClustersField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(analysisPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(numberRoutesLabel)
                            .addComponent(numberRoutesField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(extractRoutesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(AnalysisSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        displayPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Display"));

        drawClustersBox.setText("Clusters");
        drawClustersBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                drawClustersBoxActionPerformed(evt);
            }
        });

        drawRoutesBox.setSelected(true);
        drawRoutesBox.setText("Routes");
        drawRoutesBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                drawRoutesBoxActionPerformed(evt);
            }
        });

        drawEntryPointsBox.setText("Entry Points");
        drawEntryPointsBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                drawEntryPointsBoxActionPerformed(evt);
            }
        });

        drawExitPointsBox.setText("Exit Points");
        drawExitPointsBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                drawExitPointsBoxActionPerformed(evt);
            }
        });

        drawStopPointsBox.setText("Stop Points");
        drawStopPointsBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                drawStopPointsBoxActionPerformed(evt);
            }
        });

        drawDataPointsBox.setText("Data Points");
        drawDataPointsBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                drawDataPointsBoxActionPerformed(evt);
            }
        });

        colorDataPointsButton.setText("Color");
        colorDataPointsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                colorDataPointsButtonActionPerformed(evt);
            }
        });

        colorRoutesButton.setText("Color");
        colorRoutesButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                colorRoutesButtonActionPerformed(evt);
            }
        });

        colorClustersButton.setText("Color");
        colorClustersButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                colorClustersButtonActionPerformed(evt);
            }
        });

        colorEntryPointsButton.setText("Color");
        colorEntryPointsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                colorEntryPointsButtonActionPerformed(evt);
            }
        });

        colorExitPointsButton.setText("Color");
        colorExitPointsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                colorExitPointsButtonActionPerformed(evt);
            }
        });

        colorStopPointsButton.setText("Color");
        colorStopPointsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                colorStopPointsButtonActionPerformed(evt);
            }
        });

        colorClustersBox.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout colorClustersBoxLayout = new javax.swing.GroupLayout(colorClustersBox);
        colorClustersBox.setLayout(colorClustersBoxLayout);
        colorClustersBoxLayout.setHorizontalGroup(
            colorClustersBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );
        colorClustersBoxLayout.setVerticalGroup(
            colorClustersBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        colorRoutesBox.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout colorRoutesBoxLayout = new javax.swing.GroupLayout(colorRoutesBox);
        colorRoutesBox.setLayout(colorRoutesBoxLayout);
        colorRoutesBoxLayout.setHorizontalGroup(
            colorRoutesBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );
        colorRoutesBoxLayout.setVerticalGroup(
            colorRoutesBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        colorDataPointsBox.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout colorDataPointsBoxLayout = new javax.swing.GroupLayout(colorDataPointsBox);
        colorDataPointsBox.setLayout(colorDataPointsBoxLayout);
        colorDataPointsBoxLayout.setHorizontalGroup(
            colorDataPointsBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );
        colorDataPointsBoxLayout.setVerticalGroup(
            colorDataPointsBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        colorEntryPointsBox.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout colorEntryPointsBoxLayout = new javax.swing.GroupLayout(colorEntryPointsBox);
        colorEntryPointsBox.setLayout(colorEntryPointsBoxLayout);
        colorEntryPointsBoxLayout.setHorizontalGroup(
            colorEntryPointsBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );
        colorEntryPointsBoxLayout.setVerticalGroup(
            colorEntryPointsBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        colorExitPointsBox.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout colorExitPointsBoxLayout = new javax.swing.GroupLayout(colorExitPointsBox);
        colorExitPointsBox.setLayout(colorExitPointsBoxLayout);
        colorExitPointsBoxLayout.setHorizontalGroup(
            colorExitPointsBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );
        colorExitPointsBoxLayout.setVerticalGroup(
            colorExitPointsBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        colorStopPointsBox.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout colorStopPointsBoxLayout = new javax.swing.GroupLayout(colorStopPointsBox);
        colorStopPointsBox.setLayout(colorStopPointsBoxLayout);
        colorStopPointsBoxLayout.setHorizontalGroup(
            colorStopPointsBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );
        colorStopPointsBoxLayout.setVerticalGroup(
            colorStopPointsBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        clustersModeCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Single Color", "Rainbow", "With Points", "Connected" }));
        clustersModeCombo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clustersModeComboActionPerformed(evt);
            }
        });

        routesModeCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Rainbow", "Single Color" }));
        routesModeCombo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                routesModeComboActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout displayPanelLayout = new javax.swing.GroupLayout(displayPanel);
        displayPanel.setLayout(displayPanelLayout);
        displayPanelLayout.setHorizontalGroup(
            displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(displayPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(drawClustersBox)
                    .addComponent(drawRoutesBox)
                    .addComponent(drawDataPointsBox)
                    .addComponent(drawEntryPointsBox)
                    .addComponent(drawExitPointsBox)
                    .addComponent(drawStopPointsBox))
                .addGap(32, 32, 32)
                .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(displayPanelLayout.createSequentialGroup()
                        .addComponent(colorStopPointsButton)
                        .addGap(18, 18, 18)
                        .addComponent(colorStopPointsBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(displayPanelLayout.createSequentialGroup()
                        .addComponent(colorExitPointsButton)
                        .addGap(18, 18, 18)
                        .addComponent(colorExitPointsBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(displayPanelLayout.createSequentialGroup()
                        .addComponent(colorClustersButton)
                        .addGap(18, 18, 18)
                        .addComponent(colorClustersBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(clustersModeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(displayPanelLayout.createSequentialGroup()
                        .addComponent(colorRoutesButton)
                        .addGap(18, 18, 18)
                        .addComponent(colorRoutesBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(routesModeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(displayPanelLayout.createSequentialGroup()
                        .addComponent(colorDataPointsButton)
                        .addGap(18, 18, 18)
                        .addComponent(colorDataPointsBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(displayPanelLayout.createSequentialGroup()
                        .addComponent(colorEntryPointsButton)
                        .addGap(18, 18, 18)
                        .addComponent(colorEntryPointsBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(108, Short.MAX_VALUE))
        );
        displayPanelLayout.setVerticalGroup(
            displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(displayPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(colorClustersButton)
                    .addGroup(displayPanelLayout.createSequentialGroup()
                        .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(drawClustersBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(colorClustersBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(clustersModeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(8, 8, 8)
                        .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(colorRoutesBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(displayPanelLayout.createSequentialGroup()
                                .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(drawRoutesBox)
                                        .addComponent(colorRoutesButton))
                                    .addComponent(routesModeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 4, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(colorDataPointsBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(displayPanelLayout.createSequentialGroup()
                                .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(drawDataPointsBox)
                                    .addComponent(colorDataPointsButton))
                                .addGap(0, 4, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(colorEntryPointsBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(drawEntryPointsBox)
                                .addComponent(colorEntryPointsButton)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(drawExitPointsBox)
                        .addComponent(colorExitPointsButton))
                    .addComponent(colorExitPointsBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(displayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(drawStopPointsBox)
                        .addComponent(colorStopPointsButton))
                    .addComponent(colorStopPointsBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(31, 31, 31))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Neural Network"));

        trainNetworkButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/nuwc/interestengine/resources/gui/convert_icon.png"))); // NOI18N
        trainNetworkButton.setText("Train Network");
        trainNetworkButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                trainNetworkButtonActionPerformed(evt);
            }
        });

        trainNetworkButton1.setText("Test");
        trainNetworkButton1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                trainNetworkButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(trainNetworkButton)
                .addGap(18, 18, 18)
                .addComponent(trainNetworkButton1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(trainNetworkButton, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(trainNetworkButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout mainScrollerPanelLayout = new javax.swing.GroupLayout(mainScrollerPanel);
        mainScrollerPanel.setLayout(mainScrollerPanelLayout);
        mainScrollerPanelLayout.setHorizontalGroup(
            mainScrollerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainScrollerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainScrollerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(mainScrollerPanelLayout.createSequentialGroup()
                        .addComponent(displayPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(simulationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(databasePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(analysisPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        mainScrollerPanelLayout.setVerticalGroup(
            mainScrollerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainScrollerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(simulationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(databasePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(analysisPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(displayPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        mainScroller.setViewportView(mainScrollerPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainScroller, javax.swing.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainScroller, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

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
            RouteExtractor extractor = new RouteExtractor(db, painter, mainFrame,
                    lostTime, minSpeed, entryEpsilon, entryMinPoints,
                    exitEpsilon, exitMinPoints, stopEpsilon, stopMinPoints);
            routes = extractor.run();
            int n = 0;
            for (RouteObject route : routes)
            {
                n += route.points.size();
            }
            System.out.println("# points = " + n);
            numberClustersField.setText("" + extractor.getNumberOfClusters());
            numberRoutesField.setText("" + extractor.getNumberOfRoutes());
        }
        else
        {
            JOptionPane.showMessageDialog(this, "Database not populated!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_extractRoutesButtonActionPerformed

    private void playPauseButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_playPauseButtonActionPerformed
    {//GEN-HEADEREND:event_playPauseButtonActionPerformed
        playPauseButton.setEnabled(false);
        SimulationState state = simulation.getState();
        if (state == SimulationState.STOPPED)
        {
            playPauseButton.setIcon(getPauseIcon());
            playPauseButton.setText("Pause");
            simulation.start();
        }
        else if (state == SimulationState.PAUSED)
        {
            playPauseButton.setIcon(getPauseIcon());
            playPauseButton.setText("Pause");
            simulation.unpause();
        }
        else
        {
            playPauseButton.setIcon(getPlayIcon());
            playPauseButton.setText("Play");
            simulation.pause();
        }
        stopButton.setEnabled(true);
        playPauseButton.setEnabled(true);
    }//GEN-LAST:event_playPauseButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_stopButtonActionPerformed
    {//GEN-HEADEREND:event_stopButtonActionPerformed
        stopButton.setEnabled(false);
        SimulationState state = simulation.getState();
        if (state != SimulationState.STOPPED)
        {
            playPauseButton.setIcon(getPlayIcon());
            playPauseButton.setText("Play");
            simulation.stop();
        }
    }//GEN-LAST:event_stopButtonActionPerformed

    private void loadDataButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_loadDataButtonActionPerformed
    {//GEN-HEADEREND:event_loadDataButtonActionPerformed
        loadDataButton.setEnabled(false);
        extractRoutesButton.setEnabled(false);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            if (db.exists())
            {
                returnValue = JOptionPane.showConfirmDialog(this, "Overwrite database?", "Confirm", JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.CANCEL_OPTION
                        || returnValue == JOptionPane.CLOSED_OPTION)
                {
                    loadDataButton.setEnabled(true);
                    extractRoutesButton.setEnabled(true);
                    return;
                }
            }

            File[] files = fileChooser.getSelectedFiles();
            Arrays.sort(files, (File file1, File file2) ->
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

    private void entryEpsilonFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_entryEpsilonFieldActionPerformed
    {//GEN-HEADEREND:event_entryEpsilonFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_entryEpsilonFieldActionPerformed

    private void colorClustersButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_colorClustersButtonActionPerformed
    {//GEN-HEADEREND:event_colorClustersButtonActionPerformed
        colorDialog(colorClusters, defaultColorClusters, colorClustersBox, "clusters");
    }//GEN-LAST:event_colorClustersButtonActionPerformed

    private void drawClustersBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_drawClustersBoxActionPerformed
    {//GEN-HEADEREND:event_drawClustersBoxActionPerformed
        painter.setDrawClusters(drawClustersBox.isSelected());
    }//GEN-LAST:event_drawClustersBoxActionPerformed

    private void drawRoutesBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_drawRoutesBoxActionPerformed
    {//GEN-HEADEREND:event_drawRoutesBoxActionPerformed
        painter.setDrawRoutes(drawRoutesBox.isSelected());
    }//GEN-LAST:event_drawRoutesBoxActionPerformed

    private void drawDataPointsBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_drawDataPointsBoxActionPerformed
    {//GEN-HEADEREND:event_drawDataPointsBoxActionPerformed
        painter.setDrawDataPoints(drawDataPointsBox.isSelected());
    }//GEN-LAST:event_drawDataPointsBoxActionPerformed

    private void drawEntryPointsBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_drawEntryPointsBoxActionPerformed
    {//GEN-HEADEREND:event_drawEntryPointsBoxActionPerformed
        painter.setDrawEntryPoints(drawEntryPointsBox.isSelected());
    }//GEN-LAST:event_drawEntryPointsBoxActionPerformed

    private void drawExitPointsBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_drawExitPointsBoxActionPerformed
    {//GEN-HEADEREND:event_drawExitPointsBoxActionPerformed
        painter.setDrawExitPoints(drawExitPointsBox.isSelected());
    }//GEN-LAST:event_drawExitPointsBoxActionPerformed

    private void drawStopPointsBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_drawStopPointsBoxActionPerformed
    {//GEN-HEADEREND:event_drawStopPointsBoxActionPerformed
        painter.setDrawStopPoints(drawStopPointsBox.isSelected());
    }//GEN-LAST:event_drawStopPointsBoxActionPerformed

    private void colorRoutesButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_colorRoutesButtonActionPerformed
    {//GEN-HEADEREND:event_colorRoutesButtonActionPerformed
        colorDialog(colorRoutes, defaultColorRoutes, colorRoutesBox, "routes");
    }//GEN-LAST:event_colorRoutesButtonActionPerformed

    private void colorDataPointsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_colorDataPointsButtonActionPerformed
    {//GEN-HEADEREND:event_colorDataPointsButtonActionPerformed
        colorDialog(colorDataPoints, defaultColorDataPoints, colorDataPointsBox, "dataPoints");
    }//GEN-LAST:event_colorDataPointsButtonActionPerformed

    private void colorEntryPointsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_colorEntryPointsButtonActionPerformed
    {//GEN-HEADEREND:event_colorEntryPointsButtonActionPerformed
        colorDialog(colorEntryPoints, defaultColorEntryPoints, colorEntryPointsBox, "entryPoints");
    }//GEN-LAST:event_colorEntryPointsButtonActionPerformed

    private void colorExitPointsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_colorExitPointsButtonActionPerformed
    {//GEN-HEADEREND:event_colorExitPointsButtonActionPerformed
        colorDialog(colorExitPoints, defaultColorExitPoints, colorExitPointsBox, "exitPoints");
    }//GEN-LAST:event_colorExitPointsButtonActionPerformed

    private void colorStopPointsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_colorStopPointsButtonActionPerformed
    {//GEN-HEADEREND:event_colorStopPointsButtonActionPerformed
        colorDialog(colorStopPoints, defaultColorStopPoints, colorStopPointsBox, "stopPoints");
    }//GEN-LAST:event_colorStopPointsButtonActionPerformed

    private void clustersModeComboActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clustersModeComboActionPerformed
    {//GEN-HEADEREND:event_clustersModeComboActionPerformed
        painter.setClustersMode((String) clustersModeCombo.getSelectedItem());
    }//GEN-LAST:event_clustersModeComboActionPerformed

    private void routesModeComboActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_routesModeComboActionPerformed
    {//GEN-HEADEREND:event_routesModeComboActionPerformed
        painter.setRoutesMode((String) routesModeCombo.getSelectedItem());
    }//GEN-LAST:event_routesModeComboActionPerformed

    private void trainNetworkButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_trainNetworkButtonActionPerformed
    {//GEN-HEADEREND:event_trainNetworkButtonActionPerformed
        if (routes != null)
        {
            for (RouteObject route : routes)
            {
                System.out.println(route.points.size());
            }
            network = new NeuralNet(5, 50, routes.size(), 5, 350, 0.01, routes);
            network.train();
        }
        System.out.println("***** COMPLETE *****");
    }//GEN-LAST:event_trainNetworkButtonActionPerformed

    private void trainNetworkButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_trainNetworkButton1ActionPerformed
    {//GEN-HEADEREND:event_trainNetworkButton1ActionPerformed
        if (network != null)
        {
            network.test();
        }
    }//GEN-LAST:event_trainNetworkButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AnalysisSettingsPanel;
    private javax.swing.JPanel DatabaseSettingsPanel;
    private javax.swing.JPanel analysisPanel;
    private javax.swing.JComboBox<String> clustersModeCombo;
    private javax.swing.JPanel colorClustersBox;
    private javax.swing.JButton colorClustersButton;
    private javax.swing.JPanel colorDataPointsBox;
    private javax.swing.JButton colorDataPointsButton;
    private javax.swing.JPanel colorEntryPointsBox;
    private javax.swing.JButton colorEntryPointsButton;
    private javax.swing.JPanel colorExitPointsBox;
    private javax.swing.JButton colorExitPointsButton;
    private javax.swing.JPanel colorRoutesBox;
    private javax.swing.JButton colorRoutesButton;
    private javax.swing.JPanel colorStopPointsBox;
    private javax.swing.JButton colorStopPointsButton;
    private javax.swing.JPanel databasePanel;
    private javax.swing.JTabbedPane databaseTabs;
    private javax.swing.JPanel displayPanel;
    private javax.swing.JCheckBox drawClustersBox;
    private javax.swing.JCheckBox drawDataPointsBox;
    private javax.swing.JCheckBox drawEntryPointsBox;
    private javax.swing.JCheckBox drawExitPointsBox;
    private javax.swing.JCheckBox drawRoutesBox;
    private javax.swing.JCheckBox drawStopPointsBox;
    private javax.swing.JTextField entryEpsilonField;
    private javax.swing.JLabel entryEpsilonLabel;
    private javax.swing.JTextField entryMinPointsField;
    private javax.swing.JLabel entryMinPointsLabel;
    private javax.swing.JTextField exitEpsilonField;
    private javax.swing.JLabel exitEpsilonLabel;
    private javax.swing.JTextField exitMinPointsField;
    private javax.swing.JLabel exitMinPointsLabel;
    private javax.swing.JButton extractRoutesButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton loadDataButton;
    private javax.swing.JTextField lostTimeField;
    private javax.swing.JLabel lostTimeLabel;
    private javax.swing.JScrollPane mainScroller;
    private javax.swing.JPanel mainScrollerPanel;
    private javax.swing.JLabel maxLatLabel;
    private javax.swing.JSpinner maxLatSpinner;
    private javax.swing.JLabel maxLonLabel;
    private javax.swing.JSpinner maxLonSpinner;
    private javax.swing.JLabel minLatLabel;
    private javax.swing.JSpinner minLatSpinner;
    private javax.swing.JLabel minLonLabel;
    private javax.swing.JSpinner minLonSpinner;
    private javax.swing.JTextField minSpeedField;
    private javax.swing.JLabel minSpeedLabel;
    private javax.swing.JTextField numberAISPointsField;
    private javax.swing.JLabel numberAISPointsLabel;
    private javax.swing.JTextField numberClustersField;
    private javax.swing.JLabel numberClustersLabel;
    private javax.swing.JTextField numberRoutesField;
    private javax.swing.JLabel numberRoutesLabel;
    private javax.swing.JTextField numberVesselsField;
    private javax.swing.JLabel numberVesselsLabel;
    private javax.swing.JButton playPauseButton;
    private javax.swing.JComboBox<String> routesModeCombo;
    private javax.swing.JPanel simulationPanel;
    private javax.swing.JButton stopButton;
    private javax.swing.JTextField stopEpsilonField;
    private javax.swing.JLabel stopEpsilonLabel;
    private javax.swing.JTextField stopMinPointsField;
    private javax.swing.JLabel stopMinPointsLabel;
    private javax.swing.JButton trainNetworkButton;
    private javax.swing.JButton trainNetworkButton1;
    private javax.swing.JScrollPane vesselDataScroller;
    private javax.swing.JTable vesselDataTable;
    // End of variables declaration//GEN-END:variables
}
