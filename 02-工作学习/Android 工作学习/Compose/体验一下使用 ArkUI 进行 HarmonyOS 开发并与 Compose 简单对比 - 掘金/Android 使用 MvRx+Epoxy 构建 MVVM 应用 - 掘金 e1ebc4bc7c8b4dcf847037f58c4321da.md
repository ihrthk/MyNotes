# Android 使用 MvRx+Epoxy 构建 MVVM 应用 - 掘金

# 背景

在 Android 开发世界里面，大家对于MVVM等架构设计都比较熟悉了，大多数项目中会使用 RxJava 来搭建 MVVM 架构。本文介绍Airbnb开源的 MvRx 和 Epoxy框架，主要包含如下内容：

- MvRx 和 Epoxy 框架用法
- MvRx 如何初始化 ViewModel 和 State
- MvRx 如何实现数据和视图的关联 (修改 State 自动触发视图重绘)
- Epoxy 如何实现 diff 更新

开源的世界丰富多彩，笔者希望能够打开一扇窗，让我们从更多的视角来一窥响应式编程框架之美。

# MvRx+Epoxy基础介绍

## 简介

> 
> 
> - MvRx (ModelView ReactiveX, pronounced mavericks) is the Android framework from Airbnb that we use for nearly all product development at Airbnb.*When we began creating MvRx, our goal was not to create yet another architecture pattern for Airbnb, it was to make building products easier, faster, and more fun. All of our decisions have built on that. We believe that for MvRx to be successful, it must be effective for building everything from the simplest of screens to the most complex in our app.*

> 
> 
> 
> *Epoxy* *(iˈpäksē)* *is an Android library for building complex screens in a RecyclerView. [We developed Epoxy at Airbnb](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2Fairbnb-engineering%2Fepoxy-airbnbs-view-architecture-on-android-c3e1af150394%23.xv4ymrtmk)* *to simplify the process of working with RecyclerViews, and to add the missing functionality we needed. We now use Epoxy for most of the main screens in our app and it has improved our developer experience greatly.*
> 

这两个库均出自与 airbnb，上面是摘自github 中的项目介绍：

- [MvRx](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fairbnb%2FMvRx) 是 airbnb 构筑的一个应用架构，几乎用在了 airbnb 的全线产品上
- [Epoxy](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fairbnb%2Fepoxy) 主要帮助构建复杂的多 viewType 的 Recyclerview

可以看出，MvRx 和 Epoxy 的设计初衷均是为简化、提高开发效率的 (easier, faster, more fun)

## 设计理念

MvRx 综合运用了以下技术和概念：

- Kotlin
- Android Architecture Components
- RxJava
- React (conceptually)
- Epoxy (optional)

实际上，MvRx 和 Epoxy 是可以分开使用的，在 MvRx 的架构说明中，Epoxy 也是可选的。MvRx 在 google architecture 的基础上引入了 React 的一系列概念，但更加偏向于数据层的响应式设计。单独使用 MvRx，数据变化后，视图还需要自己去更新，这个更新往往又是命令式的。 (在 React 中，数据变化 ➡️ diff 计算视图变化 ➡️ 重新渲染，都是由框架自动执行的，开发者的任务只是描述视图)

Epoxy 被描述成是 RecyclerView 的辅助工具。airbnb 认为 adapter 配置 item 的方式比较呆板和混乱，不能很好支持他们的一些复杂场景如：viewType、pagination、tablet、item animations，于是用了一种新的方式来描述列表。结合 epoxy 可以实现视图层的响应式，开发者只描述视图及和数据的关系，diff 和更新由框架完成。

MvRx+Epoxy，结合 kotlin 的语法糖，写出来的代码非常像 React。这样开发出来的应用有如下特征：

- 声明式 UI
- 响应式架构（MVVM）

# 使用方式

## 核心概念

- State

MvRxState 包含渲染屏幕所需的所有数据，State 是不可变的，它的实现类必须是一个 immutable Kotlin data class，只有 ViewModel 能修改 State，修改 State 时应该调用 kotlin 数据类的 copy 方法，创建一个新的 State 对象，State 改变会触发 View 的 invalidate() 方法，从而进行界面重绘。

- ViewModel

