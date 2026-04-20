# 云音乐Android Cronet接入实践 - 掘金

> 
> 
> 
> 本文作者：[答案]
> 

# 背景

网易云音乐产品线终端类型广泛，除了移动端（IOS/安卓）之外，还有PC、MAC、Iot多终端等等。移动端由于上线时间早，用户基数大，沉淀了一些端侧相对比较稳定的网络策略和网络基础能力。然而由于各端在基础能力上存在不对齐的现状：移动端双端在这些能力细节上有差异，同时PC、MAC这方面能力相较于移动端又略微滞后。为了避免各端在网络侧反复投入人力进行能力维护和定位解决问题，同时统一网络基础设置，将端侧稳定网络策略进行沉淀复用，经过调研，我们计划采用 Google chromium项目的 Cronet 作为跨端通用网络库。Cronet 在chrome 中经过多年的打磨，稳定性得到了验证，同时 Cronet 支持 QUIC 协议，可以支持后期对弱网场景进行专项优化。安卓端作为 Cronet 的首先落地一端，已经全量在线上运行了一年多的时间，本文主要介绍接入方案和过程中解决的问题。

# 介绍

### Cronet 网络库

Cronet是 google chromium 的网络组件，可单独编译成库提供给 Android/Ios 应用使用。Cronet在性能方面表现出色，目前已经有 Youtube、Goolge 全家桶等大量应用使用 Cronet 作为网络模块。

它有以下功能：

- 支持 HTTP2/QUIC/websocket 协议
- 支持对请求设置优先级标签
- 可以使用内存缓存或磁盘缓存来存储资源
- 支持 Brotlin 压缩（有研究表明，对于文本文件，相同的压缩质量下，brotlin 通常比 gzip 高出了20%的压缩率）

### 接入方案

目前项目中使用 Okhttp 作为网络基础库，Cronet 的对外接口和 Okhttp 无法兼容，在接入上主要有两个方向：

1. 网络业务接口根据新的 Cronet 接口重新封装接口层，逐步替换老接口；
2. 业务侧不做改动的情况下，底层入口统一切换到 Cronet 侧，通过中间的胶水层来抹平差异。

方向一需要对项目中的网络请求接口做改造，但由于项目中广泛使用了 Okhttp 的特性，例如 Interceptor、cookiestore、cache、eventlistener 等等，直接使用 Cronet 接口意味着这些特性全部需要重新实现，改造成本巨大；

方向二的实现思路是在 Cronet 的最底层通过创建 CronetInterceptor 来实现 Cronet 请求，并且将它放到 Okhttp Interceptors 的最末尾保证原有 interceptors 全部执行，同时通过适配层将 Okhttp 原有能力无缝桥接到Cronet实现，不对上层有任何侵入和改动，做到业务调用侧无感知。

P.S google 官方后来推出的 Cronet 接入库 [github.com/google/cron…](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fgoogle%2Fcronet-transport-for-okhttp%25EF%25BC%258C%25E4%25B9%259F%25E6%2598%25AF%25E5%2590%258C%25E6%25A0%25B7%25E7%259A%2584%25E6%2580%259D%25E8%25B7%25AF%25E3%2580%2582)

结合我们的项目现状，我们决定使用方向二的思路来接入 Cronet。

### Android 网络库整体架构

