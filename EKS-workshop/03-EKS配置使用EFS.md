## 03. EKS配置使用EFS

EFS可以通过控制台手动创建，也可以通过AWS命令行工具创建。基于Infrastructure as Code原则，我们建议基于代码来创建EFS文件系统，首选定义环境变量

```
[root@ip-10-0-5-63 03]# CLUSTER_NAME=eks-121
[root@ip-10-0-5-63 03]# VPC_ID=$(aws eks describe-cluster --name $CLUSTER_NAME --query "cluster.resourcesVpcConfig.vpcId" --output text)
[root@ip-10-0-5-63 03]# CIDR_BLOCK=$(aws ec2 describe-vpcs --vpc-ids $VPC_ID --query "Vpcs[].CidrBlock" --output text)
```

然后我们创建安全组，定义EFS访问端口2409可以允许EKS所在VPC访问

```
MOUNT_TARGET_GROUP_NAME="eks-efs-group"
MOUNT_TARGET_GROUP_DESC="NFS access to EFS from EKS worker nodes"
MOUNT_TARGET_GROUP_ID=$(aws ec2 create-security-group --group-name $MOUNT_TARGET_GROUP_NAME --description "$MOUNT_TARGET_GROUP_DESC" --vpc-id $VPC_ID | jq --raw-output '.GroupId')
aws ec2 authorize-security-group-ingress --group-id $MOUNT_TARGET_GROUP_ID --protocol tcp --port 2049 --cidr $CIDR_BLOCK
```

创建文件EFS文件系统

```
FILE_SYSTEM_ID=$(aws efs create-file-system | jq --raw-output '.FileSystemId')
```

查看EFS LifeCycleState状态，当

```
root@ip-10-0-5-63 03]# aws efs describe-file-systems --file-system-id $FILE_SYSTEM_ID
{
    "FileSystems": [
        {                                                                                    
            "SizeInBytes": {                                                                             
                "ValueInIA": 0,                               
                "ValueInStandard": 6144,                                                               
                "Value": 6144
            },
            "NumberOfMountTargets": 0,
            "ThroughputMode": "bursting",
            "CreationToken": "8fa3ef10-abcd-1234-8e61-2480abcdxxxx",                                      
            "Encrypted": false,                                                                        
            "Tags": [],                                        
            "CreationTime": 1649492442.0,
            "PerformanceMode": "generalPurpose",
            "FileSystemId": "fs-061ad5dd27198xxxx",
            "FileSystemArn": "arn:aws:elasticfilesystem:us-east-1:8082xxxx3800:file-system/fs-061ad5dd2abcdxxx",
            "LifeCycleState": "available",
            "OwnerId": "8082xxxx3800"
        }
    ]
}
```

在EKS集群需要访问EFS文件系统的子网创建挂载目标，并且关联之前创建的安全组，若为多个子网，则需要分别创建多次

```
aws efs create-mount-target --file-system-id FileSystemId --subnet-id SubnetID --security-group SGGroupID
```

查看挂载目标点的状态，从creating变成available，则创建完成

```
[root@ip-10-0-5-63 03]# aws efs describe-mount-targets --file-system-id $FILE_SYSTEM_ID | jq --raw-output '.MountTargets[].LifeCycleState'
available
available
available
```

参考官方指南，安装EFS CSI驱动最新版https://github.com/kubernetes-sigs/aws-efs-csi-driver

```
[root@ip-10-0-5-63 03]# kubectl apply -k "github.com/kubernetes-sigs/aws-efs-csi-driver/deploy/kubernetes/overlays/stable/?ref=release-1.3"
serviceaccount/efs-csi-controller-sa created
serviceaccount/efs-csi-node-sa created
clusterrole.rbac.authorization.k8s.io/efs-csi-external-provisioner-role created
clusterrolebinding.rbac.authorization.k8s.io/efs-csi-provisioner-binding created
deployment.apps/efs-csi-controller created
daemonset.apps/efs-csi-node created
csidriver.storage.k8s.io/efs.csi.aws.com configured
```

查看efs-csi正常安装成功

