# App 启动(基于 Android 8.0)

- 总体流程说明
    
    ![Untitled](App%20%E5%90%AF%E5%8A%A8(%E5%9F%BA%E4%BA%8E%20Android%208%200)/Untitled.png)
    
    1. 首先 Launcher 进程向 AMS 请求创建App根 Activity
    2. AMS 会判断App根 Activity 所需要的应用程序进程是否存在并启动
    3. 应用程序进程启动后，AMS 会请求创建根 Activity
    
    <aside>
    💡 步骤 1 和 4 采用的是 Binder 通信，步骤 2 采用的是 Socket 通信。
    
    </aside>
    
    <aside>
    💡 如果是普通 Activity 启动会涉及几个进程呢？答案是两个，AMS所在进程和应用程序进程。
    
    </aside>
    
- 1、Launcher 请求AMS过程（使用 AIDL通信）
    
    当我们点击某个应用程序的快捷键图标时，就会通过 Launcher 请求 AMS来启动该应用程序。时序图如下：
    
    ![Untitled](App%20%E5%90%AF%E5%8A%A8(%E5%9F%BA%E4%BA%8E%20Android%208%200)/Untitled%201.png)
    
    ```kotlin
    @UnsupportedAppUsage
    public static IActivityManager getService() {
        return IActivityManagerSingleton.get();
    }
    
    @UnsupportedAppUsage
    private static final Singleton<IActivityManager> IActivityManagerSingleton =
            new Singleton<IActivityManager>() {
                @Override
                protected IActivityManager create() {
    								//注释 1
                    final IBinder b = ServiceManager.getService(Context.ACTIVITY_SERVICE);
    								//注释 2
                    final IActivityManager am = IActivityManager.Stub.asInterface(b);
                    return am;
                }
            };
    ```
    
    <aside>
    💡 getService方法调用了 IActivityManagerSingleton 的 get 方法，我们接着往下看，IActivityManagerSingleton 是一个 Singleton 类。
    
    在注释 1 得到名为“activity”的 Service 类型的对象，也就是 IBinder 类型的 AMS 的引用。
    
    接着在注释 2 将它转换成 IActivityManager类型的对象，这段代码采用的是AIDL，IActivityManager.java 类是由 AIDL 工具在编译时自动生成的，IActivityManager.aidl 的文件路径为 frameworks/base/core/java/android/app/IActivityManager.aidl。
    
    要实现进程间通信，服务端也就是 AMS只需要继承 IActivityManager.Stub 类并实现相应的方法就可以了。
    
    </aside>
    
- 2、AMS 发送启动应用程序进程请求（使用Socket 通信）
    
    要想启动一个应用程序，首先要保证这个应用程序所需要的应用程序进程已经启动。AMS在启动应用程序时会检查这个应用程序需要的应用程序进程是否存在，不存在就会请求 Zygote 进程启动需要的应用程序进程。
    
    ![Untitled](App%20%E5%90%AF%E5%8A%A8(%E5%9F%BA%E4%BA%8E%20Android%208%200)/Untitled%202.png)
    
    ```kotlin
    private ZygoteState openZygoteSocketIfNeeded(String abi) throws ZygoteStartFailedEx {
        try {
            attemptConnectionToPrimaryZygote();
    				//注释 2 ：连接Zygote 主模式返回的 ZygoteState 是否与启动应用程序进程所需要的 ABI 匹配
            if (primaryZygoteState.matches(abi)) {
                return primaryZygoteState;
            }
    			
            if (mZygoteSecondarySocketAddress != null) {
                //注释 3 ：如果不匹配，则尝试连接 Zygote 辅模式
                attemptConnectionToSecondaryZygote();
    
                if (secondaryZygoteState.matches(abi)) {
                    return secondaryZygoteState;
                }
            }
        } catch (IOException ioe) {
            throw new ZygoteStartFailedEx("Error connecting to zygote", ioe);
        }
    
        throw new ZygoteStartFailedEx("Unsupported zygote ABI: " + abi);
    }
    
    private void attemptConnectionToPrimaryZygote() throws IOException {
        if (primaryZygoteState == null || primaryZygoteState.isClosed()) {
    				//注释 1 ：与 Zygote 进程建立 Socket 连接
            primaryZygoteState = ZygoteState.connect(mZygoteSocketAddress, mUsapPoolSocketAddress);
    
            maybeSetApiDenylistExemptions(primaryZygoteState, false);
            maybeSetHiddenApiAccessLogSampleRate(primaryZygoteState);
        }
    }
    
    private void attemptConnectionToSecondaryZygote() throws IOException {
        if (secondaryZygoteState == null || secondaryZygoteState.isClosed()) {
            secondaryZygoteState =
                    ZygoteState.connect(mZygoteSecondarySocketAddress,
                            mUsapPoolSecondarySocketAddress);
    
            maybeSetApiDenylistExemptions(secondaryZygoteState, false);
            maybeSetHiddenApiAccessLogSampleRate(secondaryZygoteState);
        }
    }
    ```
    
    <aside>
    💡 在 Zygote 的 main 方法中已经创建 name 为 zygote 的 Server 端 Socket。
    
    </aside>
    
    在注释 1 处与 Zygote 进程建立 Socket 连接，并返回 ZygoteState 类型的primaryZygoteState 对象。
    在注释 2 处如果 primaryZygoteState 与启动应用程序进程所需要 ABI 不匹配，则会在注释 3 处连接 name 为 zygote_secondary 的 Socket。
    
