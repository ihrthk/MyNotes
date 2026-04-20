# 体验一下使用 ArkUI 进行 HarmonyOS 开发并与 Compose 简单对比 - 掘金

[https://juejin.cn/post/7291186128363159608](https://juejin.cn/post/7291186128363159608)

最近几年各个技术公众号和技术群都在唱衰原生安卓开发，疯狂贩卖焦虑。

搞得我也焦虑的不行，在谷歌的 Compose 推出后就赶紧去学，但是又觉得好像 Compose 的热度也不算太高，又去学 Flutter 。

转头两个都还没学明白呢，大佬们又在说鸿蒙下次更新不兼容安卓了，再不学鸿蒙开发就等着失业吧。

啊？这？这能忍？这必须学啊！

于是抽出时间来简单了解了一下使用 ArkUI 的鸿蒙应用开发。

# 编写第一个鸿蒙应用

## 搭建环境

鸿蒙应用开发有它自己的一个 IDE ，叫做 *DevEco Studio* 。

可以在 [官网](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.harmonyos.com%2Fcn%2Fdevelop%2Fdeveco-studio%2F) 下载到，按照你自己的系统选择对应的安装包后直接下载安装即可。

安装过程不用我多介绍吧，毕竟都是老程序员了，这点基础知识还是得有吧。

然后，等你安装完打开你就会发现，这个 IDE 怎么会这么眼熟呢？

眼熟就对了啊，因为：

[](%E4%BD%93%E9%AA%8C%E4%B8%80%E4%B8%8B%E4%BD%BF%E7%94%A8%20ArkUI%20%E8%BF%9B%E8%A1%8C%20HarmonyOS%20%E5%BC%80%E5%8F%91%E5%B9%B6%E4%B8%8E%20Compose%20%E7%AE%80%E5%8D%95%E5%AF%B9%E6%AF%94%20-%20%E6%8E%98%E9%87%91/f51499eff2034f3da2a987f706b127d1tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

它也是基于 Intellij PLatform 开发的。

安装好后，首次打开 DevEco Studio 会要求你进行环境下载和配置，因为鸿蒙开发使用的 ArkUI 框架是基于 ArkTS 语言的，而 ArkTS 语言是 TS 语言的超集，所以我们需要配置对应的环境。

按照 IDE 提示首先下载并安装 Node.js 和 Ohpm ，因为上面说过 ArkTS 是基于 TS 的超集，所以需要 Node.js 的支持；而 Ohpm 是华为自己的依赖管理工具。

安装好 Node.js 和 Ohpm 后需要安装鸿蒙的 SDK ，依旧是按照 IDE 提示直接下载安装即可。

安装完成后即可正常使用 IDE 了。

## Hello, World!

打开刚安装配置好的 DevEco Studio，在欢迎页面选择 *Create Project* - *Application* - *Empty Ability* 然后点击 *Next* ：

[](%E4%BD%93%E9%AA%8C%E4%B8%80%E4%B8%8B%E4%BD%BF%E7%94%A8%20ArkUI%20%E8%BF%9B%E8%A1%8C%20HarmonyOS%20%E5%BC%80%E5%8F%91%E5%B9%B6%E4%B8%8E%20Compose%20%E7%AE%80%E5%8D%95%E5%AF%B9%E6%AF%94%20-%20%E6%8E%98%E9%87%91/86b4b08b2a9e4db085e062535f9069adtplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

在这个页面会要求填写一些应用的基本信息，和安卓开发类似：

- Project name 是项目名称
- Bundle name 是包名，也是应用的唯一 ID
- Save location 就是项目保存位置
- Compile SDK 要使用的编译 SDK 版本
- Model 应用程序开发模型，Stage 是新版的模型，FA 是老版本使用的模型，这里选择 Stage 即可，他俩的区别看 [这里](https://link.juejin.cn/?target=https%3A%2F%2Fdocs.openharmony.cn%2Fpages%2Fv3.2%2Fzh-cn%2Fapplication-dev%2Fapplication-models%2Fapplication-model-description.md%2F) （吐槽一下，华为的文档链接居然是错的，还得我自己去搜索正确的文档）

修改完后点击 `Finsh` 即可创建我们的第一个 Hello World 项目。

打开这个项目的主页UI文件：

`./entry/src/main/ets/pages/index.ets`

然后在右边侧栏点击 Previewer 按钮即可打开 UI 预览：

[](%E4%BD%93%E9%AA%8C%E4%B8%80%E4%B8%8B%E4%BD%BF%E7%94%A8%20ArkUI%20%E8%BF%9B%E8%A1%8C%20HarmonyOS%20%E5%BC%80%E5%8F%91%E5%B9%B6%E4%B8%8E%20Compose%20%E7%AE%80%E5%8D%95%E5%AF%B9%E6%AF%94%20-%20%E6%8E%98%E9%87%91/e46ffeff4dbf48699598b06c10b152dctplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

如果想要运行这个程序的话，我们需要创建一个模拟器：

[](%E4%BD%93%E9%AA%8C%E4%B8%80%E4%B8%8B%E4%BD%BF%E7%94%A8%20ArkUI%20%E8%BF%9B%E8%A1%8C%20HarmonyOS%20%E5%BC%80%E5%8F%91%E5%B9%B6%E4%B8%8E%20Compose%20%E7%AE%80%E5%8D%95%E5%AF%B9%E6%AF%94%20-%20%E6%8E%98%E9%87%91/53b3d977a49d469e8c362d75844d671etplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

在顶栏菜单中的运行按钮旁边点击 *No devies* 下拉框，选择 *Device Manager*，然后按照提示创建一个模拟器并启动。

（吐槽一下华为魔改的这个 IDE 明明我已经创建过模拟器了，却不能像 Android Studio 一样直接在运行菜单中选择这个模拟器并直接一键启动运行，非得手动进入 *Device Manager* 里面启动了模拟器才能运行程序）

启动好模拟器后，运行菜单应该已经默认选中了这个模拟器，点击运行图标运行即可：

[](%E4%BD%93%E9%AA%8C%E4%B8%80%E4%B8%8B%E4%BD%BF%E7%94%A8%20ArkUI%20%E8%BF%9B%E8%A1%8C%20HarmonyOS%20%E5%BC%80%E5%8F%91%E5%B9%B6%E4%B8%8E%20Compose%20%E7%AE%80%E5%8D%95%E5%AF%B9%E6%AF%94%20-%20%E6%8E%98%E9%87%91/194d83e3b7304da9bfe1fdafbbe6bd0btplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

这样我们就能看到它的运行效果了：

[](%E4%BD%93%E9%AA%8C%E4%B8%80%E4%B8%8B%E4%BD%BF%E7%94%A8%20ArkUI%20%E8%BF%9B%E8%A1%8C%20HarmonyOS%20%E5%BC%80%E5%8F%91%E5%B9%B6%E4%B8%8E%20Compose%20%E7%AE%80%E5%8D%95%E5%AF%B9%E6%AF%94%20-%20%E6%8E%98%E9%87%91/29048300f8224e4ca97c73235ccb4cb2tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

## 项目结构

接下来，我们来了解一下 ArkUI 的项目的结构，不然我们连需要改哪儿个文件都不知道了，哈哈。

项目整体结构如图：

[](%E4%BD%93%E9%AA%8C%E4%B8%80%E4%B8%8B%E4%BD%BF%E7%94%A8%20ArkUI%20%E8%BF%9B%E8%A1%8C%20HarmonyOS%20%E5%BC%80%E5%8F%91%E5%B9%B6%E4%B8%8E%20Compose%20%E7%AE%80%E5%8D%95%E5%AF%B9%E6%AF%94%20-%20%E6%8E%98%E9%87%91/657832cf0550448d9b8138dadadc9bb0tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

其中，

### AppScope

这个目录存放应用全局所需要的资源文件。

在其下有一个 `app.json5` 配置文件，该文件是应用的全局配置文件，一个典型的全局配置文件包含以下内容：

```
json复制代码{
  "app": {
    "bundleName": "com.example.myapplication",
    "vendor": "example",
    "versionCode": 1000000,
    "versionName": "1.0.0",
    "icon": "$media:app_icon",
    "label": "$string:app_name"
  }
}

```

其中的配置分别表示：

- bundleName 包名。
- vendor 应用程序供应商。
- versionCode 数字版本号。
- versionName 版本名称。
- icon 应用图标。
- label 应用名。

另外，在该目录下还有一个 `resources` 文件夹，用于存放公共资源文件，它包括两个文件夹：

- ./base/element 主要存放公共的字符串、布局文件等资源。
- ./base/media 存放全局公共的多媒体资源文件。

### entry

这个是 entry 模块的目录，在这个项目中 entry 即为主模块。

在这个模块中可以存放代码、资源等。

其中，./entry/src 包含两个文件夹：

- main/ 主目录
- ohosTest/ 单测目录

在 ./entry/src/main 文件夹中包括以下目录和文件：

- ets/ 用于存放ets代码
- resources/ 存放模块内的多媒体及布局文件等
- module.json5 模块的配置文件

而 ./entry/src/main/ets 下又有以下目录：

- entryability/ 存放 ability 文件，用于当前ability应用逻辑和生命周期管理。
- pages/ 存放UI界面相关代码文件，初始会生成一个Index页面。

而 ./entry/src/main/resources 目录用于存放模块公共的多媒体、字符串及布局文件等资源，分别存放在element、media文件夹中。

在 ./entry 目录下还有几个文件：

- build-profile.json5 模块级配置信息，包括编译构建配置项。
- hvigorfile.ts 模块级构建脚本。
- oh-package.json5 模块级依赖配置信息文件。

### oh_modules

该目录是项目的依赖包，存放工程依赖的源文件。

一般不需要我们去手动更改。

### build-profile.json5

该文件是项目级配置信息，包括签名、产品配置等。

### hvigorfile.ts

该文件是工程级编译构建任务脚本，hvigor是基于任务管理机制实现的一款全新的自动化构建工具，主要提供任务注册编排，工程模型管理、配置管理等核心能力。

### oh-package.json5

该文件是项目级依赖配置文件，用于记录引入包的配置信息。

## 简单的代码理解

上面简单梳理了一下项目的目录结构，相信大家心里也大概有个底了，其实和安卓项目也大差不差，都是这么一回事儿。

只是依赖管理系统变了（安卓是 gradle ），配置文件变了（安卓是 groovy 或 kts 鸿蒙是 json）。

其他结构对于安卓开发者来说基本属于一看就懂。

接下来我们来看下代码结构。

首先，我们得先找到代码的入口点。

通过上面的介绍，我们已经知道了这个项目的主模块是 entry，所以入口点需要在这个模块目录中寻找。

在该模块的配置文件 `module.json5` 中，配置了入口 `ability`：

也就是说，这个 `./ets/entryability/EntryAbility.ts` 文件就是我们要找的入口点。

打开这个文件，安卓开发者理解起来应该不难，似乎，这个 ability 的概念有点像 Activity ？

可以看到这个 `EntryAbility` 继承自 `UIAbility` 并实现了其中的 `onCreate` 、`onDestroy` 、 `onWindowStageCreate` 、 `onWindowStageDestroy` 、 `onForeground` 、 `onBackground` 等方法，显然，这些方法就是这个 ability 的生命周期嘛。

然后在 `onWindowStageCreate` 生命周期中通过 `windowStage.loadContent` 加载了 `pages/Index` 的内容。

我们再来看看这个 `pages/Index` 的内容：

这个就是 ArtUI 的内容了，看起来还是非常的熟悉是吧？也是一看就懂。

`Row` 嵌套 `Column` 再添加一个 `Text` 组件，构成了这个 Hello, World 的 UI 界面。

接下来，我们简要介绍一下上述代码出现的几个要点。

### Ability

在上文中，我们说感觉 Ability 有点类似于安卓的 Aciticity ，这其实有一丝道理，但是又不完全对。

在上述代码中，我们使用的是继承自 Ability 的 UIAbility，根据官方介绍：

> 
> 
> 
> UIAbility是一种包含用户界面的应用组件，主要用于和用户进行交互。UIAbility也是系统调度的单元，为应用提供窗口在其中绘制界面。
> 
> 每一个UIAbility实例，都对应于一个最近任务列表中的任务。
> 
> 一个应用可以有一个UIAbility，也可以有多个UIAbility。
> 
> 一个UIAbility可以对应于多个页面。
> 

从这段介绍中可以看出，虽然 Activity 和 UIAbility 都是程序的 UI 界面的承载组件，且每个程序都可以有一个或多个 UIAbility(Activity)，每个 UIAbility(Activity) 也可以对应一个或多个页面。但是，多个 Activity 并不会出现多个页面在最近任务栏中（一般情况下），而一个 UIAbility 就对应了最近任务栏中的一个页面。

另外，和 Activity 一样，UIAbility 也是系统调度的单元，具有自己的生命周期，UIAbility 的生命周期如下：

它的生命周期流程和 Acticity 类似。

最后，UIAbility 也具有不同的启动模式，但是相比 Activity 的启动模式概念略有不同。

它只有三种启动模式：singleton（单实例模式）、multiton（多实例模式）和specified（指定实例模式）

singleton 模式会确保当前应用进程中的同一个 UIAbility 仅有一个，当以该模式启动某个 UIAbility 时，如果进程中已存在相同的 UIAbility 实例，则会直接复用该实例。

multiton 模式会在每次以该模式启动 UIAbility 时都创建一个新的 UIAbility 实例，而无论这个 UIAbility 是否已经在当前进程中存在。

specified 模式则可以在创建 UIAbility 时指定一个 key 与之绑定，之后打开新的 UIAbility 时都需要提供一个 key，如果该 key 存在已经绑定了的 UIAbility 则会直接复用这个 UIAbility，否则用这个 key 创建一个新的 UIAbility。

### ArkUI

接着，我们再来看看具体的 UI 部分（pages/Index），显然这里的 UI 使用的是 ArkUI 实现的。

在 ArkUI 中通过 `struct` 关键字来声明一个 UI 组件：

`struct Index { }`

然后通过 `@Component` 装饰表明 `Index` 是一个自定义的 ArkUI 组件：

```
ts复制代码@Component
struct Index { }

```

同样的，还有一个叫做 `@Entry` 的装饰用于指明该组件是该页面的入口组件：

```
ts复制代码@Entry
@Component
struct Index { }

```

经过上述操作，我们已经声明好了 `Index` 页面的入口自定义组件。

接下来只需要在 `Index` 添加描述这个组件的内容即可，为此，我们需要在其中添加一个 `build() { }`：

```
ts复制代码@Entry
@Component
struct Index {
  // ……
  build() {
    // …… 在此编写布局
  }
}

```

我们可以在 `build` 中添加内置 UI 组件或自定义组件用于描述我们的页面组成，例如样例中的居中显示一个 "Hello, World" 文本：

```
ts复制代码@Entry
@Component
struct Index {
  build() {
    Row() {
      Column() {
        Text("hello, world")
      }
      .width('100%')
    }
    .height('100%')
  }
}

```

我们还可以通过链式调用的方式，修改组件的参数：

```
ts复制代码Text(”hello, world“)
  .fontSize(50)
  .fontWeight(FontWeight.Bold)

```

这样，就把文本修改为字号 50，字重 加粗。

当然，上面的界面是没有状态的，作为声明式 UI 当然要给它定义一个状态了。

首先，我们声明一个 `message` 变量表示需要显示的文字：

`message: string = 'Hello World'`

然后，为该变量加上 `@State` 装饰即表示将该变量与 UI 绑定，当该变量值发生改变时，使用到该变量的 UI 也会自动重新渲染：

`@State message: string = 'Hello World'`

为了能够看到状态改变的效果，我们为这个文本添加一个点击事件，并在点击后更改 `message` 值：

```
ts复制代码Text(this.message)
  .fontSize(50)
  .fontWeight(FontWeight.Bold)
  .onClick(() => {
    this.message = `我被点击了 ${this.count} 次`;
    this.count++;
  })

```

现在，只要我们点击这个文本，它就会自动变更为 “我被点击了 xx 次”，其中 xx 是点击次数：

该部分完整代码如下：

```
ts复制代码@Entry
@Component
struct Index {
  count: number = 1
  @State message: string = 'Hello World'

  build() {
    Row() {
      Column() {
        Text(this.message)
          .fontSize(50)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            this.message = `我被点击了 ${this.count} 次`;
            this.count++;
          })
      }
      .width('100%')
    }
    .height('100%')
  }
}

```

# 对比 Compose

在上文中，我们已经完成了一个简单的使用 ArkUI 的鸿蒙应用，接下来，我们用 Compose 实现一个同样布局同样功能的应用做一个简单的对比。

首先是 UI 布局的承载者，

在 Compose 中使用的是 Activity：

```
kotlin复制代码class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Greeting()
        }
    }
}

```

在 Activity 的 `onCreate` 生命周期通过 `setContent` 设置具体的 UI 实现。

而在鸿蒙中则是使用 UIAbility：

```
ts复制代码export default class EntryAbility extends UIAbility {

  onWindowStageCreate(windowStage: window.WindowStage) {
    windowStage.loadContent('pages/Index', (err, data) => {});
  }
}

```

在 UiAbility 的 `onWindowStageCreate` 生命周期，通过 `windowStage.loadContent` 设置具体的 UI 实现。

而在具体的 UI 实现部分，Compose 代码如下：

```
kotlin复制代码@Composable
fun Greeting() {
    var count by remember { mutableStateOf(1) }
    var message by remember { mutableStateOf("Hello, World") }

    Row(
        modifier = Modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    message = "我被点击了 $count 次"
                    count++
                }
            )
        }
    }
}

```

ArkUI 代码如下：

```
ts复制代码@Entry
@Component
struct Index {
  count: number = 1
  @State message: string = 'Hello World'

  build() {
    Row() {
      Column() {
        Text(this.message)
          .fontSize(50)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            this.message = `我被点击了 ${this.count} 次`;
            this.count++;
          })
      }
      .width('100%')
    }
    .height('100%')
  }
}

```

需要注意的是，在 Compose 的实现中，我是为了尽可能的完全模拟 ArkUI 的界面才这样写的，实际上如果想要实现文本居中根本不需要嵌套 `Row` 和 `Column` ，直接使用其中任意一个然后:

```
kotlin复制代码Row(
    modifier = Modifier.fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
) {
     // ……
}

```

另外，其实 ArkUI 也不用嵌套，也是直接使用任意一个，然后：

```
ts复制代码Row() {
  // ……
}
.height('100%')
.width('100%')
.justifyContent(FlexAlign.Center)

```

猜测这里的 Hello World 示例使用了 Row 嵌套 Column 是为了更多的展示 ArkUI 的特性吧。

另外，这里的 `Column` 和 `Row` 在默认对齐方式上也有所区别，以 `Column` 为例，在 ArkUI 中默认是主轴（垂直方向）在 Top ，副轴（水平方向）居中；而 Compose 中则是默认垂直方向（主轴）在 Top，水平方向（副轴）在 Start 。

对于自定义组件的声明方式也有所不同，在 ArkUI 中是通过 `struct` 结构附加 `@Component` ；而 Compsoe 则是通过在一个函数上附加 `@Composable` 表示。

通过上述 Compose 和 ArkUI 的代码对比，我们还可以看出，ArkUI 声明有状态的变量只需要在前面加上 `@State` 装饰即可； Compose 则需要将其置于 `mutableStateOf()` 中，并且 Compose 在 Composeable 作用域中的变量还需要包裹在 `remember` 中，否则每次重组（UI变化）这个变量都会被重复初始化。

另外，对于组件参数的修改，在 Compose 中是通过直接给这个组件函数的参数传值实现，例如修改文本的字号和字重：

```
kotlin复制代码Text(
    text = message,
    fontSize = 50.sp,
    fontWeight = FontWeight.Bold,
)

```

当然，每个组件需要修改的参数也许会有很多，所以不可能每个参数都要给组件函数加一个形参，这会让函数很臃肿，所以在 Compose 中几乎每个组件都有一个 `modifier` 参数，这个参数中集成了非常多的常用参数修改，例如上述例子中的添加点击监听就是通过 `modifier` 添加的。

而这个 `modifier` 的参数修改也是类似于 ArkUI 的链式调用。

对于 ArkUI 来说，所有参数的修改则完全是使用在组件函数之后链式调用来实现的，例如修改字号和字重：

```
ts复制代码Text(this.message)
  .fontSize(50)
  .fontWeight(FontWeight.Bold)

```

以上就是对 Compose 和 ArkUI 的简单对比。

# 总结

经过上述的简单了解，我们可以发现，其实不管是 Android 的 Jetpack Compose 还是 Apple 的 SwiftUI 还是刚才说的鸿蒙的 ArtUI，其实核心思想都是差不多的（毕竟都是声明式 UI 的思想），对于我们普通程序员来说写起来也是差不多的感觉，无非是语法和部分风格上略微有区别。

另外前言中说的事件既是段子也是一部分现实，但是还是希望各位能弄清楚自己的定位，有一个明确的规划，不要东一榔头西一棒子的看见啥有热度就学啥。

当然，有余力的话多了解了解其他技术还是挺有帮助的。

[Android 使用 MvRx+Epoxy 构建 MVVM 应用 - 掘金](%E4%BD%93%E9%AA%8C%E4%B8%80%E4%B8%8B%E4%BD%BF%E7%94%A8%20ArkUI%20%E8%BF%9B%E8%A1%8C%20HarmonyOS%20%E5%BC%80%E5%8F%91%E5%B9%B6%E4%B8%8E%20Compose%20%E7%AE%80%E5%8D%95%E5%AF%B9%E6%AF%94%20-%20%E6%8E%98%E9%87%91/Android%20%E4%BD%BF%E7%94%A8%20MvRx+Epoxy%20%E6%9E%84%E5%BB%BA%20MVVM%20%E5%BA%94%E7%94%A8%20-%20%E6%8E%98%E9%87%91%20e1ebc4bc7c8b4dcf847037f58c4321da.md)