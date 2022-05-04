## **1. NWCD镜像仓库**

在国内宁夏或北京区域搭建EKS集群并部署应用，最头疼的问题应该是镜像的获取。为了尽可能解决这个问题，NWCD在宁夏搭建了ECR镜像仓库，将[Docker Hub](https://hub.docker.com/)，[Google Container Registry](https://console.cloud.google.com/gcr/images/google-containers/GLOBAL?pli=1)和[Quay](https://quay.io/search)中常用的公共container image自动同步至AWS中国区的ECR内，使AWS用户能更方便快捷的获取这些常见的容器镜像，具体可以参考Link：https://github.com/nwcdlabs/container-mirror

由于NWCD已经包含了大多经常使用的主流镜像，因此推荐采用webhook方案来实现自动替换镜像地址，利用webhook自动替换镜像地址可以参考Link：https://github.com/nwcdlabs/container-mirror/blob/master/webhook/README.md 通过wehook自动替换镜像地址，我们不用大量手动修改原始yaml文件，可以明显的提升部署应用的效率。

```
$ kubectl apply -f https://raw.githubusercontent.com/nwcdlabs/container-mirror/master/webhook/mutating-webhook.yaml
```

webhook部署完成后我们部署个应用进行验证，可以看到已经自动将镜像地址替换为048912060910开头的NWCD ECR镜像地址

```
$ kubectl run --generator=run-pod/v1 test --image=k8s.gcr.io/coredns:1.3.1
$ kubectl get pod test -o=jsonpath='{.spec.containers[0].image}'
# 结果应显示为048912060910.dkr.ecr.cn-northwest-1.amazonaws.com.cn/gcr/google_containers/coredns:1.3.1
```

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ii03brj31sg050n0d.jpg)



## **2. 自动将海外镜像复制到自己账号下的镜像仓库**

通过webhook自动替换NWCD镜像仓库地址虽然方便，但是NWCD的镜像仓库并不能100%保证拥有我们所需要的镜像，这时候就需要根据文档中的提示提交所需镜像list给NWCD，然后由他们去拉取镜像到NWCD仓库，实时性无法得到保证，因此建议根据AWS官方博客搭建拉取海外镜像到自己账号的ECR仓库，以便及时自动化的获得NWCD缺失的所需镜像。

Link：https://aws.amazon.com/cn/blogs/china/convenient-and-safe-use-of-overseas-public-container-images-in-aws-china/

首先需要将本方案部署在AWS海外区域，如北美、东京、新加坡、香港等区域。本操作示例通过在海外区域启动一台AmazonLinux2 EC2服务器，然后在其上部署环境，首先需要在启动好的EC2上安装NodeJS，AWS CDK等工具。

安装NodeJS和Git

```
$ sudo yum install -y gcc-c++ make
$ curl -sL https://rpm.nodesource.com/setup_12.x | sudo -E bash -
$ sudo yum install -y nodejs
$ sudo yum install git
```

安装CDK

首先需要通过aws configure命令，在EC2服务器配置AKSK

```
$ aws configure
AWS Access Key ID [None]: xxxxxxxxxxxxxxxxxxxxxxxxxx
AWS Secret Access Key [None]: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
Default region name [None]: xxxxxxxxxxxxxx
Default output format [None]:
```

然后安装CDK

```
$ sudo npm install -g aws-cdk
```

安装完成后，检查环境

```
$ aws --version
aws-cli/1.18.107 Python/2.7.18 Linux/4.14.193-149.317.amzn2.x86_64 botocore/1.17.31
$ node -v
v12.19.0
$ cdk --version
1.69.0 (build 2b474b9)
$ git version
git version 2.23.3
```

克隆github 项目并构建此CDK项目

```
$ git clone https://github.com/aws-samples/amazon-ecr-replication-for-pub-container-images.git
$ cd amazon-ecr-replication-for-pub-container-images
$ sudo npm install
```

部署此方案到AWS海外区域，其中targetRegion，targetRegionAK和targetRegionSK分别代表AWS国内区域AWS用户的access key和secret access key。此秘钥会保存在AWS Secrets Manager中以供CodeBuild登录AWS国内区域ECR

```
$ sudo npx cdk deploy --parameters targetRegion=cn-northwest-1 --parameters targetRegionAK=AKABCD12345 --parameters targetRegionSK=SK12345
```

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04iihg9oj326q0n6tjp.jpg)

CDK部署完成后，在所在区域的CodeCommit可以看到pub-images-mirror Repository

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04iexic8j31m20ceace.jpg)

