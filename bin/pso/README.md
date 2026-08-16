# 模块化粒子群优化（MPSO）

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)

一个严格遵循 **最少信息原则** 的模块化粒子群优化算法框架。
将算法拆解为 **粒子** 和 **终止条件** 两大可替换组件，支持异构粒子群体。

## ✨ 设计思想

### 为什么 MPSO 与众不同？

传统 PSO 实现中，粒子是被动的数据容器——速度、位置、pBest 都是外部赋值的字段。
**MPSO** 打破这一模式：每个粒子是**拥有自己行为的独立搜索单元**。
粒子 0 可以用标准 PSO 更新，粒子 1 可以用收缩因子 PSO，粒子 2 甚至可以用 DE 变异——完全异构。

### 核心原则：最少信息

粒子的接口**只传递它绝对无法自行推导的信息**。

| 不传递 | 原因 |
|--------|------|
| 全局最优 gBest | 粒子可从 `getCurrentXs()` 中评估所有位置自行得出 |
| 个体最优 pBest | 粒子内部自行维护 |
| 目标函数值 | 组件可通过持有的 `Problem` 引用自行评估 |
| 迭代次数 | 组件内部维护计数器，通过方法调用次数推导 |

**只传递一件原子事实**：
1. `currentPositions` — 所有粒子当前位置（只有主循环知道）

### 架构概览

PSO 组件继承自 `oa.api.spi` 中的通用接口，通过 `Particle` 接口合二为一：

```
                       oa.api.spi
             ┌────────────┼──────────────┐
       Initializer    TerminationCondition  SearchOperator
             │                │                  │
             │                │                  │
    pso.core.Particle ────────┘                  │
    (extends Initializer + SearchOperator)       │
             │                                   │
    ┌────────┴─────────┐                         │
    │                  │                         │
CustomParticle   StandardPSOParticle      PSOTerminationCondition
(组合实现)        (直接实现)               (extends TerminationCondition)

ParticleSwarmOptimization<X>
|-- Particle[]         -> init(problem, random) + initialX() + search(state)
+-- TerminationCondition -> check(state)
```

主循环流程（同步 PSO）：
```
初始化 → 收集初始位置 → 迭代[所有粒子搜索 → 交换位置数组 → 记录 → 检查终止] → 输出结果
```

## 📦 PSOState — 迭代状态封装

`PSOState<X>` 是 PSO 算法在单次迭代中的状态快照。它实现 `State<X>` 接口，通过数组模式统一访问所有粒子位置：

| 模式 | 方法 | 说明 |
|------|------|------|
| **数组**（统一） | `getCurrentXs()` | 遍历：`for (X x : state.getCurrentXs())`，数组长度等于粒子数 |

`PSOState` 仅维护一个字段：

| 字段 | 访问方法 | 说明 |
|------|----------|------|
| `currentPositions` | `state.getCurrentXs()` | 所有粒子的当前位置数组，数组长度 = 粒子数 |

> **最少信息**：与 SA 的 `SAState`（额外包含温度、接受标志）不同，PSO 状态不需要温度或接受/拒绝信息，因为粒子速度更新是确定性的，搜索结果总是被接受。gBest 和 pBest 由各粒子自行维护。

**可变性**：`set()` 直接替换内部数组引用，不进行防御性拷贝。组件不应保存数组引用跨迭代使用。

## 📁 模块结构

```
src/pso/
|-- core/                              # 核心框架
|   |-- Particle                       # 粒子接口（extends Initializer + SearchOperator）
|   |-- PSOState                       # PSO 迭代状态封装
|   +-- PSOTerminationCondition        # PSO 终止条件接口（extends TerminationCondition）
|-- BasicPSO/
|   |-- ParticleSwarmOptimization      # 主循环控制器（同步 PSO）
|   +-- BasicPSOState                  # PSOState 的 final 子类（JIT 优化）
|-- components/
|   |-- CustomParticle                 # 组合式粒子（委托 Initializer + SearchOperator）
|   +-- continuousproblem/
|       +-- StandardPSOParticle        # 针对 ContinuousProblem 的标准 PSO 粒子
|-- AGENTS.md                          # AI Agent 使用指南
+-- README.md                          # 本文档
```

| 模块 | 说明 |
|------|------|
| `core/` | 框架核心：粒子接口 + 状态封装 + 终止条件接口 |
| `BasicPSO/` | 通用 PSO 主循环：同步更新模型 + final 状态类 |
| `components/` | 开箱即用的组件实现，可直接使用或作为自定义参考 |

## 🚀 快速开始

