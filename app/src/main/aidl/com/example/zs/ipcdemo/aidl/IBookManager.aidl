package com.example.zs.ipcdemo.aidl;

import com.example.zs.ipcdemo.aidl.Book;
import com.example.zs.ipcdemo.aidl.IOnNewBookArrivedListener;

interface IBookManager{
    List<Book> getBookList();
    void addBook(in Book book);
    void registerListener(IOnNewBookArrivedListener listener);
    void unregisterListener(IOnNewBookArrivedListener listener);
}