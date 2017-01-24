package com.nuwc.parser;

import dk.tbsalling.aismessages.ais.messages.AISMessage;

public class StampedAISMessage
{
    private AISMessage message;
    private long timestamp;

    public StampedAISMessage(AISMessage message, long timestamp)
    {
        this.message = message;
        this.timestamp = timestamp;
    }

    public AISMessage getMessage()
    {
        return message;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setMessage(AISMessage message)
    {
        this.message = message;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }
}
