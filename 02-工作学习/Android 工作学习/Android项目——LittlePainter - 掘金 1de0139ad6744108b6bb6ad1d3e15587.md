# Android项目——LittlePainter - 掘金

# 一、项目简介

- 项目采用 Kotlin 语言编写，结合 `Jetpack` 相关控件，`Navigation`，`Lifecyle`，`DataBinding`，`LiveData`，`ViewModel`等搭建的 **MVVM** 架构模式
- 通过组件化拆分，实现项目更好解耦和复用
- 自定义view
- recycleview的使用
- 移动+淡入动画：补间动画
- 播放Lottie资源
- 项目截图
    
    [](Android%E9%A1%B9%E7%9B%AE%E2%80%94%E2%80%94LittlePainter%20-%20%E6%8E%98%E9%87%91/46ea629e7a7944d5afe1fd1d55666749tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)
    

[](Android%E9%A1%B9%E7%9B%AE%E2%80%94%E2%80%94LittlePainter%20-%20%E6%8E%98%E9%87%91/9e290a6c7d344bf28728e0fe4662f77ctplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

[](Android%E9%A1%B9%E7%9B%AE%E2%80%94%E2%80%94LittlePainter%20-%20%E6%8E%98%E9%87%91/5535279634f14cb3886d6b65d3ac3ce0tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

[](Android%E9%A1%B9%E7%9B%AE%E2%80%94%E2%80%94LittlePainter%20-%20%E6%8E%98%E9%87%91/71a6711c52ba49fc91d2092459ea7a39tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

[](Android%E9%A1%B9%E7%9B%AE%E2%80%94%E2%80%94LittlePainter%20-%20%E6%8E%98%E9%87%91/4d1b7d7d6a0f4dccaf2663fa6801ae54tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

github [github.com/afbasfh/Lit…](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fafbasfh%2FLittlePainter.git)

# 二、项目详情

## 2.1 MVVM（Model-View-ViewModel）

是一种基于数据绑定的架构模式，用于设计和组织应用程序的代码结构。它将应用程序分为三个主要部分：Model（模型）、View（视图）和ViewModel（视图模型）。

- Model（模型）：负责处理数据和业务逻辑。它可以是从网络获取的数据、数据库中的数据或其他数据源。Model层通常是独立于界面的，可以在多个界面之间共享。
- View（视图）：负责展示数据和与用户进行交互。它可以是Activity、Fragment、View等。View层主要负责UI的展示和用户输入的响应。
- ViewModel（视图模型）：连接View和Model，作为View和Model之间的桥梁。它负责从Model中获取数据，并将数据转换为View层可以直接使用的形式。ViewModel还负责监听Model的数据变化，并通知View进行更新。ViewModel通常是与View一一对应的，每个View都有一个对应的ViewModel。

[](Android%E9%A1%B9%E7%9B%AE%E2%80%94%E2%80%94LittlePainter%20-%20%E6%8E%98%E9%87%91/97825c9fa42a4a83ad3a2d44756e913ctplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

## 2.2 Jetpack组件

### (1) Navtgation

Google 在2018年推出了 Android Jetpack,在Jetpack里有一种管理**fragment**的新架构模式,那就是navigation. 字面意思是导航,但是除了做APP引导页面以外.也可以使用在App主页分tab的情况.. 甚至可以一个功能模块就一个activity大部分页面UI都使用fragment来实现,而navigation就成了管理fragment至关重要的架构.

这里主要用于页面的切换

### (2) ViewBinding&DataBinding

- `ViewBinding` 的出现就是不再需要写 `findViewById()`；
- `DataBinding` 是一种工具，它解决了 View 和数据之间的双向绑定；**减少代码模板**，不再需要写`findViewById()`；**释放 Activity/Fragment**，可以在 XML 中完成数据，事件绑定工作，让 `Activity/Fragment` 更加关心核心业务；**数据绑定空安全**，在 XML 中绑定数据它是空安全的，因为 `DataBinding` 在数据绑定上会自动装箱和空判断，所以大大减少了 NPE 问题。

### (3) ViewModel

`ViewModel` 具备生命感知能力的数据存储组件。页面配置更改数据不会丢失，数据共享（单 Activity 多 Fragment 场景下的数据共享），以生命周期的方式管理界面相关的数据，通常和 DataBinding 配合使用，为实现 MVVM 架构提供了强有力的支持。

### (4) LiveData

`LiveData` 是一个具有生命周期感知能力的数据订阅，分发组件。支持共享资源（一个数据支持被多个观察者接收的），支持粘性事件的分发，不再需要手动处理生命周期（和宿主生命周期自动关联），确保界面符合数据状态。在底层数据库更改时通知 View。

### (5) Room

一个轻量级 orm 数据库，**本质上是一个 SQLite 抽象层**。使用更加简单（Builder 模式，类似 Retrofit），通过**注解的形式实现相关功能，编译时自动生成实现类 IMPL**。

这里主要用于收藏点赞音乐，与 `LiveData` 和 Flow 结合处理可以避免不必要的 NPE，可以监听数据库表中的数据的变化，也可以和 RXJava 的 Observer 使用，一旦发生了 insert，update，delete等操作，`Room` 会自动读取表中最新的数据，发送给 UI 层，刷新页面。

## 2.3 RecycleView

### 1.1 什么是RecycleView

Recyclerview是可以展示大量数据 ，重视回收和复用的view的一种控件； RecyclerView是一个强大的滑动组件，与经典的ListView相比，同样拥有item回收复用的功能，这一点从它的名字Recyclerview即回收view也可以看出。RecyclerView 支持 线性布局、网格布局、瀑布流布局 三种，而且同时还能够控制横向还是纵向滚动。

### 1.2RecycleView的用法

纵向排列 布局文件：

1 .创建主布局并在主布局中添加 代码如下：

```
kotlin复制代码<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/black"
    tools:context=".home.HomeFragment">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="400dp"
        android:layout_marginTop="100dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="1.0"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>

```

2.创建子项布局文件addressbook_item.xml，并对其内部控件设置id 代码如下：

```
kotlin复制代码<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="wrap_content"
    android:layout_height="match_parent">

    <com.google.android.material.imageview.ShapeableImageView
        android:id="@+id/imageView"
        android:layout_width="250dp"
        android:layout_height="match_parent"
        android:layout_marginStart="10dp"
        android:layout_marginEnd="10dp"
        android:scaleType="centerCrop"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:srcCompat="@drawable/f1" />

    <View
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:background="@drawable/picture_border"
        app:layout_constraintBottom_toBottomOf="@+id/imageView"
        app:layout_constraintEnd_toEndOf="@+id/imageView"
        app:layout_constraintStart_toStartOf="@+id/imageView"
        app:layout_constraintTop_toTopOf="@+id/imageView" />
</androidx.constraintlayout.widget.ConstraintLayout>

```

3.创建适配器

直接继承RecyclerView.Adapter<AddressBookAdapter.ViewHolder> 然后一一实现

```
kotlin复制代码package com.example.littlepainter.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.littlepainter.db.Picture
import com.example.littlepainter.databinding.LayoutPictureItemBinding

class PictureAdapter: RecyclerView.Adapter<PictureAdapter.MyViewHolder>() {
    private var mPictures = emptyList<Picture>()

    override fun getItemCount(): Int {
        return mPictures.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LayoutPictureItemBinding.inflate(inflater,parent,false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(mPictures[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<Picture>){
        mPictures = newData
        notifyDataSetChanged()
    }

    //定义ViewHolder
    class MyViewHolder(private val binding:LayoutPictureItemBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(pictureModel: Picture){
            binding.imageView.setImageBitmap(pictureModel.thumbnail)
            binding.root.setOnClickListener {
                //切换到绘制界面
                val action = HomeFragmentDirections.actionHomeFragmentToDrawFragment(pictureModel)
                binding.root.findNavController().navigate(action)
            }
        }
    }
}

```

4.在活动中创建并设置适配器

```
kotlin复制代码binding.recyclerView.apply {
    layoutManager = ScaleLayoutManager(requireContext())
    adapter = mAdapter
    PagerSnapHelper().attachToRecyclerView(this)
}

```

### (2) ViewPager

### 2.1、什么是ViewPager

布局管理器允许左右翻转带数据的页面，你想要显示的视图可以通过实现PagerAdapter来显示。这个类其实是在早期设计和开发的，它的API在后面的更新之中可能会被改变，当它们在新版本之中编译的时候可能还会改变源码。 ViewPager经常用来连接Fragment，它很方便管理每个页面的生命周期，使用ViewPager管理Fragment是标准的适配器实现。最常用的实现一般有FragmentPagerAdapter和FragmentStatePagerAdapter。 ViewPager是android扩展包v4包中的类，这个类可以让我们左右切换当前的view。我们先来聊聊ViewPager的几个相关知识点： 1、ViewPager类直接继承了ViewGroup类，因此它一个容器类，可以添加其他的view类

2、ViewPager类需要一个PagerAdapter适配器类给它提供数据（这点跟ListView一样需要数据适配器Adater）

3、ViewPager经常和Fragment一起使用，并且官方还提供了专门的FragmentPagerAdapterFragmentStatePagerAdapter类供Fragment中的ViewPager使用

## 2.4 移动+淡入动画：补间动画

```
kotlin复制代码<alpha xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromAlpha="0.0"
    android:toAlpha="1.0"
    android:duration="500"/>

```

```
kotlin复制代码<alpha xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromAlpha="1.0"
    android:toAlpha="0.0"
    android:duration="500"/>

```

## 2.5 自定义view

### 1 .创建一个类继承于View

```
kotlin复制代码class DrawView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
}

```

### 2 .重写构造方法

```
kotlin复制代码override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
}

override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
}

```

## 2.6 播放Lottie资源

### 1.在Lottie寻找合适的资源放在资源项目下

### 2. 在布局里使用

```
<com.airbnb.lottie.LottieAnimationView
    android:id="@+id/lottieAnimationView"
    android:layout_width="0dp"
    android:layout_height="0dp"
    android:layout_marginStart="32dp"
    android:layout_marginEnd="32dp"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintDimensionRatio="1:1"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintHorizontal_bias="0.0"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintVertical_bias="0.304"
    app:lottie_autoPlay="true"
    app:lottie_loop="true"
    app:lottie_rawRes="@raw/anim4" />

```