```java
import oa.examples.continuousproblem.myproblem.MyProblem;
import oa.components.Recoders.BestRecorder;
import oa.components.terminationcondition.MaxCallTerminationCondition;
import pso.basicpso.ParticleSwarmOptimization;
import pso.core.Particle;
import pso.components.continuousproblem.StandardPSOParticle;

// 1. 定义问题
MyProblem problem = new MyProblem(2);

// 2. 组装粒子群体
Particle<double[], ?, ?>[] particles = new Particle[] {
    new StandardPSOParticle(0.729, 1.49445, 1.49445),
    new StandardPSOParticle(0.729, 1.49445, 1.49445),
    new StandardPSOParticle(0.729, 1.49445, 1.49445)
};

// 3. 创建算法实例
ParticleSwarmOptimization<double[]> pso =
    new ParticleSwarmOptimization<>(
        problem,
        particles,
        new MaxCallTerminationCondition(10000)
    );

// 4. 创建记录器并启动算法
BestRecorder<double[]> recorder = new BestRecorder<>(problem);
pso.solve(recorder);

// 5. 获取结果
System.out.println("最优值: " + problem.evaluate(recorder.getBestX()));
```

## 🧩 组件详解

### 1. Particle — 粒子接口

粒子是 MPSO 的核心抽象，同时继承 `Initializer` 和 `SearchOperator`，使每个粒子成为一个自包含的搜索单元。

```java
public interface Particle<X, Prob extends Problem<X>, S extends State<X>>
        extends Initializer<X, Prob, S>, SearchOperator<X, Prob, S> {
}
```

**核心方法**：
- `init(problem, random)` — 绑定问题实例和随机源，获取维度、边界等元数据
- `initialX()` — 生成初始位置（必须是独立新对象）
- `search(S state)` — 执行一步搜索，从当前状态出发返回新位置

**实现方式**：
- **直接实现**：如 `StandardPSOParticle`，在单个类中完整实现所有逻辑
- **组合实现**：通过 `CustomParticle` 将任意 `Initializer` 和 `SearchOperator` 组合为一个粒子

### 2. PSOTerminationCondition — 终止条件

决定算法何时停止迭代。继承自 `oa.api.spi.TerminationCondition<X, Prob, PSOState<X>>`。

```java
public interface PSOTerminationCondition<X, Prob extends Problem<X>>
        extends TerminationCondition<X, Prob, PSOState<X>> {
    boolean check(PSOState<X> state);
}
```

**核心方法**：
- `check(PSOState<X> state)` — 判断是否终止
  - 返回 `true` 表示满足终止条件，算法停止
  - 常见实现：最大调用次数、全局最优收敛、粒子多样性低于阈值

内置实现：`oa.components.terminationcondition.MaxCallTerminationCondition` — 实现了 `TerminationCondition`，基于调用次数上限终止，可同时用于 SA 和 PSO。

### 3. StandardPSOParticle — 标准 PSO 粒子

针对 `ContinuousProblem` 的经典 PSO 实现，直接实现 `Particle` 接口。

**速度-位置更新公式**：
```
v = ω·v + c₁·r₁·(pBest - x) + c₂·r₂·(gBest - x)
x = x + v
```

**参数说明**：

| 参数 | 含义 | 常用值 |
|------|------|--------|
| `ω` | 惯性权重 | 0.4~0.9 |
| `c₁` | 认知系数（个体经验） | 1.49445 |
| `c₂` | 社会系数（群体经验） | 1.49445 |

常用组合：ω=0.729, c₁=c₂=1.49445（Clerc & Kennedy 2002 收缩因子）。

**最少信息**：gBest 由粒子自行从 `state.getCurrentXs()` 中评估所有粒子位置得出，不依赖外部传入。

**性能**：位置直接原地更新，不分配临时数组。每次 `search()` 仅分配一次数组（返回值的防御性拷贝）。

### 4. CustomParticle — 组合式粒子

通过包装任意 `Initializer` 和 `SearchOperator` 实现 `Particle` 接口，实现异构粒子群。

```java
// 粒子 0：标准 PSO 搜索
Particle<double[], ContinuousProblem, BasicPSOState<double[]>> p0 =
    new CustomParticle<>(
        new UniformInitializer(),
        new StandardPSOSearch(0.729, 1.49445, 1.49445)
    );

// 粒子 1：自适应 PSO 搜索
Particle<double[], ContinuousProblem, BasicPSOState<double[]>> p1 =
    new CustomParticle<>(
        new UniformInitializer(),
        new AdaptivePSOSearch()
    );
```

## 🧩 自定义粒子

实现 `Particle` 接口或使用 `CustomParticle` 组合现有组件。以下是一个惯性权重线性衰减的粒子示例：

