package sun.gradle.plugin.aspectjx.internal.procedure

import com.android.build.api.transform.*
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import sun.gradle.plugin.aspectjx.internal.concurrent.BatchTaskScheduler
import sun.gradle.plugin.aspectjx.internal.concurrent.ITask

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class CacheInputFilesProcedure extends AbsProcedure {
    CacheInputFilesProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        AJXLog.d("Cache input files start")
        long startTime = System.nanoTime()

        BatchTaskScheduler taskScheduler = new BatchTaskScheduler(true)
        for (TransformInput input in transformInvocation.inputs) {
            for (DirectoryInput dirInput in input.directoryInputs) {
                taskScheduler.addTask(new CacheInputFileTask(dirInput))
            }
            for (JarInput jarInput in input.jarInputs) {
                taskScheduler.addTask(new CacheInputJarTask(jarInput))
            }
        }
        taskScheduler.execute()

        variantCache.commitIncludeJarConfig()
        AJXLog.d("Cache input files spend ${((System.nanoTime() - startTime) / 1_000_000) as int}ms")
        return true
    }

    private class CacheInputFileTask implements ITask {

        private DirectoryInput dirInput

        CacheInputFileTask(DirectoryInput dirInput) {
            this.dirInput = dirInput
        }

        @Override
        Object call() throws Exception {
            dirInput.file.eachFileRecurse { File file ->
                if (file.isFile()) {
                    ajxConfig.filterClassFile(dirInput, file, variantCache)
                }
            }

            // put exclude files into jar
            if (AJXUtils.countOfFiles(variantCache.excludeFileDir) > 0) {
                File excludeJar = transformInvocation.outputProvider.getContentLocation(
                        "exclude",
                        variantCache.contentTypes,
                        variantCache.scopes,
                        Format.JAR)
                AJXUtils.mergeJar(variantCache.excludeFileDir, excludeJar)
            }
            return null
        }
    }

    private class CacheInputJarTask implements ITask {

        private JarInput jarInput

        CacheInputJarTask(JarInput jarInput) {
            this.jarInput = jarInput
        }

        @Override
        Object call() throws Exception {
            ajxConfig.filterJarFile(jarInput, variantCache)
            if (!variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                String jarName = AJXUtils.getUnifiedJarName(jarInput)
                File excludeJar = transformInvocation.outputProvider.getContentLocation(
                        jarName,
                        jarInput.contentTypes,
                        jarInput.scopes,
                        Format.JAR)
                FileUtils.copyFile(jarInput.file, excludeJar)
            }
            return null
        }
    }
}
