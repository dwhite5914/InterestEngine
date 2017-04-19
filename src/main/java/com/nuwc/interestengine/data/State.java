package com.nuwc.interestengine.data;

import com.nuwc.interestengine.clustering.RouteObject;
import de.tuhh.luethke.okde.Exceptions.EmptyDistributionException;
import de.tuhh.luethke.okde.model.BaseSampleDistribution;
import de.tuhh.luethke.okde.model.SampleModel;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import org.ejml.simple.SimpleMatrix;
import org.math.plot.Plot3DPanel;

public class State
{
    private final double forgettingFactor = 1;
    private final double compressionThreshold = 0.02;
    private final float cx;
    private final float cy;
    private final float radius;
    private final RouteObject route;
    private List<StateVector> vectors;
    private SampleModel positionDist;
    private SampleModel velocityDist;

    private final Color colorScheme[] =
    {
        Color.BLUE.darker(),
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.YELLOW,
        Color.ORANGE,
        Color.RED,
        Color.RED.darker(),
    };

    public State(float cx, float cy, float radius, RouteObject route)
    {
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        this.route = route;

        calcVectors();
        calcPositionDistribution();
        calcVelocityDistribution();
    }

    public double evaluatePosition(StateVector vector)
    {
        return positionDist.evaluate(toVector(vector.x, vector.y));
    }

    public double evaluateVelocity(StateVector vector)
    {
        return velocityDist.evaluate(toVector(vector.vx, vector.vy));
    }

    private void calcPositionDistribution()
    {
        positionDist = new SampleModel(forgettingFactor, compressionThreshold);

        // Prepare samples:
        int numSamples = vectors.size();
        SimpleMatrix samples[] = new SimpleMatrix[numSamples];
        for (int i = 0; i < numSamples; i++)
        {
            StateVector vector = vectors.get(i);
            samples[i] = toVector(vector.x, vector.y);
        }

        // Initialize covariance matrix with zeros:
        double c[][] = new double[2][2];
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                c[i][j] = 0;
            }
        }

        // Prepare covariances and weights:
        SimpleMatrix cov[] = new SimpleMatrix[numSamples];
        double weights[] = new double[numSamples];
        for (int i = 0; i < numSamples; i++)
        {
            cov[i] = new SimpleMatrix(c);
            weights[i] = 1;
        }

        // Update distribution with samples:
        try
        {
            positionDist.updateDistribution(samples, cov, weights);
        }
        catch (EmptyDistributionException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException | InstantiationException
                | IllegalAccessException e)
        {
            System.out.println("Failed to update position distribution.");
        }
    }

    public double evaluatePositionCond(StateVector current, StateVector vector, float radius)
    {
        SampleModel condPositionDist = new SampleModel(1, 0.02);
        SimpleMatrix samples[] = new SimpleMatrix[10];
        SimpleMatrix cov[] = new SimpleMatrix[10];
        double weights[] = new double[10];
        double c[][] = new double[2][2];
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                c[i][j] = 0;
            }
        }

        for (int i = 0; i < 10; i++)
        {
            double r = Math.random() * radius;
            double theta = Math.random() * 2 * Math.PI;
            double dx = r * Math.cos(theta);
            double dy = r * Math.sin(theta);
            double x = current.x + dx;
            double y = current.y + dy;

            samples[i] = toVector(x, y);
            double prob = positionDist.evaluate(samples[i]);
            weights[i] = prob;
            cov[i] = new SimpleMatrix(c);
        }

        try
        {
            condPositionDist.updateDistribution(samples, cov, weights);
        }
        catch (EmptyDistributionException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException | InstantiationException
                | IllegalAccessException e)
        {
            System.out.println(
                    "Failed to update condtional position distribution.");
        }

        return condPositionDist.evaluate(toVector(vector.x, vector.y));
    }

    public Color[] evaluatePositionColors(StateVector current, List<AISPoint> points, int id)
    {
        List<Double> positionProbs = new ArrayList<>();
        double maxProb = 0;
        for (AISPoint point : points)
        {
            StateVector vector = point.toVector();
            double positionProb;
            if (id == 0)
            {
                positionProb = evaluatePosition(vector);
            }
            else
            {
                positionProb = evaluatePositionCond(current, vector, 2);
            }
            positionProbs.add(positionProb);
            if (positionProb > maxProb)
            {
                maxProb = positionProb;
            }
        }

        Color colors[] = new Color[points.size()];
        double delta = maxProb / colorScheme.length;
        for (int i = 0; i < points.size(); i++)
        {
            double positionProb = positionProbs.get(i);
            for (int j = 0; j < colorScheme.length; j++)
            {
                double low = j * delta;
                double high = low + delta;
                if (low <= positionProb && positionProb <= high)
                {
                    colors[i] = colorScheme[j];
                    break;
                }
            }
        }

        return colors;
    }

    public float getX(float lat, float lon)
    {
        return (float) (lon * 40000 * Math.cos(lat * Math.PI / 360) / 360 / 1.852);
    }

    public float getY(float lat, float lon)
    {
        return (float) (lat * 40000 / 360 / 1.852);
    }

    private void calcVelocityDistribution()
    {
        velocityDist = new SampleModel(forgettingFactor, compressionThreshold);

        // Prepare samples:
        int numSamples = vectors.size();
        SimpleMatrix samples[] = new SimpleMatrix[numSamples];
        for (int i = 0; i < numSamples; i++)
        {
            StateVector vector = vectors.get(i);
            samples[i] = toVector(vector.vx, vector.vy);
        }

        // Initialize covariance matrix with zeros:
        double c[][] = new double[2][2];
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                c[i][j] = 0;
            }
        }

        // Prepare covariances and weights:
        SimpleMatrix cov[] = new SimpleMatrix[numSamples];
        double weights[] = new double[numSamples];
        for (int i = 0; i < numSamples; i++)
        {
            cov[i] = new SimpleMatrix(c);
            weights[i] = 1;
        }

        // Update distribution with samples:
        try
        {
            velocityDist.updateDistribution(samples, cov, weights);
        }
        catch (EmptyDistributionException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException | InstantiationException
                | IllegalAccessException e)
        {
            System.out.println("Failed to update velocity distribution.");
        }
    }

    private SimpleMatrix toVector(double x, double y)
    {
        double sampleArray[][] =
        {
            {
                x
            },
            {
                y
            }
        };
        return new SimpleMatrix(sampleArray);
    }

    private void calcVectors()
    {
        vectors = new ArrayList<>();

        for (AISPoint point : route.points)
        {
            vectors.add(point.toVector());
        }
    }

    public boolean contains(AISPoint point)
    {
        float x = point.lat;
        float y = point.lon;
        float xSquared = (x - cx) * (x - cx);
        float ySquared = (y - cy) * (y - cy);
        double dist = Math.sqrt(xSquared + ySquared);

        return dist <= radius;
    }
}
