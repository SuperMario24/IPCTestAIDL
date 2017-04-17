# IPCTestAIDL
AIDL 跨进程通信

1.实体类要实现Parcelable接口，写完后要新建实体类的aidl文件。
// Book.aidl
package com.example.saber.ipctestmessage;

parcelable Book;

2.aidl接口要手动导入实体类的包。
3.写完aidl文件后，make一下，自动生成java文件。
4.aidl中除了基本数据类型，其他类型必须标上方向，in,out或者inout。
5.回调接口要为aidl接口。aidl不能使用普通接口。
6.对象不能跨进程传输，是反序列化的，不是同一个对象。
