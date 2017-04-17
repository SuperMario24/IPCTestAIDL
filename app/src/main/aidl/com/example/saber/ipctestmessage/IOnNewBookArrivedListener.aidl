// IOnNewBookArrivedListener.aidl
package com.example.saber.ipctestmessage;

import com.example.saber.ipctestmessage.Book;

interface IOnNewBookArrivedListener {
   void onNewBookArrived(in Book book);//新书到的AIDL回调接口，普通接口没法在AIDL里使用
}
