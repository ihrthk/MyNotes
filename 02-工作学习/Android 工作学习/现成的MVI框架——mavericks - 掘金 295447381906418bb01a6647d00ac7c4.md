# 现成的MVI框架——mavericks - 掘金

[https://juejin.cn/post/7144668578845818910](https://juejin.cn/post/7144668578845818910)

## 前言

Android架构演变：MVC、MVP、MVVM、MVI。

新技术层出不穷，架构一天一个样。像我这种懒惰又不自律的人，能学习上最新的技术，就已经佩服我自己了。

**所以造轮子是不可能造轮子的，这辈子都不可能造轮子！**

于是乎我就在github上审阅各种轮子，默默的star，以备不时之需。这就是我和mavericks的缘分。

废话已毕，下面我们开启正文。

## 文章提纲

- mavericks是什么？
- mavericks怎么用？
- mavericks配合其他库使用

## mavericks是什么？

mavericks是[Airbnb](https://link.juejin.cn/?target=https%3A%2F%2Fwww.airbnb.com%2F)技术团队开源的符合MVI架构的技术方案。[Github地址](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fairbnb%2Fmavericks)

据我了解[Airbnb](https://link.juejin.cn/?target=https%3A%2F%2Fwww.airbnb.com%2F)这家公司还是很牛叉的，所以这种公司开源的东西还是值得信赖。而且mavericks官方文档中也说了，他们公司很多产品都在用这套方案，所以这并不是KPI项目，可靠性还是有保证的。

至于什么是`MVI` ，我相信各位大佬应该都看了很多文章了，这里不再赘述。

## mavericks怎么用？

现在我们来说正事，关于`MVI` 架构该怎么落地，网上很多大佬都给出了自己的解决方案。

在我看来，不管是什么架构，无非是为了追求两个目标：①易用；②易维护。易用就包括分层清晰，少写模板代码等等；易维护就包括迭代影响最小，多人协同不易出错，扩展性强等等。

mavericks能满足这些条件吗？我们且往下看。

### 配置

第一步：

```
groovy复制代码implementation 'com.airbnb.android:mavericks:2.7.0'

```

第二步：

在Application中初始化。

```
kotlin复制代码Mavericks.initialize(this)

```

### 简单的计数器

学习语言，我们先输出Hello World，学习架构我们就先写个计数器，这好像成了某种约定俗称。傻笑～～

```
kotlin复制代码//驱动UI的数据
data class CounterState(val count: Int = 0) : MavericksState

//等效官方的ViewModel
class CounterViewModel(initialState: CounterState) : MavericksViewModel<CounterState>(initialState) {
    fun incrementCount() = setState { copy(count = count + 1) }
}

//UI层
class CounterFragment : Fragment(R.layout.counter_fragment), MavericksView {
    private val viewModel: CounterViewModel by fragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        counterText.setOnClickListener {
            viewModel.incrementCount()
        }
    }
    override fun invalidate() = withState(viewModel) { state ->
        counterText.text = "Count: ${state.count}"
    }
}

```

```
xml复制代码<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <TextView
        android:id="@+id/counterText"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:textSize="48dp" />
</FrameLayout>

```

代码很简单，其中出现了三个关键词：MavericksState、MavericksViewModel、MavericksView。

MavericksState必须遵循以下原则：

- **必须是data class**。因为data class默认实现了equals、hashCode等函数，方便框架计算两次更新是否是同一数据，如果两次State是完全一样的，也没有必要通知UI刷新一次；
- **属性必须是val，且必须有初始值**。一个瞬时就只有一份不可变的数据。不会出现不可预期的情况，不会有完全隐患。Flutter、Compose等都有这样的设计理念。
- **字段类型必须是不可变**。如ArrayList等导致可变更的都不行。
- **必须实现MavericksState接口**。MavericksState只是一个标识接口。

MavericksViewModel和官方ViewModel功能是一样的。只需要额外传入一个初始化的state，这个后面会讲。

MavericksView是用来标记UI层的。目前我的使用经验，MavericksView是不支持标记Activity的，只支持标记Fragment。而且不管是Google 官方，还是mavericks的技术人员都推荐我们尽量不把Activity作为UI层。如果非要mavericks支持activity，只能自己实现扩展。这是官方技术人员的原话：

> 
> 
> 
> activityViewModel() is designed to get an activity scoped view model from a Fragment. You want just `by viewModel()` [github.com/airbnb/mave…](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fairbnb%2Fmavericks%2Fblob%2Fmain%2Fmvrx%2Fsrc%2Fmain%2Fkotlin%2Fcom%2Fairbnb%2Fmvrx%2FMavericksExtensions.kt%23L211) However, I recommend using Fragments. Putting UI directly inside of an Activity is no longer recommended in general for Android.
> 

说回MavericksView，其中有个invalidate()方法我们必须实现。每次数据更新，该方法都会被回调。可以在该方法中把数据和UI进行绑定。

虽然我上面啰里吧嗦说了很多，但是代码就那么一点。不知道各位大佬觉得和Flutter的计数器比，哪个更简单呢？

### 真正使用

学习一个新框架我们不可能只拿来写个计数器。比如异步数据，异常处理等等。所以我们接着往下看：

- 初始化ViewModel
- `setState { copy(yourProp = newValue) }`
- `withState()`
- `onEach()` 和 `onAsync()` 局部更新
- `Async<T>` 和 `execute(...)` 处理异步事务
- 派生字段
- 参数传递

### 初始化ViewModel

虽然我们前面说了MavericksViewModel和ViewModel等效，但他们并没有直接关系。那我们在初始化的时候就不能再使用官方的方式。而是通过 `activityViewModel()`、`fragmentViewModel()`、`parentFragmentViewModel()`、`existingViewModel()`和 `navGraphViewModel(navGraphId: Int)` 等代理获取 `MavericksViewModel` 。

从名字上我们可以看到不同作用域的方法都有，已经能满足绝大多数需求。如果不能满足的，也可以自行扩展。

### setState()

setState完整长这样，可以看到android studio自动给我们提示的this:CounterState。

这里的this就是最后一次更新的State。所以我们copy旧的State然后把count字段+1变成一个新的State。而setState的作用就是把新的State发送出去，最终回调MavericksView层中的invalidate()方法。是不是很容易理解？

### withState()

我们可以看到invalidate()方法中是没有参数的，那我们该怎么拿到ViewModel那边发送过来的State的呢？这个时候就需要用到withState了。该方法是主动获取当前最新的State。

在MVC一把梭的架构中，我们请求个网络数据，返回的对象我们通常赋值给一个成员变量，以供在其他地方使用；在MVP架构中，通常会在P层提供get方法主动获取，或者通过接口被动接收。

而在mavericks中，withState就可以搞定数据获取问题。所以在invalidate()被回调后，我们主动获取到最新的State。就像下面这样it:CounterState。

**注意：在MavericksView层使用withState可以同步获取到最新的State，但是在ViewModel里面使用withState并不一定获取到最新的State。**

我们看个例子

```
kotlin复制代码fun setAndRetrieveState() {
  println('A')
  setState {
    println('B')
    copy(count = 1)
  }
  println('C')
  withState { state ->
    println('D')
  }
  println('E')
}
//打印结果：ACEBD

```

这是因为setState并不是在主线程上立即更新。而是把更新的State存放到队列里面，依次发送的。并且这个操作是在其他线程完成的，所以我们才能看到上面的打印顺序。

在UI层使用withState，我们完全不需要担心是否是主线程的问题，框架会自动帮我们处理。

### onEach()和onAsync()

从上面我们不难看出State其实描述的就是UI的状态。那么越是复杂的UI或者越是复杂的业务，这个State中的字段肯定就越多。而这其中的任何一个字段值的变更都会触发invalidate()的回调。如果我们把所有数据绑定，控件更新的代码都写在invalidate()中，其中任何一个字段发生改变，都会触发和本字段无关的控件刷新，这肯定不是我们想要的结果。所以onEach()和onAsync()就有用武之地了。

onEach()可以单独监听State中的某一个属性或者某几个属性。这样只有被监听的几个字段有值的变化，回调才会执行。

在Fragment中，一般在onCreate()中监听，在ViewModel中，一般在init{}中监听。

onAsync()也是单独监听的，从名字我们可以看出是监听异步逻辑的，下面我们再细说。

### Async和execute()

```
kotlin复制代码sealed class Async<out T>(private val value: T?) {

    open operator fun invoke(): T? = value

    object Uninitialized : Async<Nothing>(value = null)

    data class Loading<out T>(private val value: T? = null) : Async<T>(value = value)

    data class Success<out T>(private val value: T) : Async<T>(value = value) {
        override operator fun invoke(): T = value
    }

    data class Fail<out T>(val error: Throwable, private val value: T? = null) : Async<T>(value = value)
}

```

上面就是Async的源码了，没啥稀奇的，就是个密封类，我相信在座的各位大佬在使用kotlin之后就已经用的滚瓜烂熟了。

State中netData是Async类型的，默认Uninitialized。而网络请求我直接用retrofit请求了百度首页的内容。

而execute{}是框架封装的一个扩展函数，从this:CounterState可以看出，这和setState{}是一样的参数，所以这里可以直接copy，同时自动把最新State发送出去。

我们还可以看到判断了oldState是否是Loading，这有效防止过度请求，和无效请求。

异步数据如何监听呢？我们既可以在invalidate()统一处理（不推荐，因为需要判断状态，是Loading，还是Success，还是Fail），也可以通过onAsync{}来单独监听，如下

其中的onFail和onSuccess显而易见是干嘛的，不多说。现在我们再来学习一个高能技能点。

**高能技能点1**

我们可以看到异步处理，比如网络请求，不管是成功还是失败，都是通过Async这个字段来更新状态的。那就存在一个问题，比如一个可以刷新，可以加载更多的列表。第一次请求数据正常，回调到onSuccess正常显示，但我刷新一次，这次可能就失败了，然后onFail回调把之前的onSuccess覆盖了。但是这在UI上的效果就是，我第二次一刷新，整个页面的数据都没有了，只有一个错误在那里。这种体验肯定不好，老板也不会让我们这么干。

正常的逻辑应该是：不管后面是刷新还是加载更多，如果出错了，第一次的正常数据都应该保留。

而mavericks针对这种情况也给了很好的方案，如下：

只需要在execute()中把retainValue赋值为State中的异步字段，框架就会自动处理上面这种情况。上次正常数据任然保留，同时本次的异常信息也照常分发。

**高能技能点2**

在onEach()和onAsync()中还有一个参数deliveryMode，如下：

这个参数框架提供了两个实现，RedeliverOnStart和UniqueOnly。默认为RedeliverOnStart。

从框架的注释看，RedeliverOnStart不管是锁定状态（处于生命周期onStop）的时候State的值有没有变更，再次监听（订阅）的时候，都会发送一次最新的值。

我举个例子说明这个情况，比如主页面有个更新弹窗，State中有个字段showUpdateDialog字段来控制这个更新弹窗是否显示，为true就显示，为false就不显示。那么我们进入主页面如果有更新，showUpdateDialog就会被setState{copy(showUpdateDialog=true)}，这个时候弹窗就显示出来了。如果我们不更新，而是把弹窗手动关闭，然后点击任意二级页面，然后再回来主页面，这个时候弹窗又会弹出来。这是因为State中的showUpdateDialog字段还是true。UI和State重新绑定的时候会收到一次通知，即使在我去往二级页面过程中，State没有变化过。

为了解决这个问题，我们有两个方案：①在关闭弹窗的时候，手动把State中的showUpdateDialog字段置为false；②将deliveryMode设置为UniqueOnly。

而UniqueOnly的效果是，在锁定状态（处于生命周期onStop）State有变化，重新监听（订阅）后才会发送一次最新的值，否则不发送。

这就能很好处理比如Toast，Snack，Dialog等等只需要进入页面提示一次就可以了，而再次激活页面不需要提示的情况。

**高能技能点3**

其实这个也不算什么高能，很多资深Jetpack玩家早就烂熟于心了。代码如下：

```
kotlin复制代码viewModel.onAsync(CounterState::netData, onFail = {
      Timber.e(it)
}, onSuccess = {
      lifecycleScope.launch {
           repeatOnLifecycle(Lifecycle.State.STARTED){
                binding.tvNetworkContent.text = it.string()
           }
      }
})

```

毕竟是异步操作，网络数据回来，页面被销毁了，这就挺尴尬的。所以为了安全，该写的代码都给加上。

### 派生字段

既然State是data class类型的。那么data class所有的特性都可以使用。而这里说道的派生字段更是在复杂业务中，官方推荐使用的。

比如这里的isError字段就是一个派生字段。派生字段必须是val，其关联的字段只能是构造函数中的val字段，否则会报错。

在复杂业务中，一些UI的状态可能不单单是某个字段解决定的，更多时候是几个字段联合决定的。而联合的这几个字段甚至是不同接口返回的数据。如果我们需要监听多个接口的返回结果，处理起来是比较复杂的，所以派生字段的好处就是，UI层只需要关心派生字段的更新，而不需要关心这个字段具体是如何计算得来的。

### 参数传递

既然要遵循`MVI`中的数据源唯一的要求。那我我们从上个页面带过来的id，比如商品详情页的会从上个页面带过来商品id，视频详情页会从上个页面带过来视频id等等。

那么这个id，我们总不能等ViewModel初始化好了之后，手动传递过去吧？这看起来不太聪明的样子。而且这个id如果只是保存在UI这一层，我们还得考虑onSaveInstanceState中保存，重建的时候恢复等等操作。ViewModel本来就有保存状态的特性，我们何不把id放到ViewModel中去，这样业务也更加内聚。那针对mavericks，我们该如何操作呢？

首先从其他页面传递过来的参数我们该如何搞？

```
kotlin复制代码//需要传递的参数必须是Parcelable或者Serializable
//所以满足条件的其他封装类型，比如String,ArrayList等等都是可以的，没必要非得封装成下面这个样子
@Parcelize
data class ProjectArgs(val id: Int) : Parcelable

//传递参数 mavericks提供了asMavericksArgs()这个扩展函数
//这和官方提供的一些扩展函数没啥不同，自己封装都可以
 fun instance(): ProjectFragment {
     val fragment = ProjectFragment()
     fragment.arguments = ProjectArgs(id = 20).asMavericksArgs()
     return fragment
 }
//获取参数
private val args by args<ProjectArgs>()

```

上面的注释已经写的很清楚了，不再多说。

如果把这个参数优雅的传递到ViewModel那边去呢？

我们再来看一下这张图，其中还重载了一个构造函数。然后多了一个参数ProjectArgs，然后在this()中直接赋值给了State中的id，并且id最开始是没有初始值的。

就这样简单的一句代码，我们就把Fragment中的id传递到了State中。而我们在ViewModel中需要使用，直接withState就能获取到。是不是很方便？

这张图中还有个特别的注解@PersistState。那我们就来学学他是干什么用的。

**高能技能点4**

我们都知道在Activity或者Fragment中，一些关键数据，可以通过onSaveInstanceState()存储。

而mavericks除了上面的老办法，还提供了一个@PersistState给我们来更加优雅的做这件事。凡是被注解的字段，在低内存的时候，都会起到onSaveInstanceState()同等的效果，把该字段存储到本地磁盘。等恢复的时候，再取出本地的数据重新赋值。

同样，onSaveInstanceState()能存储哪些类型，我们这里就支持存储哪些类型。

## mavericks配合其他库使用

### 和Jetpack Navigation一起使用

需要引入额外的库

```
groovy复制代码implementation 'com.airbnb.android:mavericks-navigation:2.7.0'

```

初始化的时候有点小区别，如下：

```
kotlin复制代码Mavericks.initialize(this, viewModelDelegateFactory = DefaultNavigationViewModelDelegateFactory())

```

### 和Hilt一起使用

```
kotlin复制代码data class ExampleState(
    val data: String = "",
) : MavericksState

class ExampleViewModel @AssistedInject constructor(
    @Assisted initialState: ExampleState,
    private val exampleRepository: ExampleRepository,
) : MavericksViewModel<ExampleState>(initialState) {

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<ExampleViewModel, ExampleState> {
        override fun create(state: ExampleState): ExampleViewModel
    }

    companion object : MavericksViewModelFactory<ExampleViewModel, ExampleState> by hiltMavericksViewModelFactory()
}

@Module
@InstallIn(MavericksViewModelComponent::class)
interface ExampleViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(ExampleViewModel::class)
    fun exampleViewModelFactory(factory: ExampleViewModel.Factory): AssistedViewModelFactory<*, *>
}

```

需要注意:

- 注解@AssistedInject和Hilt不同
- initialState前面也有注解
- 下方的一串模板代码，可以在android studio中定制模板，打几个字母就搞定了
- ViewModule绑定到Components上去，注意是MavericksViewModelComponent::class，固定写法。

### 和Koin一起使用

我就觉得Hilt挺繁琐的，所以我使用Koin。

```
kotlin复制代码data class ExampleState(
    val data: String = "",
) : MavericksState

class ExampleViewModel (
    initialState: ExampleState,
    private val exampleRepository: ExampleRepository,
) : MavericksViewModel<ExampleState>(initialState) {

    companion object : MavericksViewModelFactory<ProjectViewModel, CounterState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: CounterState
        ): ProjectViewModel {
            val api: AppApi by viewModelContext.activity.inject()
            return ProjectViewModel(state, api)
        }
    }
}

```

下方还是一个模板代码，但是只有写这一处就搞定了。

## 后话

mavericks我只用来写过一个较小型的商业项目，很多复杂业务还没有遇到过。如果小伙伴在使用过程中有问题，或者有好的想法，可以留言，我们一起交流交流！

另外该团队开源的另外一个神器[Epoxy](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fairbnb%2Fepoxy)，也是用了就扔不掉，大家想看我的使用经验的话可以给我留言。