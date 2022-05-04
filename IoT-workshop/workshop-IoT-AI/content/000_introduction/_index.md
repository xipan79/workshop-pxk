---
title: "OverView"
date: 2018-10-03T10:17:52-07:00
draft: false
weight: 10
---

![Image](/images/png/1.png)

在这个实验中，我们将用树莓派来模拟一个 IOT 智能摄像头。

在树莓派上部署 AWS Greengrass ，它可以从云端加载一个用于探测人体动作的模型以及用于推理的 lambda 代码，

每秒钟生成一个推理的结果并发送到 AWS IOT 云端（Lambda程序设计为仅发送置信度高于 95%的结果）。
