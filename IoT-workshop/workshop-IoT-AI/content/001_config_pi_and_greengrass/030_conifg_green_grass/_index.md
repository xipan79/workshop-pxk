---
title: "2. 安装 Greengrass 软件"
date: 2018-10-03T10:17:52-07:00
draft: false
weight: 30
---

1.	重新 SSH 到树莓派上，运行以下命令以安装 Greengrass ，并下载 Amazon rootCA以用于安全认证。
    ```shell
    wget -O /tmp/greengrass.tar.gz https://d1onfpft10uf5o.cloudfront.net/greengrass-core/downloads/1.10.2/greengrass-linux-armv7l-1.10.2.tar.gz
    sudo mkdir -p /greengrass
    sudo tar -xzvf /tmp/greengrass.tar.gz -C /
    rm /tmp/greengrass.tar.gz
    cd /greengrass/certs/
    sudo wget -O root.ca.pem https://www.amazontrust.com/repository/AmazonRootCA1.pem
    ```

2.	在 AWS IoT 上配置 AWS IoT Greengrass

    在您的计算机上登录 AWS 管理控制台 并打开[AWS IoT 控制台](https://console.amazonaws.cn/iot/home?region=cn-north-1#/dashboard)。

    在导航窗格中，选择 **Greengrass**

    在 **Welcome to AWS IoT Greengrass (欢迎使用 AWS IoT Greengrass)** 页面上，选择 **Create a Group (创建组)**。

    如果出现提示，则在 **Greengrass needs your permission to access other services (Greengrass 需要访问其他服务的权限)** 对话框中，选择 **Grant permission (授予权限)** 以允许控制台为您创建或配置 Greengrass 服务角色。

    您必须使用服务角色授权 AWS IoT Greengrass 代表您访问其他 AWS 服务。否则，部署会失败。
    ![Image](/images/png/6.png)

    在 **Set up your Greengrass group (设置您的 Greengrass 组)** 页面上，选择 **Use default creation (使用默认创建方式)** 以创建一个组和一个 [AWS IoT Greengrass 核心](https://docs.aws.amazon.com/zh_cn/greengrass/latest/developerguide/gg-core.html)。

    每个组需要一个核心，而核心是一个管理本地 IoT 进程的设备。
    
    核心需要一个允许它访问 AWS IoT 的证书和相应密钥以及一个允许它执行 AWS IoT 和 AWS IoT Greengrass 操作的 [AWS IoT 策略](https://docs.aws.amazon.com/iot/latest/developerguide/iot-policies.html)。
    
    当您选择 **Use default creation (使用默认创建方式)** 选项时，这些安全资源是为您创建的，并且核心是在 AWS IoT 注册表中预配置的。

    ![Image](/images/png/7.png)

    给你的 Greengrass 组取一个名字，**为了避免与其他实验者取名冲突**（因为大家共享一个中国区账号），建议取 `GG_ML_xxx`（其中 xxx 是你自己的名字），以便识别你自己的 Greengrass 组。

    ![Image](/images/png/8.png)

    **点击两次下一步**，并点击创建组和核心设备，AWS IoT 使用默认安全策略创建 AWS IoT Greengrass 组并创建配置文件以加载到设备上。

    ![Image](/images/png/9.png)

    在确认页面上，在 **Download and store your Core's security resources (下载并存储您的核心的安全资源)** 下，选择 **Download these resources as a tar.gz (将这些资源作为一个 tar.gz 下载)**。
    
    您下载的 tar.gz 文件的名称以 10 位哈希值开头，该哈希值也用于证书和密钥文件名。

    ![Image](/images/png/10.png)

    **点击完成。**

4.	在树莓派上配置并运行 Greengrass

    将刚才下载的 tar.gz 文件复制到树莓派上，可以用 SCP 命令或者是远程桌面（树莓派已经预先激活了 RDP 远程桌面功能）。以下是在**你自己的电脑上**用 SCP 命令复制文件的示例：

    ```shell
    cd /directory/of/gg/configure # 刚才下载保存tar.gz 文件的目录
    scp ./yyyyyyyyyy-setup.tar.gz pi@xxx.xxx.xxx.xxx:/tmp 
    # yyyyyyyyyy-setup.tar.gz是下载的配置文件的文件名，xxx.xxx.xxx.xxx是树莓派的 IP 地址
    ```

    * 如果提示No space left on device，可以SSH 到树莓派上执行 sudo rm -rf /tmp/*，之后再执行 scp 命令

    SSH 到树莓派上，执行以下命令将 Greengrass配置文件解压到/greengrass 目录中，并启动 greengrass 守护进程：

    ```shell
    sudo tar -xzvf /tmp/yyyyyyyy-setup.tar.gz -C /greengrass # yyyyyyyy-setup.tar.gz是复制到树莓派的配置文件
    sudo /greengrass/ggc/core/greengrassd start
    ```

    您可以运行以下命令来确认 AWS IoT Greengrass Core 软件（Greengrass 守护程序）是否正常工作。将 PID-number 替换为您的 PID：

    ```shell
    ps aux | grep PID-number
    ```

    如果要设置 greengrass 在开机时自动启动，可以参考配置初始化系统以启动 [Greengrass 守护程序](https://docs.aws.amazon.com/zh_cn/greengrass/latest/developerguide/gg-core.html#start-on-boot)

    如果你的 greengrass 进程启动出现问题，可以用以下命令从日志中排查相关问题：

    ```shell
    sudo cat /greengrass/ggc/var/log/crash.log
    sudo cat /greengrass/ggc/var/log/system/runtime.log
    ```