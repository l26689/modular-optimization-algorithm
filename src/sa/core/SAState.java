package sa.core;

import java.util.Iterator;

import oa.api.State;

/**
 * 模拟退算法的迭代状态封装。
 * <p>
 * 本类继承自 {@link State}，在基础解信息之上扩展了模拟退火特有的状态字段：
 * 温度和接受标志。主算法在每次迭代中创建此对象，并将其传递给各组件
 * （扰动器、冷却策略、终止条件），使组件能获取当前迭代的完整上下文。
 *
 * <h3>状态字段说明</h3>
 * <ul>
 *   <li>{@link currentXIterator} - 当前解的迭代器，用于直接访问解元素</li>
 *   <li>{@link temperature} - 当前系统温度，用于控制扰动幅度和接受概率</li>
 *   <li>{@link isAccepted} - 上一轮迭代是否接受了新解</li>
 * </ul>
 *
 * <h3>冷启动约定</h3>
 * 首次迭代前，主算法会将 {@code isAccepted} 初始化为 {@code false}，
 * 表示"尚无历史"。各组件应能正确处理此初始状态。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]}）
 */
public class SAState<X> implements State<X> {
    private class CurrentXIterator implements Iterator<X>{
        boolean hasNext = true;
        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public X next() {
            hasNext = false;
            return currentX;
        }
    }

    private X currentX;

    /** 当前系统温度，用于控制扰动幅度和接受概率 */
    private double temperature;
    /** 上一轮迭代是否接受了新解；首次迭代时为 {@code false} */
    private boolean isAccepted;
    private CurrentXIterator currentXIterator;

    /**
     * 构造一个模拟退火迭代状态对象。
     *
     * @param currentX    当前解，不为 {@code null}
     * @param temperature 当前系统温度
     * @param isAccepted  上一轮迭代是否接受了新解；首次迭代时应为 {@code false}
     */
    public SAState(X currentX, double temperature, boolean isAccepted) {
        this.currentX = currentX;
        currentXIterator = new CurrentXIterator();
        this.temperature = temperature;
        this.isAccepted = isAccepted;
    }


    public void set(X currentX, double temperature, boolean isAccepted) {
        this.currentX = currentX;
        currentXIterator.hasNext = true;
        this.temperature = temperature;
        this.isAccepted = isAccepted;
    }

    @Override
    public Iterator<X> getCurrentXIterator() {
        return currentXIterator;
    }

    @Override
    public X[] getCurrentXs() {
        throw new UnsupportedOperationException("Unsupported method 'getCurrentXs'");
    }

    @Override
    public boolean isArraySupported() {
        return false;
    }

    public double getTemperature() {
        return temperature;
    }
    public boolean getIsAccepted() {
        return isAccepted;
    }

}
