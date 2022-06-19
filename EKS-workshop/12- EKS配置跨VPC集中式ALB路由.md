## 12. EKS配置跨VPC集中式ALB路由

在实际生产环境中，某些大型企业会专门规划一个DMZ VPC通公网，ALB/NAT/堡垒机等设备部署在DMZ中，业务VPC仅规划私有子网，所有EC2/EKS节点均运行在私有子网中，并通过TGW和DMZ VPC打通，因此我们需要部署ALB Ingress，以支持跨VPC的公网流量路由到后端EKS业务节点

#### 技术要点：

1. ALB VPC公有子网打上Load Balancer Controller需要的tag: https://aws.amazon.com/premiumsupport/knowledge-center/eks-vpc-subnet-discovery/
2. 由于AWS Load Balancer Controller与ALB不在同一个VPC，需要给controller指定VPC ID

```
helm upgrade —install aws-load-balancer-controller eks/aws-load-balancer-controller \
-n kube-system \
--set clusterName=nrt1 \
--set serviceAccount.create=false \
--set vpcId=vpc-ALB-ID \
--set serviceAccount.name=aws-load-balancer-controller
```

3. 如果使用VPC Peering连接，可以不用手动配置安全组，但仍需要指定IP模式，参考配置如下：

```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  namespace: my-app
  name: ingress-cross-vpc-test
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
```

4. 如果使用TGW连接VPC，同样需要IP模式，另外由于TGW不能跨VPC引用安全组，需要额外配置。

   a. 在ALB VPC中创建安全组，然后在ingress中指定此安全组，避免controller自动创建并挂载跨VPC安全组从而导致错误。参考配置如下：

```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  namespace: my-app
  name: ingress-cross-vpc-test
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/security-groups (http://alb.ingress.kubernetes.io/security-groups): sg-alb-security-group
    alb.ingress.kubernetes.io/manage-backend-security-group-rules (http://alb.ingress.kubernetes.io/manage-backend-security-group-rules): "false"
```

​     b. EKS worker node安全组配置入口规则，允许ALB VPC访问



