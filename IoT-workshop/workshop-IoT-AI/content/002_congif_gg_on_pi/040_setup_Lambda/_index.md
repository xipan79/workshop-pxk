---
title: "3. 创建用于边缘推理的 Lambda 函数"
date: 2018-10-03T10:17:52-07:00
draft: false
weight: 40
---

1. 在你的电脑上点击以下链接下载 Lambda 代码：

    [https://greengrass-ml-hands-on-lab.s3.cn-north-1.amazonaws.com.cn/HumanActionDetect.zip](https://greengrass-ml-hands-on-lab.s3.cn-north-1.amazonaws.com.cn/HumanActionDetect.zip)

    解压以后，编辑HumanActionDetect.py文件，在代码的93行与103行，将 `your/action` 替换成你自己的 topic，比如 `edwin/action`，并保存文件。
    ![Image](/images/png/001.png)

2. 将 lambda 代码重新打包，注意：请在 HumanActionDetect 目录中运行打包，不要在上层目录中打包。

    ![Image](/images/png/12.png)

3. 在AWS IOT Greengrass console点击选中刚才创建的 Grassgrass 组，比如GG_ML_edwin：

    * 在打开的 console 中点击 Lambda， 添加Lambda，新建 Lambda, 如下图所示：

    ![Image](/images/png/13.png)

    ![Image](/images/png/14.png)

4. 在创建 Lambda 的界面进行配置，选择从头开始创作，函数名称请输入你自己定义的名字，比如 GG_ML_edwin，运行时选择 Python 3.7。点击创建函数。

    ![Image](/images/png/15.png)

5. 在 Lambda 代码的编辑界面上方，点击操作，上传 zip 文件，并选择刚才打包好的 lambda函数，点击上传

    ![Image](/images/png/16.png)

6. 将页面拖到下方，在基本设置中点击“编辑”按钮，将处理程序改成HumanActionDetect.function_handler并保存。

    ![Image](/images/png/17.png)

    ![Image](/images/png/18.png)

7. 再在 Lambda 界面的上方点击”操作“，发布新版本，输入版本为 1，点击”发布“

    ![Image](/images/png/19.png)

    继续点击”操作“，选中”创建别名“，创建一个别名为 ”production“的 Lambda 别名，并关联版本为”1“，点击”保存“。

    ![Image](/images/png/20.png)

    ![Image](/images/png/21.png)