MvRxViewModel 完全继承自 Google 应用架构中的 ViewModel，不同的是，MvRxViewModel 包含一个 State 而不是 LiveData，View 只能观察 State 的变化。 创建 ViewModel 时必须传入一个 initialState (View 的初始状态)。

- View

一般是继承自 BaseMvRxFragment 的 Fragment，BaseMvRxFragment 实现了 MvRxView 接口，这个接口有一个 invalidate() 方法，每当 State 发生变化时 invalidate() 方法都会被调用。View 的状态是短暂的，需要重写 invalidate() 来不断更新视图，为了更好的优化，可以判断若 State 值与上次相同，则不执行更新操作，或者使用 epoxy (自动 diff)。View 既可以观察整个 State 的变化，也可以只观察 State 中的某个或某几个属性的变化。

- Async

如果数据是来自网络的，需要描述加载过程，这类数据可以用 Async。Async 是一个 sealed class，有四个子类：Uninitialized、Loading、Success、Fail。MvRxViewModel 对 Observable 提供了一个扩展函数 execute，用于将 Observable 转换成 Async，当调用 Observable.execute() 时，它将自动被 subscribe，然后 emit Async 事件。

## Simple Example

```
Kotlin复制代码 //1.抽象数据State
data class HelloWorldState(val title: String = "Hello World") : MvRxState

 //2. 继承 MvRxViewModel，实现 ViewModel
class HelloWorldViewModel(initialState: HelloWorldState) : MvRxViewModel<HelloWorldState>(initialState){
    fun getMoreExcited() = setState { copy(title = "$title!") }
}

 //3. 继承 BaseMvRxFragment，重写 invalidate
class HelloWorldFragment : BaseMvRxFragment() {
    private val viewModel: HelloWorldViewModel by fragmentViewModel()
    private lateinit var marquee: Marquee

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_hello_world, container, false).apply {
 findViewById<Toolbar>(R.id.toolbar).setupWithNavController(findNavController())
            marquee = findViewById(R.id.marquee)
    }

 override fun invalidate() = withState(viewModel) { state->
     marquee.setTitle(state.title)
     marquee.setOnClickListener { viewModel.getMoreExcited() }
     }
}

```

**1、抽象数据 State**

- State 必须是一个 immutable kotlin data class

**2、继承 MvRxViewModel，实现 ViewModel**

两种方式，定义一个 ViewModel：

- ViewModel 不需要额外参数，如 example
- ViewModel 包含有除 initialState 之外的其它构造参数，则需要实现工厂方法

```
Kotlin复制代码// 第一个参数必须是 initialState

class HelloWorldViewModel(
        initialState: HelloWorldState,
        private val dadJokeService: DadJokeService
) : MvRxViewModel<HelloWorldState>(initialState) {

    fun getMoreExcited() = setState { copy(title = "$title!") }
 // 定义一个伴生对象，实现 MvRxViewModelFactory 接口

 companion object : MvRxViewModelFactory<HelloWorldViewModel, HelloWorldState> {
        override fun create(viewModelContext: ViewModelContext, state: HelloWorldState): HelloWorldViewModel {
            val service: DadJokeService by viewModelContext.activity.inject()
            return HelloWorldViewModel(state, service)
        }
    }
}

```

定义好后，MvRx 给 Fragment 提供了三种扩展函数，用于获取：

- by fragmentViewModel()：Fragment 周期的 ViewModel
- by activityViewModel()：Activity 周期的 ViewModel，主要用在不同 Fragment 之间共享 State
- by existViewModel()：Activity 周期的 ViewModel，但必须已经存在，若不存在则会抛出异常。主要用在有前置数据依赖的情况，如多个 Fragment 协作时，依赖前一个 Fragment 设置的数据

同时注意：

- ViewModel 构造函数必须传入一个 initialState
- 只有 ViewModel 能修改 State，修改时应该调用 kotlin 数据类的 copy 方法创建一个新的对象

**3、继承 BaseMvRxFragment，重写 invalidate**

- 通过工厂方法 withState() 得到 State 值，重新绘制视图

### 引入Epoxy

上述例子只使用了 MvRx。接下来我们引入 Epoxy 库，Fragment 需要进行一些改造。（State 和 ViewModel 部分完全一样）

首先，介绍 Epoxy 的两个主要组件：

