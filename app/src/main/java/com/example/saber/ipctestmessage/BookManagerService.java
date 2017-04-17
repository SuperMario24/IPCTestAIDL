package com.example.saber.ipctestmessage;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BookManagerService extends Service {
    private static final String TAG = "BMS";

    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<>();

    //在这个Boolean值的变化的时候不允许在之间插入，保持操作的原子性,用于多线程
    private AtomicBoolean mIsServiceDestotyed = new AtomicBoolean(false);

    //支持并发读写，AIDL方法是在服务端的Binder线程池中执行的，多个客户端同时访问时，会存在多线程，CopyOnWriteArrayList进行自动的线程同步
    private CopyOnWriteArrayList<IOnNewBookArrivedListener> mListenerList = new CopyOnWriteArrayList<>();
    /**
     * aidl自动生成的Java类IBookManager.Stub，服务端Binder执行的方法
     */
    private Binder mBinder = new IBookManager.Stub() {
        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            if(!mListenerList.contains(listener)){
                mListenerList.add(listener);
            }else {
                Log.d(TAG,"alread exists");
            }
            Log.d(TAG,"registerListener,size:"+mListenerList.size());
        }

        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
            if(mListenerList.contains(listener)){
                mListenerList.remove(listener);
                Log.d(TAG,"unregister listener succeed");
            }else {
                Log.d(TAG,"not found,can not unregister");
            }
            Log.d(TAG,"unregisterListener,current size:"+mListenerList.size());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book(1,"Android"));
        mBookList.add(new Book(2,"Ios"));
        new Thread(new ServiceWorker()).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        mIsServiceDestotyed.set(true);
        super.onDestroy();
    }

    /**
     * 获取到新书后调用的方法，通知接口回调
     * @param book
     * @throws RemoteException
     */
    private void onNewBookArrived(Book book)throws RemoteException{
        mBookList.add(book);
        Log.d(TAG,"onNewBookArrived,notify listeners:"+mListenerList.size());
        for(int i=0;i<mListenerList.size();i++){
            IOnNewBookArrivedListener listener = mListenerList.get(i);
            Log.d(TAG,"onNewBookArrived,notify listener:"+listener);
            listener.onNewBookArrived(book);
        }
    };

    /**
     * 绑定服务之后，开启线程，每5秒获取一次新书数据，获取完成后调用onNewBookArrived方法，通知接口回调onNewBookArrived方法，在客户端执行
     */
    private class ServiceWorker implements Runnable{
        @Override
        public void run() {
            while(!mIsServiceDestotyed.get()){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int bookId = mBookList.size()+1;
                Book newBook = new Book(bookId,"new book#"+bookId);
                try {
                    onNewBookArrived(newBook);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }


            }
        }
    }
}
