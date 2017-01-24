package com.nuwc.parser;

import com.nuwc.interestengine.map.TriMarker;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.PositionReport;
import dk.tbsalling.aismessages.ais.messages.ShipAndVoyageData;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import dk.tbsalling.aismessages.ais.messages.types.NavigationStatus;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NMEAParser
{
    private static final Logger LOG
            = Logger.getLogger(NMEAParser.class.getName());

    private final String path;
    private final HashMap<Integer, NavigationStatus> statuses = new HashMap<>();
    private final HashMap<Integer, CrudeTrack> crudeTracks = new HashMap<>();
    private final List<TriMarker> markers;

    public NMEAParser(String path, List<TriMarker> markers)
    {
        this.path = path;
        this.markers = markers;
    }

    public void parse()
    {
        // Initialize the file input stream.
        InputStream stream = null;
        try
        {
            stream = new FileInputStream(path);
        }
        catch (FileNotFoundException e)
        {
            LOG.log(Level.SEVERE, "Failed to open file input stream.", e);
        }

        // Stream the file into AIS messages and parse each message.
        StampedAISInputStreamReader streamReader;
        streamReader = new StampedAISInputStreamReader(stream,
                aisStamped
                ->
        {
            AISMessage aisMessage = aisStamped.getMessage();
            long timestamp = aisStamped.getTimestamp();
            int mmsi = aisMessage.getSourceMmsi().getMMSI();

            if (aisMessage instanceof DynamicDataReport)
            {
                // Cast to dynamic data report and retrieve key data.
                DynamicDataReport report = (DynamicDataReport) aisMessage;
                float lat = report.getLatitude();
                float lon = report.getLongitude();
                float sog = report.getSpeedOverGround();
                float cog = report.getCourseOverGround();

                // Add visual marker to aid in data visualization.
                TriMarker marker = new TriMarker(lat, lon, cog);
                markers.add(marker);

                CrudeTrack track;
                if (crudeTracks.get(mmsi) == null)
                {
                    track = new CrudeTrack();
                }
                else
                {
                    track = crudeTracks.get(mmsi);
                }
                AISPoint point = new AISPoint();
                point.lat = lat;
                point.lon = lon;
                point.sog = sog;
                point.cog = cog;
                point.timestamp = timestamp;
                track.getTrack().add(point);
            }

            if (aisMessage instanceof PositionReport)
            {
                // Cast to position report and retrieve key data.
                PositionReport report = (PositionReport) aisMessage;
                NavigationStatus status = report.getNavigationStatus();
                NavigationStatus oldStatus = statuses.get(mmsi);
                if (oldStatus == null)
                {
                    statuses.put(mmsi, status);
                }
                else if (oldStatus != status)
                {
                    if ((oldStatus == NavigationStatus.AtAnchor
                            || oldStatus == NavigationStatus.Moored
                            || oldStatus == NavigationStatus.EngagedInFising)
                            && (status == NavigationStatus.UnderwaySailing
                            || status == NavigationStatus.UnderwayUsingEngine))
                    {
                        CrudeTrack track;
                        if (crudeTracks.get(mmsi) == null)
                        {
                            track = new CrudeTrack();
                        }
                        else
                        {
                            track = crudeTracks.get(mmsi);
                        }
                        track.getSpecials().put(timestamp, SPECIAL.START);
                        crudeTracks.put(mmsi, track);
                    }
                    else if ((oldStatus == NavigationStatus.UnderwaySailing
                            || oldStatus == NavigationStatus.UnderwayUsingEngine)
                            && (status == NavigationStatus.AtAnchor
                            || status == NavigationStatus.Moored
                            || status == NavigationStatus.EngagedInFising))
                    {
                        CrudeTrack track;
                        if (crudeTracks.get(mmsi) == null)
                        {
                            track = new CrudeTrack();
                        }
                        else
                        {
                            track = crudeTracks.get(mmsi);
                        }
                        track.getSpecials().put(timestamp, SPECIAL.END);
                        crudeTracks.put(mmsi, track);
                    }
                }
            }

            if (aisMessage instanceof StaticDataReport)
            {
                // Cast to static data report and retrieve key data.
                StaticDataReport report = (StaticDataReport) aisMessage;
                if (report.getToBow() != null && report.getToStern() != null)
                {
                    String shipType = (report.getShipType() == null)
                            ? "undefined"
                            : report.getShipType().getValue();
                    float length = report.getToBow() + report.getToStern();

                    CrudeTrack track;
                    if (crudeTracks.get(mmsi) == null)
                    {
                        track = new CrudeTrack();
                    }
                    else
                    {
                        track = crudeTracks.get(mmsi);
                    }
                    track.setMmsi(mmsi);
                    track.setType(shipType);
                    track.setLength(length);
                    crudeTracks.put(mmsi, track);
                }
                else
                {
                    // ...Do something...
                }
            }

            if (aisMessage instanceof ShipAndVoyageData)
            {
                // Cast to ship and voyage data and retrieve key data.
                ShipAndVoyageData report = (ShipAndVoyageData) aisMessage;
                String destName = report.getDestination();
            }
        }
        );

        // Run the message parser.
        try
        {
            streamReader.run();
        }
        catch (IOException e)
        {
            LOG.log(Level.SEVERE, "Failed to run message parser", e);
        }
        LOG.info("-----DONE-----");

        // TEST
        for (Integer mmsi : crudeTracks.keySet())
        {
            CrudeTrack track = crudeTracks.get(mmsi);
            if (track.getSpecials().keySet().size() > 0)
            {
                System.out.print("mmsi = " + track.getMmsi() + ", ");
                for (Long timestamp : track.getSpecials().keySet())
                {
                    SPECIAL special = track.getSpecials().get(timestamp);
                    System.out.print("(" + timestamp + ", " + track.getSpecials().get(timestamp) + "), ");
                }
                System.out.print("\n");
            }
        }
    }
}
