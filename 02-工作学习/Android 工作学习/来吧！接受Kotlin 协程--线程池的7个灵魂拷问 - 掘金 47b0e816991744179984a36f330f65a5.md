# 来吧！接受Kotlin 协程--线程池的7个灵魂拷问 - 掘金

[https://juejin.cn/post/7207078219215962170](https://juejin.cn/post/7207078219215962170)

# 前言

协程系列文章：

> 
> 
> - [一个小故事讲明白进程、线程、Kotlin 协程到底啥关系？](https://juejin.cn/post/7108651566806073380)
> - [少年，你可知 Kotlin 协程最初的样子？](https://juejin.cn/post/7109410972653060109)
> - [讲真，Kotlin 协程的挂起/恢复没那么神秘(故事篇)](https://juejin.cn/post/7110143508513554440)
> - [讲真，Kotlin 协程的挂起/恢复没那么神秘(原理篇)](https://juejin.cn/post/7111246680338464804)
> - [Kotlin 协程调度切换线程是时候解开真相了](https://juejin.cn/post/7113706345190129700)
> - [Kotlin 协程之线程池探索之旅(与Java线程池PK)](https://juejin.cn/post/7114968347325759501)
> - [Kotlin 协程之取消与异常处理探索之旅(上)](https://juejin.cn/post/7127923407001223175)
> - [Kotlin 协程之取消与异常处理探索之旅(下)](https://juejin.cn/post/7128218056882389028)
> - [来，跟我一起撸Kotlin runBlocking/launch/join/async/delay 原理&使用](https://juejin.cn/post/7128961903220490270)
> - [继续来，同我一起撸Kotlin Channel 深水区](https://juejin.cn/post/7139468247119691807)
> - [Kotlin 协程 Select：看我如何多路复用](https://juejin.cn/post/7142083646822809607)
> - [Kotlin Sequence 是时候派上用场了](https://juejin.cn/post/7160910310100992014)
> - [Kotlin Flow啊，你将流向何方？](https://juejin.cn/post/7168511169781563428)
> - [Kotlin Flow 背压和线程切换竟然如此相似](https://juejin.cn/post/7172957388348063780)
> - [Kotlin SharedFlow&StateFlow 热流到底有多热？](https://juejin.cn/post/7195569817940164668)
> - [狂飙吧，Lifecycle与协程、Flow的化学反应](https://juejin.cn/post/7202987088153018425)
> - [来吧！接受Kotlin 协程--线程池的7个灵魂拷问](https://juejin.cn/post/7207078219215962170)
> - [当，Kotlin Flow与Channel相逢](https://juejin.cn/post/7224145268740325435)
> - [这一次，让Kotlin Flow 操作符真正好用起来](https://juejin.cn/post/7226933611265605669)

之前有分析过协程里的线程池的原理：[Kotlin 协程之线程池探索之旅(与Java线程池PK)](https://juejin.cn/post/7114968347325759501)，当时偏重于整体原理，对于细节之处并没有过多的着墨，后来在实际的使用过程中遇到了些问题，也引发了一些思考，故记录之。

通过本篇文章，你将了解到：

> 
> 
> 1. 为什么要设计Dispatchers.Default和Dispatchers.IO？
> 2. Dispatchers.Default 是如何调度的？
> 3. Dispatchers.IO 是如何调度的？
> 4. 线程池是如何调度任务的？
> 5. 据说Dispatchers.Default 任务会阻塞？该怎么办？
> 6. 线程的生命周期是如何确定？
> 7. 如何更改线程池的默认配置？

# 1. 为什么要设计Dispatchers.Default和Dispatchers.IO？

## 一则小故事

书接上篇：[一个小故事讲明白进程、线程、Kotlin 协程到底啥关系？](https://juejin.cn/post/7108651566806073380)

出场人物：

> 
> 
> 
> 操作系统，简称OS
> 
> Java
> 
> Kotlin
> 

在Java的世界里支持多线程编程，开启一个线程的方式很简单：

```
复制代码    private void startNewThread() {
        new Thread(()->{
            //线程体
            //我在子线程执行...
        }).start();
    }

```

而Java也是按照此种方式创建线程执行任务。

某天，OS找到Java说到："你最近的线程创建、销毁有点频繁，我这边切换线程的上下文是要做准备和善后工作的，有一定的代价，你看怎么优化一下？"

Java无辜地答到："我也没办法啊，业务就是那么多，需要随时开启线程做支撑。"

OS不悦："你最近态度有点消极啊，说到问题你都逃避，我理解你业务复杂，需要开线程，但没必要频繁开启关闭，甚至有些线程就执行了一会就关闭，而后又立马开启，这不是玩我吗？。这问题必须解决，不然你的KPI我没法打，你回去尽快想想给个方案出来。"

Java悻悻然："好的，老大，我尽量。"

Java果然不愧是编程界的老手，很快就想到了方案，他兴冲冲地找到OS汇报："我想到了一个绝佳的方案：建立一个线程池，固定开启几个线程，有任务的时候往线程池里的任务队列扔就完事了，线程池会找到已提交的任务进行执行。当执行完单个任务之后，线程继续查找任务队列，如果没有任务执行的话就睡眠等待，等有任务过来的时候通知线程起来继续干活，这样一来就不用频繁创建与销毁线程了，perfect！"

OS抚掌夸赞："池化技术，这才是我认识的Java嘛，不过线程也无需一直存活吧？"

Java："这块我早有应对之策，线程池可以提供给外部接口用来控制线程空闲的时间，如果超过这时间没有任务执行，那就辞退它（销毁），我们不养闲人！"

OS满意点点头："该方案，我准了，细节之处你再完善一下。"

经过一段时间的优化，Java线程池框架已经比较稳定了，大家相安无事。

某天，OS又把Java叫到办公室："你最近提交的任务都是很吃CPU，我就只有8个CPU，你核心线程数设置为20个，剩余的12个根本没机会执行，白白创建了它们。"

Java沉吟片刻道："这个简单，针对计算密集型的任务，我把核心线程数设置为8就好了。"

OS略微思索："也不失为一个办法，先试试吧，看看效果再说。"

过了几天，OS又召唤了Java，面带失望地道："这次又是另一个问题了，最近提交的任务都不怎么吃CPU，基本都是IO操作，其它计算型任务又得不到机会执行，CPU天天在摸鱼。"

Java理所当然道："是呀，因为设置的核心线程数是8，被IO操作的任务占用了，同样的方式对于这种类型任务把核心线程数提高一些，比如为CPU核数的2倍，变为16，这样即使其中一些任务占用了线程，还剩下其它线程可以执行任务，一举两得。"

OS来回踱步，思考片刻后大声道："不对，你这么设置万一提交的任务都是计算密集型的咋办？又回到原点了，不妥不妥。"

Java似乎早料到OS有此疑问，无奈道：”没办法啊，我只有一个参数设置核心线程，线程池里本身不区分是计算密集型还是IO阻塞任务，鱼和熊掌不可兼得。"

OS怒火中烧，整准备拍桌子，在这关键时刻，办公室的门打开了，翩翩然进来的是Kotlin。

Kotlin看了Java一眼，对OS说到："我已经知道两位大佬的担忧，食君俸禄，与君分忧，我这里刚好有一计策，解君燃眉之急。"

OS欣喜道："小K，你有何妙计，速速道来。“

Kotlin平息了一下激动的内心："我计策说起来很简单，在提交任务的时候指定其是属于哪种类型的任务，比如是计算型任务，则选择Dispatchers.Default，若是IO型任务则选择Dispatchers.IO，这样调用者就不用关注其它的细节了。"

Java说到："这策略我不是没有想到，只是担忧越灵活可能越不稳定。"

OS打断他说："先让小K完整说一下实现过程，下来你俩仔细对一下方案，扬长避短，吃一堑长一智，这次务必要充分考虑到各种边界情况。"

Java&Kotlin："好的，我们下来排期。"

故事讲完，言归正传。

# 2. Dispatchers.Default 是如何调度的？

## Dispatchers.Default 使用

```
复制代码            GlobalScope.launch(Dispatchers.Default) {
                println("我是计算密集型任务")
            }

```

开启协程，指定其运行的任务类型为：Dispatchers.Default。

此时launch函数闭包里的代码将在线程池里执行。

Dispatchers.Default 用在计算密集型的任务场景里，此种任务比较吃CPU。

## Dispatchers.Default 原理

### 概念约定

在解析原理之前先约定一个概念，如下代码：

```
复制代码            GlobalScope.launch(Dispatchers.Default) {
                println("我是计算密集型任务")
                Thread.sleep(20000000)
            }

```

在任务里执行线程的睡眠操作，此时虽然线程处于挂起状态，但它还没执行完任务，在线程池里的状态我们认为是**忙碌**的。

再看如下代码：

```
复制代码            GlobalScope.launch(Dispatchers.Default) {
                println("我是计算密集型任务")
                Thread.sleep(2000)
                println("任务执行结束")
            }

```

当任务执行结束后，线程继续查找任务队列的任务，若没有任务可执行则进行挂起操作，在线程池里的状态我们认为是**空闲**的。

### 调度原理

*注：此处忽略了本地队列的场景*

由上图可知：

> 
> 
> 1. launch(Dispatchers.Default) 作用是创建任务加入到线程池里，并尝试通知线程池里的线程执行任务
> 2. launch(Dispatchers.Default) 执行并不耗时

# 3. Dispatchers.IO 是如何调度的？

直接看图：

很明显地看出和Dispatchers.Default的调度很相似，其中标蓝的流程是重点的差异之处。

结合Dispatchers.Default和Dispatchers.IO调度流程可知影响任务执行的步骤有两个：

> 
> 
> 1. 线程池是否有空闲的线程
> 2. 创建新线程是否成功

我们先分析第2点，从源码里寻找答案：

```
复制代码    #CoroutineScheduler
    private fun tryCreateWorker(state: Long = controlState.value): Boolean {
        //线程池已经创建并且还在存活的线程总数
        val created = createdWorkers(state)
        //当前IO类型的任务数
        val blocking = blockingTasks(state)
        //剩下的就是计算型的线程个数
        val cpuWorkers = (created - blocking).coerceAtLeast(0)

        //如果计算型的线程个数小于核心线程数，说明还可以再继续创建
        if (cpuWorkers < corePoolSize) {
            //创建线程，并返回新的计算型线程个数
            val newCpuWorkers = createNewWorker()
            //满足条件，再创建一个线程，方便偷任务
            if (newCpuWorkers == 1 && corePoolSize > 1) createNewWorker()
            //创建成功
            if (newCpuWorkers > 0) return true
        }
        //创建失败
        return false
    }

```

怎么去理解以上代码的逻辑呢？举个例子：

假设核心线程数为8，初始时创建了8个Default线程，并一直保持忙碌。

此时分别使用Dispatchers.Default 和 Dispatchers.IO提交任务，看看有什么效果。

> 
> 
> 1. Dispatchers.Default 提交任务，此时线程池里所有任务都在忙碌，于是尝试创建新的线程，而又因为当前计算型的线程数=8，等于核心线程数，此时不能创建新的线程，因此该任务暂时无法被线程执行
> 2. Dispatchers.IO 提交任务，此时线程池里所有任务都在忙碌，于是尝试创建新的线程，而当前阻塞的任务数为1，当前线程池所有线程个数为8，因此计算型的线程数为 8-1=7，小于核心线程数，最后可以创建新的线程用以执行任务

**这也是两者的最大差异，因为对于计算型(非阻塞)的任务，很占CPU，即使分配再多的线程，CPU没有空闲去执行这些线程也是白搭，而对于IO型(阻塞)的任务，不怎么占CPU，因此可以多开几个线程充分利用CPU性能。**

# 4. 线程池是如何调度任务的？

不论是launch(Dispatchers.Default) 还是launch(Dispatchers.IO) ，它们的目的是将任务加入到队列并尝试唤醒线程或是创建新的线程，而线程寻找并执行任务的功能并不是它们完成的，这就涉及到线程池调度任务的功能。

线程池里的每个线程都会经历上图流程，我们很容易得出结论：

> 
> 
> 1. 只有获得cpu许可的线程才能执行计算型任务，而cpu许可的个数就是核心线程数
> 2. 如果线程没有找到可执行的任务，那么线程将会进入挂起状态，此时线程即为空闲状态
> 3. 当线程再次被唤醒后，会判断是否已经被终止，若是则退出，此时线程就销毁了

处在空闲状态的线程被唤醒有两种可能：

> 
> 
> 1. 线程挂起的时间到了
> 2. 挂起的过程中，有新的任务加入到线程池里，此时将会唤醒线程

# 5. 据说Dispatchers.Default 任务会阻塞？该怎么办？

在了解了线程池的任务分发与调度之后，我们对线程池的核心功能有了一个比较全面的认识。

接着来看看实际的应用，先看Demo：

假设我们的设备有8核。

先开启8个计算型任务：

```
复制代码        binding.btnStartThreadMultiCpu.setOnClickListener {
            repeat(8) {
                GlobalScope.launch(Dispatchers.Default) {
                    println("cpu multi...${multiCpuCount++}")
                    Thread.sleep(36000000)
                }
            }
        }

```

每个任务里线程睡眠了很长时间。

从打印可以看出，8个任务都得到了执行，且都在不同的线程里执行。

此时再次开启一个计算型任务：

```
复制代码        var singleCpuCount = 1
        binding.btnStartThreadSingleCpu.setOnClickListener {
            repeat(1) {
                GlobalScope.launch(Dispatchers.Default) {
                    println("cpu single...${singleCpuCount++}")
                    Thread.sleep(36000000)
                }
            }
        }

```

先猜测一下结果？

答案是没有任何打印，新加入的任务没有得到执行。

既然计算型任务无法得到执行，那我们尝试换为IO任务：

```
复制代码        var singleIoCount = 1
        binding.btnStartThreadSingleIo.setOnClickListener {
            repeat(1) {
                GlobalScope.launch(Dispatchers.IO) {
                    println("io single...${singleIoCount++}")
                    Thread.sleep(10000)
                }
            }
        }

```

这次有打印了，说明IO任务得到了执行，并且是新开的线程。

这是为什么呢？

> 
> 
> 1. 计算密集型任务能分配的最大线程数为核心的线程数（默认为CPU核心个数，比如我们的实验设备上是8个），若之前的核心线程数都处在忙碌，新开的任务将无法得到执行
> 2. IO型任务能开的线程默认为64个，只要没有超过64个并且没有空闲的线程，那么就一直可以开辟新线程执行新任务

这也给了我们一个启示：**Dispatchers.Default 不要用来执行阻塞的任务，它适用于执行快速的、计算密集型的任务，比如循环、又比如计算Bitmap等。**

# 6. 线程的生命周期是如何确定？

是什么决定了线程能够挂起，又是什么决定了它唤醒后的动作？

先从挂起说起，当线程发现没有任务可执行后，它会经历如下步骤：

重点在于线程被唤醒后确定是哪种场景下被唤醒的，判断方式也很简单：

> 
> 
> 
> 线程挂起时设定了挂起的结束时间点，当线程唤醒后检查当前时间有没有达到结束时间点，若没有，则说明被新加入的任务动作唤醒的
> 

即使是没有了任务执行，若是当前线程数小于核心线程数，那么也无需销毁线程，继续等待任务的到来即可。

# 7. 如何更改线程池的默认配置？

上面几个小结涉及到核心线程数，线程挂起时间，最大线程数等，这些参数在Java提供的线程池里都可以动态配置，灵活度很高，而Kotlin里的线程池比较封闭，没有提供额外的接口进行配置。

不过好在我们可以通过设置系统参数来解决这问题。

比如你可能觉得核心线程数为cpu的个数配置太少了，想增加这数量，这想法完全是可以实现的。

先看核心线程数从哪获取的。

```
kotlin复制代码internal val CORE_POOL_SIZE = systemProp(
    //从这个属性里取值
    "kotlinx.coroutines.scheduler.core.pool.size",
    AVAILABLE_PROCESSORS.coerceAtLeast(2),//默认为cpu的个数
    minValue = CoroutineScheduler.MIN_SUPPORTED_POOL_SIZE//最小值为1
)

```

若是我们没有设置"kotlinx.coroutines.scheduler.core.pool.size"属性，那么将取到默认值，比如现在大部分是8核cpu，那么CORE_POOL_SIZE=8。

若要修改，则在线程池启动之前，设置属性值：

```
kotlin复制代码        System.setProperty("kotlinx.coroutines.scheduler.core.pool.size", "20")

```

设置为20，此时我们再按照第5小结的Demo进行测试，就会发现Dispatchers.Default 任务不会阻塞。

当然，你觉得IO任务配置的线程数太多了（默认64），想要降低，则修改属性如下：

```
kotlin复制代码        System.setProperty("kotlinx.coroutines.io.parallelism", "40")

```

其它参数也可依此定制，不过若没有强烈的意愿，建议遵守默认配置。

通过以上的7个问题的分析与解释，相比大家都比较了解线程池的原理以及使用了，那么赶紧使用Kotlin线程池来规范线程的使用吧，使用得当可以提升程序运行效率，减少OOM发生。

本文基于Kotlin 1.5.3，[文中完整实验Demo请点击](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Ffishforest%2FKotlinDemo%2Fblob%2Fmaster%2Fapp%2Fsrc%2Fmain%2Fjava%2Fcom%2Ffish%2Fkotlindemo%2FlifecycleAndCoroutine%2FThirdActivity.kt)

# 您若喜欢，请点赞、关注、收藏，您的鼓励是我前进的动力

# 持续更新中，和我一起步步为营系统、深入学习Android/Kotlin

> 
> 
> 
> 1、[Android各种Context的前世今生](https://juejin.cn/post/7015968660179124238)
> 
> 2、[Android DecorView 必知必会](https://juejin.cn/post/7015973616659464206)
> 
> 3、[Window/WindowManager 不可不知之事](https://juejin.cn/post/7015978746104512548)
> 
> 4、[View Measure/Layout/Draw 真明白了](https://juejin.cn/post/7016245187055878180)
> 
> 5、[Android事件分发全套服务](https://juejin.cn/post/7016233922828697608)
> 
> 6、[Android invalidate/postInvalidate/requestLayout 彻底厘清](https://juejin.cn/post/7017452765672636446)
> 
> 7、[Android Window 如何确定大小/onMeasure()多次执行原因](https://juejin.cn/post/7015980840047869983)
> 
> 8、[Android事件驱动Handler-Message-Looper解析](https://juejin.cn/post/7015237933120618504)
> 
> 9、[Android 键盘一招搞定](https://juejin.cn/post/7012844100994990087)
> 
> 10、[Android 各种坐标彻底明了](https://juejin.cn/post/7017834467175874591)
> 
> 11、[Android Activity/Window/View 的background](https://juejin.cn/post/7018044178709872677)
> 
> 12、[Android Activity创建到View的显示过](https://juejin.cn/post/7015959719739129869)
> 
> 13、[Android IPC 系列](https://juejin.cn/post/7023238726503383076)
> 
> 14、[Android 存储系列](https://juejin.cn/post/7012108220982362149)
> 
> 15、[Java 并发系列不再疑惑](https://juejin.cn/post/7010305230256488485)
> 
> 16、[Java 线程池系列](https://juejin.cn/post/7010622964781547527)
> 
> 17、[Android Jetpack 前置基础系列](https://juejin.cn/post/7035235129479921671)
> 
> 18、[Android Jetpack 易学易懂系列](https://juejin.cn/post/7071837699832807438)
> 
> 19、[Kotlin 轻松入门系列](https://juejin.cn/post/7097248290671951909)
> 
> 20、[Kotlin 协程系列全面解读](https://juejin.cn/post/7108651566806073380)
>