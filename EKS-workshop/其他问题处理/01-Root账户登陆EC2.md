## 01. Root账号登陆EC2

vim /etc/ssh/sshd_config，去掉PermitRootLgin Yes前的注释，如下所示

```
#LoginGraceTime 2m
PermitRootLogin yes
#StrictModes yes
#MaxAuthTries 6
#MaxSessions 10
```

vim .ssh/authorized_keys，删除掉如下一段

```
no-port-forwarding,no-agent-forwarding,no-X11-forwarding,command="echo 'Please login as the user \"ec2-user\" rather than the user \"root\".';echo;sleep 10"
```

重启sshd服务

```
systemctl restart sshd
```

通过下面的Link访问dashboard

```
38f9d3628075:demo panxiank$ ssh -i "xxx.pem" root@ec2-44-198-186-243.compute-1.amazonaws.com
The authenticity of host 'ec2-44-198-186-243.compute-1.amazonaws.com (44.198.186.243)' can't be established.
ECDSA key fingerprint is SHA256:9Ng4swOiMBZkUCApLgmU2dE3RZxPm6+eHSTEPZkQGbA.
Are you sure you want to continue connecting (yes/no/[fingerprint])? yes
Warning: Permanently added 'ec2-44-198-186-243.compute-1.amazonaws.com,44.198.186.243' (ECDSA) to the list of known hosts.
Last login: Thu Apr 14 04:06:44 2022

       __|  __|_  )
       _|  (     /   Amazon Linux 2 AMI
      ___|\___|___|

https://aws.amazon.com/amazon-linux-2/
[root@ip-172-31-11-119 ~]# 
```



