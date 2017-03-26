package com.nuwc.interestengine.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class StampedAISInputStreamReader
{
    public StampedAISInputStreamReader(InputStream inputStream, Consumer<? super StampedAISMessage> aisMessageConsumer)
    {
        this.nmeaMessageHandler = new StampedNMEAMessageHandler("SRC", aisMessageConsumer);
        this.nmeaMessageInputStreamReader = new StampedNMEAMessageInputStreamReader(inputStream, this.nmeaMessageHandler::accept);
    }

    public final synchronized void requestStop()
    {
        this.stopRequested = true;
    }

    public final synchronized boolean isStopRequested()
    {
        return stopRequested;
    }

    public void run() throws IOException
    {
        this.nmeaMessageInputStreamReader.run();
    }

    private boolean stopRequested = false;
    private final StampedNMEAMessageHandler nmeaMessageHandler;
    private final StampedNMEAMessageInputStreamReader nmeaMessageInputStreamReader;
}
