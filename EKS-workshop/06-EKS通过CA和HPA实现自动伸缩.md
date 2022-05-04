## 04. EKS集群通过HPA和CA实现自动伸缩

部署MetricsServer

```
[root@ip-10-0-5-63 01]# kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
serviceaccount/metrics-server created
clusterrole.rbac.authorization.k8s.io/system:aggregated-metrics-reader created
clusterrole.rbac.authorization.k8s.io/system:metrics-server created
rolebinding.rbac.authorization.k8s.io/metrics-server-auth-reader created
clusterrolebinding.rbac.authorization.k8s.io/metrics-server:system:auth-delegator created
clusterrolebinding.rbac.authorization.k8s.io/system:metrics-server created
service/metrics-server created
deployment.apps/metrics-server created
apiservice.apiregistration.k8s.io/v1beta1.metrics.k8s.io created
```

验证MetricsServer

```
[root@ip-10-0-5-63 01]# kubectl get deployment metrics-server -n kube-system
NAME             READY   UP-TO-DATE   AVAILABLE   AGE
metrics-server   1/1     1            1           2m9s
```

安装 HPA sample application php-apache

```
[root@ip-10-0-5-63 01]# kubectl apply -f https://k8s.io/examples/application/php-apache.yaml
deployment.apps/php-apache created
service/php-apache created
[root@ip-10-0-5-63 01]# kubectl autoscale deployment php-apache --cpu-percent=30 --min=1 --max=5
horizontalpodautoscaler.autoscaling/php-apache autoscaled
[root@ip-10-0-5-63 01]# kubectl get hpa
NAME         REFERENCE               TARGETS         MINPODS   MAXPODS   REPLICAS   AGE
php-apache   Deployment/php-apache   <unknown>/30%   1         5         0          3s
```

开启Load-Generator

```
[root@ip-10-0-5-63 01]# kubectl run -it --rm load-generator --image=busybox /bin/sh
If you don't see a command prompt, try pressing enter.
/ # while true; do wget -q -O- http://php-apache.default.svc.cluster.local; done
OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!OK!
```

持续观察hba负载变化状态

```
[root@ip-10-0-5-63 ~]# watch kubectl get hpa
Every 2.0s: kubectl get hpa                                                                                                   Sun Apr 10 10:28:51 2022

NAME         REFERENCE               TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
php-apache   Deployment/php-apache   58%/30%   1         5         5          7m56s
```

确认php-apache自动扩展

```
[root@ip-10-0-5-63 ~]# kubectl get deployment php-apache
NAME         READY   UP-TO-DATE   AVAILABLE   AGE
php-apache   5/5     5            5           7m50s
```

创建如下IAM Policy Json文档

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "autoscaling:DescribeAutoScalingGroups",
                "autoscaling:DescribeAutoScalingInstances",
                "autoscaling:DescribeLaunchConfigurations",
                "autoscaling:DescribeTags",
                "autoscaling:SetDesiredCapacity",
                "autoscaling:TerminateInstanceInAutoScalingGroup",
                "ec2:DescribeLaunchTemplateVersions"
            ],
            "Resource": "*",
            "Effect": "Allow"
        }
    ]
}
```

创建IAM Policy

```
aws iam create-policy \
    --policy-name AmazonEKSClusterAutoscalerPolicy \
    --policy-document file://cluster-autoscaler-policy.json    
{
    "Policy": {
        "PolicyName": "AmazonEKSClusterAutoscalerPolicy",
        "PermissionsBoundaryUsageCount": 0,
        "CreateDate": "2022-04-10T11:13:43Z",
        "AttachmentCount": 0,
        "IsAttachable": true,
        "PolicyId": "ANPA3YABCDXXXXDSS5QLU",
        "DefaultVersionId": "v1",
        "Path": "/",
        "Arn": "arn:aws:iam::8082xxxx3800:policy/AmazonEKSClusterAutoscalerPolicy",
        "UpdateDate": "2022-04-10T11:13:43Z"
    }
}
```

创建Service Account

```
  eksctl create iamserviceaccount \
  --cluster=eks-121 \
  --namespace=kube-system \
  --name=cluster-autoscaler \
  --attach-policy-arn=arn:aws:iam::808242303800:policy/AmazonEKSClusterAutoscalerPolicy \
  --override-existing-serviceaccounts \
  --approve
