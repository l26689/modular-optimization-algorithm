# Modular Optimization Algorithm (MOA)

[![Java](https://img.shields.io/badge/Java-25+-blue.svg)](https://www.java.com)

一个模块化优化算法框架。将算法拆解为清晰的可替换组件，像乐高一样自由组合。

面向学习、实验和定制化：每一块组件都拥有清晰的接口契约，可以独立阅读、测试和替换。

---

## 架构概览

```
src/main/java/moa/
├── framework/      抽象层 —— 接口与抽象类，通用
│   ├── api/        SPI 组件契约（Component, Initializer, Recorder, ...）
│   ├── engine/     算法运行时抽象（OptimizationAlgorithm, State）
│   └── problem/    问题域定义（Problem, Evaluable）
│
├── components/     无约束的通用实现
│
├── problem/        问题定义 + 问题约束组件
│
├── algorithm/      算法模块
│   ├── sa/  api / components / engine
│   ├── pso/ api / components / engine
│   ├── ga/  api / components / engine
│   └── ……
│
├── example/        演示用法
└── benchmark/      实验脚本
```


> 完整设计说明见 [docs/DESIGN.md](docs/DESIGN.md)。

---

## 快速开始

```bash
git clone https://github.com/l26689/modular-optimization-algorithm.git
cd modular-optimization-algorithm
mvn compile
```

运行 Sphere 示例：

```bash
mvn exec:java -Dexec.mainClass="moa.example.SphereProblemDemo"
```

或运行 Rosenbrock 示例：

```bash
mvn exec:java -Dexec.mainClass="moa.example.RosenbrockDemo"
```

---

## 核心设计

### 泛型约束

用泛型在编译期表达兼容性依赖，接口签名即约束声明：

```java
Component<X, Prob extends Problem<X>, S extends State<X>>
```

三个参数分别约束解的表示、问题类型、状态类型（适配算法）。算法模块可进一步收窄。

### SPI 接口

| 接口 | 职责 |
|---|---|
| Component | 基契约：init(Prob, Random) 绑定上下文 |
| Initializer | initializeX() 生成初始解 |
| SearchOperator | search(State) 执行状态转移 |
| Recorder | record(State) 记录优化过程 |
| TerminationCondition | check() 判定停止 |
| Reusable | reset() 回到可用状态 |

所有算法（SA、PSO、GA、DE）都是对上述接口的不同装配方式。

### 组件生命周期

构造(纯参数) -> init(Prob, Random) -> 业务方法(反复调用) -> reset()

### 可复现性

单一 Random 实例贯穿引擎和所有组件，外部传入固定种子即可复现任何优化过程。

### State 与安全

State.getCurrentXs() 统一以数组暴露当前解（单解算法一个元素，群体算法多个）。State 实例在迭代间复用，保存解前必须深拷贝（Problem.copyX()）。

---

## 内置组件

### Recorder

| 类 | 说明 |
|---|---|
| BestRecorder | 记录历史最优解 |
| LastRecorder | 记录最后接受的解 |
| ConvergenceRecorder | 收敛曲线可视化（JFreeChart） |

### TerminationCondition

| 类 | 说明 |
|---|---|
| MaxCallTerminationCondition | 基于总调用次数的终止条件 |

---

## 内置问题

| 类 | 最优值 | 特点 |
|---|---|---|
| SphereProblem | 0 | 单峰、可分离 |
| RosenbrockProblem | 0 | 香蕉形山谷、不可分离 |
| RastriginProblem | 0 | 多峰、高度多模态 |
| GriewankProblem | 0 | 多峰、边界效应 |
| AckleyProblem | 0 | 多峰、指数衰减 |

---

## 设计约束

- 冷启动：首次迭代前状态字段为默认值，组件须正确处理"无历史"情况
- 纯函数：evaluate() 和 compare() 必须是纯函数（相同输入->相同输出）
- 线程安全：框架默认单线程，组件内部可变状态需自行同步
- 随机数：组件不得自建 Random，统一由引擎注入

---

## 文档

| 文档 | 内容 |
|---|---|
| [DESIGN.md](docs/DESIGN.md) | 完整设计文档（泛型、生命周期、安全、Javadoc 规范） |
| [QUICKSTART.md](QUICKSTART.md) | 快速上手指南 |
| Javadoc | 各组件类的详细说明（含参数绑定和适用场景） |

---

## 许可

MIT License