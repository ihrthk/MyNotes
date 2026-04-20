# Android APP合规检查，你可能需要这个工具~ - 掘金

[https://juejin.cn/post/7307470097663688731](https://juejin.cn/post/7307470097663688731)

[](Android%20APP%E5%90%88%E8%A7%84%E6%A3%80%E6%9F%A5%EF%BC%8C%E4%BD%A0%E5%8F%AF%E8%83%BD%E9%9C%80%E8%A6%81%E8%BF%99%E4%B8%AA%E5%B7%A5%E5%85%B7~%20-%20%E6%8E%98%E9%87%91/83d56a8aac194b72b53f83e8ec52f370tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

虽迟但到，这是一个通过拦截Java方法调用用以检测应用是否合规的工具，如果你的APP正饱受监管部门或应用市场时不时下发整改通知的折磨，那么用它来检查你的代码以及引用的三方库是再好不过的选择了！

## 如何引入

Step 1. 添加 `mavenCentral`

```
复制代码allprojects {
    	repositories {
    		...
    	 mavenCentral()
    	}
    }

```

Step 2. 添加 `Gradle` 依赖

```
复制代码dependencies {
        ...
        implementation 'io.github.loper7:miit-rule-checker:0.2.0'
    }

```

## 如何使用

### 检查APP内是否存在不合规的方法调用

> 
> 
> 
> 检查MIITRuleChecker内置的不合规的方法，具体可见下方方法列表
> 

```
复制代码 MIITRuleChecker.checkDefaults()

```

> 
> 
> 
> 如果内置的方法不满足当前需求，可自定义方法添加到list中进行检查；
> 
> 比如新增一个 MainActivity 的 onCreate 方法的调用检查；
> 

```
kotlin复制代码val list = MIITMethods.getDefaultMethods()
list.add(MainActivity::class.java.getDeclaredMethod("onCreate" , Bundle::class.java)) MIITRuleChecker.check(list)

```

当然，如果你想检查多个内置方法外的方法，只需要创建一个新的集合，往集合里放你想检查的方法`member`,然后传入 `MIITRuleChecker.check()`内即可。

`log`打印如下所示：

[](Android%20APP%E5%90%88%E8%A7%84%E6%A3%80%E6%9F%A5%EF%BC%8C%E4%BD%A0%E5%8F%AF%E8%83%BD%E9%9C%80%E8%A6%81%E8%BF%99%E4%B8%AA%E5%B7%A5%E5%85%B7~%20-%20%E6%8E%98%E9%87%91/493e67d67c8041549ae0eef832ab6f05tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

### 检查指定方法调用并查看调用栈堆

```
复制代码//查看 WifiInfo class 内 getMacAddress 的调用栈堆
MIITRuleChecker.check(MIITMethods.WifiInfo.getMacAddress)

```

`log`打印如下所示：

[](Android%20APP%E5%90%88%E8%A7%84%E6%A3%80%E6%9F%A5%EF%BC%8C%E4%BD%A0%E5%8F%AF%E8%83%BD%E9%9C%80%E8%A6%81%E8%BF%99%E4%B8%AA%E5%B7%A5%E5%85%B7~%20-%20%E6%8E%98%E9%87%91/56f4f8d85f3e4273a05698c2871931eftplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

### 检查一定时间内指定方法调用次数统计

```
复制代码//多个方法统计 （deadline 为从方法调用开始到多少毫秒后截至统计）
val list = mutableListOf<Member?>().apply {
add(MIITMethods.LocationManager.getLastKnownLocation)
add(MIITMethods.LocationManager.requestLocationUpdates)
add(MIITMethods.Secure.getString)
}
MIITMethodCountChecker.startCount(20 * 1000,list)

//单个方法统计（deadline 为从方法调用开始到多少毫秒后截至统计）
MIITMethodCountChecker.startCount(20 * 1000,MIITMethods.LocationManager.getLastKnownLocation)

```

`log`打印如下所示：

[](Android%20APP%E5%90%88%E8%A7%84%E6%A3%80%E6%9F%A5%EF%BC%8C%E4%BD%A0%E5%8F%AF%E8%83%BD%E9%9C%80%E8%A6%81%E8%BF%99%E4%B8%AA%E5%B7%A5%E5%85%B7~%20-%20%E6%8E%98%E9%87%91/31ac91ae047b435baf5e1679e6323496tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

检查完成并完成整改后务必移除方法 miit-rule-checker 库内的所有方法调用，将库一起移除最好

## 内置方法表

| 内置常量 | 对应的系统方法 | 备注 |
| --- | --- | --- |
| `MIITMethods.WifiInfo.getMacAddress` | `android.net.wifi.WifiInfo.getMacAddress()` | 获取MAC地址 |
| `MIITMethods.WifiInfo.getIpAddress` | `android.net.wifi.WifiInfo.getIpAddress()` | 获取IP地址 |
| `MIITMethods.LocationManager.getLastKnownLocation` | `android.location.LocationManager.getLastKnownLocation(String)` | 获取上次定位的地址 |
| `MIITMethods.LocationManager.requestLocationUpdates` | `android.location.LocationManager.requestLocationUpdates(String,Long,Float,LocationListener)` |  |
| `MIITMethods.NetworkInterface.getHardwareAddress` | `java.net.NetworkInterface.getHardwareAddress()` | 获取主机地址 |
| `MIITMethods.ApplicationPackageManager.getInstalledPackages` | `android.app.ApplicationPackageManager.getInstalledPackages(Int)` | 获取已安装的应用 |
| `MIITMethods.ApplicationPackageManager.getInstalledApplications` | `android.app.ApplicationPackageManager.getInstalledApplications(Int)` | 获取已安装的应用 |
| `MIITMethods.ApplicationPackageManager.getInstallerPackageName` | `android.app.ApplicationPackageManager.getInstallerPackageName(String)` | 获取应用安装来源 |
| `MIITMethods.ApplicationPackageManager.getPackageInfo` | `android.app.ApplicationPackageManager.getPackageInfo(String，Int)` | 获取应用信息 |
| `MIITMethods.PackageManager.getInstalledPackages` | `android.content.pm.PackageManager.getInstalledPackages(Int)` | 获取已安装的应用 |
| `MIITMethods.PackageManager.getInstalledApplications` | `android.content.pm.PackageManager.getInstalledApplications(Int)` | 获取已安装的应用 |
| `MIITMethods.PackageManager.getInstallerPackageName` | `android.content.pm.PackageManager.getInstallerPackageName(String)` | 获取应用安装来源 |
| `MIITMethods.PackageManager.getPackageInfo` | `android.content.pm.PackageManager.getPackageInfo(String，Int)` | 获取应用信息 |
| `MIITMethods.PackageManager.getPackageInfo1` | `android.content.pm.PackageManager.getPackageInfo(String，PackageInfoFlags)` | 获取应用信息（版本号大于33） |
| `MIITMethods.PackageManager.getPackageInfo2` | `android.content.pm.PackageManager.getPackageInfo(VersionedPackage，Int)` | 获取应用信息（版本号大于26） |
| `MIITMethods.PackageManager.getPackageInfo3` | `android.content.pm.PackageManager.getPackageInfo(VersionedPackage，PackageInfoFlags)` | 获取应用信息（版本号大于33） |
| `MIITMethods.Secure.getString` | `android.provider.Settings.Secure.getString(ContentResolver，String)` | 获取androidId |
| `MIITMethods.TelephonyManager.getDeviceId` | `android.telephony.TelephonyManager.getDeviceId()` | 获取 DeviceId |
| `MIITMethods.TelephonyManager.getDeviceIdWithInt` | `android.telephony.TelephonyManager.getDeviceId(Int)` | 获取 DeviceId |
| `MIITMethods.TelephonyManager.getImei` | `android.telephony.TelephonyManager.getImei()` | 获取 Imei |
| `MIITMethods.TelephonyManager.getImeiWithInt` | `android.telephony.TelephonyManager.getImei(Int)` | 获取 Imei |
| `MIITMethods.TelephonyManager.getSubscriberId` | `android.telephony.TelephonyManager.getSubscriberId()` | 获取 SubscriberId |

## 相关文章

- [工业和信息化部关于开展纵深推进APP侵害用户权益专项整治行动的通知](https://link.juejin.cn/?target=https%3A%2F%2Fwww.gov.cn%2Fzhengce%2Fzhengceku%2F2020-08%2F02%2Fcontent_5531975.htm)
- [工业和信息化部关于进一步提升移动互联网应用服务能力的通知](https://link.juejin.cn/?target=https%3A%2F%2Fwww.gov.cn%2Fzhengce%2Fzhengceku%2F2023-03%2F02%2Fcontent_5744106.htm)
- [APP合规政策解读爽文](https://juejin.cn/post/7250507911201226812)

## Github

- [miit-rule-checker](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Floper7%2Fmiit-rule-checker)

## 联系我