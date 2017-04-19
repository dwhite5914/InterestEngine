package com.nuwc.interestengine.data;

import com.nuwc.interestengine.clustering.RouteObject;
import de.tuhh.luethke.okde.Exceptions.EmptyDistributionException;
import de.tuhh.luethke.okde.model.SampleModel;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.ejml.simple.SimpleMatrix;

public class KernelDensityEstimator
{
    private final double forgettingFactor;
    private final double compressionThreshold;
    private List<StateVector> vectors;
    private SampleModel positionDist;
    private SampleModel velocityDist;
    private SampleModel xVelXDist;
    private SampleModel xVelYDist;
    private SampleModel yVelXDist;
    private SampleModel yVelYDist;

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

    public KernelDensityEstimator()
    {
        this(1, 0.02);
    }

    public KernelDensityEstimator(double forgettingFactor,
            double compressionThreshold)
    {
        this.forgettingFactor = forgettingFactor;
        this.compressionThreshold = compressionThreshold;
    }

    public void fit(RouteObject route)
    {
        vectors = new ArrayList<>();
        for (AISPoint point : route.points)
        {
            vectors.add(point.toVector());
        }

        calcPositionDistribution();
        calcVelocityDistribution();
        calcVelocityDists();
    }

    public void calcVelocityDists()
    {
        velocityDist = new SampleModel(forgettingFactor, compressionThreshold);
        xVelXDist = new SampleModel(forgettingFactor, compressionThreshold);
        xVelYDist = new SampleModel(forgettingFactor, compressionThreshold);
        yVelXDist = new SampleModel(forgettingFactor, compressionThreshold);
        yVelYDist = new SampleModel(forgettingFactor, compressionThreshold);

        int numSamples = vectors.size();
        SimpleMatrix samplesXVelX[] = new SimpleMatrix[numSamples];
        SimpleMatrix samplesXVelY[] = new SimpleMatrix[numSamples];
        SimpleMatrix samplesYVelX[] = new SimpleMatrix[numSamples];
        SimpleMatrix samplesYVelY[] = new SimpleMatrix[numSamples];
        for (int i = 0; i < numSamples; i++)
        {
            StateVector vector = vectors.get(i);
            samplesXVelX[i] = toMatrix(vector.x, vector.vx);
            samplesXVelY[i] = toMatrix(vector.x, vector.vy);
            samplesYVelX[i] = toMatrix(vector.y, vector.vx);
            samplesYVelY[i] = toMatrix(vector.y, vector.vy);
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

        SimpleMatrix covXVelX[] = new SimpleMatrix[numSamples];
        SimpleMatrix covXVelY[] = new SimpleMatrix[numSamples];
        SimpleMatrix covYVelX[] = new SimpleMatrix[numSamples];
        SimpleMatrix covYVelY[] = new SimpleMatrix[numSamples];
        double weightsXVelX[] = new double[numSamples];
        double weightsXVelY[] = new double[numSamples];
        double weightsYVelX[] = new double[numSamples];
        double weightsYVelY[] = new double[numSamples];

        for (int i = 0; i < numSamples; i++)
        {
            covXVelX[i] = new SimpleMatrix(c);
            covXVelY[i] = new SimpleMatrix(c);
            covYVelX[i] = new SimpleMatrix(c);
            covYVelY[i] = new SimpleMatrix(c);
            weightsXVelX[i] = 1;
            weightsXVelY[i] = 1;
            weightsYVelX[i] = 1;
            weightsYVelY[i] = 1;
        }

        // Update distribution with samples:
        try
        {
            xVelXDist.updateDistribution(samplesXVelX, covXVelX, weightsXVelX);
            xVelYDist.updateDistribution(samplesXVelY, covXVelY, weightsXVelY);
            yVelXDist.updateDistribution(samplesYVelX, covYVelX, weightsYVelX);
            yVelYDist.updateDistribution(samplesYVelY, covYVelY, weightsYVelY);
        }
        catch (EmptyDistributionException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException | InstantiationException
                | IllegalAccessException e)
        {
            System.out.println("Failed to update velocity distribution.");
        }
    }

    public double evaluateVelocityConditional(StateVector vector)
    {
        double xVelXProb = xVelXDist.evaluate(toMatrix(vector.x, vector.vx));
        double xVelYProb = xVelYDist.evaluate(toMatrix(vector.x, vector.vy));
        double yVelXProb = yVelXDist.evaluate(toMatrix(vector.y, vector.vx));
        double yVelYProb = yVelYDist.evaluate(toMatrix(vector.y, vector.vy));

        return xVelXProb * xVelYProb * yVelXProb * yVelYProb;
    }

    public double evaluatePositionConditional(StateVector current, StateVector vector, float radius)
    {
        int numSamples = 100;
        SampleModel condPositionDist = new SampleModel(1, 0.02);
        SimpleMatrix samples[] = new SimpleMatrix[numSamples];
        SimpleMatrix cov[] = new SimpleMatrix[numSamples];
        double weights[] = new double[numSamples];
        double c[][] = new double[2][2];
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                c[i][j] = 0;
            }
        }