- 3、Zygote 接收请求并创建应用程序进程
    
    ![Untitled](App%20%E5%90%AF%E5%8A%A8(%E5%9F%BA%E4%BA%8E%20Android%208%200)/Untitled%203.png)
    
    <aside>
    💡 Socket 连接成功，后返回 ZygoteState 类型对象，会将的启动参数 argsForZygote 写入 ZygoteState 中，这样 Zygote 进程就会收到一个创建新的应用程序进程的请求。
    
    </aside>
    
    ```kotlin
    public class RuntimeInit {
    	protected static Runnable applicationInit(int targetSdkVersion, long[] disabledCompatChanges,
    	            String[] argv, ClassLoader classLoader) {
    	    // If the application calls System.exit(), terminate the process
    	    // immediately without running any shutdown hooks.  It is not possible to
    	    // shutdown an Android application gracefully.  Among other things, the
    	    // Android runtime shutdown hooks close the Binder driver, which can cause
    	    // leftover running threads to crash before the process actually exits.
    	    nativeSetExitWithoutCleanup(true);
    	
    	    VMRuntime.getRuntime().setTargetSdkVersion(targetSdkVersion);
    	    VMRuntime.getRuntime().setDisabledCompatChanges(disabledCompatChanges);
    	
    	    final Arguments args = new Arguments(argv);
    	
    	    // The end of of the RuntimeInit event (see #zygoteInit).
    	    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
    	
    	    // Remaining arguments are passed to the start class's static main
    	    return findStaticMain(args.startClass, args.startArgs, classLoader);
    	}
    	
    	protected static Runnable findStaticMain(String className, String[] argv,
    	        ClassLoader classLoader) {
    	    Class<?> cl;
    	
    	    try {
    	        cl = Class.forName(className, true, classLoader);
    	    } catch (ClassNotFoundException ex) {
    	        throw new RuntimeException(
    	                "Missing class when invoking static main " + className,
    	                ex);
    	    }
    	
    	    Method m;
    	    try {
    	        m = cl.getMethod("main", new Class[] { String[].class });
    	    } catch (NoSuchMethodException ex) {
    	        throw new RuntimeException(
    	                "Missing static main on " + className, ex);
    	    } catch (SecurityException ex) {
    	        throw new RuntimeException(
    	                "Problem getting static main on " + className, ex);
    	    }
    	
    	    int modifiers = m.getModifiers();
    	    if (! (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))) {
    	        throw new RuntimeException(
    	                "Main method is not public and static on " + className);
    	    }
    	
    	    /*
    	     * This throw gets caught in ZygoteInit.main(), which responds
    	     * by invoking the exception's run() method. This arrangement
    	     * clears up all the stack frames that were required in setting
    	     * up the process.
    	     */
    	    return new MethodAndArgsCaller(m, argv);
    	}
    
    	static class MethodAndArgsCaller implements Runnable {
    	    /** method to call */
    	    private final Method mMethod;
    	
    	    /** argument array */
    	    private final String[] mArgs;
    	
    	    public MethodAndArgsCaller(Method method, String[] args) {
    	        mMethod = method;
    	        mArgs = args;
    	    }
    	
    	    public void run() {
    	        try {
    							//注释 1
    	            mMethod.invoke(null, new Object[] { mArgs });
    	        } catch (IllegalAccessException ex) {
    	            throw new RuntimeException(ex);
    	        } catch (InvocationTargetException ex) {
    	            Throwable cause = ex.getCause();
    	            if (cause instanceof RuntimeException) {
    	                throw (RuntimeException) cause;
    	            } else if (cause instanceof Error) {
    	                throw (Error) cause;
    	            }
    	            throw new RuntimeException(ex);
    	        }
    	    }
    	}
    }
    ```
    
     注释 1 的 mMethod 指的就是 ActivityThread 的 main 方法，调用了 mMethod 的 invoke 方法后，ActivityThread main 方法就会被动态调用，应用程序进程就进入了 ActivityThread 的 main 方法中。到这里，应用程序进程就创建完成了并且运行了主线程的管理类 ActivityThread。
    
