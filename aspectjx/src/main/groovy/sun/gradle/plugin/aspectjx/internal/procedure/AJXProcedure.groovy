package sun.gradle.plugin.aspectjx.internal.procedure


import sun.gradle.plugin.aspectjx.AJXConfig
import sun.gradle.plugin.aspectjx.AJXExtension
import sun.gradle.plugin.aspectjx.AJXLog
import org.aspectj.weaver.Dump
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import sun.gradle.plugin.aspectjx.internal.cache.AJXCache

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-20
 */
class AJXProcedure extends AbsProcedure {

    Project project
    AJXCache ajxCache

    AJXProcedure(Project proj) {
        super(proj, null, null)

        project = proj
        ajxCache = new AJXCache(project)

        def configuration = new AJXConfig(project)

        project.afterEvaluate {
            def variants = configuration.variants
            if (variants && !variants.isEmpty()) {
                def variant = variants[0]
                JavaCompile javaCompile
                if (variant.hasProperty('javaCompileProvider')) {
                    //android gradle 3.3.0 +
                    javaCompile = variant.javaCompileProvider.get()
                } else {
                    javaCompile = variant.javaCompile
                }

                ajxCache.encoding = javaCompile.options.encoding
                ajxCache.sourceCompatibility = javaCompile.sourceCompatibility
                ajxCache.targetCompatibility = javaCompile.targetCompatibility
            }
            ajxCache.bootClassPath = configuration.bootClasspath.join(File.pathSeparator)

            AJXExtension ajxExtension = project.aspectjx
            //当过滤条件发生变化，clean掉编译缓存
            if (ajxCache.isExtensionChanged(ajxExtension)) {
                AJXLog.d("Aspectjx extension changed, do full build.")
                project.tasks.findByName('preBuild').dependsOn(project.tasks.findByName("clean"))
            }

            ajxCache.putExtensionConfig(ajxExtension)
        }

        //set aspectj build log output dir
        File logDir = new File(project.buildDir.absolutePath + File.separator + "outputs" + File.separator + "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        Dump.setDumpDirectory(logDir.absolutePath)
    }
}