```
[root@ip-10-0-5-63 03]# kubectl get pods -n kube-system | grep ^efs
efs-csi-controller-85b5664c85-c2hsw   3/3     Running   0          6m6s
efs-csi-controller-85b5664c85-l2jp4   3/3     Running   0          6m6s
efs-csi-node-44cf9                    3/3     Running   0          6m6s
efs-csi-node-fvglq                    3/3     Running   0          6m6s
efs-csi-node-hb9q2                    3/3     Running   0          6m6s
efs-csi-node-lc6xx                    3/3     Running   0          6m6s
efs-csi-node-p5wqs                    3/3     Running   0          6m6s
efs-csi-node-tnbp6                    3/3     Running   0          6m6s
```

部署应用进行测试，注意将efs-test目录中的yaml文件中EFS_VOLUME_ID改为创建的EFS文件系统id，如fs-xxxxxxxxxxx

```
[root@ip-10-0-5-63 03]# kubectl apply -f efs-test/
persistentvolumeclaim/efs-claim created
pod/app1 created
pod/app2 created
persistentvolume/efs-pv created
storageclass.storage.k8s.io/efs-sc created
```

然后查看pv状态

```
[root@ip-10-0-5-63 03]# kubectl describe storageclass efs-sc
Name:            efs-sc
IsDefaultClass:  No
Annotations:     kubectl.kubernetes.io/last-applied-configuration={"apiVersion":"storage.k8s.io/v1","kind":"StorageClass","metadata":{"annotations":{},"name":"efs-sc"},"provisioner":"efs.csi.aws.com"}

Provisioner:           efs.csi.aws.com
Parameters:            <none>
AllowVolumeExpansion:  <unset>
MountOptions:          <none>
ReclaimPolicy:         Delete
VolumeBindingMode:     Immediate
Events:                <none>
[root@ip-10-0-5-63 03]# kubectl get pv
NAME      CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                       STORAGECLASS   REASON   AGE
efs-pv    5Gi        RWX            Retain           Bound    default/efs-claim           efs-sc                  
[root@ip-10-0-5-63 03]# kubectl describe pv efs-pv
Name:            efs-pv
Labels:          <none>
Annotations:     pv.kubernetes.io/bound-by-controller: yes
Finalizers:      [kubernetes.io/pv-protection]
StorageClass:    efs-sc
Status:          Bound
Claim:           default/efs-claim
Reclaim Policy:  Retain
Access Modes:    RWX
VolumeMode:      Filesystem
Capacity:        5Gi
Node Affinity:   <none>
Message:
Source:
    Type:              CSI (a Container Storage Interface (CSI) volume source)
    Driver:            efs.csi.aws.com
    FSType:
    VolumeHandle:      fs-061ad5ddabcdxxxx
    ReadOnly:          false
    VolumeAttributes:  <none>
Events:                <none>
```

确认测试应用正常运行后进行测试

```
[root@ip-10-0-5-63 03]# kubectl get pods --watch
NAME   READY   STATUS              RESTARTS   AGE
app1   1/1     Running             0          22s
app2   0/1     ContainerCreating   0          22s
app2   1/1     Running             0          24s
[root@ip-10-0-5-63 03]# kubectl exec -ti app1 -- tail /data/out1.txt
Sat Apr 9 09:57:23 UTC 2022
Sat Apr 9 09:57:28 UTC 2022
Sat Apr 9 09:57:33 UTC 2022
Sat Apr 9 09:57:38 UTC 2022
Sat Apr 9 09:57:43 UTC 2022
Sat Apr 9 09:57:48 UTC 2022
[root@ip-10-0-5-63 03]# kubectl exec -ti app2 -- tail /data/out1.txt
Sat Apr 9 09:57:23 UTC 2022
Sat Apr 9 09:57:28 UTC 2022
Sat Apr 9 09:57:33 UTC 2022
Sat Apr 9 09:57:38 UTC 2022
Sat Apr 9 09:57:43 UTC 2022
Sat Apr 9 09:57:48 UTC 2022
Sat Apr 9 09:57:53 UTC 2022
Sat Apr 9 09:57:58 UTC 2022
```

