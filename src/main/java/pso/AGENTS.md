# AI Agent 使用指南 — 模块化粒子群优化（MPSO）

本文档面向 AI Agent 和开发者，提供快速理解和扩展 MPSO 框架的实用指南。

## 🎯 核心认知

MPSO 是一个**模块化优化算法框架**，将粒子群优化拆解为两个可替换组件：

| 组件 | 职责 | 调用时机 | 继承关系 |
|------|------|----------|----------|
| `Particle` | 初始化 + 搜索 | 初始时 + 每轮迭代 | `extends Initializer, SearchOperator`（`oa.api.spi`） |
| `PSOTerminationCondition` | 判断是否停止 | 每轮迭代后 | `extends TerminationCondition`（`oa.api.spi`） |

> **关键区别**：与传统 PSO 不同，MPSO 的粒子不是被动数据容器，而是**拥有自己行为的独立搜索单元**。每个粒子内部自行维护位置、速度、pBest，并自行从群体状态中评估 gBest。

## 📐 接口契约速查

### 泛型参数说明

所有组件和核心类使用三个泛型参数：

| 参数 | 含义 | 典型值 |
|------|------|--------|
| `X` | 解的表示类型 | `double[]`、`int[]`、自定义数据结构 |
| `Prob` | 问题类型，必须实现 `Problem<X>` | `ContinuousProblem`、自定义问题类 |
| `S` | 状态类型，必须实现 `State<X>` | `PSOState<X>`、`BasicPSOState<X>` |

示例：`Particle<double[], ContinuousProblem, PSOState<double[]>>` 表示一个处理连续空间的粒子。

### 组件通用模式

所有粒子遵循相同的生命周期：

```
构造 → init(problem, random) → initialX() → search(state) 被反复调用
```

### 关键方法签名

```java
// 粒子接口（继承自 Initializer + SearchOperator）
void init(Prob problem, Random random);
X initialX();
X search(S state);

// 终止条件
boolean check(PSOState<X> state);
```

`State<X>` 接口通过数组模式统一提供所有粒子位置的访问：

```java
state.getCurrentXs()          // 返回 X[]，长度为粒子数
for (X x : state.getCurrentXs()) {
    // 遍历每个粒子的当前位置
}
```

> **与 SA 的区别**：SA 的 `getCurrentXs()` 返回的数组始终只含一个元素（当前解），而 PSO 的数组长度等于粒子数。编写跨算法通用组件时，必须使用 for-each 循环遍历。

## 🔑 最少信息原则

PSO 状态**只传递无法自行推导的信息**：

- ❌ 不传递 gBest → 粒子可从 `getCurrentXs()` 中评估所有位置自行得出
- ❌ 不传递 pBest → 粒子内部自行维护
- ❌ 不传递目标函数值 → 组件可通过持有的 `Problem` 引用自行评估
- ❌ 不传递迭代次数 → 组件内部维护计数器

- ✅ 传递 `currentPositions` — 所有粒子当前位置

## 🛠️ 常见任务

### 任务一：定义新问题

```java
public class MyProblem extends ContinuousProblem {
    public MyProblem(int dimension) {
        super(createBounds(dimension, -100), createBounds(dimension, 100));
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

### 任务二：自定义标准 PSO 粒子

直接实现 `Particle` 接口，在单个类中完成所有逻辑：

```java
public class MyParticle implements Particle<double[], ContinuousProblem, PSOState<double[]>> {
    private double[] position, velocity, pBest;
    private double omega, c1, c2;
    private int dim;
    private double[] lowerBounds, upperBounds;
    private ContinuousProblem problem;
    private Random random;

