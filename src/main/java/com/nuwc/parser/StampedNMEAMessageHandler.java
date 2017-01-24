package com.nuwc.parser;

import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.Metadata;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StampedNMEAMessageHandler implements Consumer<StampedNMEAMessage>
{
    private static final Logger LOG
            = Logger.getLogger(StampedNMEAMessageHandler.class.getName());

    private final String source;
    private final ArrayList<NMEAMessage> messageFragments
            = new ArrayList<>();
    private final List<Consumer<? super StampedAISMessage>> aisMessageReceivers
            = new LinkedList<>();

    public StampedNMEAMessageHandler(String source,
            Consumer<? super StampedAISMessage>... aisMessageReceivers)
    {
        this.source = source;
        for (Consumer<? super StampedAISMessage> aisMessageReceiver
                : aisMessageReceivers)
        {
            addAisMessageReceiver(aisMessageReceiver);
        }
    }

    @Override
    public void accept(StampedNMEAMessage nmeaStamped)
    {
        NMEAMessage nmeaMessage = nmeaStamped.getMessage();
        long timestamp = nmeaStamped.getTimestamp();
        LOG.log(Level.FINER,
                "Received for processing: {0}",
                nmeaMessage.getRawMessage());

        if (!nmeaMessage.isValid())
        {
            LOG.log(Level.WARNING,
                    "NMEA message is invalid: {0}",
                    nmeaMessage.toString());
            return;
        }

        int numberOfFragments = nmeaMessage.getNumberOfFragments();
        if (numberOfFragments <= 0)
        {
            LOG.log(Level.WARNING,
                    "NMEA message is invalid: {0}",
                    nmeaMessage.toString());
            messageFragments.clear();
        }
        else if (numberOfFragments == 1)
        {
            LOG.finest("Handling unfragmented NMEA message");
            AISMessage aisMessage
                    = AISMessage.create(new Metadata(source), nmeaMessage);
            StampedAISMessage aisStamped
                    = new StampedAISMessage(aisMessage, timestamp);
            sendToAisMessageReceivers(aisStamped);
            messageFragments.clear();
        }
        else
        {
            int fragmentNumber = nmeaMessage.getFragmentNumber();
            LOG.log(Level.FINEST,
                    "Handling fragmented NMEA message with fragment number {0}",
                    fragmentNumber);
            if (fragmentNumber < 0)
            {
                LOG.log(Level.WARNING,
                        "Fragment number cannot be negative: {0}: {1}",
                        new Object[]
                        {
                            fragmentNumber,
                            nmeaMessage.getRawMessage()
                        });
                messageFragments.clear();
            }
            else if (fragmentNumber > numberOfFragments)
            {
                LOG.log(Level.FINE,
                        "Fragment number {0} higher than expected {1}: {2}",
                        new Object[]
                        {
                            fragmentNumber,
                            numberOfFragments,
                            nmeaMessage.getRawMessage()
                        });
                messageFragments.clear();
            }
            else
            {
                int expectedFragmentNumber = messageFragments.size() + 1;
                LOG.log(Level.FINEST,
                        "Expected fragment number is: {0}: {1}",
                        new Object[]
                        {
                            expectedFragmentNumber,
                            nmeaMessage.getRawMessage()
                        });

                if (expectedFragmentNumber != fragmentNumber)
                {
                    LOG.log(Level.FINE,
                            "Expected fragment number {0}; not {1}: {2}",
                            new Object[]
                            {
                                expectedFragmentNumber,
                                fragmentNumber,
                                nmeaMessage.getRawMessage()
                            });
                    messageFragments.clear();
                }
                else
                {
                    messageFragments.add(nmeaMessage);
                    LOG.log(Level.FINEST,
                            "nmeaMessage.getNumberOfFragments(): {0}",
                            nmeaMessage.getNumberOfFragments());
                    LOG.log(Level.FINEST,
                            "messageFragments.size(): {0}",
                            messageFragments.size());
                    if (nmeaMessage.getNumberOfFragments()
                            == messageFragments.size())
                    {
                        AISMessage aisMessage = AISMessage.create(
                                new Metadata(source),
                                messageFragments.toArray(
                                        new NMEAMessage[messageFragments.size()]));
                        StampedAISMessage aisStamped
                                = new StampedAISMessage(aisMessage, timestamp);
                        sendToAisMessageReceivers(aisStamped);
                        messageFragments.clear();
                    }
                    else
                    {
                        LOG.log(Level.FINEST,
                                "Fragmented message not yet complete; "
                                + "missing {0} fragment(s).",
                                nmeaMessage.getNumberOfFragments()
                                - messageFragments.size());
                    }
                }
            }
        }
    }

    private void sendToAisMessageReceivers(final StampedAISMessage aisStamped)
    {
        aisMessageReceivers.forEach(r -> r.accept(aisStamped));
    }

    @SuppressWarnings("unused")
    public void addAisMessageReceiver(
            Consumer<? super StampedAISMessage> aisMessageReceiver)
    {
        aisMessageReceivers.add(aisMessageReceiver);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<NMEAMessage> flush()
    {
        ArrayList<NMEAMessage> unhandled
                = (ArrayList<NMEAMessage>) messageFragments.clone();
        messageFragments.clear();
        return unhandled;
    }
}
