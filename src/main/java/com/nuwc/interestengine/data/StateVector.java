package com.nuwc.interestengine.data;

public class StateVector
{
    public float x;
    public float y;
    public float vx;
    public float vy;

    public StateVector()
    {
        this(0, 0, 0, 0);
    }

    public StateVector(float x, float y, float vx, float vy)
    {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }
}
