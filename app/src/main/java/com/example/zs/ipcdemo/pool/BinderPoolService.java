package com.example.zs.ipcdemo.pool;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.example.zs.ipcdemo.aidl.IBinderPool;

public class BinderPoolService extends Service {

    private static final String TAG = "BinderPoolService";

    public static final int BINDER_COMPUTE = 0;
    public static final int BINDER_SECURITY_CENTER = 1;

    private class BinderPoolImpl extends IBinderPool.Stub {

        BinderPoolImpl() {
            super();
        }

        @Override
        public IBinder queryBinder(int binderCode) throws RemoteException {
            IBinder binder = null;
            switch (binderCode) {
                case BINDER_SECURITY_CENTER: {
                    binder = new SecurityCenterImpl();
                    break;
                }
                case BINDER_COMPUTE: {
                    binder = new ComputeImpl();
                    break;
                }
                default:
                    break;
            }

            return binder;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return new BinderPoolImpl();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
