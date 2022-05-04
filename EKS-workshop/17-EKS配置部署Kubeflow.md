## 1. 安装Kubeflow

由于Kubeflow需要较多资源，如果是新创建集群，建议直接设置worker node节点数为6个。如果是之前创建好的集群，通过修改autoscaling设置，将worker node节点数调整为6个

```
aws autoscaling set-desired-capacity --auto-scaling-group-name eksctl-eksworkshop-nodegroup-ng-basic-NodeGroup-P8WDMK6HF36O --desired-capacity 6
```

根据官方文档教程，进行环境准备设置：https://www.kubeflow.org/docs/aws/deploy/install-kubeflow/

首先获取EKS 工作节点role，配置环境变量用于后续使用

```
export REGION=cn-northwest-1
export CLUSTER_NAME=eksworkshop

export STACK_NAME=$(eksctl get nodegroup --cluster $CLUSTER_NAME --region $REGION  -o json | jq -r '.[].StackName')
export NODE_INSTANCE_ROLE=$(aws cloudformation describe-stack-resources --region $REGION --stack-name $STACK_NAME | jq -r '.StackResources[] | select(.LogicalResourceId=="NodeInstanceRole") | .PhysicalResourceId' ) 
```

下载并安装kfctl

```
curl --silent --location "https://github.com/kubeflow/kfctl/releases/download/v1.0.1/kfctl_v1.0.1-0-gf3edb9b_linux.tar.gz" | tar xz -C /tmp
sudo mv -v /tmp/kfctl /usr/local/bin
```

配置和下载kubeflow文件，本实验使用非cognito版本，默认不进行身份验证。

```
export PATH=$PATH:"<path to kfctl>"
export CONFIG_URI="https://raw.githubusercontent.com/kubeflow/manifests/v1.1-branch/kfdef/kfctl_aws.v1.1.0.yaml"
export AWS_CLUSTER_NAME=<YOUR EKS CLUSTER NAME>
mkdir ${AWS_CLUSTER_NAME} && cd ${AWS_CLUSTER_NAME}
wget -O kfctl_aws.yaml $CONFIG_URI
```

替换kfctl_aws.yaml中的region和role为当前的创建eks的region和node节点使用的role

```
region: cn-northwest-1
      roles:
      - eksctl-eksworkshop-nodegroup-ng-b-NodeInstanceRole-xxxxxxxxxxxxxx
```

由于防火墙或安全限制，海外gcr.io, quay.io的镜像可能无法下载，需要通过修改镜像的方式安装，把镜像url替换成aws国内镜像站点url，其中KF_DIR是kfctl_aws.yaml所在目录，

```
sed -i "s/gcr.io/048912060910.dkr.ecr.cn-northwest-1.amazonaws.com.cn\/gcr/g" `grep "gcr.io" -rl ${KF_DIR}`
sed -i "s/quay.io/048912060910.dkr.ecr.cn-northwest-1.amazonaws.com.cn\/quay/g" `grep "quay.io" -rl ${KF_DIR}`
```

比如通过pwd查看我当前所在目录是/home/ec2-user/eksworkshop/kubeflow/，则可替换为：

```
sed -i "s/gcr.io/048912060910.dkr.ecr.cn-northwest-1.amazonaws.com.cn\/gcr/g" `grep "gcr.io" -rl /home/ec2-user/eksworkshop/kubeflow/`
```

部署kubeflow

```
kfctl apply -V -f kfctl_aws.yaml
```

查看kubeflow运行状态，大概需要15分钟，kubeflow才能正常运行。在启动过程中若有报错，可通过kubectl describe或者logs进行查看。如果有个别image拉取失败，可能是NWCD镜像仓库没有这个镜像，可提交镜像需求，或者通过海外CDK部署的环境推送缺失镜像到自己账号的ECR镜像仓库，并通过kubectl edit pod修改镜像地址

```
kubectl -n kubeflow get all
```

![](https://tva1.sinaimg.cn/large/0081Kckwly1gk45dcot8xj30x40u0dsi.jpg)



## 2. 安装Kubeflow-faring

直接导入的镜像里的原始文档会有报错，

```
!pip install --upgrade six
import importlib
importlib.reload(sys)
```

