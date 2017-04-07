package com.nuwc.interestengine.simulator;

import com.nuwc.interestengine.parser.StampedAISMessage;
import com.nuwc.interestengine.parser.StampedNMEAMessage;
import dk.tbsalling.aismessages.ais.messages.AISMessage;
import dk.tbsalling.aismessages.ais.messages.Metadata;
import dk.tbsalling.aismessages.nmea.exceptions.InvalidMessage;
import dk.tbsalling.aismessages.nmea.exceptions.NMEAParseException;
import dk.tbsalling.aismessages.nmea.exceptions.UnsupportedMessageType;
import dk.tbsalling.aismessages.nmea.messages.NMEAMessage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageConsumer implements Runnable
{
    private static final Logger LOG
            = Logger.getLogger(MessageConsumer.class.getName());

    private final String source = "SRC";
    private final ArrayList<NMEAMessage> messageFragments
            = new ArrayList<>();
    private final List<Consumer<? super StampedAISMessage>> aisMessageReceivers
            = new LinkedList<>();

    private Boolean stopRequested = false;
    ConcurrentLinkedQueue messageQueue;
    VesselManager manager;

    public MessageConsumer(ConcurrentLinkedQueue messageQueue,
            VesselManager manager)
    {
        this.messageQueue = messageQueue;
        this.manager = manager;
    }

    public final synchronized void requestStop()
    {
        this.stopRequested = true;
    }

    private synchronized Boolean stopRequested()
    {
        return this.stopRequested;
    }

    private Object[] splitPacket(String item)
    {
        String packet = "";

        // Retrive timestamp from line and record in hashmap.
        String elements[] = item.split(",");
        String lastElement = elements[elements.length - 1];
        long timestamp = Long.parseLong(lastElement);

        // Search for star which marks end of data string.
        boolean starFound = false;
        char charArray[] = item.toCharArray();
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
                packet = item.substring(0, i);
                break;
            }
        }

        return new Object[]
        {
            packet, timestamp
        };
    }

    @Override
    public void run()
    {
        while (!stopRequested())
        {
            while (!stopRequested() && !messageQueue.isEmpty())
            {
                String item = (String) messageQueue.poll();
                try
                {
                    Object parts[] = splitPacket(item);
                    String packet = (String) parts[0];
                    long timestamp = (long) parts[1];

                    // Assemble stamped NMEA message.
                    NMEAMessage nmea = NMEAMessage.fromString(packet);
                    StampedNMEAMessage nmeaStamped
                            = new StampedNMEAMessage(nmea, timestamp);
                    accept(nmeaStamped);
                    LOG.log(Level.FINE, "Received: {0}", nmea.toString());
                }
                catch (InvalidMessage e)
                {
                    LOG.log(Level.WARNING,
                            "Received invalid AIS message: \"{0}\"",
                            item);
                }
                catch (UnsupportedMessageType e)
                {
                    LOG.log(Level.WARNING,
                            "Received unsupported NMEA message: \"{0}\"",
                            item);
                }
                catch (NMEAParseException e)
                {
                    LOG.log(Level.WARNING,
                            "Received non-compliant NMEA message: \"{0}\"",
                            item);
                }
            }
        }
    }

    public void accept(StampedNMEAMessage nmeaStamped)
    {
        try
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
                manager.processData(aisStamped);
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
                            manager.processData(aisStamped);
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
        catch (Exception e)
        {
            // Pass
        }
    }
}