```



下载cluster-autoscaler-autodiscover.yaml 文件，并修改YOUR CLUSTER NAME为集群名

```
curl -o cluster-autoscaler-autodiscover.yaml https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml
```

部署

```
cluster-autoscaler-autodiscover.yaml  cluster-autoscaler-policy.json
[root@ip-10-0-5-63 06]# vi cluster-autoscaler-autodiscover.yaml
[root@ip-10-0-5-63 06]# vi cluster-autoscaler-autodiscover.yaml
[root@ip-10-0-5-63 06]# kubectl apply -f cluster-autoscaler-autodiscover.yaml
serviceaccount/cluster-autoscaler created
clusterrole.rbac.authorization.k8s.io/cluster-autoscaler created
role.rbac.authorization.k8s.io/cluster-autoscaler created
clusterrolebinding.rbac.authorization.k8s.io/cluster-autoscaler created
rolebinding.rbac.authorization.k8s.io/cluster-autoscaler created
deployment.apps/cluster-autoscaler created
```

通过下面的Link访问dashboard

```
nginx-to-scaleout-6fcd49fb84-24s4g   1/1     Running   0          2m5s
nginx-to-scaleout-6fcd49fb84-4z9ch   1/1     Running   0          67s
nginx-to-scaleout-6fcd49fb84-6d6kq   1/1     Running   0          2m5s
nginx-to-scaleout-6fcd49fb84-6gjbq   1/1     Running   0          2m28s
nginx-to-scaleout-6fcd49fb84-85bmr   1/1     Running   0          67s
nginx-to-scaleout-6fcd49fb84-b9f2b   1/1     Running   0          2m5s
nginx-to-scaleout-6fcd49fb84-cd67b   1/1     Running   0          2m5s
nginx-to-scaleout-6fcd49fb84-cdlwf   1/1     Running   0          2m5s
nginx-to-scaleout-6fcd49fb84-d8hwd   0/1     Pending   0          67s
nginx-to-scaleout-6fcd49fb84-fd7sn   1/1     Running   0          67s
nginx-to-scaleout-6fcd49fb84-j5hpv   1/1     Running   0          67s
nginx-to-scaleout-6fcd49fb84-j79tv   1/1     Running   0          2m4s
nginx-to-scaleout-6fcd49fb84-jmcxl   0/1     Pending   0          67s
nginx-to-scaleout-6fcd49fb84-ntkqh   1/1     Running   0          2m5s
nginx-to-scaleout-6fcd49fb84-spwkf   1/1     Running   0          67s
nginx-to-scaleout-6fcd49fb84-swb5b   1/1     Running   0          67s
nginx-to-scaleout-6fcd49fb84-sxfg8   1/1     Running   0          67s
nginx-to-scaleout-6fcd49fb84-tbsv2   1/1     Running   0          2m5s
nginx-to-scaleout-6fcd49fb84-x6btl   1/1     Running   0          67s
nginx-to-scaleout-6fcd49fb84-x9mm5   1/1     Running   0          2m4s
php-apache-d4cf67d68-ssvnx           1/1     Running   0          90m
nginx-to-scaleout-6fcd49fb84-d8hwd   0/1     Pending   0          70s
nginx-to-scaleout-6fcd49fb84-jmcxl   0/1     Pending   0          70s
```

通过下面的命令获取登陆token

```
[root@ip-10-0-5-63 06]# kubectl apply -f nginx-to-scale.yaml
deployment.apps/nginx-to-scaleout created
[root@ip-10-0-5-63 06]#  kubectl scale --replicas=10 deployment/nginx-to-scaleout
deployment.apps/nginx-to-scaleout scaled
```

发现

```
NAME                           STATUS   ROLES    AGE    VERSION
ip-10-0-143-12.ec2.internal    Ready    <none>   7d6h   v1.21.5-eks-9017834
ip-10-0-144-161.ec2.internal   Ready    <none>   7d6h   v1.21.5-eks-9017834
ip-10-0-146-116.ec2.internal   Ready    <none>   84s    v1.21.5-eks-9017834
ip-10-0-166-161.ec2.internal   Ready    <none>   7d6h   v1.21.5-eks-9017834
ip-10-0-24-66.ec2.internal     Ready    <none>   7d6h   v1.21.5-eks-9017834
ip-10-0-35-177.ec2.internal    Ready    <none>   7d6h   v1.21.5-eks-9017834
ip-10-0-9-144.ec2.internal     Ready    <none>   7d6h   v1.21.5-eks-9017834
```

