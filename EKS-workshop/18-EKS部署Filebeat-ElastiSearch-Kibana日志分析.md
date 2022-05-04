

# 1. 安装Filebeat

Beats是一个轻量级日志采集器，其实Beats家族有6个成员，filebeat是Beats中的一员。早期的ELK架构中使用Logstash收集、解析日志，但是Logstash对内存、cpu、io等资源消耗比较高。相比Logstash，Beats所占系统的CPU和内存几乎可以忽略不计。Filebeat是用于转发和集中日志数据的轻量级传送工具。Filebeat监视您指定的日志文件或位置，收集日志事件，并将它们转发到Elasticsearch或 Logstash进行索引。

首先下载filebeat yaml文件，然后直接部署

```
curl -L -O https://raw.githubusercontent.com/elastic/beats/7.10/deploy/kubernetes/filebeat-kubernetes.yaml
kubectl apply -f filebeat-kubenetes.yaml
```

![](https://tva1.sinaimg.cn/large/0081Kckwly1glhvw5e4vfj31ja0j8jwk.jpg)

拉取镜像的时间会比较长，大概10分钟左右。耐心等待后再次确认filebeat安装状态，发现pod已经正常运行

![](https://tva1.sinaimg.cn/large/0081Kckwly1glhvuo6vh4j31f80cqwhx.jpg)