- 4、AMS到 ApplicationThread 的调用过程
    
    ![Untitled](App%20%E5%90%AF%E5%8A%A8(%E5%9F%BA%E4%BA%8E%20Android%208%200)/Untitled%204.png)
    
    ```kotlin
    final boolean realStartActivityLocked(ActivityRecord r, ProcessRecord app,
                boolean andResume, boolean checkConfig) throws RemoteException {
    	app.thread.scheduleLaunchActivity(new Intent(r.intent), r.appToken,
                        System.identityHashCode(r), r.info,
                        // TODO: Have this take the merged configuration instead of separate global and
                        // override configs.
                        mergedConfiguration.getGlobalConfiguration(),
                        mergedConfiguration.getOverrideConfiguration(), r.compat,
                        r.launchedFromPackage, task.voiceInteractor, app.repProcState, r.icicle,
                        r.persistentState, results, newIntents, !andResume,
                        mService.isNextTransitionForward(), profilerInfo);
    }
    ```
    
    这里的 app.thread 指的是 IApplicationThread，它的实现类时 ActivityThread 的内部类 ApplicationThread，其中 ApplicationThread 继承了 IApplicationThread.Stub。app 指的是要启动的 Activity 所在的进程，因此，这段代码就是要在目标应用程序进程启动 Activity。当前代码逻辑运行在 AMS 所在的system_server 进程中，通过 ApplicationThread 来与应用程序进程进行 Binder 通信，换句话说，ApplicationThread 是 AMS 所在进程的 system_server 进程和应用程序进程的通信桥梁。
    
    ![Untitled](App%20%E5%90%AF%E5%8A%A8(%E5%9F%BA%E4%BA%8E%20Android%208%200)/Untitled%205.png)
    
