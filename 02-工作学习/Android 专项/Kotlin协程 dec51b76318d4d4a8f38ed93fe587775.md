# Kotlin协程

- 协程的基本概念
    
    协程的概念最核心的点就是函数或者一段程序能够被挂起，稍后再挂起的位置恢复；**协程与线程最大的区别在于，从任务的角度来看，线程一旦开始执行就不会暂停，直到任务结束，这个过程是连续的。线程之间是抢占式的调度，因为不存在协作的问题。**
    
    Kotlin 的协程是有栈协程（stackfull coroutine）。**这意味着每个协程在执行时会拥有自己的调用栈。**这与传统的线程相似，每个线程也有自己的调用栈。有栈协程的主要优势是更好的上下文切换和更小的内存开销。 **Kotlin 的协程库可以使用少量的线程来执行大量的协程，因为协程的切换不需要切换整个线程，而只需要切换协程的上下文。**这使得 Kotlin 协程非常适合处理高并发的情况，同时提供了优雅的异步编程解决方案。
    
- 简单协程
    - 协程的创建
        
        ```kotlin
        val continuation = suspend {
            println("In Coroutine.")
            22
        }.createCoroutine(object : Continuation<Int> {
            override val context = EmptyCoroutineContext
        
            override fun resumeWith(result: Result<Int>) {
                println("Coroutine End: $result")
            }
        })
        ```
        
        - Receiver 是一个被 suspend 修饰的挂起函数，这也是协程的执行体，我们不妨称它为协程体。
        - 参数 completion 会在协程执行完成后调用，实际上就是协程的完成回调。
        - 返回值是一个 Continuation 对象，由于现在协程仅仅被创建出来，因此需要通过这个值在之后触发协程的启动。
    - 如何启动协程
        - 调用 continuation.resume(Unit)之后，协程体会立即开始执行。
        - 这个 continuation 是 SafeContinuation 的实例。
        - 它有一个名为 delegate 的属性，这个属性才是 Continuation 的本体。
        - 它的类名类似<FileName>Kt$<FunctionName>$continuation$1这样的形式，这就是我们的协程体。
        
        ![SafeContinuation 内部包含的对象就是编译生成的匿名内部类，这个匿名内部类同时又是 BaseContinuationImpl的子类](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled.png)
        
        SafeContinuation 内部包含的对象就是编译生成的匿名内部类，这个匿名内部类同时又是 BaseContinuationImpl的子类
        
    - 简单协程API
        - 不带 Receiver 的协程体创建及启动
        
        ```kotlin
        public fun <T> (suspend () -> T).createCoroutine(
            completion: Continuation<T>
        ): Continuation<Unit> =
            SafeContinuation(createCoroutineUnintercepted(completion).intercepted(), COROUTINE_SUSPENDED)
        
        public fun <T> (suspend () -> T).startCoroutine(
            completion: Continuation<T>
        ) {
            createCoroutineUnintercepted(completion).intercepted().resume(Unit)
        }
        ```
        
        - 带 Receiver 的协程体创建及启动，Receiver 通常用于约束和扩展协程体。
        
        ```kotlin
        public fun <R, T> (suspend R.() -> T).createCoroutine(
            receiver: R,
            completion: Continuation<T>
        ): Continuation<Unit> =
            SafeContinuation(createCoroutineUnintercepted(receiver, completion).intercepted(), COROUTINE_SUSPENDED)
        
        public fun <R, T> (suspend R.() -> T).startCoroutine(
            receiver: R,
            completion: Continuation<T>
        ) {
            createCoroutineUnintercepted(receiver, completion).intercepted().resume(Unit)
        }
        ```
        
    - 函数的挂起
        - 挂起函数
            - Kotlin 协程的挂起和恢复能力本质上就是函数的挂起和恢复。
            - 使用 suspend 关键字修饰的函数叫作挂起函数，挂起函数只能在协程体或者其他挂起函数内调用。
            - 协程的挂起就是程序执行流程发生异步调用时，当前调用流程的执行状态进入等待状态。
        - 挂起点
            - 协程本体就是一个 Continuation 实例，所以挂起函数才能在协程体内执行。
            - 在协程内部挂起函数的调用出被称为挂起点。
            - 挂起点如果出现异步调用，那么当前协程就被挂起，直接对应的 Continuation 的 resume 函数被调用才会恢复执行。
            - suspendCoroutine 函数获得的 Continuation 是一个 SafeContinue 的实例。
            - SafeContinue 类的作用也非常简单，它可以确保只有发生异步调用时才会挂起。
            - 异步调用是否发生，取决于 resume 函数与对应的挂起函数的调用是否相同的调用栈上，切换函数调用栈的方法可以是切换到其他线程上执行，也可以是不切换线程当在当前函数返回之后的某一个时刻再执行。
        - CPS变换
            - CPS变化(Continuation-Passing-Style Transformation)，是通过传递 Continuation 来控制异步调用流程的。
            - Continuation 携带了协程继续执行所需要的上下文，恢复执行的时候只需要执行它的恢复调用并且把需要的参数或者异常传入即可。作为一个普通的对象，Continuation 占用的内存非常小，这也是无栈协程能够流行的一个主要原因。
            - 挂起函数同步返回：作为参数传入的 Continuation 的 resumeWith 不会被调用，函数的实际返回值就是它作为挂起函数的返回值。
            - 挂起函数挂起，执行异步逻辑：此时函数的实际返回值是一个挂起标志，通过这个标志外部协程就可以知道该函数需要挂起等到异步逻辑执行。
            - 为什么 Kotlin 语法要求挂起函数一定要运行在协程体内或者其他挂起函数中呢？答：任何一个协程体或者挂起函数都一个隐含的 Continuation 实例，编译器能够对这个实例进行正确传递。
        - 协程的上下文协程拦截器
            - EmptyCoroutineContext 是一个标准库已经定义好的 object,表示一个空的协程上下文，里面没有数据。
            
            ```kotlin
            public interface CoroutineContext {
                public interface Element : CoroutineContext {
                    /**
                     * A key of this coroutine context element.
                     */
                    public val key: Key<*>
            
                    public override operator fun <E : Element> get(key: Key<E>): E? =
                        @Suppress("UNCHECKED_CAST")
                        if (this.key == key) this as E else null
            
                    public override fun <R> fold(initial: R, operation: (R, Element) -> R): R =
                        operation(initial, this)
            
                    public override fun minusKey(key: Key<*>): CoroutineContext =
                        if (this.key == key) EmptyCoroutineContext else this
                }
            }
            
            public object EmptyCoroutineContext : CoroutineContext, Serializable {
                private const val serialVersionUID: Long = 0
                private fun readResolve(): Any = EmptyCoroutineContext
            
                public override fun <E : Element> get(key: Key<E>): E? = null
                public override fun <R> fold(initial: R, operation: (R, Element) -> R): R = initial
                public override fun plus(context: CoroutineContext): CoroutineContext = context
                public override fun minusKey(key: Key<*>): CoroutineContext = this
                public override fun hashCode(): Int = 0
                public override fun toString(): String = "EmptyCoroutineContext"
            }
            ```
            
            - 协程的上下文元素的实现
            
            ```kotlin
            public data class CoroutineName(
                /**
                 * User-defined coroutine name.
                 */
                val name: String
            ) : AbstractCoroutineContextElement(CoroutineName) {
                /**
                 * Key for [CoroutineName] instance in the coroutine context.
                 */
                public companion object Key : CoroutineContext.Key<CoroutineName>
            
                /**
                 * Returns a string representation of the object.
                 */
                override fun toString(): String = "CoroutineName($name)"
            }
            ```
            
            - 协程上下文的使用
            
            ```kotlin
            //上下文的设置
            var coroutineContext: CoroutineContext = EmptyCoroutineContext
            coroutineContext += CoroutineName("test")
            coroutineContext += CoroutineExceptionHandler { _, _ ->
                TODO()
            } 
            //-----------------------
            //上下文的获取
            val name = coroutineContext[CoroutineName]?.name
            ```
            
    - 拦截器的位置
        
        在 Continuation 和协程上下文的基础上，标准库又提供了一个叫作拦截器的组件，它允许我们拦截协程异步回调时的恢复调用。
        
        ```kotlin
        suspend { 
            suspendFunc02("Hello","Kotlin")
            suspendFunc02("Hello","Coroutine")
        }.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext
        
            override fun resumeWith(result: Result<Unit>) {
                println("Coroutine End: $result")
            }
        })
        ```
        
        我们启动了一个协程，并在其中调用了两次挂起函数 suspendFunc02，这个挂起函数每次执行都会异步挂起，那么这个过程中发生了几次恢复调用呢？
        
        - 协程启动时调用一次，通过恢复调用来开始执行协程体重开始到下一次挂起之间的逻辑。
        - 挂起点处如果异步挂起，则在恢复时会调用一次。由于这个过程中有两次挂起，因此会调用两次。
        - 由此可知，恢复调用的次数为 1+n 次，其中 n 是协程体内真正挂起执行异步逻辑的挂起点的个数。
        
        ![Untitled](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled 1.png)
        
    - 拦截器的使用
        
        挂起点恢复执行的位置都可以在需要的时候添加拦截器来实现一些 AOP 操作。拦截器也是协程上下文的一类实现，定义拦截器只需要实现拦截器的接口，并添加到对应的协程上下文中。
        
        ```kotlin
        class LogContinuation<T>(private val continuation: Continuation<T>) : Continuation<T> {
            override val context: CoroutineContext = continuation.context
        
            override fun resumeWith(result: Result<T>) {
                println("Before resumeWith: $result")
                continuation.resumeWith(result)
                println("After resumeWith.")
            }
        }
        //定义拦截器
        class LogInterceptor : ContinuationInterceptor {
            override val key: CoroutineContext.Key<*>
                get() = ContinuationInterceptor
        
            override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
                return LogContinuation(continuation)
            }
        }
        //使用拦截器
        suspend {
        	5
        }.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext = LogInterceptor()
        
            override fun resumeWith(result: Result<Unit>) {
                println("Coroutine End: $result")
            }
        })
        ```
        
        ```kotlin
        before resumeWith: Success(Kotlin.Unit)
        after resumeWidth.
        before resumeWith: Success(5)
        after resumeWith.
        before resumeWith: Success(5)
        Coroutine End: Success(5)
        after resumeWith.
        ```
        
    - 整体结构图
        
        添加拦截器之后，delegate 其实就是拦截器之后的 Continuation 实例了。
        
        协程体在挂起点处先被拦截器拦截，再被 SafeContinuation 保护了起来。想要让协程真正恢复执行，先要经过则两个过程。
        
        ![Untitled](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled 2.png)
        
