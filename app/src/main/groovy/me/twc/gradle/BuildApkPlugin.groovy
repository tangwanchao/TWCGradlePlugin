package me.twc.gradle

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.dsl.BuildType
import com.android.build.gradle.internal.dsl.ProductFlavor
import com.android.build.gradle.internal.dsl.SigningConfig
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
        def productFlavors = getAppModuleExtension(project)?.getProductFlavors()?.<ProductFlavor> toList() ?: new ArrayList<ProductFlavor>()
        productFlavors.forEach { productFlavor ->
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
            createDebugBuildApkTask(project,v)
            createReleaseBuildApkTask(project, v)
        }
    }

    /**
     * 创建 debug buildApkTask
     * @param project
     * @param productFlavorName
     */
    private static void createDebugBuildApkTask(Project project,@NotNull String productFlavorName){
        String pfnFirstUppercase = StringUtils.firstToUpperCase(productFlavorName)
        project.task("build${pfnFirstUppercase}DebugApks"){
            dependsOn("assemble${pfnFirstUppercase}Debug")
            group 'twc'
            doLast {
                // 获取签名配置
                SigningConfig debugSignConfig = getDebugSignConfig(project)
                if (debugSignConfig == null){
                    throw new RuntimeException("你应该在 build.gradle(app) 配置 signingConfigs 并添加 debug 配置")
                }
                // 获取构建 apk 配置
                BuildApkConfig buildApkConfig = project?.getExtensions()?.findByType(BuildApkConfig.class)
                if (buildApkConfig == null) {
                    throw new RuntimeException("你应该在 build.gradle(app) 配置 buildApkConfig")
                }
                final String debugApkOutPutDirPath = getApkOutputDirPath(project,productFlavorName,"debug")
                def inputApkName = FileUtil.getLatestApkName(debugApkOutPutDirPath)
                def inputApkPath = "${debugApkOutPutDirPath}/$inputApkName"
                final String appNameForProductFlavor = buildApkConfig.getAppNameByProductFlavor(productFlavorName)
                if (appNameForProductFlavor != null && !appNameForProductFlavor.isEmpty()) {
                    File renameFile = new File("${debugApkOutPutDirPath}/${appNameForProductFlavor}.apk")
                    new File(inputApkPath).renameTo(renameFile)
                    inputApkName = renameFile.name
                    inputApkPath = renameFile.path
                }
                final boolean useProtect = buildApkConfig.use360Protect()
                final boolean usePgyer = buildApkConfig.usePgyer()
                if (!useProtect && !usePgyer){
                    throw new RuntimeException("你在干啥? debug 包蒲公英和加固都不实用?")
                }

                def twcDirPath = "${debugApkOutPutDirPath}/twc"
                // 删除已有文件夹
                new File(twcDirPath)?.deleteDir()
                // 创建新文件夹
                def twcDir = new File(twcDirPath)
                twcDir.mkdir()
                def protectDir = new File("protect", twcDir)
                protectDir.mkdir()
                def zipDir = new File("zip", twcDir)
                zipDir.mkdir()
                try {
                    println("加固流程开始---")
                    if (useProtect) {
                        protect(project, buildApkConfig, inputApkPath, protectDir.path)
                    } else {
                        println("跳过加固")
                    }
                    println("加固流程完成---")

                    println("签名流程开始---")
                    if (useProtect) {
                        def needSignApkFile = protectDir.listFiles()[0]
                        def zipOutApkFilePath = "${zipDir.path}/${needSignApkFile.name}"
                        def signedApkFilePath = "${twcDir.path}/${inputApkName.replace(".apk", "_protect_sign.apk")}"
                        zipalign(project, needSignApkFile.path, zipOutApkFilePath)
                        signApk(project, debugSignConfig, zipOutApkFilePath, signedApkFilePath)
                    } else {
                        println("未使用加固,跳过重新签名流程")
                        File renameFile = new File(twcDir.path,inputApkName.replace(".apk", "_sign.apk"))
                        new File(inputApkPath).renameTo(renameFile)
                    }
                    println("签名流程结束---")

                    def apkListFiles = twcDir.listFiles(new FilenameFilter() {
                        @Override
                        boolean accept(File dir, String name) {
                            return name.endsWith(".apk")
                        }
                    })
                    assert apkListFiles.length == 1
                    println("上传到蒲公英流程开始---")
                    if (usePgyer){
                        String uploadPath = apkListFiles[0].path
                        uploadToPgyer(project,uploadPath,buildApkConfig.getPgyerApiKey())
                    }else{
                        println("不使用蒲公英内测")
                    }
                    println("上传到蒲公英流程结束---")

                }finally{
                    protectDir?.deleteDir()
                    zipDir?.deleteDir()
                }
            }
        }
    }

    /**
     * 创建 release buildApkTask
     *
     * @param project
     * @param taskName
     */
    private static void createReleaseBuildApkTask(Project project, @NotNull String productFlavorName) {
        String pfnFirstUppercase = StringUtils.firstToUpperCase(productFlavorName)
        project.task("build${pfnFirstUppercase}ReleaseApks") {
            dependsOn("assemble${pfnFirstUppercase}Release")
            group 'twc'
            doLast {
                // 获取签名配置
                def releaseSignConfig = getReleaseSignConfig(project)
                if (releaseSignConfig == null) {
                    throw new RuntimeException("你应该在 build.gradle(app) 配置 signingConfigs 并添加 release 配置")
                }
                // 获取构建 apk 配置
                def buildApkConfig = project?.getExtensions()?.findByType(BuildApkConfig.class)
                if (buildApkConfig == null) {
                    throw new RuntimeException("你应该在 build.gradle(app) 配置 buildApkConfig")
                }

                String releaseApkOutPutDirPath = getApkOutputDirPath(project,productFlavorName,"release")
                def inputApkName = FileUtil.getLatestApkName(releaseApkOutPutDirPath)
                def inputApkPath = "${releaseApkOutPutDirPath}/$inputApkName"
                String appNameForProductFlavor = buildApkConfig.getAppNameByProductFlavor(productFlavorName)
                if (appNameForProductFlavor != null && !appNameForProductFlavor.isEmpty()) {
                    File renameFile = new File("${releaseApkOutPutDirPath}/${appNameForProductFlavor}.apk")
                    new File(inputApkPath).renameTo(renameFile)
                    inputApkName = renameFile.name
                    inputApkPath = renameFile.path
                }
                def useProtect = buildApkConfig.use360Protect()
                def useChannels = buildApkConfig.useChannels()
                if (!useChannels && !useProtect) {
                    throw new RuntimeException("你在干啥?多渠道打包功能和加固功能都不使用?")
                }

                def twcDirPath = "${releaseApkOutPutDirPath}/twc"
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
                    } else {
                        println("跳过加固")
                    }
                    println("加固流程完成---")

                    println("签名流程开始---")
                    if (useProtect) {
                        def needSignApkFile = protectDir.listFiles()[0]
                        def zipOutApkFilePath = "${zipDir.path}/${needSignApkFile.name}"
                        def signedApkFilePath = "${signDir.path}/${inputApkName.replace(".apk", "_protect_sign.apk")}"
                        zipalign(project, needSignApkFile.path, zipOutApkFilePath)
                        signApk(project, releaseSignConfig, zipOutApkFilePath, signedApkFilePath)
                    } else {
                        println("未使用加固,跳过重新签名流程,将使用原始 apk 进行多渠道打包")
                        File renameFile = new File(inputApkPath.replace(".apk", "_sign.apk"))
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
                        channels(project, buildApkConfig, channelsInputApkPath, twcDir.path)
                    } else {
                        println("跳过多渠道打包---")
                        def signedApkFile = signDir.listFiles()[0]
                        if (!signedApkFile.renameTo(new File(signedApkFile.name, twcDir))) {
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
            if (config.isSupportX86()){
                args('-x86')
            }
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
                    "put", "-c", config.getChannels(), inputApkPath, outputDirPath]
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
    private static signApk(Project project, SigningConfig signConfig, String inputApkPath, String outputApkPath) {
        println("签名信息:$signConfig")
        project.exec {
            executable = 'apksigner'
            args = ['sign',
                    '--v1-signing-enabled', signConfig.v1SigningEnabled,
                    '--v2-signing-enabled', signConfig.v2SigningEnabled,
                    '--ks', signConfig.storeFile,
                    '--ks-pass', "pass:${signConfig.storePassword}",
                    '--ks-key-alias', signConfig.keyAlias,
                    '--key-pass', "pass:${signConfig.keyPassword}",
                    '--out', outputApkPath,
                    inputApkPath]
        }
        project.exec {
            executable = 'apksigner'
            args = ['verify',
                    outputApkPath]
        }
    }

    private static SigningConfig getReleaseSignConfig(Project project) {
        return getSignConfigByBuildTypeName(project, "release")
    }

    private static SigningConfig getDebugSignConfig(Project project) {
        return getSignConfigByBuildTypeName(project, "debug")
    }

    /**
     * @param project project
     * @param buildTypeName buildTypeName
     * @return SigningConfig instance or null
     */
    private static SigningConfig getSignConfigByBuildTypeName(Project project, String buildTypeName) {
        def buildTypes = getAppModuleExtension(project)?.getBuildTypes()?.<BuildType>toList() ?: new ArrayList<BuildType>()
        for(BuildType buildType in buildTypes){
            if (buildType.name == buildTypeName){
                return buildType.signingConfig
            }
        }
        return null
    }

    /**
     * @param project project
     * @return BaseAppModuleExtension instance or null
     */
    private static BaseAppModuleExtension getAppModuleExtension(Project project) {
        return project?.getExtensions()?.getByName("android") as BaseAppModuleExtension
    }

    /**
     *
     * @param project
     * @param productFlavorName productFlavorName or "",""代表没有 productFlavorName
     * @return 系统构建 apk 输出路径
     */
    private static String getApkOutputDirPath(Project project, String productFlavorName,String buildType) {
        if (productFlavorName.length() == 0) {
            return "${project.buildDir.path}/outputs/apk/$buildType"
        } else {
            return "${project.buildDir.path}/outputs/apk/${productFlavorName}/$buildType"
        }
    }

    /**
     * 上传应用到蒲公英
     * @param project
     * @param uploadFilePath
     */
    private static void uploadToPgyer(Project project,String uploadFilePath,String _api_key){
        println("开始上传到蒲公英---")
        println("uploadFilePath = $uploadFilePath")
        println("_api_key = $_api_key")
        project.exec {
            setCommandLine([
                    'curl',
                    '-F',
                    "file=@$uploadFilePath",
                    '-F',
                    "_api_key=$_api_key",
                    'https://www.pgyer.com/apiv2/app/upload'
            ])
        }
        println()
        println("上传到蒲公英完成---")
    }
}