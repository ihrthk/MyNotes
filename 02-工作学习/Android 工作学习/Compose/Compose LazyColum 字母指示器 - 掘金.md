# Compose LazyColum 字母指示器 - 掘金

[https://juejin.cn/post/7301148956435169292](https://juejin.cn/post/7301148956435169292)

今年，项目全部采用Compose，告别了XML。遇到相关的特效也选择用Compose去解决，过程周末找时间进行记录。希望对那些面临类似效果的开发者提供一些思路。如果各位有更好的思路和简介评论区见，相互学习。

[](Compose%20LazyColum%20%E5%AD%97%E6%AF%8D%E6%8C%87%E7%A4%BA%E5%99%A8%20-%20%E6%8E%98%E9%87%91/493f0f3af54c450bb2e846a1d510b5c1tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

# 一、效果演示

效果如下所示：左侧列表按首字母排序，并添加字母作为吸顶效果。右侧有文字指示器，点击可控制左侧列表滚动到相应字母吸顶位置。

# 二、分析实现

左边列表粘性标题吸附顶部效果Google已经在[LazyColumn](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.com%2Fjetpack%2Fcompose%2Flists)上提供了实现,官方也有简单的案例帮助理解。[LazyListScope::stickyHeader](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.com%2Fjetpack%2Fcompose%2Flists)。

## 1、数据结构

官网提供了stickyHeader，作为item添加一个粘性标题，即使在它之后滚动时，该标题项仍将保持固定状态。直到下一个标头取代其位置。

```
复制代码/**
 * Adds a sticky header item, which will remain pinned even when scrolling after it.
 * The header will remain pinned until the next header will take its place.
 *
 * @sample androidx.compose.foundation.samples.StickyHeaderSample
 *
 * @param key a stable and unique key representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param contentType the type of the content of this item. The item compositions of the same
 * type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param content the content of the header
 */
@ExperimentalFoundationApi
fun stickyHeader(
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit
)

```

[](Compose%20LazyColum%20%E5%AD%97%E6%AF%8D%E6%8C%87%E7%A4%BA%E5%99%A8%20-%20%E6%8E%98%E9%87%91/fb7da4d3f4f2465aa7ca60905c9ad731tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

首先，给定的列表有节点层次结构，如果你不介意一些额外工作，最好使用 Map 结构来构建 UI 数据。注意，开发者获取的数据可能没有按字母顺序排序，因此可能需要进行一些简单的数据处理。在文章中，我们使用了无商业价值的 JSON 数据进行了处理。使用了

```
js复制代码{
    "total":35,
    "rows":[
        {
            "name":"杨顺富",
            ...

```

数据处理代码如下：过于简单不做阐述

```
复制代码fun getLinkMap(context: Context): Map<String, MutableList<Row>> {
    val jsonResult = loadJSONFromAsset(context, "json_file.json")
    val stickBean = Gson().fromJson(jsonResult, StickyHeaderBean::class.java)
    val originalHashMap = HashMap<String, MutableList<Row>>()
    stickBean.rows.forEach { row ->
        val char = PinyinHelper.toHanyuPinyinStringArray(row.name.first())
        //获取首字母例如：luhenchang 结果为l
        val key = char[0][0].uppercase()
        //进行添加到Map中，如果存在，存储到当前key对应的value集合中。否则新建key进行存储
        //使用 getOrPut 函数简化添加到 Map 中的逻辑
        originalHashMap.getOrPut(key) { ArrayList() }.add(row)
    }
    //根据字母进行排序
    val sortedLinkedHashMap = originalHashMap
        .toSortedMap(compareBy { it })
        .toMap()
    // 打印结果
    sortedLinkedHashMap.forEach { (key, value) ->
        println("$key: $value")
    }
    return sortedLinkedHashMap
}

```

数据处理结果如下：

[](Compose%20LazyColum%20%E5%AD%97%E6%AF%8D%E6%8C%87%E7%A4%BA%E5%99%A8%20-%20%E6%8E%98%E9%87%91/d2f9fe08f7cb48ae8979e1fda488e7e2tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

## 2、左侧UI构建

作为案例，不做精细UI处理分装，不考虑性能问题。

```
复制代码@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun StickyHeaderLazyColum() {
    val context = LocalContext.current
    val data = getLinkMap(context)
    Column {
        StickyHeaderSearch()
        LazyColumn {
            data.forEach { (initial, contactsForInitial) ->
                stickyHeader {
                    StickyHeaderTop(initial)
                }

                items(contactsForInitial.size) { contact ->
                    StickyHeaderItem(contactsForInitial, contact)
                }
            }
        }
    }

}

@Composable
private fun StickyHeaderTop(initial: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(Color(0xFFE0E2E2))
            .padding(start = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            initial,
            color = Color.Black
        )
    }
}

@Composable
private fun StickyHeaderItem(
    contactsForInitial: MutableList<Row>,
    contact: Int
) {
    Column {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StickyHeaderLeftItem(contactsForInitial, contact)
            StickyHeaderRightItem(contactsForInitial, contact)
        }
        Divider(
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.5f))
                .height(3.dp)
        )
    }
}

```

效果如下:

左边列表我们基本是处理相关数据便于绑定组件。而ColumLazy组件使用比较简单，当然开发者应该虑性性能问题。数据处理可否优化，item相关key是否设置等。接下来看看效果列表右侧实现。

## 3、右侧UI构建

右侧简单的字母排序，使用ColumLazy实现。

```
复制代码@Composable
fun StickyHeaderLazyColum() {
    val context = LocalContext.current
    val data = getLinkMap(context)
    Column {
        StickyHeaderSearch()
        Box (contentAlignment = Alignment.TopEnd){
            LazyColumLeftUI(data)
            LazyColumRightUI(data)
        }
    }

}

@Composable
private fun LazyColumRightUI(data: Map<String, MutableList<Row>>) {
    LazyColumn(Modifier.padding(end = 10.dp)) {
        data.forEach { (initial, _) ->
            item {
                Text(
                    text = initial,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LazyColumLeftUI(data: Map<String, MutableList<Row>>) {
    LazyColumn {
        data.forEach { (initial, contactsForInitial) ->
            stickyHeader {
                StickyHeaderTop(initial)
            }

            items(contactsForInitial.size) { contact ->
                StickyHeaderItem(contactsForInitial, contact)
            }
        }
    }
}

```

## 4、联动分析

点击右上角字母，使其左侧的粘性标题吸附顶部。首先思考到LazyColum如何控制滑动位置，其次通过点击左上角的字母如何确定左侧LazyColum滑动的变量。

列表滑动相关控件官方都会提供控制滑动和测量相关的状态容器对象，而LazyColum其状态容器对象是LazyListState。在LazyListState里面提供了scrollToItem、animateScrollToItem、scrollBy、animateScrollBy等滑动相关的接口。其可以通过指定索引滑动到对应Item项，或者通过指定滑动距离滑动到目的Item项。

```
复制代码/**
 * Instantly brings the item at [index] to the top of the viewport, offset by [scrollOffset]
 * pixels.
 *
 * @param index the index to which to scroll. Must be non-negative.
 * @param scrollOffset the offset that the item should end up after the scroll. Note that
 * positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
 * scroll the item further upward (taking it partly offscreen).
 */
suspend fun scrollToItem(
    /*@IntRange(from = 0)*/
    index: Int,
    scrollOffset: Int = 0
) {
    scroll {
        snapToItemIndexInternal(index, scrollOffset)
    }
}

```

通过scrollToItem(index:Int)作为入口，也就是我们只需要计算点击字母时，对应的粘性标题Item的索引，通过scrollToItem进行滚动到具体位置。

左侧LazyColum列表和数据结构结合分析，由于stickyHeader同item都是item项，所以点击右上侧C时候，左侧对应的StickHeader索引应该是0。点击右上侧D时候，左侧对应的StickHeader索引应该3，G对应7....所以其字母和索引对应关系不难得出方法如下：

```
复制代码private fun getLeftHeaderIndexByChar(
    data: Map<String, MutableList<Row>>,
    initial: String
): Pair<Int, Int> {
    val keysBeforeList =
        data.keys.takeWhile { it != initial } // 获取输入字母之前的键
    val sum = keysBeforeList.sumOf { data[it]?.size ?: 0 } // 计算目标之前value数量的总和
    //0对应加 0、1加1、2加2。所以获取目标索引相加即可
    val indexOfSelf = data.keys.indexOf(initial) // 获取输入字母之前的键
    return Pair(sum, indexOfSelf)
}

```

通过代码进行验证

```
复制代码@Composable
private fun LazyColumRightUI(data: Map<String, MutableList<Row>>, state: LazyListState) {
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.padding(end = 10.dp)) {
        data.forEach { (initial, _) ->
            item {
                Text(
                    text = initial,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable {
                            scope.launch {
                                val (sum, indexOfSelf) = getLeftHeaderIndexByChar(data, initial)
                                state.scrollToItem(sum + indexOfSelf)
                            }
                        }
                )
            }
        }
    }
}

private fun getLeftHeaderIndexByChar(
    data: Map<String, MutableList<Row>>,
    initial: String
): Pair<Int, Int> {
    val keysBeforeList =
        data.keys.takeWhile { it != initial } // 获取输入字母之前的键
    val sum = keysBeforeList.sumOf { data[it]?.size ?: 0 } // 计算目标之前value数量的总和
    //0对应加 0、1加1、2加2。所以获取目标索引相加即可
    val indexOfSelf = data.keys.indexOf(initial) // 获取输入字母之前的键
    return Pair(sum, indexOfSelf)
}

```

# 三、代码实现

部件控制流程最好是进行状态容器的分装。

```
kotlin复制代码@Preview
@Composable
fun StickyHeaderLazyColum() {
    val context = LocalContext.current
    val stickyHeaderState = rememberStickyHeaderState()
    val data = getLinkMap(context)
    stickyHeaderState.setData(data)
    Scaffold {
        Column(Modifier.padding(it)) {
            StickyHeaderSearch {
            }
            Box(contentAlignment = Alignment.TopEnd) {
                LazyColumLeftUI(data, stickyHeaderState)
                LazyColumRightUI(data, stickyHeaderState)
            }
        }
    }

}

@Composable
fun rememberStickyHeaderState(
    state: LazyListState = LazyListState(),
    hashMap: HashMap<String, MutableList<Row>> = HashMap()
): StickyHeaderState {
    return remember(state) {
        StickyHeaderState(
            state,
            hashMap
        )
    }
}

class StickyHeaderState(
    val state: LazyListState = LazyListState(),
    private var hashMap: HashMap<String, MutableList<Row>>
) {
    fun setData(data: HashMap<String, MutableList<Row>>) {
        this.hashMap = data
    }

    suspend fun scrollToItem(initial: String) {
        val (sum, indexOfSelf) = getLeftHeaderIndexByChar(hashMap, initial)
        state.scrollToItem(sum + indexOfSelf)
    }

    private fun getLeftHeaderIndexByChar(
        data: Map<String, MutableList<Row>>,
        initial: String
    ): Pair<Int, Int> {
        val keysBeforeList =
            data.keys.takeWhile { it != initial } // 获取输入字母之前的键
        val sum = keysBeforeList.sumOf { data[it]?.size ?: 0 } // 计算目标之前value数量的总和
        //0对应加 0、1加1、2加2。所以获取目标索引相加即可
        val indexOfSelf = data.keys.indexOf(initial) // 获取输入字母之前的键
        return Pair(sum, indexOfSelf)
    }

}

@Composable
private fun LazyColumRightUI(data: Map<String, MutableList<Row>>, stickyState: StickyHeaderState) {
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.padding(end = 10.dp)) {
        data.forEach { (initial, _) ->
            item {
                Text(
                    text = initial,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable {
                            scope.launch {
                                stickyState.scrollToItem(initial)
                            }
                        }
                )
            }
        }
    }
}

```

到这里，基本实现了我们项目中的需求。在实现之前对比了一下微信联系人页面和手机通讯录联系人页面效果，都是有右上角字母可以进行触摸滑动，且左边列表自动定位到触摸字母吸顶。接下来我们进行升级实现右上角手势触摸联动。

# 四、自定义升级

如上图，右侧通过手势可以控制左侧列表。Modifier.pointerInput提供了相关屏幕事件，也提供了很多相关手势的方法，如drag、detectDragGestures、awaitDragOrCancellation、verticalDrag、detectDragGesturesAfterLongPress...我们先通过detectDragGestures来尝试解决问题。

## 1、detectDragGestures

如上图，每个Item的高度我们可以知道，而手势detectDragGestures可以拿到竖直方向高度，所以我们简单的可以通过 position.y / itemHeight 计算其触摸的索引。代码实现如下：

```
kotlin复制代码@Composable
private fun LazyColumRightUI(data: Map<String, MutableList<Row>>, stickyState: StickyHeaderState) {
    val scope = rememberCoroutineScope()
    val pxHeight = with(LocalDensity.current) {
        30.dp.roundToPx()
    }
    val indexTouch = remember { mutableIntStateOf(0) }
    LazyColumn(
        Modifier
            .padding(end = 10.dp)
            .width(40.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        val selectedIndex = (it.y / pxHeight).toInt()
                        if (selectedIndex in 0 until data.size) {
                            indexTouch.intValue = selectedIndex
                            scope.launch {
                                stickyState.scrollToItem(
                                    data.toList()[indexTouch.intValue].first
                                )
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        val selectedIndex = (change.position.y / pxHeight).toInt()
                        if (selectedIndex in 0 until data.size) {
                            indexTouch.intValue = selectedIndex
                            scope.launch {
                                stickyState.scrollToItem(
                                    data.toList()[indexTouch.intValue].first
                                )
                            }
                        }
                    },
                    onDragEnd = {

                    }
                )
            }, userScrollEnabled = false
    ) {
        data.onEachIndexed { index, initial ->
            item {
                Box(
                    Modifier
                        .height(30.dp)
                        .width(30.dp)
                        .padding(end = if (indexTouch.intValue == index && indexTouch.intValue in 1 until data.size - 1) 10.dp else 0.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = initial.key,
                        color = if (index == indexTouch.intValue) Color.Blue else Color.Black,
                        modifier = Modifier
                            .wrapContentSize()
                    )
                }
            }
        }
    }
}

```

效果如下：

到这里，似乎实现了滑动关联，但是detectDragGestures并未提供手势按下事件，在大量所提供的手势处理方法中要么像detectTapGestures有按下，没有move过程，或像detectDragGestures没有按下事件。既要拿到按下位置也要拿到移动过程位置那就需要自定义手势了。

## 2、自定义手势

首先定义一个PointerInputScope的扩展方法detectTapAndMoveGestures。让其循环检索屏幕事件，我们通过awaitFirstDown获取按下位置，通过AwaitPointerEventScope.drag拿到拖动过程中的位置。代码如下：

```
kotlin复制代码suspend fun PointerInputScope.detectTapAndMoveGestures(
    onDown: ((Offset) -> Unit)? = null,
    onMove: ((Offset) -> Unit)? = null,
) = coroutineScope {
    //不断等待获取屏幕事件
    while (true) {
        val downPointer = awaitPointerEventScope {
            awaitFirstDown()
        }
        onDown?.invoke(downPointer.position)
        val movePointer = awaitPointerEventScope {
            drag(downPointer.id, onDrag = { movePointer ->
                onMove?.invoke(movePointer.position)
            })
        }
    }
}

```

UI部分：

```
kotlin复制代码@Composable
private fun LazyColumRightUI(data: Map<String, MutableList<Row>>, stickyState: StickyHeaderState) {
    val scope = rememberCoroutineScope()
    val pxHeight = with(LocalDensity.current) {
        30.dp.roundToPx()
    }
    val indexTouch = remember { mutableIntStateOf(0) }
    LazyColumn(
        Modifier
            .padding(end = 10.dp)
            .width(40.dp)
            .pointerInput(Unit) {
                detectTapAndMoveGestures(onDown = {
                    val selectedIndex = (it.y / pxHeight).toInt()
                    if (selectedIndex in 0 until data.size) {
                        indexTouch.intValue = selectedIndex
                        scope.launch {
                            stickyState.scrollToItem(
                                data.toList()[selectedIndex].first
                            )
                        }
                    }
                }, onMove = {
                    val selectedMoveIndex = (it.y / pxHeight).toInt()
                    if (selectedMoveIndex in 0 until data.size) {
                        indexTouch.intValue = selectedMoveIndex
                        scope.launch {
                            stickyState.scrollToItem(
                                data.toList()[selectedMoveIndex].first
                            )
                        }
                    }
                })
            }, userScrollEnabled = false
    ) {
        data.onEachIndexed { index, initial ->
            item {
                Box(
                    Modifier
                        .height(30.dp)
                        .width(30.dp)
                        .padding(end = if (indexTouch.intValue == index && indexTouch.intValue in 1 until data.size - 1) 10.dp else 0.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = initial.key,
                        color = if (index == indexTouch.intValue) Color.Blue else Color.Black,
                        modifier = Modifier
                            .wrapContentSize()
                    )
                }
            }
        }
    }
}

```

效果如下：

到这里基本完成了手势触发简单的联动效果。右侧字符如果需要更多特效可以使用自定义Canvas进行实现，这里简单修改了颜色和padding的数值。如果要花里胡哨的操作，可以看看我以往写过的自定义相关文章。

[# Compose OpenAI](https://juejin.cn/post/7219508311687020599)

[# Compose HorizontalPager二级联动](https://juejin.cn/post/7288628985322733583)

[# Compose for Desktop](https://juejin.cn/post/6949446764839567374)

[# JetPack-Compose 水墨画效果](https://juejin.cn/post/6947700226858123271)

[# JetPack-Compose UI终结篇](https://juejin.cn/post/6943590136424693767) [# JetPack-Compose - 自定义绘制](https://juejin.cn/post/6937700592340959269) [# Jetpack-Compose](https://juejin.cn/post/6937226591911018532#heading-19)

## 3、双向联动

右上角既然作为指示器，那也应该跟随列表滑动吸附顶部字母进行变动，达到双向联动效果。每个滑动状态容器应该都会提供滑动过程中位置和布局测量相关的数据，所以多用多看便会找到很多有用的信息。LazyListState的LazyListLayoutInfo提供了当前Item的很多信息。

```
kotlin复制代码/**
 * Contains useful information about an individual item in lazy lists like [LazyColumn]
 *  or [LazyRow].
 *
 * @see LazyListLayoutInfo
 */
interface LazyListItemInfo {
    /**
     * The index of the item in the list.
     */
    val index: Int

    /**
     * The key of the item which was passed to the item() or items() function.
     */
    val key: Any

    /**
     * The main axis offset of the item in pixels. It is relative to the start of the lazy list container.
     */
    val offset: Int

    /**
     * The main axis size of the item in pixels. Note that if you emit multiple layouts in the composable
     * slot for the item then this size will be calculated as the sum of their sizes.
     */
    val size: Int

    /**
     * The content type of the item which was passed to the item() or items() function.
     */
    val contentType: Any? get() = null
}

```

其中index返回给你当前可见Item的索引，因为我们的 stickyHeader永远是顶部可见项目，所以可以通过index获取到当前stickyHeader的索引。对于HashMap中已知key的索引，计算对应右侧字母索引简单的算法即可解决。

```
kotlin复制代码fun rightTopSelectedIndex(
    cities: Map<String, MutableList<Row>>,
    stickyIndex: Int
): Int {
    var countSum = 0
    var currentCharIndex = 0

    cities.forEach { (_, rows) ->
        if (countSum == stickyIndex) {
            return currentCharIndex
        }

        countSum += 1 + rows.size
        currentCharIndex++
    }

    return currentCharIndex
}

```

UI代码如下：

```
kotlin复制代码@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LazyColumLeftUI(data: Map<String, MutableList<Row>>, stickyState: StickyHeaderState) {
    val uiUpdate = remember {
        derivedStateOf {
            if (stickyState.state.isScrollInProgress && stickyState.state.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val stickyIndex = stickyState.state.layoutInfo.visibleItemsInfo[0].index
                if (stickyIndex < maxSelectedIndex(data, data.size - 1)) {
                    val findIndex = rightTopSelectedIndex(data, stickyIndex)
                    //通知右上侧进行刷新文字
                    stickyState.setIndexSelected(findIndex)
                    findIndex
                }else{
                    null
                }
            } else {
                null
            }
        }
    }
    uiUpdate.value?.toString()
    LazyColumn(state = stickyState.state) {
        data.forEach { (initial, contactsForInitial) ->
            stickyHeader {
                StickyHeaderTop(initial)
            }

            items(contactsForInitial.size) { contact ->
                StickyHeaderItem(contactsForInitial, contact)
            }
        }
    }
}

```

最终效果如下：

# 五、总结

Compose相比View其简单的制定性和复用性让人爱不释手，但轮子目前可能较少，还需广大的开发者不断的写出好用的轮子。如有更好的实现方式，可以评论区各抒己见。