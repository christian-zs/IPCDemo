package com.example.zs.ipcdemo.aidl;

import com.example.zs.ipcdemo.aidl.Book;

interface IOnNewBookArrivedListener {
    void onNewBookArrived(in Book newBook);
}
