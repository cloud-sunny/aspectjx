package sun.gradle.plugin.aspectjx.internal.procedure

import com.android.build.api.transform.*
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.AJXTask
import sun.gradle.plugin.aspectjx.internal.AJXTaskManager
import sun.gradle.plugin.aspectjx.internal.AJXUtils
import sun.gradle.plugin.aspectjx.internal.cache.IncrementalStatus
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import sun.gradle.plugin.aspectjx.internal.concurrent.BatchTaskScheduler
import sun.gradle.plugin.aspectjx.internal.concurrent.ITask
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class UpdateAspectOutputProcedure extends AbsProcedure {
    AJXTaskManager ajxTaskManager

    UpdateAspectOutputProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
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
        AJXLog.d("Update aspect output start")
        long startTime = System.nanoTime()
        ajxTaskManager.aspectPath << variantCache.aspectDir
        ajxTaskManager.classPath << variantCache.includeFileDir
        ajxTaskManager.classPath << variantCache.excludeFileDir
        ajxTaskManager.classPath << variantCache.outputFileDir

        BatchTaskScheduler taskScheduler = new BatchTaskScheduler(false)
        // process class file
        taskScheduler.addTask(new ClassFileAjxTaskCollector())
        // process jar file
        for (TransformInput input in transformInvocation.inputs) {
            for (JarInput jarInput in input.jarInputs) {
                taskScheduler.addTask(new JarFileAjxTaskCollector(jarInput))
            }
        }
        taskScheduler.execute()

        if (!ajxTaskManager.isEmpty()) {
            ajxTaskManager.batchExecute()
        }
        AJXLog.d("Update aspect output spend ${((System.nanoTime() - startTime) / 1_000_000) as int}ms")
        return true
    }

    private class ClassFileAjxTaskCollector implements ITask {

        @Override
        Object call() throws Exception {
            IncrementalStatus incStatus = variantCache.incrementalStatus
            if (incStatus.isAspectChanged || incStatus.isIncludeFileChanged) {
                // process class files
                File outputJar = transformInvocation.outputProvider.getContentLocation(
                        "include",
                        variantCache.contentTypes,
                        variantCache.scopes,
                        Format.JAR)
                FileUtils.deleteQuietly(outputJar)

                AJXTask ajxTask = new AJXTask()
                ajxTask.outputDir = variantCache.outputFilePath
                ajxTask.outputJar = outputJar
                ajxTask.inPath = variantCache.includeFilePath

                ajxTaskManager.addTask(ajxTask)
            }
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
            if (jarInput.status == Status.REMOVED) {
                return null
            }
            ajxTaskManager.classPath << jarInput.file
            if (jarInput.status == Status.NOTCHANGED) {
                return null
            }
            String inputPath = jarInput.file.absolutePath
            if (!variantCache.isIncludeJar(inputPath)) {
                return null
            }
            String jarName = AJXUtils.getUnifiedJarName(jarInput)
            File outputJar = transformInvocation.outputProvider.getContentLocation(
                    jarName,
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR)
            FileUtils.deleteQuietly(outputJar)
            FileUtils.forceMkdir(outputJar.parentFile)
            String outputPath = outputJar.absolutePath
            AJXTask ajxTask = new AJXTask()
            ajxTask.inPath = inputPath
            ajxTask.outputJar = outputPath
            ajxTaskManager.addTask(ajxTask)
            return null
        }
    }
}