- EpoxyModel：描述（某个Item）视图长什么样
- EpoxyController：用于控制哪些 EpoxyModel 要展示到 RecyclerView 上，以及展示在什么位置

**1、创建 EpoxyModel**

实际上，EpoxyModel 是框架自动生成的。我们只需要自定义 View，并标上相关注解，Epoxy 就会自动生成一个原类名加上 Model_ 后缀 的类：

```
Kotlin复制代码//1.自定义View，标上注解
@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class Marquee @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val titleView: TextView
    private val subtitleView: TextView

    init {
        inflate(context, R.layout.marquee, this)
        titleView = findViewById(R.id.title)
        subtitleView = findViewById(R.id.subtitle)
        orientation = VERTICAL
 }

    @ModelProp
    fun setImgUrl(imgUrl: String) {
        //show image with you own way
    }

    @TextProp
    fun setTitle(title: CharSequence) {
        titleView.text = title
    }

    @TextProp
    fun setSubtitle(subtitle: CharSequence?) {
         subtitleView.visibility = if (subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
         subtitleView.text = subtitle
    }

    @CallbackProp
    fun setClickListener(clickListener: OnClickListener?) {
        setOnClickListener(clickListener)
    }
}

```

- @ModelView 标注类，autoLayout 描述该 item 加入到 RecyclerView 时的宽高
- @ModelProp 标注方法，只能有一个参数。使用该注解，生成的 EpoxyModel 中将会有相应的字段
- @TextProp 标注方法，参数类型必须为 CharSequence。当字段为字符串时，这个注解会更加方便，生成的 EpoxyModel 会包含若干重载方法，方便直接使用 Android String 资源
- @CallbackProp 标注方法，参数类型是一个回调接口。回调接口不同于普通字段，它们不会影响View 显示，并且需要在 item 滚出屏幕时解绑（设为 null 防止内存泄漏），这些由该注解处理

**2、在 Controller 中使用 EpoxyModel**

上述会生成一个 MarqueeModel_ 类，在 buildModels() 中使用：

```
Kotlin复制代码class HelloWorldEpoxyFragment : BaseFragment() {
    private val viewModel: HelloWorldViewModel by fragmentViewModel()

    //2.创建 epoxyController，重写 buildModels() 方法
    override fun epoxyController() = simpleController(viewModel) { state ->
        //这里配置的 item 会按顺序展示在 RecyclerView 中
        marquee {
            id("marquee")    //一定需要提供id，用于diff。否则会crash
            title(state.title)
            clickListener { _ -> viewModel.getMoreExcited() }
         }
     }
}

```

**3、集成到 RecyclerView**

最后修改 Fragment，在收到 invalidate() 通知时，调用 requestModelBuild() 来重绘界面：

```
Kotlin复制代码 //3.数据更新时，触发重绘
override fun invalidate() = recyclerView.requestModelBuild()

```

可以看出，引入 Epoxy 后，RecyclerView 的开发方式不再是编写 Adapter，而是变成定义一个个 EpoxyModel，界面被拆分成一个个 EpoxyModel 后，元素的复用也变得很简单，整个界面的开发就跟搭积木一样。

# 实现原理

## MvRx

### 定义与获取ViewModel

> 
> 
> 
> by *fragmentViewModel()*
> 

注意到获取 ViewModel 时候，我们只是做了定义，并没有创建 initialState 和 ViewModel，这一步是框架帮我们做的。

按照 MvRx 规范，只能通过框架提供的 activityViewModel() / fragmentViewModel() / existingViewModel() 来获取 ViewModel。通过这几种方式获取 ViewModel，MvRx 会帮我们完成：

1. 返回一个 Lazy 子类，在宿主 onCreate 时触发 ViewModel 的创建
2. 反射构造 initialState 和 ViewModel
3. 调用 ViewModel subscribe() 方法订阅 State 变更，若 State 改变则回调 View 的 invalidate() 方法

下面以 by *fragmentViewModel*() 为例，主要关注如何获取 initialState，并创建 ViewModel 返回的。

创建 ViewModel 的顺序如下：

1. 反射执行 MvRxViewModel 的 Companion 对象的 create() 方法创建
2. 若失败，则反射获取 MvRxViewModel 构造函数创建，要求 ViewModel 只能有一个构造函数，且该构造函数只能有一个 MvRxState 类型的参数（initialState）

