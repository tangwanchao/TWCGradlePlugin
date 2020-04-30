[![](https://jitpack.io/v/tangwanchao/TWCGradlePlugin.svg)](https://jitpack.io/#tangwanchao/TWCGradlePlugin)
---


## 说明

该插件适合需要多渠道打包或 360 加固的人使用

## 所需环境及第三方 jar
1. Java 环境
2. [360加固宝及其账号](https://jiagu.360.cn/#/global/index)
3. [VasDolly jar](https://github.com/Tencent/VasDolly)

## 功能
1. 360 加固   
2. VasDolly 多渠道打包
3. 支持 productFlavors 配置

## 使用

1. build.gradle(project) 添加插件
```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        // 具体版本号请查看 jitpack
        classpath 'com.github.tangwanchao:TWCGradlePlugin:1.0.3'
    }
}
```
2. build.gradle(app) 依赖插件，填写配置
```groovy
apply plugin: 'twc-build-apk'
android{
    ...
}
...
// 如果使用加固功能，需要配置 jarProtectPath，account，password
// 如果使用多渠道打包功能，需要配置 channels，jarVasDollyPath
// 两个功能至少选择一个使用，可同时选择使用。
buildApkConfig {
    // 360 加固 jar 路径
    jarProtectPath ''
    // 360 加固账号
    account ''
    // 360 加固密码
    password ''
    // 多渠道配置，用","分割
    channels ''
    // VasDolly jar 路径
    jarVasDollyPath ''
    // app 名字,可选
    appName ''
}
```
3. sync，之后 gradle 工具窗口将会出现相关任务

## 注意事项

1. 如果添加插件后添加 PATH 环境，需要重启 AndroidStudio   
2. 签名读取的是 signingConfigs.release 配置,如果没有该配置可能无法正常工作