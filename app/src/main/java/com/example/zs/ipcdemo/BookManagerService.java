package com.example.zs.ipcdemo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.zs.ipcdemo.aidl.Book;
import com.example.zs.ipcdemo.aidl.IBookManager;
import com.example.zs.ipcdemo.aidl.IOnNewBookArrivedListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AIDL 服务端
 */
public class BookManagerService extends Service {

    private static final String TAG = "BMS";


    private AtomicBoolean mIsServiceDestoryed = new AtomicBoolean(false);
    // 所有书籍
    private CopyOnWriteArrayList<Book> mBooks = new CopyOnWriteArrayList<>();
    // 注销监听
    private RemoteCallbackList<IOnNewBookArrivedListener> mRemoteCallbackList = new RemoteCallbackList<>();


    private Binder mBinder = new IBookManager.Stub() {
        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBooks;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            if (book != null) {
                mBooks.add(book);
                Log.d(TAG, "添加图书成功，图书的数量为：" + mBooks.size());
            }
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mRemoteCallbackList.register(listener);
            final int N = mRemoteCallbackList.beginBroadcast();
            mRemoteCallbackList.finishBroadcast();
            Log.d(TAG, "registerListener, current size:" + N);
        }

        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
            boolean success = mRemoteCallbackList.unregister(listener);
            if (success) {
                Log.d(TAG, "unregister success.");
            } else {
                Log.d(TAG, "not found, can not unregister.");
            }
            final int N = mRemoteCallbackList.beginBroadcast();
            mRemoteCallbackList.finishBroadcast();
            Log.d(TAG, "unregisterListener, current size:" + N);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Book book1 = new Book("1", "Android");
        Book book2 = new Book("2", "iOS");
        mBooks.add(book1);
        mBooks.add(book2);
        // 开启线程每5秒创建一个本新书
        new Thread(new Worker()).start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsServiceDestoryed.set(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void onNewBookArrived(Book book) throws RemoteException {
        mBooks.add(book);
        final int N = mRemoteCallbackList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            IOnNewBookArrivedListener l = mRemoteCallbackList.getBroadcastItem(i);
            if (l != null) {
                try {
                    l.onNewBookArrived(book);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mRemoteCallbackList.finishBroadcast();
    }

    private class Worker implements Runnable {

        @Override
        public void run() {
            while (!mIsServiceDestoryed.get()) {
                try {
                    Thread.sleep(5000);
                    String bookId = String.valueOf(mBooks.size() + 1);
                    Book book = new Book(bookId, "新书：" + bookId);
                    Log.d(TAG, "新书：" + bookId);
                    onNewBookArrived(book);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