进入pub-images-mirror Repository，上传images.txt文件，这个文件里包含了所需的镜像的清单，可以一次性写入多个镜像文件，但是需要注意因为网络的原因，镜像太多可能导致Build失败。

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ikd5vqj31m20j676y.jpg)

images.txt文件上传后可以直接编辑，比如我在这里填写后面做Spot实例Autoscaling时所需的一个镜像文件的原始地址为k8s.gcr.io/cuda-vector-add:v0.1，然后右下角commit

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ifkbi7j31n60b4wfk.jpg)

然后我们会看到在Build阶段，一个Build Job已经开始运行

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ij1s58j31yc0em77y.jpg)

视镜像文件的数量和大小不同，可能需要几分钟到几个小时的Build时间，等Status变为Succeeded后，镜像复制完成

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ig2m1bj31lq0f841b.jpg)

我们回到宁夏区域ECR，可以看到cuda-vector-add镜像已经复制完成，以后只需要将原始yaml文件中的地址替换为宁夏区域ECR中的镜像地址即可，具体可以通过View push commands查看

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ihk7jhj31pe0imn03.jpg)

## **3. 创建集群**

首先通过eksctl工具创建EKS集群，部署到之前在宁夏区创建好的VPC中，分别有3个Public子网和3个Private子网，在cluster.yaml文件中指明EKS集群名称，版本，区域及子网信息即可

```
$ eksctl create cluster -f cluster.yaml
```

```
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: eksworkshop
  region: cn-northwest-1
  version: "1.18"

vpc:
  subnets:
    private:
      cn-northwest-1a: { id: subnet-0983437dddf03xxxx }
      cn-northwest-1b: { id: subnet-026e8acfd79d3xxxx }
      cn-northwest-1c: { id: subnet-0a695b95ceaacxxxx }
    public:
      cn-northwest-1a: { id: subnet-0cf61f849f361xxxx }
      cn-northwest-1b: { id: subnet-0e7995c6b8100xxxx }
      cn-northwest-1c: { id: subnet-02d4f564df440xxxx }
```



## **4. 创建Basic Node Group**

EKS集群默认有两个worker节点，用于部署一些集群基础服务，比如cluster autoscaler等，通过ng-basic.yaml文件创建BasicNodeGroup，Worker Node可分布在3个可用区以确保高可用性，并通过withAddonPolicies添加ebs/efs/alb/cloudwatch等服务相应的权限，实例类型为m5.large

```
$ eksctl create nodegroup --config-file=ng-basic.yaml
```

```
---
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: eksworkshop
  region: cn-northwest-1


nodeGroups:
  # Basic workers NG - multi AZ, scale from 2
  - name: ng-basic
    ami: auto
    instanceType: m5.large
    desiredCapacity: 2
    minSize: 2
    maxSize: 6
    volumeSize: 50
    volumeType: gp2
    volumeEncrypted: true
    privateNetworking: true
    iam:
      attachPolicyARNs:
        - arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM
        - arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy
        - arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy
        - arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
      withAddonPolicies:
        autoScaler: true
        ebs: true
        fsx: true
        efs: true
        albIngress: true
        cloudWatch: true
    tags:
      k8s.io/cluster-autoscaler/enabled: 'true'
    availabilityZones: ["cn-northwest-1a", "cn-northwest-1b", "cn-northwest-1c"]
```

## **5. 创建Cluster Autoscaler**

等BasicNodeGroup Ready后，就可以创建一些基础服务，比如用Cluster Autoscaler来配合AutoScaling Group，使集群worker node根据负载的大小进行弹性伸缩。

```
$ kubectl apply -f autoscaler.yaml
```

