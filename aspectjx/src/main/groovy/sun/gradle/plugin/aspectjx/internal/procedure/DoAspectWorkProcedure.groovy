package sun.gradle.plugin.aspectjx.internal.procedure

import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.AJXTask
import sun.gradle.plugin.aspectjx.internal.AJXTaskManager
import sun.gradle.plugin.aspectjx.internal.AJXUtils
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import sun.gradle.plugin.aspectjx.internal.concurrent.BatchTaskScheduler
import sun.gradle.plugin.aspectjx.internal.concurrent.ITask
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class DoAspectWorkProcedure extends AbsProcedure {
    AJXTaskManager ajxTaskManager

    DoAspectWorkProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
        ajxTaskManager = new AJXTaskManager(
                variantCache: variantCache,
                encoding: ajxCache.encoding,
                ajcArgs: ajxCache.ajxConfig.ajcArgs,
                bootClassPath: ajxCache.bootClassPath,
                sourceCompatibility: ajxCache.sourceCompatibility,
                targetCompatibility: ajxCache.targetCompatibility
        )
    }

    @Override
    boolean doWorkContinuously() {
        AJXLog.d("Do aspectj real work start")
        // do aspectj real work
        long startTime = System.nanoTime()
        ajxTaskManager.aspectPath << variantCache.aspectDir
        ajxTaskManager.classPath << variantCache.includeFileDir
        ajxTaskManager.classPath << variantCache.excludeFileDir

        BatchTaskScheduler taskScheduler = new BatchTaskScheduler(false)
        // process class file
        taskScheduler.addTask(new ClassFileAjxTaskCollector())
        // process jar files
        for (TransformInput input in transformInvocation.inputs) {
            for (JarInput jarInput in input.jarInputs) {
                taskScheduler.addTask(new JarFileAjxTaskCollector(jarInput))
            }
        }
        taskScheduler.execute()

        ajxTaskManager.batchExecute()
        AJXLog.d("Do real aspect work spend ${((System.nanoTime() - startTime) / 1_000_000) as int}ms")
        return true
    }

    private class ClassFileAjxTaskCollector implements ITask {

        @Override
        Object call() throws Exception {
            // process class files
            File includeJar = transformInvocation.outputProvider.getContentLocation(
                    "include",
                    variantCache.contentTypes,
                    variantCache.scopes,
                    Format.JAR)

            if (!includeJar.parentFile.exists()) {
                FileUtils.forceMkdir(includeJar.getParentFile())
            }
            FileUtils.deleteQuietly(includeJar)
            FileUtils.cleanDirectory(variantCache.outputFileDir)
            // 同时记录 outputDir 和 outputJar，当 ajc 处理完成后，将 outputDir 中的内容打包到 outputJar
            AJXTask ajxTask = new AJXTask()
            ajxTask.outputJar = includeJar.absolutePath
            ajxTask.outputDir = variantCache.outputFilePath
            ajxTask.inPath = variantCache.includeFilePath
            ajxTaskManager.addTask(ajxTask)
            return null
        }
    }

    private class JarFileAjxTaskCollector implements ITask {
        private JarInput jarInput

        JarFileAjxTaskCollector(JarInput jarInput) {
            this.jarInput = jarInput
        }

        @Override
        Object call() throws Exception {
            ajxTaskManager.classPath << jarInput.file
            String inputPath = jarInput.file.absolutePath
            if (!variantCache.isIncludeJar(inputPath)) {
                return null
            }
            String jarName = AJXUtils.getUnifiedJarName(jarInput)
            File outputJar = transformInvocation.outputProvider.getContentLocation(
                    jarName,
                    jarInput.getContentTypes(),
                    jarInput.getScopes(),
                    Format.JAR)
            String outputPath = outputJar.absolutePath
            FileUtils.forceMkdir(outputJar.parentFile)
            AJXTask task = new AJXTask()
            task.inPath = inputPath
            task.outputJar = outputPath
            ajxTaskManager.addTask(task)
            return null
        }
    }
}
