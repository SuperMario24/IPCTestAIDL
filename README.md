# IPCTestAIDL
AIDL 跨进程通信

1.实体类要实现Parcelable接口，写完后要新建实体类的aidl文件。
// Book.aidl
package com.example.saber.ipctestmessage;

parcelable Book;

2.aidl接口要手动导入实体类的包。
3.写完aidl文件后，make一下，自动生成java文件。
4.aidl中除了基本数据类型，其他类型必须标上方向，in,out或者inout（in:客户端流向服务端）。
5.回调接口要为aidl接口。aidl不能使用普通接口，AIDL接口与普通接口的区别：AIDL接口只支持方法，不支持声明静态常量。
6.对象不能跨进程传输，是反序列化的，不是同一个对象。
7.CopyOnWriteArrayList-------支持并发读写的集合，自动实现线程同步，存在多个客户端同时访问的情况。
8.AIDL方法是运行在服务端的Binder线程池中的。
9.服务端回调客户端的方法时，运行在客户端的Binder线程池
10.RemoteCallbackList是系统专门提供的用于删除跨进程listener的接口。是一个Map  key是IBinder类型，value是callback（对象不同，但底层的Binder是同一个）
11.RemoteCallbackList的beginBroadcast和finishBroadcast必须配对使用。
12.RemoteCallbackList的遍历：
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
13.若服务端是耗时操作，客户端调用服务端的方法时，应开工作线程，服务端的方法本身就运行在Binder线程池中，不需要开工作线程。同时，服务端也是。
14.设置binder死亡代理和onServiceDisconnected的区别：onServiceDisconnected在客户端的UI线程中被回调，而binderDied在客户端的Binder线程池中被回调。
15. 在连接上service时设置：service.linkToDeath(mDeathRecipient,0);
IBinder.DeathRecipient mDeathRecipient = new...重写binderDied方法。重新连接（
            //先取消代理置空，再重绑
            mRemoteBookManager.asBinder().unlinkToDeath(mDeathRecipient,0);
            mRemoteBookManager = null;）
16.在这个Boolean值的变化的时候不允许在之间插入，保持操作的原子性,用于多线程
    private AtomicBoolean mIsServiceDestotyed = new AtomicBoolean(false);
17.AIDL的权限认证：1.在onBind中验证，验证失败返回null。2.在服务端的onTransact方法中验证。验证失败返回false。
