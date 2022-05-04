## 02. EKS配置使用EBS

首先需要创建IAM policy ，如下所示。本workshop所有权限均仅为示例，实际生产环境中需要遵循最小权限原则对各权限进行限制。

```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:AttachVolume",
        "ec2:CreateSnapshot",
        "ec2:CreateTags",
        "ec2:CreateVolume",
        "ec2:DeleteSnapshot",
        "ec2:DeleteTags",
        "ec2:DeleteVolume",
        "ec2:DescribeInstances",
        "ec2:DescribeSnapshots",
        "ec2:DescribeTags",
        "ec2:DescribeVolumes",
        "ec2:DetachVolume"
      ],
      "Resource": "*"
    }
  ]
}
```

通过命令kubectl -n kube-system describe configmap aws-auth，找到目前EKS node配置的role ARN，如下所示。也可以登陆控制台，通过查看work node工作节点确认

```
[root@ip-10-0-5-63 ~]# kubectl -n kube-system describe configmap aws-auth
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
  rolearn: arn:aws:iam::8082XXXX3800:role/eksctl-eks-121-nodegroup-managed-NodeInstanceRole-HXHJXXXXXXXX
```

在控制台，将刚才创建的policy附加到EKS工作节点role上，然后通过命令kubectl apply -k "github.com/kubernetes-sigs/aws-ebs-csi-driver/deploy/kubernetes/overlays/stable/?ref=release-1.5"安装，更多细节可参考链接：https://github.com/kubernetes-sigs/aws-ebs-csi-driver/

```
[root@ip-10-0-5-63 ~]# kubectl apply -k "github.com/kubernetes-sigs/aws-ebs-csi-driver/deploy/kubernetes/overlays/stable/?ref=release-1.5"
serviceaccount/ebs-csi-controller-sa created
serviceaccount/ebs-csi-node-sa created
clusterrole.rbac.authorization.k8s.io/ebs-csi-node-role created
clusterrole.rbac.authorization.k8s.io/ebs-external-attacher-role created
clusterrole.rbac.authorization.k8s.io/ebs-external-provisioner-role created
clusterrole.rbac.authorization.k8s.io/ebs-external-resizer-role created
clusterrole.rbac.authorization.k8s.io/ebs-external-snapshotter-role created
clusterrolebinding.rbac.authorization.k8s.io/ebs-csi-attacher-binding created
clusterrolebinding.rbac.authorization.k8s.io/ebs-csi-node-getter-binding created
clusterrolebinding.rbac.authorization.k8s.io/ebs-csi-provisioner-binding created
clusterrolebinding.rbac.authorization.k8s.io/ebs-csi-resizer-binding created
clusterrolebinding.rbac.authorization.k8s.io/ebs-csi-snapshotter-binding created
deployment.apps/ebs-csi-controller created
```

验证安装成功

```
[root@ip-10-0-5-63 ~]# kubectl get pods -n kube-system
NAME                                 READY   STATUS    RESTARTS   AGE
aws-node-25qzh                       1/1     Running   1          33h
aws-node-5fjxw                       1/1     Running   0          33h
aws-node-f7542                       1/1     Running   0          33h
aws-node-n9pnc                       1/1     Running   0          32h
aws-node-pdvxn                       1/1     Running   0          32h
aws-node-sp7k9                       1/1     Running   0          32h
coredns-66cb55d4f4-rt29z             1/1     Running   0          34h
coredns-66cb55d4f4-sdhs5             1/1     Running   0          34h
ebs-csi-controller-766ff9798-76hzk   6/6     Running   0          25s
ebs-csi-controller-766ff9798-thtfg   6/6     Running   0          25s
```

CSI插件安装成功后，第一步我们需要先定义storage class，以支持动态分配EBS卷。通过将volumeBingMode设置为WaitForFirstConsumer，EBS创建成功后将等待使用它的pod创建好后再进行绑定。更多关于storage class的信息可参考官方文档：        https://kubernetes.io/zh/docs/concepts/storage/storage-classes/

```
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata: 
  name: ebs-sc
provisioner: ebs.csi.aws.com
volumeBindingMode: WaitForFirstConsumer 
```

第二步来我们需要定义persistent-volume-claim，在这里需要指定之前定义的storage class，并定义EBS卷大小以及访问模式。通过ReadWriteOnce模式，一个EBS卷只能被集群中一个Pod访问，具体可参考官网：                                                                                              https://kubernetes.io/blog/2021/09/13/read-write-once-pod-access-mode-alpha/

```
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: ebs-mysql-pv-claim
spec: 
  accessModes:
    - ReadWriteOnce
  storageClassName: ebs-sc
  resources: 
    requests:
      storage: 4Gi
```

然后我们将测试部署一个Mysql数据库在EBS上来进行验证，首先我们在config map中定义创建数据库usermgme

