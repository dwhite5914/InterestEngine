package com.nuwc.parser;

import com.nuwc.clustering.Vessel;
import com.nuwc.data.AISPoint;
import com.nuwc.data.DataPoint;
import com.nuwc.data.Database;
import com.nuwc.interestengine.map.TriMarker;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.DynamicDataReport;
import dk.tbsalling.aismessages.ais.messages.StaticDataReport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class NMEAParser
{
    private final File[] files;
    private final Database db;
    private final float minLat, maxLat, minLon, maxLon;

    private final HashMap<Integer, Vessel> vessels;
    private final HashMap<Integer, String> shipTypes;
    private int test = 0;

    public NMEAParser(File[] files, List<TriMarker> markers, Database db,
            float minLat, float maxLat, float minLon, float maxLon)
    {
        this.files = files;
        this.db = db;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;

        vessels = new HashMap<>();
        shipTypes = new HashMap<>();
    }

    public void parse()
    {
        for (File file : files)
        {
            String path = file.getAbsolutePath();

            // Initialize the file input stream.
            InputStream stream = null;
            try
            {
                stream = new FileInputStream(path);
            }
            catch (FileNotFoundException e)
            {
                System.out.println("Failed to open file input stream.");
            }

            System.out.println("Parsing file at: " + path);

            // Stream the file into AIS messages and parse each message.
            StampedAISInputStreamReader streamReader;
            streamReader = new StampedAISInputStreamReader(stream, aisStamped ->
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
            );

            // Run the message parser.
            try
            {
                streamReader.run();
            }
            catch (IOException e)
            {
                System.out.println("Failed to run message parser.");
            }

            try
            {
                stream.close();
            }
            catch (IOException ex)
            {
                System.out.println("Failed to close stream.");
            }

            System.out.println("Finished parsing file at: " + path);
        }

        System.out.println("# Vessels = " + vessels.values().size());

        clearTables();
        insertTables();

        int count = 0;
        int zero = 0;
        int mmsizero = 0;
        int nulltype = 0;

        int id = 0;
        for (Vessel vessel : vessels.values())
        {
            count++;
            if (vessel.shipType == null)
            {
                vessel.shipType = "NotAvailable";
                count++;
            }

            if (vessel.track.isEmpty())
            {
                zero++;
            }

            if (vessel.mmsi == 0)
            {
                mmsizero++;
            }

            if (vessel.shipType == null)
            {
                nulltype++;
            }

            for (AISPoint point : vessel.track)
            {
                DataPoint p = new DataPoint();
                p.mmsi = vessel.mmsi;
                p.lat = point.lat;
                p.lon = point.lon;
                p.sog = point.sog;
                p.cog = point.cog;
                p.shipType = vessel.shipType;
                p.timestamp = point.timestamp;
                insertDataPoint(id++, p);
            }
        }

        System.out.println("COUNT = " + count);
        System.out.println("zero = " + zero);
        System.out.println("mmsizero = " + mmsizero);
        System.out.println("nulltype = " + nulltype);
        System.out.println("DONE");
    }

    private void insertTables()
    {
        String sql;
        sql = "CREATE TABLE VesselData(\n"
                + "    id INT NOT NULL,\n"
                + "    mmsi INT NOT NULL,\n"
                + "    lat FLOAT NOT NULL,\n"
                + "    lon FLOAT NOT NULL,\n"
                + "    sog FLOAT NOT NULL,\n"
                + "    cog FLOAT NOT NULL,\n"
                + "    shipType VARCHAR (40) NOT NULL,\n"
                + "    timestamp BIGINT NOT NULL,\n"
                + "    PRIMARY KEY (id)\n"
                + ")";
        db.runStatement(sql);
    }

    private void clearTables()
    {
        String sql;
        sql = "TRUNCATE TABLE VesselData";
        db.runStatement(sql);
        System.out.println("DROPPED");
    }

    private void insertDataPoint(int id, DataPoint p)
    {
        String sql = String.format(
                "INSERT INTO VesselData VALUES (%d, %d, %f, %f, %f, %f, '%s', %d)\n",
                id, p.mmsi, p.lat, p.lon, p.sog, p.cog, p.shipType, p.timestamp);
        db.runStatement(sql);
    }
}
