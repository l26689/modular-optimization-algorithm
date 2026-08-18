package oa.components.Recoders;

import java.util.Random;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;
import oa.api.spi.Recorder;

/**
 * 历史最优解记录器，在每次 {@link #record(State)} 调用中自动维护全局最优解。
 * <p>
 * 本类遍历状态中的所有解（通过 {@code state.getCurrentXs()}），使用
 * {@link Problem#compare(Object, Object)} 比较优劣，保留历史最优解。
 * 同时适用于单解算法（SA）和群体算法（PSO）：对于 SA，数组仅含一个元素，
 * 每次迭代与历史最优比较；对于 PSO，遍历所有粒子位置，保留全局最优。
 *
 * <h3>生命周期</h3>
 * <ol>
 *   <li>构造：无参构造，不绑定任何 Problem</li>
 *   <li>{@link #init(Problem, Random)}：由算法在 {@code solve()} 入口调用，
 *       注入 Problem 引用（用于 {@code compare()} 和 {@code copyX()}）</li>
 *   <li>{@link #record(State)}：由算法在每次迭代后调用，自动更新 {@code bestX}</li>
 *   <li>{@link #getBestX()}：在 {@code solve()} 返回后由调用方获取最优解</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * BestRecorder<double[]> recorder = new BestRecorder<>();
 * sa.solve(recorder);  // 或 pso.solve(recorder)
 * double[] bestX = recorder.getBestX();
 * }</pre>
 *
 * @param <X> 解的表示类型
 */
public class BestRecorder<X> implements Recorder<X,Problem<X>,State<X>> {
    private X bestX;
    private Problem<X> prob;

    /**
     * 记录一个状态，自动更新历史最优解。
     * <p>
     * 遍历状态中所有解，若当前解优于历史最优（或首次记录），
     * 则通过 {@link Problem#copyX(Object)} 保存其副本。
     *
     * @param state 当前算法状态，通过 {@code state.getCurrentXs()} 获取所有解
     */
    @Override
    public void record(State<X> state) {
        X[] currentXs = state.getCurrentXs();
        for(X x : currentXs) {
            if (bestX == null || prob.compare(x, bestX) >= 0) {
                bestX = prob.copyX(x);
            }
        }
    }

    /**
     * 获取历史最优解。
     * <p>
     * 注意：返回的是内部引用，调用方不应修改返回的数组内容。
     * 如需修改，请自行调用 {@link Problem#copyX(Object)} 创建副本。
     *
     * @return 历史最优解（若从未记录则为 {@code null}）
     */
    public X getBestX() { return bestX;}

    /**
     * 绑定问题实例。
     * <p>
     * 由算法在 {@code solve()} 入口调用，不由此类的使用者直接调用。
     *
     * @param prob   待优化问题，用于 {@code compare()} 和 {@code copyX()}
     * @param random 随机数生成器（此实现不使用）
     */
    @Override
    public void init(Problem<X> prob, Random random) {
        this.prob = prob;
    }
}