```
apiVersion: v1
kind: ConfigMap
metadata:
  name: usermanagement-dbcreation-script
data: 
  mysql_usermgmt.sql: |-
    DROP DATABASE IF EXISTS usermgmt;
    CREATE DATABASE usermgmt; 
```

其次我们定义部署mysql的详细信息，如数据库镜像，磁盘挂载信息，以及config map等

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
spec: 
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  strategy:
    type: Recreate 
  template: 
    metadata: 
      labels: 
        app: mysql
    spec: 
      containers:
        - name: mysql
          image: mysql:5.6
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: dbpassword11
          ports:
            - containerPort: 3306
              name: mysql    
          volumeMounts:
            - name: mysql-persistent-storage
              mountPath: /var/lib/mysql    
            - name: usermanagement-dbcreation-script
              mountPath: /docker-entrypoint-initdb.d #https://hub.docker.com/_/mysql Refer Initializing a fresh instance                                            
      volumes: 
        - name: mysql-persistent-storage
          persistentVolumeClaim:
            claimName: ebs-mysql-pv-claim
        - name: usermanagement-dbcreation-script
          configMap:
            name: usermanagement-dbcreation-script
```

最后通过定义mysql-clusterip-service，集群内pod可通过clusterip访问mysql数据库

```
apiVersion: v1
kind: Service
metadata: 
  name: mysql
spec:
  selector:
    app: mysql 
  ports: 
    - port: 3306  
  clusterIP: None # This means we are going to use Pod IP    
```

把上述定义好的yaml文件，放在kube-manifests文件夹中，然后通过kubectl apply -f kube-manifests/一键部署

```
[root@ip-10-0-5-63 02]# kubectl apply -f kube-manifests/
storageclass.storage.k8s.io/ebs-sc created
persistentvolumeclaim/ebs-mysql-pv-claim created
configmap/usermanagement-dbcreation-script created
deployment.apps/mysql created
service/mysql created
```

查看storage class，可以看到在默认的class以外，多了一个我们创建出的ebs-sc

```
[root@ip-10-0-5-63 02]# kubectl get sc
NAME            PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
ebs-sc          ebs.csi.aws.com         Delete          WaitForFirstConsumer   false                  21s
gp2 (default)   kubernetes.io/aws-ebs   Delete          WaitForFirstConsumer   false                  4d22h
```

查看pvc及pv，确认状态是Bound，即已经被绑定

```
[root@ip-10-0-5-63 02]# kubectl get pvc
NAME                 STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
ebs-mysql-pv-claim   Bound    pvc-b2be8f83-a97f-4621-89cd-ba69336f5ce9   4Gi        RWO            ebs-sc  
[root@ip-10-0-5-63 02]# kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                        STORAGECLASS   REASON   AGE
pvc-b2be8f83-a97f-4621-89cd-ba69336f5ce9   4Gi        RWO            Delete           Bound    default/ebs-mysql-pv-claim   ebs-sc               
```

登陆数据库并测试如下，并通过show schemas查看usermgmt创建成功

```
[root@ip-10-0-5-63 eks-workshop]# kubectl run -it --rm --image=mysql:5.6 --restart=Never mysql-client -- mysql -h mysql -pdbpassword11
If you don't see a command prompt, try pressing enter.

mysql> show schemas;
+---------------------+
| Database            |
+---------------------+
| information_schema  |
| #mysql50#lost+found |
| mysql               |
| performance_schema  |
| usermgmt            |
+---------------------+
5 rows in set (0.00 sec)
```

接下来，我们通过定义UserManagementMicroservice-Deployment-Service，创建用户管理微服务，并将此应用连接到之前创建的mysql数据库，所有的用户数据都保存在usermgmt数据库中

```
apiVersion: apps/v1
kind: Deployment 
metadata:
  name: usermgmt-microservice
  labels:
    app: usermgmt-restapp
spec:
  replicas: 1
  selector:
    matchLabels:
      app: usermgmt-restapp
  template:  
    metadata:
      labels: 
        app: usermgmt-restapp
    spec:
      containers:
        - name: usermgmt-restapp
          image: stacksimplify/kube-usermanagement-microservice:1.0.0
          ports: 
            - containerPort: 8095           
          env:
            - name: DB_HOSTNAME
              value: "mysql"            
            - name: DB_PORT
              value: "3306"            
            - name: DB_NAME
              value: "usermgmt"            
            - name: DB_USERNAME
              value: "root"            
            - name: DB_PASSWORD
              value: "dbpassword11" 
