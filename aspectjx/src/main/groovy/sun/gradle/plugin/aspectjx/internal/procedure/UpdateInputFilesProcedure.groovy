package sun.gradle.plugin.aspectjx.internal.procedure

import com.android.build.api.transform.*
import sun.gradle.plugin.aspectjx.AJXLog
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
class UpdateInputFilesProcedure extends AbsProcedure {

    UpdateInputFilesProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        // ajx/${variantName}/includefiles 文件夹用来存放每次需要进行 ajc 的文件，这里清除之前运行时保存的文件
        FileUtils.cleanDirectory(variantCache.includeFileDir)
        AJXLog.d("Update input files start")
        long startTime = System.nanoTime()
        BatchTaskScheduler taskScheduler = new BatchTaskScheduler(true)

        for (TransformInput input in transformInvocation.inputs) {
            for (DirectoryInput dirInput in input.directoryInputs) {
                taskScheduler.addTask(new UpdateInputFileTask(dirInput))
            }
            for (JarInput jarInput in input.jarInputs) {
                if (jarInput.status != Status.NOTCHANGED) {
                    taskScheduler.addTask(new UpdateInputJarTask(jarInput))
                }
            }
        }
        taskScheduler.execute()

        variantCache.commitIncludeJarConfig()
        AJXLog.d("Update input files spend ${((System.nanoTime() - startTime) / 1_000_000) as int}ms")
        return true
    }

    private class UpdateInputFileTask implements ITask {

        private DirectoryInput dirInput

        UpdateInputFileTask(DirectoryInput dirInput) {
            this.dirInput = dirInput
        }

        @Override
        Object call() throws Exception {
            for (Map.Entry<File, Status> entry in dirInput.changedFiles) {
                File file = entry.key
                Status status = entry.value
                if (!file.isFile()) {
                    continue
                }
                AJXLog.d("Changed file::${status.name()}::${file.absolutePath}")
                String subPath = file.absolutePath.substring(dirInput.file.absolutePath.length())
                String transName = AJXUtils.transformToClassName(subPath)
                boolean isInclude = !ajxConfig.isForceExclude(transName) && ajxConfig.isAjxInclude(transName)
                String inputDir = isInclude ? variantCache.includeFilePath : variantCache.excludeFilePath
                String outputDir = isInclude ? variantCache.outputFilePath : variantCache.excludeFilePath
                File inputFile = new File(inputDir, subPath)
                File outputFile = new File(outputDir, subPath)
                switch (status) {
                    case Status.REMOVED:
                        FileUtils.deleteQuietly(outputFile)
                        break
                    case Status.CHANGED:
                        FileUtils.deleteQuietly(outputFile)
                        variantCache.add(file, inputFile)
                        break
                    case Status.ADDED:
                        variantCache.add(file, inputFile)
                        break
                    default:
                        break
                }
                variantCache.incrementalStatus.isIncludeFileChanged |= isInclude
                variantCache.incrementalStatus.isExcludeFileChanged |= !isInclude
            }

            // 如果 include files 发生变化，则删除 include 输出的 jar
            if (variantCache.incrementalStatus.isIncludeFileChanged) {
                File includeOutputJar = transformInvocation.outputProvider.getContentLocation(
                        "include",
                        variantCache.contentTypes,
                        variantCache.scopes,
                        Format.JAR)
                FileUtils.deleteQuietly(includeOutputJar)
            }

            // 如果 exclude files 发生变化，则重新生成 exclude jar 到输出目录
            if (variantCache.incrementalStatus.isExcludeFileChanged) {
                File excludeOutputJar = transformInvocation.outputProvider.getContentLocation(
                        "exclude",
                        variantCache.contentTypes,
                        variantCache.scopes,
                        Format.JAR)
                FileUtils.deleteQuietly(excludeOutputJar)
                AJXUtils.mergeJar(variantCache.excludeFileDir, excludeOutputJar)
            }
            return null
        }
    }

    private class UpdateInputJarTask implements ITask {

        private JarInput jarInput

        UpdateInputJarTask(JarInput jarInput) {
            this.jarInput = jarInput
        }

        @Override
        Object call() throws Exception {
            AJXLog.d("Changed file::${jarInput.status.name()}::${jarInput.file.absolutePath}")
            String inputJarPath = jarInput.file.absolutePath
            String jarName = AJXUtils.getUnifiedJarName(jarInput)
            File outputJar = transformInvocation.outputProvider.getContentLocation(
                    jarName,
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR)
            if (jarInput.status == Status.REMOVED) {
                variantCache.removeIncludeJar(inputJarPath)
                FileUtils.deleteQuietly(outputJar)
            } else if (jarInput.status == Status.ADDED) {
                ajxConfig.filterJarFile(jarInput, variantCache)
            } else if (jarInput.status == Status.CHANGED) {
                FileUtils.deleteQuietly(outputJar)
            }
            boolean isInclude = variantCache.isIncludeJar(inputJarPath)
            if (!isInclude && jarInput.status != Status.REMOVED) {
                //将不需要做 AOP 处理的文件原样 copy 到输出目录
                FileUtils.copyFile(jarInput.file, outputJar)
            }
            return null
        }
    }
}
