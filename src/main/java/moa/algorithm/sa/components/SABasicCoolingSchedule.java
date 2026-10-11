package moa.algorithm.sa.components;

import java.util.Random;

import moa.algorithm.sa.api.SACoolingSchedule;
import moa.algorithm.sa.api.SAState;
import moa.framework.problem.Problem;

/**
 * SA 基础几何冷却策略 —— 温度按固定比率衰减。
 *
 * <p>实现 SACoolingSchedule<X, Prob:Problem<X>, S:SAState<X>>。是否为最终类：是。
 *
 * <p>每指定步数将温度乘以冷却因子。不依赖问题特有方法，可跨问题类型复用。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>装配 —— 构造参数 coolingRate 建议范围 0.90~0.99。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class SABasicCoolingSchedule<X> implements SACoolingSchedule<X,Problem<X>,SAState<X>> {
    private double coolingRate;
    private int currentIteration;
    private int maxIterations;
    
    public SABasicCoolingSchedule(double coolingRate, int maxIterations) {
        this.coolingRate = coolingRate;
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
    }
    
    @Override
    public void init(Problem<X> problem,Random random) {
        // 初始化操作
    }
    
    @Override
    public double cool(SAState<X> state) {
        double temperature = state.getTemperature();
        currentIteration++;
        if(currentIteration > maxIterations) {
            currentIteration = 0;
            return temperature * coolingRate;
        }
        return temperature;
    }
}