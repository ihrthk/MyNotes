# 使用Google推荐的VersionCatalog管理统一版本 - 掘金

VersionCatalog是Android在Gradle 7.0推出的一种全新管理统一版本的方式，它较以前的Groovy、buildSrc和includeBuild方式还是有所区别，它可以更好的去帮助开发者管理项目的依赖版本。在最新的Android Studio Giraffe | 2022.3.1中新建项目时，会出现下图选项，可以让开发者选择Version Catalog方式，不过后面还是标注了Experimental(实验性)。

### 创建VersionCatalog方式的项目

下面我们新建一个项目来感受下Version Catalog方式。

[](%E4%BD%BF%E7%94%A8Google%E6%8E%A8%E8%8D%90%E7%9A%84VersionCatalog%E7%AE%A1%E7%90%86%E7%BB%9F%E4%B8%80%E7%89%88%E6%9C%AC%20-%20%E6%8E%98%E9%87%91/2667481a481f4ac98c21e9e65c6eeaf3tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

项目新建好之后，可以在gradle目录下面看到lib.versions.toml文件，此文件就是定义管理所有的统一版本和依赖对象。

[](%E4%BD%BF%E7%94%A8Google%E6%8E%A8%E8%8D%90%E7%9A%84VersionCatalog%E7%AE%A1%E7%90%86%E7%BB%9F%E4%B8%80%E7%89%88%E6%9C%AC%20-%20%E6%8E%98%E9%87%91/e877ee118da64e7cac916cbfab70c4b8tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

文件内部只要有四种分类：

- [versions]：用于声明项目中依赖项引用的版本号
- [libraries]：用于声明依赖项的具体信息，包括依赖项的组织、名称和版本，此版本可直接引用versions中的值
- [plugins]：用于声明插件的具体信息，包括插件的id和版本，此版本也可直接引用versions中的值
- [bundles]：这个比较有趣，它可以声明一组依赖，它可以将多个依赖项组合在一起，方便后续的添加

### 基本用法

下面我们来看下具体的玩法，以添加Retrofit为例感觉下VersionCatalog的写法，先在toml文件中将版本和依赖项具体信息定义好

```
复制代码[versions]
retrofit-version = "2.9.0"

[libraries]
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit-version" }

```

然后就可以在模块的build.gradle.kts文件中添加对应的依赖引用

```
复制代码dependencies {
	implementation(libs.retrofit)
}

```

完成上面的操作之后，sync下项目就可以将retrofit添加到项目当中了。

versions部分不需要多加解释，它就是定义好retrofit的版本号而已，在libraries中需要注意下group和name的信息，这两个对应的时依赖项的组织和名称，也就是我们最原始方式`com.squareup.retrofit2:retrofit:2.9.0`中前面的信息，group对应的是第一个冒号前的信息，name对应的是两个冒号之间的信息，然后version.ref直接将versions中定义好的版本号赋值上去即可。对应关系如下图所示：

[](%E4%BD%BF%E7%94%A8Google%E6%8E%A8%E8%8D%90%E7%9A%84VersionCatalog%E7%AE%A1%E7%90%86%E7%BB%9F%E4%B8%80%E7%89%88%E6%9C%AC%20-%20%E6%8E%98%E9%87%91/77ff61d9cc7f44cfb5bcd83d1d451fd6tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

### bundles用法

学习了单个依赖的添加方式之后，我们再来看看bundles的用法，结合retrofit的依赖再添加下converter-gson依赖，毕竟这两个基本是绑定在一起使用的(使用Gson处理接口返回数据的前提下🤣)

```
复制代码[versions]
retrofit-version = "2.9.0"

[libraries]
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit-version" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit-version" }

[bundles]
retrofit-bundles = ["retrofit", "retrofit-gson"]

```

因为converter-gson和retrofit版本号是一致的，所以不用再单独的添加versions信息，只需要在libraries中添加converter-gson具体信息即可。

然后在bundles中将retrofit和retrofit-gson配置到单独的数组中，这样就可以在build.gradle通过`libs.bundles.retrofit.bundles`对其两个同时引用。

```
复制代码dependencies {
	implementation(libs.bundles.retrofit.bundles)
}

```

这方式在我眼里绝对是个开发者的福利，不仅仅是retrofit需要这样的同组添加依赖，还有room、Compose、lifecycle系列等都可以采用这样的方式进行大批量的依赖添加，这样我们在新建模块添加依赖时就不至于丢三落四了🍗。

### plugins使用

VersionCatalog不仅仅可以管理三方库的依赖项，还可以对plugins进行统一管理，在toml文件中有plugins部门专门对插件的版本依赖进行管理，下面我们体会下

```
ini复制代码[versions]
org-jetbrains-kotlin-android = "1.8.10"

[plugins]
org-jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "org-jetbrains-kotlin-android" }

```

上面代码中我们先将kotlin的版本1.8.10在versions中定义好，然后在plugins中将kotlin的插件具体信息定义完善，最后就可以直接在根目录的build.gradle.kts中对其引用

```
复制代码plugins {
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
}

```

在build.gradle.kts文件的plugins中采用别名alias形式执行toml文件中的org-jetbrains-kotlin-android即可，注意需要将-转成.的形式，这和我们之前的`id 'org.jetbrains.kotlin.android' version '1.8.10' apply false`方式还是有点区别的，从写法上面来看toml的方式变得更为简洁了，而且我们也可以将各模块下build.gradle.kts中plugins信息执行toml定义好的插件。

```
复制代码// toml形式
plugins {
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

// 未使用toml形式
plugins {
    id 'org.jetbrains.kotlin.android'
}

```

这样就可以在toml文件中定义一次，后续通用即可，非常棒的设计👍。

### VersionCatalog额外的优势和劣势

在Android Studio Giraffe | 2022.3.1版本中，我们可以直接在toml文件中观察到依赖项有没有新版本的提示，具体效果看下方图片：

[](%E4%BD%BF%E7%94%A8Google%E6%8E%A8%E8%8D%90%E7%9A%84VersionCatalog%E7%AE%A1%E7%90%86%E7%BB%9F%E4%B8%80%E7%89%88%E6%9C%AC%20-%20%E6%8E%98%E9%87%91/5c96ba2f0c564bd191cc584bb67c83a7tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

比如core-ktx库我们定义的是1.9.0版本，但是最新版本已经为1.10.1，在对应的版本号中会有高亮提示，提醒开发者此库有新的版本，可以进行option+enter一键修改。

但是截止Android Studio Giraffe | 2022.3.1版本，还不支持从toml一键跳转到对应的引用处，无法查看在哪些地方对其引用了，后续的Android StudioHedgehog或许会将此功能加上，到时候VersionCatalog会更加智能。

**到这为止VersionCatalog基本的使用知识已经介绍完了，大家如果感兴趣可以去新建项目体验下，整体的使用过程还是比较舒服的，而且官方的示例[Now In Android](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fandroid%2Fnowinandroid)项目也采用这种方式管理项目的统一版本，后续Google应该会都采用此方式。感谢大家的阅读🙂！**

## **关于我**

**我是Taonce，如果觉得本文对你有所帮助，帮忙关注、赞或者收藏三连一下，谢谢🙂～**