创建 initialState 的顺序如下：

1. 反射执行 MvRxViewModel 的 Companion 对象的 initialState() 方法创建
2. 若失败，则反射获取 State 的含参构造函数创建，要求该构造函数只能有一个 Parcel 类型的参数
3. 若失败，则反射获取 State 的默认无参构造函数创建

最后，注意到创建好 ViewModel 后，还用 lifecycleAwareLazy() 包裹了一下：

```
Kotlin复制代码class lifecycleAwareLazy<out T>(private val owner: LifecycleOwner, initializer: () -> T) : Lazy<T>, Serializable {
    private var initializer: (() -> T)? = initializer

    @Volatile
    @SuppressWarnings("Detekt.VariableNaming")
    private var _value: Any? = UninitializedValue
    // final field is required to enable safe publication of constructed instance
    private val lock = this

    init {
        owner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onStart() {
                if (!isInitialized()) value
                owner.lifecycle.removeObserver(this)
            }
        })
    }
 }

```

lifecycleAwareLazy 是一个 Lazy 实现类，这里的 Lazy 并不是指到了用到 ViewModel 时候才创建，从实现中可以看出它监听了宿主的生命周期，在宿主 onCreate() 时就已经将 ViewModel 创建出来了。这样做是为了尽快执行一些初始数据的逻辑，如在 ViewModel init() 中请求网络。即 ViewModel 的 init() 在宿主 onCreate 时就开始执行了。

### 修改数据

> 
> 
> 
> fun setState(reducer: S.() -> S)
> 

> 
> 
> 
> fun Observable.execute(stateReducer: S.(Async) -> S)
> 

提个问题🤔：

1. setState/execute 的参数是为什么是一个 lambda？
2. execute 如何将 Observable 转换成 Async？

在 MvRxViewModel 中，更新 State 的两个主要方法是 setState 和 execute，这两个方法的参数都是一个 lambda（State 上的扩展函数）。execute 是 Observable 的一个扩展函数，在逻辑处理时候经常会用到 Rxjava，execute 能方便的将 Observable 转成 Async 对象。execute 调用的其实也是 setState。

```
Kotlin复制代码//BaseMvRxViewModel.kt

protected fun setState(reducer: S.() -> S) {
    if (debugMode) {
        //...
 } else {
        stateStore.set(reducer)
    }
}

```

可以看出，setState 仅是简单转发调用了 MvRxStateStore 的 set() 方法，接下来这个 lambda block 会被存储到一个队列中等待执行。队列中任务的调度涉及一个双队列的设计。

### 双队列设计

```
Kotlin复制代码//RealMvRxStateStore.kt
class RealMvRxStateStore<S : Any>(initialState: S) : MvRxStateStore<S> {
    // State 存储在 Observable 对象里
    private val subject: BehaviorSubject<S> = BehaviorSubject.createDefault(initialState)

    /**
 * A subject that is used to flush the setState and getState queue. The value emitted on the subject is
 * not used. It is only used as a signal to flush the queues.
 */

 private val flushQueueSubject = BehaviorSubject.create<Unit>()
    private val jobs = Jobs<S>()

    init {
        // 所有 set/get 任务都在同一条后台线程中处理
        flushQueueSubject.observeOn(Schedulers.newThread())
        // We don't want race conditions with setting the state on multiple background threads
        // simultaneously in which two state reducers get the same initial state to reduce.
            .subscribe( { _ -> flushQueues()  } , ::handleError)
        // Ensure that state updates don't get processes after dispose.
            .registerDisposable()

    }

    // 获取 State
 override fun get(block: (S) -> Unit) {
        jobs.enqueueGetStateBlock(block)
        flushQueueSubject.onNext(Unit)
    }

    // 更新 State
 override fun set(stateReducer: S.() -> S) {
        jobs.enqueueSetStateBlock(stateReducer)
        flushQueueSubject.onNext(Unit)
    }

 private class Jobs<S> {
        // 双队列设计，set/get 任务分别存储在两个队列中
        private val getStateQueue = LinkedList<(state: S) -> Unit>()
        private var setStateQueue = LinkedList<S.() -> S>()

        @Synchronized
        fun enqueueGetStateBlock(block: (state: S) -> Unit) {
            getStateQueue.add(block)
        }

        @Synchronized
        fun enqueueSetStateBlock(block: S.() -> S) {
            setStateQueue.add(block)
        }

        @Synchronized
        fun dequeueGetStateBlock(): ((state: S) -> Unit)? {
            return getStateQueue.poll()
        }

        @Synchronized
        fun dequeueAllSetStateBlocks(): List<(S.() -> S)>? {

            // do not allocate empty queue for no-op flushes
            if (setStateQueue.isEmpty()) return null

            val queue = setStateQueue
            setStateQueue = LinkedList()

            return queue
        }
    }

 private tailrec fun flushQueues() {
        // 1.将 set 任务全部执行完
        flushSetStateQueue()

        // 2.执行第一个 get 任务
        val block = jobs.dequeueGetStateBlock() ?: return

        block(state)
        // 3.再次执行（防止 block 中又调用了 set 任务）
        flushQueues()
    }

    private fun flushSetStateQueue() {
        val blocks = jobs.dequeueAllSetStateBlocks() ?: return

        for (block in blocks) {
            val newState = state.block()
            // do not coalesce state change. it's more expected to notify for every state change.
            if (newState != state) {
                // 更新 State，触发变更回调
                subject.onNext(newState)
            }
        }
    }

}

```

