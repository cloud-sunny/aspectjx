package sun.gradle.plugin.aspectjx.internal.procedure

import com.android.build.api.transform.*
import com.google.common.io.ByteStreams
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.AJXUtils
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import sun.gradle.plugin.aspectjx.internal.concurrent.BatchTaskScheduler
import sun.gradle.plugin.aspectjx.internal.concurrent.ITask
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class UpdateAspectFilesProcedure extends AbsProcedure {
    UpdateAspectFilesProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        long startTime = System.nanoTime()
        AJXLog.d("Update aspect files start")

        // update aspect files
        Map<String, Set<String>> jarAspects = new ConcurrentHashMap<>()
        jarAspects.putAll(variantCache.loadJarAspects())

        BatchTaskScheduler taskScheduler = new BatchTaskScheduler(true)
        for (TransformInput input in transformInvocation.inputs) {
            for (DirectoryInput dirInput in input.directoryInputs) {
                taskScheduler.addTask(new UpdateAspectFileTask(dirInput))
            }
            for (JarInput jarInput in input.jarInputs) {
                if (jarInput.status != Status.NOTCHANGED) {
                    taskScheduler.addTask(new UpdateAspectJarTask(jarInput, jarAspects))
                }
            }
        }
        taskScheduler.execute()

        variantCache.saveJarAspects(jarAspects)

        boolean hasAspect = AJXUtils.countOfFiles(variantCache.aspectDir) > 0
        if (!hasAspect) {
            //do work with no aspectj
            AJXUtils.fullCopyFiles(transformInvocation)
        }
        AJXLog.d("Update aspect files spend ${((System.nanoTime() - startTime) / 1_000_000) as int}ms")
        return hasAspect
    }

    private class UpdateAspectFileTask implements ITask {

        private DirectoryInput dirInput

        UpdateAspectFileTask(DirectoryInput dirInput) {
            this.dirInput = dirInput
        }

        @Override
        Object call() throws Exception {
            for (Map.Entry<File, Status> entry in dirInput.changedFiles) {
                File file = entry.key
                Status status = entry.value
                if (!AJXUtils.isAspectClass(file)) {
                    continue
                }
                AJXLog.d("Collect aspect file from dir: ${file.absolutePath}")
                AJXLog.d("Changed aspect file::${status.name()}::${file.absolutePath}")
                variantCache.incrementalStatus.isAspectChanged = true
                String subPath = file.absolutePath.substring(dirInput.file.absolutePath.length())
                File cacheFile = new File(variantCache.aspectPath, subPath)
                switch (status) {
                    case Status.REMOVED:
                        FileUtils.deleteQuietly(cacheFile)
                        break
                    case Status.CHANGED:
                        FileUtils.deleteQuietly(cacheFile)
                        variantCache.add(file, cacheFile)
                        break
                    case Status.ADDED:
                        variantCache.add(file, cacheFile)
                        break
                    default:
                        break
                }
            }
            return null
        }
    }

    private class UpdateAspectJarTask implements ITask {

        private JarInput jarInput
        private Map<String, Set<String>> jarAspects

        UpdateAspectJarTask(JarInput jarInput, Map<String, Set<String>> jarAspects) {
            this.jarAspects = jarAspects
            this.jarInput = jarInput
        }

        @Override
        Object call() throws Exception {
            String key = jarInput.file.absolutePath
            Set<String> oldAspects = jarAspects.remove(key)
            boolean hasOldAspect = oldAspects != null && oldAspects.size() > 0
            if (jarInput.status == Status.REMOVED) {
                // 如果 jar 被移除，删除该 jar 中所有的 aspect 文件缓存，并记录 aspect 文件发生改变了，需要重新执行 ajc
                if (hasOldAspect) {
                    for (String oldAspect in oldAspects) {
                        FileUtils.deleteQuietly(new File(oldAspect))
                    }
                    variantCache.incrementalStatus.isAspectChanged = true
                }
            } else {
                Set<String> aspects = new HashSet<String>()
                new JarFile(jarInput.file).withCloseable { JarFile jarFile ->
                    Enumeration<JarEntry> entries = jarFile.entries()
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement()
                        String entryName = jarEntry.getName()
                        if (jarEntry.isDirectory() || !AJXUtils.isClassFile(entryName)) {
                            continue
                        }
                        String transName = AJXUtils.transformToClassName(entryName)
                        if (!ajxConfig.isAjxInclude(transName)) {
                            continue
                        }
                        InputStream is = jarFile.getInputStream(jarEntry)
                        byte[] bytes = ByteStreams.toByteArray(is)
                        if (!AJXUtils.isAspectClass(bytes)) {
                            continue
                        }
                        File cacheAspect = new File(variantCache.aspectPath, entryName)
                        aspects.add(cacheAspect.absolutePath)
                        if (jarInput.status == Status.CHANGED) {
                            if (!cacheAspect.exists()) {
                                // new aspect file found in jar
                                AJXLog.d("Collect aspect file from jar: ${entryName}")
                                AJXLog.d("Changed aspect file::${Status.ADDED}::${entryName}")
                                variantCache.incrementalStatus.isAspectChanged = true
                                variantCache.add(bytes, cacheAspect)
                                continue
                            }
                            byte[] cacheAspectData = FileUtils.readFileToByteArray(cacheAspect)
                            if (!Arrays.equals(cacheAspectData, bytes)) {
                                // aspect file changed in jar
                                AJXLog.d("Collect aspect file from jar: ${entryName}")
                                AJXLog.d("Changed aspect file::${Status.CHANGED}::${entryName}")
                                variantCache.incrementalStatus.isAspectChanged = true
                                FileUtils.deleteQuietly(cacheAspect)
                                variantCache.add(bytes, cacheAspect)
                            }
                        } else if (jarInput.status == Status.ADDED) {
                            // new aspect file found in jar
                            AJXLog.d("Collect aspect file from jar: ${entryName}")
                            AJXLog.d("Changed aspect file::${Status.ADDED}::${entryName}")
                            variantCache.incrementalStatus.isAspectChanged = true
                            variantCache.add(bytes, cacheAspect)
                        }
                    }
                }
                if (jarInput.status == Status.CHANGED && hasOldAspect) {
                    for (String oldAspect in oldAspects) {
                        if (!aspects.contains(oldAspect)) {
                            // old aspect removed in new jar
                            AJXLog.d("Changed aspect file::${Status.REMOVED}::${oldAspect}")
                            FileUtils.deleteQuietly(new File(oldAspect))
                        }
                    }
                }
                if (!aspects.isEmpty()) {
                    jarAspects.put(key, aspects)
                }
            }
            return null
        }
    }
}
