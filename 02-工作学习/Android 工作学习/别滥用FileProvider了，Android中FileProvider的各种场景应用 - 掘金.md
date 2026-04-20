# 别滥用FileProvider了，Android中FileProvider的各种场景应用 - 掘金

[https://juejin.cn/post/7140166121595863076](https://juejin.cn/post/7140166121595863076)

我正在参与掘金创作者训练营第6期，[点击了解活动详情](https://juejin.cn/post/7134166674160222221/)

### 前言

有部分同学只要是上传或者下载，只要用到了文件，不管三七二十一写个 FileProvider 再说。

不是每一种情况都需要使用 FileProvider 的，啥？你问行不行？有没有毛病？

这... 写了确实可以，没毛病！但是这没有必要啊。

如果不需要FileProvider就不需要定义啊，如果定义了重复的 FileProvider，还会导致清单文件合并失败，需要处理冲突，从而引出又一个问题，解决 FileProvider 的冲突问题，当然这不是本文的重点，网上也有解决方案。

这里我们只使用 FileProvider 来说，分析一下如下场景：

1.比如我们下载文件到SD卡，当然我们一般都下载到download目录下，那么使用这个文件，需要 FileProvider 吗？

不需要！因为他是共享文件夹中，并不是在沙盒中。

2.那我们把文件保存到沙盒中，比如 `getExternalFilesDir` 。那么我们使用这个沙盒中的文件，需要 FileProvider 吗？

3.看情况，如果只是把此文件上传到服务器，上传到云平台，也就是我们自己App使用自己的沙盒，是不需要 FileProvider 的

4.如果是想使用系统打开文件，或者传递给第三方App，那么是需要 FileProvider 的。

> 
> 
> 
> 也就是说一般使用场景，我们只有在自己App沙盒中的文件，需要给别的App操作的时候，我们才需要使用 FileProvider 。
> 

比较典型的例子是，下载Apk到自己的沙盒文件中，然后调用Android的Apk安装器去安装应用（这是一个单独的App），我们就需要 FileProvider 。

或者我们沙盒中的图片，需要发送到第三方的App里面展示，我们需要 FileProvider 。

话不多说，我们从常规的使用与示例上来看看怎么使用，清楚它的一些小细节。

### 一、常规使用与定义

一般来说没有什么特殊的需求，我们使用系统自带的 FileProvider 类来定义即可。

我们再清单文件注册我们的FileProvider

```
xml复制代码    <provider
        android:authorities="com.guadou.kt_demo.fileprovider"
        android:name="androidx.core.content.FileProvider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_path">
        </meta-data>
    </provider>

```

属性的一些说明：

1. authorities 是标记我们这个ContentProvider的唯一标识，是一个用于认证的暗号，我们一般默认使用包名+fileprovider来定义。（能不能使用别的，可以，abcd都行，但是没必要）
2. name 是具体的FileProvider类，如果是系统的，就用上面的这种，如果是自定义的，就写自定义FileProvider的全类名。
3. exported 是否限制其他应用获取此FileProvider。
4. grantUriPermissions 是否授权其他应用获取访问Uri权限，一般为true。
5. meta-data 和下面的 name 都是固定的写法，重点是 resource 需要自己实现规则，定义哪些私有文件会被提供访问。

看看我们定义的file_path文件：

```
xml复制代码<?xml version="1.0" encoding="utf-8"?>
<paths>

    <root-path name="myroot" path="." />

    <external-path name="external_file" path="." />

    <files-path name="inner_app_file" path="." />

    <cache-path name="inner_app_cache" path="." />

    <external-files-path name="external_app_file" path="." />
    <external-files-path name="log_file" path="log" />

    <external-cache-path name="external_app_cache" path="." />
    <external-cache-path name="naixiao_img" path="pos" />

</paths>

```

属性的含义如下：

1. root-path 从SD卡开始找 例如 storage/emulated/0/Android/data/com.guadou.kt_demo/cache/pos/naixiao-1122.jpg
2. external-path 从外置SD卡开始 例如 Android/data/com.guadou.kt_demo/cache/pos/naixiao-1122.jpg
3. external-files-path 外置沙盒file目录 例如 pos/naixiao-1122.jpg （真实目录在 Android/data/com.guadou.kt_demo/cache/pos/）
4. external-cache-path 外置沙盒cache目录 例如 naixiao-1122.jpg （真实目录在 Android/data/com.guadou.kt_demo/cache/）
5. files-path 和上面的同理，只是在内置的data/data目录下面
6. cache-path 和上面的同理，只是在内置的data/data目录下面

总共使用的就这么几个，大家可以看到我的定义，它是可以重复定义的。

比我我用到的这两个，是的同样类型的可以定义多个，

```
xml复制代码 <external-cache-path name="external_app_cache" path="." />
 <external-cache-path name="naixiao_img" path="pos" />

```

如果我定义了两个同类型的 external-cache-path ，他们的 name 你可以随便取，叫abc都行，主要是path ， 推荐大家如果想暴露根目录就使用点. ， 如果想暴露指定的目录就写对应的文件夹名称。

比我我现在有一个图片在这个目录下

> 
> 
> 
> storage/emulated/0/Android/data/com.guadou.kt_demo/cache/pos/naixiao-1122.jpg
> 

通过 FileProvider 获取Uri 也是分优先顺序的。

比如我定义了pos的目录，那么打印如下：

> 
> 
> 
> 打印Uri:content://com.guadou.kt_demo.fileprovider/naixiao_img/naixiao-1122.jpg
> 

那我们现在把pos的去掉，只要这个。

```
xml复制代码<external-cache-path name="external_app_cache" path="." />

```

那么打印就如下：

换了name，多了pos的路径。

那我们都去掉呢？只保留外置SD卡和SD卡的规则。

```
xml复制代码    <root-path name="myroot" path="." />
    <external-path name="external_file" path="." />

```

那么打印就如下：

> 
> 
> 
> 打印Uri:content://com.guadou.kt_demo.fileprovider/external_file/Android/data/com.guadou.kt_demo/cache/pos/naixiao-1122.jpg
> 

就走到了外置SD卡的规则中去了。

那我们再去掉外置卡的规则。此时定义如下

```
xml复制代码 <root-path name="myroot" path="." />

```

此时打印如下：

> 
> 
> 
> 打印Uri:content://com.guadou.kt_demo.fileprovider/myroot/storage/emulated/0/Android/data/com.guadou.kt_demo/cache/pos/naixiao-1122.jpg
> 

可以看到它的匹配规则是一层一层往上找的，那我们再去掉SD卡的规则呢。。。

那不就空了吗，此时就崩溃报错了，这样是真拿不到Uri了...

**使用示例：**

说到这里，我们还没有真的使用 FileProvider ，下面我们以一个图片实例为例子演示如何发送到系统的App

```
kotlin复制代码    //测试FileProvider
    fun fileProvider1() {

        val drawable = drawable(R.drawable.chengxiao)
        val bd: BitmapDrawable = drawable as BitmapDrawable
        val bitmap = bd.bitmap
        FilesUtils.getInstance().saveBitmap(bitmap, "naixiao-1122.jpg")

        val filePath = FilesUtils.getInstance().sdpath + "naixiao-1122.jpg"

        YYLogUtils.w("文件原始路径：$filePath")

        val uri = FileProvider.getUriForFile(commContext(), "com.guadou.kt_demo.fileprovider", File(filePath))

        YYLogUtils.w("打印Uri:$uri")

        //到系统中找打开对应的文件
        openFile(filePath, uri)
    }

    private fun openFile(path: String, uri: Uri) {
        //取得文件扩展名
        val extension: String = path.substring(path.lastIndexOf(".") + 1)

        //通过扩展名找到mimeType
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        YYLogUtils.w("mimeType: $mimeType")

        try {
            //构造Intent，启动意图，交由系统处理
            startActivity(Intent().apply {
                //临时赋予读写权限
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                //表示用其它应用打开
                action = Intent.ACTION_VIEW
                //给Intent 赋值
                setDataAndType(uri, mimeType)
            })
        } catch (e: Exception) {
            e.printStackTrace()
            YYLogUtils.e("不能打开这种类型的文件")
        }
    }

```

很简单的一个例子，我们把drawable中的一个图片，保存到我们私有沙盒目录中，目录为

> 
> 
> 
> 文件原始路径：/storage/emulated/0/Android/data/com.guadou.kt_demo/cache/pos/naixiao-1122.jpg
> 

我们通过 FileProvider 拿到 content://开头的uri路径。然后通过Intent匹配找到对于的第三方App来接收。

运行结果如下：

打开了系统自带的图片查看器，还能编辑图片，查看信息等。

那么打印就如下：

content 是 scheme。

com.guadou.kt_demo.fileprovider 即为我们在清单文件中定义的 authorities，即是我们的FileProvider的唯一表示,在接收的时候作为host。

这样封装之后，当其他的App收到这个Uri就无法从这些信息得知我们的文件的真实路径，相对有安全保障。

其他场景中,比如沙盒中的Apk文件想要安装，也是一样的流程，我们需要赋予读写权限，然后设置DataAndType即可。代码的注释很详细，大家可以参考参考。

此时我们都是发送了一个Intent，让系统自己去匹配符合条件的Activity。那有没有可能我们自己做一个App去匹配它。

这... 好像还真行。

### 二、能不能自定义接收文件？

其实我们仿造系统的App的做法，我们在自定义的Activity中加入指定Filter即可，比如这里我需要接收图片，那么我定义如下的 intent-filter :

```
xml复制代码     <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />

            </intent-filter>
        </activity>

        <activity
            android:name=".ReceiveImageActivity"
            android:exported="true">

            <intent-filter>

                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />

                <data android:scheme="content" />
                <data android:scheme="file" />
                <data android:scheme="http" />
                <data android:mimeType="image/*" />

            </intent-filter>

        </activity>

```

都是一些固定的写法，我们在Activity上指明，它可以接收图片数据，此时我们再回到第一个App，发送图片，看看运行的效果：

之前还是图片查看器，现在可以选择我们自己的App来接收图片数据了，但是我们如何接收数据呢？

其实都是一些固定的代码，主要是拿到input流，然后操作流的处理。

```
kotlin复制代码    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive_image)

        if (intent != null && intent.action == Intent.ACTION_VIEW) {

            val uri = intent.data
            YYLogUtils.w("uri: $uri")

            if (uri != null && uri.scheme != null && uri.scheme == "content") {

                val fis = contentResolver.openInputStream(uri)

                if (fis != null) {

                    val bitmap = BitmapFactory.decodeStream(fis)
                    //展示
                    if (bitmap != null) {
                        val ivReveiverShow = findViewById<ImageView>(R.id.iv_reveiver_show)
                        ivReveiverShow.setImageBitmap(bitmap)
                    }

                }
            }
        }
    }

```

最简单的做法，直接根据uri打开输入流，然后我们可以通过 BitmapFactory 就可以拿到 Bitmap了，就能展示图片到ImageView上面。

效果如图：

甚至我们拿到了 input 流，我们还能对流进行copy 操作，把你的图片保存到我自己的沙盒目录中，例如：

```
kotlin复制代码
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive_image)

        if (intent != null && intent.action == Intent.ACTION_VIEW) {

            val uri = intent.data
            YYLogUtils.w("uri: $uri")

            if (uri != null && uri.scheme != null && uri.scheme == "content") {

                val fis = contentResolver.openInputStream(uri)

                if (fis != null) {

                    val inBuffer = fis.source().buffer()

                    val outFile = File(getExternalFilesDir("xiaoxiao"), "naixiao5566.jpg")
                    outFile.sink().buffer().use {
                        it.writeAll(inBuffer)
                        inBuffer.close()
                    }

                    YYLogUtils.w("存放的路径：${outFile.absolutePath}")

                    //展示
                    val ivReveiverShow = findViewById<ImageView>(R.id.iv_reveiver_show)
                    ivReveiverShow.extLoad(outFile.absolutePath)

                }
            }
        }
    }

```

保存到自己的沙盒文件之后，我们看一看效果：

好像还真的能行，秀啊。

那此时有人还会有一个疑问，你这方法都是我主动的发送给别人去展示，去操作！这都不是事，关键是能不能让别人主动的来操作、玩弄我的沙盒文件？

比如我做的App想获取微信，支付宝这些别人的App的沙盒中的图片？行不行？有没有方法可以做到？

这...，你别逗我了。

### 三、能不能主动查询对方的沙盒？

转头一想，好像还真行，有操作空间啊... 既然 FileProvider 是继承自 ContentProvider 。那凭什么我们的App都能获取到别人App的数据库了，不能获取别人的沙盒文件呢？那数据库文件不也存在沙盒中么？

例如联系人App,我们开发的第三方App可以通过 ContentProvider 获取到联系人App中的联系人数据，那么只要第三方的App定义好对应的 ContentProvider 我不就能获取到它沙盒的文件了吗？

说到就做，我们先把FileProvider设置为可访问

```
xml复制代码     <provider
        android:name=".MyFileProvider"
        android:authorities="com.guadou.kt_demo.fileprovider"
        android:exported="true"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_path">

        </meta-data>
    </provider>

```

是的，`android:exported="true"` 设置成功之后我们直接通过 contentResolver 去查询不就好了吗？

先运行一下试试！ 运行就崩了？

什么鬼哦，看看FileProvider的代码，原来不允许开放

> 
> 
> 
> 原来 FileProvider的 exported 和 grantUriPermissions 都是指定的写法，不能改变，并且不允许暴露，不允许给别的App主动访问！
> 

这和我们的需求不符合啊，我就要主动访问，既然你不行，那我不用你行了吧！我继承 ContentProvider 行了吧！我自己实现文件获取、Cursor封装行了吧！

不皮了，其实我们直接通过继承 ContentProvider 并且允许 `exported` ，然后我们通过自己实现的query方法，返回指定的Cursor信息，就可以实现！

部分代码如下：

```
java复制代码public class MyFileProvider extends ContentProvider {

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);

        mStrategy = getPathStrategy(context, info.authority);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        YYLogUtils.w("走到query方法");

        final File file = mStrategy.getFileForUri(uri);
        YYLogUtils.w("file:" + file);

        if (!file.exists()) {
            return null;
        }

        boolean directory = file.isDirectory();
        if (directory) {
            YYLogUtils.w("说明是文件夹啊！");

            File[] files = file.listFiles();
            for (File childFile : files) {
                if (childFile.isFile()) {
                    String name = childFile.getName();
                    String path = childFile.getPath();
                    long size = childFile.length();
                    Uri uriForFile = mStrategy.getUriForFile(childFile);
                    YYLogUtils.w("name:" + name + " path:" + path + " size: " + size +" uriForFile:"+uriForFile);
                }
            }
            //自己遍历封装Cursor实现
            return null;

        } else {
            YYLogUtils.w("说明是文件啊！");

            if (projection == null) {
                projection = COLUMNS;
            }

            String[] cols = new String[projection.length];
            Object[] values = new Object[projection.length];
            int i = 0;
            for (String col : projection) {
                if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                    cols[i] = OpenableColumns.DISPLAY_NAME;
                    values[i++] = file.getName();
                } else if (OpenableColumns.SIZE.equals(col)) {
                    cols[i] = OpenableColumns.SIZE;
                    values[i++] = file.length();
                }
            }

            cols = copyOf(cols, i);
            values = copyOf(values, i);

            final MatrixCursor cursor = new MatrixCursor(cols, 1);
            cursor.addRow(values);

            return cursor;
        }

    }
}

```

我简单的做了文件和文件夹的处理，并不完整，如果是文件我们可以直接返回一个简单的cursor，如果是文件夹需要大家自己拼接子文件的cursor并返回。

接下来我们看看其他App如何主动这些文件，在另一个App中我们先加上权限：

```
xml复制代码<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.hongyegroup.receiver">

    <queries>
        <provider android:authorities="com.guadou.kt_demo.fileprovider" />
    </queries>

    ...

</manifest>

```

然后我们直接使用 `contentResolver.query`

```
kotlin复制代码  private fun queryFiles() {
        val uri = Uri.parse("content://com.guadou.kt_demo.fileprovider/external_app_cache/pos/naixiao-1122.jpg")

        val cursor = contentResolver.query(uri, null, null, null, null)

        if (cursor != null) {

            while (cursor.moveToNext()) {

                val fileName = cursor.getString(cursor.getColumnIndex("_display_name"));
                val size = cursor.getLong(cursor.getColumnIndex("_size"));

                YYLogUtils.w("name: $fileName  size: $size")
                Toast.makeText(this, "name: $fileName  size: $size", Toast.LENGTH_SHORT).show()
            }

            cursor.close()

        } else {
            YYLogUtils.w("cursor-result: 为空啊")
            Toast.makeText(this, "cursor-result: 为空啊", Toast.LENGTH_SHORT).show()
        }
    }

```

如果我们知道它的指定文件Uri，我们可以通过query查询到文件的一些基本信息。具体是哪些信息，需要对方提供和定义。

如果想操作对方的文件，由于我们已经拿到了对方的Uri，我们可以直接通过inputStream来操作,例如：

```
kotlin复制代码        val fis = contentResolver.openInputStream(uri)
        if (fis != null) {

            val inBuffer = fis.source().buffer()

            val outFile = File(getExternalFilesDir(null), "abc")
            outFile.sink().buffer().use {
                it.writeAll(inBuffer)
                inBuffer.close()
            }

            YYLogUtils.w("保存文件成功")

        }

```

这些都是简单的基本操作，重点是如果我不知道具体的文件呢？

我就想把对方App的沙盒中的文件夹下面的全部文件都拿到，行不行？

行！只要对方App配合就行，例如：

```
kotlin复制代码     private fun queryFiles() {

        val uri = Uri.parse("content://com.guadou.kt_demo.fileprovider/external_app_cache/pos/")

        val cursor = contentResolver.query(uri, null, null, null, null)

        if (cursor != null) {

            while (cursor.moveToNext()) {

                val fileName = cursor.getString(cursor.getColumnIndex("_display_name"));
                val size = cursor.getLong(cursor.getColumnIndex("_size"));
                val uri = cursor.getString(cursor.getColumnIndex("uri"));

                val fileUri = Uri.parse(uri)

                //就可以使用IO或者BitmapFactory来操作流了

                YYLogUtils.w("name: $fileName  size: $size")
                Toast.makeText(this, "name: $fileName  size: $size", Toast.LENGTH_SHORT).show()
            }

            cursor.close()

        } else {
            YYLogUtils.w("cursor-result: 为空啊")
            Toast.makeText(this, "cursor-result: 为空啊", Toast.LENGTH_SHORT).show()
        }

    }

```

这样就是把对方外置SD卡下面的cache目录下的pos目录下的全部文件拿到手，当然了，这个需要对方App封装对应的cursor才行哦。

打印的Log如下：

只要对方封装的Cursor，我们可以把名字，大小，uri等信息都封装到Cursor中，提供给对方获取。

### 总结

FileProvider的主要应用场景就是分享，把自己沙盒中的文件分享，主动提供给其他匹配的App去使用。

使用其他App的图片？查询了目前市场上的主流App,微信，支付宝，闲鱼，美团，等App，例如在保存文件的时候都没有存在自己的沙盒中了，都是默认在DCIM或Pictures中，并存入 MediaStore 保存到图库中。

这样就算公共目录，无需FileProvider，大家直接通过 MediaStore 就能获取和使用。

而如果想主动访问其他App的沙盒文件，则需要对方App全方位配合，一般用于自家App的全家桶之类的应用。相对来说相对应用场景比较少。

不是做不到，只是大家觉得没有必要而已，毕竟定义和使用相对复杂，并且有暴露风险，被攻击的风险等。

本文全部代码均以开源，[源码在此](https://link.juejin.cn/?target=https%3A%2F%2Fgitee.com%2Fnewki123456%2FKotlin-Room)。大家可以点个Star关注一波。

好了，本期内容如有错漏的地方，希望同学们可以指出交流。如果有更好的方法，也欢迎大家评论区讨论。

如果感觉本文对你有一点点点的启发，还望你能`点赞`支持一下,你的支持是我最大的动力。

Ok,这一期就此完结。