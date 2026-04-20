# UI 绘制相关

- 简述一下 Android 中UI 的刷新机制？
    - 界面刷新的本质流程
        1. 通过 ViewRootlmpl 的scheduleTraversals进行界面刷新的三大流程。
    1. 调用到scheduleTraversals时不会立即执行，而是将该操作保存到待执行队列中；并给底层的刷新信
    号注册监听。
        1. 当VSYNC信号到来时，会从待执行队列中取出对应的scheduleTraversals操作，并将其加入到主线程的消息队列中。
        2. 主线程从消息队列中取出并执行三大流程：onMeasure-onLayout-onDraw
    - 同步屏障的作用
        1. 同步屏障用于阻塞住所有的同步消息（底层VSYNC的回调onVsync方法提交的消息是异步消息）
        2. 用于保证界面刷新功能的performTraversal的优先执行。
    - 同步屏障的原理
        1. 主线程的Looper会一直循环调用MessageQueue的next方法并且取出队列头部的Message执行，遇到同步屏障（一种特殊消息）后会去寻找异步消息执行；如果没有找到异步消息就会一直阻塞下去，除非将同步屏障取出，否则永远不会执行同步消息。
        2. 界面刷新操作是异步消息，具有最高优先级
- 谈一谈获取View宽高的几种方法？
    1. OnGlobalLayoutListener获取
    2. OnPreDrawListener获取
    3. OnLayoutChangeListener获取
    4. 重写View的onSizeChanged0
    5. 使用View.post0方法
    
- **requestLayout和invalidate区别**
    
    ![Untitled](assets/UI 绘制相关 c3329706dec24071976c8eb57b6fd6ba/Untitled.png)
    
    1. requestLayout：
        
        requestLayout会直接递归调用父窗口的requestLayout，直到ViewRootImpl,然后触发peformTraversals，
        
        由于mLayoutRequested为true，会导致onMeasure和onLayout被调用。不一定会触发OnDraw。requestLayout触发onDraw可能是因为在在layout过程中发现l,t,r,b和以前不一样，那就会触发一次invalidate，所以触发了onDraw，也可能是因为别的原因导致mDirty非空（比如在跑动画）
        
    2. invalidate：
        
        view的invalidate不会导致ViewRootImpl的invalidate被调用，而是递归调用父view的invalidateChildInParent，直到ViewRootImpl的invalidateChildInParent，然后触发peformTraversals，
        
        会导致当前view被重绘,由于mLayoutRequested为false，不会导致onMeasure和onLayout被调用，
        
        而OnDraw会被调用
        
- 3.7 针对RecyclerView你做了哪些优化？
- 3.9 谈谈自定义LayoutManager的流程？
    1. 确定ltemView的LayoutParams，generateDefaultLayoutParams
    2. 确定所有ItemView在recyclerview的位置，并且回收和复用itemview
    onLayoutChildren
    3. 添加滑动canScrollVertically
- 

3.12 谈一谈插值器和估值器：
1、插值器，根据时间（动画时常）流逝的百分比来计算属
性变化的百分比。系统默认的有匀速，加减速，减速插值器。
2、估值器，通过上面插值器得到的百分比计算出具体变化
國票元X有理，淨点四，颜色估值票
3、自定义只需要重写他们的evaluate方法就可以了。

[Android面试官爱问的12个自定义View的问题](UI%20%E7%BB%98%E5%88%B6%E7%9B%B8%E5%85%B3/Android%E9%9D%A2%E8%AF%95%E5%AE%98%E7%88%B1%E9%97%AE%E7%9A%8412%E4%B8%AA%E8%87%AA%E5%AE%9A%E4%B9%89View%E7%9A%84%E9%97%AE%E9%A2%98%20fbdd6e0860674a4fbc3216f89f26134a.md)

[AndroidUI](UI%20%E7%BB%98%E5%88%B6%E7%9B%B8%E5%85%B3/AndroidUI%205396961aea084c0ca34c7397c915fc5e.md)

- view 的 post

**Android View的绘制流程**

