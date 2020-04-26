package me.twc.protect.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class ProtectPlugin implements Plugin<Project> {
    void apply(Project project){
        project.task('ProtectPlugin'){
            group 'twc'
            doFirst {
                println '加固插件测试-开始'
            }
            doLast{
                println '加固插件测试-结束'
            }
        }
    }
}