可以看出，更新和获取 State 方法传入的参数均是一个 lambda block，这些 block 最后都会运行在一条后台线程中（解决并发修改 State 问题）。但在调度这些 block 的顺序上，MvRx 设计了双队列的任务调度算法，使得 setState block 的优先级高于 getState block。

在双队列中，setStateQueue 有更高的优先级，每次从 getStateQueue 中取任务前需要先将 setStateQueue 中的任务全部执行完。这么设计主要解决一个竞争问题，即 getState block 中又调用了 setState block。

考虑这样一段代码：

```
Kotlin复制代码getState { state ->

     if (state.isLoading) return
     setState { state ->
     state.copy(isLoading = true)
     }

 // make a network call
 }

```

如果连续执行两次，可能会发出两次网络请求，但却是不符合本意的。这里原意是想：第一次执行判断状态为非 Loading，将值设为 Loading，接着请求网络；第二次执行判断已经 Loading 了就 return。

为了便于分析，把这两次调用简化如下：

```
Kotlin复制代码getStateA {
    setStateA {}
}

getStateB {
    setStateB {}
}

```

如果只有一个队列，那么 block 的执行顺序和插入顺序一致：

*getStateA -> getStateB -> setStateA -> setStateB（不符合原意）*

但如果是双队列的设计：

1. 两个 getState block 都插入队列后
- setStateQueue: []
- getStateQueue: [A, B]
1. 第一个 getState block 执行完后
- setStateQueue: [A]
- getStateQueue: [B]
1. 因为 setStateQueue 优先级更高，先执行 setStateA
- setStateQueue: []
- getStateQueue: [B]
1. 最后执行 setStateB
- setStateQueue: []
- getStateQueue: []

最终 block 的执行顺序会是：

*getStateA->setStateA->getStateB ->setStateB（符合原意）*

### 渲染回调

> 
> 
> 
> fun invalidate()
> 

从上小节可知，State 并不是简单以成员变量方式存放的，State 存在 BehaviorSubject 对象中。BehaviorSubject 继承自 Subject，Subject 同时实现了 Observable 和 Observer 接口，意味着 Subject 即可以是观察者，也可以是被观察者。在 MvRxStateStore 中，State 值一旦更新，就会触发观察者收到变更通知。

而注册观察者的地方，就在最初获取 ViewModel 时候：

```
Kotlin复制代码inline fun <T, reified VM : BaseMvRxViewModel<S>, reified S : MvRxState> T.fragmentViewModel(
    viewModelClass: KClass<VM> = VM::class,
    crossinline keyFactory: () -> String = { viewModelClass.java.name }
) where T : Fragment, T : MvRxView = lifecycleAwareLazy(this) {

 MvRxViewModelProvider.get(
        viewModelClass.java,
        S::class.java,
        FragmentViewModelContext(this.requireActivity(), _fragmentArgsProvider(), this),
        keyFactory()
    ).apply {

        // 在创建 ViewModel 后，直接向其 State Observable 注册了变更监听
        subscribe(this@fragmentViewModel, subscriber = { postInvalidate() } ) }

 }

```

