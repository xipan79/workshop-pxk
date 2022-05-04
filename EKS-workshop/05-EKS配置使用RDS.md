## 05. EKS配置使用RDS

EKS可以使用external-name-service访问外部服务，比如典型应用场景之一就是访问RDS。为了创建RDS Mysql，首先我们需要创建一个安全组，将3306端口开放，如下图所示

![](https://github.com/xipan79/EKS-workshop/raw/main/EKS-workshop/screenprint/05/1.png)

其次，我们需要定义subnet group，如下图所示

![](https://github.com/xipan79/EKS-workshop/blob/main/EKS-workshop/screenprint/05/2.png)

然后，我们可以创建RDS Mysql，比如可通过如下步骤及参数设置创建一个RDS Mysql

```
Go to Services -> RDS
Click on Create Database
Choose a Database Creation Method: Standard Create
Engine Options: MySQL
Edition: MySQL Community
Version: 5.7.22 
Template Size: Free Tier
DB instance identifier: usermgmtdb
Master Username: dbadmin
Master Password: dbpassword11
Confirm Password: dbpassword11
DB Instance Size: leave to defaults
Storage: leave to defaults
Connectivity
VPC: eksctl-eks-121-cluster/VPC
Additional Connectivity Configuration
Subnet Group: eks-rds-subnetgroup
Publicyly accessible: NO 
VPC Security Group: eks-rds-securitygroup
Availability Zone: No Preference
Database Port: 3306
Rest all leave to defaults
Click on Create Database
```

创建如下eks-externalname-service.yaml

```
apiVersion: v1
kind: Service
metadata:
  name: mysql
spec:
  type: ExternalName
  externalName: usermgmtdb.cklkabcdxxxx.us-east-1.rds.amazonaws.com

```

部署eks-externalname-service.yaml

[root@ip-10-0-5-63 05]# kubectl apply -f  mysql-externalname-service.yaml
service/mysql created

[root@ip-10-0-5-63 05]# kubectl get svc
NAME         TYPE           CLUSTER-IP   EXTERNAL-IP                                           PORT(S)   AGE
kubernetes   ClusterIP      172.20.0.1   <none>                                                443/TCP   7d5h
mysql        ExternalName   <none>       usermgmtdb.cklkabcdxxxx.us-east-1.rds.amazonaws.com   <none>    24m

RDS创建成功后，获取RDS访问Endpoint，并进行测试

```
[root@ip-10-0-5-63 05]# kubectl run -it --rm --image=mysql:5.7.22 --restart=Never mysql-client -- mysql -h usermgmtdb.cklkabcdxxxx.us-east-1.rds.amazonaws.com -u dbadmin -pdbpassword11
If you don't see a command prompt, try pressing enter.

mysql> show schemas;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| innodb             |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
5 rows in set (0.00 sec)

mysql> create database usermgmt;
Query OK, 1 row affected (0.00 sec)

mysql> show schemas;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| innodb             |
| mysql              |
| performance_schema |
| sys                |
| usermgmt           |
+--------------------+
6 rows in set (0.01 sec)
```
