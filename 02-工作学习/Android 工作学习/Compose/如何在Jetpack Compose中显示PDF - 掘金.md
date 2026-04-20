# 如何在Jetpack Compose中显示PDF? - 掘金

[https://juejin.cn/post/7298918307746660390](https://juejin.cn/post/7298918307746660390)

## 当读取和显示 PDF 的组件缺失时该怎么办? 声明式编程可以拯救你.

Jetpack Compose已经存在好几年了, 但***在某些方面它的使用仍然面临挑战***. 例如, 缺少用于查看PDF的官方组件, 而为数不多的第三方库通常也是有代价的.

在我们的应用中, 我们会遇到在许多场景中显示 PDF 的需求. 比如, 我们提供的每项服务都有自己的条款和条件, 每月的费用报告也以 PDF 文档的形式分发.

因此, 我们萌生了这样一个的想法, **创建自定义库, 以便使用 Compose直接查看 PDF**. 这样, 我们就可以绕过使用 `WebView` 或通过互操作性集成依赖于经典视图的库.

在本文中, 我们将探讨如何利用 Android SDK 中的 `PdfRenderer` 类在 Compose 框架内渲染和查看 PDF 页面.

## `PdfRenderer`类

`PdfRenderer`类是`android.graphics.pdf`包的一部分, 可用于渲染**PDF**页面并获取`Bitmap`来显示.

让我们来看看该类及其方法:

```
复制代码public final class PdfRenderer implements AutoCloseable {

   public PdfRenderer(
      @NonNull ParcelFilDescriptor input
   ) throws IOEception

   public void close()

   public int getPageCount()

   public Page openPage(int index)

   public final class Page implements AutoCloseable {
      public static final int RENDER_MODE_FOR_DISPLAY = 1;
      public static final int RENDER_MODE_FOR_PRINT = 2;

      private Page()

      public int getIndex()

      public int getWidth()

      public int getHeight()

      public void render(
          @NonNull Bitmap destination,
          @Nullable Rect destClip,
          @Nullable Matrix transform,
          int renderMode
      )
   }
}

```

第一个方法是类的构造函数, 我们可以看到它需要一个`ParcelFileDescriptor`类的实例.

下面的三个方法是:

- `close()`: 该方法用于关闭已使用过的渲染, 关闭渲染对避免内存泄漏至关重要, 但必须谨慎, 因为关闭渲染后, 将无法在该实例上调用其他方法.
- `getPageCount()`: 方法签名本身不言自明, 它返回 PDF 的页数.
- `openPage(int index)`: 该方法返回与所传索引相对应的页面类实例.

现在让我们来看看 `Page` 类:

- 正如我们所猜测的, `getWidth()` 和 `getHeight()`方法分别以**像素**为单位返回当前页面的宽度和高度.
- `getIndex()` 方法只是返回当前页面的索引.
- `close()`方法用于在完成对该实例的操作后关闭页面, 需要注意的是, 一次只能打开一个页面, 因此在打开另一个页面之前, 需要调用该方法关闭当前页面实例.

`render()`方法值得进一步研究, 它是将页面渲染为`Bitmap`的主要方法.

该方法的输入参数如下:

- 目标 `Bitmap`, 该方法将在此实例上加载页面渲染.
- `destClip`: 一个`Rect`类的实例, 是一个可选参数, 如果你只想在目标Bitmap的一部分上渲染页面, 它可能会很有用.
- `transform`: 是`Matrix`类的一个实例, 是一个可选参数, 用于实现以点表示的页面坐标和以**像素**表示的Bitmap坐标之间的转换.
- `renderMode`是一个整数, 表示渲染模式, 有两种模式, 它们是`Page`类中定义的两个常量值: `RENDER_MODE_FOR_DISPLAY`和`RENDER_MODE_FOR_PRINT`. 在本例中, 我们将只使用第一种模式.

要知道的一个重要细节是, 如果没有传递`destClip`和`transform`参数, 页面将被渲染以占据整个**目标Bitmap**, 这就清楚地表明, 如果我们想避免页面变形, 目标Bitmap的宽度和高度之间的比例必须与要渲染的页面相匹配.

正如我们稍后将看到的, `getWidth()` 和 `getHeight()`方法在这方面将非常有用.

更多详情, 请参阅官方文档:

[developer.android.com/reference/a…](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.com%2Freference%2Fandroid%2Fgraphics%2Fpdf%2FPdfRenderer.Page)

# **第一个实现**

让我们尝试制定第一个实现方案, 其背后的想法是创建一个类, 该类能够在给定`ParcelFileDescriptor`作为输入的情况下, 提供`Bitmap`作为输出的列表.

然后, 我们将使用`LazyColumn`和Composable的`Image`在垂直列表中显示每个`Bitmap`.

为了清楚起见, 我们将把这个类称为`PdfRender`, 其使用方法如下:

```
复制代码@Composable
fun PDFReader(file: File) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val pdfReader = PdfRender(
            fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        )
        LazyColumn {
            items(count = pdfRender.pageCount) {
                Image(
                    bitmap = pdfRender.pageLists[index].pageContent.asImageBitmap(),
                    contentDescription = "Psd page number: $index"
                )
            }
        }
    }
}

```

代码是自解释的, 但总结一下, 我们可以看到首先创建了一个 `ParcelFileDescriptor` 实例, 然后将其传递给 `PdfRender` 类, 该类将显示 pdf 中包含的页数和 `Bitmap` 列表.

此时, 如前所述, 会创建一个`LazyColumn`, 并在其中遍历`Bitmap`列表, 调用每个元素的可合成图像.

现在让我们看看`PdfRender`类的实现:

```
复制代码internal class PdfRender(
    private val fileDescriptor: ParcelFileDescriptor
) {
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val pageCount get() = pdfRenderer.pageCount

    val pageLists: List<Page> = List(pdfRenderer.pageCount) {
        Page(
            index = it,
            pdfRenderer = pdfRender
        )
    }

    fun close() {
        pageLists.forEach {
            it.recycle()
        }
        pdfRenderer.close()
        fileDescriptor.close()
    }

    class Page(
        val index: Int,
        val pdfRenderer: PdfRenderer
    ) {
        val pageContent = createBitmap()

        private fun createBitmap(): Bitmap {
            val newBitmap: Bitmap
            pdfRenderer.openPage(index).use {
                newBitmap = createBlankBitmap(
                    width = currentPage.width,
                    height = currentPage.height
                )
                currentPage.render(
                    newBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
            }
            return newBitmap
        }

        fun recycle() {
            pageContent.recycle()
        }

        private fun createBlankBitmap(
            width: Int,
            height: Int
        ): Bitmap {
            return createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawBitmap(this, 0f, 0f, null)
            }
        }
    }
}

```

首先, 创建一个`PdfRenderer`类的实例, 并通过`pageCount`字段显示PDF的页数, 该字段直接引用`PdfRenderer`类的`pageCount`字段.

随后, 创建了`Page`类的实例列表和`close()`函数, 该函数可用于释放已使用的内存并关闭`PdfRenderer`和`ParcelFileDescriptor`.

分析`Page`类, 我们可以看到它是如何将两个参数作为输入的, 即`PdfRenderer`实例和页面索引.

在创建每个实例时, `pageContent` 字段通过调用`createBitmap()`函数来填充.

在该函数的执行过程中, 第一步是通过`openPage`方法打开当前页面, 通过该方法提供的实例, 我们可以获取当前页面的大小, 然后创建一个与页面大小相同的空`Bitmap`, 并将其传递给当前页面实例的 `render` 方法.

该方法会渲染页面, 并将渲染结果填充到传入的`Bitmap`中.

# **让我们看看最终结果**

在这个测试中, 我使用了一个超过 200MB 的超大 PDF 文件来说明渲染时间是如何过长的. 显然, 我们无法忍受这种性能水平. 此外, 在使用 Android Studio 剖析器分析应用程序时, 我们发现内存消耗也过大.

对于一个只打开 PDF 的应用程序来说, 近 6GB 的内存消耗是相当大的. 性能不佳的原因很简单: **所有页面都是在打开时渲染的**, 所有创建的Bitmap即使在不显示时也会保留在内存中.

然而, 要找到解决这个问题的办法比想象的要复杂得多. 事实上, 我们不可能并行渲染每个页面以减少加载时间, 因为如前所述, **`PdfRenderer`类不允许我们同时打开多个页面**.

此外, 我们还需要设计一种方法, 只渲染当前显示的页面, 最多也只渲染紧随其后的页面, 并将其保留在内存中.

另一个可能不会立即显现的问题是用户界面冻结; 事实上, 在当前的实现中, 用户界面一直冻结到所有页面加载和渲染完毕.

# **协程前来拯救**

要解决用户界面冻结和加载时间的问题, 一种实用的方法是使用例程将渲染过程转移到异步线程中. 不过, 虽然转移到并行线程可以解决前两个问题, 但却会带来**新的挑战**:

- 调用Composable Image时, Bitmap可能尚未渲染.
- 然而, 渲染图像必须按顺序进行, 因为一次只能打开一个页面. 尽管如此, 如果一个页面已渲染完成, 我们希望立即显示它, 而无需等待其他页面的渲染.

为了解决Compose中文件的创建与页面渲染之间的异步性问题, 我们可以使用 `StateFlow`. 它允许我们在Compose应用中使用 `collectAsState` 方法, 因此当一个新值发布时, Composable应用也会随之更新.

因此, 我们的想法是将`Page`类的`pageContent`字段从一个简单的`Bitmap`转化为一个`MutableStateFlow<Bitmap?>`. 因此, 在Compose应用中, 只有当状态流中填充了`Bitmap`时, 我们才会调用`Image`函数.

让我们看看示例代码:

```
复制代码page.pageContent.collectAsState().value?.asImageBitmap()?.let {
    Image(
        bitmap = it,
        contentDescription = "Pdf page number: $index"
    )
}

```

至于渲染的顺序问题, 由于我们将其执行转移到了一个协程中, 最直接的解决方案就是使用`Mutex`来确保同一时间只有一个页面被渲染.

```
kotlin复制代码class Page(
    val mutex: Mutex,
    val index: Int,
    val pdfRenderer: PdfRenderer,
    val coroutineScope: CoroutineScope
) {
    var isLoaded = false

    var job: Job? = null

    val pageContent = MutableStateFlow<Bitmap?>(null)

    fun load() {
        if (!isLoaded) {
            job = coroutineScope.launch {
                mutex.withLock {
                    val newBitmap: Bitmap
                    pdfRenderer.openPage(index).use { currentPage ->
                        newBitmap = createBlankBitmap(
                            width = currentPage.width,
                            height = currentPage.height
                        )
                        currentPage.render(
                            newBitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                    }
                    isLoaded = true
                    pageContent.emit(newBitmap)
                }
            }
        }
    }

    fun recycle() {
        isLoaded = false
        val oldBitmap = pageContent.value
        pageContent.tryEmit(null)
        oldBitmap?.recycle()
    }

    private fun createBlankBitmap(
        width: Int,
        height: Int
    ): Bitmap {
        return createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            canvas.drawBitmap(this, 0f, 0f, null)
        }
    }
}

```

从这个代码片段中, 我们可以看到我们是如何将页面渲染转移到一个名为`load()`的函数中的, 在这个函数中, 我们在一个`CoroutineScope`上启动了一个协程, 这个`CoroutineScope`是由`PdfRender`类提供的, 因此它对于所有`Page`实例来说都是唯一的.

然后, 在`whitLock`方法的回调中执行渲染, 确保一次只打开, 渲染一个页面, 然后在`MutableStateFlow pageContent`上输出.

此时, 我们已经解决了异步问题, 但性能问题依然存在. 事实上, 即使我们现在可以将渲染转移到一个协程中, 但事实依然是, 在打开时渲染所有页面会浪费计算资源和内存.

不过, 在这一点上, 只要每个 `Page` 实例的 `load()` 方法只在该页面必须出现在屏幕上时才被调用, 解决方案就会立竿见影.

为此, 我们可以使用**Compose**提供的组件或`SideEffect`.

```
ini复制代码@Composable
fun PDFReader(file: File) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val pdfRender = PdfRender(
            fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        )
        LazyColumn {
            items(count = pdfRender.pageCount) { index ->
                val page = pdfRender.pageLists[index]
                LaunchedEffect(key1 = Unit) {
                    page.load()
                }
                page.pageContent.collectAsState().value?.asImageBitmap()?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Pdf page number: $index"
                    )
                }
            }
        }
    }
}

```

从代码片段中可以看到, 我们添加了一个`LaunchedEffect`, 并将`key1`参数设置为`Unit`.

当一个页面从屏幕上消失时, 它的Composable元素将离开组合, 然后在它回来时被重新创建, 然后`LaunchedEffect`将再次被执行.

然而, 如果用户反复滚动列表并交替滚动方向, 这可能会导致无用的重复页面渲染.

在前面展示的`Page`类的实现中已经有了解决方案, 事实上, `isLoaded`变量在渲染完成后被设置为true, 并在`load()`函数启动时进行检查, 如果变量结果为true, 则不会再次渲染.

在`Page`的实现中, 我们已经可以看到变量`isLoaded`是如何在`recycle()`方法中设置为false的. 我们将在下一章看到它, 与此同时, 让我们看看`PdfRender`类的当前实现和我们得到的结果.

```
kotlin复制代码internal class PdfRender(
    private val fileDescriptor: ParcelFileDescriptor
) {
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val pageCount get() = pdfRenderer.pageCount
    private val mutex: Mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val pageLists: List<Page> = List(pdfRenderer.pageCount) {
        Page(
            index = it,
            pdfRenderer = pdfRender,
            coroutineScope = coroutineScope,
            mutex = mutex
        )
    }

    fun close() {
        pageLists.forEach {
            it.recycle()
        }
        pdfRenderer.close()
        fileDescriptor.close()
    }

    class Page(
        val mutex: Mutex,
        val index: Int,
        val pdfRenderer: PdfRenderer,
        val coroutineScope: CoroutineScope
    ) {
        var isLoaded = false

        var job: Job? = null

        val pageContent = MutableStateFlow<Bitmap?>(null)

        fun load() {
            if (!isLoaded) {
                job = coroutineScope.launch {
                    mutex.withLock {
                        val newBitmap: Bitmap
                        pdfRenderer.openPage(index).use { currentPage ->
                            newBitmap = createBlankBitmap(
                                width = currentPage.width,
                                height = currentPage.height
                            )
                            currentPage.render(
                                newBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                        }
                        isLoaded = true
                        pageContent.emit(newBitmap)
                    }
                }
            }
        }

        fun recycle() {
            isLoaded = false
            val oldBitmap = pageContent.value
            pageContent.tryEmit(null)
            oldBitmap?.recycle()
        }

        private fun createBlankBitmap(
            width: Int,
            height: Int
        ): Bitmap {
            return createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawBitmap(this, 0f, 0f, null)
            }
        }
    }
}

```

在启动时间方面, 结果非常显著.

但如果我们看看耗用的内存, 就会发现情况并没有改变.

我们可以看到, 与之前不同的是, 所有内存并没有立即被占用, 这是由于实现了异步加载. 不过, 虽然我们可能会认为只有屏幕上可见的页面才会停止加载, 但实际上, 正如我们所看到的, 所有页面都会不加区分地停止加载. 原因很简单, 在创建 `LazyColumn` 时, 所有元素都没有尺寸, 因此必须将它们全部显示出来, 因此`load()`函数会在列表的所有元素上启动.

# **让我们优化内存的使用**

为了更好地管理内存, 我们需要实施两种新机制:

- 在渲染之前, 给所有页面一个初始大小, 这样`load()`方法只针对当时实际显示的页面.
- 当页面不再显示时, 调用`recycle()`方法来释放内存.

让我们从第一点开始:

```
kotlin复制代码internal class PdfRender(
    private val fileDescriptor: ParcelFileDescriptor
) {
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val pageCount get() = pdfRenderer.pageCount
    private val mutex: Mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val pageLists: List<Page> = List(pdfRenderer.pageCount) {
        Page(
            index = it,
            pdfRenderer = pdfRender,
            coroutineScope = coroutineScope,
            mutex = mutex
        )
    }

    fun close() {
        pageLists.forEach {
            it.recycle()
        }
        pdfRenderer.close()
        fileDescriptor.close()
    }

    class Page(
        val mutex: Mutex,
        val index: Int,
        val pdfRenderer: PdfRenderer,
        val coroutineScope: CoroutineScope
    ) {
        var isLoaded = false

        var job: Job? = null

        val dimension = pdfRenderer.openPage(index).use { currentPage ->
            Dimension(
                width = currentPage.width,
                height = currentPage.height
            )
        }

        fun heightByWidth(width: Int): Int {
            val ratio = dimension.width.toFloat() / dimension.height
            return (ratio * width).toInt()
        }

        val pageContent = MutableStateFlow<Bitmap?>(null)

        fun load() {
            if (!isLoaded) {
                job = coroutineScope.launch {
                    mutex.withLock {
                        val newBitmap: Bitmap
                        pdfRenderer.openPage(index).use { currentPage ->
                            newBitmap = createBlankBitmap(
                                width = currentPage.width,
                                height = currentPage.height
                            )
                            currentPage.render(
                                newBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                        }
                        isLoaded = true
                        pageContent.emit(newBitmap)
                    }
                }
            }
        }

        fun recycle() {
            isLoaded = false
            val oldBitmap = pageContent.value
            pageContent.tryEmit(null)
            oldBitmap?.recycle()
        }

        private fun createBlankBitmap(
            width: Int,
            height: Int
        ): Bitmap {
            return createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawColor(android.graphics.Color.WHITE)
                canvas.drawBitmap(this, 0f, 0f, null)
            }
        }
    }

    data class Dimension(
        val width: Int,
        val height: Int
    )
}

```

从这段代码中可以看到, 我们创建了一个数据类, 作为页面尺寸的容器, 然后我们添加了一个`dimension`字段, 用于精确存储在每个`Page`类初始化阶段填充的单个页面的尺寸.

为此, 我们必须调用`openPage()`函数, 这将在创建`Page`实例时引入少量开销, 但这将为我们的内存管理带来巨大优势.

我们还添加了一个`heightByWidth()`函数, 用于根据页面的纵横比从最大宽度计算页面的高度.

至于第二点, 整个实现过程都在Composable `PDFReader` 中.

```
scss复制代码@Composable
fun PDFReader(file: File) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val pdfRender = PdfRender(
            fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        )
        DisposableEffect(key1 = Unit) {
            onDispose {
                pdfRender.close()
            }
        }
        LazyColumn {
            items(count = pdfRender.pageCount) { index ->
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val page = pdfRender.pageLists[index]
                    DisposableEffect(key1 = Unit) {
                        page.load()
                        onDispose {
                            page.recycle()
                        }
                    }
                    page.pageContent.collectAsState().value?.asImageBitmap()?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Pdf page number: $index",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                page
                                    .heightByWidth(constraints.maxWidth)
                                    .pxToDp()
                            )
                    )
                }
            }
        }
    }
}

```

与之前的实现相比, 主要区别在于加入了`DisposableEffect`. 当可编程渲染器退出组合时, 它可以方便地调用`PdfRender`类的`close()`函数. 关闭`PdfRenderer`并回收所有已创建的`Bitmap`时, 必须执行该操作. 要进一步了解`DisposableEffect`的工作原理, 可以**查看官方文档**:

[developer.android.com/jetpack/com…](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.com%2Fjetpack%2Fcompose%2Fside-effects%23disposableeffect).

第二个不同之处是使用了`BoxWithConstraints`来包含`LazyColumn`中的每个项. 这有利于确定元素可占用的最大宽度.

下一个修改是将 `LaunchedEffect` 替换为 `DisposableEffect`. 这不仅使我们能够仅在显示页面时调用 `load()` 函数, 而且**方便了在页面退出组合**时执行 `recycle()` 函数. 这确保了与该页面相关的`Bitmap`被回收, 从而释放了内存.

最后一项更改是针对尚未渲染显示页面的情况. 我们不显示任何内容(这会导致组件的高度为 0), 而是创建一个具有最大可能宽度的方框, 高度则根据方框的宽度和页面的纵横比计算得出. **这种方法可以防止`LazyColumn`对列表中的所有项目触发`load()`函数, 而只对当前显示的项目触发**.

现在, 让我们来看看最终结果.

我们可以看到, 即使在创建 `Page` 实例时由于计算页面大小而引入了少量开销, 加载时间仍然基本相同.

现在我们来看看内存使用情况.

我们从占用 6GB 多内存到只占用 200MB 多一点.

在宣布自己完全满意之前, 让我们看看如果用户滚动页面列表, 内存会发生什么变化.

如图所示, 在滚动过程中, RAM 使用量会出现峰值, 这主要是由于要初始化新页面以便显示. 不过, **由于对退出组合的页面进行了循环利用, 这些峰值得到了及时缓解**.