在创建 ViewModel 后，顺带就直接向这个 BehaviorSubject 注册了观察者，一旦 State 变化就回调 MvRxView. postInvalidate() 方法，最终触发 invalidate() 重新渲染视图。

提个问题🤔：

在向 LiveData 注册监听时是能自动感知 LifeCycleOwner 生命周期的，如果宿主已经不再处于活跃状态了，则观察者不会收到变更通知。这里注册的是 Observer，是否会感知到生命周期？若是则是怎样实现的呢？

### 小结

上述过程时序图：

1. 当 Fragment onCreate() 时，会创建出 MvRxViewModel 实例
2. 当 ViewModel 读取或修改数据时，这些操作被放到了双队列中，在一条后台线程中串行处理。双队列中写操作比读操作优先级更高，在执行读操作前总是得先把所有写操作执行完
3. State 是一个可观察对象，ViewModel 观察着 State，每当 State 变化就会通知 ViewModel，ViewModel 回调 Fragment invalidate() ，触发视图重新刷新

## Epoxy

> 
> 
> 
> *环氧树脂（Epoxy），又称作人工树脂、人造树脂、树脂胶等。是一类非常重要的热固性塑料，广泛用于黏着剂，涂料等用途。（引申义：粘合数据 State 和视图 RecyclerView）*
> 

### 构建Model

> 
> 
> 
> requestModelBuild()
> 

当数据发生变化，需要通过调用 EpoxyController 的 requestModelBuild() 方法来更新界面：

```
Kotlin复制代码public abstract class EpoxyController {
    //...

    private final Runnable buildModelsRunnable = new Runnable() {
      @Override
      public void run() {
        // Do this first to mark the controller as being in the model building process.
        threadBuildingModels = Thread.currentThread();

        // This is needed to reset the requestedModelBuildType back to NONE.
        // As soon as we do this another model build can be posted.
        cancelPendingModelBuild();

        helper.resetAutoModels();
        modelsBeingBuilt = new ControllerModelList(getExpectedModelCount());
        timer.start("Models built");

        try {

          //抽象方法，在实现自己的 EpoxyController 时需要重写该方法来描述自己的界面

          //1.build models
          buildModels();
        } catch (Throwable throwable) {

          timer.stop();
          modelsBeingBuilt = null;
          hasBuiltModelsEver = true;
          threadBuildingModels = null;
          stagedModel = null;

          throw throwable;
        }

        addCurrentlyStagedModelIfExists();

        timer.stop();

        //2.拦截器 interceptor
        runInterceptors();

        filterDuplicatesIfNeeded(modelsBeingBuilt);
        modelsBeingBuilt.freeze();

        timer.start("Models diffed");

        //3.diff models
        adapter.setModels(modelsBeingBuilt);
        // This timing is only right if diffing and model building are on the same thread
        timer.stop();

        modelsBeingBuilt = null;
        hasBuiltModelsEver = true;
        threadBuildingModels = null;
      }
    };

    //请求立即刷新
    public void requestModelBuild() {
      if (isBuildingModels()) {
        throw new IllegalEpoxyUsage("Cannot call `requestModelBuild` from inside `buildModels`");
      }

      if (hasBuiltModelsEver) {
        requestDelayedModelBuild(0);
      } else {
        buildModelsRunnable.run();
      }
    }
}

```

可见，requestModelBuild() 方法通过 modelBuildHandler 将一个 buildModelsRunnable 任务 post 到队列中串行处理。该任务会调用 buildModels() 方法（创建 EpoxyController 时我们重写的），走到我们描述界面的代码中去，构建好所有的 Models。

modelBuildHandler 是在构造函数中传进来的，默认用的是 mainLooper，即每次 buildModels() 默认都是被抛到主线程的消息队列中，串行处理的。若要在子线程处理，可以用 AsyncEpoxyController。

注意到这里调用了 requestDelayedModelBuild，和限频更新有关。

### debounce限频更新

用户有时会连续快速触发多次数据更新，这时最佳的是直接使用最后一次的数据来构建 Model，每次都重新构建 Model 不是很必要而且影响性能。这里为了让开发者能随时调用 requestModelBuild() 而不关心这些细节，Epoxy 用了一个标记位来跟踪是否已经 post 了 buildModel 任务。

