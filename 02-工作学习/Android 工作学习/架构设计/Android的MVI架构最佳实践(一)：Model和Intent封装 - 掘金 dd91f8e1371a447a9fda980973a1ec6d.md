# Android的MVI架构最佳实践(一)：Model和Intent封装 - 掘金

[https://juejin.cn/post/7278498472860909605](https://juejin.cn/post/7278498472860909605)

# Android的MVI架构最佳实践(一)：Model和Intent封装

[forJrking](https://juejin.cn/user/2612095355987191/posts)

2,604   阅读6分钟

专栏：

- [Android的MVI架构最佳实践(一)：Model和Intent](https://juejin.cn/post/7278498472860909605)
- [Android的MVI架构最佳实践(二)：View和repeatOnLifecycle](https://juejin.cn/post/7278496260478746661)
- [Android的MVI架构最佳实践(三)：Compose封装](https://juejin.cn/post/7278857046946496549)
- [Android的MVI架构最佳实践(四)：单元测试](https://juejin.cn/post/7281080737490059299)
- [Android的MVI架构最佳实践(五)：UI测试](https://juejin.cn/post/7291561432781013052)

## 前言

在此篇中我们会简单介绍MVI的设计思想，并基于`Android Jetpack Components`实现可以用于Activity、Fragment、Compose的MVI的架构设计。主旨在简化大量的MVI模版代码，提升开发效率和统一代码结构，并且会提供单元测试和UI测试指南，可帮助我们更好地组织代码以创建健壮且可维护的应用程序。你需要储备的知识点`androidx.lifecycle和kotlin-coroutines`，以及`Flow和Channel`。

## MVI简介

与MVC，MVP或MVVM一样，MVI是一种体系结构设计模式，与Flux或Redux属于同一家族。提倡一种单向可信任数据流的设计思想，非常适合数据驱动型的UI展示项目。当然MVI也有很多缺点，可以在其他博客中了解。由于MVI和声明式UI是绝配，所以在Android的Compose中将很有前景，我们必须要掌握。

[](Android%E7%9A%84MVI%E6%9E%B6%E6%9E%84%E6%9C%80%E4%BD%B3%E5%AE%9E%E8%B7%B5(%E4%B8%80)%EF%BC%9AModel%E5%92%8CIntent%E5%B0%81%E8%A3%85%20-%20%E6%8E%98%E9%87%91/17b11507f81d43ef8b6470be8cab9b56tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

**MVI**即“模型”(Model)，“视图”(View)和“意图”(Intent)单词词缩写而成：

- **Model**: 与其他MVVM中的Model不同的是，MVI的Model主要指UI状态（State）。当前界面展示的内容无非就是UI状态的一个快照：例如数据加载过程、控件位置等都是一种UI状态
- **View**: 与其他MVX中的View一致，可能是一个Activity、Fragment或者任意UI承载单元。MVI中的View通过订阅State的变化实现界面刷新
- **Intent**: 此Intent不是Activity的Intent，用户的任何操作都被包装成UserIntent后发送给Model进行数据请求

## MVI的Model和Intent封装

[](Android%E7%9A%84MVI%E6%9E%B6%E6%9E%84%E6%9C%80%E4%BD%B3%E5%AE%9E%E8%B7%B5(%E4%B8%80)%EF%BC%9AModel%E5%92%8CIntent%E5%B0%81%E8%A3%85%20-%20%E6%8E%98%E9%87%91/aee46a5796de446db5ffeab24e9ea1d6tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

从[android单向数据流(UDF)](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.com%2Ftopic%2Farchitecture%2Fui-layer%2Fstateholders%3Fhl%3Dzh-cn)界面层指南图中可以看到，为了统一管理这个单线的数据流我们使用`ViewModel`来作为封装容器和UI交互。UI上的一些点击或者用户事件，都会封装成**events**，发送给**ViewModel**，再由`ViewModel`转换data为`UI state`传递给UI。

### `M`、`I`的结构

为了防止从字面上混淆Model和Intent的概念，我们在这里给他们分别起别名用于区分。**Action**代`Intent或者图中的events`。`Model(State)`一分为二：UIState一般是一种持久的UI形态，在发生生命周期变化时候需要回放。UIEffect一般是一次性消费UI事件，如弹窗、toast、导航等。所以我们拆分Model为`State和Effect`。

```
复制代码/** 用户与ui的交互事件*/
interface Action
/** ui响应的状态*/
interface State
/** ui响应的事件*/
interface Effect

```

### ViewModel中`M`、`I`的管理

### **Model(State)**

State需要订阅观察者模式给view提供数据，在非Compose中我们可以使用`LiveData和StateFlow`, 在Compose中我们可以直接使用`State`。为了兼容性我们选择`StateFlow或者自定义SharedFlow`。

```
kotlin复制代码abstract class BaseViewModel<S : State> : ViewModel() {
    /**继承BaseViewModel需要实现state默认值*/
    abstract fun initialState(): S

    private val _state by lazy {
        MutableStateFlow(value = initialState())
    }
    /**在view中用于订阅*/
    val state: StateFlow<S> by lazy { _state.asStateFlow() }

    protected fun emitState(builder: suspend () -> S?) = viewModelScope.launch {
        builder()?.let { _state.emit(it) }
    }

    /**suspend 函数在flow或者scope中emit状态*/
    protected suspend fun emitState(state: S) = _state.emit(state)
}

```

如果我们想使用`livedata`一样不需要默认值，我们可以自定义SharedFlow可以实现一样的效果，但是由于SharedFlow默认是不防抖的，所以我们要借助函数`kotlinx.coroutines.flow.distinctUntilChanged()`，最终实现如下，后续代码展示中我们使用这种:

```
复制代码private val _state = MutableSharedFlow<S>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
val state: Flow<S> by lazy { _state.distinctUntilChanged() }

```

### **Model(Effect)**

Effect 指Android中的一次性事件，比如toast、navigation、backpress、click等等，由于这些状态都是一次性的消费所以不能使用livedata和StateFlow，我们可以使用SharedFlow或者Channel，考虑多个Composable中要共享viewmodel获取sideEffect，这里使用SharedFlow更方便。

```
复制代码abstract class BaseViewModel<S : State, E : Effect> : ViewModel() {
    .... state 代码
    /**
     * [effect]事件带来的副作用，通常是一次性事件 例如：弹Toast、导航Fragment等
     */
    private val _effect = MutableSharedFlow<E>()
    val effect: SharedFlow<E> by lazy { _effect.asSharedFlow() }

    protected fun emitEffect(builder: suspend () -> E?) = viewModelScope.launch {
        builder()?.let { _effect.emit(it) }
    }

    protected suspend fun emitEffect(effect: E) = _effect.emit(effect)
}

```

### **UserIntent(Action)**

action用于描述各种请求State或者Effect的动作，由View发送ViewModel订阅消费，典型的生产者消费者模式，考虑是一对一的关系我们使用Channel来实现，*有些开发者喜欢直接调用ViewModel方法，如果方法还有返回值，就破坏了数据的单向流动。*

```
复制代码abstract class BaseViewModel<A : Action, S : State, E : Effect> : ViewModel() {

    private val _action = Channel<A>()

    init {
        viewModelScope.launch {
            _action.consumeAsFlow().collect {
                /*replayState：很多时候我们需要通过上个state的数据来处理这次数据，所以我们要获取当前状态传递*/
                onAction(it, replayState)
            }
        }
    }

    /** [actor] 用于在非viewModelScope外使用*/
    val actor: SendChannel<A> by lazy { _action }

    fun sendAction(action: A) = viewModelScope.launch {
        _action.send(action)
    }

    /** 订阅事件的传入 onAction()分发处理事件 */
    protected abstract fun onAction(action: A, currentState: S?)

    ....上面实现的代码部分
}

```

## Sample

需求: 登录页面点击登录按钮，请求网络返回登录结果，登录成功跳转，登录失败展示错误页面。 action: 登录按钮点击OnLogonClicked;

```
复制代码sealed class LogonAction : Action {
    object OnLogOnClicked : LogonAction()
}

```

state: 登录中Loading, 失败错误页面 Error

```
kotlin复制代码sealed class LogonState : State {
    object Loading : LogOnState()
    data class Error(val ex: Throwable) : LogonState()
}

```

effect: 登录成功跳转页面NavigationToHost

```
kotlin复制代码sealed class LogonEffect : Effect {
    data class NavigationToHost(val response: Int) : LogonEffect()
}

```

ViewModel在onAction中分发处理 action, repository获取数据。

```
kotlin复制代码class LogonViewModel(
    private val repo: LogonRepo
) : BaseViewModel<LogonAction, LogonState, LogonEvent>() {

    override fun onAction(action: LogonAction, currentState: LogonState?) {
        when (action) {
            LogonAction.OnLogonClicked -> logon()
            else ->{}
        }
    }

    private fun logon() {
        repo.fetchLogon()
            .onStart {
                emitState(LogonState.Loading)
            }.catch { ex ->
                emitState(LogonState.Error(ex))
            }.onEach { result ->
                emitEffect(LogonEvent.NavigationToHost(result))
            }.launchIn(viewModelScope)
    }
}

object LogonRepo {
    fun fetchLogon() = flow {
        delay(2500)
        emit(Random.nextInt(3))
    }.flowOn(Dispatchers.IO)
}

```

## 总结和补充

- 定义的Action,State,Effect中需要数据传递的，建议使用data class，而且的所有字段必须是val的，因为MVI需要单一的可信任的数据源。如果要对属性进行修改，可以使用copy函数。
- 由于所有的UI state都需要在viewModle中emit一个State对象会耗费资源，如果遇到频繁修改某个UI组件的需求，应该单独定义一个数据流给它单独使用，避免出现频繁GC内存抖动和卡顿问题。
- 由于state每次的变化都新创建对象，不要直接把repository的复杂的数据返回给view层处理，在这种情况我们一般对View所需的状态进行抽象一个View的model, 在ViewModel中通过reducer来做mapping，reducer的作用专门用来把remote或者local的data转换为state或者sideEffect。这样在单元测试时候可以单独对reducer和ViewModel覆盖。当逻辑变化时候单独修改reducer即可满足要求，相同的数据计算逻辑也可以进行复用reducer。
- 项目很复杂则需要对模块的功能更加单一，例如ViewModel仅仅负责管理，内部不包含任何判断和计算逻辑，使用reducer来mapping, CoroutineDispatcherProvider来管理线程切换,并使用hilt或者koin来辅助解耦。

jetpack结合MVI的设计思想很容易实现MVI的建议框架，在后续的coding中还是有很多细节要保持。下一篇中我们会封装Activity和Fragment来支持MVI中的View层，简化View中的模版代码并且处理flow替换livedata的生命周期感知问题。