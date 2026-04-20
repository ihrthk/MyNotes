# Android自定义ViewGroup嵌套与交互实战，幕布全屏滚动效果 - 掘金

[https://juejin.cn/post/7189473736729788473](https://juejin.cn/post/7189473736729788473)

# 自定义 ViewGroup 全屏选中效果

## 前言

事情是这个样子的，前几天产品丢给我一个视频，你觉得这个效果怎么样？我们的 App 也做一个这个效果吧！

[](Android%E8%87%AA%E5%AE%9A%E4%B9%89ViewGroup%E5%B5%8C%E5%A5%97%E4%B8%8E%E4%BA%A4%E4%BA%92%E5%AE%9E%E6%88%98%EF%BC%8C%E5%B9%95%E5%B8%83%E5%85%A8%E5%B1%8F%E6%BB%9A%E5%8A%A8%E6%95%88%E6%9E%9C%20-%20%E6%8E%98%E9%87%91/3391a20637974c03913a64f6eeca7499tplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

我当时的反应：

[](Android%E8%87%AA%E5%AE%9A%E4%B9%89ViewGroup%E5%B5%8C%E5%A5%97%E4%B8%8E%E4%BA%A4%E4%BA%92%E5%AE%9E%E6%88%98%EF%BC%8C%E5%B9%95%E5%B8%83%E5%85%A8%E5%B1%8F%E6%BB%9A%E5%8A%A8%E6%95%88%E6%9E%9C%20-%20%E6%8E%98%E9%87%91/06f7162bed5d47baa3b803e88cef9538tplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

1d0bc9d5801e43b18a7b0c3a61097fc9 (1).jpeg

开什么玩笑！就没见过这么玩的，这不是坑人吗？

此时产品幽幽的回了一句，“别人都能做，你怎么不能做，并且iOS说可以做，还很简单。”

我心里一万个不信，糟老头子太坏了，想骗我？

[](Android%E8%87%AA%E5%AE%9A%E4%B9%89ViewGroup%E5%B5%8C%E5%A5%97%E4%B8%8E%E4%BA%A4%E4%BA%92%E5%AE%9E%E6%88%98%EF%BC%8C%E5%B9%95%E5%B8%83%E5%85%A8%E5%B1%8F%E6%BB%9A%E5%8A%A8%E6%95%88%E6%9E%9C%20-%20%E6%8E%98%E9%87%91/70c17c3fdfb14715a600df965925dd26tplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

我立马和iOS同事统一战线，说不能做，实现不了吧。结果iOS同事幽幽的说了一句 “已经做了，四行代码完成”。

[](Android%E8%87%AA%E5%AE%9A%E4%B9%89ViewGroup%E5%B5%8C%E5%A5%97%E4%B8%8E%E4%BA%A4%E4%BA%92%E5%AE%9E%E6%88%98%EF%BC%8C%E5%B9%95%E5%B8%83%E5%85%A8%E5%B1%8F%E6%BB%9A%E5%8A%A8%E6%95%88%E6%9E%9C%20-%20%E6%8E%98%E9%87%91/7b0011ee5fa64a07949e29a50a9fb85atplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

我勒个去，就指着我卷是吧。

这也没办法了，群里问问大神有什么好的方案，“xdm，车先减个速,（图片）这个效果怎么实现？”

“做不了...”

“让产品滚...”

“没做过，也没见过...”

“性能不好，不推荐，换方案吧。”

“GridView嵌套ScrollView , 要不横向RV嵌套纵向RV？...”

“一个RV就可以了，自定义布局排列，有手就行...”

“不理他，继续开车...”

...群里技术氛围果然没有让我失望，哎，看来还是得靠自己，抬头望了望天天，扣了扣脑阔，无语啊。

好了，说了这么多玩笑话，回归正题，其实关于标题的这种效果，确实是很少见，开源的效果也比较少见。

到底怎么做呢？我觉得这两种方案都是是可以的：

1. 使用RV自定义LayoutManager，自定义内部排列的布局，重写一些滚动的逻辑。
2. 使用自定义ViewGroup自己填充布局，然后使用事件来处理滚动。

其中两种方案都有各自的使用场景，各有利弊。相信跟着我一起复习的小伙伴们都是来看自定义View系列的。所以这里就只讲自定义ViewGroup的方式实现。

Ok,下面跟着我一起再巩固一次 ViewGroup 的测量与布局，加上事件的处理，我们就能完成对应的功能啦。

话不多说，Let's go

[](Android%E8%87%AA%E5%AE%9A%E4%B9%89ViewGroup%E5%B5%8C%E5%A5%97%E4%B8%8E%E4%BA%A4%E4%BA%92%E5%AE%9E%E6%88%98%EF%BC%8C%E5%B9%95%E5%B8%83%E5%85%A8%E5%B1%8F%E6%BB%9A%E5%8A%A8%E6%95%88%E6%9E%9C%20-%20%E6%8E%98%E9%87%91/0e526cc29ee84b22911915126f8561bftplv-k3u1fbpfcp-zoom-in-crop-mark1512000.awebp)

### 一、布局的测量与布局

首先GridView嵌套ScrollView，RV 嵌套 RV 什么的，就宽度就限制死了，其次滚动方向也固定死了，不好做。

肯定是选用自定义 ViewGroup 的方案，自己测量，自己布局，自己实现滚动与缩放逻辑。

从产品发的竞品App的视频来看，我们需要先明确三个变量，一行显示多少个Item、垂直距离每一个Item的间距，水平距离每一个Item的间距。

然后我们测量每一个ItemView的宽度，每一个Item的宽度加起来就是ViewGroup的宽度，每一个Item的高度加起来就是ViewGroup的高度。

我们目前先不限定Item的宽高，先试着测量一下：

```
java复制代码class CurtainViewContrainer extends ViewGroup {

    private int horizontalSpacing = 20;  //每一个Item的左右间距
    private int verticalSpacing = 20;  //每一个Item的上下间距
    private int mRowCount = 6;   // 一行多少个Item

    private Adapter mAdapter;

    public CurtainViewContrainer(Context context) {
        this(context, null);
    }

    public CurtainViewContrainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CurtainViewContrainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int sizeWidth = MeasureSpec.getSize(widthMeasureSpec) - this.getPaddingRight() - this.getPaddingLeft();
        final int modeWidth = MeasureSpec.getMode(widthMeasureSpec);

        final int sizeHeight = MeasureSpec.getSize(heightMeasureSpec) - this.getPaddingTop() - this.getPaddingBottom();
        final int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        int childCount = getChildCount();

        if (mAdapter == null || mAdapter.getItemCount() == 0 || childCount == 0) {
            setMeasuredDimension(sizeWidth, 0);
            return;
        }

        int curCount = 1;
        int totalControlHeight = 0;
        int totalControlWidth = 0;
        int layoutChildViewCurX = this.getPaddingLeft();
        int curRow = 0;
        int curColumn = 0;
        SparseArray<Integer> rowWidth = new SparseArray<>(); //全部行的宽度

        //开始遍历
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);

            int row = curCount / mRowCount;    //当前子View是第几行
            int column = curCount % mRowCount; //当前子View是第几列

            //测量每一个子View宽度
            measureChild(childView, widthMeasureSpec, heightMeasureSpec);

            int width = childView.getMeasuredWidth();
            int height = childView.getMeasuredHeight();

            boolean isLast = (curCount + 1) % mRowCount == 0;

            if (row == curRow) {
                layoutChildViewCurX += width + horizontalSpacing;
                totalControlWidth += width + horizontalSpacing;

                rowWidth.put(row, totalControlWidth);

            } else {
                //已经换行了
                layoutChildViewCurX = this.getPaddingLeft();
                totalControlWidth = width + horizontalSpacing;

                rowWidth.put(row, totalControlWidth);

                //添加高度
                totalControlHeight += height + verticalSpacing;
            }

            //最多只摆放9个
            curCount++;
            curRow = row;
            curColumn = column;
        }

        //循环结束之后开始计算真正的宽度
        List<Integer> widthList = new ArrayList<>(rowWidth.size());
        for (int i = 0; i < rowWidth.size(); i++) {
            Integer integer = rowWidth.get(i);
            widthList.add(integer);
        }

        Integer maxWidth = Collections.max(widthList);

        setMeasuredDimension(maxWidth, totalControlHeight);

    }

```

当遇到高度不统一的情况下，就会遇到问题，所以我们记录一下每一行的最高高度，用于计算控件的测量高度。

虽然这样测量是没有问题的，但是布局还是有坑，姑且先这么测量：

```
java复制代码    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int childCount = getChildCount();

        int curCount = 1;
        int layoutChildViewCurX = l;
        int layoutChildViewCurY = t;

        int curRow = 0;
        int curColumn = 0;
        SparseArray<Integer> rowWidth = new SparseArray<>(); //全部行的宽度

        //开始遍历
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);

            int row = curCount / mRowCount;    //当前子View是第几行
            int column = curCount % mRowCount; //当前子View是第几列

            //每一个子View宽度

            int width = childView.getMeasuredWidth();
            int height = childView.getMeasuredHeight();

            childView.layout(layoutChildViewCurX, layoutChildViewCurY, layoutChildViewCurX + width, layoutChildViewCurY + height);

            if (row == curRow) {
                //同一行
                layoutChildViewCurX += width + horizontalSpacing;

            } else {
                //换行了
                layoutChildViewCurX = l;
                layoutChildViewCurY += height + verticalSpacing;
            }

            //最多只摆放9个
            curCount++;
            curRow = row;
            curColumn = column;
        }

        performBindData();
    }

```

这样做并没有紧挨着头上的Item，目前我们把Item的宽高都使用同样的大小，是勉强能看的，一旦高度不统一，就不能看了。

先不管那么多，先固定大小显示出来看看效果。

反正是能看了，一个寨版的 GridView ，但是超出了宽度的限制。接下来我们先做事件的处理，让他动起来。

### 二、全屏滚动逻辑

首先我们需要把显示的 ViewGroup 控件封装为一个类，让此ViewGroup在另一个ViewGroup内部移动，不然还能让内部的每一个子View单独移动吗？肯定是整体一起移动更方便一点。

然后我们触摸容器 ViewGroup 中控制子 ViewGroup 移动即可，那怎么移动呢？

我知道，用 MotionEvent + Scroller 就可以滚动啦！

可以！又不可以，Scroller确实是可以动起来，但是在我们拖动与缩放之后，不能影响到内部的点击事件。

那可以不可以用 ViewDragHelper 来实现动作效果？

也不行，虽然 ViewDragHelper 是ViewGroup专门用于移动的帮助类，但是它内部其实还是封装的 MotionEvent + Scroller。

而 Scroller 为什么不行？

这种效果我们不能使用 Canvas 的移动，不能使用 Sroller 去移动，因为它们不能记录移动后的 View 变化矩阵，我们需要使用基本的 setTranslation 来实现，自己控制矩阵的变化从而控制整个视图树。

我们把触摸的拦截与事件的处理放到一个公用的事件处理类中：

```
java复制代码public class TouchEventHandler {

    private static final float MAX_SCALE = 1.5f;  //最大能缩放值
    private static final float MIN_SCALE = 0.8f;  //最小能缩放值
    //当前的触摸事件类型
    private static final int TOUCH_MODE_UNSET = -1;
    private static final int TOUCH_MODE_RELEASE = 0;
    private static final int TOUCH_MODE_SINGLE = 1;
    private static final int TOUCH_MODE_DOUBLE = 2;

    private View mView;
    private int mode = 0;
    private float scaleFactor = 1.0f;
    private float scaleBaseR;
    private GestureDetector mGestureDetector;
    private float mTouchSlop;
    private MotionEvent preMovingTouchEvent = null;
    private MotionEvent preInterceptTouchEvent = null;
    private boolean mIsMoving;
    private float minScale = MIN_SCALE;
    private FlingAnimation flingY = null;
    private FlingAnimation flingX = null;

    private ViewBox layoutLocationInParent = new ViewBox();  //移动中不断变化的盒模型
    private final ViewBox viewportBox = new ViewBox();   //初始化的盒模型
    private PointF preFocusCenter = new PointF();
    private PointF postFocusCenter = new PointF();
    private PointF preTranslate = new PointF();
    private float preScaleFactor = 1f;
    private final DynamicAnimation.OnAnimationUpdateListener flingAnimateListener;
    private boolean isKeepInViewport = false;
    private TouchEventListener controlListener = null;
    private int scalePercentOnlyForControlListener = 0;

    public TouchEventHandler(Context context, View view) {
        this.mView = view;
        flingAnimateListener = (animation, value, velocity) -> keepWithinBoundaries();

        mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        flingX = new FlingAnimation(mView, DynamicAnimation.TRANSLATION_X);
                        flingX.setStartVelocity(velocityX)
                                .addUpdateListener(flingAnimateListener)
                                .start();

                        flingY = new FlingAnimation(mView, DynamicAnimation.TRANSLATION_Y);
                        flingY.setStartVelocity(velocityY)
                                .addUpdateListener(flingAnimateListener)
                                .start();
                        return false;
                    }
                });
        ViewConfiguration vc = ViewConfiguration.get(view.getContext());
        mTouchSlop = vc.getScaledTouchSlop() * 0.8f;
    }

    /**
     * 设置内部布局视图窗口高度和宽度
     */
    public void setViewport(int winWidth, int winHeight) {
        viewportBox.setValues(0, 0, winWidth, winHeight);
    }

    /**
     * 暴露的方法，内部处理事件并判断是否拦截事件
     */
    public boolean detectInterceptTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        onTouchEvent(event);
        if (action == MotionEvent.ACTION_DOWN) {
            preInterceptTouchEvent = MotionEvent.obtain(event);
            mIsMoving = false;
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsMoving = false;
        }
        if (action == MotionEvent.ACTION_MOVE && mTouchSlop < calculateMoveDistance(event, preInterceptTouchEvent)) {
            mIsMoving = true;
        }
        return mIsMoving;
    }

    /**
     * 当前事件的真正处理逻辑
     */
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);

        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mode = TOUCH_MODE_SINGLE;
                preMovingTouchEvent = MotionEvent.obtain(event);

                if (flingX != null) {
                    flingX.cancel();
                }
                if (flingY != null) {
                    flingY.cancel();
                }
                break;
            case MotionEvent.ACTION_UP:
                mode = TOUCH_MODE_RELEASE;
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                mode = TOUCH_MODE_UNSET;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mode++;
                if (mode >= TOUCH_MODE_DOUBLE) {
                    scaleFactor = preScaleFactor = mView.getScaleX();
                    preTranslate.set(mView.getTranslationX(), mView.getTranslationY());
                    scaleBaseR = (float) distanceBetweenFingers(event);
                    centerPointBetweenFingers(event, preFocusCenter);
                    centerPointBetweenFingers(event, postFocusCenter);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode >= TOUCH_MODE_DOUBLE) {
                    //双指缩放
                    float scaleNewR = (float) distanceBetweenFingers(event);
                    centerPointBetweenFingers(event, postFocusCenter);
                    if (scaleBaseR <= 0) {
                        break;
                    }
                    scaleFactor = (scaleNewR / scaleBaseR) * preScaleFactor * 0.15f + scaleFactor * 0.85f;
                    int scaleState = TouchEventListener.FREE_SCALE;
                    float finalMinScale = isKeepInViewport ? minScale : minScale * 0.8f;
                    if (scaleFactor >= MAX_SCALE) {
                        scaleFactor = MAX_SCALE;
                        scaleState = TouchEventListener.MAX_SCALE;
                    } else if (scaleFactor <= finalMinScale) {
                        scaleFactor = finalMinScale;
                        scaleState = TouchEventListener.MIN_SCALE;
                    }
                    if (controlListener != null) {
                        int current = (int) (scaleFactor * 100);
                        //回调
                        if (scalePercentOnlyForControlListener != current) {
                            scalePercentOnlyForControlListener = current;
                            controlListener.onScaling(scaleState, scalePercentOnlyForControlListener);
                        }
                    }
                    mView.setPivotX(0);
                    mView.setPivotY(0);
                    mView.setScaleX(scaleFactor);
                    mView.setScaleY(scaleFactor);
                    float tx = postFocusCenter.x - (preFocusCenter.x - preTranslate.x) * scaleFactor / preScaleFactor;
                    float ty = postFocusCenter.y - (preFocusCenter.y - preTranslate.y) * scaleFactor / preScaleFactor;
                    mView.setTranslationX(tx);
                    mView.setTranslationY(ty);
                    keepWithinBoundaries();
                } else if (mode == TOUCH_MODE_SINGLE) {
                    //单指移动
                    float deltaX = event.getRawX() - preMovingTouchEvent.getRawX();
                    float deltaY = event.getRawY() - preMovingTouchEvent.getRawY();
                    onSinglePointMoving(deltaX, deltaY);
                }
                break;
            case MotionEvent.ACTION_OUTSIDE:
                //外界的事件
                break;
        }
        preMovingTouchEvent = MotionEvent.obtain(event);
        return true;
    }

    /**
     * 计算两个事件的移动距离
     */
    private float calculateMoveDistance(MotionEvent event1, MotionEvent event2) {
        if (event1 == null || event2 == null) {
            return 0f;
        }
        float disX = Math.abs(event1.getRawX() - event2.getRawX());
        float disY = Math.abs(event1.getRawX() - event2.getRawX());
        return (float) Math.sqrt(disX * disX + disY * disY);
    }

    /**
     * 单指移动
     */
    private void onSinglePointMoving(float deltaX, float deltaY) {
        float translationX = mView.getTranslationX() + deltaX;
        mView.setTranslationX(translationX);
        float translationY = mView.getTranslationY() + deltaY;
        mView.setTranslationY(translationY);
        keepWithinBoundaries();
    }

    /**
     * 需要保持在界限之内
     */
    private void keepWithinBoundaries() {
        //默认不在界限内，不做限制，直接返回
        if (!isKeepInViewport) {
            return;
        }
        calculateBound();
        int dBottom = layoutLocationInParent.bottom - viewportBox.bottom;
        int dTop = layoutLocationInParent.top - viewportBox.top;
        int dLeft = layoutLocationInParent.left - viewportBox.left;
        int dRight = layoutLocationInParent.right - viewportBox.right;
        float translationX = mView.getTranslationX();
        float translationY = mView.getTranslationY();
        //边界限制
        if (dLeft > 0) {
            mView.setTranslationX(translationX - dLeft);
        }
        if (dRight < 0) {
            mView.setTranslationX(translationX - dRight);
        }
        if (dBottom < 0) {
            mView.setTranslationY(translationY - dBottom);
        }
        if (dTop > 0) {
            mView.setTranslationY(translationY - dTop);
        }
    }

    /**
     * 移动时计算边界，赋值给本地的视图
     */
    private void calculateBound() {
        View v = mView;
        float left = v.getLeft() * v.getScaleX() + v.getTranslationX();
        float top = v.getTop() * v.getScaleY() + v.getTranslationY();
        float right = v.getRight() * v.getScaleX() + v.getTranslationX();
        float bottom = v.getBottom() * v.getScaleY() + v.getTranslationY();
        layoutLocationInParent.setValues((int) top, (int) left, (int) right, (int) bottom);
    }

    /**
     * 计算两个手指之间的距离
     */
    private double distanceBetweenFingers(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            float disX = Math.abs(event.getX(0) - event.getX(1));
            float disY = Math.abs(event.getY(0) - event.getY(1));
            return Math.sqrt(disX * disX + disY * disY);
        }
        return 1;
    }

    /**
     * 计算两个手指之间的中心点
     */
    private void centerPointBetweenFingers(MotionEvent event, PointF point) {
        float xPoint0 = event.getX(0);
        float yPoint0 = event.getY(0);
        float xPoint1 = event.getX(1);
        float yPoint1 = event.getY(1);
        point.set((xPoint0 + xPoint1) / 2f, (yPoint0 + yPoint1) / 2f);
    }

    /**
     * 设置视图是否要保持在窗口中
     */
    public void setKeepInViewport(boolean keepInViewport) {
        isKeepInViewport = keepInViewport;
    }

    /**
     * 设置控制的监听回调
     */
    public void setControlListener(TouchEventListener controlListener) {
        this.controlListener = controlListener;
    }
}

```

由于内部封装了移动与缩放的处理，所以我们只需要在事件容器内部调用这个方法即可：

```
java复制代码
public class CurtainLayout extends FrameLayout {

    private final TouchEventHandler mGestureHandler;
    private CurtainViewContrainer mCurtainViewContrainer;
    private boolean disallowIntercept = false;

    public CurtainLayout(@NonNull Context context) {
        this(context, null);
    }

    public CurtainLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CurtainLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setClipChildren(false);
        setClipToPadding(false);

        mCurtainViewContrainer = new CurtainViewContrainer(getContext());
        addView(mCurtainViewContrainer);

        mGestureHandler = new TouchEventHandler(getContext(), mCurtainViewContrainer);

        //设置是否在窗口内移动
        mGestureHandler.setKeepInViewport(false);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        this.disallowIntercept = disallowIntercept;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return (!disallowIntercept && mGestureHandler.detectInterceptTouchEvent(event)) || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return !disallowIntercept && mGestureHandler.onTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mGestureHandler.setViewport(w, h);
    }
}

```

对于一些复杂的处理都做了相关的注释，接下来看看加了事件处理之后的效果：

已经可以自由拖动与缩放了，但是目前的测量与布局是有问题的，加下来我们抽取与优化一下。

### 三、抽取Adapter与LayoutManager

首先,内部的子View肯定是不能直接写在 xml 中的，太不优雅了，加下来我们定义一个Adapter,用于填充数据，顺便做一个多类型的布局。

```
java复制代码public abstract class CurtainAdapter {

    //返回总共子View的数量
    public abstract int getItemCount();

    //根据索引创建不同的布局类型，如果都是一样的布局则不需要重写
    public int getItemViewType(int position) {
        return 0;
    }

    //根据类型创建对应的View布局
    public abstract View onCreateItemView(@NonNull Context context, @NonNull ViewGroup parent, int itemType);

    //可以根据类型或索引绑定数据
    public abstract void onBindItemView(@NonNull View itemView, int itemType, int position);

}

```

然后就是在绘制布局中通过设置 Apdater 来实现布局的添加与绑定逻辑。

```
java复制代码
    public void setAdapter(CurtainAdapter adapter) {
        mAdapter = adapter;
        inflateAllViews();
    }

    public CurtainAdapter getAdapter() {
        return mAdapter;
    }

    //填充Adapter布局
    private void inflateAllViews() {
        removeAllViewsInLayout();

        if (mAdapter == null || mAdapter.getItemCount() == 0) {
            return;
        }

        //添加布局
        for (int i = 0; i < mAdapter.getItemCount(); i++) {

            int itemType = mAdapter.getItemViewType(i);

            View view = mAdapter.onCreateItemView(getContext(), this, itemType);

            addView(view);
        }

        requestLayout();
    }

    //绑定布局中的数据
    private void performBindData() {
        if (mAdapter == null || mAdapter.getItemCount() == 0) {
            return;
        }

        post(() -> {

            for (int i = 0; i < mAdapter.getItemCount(); i++) {
                int itemType = mAdapter.getItemViewType(i);
                View view = getChildAt(i);

                mAdapter.onBindItemView(view, itemType, i);
            }

        });

    }

```

当然需要在指定的地方调用了，测量与布局中都需要处理。

```
java复制代码   @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int childCount = getChildCount();

        if (mAdapter == null || mAdapter.getItemCount() == 0 || childCount == 0) {
            setMeasuredDimension(0, 0);
            return;
        }

      ...
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (mAdapter == null || mAdapter.getItemCount() == 0) {
            return;
        }

         performLayout();

        performBindData();

    }

```

接下来的重点就是我们对布局的方式进行抽象化，最简单的肯定是上面这种宽高固定的，如果是垂直的排列，我们设置一个垂直的瀑布流管理器，设置宽度固定，高度自适应，如果宽度不固定，那么是无法到达瀑布流的效果的。

同理对另一种水平排列的瀑布流我们设置高度固定，宽度自适应。

所以必须要设置 LayoutManager，如果不设置就抛异常。

接下来就是 LayoutManager 的接口与具体调用：

```
java复制代码public interface ILayoutManager {

    public static final int DIRECTION_VERITICAL = 0;
    public static final int DIRECTION_HORIZONTAL = 1;

    public abstract int[] performMeasure(ViewGroup viewGroup, int rowCount, int horizontalSpacing, int verticalSpacing, int fixedValue);

    public abstract void performLayout(ViewGroup viewGroup, int rowCount, int horizontalSpacing, int verticalSpacing, int fixedValue);

    public abstract int getLayoutDirection();

}

```

有了接口之后我们就可以先写调用了：

```
java复制代码class CurtainViewContrainer extends ViewGroup {

    private ILayoutManager mLayoutManager;
    private int horizontalSpacing = 20;  //每一个Item的左右间距
    private int verticalSpacing = 20;  //每一个Item的上下间距
    private int mRowCount = 6;   // 一行多少个Item
    private int fixedWidth = CommUtils.dip2px(150);  //如果是垂直瀑布流，需要设置宽度固定
    private int fixedHeight = CommUtils.dip2px(180); //先写死，后期在抽取属性

    private CurtainAdapter mAdapter;

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int childCount = getChildCount();

        if (mAdapter == null || mAdapter.getItemCount() == 0 || childCount == 0) {
            setMeasuredDimension(0, 0);
            return;
        }

        measureChildren(widthMeasureSpec, heightMeasureSpec);

        if (mLayoutManager != null && (fixedWidth > 0 || fixedHeight > 0)) {

            for (int i = 0; i < childCount; i++) {
                View childView = getChildAt(i);

                if (mLayoutManager.getLayoutDirection() == ILayoutManager.DIRECTION_VERITICAL) {
                    measureChild(childView,
                            MeasureSpec.makeMeasureSpec(fixedWidth, MeasureSpec.EXACTLY),
                            heightMeasureSpec);
                } else {
                    measureChild(childView,
                            widthMeasureSpec,
                            MeasureSpec.makeMeasureSpec(fixedHeight, MeasureSpec.EXACTLY));
                }
            }

            int[] dimensions = mLayoutManager.performMeasure(this, mRowCount, horizontalSpacing, verticalSpacing,
                    mLayoutManager.getLayoutDirection() == ILayoutManager.DIRECTION_VERITICAL ? fixedWidth : fixedHeight);
            setMeasuredDimension(dimensions[0], dimensions[1]);

        } else {
            throw new RuntimeException("You need to set the layoutManager first");
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (mAdapter == null || mAdapter.getItemCount() == 0) {
            return;
        }

        if (mLayoutManager != null && (fixedWidth > 0 || fixedHeight > 0)) {
            mLayoutManager.performLayout(this, mRowCount, horizontalSpacing, verticalSpacing,
                    mLayoutManager.getLayoutDirection() == ILayoutManager.DIRECTION_VERITICAL ? fixedWidth : fixedHeight);

            performBindData();
        } else {
            throw new RuntimeException("You need to set the layoutManager first");
        }

    }

```

那么我们先来水平的LayoutManager，相对简单一些，看看如何具体实现：

```
java复制代码
public class HorizontalLayoutManager implements ILayoutManager {

    @Override
    public int[] performMeasure(ViewGroup viewGroup, int rowCount, int horizontalSpacing, int verticalSpacing, int fixedHeight) {

        int childCount = viewGroup.getChildCount();
        int curCount = 0;
        int totalControlHeight = 0;
        int totalControlWidth = 0;
        int curRow = 0;
        SparseArray<Integer> rowTotalWidth = new SparseArray<>();  //每一行的总宽度

        //开始遍历
        for (int i = 0; i < childCount; i++) {
            View childView = viewGroup.getChildAt(i);

            int row = curCount / rowCount;    //当前子View是第几行

            //已经测量过了，直接取宽高
            int width = childView.getMeasuredWidth();

            if (row == curRow) {
                //当前行
                totalControlWidth += width + horizontalSpacing;

            } else {
                //换行了
                totalControlWidth = width + horizontalSpacing;
            }

            rowTotalWidth.put(row, totalControlWidth);

            //赋值
            curCount++;
            curRow = row;
        }

        //循环结束之后开始计算真正的宽高
        totalControlHeight = (rowCount * (fixedHeight + verticalSpacing)) - verticalSpacing +
                viewGroup.getPaddingTop() + viewGroup.getPaddingBottom();

        List<Integer> widthList = new ArrayList<>();
        for (int i = 0; i < rowTotalWidth.size(); i++) {
            Integer width = rowTotalWidth.get(i);
            widthList.add(width);
        }
        totalControlWidth = Collections.max(widthList);

        rowTotalWidth.clear();
        rowTotalWidth = null;

        return new int[]{totalControlWidth - horizontalSpacing, totalControlHeight - verticalSpacing};
    }

    @Override
    public void performLayout(ViewGroup viewGroup, int rowCount, int horizontalSpacing, int verticalSpacing, int fixedHeight) {
        int childCount = viewGroup.getChildCount();

        int curCount = 1;
        int layoutChildViewCurX = viewGroup.getPaddingLeft();
        int layoutChildViewCurY = viewGroup.getPaddingTop();

        int curRow = 0;

        //开始遍历
        for (int i = 0; i < childCount; i++) {
            View childView = viewGroup.getChildAt(i);

            int row = curCount / rowCount;    //当前子View是第几行

            //每一个子View宽度
            int width = childView.getMeasuredWidth();

            childView.layout(layoutChildViewCurX, layoutChildViewCurY, layoutChildViewCurX + width, layoutChildViewCurY + fixedHeight);

            if (row == curRow) {
                //同一行
                layoutChildViewCurX += width + horizontalSpacing;

            } else {
                //换行了
                layoutChildViewCurX = childView.getPaddingLeft();
                layoutChildViewCurY += fixedHeight + verticalSpacing;
            }

            //赋值
            curCount++;
            curRow = row;

        }
    }

    @Override
    public int getLayoutDirection() {
        return DIRECTION_HORIZONTAL;
    }
}

```

对于水平的布局方式来说，高度是固定的，我们很容易的就能计算出来，但是宽度每一行的可能都不一样，我们用一个List记录每一行的总宽度，在最后设置的时候取出最大的一行作为容器的宽度，记得要减去一个间距哦。

那么不同宽度的水平布局方式效果的实现就是这样：

实现是实现了，但是这么计算是不是有问题？每一行的最高高度好像不是太准确，如果每一列都有一个最大高度，但是不是同一列，那么测量的高度就比实际高度要更高。

加一个灰色背景就可以看到效果：

我们再优化一下，它应该是计算每一列的总共高度，然后选出最大高度才对：

```
java复制代码    @Override
    public int[] performMeasure(ViewGroup viewGroup, int rowCount, int horizontalSpacing, int verticalSpacing, int fixedWidth) {

        int childCount = viewGroup.getChildCount();
        int curPosition = 0;
        int totalControlHeight = 0;
        int totalControlWidth = 0;
        SparseArray<List<Integer>> columnAllHeight = new SparseArray<>(); //每一列的全部高度

        //开始遍历
        for (int i = 0; i < childCount; i++) {
            View childView = viewGroup.getChildAt(i);

            int row = curPosition / rowCount;    //当前子View是第几行
            int column = curPosition % rowCount;    //当前子View是第几列

            //已经测量过了，直接取宽高
            int height = childView.getMeasuredHeight();

            List<Integer> integers = columnAllHeight.get(column);
            if (integers == null || integers.isEmpty()) {
                integers = new ArrayList<>();
            }
            integers.add(height + verticalSpacing);
            columnAllHeight.put(column, integers);

            //赋值
            curPosition++;
        }

        //循环结束之后开始计算真正的宽高
        totalControlWidth = (rowCount *
                (fixedWidth + horizontalSpacing) + viewGroup.getPaddingLeft() + viewGroup.getPaddingRight());

        List<Integer> totalHeights = new ArrayList<>();
        for (int i = 0; i < columnAllHeight.size(); i++) {
            List<Integer> heights = columnAllHeight.get(i);
            int totalHeight = 0;
            for (int j = 0; j < heights.size(); j++) {
                totalHeight += heights.get(j);
            }
            totalHeights.add(totalHeight);
        }
        totalControlHeight = Collections.max(totalHeights);

        columnAllHeight.clear();
        columnAllHeight = null;

        return new int[]{totalControlWidth - horizontalSpacing, totalControlHeight - verticalSpacing};
    }

```

再看看效果：

宽高真正的测量准确之后我们接下来就开始属性的抽取与封装了。

### 四、自定义属性

我们先前都是使用的成员变量来控制一些间距与逻辑的触发，这就跟业务耦合了，如果想做到通用的一个效果，肯定还是要抽取自定义属性，做到对应的配置开关，就可以适应更多的场景使用，也是开源项目的必备技能。

细数一下我们需要控制的属性：

1. enableScale 是否支持缩放
2. maxScale 缩放的最大比例
3. minScale 缩放的最小比例
4. moveInViewport 是否只能在布局内部移动
5. horizontalSpacing item的水平间距
6. verticalSpacing item的垂直间距
7. fixed_width 竖向的排列 - 宽度定死 并设置对应的LayoutManager
8. fixed_height 横向的排列 - 高度定死 并设置对应的LayoutManager

定义属性如下：

```
xml复制代码    <!--  全屏幕布布局自定义属性  -->
    <declare-styleable name="CurtainLayout">
        <!--Item的横向间距-->
        <attr name="horizontalSpacing" format="dimension" />
        <!--Item的垂直间距-->
        <attr name="verticalSpacing" format="dimension" />
        <!--每行需要展示多少数量的Item-->
        <attr name="rowCount" format="integer" />
        <!--垂直方向瀑布流布局，固定宽度为多少-->
        <attr name="fixedWidth" format="dimension" />
        <!--水平方向瀑布流布局，固定高度为多少-->
        <attr name="fixedHeight" format="dimension" />
        <!--是否只能在布局内部移动 当为false时候为自由移动-->
        <attr name="moveInViewport" format="boolean" />
        <!--是否可以缩放-->
        <attr name="enableScale" format="boolean" />
        <!--最大与最小的缩放比例-->
        <attr name="maxScale" format="float" />
        <attr name="minScale" format="float" />
    </declare-styleable>

```

取出属性并对容器布局与触摸处理器做赋值的操作：

```
java复制代码
public class CurtainLayout extends FrameLayout {

    private int horizontalSpacing;
    private int verticalSpacing;
    private int rowCount;
    private int fixedWidth;
    private int fixedHeight;
    private boolean moveInViewport;
    private boolean enableScale;
    private float maxScale;
    private float minScale;

    public CurtainLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setClipChildren(false);
        setClipToPadding(false);

        mCurtainViewContrainer = new CurtainViewContrainer(getContext());
        addView(mCurtainViewContrainer);

        initAttr(context, attrs);

        mGestureHandler = new TouchEventHandler(getContext(), mCurtainViewContrainer);

        //设置是否在窗口内移动
        mGestureHandler.setKeepInViewport(moveInViewport);
        mGestureHandler.setEnableScale(enableScale);
        mGestureHandler.setMinScale(minScale);
        mGestureHandler.setMaxScale(maxScale);

        mCurtainViewContrainer.setHorizontalSpacing(horizontalSpacing);
        mCurtainViewContrainer.setVerticalSpacing(verticalSpacing);
        mCurtainViewContrainer.setRowCount(rowCount);
        mCurtainViewContrainer.setFixedWidth(fixedWidth);
        mCurtainViewContrainer.setFixedHeight(fixedHeight);

        if (fixedWidth > 0 || fixedHeight > 0) {
            if (fixedWidth > 0) {
                mCurtainViewContrainer.setLayoutDirectionVertical(fixedWidth);
            } else {
                mCurtainViewContrainer.setLayoutDirectionHorizontal(fixedHeight);
            }
        }
    }

    /**
     * 获取自定义属性
     */
    private void initAttr(Context context, AttributeSet attrs) {

        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.CurtainLayout);
        this.horizontalSpacing = mTypedArray.getDimensionPixelSize(R.styleable.CurtainLayout_horizontalSpacing, 20);
        this.verticalSpacing = mTypedArray.getDimensionPixelSize(R.styleable.CurtainLayout_verticalSpacing, 20);
        this.rowCount = mTypedArray.getInteger(R.styleable.CurtainLayout_rowCount, 6);
        this.fixedWidth = mTypedArray.getDimensionPixelOffset(R.styleable.CurtainLayout_fixedWidth, 150);
        this.fixedHeight = mTypedArray.getDimensionPixelSize(R.styleable.CurtainLayout_fixedHeight, 180);
        this.moveInViewport = mTypedArray.getBoolean(R.styleable.CurtainLayout_moveInViewport, false);
        this.enableScale = mTypedArray.getBoolean(R.styleable.CurtainLayout_enableScale, true);
        this.minScale = mTypedArray.getFloat(R.styleable.CurtainLayout_minScale, 0.7f);
        this.maxScale = mTypedArray.getFloat(R.styleable.CurtainLayout_maxScale, 1.5f);

        mTypedArray.recycle();
    }
    ...

    public void setMoveInViewportInViewport(boolean moveInViewport) {
        this.moveInViewport = moveInViewport;
        mGestureHandler.setKeepInViewport(moveInViewport);
    }

    public void setEnableScale(boolean enableScale) {
        this.enableScale = enableScale;
        mGestureHandler.setEnableScale(enableScale);
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
        mGestureHandler.setMinScale(minScale);
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
        mGestureHandler.setMaxScale(maxScale);
    }

    public void setHorizontalSpacing(int horizontalSpacing) {
        mCurtainViewContrainer.setHorizontalSpacing(horizontalSpacing);
    }

    public void setVerticalSpacing(int verticalSpacing) {
        mCurtainViewContrainer.setVerticalSpacing(verticalSpacing);
    }

    public void setRowCount(int rowCount) {
        mCurtainViewContrainer.setRowCount(rowCount);
    }

    public void setFixedWidth(int fixedWidth) {
        mCurtainViewContrainer.setLayoutDirectionVertical(fixedWidth);
    }

    public void setFixedHeight(int fixedHeight) {
        mCurtainViewContrainer.setLayoutDirectionHorizontal(fixedHeight);
    }

```

然后在布局容器与事件处理类中做对应的赋值操作即可。

如何使用？

```
xml复制代码    <CurtainLayout
        android:id="@+id/curtain_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"

        app:enableScale="true"
        app:fixedWidth="150dp"
        app:horizontalSpacing="10dp"
        app:maxScale="1.5"
        app:minScale="0.8"
        app:moveInViewport="true"
        app:rowCount="6"
        app:verticalSpacing="10dp">

    </CurtainLayout>

```

如果在xml中设置过 fixedWidth 或者 fixedHeight ，那么在 Activity 中也可以不设置 LayoutManager 了。

```
kotlin复制代码
    val list = listOf<String>( ... )

    val adapter = Viewgroup6Adapter(list)

    val curtainView = findViewById<CurtainLayout>(R.id.curtain_view)

    curtainView.adapter = adapter

```

最终效果：

## 后记

关于 ViewGroup 的测量与布局与事件，我们已经从易到难复习了四期了，相信同学应该是能掌握了。

话说到里就应该到了完结时刻，关于自定义View与自定义ViewGroup的复习与回顾就到此告一段落了，对于市面上能见到的一些布局效果，基本上能通过自定义ViewGroup与自定义View来实现。其实很早就想完结了，因为感觉这些东西有一点过于基础了，好像大家都不是很有兴趣看这些基础的东西。

自定义 View 可以很方便的做自定义的绘制与本身与内部的一些移动，而对于一些多View移动的特效，我们就算用自定义 View 难以实现或实现的比较复杂的话，也能使用 Behivor 或者 MotionLayot 来实现，或者一些滚动的效果我们甚至可以使用RV的LayoutManager来做也是可行的，当然这就是另外的知识点了，相信大家也或多或少有了解的。

如果有兴趣也可以看看我之前的 Behivor 文章 [【传送门】](https://juejin.cn/post/7108992111600631839) 或者 MotionLayot 的文章，[【传送门】](https://juejin.cn/post/7110027299214999589)。

同时也可以搜索与翻看之前的文章哦。

本文的代码均可以在我的Kotlin测试项目中看到，[【传送门】](https://link.juejin.cn/?target=https%3A%2F%2Fgitee.com%2Fnewki123456%2FKotlin-Room)。你也可以关注我的这个Kotlin项目，我有时间都会持续更新。

关于本文的全屏滑动效果，我也会开源传到 MavenCentral 供大家直接依赖或学习使用，[【传送门】](https://link.juejin.cn/?target=https%3A%2F%2Fgitee.com%2Fnewki123456%2FCurtainLayout)

使用方式：Gradle中直接依赖即可：

> 
> 
> 
> implementation "com.gitee.newki123456:curtain_layout:1.0.0"
> 

好了，如果类似的效果有更多的更好的其他方式，也希望大家能评论区交流一下。

惯例，我如有讲解不到位或错漏的地方，希望同学们可以指出。

如果感觉本文对你有一点点的帮助，还望你能`点赞`支持一下,你的支持是我最大的动力。

哎，找图片都找了接近一个小时，如果大家想要对应的图片也可以去项目中拿哦！😅😅

Ok,这一期就此完结。