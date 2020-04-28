package me.twc.gradle

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
        def androidProperties = project?.getExtensions()?.getByName("android")?.properties
        androidProperties.forEach { k, v ->
            if (k == "productFlavors") {
                (v as Set).forEach { productFlavorsValue ->
                    result.add(productFlavorsValue.properties.get("name"))
                }
            }
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
        def productFlavorNames = new ArrayList<String>()
        if (productFlavorNameList.isEmpty()) {
            productFlavorNames.add("")
        } else {
            productFlavorNameList.forEach { v ->
                productFlavorNames.add(v)
            }
        }
        productFlavorNames.forEach { productFlavorName ->
            createBuildApkTask(project, productFlavorName)
        }
    }

    /**
     * create task by taskName
     *
     * @param project
     * @param taskName
     */
    private static void createBuildApkTask(Project project,@NotNull String productFlavorName) {
        def productFlavorNameUpperCase
        if (productFlavorName.length() == 0){
            productFlavorNameUpperCase = ""
        }else if (productFlavorName.length() == 1){
            productFlavorNameUpperCase = "${productFlavorName.toUpperCase()}"
        }else{
            productFlavorNameUpperCase = "${productFlavorName.substring(0,1).toUpperCase()}${productFlavorName.substring(1)}"
        }
        project.task("build${productFlavorNameUpperCase}Apks") {
            dependsOn("assemble${productFlavorNameUpperCase}Release")
            group 'twc'
            doLast {
                def releaseSignConfig = getReleaseSignConfig(project)
                if (releaseSignConfig == null) {
                    throw new RuntimeException("你应该在 build.gradle(app) 配置 signingConfigs 并添加 release 配置")
                }
                def buildApkConfig = project?.getExtensions()?.findByType(BuildApkConfig.class)
                if (!buildApkConfig.check()) {
                    throw new RuntimeException("你应该在 build.gradle(app) 配置 buildApkConfig")
                }

                def assembleReleaseOutputDir
                if (productFlavorName.length() == 0){
                    assembleReleaseOutputDir = "${project.buildDir.path}/outputs/apk/release"
                }else{
                    assembleReleaseOutputDir = "${project.buildDir.path}/outputs/apk/${productFlavorName}/release"
                }

                def outputJson = new JsonSlurper().parse(new File("$assembleReleaseOutputDir/output.json")) as ArrayList
                def inputApkName = (outputJson.get(0) as Map).get("path") as String

                def outputDirPath = "${assembleReleaseOutputDir}/channelApks"
                // 删除已有文件夹
                new File(outputDirPath)?.deleteDir()
                new File(outputDirPath)?.mkdir()
                // 执行加固-登录360加固保
                println("开始登陆加固宝---")
                project.exec {
                    executable = 'java'
                    args = ['-jar', buildApkConfig.getJarPath(),
                            '-login', buildApkConfig.getAccount(), buildApkConfig.getPassword()]

                }
                println("登陆加固宝成功---")
                println("开始导入签名信息---")
                // 执行加固-导入签名信息
                project.exec {
                    executable = 'java'
                    args = ['-jar', buildApkConfig.getJarPath(),
                            '-importsign', releaseSignConfig.get("storeFile"), releaseSignConfig.get("storePassword"), releaseSignConfig.get("keyAlias"), releaseSignConfig.get("keyPassword")]
                }
                // 执行加固-导入渠道信息
                if (buildApkConfig.getJarPath() != null){
                    println("开始导入渠道信息---")
                    project.exec {
                        executable = 'java'
                        args = ['-jar', buildApkConfig.getJarPath(),
                                '-importmulpkg', buildApkConfig.getChannelFilePath()]
                    }
                    println("导入渠道信息成功---")
                }else{
                    println("跳过多渠道配置---")
                }


                // 执行加固-查看360加固签名信息
                project.exec {
                    executable = 'java'
                    args = ['-jar', buildApkConfig.getJarPath(),
                            '-showsign']
                }

                // 执行加固-查看360加固渠道信息
                project.exec {
                    executable = 'java'
                    args = ['-jar', buildApkConfig.getJarPath(),
                            '-showmulpkg']
                }
                // 执行加固-初始化加固服务配置,后面可不带参数
                project.exec {
                    executable = 'java'
                    args = ['-jar', buildApkConfig.getJarPath(),
                            '-config']
                }
                // 执行加固
                project.exec {
                    executable = 'java'
                    args = ['-jar', buildApkConfig.getJarPath(),
                            '-jiagu', "${assembleReleaseOutputDir}/$inputApkName", outputDirPath, '-autosign','-automulpkg']
                }
                println("加固完成---")
            }
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