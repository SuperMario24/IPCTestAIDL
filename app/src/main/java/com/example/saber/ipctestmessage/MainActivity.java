package com.example.saber.ipctestmessage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int MESSAGE_NEW_BOOK_ARRIVED = 1;//msg.what

    private IBookManager mRemoteBookManager;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_NEW_BOOK_ARRIVED:
                    Log.d(TAG,"receive new Book :"+msg.obj);
                    break;
            }
        }
    };

    private Messenger mService;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IBookManager bookManager = IBookManager.Stub.asInterface(service);
            try {
//                List<Book> list = bookManager.getBookList();
//                Log.i(TAG,"query book list,list type:"+list.getClass().getCanonicalName());
//                Log.i(TAG,"query book list:"+list.toString());

//                List<Book> list = bookManager.getBookList();
//                Log.i(TAG,"query book list:"+list.toString());
//                Book book = new Book(3,"Android开发艺术探索");
//                bookManager.addBook(book);
//                Log.i(TAG,"add book:"+book);
//                List<Book> newList = bookManager.getBookList();
//                Log.i(TAG,"query book list:"+newList.toString());

                mRemoteBookManager = bookManager;
                List<Book> list = bookManager.getBookList();
                Log.i(TAG,"query book list,list type:"+list.getClass().getCanonicalName());
                Log.i(TAG,"query book list:"+list.toString());
                Book book = new Book(3,"Android进阶");
                bookManager.addBook(book);
                Log.i(TAG,"add book:"+book);
                List<Book> newList = bookManager.getBookList();
                Log.i(TAG,"query book list:"+newList.toString());
                bookManager.registerListener(mOnNewBookArrivedListener);//完成绑定后，注册接口，setListener
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private IOnNewBookArrivedListener mOnNewBookArrivedListener = new IOnNewBookArrivedListener.Stub() {
        @Override
        public void onNewBookArrived(Book book) throws RemoteException {
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED,book).sendToTarget();//获取到新书后回调该方法，子线程通知主线程更新UI
        }
    };







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this,BookManagerService.class);
        bindService(intent,connection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onDestroy() {
        if(mRemoteBookManager != null&&mRemoteBookManager.asBinder().isBinderAlive()){

            try {
                Log.i(TAG,"unregister listener:"+mOnNewBookArrivedListener);
                mRemoteBookManager.unregisterListener(mOnNewBookArrivedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(connection);
        super.onDestroy();
    }
}
