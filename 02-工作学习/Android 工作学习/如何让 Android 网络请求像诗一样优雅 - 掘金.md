# 如何让 Android 网络请求像诗一样优雅 - 掘金

[https://juejin.cn/post/7266768708139434045](https://juejin.cn/post/7266768708139434045)

在 Android 应用开发中，网络请求必不可少，如何去封装才能使自己的请求代码显得更加简洁优雅，更加方便于以后的开发呢？这里利用 Kotlin 的函数式编程和 Retrofit 来从零开始封装一个网络请求框架，下面就一起来瞧瞧吧！

首先，引入网络请求框架的依赖。

```
复制代码implementation 'com.squareup.okhttp3:okhttp:4.9.1'
implementation 'com.squareup.okhttp3:logging-interceptor:4.9.1'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

```

## 定义拦截器

我们可以先自定义一些拦截器，对一些公共提交的字段做封装，比如 token。在服务器注册成功或者登录成功之后获取 token，过期之后便无法正常请求接口，所以需要在请求接口时判断 token 是否过期，由于接口众多，不可能每个接口都进行判断，所以需要全局设置一个拦截器判断 token。

```
复制代码class TokenInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 当前拦截器中收到的请求对象
        val request = chain.request()
        // 执行请求
        var response = chain.proceed(request)
        if (response.body == null) {
            return response
        }
        val mediaType = response.body!!.contentType() ?: return response
        val type = mediaType.toString()
        if (!type.contains("application/json")) {
            return response
        }
        val result = response.body!!.string()
        var code = ""
        try {
            val jsonObject = JSONObject(result)
            code = jsonObject.getString("code")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // 重新构建 response
        response = response.newBuilder().body(result.toResponseBody(null)).code(200).build()
        if (isTokenExpired(code)) {
            // token 过期，需要获取新的 token
            val newToken = getNewToken() ?: return response
            // 重新构建新的 token 请求
            val builder = request.url.newBuilder().setEncodedQueryParameter("token", newToken)
            val newRequest = request.newBuilder().method(request.method, request.body)
                .url(builder.build()).build()
            return chain.proceed(newRequest)
        }
        return response
    }

    // 判断 token 是否过期
    private fun isTokenExpired(code: String) =
        TextUtils.equals(code, "401") || TextUtils.equals(code, "402")

    // 刷新 token
    private fun getNewToken() = ServiceManager.instance.refreshToken()

}

```

这里是 token 过期之后直接重新请求接口获取新的 token，这需要根据具体业务需求来，有些可能是过期之后跳转到登录页面，让用户重新登录等等。

我们还可以再定义一个拦截器，全局添加 token。

```
复制代码class TokenHeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val headers = request.headers
        var token = headers["token"]
        if (TextUtils.isEmpty(token)) {
            token = ServiceManager.instance.getToken()
            request = request.newBuilder().addHeader("token", token).build()
        }
        return chain.proceed(request)
    }

}

```

## 创建 retrofit

```
复制代码class RetrofitUtil {

    companion object {

        private const val TIME_OUT = 20L

        private fun createRetrofit(): Retrofit {

            // OkHttp 提供的一个拦截器，用于记录和查看网络请求和响应的日志信息。
            val interceptor = HttpLoggingInterceptor()
            // 打印请求和响应的所有内容，响应状态码和执行时间等等。
            interceptor.level = HttpLoggingInterceptor.Level.BODY

            val okHttpClient = OkHttpClient().newBuilder().apply {
                addInterceptor(interceptor)
                addInterceptor(TokenInterceptor())
                addInterceptor(TokenHeaderInterceptor())
                retryOnConnectionFailure(true)
                connectTimeout(TIME_OUT, TimeUnit.SECONDS)
                writeTimeout(TIME_OUT, TimeUnit.SECONDS)
                readTimeout(TIME_OUT, TimeUnit.SECONDS)
            }.build()

            return Retrofit.Builder().apply {
                addConverterFactory(GsonConverterFactory.create())
                baseUrl(ServiceManager.instance.baseHttpUrl)
                client(okHttpClient)
            }.build()

        }

        fun <T> getAPI(clazz: Class<T>): T {
            return createRetrofit().create(clazz)
        }

    }
}

```

## 网络请求封装

定义通用基础请求返回的数据结构

```
复制代码private const val SERVER_SUCCESS = "200"

data class BaseResp<T>(val code: String, val message: String, val data: T)

fun <T> BaseResp<T>?.isSuccess() = this?.code == SERVER_SUCCESS

```

请求状态流程封装，可以根据具体业务流程实现方法。

```
复制代码class RequestAction<T> {

    // 开始请求
    var start: (() -> Unit)? = null
        private set

    // 发起请求
    var request: (suspend () -> BaseResp<T>)? = null
        private set

    // 请求成功
    var success: ((T?) -> Unit)? = null
        private set

    // 请求失败
    var error: ((String) -> Unit)? = null
        private set

    // 请求结束
    var finish: (() -> Unit)? = null
        private set

    fun request(block: suspend () -> BaseResp<T>) {
        request = block
    }

    fun start(block: () -> Unit) {
        start = block
    }

    fun success(block: (T?) -> Unit) {
        success = block
    }

    fun error(block: (String) -> Unit) {
        error = block
    }

    fun finish(block: () -> Unit) {
        finish = block
    }

}

```

因为网络请求都是在 ViewModel 中进行的，我们可以定义一个 ViewModel 的扩展函数，用来处理网络请求。

```
kotlin复制代码fun <T> ViewModel.netRequest(block: RequestAction<T>.() -> Unit) {

    val action = RequestAction<T>().apply(block)

    viewModelScope.launch {
        try {
            action.start?.invoke()
            val result = action.request?.invoke()
            if (result.isSuccess()) {
                action.success?.invoke(result!!.data)
            } else {
                action.error?.invoke(result!!.message)
            }
        } catch (ex: Exception) {
            // 可以做一些定制化的返回错误提示
            action.error?.invoke(getErrorTipContent(ex))
        } finally {
            action.finish?.invoke()
        }
    }

}

```

```
kotlin复制代码private const val SERVER_ERROR = "HTTP 500 Internal Server Error"
private const val HTTP_ERROR_TIP = "服务器或者网络连接错误"

fun getErrorTipContent(ex: Throwable) = if (ex is ConnectException || ex is UnknownHostException
    || ex is SocketTimeoutException || SERVER_ERROR == ex.message.toString()
) HTTP_ERROR_TIP else ex.message.toString()

```

## 使用案例

定义网络请求接口

```
kotlin复制代码interface HttpApi {

    @GET("/exampleA/exampleP/exampleI/exampleApi/getNetData")
    suspend fun getNetData(@QueryMap params: HashMap<String, String>): BaseResp<NetDataBean>

    @GET("/exampleA/exampleP/exampleI/exampleApi/getTestData")
    suspend fun getTestData(
        @Query("param1") param1: String,
        @Query("param2") param2: String
    ): BaseResp<NetDataBean>

    @GET("/exampleA/exampleP/exampleI/exampleApi/{id}")
    fun getNetTask(
        @Path("id") id: String,
        @QueryMap params: HashMap<String, String>,
    ): Call<BaseResp<TaskBean>>

    @FormUrlEncoded
    @POST("/exampleA/exampleP/exampleI/exampleApi/confirm")
    suspend fun confirm(@Field("id") id: String, @Field("token") token: String): BaseResp<String>

    @FormUrlEncoded
    @POST("/exampleA/exampleP/exampleI/exampleApi/upload")
    suspend fun upload(@FieldMap params: Map<String, String>): BaseResp<String>

}

```

我们可以写一个网络请求帮助类，用于请求的创建。

```
kotlin复制代码class RequestHelper {

    private val httpApi = RetrofitUtil.getAPI(HttpApi::class.java)

    companion object {
        val instance: RequestHelper by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            RequestHelper()
        }
    }

    suspend fun getNetData(params: HashMap<String, String>) = httpApi.getNetData(params)

    suspend fun getTestData(branchCode: String, token: String) =
        httpApi.getTestData(branchCode, token)

    suspend fun getNetTask(id: String, params: HashMap<String, String>) =
        httpApi.getNetTask(id, params)

    suspend fun confirm(id: String, token: String) = httpApi.confirm(id, token)

    suspend fun upload(params: HashMap<String, String>) = httpApi.upload(params)

}

```

定义用户的意图和 UI 状态

```
kotlin复制代码// 定义用户意图
sealed class MainIntent {
    object FetchData : MainIntent()
}

// 定义 UI 状态
sealed class MainUIState {
    object Loading : MainUIState()
    data class NetData(val data: NetDataBean?) : MainUIState()
    data class Error(val error: String?) : MainUIState()
}

```

ViewModel 中做意图的处理和 UI 状态的变更，根据网络请求结果传递不同的状态，使用定义的扩展方法去执行网络请求，封装过后的网络请求就很简洁方便了，下面演示下具体使用。

```
kotlin复制代码class MainViewModel : ViewModel() {

    val mainIntent = Channel<MainIntent>(Channel.UNLIMITED)

    private val _mainUIState = MutableStateFlow<MainUIState>(MainUIState.Loading)
    val mainUIState: StateFlow<MainUIState>
        get() = _mainUIState

    init {
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collect {
                if (it is MainIntent.FetchData) {
                    getNetDataResult()
                }
            }
        }
    }
    // 使用
    private fun getNetDataResult() = netRequest {
        start { _mainUIState.value = MainUIState.Loading }
        request {
            val paramMap = hashMapOf<String, String>()
            paramMap["param1"] = "param1"
            paramMap["param2"] = "param2"
            RequestHelper.instance.getNetData(paramMap)
        }
        success { _mainUIState.value = MainUIState.NetData(it) }
        error { _mainUIState.value = MainUIState.Error(it) }
    }

}

```

这样是不是看起来很简洁呢？接下来，Activity 负责发送意图和接收 UI 状态进行相关的处理就行啦！

```
kotlin复制代码class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        initData()
        observeViewModel()
    }

    private fun initData() {
        lifecycleScope.launch {
            // 发送意图
            viewModel.mainIntent.send(MainIntent.FetchData)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.mainUIState.collect {
                when (it) {
                    is MainUIState.Loading -> showLoading()
                    // 这里拿到网络请求返回的数据，根据业务自行操作，这里只做简单的显示。
                    is MainUIState.NetData -> showText(it.data.toString())
                    is MainUIState.Error -> showText(it.error)
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.netText.visibility = View.GONE
    }

    private fun showText(result: String?) {
        binding.progressBar.visibility = View.GONE
        binding.netText.visibility = View.VISIBLE
        binding.netText.text = result
    }

}

```

## 文件的上传与下载

如果是文件的上传和下载呢？其实文件还不太一样，这涉及到上传进度，文件的处理等方面，所以，为了方便开发使用，我们可以针对文件单独再做一下封装。

定义文件上传对象

```
kotlin复制代码data class UpLoadFileBean(val file: File, val fileKey: String)

```

自定义 RequestBody，从中获取上传进度。

```
kotlin复制代码class ProgressRequestBody(
    private var requestBody: RequestBody,
    var onProgress: ((Int) -> Unit)?,
) : RequestBody() {

    private var bufferedSink: BufferedSink? = null

    override fun contentType(): MediaType? = requestBody.contentType()

    override fun contentLength(): Long {
        return requestBody.contentLength()
    }

    override fun writeTo(sink: BufferedSink) {
        if (bufferedSink == null) bufferedSink = createSink(sink).buffer()
        bufferedSink?.let {
            requestBody.writeTo(it)
            it.flush()
        }
    }

    private fun createSink(sink: Sink): Sink = object : ForwardingSink(sink) {
        // 当前写入字节数
        var bytesWritten = 0L

        // 总字节长度
        var contentLength = 0L

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)

            if (contentLength == 0L) {
                contentLength = contentLength()
            }

            // 增加当前写入的字节数
            bytesWritten += byteCount

            CoroutineScope(Dispatchers.Main).launch {
                // 进度回调
                onProgress?.invoke((bytesWritten * 100 / contentLength).toInt())
            }
        }
    }

}

```

创建 MultipartBody.Part

```
kotlin复制代码fun <T> createPartList(action: UpLoadFileAction<T>): List<MultipartBody.Part> =
    MultipartBody.Builder().apply {
        // 公共参数 token
        addFormDataPart("token", ServiceManager.instance.getToken())

        // 其他基本参数
        action.params?.forEach {
            if (it.key.isNotBlank() && it.value.isNotBlank()) {
                addFormDataPart(it.key, it.value)
            }
        }

        // 文件校验
        action.fileData?.let {
            addFormDataPart(
                it.fileKey, it.file.name, ProgressRequestBody(
                    requestBody = it.file
                    .asRequestBody("application/octet-stream".toMediaTypeOrNull()),
                    onProgress = action.progress
                )
            )
        }
    }.build().parts

```

定义文件上传行为

```
kotlin复制代码class UpLoadFileAction<T> {

    // 请求体
    lateinit var request: (suspend () -> BaseResp<T>)
        private set

    lateinit var parts: List<MultipartBody.Part>

    // 其他普通参数
    var params: HashMap<String, String>? = null
        private set

    // 文件参数
    var fileData: UpLoadFileBean? = null
        private set

    // 初始化参数
    fun init(params: HashMap<String, String>?, fileData: UpLoadFileBean?) {
        this.params = params
        this.fileData = fileData
        parts = createPartList(this)
    }

    var start: (() -> Unit)? = null
        private set

    var success: (() -> Unit)? = null
        private set

    var error: ((String) -> Unit)? = null
        private set

    var progress: ((Int) -> Unit)? = null
        private set

    var finish: (() -> Unit)? = null
        private set

    fun start(block: () -> Unit) {
        start = block
    }

    fun success(block: () -> Unit) {
        success = block
    }

    fun error(block: (String) -> Unit) {
        error = block
    }

    fun progress(block: (Int) -> Unit) {
        progress = block
    }

    fun finish(block: () -> Unit) {
        finish = block
    }

    fun request(block: suspend () -> BaseResp<T>) {
        request = block
    }

}

```

同样，定义 ViewModel 的扩展函数，用来执行文件上传。

```
kotlin复制代码fun <T> ViewModel.upLoadFile(
    block: UpLoadFileAction<T>.() -> Unit,
    params: HashMap<String, String>?,
    fileData: UpLoadFileBean?,
) = viewModelScope.launch {
    val action = UpLoadFileAction<T>().apply(block)
    try {
        action.init(params, fileData)
        action.start?.invoke()
        val result = action.request.invoke()
        if (result.isSuccess()) {
            action.success?.invoke()
        } else {
            action.error?.invoke(result.message)
        }
    } catch (ex: Exception) {
        action.error?.invoke(getErrorTipContent(ex))
    } finally {
        action.finish?.invoke()
    }
}

```

定义文件上传接口

```
kotlin复制代码interface HttpApi {
    //...

    @Multipart
    @POST("/exampleA/exampleP/exampleI/exampleApi/uploadFile")
    suspend fun uploadFile(@Part partLis: List<MultipartBody.Part>): BaseResp<String>

}

```

在 RequestHelper 中定义上传文件方法

```
kotlin复制代码class RequestHelper {

    private val httpApi = RetrofitUtil.getAPI(HttpApi::class.java)

    companion object {
        val instance: RequestHelper by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            RequestHelper()
        }
    }

    //...

    suspend fun uploadFile(partList: List<MultipartBody.Part>) = httpApi.uploadFile(partList)

}

```

封装过后的文件上传就很简洁方便了，下面演示下具体使用。

```
kotlin复制代码private fun uploadMyFile() = upLoadFile(
    params = hashMapOf("param1" to "param1", "param2" to "param2"),
    fileData = UpLoadFileBean(File(absoluteFilePath), "file"),
) {
    start {
        // TODO: 开始上传，此处可以显示加载动画
    }
    request { RequestHelper.instance.uploadFile(parts) }
    success {
        // TODO: 上传成功
    }
    error {
        // TODO: 上传失败
    }
    finish {
        // TODO: 上传结束，此处可以关闭加载动画
    }
}

```

既然上传文件都有了，那怎么少得了下载呢？其实，下载比上传更简单，下面就来写一下，同样利用了 kotlin 的函数式编程，我们添加 ViewModel 的扩展函数，需要注意的是，由于这边是直接使用 OkHttp 的同步请求，所以把这部分代码放在了 IO 线程中。

```
kotlin复制代码fun ViewModel.downLoadFile(
    downLoadUrl: String,
    dirPath: String,
    fileName: String,
    progress: ((Int) -> Unit)?,
    success: (File) -> Unit,
    failed: (String) -> Unit,
) = viewModelScope.launch(Dispatchers.IO) {
    try {
        val fileDir = File(dirPath)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }
        val downLoadFile = File(fileDir, fileName)
        val request = Request.Builder().url(downLoadUrl).get().build()
        val response = OkHttpClient.Builder().build().newCall(request).execute()
        if (response.isSuccessful) {
            response.body?.let {
                val totalLength = it.contentLength().toDouble()
                val stream = it.byteStream()
                stream.copyTo(downLoadFile.outputStream()) { currentLength ->
                    // 当前下载进度
                    val process = currentLength / totalLength * 100
                    progress?.invoke(process.toInt())
                }
                success.invoke(downLoadFile)
            } ?: failed.invoke("response body is null")
        } else failed.invoke("download failed：$response")
    } catch (ex: Exception) {
        failed.invoke("download failed：${getErrorTipContent(ex)}")
    }
}

// InputStream 添加扩展函数，实现字节拷贝。
private fun InputStream.copyTo(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    progress: (Long) -> Unit,
): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
        progress(bytesCopied)
    }
    return bytesCopied
}

```

然后，使用就会变得很简洁了，如下所示：

```
fun downloadMyFile(downLoadUrl: String, dirPath: String, fileName: String) =
    downLoadFile(
        downLoadUrl = downLoadUrl,
        dirPath = dirPath,
        fileName = fileName,
        progress = {
            // TODO: 这里可以拿到进度
        },
        success = {
            // TODO: 下载成功，拿到下载的文件对象 File
        },
        failed = {
            // TODO: 下载失败，返回原因
        }

    )

```