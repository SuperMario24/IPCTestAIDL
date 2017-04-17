// IBookManager.aidl
package com.example.saber.ipctestmessage;

import com.example.saber.ipctestmessage.Book;
import com.example.saber.ipctestmessage.IOnNewBookArrivedListener;

interface IBookManager {
    List<Book> getBookList();//获取服务器图书
    void addBook(in Book book);//添加图书
    void registerListener(IOnNewBookArrivedListener listener);//注册接口setListener
    void unregisterListener(IOnNewBookArrivedListener listener);//注销接口
}
