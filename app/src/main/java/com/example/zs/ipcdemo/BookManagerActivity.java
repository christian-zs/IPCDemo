package com.example.zs.ipcdemo;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.zs.ipcdemo.aidl.Book;
import com.example.zs.ipcdemo.aidl.IBookManager;
import com.example.zs.ipcdemo.aidl.IOnNewBookArrivedListener;

/**
 * AIDL 客户端
 */
public class BookManagerActivity extends AppCompatActivity {


    private static final String TAG = "BookManagerActivity";
    // 新增数量标示
    private static final int MESSAGE_NEW_BOOK_ARRIVED = 1;
    // 图书管理
    private IBookManager mBookManager;
    // 消息处理
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_NEW_BOOK_ARRIVED:
                    Log.d(TAG, "arrived new book :" + msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    // 连接服务
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, IBinder service) {
            //  IBinder 通过 asInterface 判断
            // asInterface方法的作用是判断参数——也就是IBinder对象，和自己是否在同一个进程：
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
    // 新书通知监听
    private IOnNewBookArrivedListener arrivedListener = new IOnNewBookArrivedListener.Stub() {
        @Override
        public void onNewBookArrived(Book newBook) {

            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED, newBook).sendToTarget();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_manager);
        Intent intent = new Intent(BookManagerActivity.this, BookManagerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销连接
        unbindService(connection);
        // 注销新书提醒
        assert mBookManager != null;
        if (mBookManager.asBinder().isBinderAlive()) {
            try {
                mBookManager.unregisterListener(arrivedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
