# Android自定义View的图片绘制与交互实战，用户新功能指引控件实现 - 掘金

[https://juejin.cn/post/7310878717178036274](https://juejin.cn/post/7310878717178036274)

# Android自定义View的图片绘制与交互实战，用户新功能指引控件实现

613   阅读14分钟

专栏：

## 前言

自定义 View 的学(复)习又来了，距离之前更新自定义 View 系列的文章已经过一年多了，基本上已经覆盖了大部分自定义 View 的各种路线与思路以及相关的实现与解析，有兴趣的可以去我自定义 View 专栏指点一下[【传送门】](https://juejin.cn/column/7170970245723586574)。（PS：说实话这么长时间其实我自己都忘了这真的是我写的吗？果然我还是资质愚钝啊 😂，赶紧复习一下）

当然我的一些文章都是在基础之上的，最好需要读者有一定自定义 View 基础最好，有了基础之后可以跟着学(复)习一下各种自定义 View 实现思路，本文就基于之前的文章一些没有讲到的点进行的一些补充。

之前的自定义 View 我们讲到了各种圆形，条形，文字，甚至还绘制了贝塞尔曲线，sin曲线等，唯独对 BitMap 图片的绘制没有怎么讲，本期我们就一起学(复)习一下以我们常用的用户新功能指引的自定义View实现来作为示例。

用户新功能指引我们一般都是在指定的 View 对象上方覆盖一层指引的图片，如果有多个新功能，我们还需要有点击下一步直至完成指引。在这一个自定义View中我们就能很有效的学(复)习到如何定位图片、如何绘制图片、如何定义点击范围等。

本期的最终效果图如下：

[](Android%E8%87%AA%E5%AE%9A%E4%B9%89View%E7%9A%84%E5%9B%BE%E7%89%87%E7%BB%98%E5%88%B6%E4%B8%8E%E4%BA%A4%E4%BA%92%E5%AE%9E%E6%88%98%EF%BC%8C%E7%94%A8%E6%88%B7%E6%96%B0%E5%8A%9F%E8%83%BD%E6%8C%87%E5%BC%95%E6%8E%A7%E4%BB%B6%E5%AE%9E%E7%8E%B0%20-%20%E6%8E%98%E9%87%91/80f8e7f51daf488b83d63318eaca13f2tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

下面就跟着一起开始吧，话不多说，Let's go

[](Android%E8%87%AA%E5%AE%9A%E4%B9%89View%E7%9A%84%E5%9B%BE%E7%89%87%E7%BB%98%E5%88%B6%E4%B8%8E%E4%BA%A4%E4%BA%92%E5%AE%9E%E6%88%98%EF%BC%8C%E7%94%A8%E6%88%B7%E6%96%B0%E5%8A%9F%E8%83%BD%E6%8C%87%E5%BC%95%E6%8E%A7%E4%BB%B6%E5%AE%9E%E7%8E%B0%20-%20%E6%8E%98%E9%87%91/0e526cc29ee84b22911915126f8561bftplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

### 一、封装参数，实现基本元素的绘制

我们最好是把每一个步骤需要的一些资源封装到一个单独的对象来管理，那么需要哪些属性呢？

首先我们的指引分大致可以分为为主图，箭头，提示内容三部分，我们先对主图和提示内容做一些封装。

我们显示出来的指引主图是基于页面上的一个 View 展示的，所以我们需要一个锚点目标 View 。如同我们无法预知 UI 会给我们什么样的图?有多大?所以我们需要自己定义偏移量。

[](Android%E8%87%AA%E5%AE%9A%E4%B9%89View%E7%9A%84%E5%9B%BE%E7%89%87%E7%BB%98%E5%88%B6%E4%B8%8E%E4%BA%A4%E4%BA%92%E5%AE%9E%E6%88%98%EF%BC%8C%E7%94%A8%E6%88%B7%E6%96%B0%E5%8A%9F%E8%83%BD%E6%8C%87%E5%BC%95%E6%8E%A7%E4%BB%B6%E5%AE%9E%E7%8E%B0%20-%20%E6%8E%98%E9%87%91/5e6d54cf47fb49b48e75d9ea5658bf71tplv-k3u1fbpfcp-jj-mark3024000q75.awebp)

在自定义的实体对象中，我们还需要主图与提示内容图片的资源文件，其次我们需要分别定义主图和提示内容图片的偏移值方便调整位置，我们可以在锚点View的基础上开始设置上下偏移。

我们定义好这样一个对象：

```
复制代码public class GuideInfo {

    public View targetView;   //当前指引的具体目标,我称之为锚点View

    public int mainImgRes;    // 每一个锚点View对应的主指引图资源，一般都是覆盖在锚点View上面
    public int[] mainImgLocation;  //每一个锚点View对应的屏幕XY坐标，对应主指引图的展示位置

    public int tipImgRes;   //具体提示的图片资源，一般是在主指引图的下方
    public int tipImgMoveX;   //提示View图需要偏移的X位置
    public int tipImgMoveY;   //提示View图需要偏移的Y位置
}

```

剩下的我们就能找到对应的锚点 View ,并且设置主图的偏移 Location 位置，并设置对应的图片资源和提示文本的偏移XY值。

我们在 Activity 中设置如下：

```
scss复制代码    val partTimeJobLocation = IntArray(2)
    mYYJobsLl.getLocationOnScreen(partTimeJobLocation)
    partTimeJobLocation[0] = partTimeJobLocation[0] + CommUtils.dip2px(10)
    partTimeJobLocation[1] = partTimeJobLocation[1] - CommUtils.dip2px(25)

    val cvBuildLocation = IntArray(2)
    mCVBuildLl.getLocationOnScreen(cvBuildLocation)
    cvBuildLocation[0] = cvBuildLocation[0] + 0
    cvBuildLocation[1] = cvBuildLocation[1] - CommUtils.dip2px(25)

    val freelancerLocation = IntArray(2)
    mFreelancerLl.getLocationOnScreen(freelancerLocation)
    freelancerLocation[0] = freelancerLocation[0] + CommUtils.dip2px(10)
    freelancerLocation[1] = freelancerLocation[1] - CommUtils.dip2px(25)

    val fullTimeLocation = IntArray(2)
    mFullTimeLl.getLocationOnScreen(fullTimeLocation)
    fullTimeLocation[0] = fullTimeLocation[0] + CommUtils.dip2px(10)
    fullTimeLocation[1] = fullTimeLocation[1] - CommUtils.dip2px(30)

    val rewardsLocation = IntArray(2)
    mRewardsLl.getLocationOnScreen(rewardsLocation)
    rewardsLocation[0] = rewardsLocation[0] + 0
    rewardsLocation[1] = rewardsLocation[1] - CommUtils.dip2px(25)

    val newsFeedLocation = IntArray(2)
    mNewsFeedLl.getLocationOnScreen(newsFeedLocation)
    newsFeedLocation[0] = newsFeedLocation[0] + CommUtils.dip2px(10)
    newsFeedLocation[1] = newsFeedLocation[1] - CommUtils.dip2px(25)

    val infos = listOf(
        GuideInfo(
            mYYJobsLl, R.drawable.iv_picture_part_time_job, partTimeJobLocation,
            R.drawable.iv_yy_part_time_job_word, CommUtils.dip2px(30), 0
        ),
        GuideInfo(
            mCVBuildLl, R.drawable.iv_picture_cv_builder, cvBuildLocation,
            R.drawable.iv_cv_builder_word, 0, 0
        ),
        GuideInfo(
            mFreelancerLl, R.drawable.iv_picture_freelancer, freelancerLocation,
            R.drawable.iv_yy_freelancer_word, -CommUtils.dip2px(30), 0
        ),
        GuideInfo(
            mFullTimeLl, R.drawable.iv_picture_full_time_jobs, fullTimeLocation,
            R.drawable.iv_yy_full_time_word, CommUtils.dip2px(30), 0
        ),
        GuideInfo(
            mRewardsLl, R.drawable.iv_picture_rewards, rewardsLocation,
            R.drawable.iv_yy_promotion_word, 0, 0
        ),
        GuideInfo(
            mNewsFeedLl, R.drawable.iv_picture_news_feed, newsFeedLocation,
            R.drawable.iv_yy_new_feed_word, -CommUtils.dip2px(60), 0
        ),
    )

    //设置数据源
     mUserGuideView.setupGuideInfo(infos)

```

接下来我们就能创建我们的用户指引 View 了，我们先在其中绘制指引的前景，主图，和指引提示图。

```
复制代码public class UserGuideView extends View {

    private Bitmap fgBitmap;  // 前景
    private Canvas mCanvas;   // 绘制Bitmap画布
    private Paint mPaint;    // 绘制Bitmap画笔

    private int screenW, screenH;   // 屏幕宽高
    private int contentOffestMargin = 20; //内容偏移值
    private int margin = 20;   //targetView与底部的间距的调整

    private int maskColor = 0x99000000;   // 蒙版层颜色

    private List<GuideInfo> guideInfos;  //全部封装的指引数据
    private View targetView;      //当前步骤需要绘制的锚定View
    private Integer mImageRes;   //当前步骤需要绘制的当前主图片
    private int[] mLocation;     //当前步骤需要绘制的当前主图片的位置，为锚定View的X,Y坐标
    private Bitmap tipBitmap;   //当前步骤需要绘制的Tip位图
    private int tipViewMoveX;   //当前步骤需要绘制的Tip图片的X偏移
    private int tipViewMoveY;   //当前步骤需要绘制的Tip图片的Y偏移

    private int curIndedx = 0;   //当前的步骤

    public UserGuideView(Context context) {
        this(context, null);
    }

    public UserGuideView(Context context, AttributeSet set) {
        this(context, set, -1);
    }

    public UserGuideView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 初始化对象
        init(context);
    }

    /**
     * 初始化对象
     */
    private void init(Context context) {
        screenW = ScreenUtils.getScreenWidth(context);
        screenH = ScreenUtils.getScreenHeith(context);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

        mPaint.setARGB(0, 255, 0, 0);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        BlurMaskFilter.Blur blurStyle = BlurMaskFilter.Blur.SOLID;
        mPaint.setMaskFilter(new BlurMaskFilter(15, blurStyle));

        fgBitmap = createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888, 2);
        if (fgBitmap == null) {
            throw new RuntimeException("无法创建Bitmap");
        }
        mCanvas = new Canvas(fgBitmap);

        mCanvas.drawColor(maskColor);

    }

    /**
     * 设置数据入口
     */
    public void setupGuideInfo(List<GuideInfo> infos) {
        guideInfos = infos;
        check2NextGuide();
    }

    //点击下一步去下一个指引,如果没有了则直接关闭，并回调
    private void check2NextGuide() {

        if (guideInfos == null || guideInfos.size() == 0) {
            this.setVisibility(View.GONE);
            if (this.onDismissListener != null) {
                onDismissListener.onDismiss(UserGuideView.this);
            }
        } else {
            //当有值的时候
            if (curIndedx >= guideInfos.size()) {
                this.setVisibility(View.GONE);
                if (this.onDismissListener != null) {
                    onDismissListener.onDismiss(UserGuideView.this);
                }
            } else {
                setNextTagetView(guideInfos.get(curIndedx));
                curIndedx++;
            }

        }
    }

    //设置下一步要绘制的Target
    private void setNextTagetView(GuideInfo info) {

        if (info != null && info.targetView != null) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            mCanvas.drawPaint(paint);
            mCanvas.drawColor(maskColor);

            this.targetView = info.targetView;
            this.mImageRes = info.mainImgRes;
            this.mLocation = info.mainImgLocation;
            this.tipBitmap = getBitmapFromResId(info.tipImgRes);
            this.tipViewMoveX = info.tipImgMoveX;
            this.tipViewMoveY = info.tipImgMoveY;
        }

        invalidate();
        setVisibility(VISIBLE);
    }
}

```

我们的变量与数据的赋值，切换的逻辑就完成了，我们把所有的数据源传入进来，根据当前索引来切换要绘制的资源，最终调用了 invalidate 刷新方法，那么加下来的重点就是在 onDraw() 如何绘制了。

```
复制代码    protected void onDraw(Canvas canvas) {

        // 绘制前景位图
        canvas.drawBitmap(fgBitmap, 0, 0, null);

        // 如果目标视图为空，则直接返回
        if (targetView == null) {
            return;
        }

        // 初始化变量
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        int vWidth = targetView.getWidth();
        int vHeight = targetView.getHeight();

        // 获取目标视图在屏幕上的可见矩形，注意上下左右的默认间距
        Rect tagetRect = new Rect();
        targetView.getGlobalVisibleRect(tagetRect);
        tagetRect.offset(0, -statusBarHeight);
        left = tagetRect.left - contentOffestMargin;
        top = tagetRect.top - contentOffestMargin;
        right = tagetRect.right + contentOffestMargin;
        bottom = tagetRect.bottom + contentOffestMargin;

        // 根据目标视图位置调整矩形边界，避免遮挡或超出屏幕
        if (left == 0) {
            left += contentOffestMargin;
        } else if (top == 0) {
            top += contentOffestMargin;
        } else if (right == screenW) {
            right -= contentOffestMargin;
        } else if (bottom == screenH) {
            bottom -= contentOffestMargin;
        }

        //绘制主图Image
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mImageRes);
        Paint paint = new Paint();
        mCanvas.drawBitmap(bitmap, mLocation[0] - contentOffestMargin, mLocation[1] - contentOffestMargin, paint);

        // 根据目标视图位置绘制箭头和提示view
        if (bottom < screenH / 2 || (screenH / 2 - top > bottom - screenH / 2)) {
            // 获取提示详情View的顶部位置
            int jtTop = getUpFormTargetBottom(bottom, vHeight);

            if (right <= screenW / 2) { //如果提示View在左侧显示

                if (tipBitmap != null) {
                    int tipTop = jtTop + tipViewMoveY;  //top需要加上偏移Y

                    // 如果提示图片超出屏幕左边界,不能超过左边界
                    if (left < contentOffestMargin) {
                        left = contentOffestMargin;
                    }
                    int tipLeft = left + tipViewMoveX;   //left需要需要加上偏移X

                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            } else if (left >= screenW / 2) { //右

                if (tipBitmap != null) {
                    int tipTop = jtTop + tipViewMoveY; //top需要加上偏移Y

                    // 如果提示图片超出屏幕右边界
                    if (left + tipBitmap.getWidth() > screenW - contentOffestMargin) {
                        left = screenW - tipBitmap.getWidth() - contentOffestMargin;
                    }
                    int tipLeft = left + tipViewMoveX;

                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            } else {// 中

                //中间就默认居中展示
                if (tipBitmap != null) {
                    int tipTop = /*showArrow ? jtTop + jtUpCenter.getHeight() + tipViewMoveY :*/ jtTop + tipViewMoveY;
                    int tipLeft = left + vWidth / 2 - tipBitmap.getWidth() / 2 + tipViewMoveX;
                    canvas.drawBitmap(tipBitmap, tipLeft + tipViewMoveX, tipTop + tipViewMoveY, null);

                }
            }
        } else { //屏幕下面是同样的逻辑

            int jtDownCenterTop = getDownFormTargetTop(jtDownCenter, top, vHeight);

            if (right <= screenW / 2) { // 左侧
                int jtTop = getDownFormTargetTop(jtDownLeft, top, vHeight);

                if (tipBitmap != null) {
                    int tipTop = top - tipBitmap.getHeight() - margin + tipViewMoveX;

                    // 如果提示图片超出屏幕左边界,不能超过左边界
                    if (left < contentOffestMargin) {
                        left = contentOffestMargin;
                    }
                    int tipLeft = left + tipViewMoveX;   //left需要需要加上偏移X

                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            } else if (left >= screenW / 2) {// 右侧
                int jtTop = getDownFormTargetTop(jtDownRight, top, vHeight);

                if (tipBitmap != null) {

                    int tipTop = top - tipBitmap.getHeight() - margin + tipViewMoveY;

                    // 如果提示图片超出屏幕右边界
                    if (left + tipBitmap.getWidth() > screenW - contentOffestMargin) {
                        left = screenW - tipBitmap.getWidth() - contentOffestMargin;
                    }
                    int tipLeft = left + tipViewMoveX;

                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            } else { // 中间

                if (tipBitmap != null) {
                    int tipLeft = left + contentOffestMargin + (vWidth / 2 - tipBitmap.getWidth() / 2) + tipViewMoveX;
                    int tipTop = showArrow ? jtDownCenterTop - tipBitmap.getHeight() + tipViewMoveY : top - tipBitmap.getHeight() - margin + tipViewMoveY;
                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            }
        }

    }

    private int getUpFormTargetBottom(int targetBottom, int targetHeight) {
        int jtTop = 0;
        if (highLightStyle == VIEWSTYLE_CIRCLE) {
            jtTop = targetBottom + (0 - targetHeight / 2) + margin + contentOffestMargin;
        } else {
            jtTop = targetBottom + margin + contentOffestMargin;
        }
        return jtTop;
    }

    private int getDownFormTargetTop(Bitmap jtBitmap, int targetTop, int targetHeight) {
        int jtTop = 0;
        if (highLightStyle == VIEWSTYLE_CIRCLE) {
            jtTop = targetTop - (0 - targetHeight / 2) - jtBitmap.getHeight() - margin - contentOffestMargin;
        } else {
            jtTop = targetTop - jtBitmap.getHeight() - margin - contentOffestMargin;
        }
        return jtTop;
    }

```

基本上每一步都有详细的步骤，我们先进行前景的颜色绘制，然后确定整体的绘制区域的边距，方便后期判断是否超出边界。

主图的绘制，由于我们在数据赋值之前就已经设置好了偏移量，所以我们可以直接绘制，当然如果你觉得不保险也可以加入边界的判断，这里我就在后期修改XY的偏移值就能实现，一般主图不会很大我就没有判断。

而提示内容图片一般会相对比较大，所以我们最好是对X的判断进行是否超出边界的判断。再次基础上我们再进行XY的偏移值进行微调，此时就可以做出 UI 效果图类似的布局。

由于我们需要判断内容提示图片在哪一个方位，他们的边界判断逻辑也有一些区别，所以对它在上方还是下方，在左侧还是右侧还是中间都做了不同的处理。最终确定了最终的top 和 left 之后我们就能绘制出对应的内容提示图片了。

那么此时的效果就如下图所示：

当然如果你的 UI 设计指引的时候没有内容提示图片这一方面，你也可以直接传空的资源，此时就只有主图一个图片，调整对应的主图位置就能实现一个图片的效果了。

### 二、加入箭头，实现可控元素绘制

其实可以看出我们在绘制内容区域的时候其实是预留了箭头的一些变量的，为什么去掉呢？是因为箭头也是可选的，例如我们有些指引就是有箭头，有些指引确没有箭头。

所以我们就需要定义一个变量，是否需要展示箭头，并设置对应的绘制代码，其次提示区域的 top 此时就需要重新判断如果有箭头则需要加上箭头的高度等等。

我们先在自定义的对象中加入箭头的相关处理：

```
arduino复制代码public class GuideInfo {

    ...

    public int arrowImgRes;   //箭头的图片资源，一般是在主指引图的下方
    public int arrowImgMoveX;   //箭头需要偏移的X位置
    public int arrowImgMoveY;   //箭头需要偏移的Y位置
}

```

其次我们在 Activity 赋值对象的时候就要定义箭头的相关处理：

```
scss复制代码val infos = listOf(
    GuideInfo(
        mYYJobsLl, R.drawable.iv_picture_part_time_job, partTimeJobLocation,
        R.drawable.iv_yy_part_time_job_word, CommUtils.dip2px(30), 0,
        R.drawable.iv_arrow_yy_part_time_guide, CommUtils.dip2px(30), 0,
    ),
    GuideInfo(
        mCVBuildLl, R.drawable.iv_picture_cv_builder, cvBuildLocation,
        R.drawable.iv_cv_builder_word, 0, 0,
        R.drawable.iv_arrow_cv_builder, -CommUtils.dip2px(20), 0,
    ),
    GuideInfo(
        mFreelancerLl, R.drawable.iv_picture_freelancer, freelancerLocation,
        R.drawable.iv_yy_freelancer_word, -CommUtils.dip2px(30), 0,
        R.drawable.iv_arrow_yy_rewards_guide, CommUtils.dip2px(20), 0,
    ),
    GuideInfo(
        mFullTimeLl, R.drawable.iv_picture_full_time_jobs, fullTimeLocation,
        R.drawable.iv_yy_full_time_word, CommUtils.dip2px(30), 0,
        R.drawable.iv_arrow_yy_full_time_guide, CommUtils.dip2px(30), 0,
    ),
    GuideInfo(
        mRewardsLl, R.drawable.iv_picture_rewards, rewardsLocation,
        R.drawable.iv_yy_promotion_word, 0, 0,
        R.drawable.iv_arrow_yy_promotion_guide, -CommUtils.dip2px(20), 0,
    ),
    GuideInfo(
        mNewsFeedLl, R.drawable.iv_picture_news_feed, newsFeedLocation,
        R.drawable.iv_yy_new_feed_word, -CommUtils.dip2px(60), 0,
        R.drawable.iv_arrow_yy_new_feed_guide, CommUtils.dip2px(30), 0,
    ),
)

    mUserGuideView.setShowArrow(true)
    //设置数据源
    mUserGuideView.setupGuideInfo(infos)

```

赋值与解构我们就顺便改了：

```
复制代码 private void setNextTagetView(GuideInfo info) {

    ...
    this.arrowBitmap = getBitmapFromResId(info.arrowImgRes);
    this.arrowMoveX = info.arrowImgMoveX;
    this.arrowMoveY = info.arrowImgMoveY;

 }

```

重点就是我们的是否展示箭头的判断啦，如果有需要处理它的高度和偏移值。

OnDraw 的需要修改的核心代码如下：

```
scss复制代码      ...

       // 根据目标视图位置绘制箭头和提示view
        if (bottom < screenH / 2 || (screenH / 2 - top > bottom - screenH / 2)) {
            // 获取提示详情View的顶部位置
            int jtTop = getUpFormTargetBottom(bottom, vHeight);

            if (right <= screenW / 2) { //如果提示View在左侧显示
                if (showArrow && arrowBitmap != null) {
                    canvas.drawBitmap(arrowBitmap, left + arrowBitmap.getWidth() / 2 + arrowMoveX, jtTop + arrowMoveY, null);
                }

                if (tipBitmap != null) {
                    int tipTop = showArrow && arrowBitmap != null ? jtTop + arrowBitmap.getHeight() + tipViewMoveY + arrowMoveY
                            : jtTop + tipViewMoveY;  //top需要加上偏移Y,注意处理箭头的高度和偏移Y

                    // 如果提示图片超出屏幕左边界,不能超过左边界
                    if (left < contentOffestMargin) {
                        left = contentOffestMargin;
                    }
                    int tipLeft = left + tipViewMoveX;   //left需要需要加上偏移X

                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            } else if (left >= screenW / 2) { //右
                if (showArrow && arrowBitmap != null) {
                    canvas.drawBitmap(arrowBitmap, left - arrowBitmap.getWidth() / 2 + arrowMoveX, jtTop + arrowMoveY, null);
                }
                if (tipBitmap != null) {
                    int tipTop = showArrow && arrowBitmap != null ? jtTop + arrowBitmap.getHeight() + tipViewMoveY + arrowMoveY
                            : jtTop + tipViewMoveY;  //top需要加上偏移Y,注意处理箭头的高度和偏移Y

                    // 如果提示图片超出屏幕右边界
                    if (left + tipBitmap.getWidth() > screenW - contentOffestMargin) {
                        left = screenW - tipBitmap.getWidth() - contentOffestMargin;
                    }
                    int tipLeft = left + tipViewMoveX;

                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            } else {// 中
                if (showArrow && arrowBitmap != null) {
                    canvas.drawBitmap(arrowBitmap, left + vWidth / 2 - arrowBitmap.getWidth() / 2 + arrowMoveX, jtTop + arrowMoveY, null);
                }

                //中间就默认居中展示
                if (tipBitmap != null) {
                    int tipTop = showArrow && arrowBitmap != null ?
                            jtTop + arrowBitmap.getHeight() + tipViewMoveY + arrowMoveY :
                            jtTop + tipViewMoveY;

                    int tipLeft = left + vWidth / 2 - tipBitmap.getWidth() / 2 + tipViewMoveX;

                    canvas.drawBitmap(tipBitmap, tipLeft + tipViewMoveX, tipTop + tipViewMoveY, null);

                }
            }
        } else { //屏幕下面是同样的逻辑

            int jtDownCenterTop = getDownFormTargetTop(arrowBitmap, top, vHeight);

            if (right <= screenW / 2) { // 左侧
                int jtTop = getDownFormTargetTop(arrowBitmap, top, vHeight);
                if (showArrow && arrowBitmap != null) {
                    canvas.drawBitmap(arrowBitmap, left + vWidth / 2 + arrowMoveX, jtTop + arrowMoveY, null);
                }
                if (tipBitmap != null) {
                    int tipTop = showArrow && arrowBitmap != null ?
                            jtTop - tipBitmap.getHeight() + tipViewMoveY :
                            top - tipBitmap.getHeight() - margin + tipViewMoveX;

                    // 如果提示图片超出屏幕左边界,不能超过左边界
                    if (left < contentOffestMargin) {
                        left = contentOffestMargin;
                    }
                    int tipLeft = left + tipViewMoveX;   //left需要需要加上偏移X

                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            } else if (left >= screenW / 2) {// 右侧

                int jtTop = getDownFormTargetTop(arrowBitmap, top, vHeight);
                if (showArrow && arrowBitmap != null) {
                    canvas.drawBitmap(arrowBitmap, left + arrowBitmap.getWidth() / 2 + arrowMoveX, jtTop + arrowMoveY, null);
                }

                if (tipBitmap != null) {

                    int tipTop = showArrow && arrowBitmap != null ?
                            jtTop - tipBitmap.getHeight() + tipViewMoveY :
                            top - tipBitmap.getHeight() - margin + tipViewMoveY;

                    // 如果提示图片超出屏幕右边界
                    if (left + tipBitmap.getWidth() > screenW - contentOffestMargin) {
                        left = screenW - tipBitmap.getWidth() - contentOffestMargin;
                    }
                    int tipLeft = left + tipViewMoveX;

                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            } else { // 中间
                if (showArrow && arrowBitmap != null) {
                    canvas.drawBitmap(arrowBitmap, left + (vWidth / 2 - arrowBitmap.getWidth() / 2) + arrowMoveX,
                            jtDownCenterTop + arrowMoveY, null);
                }
                if (tipBitmap != null) {
                    int tipLeft = left + contentOffestMargin + (vWidth / 2 - tipBitmap.getWidth() / 2) + tipViewMoveX;
                    int tipTop = showArrow && arrowBitmap != null ?
                            jtDownCenterTop - tipBitmap.getHeight() + tipViewMoveY :
                            top - tipBitmap.getHeight() - margin + tipViewMoveY;

                    canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);

                }
            }
        }

```

当我们设置为可展示箭头之后，此时的效果图如下：

### 三、加入区域矩阵，实现指定区域事件

那么我们是如何通过事件来触发下一步的呢？目前是通过整体的 View 的 onTouchEvent 的 ACTION_UP 中来触发的

```
typescript复制代码    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:

                check2NextGuide();
                return true;

            }
        }
        return true;
    }

```

那么产品现在提出一个需求，只有点击提示图片的 Next 按钮才能进行下一步，那么我们需要做的就是需要对指定的范围进行点击，而根据产品的需求，我们就需要对提示图片进行范围限定，我们用一个矩阵来记录。

```
scala复制代码public class UserGuideView extends View {

    private Rect tipViewHitRect;  //提示的区域矩阵
    private boolean showArrow = false; // 是否显示指示箭头
    private OnDismissListener onDismissListener;  //关闭GuideView的监听
    private boolean touchOutsideEffect = true;  //是否能触摸外部取消指引View
    ...

    @Override
    protected void onDraw(Canvas canvas) {

      ....

      canvas.drawBitmap(tipBitmap, tipLeft, tipTop, null);
      tipViewHitRect = new Rect(tipLeft, tipTop, tipLeft + tipBitmap.getWidth(), tipTop + tipBitmap.getHeight());
    }
}

```

我们在每一次绘制 tipBitmap 也就是提示图片的时候，我们把当前的提示图片大小矩阵记录下来，当点击的时候我们判断是否在这个范围内才能点击。

```
csharp复制代码    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                //点击外部也能触发指引View
                if (touchOutsideEffect) {
                    check2NextGuide();
                    return true;
                } else {
                    //点击外部不能触发指引，只能在绘制范围内点击才行
                    int touchX = (int) event.getX();
                    int touchY = (int) event.getY();
                    //点击区域是否在绘制范围内，否则不做处理
                    if (tipViewHitRect != null && tipViewHitRect.contains(touchX, touchY)) {
                        check2NextGuide();
                        return true;
                    }
                }
        }
        return true;
    }

```

同时在内部进行超过索引的隐藏与回调：

```
kotlin复制代码 //点击下一步去下一个指引,如果没有了则直接关闭，并回调
    private void check2NextGuide() {

        if (guideInfos == null || guideInfos.size() == 0) {
            this.setVisibility(View.GONE);
            if (this.onDismissListener != null) {
                onDismissListener.onDismiss(UserGuideView.this);
            }
        } else {
            //当有值的时候
            if (curIndedx >= guideInfos.size()) {
                this.setVisibility(View.GONE);
                if (this.onDismissListener != null) {
                    onDismissListener.onDismiss(UserGuideView.this);
                }
            } else {
                setNextTagetView(guideInfos.get(curIndedx));
                curIndedx++;
            }

        }
    }

```

以上我们就完成了一个简单的基于图片的用户指引控件了。

## 总结

剩下的完善细节，例如可以把蒙层前景的颜色值，是否启动外部触摸，是否展示箭头等变量设置到自定义属性中。

如果你们的产品还需要更加的细分的话，例如提示的图片 View 与下一步的图片 View 要分开，那么就需要你自己拆分，再定义一个对应的 NextView，与对应的图片绘制，相信各位看完此篇对你们来说不算难事了。

通过本文我们其实可以更加的理解绘制坐标系，图片的绘制无非就是top left的确定，限制矩阵也无非就是上下左右的赋值，理解了这些对于其他方式的绘制，如线条，圆形，文本等都能驾轻就熟了。之后再学(复)习我专栏的其他绘制文章[【传送门】](https://juejin.cn/column/7170970245723586574)。相信 View 的绘制这一块难不到你了。

View 的绘制到这里就告一段落，接下来我们还是回归相对复杂一些的 ViewGroup 定义，ViewGroup 的事件，滚动，协调，布局等知识点也会相对更多。后期我也会基于之前的 ViewGroup 文章进行一些补充。

当然如果你想查看源码或者做一些定制化的修改，点击查看源码[传送门](https://link.juejin.cn/?target=https%3A%2F%2Fgitee.com%2Fnewki123456%2FKotlin-Room)。同时，你也可以关注我的Kotlin项目，项目会持续更新。

惯例，我如有讲解不到位或错漏的地方，希望同学们可以指出交流哦！

如果感觉本文对你有一点点的启发，还望你能`点赞`支持一下,你的支持是我最大的动力啦！

Ok,这一期就此完结。

标签：

[Android](https://juejin.cn/tag/Android)[APP](https://juejin.cn/tag/APP)

话题：

[金石计划征文活动](https://juejin.cn/theme/detail/7218019389664067621?contentType=1)

本文收录于以下专栏

1 / 2

Android开发

专栏目录

Android开发相关

150 订阅

·

134 篇文章

上一篇

从Dart到Kotlin扩展函数的基本常规操作及应用场景

下一篇

Android自定义ViewGroup的滚动与惯性滚动效果实现的不同组合方式

评论 2

即可发布评论！

最热

最新

大佬，强🏻！！

5小时前

点赞

评论