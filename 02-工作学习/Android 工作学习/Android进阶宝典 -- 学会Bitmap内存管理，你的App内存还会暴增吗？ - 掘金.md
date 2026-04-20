# Android进阶宝典 -- 学会Bitmap内存管理，你的App内存还会暴增吗？ - 掘金

[https://juejin.cn/post/7201444843138580517](https://juejin.cn/post/7201444843138580517)

相信伙伴们在日常的开发中，一定对图片加载有所涉猎，而且对于图片加载现有的第三方库也很多，例如Glide、coil等，使用这些三方库我们好像就没有啥担忧的，他们内部的内存管理和缓存策略做的很好，但是一旦在某些场景中无法使用图片加载库，或者项目中没有使用图片加载库而且重构难度大的情况下，对于Bitmap内存的管理就显得尤为重要了，一旦使用出现问题，那么OOM是常有的事。

在Android 8.0之后，Bitmap的内存分配从Java堆转移到了Native堆中，所以我们可以通过Android profiler性能检测工具查看内存使用情况。

未经过内存管理，列表滑动前内存状态：

[](Android%E8%BF%9B%E9%98%B6%E5%AE%9D%E5%85%B8%20--%20%E5%AD%A6%E4%BC%9ABitmap%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86%EF%BC%8C%E4%BD%A0%E7%9A%84App%E5%86%85%E5%AD%98%E8%BF%98%E4%BC%9A%E6%9A%B4%E5%A2%9E%E5%90%97%EF%BC%9F%20-%20%E6%8E%98%E9%87%91/1a15b6f38f5d47aa92f7a607a57c4870tplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

列表滑动时，内存状态：

[](Android%E8%BF%9B%E9%98%B6%E5%AE%9D%E5%85%B8%20--%20%E5%AD%A6%E4%BC%9ABitmap%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86%EF%BC%8C%E4%BD%A0%E7%9A%84App%E5%86%85%E5%AD%98%E8%BF%98%E4%BC%9A%E6%9A%B4%E5%A2%9E%E5%90%97%EF%BC%9F%20-%20%E6%8E%98%E9%87%91/f56aa0ba4bb4441185b7254d0175c1fftplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

通过上面两张图我们可以发现，Java堆区的内存没有变化，但是Native的内存发生了剧烈的抖动，而且伴随着频繁的GC，如果有了解JVM的伙伴，这种情况下必定伴随着应用的卡顿，所以对于Bitmap加载，就要避免频繁地创建和回收，因此本章将会着重介绍Bitmap的内存管理。

# 1 Bitmap“整容”

首先我们需要明确一点，既然是内存管理，难道只是对图片压缩保证不会OOM吗？其实不是的，内存管理一定是多面多点的，压缩是一方面，为什么起标题为“整容”，是因为最终加载到内存的Bitmap一定不是单纯地通过decodeFile就能完成的。

## 1.1 Bitmap内存复用

上图内存状态对应的列表代码如下：

```
复制代码override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    bindBitmap(holder)
}

///sdcard/img.png
private fun bindBitmap(holder: ViewHolder) {
    val bitmap = BitmapFactory.decodeFile("/sdcard/img.png")
    holder.binding.ivImg.setImageBitmap(bitmap)
}

```

如果熟悉RecyclerView的缓存机制应该了解，当RecyclerView的Item移出页面之后，会放在缓存池当中；当下面的item显示的时候，首先会从缓存池中取出缓存，直接调用onBindViewHolder方法，所以依然会重新创建一个Bitmap，因此针对列表的缓存特性可以选择Bitmap内存复用机制。

[](Android%E8%BF%9B%E9%98%B6%E5%AE%9D%E5%85%B8%20--%20%E5%AD%A6%E4%BC%9ABitmap%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86%EF%BC%8C%E4%BD%A0%E7%9A%84App%E5%86%85%E5%AD%98%E8%BF%98%E4%BC%9A%E6%9A%B4%E5%A2%9E%E5%90%97%EF%BC%9F%20-%20%E6%8E%98%E9%87%91/db2c5eb6b8cb4e889fd70c1321322746tplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

看上面这张图，因为顶部的Item在新建的时候，已经在native堆区中分配了一块内存，所以当这块区域被移出屏幕的时候，下面显示的Item不需要再次分配内存空间，而是复用移出屏幕的Item的内存区域，从而避免了频繁地创建Bitmap导致内存抖动。

```
kotlin复制代码override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    bindBitmap(holder)
}

///sdcard/img.png
private fun bindBitmap(holder: ViewHolder) {

    if (option == null) {
        option = BitmapFactory.Options()
        //开启内存复用
        option?.inMutable = true
    }
    val bitmap = BitmapFactory.decodeFile("/sdcard/img.png", option)
    option?.inBitmap = bitmap
    holder.binding.ivImg.setImageBitmap(bitmap)
}

```

那么如何实现内存复用，在BitmapFactory中提供了Options选项，当设置inMutable属性为true之后，就代表开启了内存复用，**此时如果新建了一个Bitmap，并将其添加到inBitmap中，那么后续所有Bitmap的创建，只要比这块内存小，那么都会放在这块内存中，避免重复创建。**

滑动前：

[](Android%E8%BF%9B%E9%98%B6%E5%AE%9D%E5%85%B8%20--%20%E5%AD%A6%E4%BC%9ABitmap%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86%EF%BC%8C%E4%BD%A0%E7%9A%84App%E5%86%85%E5%AD%98%E8%BF%98%E4%BC%9A%E6%9A%B4%E5%A2%9E%E5%90%97%EF%BC%9F%20-%20%E6%8E%98%E9%87%91/59fa4dbe4dbe410b94b7a8fce598090dtplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

滑动时：

[](Android%E8%BF%9B%E9%98%B6%E5%AE%9D%E5%85%B8%20--%20%E5%AD%A6%E4%BC%9ABitmap%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86%EF%BC%8C%E4%BD%A0%E7%9A%84App%E5%86%85%E5%AD%98%E8%BF%98%E4%BC%9A%E6%9A%B4%E5%A2%9E%E5%90%97%EF%BC%9F%20-%20%E6%8E%98%E9%87%91/4c9c5af1f91a4cd6bffa3b3533fd49f7tplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

通过上图我们发现，即便是在滑动的时候，Native内存都没有明显的变化。

## 1.2 Bitmap压缩

像1.1中这种加载形式，其实都是会直接将Bitmap加载到native内存中，例如我们设置的ImageView只有100*100，那么图片的大小为1000 * 800，其实是不需要将这么大体量的图片直接加载到内存中，那么有没有一种方式，在图片加载到内存之前就能拿到这些基础信息呢？

当然有了，这里还是要搬出BitmapFactory.Option这个类，其中inJustDecodeBounds这个属性的含义，从字面意思上就可以看出，只解码边界，也就是意味着在加载内存之前，是会拿到Bitmap的宽高的，注意需要成对出现，开启后也需要关闭。

```
kotlin复制代码private fun bindBitmap(holder: ViewHolder) {

    if (option == null) {
        option = BitmapFactory.Options()
        //开启内存复用
        option?.inMutable = true
    }

    //在加载到内存之前，获取图片的基础信息
    option?.inJustDecodeBounds = true
    BitmapFactory.decodeFile("/sdcard/img.png",option)
    //获取宽高
    val outWidth = option?.outWidth ?: 100
    val outHeight = option?.outHeight ?: 100
    //计算缩放系数
    option?.inSampleSize = calculateSampleSize(outWidth, outHeight, 100, 100)
    option?.inJustDecodeBounds = false
    val bitmap = BitmapFactory.decodeFile("/sdcard/img.png", option)
    option?.inBitmap = bitmap
    holder.binding.ivImg.setImageBitmap(bitmap)
}

private fun calculateSampleSize(
    outWidth: Int,
    outHeight: Int,
    maxWidth: Int,
    maxHeight: Int
): Int? {
    var sampleSize = 1
    Log.e("TAG","outWidth $outWidth outHeight $outHeight")
    if (outWidth > maxWidth && outHeight > maxHeight) {
        sampleSize = 2
        while (outWidth / sampleSize > maxWidth && outHeight / sampleSize > maxHeight) {
            sampleSize *= 2
        }
    }
    return sampleSize
}

```

然后会需要计算一个压缩的系数，给BitmapFactory.Option类的inSampleSize赋值，这样Bitmap就完成了缩放，我们再次看运行时的内存状态。

[](Android%E8%BF%9B%E9%98%B6%E5%AE%9D%E5%85%B8%20--%20%E5%AD%A6%E4%BC%9ABitmap%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86%EF%BC%8C%E4%BD%A0%E7%9A%84App%E5%86%85%E5%AD%98%E8%BF%98%E4%BC%9A%E6%9A%B4%E5%A2%9E%E5%90%97%EF%BC%9F%20-%20%E6%8E%98%E9%87%91/8e31ccd15af64eda90cb1fa710d7f94etplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

Native内存几乎下降了一半。

# 2 手写图片缓存框架

在第一节中，我们对于Bitmap自身做了一些处理，例如压缩、内存复用。虽然做了这些处理，但是不足以作为一个优秀的框架对外输出。

为什么呢？像1.2节中，我们虽然做了内存复用以及压缩，但是每次加载图片都需要重新调用decodeFile拿到一个bitmap对象，其实这都是同一张图片，即便是在项目中，肯定也存在相同的图片，那么我们肯定不能重复加载，因此对于加载过的图片我们想缓存起来，等到下次加载的时候，直接拿缓存中的Bitmap，其实也是加速了响应时间。

## 2.1 内存缓存

[](Android%E8%BF%9B%E9%98%B6%E5%AE%9D%E5%85%B8%20--%20%E5%AD%A6%E4%BC%9ABitmap%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86%EF%BC%8C%E4%BD%A0%E7%9A%84App%E5%86%85%E5%AD%98%E8%BF%98%E4%BC%9A%E6%9A%B4%E5%A2%9E%E5%90%97%EF%BC%9F%20-%20%E6%8E%98%E9%87%91/6110de85a11d4ce2adfac15b91704990tplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

首先一个成熟的图片加载框架，三级缓存是必须的，像Glide、coil的缓存策略，如果能把这篇文章搞懂了，那么就全通了。

在Android中，提供了LruCache这个类，也是内存缓存的首选，如果熟悉LruCache的伙伴，应该明白其中的原理。它其实是一个双向链表，以最近少用原则，当缓存中的数据长时间不用，而且有新的成员加入进来之后，就会移除尾部的成员，那么我们首先搞定内存缓存。

```
kotlin复制代码class BitmapImageCache {

    private var context: Context? = null

    //默认关闭
    private var isEnableMemoryCache: Boolean = false
    private var isEnableDiskCache: Boolean = false

    constructor(builder: Builder) {
        this.context = context
        this.isEnableMemoryCache = builder.isEnableMemoryCache
        this.isEnableDiskCache = builder.isEnableDiskCache
    }

    class Builder {

        var context: Context? = null

        //是否开启内存缓存
        var isEnableMemoryCache: Boolean = false

        //是否开启磁盘缓存
        var isEnableDiskCache: Boolean = false

        fun with(context: Context): Builder {
            this.context = context
            return this
        }

        fun enableMemoryCache(isEnable: Boolean): Builder {
            this.isEnableMemoryCache = isEnable
            return this
        }

        fun enableDiskCache(isEnable: Boolean): Builder {
            this.isEnableDiskCache = isEnable
            return this
        }

        fun build(): BitmapImageCache {
            return BitmapImageCache(this)
        }
    }
}

```

基础框架采用建造者设计模式，基本都是一些开关，控制是否开启内存缓存，或者磁盘缓存，接下来进行一些初始化操作。

首先对于内存缓存，我们使用LruCache，其中有两个核心的方法：sizeOf和entryRemoved，方法的作用已经在注释里了。

```
kotlin复制代码class BitmapLruCache(
    val size: Int
) : LruCache<String, Bitmap>(size) {

    /**
     * 告诉系统Bitmap内存的大小
     */
    override fun sizeOf(key: String, value: Bitmap): Int {
        return value.allocationByteCount
    }

    /**
     * 当Lru中的成员被移除之后，会走到这个回调
     * @param oldValue 被移除的Bitmap
     */
    override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
        super.entryRemoved(evicted, key, oldValue, newValue)

    }
}

```

当LruCache中元素被移除之后，我们想是不是就需要回收了，那这样的话其实就错了。**记不记得我们前面做的内存复用策略，如果当前Bitmap内存是可以被复用的，直接回收掉，那内存复用就没有意义了，所以针对可复用的Bitmap，可以放到一个复用池中，保证其在内存中**。

```
kotlin复制代码/**
 * 当Lru中的成员被移除之后，会走到这个回调
 * @param oldValue 被移除的Bitmap
 */
override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {

    if (oldValue.isMutable) {
        //放入复用池
        reusePool?.add(WeakReference(oldValue))
    } else {
        //回收即可
        oldValue.recycle()
    }
}

```

**所以这里加了一个判断，当这个Bitmap是支持内存复用的话，就加到复用池中，保证其他Item在复用内存的时候不至于找不到内存地址，前提是还没有被回收**；那么这里就有一个问题，当复用池中的对象（弱引用）被释放之后，Bitmap如何回收呢？与弱引用配套的有一个引用队列，当弱引用被GC回收之后，会被加到引用队列中。

```
kotlin复制代码class BitmapLruCache(
    val size: Int,
    val reusePool: MutableSet<WeakReference<Bitmap>>?,
    val referenceQueue: ReferenceQueue<Bitmap>?
) : LruCache<String, Bitmap>(size) {

    /**
     * 告诉系统Bitmap内存的大小
     */
    override fun sizeOf(key: String, value: Bitmap): Int {
        return value.allocationByteCount
    }

    /**
     * 当Lru中的成员被移除之后，会走到这个回调
     * @param oldValue 被移除的Bitmap
     */
    override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {

        if (oldValue.isMutable) {
            //放入复用池
            reusePool?.add(WeakReference(oldValue, referenceQueue))
        } else {
            //回收即可
            oldValue.recycle()
        }
    }
}

```

这里需要公开一个方法，开启一个线程一直检测引用队列中是否有复用池回收的对象，如果拿到了那么就主动销毁即可。

```
kotlin复制代码/**
 * 开启弱引用回收检测，目的为了回收Bitmap
 */
fun startWeakReferenceCheck() {
    //开启一个线程
    Thread {
        try {
            while (!shotDown) {
                val reference = referenceQueue?.remove()
                val bitmap = reference?.get()
                if (bitmap != null && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {

        }

    }.start()
}

```

另外再加几个方法，主要就是往缓存中加数据。

```
kotlin复制代码fun putCache(key: String, bitmap: Bitmap) {
    lruCache?.put(key, bitmap)
}

fun getCache(key: String): Bitmap? {
    return lruCache?.get(key)
}

fun clearCache() {
    lruCache?.evictAll()
}

```

初始化的操作，我们把它放在Application中进行初始化操作

```
kotlin复制代码class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        bitmapImageCache = BitmapImageCache.Builder()
            .enableMemoryCache(true)
            .with(this)
            .build()
        //开启内存检测
        bitmapImageCache?.startWeakReferenceCheck()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @JvmStatic
        var bitmapImageCache: BitmapImageCache? = null
    }
}

```

从实际的效果中，我们可以看到：

```
java复制代码2023-02-18 17:54:10.154 32517-32517/com.lay.nowinandroid E/TAG: outWidth 800 outHeight 560
2023-02-18 17:54:10.154 32517-32517/com.lay.nowinandroid E/TAG: 没有从缓存中获取
2023-02-18 17:54:10.169 32517-32517/com.lay.nowinandroid E/TAG: 从缓存中获取 Bitmap
2023-02-18 17:54:10.187 32517-32517/com.lay.nowinandroid E/TAG: 从缓存中获取 Bitmap
2023-02-18 17:54:16.740 32517-32517/com.lay.nowinandroid E/TAG: 从缓存中获取 Bitmap
2023-02-18 17:54:16.756 32517-32517/com.lay.nowinandroid E/TAG: 从缓存中获取 Bitmap
2023-02-18 17:54:16.926 32517-32517/com.lay.nowinandroid E/TAG: 从缓存中获取 Bitmap
2023-02-18 17:54:17.102 32517-32517/com.lay.nowinandroid E/TAG: 从缓存中获取 Bitmap

```

其实加了内存缓存之后，跟inBitmap的价值基本就是等价的了，也是为了避免频繁地申请内存，可以认为是一个双保险，加上对图片压缩以及LruCache的缓存策略，真正内存打满的场景还是比较少的。

## 2.2 复用池的处理

在前面我们提到了，为了保证可复用的Bitmap不被回收，从而加到了一个复用池中，那么当从缓存中没有取到数据的时候，就会从复用池中取，相当于是在内存缓存中加了一个二级缓存。

针对上述图中的流程，可以对复用池进行处理。

```
kotlin复制代码/**
 * 从复用池中取数据
 */
fun getBitmapFromReusePool(width: Int, height: Int, sampleSize: Int): Bitmap? {

    var bitmap: Bitmap? = null
    //遍历缓存池
    val iterator = reusePool?.iterator() ?: return null
    while (iterator.hasNext()) {
        val checkedBitmap = iterator.next().get()
        if (checkBitmapIsAvailable(width, height, sampleSize, bitmap)) {
            bitmap = checkedBitmap
            iterator.remove()
            //放在
            break
        }
    }

    return bitmap

}

/**
 * 检查当前Bitmap内存是否可复用
 */
private fun checkBitmapIsAvailable(
    width: Int,
    height: Int,
    sampleSize: Int,
    bitmap: Bitmap?
): Boolean {
    if (bitmap == null) {
        return false
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
        return width < bitmap.width && height < bitmap.height && sampleSize == 1
    }
    var realWidth = 0
    var realHeight = 0
    //支持缩放
    if (sampleSize > 1) {
        realWidth = width / sampleSize
        realHeight = height / sampleSize
    }
    val allocationSize = realHeight * realWidth * getBitmapPixel(bitmap.config)
    return allocationSize <= bitmap.allocationByteCount
}

/**
 * 获取Bitmap的像素点位数
 */
private fun getBitmapPixel(config: Bitmap.Config): Int {
    return if (config == Bitmap.Config.ARGB_8888) {
        4
    } else {
        2
    }
}

```

这里需要注意一点就是，如果想要复用内存，那么申请的内存一定要比复用的这块内存小，否则就不能匹配上。

所以最终的一个流程就是（**这里没考虑磁盘缓存，如果用过Glide就会知道，磁盘缓存会有问题**），首先从内存中取，如果取到了，那么就直接渲染展示；如果没有取到，那么就从复用池中取出一块内存，然后让新创建的Bitmap复用这块内存。

```
kotlin复制代码//从内存中取
var bitmap = BitmapImageCache.getCache(position.toString())
if (bitmap == null) {
    //从复用池池中取
    val reuse = BitmapImageCache.getBitmapFromReusePool(100, 100, 1)
    Log.e("TAG", "从网络加载了数据")
    bitmap = ImageUtils.load(imagePath, reuse)
    //放入内存缓存
    BitmapImageCache.putCache(position.toString(), bitmap)
} else {
    Log.e("TAG", "从内存加载了数据")
}

```

最终的一个呈现就是：

```
java复制代码2023-02-18 21:31:57.805 29198-29198/com.lay.nowinandroid E/TAG: 从网络加载了数据
2023-02-18 21:31:57.819 29198-29198/com.lay.nowinandroid E/TAG: outWidth 800 outHeight 560
2023-02-18 21:31:57.830 29198-29198/com.lay.nowinandroid E/TAG: 加入复用池 android.graphics.Bitmap@6c19c7b
2023-02-18 21:31:57.830 29198-29198/com.lay.nowinandroid E/TAG: setImageBitmap android.graphics.Bitmap@473ed07
2023-02-18 21:31:57.849 29198-29198/com.lay.nowinandroid E/TAG: 从网络加载了数据
2023-02-18 21:31:57.857 29198-29198/com.lay.nowinandroid E/TAG: outWidth 788 outHeight 514
2023-02-18 21:31:57.871 29198-29198/com.lay.nowinandroid E/TAG: 加入复用池 android.graphics.Bitmap@2a7844
2023-02-18 21:31:57.872 29198-29198/com.lay.nowinandroid E/TAG: setImageBitmap android.graphics.Bitmap@4d852a3
2023-02-18 21:31:57.917 29198-29198/com.lay.nowinandroid E/TAG: 从网络加载了数据
2023-02-18 21:31:57.943 29198-29198/com.lay.nowinandroid E/TAG: outWidth 34 outHeight 8
2023-02-18 21:31:57.958 29198-29198/com.lay.nowinandroid E/TAG: setImageBitmap android.graphics.Bitmap@a3d491e
2023-02-18 21:31:58.651 29198-29198/com.lay.nowinandroid E/TAG: 从内存加载了数据
2023-02-18 21:31:58.651 29198-29198/com.lay.nowinandroid E/TAG: setImageBitmap android.graphics.Bitmap@62fcf27
2023-02-18 21:31:58.706 29198-29198/com.lay.nowinandroid E/TAG: 从内存加载了数据
2023-02-18 21:31:58.707 29198-29198/com.lay.nowinandroid E/TAG: setImageBitmap android.graphics.Bitmap@e2f8a1a
2023-02-18 21:31:58.766 29198-29198/com.lay.nowinandroid E/TAG: 从内存加载了数据

```

其实真正要保证我们的内存稳定，就是尽量避免重复创建对象，尤其是大图片，在加载的时候尤其需要注意，在项目中出现内存始终不降的主要原因也是对Bitmap的内存管理不当，所以掌握了上面的内容，就可以针对这些问题进行优化。总之万变不离其宗，内存是App的生命线，如果在面试的时候问你如何设计一个图片加载框架，内存管理是核心，当出现文章一开头那样的内存曲线的时候，就需要重点关注你的Bitmap是不是又“乱飙”了。

# 附录 - ImageUtils

```
object ImageUtils {

    private val MAX_WIDTH = 100
    private val MAX_HEIGHT = 100

    /**
     * 加载本地图片
     * @param reuse 可以复用的Bitmap内存
     */
    fun load(imagePath: String, reuse: Bitmap?): Bitmap {

        val option = BitmapFactory.Options()
        option.inMutable = true
        option.inJustDecodeBounds = true

        BitmapFactory.decodeFile(imagePath, option)
        val outHeight = option.outHeight
        val outWidth = option.outWidth
        option.inSampleSize = calculateSampleSize(outWidth, outHeight, MAX_WIDTH, MAX_HEIGHT)

        option.inJustDecodeBounds = false
        option.inBitmap = reuse
        //新创建的Bitmap复用这块内存
        return BitmapFactory.decodeFile(imagePath, option)
    }

    private fun calculateSampleSize(
        outWidth: Int,
        outHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        var sampleSize = 1
        Log.e("TAG", "outWidth $outWidth outHeight $outHeight")
        if (outWidth > maxWidth && outHeight > maxHeight) {
            sampleSize = 2
            while (outWidth / sampleSize > maxWidth && outHeight / sampleSize > maxHeight) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }
}

```