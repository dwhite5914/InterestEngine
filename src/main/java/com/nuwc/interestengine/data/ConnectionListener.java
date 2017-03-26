package com.nuwc.interestengine.data;

public interface ConnectionListener
{
    public void connectionStarted();
    public void connectionInterrupted();
    public void connectionEnded();
}
