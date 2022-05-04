## 10. EKS配置ALB Ingress SSL

如果已有证书，可上传至AWS Certificate Manager，否则，需要先在ACM中申请一个证书，如下图所示

![]()

证书申请后，需创建DNS记录验证，验证通过后状态变为成功，记录下证书的ARN

![]()

创建部署ALB SSL Ingress，主要需要增加如下annotation，定义HTTPS443端口，以及证书的ARN，通常可以直接用默认的SSL-Policy，如果有特殊需要也可以自己定义

```
    ## SSL Settings
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}, {"HTTP":80}]'
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:8082xxxx3800:certificate/006dde6e-5fd3-429b-a6c3-a62c1b62xxxx
    #alb.ingress.kubernetes.io/ssl-policy: ELBSecurityPolicy-TLS-1-1-2017-01 #Optional (Picks default if not used)  
```

如果需要定义80端口Redirect到443，那么再额外增加如下注释：

```
    # SSL Redirect Setting
    alb.ingress.kubernetes.io/ssl-redirect: '443'   
```

在R53中创建一条DNS记录

![]()

访问验证，已经实现安全的SSL访问

![]()

