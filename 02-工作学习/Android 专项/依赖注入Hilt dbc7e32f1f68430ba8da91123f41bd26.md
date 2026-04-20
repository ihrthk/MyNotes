# 依赖注入Hilt

官方文档：Android 中的依赖项注入

[https://developer.android.com/training/dependency-injection/hilt-android](https://developer.android.com/training/dependency-injection/hilt-android)

Hilt版本更新
[https://developer.android.google.cn/jetpack/androidx/releases/hilt?hl=en](https://developer.android.google.cn/jetpack/androidx/releases/hilt?hl=en)

CodeLab：在 Android 应用中使用 Hilt

[https://developer.android.com/codelabs/android-hilt?hl=zh_cn#0](https://developer.android.com/codelabs/android-hilt?hl=zh_cn#0)

解决方法：Github 代码地址

[https://github.com/ihrthk/codelab-android-hilt](https://github.com/ihrthk/codelab-android-hilt)

- 1、配置 hilt 相关依赖
    
    在根目录的 build.gradle文件
    
    ```kotlin
    buildscript {
        ext.kotlin_version = '1.6.20'
        ext.hilt_version = '2.40.1'
        repositories {
            google()
            mavenCentral()
        }
        dependencies {
            classpath 'com.android.tools.build:gradle:7.3.0'
            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
            classpath "com.google.dagger:hilt-android-gradle-plugin:$hilt_version"
        }
    }
    ```
    
    在主模块的 build.gradle 文件
    
    ```kotlin
    plugins {
        id 'com.android.application'
        id 'kotlin-android'
        id 'kotlin-parcelize'
        id 'kotlin-kapt'
        id 'dagger.hilt.android.plugin'
    }
    dependencies {
        // Hilt dependencies
        implementation "com.google.dagger:hilt-android:$hilt_version"
        kapt "com.google.dagger:hilt-android-compiler:$hilt_version"
    }
    ```
    
- 2、在 Application 配置 Hilt 注解
    
    要添加**附着于应用的生命周期的容器**，我们需要为 `Application` 类添加 `@HiltAndroidApp` 注解
    
    ```kotlin
    @HiltAndroidApp
    class LogApplication : Application() {
        ...
    }
    ```
    
    > `@HiltAndroidApp` 会触发 Hilt 的代码生成操作，生成的代码包括应用的一个基类，该基类可使用依赖项注入。application 容器是应用的父级容器，这意味着其他容器可以访问它提供的依赖项
    > 
- 3、通过构造函数提供实例使用：`@Inject`
    
    如要想让 `LogsFragment` 使用 Hilt，我们需要先为其添加 `@AndroidEntryPoint` 注解
    
    <aside>
    💡 利用 `@AndroidEntryPoint`，Hilt 可创建附着于 `LogsFragment` 生命周期的**依赖项容器**，并能够将实例注入 `LogsFragment`。
    
    </aside>
    
    1. 为 Android 类添加 `@AndroidEntryPoint` 注解会创建一个沿袭 Android 类生命周期的依赖项容器。
        
        ```kotlin
        @AndroidEntryPoint
        class LogsFragment : Fragment() {
            ...
        }
        ```
        
        > Hilt 目前支持以下 Android 类型：`Application`（通过使用 `@HiltAndroidApp`）、`Activity`、`Fragment`、`View`、`Service` 和 `BroadcastReceiver`。
        > 
        > 
        > Hilt 仅支持扩展 [`FragmentActivity`](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity?hl=zh-cn)（例如 [`AppCompatActivity`](https://developer.android.com/reference/kotlin/androidx/appcompat/app/AppCompatActivity?hl=zh-cn)）的 activity 和扩展 Jetpack 库 `Fragment` 的 fragment，不支持 Android 平台中的 `Fragment`（现已弃用）。
        > 
    2. 我们可以利用 **`@Inject`** **注解让 Hilt 注入不同类型的实例**：这就是所谓的**字段注入**。
        
        ```kotlin
        @AndroidEntryPoint
        class LogsFragment : Fragment() {
        
            @Inject lateinit var dateFormatter: DateFormatter
        
            ...
        }
        ```
        
        <aside>
        💡 在后台，Hilt 将使用自动生成的 `LogsFragment` 依赖项容器中内置的实例在 `onAttach()` 生命周期方法中填充这些字段。
        注意：由 Hilt 注入的字段不能是私有字段。
        
        </aside>
        
    3. 如要告知 Hilt 如何提供类型的实例，请向要注入的类的构造函数添加 @Inject 注解。
        
        打开 `util/DateFormatter.kt` 文件并为 `DateFormatter` 的构造函数添加 `@Inject` 注解。
        
        ```kotlin
        class DateFormatter @Inject constructor() { ... }
        ```
        
        > 请注意，要在 Kotlin 中为构造函数添加注解，您还需要 `constructor` 关键字。通过它，Hilt 便会知道如何提供 `DateFormatter` 的实例。
        > 
- 4、无法通过构造提供实例使用：`@Provides`
    
    > **模块用于向 Hilt 添加绑定**，换句话说，用于告知 Hilt 如何提供不同类型的实例。
    在 Hilt 模块中，您可以为**无法通过构造函数注入的类型**（例如接口或未包含在您项目中的类）添加绑定。
    这种类型的一个示例是数据库`XxDao`，您需要使用其构建器来创建实例。
    **Hilt 模块是带有`@Module` 和 `@InstallIn`** 注解的类。
    `@Module` 会告知 Hilt 这是一个模块，而 `@InstallIn` 会通过指定 Hilt 组件告知 Hilt 绑定在哪些容器中可用。
    您可以将 Hilt 组件视为容器，**对于每个可被 Hilt 注入的 Android 类，都有一个关联的 Hilt 组件**。
    例如，`Application` 容器与 `ApplicationComponent` 相关联，而 `Fragment` 容器与 `FragmentComponent` 相关联。
    > 
    1. 准备工作，我们来创建一个可添加绑定的 Hilt 模块。创建一个名为 `di` 的新文件包，并在该文件包中创建一个名为 `DatabaseModule.kt` 的新文件。
        
        ```kotlin
        @InstallIn(SingletonComponent::class)
        @Module
        object DatabaseModule {
        
        }
        ```
        
        <aside>
        💡 在 Kotlin 中，仅包含 `@Provides` 函数的模块可以是 `object` 类。通过这种方式，提供程序会得到优化，并几乎内嵌到生成的代码中。
        
        </aside>
        
    2. 使用 @Provides 提供实例，我们可以在 Hilt 模块中为函数添加 `@Provides` 注解，告知 Hilt 如何提供无法通过构造函数注入的类型。
        
        ```kotlin
        @InstallIn(SingletonComponent::class)
        @Module
        object DatabaseModule {
        
            @Singleton
            @Provides
            fun providerAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
                return Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "logging.db"
                ).build()
            }
        
        		@Provides
            fun provideLogDao(appDatabase: AppDatabase): LogDao = appDatabase.logDao()
        }
        ```
        
        <aside>
        💡 当提供 `LogDao` 的实例时，还需要将 `AppDatabase` 作为传递依赖项。
        
        </aside>
        
    3. 使用Inject注入字段，然后就可以使用 dao 实例了
        
        ```kotlin
        @AndroidEntryPoint
        class LogsFragment : Fragment() {
        
            @Inject
            lateinit var dao: LogDao
        }
        ```
        
- 5、为接口提供实现实例使用：**`@**Binds`
    
    <aside>
    💡 对于我们无法使用构造函数注入。
    **要告知 Hilt 为接口使用哪种实现，可以对 Hilt 模块内的函数使用 `@Binds` 注解**。
    
    </aside>
    
    1. 准备工作，在 `di` 文件夹中创建一个名为 `NavigationModule.kt` 的新文件。在该文件中，我们来创建一个名为 `NavigationModule` 的新抽象类，并为其添加 `@Module` 和 `@InstallIn(ActivityComponent::class)` 注解。
        
        ```kotlin
        @InstallIn(ActivityComponent::class)
        @Module
        abstract class NavigationModule {
        
            @Binds
            abstract fun bindNavigator(impl: AppNavigatorImpl): AppNavigator
        }
        ```
        
        <aside>
        💡 必须为抽象函数添加 `@Binds` 注解（它是抽象的，因此不包含任何代码，且该类也需要是抽象的）。
        抽象函数的返回值类型是我们要提供实现的接口
        
        </aside>
        
    2. 打开 `navigator/AppNavigatorImpl.kt` 文件，然后执行该操作：
        
        > 现在，我们必须告知 Hilt 如何提供 `AppNavigatorImpl` 的实例。
        由于该类可以进行构造函数注入，因此我们只需为其构造函数添加 `@Inject` 注解。
        > 
        
        ```kotlin
        class AppNavigatorImpl @Inject constructor(
            private val activity: FragmentActivity
        ) : AppNavigator {
            ...
        }
        ```
        
    3. 使用Inject注入字段，然后就可以使用 navigator 实例了
        
        ```kotlin
        @AndroidEntryPoint
        class MainActivity : AppCompatActivity() {
        
            @Inject lateinit var navigator: AppNavigator
        
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)
        
                if (savedInstanceState == null) {
                    navigator.navigateTo(Screens.BUTTONS)
                }
            }
        
            ...
        }
        ```
        
- 6、为同一类型的不同实现使用：`@Qualifier`
    
    > `ButtonsFragment` 和 `LogsFragment` 这两个 Fragment 都使用 `LoggerLocalDataSource`。
    但是，**如果我们需要在同一项目中提供两种实现，该怎么办？**
    例如，在应用运行时使用 `LoggerInMemoryDataSource`并在 `Service` 中使用 `LoggerLocalDataSource`。
    > 
    1. 让我们让 `LoggerLocalDataSource` 实现此接口
        
        ```kotlin
        @Singleton
        class LoggerLocalDataSource @Inject constructor(
            private val logDao: LogDao
        ) : LoggerDataSource {
            ...
            override fun addLog(msg: String) { ... }
            override fun getAllLogs(callback: (List<Log>) -> Unit) { ... }
            override fun removeLogs() { ... }
        }
        ```
        
    2. 现在，我们创建 `LoggerDataSource` 的另一个实现，名为 `LoggerInMemoryDataSource`，它会将日志保存到内存中。
        
        ```kotlin
        class LoggerInMemoryDataSource @Inject constructor() : LoggerDataSource {
        
            private val logs = LinkedList<Log>()
        
            override fun addLog(msg: String) {
                logs.addFirst(Log(msg, System.currentTimeMillis()))
            }
        
            override fun getAllLogs(callback: (List<Log>) -> Unit) {
                callback(logs)
            }
        
            override fun removeLogs() {
                logs.clear()
            }
        }
        ```
        
    3. 在 `di` 文件夹中创建一个名为 `LoggingModule.kt` 的新文件
        
        > 由于 `LoggerDataSource` 的不同实现的作用域限定为不同的容器，因此我们不能使用同一个模块。`LoggerInMemoryDataSource` 的作用域限定为 `Activity` 容器。
        而 `LoggerLocalDataSource` 的作用域限定为 `Application` 容器。
        > 
        
        ```kotlin
        @InstallIn(ActivityComponent::class)
        @Module
        abstract class LoggingInMemoryModule {
        
            @ActivityScoped
            @Binds
            abstract fun bindInMemoryLogger(impl: LoggerInMemoryDataSource): LoggerDataSource
        }
        
        @InstallIn(ApplicationComponent::class)
        @Module
        abstract class LoggingDatabaseModule {
        
            @Singleton
            @Binds
            abstract fun bindDatabaseLogger(impl: LoggerLocalDataSource): LoggerDataSource
        }
        ```
        
    4. 定义限定符
        
        ```kotlin
        @Qualifier
        annotation class InMemoryLogger
        
        @Qualifier
        annotation class DatabaseLogger
        
        @InstallIn(ActivityComponent::class)
        @Module
        abstract class LoggingInMemoryModule {
        
            @InMemoryLogger
            @ActivityScoped
            @Binds
            abstract fun bindInMemoryLogger(impl: LoggerInMemoryDataSource): LoggerDataSource
        }
        
        @InstallIn(SingletonComponent::class)
        @Module
        abstract class LoggingDatabaseModule {
        
            @DatabaseLogger
            @Singleton
            @Binds
            abstract fun bindDatabaseLogger(impl: LoggerLocalDataSource): LoggerDataSource
        }
        ```
        
    5. 使用Inject及限定符注入字段
        
        打开 `LogsFragment` 并对 logger 字段使用 `@InMemoryLogger` 限定符，已告知 Hilt 注入 `LoggerInMemoryDataSource`的实例：
        
        ```kotlin
        @AndroidEntryPoint
        class LogsFragment : Fragment() {
        
            @InMemoryLogger
            @Inject lateinit var logger: LoggerDataSource
            ...
        }
        ```
        
        对 `ButtonsFragment` 执行相同的操作：
        
        ```kotlin
        @AndroidEntryPoint
        class ButtonsFragment : Fragment() {
        
            @InMemoryLogger
            @Inject lateinit var logger: LoggerDataSource
            ...
        }
        ```
        
- 7、Hilt 不支持的类中注入依赖项使用：**`@**EntryPoint`
    
    正如我们之前看到的那样，Hilt 随附对最常见的 Android 组件的支持。但是，您可能需要在 Hilt 不直接支持或无法使用 Hilt 的类中执行字段注入。在此类情况下，您可以使用 `@EntryPoint`。
    
    > 背景需求：我们希望能够在应用进程之外导出日志。为此，需要使用 [`ContentProvider`](https://developer.android.com/reference/android/content/ContentProvider?hl=zh-cn)。我们仅允许使用方使用 `ContentProvider` 查询应用中的某个特定日志（提供一个 `id`）或所有日志。我们将使用 Room 数据库检索数据。因此，`LogDao` 类应提供使用数据库 [`Cursor`](https://developer.android.com/reference/android/database/Cursor?hl=zh-cn) 返回所需信息的方法。
    > 
    1. 打开 `LogDao.kt` 文件并向接口添加以下方法。
        
        ```kotlin
        @Dao
        interface LogDao {
            ...
        
            @Query("SELECT * FROM logs ORDER BY id DESC")
            fun selectAllLogsCursor(): Cursor
        
            @Query("SELECT * FROM logs WHERE id = :id")
            fun selectLogById(id: Long): Cursor?
        }
        ```
        
    2. 在新的 `contentprovider` 目录下创建一个名为 `LogsContentProvider.kt` 的新文件，并在其中包含以下内容：
        
        ```kotlin
        /** The authority of this content provider.  */
        private const val LOGS_TABLE = "logs"
        
        /** The authority of this content provider.  */
        private const val AUTHORITY = "com.example.android.hilt.provider"
        
        /** The match code for some items in the Logs table.  */
        private const val CODE_LOGS_DIR = 1
        
        /** The match code for an item in the Logs table.  */
        private const val CODE_LOGS_ITEM = 2
        
        /**
         * A ContentProvider that exposes the logs outside the application process.
         */
        class LogsContentProvider: ContentProvider() {
        
            private val matcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
                addURI(AUTHORITY, LOGS_TABLE, CODE_LOGS_DIR)
                addURI(AUTHORITY, "$LOGS_TABLE/*", CODE_LOGS_ITEM)
            }
        
            override fun onCreate(): Boolean {
                return true
            }
        
            /**
             * Queries all the logs or an individual log from the logs database.
             *
             * For the sake of this codelab, the logic has been simplified.
             */
            override fun query(
                uri: Uri,
                projection: Array<out String>?,
                selection: String?,
                selectionArgs: Array<out String>?,
                sortOrder: String?
            ): Cursor? {
                val code: Int = matcher.match(uri)
                return if (code == CODE_LOGS_DIR || code == CODE_LOGS_ITEM) {
                    val appContext = context?.applicationContext ?: throw IllegalStateException()
                    val logDao: LogDao = getLogDao(appContext)
        
                    val cursor: Cursor? = if (code == CODE_LOGS_DIR) {
                        logDao.selectAllLogsCursor()
                    } else {
                        logDao.selectLogById(ContentUris.parseId(uri))
                    }
                    cursor?.setNotificationUri(appContext.contentResolver, uri)
                    cursor
                } else {
                    throw IllegalArgumentException("Unknown URI: $uri")
                }
            }
        
            override fun insert(uri: Uri, values: ContentValues?): Uri? {
                throw UnsupportedOperationException("Only reading operations are allowed")
            }
        
            override fun update(
                uri: Uri,
                values: ContentValues?,
                selection: String?,
                selectionArgs: Array<out String>?
            ): Int {
                throw UnsupportedOperationException("Only reading operations are allowed")
            }
        
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
                throw UnsupportedOperationException("Only reading operations are allowed")
            }
        
            override fun getType(uri: Uri): String? {
                throw UnsupportedOperationException("Only reading operations are allowed")
            }
        }
        ```
        
        ```kotlin
        <application
        		android:name=".LogApplication"
            android:allowBackup="true"
            android:icon="@mipmap/ic_launcher"
            android:label="@string/app_name"
            android:roundIcon="@mipmap/ic_launcher_round"
            android:supportsRtl="true"
            android:theme="@style/AppTheme">
        		<provider
        		    android:authorities="@string/content_authority"
        		    android:name=".contentprovider.LogsContentProvider"
        		    android:exported="true"/>
        </application>
        ```
        
    3. `getLogDao(appContext)` 调用无法编译！我们需要通过从 Hilt application 容器获取 `LogDao` 依赖项来实现它。
        
        我们需要创建一个带有 `@EntryPoint` 注解的新接口才能访问它。
        
        **入口点是一个接口，对于我们所需的每个绑定（包括其限定符），都具有访问器方法**。此外，该接口还必须带有 `@InstallIn` 注解，以指定要安装入口点的组件。
        
        最佳做法是在使用入口点接口的类中添加新的接口。因此，我们将该接口添加到 `LogsContentProvider.kt` 文件中：
        
        ```kotlin
        class LogsContentProvider: ContentProvider() {
        
            @InstallIn(ApplicationComponent::class)
            @EntryPoint
            interface LogsContentProviderEntryPoint {
                fun logDao(): LogDao
            }
        
            ...
        }
        ```
        
    4. 如需访问入口点，请使用来自 `EntryPointAccessors` 的适当静态方法。
        
        ```kotlin
        class LogsContentProvider: ContentProvider() {
            ...
        
            private fun getLogDao(appContext: Context): LogDao {
                val hiltEntryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    LogsContentProviderEntryPoint::class.java
                )
                return hiltEntryPoint.logDao()
            }
        }
        ```
        

- 注意：我们需要为Fragment 容器的`Activity` 添加 `@AndroidEntryPoint` 注解。
    
    Hilt 需要了解托管 `Fragment` 的 `Activity` 才能正常运作。我们需要为 `MainActivity` 添加 `@AndroidEntryPoint` 注解。
    
    打开 `ui/MainActivity.kt` 文件并为 `MainActivity` 添加 `@AndroidEntryPoint` 注解：
    
    ```kotlin
    @AndroidEntryPoint
    class MainActivity : AppCompatActivity() { ... }
    ```
    
- 将实例的作用域限定为容器
    
    将实例的作用域限定为容器意味着在Hilt中，可以通过注解来指定某个类或对象的生命周期范围，并确保在该范围内只创建一个实例。
    
    这样可以有效地管理和控制依赖项的生命周期，避免重复创建和销毁实例，提高应用程序的性能和效率。
    
    在Hilt中，可以使用以下注解来限定实例的作用域为容器：
    
    1. @Singleton：将实例的作用域限定为应用程序的整个生命周期。使用@Singleton注解的实例在整个应用程序中只会创建一次，并且可以在不同的组件中共享。
    2. @ActivityScoped：将实例的作用域限定为Activity的生命周期。使用@ActivityScoped注解的实例在同一个Activity中只会创建一次，并且可以在该Activity的不同组件中共享。
    3. @FragmentScoped：将实例的作用域限定为Fragment的生命周期。使用@FragmentScoped注解的实例在同一个Fragment中只会创建一次，并且可以在该Fragment的不同组件中共享。