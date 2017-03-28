package com.nuwc.interestengine.neuralnet;

import com.nuwc.interestengine.Utils;
import com.nuwc.interestengine.clustering.RouteObject;
import com.nuwc.interestengine.data.AISPoint;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.spark.api.TrainingMaster;
import org.deeplearning4j.spark.impl.multilayer.SparkDl4jMultiLayer;
import org.deeplearning4j.spark.impl.paramavg.ParameterAveragingTrainingMaster;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeuralNet
{
    private int seed = 123;
    private int numInputs;
    private int numHidden;
    private int numOutputs;
    private int batchSize;
    private int numEpochs;
    private double learningRate;
    private final List<RouteObject> routes;
    private double sogMean;
    private double sogStdev;
    private List<String> types;
    private SparkDl4jMultiLayer sparkNet;

    private static final Logger log
            = LoggerFactory.getLogger(NeuralNet.class);

    public NeuralNet(int numInputs, int numHidden, int numOutputs,
            int batchSize, int numEpochs, double learningRate,
            List<RouteObject> routes)
    {
        this.numInputs = numInputs;
        this.numHidden = numHidden;
        this.numOutputs = numOutputs;
        this.batchSize = batchSize;
        this.numEpochs = numEpochs;
        this.learningRate = learningRate;
        this.routes = routes;
        this.types = new ArrayList<>();
        sogStats();
    }

    public void test()
    {
        double lat = preprocess("lat", 45.0f);
        double lon = preprocess("lon", 14.5f);
        double sog = preprocess("sog", 3.0f);
        double cog = preprocess("cog", 45.0f);
        double type = preprocess("type", "NotAvailable");
        Vector result = sparkNet.predict(Vectors.dense(new double[]
        {
            lat, lon, sog, cog, type
        }));
        double prediction[] = result.toArray();
        System.out.println("***** SAMPLE: *****");
        for (double d : prediction)
        {
            System.out.print(d + " ");
        }
    }

    public void train()
    {
        DataSet allData = getDataSet();
        allData.shuffle();
        SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.9);
        DataSet trainDataSet = testAndTrain.getTrain();
        DataSet testDataSet = testAndTrain.getTest();
        List<DataSet> trainBatches = trainDataSet.batchBy(batchSize);
        List<DataSet> testBatches = testDataSet.batchBy(batchSize);

        System.out.println("# Train Batches = " + trainBatches.size());
        System.out.println("# Test Batches = " + testBatches.size());

        SparkConf sparkConfig = new SparkConf();
        sparkConfig.setMaster("local[*]");
        sparkConfig.setAppName("DL4J Spark Learner");
        JavaSparkContext sc = new JavaSparkContext(sparkConfig);

        JavaRDD<DataSet> trainData = sc.parallelize(trainBatches);
        JavaRDD<DataSet> testData = sc.parallelize(testBatches);

        MultiLayerConfiguration config = getNetConfig();
        TrainingMaster tm
                = new ParameterAveragingTrainingMaster.Builder(batchSize)
                        .averagingFrequency(5)
                        .workerPrefetchNumBatches(2)
                        .batchSizePerWorker(batchSize)
                        .build();

        sparkNet = new SparkDl4jMultiLayer(sc, config, tm);
        for (int i = 0; i < numEpochs; i++)
        {
            sparkNet.fit(trainData);
            log.info("Complete Epoch {}", i);
        }

        Evaluation eval = sparkNet.evaluate(testData);
        log.info("***** Evaluation *****");
        log.info(eval.stats());

        tm.deleteTempFiles(sc);

        for (int i = 0; i < types.size(); i++)
        {
            System.out.println(i + " -> " + types.get(i));
        }

        File nnDir = new File(Utils.getResource("") + "/neuralnet");
        if (!nnDir.exists())
        {
            nnDir.mkdir();
        }

        File nnFile = new File(nnDir.getAbsolutePath() + "/trained_model.zip");
        boolean saveUpdaters = true;
        MultiLayerNetwork network = sparkNet.getNetwork();
        try
        {
            ModelSerializer.writeModel(network, nnFile, saveUpdaters);
        }
        catch (IOException e)
        {
            System.out.println("Failed to save neural network.");
        }
    }

    public DataSet getDataSet()
    {
        int id = 0, k = 0;
        int n = numPoints();
        double data[][] = new double[n][numInputs];
        float labelData[][] = new float[n][numOutputs];
        for (RouteObject route : routes)
        {
            for (AISPoint point : route.points)
            {
                data[k][0] = preprocess("lat", point.lat);
                data[k][1] = preprocess("lon", point.lon);
                data[k][2] = preprocess("sog", point.sog);
                data[k][3] = preprocess("cog", point.cog);
                data[k][4] = preprocess("type", point.shipType);
                labelData[k][id] = 1;
                k++;
            }
            id++;
        }

        System.out.println("Number of points: " + n);
        System.out.println("2 sample data points:");
        for (int i = 0; i < 5; i++)
        {
            System.out.print(data[0][i] + " ");
        }
        System.out.println();
        for (int i = 0; i < 5; i++)
        {
            System.out.print(data[1][i] + " ");
        }
        System.out.println();
        System.out.println("2 sample labels:");
        for (int i = 0; i < numOutputs; i++)
        {
            System.out.print(labelData[0][i] + " ");
        }
        System.out.println();
        for (int i = 0; i < numOutputs; i++)
        {
            System.out.print(labelData[1][i] + " ");
        }
        System.out.println();

        System.out.println("id = " + id);

        INDArray features = Nd4j.create(data);
        INDArray labels = Nd4j.create(labelData);
        DataSet dataset = new DataSet(features, labels);
        return dataset;
    }

    private int numPoints()
    {
        int numPoints = 0;
        for (RouteObject route : routes)
        {
            numPoints += route.points.size();
        }

        return numPoints;
    }

    private void sogStats()
    {
        double sum = 0;
        int n = 0;
        for (RouteObject route : routes)
        {
            for (AISPoint point : route.points)
            {
                sum += point.sog;
                n++;
            }
        }
        sogMean = sum / n;

        sum = 0;
        for (RouteObject route : routes)
        {
            for (AISPoint point : route.points)
            {
                sum += Math.pow(point.sog - sogMean, 2);
            }
        }
        sogStdev = sum / n;
    }

    private double preprocess(String feature, Object value)
    {
        switch (feature)
        {
            case "lat":
                return minMaxNormalize((double) ((float) ((Float) value)), 42, 47, -1, 1);
            case "lon":
                return minMaxNormalize((double) ((float) ((Float) value)), 10, 18, -1, 1);
            case "sog":
                return zScoreNormalize((double) ((float) ((Float) value)), sogMean, sogStdev);
            case "cog":
                return minMaxNormalize((double) ((float) ((Float) value)), 0, 360, -1, 1);
            default:  // type
                return quantifyType((String) value);
        }
    }

    private double quantifyType(String value)
    {
        if (types.contains(value))
        {
            return types.indexOf(value);
        }
        else
        {
            types.add(value);
            return types.size() - 1;
        }
    }

    private double minMaxNormalize(double value, double min, double max,
            double newMin, double newMax)
    {
        return (newMax - newMin) * (value - min) / (max - min) + newMin;
    }

    private double zScoreNormalize(double value, double mean, double stdev)
    {
        return (value - mean) / stdev;
    }

    private MultiLayerConfiguration getNetConfig()
    {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(numInputs)
                        .nOut(numHidden)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new DenseLayer.Builder()
                        .nIn(numHidden)
                        .nOut(20)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU)
                        .build())
                .layer(2, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .nIn(20)
                        .nOut(numOutputs)
                        .build())
                .pretrain(false).backprop(true).build();

        return config;
    }
}