[https://www.jianshu.com/p/5a71014e7b1b](https://www.jianshu.com/p/5a71014e7b1b)

[RecyclerView](UI%20%E7%BB%98%E5%88%B6%E7%9B%B8%E5%85%B3/RecyclerView%20186a12e5c6304d808c314f9a377da59b.md)

[ConstraintLayout](UI%20%E7%BB%98%E5%88%B6%E7%9B%B8%E5%85%B3/ConstraintLayout%2016c309d103f5412f900d97e75b89efcb.md)

- ViewRootImpl 是在哪里被创建的?
    
    答：在执行 Activity的 onResume 方法的之后，再之后才调用 ViewRootImpl 的 requestLayout。
    
- requestWindowFeature为什么要写在setContentView的前面？
    
    因为一个设置mLocalFeatures变量，另一个是读取mLocalFeatures变量。
    
    - 先设置好mLocalFeatures
        
        ```kotlin
        public final boolean requestWindowFeature(int featureId) {
            return getWindow().requestFeature(featureId);
        }
        ```
        
        ```kotlin
        public boolean requestFeature(int featureId) {
            final int flag = 1<<featureId;
            mFeatures |= flag;
            mLocalFeatures |= mContainer != null ? (flag&~mContainer.mFeatures) : flag;
            return (mFeatures&flag) != 0;
        }
        ```
        
    - 然后读取mLocalFeatures
        
        ```kotlin
        public void setContentView(@LayoutRes int layoutResID) {
            getWindow().setContentView(layoutResID);
            initWindowDecorActionBar();
        }
        ```
        
        ```kotlin
        @Override
        public void setContentView(int layoutResID) {
            // Note: FEATURE_CONTENT_TRANSITIONS may be set in the process of installing the window
            // decor, when theme attributes and the like are crystalized. Do not check the feature
            // before this happens.
            if (mContentParent == null) {
                installDecor();
            } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
                mContentParent.removeAllViews();
            }
        }
        ```
        
        ```kotlin
        private void installDecor() {
        	  mForceDecorInstall = false;
        	  if (mDecor == null) {
        	      mDecor = generateDecor(-1);
        	      mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        	      mDecor.setIsRootNamespace(true);
        	      if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
        	          mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
        	      }
        	  } else {
        	      mDecor.setWindow(this);
        	  }
        	  if (mContentParent == null) {
        	      mContentParent = generateLayout(mDecor);
        		}
        }
        ```
        
        ```kotlin
        protected ViewGroup generateLayout(DecorView decor) {
        		int layoutResource;
            int features = getLocalFeatures();
        }
        ```
        
        ```kotlin
        protected final int getLocalFeatures() {
            return mLocalFeatures;
        }
        ```
        
- Activity、Window 和 DecorView 的关系
    
    ![Untitled](assets/UI 绘制相关 c3329706dec24071976c8eb57b6fd6ba/Untitled 1.png)
    
    - 1、绑定 PhoneWindow 到 Activity 上
        
        ```kotlin
        private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
                ActivityInfo aInfo = r.activityInfo;
                if (r.packageInfo == null) {
                    r.packageInfo = getPackageInfo(aInfo.applicationInfo, mCompatibilityInfo,
                            Context.CONTEXT_INCLUDE_CODE);
                }
        
                ComponentName component = r.intent.getComponent();
                if (component == null) {
                    component = r.intent.resolveActivity(
                        mInitialApplication.getPackageManager());
                    r.intent.setComponent(component);
                }
        
                if (r.activityInfo.targetActivity != null) {
                    component = new ComponentName(r.activityInfo.packageName,
                            r.activityInfo.targetActivity);
                }
        
                ContextImpl appContext = createBaseContextForActivity(r);
                Activity activity = null;
                try {
                    java.lang.ClassLoader cl = appContext.getClassLoader();
                    activity = mInstrumentation.newActivity(
                            cl, component.getClassName(), r.intent);
                    StrictMode.incrementExpectedActivityCount(activity.getClass());
                    r.intent.setExtrasClassLoader(cl);
                    r.intent.prepareToEnterProcess(isProtectedComponent(r.activityInfo),
                            appContext.getAttributionSource());
                    if (r.state != null) {
                        r.state.setClassLoader(cl);
                    }
                } catch (Exception e) {
                    if (!mInstrumentation.onException(activity, e)) {
                        throw new RuntimeException(
                            "Unable to instantiate activity " + component
                            + ": " + e.toString(), e);
                    }
                }
        
                try {
                    Application app = r.packageInfo.makeApplicationInner(false, mInstrumentation);
        
                    if (localLOGV) Slog.v(TAG, "Performing launch of " + r);
                    if (localLOGV) Slog.v(
                            TAG, r + ": app=" + app
                            + ", appName=" + app.getPackageName()
                            + ", pkg=" + r.packageInfo.getPackageName()
                            + ", comp=" + r.intent.getComponent().toShortString()
                            + ", dir=" + r.packageInfo.getAppDir());
        
                    // updatePendingActivityConfiguration() reads from mActivities to update
                    // ActivityClientRecord which runs in a different thread. Protect modifications to
                    // mActivities to avoid race.
                    synchronized (mResourcesManager) {
                        mActivities.put(r.token, r);
                    }
        
                    if (activity != null) {
                        CharSequence title = r.activityInfo.loadLabel(appContext.getPackageManager());
                        Configuration config =
                                new Configuration(mConfigurationController.getCompatConfiguration());
                        if (r.overrideConfig != null) {
                            config.updateFrom(r.overrideConfig);
                        }
                        if (DEBUG_CONFIGURATION) Slog.v(TAG, "Launching activity "
                                + r.activityInfo.name + " with config " + config);
                        Window window = null;
                        if (r.mPendingRemoveWindow != null && r.mPreserveWindow) {
                            window = r.mPendingRemoveWindow;
                            r.mPendingRemoveWindow = null;
                            r.mPendingRemoveWindowManager = null;
                        }
        
                        // Activity resources must be initialized with the same loaders as the
                        // application context.
                        appContext.getResources().addLoaders(
                                app.getResources().getLoaders().toArray(new ResourcesLoader[0]));
        
                        appContext.setOuterContext(activity);
                        activity.attach(appContext, this, getInstrumentation(), r.token,
                                r.ident, app, r.intent, r.activityInfo, title, r.parent,
                                r.embeddedID, r.lastNonConfigurationInstances, config,
                                r.referrer, r.voiceInteractor, window, r.activityConfigCallback,
                                r.assistToken, r.shareableActivityToken);
        
                        if (customIntent != null) {
                            activity.mIntent = customIntent;
                        }
                        r.lastNonConfigurationInstances = null;
                        checkAndBlockForNetworkAccess();
                        activity.mStartedActivity = false;
                        int theme = r.activityInfo.getThemeResource();
                        if (theme != 0) {
                            activity.setTheme(theme);
                        }
        
                        if (r.mActivityOptions != null) {
                            activity.mPendingOptions = r.mActivityOptions;
                            r.mActivityOptions = null;
                        }
                        activity.mLaunchedFromBubble = r.mLaunchedFromBubble;
                        activity.mCalled = false;
                        // Assigning the activity to the record before calling onCreate() allows
                        // ActivityThread#getActivity() lookup for the callbacks triggered from
                        // ActivityLifecycleCallbacks#onActivityCreated() or
                        // ActivityLifecycleCallback#onActivityPostCreated().
                        r.activity = activity;
                        if (r.isPersistable()) {
                            mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
                        } else {
                            mInstrumentation.callActivityOnCreate(activity, r.state);
                        }
                        if (!activity.mCalled) {
                            throw new SuperNotCalledException(
                                "Activity " + r.intent.getComponent().toShortString() +
                                " did not call through to super.onCreate()");
                        }
                        r.mLastReportedWindowingMode = config.windowConfiguration.getWindowingMode();
                    }
                    r.setState(ON_CREATE);
        
                } catch (SuperNotCalledException e) {
                    throw e;
        
                } catch (Exception e) {
                    if (!mInstrumentation.onException(activity, e)) {
                        throw new RuntimeException(
                            "Unable to start activity " + component
                            + ": " + e.toString(), e);
                    }
                }
        
                return activity;
            }
        ```
        
    - 2、将 DecorView 添加到 Window 上
        
        ```kotlin
        @Override
        public void handleResumeActivity(ActivityClientRecord r, boolean finalStateRequest,
                boolean isForward, boolean shouldSendCompatFakeFocus, String reason) {
            // If we are getting ready to gc after going to the background, well
            // we are back active so skip it.
            unscheduleGcIdler();
            mSomeActivitiesChanged = true;
        
            // TODO Push resumeArgs into the activity for consideration
            // skip below steps for double-resume and r.mFinish = true case.
        		//注释 1：执行 Activity的 onResume 方法
            if (!performResumeActivity(r, finalStateRequest, reason)) {
                return;
            }
        		final Activity a = r.activity;
        		if (r.window == null && !a.mFinished && willBeVisible) {
                r.window = r.activity.getWindow();
                View decor = r.window.getDecorView();
                decor.setVisibility(View.INVISIBLE);
                ViewManager wm = a.getWindowManager();
                WindowManager.LayoutParams l = r.window.getAttributes();
                a.mDecor = decor;
                l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
                l.softInputMode |= forwardBit;
                if (r.mPreserveWindow) {
                    a.mWindowAdded = true;
                    r.mPreserveWindow = false;
                    // Normally the ViewRoot sets up callbacks with the Activity
                    // in addView->ViewRootImpl#setView. If we are instead reusing
                    // the decor view we have to notify the view root that the
                    // callbacks may have changed.
                    ViewRootImpl impl = decor.getViewRootImpl();
                    if (impl != null) {
                        impl.notifyChildRebuilt();
                    }
                }
                if (a.mVisibleFromClient) {
                    if (!a.mWindowAdded) {
                        a.mWindowAdded = true;
        								//注释 2：执行 new ViewRootImpl
                        wm.addView(decor, l);
                    } else {
                        // The activity will get a callback for this {@link LayoutParams} change
                        // earlier. However, at that time the decor will not be set (this is set
                        // in this method), so no action will be taken. This call ensures the
                        // callback occurs with the decor set.
                        a.onWindowAttributesChanged(l);
                    }
                }
        
                // If the window has already been added, but during resume
                // we started another activity, then don't yet make the
                // window visible.
            } else if (!willBeVisible) {
                if (localLOGV) Slog.v(TAG, "Launch " + r + " mStartedActivity set");
                r.hideForNow = true;
            }
        }
        ```
        
    - 3、创建 ViewRootImpl 实例
        
        ```kotlin
        private final WindowManagerGlobal mGlobal = WindowManagerGlobal.getInstance();
        
        @Override
        public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
            applyTokens(params);
            mGlobal.addView(view, params, mContext.getDisplayNoVerify(), mParentWindow,
                    mContext.getUserId());
        }
        ```
        
        ```kotlin
        @UnsupportedAppUsage
        private final ArrayList<View> mViews = new ArrayList<View>();
        @UnsupportedAppUsage
        private final ArrayList<ViewRootImpl> mRoots = new ArrayList<ViewRootImpl>();
        @UnsupportedAppUsage
        private final ArrayList<WindowManager.LayoutParams> mParams =
                new ArrayList<WindowManager.LayoutParams>();
        
        public void addView(View view, ViewGroup.LayoutParams params,
                    Display display, Window parentWindow, int userId) {
        		IWindowSession windowlessSession = null;
            // If there is a parent set, but we can't find it, it may be coming
            // from a SurfaceControlViewHost hierarchy.
            if (wparams.token != null && panelParentView == null) {
                for (int i = 0; i < mWindowlessRoots.size(); i++) {
                    ViewRootImpl maybeParent = mWindowlessRoots.get(i);
                    if (maybeParent.getWindowToken() == wparams.token) {
                        windowlessSession = maybeParent.getWindowSession();
                        break;
                    }
                }
            }
        		if (windowlessSession == null) {
                root = new ViewRootImpl(view.getContext(), display);
            } else {
                root = new ViewRootImpl(view.getContext(), display,
                        windowlessSession, new WindowlessWindowLayout());
            }
        		mViews.add(view);
            mRoots.add(root);
            mParams.add(wparams);
        
            // do this last because it fires off messages to start doing things
            try {
                root.setView(view, wparams, panelParentView, userId);
            } catch (RuntimeException e) {
                final int viewIndex = (index >= 0) ? index : (mViews.size() - 1);
                // BadTokenException or InvalidDisplayException, clean up.
                if (viewIndex >= 0) {
                    removeViewLocked(viewIndex, true);
                }
                throw e;
            }
        }
        
        ```
        
    - 4、将 ViewRootImpl 设置为 DecorView 的 parent
        
        ```kotlin
        public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView,
                    int userId) {
        		synchronized (this) {
                if (mView == null) {
                    mView = view;
        						// Schedule the first layout -before- adding to the window
                    // manager, to make sure we do the relayout before receiving
                    // any other events from the system.
                    //View 第一次执行测量和绘制
        						requestLayout();
        						//将 ViewRoot 设置为 DecorView 的 parent
        						view.assignParent(this);
        				}
        		}
        }
        
        ```
        
- View 绘制流程源码解析，子线程可以更新UI吗？
    - 1、就是要绕开 ViewRootImpl 的checkThread 方法
        
        ```kotlin
        @Override
        public void requestLayout() {
            if (!mHandlingLayoutInLayoutRequest) {
                checkThread();
                mLayoutRequested = true;
                scheduleTraversals();
            }
        }
        ```
        
        ```kotlin
        void checkThread() {
            Thread current = Thread.currentThread();
            if (mThread != current) {
                throw new CalledFromWrongThreadException(
                        "Only the original thread that created a view hierarchy can touch its views."
                                + " Expected: " + mThread.getName()
                                + " Calling: " + current.getName());
            }
        }
        ```
        
    - 2、View 和 ViewRootImpl 是什么关系
        
        View树的顶层是 DecorView，DecorView 的 parent 是 ViewRootImpl
        
        ![Untitled](assets/UI 绘制相关 c3329706dec24071976c8eb57b6fd6ba/Untitled 2.png)
        
    - 3、ViewRootImpl 的 performTraversals 的执行过程
        
        ```kotlin
        @Override
        public void requestLayout() {
            if (!mHandlingLayoutInLayoutRequest) {
                checkThread();
                mLayoutRequested = true;
                scheduleTraversals();
            }
        }
        ```
        
        ```kotlin
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        void scheduleTraversals() {
            if (!mTraversalScheduled) {
                mTraversalScheduled = true;
                mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
                mChoreographer.postCallback(
                        Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
                notifyRendererOfFramePending();
                pokeDrawLockIfNeeded();
            }
        }
        ```
        
        ```kotlin
        final class TraversalRunnable implements Runnable {
            @Override
            public void run() {
                doTraversal();
            }
        }
        ```
        
        ![Untitled](assets/UI 绘制相关 c3329706dec24071976c8eb57b6fd6ba/Untitled 3.png)
        
    - 4、什么情况子线程可以更新UI
        - (1)已经正在处理 requestLayout任务了，再次触发就不会被执行了。
            
            ![Untitled](assets/UI 绘制相关 c3329706dec24071976c8eb57b6fd6ba/Untitled 4.png)
            
        - 2、防止重绘的机制，并不是只有 ViewRootImpl 在做
            
            ![Untitled](assets/UI 绘制相关 c3329706dec24071976c8eb57b6fd6ba/Untitled 5.png)
            
            - 开始布局，mPrivateFlags设置PFLAG_FORCE_LAYOUT
                
                ```kotlin
                @CallSuper
                public void requestLayout() {
                    if (isRelayoutTracingEnabled()) {
                        Trace.instantForTrack(TRACE_TAG_APP, "requestLayoutTracing",
                                mTracingStrings.classSimpleName);
                        printStackStrace(mTracingStrings.requestLayoutStacktracePrefix);
                    }
                
                    if (mMeasureCache != null) mMeasureCache.clear();
                
                    if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == null) {
                        // Only trigger request-during-layout logic if this is the view requesting it,
                        // not the views in its parent hierarchy
                        ViewRootImpl viewRoot = getViewRootImpl();
                        if (viewRoot != null && viewRoot.isInLayout()) {
                            if (!viewRoot.requestLayoutDuringLayout(this)) {
                                return;
                            }
                        }
                        mAttachInfo.mViewRequestingLayout = this;
                    }
                
                    mPrivateFlags |= PFLAG_FORCE_LAYOUT;
                    mPrivateFlags |= PFLAG_INVALIDATED;
                
                    if (mParent != null && !mParent.isLayoutRequested()) {
                        mParent.requestLayout();
                    }
                    if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == this) {
                        mAttachInfo.mViewRequestingLayout = null;
                    }
                }
                ```
                
            - 完成布局，mPrivateFlags取消设置PFLAG_FORCE_LAYOUT
                
                ```kotlin
                public void layout(int l, int t, int r, int b) {
                    if ((mPrivateFlags3 & PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT) != 0) {
                        if (isTraversalTracingEnabled()) {
                            Trace.beginSection(mTracingStrings.onMeasureBeforeLayout);
                        }
                        onMeasure(mOldWidthMeasureSpec, mOldHeightMeasureSpec);
                        if (isTraversalTracingEnabled()) {
                            Trace.endSection();
                        }
                        mPrivateFlags3 &= ~PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
                    }
                
                    int oldL = mLeft;
                    int oldT = mTop;
                    int oldB = mBottom;
                    int oldR = mRight;
                
                    boolean changed = isLayoutModeOptical(mParent) ?
                            setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);
                
                    if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
                        if (isTraversalTracingEnabled()) {
                            Trace.beginSection(mTracingStrings.onLayout);
                        }
                        onLayout(changed, l, t, r, b);
                        if (isTraversalTracingEnabled()) {
                            Trace.endSection();
                        }
                
                        if (shouldDrawRoundScrollbar()) {
                            if(mRoundScrollbarRenderer == null) {
                                mRoundScrollbarRenderer = new RoundScrollbarRenderer(this);
                            }
                        } else {
                            mRoundScrollbarRenderer = null;
                        }
                
                        mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;
                
                        ListenerInfo li = mListenerInfo;
                        if (li != null && li.mOnLayoutChangeListeners != null) {
                            ArrayList<OnLayoutChangeListener> listenersCopy =
                                    (ArrayList<OnLayoutChangeListener>)li.mOnLayoutChangeListeners.clone();
                            int numListeners = listenersCopy.size();
                            for (int i = 0; i < numListeners; ++i) {
                                listenersCopy.get(i).onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB);
                            }
                        }
                    }
                
                    final boolean wasLayoutValid = isLayoutValid();
                
                    mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
                    mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;
                
                    if (!wasLayoutValid && isFocused()) {
                        mPrivateFlags &= ~PFLAG_WANTS_FOCUS;
                        if (canTakeFocus()) {
                            // We have a robust focus, so parents should no longer be wanting focus.
                            clearParentsWantFocus();
                        } else if (getViewRootImpl() == null || !getViewRootImpl().isInLayout()) {
                            // This is a weird case. Most-likely the user, rather than ViewRootImpl, called
                            // layout. In this case, there's no guarantee that parent layouts will be evaluated
                            // and thus the safest action is to clear focus here.
                            clearFocusInternal(null, /* propagate */ true, /* refocus */ false);
                            clearParentsWantFocus();
                        } else if (!hasParentWantsFocus()) {
                            // original requestFocus was likely on this view directly, so just clear focus
                            clearFocusInternal(null, /* propagate */ true, /* refocus */ false);
                        }
                        // otherwise, we let parents handle re-assigning focus during their layout passes.
                    } else if ((mPrivateFlags & PFLAG_WANTS_FOCUS) != 0) {
                        mPrivateFlags &= ~PFLAG_WANTS_FOCUS;
                        View focused = findFocus();
                        if (focused != null) {
                            // Try to restore focus as close as possible to our starting focus.
                            if (!restoreDefaultFocus() && !hasParentWantsFocus()) {
                                // Give up and clear focus once we've reached the top-most parent which wants
                                // focus.
                                focused.clearFocusInternal(null, /* propagate */ true, /* refocus */ false);
                            }
                        }
                    }
                
                    if ((mPrivateFlags3 & PFLAG3_NOTIFY_AUTOFILL_ENTER_ON_LAYOUT) != 0) {
                        mPrivateFlags3 &= ~PFLAG3_NOTIFY_AUTOFILL_ENTER_ON_LAYOUT;
                        notifyEnterOrExitForAutoFillIfNeeded(true);
                    }
                
                    notifyAppearedOrDisappearedForContentCaptureIfNeeded(true);
                }
                ```
                
            - 再次调用requestLayout 就不会设置PFLAG_FORCE_LAYOUT 了
                
                ```kotlin
                if (mParent != null && !mParent.isLayoutRequested()) {
                    mParent.requestLayout();
                }
                ```
                
                所以第二次在子线程执行 textView.requestLayout 不会调用，ViewRootImpl的requestLayout
                
        
        ![Untitled](assets/UI 绘制相关 c3329706dec24071976c8eb57b6fd6ba/Untitled 6.png)