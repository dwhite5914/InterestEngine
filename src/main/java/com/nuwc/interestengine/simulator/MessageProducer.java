package com.nuwc.interestengine.simulator;

import com.nuwc.interestengine.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageProducer implements Runnable
{
    private final File files[];
    private Boolean stopRequested = false;
    ConcurrentLinkedQueue messageQueue;
    private long firstTimestamp = 0;
    private long firstTime = 0;
    private float simRate = 1;

    public MessageProducer(File files[], ConcurrentLinkedQueue messageQueue)
    {
        this.files = files;
        this.messageQueue = messageQueue;
    }

    public synchronized void setSimRate(float simRate)
    {
        this.simRate = simRate;
    }

    public synchronized float getSimRate(float simRate)
    {
        return simRate;
    }

    public final synchronized void requestStop()
    {
        this.stopRequested = true;
    }

    private synchronized Boolean stopRequested()
    {
        return this.stopRequested;
    }

    private long getTimestamp(String line)
    {
        // Retrive timestamp from line and record in hashmap.
        String elements[] = line.split(",");
        String lastElement = elements[elements.length - 1];
        long timestamp = Long.parseLong(lastElement);

        return timestamp;
    }

    @Override
    public void run()
    {
        boolean first = true;

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
                continue;
            }
            InputStreamReader reader = new InputStreamReader(stream,
                    Charset.defaultCharset());
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            try
            {
                while ((line = bufferedReader.readLine()) != null && !stopRequested())
                {
                    long timestamp, time, fileTime, realTime;
                    do
                    {
                        timestamp = getTimestamp(line);
                        time = System.currentTimeMillis();
                        if (first)
                        {
                            firstTimestamp = timestamp;
                            firstTime = time;
                            first = false;
                        }

                        fileTime = (timestamp - firstTimestamp) * 1000;
                        realTime = time - firstTime;
                    }
                    while (fileTime > simRate * realTime);

                    messageQueue.offer(line);
                }
            }
            catch (IOException e)
            {
                System.out.println("Failed to run simulator.");
            }
        }
    }
}
