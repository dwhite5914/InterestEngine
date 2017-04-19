package com.nuwc.interestengine.simulator;

import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.data.AISPoint;
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
    private final HashMap<Integer, String> shipTypes;
    private final HashMap<Integer, String> shipNames;
    private final HashMap<Integer, String> destinations;
    private final RoutePainter routePainter;
    private final Timer updater;

    public VesselManager(float minLat, float maxLat, float minLon, float maxLon,
            RoutePainter routePainter)
    {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.routePainter = routePainter;

        vessels = new ConcurrentHashMap<>();
        shipTypes = new HashMap<>();
        shipNames = new HashMap<>();
        destinations = new HashMap<>();
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
            }
        }, 0, 5000);
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
                    if (vessel.shipType == null
                            && shipTypes.containsKey(mmsi))
                    {
                        vessel.shipType = shipTypes.get(mmsi);
                    }
                    if (vessel.shipName == null
                            && shipNames.containsKey(mmsi))
                    {
                        vessel.shipName = shipNames.get(mmsi);
                    }
                    if (vessel.destination == null
                            && destinations.containsKey(mmsi))
                    {
                        vessel.destination = destinations.get(mmsi);
                    }
                }
                else
                {
                    vessel = new Vessel(mmsi);
                    if (shipTypes.containsKey(mmsi))
                    {
                        vessel.shipType = shipTypes.get(mmsi);
                    }
                    if (shipNames.containsKey(mmsi))
                    {
                        vessel.shipName = shipNames.get(mmsi);
                    }
                    if (destinations.containsKey(mmsi))
                    {
                        vessel.destination = destinations.get(mmsi);
                    }
                    vessels.put(mmsi, vessel);
                }
                vessel.track.add(
                        new AISPoint(lat, lon, sog, cog, timestamp));

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
            String shipType = (report.getShipType() == null)
                    ? "NotAvailable"
                    : report.getShipType().getValue();
            String shipName = null;
            String destination = null;
            if (aisMessage instanceof ShipAndVoyageData)
            {
                ShipAndVoyageData extendedReport
                        = (ShipAndVoyageData) aisMessage;
                shipName = extendedReport.getShipName();
                destination = extendedReport.getDestination();
            }

            Vessel vessel;
            if (vessels.containsKey(mmsi))
            {
                vessel = vessels.get(mmsi);
                vessel.shipType = shipType;
            }
            else
            {
                if (!shipTypes.containsKey(mmsi))
                {
                    shipTypes.put(mmsi, shipType);
                }
                if (shipName != null && !shipNames.containsKey(mmsi))
                {
                    shipNames.put(mmsi, shipName);
                }
                if (destination != null && !destinations.containsKey(mmsi))
                {
                    destinations.put(mmsi, destination);
                }
            }
        }
    }
}