- 5、ActivityThread 启动 Activity 的过程
    
    接着查看 ApplicationThread 的 scheduleLaunchActivity 方法，其中 ApplicationThread 是 ActivityThread 的内部类，它管理着当前应用程序创建的主线程。
    
    ![Untitled](App%20%E5%90%AF%E5%8A%A8(%E5%9F%BA%E4%BA%8E%20Android%208%200)/Untitled%206.png)
    
    ApplicationThread 的 scheduleLaunchActivity 方法如下：
    
    ```kotlin
    @Override
    public final void scheduleLaunchActivity(Intent intent, IBinder token, int ident,
            ActivityInfo info, Configuration curConfig, Configuration overrideConfig,
            CompatibilityInfo compatInfo, String referrer, IVoiceInteractor voiceInteractor,
            int procState, Bundle state, PersistableBundle persistentState,
            List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents,
            boolean notResumed, boolean isForward, ProfilerInfo profilerInfo) {
    
        updateProcessState(procState, false);
    
        ActivityClientRecord r = new ActivityClientRecord();
    
        r.token = token;
        r.ident = ident;
        r.intent = intent;
        r.referrer = referrer;
        r.voiceInteractor = voiceInteractor;
        r.activityInfo = info;
        r.compatInfo = compatInfo;
        r.state = state;
        r.persistentState = persistentState;
    
        r.pendingResults = pendingResults;
        r.pendingIntents = pendingNewIntents;
    
        r.startsNotResumed = notResumed;
        r.isForward = isForward;
    
        r.profilerInfo = profilerInfo;
    
        r.overrideConfig = overrideConfig;
        updatePendingConfiguration(curConfig);
    
        sendMessage(H.LAUNCH_ACTIVITY, r);
    }
    ```
    
    ```kotlin
    private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
        // System.out.println("##### [" + System.currentTimeMillis() + "] ActivityThread.performLaunchActivity(" + r + ")");
    		//注释 1 ：获取 ActivityInfo 类
        ActivityInfo aInfo = r.activityInfo;
        if (r.packageInfo == null) {
    				//注释 2 ：获取 APK 文件的描述类 LoadedApk
            r.packageInfo = getPackageInfo(aInfo.applicationInfo, r.compatInfo,
                    Context.CONTEXT_INCLUDE_CODE);
        }
    		//注释 3：
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
    		//注释 4：创建要启动 Activity 的上下文环境
        ContextImpl appContext = createBaseContextForActivity(r);
        Activity activity = null;
        try {
            java.lang.ClassLoader cl = appContext.getClassLoader();
    				//注释 5：用类加载器来创建该 Activity 的实例
            activity = mInstrumentation.newActivity(
                    cl, component.getClassName(), r.intent);
            StrictMode.incrementExpectedActivityCount(activity.getClass());
            r.intent.setExtrasClassLoader(cl);
            r.intent.prepareToEnterProcess();
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
    				//注释 6：创建 Application
            Application app = r.packageInfo.makeApplication(false, mInstrumentation);
    
            if (localLOGV) Slog.v(TAG, "Performing launch of " + r);
            if (localLOGV) Slog.v(
                    TAG, r + ": app=" + app
                    + ", appName=" + app.getPackageName()
                    + ", pkg=" + r.packageInfo.getPackageName()
                    + ", comp=" + r.intent.getComponent().toShortString()
                    + ", dir=" + r.packageInfo.getAppDir());
    
            if (activity != null) {
                CharSequence title = r.activityInfo.loadLabel(appContext.getPackageManager());
                Configuration config = new Configuration(mCompatConfiguration);
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
                appContext.setOuterContext(activity);
    						//注释 7：初始化 Activity
                activity.attach(appContext, this, getInstrumentation(), r.token,
                        r.ident, app, r.intent, r.activityInfo, title, r.parent,
                        r.embeddedID, r.lastNonConfigurationInstances, config,
                        r.referrer, r.voiceInteractor, window, r.configCallback);
    
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
    
                activity.mCalled = false;
                if (r.isPersistable()) {
    								//注释 8：
                    mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
                } else {
                    mInstrumentation.callActivityOnCreate(activity, r.state);
                }
                if (!activity.mCalled) {
                    throw new SuperNotCalledException(
                        "Activity " + r.intent.getComponent().toShortString() +
                        " did not call through to super.onCreate()");
                }
                r.activity = activity;
                r.stopped = true;
                if (!r.activity.mFinished) {
                    activity.performStart();
                    r.stopped = false;
                }
                if (!r.activity.mFinished) {
                    if (r.isPersistable()) {
                        if (r.state != null || r.persistentState != null) {
                            mInstrumentation.callActivityOnRestoreInstanceState(activity, r.state,
                                    r.persistentState);
                        }
                    } else if (r.state != null) {
                        mInstrumentation.callActivityOnRestoreInstanceState(activity, r.state);
                    }
                }
                if (!r.activity.mFinished) {
                    activity.mCalled = false;
                    if (r.isPersistable()) {
                        mInstrumentation.callActivityOnPostCreate(activity, r.state,
                                r.persistentState);
                    } else {
                        mInstrumentation.callActivityOnPostCreate(activity, r.state);
                    }
                    if (!activity.mCalled) {
                        throw new SuperNotCalledException(
                            "Activity " + r.intent.getComponent().toShortString() +
                            " did not call through to super.onPostCreate()");
                    }
                }
            }
            r.paused = true;
    
            mActivities.put(r.token, r);
    
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
    
    - 注释 1 处：用来获取 ActivityInfo，用来存储代码以及 AndroidManifes设置的 Activity 和 Receiver 节点信息，比如 Activity 的 theme 和 launchMode。
    - 注释 2 处：获取 APK文件的描述类 LoadedAPk。
    - 注释 3 处：获取要启动的 Activity 的 ComponentName 类。
    - 注释 4 处：用来创建要启动 Activity 的上下文环境。
    - 注释 5 处：根据 ComponentName 中存储的 Activity 类名，用类加载器来创建该 Activity 的实例。
    - 注释 6 处：用来创建 Application，makeApplication 方法内部类会调用 Application 的 onCreate 方法。
    - 注释 7 处：调用 Activity 的 attach 方法初始化 Activity，在 attach 方法中会创建 Window对象(PhoneWindow)并与 Activity 自身进行关联。
    - 注释 8 处：调用 Instrumentation 的 callActivityOnCreate 方法来启动 Activity。
- 问题收集
    - 1、在 Zygote 里是怎么调用 ActivityThread 的 main 方法的？
        
        
    - 2、Zygote 是怎么 Fork 应用程序进程的？
        -