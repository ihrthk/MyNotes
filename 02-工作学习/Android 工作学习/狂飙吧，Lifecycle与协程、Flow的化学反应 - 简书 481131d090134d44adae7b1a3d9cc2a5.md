# 狂飙吧，Lifecycle与协程、Flow的化学反应 - 简书

[https://www.jianshu.com/p/a8520cbdc3d8](https://www.jianshu.com/p/a8520cbdc3d8)

# 前言

原本上篇已经结束协程系列了，后面有小伙伴建议可以再讲讲实际的使用，感觉停不下来了，再用几篇收尾吧。我们知道Android开发绕不开的一个重要课题即是生命周期 ，引入了协程后两者该怎么配合呢？

通过本篇文章，你将了解到：

> 
> 
> 1. 生命周期的前世今生
> 2. Activity与协程的结合
> 3. ViewModel与协程的配合
> 4. Application创建全局的协程作用域
> 5. Flow、协程、生命周期的三角关系

# 1. 生命周期的前世今生

## 生命周期简述

现在的系统设计更聚焦于UI和数据的分离，当前的UI展示需要哪些数据的支持，在什么时候需要展示这些数据，这些都需要开发者自己去控制。若控制不得当，可能会出现内存泄漏、资源浪费等现象。

Android提供了四大组件，其中Activity是用来展示UI的，它的创建到销毁即是它的一个完整生命周期，四大组件中我们比较关注Activity和Service的生命周期，尤其是Activity是重中之重，而Fragment的生命周期依赖于Activity，因此只要弄懂了Activity的生命周期，其它不在话下。

## Activity 生命周期关注点

### Activity内存泄漏

以典型的后台获取数据，Toast到UI上为例：

```
        binding.btnStartLifecycle.setOnClickListener {
            thread {
                //模拟网络获取数据
                Thread.sleep(5000)
                runOnUiThread {
                    //线程持有Activity实例
                    Toast.makeText(this@ThirdActivity, "hello world", Toast.LENGTH_SHORT).show()
                }
            }
        }

```

后台开启线程，模拟网络请求，等待5s后弹出Toast。

正常场景下没问题，若此时还未弹出Toast就退出Activity，会发生什么呢？

显而易见，当然会**内存泄漏**，因为Activity实例被线程持有，无法回收，Activity泄漏了。

### 资源浪费

以后台获取数据，展示到Activity上为例：

```
        binding.btnStartGetInfo.setOnClickListener {
            thread {
                //模拟获取数据
                var count = 0
                while (true) {
                    Thread.sleep(2000)
                    runOnUiThread {
                        binding.count.text = "计算值:${count++}"
                        println("${binding.count.text}")
                    }
                }
            }
        }

```

后台开启线程，模拟网络请求，等待5s后更新TextView。

正常场景下没问题，若此时回到桌面或是切换到其它App，我们是不需要更新UI，也就不需要获取网络数据，此种情况下就会存在资源浪费，应当避免这种写法。

存在以上两种现象是因为在实现功能的过程中没有注意Activity的生命周期，简而言之，我们关注Activity生命周期就是为了解决两类问题：

image.png

解决方法也很简单，不管是Activity退出还是回到后台都会有各个阶段生命周期的回调。因此，只要监听了Activity周期，在对应的地方进行防护就可以解决上述问题。

详情请移步：[Android Activity 生命周期详解及监听](https://www.jianshu.com/p/233a908361fd)

# 2. Activity与协程的结合

## 没有关联生命周期的协程的使用

先看Demo：

```
        val scope = CoroutineScope(Job())
        binding.btnStartUnlifecyleCoroutine.setOnClickListener {
            scope.launch {
                delay(5000)
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(this@ThirdActivity, "协程还在运行中", Toast.LENGTH_SHORT).show()
                }
            }
        }

```

如上，构造了协程作用域，通过它启动协程，5s后在后台打印。

当点击该按钮后，我们退出Activity，最后发现Toast还会出现，说明发生了泄漏。

## 关联生命周期的协程的使用

### 解决泄漏

协程的出现简化了我们的编程结构，然而只要和Activity产生瓜葛都避免不了要关注它的生命周期。

还好，协程内部主动关联了生命周期，不用开发者去手动处理，来看看怎么使用的。

```
        binding.btnStartWithlifecyleCoroutine.setOnClickListener {
            lifecycleScope.launch {
                delay(5000)
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@ThirdActivity, "协程还在运行中", Toast.LENGTH_SHORT).show()
                }
                //假设有网络请求
                println("协程还在运行中")
            }
        }

```

与上个demo不同的是协程作用域的选择，这次用的是lifecycleScope，它是LifecycleOwner的扩展属性。

点击按钮后，退出Activity，此时看不到Toast，也看不到打印，说明协程作用域检测到Activity退出后将自己销毁了，也就不会引用Activity实例，当然就解决了内存泄漏问题。

### 避免资源浪费

细心的你可能发现了：若此时点击按钮后回到桌面，发现打印还在继续，实际上为了节约资源我们不想让它们继续运行，怎么办呢？

当然，协程也考虑了这种场景，提供了几个便利的函数。

```
        binding.btnStartPauseLifecyleCoroutine.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                delay(5000)
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@ThirdActivity, "协程还在运行中", Toast.LENGTH_SHORT).show()
                }
                println("协程还在运行中")
            }
        }

```

点击按钮后，退回到桌面，等待几秒后也没发现打印，从桌面回到App后，发现Toast和打印都出现了。

这也符合了我们的要求：App在前台时协程工作，App在后台时协程停止工作，避免不必要的资源浪费。

launchWhenResumed()函数顾名思义是当Activity处在Resume状态时激活协程，非Resume状态时挂起协程，类似的还有launchWhenCreated、launchWhenStarted。

## 关联生命周期的协程的原理

### 解决内存泄漏的原理

知道了怎么使用，又到了探索原理的时刻，重点在协程作用域。

```
#LifecycleOwner.kt
//扩展属性
public val LifecycleOwner.lifecycleScope: LifecycleCoroutineScope
    get() = lifecycle.coroutineScope

#Lifecycle.kt
public val Lifecycle.coroutineScope: LifecycleCoroutineScope
    get() {
        while (true) {
            val existing = mInternalScopeRef.get() as LifecycleCoroutineScopeImpl?
            if (existing != null) {
                return existing
            }
            //构造新的协程作用域，默认在主线程执行协程
            val newScope = LifecycleCoroutineScopeImpl(
                this,
                SupervisorJob() + Dispatchers.Main.immediate
            )
            if (mInternalScopeRef.compareAndSet(null, newScope)) {
                //协程作用域关联生命周期
                newScope.register()
                return newScope
            }
        }
    }

fun register() {
    launch(Dispatchers.Main.immediate) {
        if (lifecycle.currentState >= Lifecycle.State.INITIALIZED) {
            //监听生命周期变化
            lifecycle.addObserver(this@LifecycleCoroutineScopeImpl)
        } else {
            //如果已经处在destroy状态，直接取消协程
            coroutineContext.cancel()
        }
    }
}

```

由上可知：

> 
> 
> 1. LifecycleOwner有个扩展属性lifecycleScope，而LifecycleOwner又持有了Lifecycle，因此LifecycleOwner的lifecycleScope来自于Lifecycle的扩展属性coroutineScope
> 2. 既然是Lifecycle的扩展属性，理所当然可以监听Lifecycle的状态变化

lifecycleScope 监听了Lifecycle的状态变化，直接看其回调的处理即可：

```
#Lifecycle.kt
override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    if (lifecycle.currentState <= Lifecycle.State.DESTROYED) {
        //如果处于Destroy状态，也就是Activity被销毁了，那么移除监听者
        lifecycle.removeObserver(this)
        //取消协程
        coroutineContext.cancel()
    }
}

```

至此就比较明了了：

> 
> 
> 
> 每个Activity实例就是一个LifecycleOwner，进而每个Activity都关联了一个lifecycleScope对象，该对象可以监听Activity的生命周期，在Activity销毁时取消协程。
> 

### 避免资源浪费原理

相较于解决内存泄漏原理，避免资源浪费原理比较绕，我们简单捋一下。

以launchWhenResumed函数为例，它是LifecycleCoroutineScope里的函数：

```
#Lifecycle.kt
public fun launchWhenResumed(block: suspend CoroutineScope.() -> Unit): Job = launch {
    //启动了协程
    lifecycle.whenResumed(block)
}

#PausingDispatcher.kt
public suspend fun <T> Lifecycle.whenResumed(block: suspend CoroutineScope.() -> T): T {
    return whenStateAtLeast(Lifecycle.State.RESUMED, block)
}

public suspend fun <T> Lifecycle.whenStateAtLeast(
    minState: Lifecycle.State,
    block: suspend CoroutineScope.() -> T
): T = withContext(Dispatchers.Main.immediate) {
    //切换协程，在主线程执行
    val job = coroutineContext[Job] ?: error("when[State] methods should have a parent job")
    //协程分发器
    val dispatcher = PausingDispatcher()
    //关联了生命周期
    val controller =
        LifecycleController(this@whenStateAtLeast, minState, dispatcher.dispatchQueue, job)
    try {
        //在新的协程里执行block
        withContext(dispatcher, block)
    } finally {
        controller.finish()
    }
}

```

以上透露了三个信息：

> 
> 
> 1. launchWhenResumed 不是挂起函数，它内部启动了新的协程
> 2. launchWhenResumed的闭包要通过PausingDispatcher 调度
> 3. LifecycleController 关联了生命周期

重点看第3点：

```
#LifecycleController.kt
private val observer = LifecycleEventObserver { source, _ ->
    if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
        //取消协程
        handleDestroy(parentJob)
    } else if (source.lifecycle.currentState < minState) {
        //小于目标状态，比如非Resume，则挂起协程
        dispatchQueue.pause()
    } else {
        //继续分发协程
        dispatchQueue.resume()
    }
}

init {
    if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
        handleDestroy(parentJob)
    } else {
        //LifecycleController 初始化时监听生命周期
        lifecycle.addObserver(observer)
    }
}

```

还是通过了lifecycle关联了生命周期。

以上代码结合着看估计还是有点懵，也有点绕，没关系老规矩，用图一看便知：

image.png

重点在于是否可以分发的判断，该判断是基于DispatchQueue里的状态：

```
    fun canRun() = finished || !paused

```

当非Resume状态时，paused=true，不能分发；

当处在Resume状态时，paused=false，能分发。

当Activity退出，finished=true。

# 3. ViewModel与协程的配合

## 没有关联生命周期的协程的使用

在MVVM的架构里，推荐的做法是在ViewModel里进行数据的请求，如：

```
    val liveData = MutableLiveData<String>()
    fun getStuInfo() {
        thread {
            //模拟网络请求
            Thread.sleep(2000)
            liveData.postValue("hello world")
        }
    }

```

而后在Activity里监听数据的变化：

```
        //监听数据变化
        val vm  by viewModels<MyVM>()
        vm.liveData.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        vm.getStuInfo()

```

当然直接开线程的请求数据的方式并不优雅，既然有了协程，那么用协程切换到子线程请求即可。

```
    val scope = CoroutineScope(Job())
    fun getStuInfoV2() {
        scope.launch {
            //模拟网络请求
            delay(4000)
            liveData.postValue("hello world")
            println("hello world")
        }
    }

```

和上面一样的测试步骤：

当退出Activity后，ViewModel里的协程打印还在持续，虽然此时Activity并没有泄漏，但我们也知道ViewModel是为Activity服务的，Activity都销毁了，ViewModel没存在的必要了，因此其关联的协程也该取消达到节约资源的目的。

## 关联生命周期的协程的使用

```
    fun getInfo() {
        viewModelScope.launch {
            //模拟网络请求
            delay(4000)
            liveData.postValue("hello world")
            println("hello world")
        }
    }

```

此种写法比上面的更简洁。

当退出Activity后，协程被取消了，当然打印也不会出现了。

## 关联生命周期的协程的原理

重点在viewModelScope对象，它是ViewModel的一个扩展属性：

```
#ViewModel.kt
public val ViewModel.viewModelScope: CoroutineScope
    get() {
        //查缓存
        val scope: CoroutineScope? = this.getTag(JOB_KEY)
        if (scope != null) {
            return scope
        }
        //加入到缓存里
        return setTagIfAbsent(
            JOB_KEY,
            //构造协程作用域
            CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        )
    }

```

ViewModel构造了一个扩展属性：viewModelScope，用以表示当前ViewModel的协程作用域，将作用域对象存储到Map里。

后续在ViewModel里想要使用协程的地方调用viewModelScope即可，极大增强了便利性。

接下来看看它如何在Activity销毁后取消协程。

```
    final void clear() {
        mCleared = true;
        if (mBagOfTags != null) {
            synchronized (mBagOfTags) {
                //从缓存取出协程作用域
                for (Object value : mBagOfTags.values()) {
                //取消协程
                closeWithRuntimeException(value);
            }
            }
        }
    }

```

整个流程用图表示：

image.png

上面的流程涉及到ViewModel的原理，有兴趣可以移步：[Jetpack ViewModel 抽丝剥茧](https://www.jianshu.com/p/49b3813ff191)

# 4. Application创建全局的协程作用域

无论是Activity里的lifecycleScope亦或是ViewModel里的viewModelScope，都和页面有关系，页面销毁了它们都没有存在的必要了。而有时候我们需要在页面之外的其它地方使用协程，它们不受页面创建与销毁的影响，通常我们会想到使用全局的协程。

image.png

## 自定义Application扩展属性

```
val Application.scope: CoroutineScope
get() {
    return CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
//使用
application.scope.launch {
    delay(5000)
    println("协程在全局状态运行1")
}

```

构造了全局的协程作用域，当在其它模块拿到Application实例时就可以访问该扩展属性。

此种方式的好处：可以方便地自定义协程上下文。

## GlobalScope

一般在测试的时候使用，不推荐使用在正式的项目里。

```
GlobalScope.launch {
    delay(5000)
    println("协程在全局状态运行2")
}

```

## ProcessLifecycleOwner

官方出品，它更多的时候被用来监测App在前后台的状态，原理是通过监听Lifecycle，既然有Lifecycle，当然有协程作用域了：

```
ProcessLifecycleOwner.get().lifecycleScope.launch {
    delay(5000)
    println("协程在全局状态运行3")
}

```

# 5. Flow、协程、生命周期的三角关系

## 概念明晰

从Android开发的角度来看，三者有如下区别：

> 
> 
> 1. 生命周期主要说的是UI的生命周期
> 2. Flow和协程是Kotlin语言范畴的，Kotlin是跨平台的
> 3. Flow必须要在协程里使用
> 4. 结合1.2两点，我们发现关联了生命周期的协程作用域都是以扩展属性的形式存在的，毕竟其它平台可能不需要关联生命周期

## Flow 与生命周期

### LiveData关联生命周期

Flow号称是LiveData的增强实现，我们知道LiveData是可以检测生命周期的，如：

```
        binding.btnStartLifecycleLivedata.setOnClickListener {
            vm.liveData.observe(this) {
                //接收数据
                println("hello world")
            }
            vm.getInfo()
        }

```

当App退回到桌面，此时即使ViewModel里继续往LiveData里赋值，也不会触发LiveData回调。当App恢复到前台后，LiveData回调将被触发。

此种设计是为了避免不必要的资源浪费。

### Flow结合launchWhenXX

此时你可能会想到：不用LiveData传递数据，改用Flow替代它，该怎么关联生命周期呢？

按照前面的经验，很容易有如下写法：

```
        binding.btnStartLifecycleFlowWhen.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                MyFlow().flow.collect {
                    println("collect when $it")
                }
            }
        }

    val flow = flow {
        var count = 0
        while (true) {
            kotlinx.coroutines.delay(1000)
            println("emit hello world $count")
            emit(count++)
        }
    }

```

构造一个冷流Flow，在Activity里通过launchWhenResumed启动协程，并在协程里调用collect末端操作符。collect触发flow闭包里的代码执行，源源不断地发射数据，collect闭包里的打印也将持续。

此时将App退回到桌面，发现打印没有出现，而后将App返回前台，打印继续。如此一来就可以达成和LiveData一样的效果。

从打印结果我们还发现有趣的现象：

> 
> 
> 
> 在打印到数字5的时候，我们退回桌面，等待若干秒后再回到前台，此时从6开始打印
> 
> 说明launchWhenXX函数在Activity不活跃时并没有终止flow上游的工作，仅仅只是将协程挂起了
> 

### Flow结合repeatOnLifecycle

而更多的时候，当Activity不活跃时，我们不想要flow继续工作，此时引入了另一个API：repeatOnLifecycle

```
        binding.btnStartLifecycleFlowRepeat.setOnClickListener {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    MyFlow().flow.collect {
                        println("collect repeat $it")
                    }
                }
                println("repeatOnLifecycle over")
            }
        }

```

通过打印发现：

> 
> 
> 
> 在打印到数字5的时候，我们退回桌面，等待若干秒后再回到前台，此时从0开始打印
> 
> 说明repeatOnLifecycle函数在Activity不活跃时终止了flow上游的工作，因为协程被取消了。当Activity活跃后，协程又重新启动，flow工作重来一次
> 

你也许还有疑惑：上面的Demo没有直接证明两者的区别，因为在Activity退到桌面后flow闭包里的打印都没出现。

对Demo稍加修改，结果就会显而易见：

```
    val flow = flow {
        var count = 0
        while (true) {
            kotlinx.coroutines.delay(1000)
            println("emit hello world $count")
            emit(count++)
        }
    }.flowOn(Dispatchers.IO)

```

> 
> 
> 
> 使用repeatOnLifecycle时，在Activity退到桌面后，打印消失，说明flow停止工作
> 
> 使用launchWhenXX是，在Activity退到桌面后，打印继续，说明flow在工作
> 

### repeatOnLifecycle 原理

repeatOnLifecycle 是LifecycleOwner的扩展函数，进而是lifecycle的扩展函数，因此它就拥有了生命周期。

repeatOnLifecycle 函数里开启了新的协程，并监听生命周期的变化：

```
//监听生命周期
observer = LifecycleEventObserver { _, event ->
    if (event == startWorkEvent) {
        //大于目标生命状态，则启动协程
        launchedJob = this@coroutineScope.launch {
            // Mutex makes invocations run serially,
            // coroutineScope ensures all child coroutines finish
            mutex.withLock {
                coroutineScope {
                    block()
                }
            }
        }
        return@LifecycleEventObserver
    }
    if (event == cancelWorkEvent) {
        //小于目标生命状态，则取消协程
        launchedJob?.cancel()
        launchedJob = null
    }
    if (event == Lifecycle.Event.ON_DESTROY) {
        //Activity退出，则唤醒挂起的协程
        cont.resume(Unit)
    }
}
this@repeatOnLifecycle.addObserver(observer as LifecycleEventObserver)

```

repeatOnLifecycle 还有另一种使用方式：

```
                MyFlow().flow.flowWithLifecycle(this@ThirdActivity.lifecycle, Lifecycle.State.RESUMED)
                    .collectLatest {
                        println("collect repeat $it")
                    }

```

和repeatOnLifecycle一样的效果，只是此种方式产生的Flow是线程安全的。

### launchWhenXX与repeatOnLifecycle区别与应用场景

image.png

最后，总结三者之间的关系。

image.png

Flow很强大也很好用，关键是怎么用，如何从众多的Flow操作符选择合适进行业务开发，如何一眼就分辨它们的作用，下篇将揭开Flow常见操作符神秘的面纱，敬请关注。

本文基于Kotlin 1.5.3，[文中完整实验Demo请点击](https://links.jianshu.com/go?to=https%3A%2F%2Fgithub.com%2Ffishforest%2FKotlinDemo%2Ftree%2Fmaster%2Fapp%2Fsrc%2Fmain%2Fjava%2Fcom%2Ffish%2Fkotlindemo%2FlifecycleAndCoroutine)

# 您若喜欢，请点赞、关注、收藏，您的鼓励是我前进的动力

# 持续更新中，和我一起步步为营系统、深入学习Android/Kotlin

> 
> 
> 
> 1、[Android各种Context的前世今生](https://www.jianshu.com/p/bd412e96d027)
> 
> 2、[Android DecorView 必知必会](https://www.jianshu.com/p/d6bf7f784265)
> 
> 3、[Window/WindowManager 不可不知之事](https://www.jianshu.com/p/3bb40beae95d)
> 
> 4、[View Measure/Layout/Draw 真明白了](https://www.jianshu.com/p/23822b8f900d)
> 
> 5、[Android事件分发全套服务](https://www.jianshu.com/p/236d088e7025)
> 
> 6、[Android invalidate/postInvalidate/requestLayout 彻底厘清](https://www.jianshu.com/p/02073c90ef98)
> 
> 7、[Android Window 如何确定大小/onMeasure()多次执行原因](https://www.jianshu.com/p/a7ab49462ebe)
> 
> 8、[Android事件驱动Handler-Message-Looper解析](https://www.jianshu.com/p/5036d176b202)
> 
> 9、[Android 键盘一招搞定](https://www.jianshu.com/p/91b7c3abface)
> 
> 10、[Android 各种坐标彻底明了](https://www.jianshu.com/p/a2a7431f3e04)
> 
> 11、[Android Activity/Window/View 的background](https://www.jianshu.com/p/3dd994614758)
> 
> 12、[Android Activity创建到View的显示过](https://www.jianshu.com/p/0c6f4a65c825)
> 
> 13、[Android IPC 系列](https://www.jianshu.com/p/8d112c74979a)
> 
> 14、[Android 存储系列](https://www.jianshu.com/p/3cbb7febbfa3)
> 
> 15、[Java 并发系列不再疑惑](https://www.jianshu.com/p/5334a131151e)
> 
> 16、[Java 线程池系列](https://www.jianshu.com/p/506f325ee250)
> 
> 17、[Android Jetpack 前置基础系列](https://www.jianshu.com/p/cede93e863f0)
> 
> 18、[Android Jetpack 易懂易学系列](https://www.jianshu.com/p/404623c209ec)
> 
> 19、[Kotlin 轻松入门系列](https://www.jianshu.com/p/06876b71d662)
> 
> 20、[Kotlin 协程系列全面解读](https://www.jianshu.com/p/68ac68cd7455)
> 

©著作权归作者所有,转载或内容合作请联系作者