package com.atguigu.nio;

import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 一.通道（Channel）：用于源节点与目标节点得连接。在JAVA NIO中负责缓冲区数据的传输。Channel本身不存储数据，因此需要配合缓冲区进行传输.
 * 二.通道的主要实现类
 * java.nio.channels.Channel 接口：
 * //操作本地文件
 *      --FileChannel
 * //网络数据传输
 *      --SocketChannel
 *      --ServerSocketChannel
 *      --DatagramChannel
 * 三.获取通道
 * 1.java 针对支持通道的类提供了getChannel()方法
 *      本地IO：
 *      FileInputStream/FileOutPutStream
 *      RandomAccessFile
 *      网络IO：
 *      Socket
 *      ServerSocket
 *      DatagramSocket
 * 2.在JDK 1.7 中的NIO.2 针对各个通道提供了静态方法 open()
 * 3.在JDK 1.7 中的NIO.2 的Files 工具类的newByteChannel()
 *
 * 四.通道之间的数据传输
 * transferFrom()
 * transferTo()
 *
 * 五.分散(Scatter)与聚集(Gather)
 * 分散读取(Scattering Reads)：将通道中的数据分散到多个缓冲区中
 * 聚集写入(Gathering Writes)：将多个缓冲区中的数据聚集到通道中
 *
 * 六. 字符集：Charset
 * 编码：字符串->字节数组
 * 解码：字节数组->字符串
 */
public class TestChannel {
    @Test
    public void test6() throws IOException {
        Charset cs1 = Charset.forName("GBK");
        //获取编码器
        CharsetEncoder ce = cs1.newEncoder();
        //获取解码器
        CharsetDecoder cd = cs1.newDecoder();

        CharBuffer cBuf = CharBuffer.allocate(1024);
        cBuf.put("张三");
        cBuf.flip();
        //编码
        ByteBuffer bBuf = ce.encode(cBuf);
        for (int i = 0; i < bBuf.limit(); i++) {
            System.out.println(bBuf.get());
        }
        //解码
        bBuf.flip();
        CharBuffer cBuf2 = cd.decode(bBuf);
        System.out.println(cBuf2.toString());

        System.out.println("*************************");
        Charset cs2 = Charset.forName("GBK");
        bBuf.flip();
        CharBuffer cBuf3 = cs2.decode(bBuf);
        System.out.println(cBuf3.toString());
    }

    //字符集
    @Test
    public void test5(){
        Map<String, Charset> map = Charset.availableCharsets();

        Set<Entry<String, Charset>> set = map.entrySet();

        for (Entry<String, Charset> entry : set) {
            System.out.println(entry.getKey()+"="+entry.getValue());
        }
    }

    //分散和聚集
    @Test
    public void test4() throws IOException {
        RandomAccessFile raf1 = new RandomAccessFile("1.txt","rw");
        //1.获取通道
        FileChannel channel = raf1.getChannel();
        //2.分配指定大小的缓冲区
        ByteBuffer buf1 = ByteBuffer.allocate(100);
        ByteBuffer buf2 = ByteBuffer.allocate(1024);
        //3.分散读取
        ByteBuffer[] bufs = {buf1,buf2};
        channel.read(bufs);

        for (ByteBuffer buf : bufs) {
            buf.flip();
        }
        System.out.println(new String(bufs[0].array(),0,bufs[0].limit()));
        System.out.println("--------------------------");
        System.out.println(new String(bufs[1].array(),0,bufs[1].limit()));
        //4.聚集写入
        RandomAccessFile raf2 = new RandomAccessFile("2.txt","rw");
        FileChannel channel2 = raf2.getChannel();
        channel2.write(bufs);
    }

    //通道之间的数据传输(非直接缓冲区)
    @Test
    public void test3() throws IOException {
        FileChannel inchannel = FileChannel.open(Paths.get("1.jpg"), StandardOpenOption.READ);
        FileChannel outchannel = FileChannel.open(Paths.get("3.jpg"), StandardOpenOption.WRITE,StandardOpenOption.READ,StandardOpenOption.CREATE_NEW);

        //inchannel.transferTo(0,inchannel.size(),outchannel);
        outchannel.transferFrom(inchannel,0,inchannel.size());

        inchannel.close();
        outchannel.close();
    }

    //2.使用直接缓冲区完成文件的复制(内存映射文件)
    @Test
    public void test2() throws IOException {
        long start = System.currentTimeMillis();

        FileChannel inchannel = FileChannel.open(Paths.get("1.jpg"), StandardOpenOption.READ);
        FileChannel outchannel = FileChannel.open(Paths.get("3.jpg"), StandardOpenOption.WRITE,StandardOpenOption.READ,StandardOpenOption.CREATE_NEW);
        //内存映射文件
        MappedByteBuffer inMappedBuf = inchannel.map(MapMode.READ_ONLY, 0, inchannel.size());
        MappedByteBuffer outMappedBuf = outchannel.map(MapMode.READ_WRITE,0,inchannel.size());

        //直接对缓冲区进行数据的读写操作
        byte[] dst = new byte[inMappedBuf.limit()];
        inMappedBuf.get(dst);
        outMappedBuf.put(dst);

        inchannel.close();
        outchannel.close();
        long end = System.currentTimeMillis();
        System.out.println(start-end);
    }

    //1.利用通道完成文件的复制(非直接缓冲区)
    @Test
    public void test1(){
        long start = System.currentTimeMillis();
        FileInputStream fis = null;
        FileInputStream fos = null;
        //获取通道
        FileChannel inchannel = null;
        FileChannel outchannel = null;
        try {
            fis = new FileInputStream("1.jpg");
            fos = new FileInputStream("2.jpg");

            inchannel = fis.getChannel();
            outchannel = fos.getChannel();
            //分配指定大小的缓冲区
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            //将通道中的数据存入缓冲区中
            while (inchannel.read(buffer) != -1){
                //切换读取数据的模式
                buffer.flip();
                outchannel.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (outchannel != null){
                try {
                    outchannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inchannel != null){
                try {
                    inchannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println(start-end);
    }
}
