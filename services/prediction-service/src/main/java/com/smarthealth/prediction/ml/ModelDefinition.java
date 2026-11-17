package com.smarthealth.prediction.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelDefinition {

    @JsonProperty("model_type")
    private String modelType;

    private String disease;
    private Double threshold;

    @JsonProperty("numerical_features")
    private List<String> numericalFeatures;

    @JsonProperty("categorical_features")
    private List<String> categoricalFeatures;

    @JsonProperty("imputer_medians")
    private List<Double> imputerMedians;

    @JsonProperty("scaler_mean")
    private List<Double> scalerMean;

    @JsonProperty("scaler_scale")
    private List<Double> scalerScale;

    private List<List<String>> categories;

    // For Logistic Regression
    private List<Double> coefficients;
    private Double intercept;

    // For Random Forest
    @JsonProperty("n_estimators")
    private Integer nEstimators;
    private List<DecisionTreeDefinition> trees;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DecisionTreeDefinition {
        @JsonProperty("node_count")
        private int nodeCount;

        @JsonProperty("children_left")
        private int[] childrenLeft;

        @JsonProperty("children_right")
        private int[] childrenRight;

        private int[] feature;
        private double[] threshold;
        private double[][] value;

        public int getNodeCount() { return nodeCount; }
        public void setNodeCount(int nodeCount) { this.nodeCount = nodeCount; }
        public int[] getChildrenLeft() { return childrenLeft; }
        public void setChildrenLeft(int[] childrenLeft) { this.childrenLeft = childrenLeft; }
        public int[] getChildrenRight() { return childrenRight; }
        public void setChildrenRight(int[] childrenRight) { this.childrenRight = childrenRight; }
        public int[] getFeature() { return feature; }
        public void setFeature(int[] feature) { this.feature = feature; }
        public double[] getThreshold() { return threshold; }
        public void setThreshold(double[] threshold) { this.threshold = threshold; }
        public double[][] getValue() { return value; }
        public void setValue(double[][] value) { this.value = value; }
    }

    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getDisease() { return disease; }
    public void setDisease(String disease) { this.disease = disease; }
    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }
    public List<String> getNumericalFeatures() { return numericalFeatures; }
    public void setNumericalFeatures(List<String> numericalFeatures) { this.numericalFeatures = numericalFeatures; }
    public List<String> getCategoricalFeatures() { return categoricalFeatures; }
    public void setCategoricalFeatures(List<String> categoricalFeatures) { this.categoricalFeatures = categoricalFeatures; }
    public List<Double> getImputerMedians() { return imputerMedians; }
    public void setImputerMedians(List<Double> imputerMedians) { this.imputerMedians = imputerMedians; }
    public List<Double> getScalerMean() { return scalerMean; }
    public void setScalerMean(List<Double> scalerMean) { this.scalerMean = scalerMean; }
    public List<Double> getScalerScale() { return scalerScale; }
    public void setScalerScale(List<Double> scalerScale) { this.scalerScale = scalerScale; }
    public List<List<String>> getCategories() { return categories; }
    public void setCategories(List<List<String>> categories) { this.categories = categories; }
    public List<Double> getCoefficients() { return coefficients; }
    public void setCoefficients(List<Double> coefficients) { this.coefficients = coefficients; }
    public Double getIntercept() { return intercept; }
    public void setIntercept(Double intercept) { this.intercept = intercept; }
    public Integer getNEstimators() { return nEstimators; }
    public void setNEstimators(Integer nEstimators) { this.nEstimators = nEstimators; }
    public List<DecisionTreeDefinition> getTrees() { return trees; }
    public void setTrees(List<DecisionTreeDefinition> trees) { this.trees = trees; }
}
