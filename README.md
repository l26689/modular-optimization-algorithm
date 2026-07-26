# Modular Optimization Algorithm (MOA)

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)

一个严格遵循 **最少信息原则** 的模块化优化算法框架。  
将算法拆解为清晰的可替换组件，像乐高一样自由组合。

## ✨ 设计思想

### 为什么还要造轮子？

现有实现要么是工业级黑盒（如 Optuna），要么是高度耦合的教学代码。  
**MOA** 面向学习、实验和定制化：每一块组件都拥有清晰的接口契约，可以独立阅读、测试和替换。

### 核心原则：最少信息

组件的接口**只传递它绝对无法自行推导的信息**。

| 不传递 | 原因 |
|--------|------|
| 目标函数值 `Y` | 组件可通过 `Problem.evaluate()` 自行获取 |
| 是否改进 | 组件可通过 `Problem.compare()` 自行比较 |
| 迭代次数 | 组件内部维护计数器，通过方法调用次数推导 |

**只传递原子事实**：
1. `temperature` – 只有主循环知道
2. `currentX` – 当前解（组件无法感知外部状态）
3. `isAccepted` – 上一次概率接受的结果（只有主循环拥有随机数）

### 架构概览

```
MOA/
├── oa.api/                    # 通用优化算法抽象
│   ├── OptimizationAlgorithm  # 算法基类
│   ├── Problem                # 问题定义接口
│   ├── Recorder               # 记录器接口
│   ├── State                  # 状态基类
│   └── Reusable               # 可复用契约
├── sa.core/                   # 模拟退火核心
│   ├── SimulatedAnnealing     # 主循环控制器
│   ├── SAState                # SA 迭代状态
│   ├── SAInitializer          # 初始化组件
│   ├── SAPerturbation         # 扰动组件
│   ├── SACoolingSchedule      # 冷却策略
│   └── SATerminationCondition # 终止条件
├── sa.components/             # SA 内置实现
├── oa.components/             # 通用组件（Recorder 等）
└── oa.examples/               # 示例问题
```

## 🚀 快速开始

详见 [QUICKSTART.md](QUICKSTART.md)，5 分钟内运行你的第一个优化示例。

## 🧩 核心概念

### Problem — 问题定义

实现 `Problem<X, Y>` 接口，定义解的表示类型 `X` 和目标值类型 `Y`：

```java
public interface Problem<X, Y> {
    Y evaluate(X x);           // 评估解的质量（必须是纯函数）
    X copyX(X x);              // 深拷贝解
    double compare(Y y1, Y y2); // 比较两个目标值
}
```

`compare()` 方法定义**偏序关系**：
- **正值**（`> 0`）—— `y1` 优于 `y2`
- **负值**（`< 0`）—— `y1` 劣于 `y2`
- **零**（`= 0`）—— 两者等优 **或** 无法比较（无支配关系）

绝对值表示优劣差距的大小，使 Metropolis 准则能动态调整接受概率。

### Recorder — 结果记录与评估

`solve()` 方法返回 `void`，优化结果通过 `Recorder` 对外提供：

```java
public interface Recorder<X, Y, Prob extends Problem<X,Y>, S extends State<X>> {
    void record(S state);  // 在适当时机被算法调用
}
```

内置 Recorder：
- **BestRecorder** — 记录历史最优解
- **LastRecorder** — 记录最后一次接受的解

### State — 算法状态

`State<X>` 是状态基类，持有当前解。SA 扩展为 `SAState<X>`，额外包含：
- `temperature()` — 当前系统温度
- `isAccepted()` — 上一轮是否接受新解

### Reusable — 可复用契约

实现 `Reusable` 接口的组件支持 `reset()` 操作，可在多次独立优化运行间复用，避免反复创建实例。

## 🧩 自定义组件

所有组件只需继承对应的抽象类并实现核心方法。以下是一个线性冷却策略示例：

```java
public class LinearCoolingSchedule extends SACoolingSchedule<double[], Double, ContinuousProblem> {
    private double coolingRate;
    private int currentIteration;
    private int maxIterations;

    public LinearCoolingSchedule(double coolingRate, int maxIterations) {
        this.coolingRate = coolingRate;
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
    }

    @Override
    protected void init(ContinuousProblem problem, Random random) {
        // 绑定问题实例（此实现无需额外操作）
    }

    @Override
    protected double cool(SAState<double[]> state) {
        double temperature = state.temperature();
        currentIteration++;
        if (currentIteration > maxIterations) {
            currentIteration = 0;
            return temperature * coolingRate;
        }
        return temperature;
    }
}
```

## 🎯 自定义问题

要让 MOA 优化你的问题，只需创建一个类实现 `Problem<X, Y>` 接口。为方便起见，连续优化问题可直接继承 `ContinuousProblem`：

```java
public class MyProblem extends ContinuousProblem {

    public MyProblem(int dimension) {
        super(createBounds(dimension, -100), createBounds(dimension, 100));
    }

    @Override
    public Double evaluate(double[] x) {
        double sum = 0.0;
        for (double v : x) {
            sum += v * v;
        }
        return sum;
    }

    @Override
    public double[] copyX(double[] x) {
        return x.clone();
    }
}
```

> **注**：`createBounds()` 已在 `ContinuousProblem` 中定义，子类可直接使用。非连续问题可直接实现 `Problem<X, Y>` 接口。

## ⚡ 性能建议：为评估添加缓存

如果评估代价较高，建议在 Problem 实现类内部引入缓存：

```java
public class CachedMyProblem extends ContinuousProblem {
    private double[] lastX = null;
    private Double lastValue = null;

    public CachedMyProblem(int dimension) {
        super(createBounds(dimension, -100), createBounds(dimension, 100));
    }

    @Override
    public Double evaluate(double[] x) {
        if (lastX != null && java.util.Arrays.equals(x, lastX)) {
            return lastValue;
        }
        double value = 0.0;
        for (double v : x) value += v * v;
        lastX = x.clone();
        lastValue = value;
        return value;
    }

    @Override
    public double[] copyX(double[] x) {
        return x.clone();
    }
}
```

## ⚠️ 设计约束

**冷启动**：详见 [SAState 迭代状态封装](src/sa/README.md#sastate----迭代状态封装)。

**线程安全**：框架默认单线程运行，所有组件内部可变状态需自行同步。

**不可变性**：传入组件的 `currentX` 解不应被原地修改；扰动方法必须返回新对象。

**纯函数**：`Problem.evaluate()` 必须是纯函数（相同输入 -> 相同输出），且每次返回的 `Y` 都必须是独立的新对象。

**随机数复用**：所有组件共享主算法注入的同一 `Random` 实例，不应自行创建独立的随机数生成器。

## 🔮 未来演进

- 离散问题适配示例（TSP、背包）
- 粒子群优化（PSO）模块完善
- 更多内置组件实现
- 多目标优化支持

## 📄 许可

本项目采用 [MIT License](LICENSE) 许可证。

欢迎 Issue 和 PR。如果你也喜欢"让代码自己说话"的风格，这个项目就是为你准备的。