```

通过kubectl apply -f kube-manifests/创建应用，并查看状态如下所示

```
[root@ip-10-0-5-63 02]# kubectl apply -f kube-manifests/
storageclass.storage.k8s.io/ebs-sc unchanged
persistentvolumeclaim/ebs-mysql-pv-claim unchanged
configmap/usermanagement-dbcreation-script unchanged
deployment.apps/mysql unchanged
service/mysql unchanged
deployment.apps/usermgmt-microservice created
service/usermgmt-restapp-service created
[root@ip-10-0-5-63 02]# kubectl get pods
NAME                                     READY   STATUS    RESTARTS   AGE
mysql-6fdd448876-jwglf                   1/1     Running   0          10h
usermgmt-microservice-6c6cdb5758-bt8zs   1/1     Running   0          11s
[root@ip-10-0-5-63 02]# kubectl get sc,pvc,pv
NAME                                        PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
storageclass.storage.k8s.io/ebs-sc          ebs.csi.aws.com         Delete          WaitForFirstConsumer   false                  10h
storageclass.storage.k8s.io/gp2 (default)   kubernetes.io/aws-ebs   Delete          WaitForFirstConsumer   false                  5d8h

NAME                                       STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
persistentvolumeclaim/ebs-mysql-pv-claim   Bound    pvc-b2be8f83-a97f-4621-89cd-ba69336f5ce9   4Gi        RWO            ebs-sc         10h

NAME                                                        CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                        STORAGECLASS   REASON   AGE
persistentvolume/pvc-b2be8f83-a97f-4621-89cd-ba69336f5ce9   4Gi        RWO            Delete           Bound    default/ebs-mysql-pv-claim   ebs-sc        10h
```

最后，通过定义UserManagement-Service，我们把用户管理应用通过nodeport暴露到公网

```
apiVersion: v1
kind: Service
metadata:
  name: usermgmt-restapp-service
  labels: 
    app: usermgmt-restapp
spec:
  type: NodePort
  selector:
    app: usermgmt-restapp
  ports: 
    - port: 8095
      targetPort: 8095
      nodePort: 31231
```

查看服务已经成功部署

```
[root@ip-10-0-5-63 02]# kubectl get svc
NAME                       TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)          AGE
kubernetes                 ClusterIP   172.20.0.1       <none>        443/TCP          5d9h
mysql                      ClusterIP   None             <none>        3306/TCP         10h
usermgmt-restapp-service   NodePort    172.20.168.161   <none>        8095:31231/TCP   15m
```

登陆网址http://<EKS-WorkerNode-Public-IP>:31231/usermgmt/health-status查看，用户管理微服务已经正常运行

![](https://github.com/xipan79/EKS-workshop/raw/main/EKS-workshop/screenprint/02/1.png)

然后，我们可以通过Postman对用户管理应用微服务进行测试，创建environment，变量为url，value为微服务Nodeport访问IP地址+端口号，如图下所示

![](https://github.com/xipan79/EKS-workshop/raw/main/EKS-workshop/screenprint/02/2.png)

将Resource中的AWS-EKS-Microservices.postman_collection.json文件下载到本地，并导入Postman，然后就可以验证如下操作，比如查询微服务健康状况

![](https://github.com/xipan79/EKS-workshop/raw/main/EKS-workshop/screenprint/02/3.png)

增加用户microtest1

![](https://github.com/xipan79/EKS-workshop/raw/main/EKS-workshop/screenprint/02/4.png)

列出所有用户

![](https://github.com/xipan79/EKS-workshop/raw/main/EKS-workshop/screenprint/02/5.png)

在Mysql数据库中查看确认

```
[root@ip-10-0-5-63 02]# kubectl run -it --rm --image=mysql:5.6 --restart=Never mysql-client -- mysql -h mysql -pdbpassword11
If you don't see a command prompt, try pressing enter.

mysql> show schemas;
+---------------------+
| Database            |
+---------------------+
| information_schema  |
| #mysql50#lost+found |
| mysql               |
| performance_schema  |
| usermgmt            |
+---------------------+
5 rows in set (0.00 sec)

mysql> use usermgmt;
Reading table information for completion of table and column names
You can turn off this feature to get a quicker startup with -A

Database changed
mysql> show tables;
+--------------------+
| Tables_in_usermgmt |
+--------------------+
| users              |
+--------------------+
1 row in set (0.00 sec)

mysql> select * from users;
+------------+------------------------+---------+------------+------------+--------------------------------------------------------------+------------+
| username   | email                  | enabled | firstname  | lastname   | password                                                     | role|
+------------+------------------------+---------+------------+------------+--------------------------------------------------------------+------------+
| microtest1 | dkalyanreddy@gmail.com |        | MicroFName | MicroLname | $2a$04$i2btKDAzifonzhC.DJm8tu2rivT8Pbh473sOLT2LPhy0pM7fO05.S | ROLE_ADMIN |
+------------+------------------------+---------+------------+------------+--------------------------------------------------------------+------------+
1 row in set (0.00 sec)
```

