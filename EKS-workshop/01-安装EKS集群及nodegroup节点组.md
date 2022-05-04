## 1. 安装AWS CLI

根据AWS官方文档，安装CLI工具，建议安装最新版v2，参考链接

https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html

## 2. 安装Kubectl

根据AWS官方文档，安装kubectl工具，需注意kubectl工具版本需要和所需创建并管理的集群版本保持一致

https://docs.aws.amazon.com/eks/latest/userguide/install-kubectl.html

## 3. 安装eksctl

根据AWS官方文档，安装eksctl工具

https://docs.aws.amazon.com/eks/latest/userguide/eksctl.html#installing-eksctl

## 4. 创建EKS集群

建议以yaml文件创建并管理集群，可参考resource目录中cluster.yaml文件创建集群

eksctl create cluster -f cluster.ymal

```
root@ip-10-0-5-63 01]# eksctl create cluster -f cluster.ymal
2022-04-01 12:32:05 [ℹ]  eksctl version 0.90.0
2022-04-01 12:32:05 [ℹ]  using region us-east-1
2022-04-01 12:32:05 [✔]  using existing VPC (vpc-0bc1e2d68b76fc8eb) and subnets (private:map[us-east-1a:{subnet-0674477df07b3dd46 us-east-1a 10.0.128.0/20} us-east-1b:{subnet-00f5c86ac2f784159 us-east-1b 10.0.144.0/20} us-east-1c:{subnet-0d060a2eed4fd2106 us-east-1c 10.0.160.0/20}] public:map[])
2022-04-01 12:32:05 [!]  custom VPC/subnets will be used; if resulting cluster doesn't function as expected, make sure to review the configuration of VPC/subnets
2022-04-01 12:32:05 [ℹ]  using Kubernetes version 1.21
2022-04-01 12:32:05 [ℹ]  creating EKS cluster "eks-121" in "us-east-1" region with
2022-04-01 12:32:05 [ℹ]  will create a CloudFormation stack for cluster itself and 0 nodegroup stack(s)
2022-04-01 12:32:05 [ℹ]  will create a CloudFormation stack for cluster itself and 0 managed nodegroup stack(s)
2022-04-01 12:32:05 [ℹ]  if you encounter any issues, check CloudFormation console or try 'eksctl utils describe-stacks --region=us-east-1 --cluster=eks-121'
2022-04-01 12:32:05 [ℹ]  Kubernetes API endpoint access will use default of {publicAccess=true, privateAccess=false} for cluster "eks-121" in "us-east-1"
2022-04-01 12:32:05 [ℹ]  CloudWatch logging will not be enabled for cluster "eks-121" in "us-east-1"
2022-04-01 12:32:05 [ℹ]  you can enable it with 'eksctl utils update-cluster-logging --enable-types={SPECIFY-YOUR-LOG-TYPES-HERE (e.g. all)} --region=us-east-1 --cluster=eks-121'
2022-04-01 12:32:05 [ℹ]
2 sequential tasks: { create cluster control plane "eks-121", wait for control plane to become ready
```

## 5. 创建nodegroup节点组

建议以yaml文件创建并管理nodegroup节点组，可参考resource目录中ng-basic.yaml文件创建基本节点组

eksctl create nodegroup -f ng-basic.yaml

```
root@ip-10-0-5-63 01]# eksctl create nodegroup -f ng-basic.yaml
2022-04-01 13:15:50 [ℹ]  eksctl version 0.90.0
2022-04-01 13:15:50 [ℹ]  using region us-east-1
2022-04-01 13:15:50 [ℹ]  will use version 1.21 for new nodegroup(s) based on control plane version
2022-04-01 13:15:51 [ℹ]  nodegroup "managed-ng-basic" will use "" [AmazonLinux2/1.21]
2022-04-01 13:15:51 [ℹ]  1 nodegroup (managed-ng-basic) was included (based on the include/exclude rules)
2022-04-01 13:15:51 [ℹ]  will create a CloudFormation stack for each of 1 managed nodegroups in cluster "eks-121"
2022-04-01 13:15:51 [ℹ]
2 sequential tasks: { fix cluster compatibility, 1 task: { 1 task: { create managed nodegroup "managed-ng-basic" } }
}
2022-04-01 13:15:51 [ℹ]  checking cluster stack for missing resources
2022-04-01 13:15:51 [ℹ]  cluster stack has all required resources
2022-04-01 13:15:51 [ℹ]  building managed nodegroup stack "eksctl-eks-121-nodegroup-managed-ng-basic"
2022-04-01 13:15:52 [ℹ]  deploying stack "eksctl-eks-121-nodegroup-managed-ng-basic"
```

## 6. 查看nodegroup和节点

查看节点组eksctl get nodegroup --cluster eks-121

```
[root@ip-10-0-5-63 01]# eksctl get nodegroup --cluster eks-121
2022-04-02 09:33:33 [ℹ]  eksctl version 0.90.0
2022-04-02 09:33:33 [ℹ]  using region us-east-1
CLUSTER NODEGROUP               STATUS  CREATED                 MIN SIZE        MAX SIZE        DESIRED CAPACITY        INSTANCE TYPE   IMAGE ID        ASG NAME                                                     TYPE
eks-121 managed-ng-basic        ACTIVE  2022-04-01T13:16:20Z    3               10              3                       m5.large        AL2_x86_64      eks-managed-ng-basic-6cbff2a2-7e1d-22a8-d0ec-015b5120c0c5    managed
[root@ip-10-0-5-63 01]# kubectl get node
NAME                           STATUS   ROLES    AGE   VERSION
ip-10-0-133-250.ec2.internal   Ready    <none>   20h   v1.21.5-eks-9017834
ip-10-0-156-32.ec2.internal    Ready    <none>   20h   v1.21.5-eks-9017834
ip-10-0-174-83.ec2.internal    Ready    <none>   20h   v1.21.5-eks-9017834
```

## 7. 查看nodegroup和节点

登陆AWS控制台，在EC2中查看EKS工作节点，如下图所示

![](https://github.com/xipan79/EKS-workshop/raw/main/EKS-workshop/screenprint/01/1.png)

## 8. 创建并关联OIDC Provider

要将 IAM 角色用于Service Account，集群必须创建并关联 IAM OIDC Provider

```
[root@ip-10-0-5-63 01]# eksctl utils associate-iam-oidc-provider \
>     --region us-east-1 \
>     --cluster eks-121 \
>     --approve
2022-04-02 10:19:44 [ℹ]  eksctl version 0.90.0
2022-04-02 10:19:44 [ℹ]  using region us-east-1
2022-04-02 10:19:44 [ℹ]  will create IAM Open ID Connect provider for cluster "eks-121" in "us-east-1"
2022-04-02 10:19:44 [✔]  created IAM Open ID Connect provider for cluster "eks-121" in "us-east-1"
```