- 复合协程
    - 协程的描述
        - 协程的描述类
            
            对协程的简单协程做进一步封装，目的就是降低协程的创建和管理的成本。
            
            官方协程框架用 Job类 来描述协程的状态。
            
            ```kotlin
            public interface Job : CoroutineContext.Element {
               
                public companion object Key : CoroutineContext.Key<Job>
            
                public val isActive: Boolean
            
                public val isCompleted: Boolean
            
                public val isCancelled: Boolean
            
                public fun start(): Boolean
            
                public fun cancel(cause: CancellationException? = null)
            
                public val children: Sequence<Job>	
            		
                public suspend fun join()
            
                public val onJoin: SelectClause0
            
                public fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle
            }
            ```
            
            - 与 Thread 相比，Job 同样有 join，调用时会挂起，直到协程完成。
            - 它的 Cancel 可类比为 Thread 的 interrupt，用于取消协程。
            - isActive 则可以类比 Thread 的 isAlive，用于查询协程是否仍在执行。
            - key 主要是用于将协程的 Job实例存入它的上下文中，这样我们只要能够获得协程的上下文就可以拿到 Job 的实例。
            - invokeOnCompletion 则可以注册一个协程完成时的回调
        - 协程的状态
            
            协程生命周期状态 :
            
            - 新创建 New
            - 活跃 Active : 通过调用 Job#isActivity 获取当前是否处于活跃状态。
            - 完成中 Completing
            - 已完成 Completed : 通过调用 Job#isCompleted 获取当前是否处于已完成状态 。
            - 取消中 Canceling
            - 已取消 Cancelled : 通过调用 Job#isCancelled 获取当前是否处于取消状态 。
            
            ![Untitled](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled 3.png)
            
        - 协程的初步实现
            
            ```kotlin
            public abstract class AbstractCoroutine<in T>(
                parentContext: CoroutineContext,
                initParentJob: Boolean,
                active: Boolean
            ) : JobSupport(active), Job, Continuation<T>, CoroutineScope {
            
            		init {
                    if (initParentJob) initParentJob(parentContext[Job])
                }
            
                @Suppress("LeakingThis")
                public final override val context: CoroutineContext = parentContext + this
            
                public override val coroutineContext: CoroutineContext get() = context
            
                override val isActive: Boolean get() = super.isActive
            
            }
            ```
            
    - 协程的创建
        - 无返回值的 launch
            
            如果一个协程的返回值是 Unit，，那么我们可以称为“无返回值”。对于这样的协程，我们只需要启动它即可。用 launch 创建的协程可以立即运行起来。
            
            ```kotlin
            GlobalScope.launch {
                println(1)
                delay(1000)
                println(2)
            }
            ```
            
            launch 的实现，代码如下：
            
            ```kotlin
            public fun CoroutineScope.launch(
                context: CoroutineContext = EmptyCoroutineContext,
                start: CoroutineStart = CoroutineStart.DEFAULT,
                block: suspend CoroutineScope.() -> Unit
            ): Job {
                val newContext = newCoroutineContext(context)
                val coroutine = if (start.isLazy)
                    LazyStandaloneCoroutine(newContext, block) else
                    StandaloneCoroutine(newContext, active = true)
                coroutine.start(start, coroutine, block)
                return coroutine
            }
            ```
            
            其中StandaloneCoroutine是 AbstractCoroutine 的子类，目前只有一个空实现，代码如下：
            
            ```kotlin
            private open class StandaloneCoroutine(
                parentContext: CoroutineContext,
                active: Boolean
            ) : AbstractCoroutine<Unit>(parentContext, initParentJob = true, active = active) {
                override fun handleJobException(exception: Throwable): Boolean {
                    handleCoroutineException(context, exception)
                    return true
                }
            }
            ```
            
        - 有返回值的 async
            
            我们很多时候更想拿到协程的返回值，因此我们基于协程 Job 在定义一个接口 Deferred。
            
            ```kotlin
            public interface Deferred<out T> : Job {
            
                public suspend fun await(): T
            
                public val onAwait: SelectClause1<T>
            
                @ExperimentalCoroutinesApi
                public fun getCompleted(): T
            
                @ExperimentalCoroutinesApi
                public fun getCompletionExceptionOrNull(): Throwable?
            }
            ```
            
            接下来我们给出 async 函数的实现，代码如下：
            
            ```kotlin
            public fun <T> CoroutineScope.async(
                context: CoroutineContext = EmptyCoroutineContext,
                start: CoroutineStart = CoroutineStart.DEFAULT,
                block: suspend CoroutineScope.() -> T
            ): Deferred<T> {
                val newContext = newCoroutineContext(context)
                val coroutine = if (start.isLazy)
                    LazyDeferredCoroutine(newContext, block) else
                    DeferredCoroutine<T>(newContext, active = true)
                coroutine.start(start, coroutine, block)
                return coroutine
            }
            ```
            
            使用示例如下：
            
            ```kotlin
            suspend fun getValue():String{
                delay(1000)
                return "HelloWorld"
            }
            
            suspend fun main(){
                val deferred = GlobalScope.async { 
                    getValue()
                }
                val result = deferred.await()
                println(deferred)
            }
            ```
            
        - 协程的实现类UML
            
            ![Untitled](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled 4.png)
            
    - 协程的调度
        
        与线程调度的不同：协程在哪里挂起，什么时候恢复是开发者自己决定的，这意味着调度工作不能交给操作系统，而应该在用户态解决，也正是因为这一点，协程也经常被称作用户态线程。
        
        协程的调度位置：协程的恢复和挂起不在同一函数调用栈中执行就是挂起点挂起的充分条件，线程切换并不是必要条件。只有当挂起点真正挂起，我们才有机会实现调度，而实现调度需要使用协程的拦截器。
        
        异步的情况包括以下形式：
        
        - 挂起点对应的挂起函数内部切换了线程，并在该线程内部调用 Continuation 的resume调用来恢复。
        - 挂起函数内部通过某种事件循环的机制将 Continuation 的恢复调用转到新的线程调用栈上执行。
        - 挂起函数内部将 Continuation 实例保存，在后续某个时机在执行恢复调用。
        
        协程的调度器的本质：就是解决挂起点恢复之后的协程逻辑在哪里运行的问题。
        
        调度器接口的定义：
        
        ```kotlin
        public interface ContinuationInterceptor : CoroutineContext.Element {
        
            companion object Key : CoroutineContext.Key<ContinuationInterceptor>
        
            public fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>
        
            public fun releaseInterceptedContinuation(continuation: Continuation<*>) {
                /* do nothing by default */
            }
        
            public override operator fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
                // getPolymorphicKey specialized for ContinuationInterceptor key
                @OptIn(ExperimentalStdlibApi::class)
                if (key is AbstractCoroutineContextKey<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    return if (key.isSubKey(this.key)) key.tryCast(this) as? E else null
                }
                @Suppress("UNCHECKED_CAST")
                return if (ContinuationInterceptor === key) this as E else null
            }
        
            public override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
                // minusPolymorphicKey specialized for ContinuationInterceptor key
                @OptIn(ExperimentalStdlibApi::class)
                if (key is AbstractCoroutineContextKey<*, *>) {
                    return if (key.isSubKey(this.key) && key.tryCast(this) != null) EmptyCoroutineContext else this
                }
                return if (ContinuationInterceptor === key) EmptyCoroutineContext else this
            }
        }
        ```
        
        使用拦截器实现调度：
        
        ```kotlin
        public abstract class CoroutineDispatcher :
            AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
        
            /** @suppress */
            @ExperimentalStdlibApi
            public companion object Key : AbstractCoroutineContextKey<ContinuationInterceptor, CoroutineDispatcher>(
                ContinuationInterceptor,
                { it as? CoroutineDispatcher })
        
            public open fun isDispatchNeeded(context: CoroutineContext): Boolean = true
        
            public abstract fun dispatch(context: CoroutineContext, block: Runnable)
        
            public final override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
                DispatchedContinuation(this, continuation)
        
            public final override fun releaseInterceptedContinuation(continuation: Continuation<*>) {
                val dispatched = continuation as DispatchedContinuation<*>
                dispatched.release()
            }
        }
        ```
        
    - 协程的取消
        
        为了支持取消，我们需要 Continuation 提供一个取消状态和回调，并在检查到当前协程取消的事件给 CompletableFuture，代码如下：
        
        ```kotlin
        public suspend inline fun <T> suspendCancellableCoroutine(
            crossinline block: (CancellableContinuation<T>) -> Unit
        ): T =
            suspendCoroutineUninterceptedOrReturn { uCont ->
                val cancellable = CancellableContinuationImpl(uCont.intercepted(), resumeMode = MODE_CANCELLABLE)
                cancellable.initCancellability()
                block(cancellable)
                cancellable.getResult()
            }
        ```
        
        - 一旦这里的 Continuation 实例锁对应的协程取消，通过 invokeOnCancellation 注册给 Continuation 实例的回调就会被调用，进而取消掉 completableFuture 实例。
        - 这个函数也暴露了挂起点的执行的本质。suspendCoroutineUninterceptedOrReturn 的参数是一个函数，而这个函数有一个参数是 Continuation，实际上就是协程体编译后生成的匿名内部类的实例。
        - 而uCont.intercepted()返回的对象，就是被拦截器拦截之后的 Continuation 对象。
        - SafeContinuation 的作用就是确保传入的 Continuation 对象的恢复调用只被调用一次。如果在 block(safe)执行的过程中就直接调用了 Continuation 的恢复调用，那么cancellable.getResult() 执行时就会获取到结果，这样就不会真正挂起了。
        - 所谓的挂起点一定要切换函数调用栈实现异步才会真正挂起，这其实就是由 SafeContinuation 来保证的。
        
        协程体的实体类关系图
        
        ![Untitled](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled 5.png)
        
        挂起函数的实现规则
        
        挂起函数的背后通常就是一个异步操作，这个操作通常都很耗时，因此挂起函数的实现应该仔细考虑是否支持对取消状态的响应。
        
        如果需要响应取消状态，需要做到以下两点：
        
        - 真正发生挂起时，使用suspendCancellableCoroutine来获取 CancellableContinuation的实例，通过 invokeOnCancellation 来注册回调监听所在的协程的取消事件。
        - 未发生挂起时，可以直接通过挂起属性 coroutineContext 来获取所在协程的 Job 实例，进而检查所在协程的状态。如果已经取消，则直接抛出取消一次来相应取消。(使用cancel函数主动取消了协程。当协程被取消时，delay函数就会抛出CancellationException)
        
        示例代码：
        
        ```kotlin
        suspend fun mySuspendFunction() {
            if (coroutineContext.isActive) {
                // 执行一些操作
                // ...
                
                // 检查协程的取消状态
                if (coroutineContext.isCancelled) {
                    throw CancellationException("Coroutine is cancelled")
                }
                
                // 执行其他操作
                // ...
            }
        }
        ```
        
    - 协程的异常处理
        
        异常处理是异步程序的另一个需要解决的关键问题。
        
        定义异常处理，代码如下：
        
        ```kotlin
        @Suppress("FunctionName")
        public inline fun CoroutineExceptionHandler(crossinline handler: (CoroutineContext, Throwable) -> Unit): CoroutineExceptionHandler =
            object : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
                override fun handleException(context: CoroutineContext, exception: Throwable) =
                    handler.invoke(context, exception)
            }
        ```
        
        要处理协程的未捕获异常，我们需要在 AbstractCoroutine 中定义一个子类可见的函数。子类根据自身需要覆写它并实现自己的异常处理逻辑，返回值为 true 表示异常已处理：
        
        ```kotlin
        protected open fun handleJobException(exception: Throwable): Boolean = false
        ```
        
        AbstractCoroutine 的子类目前只有两个：
        
        - StandaloneCoroutine：由 launch 启动，协程本身无返回结果。我们期望它能够遇到未捕获一次时，调用自身的异常处理器进行处理，如果没有异常处理器，则将异常抛给 completion 调用时所在的线程的 uncaughtExceptionHandler 来处理。
        
        ```kotlin
        override fun handleJobException(exception: Throwable): Boolean {
            handleCoroutineException(context, exception)
            return true
        }
        @InternalCoroutinesApi
        public fun handleCoroutineException(context: CoroutineContext, exception: Throwable) {
            // Invoke an exception handler from the context if present
            try {
                context[CoroutineExceptionHandler]?.let {
                    it.handleException(context, exception)
                    return
                }
            } catch (t: Throwable) {
                handleCoroutineExceptionImpl(context, handlerException(exception, t))
                return
            }
            // If a handler is not present in the context or an exception was thrown, fallback to the global handler
            handleCoroutineExceptionImpl(context, exception)
        }
        internal actual fun handleCoroutineExceptionImpl(context: CoroutineContext, exception: Throwable) {
            // use additional extension handlers
            for (handler in handlers) {
                try {
                    handler.handleException(context, exception)
                } catch (t: Throwable) {
                    // Use thread's handler if custom handler failed to handle exception
                    val currentThread = Thread.currentThread()
                    currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, handlerException(exception, t))
                }
            }
        
            // use thread's handler
            val currentThread = Thread.currentThread()
            // addSuppressed is never user-defined and cannot normally throw with the only exception being OOM
            // we do ignore that just in case to definitely deliver the exception
            runCatching { exception.addSuppressed(DiagnosticCoroutineContextException(context)) }
            currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
        }
        ```
        
        - DeferredCoroutine：由 async 启动，协程存在返回结果。既然存在返回结果，调用者总是会通过 await 来获取它的结果，因此我们期望它不要主动抛出未捕获的异常，而是在 await 调用时在抛出；我们已经支持了在遇到未捕获的异常时 await 会直接将其抛出的功能，因此无须再做其他实现。
        
        区别对待取消异常：在协程取消时，挂起函数通过抛出取消异常来实现对取消状态的响应，这一点类似于线程的中断异常，因此未捕获异常中不应该包含取消异常。
        
        ![Untitled](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled 6.png)
        
    - 协程作用域
        
        通常我们提到域，都是用来描述范围的，域即有约束作用又有额外能力提供。生活中这样的例子很多，例如公司电脑人域之后就可以访问公司内网，相应的也会受到公司 IT 部门的监控。
        
        - 声明作用域：除了添加 Receiver 之后，我们也需要注意下 newCoroutineContext 中我们将 coroutineContext 也一并添加到用于启动协程的上下文中了，这样即将创建的协程就可以获取到作用域的上下文了。
        
        ```kotlin
        public fun CoroutineScope.launch(
            context: CoroutineContext = EmptyCoroutineContext,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            block: suspend CoroutineScope.() -> Unit
        ): Job {
            val newContext = newCoroutineContext(context)
            val coroutine = if (start.isLazy)
                LazyStandaloneCoroutine(newContext, block) else
                StandaloneCoroutine(newContext, active = true)
            coroutine.start(start, coroutine, block)
            return coroutine
        }
        public actual fun CoroutineScope.newCoroutineContext(context: CoroutineContext): CoroutineContext {
            val combined = foldCopies(coroutineContext, context, true)
            val debug = if (DEBUG) combined + CoroutineId(COROUTINE_ID.incrementAndGet()) else combined
            return if (combined !== Dispatchers.Default && combined[ContinuationInterceptor] == null)
                debug + Dispatchers.Default else debug
        }
        ```
        
        - 建立父子关系：父协程取消之后，子协程也需要取消；如果父协程存在那么需要监听它的取消回调，在父协程取消时，确保子协程也进入取消状态。
        - 顶级作用域：通过GlobalScope 创建的协程将不会有父协程，我们也可以把它称作**根协程**。
        
        ```kotlin
        @DelicateCoroutinesApi
        public object GlobalScope : CoroutineScope {
            /**
             * Returns [EmptyCoroutineContext].
             */
            override val coroutineContext: CoroutineContext
                get() = EmptyCoroutineContext
        }
        ```
        
        - 协同作用域：默认情况下，在一个协程体直接创建协程，二者会直接产生父子关系，并且子协程所在的作用域为**协同作用域；**协同作用域的效果就是将父子协程绑定到了一起，父取消则子取消，子异常则父“连坐”。
        - 主从作用域：主从作用域与协同作用域的区别只有一点，即在子协程的未捕获是否向上传播，主从作用域像一道防火墙一样阻断了子协程的向上扩散。
        
        ```kotlin
        private class SupervisorCoroutine<in T>(
            context: CoroutineContext,
            uCont: Continuation<T>
        ) : ScopeCoroutine<T>(context, uCont) {
            override fun childCancelled(cause: Throwable): Boolean = false
        }
        ```
        
        - 挂起函数内获取作用域：我们直到一个挂起函数只能运行在某一个协程当中，而在我们现在框架的基础上，创建协程又必然会存在作用域。
        
        ```kotlin
        public suspend fun <R> coroutineScope(block: suspend CoroutineScope.() -> R): R {
            contract {
                callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            }
            return suspendCoroutineUninterceptedOrReturn { uCont ->
                val coroutine = ScopeCoroutine(uCont.context, uCont)
                coroutine.startUndispatchedOrReturn(coroutine, block)
            }
        }
        ```
        
        - 父协程等待子协程完成：作用域还要求父协程必须等待子协程执行完，之后才可以进入完成状态。这要求父协程的 resumeWith 执行完成之后，还需要对子协程进行检查。如果子协程尚未完成，则向子协程中注册完成回调，直到所有的子协程的完成回调都触发。父协程才能将自己状态流转为完成状态，并触发对应的完成回调。
        
        ![Untitled](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled 7.png)
        
