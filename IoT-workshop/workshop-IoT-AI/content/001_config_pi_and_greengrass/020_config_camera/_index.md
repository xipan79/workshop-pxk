---
title: "1. 安装摄像头"
date: 2018-10-03T10:17:52-07:00
draft: false
weight: 20
---
1. 安装摄像头
    请将实验材料中的摄像头的数据线插入树莓派的摄像头接口中，如图所示：

{{% notice warning %}}
注意摄像头排线的安装方向
{{% /notice%}}

![Image](/images/png/1.png)

2. SSH连接到树莓派

    请用你熟悉的 SSH 工具连接树莓派，树莓派的 IP 地址在你手头的实验提示文档上。用户名为 `pi`，密码为 `awsworkshop`。
    
    登录后会提示如下信息：

    ![Image](/images/png/2.png)

3. 配置激活摄像头
    在命令行提示符下运行：

    ```shell
    sudo raspi-config
    ```

    您将看到以下内容：

    ![Image](/images/png/3.png)

    选择 5 Interfacing Options回车，选择 P1 Camera并回车，选择 Yes并回车以激活 camera。按 ESC 退出配置界面。

    ![Image](/images/png/4.png)

4. OS 参数设置
    在树莓派 OS 的命令行中运行如下命令：
    ```shell
    sudo adduser --system ggc_user
    sudo addgroup --system ggc_group
    cd /etc/sysctl.d
    sudo nano 98-rpi.conf
    ```

    使用文本编辑器（nano）将以下两行添加到文件的末尾。按 control+x 退出，并输入 yes以确认保存，回车保存文件。
    
    ```shell
    fs.protected_hardlinks = 1
    fs.protected_symlinks = 1
    ```
    ![Image](/images/png/5.png)

    在命令行中输入命令：

    ```shell
    sudo nano /boot/cmdline.txt
    ```

    将以下内容附加到现有行的末尾，**而不是作为新行**。

    ```shell
    cgroup_enable=memory cgroup_memory=1
    ```

    现在重启 树莓派。

    ```shell
    sudo reboot
    ```