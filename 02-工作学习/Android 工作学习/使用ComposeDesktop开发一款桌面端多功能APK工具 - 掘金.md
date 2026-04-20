# 使用ComposeDesktop开发一款桌面端多功能APK工具 - 掘金

[https://juejin.cn/post/7122645579439538183](https://juejin.cn/post/7122645579439538183)

我正在参加「创意开发 投稿大赛」详情请看：[掘金创意开发大赛来了！](https://juejin.cn/post/7120441631530549284)

终于算是忙完了一个阶段！！！从4月份开始，工作内容以及职务上都进行了较大的变动，最直接的就是从海外项目组调到了国内项目组。国内项目组目前有两个应用在同时跑着，而且还有几个马甲包也要维护，不知道大家发版的时候复杂不复杂，反正我们每次发版的时候都需要经历--打包、加固、对齐、重签名、打渠道包、上传云存储、生成渠道推广链接、生成内更SQL、上传Mapping文件等等步骤(xN)，简直是折磨人啊。

所以首要任务就是做出一套自动化的基础设施来，最初直接考虑到的方案是【**Jenkins+Docker+360命令行加固+VasDolly+Bugly等**】的方案（下一篇文章会给大家分享该方案），整个过程下来基本能达到自动化的目的。就这么稳定的跑了一个多月，然而，在5月下旬的时候360加固发布了一个通知，大致内容就是免费版用户无法使用命令行的加固方式了，只能手动用工具加固。这就导致最初的方案直接垮掉，我花费了个把月学习Linux，Pipeline，Docker，还制作了各种镜像，结果突然不能用了，心塞。然而路还是要继续走下去的，在尽量不花钱的前提下，想到了开发桌面端工具的方案。

# 功能一览

接下来先给大家一览下桌面端工具的基本功能，我的电脑是Windows的，所以都是基于Windows平台下的build-tools相关工具进行开发的。首先大部分的功能都是基于jar或exe文件，那么在Java（Kotlin）中我们可以通过如下方式来调用这些外部程序，exec其实最终也是调用了ProcessBuilder，整体的原理就是如此：

```
//方式1
Runtime.getRuntime().exec(cmd)

//方式2
ProcessBuilder(cmd)

```

## 多渠道打包

这是该工具最基本的功能，使用[**VasDolly**](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2FTencent%2FVasDolly)方案对APK文件进行多渠道打包（当然该APK文件需要是签名好的）。

多渠道包命令行工具即 VasDolly.jar，该文件可以在上述GitHub仓库中找到，常用的命令如下：

```
// 获取指定APK的签名方式
java -jar VasDolly.jar get -s [源apk地址]

// 获取指定APK的渠道信息
java -jar VasDolly.jar get -c [源apk地址]

// 删除指定APK的渠道信息
java -jar VasDolly.jar remove -c [源apk地址]

// 通过指定渠道字符串添加渠道信息
java -jar VasDolly.jar put -c "channel1,channel2" [源apk地址] [apk输出目录]

// 通过指定某个渠道字符串添加渠道信息到目标APK
java -jar VasDolly.jar put -c "channel1" [源apk地址] [输出apk地址]

// 通过指定渠道文件添加渠道信息
java -jar VasDolly.jar put -c channel.txt [源apk地址] [apk输出目录]

// 提供了FastMode，生成渠道包时不进行强校验，速度可提升10倍以上
java -jar VasDolly.jar put -c channel.txt -f [源apk地址] [apk输出目录]

```

## 对齐和签名

上传应用市场前，APK文件大部分会被市场要求进行加固，无论是使用腾讯乐固还是360加固等方式，加固后APK的签名信息总会被破坏，所以我们需要对加固后的APK文件重新进行签名。

首先我们需要准备好应用的签名信息，该工具支持导入签名文件，并保存相应的StorePass、KeyAlias、KeyPass信息，如下：

当选择APK后，程序会判断选择的APK是否进行了签名，如果没有签名，那么就会弹窗提醒用户选择配置好的签名文件进行签名，签名之后才可进行多渠道打包的过程。 注：该功能现已升级，添加签名文件的时候绑定包名，选择apk后会自动获取到包名然后查找到对应的签名文件自动对齐签名处理，无需手动进行选择了。

签名的过程则需要用到Android SDK中的两个文件，以Windows系统为例，一个是处理对齐的【build-tools\版本号\zipalign.exe】文件，另一个则是用来签名的【build-tools\版本号\lib\apksigner.jar】文件。

我们先看下zipalign工具的官方说明：

> 
> 
> 
> zipalign is a zip archive alignment tool. It ensures that all uncompressed files in the archive are aligned relative to the start of the file. This allows those files to be accessed directly via [mmap(2)](https://link.juejin.cn/?target=https%3A%2F%2Fman7.org%2Flinux%2Fman-pages%2Fman2%2Fmmap.2.html), removing the need to copy this data in RAM and reducing your app's memory usage. zipalign是一种zip归档对齐工具。它确保存档中所有未压缩的文件都与文件的开头对齐。这允许通过mmap直接访问这些文件，无需将这些数据复制到RAM中，并减少应用程序的内存使用。
> 
> zipalign should be used to optimize your APK file before distributing it to end-users. This is done automatically if you build with Android Studio. This documentation is for maintainers of custom build systems. 在将APK文件分发给用户之前，应使用zipalign优化APK文件。如果您使用Android Studio进行构建，这将自动完成。本文档面向定制构建系统的维护人员。
> 

Google官方现在要求在使用apksigner对APK文件进行签名前需要先使用zipalign来优化APK文件，具体命令如下，以Windows下的zipalign.exe文件为例：

```
//对齐APK
zipalign.exe -p -f -v 4 [源apk路径] [输出apk路径]

//验证APK是否对齐
zipalign.exe -c -v 4 [源apk路径]

```

其他相关的内容可以参阅官网 [zipalign](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.google.cn%2Fstudio%2Fcommand-line%2Fzipalign) 。

当APK文件对齐后，就可以给对齐后的APK进行签名操作了，签名的方法有两种，我们这里单说使用--ks选项指定密钥库的方式，具体命令如下：

```
java -jar apksigner.jar sign
    --verbose
    --ks [KeyStore文件路径]
    --ks-pass pass:[KeyStorePass]
    --ks-key-alias [KeyAlias]
    --key-pass pass:[KeyPass]
    --out [输出apk路径]
    [源apk路径]

```

命令本身很简单，别搞错参数就好，尤其是两个密码的参数，后面需要使用【pass:密码】。输入密码这里还支持其他格式，如果有需要请参阅官网 [apksigner](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.google.cn%2Fstudio%2Fcommand-line%2Fapksigner%3Fhl%3Den) 。

加固、对齐、重签名后，这个apk就可以进行多渠道打包的处理了，然后即可发布到相关市场和渠道。

## 其他内容

在项目中还有很多其他的相关配置，比如发版的时候需要对APP进行应用内的更新通知。那么就需要我们填写发版的相关信息，版本名、版本号、更新日志等等内容都需要完善（可根据APK文件的命名来获取部分信息），然后通过这些信息生成应用内部更新的SQL语句，发送钉钉通知给相关后台人员处理。通知这一步又用到了钉钉的SDK，该工具支持配置钉钉机器人Webhook地址以及需要艾特的人员信息。

打出来的这些包都需要统一上传到云存储上面，这一步使用了AWS的云存储SDK，可以配置云存储桶地址等信息，免去人工手动上传apk的烦恼。上传完毕后会根据文件名生成相应的下载链接并通知到钉钉群，以便市场人员获取到渠道最新的推广链接等。

接下来就说下桌面端的开发过程，至于Compose MultiPlatform的介绍，请参阅[官网地址](https://link.juejin.cn/?target=https%3A%2F%2Fwww.jetbrains.com%2Flp%2Fcompose-mpp%2F)。本文主要就描述下一些针对桌面端的相关需求。

## 弹窗

关于弹窗，ComposeDesktop同样提供了Dialog可组合函数：

```
@Composable
public fun Dialog(
    onCloseRequest: () -> kotlin.Unit,
    state: androidx.compose.ui.window.DialogState,
    visible: kotlin.Boolean,
    title: kotlin.String,
    icon: androidx.compose.ui.graphics.painter.Painter?,
    undecorated: kotlin.Boolean,
    transparent: kotlin.Boolean,
    resizable: kotlin.Boolean,
    enabled: kotlin.Boolean,
    focusable: kotlin.Boolean,
    onPreviewKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> kotlin.Boolean,
    onKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> kotlin.Boolean,
    content: @Composable() (DialogWindowScope.() -> kotlin.Unit)
    ): kotlin.Unit { /* compiled code */ }

```

大部分的参数都可以直接看出他的作用，主要看一下state参数，该参数可以控制弹窗的位置及大小，例如我们配置一个在屏幕中央，宽高为500*300dp的弹窗，那么示例代码如下：

```
state = DialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(500.dp, 300.dp),
        )

```

不过这个弹窗没有阴影，如果想添加的话可以内部套一层Surface来做出阴影效果：

```
Surface(
    modifier = Modifier.fillMaxSize().padding(20.dp),
    elevation = 10.dp,
    shape = RoundedCornerShape(16.dp)
)

```

## 文件选择器

关于文件选择器这一块目前Compose还没有专门的函数，但是我们还是可以使用原有的方案：

- javax.swing.JFileChooser
- java.awt.FileDialog

个人还是更偏向于使用JFileChooser，因为使用第二种方案的话，在页面重组的情况下总是会莫名的弹出选择框来。一个简单的文件选择器如下所示：

```
private fun showFileSelector(
    suffixList: Array<String>,
    onFileSelected: (String) -> Unit
) {
    JFileChooser().apply {
        //设置页面风格
        try {
            val lookAndFeel = UIManager.getSystemLookAndFeelClassName()
            UIManager.setLookAndFeel(lookAndFeel)
            SwingUtilities.updateComponentTreeUI(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = false
        fileFilter = FileNameExtensionFilter("文件过滤", *suffixList)

        val result = showOpenDialog(ComposeWindow())
        if (result == JFileChooser.APPROVE_OPTION) {
            val dir = this.currentDirectory
            val file = this.selectedFile
            println("Current apk dir: ${dir.absolutePath} ${dir.name}")
            println("Current apk name: ${file.absolutePath} ${file.name}")
            onFileSelected(file.absolutePath)
        }
    }
}

```

该方式在使用的过程中也有一定的缺陷，就是每次打开文件弹窗总是会卡顿一下，所以后续也是有了寻找其他高效选择文件方式的想法。

## 文件拖拽

选择文件除了上面的弹窗选择方式，还有另一种神奇的方式 - 拖拽选择，本来也是没有头绪，然而在Slack闲逛的时候发现了Jim Sproch推荐了一篇相关的文章：[dev.to/tkuenneth/f…](https://link.juejin.cn/?target=https%3A%2F%2Fdev.to%2Ftkuenneth%2Ffrom-swing-to-jetpack-compose-desktop-2-4a4h) 。看完后也是恍然大悟，但是在Compose Desktop中，window是整个窗口，如何让某一个指定的区域响应我们的文件拖拽事件呢？

还记得在Android上有ComposeView吧，用来嵌套原来的那一套View体系。那么在这里我也是采用了类似的这么一种方式，实例一个空的JPanel控件然后给它安排到window中去。具体位置及大小的设置呢，在Compose中可以通过 **onPlaced(onPlaced: (LayoutCoordinates) -> Unit)** 修饰符来获取到，示例代码如下所示：

```
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DropBoxPanel(
    modifier: Modifier,
    window: ComposeWindow,
    component: JPanel = JPanel(),
    onFileDrop: (List<String>) -> Unit
) {

    val dropBoundsBean = remember {
        mutableStateOf(DropBoundsBean())
    }

    Box(
        modifier = modifier.onPlaced {
            dropBoundsBean.value = DropBoundsBean(
                x = it.positionInWindow().x,
                y = it.positionInWindow().y,
                width = it.size.width,
                height = it.size.height
            )
        }) {
        LaunchedEffect(true) {
            component.setBounds(
                dropBoundsBean.value.x.roundToInt(),
                dropBoundsBean.value.y.roundToInt(),
                dropBoundsBean.value.width,
                dropBoundsBean.value.height
            )
            window.contentPane.add(component)

            val target = object : DropTarget(component, object : DropTargetAdapter() {
                override fun drop(event: DropTargetDropEvent) {

                    event.acceptDrop(DnDConstants.ACTION_REFERENCE)
                    val dataFlavors = event.transferable.transferDataFlavors
                    dataFlavors.forEach {
                        if (it == DataFlavor.javaFileListFlavor) {
                            val list = event.transferable.getTransferData(it) as List<*>

                            val pathList = mutableListOf<String>()
                            list.forEach { filePath ->
                                pathList.add(filePath.toString())
                            }
                            onFileDrop(pathList)
                        }
                    }
                    event.dropComplete(true)

                }
            }) {

            }
        }

        SideEffect {
            component.setBounds(
                dropBoundsBean.value.x.roundToInt(),
                dropBoundsBean.value.y.roundToInt(),
                dropBoundsBean.value.width,
                dropBoundsBean.value.height
            )
        }

        DisposableEffect(true) {
            onDispose {
                window.contentPane.remove(component)
            }
        }
    }
}

```

实际运行效果如下，个人感觉基本还是能达到目的的：

## 数据的保存

最开始的时候，功能很少，每个配置的数据都是使用了txt文件来一行行保存，但是到了后来功能越来越复杂，单纯的按行来处理貌似有点捉襟见肘了，所以考虑使用json来保存复杂的类型数据。

json数据的处理从原生JSON到FastJson，Gson，Moshi等都已经体验过了，于是乎便采用了之前未使用过的[**Jackson**](https://link.juejin.cn/?target=)。然而不得不说，就目前为止，jackson是我用过最简洁、优雅的一款解析库。

假如我有一个List类型的列表数据，那么当我要把这个数据存储到文件的时候只需：

```
jacksonObjectMapper().writeValue(File, List<String>)

```

而从文件中读取数据也是简单的狠啊：

```
//方式1
val list = jacksonObjectMapper().readValue<List<String>>(jsonFile)

//方式2
val list : List<String> = jacksonObjectMapper().readValue(jsonFile)

```

这种简洁真的是深入我心。继续深入了解下Jackson，你会发现它的可扩展性以及可定制性都很强，简直相见恨晚啊。之前也是在一个舒适圈待习惯了，这次主动跳出来居然有了意想不到的收获。

但是呢，每个框架也会有它自己的注意点，比如jackson，属性命名不可以是is开头，否则序列化等就会报错。这点似乎在阿里巴巴JAVA手册中好像也有提到，具体原因请大家自行百度（Google）。

## 资源的拷贝

当我们使用[java -jar xxx.jar]命令执行jar文件的时候，需要明确指定 jar文件的地址，但是在Compose Desktop中我们要怎么存放并读取这个jar文件呢 ？我们可以从Compose Desktop中读取并展示图片的相关代码中得到启发，假如有一个sample.svg图标文件存放到了项目的 resources 文件夹下，那么我们在引用这张图片的时候就可以使用：

```
painterResource("sample.svg")

```

我们点进去这个方法看下：

```
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun painterResource(
    resourcePath: String
): Painter = painterResource(
    resourcePath,
    ResourceLoader.Default
)

@ExperimentalComposeUiApi
@Composable
fun painterResource(
    resourcePath: String,
    loader: ResourceLoader
): Painter = when (resourcePath.substringAfterLast(".")) {
    "svg" -> rememberSvgResource(resourcePath, loader)
    "xml" -> rememberVectorXmlResource(resourcePath, loader)
    else -> rememberBitmapResource(resourcePath, loader)
}

```

里面居然有个ResourceLoader类，这名字一听就有戏啊，大概率就是我们需要的内容，而传递的默认参数是ResourceLoader.Default，那么就看下Default的源码吧：

```
//==========Resources.desktop.kt文件==========
@ExperimentalComposeUiApi
interface ResourceLoader {
    companion object {
        /**
         * Resource loader which is capable to load resources from `resources` folder in an application's
         * project. Ability to load from dependent modules resources is not guaranteed in the future.
         * Use explicit `ClassLoaderResourceLoader` instance if such guarantee is needed.
         */
        @ExperimentalComposeUiApi
        val Default = ClassLoaderResourceLoader()
    }
    fun load(resourcePath: String): InputStream
}

@ExperimentalComposeUiApi
class ClassLoaderResourceLoader : ResourceLoader {
    override fun load(resourcePath: String): InputStream {
        // TODO(https://github.com/JetBrains/compose-jb/issues/618): probably we shouldn't use
        //  contextClassLoader here, as it is not defined in threads created by non-JVM
        val contextClassLoader = Thread.currentThread().contextClassLoader!!
        val resource = contextClassLoader.getResourceAsStream(resourcePath)
            ?: (::ClassLoaderResourceLoader.javaClass).getResourceAsStream(resourcePath)
        return requireNotNull(resource) { "Resource $resourcePath not found" }
    }
}

//==========ClassLoader类==========
public InputStream getResourceAsStream(String name) {
    Objects.requireNonNull(name);
    URL url = getResource(name);
    try {
        return url != null ? url.openStream() : null;
    } catch (IOException e) {
        return null;
    }
}

public URL getResource(String name) {
    Objects.requireNonNull(name);
    URL url;
    if (parent != null) {
        url = parent.getResource(name);
    } else {
        url = BootLoader.findResource(name);
    }
    if (url == null) {
        url = findResource(name);
    }
    return url;
}

```

上述源码的整个逻辑基本上就是两步，根据资源文件名获取到资源文件，然后获取资源文件的输入流。看到这里其实我们已经有两种方案了：

- 方案一：直接拿到文件的URL然后获取到文件的路径
- 方案二：根据文件的输入流，将文件重新保存到本机相关目录

然而事情并没有这么简单，如果我们使用方案一，那么在编译运行的时候完全没有问题，所有的资源文件会被保存到【\build\processedResources\jvm】下，此时我们直接可以通过文件的URL获取到文件路径，然后调用即可。但是，当我们打包成安装包后，例如在Windows下使用packageMsi命令打包出msi文件并安装到电脑上后，运行程序，这时候你就会发现资源文件所在的路径就很奇怪，例如我的工程下是【C:\Program Files\工程名\app\工程名-jvm-1.0-SNAPSHOT-xxxxxx.jar**!/**资源文件名】，也就是说所有的资源文件被打包进了这个快照文件，如果此时直接使用该路径运行java -jar 等命令，那么肯定就会报错了。

所以最稳妥的方式还是使用方案二，使用ResourceLoader获取到资源文件流然后重新保存到本机上的相关目录就好了，伪代码如下：

```
ResourceLoader.Default.load(resourcesPath)
    .use { inputStream ->
        val fos = FileOutputStream(file)
        val buffer = ByteArray(1024)
        var len: Int
            while (((inputStream.read(buffer).also { len = it })) != -1) {
                fos.write(buffer, 0, len)
                }
          fos.flush()
              inputStream.close()
              fos.close()
          }

```

## 打包MSI

在Windows环境下打包Msi格式安装包的时候，有一个downloadWix的Task，该Task涉及到了Wix资源的下载，如下 ：

在IDEA中下载可能会非常的缓慢，此时我们可以复制上述地址，登上梯子，然后直接去GitHub下载。下载完毕后直接放入【/build/wixToolset】目录下即可，再次编译速度就会起飞了。

简直没想到啊，作为一个Android开发者，现在借助Compose Desktop开发起桌面端居然能这么的轻车熟路，我对Compose真是越来越喜欢了。

另外呢，跳出业务这一段时间来处理这些东西也让我对干预APK的打包等过程从理论迈出了实践的一步，同时对市场和运营同学的工作也有了更多了解，通过该工具帮助其处理了部分重复机械式的工作，部门间的感情也得到了进一步的增温（狗头滑稽）。

就编到这吧，桌面工具还需要持续的维护跟优化，基本是面向市场和运营同事编程了。关于开头说的Jenkins那一套其实早就写好了，是鄙人少有的万字长文，但是中间变故太大，一直也没发布出来，接下来会重新整理下并发布，还请大家多多指正。