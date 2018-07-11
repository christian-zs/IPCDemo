![](http://upload-images.jianshu.io/upload_images/2515909-74dc6912fc0e389b.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

> 首先介绍 Android 序列化机制、Binder 工作原理，然后再通过创建 AIDL 进行进程间通信，一步步分析 AIDL 原理还有工作流程。
# 前言
在这里首先介绍 Android 序列化机制、Binder 主要是因为 AIDL 与这两个家伙密切相关，所以我们先了解一下他们，这样有助于我们更好的了解 AIDL 工作原理。

# Android 序列化机制
在 Android 系统中关于序列化的方法一般有两种，分别是实现 Serializable 接口和 Parcelable 接口
- Serializable  是来自 Java 中的序列化接口
-  Parcelable   是 Android 自带的序列化接口

上述的两种序列化接口都有各自不同的优缺点，我们在实际使用时需根据不同情况而定。Serializable 的使用比较简单，创建一个版本号即可；而 Parcelable 则相对复杂一些，会有四个方法需要实现。一般在保存数据到 SD 卡或者网络传输时建议使用 Serializable 即可，虽然效率差一些，好在使用方便。而在运行时数据传递时建议使用 Parcelable，比如 Intent，Bundle 等，Android 底层做了优化处理，效率很高。因此我们下面主要看一下 Parcelable。
### Parcelable 使用与分析
首先我们创建一个 Book 对象并且实现 Parcelable接口。
```
package com.example.zs.ipcdemo.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class Book implements Parcelable {
    private String bookId;
    private String bookName;

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public static Creator<Book> getCREATOR() {
        return CREATOR;
    }

    public Book(String bookId, String bookName) {
        this.bookId = bookId;
        this.bookName = bookName;
    }

    protected Book(Parcel in) {
        bookId = in.readString();
        bookName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bookId);
        dest.writeString(bookName);
    }

    public void readFromParcel(Parcel in) {
        bookId = in.readString();
        bookName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };
}

```
我们通过代码可以发现，在实现序列化的过程中主要的功能有序列化、反序列、描述内容，我们可以看出来其实内部都是 **Parcel** 一系列的 **readString**，**writeString** 方法来实现的，描述内容大部分都返回 0 。**Parcel** 就是一个存放读取数据的容器，可以在 Binder 中自由传输。这里不对 **Parcel** 做详细的讲解了有兴趣的可以看看这这篇博客 [Android 中 Parcel的分析以及使用](https://blog.csdn.net/qinjuning/article/details/6785517)

# Binder
简单来说 Binder 是 Android 中的一个类，它继承了 IBinder 接口。下面我们从不同角度来说一下 Binder。

-  从 IPC 角度来说，Binder 是 Android 中的一种跨进程通信方式，Binder 还可以理解为一种虚拟的物理设备，它的设备驱动是 /dev/binder，该通信方式在 linux 中没有。

-  从 Android Framework 角度来说，Binder 是 ServiceManager 连接各种 Manager（ActivityManager、WindowManager，etc）和相应 ManagerService 的桥梁。

- 从 Android 应用层来说，Binder 是客户端和服务端进行通信的媒介，当你 bindService 的时候，服务端会返回一个包含了服务端业务调用的 Binder 对象，通过这个 Binder 对象，客户端就可以获取服务端提供的服务或者数据，这里的服务包括普通服务和基于 AIDL 的服务。

####分析 Binder 组成结构
![摘自田维术的博客](https://upload-images.jianshu.io/upload_images/2515909-a705378c983d10da.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

图中的 ServiceManager，负责把 Binder Server 注册到一个容器中，当客户端调用 Server 时 ，Binder驱动去先去 Binder Server 容器中找你要调用的 Server 通知他做那些那些事情。
####分析 Binder 通讯流程
![摘自田维术的博客](https://upload-images.jianshu.io/upload_images/2515909-8767a30c695fbafd.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

从图中我们可以看到，Client 想要直接调用 Server 的 add 方法，是不可以的，因为它们在不同的进程中，这时候就需要 Binder 来帮忙了。

- 第一步 Server 在 ServiceManager 这个容器中注册。
- 第二部 Client 想要调用 Server 的 add 方法，就需要先获取 Server 对象， 但是 ServiceManager 不会把真正的 Server 对象返回给 Client，而是把 Server 的一个代理对象返回给 Client ，也就是 Proxy 。
- 第三部 Client 调用 Proxy 的 add 方法，ServiceManager 会帮他去调用 Server 的 add 方法，并把结果返回给 Client 。
#AIDL
我们的主角 **AIDL** 终于登场了，我们先创建一个简单的 AIDL 然后逐步分析它的工作原理。
####创建 Book 类
在主包下面建一个 aidl 包专门存放进程通讯用到的数据， Book 类继承 Parcelable 接口（原因：AIDL 只能传送继承 Parcelable 接口的类）
```
package com.example.zs.ipcdemo.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class Book implements Parcelable {
    private String bookId;
    private String bookName;

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public static Creator<Book> getCREATOR() {
        return CREATOR;
    }

    public Book(String bookId, String bookName) {
        this.bookId = bookId;
        this.bookName = bookName;
    }

    protected Book(Parcel in) {
        bookId = in.readString();
        bookName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bookId);
        dest.writeString(bookName);
    }

    public void readFromParcel(Parcel in) {
        bookId = in.readString();
        bookName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };
}

```

####创建 Book.aidl 、IBookManager.aidl
为什么要有 Book.aidl 类，因为只有将类在 aidl 中声明时候，AIDL 才能调用 Book 类。所以说要让 AIDL 能够传送自定义类需要  1、继承 Parcelable 接口  2、创建同名 .aidl 文件声明自己。
```
package com.example.zs.ipcdemo.aidl;

parcelable Book;
```
IBookManager.aidl 设置让客户端允许调用的接口。
```
package com.example.zs.ipcdemo.aidl;

import com.example.zs.ipcdemo.aidl.Book;

interface IBookManager{
    List<Book> getBookList();
    void addBook(in Book book);
}
```
这里面有个地方要注意一下  addBook 方法中有 in 这个类型，下面我们简单说一下 AIDL 文件中 in 类型out 类型和 inout 数据的区别。

- in ：客户端的参数输入：是把实参的值赋值给行参   那么对行参的修改，不会影响实参的值 。
- out ：服务端的参数输入：传递以后，行参和实参都是同一个对象，只是他们名字不同而已，对行参的修改将影响实参的值。
- inout ：这个可以叫输入输出参数，客户端可输入、服务端也可输入。客户端输入了参数到服务端后，服务端也可对该参数进行修改等，最后在客户端上得到的是服务端输出的参数。

我们手动 ReBuild 一下我们的项目系统在这个目录下**app\build\generated\source\aidl\debug** 为我们自动生成 IBookManger.java ，接下来让我们看一下 IBookManger.java 的庐山真面目。
```
/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: E:\\GitHub\\IPCDemo\\app\\src\\main\\aidl\\com\\example\\zs\\ipcdemo\\aidl\\IBookManager.aidl
 */
package com.example.zs.ipcdemo.aidl;

public interface IBookManager extends android.os.IInterface {
    /**
     * Local-side IPC implementation stub class.
     */
    public static abstract class Stub extends android.os.Binder implements com.example.zs.ipcdemo.aidl.IBookManager {
        private static final java.lang.String DESCRIPTOR = "com.example.zs.ipcdemo.aidl.IBookManager";

        /**
         * Construct the stub at attach it to the interface.
         */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an com.example.zs.ipcdemo.aidl.IBookManager interface,
         * generating a proxy if needed.
         */
        public static com.example.zs.ipcdemo.aidl.IBookManager asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof com.example.zs.ipcdemo.aidl.IBookManager))) {
                return ((com.example.zs.ipcdemo.aidl.IBookManager) iin);
            }
            return new com.example.zs.ipcdemo.aidl.IBookManager.Stub.Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_getBookList: {
                    data.enforceInterface(DESCRIPTOR);
                    java.util.List<com.example.zs.ipcdemo.aidl.Book> _result = this.getBookList();
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                }
                case TRANSACTION_addBook: {
                    data.enforceInterface(DESCRIPTOR);
                    com.example.zs.ipcdemo.aidl.Book _arg0;
                    if ((0 != data.readInt())) {
                        _arg0 = com.example.zs.ipcdemo.aidl.Book.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    this.addBook(_arg0);
                    reply.writeNoException();
                    return true;
                }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements com.example.zs.ipcdemo.aidl.IBookManager {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public java.util.List<com.example.zs.ipcdemo.aidl.Book> getBookList() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.util.List<com.example.zs.ipcdemo.aidl.Book> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getBookList, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createTypedArrayList(com.example.zs.ipcdemo.aidl.Book.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void addBook(com.example.zs.ipcdemo.aidl.Book book) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if ((book != null)) {
                        _data.writeInt(1);
                        book.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(Stub.TRANSACTION_addBook, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerListener(com.example.zs.ipcdemo.aidl.IOnNewBookArrivedListener listener) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((listener != null)) ? (listener.asBinder()) : (null)));
                    mRemote.transact(Stub.TRANSACTION_registerListener, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterListener(com.example.zs.ipcdemo.aidl.IOnNewBookArrivedListener listener) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((listener != null)) ? (listener.asBinder()) : (null)));
                    mRemote.transact(Stub.TRANSACTION_unregisterListener, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        static final int TRANSACTION_getBookList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_addBook = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);

    }

    public java.util.List<com.example.zs.ipcdemo.aidl.Book> getBookList() throws android.os.RemoteException;

    public void addBook(com.example.zs.ipcdemo.aidl.Book book) throws android.os.RemoteException;
}

```
刚看有点蒙逼，别急我们一步一步分析，IBookManager 文件中，包括 IBookManager.aidl 的两个接口，以及 Stub 和 Proxy 两个实现了  IBookManager 接口的类，其中 Stub 是定义在 IBookManager 接口中的，而 Proxy 则定义在 Stub 类中。
![IBookManager 类型结构分析](https://upload-images.jianshu.io/upload_images/2515909-a3f97a60b67655b9.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/840)

#### Client 执行流程
1、当服务连接建立完成后，客户端执行代码。**IBookManager.Stub.asInterface(service)**    这句话方法的作用是判断传入的参数 IBinder 对象和自己是否在同一个进程如果不是则把这个IBinder 参数包装成一个 Proxy 对象，这时调用 Stub 的 sum 方法，间接调用 Proxy 的 getBookList() 方法，我看一下代码。
```
// 连接服务
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, IBinder service) {
            //  IBinder 通过 asInterface 判断
            //  asInterface方法的作用是判断参数——也就是IBinder对象，和自己是否在同一个进程：
            //  是: 则直接转换、直接使用，接下来就跟 Binder 跨进程通信无关啦
            //  否: 则把这个IBinder参数包装成一个 Proxy 对象，这时调用 Stub 的方法，间接调用Proxy的方法
            IBookManager bookManager = IBookManager.Stub.asInterface(service);
            try {
                mBookManager = bookManager;
                bookManager.getBookList();
                Log.d(TAG, "get books count：" + bookManager.getBookList().size());
                bookManager.registerListener(arrivedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBookManager = null;
        }
    };
```

```
public static com.example.zs.ipcdemo.aidl.IBookManager asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            // 作用是判断传入的参数 IBinder 对象和自己是否在同一个进程
            if (((iin != null) && (iin instanceof com.example.zs.ipcdemo.aidl.IBookManager))) {
                return ((com.example.zs.ipcdemo.aidl.IBookManager) iin);
            }
            return new com.example.zs.ipcdemo.aidl.IBookManager.Stub.Proxy(obj);
        }
```

2、Proxy 在自己的 sumgetBookList() 方法中，会使用 Parcelable 来准备数据，把函数名称、函数参数都写入 _data，让 _reply 接收函数返回值。最后使用 IBinder 的 transact() 方法，把数据就传给 Binder 的 Server 端了。
```
 @Override
            public java.util.List<com.example.zs.ipcdemo.aidl.Book> getBookList() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.util.List<com.example.zs.ipcdemo.aidl.Book> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    // 通知服务器调用该方法
                    mRemote.transact(Stub.TRANSACTION_getBookList, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createTypedArrayList(com.example.zs.ipcdemo.aidl.Book.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

```
#### Server 执行流程
Server 则是通过 onTransact()  方法接收 Client 进程传过来的数据，包括函数名称、函数参数，找到对应的函数，这里是 getBookList() ，把参数喂进去，得到结果，返回。所以 onTransact() 函数经历了读数据、执行要调用的函数、把执行结果再写数据的过程。
```
 @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                // 客户端调用指令 getBookList()
                case TRANSACTION_getBookList: {
                    data.enforceInterface(DESCRIPTOR);
                    // 调用本地服务 getBookList() 方法
                    java.util.List<com.example.zs.ipcdemo.aidl.Book> _result = this.getBookList();
                    reply.writeNoException();
                    // 将数据传给客户端
                    reply.writeTypedList(_result);
                    return true;
                }
                case TRANSACTION_addBook: {
                    data.enforceInterface(DESCRIPTOR);
                    com.example.zs.ipcdemo.aidl.Book _arg0;
                    if ((0 != data.readInt())) {
                        _arg0 = com.example.zs.ipcdemo.aidl.Book.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    this.addBook(_arg0);
                    reply.writeNoException();
                    return true;
                }
            return super.onTransact(code, data, reply, flags);
        }
```
以上就是 AIDL 的具体原理以及工作流程的分析，希望对大家有所帮助。








