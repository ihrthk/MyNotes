# 两年 Compose 项目实践总结，常见错误一览 - 掘金

[https://juejin.cn/post/7139864943918055432](https://juejin.cn/post/7139864943918055432)

从 Compose 还在 alpha 到现在，用 Compose 完整的从零到一写了三个应用：[Twidere X Android](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2FTwidereProject%2FTwidereX-Android)、[Mask-Android](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2FDimensionDev%2FMask-Android)，还有一个暂未公开的项目，说实话这三个应用每一个都有不一样的收获，在立项的时候也吸取了之前的经验，在 2021 年的时候我在[另一个网站总结过一次经验](https://link.juejin.cn/?target=https%3A%2F%2Fzhuanlan.zhihu.com%2Fp%2F351794462)，我觉得现在是时候再总结一下了。

# 要点

直接说几个总结出来的要点吧：

1. Compose UI 最核心的一个思想就是：状态向下，事件向上，Compose UI 组件的状态都应该来自其参数而不是自身，不要在 Compose UI 组件中做任何计算，有非常多的性能问题其实是来自对于这一条核心思想的不理解。
2. 如果一个组件不得不内部持有一些状态，切记将这些状态所有的变量都用上 `remember`，因为 Compose 函数是会被非常频繁的执行，不用 `remember` 的话会导致频繁的赋值和初始化，甚至进行一些计算操作。
3. Compose UI 组件的参数最好是不可变（immutable）的，否则最好的情况是遇到和预期表现不符，最差的情况就是影响到性能了。
4. 每个 Compose UI 组件最好都有 Modifier，这样 Compose UI 组件就可以很方便的在不同地方复用。
5. 为了可维护性，请尽量拆分基础 Compose UI 组件和业务 Compose UI 组件，基础 Compose UI 组件尽量拆分的细一些，业务 Compose UI 组件看情况，最好也要拆分的细一些，你不会想去维护一个上千行的 Compose UI 组件的，同时细分也会提高一定的复用率。

# 常见错误用法

这里总结了一些常见的不正确的用法，其中大部分会导致性能问题，有很多人会说 Compose 性能差，但其实更多的是本身的用法有误。

## 滥用 `remember { mutableStateOf() }`

Compose UI 最核心的一个思想就是：状态向下，事件向上。这句话举个例子可能会更好理解。

一般初学者在看完教程之后马上就会写下这样的代码：

```
Kotlin复制代码@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    Button(
        onClick = {
            count++
        }
    ) {
        Text("count $count")
    }
}

```

然后当业务逻辑复杂之后，他的代码可能会像这样：

```
Kotlin复制代码@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }

    Column {
        Button(
            onClick = {
                count++
            }
        ) {
            Text("count $count")
        }
        TextField(
            value = text,
            onValueChange = {
                text = it
            }
        )
        OtherCounter()
    }
}

@Composable
fun OtherCounter() {
    var text by remember { mutableStateOf("Hello world!") }
    Column {
        Text(text)
        TextField(
            value = text,
            onValueChange = {
                text = it
            }
        )
    }
}

```

抛开代码的业务逻辑不谈，这里的 Composable 函数是带状态的，这会带来不必要的 recomposition，从而导致写出来的 Compose UI 出现性能问题，按照核心思想状态向下，事件向上，上面的代码应该这样写：

```
复制代码@Composable
fun CounterRoute(
    viewModel: CounterViewModel = viewModel<CounterViewModel>()
) {
    val state by viewModel.state.collectAsState()
    Counter(
        state = state,
        onIncrement = {
            viewModel.onIncrement()
        },
        onTextChange = {
            viewModel.onTextChange(it)
        },
        onOtherTextChange = {
            viewModel.onOtherTextChange(it)
        },
    )
}

@Composable
fun Counter(
    state: CounterState,
    onIncrement: () -> Unit,
    onTextChange: (String) -> Unit,
    onOtherTextChange: (String) -> Unit,
) {
    Column {
        Button(
            onClick = {
                onIncrement.invoke()
            }
        ) {
            Text("count ${state.count}")
        }
        TextField(
            value = state.text,
            onValueChange = {
                onTextChange.invoke(it)
            }
        )
        OtherCounter(
            text = state.otherText,
            onTextChange = onOtherTextChange,
        )
    }
}

@Composable
fun OtherCounter(
    text: String,
    onTextChange: (String) -> Unit,
) {
    Column {
        Text(text)
        TextField(
            value = text,
            onValueChange = {
                onTextChange.invoke(it)
            }
        )
    }
}

```

这样的写法吧所有状态都放到顶层，同时事件也交由顶层处理，这样的 Compose UI 组件是没有任何状态的，这样的的 Compose UI 组件会有非常好的性能。

## 忘记 `remember`

刚刚说完滥用，现在说忘记。当一个组件不得不内部持有状态的时候，这个时候切记：一定要吧所有的变量都用上 `remember`。

常见的有这样的错误：

```
Kotlin复制代码@Composable
fun SomeList() {
    val list = listOf("a", "b", "c")
    LazyColumn {
        items(list) {
            Text(it)
        }
    }
}

```

这里的 `list` 完全没有被 `remember`，而 Compose 函数会非常频繁的执行，这就导致每次执行到 `val list = listOf("a", "b", "c")` 的时候都会有一次生成赋值甚至计算的操作，这样的写法是非常影响性能的，正确的写法应该是这样：

```
Kotlin复制代码@Composable
fun SomeList() {
    val list = remember { listOf("a", "b", "c") }
    LazyColumn {
        items(list) {
            Text(it)
        }
    }
}

```

当然最好是把 `list` 移到参数上：

```
Kotlin复制代码@Composable
fun SomeList(
    list: List<String>,
) {
    LazyColumn {
        items(list) {
            Text(it)
        }
    }
}

```

## 参数是可变的

还是接着上一个例子，光是 `list` 移动到参数还是不够的，因为你可以在 Composable 函数外边更改这个列表，比如执行 `list.add("")` 的操作，Compose 编译器会认为这个 Composable 函数仍然是带状态的，所以还不是最优化的状态。最好是使用 [kotlinx.collections.immutable](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2FKotlin%2Fkotlinx.collections.immutable) 里面的 `ImmutableList`：

```
Kotlin复制代码@Composable
fun SomeList(
    list: ImmutableList<String>,
) {
    LazyColumn {
        items(list) {
            Text(it)
        }
    }
}

```

除了基础类型之外，其他参数中的自定义 class 最好是标记上 `@Immutable`，这样 Compose 编译器会优化这个 Composable 函数。当然不要定义一个 `data class` 然后里面一个 `var a: String` 然后问为什么 `a.a = "b"` 没有效果，建议传给 Composable 函数的 `data class` 全是 `val`。

## 没开启 R8

R8 对于 Compose 的提升是非常巨大的，如果是简单 UI 的话没有 R8 可能还可以用，复杂 UI 下非常推荐开启 R8，代码优化之后的性能的 Debug 的性能差距极大。

# 最后

其实理解了 Compose UI 的核心思想之后，写出来的 Compose 程序应该不会有什么性能问题，而且在这个核心思想下写出来的 Compose UI 逻辑非常的清晰，因为整个 UI 是无状态的，你只需要关系在什么状态下这个 UI 显示的是什么样的，心智负担非常小。

举个[例子](https://link.juejin.cn/?target=https%3A%2F%2Ftwitter.com%2Fmtlaster%2Fstatus%2F1566702596596699136)：

> Some Twitter Client for #Android and #iOS
> 
> 
> But written in [#JetpackCompose](https://link.juejin.cn/?target=https%3A%2F%2Ftwitter.com%2Fhashtag%2FJetpackCompose%3Fsrc%3Dhash%26ref_src%3Dtwsrc%255Etfw) [#MaterialYou](https://link.juejin.cn/?target=https%3A%2F%2Ftwitter.com%2Fhashtag%2FMaterialYou%3Fsrc%3Dhash%26ref_src%3Dtwsrc%255Etfw) looks not that bad in iOS.
> 
> 👀 [pic.twitter.com/sWPHVTlA7k](https://link.juejin.cn/?target=https%3A%2F%2Ft.co%2FsWPHVTlA7k)
> 
> [September 5, 2022](https://link.juejin.cn/?target=https%3A%2F%2Ftwitter.com%2FMTlaster%2Fstatus%2F1566702596596699136%3Fref_src%3Dtwsrc%255Etfw)
> 

可以看到在 Android 上其实 `LazyColumn` 也是非常流畅的。

另外，Compose 纯函数无状态真的是非常的解放心智！从此画 UI 真的是信手拈来，每个 UI 状态都了如指掌。

上一次我总结之后还总结几个 2022 年的 Compose 面试题，今年已经 2022 了，我们来总结一下 2023 年 Compose 的面试题吧：

- 什么是有状态的 Composable 函数？什么是无状态的 Composable 函数？
- Compose 的状态提升如何理解？有什么好处？
- 如何理解 MVI 架构？和 MVVM、MVP、MVC 有什么不同的？
- 在 Android 上，当一个 Flow 被 `collectAsState`，应用转入后台时，如果这个 Flow 再进行更新，对应的 State 会不会更新？对应的 Composable 函数会不会更新？

好像总结的也不是很多的样子