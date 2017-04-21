package com.example.saber.ipctestmessage;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BookManagerService extends Service {
    private static final String TAG = "BMS";

    //支持并发读写，AIDL方法是在服务端的Binder线程池中执行的，多个客户端同时访问时，会存在多线程，CopyOnWriteArrayList进行自动的线程同步
    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<>();

    //在这个Boolean值的变化的时候不允许在之间插入，保持操作的原子性,用于多线程
    private AtomicBoolean mIsServiceDestotyed = new AtomicBoolean(false);

    //RemoteCallbackList是系统专门提供的用于删除跨进程listener的接口。是一个Map  key是IBinder类型，value是callback（对象不同，但底层的Binder是同一个）
    private RemoteCallbackList<IOnNewBookArrivedListener> mListenerList = new RemoteCallbackList<>();
    /**
     * aidl自动生成的Java类IBookManager.Stub，服务端Binder执行的方法
     */
    private Binder mBinder = new IBookManager.Stub() {
        @Override
        public List<Book> getBookList() throws RemoteException {
            SystemClock.sleep(10000);
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.register(listener);
            Log.i(TAG,"register listener number:"+mListenerList.beginBroadcast());
        }

        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
           mListenerList.unregister(listener);
            Log.i(TAG,"unregister listener number:"+mListenerList.beginBroadcast());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book(1,"Android"));
        mBookList.add(new Book(2,"Ios"));
        new Thread(new ServiceWorker()).start();
    }

    public boolean onTransact(){
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //进行权限验证
        int check = checkCallingOrSelfPermission("com.example.saber.ipctestmessage.permission.ACCESS_BOOK_SERVICE");
        if(check == PackageManager.PERMISSION_DENIED){//验证不通过
            return null;
        }
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
        final int N = mListenerList.beginBroadcast();
        for(int i=0;i<N;i++){
            IOnNewBookArrivedListener listener = mListenerList.getBroadcastItem(i);
            if(listener != null){
                try {
                    listener.onNewBookArrived(book);//服务端回调，若此方法在客户端比较耗时，则此方法应该开工作线程运行。
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
            mListenerList.finishBroadcast();

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
