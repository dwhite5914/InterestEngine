package com.nuwc.interestengine.data;

public class DataPoint
{
    public int mmsi;
    public float lat;
    public float lon;
    public float sog;
    public float cog;
    public long timestamp;
    public String shipType;

    public DataPoint()
    {
        // Pass.
    }
}