```
---
apiVersion: v1
kind: ServiceAccount
metadata:
  labels:
    k8s-addon: cluster-autoscaler.addons.k8s.io
    k8s-app: cluster-autoscaler
  name: cluster-autoscaler
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: cluster-autoscaler
  labels:
    k8s-addon: cluster-autoscaler.addons.k8s.io
    k8s-app: cluster-autoscaler
rules:
  - apiGroups: [""]
    resources: ["events", "endpoints"]
    verbs: ["create", "patch"]
  - apiGroups: [""]
    resources: ["pods/eviction"]
    verbs: ["create"]
  - apiGroups: [""]
    resources: ["pods/status"]
    verbs: ["update"]
  - apiGroups: [""]
    resources: ["endpoints"]
    resourceNames: ["cluster-autoscaler"]
    verbs: ["get", "update"]
  - apiGroups: [""]
    resources: ["nodes"]
    verbs: ["watch", "list", "get", "update"]
  - apiGroups: [""]
    resources:
      - "pods"
      - "services"
      - "replicationcontrollers"
      - "persistentvolumeclaims"
      - "persistentvolumes"
    verbs: ["watch", "list", "get"]
  - apiGroups: ["extensions"]
    resources: ["replicasets", "daemonsets"]
    verbs: ["watch", "list", "get"]
  - apiGroups: ["policy"]
    resources: ["poddisruptionbudgets"]
    verbs: ["watch", "list"]
  - apiGroups: ["apps"]
    resources: ["statefulsets", "replicasets", "daemonsets"]
    verbs: ["watch", "list", "get"]
  - apiGroups: ["storage.k8s.io"]
    resources: ["storageclasses", "csinodes"]
    verbs: ["watch", "list", "get"]
  - apiGroups: ["batch", "extensions"]
    resources: ["jobs"]
    verbs: ["get", "list", "watch", "patch"]
  - apiGroups: ["coordination.k8s.io"]
    resources: ["leases"]
    verbs: ["create"]
  - apiGroups: ["coordination.k8s.io"]
    resourceNames: ["cluster-autoscaler"]
    resources: ["leases"]
    verbs: ["get", "update"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: cluster-autoscaler
  namespace: kube-system
  labels:
    k8s-addon: cluster-autoscaler.addons.k8s.io
    k8s-app: cluster-autoscaler
rules:
  - apiGroups: [""]
    resources: ["configmaps"]
    verbs: ["create","list","watch"]
  - apiGroups: [""]
    resources: ["configmaps"]
    resourceNames: ["cluster-autoscaler-status", "cluster-autoscaler-priority-expander"]
    verbs: ["delete", "get", "update", "watch"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: cluster-autoscaler
  labels:
    k8s-addon: cluster-autoscaler.addons.k8s.io
    k8s-app: cluster-autoscaler
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-autoscaler
subjects:
  - kind: ServiceAccount
    name: cluster-autoscaler
    namespace: kube-system

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: cluster-autoscaler
  namespace: kube-system
  labels:
    k8s-addon: cluster-autoscaler.addons.k8s.io
    k8s-app: cluster-autoscaler
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: cluster-autoscaler
subjects:
  - kind: ServiceAccount
    name: cluster-autoscaler
    namespace: kube-system

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-autoscaler
  namespace: kube-system
  labels:
    app: cluster-autoscaler
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cluster-autoscaler
  template:
    metadata:
      labels:
        app: cluster-autoscaler
      annotations:
        prometheus.io/scrape: 'true'
        prometheus.io/port: '8085'
    spec:
      serviceAccountName: cluster-autoscaler
      containers:
        - image: 685200526309.dkr.ecr.cn-northwest-1.amazonaws.com.cn/gcr/google_containers/autoscaling/cluster-autoscaler:v1.17.3
          name: cluster-autoscaler
          resources:
            limits:
              cpu: 100m
              memory: 300Mi
            requests:
              cpu: 100m
              memory: 300Mi
          command:
            - ./cluster-autoscaler
            - --v=4
            - --stderrthreshold=info
            - --cloud-provider=aws
            - --skip-nodes-with-local-storage=false
            - --expander=least-waste
            - --aws-use-static-instance-list=true
            - --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/eksworkshop
          volumeMounts:
            - name: ssl-certs
              mountPath: /etc/ssl/certs/ca-certificates.crt
              readOnly: true
          imagePullPolicy: "Always"
      volumes:
        - name: ssl-certs
          hostPath:
            path: "/etc/ssl/certs/ca-bundle.crt"
```

由于国内拿不到aws ec2 price api，所以在command项里增加- --aws-use-static-instance-list=true，采用静态列表。另外注意将下面的EKS集群名称eksworkshop替换为自己的集群名称

```
- --aws-use-static-instance-list=true
- --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/eksworkshop
```

当ClusterAutoscaler创建好以后，我们看一下当前的pod情况，所需的主要相关服务都已经正常启动

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ijx408j318608ojv3.jpg)



## **6. 创建Spot Node Group**

通过ng-spot.yaml文件创建NodeGroup，用于机器学习训练的Worker Node均部署在cn-northwest-1a这个可用区，并通过withAddonPolicies添加ebs/efs/alb/cloudwatch等服务相应的权限，实例类型为G4dn Spot

```
$ eksctl create nodegroup --config-file=ng-spot.yaml
```

