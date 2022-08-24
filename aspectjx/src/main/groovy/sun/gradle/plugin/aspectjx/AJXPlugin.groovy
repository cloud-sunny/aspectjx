package sun.gradle.plugin.aspectjx

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import sun.gradle.plugin.aspectjx.AJXExtension
import sun.gradle.plugin.aspectjx.AJXTransform
import sun.gradle.plugin.aspectjx.internal.TimeTrace

/**
 * aspectj plugin,
 * @author simon* @version 1.0.0* @since 2016-04-20
 */
class AJXPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("aspectjx", AJXExtension)

        project.configurations {
            aspectjx
        }

        project.dependencies {
            aspectjx gradleApi()
            aspectjx localGroovy()
            aspectjx "com.google.code.gson:gson:2.8.6"
            aspectjx "commons-io:commons-io:2.4"
            aspectjx "com.google.guava:guava:28.1-jre"
            aspectjx "org.aspectj:aspectjrt:1.9.7"
//            Caused by: org.gradle.api.artifacts.UnknownConfigurationException: Configuration with name 'implementation' not found
//            if (project.configurations["implementation"] != null) {
//                implementation "org.aspectj:aspectjrt:1.9.7"
//            } else if (project.configurations["compile"] != null) {
//                compile "org.aspectj:aspectjrt:1.9.7"
//            }
        }

        if (project.plugins.hasPlugin(AppPlugin)) {
            // build time trace
            def timeTrace = new TimeTrace()
            timeTrace.projectsEvaluated(project.gradle)
            project.gradle.addListener(timeTrace)

            // register AJXTransform
            AppExtension android = project.extensions.getByType(AppExtension)
            android.registerTransform(new AJXTransform(project))
        }
//        println("=======project.task start========")
//        project.task('hello') {
////            println "--logEnabled----->>>" + project.extensions.findByName("logEnabled")
//            println("=======hello task run========")
//        }
//        println("=======project.task end========")
    }
}