package com.nuwc.interestengine.data;

public class Tile
{
    double xLow, yLow, xUpp, yUpp;

    public Tile(double xLow, double yLow, double xUpp, double yUpp)
    {
        this.xLow = xLow;
        this.yLow = yLow;
        this.xUpp = xUpp;
        this.yUpp = yUpp;
    }

    public boolean contains(AISPoint point)
    {
        StateVector vector = point.toVector();
        if (xLow <= vector.x && vector.x < xUpp
                && yLow <= vector.y && vector.y < yUpp)
        {
            return true;
        }

        return false;
    }
}
