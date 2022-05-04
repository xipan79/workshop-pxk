## 04. EKS配置使用FSx for lustre

首选，我们定义如下环境变量

```
ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text)
CLUSTER_NAME=eks-121
VPC_ID=$(aws eks describe-cluster --name $CLUSTER_NAME --query "cluster.resourcesVpcConfig.vpcId" --output text)
SUBNET_ID=$(aws eks describe-cluster --name $CLUSTER_NAME --query "cluster.resourcesVpcConfig.subnetIds[0]" --output text)
SECURITY_GROUP_ID=$(aws eks describe-cluster --name $CLUSTER_NAME --query "cluster.resourcesVpcConfig.securityGroupIds" --output text)
CIDR_BLOCK=$(aws ec2 describe-vpcs --vpc-ids $VPC_ID --query "Vpcs[].CidrBlock" --output text)
S3_LOGS_BUCKET=eks-fsx-lustre-$(cat /dev/urandom | LC_ALL=C tr -dc "[:alpha:]" | tr '[:upper:]' '[:lower:]' | head -c 32)
SECURITY_GROUP_ID=$(aws eks describe-cluster --name $CLUSTER_NAME --query "cluster.resourcesVpcConfig.clusterSecurityGroupId" --output text)
```

其次，如果之前没有定义过OIDC Provider，那么需要进行

```
eksctl utils associate-iam-oidc-provider \
    --region $AWS_REGION \
    --cluster $CLUSTER_NAME \
    --approve
```

创建如下IAM Policy，对相关API操作进行授权

```
cat << EOF >  fsx-csi-driver.json
{
    "Version":"2012-10-17",
    "Statement":[
        {
            "Effect":"Allow",
            "Action":[
                "iam:CreateServiceLinkedRole",
                "iam:AttachRolePolicy",
                "iam:PutRolePolicy"
            ],
            "Resource":"arn:aws:iam::*:role/aws-service-role/s3.data-source.lustre.fsx.amazonaws.com/*"
        },
        {
            "Action":"iam:CreateServiceLinkedRole",
            "Effect":"Allow",
            "Resource":"*",
            "Condition":{
                "StringLike":{
                    "iam:AWSServiceName":[
                        "fsx.amazonaws.com"
                    ]
                }
            }
        },
        {
            "Effect":"Allow",
            "Action":[
                "s3:ListBucket",
                "fsx:CreateFileSystem",
                "fsx:DeleteFileSystem",
                "fsx:TagResource",
                "fsx:DescribeFileSystems"
            ],
            "Resource":[
                "*"
            ]
        }
    ]
}
EOF
```

创建policy

```
 aws iam create-policy \
        --policy-name Amazon_FSx_Lustre_CSI_Driver \
        --policy-document file://fsx-csi-driver.json
```

创建ServiceAccount

```
eksctl create iamserviceaccount \
    --region $AWS_REGION \
    --name fsx-csi-controller-sa \
    --namespace kube-system \
    --cluster $CLUSTER_NAME \
    --attach-policy-arn arn:aws:iam::$ACCOUNT_ID:policy/Amazon_FSx_Lustre_CSI_Driver \
    --approve
```

将Cloudformation中输出的Role Arn定义为变量

```
export ROLE_ARN=$(aws cloudformation describe-stacks --stack-name eksctl-eks-121-addon-iamserviceaccount-kube-system-fsx-csi-controller-sa --query "Stacks[0].Outputs[0].OutputValue" --output text)
```

创建FSx CSI驱动

```
kubectl apply -k "github.com/kubernetes-sigs/aws-fsx-csi-driver/deploy/kubernetes/overlays/stable/?ref=master"
```

查看确认FSx CSI驱动已经正确安装

```
[root@ip-10-0-5-63 04]# kubectl get po -n kube-system | grep ^fsx
fsx-csi-controller-6f7c4f9c5d-7v22h   4/4     Running   0          3h50m
fsx-csi-controller-6f7c4f9c5d-jwcd2   4/4     Running   0          3h50m
fsx-csi-node-4lcfk                    3/3     Running   0          3h50m
fsx-csi-node-cswfx                    3/3     Running   0          3h50m
fsx-csi-node-k9kkd                    3/3     Running   0          3h50m
fsx-csi-node-r45mt                    3/3     Running   0          3h50m
fsx-csi-node-rqhhn                    3/3     Running   0          3h50m
fsx-csi-node-v2qqs                    3/3     Running   0          3h50m
```

创建Service Account，关联之前创建的Role ARN

```
kubectl annotate serviceaccount -n kube-system fsx-csi-controller-sa \
 eks.amazonaws.com/role-arn=$ROLE_ARN --overwrite=true
```

创建S3存储桶并上传一个测试文件

```
aws s3 mb s3://$S3_LOGS_BUCKET
echo test-file >> testfile
aws s3 cp testfile s3://$S3_LOGS_BUCKET/export/testfile
```

创建StorageClass定义文件

```
cat << EOF > storageclass.yaml
---
kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
    name: fsx-sc
provisioner: fsx.csi.aws.com
parameters:
    subnetId: ${SUBNET_ID}
    securityGroupIds: ${SECURITY_GROUP_ID}
    s3ImportPath: s3://${S3_LOGS_BUCKET}
    s3ExportPath: s3://${S3_LOGS_BUCKET}/export
    deploymentType: SCRATCH_2
mountOptions:
    - flock
EOF
```

创建StorageClass

```
kubectl apply -f storageclass.yaml
```

下载pv claim文件

```
curl -o claim.yaml https://raw.githubusercontent.com/kubernetes-sigs/aws-fsx-csi-driver/master/examples/kubernetes/dynamic_provisioning_s3/specs/claim.yaml
```

创建PV claim

```
kubectl apply -f claim.yaml
```

确认文件系统已经生成并绑定

```
[root@ip-10-0-5-63 04]# kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                       STORAGECLASS   REASON   AGE
efs-pv                                     5Gi        RWX            Retain           Bound    default/efs-claim           efs-sc                  21h
efs-pvc                                    5Gi        RWX            Retain           Bound    storage/efs-storage-claim   efs-sc                  22h
pvc-dc429030-0452-497e-b643-0a12d7e04950   1200Gi     RWX            Delete           Bound    default/fsx-claim           fsx-sc
```

部署测试应用

```
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/aws-fsx-csi-driver/master/examples/kubernetes/dynamic_provisioning_s3/specs/pod.yaml
```

验证测试

```
[root@ip-10-0-5-63 04]# kubectl get pods
NAME      READY   STATUS    RESTARTS   AGE
fsx-app   1/1     Running   0          3m58s
[root@ip-10-0-5-63 04]# kubectl exec fsx-app -- ls /data
export
out.txt
```