    public MyParticle(double omega, double c1, double c2) {
        this.omega = omega;
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

### 任务三：异构粒子群

使用 `CustomParticle` 组合不同的初始化和搜索策略：

```java
// 粒子 0：标准 PSO
Particle<double[], ContinuousProblem, BasicPSOState<double[]>> p0 =
    new CustomParticle<>(
        new UniformInitializer(),
        new StandardPSOSearch(0.729, 1.49445, 1.49445)
    );

// 粒子 1：自适应惯性权重
Particle<double[], ContinuousProblem, BasicPSOState<double[]>> p1 =
    new CustomParticle<>(
        new UniformInitializer(),
        new AdaptiveInertiaSearch(0.9, 0.4)
    );
```

### 任务四：运行优化

```java
MyProblem problem = new MyProblem(2);
ParticleSwarmOptimization<double[]> pso =
    new ParticleSwarmOptimization<>(
        problem,
        new Particle[] {
            new StandardPSOParticle(0.729, 1.49445, 1.49445),
            new StandardPSOParticle(0.729, 1.49445, 1.49445),
            new StandardPSOParticle(0.729, 1.49445, 1.49445)
        },
        new MaxCallTerminationCondition(10000)
    );

BestRecorder<double[]> recorder = new BestRecorder<>();
pso.solve(recorder);
System.out.println("最优值: " + problem.evaluate(recorder.getBestX()));
```

### 任务五：可复现结果

```java
ParticleSwarmOptimization<double[]> pso =
    new ParticleSwarmOptimization<>(
        new Random(42),  // 固定种子
        problem,
        particles,
        new MaxCallTerminationCondition(10000)
    );
```

## ⚠️ 关键约束

| 约束 | 说明 |
|------|------|
| 同步更新 | 所有粒子基于同一稳定群体状态搜索，新位置收集完毕后一次性替换 |
| 不可变性 | `search()` 返回的解必须是独立新对象（防御性拷贝），不得返回粒子内部状态引用 |
| 纯函数 | `compare()` 内部调用的评估逻辑必须是纯函数，相同输入 → 相同输出 |
| 随机数 | 使用注入的 `Random`，不得自行创建 |
| 线程安全 | 框架单线程运行，组件内部状态需自行同步 |
| 异构群体 | 粒子数组中的元素类型可以不同，只要都实现 `Particle<X, ?, BasicPSOState<X>>` |

## 📋 核心方法语义

### Problem.compare(X x1, X x2)

比较两个解对应的目标值的优劣，返回带符号的差值以指示优劣程度。本方法内部可能会调用 `evaluate()` 获取目标值后进行对比：

| 返回值 | 含义 |
|--------|------|
| `> 0`（正值） | `x1` 优于 `x2` |
| `< 0`（负值） | `x1` 劣于 `x2` |
| `= 0` | 两者等优 **或** 无法比较（无支配关系） |

- 返回值的**绝对值**表示优劣差距的大小，绝对值越大差距越显著
- 此方法**不保证**反对称性（即 `compare(A, B) != -compare(B, A)` 可能成立）
- 示例：最小化问题中 `compare(x1, x2) = evaluate(x2) - evaluate(x1)`，当 `evaluate(x1) < evaluate(x2)` 时返回正值

### Particle.search(S state)

执行一步搜索，从当前群体状态出发返回新位置：

| 返回值 | 含义 |
|--------|------|
| 非 null 的 X | 粒子移动后的新位置（防御性拷贝，调用方拥有所有权） |

- 粒子内部自行维护位置、速度、pBest 等状态
- gBest 由粒子自行从 `state.getCurrentXs()` 中评估得出
- 返回的必须是新对象，不得返回粒子内部引用

### Particle.initialX()

生成粒子的初始位置：

| 返回值 | 含义 |
|--------|------|
| 非 null 的 X | 随机初始位置（调用方拥有所有权） |

- 必须是独立新对象
- 同时初始化粒子内部状态（position、pBest 等）

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
| [README.md](README.md) | 模块详细文档 |
| [../../README.md](../../README.md) | 项目总览 |
| [../../QUICKSTART.md](../../QUICKSTART.md) | 快速开始 |
| `core/Particle.java` | 粒子接口定义 |
| `core/PSOState.java` | 状态封装 |
| `BasicPSO/ParticleSwarmOptimization.java` | 主循环实现 |
| `components/CustomParticle.java` | 组合式粒子 |
| `components/continuousproblem/StandardPSOParticle.java` | 标准 PSO 粒子实现 |
| `../../oa/components/terminationcondition/` | 通用终止条件（MaxCallTerminationCondition 等） |