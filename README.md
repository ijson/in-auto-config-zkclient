
# 因github国内无法正常使用 本项目废弃,该项目被迁移至:https://gitee.com/ijson/in-auto-config-zkclient, 感谢各位支持

### 自动拉取配置客户端

>  本项目为 [in配置自动化服务端](https://github.com/ijson/in-auto-config) 服务的客户端,也可作为本地配置文件读取工具使用


### 使用模式

#### 读取本地配置项
> 指的是配置文件已经在本地,不需要从服务端动态的更新配置文件,只对本地文件的读取,此模式会自动关闭与zookeeper的心跳链接,推荐对本地文件修改度不高的项目使用

##### 本地文件读取使用指南


#### 引用方式

```
<dependency>
  <groupId>com.ijson.common</groupId>
  <artifactId>in-auto-config-zkclient</artifactId>
  <version>1.0.9</version>
</dependency>
```

#### 远程拉取服务端配置项
> 指的是需要从远程服务端获取配置更新,服务端配置文件修改后,自动下载到本地,达到不重启服务,即可修改本地配置文件的方案,此模式会与zookeeper建立心跳链接,推荐内网使用此模式

1. 配置本地VMOption参数为:

   | 配置|默认值|描述 |
   |---|---|---|
   |zk.enable |false|访问远程zookeeper服务器,可忽略|
   |zk.auth| in\:ijson|用 username\:password 字符串来产生一个MD5串|
   |zk.authType|digest|认证类型|
   |zk.basePath|in/config|存储位置|
   |config.url|http://config.ijson.com/in/config/api|zookeeper配置获取地址,可自行编写①|
   |process.profile|config.url中的profile,区分环境||
   |process.name|用于区分产品线||
   |custom.zk.server.url|config.url中地址重写||



### 附录
① 格式结构

http://config.ijson.com/in/config/api?profile=develop&name=demo
```
zookeeper.servers=115.29.102.69:2181 zookeeper.authenticationType=digest zookeeper.authentication=in:ijson zookeeper.basePath=/in/config
```


-Dprocess.profile=ceshi -Dzookeeper.servers=http://localhost:8080/in/config/api -Dprocess.name=demo -zk.enable=true
