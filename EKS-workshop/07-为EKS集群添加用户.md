# 1. 添加IAM用户

首先，在IAM中添加一个用户，比如eks-test。如果只需要以命令行管理EKS集群的话，可以只勾选Programmatic access。如果需要控制台登陆，则Management Console Access也需要勾选

![](https://tva1.sinaimg.cn/large/0081Kckwly1glogrv5ttkj31j40litcq.jpg)



新创建的用户没有访问EKS集群的权限，我们按如下步骤在aws configure中输入eks-test用户的access key和secret key，并尝试访问集群，发现未登陆报错提示

![](https://tva1.sinaimg.cn/large/0081Kckwly1gloh07mctkj313u0dg412.jpg)



## 2. 在aws-auth中配置权限

要向 eks-test用户 授予对集群的访问权限，请将 mapUsers部分添加到您的 aws-auth.yaml文件中。首选需要通过aws configure将当前用户切换回创建EKS集群的管理员，拥有对集群的完全控制权，然后配置aws-auth.yaml文件。关于这部分权限设置，可参考AWS博客：

https://aws.amazon.com/cn/premiumsupport/knowledge-center/amazon-eks-cluster-access/

```
kubectl edit -n kube-system configmap/aws-auth
```

```
kubectl describe configmap -n kube-system aws-auth
```

```
Name:         aws-auth
Namespace:    kube-system
Labels:       <none>
Annotations:  <none>

Data
====
mapRoles:
----
- groups:
  - system:bootstrappers
  - system:nodes
  rolearn: arn:aws-cn:iam::685200526309:role/eksctl-eksworkshop-nodegroup-ng-b-NodeInstanceRole-1T2FMXXXXXXXX
  username: system:node:{{EC2PrivateDNSName}}
- groups:
  - system:masters
  rolearn: arn:aws-cn:iam::685200526309:role/eksctl-eksworkshop-nodegroup-ng-b-NodeInstanceRole-1T2FMXXXXXXXX
  username: eks-testuser

mapUsers:
----
- userarn: arn:aws-cn:iam::685200526309:user/eks-test
  username: eks-test
  groups:
- system:masters
```

此时我们通过aws configure切换回eks-test用户，并再次尝试访问集群，发现K8S Forbidden授权错误

![](https://tva1.sinaimg.cn/large/0081Kckwly1glohcii6wyj31wk08o0up.jpg)



## 3. 在K8S RBAC中配置权限

在前两步中我们创建了IAM用户，并通过编辑aws-auth对其授权访问EKS，但是最后我们还需要通过K8S自带的权限管理系统RBAC，通过RoleBinding或者ClusterRoleBinding，对IAM用户进行权限设置。关于这部分设置可参考K8S官方文档

https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/

我们先通过aws configure切换回到EKS管理员，然后输入如下命令，其中eks-test-cluster-admin-binding是自行取的命名，cluster-admin是K8S自带的超级管理员权限，eks-test是之前在IAM中创建的用户

```
kubectl create clusterrolebinding eks-test-cluster-admin-binding --clusterrole=cluster-admin --user=eks-test
```

![](https://tva1.sinaimg.cn/large/0081Kckwly1glohl1bid8j31y608u40j.jpg)

然后我们再通过aws configure切换回eks-test用户，发现其已经具备集群管理权限

![](https://tva1.sinaimg.cn/large/0081Kckwly1glohnftmjsj31fq0kiq7y.jpg)



如果访问集群的IAM用户同时还要求从控制台查看集群相关信息，可参考官网设置：https://docs.aws.amazon.com/eks/latest/userguide/add-user-role.html，比如，若需要查看整个集群的信息，可做如下操作：

```
curl -o eks-console-full-access.yaml https://s3.us-west-2.amazonaws.com/amazon-eks/docs/eks-console-full-access.yaml

kubectl apply -f eks-console-full-access.yaml
```

注2: 如果需要更细粒度的权限管控，比如某用于仅能对某namespace进行特定操作，请参考K8S官网进行设置。

