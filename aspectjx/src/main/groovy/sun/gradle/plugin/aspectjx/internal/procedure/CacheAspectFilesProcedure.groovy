package sun.gradle.plugin.aspectjx.internal.procedure

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.google.common.io.ByteStreams
import org.gradle.api.Project
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import sun.gradle.plugin.aspectjx.internal.concurrent.BatchTaskScheduler
import sun.gradle.plugin.aspectjx.internal.concurrent.ITask

import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * 找到项目下以及项目依赖的 jar 和 module 中的 aspect 文件，并将其缓存。
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class CacheAspectFilesProcedure extends AbsProcedure {
    CacheAspectFilesProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        AJXLog.d("Cache aspect files start")
        //缓存 aspect文件
        long startTime = System.nanoTime()

        BatchTaskScheduler taskScheduler = new BatchTaskScheduler(true)
        // 在全量编译时将收集到的 aspect 文件信息写入磁盘，在增量编译时读取该文件，根据文件内容判断是否有 aspect 文件
        // 从 jar 中删除了，处理 jar 文件本身没删除，但是其中的 aspect 文件被删了的场景
        // 集合的 key 为包含 aspect 文件的 jar 的文件路径，value 是 aspect 文件在该 jar 中的 entry path
        Map<String, Set<String>> jarAspects = new ConcurrentHashMap<>()
        for (TransformInput input in transformInvocation.inputs) {
            for (DirectoryInput dirInput in input.directoryInputs) {
                taskScheduler.addTask(new CacheAspectFileTask(dirInput))
            }
            for (JarInput jarInput in input.jarInputs) {
                taskScheduler.addTask(new CacheAspectJarTask(jarInput, jarAspects))
            }
        }
        taskScheduler.execute()

        variantCache.saveJarAspects(jarAspects)

        AJXLog.d("Cache aspect files spend ${((System.nanoTime() - startTime) / 1_000_000) as int}ms")
        boolean hasAspect = AJXUtils.countOfFiles(variantCache.aspectDir) > 0
        if (!hasAspect) {
            AJXUtils.doWorkWithNoAspectj(transformInvocation)
        }
        return hasAspect
    }

    private class CacheAspectFileTask implements ITask {
        private DirectoryInput dirInput

        CacheAspectFileTask(DirectoryInput dirInput) {
            this.dirInput = dirInput
        }

        @Override
        Object call() throws Exception {
            dirInput.file.eachFileRecurse { File file ->
                if (AJXUtils.isAspectClass(file)) {
                    AJXLog.d("collect aspect file: ${file.absolutePath}")
                    String subPath = file.absolutePath.substring(dirInput.file.absolutePath.length())
                    File cacheFile = new File(variantCache.aspectPath, subPath)
                    variantCache.add(file, cacheFile)
                }
            }
            return null
        }
    }

    private class CacheAspectJarTask implements ITask {

        private JarInput jarInput
        private Map<String, Set<String>> jarAspects

        CacheAspectJarTask(JarInput jarInput, Map<String, Set<String>> jarAspects) {
            this.jarAspects = jarAspects
            this.jarInput = jarInput
        }

        @Override
        Object call() throws Exception {
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
                    if (AJXUtils.isAspectClass(bytes)) {
                        AJXLog.d("collect aspect file: ${entryName}")
                        File cacheFile = new File(variantCache.aspectPath + File.separator + entryName)
                        variantCache.add(bytes, cacheFile)
                        String key = jarInput.file.absolutePath
                        Set<String> aspects = jarAspects.get(key)
                        if (aspects == null) {
                            aspects = new HashSet<>()
                            jarAspects.put(key, aspects)
                        }
                        aspects.add(cacheFile.absolutePath)
                    }
                }
            }
            return null
        }
    }
}
