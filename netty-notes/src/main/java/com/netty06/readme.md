1,本次系统调优参考的连接
https://colobu.com/2014/09/18/linux-tcpip-tuning/  
https://blog.csdn.net/duan19056/article/details/51210110  

http://blog.chinaunix.net/uid-29081804-id-3830203.html  

系统的修改：
1./etc/sysctl.conf
#my params
net.ipv4.tcp_syn_retries = 1  
net.ipv4.tcp_synack_retries = 1  
net.ipv4.tcp_keepalive_time = 600  
net.ipv4.tcp_keepalive_probes = 3  
net.ipv4.tcp_keepalive_intvl =15  
net.ipv4.tcp_retries2 = 5  
net.ipv4.tcp_fin_timeout = 2  
net.ipv4.tcp_max_tw_buckets = 36000  
net.ipv4.tcp_tw_recycle = 1  
net.ipv4.tcp_tw_reuse = 1  
net.ipv4.tcp_max_orphans = 32768  
net.ipv4.tcp_syncookies = 1  
net.ipv4.tcp_max_syn_backlog = 16384  
net.ipv4.tcp_wmem = 8192 131072 16777216  
net.ipv4.tcp_rmem = 32768 131072 16777216  
net.ipv4.tcp_mem = 786432 1048576 1572864  
net.ipv4.ip_local_port_range = 2048 65000  
net.core.netdev_max_backlog = 16384  


2.在系统文件/etc/security/limits.conf中修改这个数量限制，  

在文件中加入内容：这个修改后连接确实上去几千。但是由于是虚拟机，且单核四线程，内存4G.后面连接达到6400+就再也不行了。  

* soft nofile 65536 
* hard nofile 65536  



查看当前TCP连接的状态和对应的连接数量:  
netstat -n | awk '/^tcp/ {++S[$NF]} END {for(a in S) print a, S[a]}'  

LAST_ACK 16  
SYN_RECV 348  
ESTABLISHED 70  
FIN_WAIT1 229  
FIN_WAIT2 30  
CLOSING 33  
TIME_WAIT 18098  

调整前：查看端口连接数  
netstat -an|grep 9091|wc -l  
4163
  
调整后：最大连接数  
netstat -an|grep 9091|wc -l  
6343  

查看当前系统打开的文件数量  

lsof | wc -l  
watch "lsof | wc -l"  

查看某一进程的打开文件数量  
lsof -p pid | wc -l  
OR
ls /proc/[pid]/fd | wc -  

系统总的文件句柄数：  
/proc/sys/fs/file-max  
整个系统目前使用的文件句柄数
/proc/sys/fs/file-nr  


###使用阿里云服务器压测netty连接 20200313  

配置： 2核心cpu 8GB内存  

##### 配置阿里云服务对外端口映射 为9091
该配置直接登入阿里云配置即可。

##### 开放centos7 9091防火墙端口
添加  
`firewall-cmd --zone=public --add-port=9091/tcp --permanent    （--permanent永久生效，没有此参数重启后失效）`

重新载入使它生效  
`firewall-cmd --reload`

查看所有打开的端口：   
`firewall-cmd --zone=public --list-ports`  

`19002/tcp 19003/tcp 19004/tcp 9091/tcp`  

删除  
`firewall-cmd --zone= public --remove-port=80/tcp --permanent` 

#### 阿里云服务器默认配置

1.文件句柄限制： ulimit -a  

```
core file size          (blocks, -c) 0
data seg size           (kbytes, -d) unlimited
scheduling priority             (-e) 0
file size               (blocks, -f) unlimited
pending signals                 (-i) 31204
max locked memory       (kbytes, -l) 64
max memory size         (kbytes, -m) unlimited
open files                      (-n) 65535  //确保大小够用 默认1024
pipe size            (512 bytes, -p) 8
POSIX message queues     (bytes, -q) 819200
real-time priority              (-r) 0
stack size              (kbytes, -s) 8192
cpu time               (seconds, -t) unlimited
max user processes              (-u) 31204
virtual memory          (kbytes, -v) unlimited
file locks                      (-x) unlimited
```

2.cat /etc/security/limits.conf  

```
# End of file
root soft nofile 65535
root hard nofile 65535
* soft nofile 65535
* hard nofile 65535

```

3.服务端链接达到：
`netstat -an|grep 9091|wc -l`

8000+  

  
客户端机器内存不足 最终nettyClient报错： 

```
Caused by: java.net.SocketException: No buffer space available (maximum connections reached?): connect
```

服务端cpu 内存使用情况：free -m    

```
              total        used        free      shared  buff/cache   available
Mem:           7821        4956         895           0        1969        2547
Swap:             0           0           0

```