```
Kotlin复制代码    //请求延迟刷新
    //调用时，若上一次请求还未执行，则上一次请求会被取消
    public synchronized void requestDelayedModelBuild(int delayMs) {

      if (isBuildingModels()) {
        throw new IllegalEpoxyUsage(
            "Cannot call `requestDelayedModelBuild` from inside `buildModels`");
      }

      if (requestedModelBuildType == RequestedModelBuildType.DELAYED) {
        cancelPendingModelBuild(); //取消上一次延迟请求
      } else if (requestedModelBuildType == RequestedModelBuildType.NEXT_FRAME) {
        return; //请求正在执行，放弃此次调用
      }

      requestedModelBuildType =
          delayMs == 0 ? RequestedModelBuildType.NEXT_FRAME : RequestedModelBuildType.DELAYED;
      modelBuildHandler.postDelayed(buildModelsRunnable, delayMs);
    }

```

标记位 requestedModelBuildType：

```
Kotlin复制代码@Retention(RetentionPolicy.SOURCE)

@IntDef({RequestedModelBuildType.NONE,
         RequestedModelBuildType.NEXT_FRAME,
         RequestedModelBuildType.DELAYED})

private @interface RequestedModelBuildType {
    int NONE = 0;        //当前还没有请求
    int NEXT_FRAME = 1;  //当前有请求正在执行
    int DELAYED = 2;     //当前有请求，但在延时等待执行
}

```

限频更新策略如下：

- 若调用的是延时刷新 (delayMs>0)，在调用时若上一次延迟请求尚未执行，则取消上一次的请求，将此次请求加入到延迟队列中等待执行。
- 若调用的是立即更新 (delayMs=0)，在调用时若在有请求正在执行，则放弃此次调用。

以立即更新为例：

假设用户连续触发了如下刷新 (每次刷新 i+1)：

*requestModelBuildA-> requestModelBuildB-> requestModelBuildC -> requestModelBuildD*

在执行第一个 requestModelBuildA 时，首先会将 requestedModelBuildType 标记置为 *NEXT_FRAME，* 接着 post 一个更新任务。接下来 B 遇到这个 *NEXT_FRAME* 标记就直接 return 了，直到 buildModelA **执行时候又会将这个标记位设置为 *NONE，* C post 后，同理 D 又被拦截掉了。

最终可能只执行了两次：

*buildModelA -> buildModelC*

每次执行时，会取当前 State 的最新值，最终得到 i=4。

### diff

从上面知，buildModels() 构建完后，通过 adapter.setModels 通知数据更新： diff 用的还是 RecyclerView DiffUtil 算法，通过 id() 来比较两个 EpoxyModel 是否同一个 item，通过 equals() 来比较内容是否一致。

这里，EpoxyModel 是 Epoxy 为每个加了 @ModelView 注解的自定义 View 自动生成的。这个生成类重写了 equals() 和 hashCode() 方法，根据所有字段的值计算出一个结果来标识 View 的状态，也即只要有字段的值发生变化，View 的状态就变了，这一项就需要重新 update。

### 绑定数据

计算出差量数据集后，RecyclerView Adapter 会重新走流程：

onBindViewHolder() 调用 EpoxyModel 的 bind() 方法将数据设置到 View 上：

### 小结

上述过程时序图：

1. 通过 requestModelBuild() 来触发 RecyclerView 更新，重复的更新任务会被舍弃
2. 首先，执行 buildModels() 方法构建 EpoxyModel 列表，该方法在独立线程中串行处理，通过重写该方法来配置 RecyclerView 要展示的所有项，构建完后 model 不能再被修改
3. 接着，遍历这个 EpoxyModel 列表，diff 比较 model 的状态，该方法在也是在独立线程中串行处理。EpoxyModel 的状态取决于 equals() 和 hashCode() 方法，每个生成的 EpoxyModel 都重写了这两个方法，它们是基于 EpoxyModel 所有字段的值计算得到的
4. 最后，diff 后的结果回到主线程，设置到 RecyclerView 中，onBindViewHolder() 回调时会调用 EpoxyModel bind() 方法将数据绑定到 View 上

# 小结

## React是怎样构建应用的

