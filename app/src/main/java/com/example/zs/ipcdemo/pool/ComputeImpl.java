package com.example.zs.ipcdemo.pool;

import android.os.IBinder;
import android.os.RemoteException;

import com.example.zs.ipcdemo.aidl.ICompute;

public class ComputeImpl extends ICompute.Stub {
    @Override
    public int add(int a, int b) throws RemoteException {
        return a + b;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
