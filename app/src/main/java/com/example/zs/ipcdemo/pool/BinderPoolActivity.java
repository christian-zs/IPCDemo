package com.example.zs.ipcdemo.pool;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.example.zs.ipcdemo.R;
import com.example.zs.ipcdemo.aidl.ICompute;
import com.example.zs.ipcdemo.aidl.ISecurityCenter;

public class BinderPoolActivity extends Activity {
    private static final String TAG = "BinderPoolActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binder_pool);
        // 开启线程访问 连接线程池 采用 CountDownLatch（）同步调用
        new Thread(new Runnable() {

            @Override
            public void run() {
                doWork();
            }
        }).start();
    }

    /**
     * connect binder pool
     */
    private void doWork() {
        // 获取binder pool 实例≥≥
        BinderPool binderPool = BinderPool.getBinderPool(BinderPoolActivity.this);
        // 通过binder code 获取想要的binder
        IBinder securityBinder = binderPool
                .queryBinder(BinderPool.BINDER_SECURITY_CENTER);

        ISecurityCenter mSecurityCenter = SecurityCenterImpl.asInterface(securityBinder);

        Log.d(TAG, "visit ISecurityCenter");
        String msg = "helloworld-安卓";
        System.out.println("content:" + msg);
        try {
            String password = mSecurityCenter.encrypt(msg);
            System.out.println("encrypt:" + password);
            System.out.println("decrypt:" + mSecurityCenter.decrypt(password));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "visit ICompute");
        IBinder computeBinder = binderPool.queryBinder(BinderPool.BINDER_COMPUTE);
        ICompute mCompute = ComputeImpl.asInterface(computeBinder);
        try {
            System.out.println("3+5=" + mCompute.add(3, 5));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
