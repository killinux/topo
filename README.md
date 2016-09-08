# topo
a topology for openstack 
#
put this into tomcat 
#
topo/haotopology/the-graph-editor/single.html
#
假设我的本地内网ip是192.168.139.* 我的aws的服务器是52.78.83.*，
#
拓扑图在aws上，openstack在本地，
#
在本地开个ssh隧道，把本地的mysql的3306端口映射到aws的2222端口，
#
ssh -N -f -R 2222:localhost:3306 52.78.83.*
#
这样aws的127.0.0.1的2222就是我本地openstack的mysql，同理，数据库太重要我们可以暴露其他的keystone等端口，demo就先这样
#
为了解决 远程ssh远程连mysql可能会连不上的问题 ，mysql 配置文件修改
/etc/my.conf
#
[mysqld]
#
skip-name-resolve
#
\#bind-address = 192.168.139.* #
#
在aws上测试 mysql -uroot -p -h127.0.0.1 -P2222
#
如果能连到本地就是成功了