        int count = 0;
        double probSum = 0;
        for (int i = 0; i < numSamples; i++)
        {
            double r = Math.random() * radius;
            double theta = Math.random() * 2 * Math.PI;
            double dx = r * Math.cos(theta);
            double dy = r * Math.sin(theta);
            double x = current.x + dx;
            double y = current.y + dy;

            samples[i] = toMatrix(x, y);
            double prob = positionDist.evaluate(samples[i]);
            probSum += prob;
            if (prob == 0)
            {
                count++;
            }
            weights[i] = prob;
            cov[i] = new SimpleMatrix(c);
        }

        if (probSum == 0)
        {
            return 0;
        }

        return positionDist.evaluate(toMatrix(vector.x, vector.y)) / probSum;

//        if (count == numSamples)
//        {
//            return 0;
//        }
//
//        try
//        {
//            condPositionDist.updateDistribution(samples, cov, weights);
//        }
//        catch (EmptyDistributionException | IllegalArgumentException
//                | InvocationTargetException | NoSuchMethodException
//                | SecurityException | InstantiationException
//                | IllegalAccessException e)
//        {
//            System.out.println(
//                    "Failed to update condtional position distribution.");
//            return 0;
//        }
//        catch (RuntimeException e)
//        {
//            System.out.println("Failed to decompose matrix.");
//            return 0;
//        }
//
//        return condPositionDist.evaluate(toMatrix(vector.x, vector.y));
    }

    public double evaluatePositionCond(StateVector current, StateVector vector, float radius)
    {
        int numSamples = 10;
        SampleModel condPositionDist = new SampleModel(1, 0.02);
        SimpleMatrix samples[] = new SimpleMatrix[numSamples];
        SimpleMatrix cov[] = new SimpleMatrix[numSamples];
        double weights[] = new double[numSamples];
        double c[][] = new double[2][2];
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                c[i][j] = 0;
            }
        }

        int count = 0;
        for (int i = 0; i < numSamples; i++)
        {
            double r = Math.random() * radius;
            double theta = Math.random() * 2 * Math.PI;
            double dx = r * Math.cos(theta);
            double dy = r * Math.sin(theta);
            double x = current.x + dx;
            double y = current.y + dy;

            samples[i] = toMatrix(x, y);
            double prob = positionDist.evaluate(samples[i]);
            if (prob == 0)
            {
                count++;
            }
            weights[i] = prob;
            cov[i] = new SimpleMatrix(c);
        }

        if (count == numSamples)
        {
            return 0;
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
            return 0;
        }
        catch (RuntimeException e)
        {
            System.out.println("Failed to decompose matrix.");
            return 0;
        }

        return condPositionDist.evaluate(toMatrix(vector.x, vector.y));
    }

    public double evaluatePosition(StateVector vector)
    {
        if (vectors == null)
        {
            return -1;
        }

        return positionDist.evaluate(toMatrix(vector.x, vector.y));
    }

    public double evaluateVelocity(StateVector vector)
    {
        if (vectors == null)
        {
            return -1;
        }

        return velocityDist.evaluate(toMatrix(vector.vx, vector.vy));
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

    private void calcPositionDistribution()
    {
        positionDist = new SampleModel(forgettingFactor, compressionThreshold);

        // Prepare samples:
        int numSamples = vectors.size();
        SimpleMatrix samples[] = new SimpleMatrix[numSamples];
        for (int i = 0; i < numSamples; i++)
        {
            StateVector vector = vectors.get(i);
            samples[i] = toMatrix(vector.x, vector.y);
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
        try
        {
            List<SimpleMatrix> initSamples = new ArrayList<>();
            initSamples.add(samples[0]);
            initSamples.add(samples[1]);
            initSamples.add(samples[2]);
            SimpleMatrix cov[] =
            {
                new SimpleMatrix(c), new SimpleMatrix(c), new SimpleMatrix(c)
            };
            double weights[] =
            {
                1, 1, 1
            };

            positionDist.updateDistribution(
                    initSamples.toArray(new SimpleMatrix[3]), cov, weights);

            for (int i = 3; i < numSamples; i++)
            {
                SimpleMatrix pos = samples[i];
                positionDist.updateDistribution(pos, new SimpleMatrix(c), 1d);
            }
        }
        catch (EmptyDistributionException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException | InstantiationException
                | IllegalAccessException e)
        {
            System.out.println("Failed to update position distribution.");
        }
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
            samples[i] = toMatrix(vector.vx, vector.vy);
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

    private SimpleMatrix toMatrix(double x, double y)
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
}
