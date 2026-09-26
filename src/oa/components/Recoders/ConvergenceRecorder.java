package oa.components.recoders;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
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

        X localBest = null;
        for (X x : currentXs) {
            if (x == null) {
                continue;
            }
            if (localBest == null || prob.compare(x, localBest) > 0) {
                localBest = x;
            }
        }

        if (localBest != null) {
            if (bestX == null || prob.compare(localBest, bestX) > 0) {
                bestX = prob.copyX(localBest);
            }
        }

        if (callCount % recordInterval == 0 && localBest != null) {
            double objectiveValue = computeObjectiveValue(localBest);
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
        JFreeChart chart = buildChart(title, merged);
        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseZoomable(false);
        panel.setMouseWheelEnabled(false);
        addChartInteraction(panel, chart);
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setSize(900, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void addChartInteraction(ChartPanel panel, JFreeChart chart) {
        final java.awt.Point[] dragStart = { null };
        final java.awt.Point[] zoomStart = { null };
        final java.awt.Rectangle[] zoomRect = { null };

        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    dragStart[0] = e.getPoint();
                } else if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    zoomStart[0] = e.getPoint();
                    zoomRect[0] = new java.awt.Rectangle();
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON3 && zoomRect[0] != null) {
                    java.awt.Rectangle r = zoomRect[0];
                    if (r.width > 5 && r.height > 5) {
                        panel.zoom(new java.awt.geom.Rectangle2D.Double(
                            r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight()));
                    }
                    zoomRect[0] = null;
                    chart.getXYPlot().clearAnnotations();
                }
                dragStart[0] = null;
            }
        });

        panel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                XYPlot plot = chart.getXYPlot();
                if (dragStart[0] != null) {
                    int dx = e.getX() - dragStart[0].x;
                    int dy = e.getY() - dragStart[0].y;
                    dragStart[0] = e.getPoint();

                    java.awt.geom.Rectangle2D dataArea = panel.getChartRenderingInfo().getPlotInfo().getDataArea();
                    double xRange = plot.getDomainAxis().getUpperBound() - plot.getDomainAxis().getLowerBound();
                    double yRange = plot.getRangeAxis().getUpperBound() - plot.getRangeAxis().getLowerBound();
                    double dxData = -dx / dataArea.getWidth() * xRange;
                    double dyData = dy / dataArea.getHeight() * yRange;

                    plot.getDomainAxis().setRange(
                        plot.getDomainAxis().getLowerBound() + dxData,
                        plot.getDomainAxis().getUpperBound() + dxData);
                    plot.getRangeAxis().setRange(
                        plot.getRangeAxis().getLowerBound() + dyData,
                        plot.getRangeAxis().getUpperBound() + dyData);
                } else if (zoomStart[0] != null) {
                    int x = Math.min(zoomStart[0].x, e.getX());
                    int y = Math.min(zoomStart[0].y, e.getY());
                    int w = Math.abs(e.getX() - zoomStart[0].x);
                    int h = Math.abs(e.getY() - zoomStart[0].y);
                    zoomRect[0] = new java.awt.Rectangle(x, y, w, h);
                    panel.repaint();
                }
            }
        });

        panel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                XYPlot plot = chart.getXYPlot();
                java.awt.geom.Rectangle2D dataArea = panel.getChartRenderingInfo().getPlotInfo().getDataArea();
                double factor = (e.getWheelRotation() < 0) ? 0.8 : 1.25;
                double x = e.getX();
                double y = e.getY();
                double cx = plot.getDomainAxis().java2DToValue(x, dataArea, plot.getDomainAxisEdge());
                double cy = plot.getRangeAxis().java2DToValue(y, dataArea, plot.getRangeAxisEdge());
                double xLo = cx - (cx - plot.getDomainAxis().getLowerBound()) * factor;
                double xHi = cx + (plot.getDomainAxis().getUpperBound() - cx) * factor;
                double yLo = cy - (cy - plot.getRangeAxis().getLowerBound()) * factor;
                double yHi = cy + (plot.getRangeAxis().getUpperBound() - cy) * factor;
                plot.getDomainAxis().setRange(xLo, xHi);
                plot.getRangeAxis().setRange(yLo, yHi);
            }
        });
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
        JFreeChart chart = buildChart(title, merged);
        try {
            File outputFile = new File(filePath);
            String ext = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
            String format = "png";
            if (ext.equals("jpg") || ext.equals("jpeg")) {
                format = "jpg";
            } else if (!ext.equals("png")) {
                outputFile = new File(filePath + ".png");
            }
            if (format.equals("jpg")) {
                ChartUtils.saveChartAsJPEG(outputFile, chart, 800, 600);
            } else {
                ChartUtils.saveChartAsPNG(outputFile, chart, 800, 600);
            }
            System.out.println("[ConvergenceRecorder] Chart saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[ConvergenceRecorder] Failed to save chart: " + e.getMessage());
        }
    }

    private JFreeChart buildChart(String title, Map<String, RunData> merged) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        List<String> names = new ArrayList<>(merged.keySet());

        for (int s = 0; s < names.size(); s++) {
            RunData data = merged.get(names.get(s));
            XYSeries series = new XYSeries(names.get(s), false, true);
            for (int i = 0; i < data.iterations.size(); i++) {
                double val = data.values.get(i);
                if (!Double.isNaN(val) && !Double.isInfinite(val)) {
                    series.add(data.iterations.get(i).doubleValue(), val, false);
                }
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
            title, "Iteration", "Objective Value", dataset,
            PlotOrientation.VERTICAL, true, true, false);

        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        ((NumberAxis) plot.getDomainAxis()).setAutoRangeIncludesZero(false);
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        for (int s = 0; s < names.size(); s++) {
            Color color = CURVE_COLORS[s % CURVE_COLORS.length];
            int n = merged.get(names.get(s)).values.size();

            renderer.setSeriesPaint(s, color);
            renderer.setSeriesStroke(s, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            renderer.setSeriesLinesVisible(s, true);
            renderer.setSeriesShapesVisible(s, true);
            renderer.setSeriesToolTipGenerator(s,
                new StandardXYToolTipGenerator("{0}<br>Iter={1}, Value={2}",
                    NumberFormat.getIntegerInstance(),
                    NumberFormat.getNumberInstance()));
        }

        plot.setRenderer(renderer);
        return chart;
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

}