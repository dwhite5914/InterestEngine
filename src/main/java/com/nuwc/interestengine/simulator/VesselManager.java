package com.nuwc.interestengine.simulator;

import com.nuwc.interestengine.clustering.Vessel;
import com.nuwc.interestengine.data.AISPoint;
import com.nuwc.interestengine.map.RoutePainter;
import com.nuwc.interestengine.parser.StampedAISMessage;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class VesselManager
{
    private final float minLat, maxLat, minLon, maxLon;
    private final HashMap<Integer, Vessel> vessels;
    private final HashMap<Integer, String> shipTypes;
    private final RoutePainter painter;
    private final Timer timer;

    public VesselManager(float minLat, float maxLat, float minLon, float maxLon,
            RoutePainter painter)
    {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.painter = painter;

        vessels = new HashMap<>();
        shipTypes = new HashMap<>();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                painter.replaceVessels(vessels.values());
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
                }
                else
                {
                    vessel = new Vessel(mmsi);
                    if (shipTypes.containsKey(mmsi))
                    {
                        vessel.shipType = shipTypes.get(mmsi);
                    }
                    vessels.put(mmsi, vessel);
                }
                vessel.track.add(
                        new AISPoint(lat, lon, sog, cog, timestamp));
            }
        }

        if (aisMessage instanceof StaticDataReport)
        {
            StaticDataReport report = (StaticDataReport) aisMessage;
            String shipType = (report.getShipType() == null)
                    ? "NotAvailable"
                    : report.getShipType().getValue();

            Vessel vessel;
            if (vessels.containsKey(mmsi))
            {
                vessel = vessels.get(mmsi);
                vessel.shipType = shipType;
            }
            else if (!shipTypes.containsKey(mmsi))
            {
                shipTypes.put(mmsi, shipType);
            }
        }
    }
}
