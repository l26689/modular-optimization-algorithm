package oa.components.recoders;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Evaluable;
import oa.api.problem.Problem;
import oa.api.spi.Reusable;
import oa.api.spi.component.Recorder;

public class ConvergenceRecorder<X,Prob extends Problem<X> & Evaluable<X, ?>> implements Recorder<X, Prob, State<X>>, Reusable {

    private Prob prob;

    private String currentRunName;
    private int recordInterval;

    private X bestX;
    private int callCount;

    private final List<Double> currentConvergenceValues;
    private final List<Integer> currentIterations;

    private final Map<String, RunData> runHistory;

    private static final String DEFAULT_RUN_NAME = "run";
    private static final int DEFAULT_INTERVAL = 1;

    private static final Color[] CURVE_COLORS = {
        new Color(31, 119, 180),
        new Color(255, 127, 14),
        new Color(44, 160, 44),
        new Color(214, 39, 40),
        new Color(148, 103, 189),
        new Color(140, 86, 75),
        new Color(227, 119, 194),
        new Color(127, 127, 127),
        new Color(188, 189, 34),
        new Color(23, 190, 207),
    };

    private static class RunData {
        final int interval;
        final List<Integer> iterations;
        final List<Double> values;

        RunData(int interval) {
            this.interval = interval;
            this.iterations = new ArrayList<>();
            this.values = new ArrayList<>();
        }
    }

    public ConvergenceRecorder() {
        this(DEFAULT_RUN_NAME, DEFAULT_INTERVAL);
    }

    public ConvergenceRecorder(String runName, int recordInterval) {
        if (recordInterval <= 0) {
            throw new IllegalArgumentException("The record interval must be a positive integer.");
        }
        this.currentRunName = (runName != null) ? runName : DEFAULT_RUN_NAME;
        this.recordInterval = recordInterval;
        this.currentConvergenceValues = new ArrayList<>();
        this.currentIterations = new ArrayList<>();
        this.runHistory = new LinkedHashMap<>();
    }

    @Override
    public void init(Prob prob, Random random) {
        this.prob = prob;
    }

    @Override
    public void record(State<X> state) {
        X[] currentXs = state.getCurrentXs();
        if (currentXs == null || currentXs.length == 0) {
            return;
        }

        for (X x : currentXs) {
            if (x == null) {
                continue;
            }
            if (bestX == null || prob.compare(x, bestX) > 0) {
                bestX = prob.copyX(x);
            }
        }

        if (callCount % recordInterval == 0) {
            double objectiveValue = computeObjectiveValue(bestX);
            currentConvergenceValues.add(objectiveValue);
            currentIterations.add(callCount);
        }

        callCount++;
    }

    private double computeObjectiveValue(X x) {
        if (prob != null) {
            Object result = prob.evaluate(x);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
        }
        return callCount;
    }

    @Override
    public void reset() {
        archiveCurrentRun();
        this.bestX = null;
        this.callCount = 0;
        this.currentConvergenceValues.clear();
        this.currentIterations.clear();
    }

    public void reset(String runName, int recordInterval) {
        if (recordInterval <= 0) {
            throw new IllegalArgumentException("The record interval must be a positive integer.");
        }
        archiveCurrentRun();
        this.currentRunName = (runName != null) ? runName : DEFAULT_RUN_NAME;
        this.recordInterval = recordInterval;
        this.bestX = null;
        this.callCount = 0;
        this.currentConvergenceValues.clear();
        this.currentIterations.clear();
    }

    private void archiveCurrentRun() {
        if (bestX == null && currentConvergenceValues.isEmpty()) {
            return;
        }
        String uniqueName = makeUniqueName(currentRunName);
        RunData data = new RunData(recordInterval);
        data.iterations.addAll(currentIterations);
        data.values.addAll(currentConvergenceValues);
        runHistory.put(uniqueName, data);
    }

    private String makeUniqueName(String baseName) {
        if (!runHistory.containsKey(baseName)) {
            return baseName;
        }
        int suffix = 2;
        String candidate;
        do {
            candidate = baseName + "-" + suffix;
            suffix++;
        } while (runHistory.containsKey(candidate));
        return candidate;
    }

    public List<Double> getConvergenceData(String runName) {
        RunData data = runHistory.get(runName);
        if (data != null) {
            return new ArrayList<>(data.values);
        }
        return new ArrayList<>();
    }

    public List<Integer> getIterationData(String runName) {
        RunData data = runHistory.get(runName);
        if (data != null) {
            return new ArrayList<>(data.iterations);
        }
        return new ArrayList<>();
    }

    public X getBestX() {
        if (bestX == null) {
            return null;
        }
        return prob.copyX(bestX);
    }

    public double getBestValue() {
        if (bestX == null) {
            return Double.NaN;
        }
        return computeObjectiveValue(bestX);
    }