- 协程官方框架
    - 协程框架的概述
        - 协程的启动模式
            
            ```kotlin
            public fun CoroutineScope.launch(
                context: CoroutineContext = EmptyCoroutineContext,
                start: CoroutineStart = CoroutineStart.DEFAULT,
                block: suspend CoroutineScope.() -> Unit
            ): Job {
                val newContext = newCoroutineContext(context)
                val coroutine = if (start.isLazy)
                    LazyStandaloneCoroutine(newContext, block) else
                    StandaloneCoroutine(newContext, active = true)
                coroutine.start(start, coroutine, block)
                return coroutine
            }
            ```
            
            官方的框架中的 launch 等 api 都多个一个参数 start，它的类型是CoroutineStart，它的值有四种：
            
            - DEFAULT：协程创建后，立即开始调度，在调度前如果协程被取消，其将直接进入取消响应的状态。
            - ATOMIC：协程创建后，立即开始调度，协程执行到第一个挂起点之前不响应取消。
            - LAZY：只有协程被需要时，包括主动调用协程的 start、join 或者 await 等函数时才会开始调度，如果调度前被取消，那么该协程将直接进入异常结束状态。
            - UNDISPATCHED：协程创建后立即在当前函数调用栈中执行，直到遇到第一个真正挂起点。
            
            <aside>
            💡 这些启动模式的设计主要是为了应对某些特殊的场景。业务开发实践中通常使用 DEFAULT 和 LAZY 这两个启动模式就足够了。
            
            </aside>
            
        - 协程的调度器
            
            对于调度器的实现机制我们已经非常清楚了，官方框架中预置了 4 个调度器，我们可以通过 Dispatchers 对象访问它们。
            
            - Default：默认调度器，适合处理后台计算，其是一个 CPU 密集型任务调度器。
            - IO：IO 调度器，适合执行 IO 相关操作，其是一个 IO 密集型任务调度器。
            - Main：UI 调度器，根据平台不同会被初始化为对应的 UI 线程的调度器，例如在 Android 平台上他会将协程协程调度到 UI 事件循环中执行，即通常在主线程执行。
            - Unconfined：“无所谓”调度器，不要求协程执行在特定线程上。协程的调度器如果是 Unconfined，那么它在挂起点恢复执行时会在恢复所在的线程上直接执行，当然，如果嵌套创建以它为调度器的协程，那么这些协程会在启动时被调度到协程框架内部的事件循环上，以避免出现StackOverFlow。
            
            将线程池转换成调度器
            
            ```kotlin
            Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            ```
            
            <aside>
            💡 withContext会将参数中的 Lambda 表达式调度到对应的调度器上，它自己本身就是一个挂起函数，返回值为 Lambda 表达式的值，由此可见它的作用几乎等价于 async{}.await()；与 async{}.await()相比，withContext 的内存开销更低，因此对于使用 async 之后立即调用 await 的情况，应该优先使用 withContext。
            
            </aside>
            
        - 协程的全局拦截器
            
            在根协程未设置异常拦截器时，未捕获异常会优先传递给全局异常拦截器处理，之后再交给线程所在的 UncaughtExceptionHandler。
            
            1. 定义全局拦截器
                
                ```kotlin
                class GlobalCoroutineExceptionHandler : CoroutineExceptionHandler {
                    override val key: CoroutineContext.Key<*>
                        get() = CoroutineExceptionHandler
                
                    override fun handleException(context: CoroutineContext, exception: Throwable) {
                        println("Global Coroutine exception: $exception")
                    }
                }
                ```
                
            2. 需要在 classpath 下面创建 META-INF/services 目录，并在其中创建一个名为 kotlinx.coroutines.CoroutineExceptionHandler 的文件，文件内容就是我们的全局异常处理器的全类名。本例中该文件的内容为：
                
                ```kotlin
                com.example.kotlincoroutinesample.GlobalCoroutineExceptionHandler
                ```
                
            
            <aside>
            💡 配置全局异常处理器，在 Default 或者 IO调度上遇到未捕获的异常时发生闪退，但是可以到异常信息，此时全局异常处理器的配置就显得格外重要了。
            
            </aside>
            
        - 协程的取消检查
            
            我们已经知道挂起函数可以通过 suspendCancellableCoroutine 来响应所在协程的取消状态。我们在设计异步任务时，异步任务的取消响应点可能就在这些挂起点处。那我们就要看看官方协程还提供了哪些对逻辑没有影响的挂起函数，这其中最合适的就是 yield 函数，yield 函数的作用主要是检查所在协程的状态，如果已经取消，则抛出取消异常予以响应。此外，它还会尝试出让线程的执行权，给其他协程提供执行的机会。代码如下：
            
            ```kotlin
            suspend fun InputStream.copyToSuspend(
                out: OutputStream,
                bufferSize: Int = DEFAULT_BUFFER_SIZE,
            ): Long {
                var bytesCopied: Long = 0
                val buffer = ByteArray(bufferSize)
                var bytes = read(buffer)
                while (bytes >= 0) {
                    yield()
                    out.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = read(buffer)
                }
                return bytesCopied
            }
            ```
            
        - 协程的超时取消
            
            我们发送网络请求，通常会设置一个超时来对网络不佳的情况，所有的网络框架(如 OkHttp)都会提供这样的参数。使用withTimeout()实现超时取消，代码如下：
            
            ```kotlin
            GlobalScope.launch {
                val user = withTimeout(5_000) {
                    getUserSuspend()
                }
                println(user)
            }
            ```
            
        - 禁止取消
            
            我们做示例的时候希望用 delay 函数来模拟耗时任务，在外部又尝试取消这个耗时任务以观察协程的取消响应的效果。
            
            ```kotlin
            GlobalScope.launch {
                val job = launch { 
                    listOf(1,2,3,4).forEach { 
                        yield()
                        delay(it*1000L)
                    }
                }
            }
            ```
            
            我本意时希望研究 yield 函数的作用，然而在运行中，响应协程取消的不一定是 yield 函数，因为 delay 函数自身也可以响应取消，甚至由于它执行时挂起的时间跨度更大，反而非常容易干扰实验效果。
            
            官方框架为我们提供一个名为 NonCancellable 的上下文实现，它的作用就是禁止作用范围内的协程取消。为了确保 delay 函数不响应取消，我们对前面的代码稍作修改，需要注意的是，NonCanncellable 需要与 withContext 配合使用，不应该当作为 launch 这样的协程构造器的上下文传入，因为这样做没有任何意义。
            
            ```kotlin
            GlobalScope.launch {
                val job = launch {
                    listOf(1, 2, 3, 4).forEach {
                        yield()
                        withContext(NonCancellable) {
                            delay(it * 1000L)
                        }
                    }
                }
            }
            ```
            
    - 热数据通道 Channel
        - 认识 Channel
            
            **Channel 实际上就是一个并发安全的队列，它可以用来连接协程，实现不同协程的通信。代码如下：**
            
            ```kotlin
            suspend fun main() {
                val channel = Channel<Int>()
                val producer = GlobalScope.launch {
                    var i = 0
                    while (true) {
                        delay(1000)
                        channel.send(i++)
                    }
                }
                val consumer = GlobalScope.launch {
                    while (true) {
                        val element = channel.receive()
                        println(element)
                    }
                }
                producer.join()
                consumer.join()
            }
            ```
            
            上述代码构造了两个协程 producer 和 consumer，我们没有为它们明确指定调度器，所以它们的调度器都是默认的，在 Java 平台上就是基于线程池实现的 Default。它们可以运行在不同线程上，也可以运行在同一线程上。
            
        - Channel 的容量
            
             Channel 实际上就是一个队列，队列中一定存在缓冲区，那么一旦这个缓冲区满了，并且一直没有人调用 receive 并取走元素，send 就需要挂起，等待接受者取走数据之后再写入 Channel。接下来我们看 Channel 缓冲区的定义，代码如下：
            
            ```kotlin
            public fun <E> Channel(
                capacity: Int = RENDEZVOUS,
                onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
                onUndeliveredElement: ((E) -> Unit)? = null
            ): Channel<E> =
                when (capacity) {
                    RENDEZVOUS -> {
                        if (onBufferOverflow == BufferOverflow.SUSPEND)
                            RendezvousChannel(onUndeliveredElement) // an efficient implementation of rendezvous channel
                        else
                            ArrayChannel(1, onBufferOverflow, onUndeliveredElement) // support buffer overflow with buffered channel
                    }
                    CONFLATED -> {
                        require(onBufferOverflow == BufferOverflow.SUSPEND) {
                            "CONFLATED capacity cannot be used with non-default onBufferOverflow"
                        }
                        ConflatedChannel(onUndeliveredElement)
                    }
                    UNLIMITED -> LinkedListChannel(onUndeliveredElement) // ignores onBufferOverflow: it has buffer, but it never overflows
                    BUFFERED -> ArrayChannel( // uses default capacity with SUSPEND
                        if (onBufferOverflow == BufferOverflow.SUSPEND) CHANNEL_DEFAULT_CAPACITY else 1,
                        onBufferOverflow, onUndeliveredElement
                    )
                    else -> {
                        if (capacity == 1 && onBufferOverflow == BufferOverflow.DROP_OLDEST)
                            ConflatedChannel(onUndeliveredElement) // conflated implementation is more efficient but appears to work in the same way
                        else
                            ArrayChannel(capacity, onBufferOverflow, onUndeliveredElement)
                    }
                }
            ```
            
            我们构造Channel的时候调用一个名为 Channel 的函数，虽然两个 “Channel”看起来时一样的，但它却确实不是 Channel 的构造函数。在 Kotlin 中我们经常定义一个顶层函数来伪装同名类型的构造器，这本质就是工厂函数。Channel 函数有一个参数叫作 capacity，该参数用于指定缓冲区的容量。
            
            - RENDEZVOUS默认值为 0，RENDEZVOUS的本意就是描述“不见不散”的场景，如果不调用 receive，send 就会一直挂起等待。换句话说，在前一个列子里面，如果 consumer 不调用 receive，producer 里面的第一个 send 就挂起了。
            - UNLIMITED，比较好理解，其来者不拒，从它给出的实现类 LinkedListChannel 来看，这一点与 LinkedBlockingQueue 有异曲同工之妙。
            - CONFLATED的字面意思是合并，那是不是这边发 1、2、3、4、5，那么就会收到一个[1,2,3,4,5]的集合呢？实际上这个函数的效果只保留最后一个元素，所以这不是合并而是置换，**即这个类型的 Channel 只有一个元素大小的缓冲区，每次有新元素过来，都会用新的替换旧的。**也就是是说发送端发送了 1、2、3、4、5 之后，接收端才接收的话，那么只会收到 5.
            
    - 冷数据流 Flow
        
        Flow 就是 Kotlin 协程与响应式编程结合的产物。
        
        - 认识 Flow
            
            介绍 Flow 之前，我们先来回顾一下序列生成器，代码如下：
            
            ```kotlin
            fun main() {
                val ints = sequence<Int> {
                    (1..3).forEach {
                        yield(it)
                    }
                }
            }
            ```
            
            每次访问 ints 的下一个元素时，序列生成器就执行内部的逻辑直到遇到 yield，如果希望在元素之间加个延时怎么办？代码如下：
            
            ```kotlin
            fun main() {
                val ints = sequence<Int> {
                    (1..3).forEach {
                        yield(it)
                        //Error
                        delay(1000)
                    }
                }
            }
            
            @RestrictsSuspension
            @SinceKotlin("1.3")
            public abstract class SequenceScope<in T> internal constructor() {
                
                public abstract suspend fun yield(value: T)
            
                public abstract suspend fun yieldAll(iterator: Iterator<T>)
            
                public suspend fun yieldAll(elements: Iterable<T>) {
                    if (elements is Collection && elements.isEmpty()) return
                    return yieldAll(elements.iterator())
                }
            
                public suspend fun yieldAll(sequence: Sequence<T>) = yieldAll(sequence.iterator())
            }
            ```
            
            受RestrictsSuspension注解的约束，delay 函数不能在SequenceScope的扩展成员中被调用，因此也不能在序列生成器的协程体内调用。
            
            既然序列生成器有这么多限制，我们就有必要认识一下 Flow 了。Flow 的 API 与序列生成器极为相似，代码如下：
            
            ```kotlin
            fun main() {
                val intFlow = flow<Int> {
                    (1..3).forEach { 
                        emit(it)
                        delay(1000)
                    }
                }
            }
            ```
            
            新元素由 emit 函数提供，Flow 的执行体内部也可以调用其他挂起函数，这样我们就可以在每次提供一个新元素后再延时 1000ms 了。
            
            Flow 也可以运行所使用的调度器：
            
            ```kotlin
            intFlow.flowOn(Dispatchers.IO)
            ```
            
            通过 flowOn 设置的调度器只对它之前的操作有影响，因此这里意味着 intFlow 的构造逻辑会在 IO 调度器上执行。
            
            最终消费 intFlow 需要调用 collect 函数，这个函数也是一个挂起函数。我们启动一个协程来消费 intFlow，代码如下：
            
            ```kotlin
            suspend fun main() {
                GlobalScope.launch(myDispatcher) {
                    intFlow.flowOn(Dispatchers.IO).collect{
                        println(it)
                    }
                }.join()
            }
            ```
            
            为了方便区分，我们为协程设置一个自定义的调度器，他会将协程调度到名叫 MyThread 的线程上，结果如果下：
            
            ```kotlin
            [MyThread] 1
            [MyThread] 2
            [MyThread] 3
            ```
            
        - 对比 RxJava 的线程切换
            
            RxJava 也是一个基于响应式编程模型的异步框架，它提供了两个切换调度器的 API ，分别是 subscribeOn 和 observeOn，代码如下：
            
            ```kotlin
            Observable.create<Int> {
                (1..3).forEach { e ->
                    it.onNext(e)
                }
                it.onComplete()
            }.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.from(myExecutor))
                .subscribe {
                    println(it)
                }
            ```
            
            其中 subscribeOn 指定的调度器影响前面的逻辑，observeOn 影响后面的逻辑，因此 it.onNext(e)在它的 io 调度器上执行，而最后的 println(it)在通过 myExecutor 创建出来的调度器上执行。
            
            Flow 的调度器API中看似只有 flowOn 与 subscribeOn 对应，其实不然，还有 collect 函数所在的协程的调度器与 observeOn 指定的调度器对应。
            
            在学习和使用 RxJava 的过程中，subscribeOn 和 observeOn 经常被混淆；而在 Flow 中 collect 函数所在的协程自然就是订阅者，它运行在哪个调度器上由它自己指定，非常容易区分。
            
        - 冷数据流
            
            在一个 Flow 创建出来之后，不消费则不生产，多次消费则多次生产，生产和消费总是相对应的。代码如下：
            
            ```kotlin
            GlobalScope.launch { 
                intFlow.collect {
                    println(it)
                }
                intFlow.collect {
                    println(it)
                }
            }.join()
            ```
            
            intFlow 就是上面开始创建的 Flow，消费它会输出 “1,2,3”，重复消费它会重复输出“1,2,3”
            
            所谓冷数据流，就是只有消费时才会生产的数据流，这一点与 Channel 正好相反，Channel 的发送端并不依赖接收端。
            
        - 异常处理
            
            Flow 的异常处理也比较直接，直接调用 catch 函数即可。代码如下；
            
            ```kotlin
            flow<Int> {
                emit(1)
                throw ArithmeticException("Div 0")
            }.catch { t: Throwable ->
                println("caught error : $t")
            }
            ```
            
            我们在 Flow 中抛出一个异常，catch 函数就可以直接捕获到这个异常。如果没有调用 catch 函数，未捕获的异常会在消费时抛出。请注意，catch 函数只能捕获它上游的异常。
            
            如果想要在 Flow 完成时执行逻辑，可以使用 onCompletion：
            
            ```kotlin
            flow<Int> {
                emit(1)
                throw ArithmeticException("Div 0")
            }.catch { t: Throwable ->
                println("caught error : $t")
            }.onCompletion { t: Throwable? ->
                println("finally")
            }
            ```
            
            我们也可以使用 emit 重新生产新元素。细心的读者一定会发现，emit 定义在 FlowCollector 中，因此只要遇到 Receiver 为 FlowCollector 的函数，我们就可以生产新的元素。
            
            ```kotlin
            flow<Int> {
                emit(1)
                throw ArithmeticException("Div 0")
            }.catch { t: Throwable ->
                println("caught error : $t")
                emit(10)
            }
            ```
            
        - 末端操作符
            
            前面的例子中，我们用 collect 消费 Flow 的数据。collect 是最基本的末端**操作符**，功能与 RxJava 的 subscribe 类似。除了 collect 之外，还有其他常见的末端操作符，它们大体分为两类：
            
            - 集合类型转换操作符，包括 toList、toSet 等。
            - 聚合操作符，包括将 Flow 规约到单值的 reduce、fold 等操作；还有获得单个元素的操作符，包括 single、singleOrNull、first 等。
            
            实际上，识别是否为末端操作符，还有一个简单的方法：由于 Flow 的消费端一定需要运行在协程中，因此末端操作符都是挂起函数。
            
        - 分离 Flow 的消费和触发
            
            我们除了可以在 collect 处消费 Flow 的元素以外，还可以通过 onEach 来做到这一点。这样消费的具体操作就不需要与末端操作符放到一起，collect 函数可以放到其他任意位置调用，示例代码如下：
            
            ```kotlin
            fun createFlow() = flow<Int> {
                (1..3).forEach {
                    emit(it)
                    delay(100)
                }
            }
            
            fun main() {
                GlobalScope.launch {
                    createFlow().collect()
                }
            }
            ```
            
            由此，我们又可以衍生出一种新的新的消费 Flow 的写法，代码如下：
            
            ```kotlin
            fun main() {
                createFlow().launchIn(GlobalScope)
            }
            ```
            
            其中，launch 函数只接收一个 CoroutineScope 类型的参数。
            
        - Flow 的取消
            
             Flow 没有提供取消操作，因为并不需要。
            
            Flow 的消费依赖于 collect 这样的末端操作符，而它们又必须在协程中调用，因此 Flow 的取消主要依赖于末端操作符所在的协程的状态。
            
            如此看来，想要取消 Flow 只需要取消它所在的协程即可。
            
        - 其他 Flow 的创建方式
            
            我们已经知道了 flow{}这种形式的创建方式，不过在这当中无法随意切换调度器，这是因为 emit 函数不是线程安全的。
            
            ```kotlin
            fun main() {
                //BAD!!
                flow {
                    emit(1)
                    withContext(Dispatchers.IO) {
                        emit(2)
                    }
                }
            }
            ```
            
            想要在生成元素时切换调度器，就必须使用 channelFlow 函数来创建 Flow：
            
            ```kotlin
            fun main() {
                channelFlow<Int> {
                    send(1)
                    withContext(Dispatchers.IO) {
                        send(2)
                    }
                }
            }
            ```
            
            此外，我们也可以通过集合框架来创建 Flow：
            
            ```kotlin
            fun main() {
                listOf(1, 2, 3, 4).asFlow()
                setOf(1, 2, 3, 4).asFlow()
                flowOf(1, 2, 3, 4)
            }
            ```
            
        - Flow 的背压
            
            只要是响应式编程，就一定会有背压问题。
            
            背压问题在生产者的生产速率高于消费者的处理速率的情况下出现；为了保证数据不丢失，我们也会考虑添加缓冲来缓解背压问题，代码如下：
            
            ```kotlin
            fun main() {
               flow {
                   List(100) {
                       emit(it)
                   }
               }.buffer()
            }
            ```
            
            我们也可以为 buffer 指定一个容量。不过，如果只是单纯地添加缓冲，而不是从根本上解决问题，还是会造成数据的积压。
            
            出现背压问题的根本原因时生产者和消费者速率不匹配，此时除可直接优化消费的性能以外，还可以采用一些取舍的手段。
            
            - 第一种是  conflate。与 Channel 的Conflate 模式一致，新数据会覆盖老数据。我们快速发送了 100 个元素，最后接收到的只有 2 个，当然这个结果不一定每次都一样。示例代码如下：
            
            ```kotlin
            suspend fun main() {
                flow<Int> {
                    List(100) {
                        emit(it)
                    }
                }.conflate()
                    .collect {
                        println("Collecting $it")
                        delay(100)
                        println("$it collected")
                    }
            }
            ```
            
            - 第二种是 collectLatest。顾名思义，其处理最新的数据。这看上去似乎与 conflate 没有区别，其实区别很大：collectLatest 并不会直接用新数据覆盖老数据，而是每一个数据都会处理，只不过如果前一个还没有被处理完，后一个就来了的话，处理前一个数据的逻辑就会被取消。
            
            ```kotlin
            suspend fun main() {
                flow<Int> {
                    List(100) {
                        emit(it)
                    }
                }.collectLatest {
                    println("Collecting $it")
                    delay(100)
                    println("$it collected")
                }
            }
            ```
            
            前面的 Collecting 输出了 0-99 的所有结果，而 collected 却只有 99，因为后面的数据到达时，处理上一个数据的操作正好被挂起了。
            
    - 并发安全
        
        我们使用线程在解决并发问题的时候，总是会遇到线程的安全的问题，而在 Java 平台上的 Kotlin 协程实现免不了存在并发调度的情况，因此线程安全同样值得留意。
        
        例如：不安全的计数
        
        ```kotlin
        suspend fun main() {
            var count = 0
            List(1000) {
                GlobalScope.launch { 
                    count++
                }
            }.joinAll()
            println(count)
        }
        ```
        
        运行在 Java 平台上，默认启动的协程会被调度到 Default 这个基于线程池的调度器上，因此 count++是不安全的，最终的结果也证实了这一点。解决这个问题我们都有丰富的经验，例如将 count 声明为原子类型，确保自增操作为原子操作，代码如下：
        
        ```kotlin
        suspend fun main() {
            var count = AtomicInteger(0)
            List(1000) {
                GlobalScope.launch {
                    count.incrementAndGet()
                }
            }.joinAll()
            println(count)
        }
        ```
        
        协程的并发工具：除了我们在线程中常用的解决并发问题的手段之外，协程框架也提供了一些并发安全的工具，包括：
        
        - Channel：并发安全的消息通道，我们已经非常熟悉。
        - Mutex：轻量级锁，它的 lock 和 unlock 从语义上与线程锁比较类似，之所以轻量是因为它在获取不到锁时不会阻塞线程，而只是挂起等待锁的释放，代码如下：
            
            ```kotlin
            suspend fun main() {
                var count = 0
                val mutex = Mutex()
                List(1000) {
                    GlobalScope.launch {
                       mutex.withLock { 
                           count++
                       } 
                    }
                }.joinAll()
                println(count)
            }
            ```
            
        - Semaphore：轻量级信号量，信号量可以有多个，协程在获取信号量后即可执行并发操作。当 Semaphore 的参数为 1 时，效果等价于 Mutex，相关示例代码如下：
            
            ```kotlin
            suspend fun main() {
                var count = 0
                var semaphore = Semaphore(1)
                List(1000) {
                    GlobalScope.launch {
                        semaphore.withPermit { 
                            count++
                        }
                    }
                }.joinAll()
                println(count)
            }
            ```
            
            与线程相比，协程的 API 在需要等待时挂起即可，因此显得更加轻量，加上它更具表现力的异步能力，只要使用得当，就可以用更少的资源实现更复杂的逻辑。
            
    

[谱写Kotlin面试指南三部曲-协程篇 - 掘金](Kotlin%E5%8D%8F%E7%A8%8B/%E8%B0%B1%E5%86%99Kotlin%E9%9D%A2%E8%AF%95%E6%8C%87%E5%8D%97%E4%B8%89%E9%83%A8%E6%9B%B2-%E5%8D%8F%E7%A8%8B%E7%AF%87%20-%20%E6%8E%98%E9%87%91%20a523d4206edf4e4aa711eae7cac9b60f.md)

- 怎么从零把 Kotlin 协程学到95分?
    
    ![Untitled](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled 8.png)
    
- 协程的异常处理为什么比线程还难
    
    ![Untitled](assets/Kotlin协程 dec51b76318d4d4a8f38ed93fe587775/Untitled 9.png)
    

- 异常传播传播与处理
    1. **CoroutineExceptionHandler 是协程的全局兜底策略，只在根协程有效，在子协程无效。**
    2. **launch会立即传播，而async会延迟传播。**
    3. **async的异常不是"传播"，而是"存储和重新抛出"。**
    4. **SupervisorJob 为 launch 提供父子隔离，为 async 提供兄弟隔离。**