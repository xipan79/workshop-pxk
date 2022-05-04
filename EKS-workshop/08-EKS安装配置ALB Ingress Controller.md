## 08. EKS安装配置ALB Ingress Controller

下载IAM Policy

```
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.4.1/docs/install/iam_policy.json
```

创建IAM policy文件

```
[root@ip-10-0-5-63 08]# aws iam create-policy \
>     --policy-name AWSLoadBalancerControllerIAMPolicy \
>     --policy-document file://iam_policy.json
{
    "Policy": {
        "PolicyName": "AWSLoadBalancerControllerIAMPolicy",
        "PermissionsBoundaryUsageCount": 0,
        "CreateDate": "2022-04-14T03:22:21Z",
        "AttachmentCount": 0,
        "IsAttachable": true,
        "PolicyId": "ANPA3YLX62M4EC4OL54NC",
        "DefaultVersionId": "v1",
        "Path": "/",
        "Arn": "arn:aws:iam::8082xxxx3800:policy/AWSLoadBalancerControllerIAMPolicy",
        "UpdateDate": "2022-04-14T03:22:21Z"
    }
}
```

通过下面的Link访问dashboard

```
eksctl create iamserviceaccount \
  --cluster=eks-121 \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --role-name "AmazonEKSLoadBalancerControllerRole" \
  --attach-policy-arn=arn:aws:iam::8082xxxx3800:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve

```

验证service account

```
[root@ip-10-0-5-63 08]# eksctl  get iamserviceaccount --cluster eks-121
2022-04-14 03:27:11 [ℹ]  eksctl version 0.90.0
2022-04-14 03:27:11 [ℹ]  using region us-east-1
NAMESPACE       NAME                            ROLE ARN
kube-system     aws-load-balancer-controller    arn:aws:iam::808242303800:role/AmazonEKSLoadBalancerControllerRole
kube-system     cluster-autoscaler              arn:aws:iam::8082xxxx3800:role/eksctl-eks-121-addon-iamserviceaccount-kube-Role1-17FO3WYVK2ZCH
kube-system     fsx-csi-controller-sa           arn:aws:iam::8082xxxx3800:role/eksctl-eks-121-addon-iamserviceaccount-kube-Role1-1V15E6ALTNFOU
```

查看service account具体信息

```
[root@ip-10-0-5-63 08]# kubectl describe sa aws-load-balancer-controller -n kube-system
Name:                aws-load-balancer-controller
Namespace:           kube-system
Labels:              app.kubernetes.io/managed-by=eksctl
Annotations:         eks.amazonaws.com/role-arn: arn:aws:iam::808242303800:role/AmazonEKSLoadBalancerControllerRole
Image pull secrets:  <none>
Mountable secrets:   aws-load-balancer-controller-token-jmjvx
Tokens:              aws-load-balancer-controller-token-jmjvx
```

安装Helm

```
[root@ip-10-0-5-63 08]# curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 > get_helm.sh
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 11156  100 11156    0     0   680k      0 --:--:-- --:--:-- --:--:--  726k
[root@ip-10-0-5-63 08]# chmod 700 get_helm.sh
[root@ip-10-0-5-63 08]# ./get_helm.sh
Downloading https://get.helm.sh/helm-v3.8.2-linux-amd64.tar.gz
Verifying checksum... Done.
Preparing to install helm into /usr/local/bin
helm installed into /usr/local/bin/helm
[root@ip-10-0-5-63 08]# helm version
version.BuildInfo{Version:"v3.8.2", GitCommit:"6e3701edea09e5d55a8ca2aae03a68917630e91b", GitTreeState:"clean", GoVersion:"go1.17.5"}
```

添加Helm Repository并更新

```
[root@ip-10-0-5-63 08]# helm repo add eks https://aws.github.io/eks-charts
"eks" has been added to your repositories
[root@ip-10-0-5-63 08]# helm repo update
Hang tight while we grab the latest from your chart repositories...
...Successfully got an update from the "eks" chart repository
Update Complete. ⎈Happy Helming!⎈
[root@ip-10-0-5-63 08]#
```

安装AWS-loadbalancer-controller

