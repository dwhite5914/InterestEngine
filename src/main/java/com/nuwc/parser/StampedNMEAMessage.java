package com.nuwc.parser;

import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;

public class StampedNMEAMessage
{
    private NMEAMessage message;
    private long timestamp;

    public StampedNMEAMessage(NMEAMessage message, long timestamp)
    {
        this.message = message;
        this.timestamp = timestamp;
    }

    public NMEAMessage getMessage()
    {
        return message;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setMessage(NMEAMessage message)
    {
        this.message = message;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }
}
