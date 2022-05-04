---
title: "部署与验证"
weight: 13
chapter: false
draft: false
---

在Greengrass组的界面右上角，选中”操作“，”部署“，即可将刚才我们配置的所有参数、代码以及机器学习的模型部署到树莓派上，如下图所示：
![Image](/images/png/42.png)

验证模式是否成功部署， Lambda 是否正常运行

在 AWS IOT console ，点击”测试“ ，并在右侧的 subscription topic 里面输入 xxxx/action (刚才你自己设定的 topic)，点击 subscript to topic。

此时你在树莓派摄像头前可以做各种动作，比如将手机放到耳边，做打电话状。

保持这个动作**几秒钟**，你就能在 Console 界面看到推理的结果。

如下图所示：
![Image](/images/png/43.png)
