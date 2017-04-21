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
import android.view.View;
import android.widget.Button;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnGetBook;

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

                //给Binder设置死亡代理
                service.linkToDeath(mDeathRecipient,0);

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
            //TODO  当连接中断做的操作，此方法在客户端UI线程中执行，DeathRecipient死亡代理则是在客户端Binder线程池中执行。


        }
    };

    /**
     * 给Binder设置死亡代理
     */
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        /**
         * 连接中断时
         */
        @Override
        public void binderDied() {
            if(mRemoteBookManager == null){
                return;
            }
            //先取消代理置空，再重绑
            mRemoteBookManager.asBinder().unlinkToDeath(mDeathRecipient,0);
            mRemoteBookManager = null;
            //重新绑定远程Service
            Intent intent = new Intent(MainActivity.this,BookManagerService.class);
            bindService(intent,connection, Context.BIND_AUTO_CREATE);
        }
    };

    private IOnNewBookArrivedListener mOnNewBookArrivedListener = new IOnNewBookArrivedListener.Stub() {
        /**
         *此方法为服务端回调，运行在客户端的Binder的线程池中，是非UI线程，所有要用Handler更新UI
         */
        @Override
        public void onNewBookArrived(Book book) throws RemoteException {
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED,book).sendToTarget();//获取到新书后回调该方法，子线程通知主线程更新UI
        }
    };







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGetBook = (Button) findViewById(R.id.btn_get_book);

        Intent intent = new Intent(this,BookManagerService.class);
        bindService(intent,connection, Context.BIND_AUTO_CREATE);

        /**
         * 获取图书
         */
        btnGetBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mRemoteBookManager.getBookList();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        });
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
