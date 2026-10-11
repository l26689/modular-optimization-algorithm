package moa.example.meta.server;

import java.util.HashMap;
import java.util.Map;

import moa.problem.continuous.ContinuousProblem;

/**
 * 适配器：把「用户的目标函数 + 维度 + 上下界」包装成框架认识的 {@link ContinuousProblem}。
 * <p>
 * 元搜索的内层算法（SA/PSO/DE）都要求解一个 {@code ContinuousProblem}。
 * 预设问题本来就是 {@code ContinuousProblem}，直接可用；但用户粘贴的代码只是一个
 * {@link ObjectiveFunction}，需要用本类把它升级成 {@code ContinuousProblem}——
 * 维度、边界来自网页输入框，评估逻辑委托给函数。
 * <p>
 * 带评估缓存：元搜索的内层算法会反复对同一批解调用 {@code evaluate}，
 * 而用户的目标函数可能很贵（或要求纯函数），缓存能把重复求值压到一次。
 * 缓存安全的前提是 {@link ObjectiveFunction#evaluate} 是纯函数——这正是
 * {@link UserCodeCompiler} 三道体检里「纯函数」那一项要保证的。
 */
public final class RuntimeProblem extends ContinuousProblem {

    /** 用户提供的目标函数。 */
    private final ObjectiveFunction fn;

    /** 解 → 成本 的缓存，key 是量化后的解字符串。 */
    private final Map<String, Double> cache = new HashMap<>();

    /**
     * @param dim   维度
     * @param lower 各维下界（长度 = dim）
     * @param upper 各维上界（长度 = dim）
     * @param fn    用户目标函数
     */
    public RuntimeProblem(int dim, double[] lower, double[] upper, ObjectiveFunction fn) {
        super(lower, upper);
        this.fn = fn;
    }

    @Override
    public Double evaluate(double[] x) {
        String key = keyOf(x);
        Double hit = cache.get(key);
        if (hit != null) {
            return hit;
        }
        double value = fn.evaluate(x);
        cache.put(key, value);
        return value;
    }

    /** 量化成字符串当缓存 key（1e-6 精度，浮点末位抖动不产生新条目）。 */
    private static String keyOf(double[] x) {
        StringBuilder sb = new StringBuilder(x.length * 12);
        for (double v : x) {
            sb.append(Math.round(v * 1e6)).append('|');
        }
        return sb.toString();
    }
}