[](%E4%BA%91%E9%9F%B3%E4%B9%90Android%20Cronet%E6%8E%A5%E5%85%A5%E5%AE%9E%E8%B7%B5%20-%20%E6%8E%98%E9%87%91/53b1a796ceb74499a1aa382785b6448etplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

作为一个通用的网络模块，我们将整体抽象出了四层来展示，从底层到业务层方向分别为：协议层、通用能力、适配层、业务支撑层。

### 协议层

这部分主要是从 chromuim 中抽离出的 Cronet 源码部分，主要是 Cronet 的基础能力，包括了不同网络协议的实现以及 Cronet 内部的优化，是 Cronet 的最核心实现。这部分除了一些向上接口之外，通常不会对源码做过多的改动。

### 通用能力层

这一层主要包括我们从 java 侧沉淀到 C++ 层的一些通用网络策略和网络组件（APM、Httpdns等），这一层通过拆分出不同组件的方式相互隔离，共同依赖于协议层。

### 适配层

适配层定义为胶水层，主要目的是保证在上层接口无须做任何改动的情况下将底层实现在 Okhttp 和 Cronet 间进行切换。

### 业务支撑层

支撑端侧各种业务的能力，这层无须做改动。

### 适配方案

### okhttp接口适配

1、interceptor 适配

前文提到，通过创建 CronetInterceptor 且放到 okhttp addInterceptor 的最末尾保证 interceptor 全部执行，但是仍有部分 interceptors 是覆盖不到的，那就是 Okhttp 内置的 interceptor。内置的主要有：

```
复制代码RetryAndFollowUpInterceptor
BridgeInterceptor
CacheInterceptor
ConnectInterceptor
CallServerInterceptor

```

其中

```
复制代码RetryAndFollowUpInterceptor
BridgeInterceptor
CacheInterceptor

```

这三个主要负责重定向、鉴权、cookie、缓存等逻辑，和 Okhttp 的接口息息相关，这部分逻辑是我们主要适配的内容。适配也非常简单，只需要把这些 interceptor 的核心逻辑移植到我们创建的 CronetInterceptor 即可，这样就能保证上层业务使用到的 cookiestore、cache 等 okhttp api 不受影响。

```
复制代码ConnectInterceptor
CallServerInterceptor

```

这两个 interceptor 主要负责的是核心的网络请求的全部后续细节，Cronet 有自己来接管自然无需适配。

2、eventlistener适配

由于 okhttp eventlistener 依赖的一些回调例如 connectEnd、dnsEnd 等是在这两个拦截器中调用的，虽然Cronet 有自己的是 Callback：

```
复制代码
public abstract void onRedirectReceived(UrlRequest var1, UrlResponseInfo var2, String var3)
public abstract void onResponseStarted(UrlRequest var1, UrlResponseInfo var2)
public abstract void onReadCompleted(UrlRequest var1, UrlResponseInfo var2, ByteBuffer var3)
public abstract void onSucceeded(UrlRequest var1, UrlResponseInfo var2);
public abstract void onFailed(UrlRequest var1, UrlResponseInfo var2, CronetException var3);

```

但是没有 okhttp eventlistener 提供的全面，如果需要完整的实现 okhttp eventlistener，需要对 Cronet 的核心关键请求点做改造来透出给 java 层，考虑到成本和使用场景，我们没有对这部分做改造，而是直接采用 Cronet 的 callback 做桥接来实现了部分的核心 eventlistener 的 callback。

3、超时逻辑适配

业务侧指定请求的超时时间来做一些策略也是常见的操作，而 Cronet 并未提供超时相关的 api，于是我们基于Cronet 源码开发了建链超时和读流超时等能力

```
arduino复制代码void CronetURLRequest::SetOriginRequestID(uint32_t origin_request_id)
void CronetURLRequest::SetConnectTimeoutDuration(uint32_t connect_timeout_ms）

```

并通过 jni 暴露给 java 层，java 层通过适配层桥接到 Okhttp 接口：

*CronetUrlRequest.java类*

```
scss复制代码mRequestContext.onRequestStarted();
if (mInitialMethod != null) {
if (!nativeSetHttpMethod(mUrlRequestAdapter, mInitialMethod)) {
throw new IllegalArgumentException("Invalid http method " + mInitialMethod);
}
}
if (mRequestId > 0) {
nativeSetOriginRequestID(mUrlRequestAdapter, mRequestId);
}
// 将业务侧设置的超时时间传递到Cronet
if (connectTime > 0) {
nativeSetConnectTimeoutDuration(mUrlRequestAdapter, (int) connectTime);
}
// 将业务侧设置的超时时间传递到Cronet
if (readTime > 0) {
nativeSetReadTimeoutDuration(mUrlRequestAdapter, (int) readTime);
}

```

这样上层业务侧无需任何改动既可继续使用 Okhttp 原有能力。

### 网络请求适配

1、请求维度适配

发起请求时，由原先的通过 Okhttp 内置 interceptor 发起请求切换到使用 Cronet 发起请求后，需要在 Okhttp 接口到 Cronet 接口间做一下请求和响应的适配转换。

*网络请求切换示意*图

同时由于将之前的一些 java 层网络策略下沉到 C++ 实现，之前的一些 java 层的直接调用和传参我们通过基于CronetUrlRequest 进行扩展打通了向 Cronet 的 jni 调用

2、全局调用适配

下沉到 C++ 的网络策略，为尽可能做到和 Cronet 原有代码的解耦，在 C++ 以一个个独立插件形式存在。java 侧通过 CronetRequestContext 设置到 C++ 侧，然后向对应注册的组件进行分发，这个链路上涉及到 java、jni 和C++ 的代码改动，为了降低后续网络策略的开发维护成本，采用了类 JsBridge 的方法，开发了'CppBridge'，将java 和 C++ 之间的方法调用协议化，通过 json 传递数据，这样避免了后续对插件做更新带来的 java 到 C++ 请求链路上繁琐的开发工作，且 C++ 策略可以通过java层的配置中心能力进行动态配置。

### 解决问题

1、线程优化

众所周知，网络请求需要在子线程中发起，在 Cronet 的官方文档介绍中，推荐通过传入 Executor 来负责执行网络请求：

然后在 okhttp interceptor 中已经是子线程的执行环境，如果仍然传入独立对 executor，会造成不必要的线程切换和时间消耗。通过查看 Cronet 源码，发现其 CronetHttpURLConnection 使用的 MessageLoop 类实现是在当前线程，使用 MessageLoop 即可减少不必要的多余线程引入。

*通过 MessageLoop 请求生命周期*

2、兼容性解决

不同网络库之间切换，兼容性问题在所难免。虽然同样遵循 http 协议，但是对于一些边界条件的处理不一致或处理严格程度不同也会引起兼容性偏差。篇幅所限，这里仅介绍几个兼容点：

1. 
    
    Cronet 库对于http链接数设置为了6个，如果有对于 http 请求的不当使用，例如不正常持有未释放，一旦达到了6个，后续的请求将会 block 直到前序连接资源释放，这在 http1.1 下更容易触发；
    
2.   
    
    Cronet 对请求做了检测，如请求 body 未设置 Content-Type，将会直接抛出异常，
    
    ```
    arduino复制代码if (!hasContentType) {
        throw new IllegalArgumentException("Requests with upload data must have a Content-Type.");
    }
    
    ```
    
    在某些特殊设置情况下，存在有 request body 未设置 Content-Type 的情况将会直接导致请求抛异常；
    
3. 
    
    Cronet 请求返回4xx时，会直接抛出异常，而 okhttp 是通过将结果连带 code 返回到上层，交由使用者自己去处理。
    

兼容性优化没有统一的解决办法，只能见招拆招，通常是向前保证兼容性或推动优化不合理代码来解决。

3、重定向问题解决

Http 请求发生重定向时，请求 header 中的 Host 字段需要更新为新的目标主机地址，否则服务端校验Host字段和实际请求的 host 不一致时会拒绝请求。首先看一下 Okhttp 是如何实现的这个功能：

okhttp 在 RetryAndFollowUpInterceptor 类中，302会触发重新构建请求对象:

之后在 BridgeInterceptor 中，重新设置 Host：

而 Cronet 在 android 侧的默认实现中，并未对此进行更新，查看cronet代码：

*类：cronet_url_request.cc*

可以看到，cronet 下层接口是支持对重定向时传入修改的 header 的，但是默认传入了空，也没有提供暴露给 java 侧的接口来进行设置。

解决方案：对 cronet 重定向时更新 header 的能力进行打通，新增设置接口：

```
复制代码void CronetURLRequest::NetworkTasks::SetRedirectHeader(
    const std::string& key,
    const std::string& value) {
  DCHECK_CALLED_ON_VALID_THREAD(network_thread_checker_);
  DCHECK(url_request_.get());
  if (redirect_request_headers_ == base::nullopt) {
    redirect_request_headers_ = base::make_optional<net::HttpRequestHeaders>();
  }
  redirect_request_headers_->SetHeader(key, value);
}

```

在重定向时将从 java 侧设置下来的 header 传入：

```
typescript复制代码  @Override
    protected void handleRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
        try {
            Uri newUri = Uri.parse(newLocationUrl);
            String host = newUri.getHost();
            // 更新Host
            request.setRedirectHeader("Host", host);
            request.followRedirect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

```

cronet 执行 FollowDeferredRedirect (真正重定向的方法)时，将原有方法替换为传入重定向 header 的方法：

```
arduino复制代码void CronetURLRequest::NetworkTasks::FollowDeferredRedirect() {
  DCHECK_CALLED_ON_VALID_THREAD(network_thread_checker_);
#if defined(WOW_BUILD)
  url_request_->FollowDeferredRedirect(
      this->redirect_request_headers_ /* modified_request_headers */);
#else
  url_request_->FollowDeferredRedirect(
      base::nullopt /* modified_request_headers */);
#endif
}

```

### 灰度&上线

网络库切换牵扯业务的方方面面，影响面较大，上线需要格外谨慎：

1. 
    
    在上线前的开发阶段，在开发环境提前切换到 Cronet，如果有问题可以尽早暴露；
    
2. 
    
    灰度阶段反复分流验证，结合稳定性平台和舆情信息反馈观察，确保 Cronet sdk 的稳定性；
    
3. 
    
    技术上，为了防止有其他异常情况引起的网络不可用，对非网络抖动引起的网络请求异常自动降级到 Okhttp，达到一定次数后开始彻底降低回 Okhttp，并上报日志进行分析；对网络组件以最小粒度进行动态配置，保证根据任意的组件都可以按需更新/开闭以进行线上ab效果观测；对网络请求各阶段的进行全面端到端数据埋点。
    
4.   
    
    上线后，拉长观测周期，分阶段放量。反复从各个维度比对网络性能数据，发现异常数据及时分析定位解决，确保数据是完全正向的。分析维度包括：
    
    - 首包时长/请求时长
    - 错误率
    - 长尾数据分析
    - 业务体感数据
    
    这个阶段相对较为漫长，通常是从数据侧发现问题后，结合对应的业务场景去进一步定位问题，在针对不同具体错误类型的数据分析过程中，我们也发现了一些上层非正常使用带来的错误率问题，并一起促进优化降低了部分场景的错误率。
    

目前 android cronet 已经线上全量稳定运行了一年多时间，从统计数据来看，主站api请求时长有16%的优化，错误率有4%的优化，cdn请求不同域名也有不同程度的优化。

### 后续规划

弱网场景的特殊优化是业务开发中经常遇到的，云音乐基于 Cronet 的 nqe 模块做二次开发，对外提供弱网检测通知能力（正在进行中）；

Cronet 的一个核心功能便是支持 quic 协议，作为下一代的网络通信协议，quic 协议具有一系列的协议层面优化：

1、 更少的建链 RTT

2、链接迁移

不同于 tcp 的四元组标识，quic 使用 cid 作为标识，cid 不变即可维持 quic 连接不中断

3、解决tcp队头阻塞（head of line blocking）问题

4、拥塞控制算法实现

将固化在操作系统实现的 tcp 拥塞控制等算法在应用层实现，无需升级操作系统即可实现对算法的升级

5、更好的安全性

2022年6月6日，IETF 正式发布了 http3 协议，云音乐在线上也小范围进行了 quic 协议的测试，在部分场景下quic 表现了更优秀的网络性能。当然在线上想充分利用 quic 的全部特性例如：连接恢复时的0RTT、链接迁移等特性，还需要对服务端前端机集群进行相应的改造。后续云音乐也会对这业界方面进展持续保持关注。

> 
> 
> 
> 本文发布自网易云音乐技术团队，文章未经授权禁止任何形式的转载。我们常年招收各类技术岗位，如果你准备换工作，又恰好喜欢云音乐，那就加入我们 grp.music-fe(at)corp.netease.com！
>