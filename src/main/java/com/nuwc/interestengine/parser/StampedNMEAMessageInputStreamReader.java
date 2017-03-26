package com.nuwc.interestengine.parser;

import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.exceptions.NMEAParseException;
import dk.tbsalling.aismessages.nmea.exceptions.UnsupportedMessageType;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StampedNMEAMessageInputStreamReader
{
    private static final Logger LOG
            = Logger.getLogger(StampedNMEAMessageInputStreamReader.class.getName());

    private Boolean stopRequested = false;
    private final InputStream inputStream;
    private final Consumer<? super StampedNMEAMessage> nmeaMessageHandler;

    public StampedNMEAMessageInputStreamReader(InputStream inputStream,
            Consumer<? super StampedNMEAMessage> nmeaMessageHandler)
    {
        this.nmeaMessageHandler = nmeaMessageHandler;
        this.inputStream = inputStream;
    }

    public final synchronized void requestStop()
    {
        this.stopRequested = true;
    }

    public void run() throws IOException
    {
        LOG.info("NMEAMessageInputStreamReader running.");

        InputStreamReader reader = new InputStreamReader(inputStream,
                Charset.defaultCharset());
        BufferedReader bufferedReader = new BufferedReader(reader);
        String string;
        while ((string = bufferedReader.readLine()) != null && !stopRequested())
        {
            try
            {
                // Retrive timestamp from line and record in hashmap.
                String elements[] = string.split(",");
                String lastElement = elements[elements.length - 1];
                long timestamp = Long.parseLong(lastElement);

                // Search for star which marks end of data string.
                boolean starFound = false;
                char charArray[] = string.toCharArray();
                for (int i = 0; i < charArray.length; i++)
                {
                    // Flag upon finding a star.
                    if (charArray[i] == '*')
                    {
                        starFound = true;
                    }

                    // Look for first comma after star.
                    if (starFound && charArray[i] == ',')
                    {
                        // Remove timestamp starting at comma.
                        string = string.substring(0, i);
                        break;
                    }
                }

                // Assemble stamped NMEA message.
                NMEAMessage nmea = NMEAMessage.fromString(string);
                StampedNMEAMessage nmeaStamped
                        = new StampedNMEAMessage(nmea, timestamp);
                nmeaMessageHandler.accept(nmeaStamped);
                LOG.log(Level.FINE, "Received: {0}", nmea.toString());
            }
            catch (InvalidMessage e)
            {
                LOG.log(Level.WARNING,
                        "Received invalid AIS message: \"{0}\"",
                        string);
            }
            catch (UnsupportedMessageType e)
            {
                LOG.log(Level.WARNING,
                        "Received unsupported NMEA message: \"{0}\"",
                        string);
            }
            catch (NMEAParseException e)
            {
                LOG.log(Level.WARNING,
                        "Received non-compliant NMEA message: \"{0}\"",
                        string);
            }
        }

        LOG.info("NMEAMessageInputStreamReader stopping.");
    }

    private synchronized Boolean stopRequested()
    {
        return this.stopRequested;
    }
}
