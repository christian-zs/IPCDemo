package com.example.zs.ipcdemo.pool;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.example.zs.ipcdemo.aidl.IBinderPool;

import java.util.concurrent.CountDownLatch;

/**
 * Binder 连接池 单例模式 负责为各个 Binder 通过 code 查询，负责维护 Binder 链接维护。
 */
public class BinderPool {

    private static final String TAG = "BinderPool";

    public static final int BINDER_COMPUTE = 0;
    public static final int BINDER_SECURITY_CENTER = 1;
    private IBinderPool mIBinderPool;
    private Context mContext;
    private CountDownLatch mConnectBinderPoolCountDownLatch;
    private static volatile BinderPool mBinderPool;

    public BinderPool(Context context) {
        mContext = context.getApplicationContext();
        connectBinderPoolService();

    }

    public static BinderPool getBinderPool(Context context) {
        if (mBinderPool == null) {
            synchronized (BinderPool.class) {
                if (mBinderPool == null) {
                    mBinderPool = new BinderPool(context);
                }
            }
        }
        return mBinderPool;
    }

    /**
     * 链接 BinderService
     */
    private synchronized void connectBinderPoolService() {
        mConnectBinderPoolCountDownLatch = new CountDownLatch(1);
        Intent service = new Intent(mContext, BinderPoolService.class);
        mContext.bindService(service, mBinderPoolConnection,
                Context.BIND_AUTO_CREATE);
        try {
            mConnectBinderPoolCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * query binder by binderCode from binder pool
     *
     * @param binderCode the unique token of binder
     * @return binder who's token is binderCode<br>
     * return null when not found or BinderPoolService died.
     */
    public IBinder queryBinder(int binderCode) {
        IBinder binder = null;
        try {
            if (mIBinderPool != null) {
                binder = mIBinderPool.queryBinder(binderCode);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return binder;
    }

    private ServiceConnection mBinderPoolConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // ignored.
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIBinderPool = IBinderPool.Stub.asInterface(service);
            try {
                mIBinderPool.asBinder().linkToDeath(mBinderPoolDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mConnectBinderPoolCountDownLatch.countDown();
        }
    };

    private IBinder.DeathRecipient mBinderPoolDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.w(TAG, "binder died.");
            mIBinderPool.asBinder().unlinkToDeath(mBinderPoolDeathRecipient, 0);
            mIBinderPool = null;
            connectBinderPoolService();
        }
    };
}