MvRx 依赖了 React 的概念，我们也看看 React 是如何构建应用的：

- 第一步：将设计稿给出的 UI 划分为组件层级
- 第二步：创建一个静态页面
- 第三步：确定 State 的最小且完整表示
- 第四步：确定哪个组件来拥有 State
- 第五步：添加反向数据流（回调）

其中一些细节如下：

**1、关于组件**

- 根据单一功能原则来划定组件，一个组件只负责一个功能
- 每个组件需要与数据模型的某部分匹配。 因为 UI 结构和数据模型会倾向于遵守相同的信息结构
- 编写组件时，建议将渲染 UI 和添加交互这两个过程分开。因为编写一个应用的静态页面时，往往要编写大量代码，而不用考虑太多交互细节；添加交互功能时则要考虑大量细节，而不需要编写太多代码

**2、关于数据（props 和 state）**

- props 和 state 都是用来保存信息的，都可以控制组件的展示，不同的是：props 是父组件向子组件传递数据的方式，而 state 是在组件内自己管理的。state 具有触发数据模型改变的能力，从这个角度，可以将组件分为控制组件和展示组件，控制组件拥有 state，并组合其他展示组件
- 只保留应用所需的可变 state 的最小集合，其他数据均由它计算产生
- React 中的数据流是单向的，并顺着组件层级从上往下传递
- 要确定哪个组件拥有 state，可以找根据这个 state 进行渲染的所有组件的共同父组件，如果找不到可以直接创建一个新的组件并置于所有组件之上（状态提升），由这个共同父组件拥有 state
- 处于较低层级的组件要更新较高层级组件中的 state，需要通过添加反向数据流（回调）

**3、组合和继承**

- React 中没有用到继承来构建组件的情况，通过 props 和组合已经能够灵活的定制组件：组件接受任意 props，props 可以是各种类型（基本数据类型/React 元素/函数）。想要在组件间复用非 UI 的功能，React 建议将其提取为一个单独的 JavaScript 模块，组件可以直接引入（import）而无需通过 extend 继承它们

上面这些内容也可以应用到原生开发：

1. 拿到设计稿后先对页面做大致的拆分，将 UI 划分成一系列层级的组件
2. 确定一些（功能）最小且完整的组件
3. 将该最小完整组件的顶层 View 作为控制 View，拥有 state，同时包含一定的和逻辑层的交互逻辑。其下的 View 作为展示 View，一般只拥有一些属性和 setter/getter 方法，属性值由父 View 计算好后从上往下传递，事件则通过回调方式回到控制 View 中
4. 需要复用的代码可以提取成一个单独的类或模块

# Reference

- [*github.com/airbnb/mave…*](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fairbnb%2Fmavericks)
- [*github.com/airbnb/epox…*](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fairbnb%2Fepoxy)
- [*zh-hans.reactjs.org/docs/thinki…*](https://link.juejin.cn/?target=https%3A%2F%2Fzh-hans.reactjs.org%2Fdocs%2Fthinking-in-react.html)

我们是字节深圳飞书移动端团队，致力于打造全球最领先的文档类创作和内容管理工具，目前深圳职位火热招聘中，感兴趣的童鞋可以投递简历到[[zenghao.howie@bytedance.com](https://link.juejin.cn/?target=mailto%3Azenghao.howie%40bytedance.com)]

iOS 职位：[job.toutiao.com/s/Nx12FCw](https://link.juejin.cn/?target=https%3A%2F%2Fjob.toutiao.com%2Fs%2FNx12FCw)

Android 职位：[job.toutiao.com/s/Nx1MJAA](https://link.juejin.cn/?target=https%3A%2F%2Fjob.toutiao.com%2Fs%2FNx1MJAA)

前端职位：[job.toutiao.com/s/N9ofyju](https://link.juejin.cn/?target=https%3A%2F%2Fjob.toutiao.com%2Fs%2FN9ofyju)

后端职位：[job.toutiao.com/s/Nx1DXTR](https://link.juejin.cn/?target=https%3A%2F%2Fjob.toutiao.com%2Fs%2FNx1DXTR)

公司福利有：

免费三餐，保你长胖;每月房补1500元;苹果MacBook Pro;全额五险一金，并额外购买商业保险;免费健身房+年度体检; 每年6天-15天年假，8天带薪病假