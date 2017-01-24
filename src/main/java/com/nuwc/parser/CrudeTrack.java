package com.nuwc.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CrudeTrack
{
    private final List<AISPoint> track;
    private final HashMap<Long, SPECIAL> specials;
    private int mmsi;
    private String type;
    private float length;

    public CrudeTrack()
    {
        track = new ArrayList<>();
        specials = new HashMap<>();
    }

    public List<AISPoint> getTrack()
    {
        return track;
    }

    public HashMap<Long, SPECIAL> getSpecials()
    {
        return specials;
    }

    public int getMmsi()
    {
        return mmsi;
    }

    public String getType()
    {
        return type;
    }

    public float getLength()
    {
        return length;
    }

    public void setMmsi(int mmsi)
    {
        this.mmsi = mmsi;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setLength(float length)
    {
        this.length = length;
    }
}

enum SPECIAL
{
    START,
    END
}
