package com.smarthealth.prediction.ml;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModelEngine {

    private static final Logger log = LoggerFactory.getLogger(ModelEngine.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ModelDefinition> models = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Loading ML models from classpath:/models/*_model.json...");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath*:models/*_model.json");
            for (Resource r : resources) {
                try (InputStream is = r.getInputStream()) {
                    ModelDefinition def = objectMapper.readValue(is, ModelDefinition.class);
                    models.put(def.getDisease(), def);
                    log.info("Successfully loaded ML model: {} (Type: {}, Threshold: {})",
                            def.getDisease(), def.getModelType(), def.getThreshold());
                }
            }
            log.info("All {} ML models loaded and verified ready for pure-Java inference!", models.size());
        } catch (Exception e) {
            log.error("Failed to load ML models from classpath: {}", e.getMessage(), e);
        }
    }

    public Set<String> getAvailableModels() {
        return models.keySet();
    }

    public Double getThreshold(String disease) {
        ModelDefinition def = models.get(disease);
        return def != null ? def.getThreshold() : 0.5;
    }

    public double predictProbability(String disease, Map<String, Object> featureMap) {
        ModelDefinition model = models.get(disease);
        if (model == null) {
            throw new IllegalArgumentException("Unknown disease model: " + disease);
        }

        // 1. Transform features into the 33-dimensional normalized vector
        double[] x = preprocess(model, featureMap);

        // 2. Perform inference
        if ("logistic_regression".equalsIgnoreCase(model.getModelType())) {
            return predictLogisticRegression(model, x);
        } else if ("random_forest".equalsIgnoreCase(model.getModelType())) {
            return predictRandomForest(model, x);
        } else {
            throw(new UnsupportedOperationException("Unsupported model type: " + model.getModelType()));
        }
    }

    private double[] preprocess(ModelDefinition model, Map<String, Object> featureMap) {
        List<String> numFeatures = model.getNumericalFeatures();
        List<Double> medians = model.getImputerMedians();
        List<Double> means = model.getScalerMean();
        List<Double> scales = model.getScalerScale();

        List<String> catFeatures = model.getCategoricalFeatures();
        List<List<String>> categories = model.getCategories();

        // Calculate total feature dimensions
        int totalCatDims = 0;
        for (List<String> cats : categories) {
            totalCatDims += cats.size();
        }
        double[] vector = new double[numFeatures.size() + totalCatDims];
        int idx = 0;

        // Numerical: impute median + standard scale
        for (int i = 0; i < numFeatures.size(); i++) {
            String col = numFeatures.get(i);
            Object rawVal = featureMap.get(col);
            double val = (rawVal instanceof Number) ? ((Number) rawVal).doubleValue() : medians.get(i);
            double mean = means.get(i);
            double scale = scales.get(i);
            vector[idx++] = (val - mean) / scale;
        }

        // Categorical: one-hot encode
        for (int i = 0; i < catFeatures.size(); i++) {
            String col = catFeatures.get(i);
            String val = String.valueOf(featureMap.getOrDefault(col, ""));
            List<String> cats = categories.get(i);
            for (String c : cats) {
                vector[idx++] = c.equalsIgnoreCase(val) ? 1.0 : 0.0;
            }
        }

        return vector;
    }

    private double predictLogisticRegression(ModelDefinition model, double[] x) {
        List<Double> coefs = model.getCoefficients();
        double z = model.getIntercept();
        for (int i = 0; i < x.length && i < coefs.size(); i++) {
            z += coefs.get(i) * x[i];
        }
        // Sigmoid function
        double proba = 1.0 / (1.0 + Math.exp(-z));
        return Math.round(proba * 10000.0) / 10000.0;
    }

    private double predictRandomForest(ModelDefinition model, double[] x) {
        List<ModelDefinition.DecisionTreeDefinition> trees = model.getTrees();
        if (trees == null || trees.isEmpty()) {
            return 0.0;
        }

        double sumProb = 0.0;
        for (ModelDefinition.DecisionTreeDefinition tree : trees) {
            int node = 0;
            int[] left = tree.getChildrenLeft();
            int[] right = tree.getChildrenRight();
            int[] feature = tree.getFeature();
            double[] threshold = tree.getThreshold();
            double[][] value = tree.getValue();

            while (left[node] != -1) {
                int featIdx = feature[node];
                double thresh = threshold[node];
                if (x[featIdx] <= thresh) {
                    node = left[node];
                } else {
                    node = right[node];
                }
            }
            double[] counts = value[node];
            double total = counts[0] + counts[1];
            double p1 = (total > 0) ? (counts[1] / total) : 0.0;
            sumProb += p1;
        }

        double avgProb = sumProb / trees.size();
        return Math.round(avgProb * 10000.0) / 10000.0;
    }
}
