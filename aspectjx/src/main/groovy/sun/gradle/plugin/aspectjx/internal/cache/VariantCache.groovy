package sun.gradle.plugin.aspectjx.internal.cache

import com.android.build.api.transform.QualifiedContent
import com.google.common.collect.ImmutableSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import sun.gradle.plugin.aspectjx.internal.AJXUtils
import sun.gradle.plugin.aspectjx.internal.model.JarInfo
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.concurrent.ConcurrentHashMap

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-13
 */
class VariantCache {

    Project project
    AJXCache ajxCache
    String variantName
    String cachePath
    String aspectPath
    String includeFilePath
    String excludeFilePath
    String outputFilePath
    String includeJarConfigPath
    String jarAspectsPath
    String INTERMEDIATES = "intermediates";
    IncrementalStatus incrementalStatus
    Set<QualifiedContent.ContentType> contentTypes = ImmutableSet.<QualifiedContent.ContentType> of(QualifiedContent.DefaultContentType.CLASSES)
    Set<QualifiedContent.Scope> scopes = ImmutableSet.<QualifiedContent.Scope> of(QualifiedContent.Scope.EXTERNAL_LIBRARIES)

    Map<String, JarInfo> includeJarInfos = new ConcurrentHashMap<>()

    VariantCache(Project project, AJXCache cache, String variantName) {
        this.project = project
        this.variantName = variantName
        this.ajxCache = cache
        this.ajxCache.put(variantName, this)
        incrementalStatus = new IncrementalStatus()
        init()
    }

    private void init() {
        cachePath = project.buildDir.absolutePath + File.separator + INTERMEDIATES + "/ajx/" + variantName
        aspectPath = cachePath + File.separator + "aspects"
        includeFilePath = cachePath + File.separator + "includefiles"
        excludeFilePath = cachePath + File.separator + "excludefiles"
        includeJarConfigPath = cachePath + File.separator + "includejars.json"
        outputFilePath = cachePath + File.separator + "outputfiles"
        jarAspectsPath = cachePath + File.separator + "jar_aspects.json"
        if (!aspectDir.exists()) {
            aspectDir.mkdirs()
        }

        if (!includeFileDir.exists()) {
            includeFileDir.mkdirs()
        }

        if (!excludeFileDir.exists()) {
            excludeFileDir.mkdirs()
        }

        if (!outputFileDir.exists()) {
            outputFileDir.mkdirs()
        }

        if (includeJarConfig.exists()) {
            List<JarInfo> jarInfoList = AJXUtils.optFromJsonString(FileUtils.readFileToString(includeJarConfig), new TypeToken<List<JarInfo>>() {
            }.getType())

            if (jarInfoList != null) {
                jarInfoList.each { JarInfo jarInfo ->
                    includeJarInfos.put(jarInfo.path, jarInfo)
                }
            }
        }
    }

    File getCacheDir() {
        return new File(cachePath)
    }

    File getAspectDir() {
        return new File(aspectPath)
    }

    File getIncludeFileDir() {
        return new File(includeFilePath)
    }

    File getExcludeFileDir() {
        return new File(excludeFilePath)
    }

    File getIncludeJarConfig() {
        return new File(includeJarConfigPath)
    }

    File getOutputFileDir() {
        return new File(outputFilePath)
    }

    static void add(File sourceFile, File cacheFile) {
        if (sourceFile == null || cacheFile == null) {
            return
        }
        if (!sourceFile.isFile()) {
            return
        }
        byte[] bytes = FileUtils.readFileToByteArray(sourceFile)
        add(bytes, cacheFile)
    }

    static void add(byte[] classBytes, File cacheFile) {
        if (classBytes == null || cacheFile == null) {
            return
        }

        FileUtils.writeByteArrayToFile(cacheFile, classBytes)
    }

    void addIncludeJar(String jarPath) {
        if (jarPath != null) {
            includeJarInfos.put(jarPath, new JarInfo(path: jarPath))
        }
    }

    void removeIncludeJar(String jarPath) {
        includeJarInfos.remove(jarPath)
    }

    boolean isIncludeJar(String jarPath) {
        if (jarPath == null) {
            return false
        }

        return includeJarInfos.containsKey(jarPath)
    }

    void commitIncludeJarConfig() {
        FileUtils.deleteQuietly(includeJarConfig)

        if (!includeJarConfig.exists()) {
            includeJarConfig.createNewFile()
        }

        List<JarInfo> jarInfoList = new ArrayList<>()
        includeJarInfos.each { String key, JarInfo value ->
            jarInfoList.add(value)
        }

        FileUtils.write(includeJarConfig, AJXUtils.optToJsonString(jarInfoList), "UTF-8")
    }

    void saveJarAspects(Map<String, Set<String>> jarAspects) {
        String json = new Gson().toJson(jarAspects)
        File aspectsFile = new File(jarAspectsPath)
        FileUtils.writeStringToFile(aspectsFile, json)
    }

    Map<String, Set<String>> loadJarAspects() {
        File aspectsFile = new File(jarAspectsPath)
        if (aspectsFile.exists()) {
            String json = FileUtils.readFileToString(aspectsFile)
            return new Gson().fromJson(json, new TypeToken<Map<String, Set<String>>>() {}.getType())
        }
        return Collections.emptyMap()
    }

    void reset() {
        close()

        init()
    }

    void close() {
        FileUtils.deleteDirectory(cacheDir)
        includeJarInfos.clear()
    }
}
