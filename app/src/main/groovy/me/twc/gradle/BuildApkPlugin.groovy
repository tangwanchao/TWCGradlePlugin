package me.twc.gradle

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.ProductFlavor
import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.annotations.NotNull

class BuildApkPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.getExtensions().create("buildApkConfig", BuildApkConfig)
        project.afterEvaluate {
            def productFlavorNameList = getProductFlavorNameList(project)
            createBuildApkTasks(project, productFlavorNameList)
        }
    }

    /**
     * get productFlavor name list by android project.extensions
     *
     * @param project project
     * @return ProductFlavor name list
     */
    private static List<String> getProductFlavorNameList(Project project) {
        def result = new ArrayList<String>()
        def androidExt = project?.getExtensions()?.getByName("android") as BaseExtension
        def productFlavors = androidExt?.getProductFlavors()?.<ProductFlavor>toList() ?: new ArrayList<ProductFlavor>()
        productFlavors.forEach { productFlavor->
            result.add(productFlavor.name)
        }
        return result
    }

    /**
     * create tasks by productFlavor name list
     *
     * @param project
     * @param productFlavorNameList
     */
    private static void createBuildApkTasks(Project project, List<String> productFlavorNameList) {
        // 如果没有配置 productFlavor,默认 productFlavorName 为 ""
        if (productFlavorNameList.isEmpty()) {
            productFlavorNameList.add("")
        }
        productFlavorNameList.forEach { v ->
            createBuildApkTask(project, v)
        }
    }

    /**
     * create task by taskName
     *
     * @param project
     * @param taskName
     */
    private static void createBuildApkTask(Project project, @NotNull String productFlavorName) {
        def productFlavorNameUpperCase
        if (productFlavorName.length() == 0) {
            productFlavorNameUpperCase = ""
        } else if (productFlavorName.length() == 1) {
            productFlavorNameUpperCase = "${productFlavorName.toUpperCase()}"
        } else {
            productFlavorNameUpperCase = "${productFlavorName.substring(0, 1).toUpperCase()}${productFlavorName.substring(1)}"
        }
        project.task("build${productFlavorNameUpperCase}Apks") {
            dependsOn("assemble${productFlavorNameUpperCase}Release")
            group 'twc'
            doLast {
                // 获取签名配置
                def releaseSignConfig = getReleaseSignConfig(project)
                if (releaseSignConfig == null) {
                    throw new RuntimeException("你应该在 build.gradle(app) 配置 signingConfigs 并添加 release 配置")
                }
                // 获取构建 apk 配置
                def buildApkConfig = project?.getExtensions()?.findByType(BuildApkConfig.class)

                // 获取 assembleRelease apk 输出文件夹
                def assembleReleaseOutputDir
                if (productFlavorName.length() == 0) {
                    assembleReleaseOutputDir = "${project.buildDir.path}/outputs/apk/release"
                } else {
                    assembleReleaseOutputDir = "${project.buildDir.path}/outputs/apk/${productFlavorName}/release"
                }


                def outputJson = new JsonSlurper().parse(new File("$assembleReleaseOutputDir/output.json")) as ArrayList
                def inputApkName = (outputJson.get(0) as Map).get("path") as String
                def inputApkPath = "${assembleReleaseOutputDir}/$inputApkName"
                if (buildApkConfig.appName != null){
                    File renameFile = new File("${assembleReleaseOutputDir}/${buildApkConfig.appName}.apk")
                    new File(inputApkPath).renameTo(renameFile)
                    inputApkName = renameFile.name
                    inputApkPath = renameFile.path
                }
                def useProtect = buildApkConfig.use360Protect()
                def useChannels = buildApkConfig.useChannels()
                if (!useChannels && !useProtect) {
                    throw new RuntimeException("你在干啥?多渠道打包功能和加固功能都不使用?")
                }

                def twcDirPath = "${assembleReleaseOutputDir}/twc"
                // 删除已有文件夹
                new File(twcDirPath)?.deleteDir()
                // 创建新文件夹
                def twcDir = new File(twcDirPath)
                twcDir.mkdir()
                def protectDir = new File("protect", twcDir)
                protectDir.mkdir()
                def zipDir = new File("zip", twcDir)
                zipDir.mkdir()
                def signDir = new File("signs", twcDir)
                signDir.mkdir()

                try {
                    println("加固流程开始---")
                    if (useProtect) {
                        protect(project, buildApkConfig, inputApkPath, protectDir.path)
                    }else{
                        println("跳过加固")
                    }
                    println("加固流程完成---")

                    println("签名流程开始---")
                    if (useProtect){
                        def needSignApkFile = protectDir.listFiles()[0]
                        def zipOutApkFilePath = "${zipDir.path}/${needSignApkFile.name}"
                        def signedApkFilePath = "${signDir.path}/${inputApkName.replace(".apk","_protect_sign.apk")}"
                        zipalign(project,needSignApkFile.path,zipOutApkFilePath)
                        signApk(project,releaseSignConfig,zipOutApkFilePath,signedApkFilePath)
                    }else{
                        println("未使用加固,跳过重新签名流程,将使用原始 apk 进行多渠道打包")
                        File renameFile = new File(inputApkPath.replace(".apk","_sign.apk"))
                        new File(inputApkPath).renameTo(renameFile)
                        inputApkName = renameFile.name
                        inputApkPath = renameFile.path
                    }
                    println("签名流程结束---")

                    println("多渠道打包流程开始---")
                    if (useChannels) {
                        def channelsInputApkPath
                        if (useProtect) {
                            channelsInputApkPath = signDir.listFiles()[0].path
                        } else {
                            channelsInputApkPath = inputApkPath
                        }
                        channels(project, buildApkConfig, channelsInputApkPath,twcDir.path)
                    } else {
                        println("跳过多渠道打包---")
                        def signedApkFile = signDir.listFiles()[0]
                        if(!signedApkFile.renameTo(new File(signedApkFile.name,twcDir))){
                            throw new RuntimeException("移动文件失败")
                        }
                    }
                    println("多渠道打包流程完成---")

                } finally {
                    protectDir?.deleteDir()
                    signDir?.deleteDir()
                    zipDir?.deleteDir()
                }
            }
        }
    }

    /**
     * 执行 360 加固
     * @param project project
     * @param config BuildApkConfig
     * @param inputApkPath 需要加固的 apk
     * @param outputDirPath 加固后输出文件夹路径
     */
    private static void protect(Project project, BuildApkConfig config, String inputApkPath, String outputDirPath) {
        println("执行加固-登录360加固保")
        project.exec {
            executable = 'java'
            args = ['-jar', config.getJarProtectPath(),
                    '-login', config.getAccount(), config.getPassword()]

        }
        println("执行加固-登录360加固保成功")
        println("执行加固-初始化配置")
        project.exec {
            executable = 'java'
            args = ['-jar', config.getJarProtectPath(),
                    '-config']
        }
        println("执行加固-初始化配置成功")
        println("执行加固-构建加固后 apk")
        project.exec {
            executable = 'java'
            args = ['-jar', config.getJarProtectPath(),
                    '-jiagu', inputApkPath, outputDirPath]
        }
        println("执行加固-构建加固后 apk 成功")
    }

    /**
     * 多渠道打包
     * @param project
     * @param config
     * @param inputApkPath 需要多渠道打包的原始 apk
     * @param outputDirPath 多渠道打包输出文件夹路径
     */
    private static void channels(Project project, BuildApkConfig config, String inputApkPath, String outputDirPath) {
        println("开始 VasDolly 多渠道打包---")
        println("渠道信息: ${config.getChannels()}")
        project.exec {
            executable = 'java'
            args = ['-jar', config.getJarVasDollyPath(),
                    "put","-c", config.getChannels(), inputApkPath, outputDirPath]
        }
        println("VasDolly 多渠道打包完成---")
    }


    /**
     *  zipalign 对齐
     */
    private static void zipalign(Project project, String inputApkPath, String outputApkPath) {
        project.exec {
            executable = 'zipalign'
            args = ['-f', '-p', 4,
                    inputApkPath, outputApkPath]
        }
        project.exec {
            executable = 'zipalign'
            args = ['-c', 4, outputApkPath]
        }
    }

    /**
     * 对 apk 进行签名
     */
    private static signApk(Project project, Map signConfig, String inputApkPath, String outputApkPath) {
        project.exec {
            executable = 'apksigner'
            args = ['sign',
                    '--v1-signing-enabled',signConfig.get("v1SigningEnabled"),
                    '--v2-signing-enabled',signConfig.get("v2SigningEnabled"),
                    '--ks',signConfig.get("storeFile"),
                    '--ks-pass',"pass:${signConfig.get("storePassword")}",
                    '--ks-key-alias',signConfig.get("keyAlias"),
                    '--key-pass',"pass:${signConfig.get("keyPassword")}",
                    '--out',outputApkPath,
                    inputApkPath]
        }
        project.exec {
            executable = 'apksigner'
            args = ['verify',
                    outputApkPath]
        }
    }

    /**
     * @param project project
     * @return release signingConfig map,or null
     */
    private static Map getReleaseSignConfig(Project project) {
        def result = null
        def androidProperties = project?.getExtensions()?.getByName("android")?.properties
        androidProperties.forEach { k, v ->
            if (k == "signingConfigs") {
                (v as Set).forEach { signConfig ->
                    def signConfigProperties = signConfig.properties
                    if (signConfigProperties.get("name") == "release") {
                        result = signConfigProperties
                    }
                }
            }
        }
        return result
    }
}