    public int getCallCount() {
        return callCount;
    }

    public String getCurrentRunName() {
        return currentRunName;
    }

    public int getRecordInterval() {
        return recordInterval;
    }

    public List<String> getRunNames() {
        List<String> names = new ArrayList<>(runHistory.keySet());
        if (bestX != null && !currentConvergenceValues.isEmpty()) {
            String uniqueName = makeUniqueName(currentRunName);
            names.add(uniqueName);
        }
        return names;
    }

    public void printConvergenceData() {
        Map<String, RunData> merged = buildMergedRunMap();
        if (merged.isEmpty()) {
            System.out.println("[ConvergenceRecorder] No data.");
            return;
        }
        for (Map.Entry<String, RunData> entry : merged.entrySet()) {
            RunData data = entry.getValue();
            if (data.values.isEmpty()) continue;
            double first = data.values.get(0);
            double last = data.values.get(data.values.size() - 1);
            double best = Double.NaN;
            for (double v : data.values) {
                if (Double.isNaN(best) || v < best) best = v;
            }
            System.out.printf("%s: points=%d  first=%.4e  last=%.4e  best=%.4e%n",
                entry.getKey(), data.values.size(), first, last, best);
        }
    }

    public void visualize() {
        visualize("Convergence Curves");
    }

    public void visualize(String title) {
        Map<String, RunData> merged = buildMergedRunMap();
        if (merged.isEmpty()) {
            System.out.println("[ConvergenceRecorder] No data to visualize.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFreeChart chart = createChart(title, merged);
            showChartFrame(title, chart);
        });
    }

    private void showChartFrame(String title, JFreeChart chart) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
        frame.add(chartPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void saveChart(String filePath) {
        saveChart(filePath, "Convergence Curves");
    }

    public void saveChart(String filePath, String title) {
        Map<String, RunData> merged = buildMergedRunMap();
        if (merged.isEmpty()) {
            System.out.println("[ConvergenceRecorder] No data to save.");
            return;
        }
        JFreeChart chart = createChart(title, merged);
        try {
            File outputFile = new File(filePath);
            String ext = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
            int width = 800;
            int height = 600;
            if (ext.equals("jpg") || ext.equals("jpeg")) {
                ChartUtils.saveChartAsJPEG(outputFile, chart, width, height);
            } else {
                if (!ext.equals("png")) {
                    outputFile = new File(filePath + ".png");
                }
                ChartUtils.saveChartAsPNG(outputFile, chart, width, height);
            }
            System.out.println("[ConvergenceRecorder] Chart saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[ConvergenceRecorder] Failed to save chart: " + e.getMessage());
        }
    }

    private Map<String, RunData> buildMergedRunMap() {
        Map<String, RunData> merged = new LinkedHashMap<>(runHistory);
        if (bestX != null && !currentConvergenceValues.isEmpty()) {
            RunData current = new RunData(recordInterval);
            current.iterations.addAll(currentIterations);
            current.values.addAll(currentConvergenceValues);
            String uniqueName = makeUniqueNameForMerged(merged, currentRunName);
            merged.put(uniqueName, current);
        }
        return merged;
    }

    private String makeUniqueNameForMerged(Map<String, RunData> merged, String baseName) {
        if (!merged.containsKey(baseName)) {
            return baseName;
        }
        int suffix = 2;
        String candidate;
        do {
            candidate = baseName + "-" + suffix;
            suffix++;
        } while (merged.containsKey(candidate));
        return candidate;
    }

    private JFreeChart createChart(String title, Map<String, RunData> merged) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        List<String> names = new ArrayList<>(merged.keySet());

        for (String name : names) {
            RunData data = merged.get(name);
            String seriesName = name + " (" + data.values.size() + " pts, interval=" + data.interval + ")";
            XYSeries series = new XYSeries(seriesName, false);
            for (int i = 0; i < data.iterations.size(); i++) {
                double val = data.values.get(i);
                if (!Double.isNaN(val) && !Double.isInfinite(val)) {
                    series.add(data.iterations.get(i).doubleValue(), val);
                }
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            "Iteration",
            "Objective Value",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));
        plot.setOutlineVisible(false);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultLinesVisible(true);
        renderer.setDefaultShapesVisible(true);

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            Color color = CURVE_COLORS[i % CURVE_COLORS.length];
            renderer.setSeriesPaint(i, color);
            renderer.setSeriesStroke(i, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            renderer.setSeriesShapesVisible(i, dataset.getSeries(i).getItemCount() <= 200);
        }

        plot.setRenderer(renderer);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setAutoRangeIncludesZero(false);
        domainAxis.setLowerMargin(0.02);
        domainAxis.setUpperMargin(0.02);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setLowerMargin(0.05);
        rangeAxis.setUpperMargin(0.05);

        return chart;
    }
}