```
---
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: eksworkshop
  region: cn-northwest-1


nodeGroups:
  # spot workers NG - Single AZ, scale from 0
  - name: g4dn-spot
    ami: auto
    instanceType: mixed
    desiredCapacity: 0
    minSize: 0
    maxSize: 3
    volumeSize: 50
    volumeType: gp2
    volumeEncrypted: true
    iam:
      attachPolicyARNs:
        - arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM
        - arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy
        - arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy
        - arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
      withAddonPolicies:
        autoScaler: true
        ebs: true
        fsx: true
        efs: true
        albIngress: true
        cloudWatch: true
    instancesDistribution:
      onDemandPercentageAboveBaseCapacity: 0
      instanceTypes:
        - g4dn.xlarge
        - g4dn.2xlarge
      spotInstancePools: 2
    tags:
      k8s.io/cluster-autoscaler/node-template/taint/dedicated: nvidia.com/gpu=true
      k8s.io/cluster-autoscaler/node-template/label/nvidia.com/gpu: 'true'
      k8s.io/cluster-autoscaler/enabled: 'true'
    labels:
      lifecycle: Ec2Spot
      nvidia.com/gpu: 'true'
      k8s.amazonaws.com/accelerator: nvidia-tesla
    taints:
      nvidia.com/gpu: "true:NoSchedule"
    privateNetworking: true
    availabilityZones: ["cn-northwest-1a"]
```



## **7. 部署Nvidia Device Plugin**

通过nvidia-device-plugin.yaml文件创建DaemonSet，把worker node上的GPU信息传递出来。在国内部署时由于网络的原因可能无法拿到镜像，可以通过之前的方法先把镜像下载到宁夏ECR仓库，然后修改yaml文件中的镜像地址为国内地址

```
$ kubectl apply -f nvidia-device-plugin.yaml
```

```
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: nvidia-device-plugin-daemonset-1.12
  namespace: kube-system
spec:
  selector:
    matchLabels:
      name: nvidia-device-plugin-ds
  updateStrategy:
    type: RollingUpdate
  template:
    metadata:
      labels:
        name: nvidia-device-plugin-ds
    spec:
      tolerations:
      - key: nvidia.com/gpu
        operator: Exists
        effect: NoSchedule
      containers:
      - image: 685200526309.dkr.ecr.cn-northwest-1.amazonaws.com.cn/dockerhub/nvidia/k8s-device-plugin:1.11
        name: nvidia-device-plugin-ctr
        securityContext:
          allowPrivilegeEscalation: false
          capabilities:
            drop: ["ALL"]
        volumeMounts:
          - name: device-plugin
            mountPath: /var/lib/kubelet/device-plugins
      volumes:
        - name: device-plugin
          hostPath:
            path: /var/lib/kubelet/device-plugins
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: beta.kubernetes.io/instance-type
                operator: In
                values:
                - g4dn.xlarge
                - g4dn.2xlarge
```



## **8. 测试SpotNodeGroup自动扩展**

由于SpotNodeGroup desired的实例数量是0，因此当ng-spot创建好以后，实际上在EC2控制台并没有G4dn.spot实例。我们创建一个需要GPU的应用来测试集群的AutoScaling功能

```
$ kubectl apply -f cuda.yaml
```

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cuda-vector-add
  labels:
    app: cuda-vector-add
spec:
  replicas: 3
  selector:
    matchLabels:
      app: cuda-vector-add
  template:
    metadata:
      name: cuda-vector-add
      labels:
        app: cuda-vector-add
    spec:
      nodeSelector:
        nvidia.com/gpu: 'true'
      tolerations:
      - key: "nvidia.com/gpu"
        operator: "Exists"
        effect: "NoSchedule"
      containers:
        - name: cuda-vector-add
          # https://github.com/kubernetes/kubernetes/blob/v1.7.11/test/images/nvidia-cuda/Dockerfile
          image: "k8s.048912060910.dkr.ecr.cn-northwest-1.amazonaws.com.cn/gcr/cuda-vector-add:v0.1"
          resources:
            limits:
              nvidia.com/gpu: 1 #
```

可以看到，由于集群里目前并没有SPOT实例，因此我们创建的需要GPU的应用处于pending状态

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ikvj8yj317y0bkwjh.jpg)

然后我们回到EC2控制台，可以看到3台G4dn.xlarge SPOT实例正在启动，稍等两分钟等GPU实例状态正常以后，可以看到我们部署的3个cuda Pod已经正常运行。

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ih34p9j31jw086n0a.jpg)

然后再查看Pod状态，发现3个cuda应用已经完成

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04ijbpjrj31c40ka7df.jpg)

我们删除cuda测试应用，几分钟后会发现G4dn Spot实例会根据AutoScaling中desired capacity的设置，重新回复到0。在EC2控制台会看到资源被回收

![](https://tva1.sinaimg.cn/large/0081Kckwgy1gk04igls6bj31kg08441n.jpg)

