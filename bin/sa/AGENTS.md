# AI Agent 使用指南 — 模块化模拟退火（MSA）

本文档面向 AI Agent 和开发者，提供快速理解和扩展 MSA 框架的实用指南。

## 🎯 核心认知

MSA 是一个**模块化优化算法框架**，将模拟退火拆解为四个可替换组件：

| 组件 | 职责 | 调用时机 | 继承关系 |
|------|------|----------|----------|
| `SAInitializer` | 生成初始解和初始温度 | 算法启动时 | `extends Initializer`（`oa.api.spi`） |
| `SAPerturbation` | 从当前解生成候选解 | 每轮迭代 | `extends SearchOperator`（`oa.api.spi`） |
| `SACoolingSchedule` | 降低温度 | 每轮迭代后 | `extends Component`（`oa.api.spi`） |
| `SATerminationCondition` | 判断是否停止 | 每轮迭代后 | `extends TerminationCondition`（`oa.api.spi`） |

## 📐 接口契约速查

### 泛型参数说明

所有组件和核心类使用三个泛型参数：

| 参数 | 含义 | 典型值 |
|------|------|--------|
| `X` | 解的表示类型 | `double[]`、`int[]`、自定义数据结构 |
| `Prob` | 问题类型，必须实现 `Problem<X>` | `ContinuousProblem`、自定义问题类 |

示例：`SAPerturbation<double[], ContinuousProblem>` 表示一个处理连续空间扰动器。

### 组件通用模式

所有组件遵循相同的生命周期：

```
构造 → init(problem, random) → 核心方法被反复调用 → (可选) reset() → 重新使用
```

### 关键方法签名

```java
// 初始化器
X initialX();
double initialTemperature();

// 扰动器
X search(SAState<X> state);              // 继承自 SearchOperator，生成候选解

// 冷却策略
double cool(SAState<X> state);

// 终止条件
boolean check(SAState<X> state);
```

`State<X>` 接口通过数组模式统一提供当前解的访问：

| 模式 | 方法 | 适用场景 |
|------|------|----------|
| **数组**（统一） | `getCurrentXs()` | 所有解类型，遍历：`for (X x : state.getCurrentXs())`；SA 中数组只含一个元素，群体算法含多个 |

`SAState` 在基类之上额外封装了 SA 特有的字段：

```java
// SA 专用简写：SAState 的数组始终只包含一个元素（当前解），
// 因此 SA 组件可直接通过索引 [0] 获取当前解
state.getCurrentXs()[0]            // 当前解（只读）
state.getTemperature()             // 当前温度
state.getIsAccepted()              // 上一轮是否接受新解（见下方冷启动说明）
```

> **⚠️ 重要**：`getCurrentXs()[0]` 直接索引是 SA 组件的特权。若编写同时支持 SA 和其他算法（如群体算法）的通用组件，必须使用 for-each 循环 `for (X x : state.getCurrentXs())` 遍历，因为其他算法的数组可能包含多个元素。

**冷启动细节**：
- `search()` 和 `check()` 的首次调用中，`isAccepted` 为 `false`（表示"尚无历史"）
- `cool()` 的首次调用发生在第一轮迭代**之后**，此时 `isAccepted` 已是 Metropolis 准则的真实结果，**不是**默认 `false`

## 🔑 最少信息原则

组件接口**只传递无法自行推导的信息**：

- ❌ 不传递目标函数值 → `evaluate()` 已从 `Problem` 移除，组件应通过 `compare()` 比较解
- ❌ 不传递是否改进 → 组件可通过 `Problem.compare()` 比较
- ❌ 不传递迭代次数 → 组件内部维护计数器

- ✅ 传递 `currentX`、`temperature`、`isAccepted`

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

### 任务二：自定义扰动器

```java
public class GaussianPerturbation implements SAPerturbation<double[], ContinuousProblem> {
    private ContinuousProblem problem;
    private Random random;

    @Override
    public void init(ContinuousProblem problem, Random random) {
        this.problem = problem;
        this.random = random;
    }

    @Override
    public double[] search(SAState<double[]> state) {
        double[] x = state.getCurrentXs()[0];
        double[] newX = problem.copyX(x);
        double temperature = state.getTemperature();

        for (int i = 0; i < newX.length; i++) {
            newX[i] += random.nextGaussian() * temperature * 0.1;
        }
        return newX;
    }
}
```

