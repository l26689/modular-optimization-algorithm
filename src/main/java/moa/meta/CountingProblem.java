package moa.meta;

import moa.problem.continuous.ContinuousProblem;

/**
 * <b>计数装饰器</b>：包住一个 {@link ContinuousProblem}，统计目标函数被调用的次数。
 * <p>
 * 用于"按评估次数对齐"三种内层算法。内层算法（SA / PSO / DE）解目标问题时，
 * 每调一次 {@link #evaluate} 就计数一次——无论是直接调用，还是通过
 * {@code compare}（其内部就是两次 {@code evaluate}）间接调用，都会被数到。
 * <p>
 * 因为 {@code compare} 的默认实现是 {@code evaluate(x2) - evaluate(x1)}，
 * 而本类覆盖了 {@link #evaluate}，所以 {@code compare} 的两次求值会自动被计数，
 * 无需单独处理。
 * <p>
 * 装饰器<b>不做缓存</b>：我们要对齐的是"算法调用目标函数的次数"（算法的工作量），
 * 不是"底层函数真正计算了几次"。缓存会扭曲这个指标。
 */
public final class CountingProblem extends ContinuousProblem {

    /** 被包装的真实问题。 */
    private final ContinuousProblem delegate;

    /** 累计评估次数。 */
    private int evalCount = 0;

    public CountingProblem(ContinuousProblem delegate) {
        super(delegate.getLowerBounds(), delegate.getUpperBounds());
        this.delegate = delegate;
    }

    @Override
    public Double evaluate(double[] x) {
        evalCount++;
        return delegate.evaluate(x);
    }

    @Override
    public double[] copyX(double[] x) {
        return delegate.copyX(x);
    }

    /** 到目前为止被评估的次数。 */
    public int getEvalCount() {
        return evalCount;
    }
}
