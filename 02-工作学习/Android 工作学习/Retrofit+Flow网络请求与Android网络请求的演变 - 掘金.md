# Retrofit+Flow网络请求与Android网络请求的演变 - 掘金

Retrofit网络请求我想大家都不陌生，今天我就来梳理一下技术是如何一步一步进步，逼格是如何一步一步变高的。

### Retrofit使用方式演变

### 萌新

刚开始接触Retrofit的时候是从okhttp和volley以及android系统源码里面那个HttpPost与HttpGet切换过来的。

```
复制代码public interface AuthService {

    @POST("v1/login")
    @FormUrlEncoded
    ResponseBody login(@Field("username") String username, @Field("password") String password);
}

```

那个时候市面上主流还是用的Java，也不知道从哪天开始，突然发现Retrofit这种代理接口的方式用着很爽。于是用着用着就上瘾了，甚至都不知道Retrofit是使用的动态代理的方式。这种方式是通过responseBody.body().string()拿到json字符串，然后再自己通过json解析库解析出数据的。

### 小白

然后有一天到处看博客或技术文章，于是就发现了Retrofit的返回值原来不仅仅可以是ResponseBody，还可以是T。

```
复制代码public interface AuthService {

    @POST("v2/login")
    @FormUrlEncoded
    LoginResponse login(@Field("username") String username, @Field("password") String password);
}

```

这时已经意识到可以json解析的过程交给retrofit框架。

```
复制代码implementation(‘com.squareup.retrofit2:converter-gson:2.8.1’)

```

加了个gson转换器的依赖，对吧？

### 新手

后来，为了满足对更高逼格的追求，返回值直接跟OkHttp的Call结合，然后使用enqueue的方式进行请求，于是就变成了Call<T>。

```
复制代码public interface AuthService {

    @POST("v3/login")
    @FormUrlEncoded
    Call<LoginResponse> login(@Field("username") String username, @Field("password") String password);
}

```

### 初级

再后来，发现市面上RxJava的热度突然飙升，于是乎，就开始研究起了RxJava，这时候，功力开始有所长进。

```
复制代码public interface AuthService {

    @POST("v4/login")
    @FormUrlEncoded
    Observable<LoginResponse> login(@Field("username") String username, @Field("password") String password);
}

```

这时你可能就需要依赖这几个库了，版本号偏高暂且不去计较，也有可能用的是rxjava第一代。

```
复制代码implementation ‘com.squareup.retrofit2:adapter-rxjava2:2.8.1’
implementation ‘io.reactivex.rxjava2:rxjava:2.0.1’
implementation ‘io.reactivex.rxjava2:rxandroid:2.0.1’

```

### 中级

随着Kotlin的兴起，市面上对网络请求的写法也是大相径庭，网络框架也开始演变出自己的风格，甚至有些公司自己封装网络请求库，没有什么问题啊，反正主要思路就是动态代理。百花齐放的时代来临。

```
复制代码interface AuthService {

    @POST("v5/login")
    fun login(@Body body: RequestBody): Observable<BaseResponse<LoginUser>>
}

```

为了追求更加新颖的写法，将@Field换成了@Body，返回值模型增加了公共的code、msg等。

### 高级一阶

由于经验逐渐变得丰富，你开始使用Kotlin的协程，因为你对更牛逼技术的追求一直没有停止过。

```
复制代码interface AuthService {

    @POST("v6/login")
    suspend fun login(@Body body: RequestBody): BaseResponse<LoginUser>
}

```

这个时候retrofit的写法就已经进入到了第6代，你问为什么是第6代？这个不是重点，我编的。你直接将API接口中定义的函数变成了suspend函数，方便在协程作用域发起。同时你去掉了Observable这个RxJava的产物，返回值又回到了最初的状态。你不禁感慨，从哪里来，到哪里去。返璞归真了！

### 高级二阶

你以为到这就结束了？随着Flow的问世，网络请求就进入到了第七世代。Flow是基于协程的产物，可以不用挂起函数了。而且Flow具备RxJava的优良特性，可以对数据流进行变换，也可以监听函数执行的生命周期。这样就方便添加显示加载中对话框和隐藏加载中对话框，以及加载进度了。

```
复制代码interface AuthService {

    @POST("v7/login")
    fun login(@Body body: RequestBody): Flow<BaseResponse<LoginUser>>
}

```

### dcache框架如何支持协程和Flow

我的dcache框架1.x的稳定版本，不支持flow。

```
kts复制代码implementation("com.github.dora4:dcache-android:1.8.5")

```

你需要使用2.0.12及以上版本，对flow请求有很好的支持。

```
复制代码implementation("com.github.dora4:dcache-android:2.0.12")

```

接下来我们简单阅读下DoraHttp.kt的源代码。

```
复制代码/**
 * 将一个普通的api接口包装成Flow返回值的接口。
 */
suspend fun <T> flowResult(requestBlock: suspend () -> T,
                           loadingBlock: ((Boolean) -> Unit)? = null,
                           errorBlock: ((String) -> Unit)? = null,
) : Flow<T> {
    return flow {
        // 设置超时时间为10秒
        val response = withTimeout(10 * 1000) {
            requestBlock()
        }
        emit(response)
    }
        .flowOn(Dispatchers.IO)
        .onStart {
            loadingBlock?.invoke(true)
        }
        .catch { e ->
            errorBlock?.invoke(e.toString())
        }
        .onCompletion {
            loadingBlock?.invoke(false)
        }
}

```

这个函数建议在net作用域内执行，net协程作用域的定义请参见DoraHttp.kt的详细源代码，[github.com/dora4/dcach…](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fdora4%2Fdcache-android%2Fblob%2Fmaster%2Fdcache%2Fsrc%2Fmain%2Fjava%2Fdora%2Fhttp%2FDoraHttp.kt) 。高阶函数的block参数定义中，如果加suspend关键字，则可以传入suspend块，也可以传入普通的方法块。如果不加suspend关键字，则只能传入普通方法块。这个函数对应第6代的写法，可以翻看前面的内容。Flow<T>最终调用collect {} 来处理业务逻辑。

```
kotlin复制代码
/**
 * 直接发起Flow请求，如果你使用框架内部的[dora.http.retrofit.RetrofitManager]的话，需要开启
 * [dora.http.retrofit.RetrofitManager]的flow配置选项[dora.http.retrofit.RetrofitManager.Config.useFlow]
 * 为true。
 */
suspend fun <T> flowRequest(requestBlock: () -> Flow<T>,
                            successBlock: ((T) -> Unit),
                            failureBlock: ((String) -> Unit)? = null,
                            loadingBlock: ((Boolean) -> Unit)? = null
) {
    requestBlock()
        .flowOn(Dispatchers.IO)
        .onStart {
            loadingBlock?.invoke(true)
        }
        .catch { e ->
            failureBlock?.invoke(e.toString())
        }
        .onCompletion {
            loadingBlock?.invoke(false)
        }.collect {
            successBlock(it)
        }
}

```

这个源码对应第7代的写法。