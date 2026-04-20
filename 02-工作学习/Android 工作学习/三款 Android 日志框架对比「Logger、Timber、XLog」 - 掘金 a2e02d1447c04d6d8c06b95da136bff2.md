# 三款 Android 日志框架对比「Logger、Timber、XLog」 - 掘金

[https://juejin.cn/post/7274982412245925928](https://juejin.cn/post/7274982412245925928)

### 为什么需要日志框架

如果我们需要用三方库，那就意味着基于原生方案会存在一些痛点，我们不得不使用某种手段去解决这些痛点。那原生 Logcat 存在哪些痛点，我们来聊一聊：

- 日志不能持久化，缓冲区日志很容易丢失
- 如果系统压力大有可能会导致日志折叠、丢失
- 无法定义日志输出格式，如：json、xml
- 无法快速定位日志输出时的代码位置

其实前两个才是主要痛点，日志不丢失，有途径能获取到已打印的日志这是我们最基础的需求。只要日志不丢失，其他问题说实话都可以克服。

好，痛点知道了，那我们就要选择一个日志方案去解决这些痛点。不过 Github 上的日志框架有很多，选哪个呢？那不妨先聊下，我们希望日志框架能提供哪些功能。

### 期望中的日志框架能力

- 方案轻量，入侵度越低越好
- 集成方便、快捷，不应该在集成日志库上花费太多时间
- 日志留存，至少可以存储到本地磁盘
- 文件策略管理，处理文件备份、删除等策略
- 输出格式规整，最好能自定义输出格式
- 方便筛选，可根据日志级别、标签筛选
- 代码位置定位，可从 IDE 直接调到相关代码位置
- 配置丰富，给开发者高度的自由

如果日志框架能达到以上要求，那我们认为这个日志框架就比较不错了，那接下来我们就对比下 Github 上的这几款高星日志框架 Logger、Timber、XLog。

> 
> 
> 
> 以上我没有说到性能，其实仔细想来，在日志框架里，性能是一个伪需求。为什么？我们多数日志在正式版本上并不会打印，正式版本能打印的都是比较少的关键日志。而且，在 Android 里，我们提到的性能指标一般都是针对主线程，因为主线程的性能会反馈到界面上，能让用户感知到，所以主线程的工作效率一定要高。而耗时操作，比如说这里的日志 IO 操作，其实都在子线程执行，不会影响主线程，只要性能不要太差就行。
> 

### Logger

Logger [orhanobut/logger](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Forhanobut%2Flogger)是一个比较早期的日志框架，积累到现在的人气超高，拥有将近 14K 的 Star。这个库非常轻量，满打满算整个库只有 13 个类，你敢信！

集成比较简单，只需要指定输出日志的 Adapter

```
java复制代码FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
.showThreadInfo(true) // (Optional) Whether to show thread info or not. Default true
.methodCount(1) // (Optional) How many method line to show. Default 2
// .methodOffset(3) // (Optional) Skips some method invokes in stack trace. Default 0
.tag("My custom tag") // (Optional) Custom tag for each log. Default PRETTY_LOGGER
.build();

Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));

Logger.addLogAdapter(new DiskLogAdapter());

```

配置比较简单，可以配置控制台、文件输出。

[](%E4%B8%89%E6%AC%BE%20Android%20%E6%97%A5%E5%BF%97%E6%A1%86%E6%9E%B6%E5%AF%B9%E6%AF%94%E3%80%8CLogger%E3%80%81Timber%E3%80%81XLog%E3%80%8D%20-%20%E6%8E%98%E9%87%91/c2e9d7084e3344858a5b69d6f6ec34f6tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

日志输出格式还是挺好看的，可以直接输出 Collection、json、xml 类型数据，但是不能自定义输出格式。 日志可以保存到磁盘，但不能配置文件相关策略（文件名、备份、删除等），可以理解为，有存储文件功能，但不多。

前面也提到，这个框架非常轻量，只有十多个类，它可以满足我们基本的日志需求，将日志保存到文件，且不会丢失。日志输出格式也还不错。但相对而言，对于个性化的支持就比较欠缺了。比如，输出格式是不能简单自定义的，比如我如果只想输出一行日志，不输出表格线，那就会比较麻烦。

主观打分：

| 指标 | 分数 |
| --- | --- |
| Stars | 13K |
| 轻量 | 🌟🌟🌟🌟🌟 |
| 集成成本 | 🌟🌟🌟🌟🌟 |
| 日志留存 | 🌟🌟 |
| 日志管理策略 | ❌ |
| 输出格式规整 | 🌟🌟🌟 |
| 方便筛选 | ⭕️ |
| 代码位置定位 | ⭕️ |
| 配置灵活 | 🌟 |

### Timber

[](%E4%B8%89%E6%AC%BE%20Android%20%E6%97%A5%E5%BF%97%E6%A1%86%E6%9E%B6%E5%AF%B9%E6%AF%94%E3%80%8CLogger%E3%80%81Timber%E3%80%81XLog%E3%80%8D%20-%20%E6%8E%98%E9%87%91/2f89087c6cb549eba20f3bf1c077a58dtplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

Timber [JakeWharton/timber](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2FJakeWharton%2Ftimber)是 Jake Wharton 大神出品。他原话是：老是要把打日志这部分代码拷来拷去太麻烦了，所以以库的形式开源出来。Timber 与其他日志库不太一样的是它并没有提供很多功能，而是搭建了一个日志功能框架，大家可以按照自己的需求来构建自己的`Tree`。

前面我们说 Logger 库很简单只有十几个类，而 Timber 更简单，只有一个类文件，使用 Kotlin 语言。不过这些代码主要是框架代码，只有一个实现类`DebugTree`用来实现原生控制台输出日志,**可以自已自定义输出格式，可以不用指定 TAG，默认 TAG 为类名**，来看看如何使用。

```
java复制代码if (BuildConfig.DEBUG) {
    Timber.plant(new DebugTree());
} else {
    Timber.plant(new CrashReportingTree());
}

Timber.i("A button with ID %s was clicked to say '%s'.", button.getId(), button.getText());

```

一个很优秀的日志框架，如果你需要输出到文件、云端，那可以定义自己的`FileLoggingTree`、`CloudFileTree`,然后初始化时用 `Timber.plant()`方法，把自定的 Tree "种植"下去就行。

如果你需要将日志输出到文件的实现，那这个库是不支持的。它可以将日志多种输出形式集中成`Tree`，通过`Forest`去统一管理。

| 指标 | 分数 |
| --- | --- |
| Stars | 10K |
| 轻量 | 🌟🌟🌟🌟🌟 |
| 集成成本 | 🌟🌟🌟🌟🌟 |
| 日志留存 | ❌ |
| 日志管理策略 | ❌ |
| 输出格式规整 | 🌟🌟🌟🌟 |
| 方便筛选 | ⭕️ |
| 代码位置定位 | ❌ |
| 配置灵活 | 🌟🌟 |

### XLog

轻量、美观强大、可扩展的 Android 和 Java 日志库，可同时将日志打印在如 Logcat、Console 和文件中。如果你愿意，你可以将日志打印到任何地方。这是 XLog 的自我介绍。[elvishew/xLog: Android logger, pretty, powerful and flexible, log to everywhere, save to file, all you want is here. (github.com)](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Felvishew%2FxLog)

使用下来感觉确实如它自己所述：轻量、美观、强大、可扩展，它的星没有前两者那么多，但也不少，2.9K+。

日志输出：

[](%E4%B8%89%E6%AC%BE%20Android%20%E6%97%A5%E5%BF%97%E6%A1%86%E6%9E%B6%E5%AF%B9%E6%AF%94%E3%80%8CLogger%E3%80%81Timber%E3%80%81XLog%E3%80%8D%20-%20%E6%8E%98%E9%87%91/aebb56b53a8040d4aa937bc7dfddb1e4tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

可以看到这个输出格式与 **Logger** 还是挺像的，不同的是 **XLog** 可以自定义输出格式，**Logger** 不行。就比如这些花里胡哨的 **boder**，在 **XLog** 里可以方便配置，而 **Logger** 则麻烦不少。

配置：

```
java复制代码LogConfiguration config = new LogConfiguration.Builder()
    .logLevel(BuildConfig.DEBUG ? LogLevel.ALL             // 指定日志级别，低于该级别的日志将不会被打印，默认为 LogLevel.ALL
        : LogLevel.NONE)
    .tag("MY_TAG")                                         // 指定 TAG，默认为 "X-LOG"
    .enableThreadInfo()                                    // 允许打印线程信息，默认禁止
    .enableStackTrace(2)                                   // 允许打印深度为 2 的调用栈信息，默认禁止
    .enableBorder()                                        // 允许打印日志边框，默认禁止
    .jsonFormatter(new MyJsonFormatter())                  // 指定 JSON 格式化器，默认为 DefaultJsonFormatter
    .xmlFormatter(new MyXmlFormatter())                    // 指定 XML 格式化器，默认为 DefaultXmlFormatter
    .throwableFormatter(new MyThrowableFormatter())        // 指定可抛出异常格式化器，默认为 DefaultThrowableFormatter
    .threadFormatter(new MyThreadFormatter())              // 指定线程信息格式化器，默认为 DefaultThreadFormatter
    .stackTraceFormatter(new MyStackTraceFormatter())      // 指定调用栈信息格式化器，默认为 DefaultStackTraceFormatter
    .borderFormatter(new MyBoardFormatter())               // 指定边框格式化器，默认为 DefaultBorderFormatter
    .addObjectFormatter(AnyClass.class,                    // 为指定类型添加对象格式化器
        new AnyClassObjectFormatter())                     // 默认使用 Object.toString()
    .addInterceptor(new BlacklistTagsFilterInterceptor(    // 添加黑名单 TAG 过滤器
        "blacklist1", "blacklist2", "blacklist3"))
    .addInterceptor(new MyInterceptor())                   // 添加一个日志拦截器
    .build();

Printer androidPrinter = new AndroidPrinter(true);         // 通过 android.util.Log 打印日志的打印器
Printer consolePrinter = new ConsolePrinter();             // 通过 System.out 打印日志到控制台的打印器
Printer filePrinter = new FilePrinter                      // 打印日志到文件的打印器
    .Builder("<日志目录全路径>")                             // 指定保存日志文件的路径
    .fileNameGenerator(new DateFileNameGenerator())        // 指定日志文件名生成器，默认为 ChangelessFileNameGenerator("log")
    .backupStrategy(new NeverBackupStrategy())             // 指定日志文件备份策略，默认为 FileSizeBackupStrategy(1024 * 1024)
    .cleanStrategy(new FileLastModifiedCleanStrategy(MAX_TIME))     // 指定日志文件清除策略，默认为 NeverCleanStrategy()
    .flattener(new MyFlattener())                          // 指定日志平铺器，默认为 DefaultFlattener
    .writer(new MyWriter())                                // 指定日志写入器，默认为 SimpleWriter
    .build();

XLog.init(                                                 // 初始化 XLog
    config,                                                // 指定日志配置，如果不指定，会默认使用 new LogConfiguration.Builder().build()
    androidPrinter,                                        // 添加任意多的打印器。如果没有添加任何打印器，会默认使用 AndroidPrinter(Android)/ConsolePrinter(java)
    consolePrinter,
    filePrinter);

```

**XLog** 的配置非常丰富、灵活，所以它比前两个库类要多一些，50+左右，也算非常轻量。如果你不想自定义配置，只需要`XLog.init(LogLevel.ALL);`即可完成初始化，全部使用 **XLog** 缺省配置。

**XLog** 的配置非常多，主要分为几大类，我们来看下配置接口的目录结构：

```
arduino复制代码├── formatter
│ ├── Formatter.java  // 日志输出格式化接口
│ ├── border
│ │ └── BorderFormatter.java  // 装饰线格式化接口
│ ├── message
│ │ ├── json
│ │ │ └── JsonFormatter.java  // json 格式化接口
│ │ ├── object
│ │ │ └── ObjectFormatter.java  // 对象格式化接口
│ │ ├── throwable
│ │ │ └── ThrowableFormatter.java  // 异常格式化接口
│ │ └── xml
│ │ └── XmlFormatter.java  // xml 格式化接口
│ ├── stacktrace
│ │ └── StackTraceFormatter.java  // 堆栈格式化接口
│ └── thread
│ └── ThreadFormatter.java  // 线程id、name 输出格式化
├── interceptor
│ └── Interceptor.java  // 拦截器
└── printer
│ └── Printer.java //日志输出接口
├── file
│ ├── backup
│ │ ├── BackupStrategy.java  // 日志备份策略接口
│ │ └── BackupStrategy2.java
│ ├── clean
│ │ └── CleanStrategy.java  // 日志清除策略接口
│ ├── naming
│ │ └── FileNameGenerator.java  // 文件命名接口
│ └── writer
│   └── Writer.java  // 文件输入接口
└── flattener
  └── LogFlattener.java // 日志字段排列接口

```

配置主要分为以下几类：

- 日志打印
- 日志格式化
- 文件输入
- 文件备份策略
- 文件清除策略
- 日志字段排列

**XLog** 的架构思想与 **Timber** 差不多是一致的，日志框架基于功能接口管理所有日志输出，框架本身的日志输出实现一样是基于框架定义的接口。不同的是，**XLog** 接口定义得更细致，有二十多个接口。同时，框架本身也有所有接口的全部默认实现，这些实现就已经可以满足部分开发者。如果不满足，那 **XLog** 的高可配置性就体现出来了，你可以基于框架定义自己的实现。

```
java复制代码public static void init(LogConfiguration logConfiguration, Printer... printers) {
    ......
    sPrinter = new PrinterSet(printers);
    sLogger = new Logger(sLogConfiguration, sPrinter);
}

```

此方法是初始化入口，假如你不想用默认的文件输出方式，你可以基于`Printer`接口重新实现一个`CustomFilePrinter`。或者你想将日志输出到云端，可以定义个`CloudLogPrinter`,然后初始化时使用自定义的`Printer`，像这样：

```
scss复制代码XLog.init(
    config,
    androidPrinter,
    CustomFilePrinter(),
    CloudLogPrinter()
);

```

另外，如果你的工程里有其他第三方日志或 Android 原生日志输出，也希望这些地方的日志也能输出到文件，**XLog**可以提供一个简单的方式，让你不需要改代码便能输出日志到文件。

```
java复制代码// Intercept all logs(including logs logged by third party modules/libraries) and print them to file.
LibCat.config(true, filePrinter);

```

**这个操作还是蛮实用的吧？**

**XLog** 的其他配置细节就不跟大家细聊了，其配置能力就是上述那几类，代码逻辑架构很清晰，逻辑也比较简单大家可以自行了解，总之是一个比较优秀的日志库。

老规矩，打个分：

| 指标 | 分数 |
| --- | --- |
| Stars | 2.9K |
| 轻量 | 🌟🌟🌟🌟 |
| 集成成本 | 🌟🌟🌟🌟 |
| 日志留存 | ⭕️ |
| 日志管理策略 | 🌟🌟🌟🌟🌟 |
| 输出格式规整 | 🌟🌟🌟🌟🌟 |
| 方便筛选 | ⭕️ |
| 代码位置定位 | ⭕️ |
| 配置灵活 | 🌟🌟🌟🌟🌟 |

### 其他

Log4j、logback 也是比较优秀的日志框架，不过他们更主要应用场景是在服务端。当然如果一定要在 Android 里使用它们也不是不行，就是需要比较深度的定制、封装。而且，相对而言，这两个库要比上述几个要复杂很多，也比较重，本篇文章就不做介绍了，感兴趣的同学可以从其他途径了解。

到这里关于 Android 比较流行的这三个日志框架对比到这边就差不多了，**Logger** 相对而言比较老了，对于现在的项目可能会有些水土不服。另外两个都比较优秀，区别就是**Timber**的配置没有**XLog**那么丰富，也没有默认的文件输出实现。

就这三个日志框架，目前来看 **XLog** 的适应场景会更广泛些。当然，没有最好的，只有最合适的，大家可以根据自己的需求来选择开源的框架。也欢迎大家多多交流！