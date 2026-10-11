package moa.algorithm.sa.components;

import moa.problem.continuous.ContinuousProblem;
import moa.algorithm.sa.api.SAInitializer;
import moa.algorithm.sa.api.SAState;
import moa.problem.continuous.components.RandomInitializer;

/**
 * SA 基础初始化器 —— 随机生成初始解并指定初始温度。
 *
 * <p>继承 RandomInitializer<SAState<double[]>>，实现 SAInitializer<X:double[], Prob:ContinuousProblem>。是否为最终类：是。
 *
 * <p>在问题边界内随机采样生成初始解，初始温度由构造参数指定。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>绑定 —— X 绑定为 double[]，Prob 绑定为 ContinuousProblem。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class SABasicInitializer  extends RandomInitializer<SAState<double[]>> implements SAInitializer<double[],ContinuousProblem> {
    private double initialTemp;
    
    public SABasicInitializer(double initialTemp) {
        this.initialTemp = initialTemp;
    }

    @Override
    public double initialTemperature() {
        return initialTemp;
    }
}