```java
public class LDWParticle implements Particle<double[], ContinuousProblem, PSOState<double[]>> {
    private double[] position, velocity, pBest;
    private double omegaStart, omegaEnd, c1, c2;
    private int dim, maxIterations, iteration;
    private double[] lowerBounds, upperBounds;
    private ContinuousProblem problem;
    private Random random;

    public LDWParticle(double omegaStart, double omegaEnd, double c1, double c2) {
        this.omegaStart = omegaStart;
        this.omegaEnd = omegaEnd;
        this.c1 = c1;
        this.c2 = c2;
    }

    @Override
    public void init(ContinuousProblem problem, Random random) {
        this.problem = problem;
        this.random = random;
        this.dim = problem.getDimension();
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
        this.velocity = new double[dim];
        this.iteration = 0;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override
    public double[] initialX() {
        double[] x = new double[dim];
        for (int i = 0; i < dim; i++) {
            double range = upperBounds[i] - lowerBounds[i];
            x[i] = lowerBounds[i] + random.nextDouble() * range;
        }
        this.position = problem.copyX(x);
        this.pBest = problem.copyX(x);
        return x;
    }

    @Override
    public double[] search(PSOState<double[]> state) {
        // 线性递减惯性权重
        double omega = omegaStart - (omegaStart - omegaEnd) * ((double) iteration / maxIterations);
        iteration++;

        double[] gBest = findGBest(state);
        for (int d = 0; d < dim; d++) {
            double r1 = random.nextDouble();
            double r2 = random.nextDouble();
            velocity[d] = omega * velocity[d]
                    + c1 * r1 * (pBest[d] - position[d])
                    + c2 * r2 * (gBest[d] - position[d]);
            position[d] += velocity[d];
            if (position[d] < lowerBounds[d]) position[d] = lowerBounds[d];
            if (position[d] > upperBounds[d]) position[d] = upperBounds[d];
        }
        if (problem.compare(position, pBest) > 0) {
            this.pBest = problem.copyX(position);
        }
        return problem.copyX(position);
    }

    private double[] findGBest(PSOState<double[]> state) {
        double[][] positions = state.getCurrentXs();
        double[] best = positions[0];
        for (int i = 1; i < positions.length; i++) {
            if (problem.compare(positions[i], best) > 0) best = positions[i];
        }
        return best;
    }
}
```

## 🎯 自定义问题

要让 MPSO 优化你的问题，只需创建一个类实现 `Problem<X>` 接口：

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

    private static double[] createBounds(int dim, double value) {
        double[] bounds = new double[dim];
        java.util.Arrays.fill(bounds, value);
        return bounds;
    }
}
```

## ⚠️ 设计约束

**同步更新**：本框架实现的是同步 PSO。每轮迭代中，所有粒子基于同一个稳定的群体状态执行搜索，新位置收集完毕后一次性替换整个群体状态。异步 PSO 需要不同的实现，不在本类范围内。

**不可变性**：`search()` 返回的解必须是独立新对象（防御性拷贝），不得返回粒子内部状态引用。粒子内部的位置修改是允许的（原地更新），但必须确保不修改 `state.getCurrentXs()` 中的解。

**纯函数**：`compare()` 内部调用的评估逻辑必须是纯函数（相同输入 → 相同输出）。若实现了 `Evaluable`，`evaluate()` 也必须是纯函数且每次返回独立新对象。

**随机数复用**：所有粒子共享主算法注入的同一 `Random` 实例，不应自行创建独立的随机数生成器。

**异构群体**：粒子数组中的元素类型可以不同，只要它们都实现 `Particle<X, ?, BasicPSOState<X>>`。这使得同一轮迭代中不同粒子可以执行完全不同的搜索策略。

**JIT 优化**：`BasicPSOState` 和 `StandardPSOParticle`、`CustomParticle` 均声明为 `final`，触发 JIT 编译器的 CHA（类层次分析）去虚拟化优化，消除虚方法调用开销。

## 📋 核心方法语义

### Problem.compare(X x1, X x2)

比较两个解的目标值的优劣，返回带符号的差值以指示优劣程度。本方法内部可能会调用 `evaluate()` 获取目标值后进行对比：

| 返回值 | 含义 |
|--------|------|
| `> 0`（正值） | `evaluate(x1)` 优于 `evaluate(x2)` |
| `< 0`（负值） | `evaluate(x1)` 劣于 `evaluate(x2)` |
| `= 0` | 两者等优 **或** 无法比较（无支配关系） |

### Particle.search(PSOState state)

执行一步搜索，从当前群体状态出发返回新位置：

| 返回值 | 含义 |
|--------|------|
| 非 null 的 X | 粒子移动后的新位置（防御性拷贝，调用方拥有所有权） |

- 粒子内部自行维护位置、速度、pBest 等状态
- gBest 由粒子自行从 `state.getCurrentXs()` 中评估得出
- 返回的必须是新对象，不得返回粒子内部引用

### PSOTerminationCondition.check(PSOState state)

判断算法是否应当终止：

| 返回值 | 含义 |
|--------|------|
| `true` | 满足终止条件，算法将停止迭代 |
| `false` | 继续迭代 |

- 实现可通过内部计数器统计调用次数来推导迭代次数
- 与 SA 不同，PSO 终止条件不需要处理冷启动（PSO 状态始终包含完整的粒子位置信息）

## 📚 相关文件

| 文件 | 用途 |
|------|------|
| [AGENTS.md](AGENTS.md) | AI Agent 使用指南 |
| [../../README.md](../../README.md) | 项目总览 |
| [../../QUICKSTART.md](../../QUICKSTART.md) | 快速开始 |
| `core/Particle.java` | 粒子接口定义 |
| `core/PSOState.java` | 状态封装 |
| `BasicPSO/ParticleSwarmOptimization.java` | 主循环实现 |
| `components/CustomParticle.java` | 组合式粒子 |
| `components/continuousproblem/StandardPSOParticle.java` | 标准 PSO 粒子实现 |
| `../../oa/components/terminationcondition/` | 通用终止条件（MaxCallTerminationCondition 等） |