```
[root@ip-10-0-5-63 08]# helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
>   -n kube-system \
>   --set clusterName=eks-121 \
>   --set serviceAccount.create=false \
>   --set serviceAccount.name=aws-load-balancer-controller
NAME: aws-load-balancer-controller
LAST DEPLOYED: Thu Apr 14 05:34:46 2022
NAMESPACE: kube-system
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
AWS Load Balancer controller installed!
```

确认aws-load-balancer-controller已经安装好

```
[root@ip-10-0-5-63 08]# kubectl get deployment -n kube-system aws-load-balancer-controller
NAME                           READY   UP-TO-DATE   AVAILABLE   AGE
aws-load-balancer-controller   2/2     2            2           114s
```

创建IngressClass，通过ingressclass.kubernetes.io/is-default-class: "true"可以定义为默认IngressClass

```
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  labels:
    app.kubernetes.io/component: controller
  name: my-aws-ingress-class
  annotations:
    ingressclass.kubernetes.io/is-default-class: "true"
spec:
  controller: ingress.k8s.aws/alb
```

定义后端测试应用

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app1-nginx-deployment
  labels:
    app: app1-nginx
spec:
  replicas: 1
  selector:
    matchLabels:
      app: app1-nginx
  template:
    metadata:
      labels:
        app: app1-nginx
    spec:
      containers:
        - name: app1-nginx
          image: stacksimplify/kube-nginxapp1:1.0.0
          ports:
            - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: app1-nginx-nodeport-service
  labels:
    app: app1-nginx
  annotations:
#Important Note:  Need to add health check path annotations in service level if we are planning to use multiple targets in a load balancer    
#    alb.ingress.kubernetes.io/healthcheck-path: /app1/index.html
spec:
  type: NodePort
  selector:
    app: app1-nginx
  ports:
    - port: 80
      targetPort: 80
```

定义ALB Ingress

```
# Annotations Reference: https://kubernetes-sigs.github.io/aws-load-balancer-controller/latest/guide/ingress/annotations/
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-nginxapp1
  labels:
    app: app1-nginx
  annotations:
    # Load Balancer Name
    alb.ingress.kubernetes.io/load-balancer-name: app1ingress
    #kubernetes.io/ingress.class: "alb" (OLD INGRESS CLASS NOTATION - STILL WORKS BUT RECOMMENDED TO USE IngressClass Resource) # Additional Notes: https://kubernetes-sigs.github.io/aws-load-balancer-controller/v2.3/guide/ingress/ingress_class/#deprecated-kubernetesioingressclass-annotation
    # Ingress Core Settings
    alb.ingress.kubernetes.io/scheme: internet-facing
    # Health Check Settings
    alb.ingress.kubernetes.io/healthcheck-protocol: HTTP 
    alb.ingress.kubernetes.io/healthcheck-port: traffic-port
    alb.ingress.kubernetes.io/healthcheck-path: /app1/index.html    
    alb.ingress.kubernetes.io/healthcheck-interval-seconds: '15'
    alb.ingress.kubernetes.io/healthcheck-timeout-seconds: '5'
    alb.ingress.kubernetes.io/success-codes: '200'
    alb.ingress.kubernetes.io/healthy-threshold-count: '2'
    alb.ingress.kubernetes.io/unhealthy-threshold-count: '2'
spec:
  ingressClassName: my-aws-ingress-class # Ingress Class
  defaultBackend:
    service:
      name: app1-nginx-nodeport-service
      port:
        number: 80       
```

查看当前部署信息

```
[root@ip-10-0-5-63 08]# kubectl get deploy
NAME                    READY   UP-TO-DATE   AVAILABLE   AGE
app1-nginx-deployment   1/1     1            1           127m
[root@ip-10-0-5-63 08]# kubectl get svc
NAME                          TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
app1-nginx-nodeport-service   NodePort    172.20.167.77   <none>        80:31067/TCP   127m
kubernetes                    ClusterIP   172.20.0.1      <none>        443/TCP        11d
[root@ip-10-0-5-63 08]# kubectl get ingress
NAME                CLASS                  HOSTS   ADDRESS                                             PORTS   AGE
ingress-nginxapp1   my-aws-ingress-class   *       app1ingress-836580206.us-east-1.elb.amazonaws.com   80      123m
```

