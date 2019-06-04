## Glide使用 && Glide源码分析
## 一、Glide基本使用
[Glide官方文档地址](https://bumptech.github.io/glide/)<br/>
### 1.1、 Example
```
Glide.with(imageView.context)
    .load(roundRectUrl)
    .apply(RequestOptions().priority(Priority.HIGH))
    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.DATA))
    .apply(RequestOptions.centerCropTransform())
    .apply(
        RequestOptions.bitmapTransform(
            RoundedCornersTransformation(
                radius, 0, RoundedCornersTransformation.CornerType.ALL
            )
        )
    )
    .into(imageView)
```
### 1.2、自定义AppGlideModule
Glide4.0之后，GlideModule推荐使用注解(@GlideModule)形式实现
```
@GlideModule
class GlideConfigModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        //设置内存大小
        builder.setMemoryCache(LruResourceCache(1024 * 1024 * 100)) // 100M

        //设置图片缓存大小
        builder.setBitmapPool(LruBitmapPool(1024 * 1024 * 50))

        /**
         * 设置磁盘缓存大小
         */
        // 内部缓存目录  data/data/packageName/DiskCacheName
        builder.setDiskCache(
            InternalCacheDiskCacheFactory(context, "GlideDemo", 1024 * 1024 * 100)
        )
        // 外部磁盘SD卡
        /*builder.setDiskCache(
            ExternalPreferredCacheDiskCacheFactory(context, "GlideDemo", 1024 * 1024 * 10)
        )*/
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // 替换网络请求组件，使用OkHttp
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(ProgressInterceptor)
        val okHttpClient = builder.build()

        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient))
    }

    /**
     * 禁用清单解析
     */
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}
```

## 二、前言
### 2.1  一个好的图片加载框架必备的功能：
当阅读Glide源码之前，我们应该考虑一个问题，假设让我们自己去实现一个图片加载框架，会需要哪些功能？<br/><br/>
&emsp;1、网络组件，下载图片&emsp;&emsp;HttpUrlConnection&emsp;&emsp;OkHttp <br/>
&emsp;2、请求队列，线程池&emsp;&emsp;&emsp;优先级处理，请求的终止<br/>
&emsp;3、缓存： 内存缓存，本地文件缓存，服务器缓存等等<br/>
&emsp;4、内存资源回收机制：  先进先出，或者生命周期<br/>
&emsp;5、更加基础的组件就是各种图片资源的转码解码了<br/>
### 2.2 Android图片加载框架对比
&emsp;&emsp;假设你去面试，面试官问你使用什么图片框架，你说你用Glide。面试官并不是想让你说Glide，而是你选择Glide图片加载框架的原因和理由。它与其他图片加载框架的优缺点。<br/><br/>
&emsp;&emsp;这里引用前辈的文章，就不在单独再写一篇了：[Android图片加载框架Fresco，Glide，Picasso对比分析](https://www.jianshu.com/p/df4ca17432e5)

## 三、Glide流程分析
### 3.1 流程图
这是从网上找的流程图：
![](https://user-gold-cdn.xitu.io/2019/6/3/16b1c8aff0ae242f?w=782&h=390&f=jpeg&s=26424)<br/>
&emsp;&emsp;你心中会想：这么简单？？<br/>
&emsp;&emsp;我回答你，当然不是这么简单，但这确实是最主要最底层的架构，所有的图片加载框架都一样，区别在于在这个基础上延伸的各种小细节，比如多少级缓存、缓存的位置(是缓存在应用的内存中，还是缓存在系统的共享匿名内存中)、生命周期管理等等。<br/>
### 3.2 Glide使用三步曲源码分析
这里就需要去阅读我的上一篇文章了：[Glide源码分析One](https://juejin.im/post/5cf4c9aae51d4556f76e8042)
### 3.3 类关系图
OK，当你看完了3.2后，再看Glide的类关系图你会相当的清晰，这个图也是我从网上找的：
![](https://user-gold-cdn.xitu.io/2019/6/3/16b1c8e15581a34c?w=1787&h=1792&f=png&s=116539)<br/>

----
**接下来就是一些细节问题了**<br/><br/>

## 四、生命周期管理
生命周期管理是在:&emsp;第一步<font color=red>Glide.with()</font> 中创建的，具体逻辑在<font color=red>RequestManagerRetriever.get()</font>中实现：
<br/>
<br/>
### 4.1 RequestManagerRetriever.get():
```
public RequestManager get(@NonNull Context context) {
  、、、
  if (Util.isOnMainThread() && !(context instanceof Application)) {
    if (context instanceof FragmentActivity) {
      return get((FragmentActivity) context);
    } else if (context instanceof Activity) {
      return get((Activity) context);
    } else if (context instanceof ContextWrapper) {
      return get(((ContextWrapper) context).getBaseContext());
    }
  }
  return getApplicationManager(context);
}
```
&emsp;&emsp;get()方法重载了很多，参数对应与Glide.with()方法的参数，分别有Context、Activity、FragmentActivity、Fragment、android.support.v4.app.Fragment、View.<br/>
<font color=red>参数很重要，参数很重要，参数很重要！！！接下来给出解释：</font><br/>
&emsp;&emsp;<font color=red>1、RequestManagerRetriever.get(Activity):  参数为Activity的get()方法</font>
```
  public RequestManager get(@NonNull Activity activity) {
    if (Util.isOnBackgroundThread()) {
      return get(activity.getApplicationContext());
    } else {
      assertNotDestroyed(activity);
      android.app.FragmentManager fm = activity.getFragmentManager();  // FragmentManager直接来自Activity
      return fragmentGet(
          activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
    }
  }
```
&emsp;&emsp;<font color=red>2、RequestManagerRetriever.get(Fragment):  参数为Fragment的get()方法</font>
```
  public RequestManager get(@NonNull Fragment fragment) {
    if (Util.isOnBackgroundThread()) {
      return get(fragment.getActivity().getApplicationContext());
    } else {
      FragmentManager fm = fragment.getChildFragmentManager();  // FragmentManager来自Fragment
      return supportFragmentGet(fragment.getActivity(), fm, fragment, fragment.isVisible());
    }
  }
```
&emsp;&emsp;<font color=red>3、RequestManagerRetriever.get(View):  参数为View的get()方法</font>
```
  public RequestManager get(@NonNull View view) {
    ...
    Activity activity = findActivity(view.getContext());
    if (activity == null) {
      return get(view.getContext().getApplicationContext());
    }

    // Support Fragments.
    // Although the user might have non-support Fragments attached to FragmentActivity, searching
    // for non-support Fragments is so expensive pre O and that should be rare enough that we
    // prefer to just fall back to the Activity directly.
    if (activity instanceof FragmentActivity) {
      Fragment fragment = findSupportFragment(view, (FragmentActivity) activity);
      return fragment != null ? get(fragment) : get(activity);
    }

    // Standard Fragments.
    android.app.Fragment fragment = findFragment(view, activity);
    if (fragment == null) {
      return get(activity);
    }
    return get(fragment);
  }
```
<font color=red><b>通过View去寻找其所在的Fragment，如果该View在Fragment中，直接走参数为Fragment的get(Fragment)方法；如果在Activity中，则直接走参数为Activity的get(Activity)方法</b></font><br/>

重载的所有get()方法，最终都只有三种情况：<br/>
&emsp;&emsp;1）、 getApplicationManager()<br/>
&emsp;&emsp;2）、 fragmentGet()<br/>
&emsp;&emsp;3）、 supportFragmentGet()<br/>
- 1、 getApplicationManager()
当参数为ApplicationContext或者运行在非主线程时，会执行到getApplicationManager()方法:
```
private RequestManager getApplicationManager(@NonNull Context context) {
  // Either an application context or we are on a background thread.
  if (applicationManager == null) {
    synchronized (this) {
      if (applicationManager == null) {
        // Normally pause/resume is taken care of by the fragment we add to the fragment or
        // activity. However, in this case since the manager attached to the application will not
        // receive lifecycle events, we must force the manager to start resumed using
        // ApplicationLifecycle.

        // TODO(b/27524013): Factor out this Glide.get() call.
        Glide glide = Glide.get(context.getApplicationContext());
        applicationManager =
            factory.build(
                glide,
                new ApplicationLifecycle(),
                new EmptyRequestManagerTreeNode(),
                context.getApplicationContext());
      }
    }
  }
  return applicationManager;
}
```
看该方法的注释，<font color=red>Request(也就是缓存)的生命周期是跟随Application</font></br>
- 2、fragmentGet(FragmentManager)
```
  private RequestManager fragmentGet(@NonNull Context context,
      @NonNull android.app.FragmentManager fm,
      @Nullable android.app.Fragment parentHint,
      boolean isParentVisible) {
    RequestManagerFragment current = getRequestManagerFragment(fm, parentHint, isParentVisible); 
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      Glide glide = Glide.get(context);
      // build RequestManager时，将RequestManagerFragment的生命周期(current.getGlideLifecycle())传入
      requestManager =
          factory.build(
              glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }

private RequestManagerFragment getRequestManagerFragment(
      @NonNull final android.app.FragmentManager fm,
      @Nullable android.app.Fragment parentHint,
      boolean isParentVisible) {
    // 通过FragmentManager寻找是否已经添加过RequestManagerFragment，避免重复添加
    RequestManagerFragment current = (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG); 
    if (current == null) {
      current = pendingRequestManagerFragments.get(fm);
      if (current == null) {
        current = new RequestManagerFragment();  // 新建一个空布局的Fragment
        current.setParentFragmentHint(parentHint);
        if (isParentVisible) {
          current.getGlideLifecycle().onStart();
        }
        // 在原有的界面上添加一个空布局的Fragment，用于生命周期管理
        pendingRequestManagerFragments.put(fm, current); 
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        handler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
  }
```
<font color=red>Glide的请求的生命周期管理是通过向页面添加一个空布局的Fragment来实现的，上面说参数很重要，这里给大家解惑。<br/>
&emsp;&emsp; 1、当参数为Activity时，空布局的Fragment直接添加在Activity上，此时的Request的生命周期跟随Activity;<br/>
&emsp;&emsp; 2、当参数为Fragment时，空布局的Fragment直接添加在Fragment上，此时的Request的生命周期跟随父Fragment;<br/>
&emsp;&emsp; 3、单参数为View时，这时会通过View去找寻该View是在Fragment上还是在Activity中，分别走以上两种情况;</font>
### 4.2 RequestManager
```
RequestManager(
    Glide glide,
    Lifecycle lifecycle,
    RequestManagerTreeNode treeNode,
    RequestTracker requestTracker,
    ConnectivityMonitorFactory factory,
    Context context) {
  ···
  // If we are the application level request manager, we may be created on a background thread.
  // In that case we cannot risk synchronously pausing or resuming requests, so we hack around the
  // issue by delaying adding ourselves as a lifecycle listener by posting to the main thread.
  // This should be entirely safe.
  if (Util.isOnBackgroundThread()) {
    mainHandler.post(addSelfToLifecycle);
  } else {
    lifecycle.addListener(this);
  }
  ···
}

private final Runnable addSelfToLifecycle = new Runnable() {
  @Override
  public void run() {
    lifecycle.addListener(RequestManager.this);
  }
};
```
1、<font color=red>mainHandler.post(addSelfToLifecycle);</font><br/>
&emsp;&emsp;当前线程为后台进程、或者是application等级的主线程时，通过mainHandler切回主线程，因为得刷新UI，主线程安全<br/>
2、<font color=red>lifecycle.addListener(this);</font><br/>
&emsp;&emsp; 直接监听上一步生成的布局为空的fragment的生命周期<br/>

## 五、优先级Priority
### 5.1 Priority 类
```
public enum Priority {
  IMMEDIATE,
  HIGH,
  NORMAL,
  LOW,
}
```
### 5.2 DecodeJob
&emsp;&emsp;前面分析Glide执行流程时发现，DecodeJob是真正干活的类，其实现了Runnable接口，但是它还<font color=red>实现了Comparable<DecodeJob<?>>接口.</font><br/>
Engine实例化DecodeJob时，会将优先级属性传入DecodeJob中，具体使用在其实现的比较方法中：</br>
```
/**
 * @return  a negative integer, zero, or a positive integer as  
 * this object is less than, equal to, or greater than the specified        
 **/
@Override
public int compareTo(@NonNull DecodeJob<?> other) {
  int result = getPriority() - other.getPriority();
  if (result == 0) {
    result = order - other.order;
  }
  return result;
}
```
<font color=red>ThreadPoolExecutor：</font><br/>
&emsp;&emsp;execute()<br/>
<font color=red>PriorityBlockingQueue：</font>&emsp;&emsp;// 线程池的任务队列，选择高优先级的任务优先执行<br/>
&emsp;&emsp;offer()<br/>
&emsp;&emsp;siftUpComparable()<br/>
<font color=red>同时实现Runnable和Comparable<DecodeJob<?>>接口，线程池自动实现优先级的自动排序。</font><br/>
## 六、缓存
### 6.1 LRU缓存算法
在Glide初始化时，会指定一个默认的缓存算法，在GlideBuilder.build()方法中
```
Glide build(@NonNull Context context) {
  ···
  if (memoryCache == null) {
      memoryCache = new LruResourceCache(memorySizeCalculator.getMemoryCacheSize());
  }
  ···
}
```
&emsp;&emsp;使用了<font color=red>LRU（Least Recently Used）算法</font>，核心就是最近最少使用。在算法的内部维护了一个<font color=red>LinkHashMap的链表(双向链表)</font>，通过put数据的时候判断是否内存已经满了，如果满了，则将最近最少使用的数据给剔除掉，从而达到内存不会爆满的状态。<br/>
![](https://user-gold-cdn.xitu.io/2019/6/4/16b205c5f73baa11)<br/>
&emsp;&emsp;更重要的一点是，当我们通过<font color=red>get()</font>方法获取数据的时候，这个获取的数据会从队列中跑到队列尾来，从而很好的满足我们LruCache的算法设计思想。<br/>
LinkedHashMap.get():<br/>
```
public V get(Object key) {
    Node<K,V> e;
    if ((e = getNode(hash(key), key)) == null)
        return null;
    if (accessOrder)
        afterNodeAccess(e);
    return e.value;
}

void afterNodeAccess(Node<K,V> e) { // move node to last
    LinkedHashMapEntry<K,V> last;
    if (accessOrder && (last = tail) != e) {
        LinkedHashMapEntry<K,V> p =
            (LinkedHashMapEntry<K,V>)e, b = p.before, a = p.after;
        p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a != null)
            a.before = b;
        else
            last = b;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
        tail = p;
        ++modCount;
    }
}
```
### 6.2 Glide缓存逻辑
缓存逻辑关键类<font color=red>Engine</font>
#### 6.2.1 缓存Key
* Enginr.load():
```
public synchronized <R> LoadStatus load(...) {
  EngineKey key = keyFactory.buildKey(model, signature, width, height, transformations, resourceClass, transcodeClass, options);
  ...
}
```
model： Url、Uri、FilePath，图片来源地址<br/>
<br/>
这里有个前提知识必须得知道：<br/>
&emsp;&emsp;DiskCacheStrategy.NONE&emsp;&emsp;&emsp;&emsp;&emsp;什么都不缓存<br/>
&emsp;&emsp;DiskCacheStrategy.DATA&emsp;&emsp;&emsp;&emsp;&emsp;<font color=red> 只缓存原来的全分辨率的图像。</font> <br/>
&emsp;&emsp;DiskCacheStrategy.RESOURCE&emsp;&emsp;&emsp;<font color=red>只缓存最终的图像</font>，即降低分辨率后的（或者是
转换后的）<br/>
&emsp;&emsp;DiskCacheStrategy.ALL&emsp;&emsp;&emsp;&emsp;&emsp;&emsp; 缓存所有版本的图像<br/>
&emsp;&emsp;DiskCacheStrategy.AUTOMATIC&emsp;&emsp;让Glide根据图片资源智能地选择使用哪一种缓存策略
（默认选项）<br/>
<br/>
<font color=red>width，height：从缓存的key中包含宽高能得出，同一张图片，假设设置的不是缓存原始图片，不同宽高将缓存不同的图片。  <br/>
优点： 加载更快，少了图片宽高处理的过程。<br/>
缺点： 更费内存<br/></font>
<br/>
<font color=red>另一Android图片加载框架Picasso，只缓存一张全尺寸的图片，优点是占用内存小，缺点是再次显时示，需要重新调整大小。</font><br/>
### 6.2 缓存读取逻辑
 Enginr.load():
 ```
 public synchronized <R> LoadStatus load() {
  ... 
  EngineKey key = keyFactory.buildKey(model, signature, width, height, transformations, resourceClass, transcodeClass, options);

EngineResource<?> active = loadFromActiveResources(key, isMemoryCacheable);
if (active != null) {
  cb.onResourceReady(active, DataSource.MEMORY_CACHE);
  if (VERBOSE_IS_LOGGABLE) {
    logWithTimeAndKey("Loaded resource from active resources", startTime, key);
  }
  return null;
}

EngineResource<?> cached = loadFromCache(key, isMemoryCacheable);
if (cached != null) {
  cb.onResourceReady(cached, DataSource.MEMORY_CACHE);
  if (VERBOSE_IS_LOGGABLE) {
    logWithTimeAndKey("Loaded resource from cache", startTime, key);
  }
  return null;
}

EngineJob<?> current = jobs.get(key, onlyRetrieveFromCache);
if (current != null) {
  current.addCallback(cb, callbackExecutor);
  if (VERBOSE_IS_LOGGABLE) {
    logWithTimeAndKey("Added to existing load", startTime, key);
  }
  return new LoadStatus(cb, current);
}

 // 生成网络请求逻辑
 ...
}

@Nullable
private EngineResource<?> loadFromActiveResources(Key key, boolean isMemoryCacheable) {
  if (!isMemoryCacheable) {
    return null;
  }
  EngineResource<?> active = activeResources.get(key);
  if (active != null) {
    active.acquire();  // 引用计数器加1
  }
  return active;
}

private EngineResource<?> loadFromCache(Key key, boolean isMemoryCacheable) {
  if (!isMemoryCacheable) {
    return null;
  }

  EngineResource<?> cached = getEngineResourceFromCache(key);  // 将缓存数据从缓存队列中移出
  if (cached != null) {
    cached.acquire();  // 引用计数器加1
    activeResources.activate(key, cached);  // 将数据放入正在活动的缓存列表中
  }
  return cached;
}
 ```
 EngineResource.acquire():
 ```
 synchronized void acquire() {
  if (isRecycled) {
    throw new IllegalStateException("Cannot acquire a recycled resource");
  }
  ++acquired; // 资源被引用的计数器
}
 ```
 ### 6.3 缓存生成 
 其实就是在图片数据加载成功后，放入缓存列表。回调的最终方法在Engine的onEngineJobComplete()方法。
 ```
 public synchronized void onEngineJobComplete(
    EngineJob<?> engineJob, Key key, EngineResource<?> resource) {
  // A null resource indicates that the load failed, usually due to an exception.
  if (resource != null) {
    resource.setResourceListener(key, this);

    if (resource.isCacheable()) { // 判断该资源是否需要缓存
      activeResources.activate(key, resource);   // 缓存
    }
  }
  jobs.removeIfCurrent(key, engineJob);
}
 ```
 ### 6.4 缓存的移除
 Engine.onResourceReleased():
 ```
 public synchronized void onResourceReleased(Key cacheKey, EngineResource<?> resource) {
  activeResources.deactivate(cacheKey); // 从活动列表移除
  if (resource.isCacheable()) {
    cache.put(cacheKey, resource);  // 放入缓存列表
  } else {
    resourceRecycler.recycle(resource);
  }
}
 ```
移除缓存的具体调用流程：<br/>
&emsp;&emsp;这里就得扯到前面的生命周期管理了，RequestManager中实现了LifecycleListener，接收Fragment或Activity的生命周期，这里主要分析onDestory()方法： <br/>
RequestManager.onDestory():
```
public synchronized void onDestroy() {
  targetTracker.onDestroy();
  for (Target<?> target : targetTracker.getAll()) {
    clear(target);
  }
  targetTracker.clear();
  requestTracker.clearRequests();
  lifecycle.removeListener(this);
  lifecycle.removeListener(connectivityMonitor);
  mainHandler.removeCallbacks(addSelfToLifecycle);
  glide.unregisterRequestManager(this);
}
```
RequestTracker.clearRequests() -> SingleRequest.clear() -> Engine.release() -> EngineResource.release()
```
void release() {
  // To avoid deadlock, always acquire the listener lock before our lock so that the locking
  // scheme is consistent (Engine -> EngineResource). Violating this order leads to deadlock
  // (b/123646037).
  synchronized (listener) {
    synchronized (this) {
      if (acquired <= 0) {
        throw new IllegalStateException("Cannot release a recycled or not yet acquired resource");
      }
      if (--acquired == 0) { // 引用数值标准acquire减1
        listener.onResourceReleased(key, this);
      }
    }
  }
}
```
<br/>
参考了下面的优秀文章：<br/>

&emsp;&emsp;[Glide源码分析](https://github.com/android-cn/android-open-project-analysis/tree/master/tool-lib/image-cache/glide)<br/>
&emsp;&emsp;[Glide源码分析流程思维导图](https://www.jianshu.com/p/77959188d234)<br/>
<br/>
<br/>
&copy;爱穿衬衫的程序员
