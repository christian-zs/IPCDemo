// IMyAidlInterface.aidl
package com.example.zs.ipcdemo.aidl;

// Declare any non-default types here with import statements

interface IBinderPool {
   // 传入绑定 code 返回对应的 Binder 对象，
   IBinder queryBinder(int code);

}