### 任务三：自适应冷却策略

```java
public class AdaptiveCooling implements SACoolingSchedule<double[], ContinuousProblem> {
    private double baseRate;
    private int acceptedCount;
    private int totalCalls;

    public AdaptiveCooling(double baseRate) {
        this.baseRate = baseRate;
        this.acceptedCount = 0;
        this.totalCalls = 0;
    }

    @Override
    public void init(ContinuousProblem problem, Random random) {}

    @Override
    public double cool(SAState<double[]> state) {
        totalCalls++;
        if (state.getIsAccepted()) acceptedCount++;

        double rate = baseRate;
        if (totalCalls > 100) {
            double acceptRate = (double) acceptedCount / totalCalls;
            rate = acceptRate > 0.5 ? 0.95 : 0.99;
        }
        return state.getTemperature() * rate;
    }
}
```

### 任务四：运行优化

```java
MyProblem problem = new MyProblem(2);
SimulatedAnnealing<double[]> sa =
    new SimulatedAnnealing<>(
        problem,
        new SABasicInitializer(100),
        new ContinuousUniformSearch(),
        new SABasicCoolingSchedule(0.99, 100),
        new MaxCallTerminationCondition<double[]>(10000)
    );

BestRecorder<double[]> recorder = new BestRecorder<>(problem);
sa.solve(recorder);
System.out.println("最优值: " + problem.evaluate(recorder.getBestX()));
```

### 任务五：可复现结果

```java
SimulatedAnnealing<double[]> sa =
    new SimulatedAnnealing<>(
        new Random(42),  // 固定种子
        problem,
        new SABasicInitializer(100),
        new ContinuousUniformSearch(),
        new SABasicCoolingSchedule(0.99, 100),
        new MaxCallTerminationCondition<double[]>(10000)
    );
```

## ⚠️ 关键约束

| 约束 | 说明 |
|------|------|
| 冷启动 | `search()` 和 `check()` 首次调用时 `isAccepted` 为 `false`，应视为冷启动信号；`cool()` 首次调用时 `isAccepted` 已是真实结果 |
| 不可变性 | 不得原地修改 `state.getCurrentXs()[0]` 获取的解，必须返回新对象 |
| 纯函数 | `compare()` 内部调用的评估逻辑必须是纯函数，相同输入 -> 相同输出。若实现了 `Evaluable`，`evaluate()` 也必须是纯函数且每次返回独立新对象 |
| 随机数 | 使用注入的 `Random`，不得自行创建 |
| 线程安全 | 框架单线程运行，组件内部状态需自行同步 |

## 📋 核心方法语义

### Problem.compare(X x1, X x2)

比较两个解对应的的目标值的优劣，返回带符号的差值以指示优劣程度。本方法内部可能会调用 `evaluate()` 获取目标值后进行对比：

| 返回值 | 含义 |
|--------|------|
| `> 0`（正值） | `x1` 优于 `x2` |
| `< 0`（负值） | `x1` 劣于 `x2` |
| `= 0` | 两者等优 **或** 无法比较（无支配关系） |

- 返回值的**绝对值**表示优劣差距的大小，绝对值越大差距越显著
- 此方法**不保证**反对称性（即 `compare(A, B) != -compare(B, A)` 可能成立）
- 示例：最小化问题中 `compare(x1, x2) = evaluate(x2) - evaluate(x1)`，当 `evaluate(x1) < evaluate(x2)` 时返回正值

### SATerminationCondition.check(SAState state)

判断算法是否应当终止：

| 返回值 | 含义 |
|--------|------|
| `true` | 满足终止条件，算法将停止迭代 |
| `false` | 继续迭代 |

- 首次调用时 `state.getIsAccepted()` 为 `false`（冷启动），不应据此决定是否终止
- 实现可通过内部计数器统计调用次数来推导迭代次数

## 📚 相关文件

| 文件 | 用途 |
|------|------|
| [README.md](README.md) | 模块详细文档 |
| [../../README.md](../../README.md) | 项目总览 |
| [../../QUICKSTART.md](../../QUICKSTART.md) | 快速开始 |
| `core/SimulatedAnnealing.java` | 主循环实现 |
| `core/SAState.java` | 状态封装 |
| `components/basiccomponents/` | 内置组件实现 |
| `../../oa/components/terminationcondition/` | 通用终止条件（MaxCallTerminationCondition 等） |