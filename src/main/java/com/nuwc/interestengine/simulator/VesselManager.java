package com.nuwc.interestengine.simulator;

import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.data.AISPoint;
import com.nuwc.interestengine.gui.OptionsPanel;
import com.nuwc.interestengine.map.RoutePainter;
import com.nuwc.interestengine.parser.StampedAISMessage;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.ExtendedClassBEquipmentPositionReport;
import dk.tbsalling.aismessages.ais.messages.PositionReport;
import dk.tbsalling.aismessages.ais.messages.ShipAndVoyageData;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import dk.tbsalling.aismessages.ais.messages.types.NavigationStatus;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class VesselManager
{
    private final float minLat, maxLat, minLon, maxLon;
    private final ConcurrentHashMap<Integer, Vessel> vessels;
    private final HashMap<Integer, String> callSigns;
    private final HashMap<Integer, String> shipNames;
    private final HashMap<Integer, String> shipTypes;
    private final HashMap<Integer, Integer> toBows;
    private final HashMap<Integer, Integer> toSterns;
    private final HashMap<Integer, Integer> toStarboards;
    private final HashMap<Integer, Integer> toPorts;
    private final HashMap<Integer, Integer> lengths;
    private final HashMap<Integer, Integer> widths;
    private final HashMap<Integer, Integer> imos;
    private final HashMap<Integer, String> destinations;
    private final HashMap<Integer, String> etas;
    private final HashMap<Integer, Float> draughts;

    private final RoutePainter routePainter;
    private final Timer updater;
    private final OptionsPanel optionsPanel;

    public VesselManager(float minLat, float maxLat, float minLon, float maxLon,
            OptionsPanel optionsPanel, RoutePainter routePainter)
    {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.optionsPanel = optionsPanel;
        this.routePainter = routePainter;

        vessels = new ConcurrentHashMap<>();

        callSigns = new HashMap<>();
        shipNames = new HashMap<>();
        shipTypes = new HashMap<>();
        toBows = new HashMap<>();
        toSterns = new HashMap<>();
        toStarboards = new HashMap<>();
        toPorts = new HashMap<>();
        lengths = new HashMap<>();
        widths = new HashMap<>();
        imos = new HashMap<>();
        destinations = new HashMap<>();
        etas = new HashMap<>();
        draughts = new HashMap<>();

        updater = new Timer();
        startUpdater();
    }

    private void startUpdater()
    {
        updater.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                routePainter.replaceVessels(vessels.values());
                optionsPanel.updateInfo();
            }
        }, 0, 1000);
    }

    public void processData(StampedAISMessage aisStamped)
    {
        AISMessage aisMessage = aisStamped.getMessage();
        long timestamp = aisStamped.getTimestamp();
        int mmsi = aisMessage.getSourceMmsi().getMMSI();

        if (aisMessage instanceof DynamicDataReport)
        {
            DynamicDataReport report = (DynamicDataReport) aisMessage;
            float lat = report.getLatitude();
            float lon = report.getLongitude();
            float sog = report.getSpeedOverGround();
            float cog = report.getCourseOverGround();

            if (minLat < lat && lat < maxLat
                    && minLon < lon && lon < maxLon)
            {
                Vessel vessel;
                if (vessels.containsKey(mmsi))
                {
                    vessel = vessels.get(mmsi);
                    if (vessel.callSign == null
                            && callSigns.containsKey(mmsi))
                    {
                        vessel.callSign = callSigns.get(mmsi);
                    }
                    if (vessel.shipName == null
                            && shipNames.containsKey(mmsi))
                    {
                        vessel.shipName = shipNames.get(mmsi);
                    }
                    if (vessel.shipType == null
                            && shipTypes.containsKey(mmsi))
                    {
                        vessel.shipType = shipTypes.get(mmsi);
                    }
                    if (vessel.toBow == null
                            && toBows.containsKey(mmsi))
                    {
                        vessel.toBow = toBows.get(mmsi);
                    }
                    if (vessel.toStern == null
                            && toSterns.containsKey(mmsi))
                    {
                        vessel.toStern = toSterns.get(mmsi);
                    }
                    if (vessel.toStarboard == null
                            && toStarboards.containsKey(mmsi))
                    {
                        vessel.toStarboard = toStarboards.get(mmsi);
                    }
                    if (vessel.toPort == null
                            && toPorts.containsKey(mmsi))
                    {
                        vessel.toPort = toPorts.get(mmsi);
                    }
                    if (vessel.shipLength == null
                            && lengths.containsKey(mmsi))
                    {
                        vessel.shipLength = lengths.get(mmsi);
                    }
                    if (vessel.shipWidth == null
                            && widths.containsKey(mmsi))
                    {
                        vessel.shipWidth = widths.get(mmsi);
                    }
                    if (vessel.imo == null
                            && imos.containsKey(mmsi))
                    {
                        vessel.imo = imos.get(mmsi);
                    }
                    if (vessel.destination == null
                            && destinations.containsKey(mmsi))
                    {
                        vessel.destination = destinations.get(mmsi);
                    }
                    if (vessel.eta == null
                            && etas.containsKey(mmsi))
                    {
                        vessel.eta = etas.get(mmsi);
                    }
                    if (vessel.draught == null
                            && draughts.containsKey(mmsi))
                    {
                        vessel.draught = draughts.get(mmsi);
                    }
                }
                else
                {
                    vessel = new Vessel(mmsi);
                    if (callSigns.containsKey(mmsi))
                    {
                        vessel.callSign = callSigns.get(mmsi);
                    }
                    if (shipNames.containsKey(mmsi))
                    {
                        vessel.shipName = shipNames.get(mmsi);
                    }
                    if (shipTypes.containsKey(mmsi))
                    {
                        vessel.shipType = shipTypes.get(mmsi);
                    }
                    if (toBows.containsKey(mmsi))
                    {
                        vessel.toBow = toBows.get(mmsi);
                    }
                    if (toSterns.containsKey(mmsi))
                    {
                        vessel.toStern = toSterns.get(mmsi);
                    }
                    if (toStarboards.containsKey(mmsi))
                    {
                        vessel.toStarboard = toStarboards.get(mmsi);
                    }
                    if (toPorts.containsKey(mmsi))
                    {
                        vessel.toPort = toPorts.get(mmsi);
                    }
                    if (imos.containsKey(mmsi))
                    {
                        vessel.imo = imos.get(mmsi);
                    }
                    if (destinations.containsKey(mmsi))
                    {
                        vessel.destination = destinations.get(mmsi);
                    }
                    if (etas.containsKey(mmsi))
                    {
                        vessel.eta = etas.get(mmsi);
                    }
                    if (draughts.containsKey(mmsi))
                    {
                        vessel.draught = draughts.get(mmsi);
                    }
                    vessels.put(mmsi, vessel);
                }
                vessel.track.add(
                        new AISPoint(lat, lon, sog, cog, timestamp));

                optionsPanel.checkAnomaly(vessel);

                if (report instanceof ExtendedClassBEquipmentPositionReport)
                {
                    ExtendedClassBEquipmentPositionReport extendedReport
                            = (ExtendedClassBEquipmentPositionReport) report;
                    String shipName = extendedReport.getShipName();
                    if (shipName != null)
                    {
                        vessel.shipName = shipName;
                    }
                }

                if (aisMessage instanceof PositionReport)
                {
                    PositionReport extendedReport = (PositionReport) report;
                    NavigationStatus status = extendedReport.getNavigationStatus();
                    vessel.navStatus = status;
                }
            }
        }

        if (aisMessage instanceof StaticDataReport)
        {
            StaticDataReport report = (StaticDataReport) aisMessage;
            String callSign = report.getCallsign();
            String shipName = report.getShipName();
            String shipType = report.getShipType().getValue();
            Integer toBow = report.getToBow();
            Integer toStern = report.getToStern();
            Integer toStarboard = report.getToStarboard();
            Integer toPort = report.getToPort();
            Integer imo = null;
            String destination = null;
            String eta = null;
            Float draught = null;
            if (aisMessage instanceof ShipAndVoyageData)
            {
                ShipAndVoyageData extendedReport
                        = (ShipAndVoyageData) aisMessage;
                imo = extendedReport.getImo().getIMO();
                destination = extendedReport.getDestination();
                eta = extendedReport.getEta();
                draught = extendedReport.getDraught();
            }

            Vessel vessel;
            if (vessels.containsKey(mmsi))
            {
                vessel = vessels.get(mmsi);
                if (callSign != null)
                {
                    vessel.callSign = callSign;
                }
                if (shipName != null)
                {
                    vessel.shipName = shipName;
                }
                if (shipType != null)
                {
                    vessel.shipType = shipType;
                }
                if (toBow != null)
                {
                    vessel.toBow = toBow;
                }
                if (toStern != null)
                {
                    vessel.toStern = toStern;
                }
                if (toStarboard != null)
                {
                    vessel.toStarboard = toStarboard;
                }
                if (toPort != null)
                {
                    vessel.toPort = toPort;
                }
                if (toBow != null && toStern != null)
                {
                    vessel.shipLength = toBow + toStern;
                }
                if (toStarboard != null && toPort != null)
                {
                    vessel.shipWidth = toStarboard + toPort;
                }
                if (imo != null)
                {
                    vessel.imo = null;
                }
                if (destination != null)
                {
                    vessel.destination = destination;
                }
                if (eta != null)
                {
                    vessel.eta = eta;
                }
                if (draught != null)
                {
                    vessel.draught = draught;
                }
            }
            else
            {
                if (callSign != null && !callSigns.containsKey(mmsi))
                {
                    callSigns.put(mmsi, callSign);
                }
                if (shipName != null && !shipNames.containsKey(mmsi))
                {
                    shipNames.put(mmsi, shipName);
                }
                if (shipType != null && !shipTypes.containsKey(mmsi))
                {
                    shipTypes.put(mmsi, shipType);
                }
                if (toBow != null && !toBows.containsKey(mmsi))
                {
                    toBows.put(mmsi, toBow);
                }
                if (toStern != null && !toSterns.containsKey(mmsi))
                {
                    toSterns.put(mmsi, toStern);
                }
                if (toStarboard != null && !toStarboards.containsKey(mmsi))
                {
                    toStarboards.put(mmsi, toStarboard);
                }
                if (toPort != null && !toPorts.containsKey(mmsi))
                {
                    toPorts.put(mmsi, toPort);
                }
                if (toBow != null && toStern != null
                        && !lengths.containsKey(mmsi))
                {
                    lengths.put(mmsi, toBow + toStern);
                }
                if (toStarboard != null && toPort != null
                        && !widths.containsKey(mmsi))
                {
                    widths.put(mmsi, toStarboard + toPort);
                }
                if (imo != null && !imos.containsKey(mmsi))
                {
                    imos.put(mmsi, imo);
                }
                if (destination != null && !destinations.containsKey(mmsi))
                {
                    destinations.put(mmsi, destination);
                }
                if (eta != null && !etas.containsKey(mmsi))
                {
                    etas.put(mmsi, eta);
                }
                if (draught != null && !draughts.containsKey(mmsi))
                {
                    draughts.put(mmsi, draught);
                }
            }
        }
    }
}
