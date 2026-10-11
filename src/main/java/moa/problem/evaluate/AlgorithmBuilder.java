package moa.problem.evaluate;

import java.util.Random;

import moa.framework.engine.OptimizationAlgorithm;
import moa.framework.problem.Problem;

/**
 * 算法构建器契约 —— 以工厂模式创建算法实例。
 *
 * <p>是否为最终类：否（接口）。
 *
 * <p>接收问题和随机源，返回装配完成的 OptimizationAlgorithm 实例。用于算法评估场景中的批量构建。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>可复现 —— 必须使用传入的 Random 实例，不得自行创建。</li>
 * </ul>
 *
 * <p><b>参见文档 6</b>
 */
public interface AlgorithmBuilder<X, Prob extends Problem<X>> {
    OptimizationAlgorithm<X, Prob, ?> build(Prob problem, Random random);
}