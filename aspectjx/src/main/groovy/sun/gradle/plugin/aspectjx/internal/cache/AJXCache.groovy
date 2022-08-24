package sun.gradle.plugin.aspectjx.internal.cache

import sun.gradle.plugin.aspectjx.AJXExtension
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.AJXUtils
import sun.gradle.plugin.aspectjx.internal.model.AJXExtensionConfig
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.concurrent.ConcurrentHashMap

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-03
 */
class AJXCache {

    Project project
    String cachePath
    Map<String, VariantCache> variantCacheMap = new ConcurrentHashMap<>()

    String extensionConfigPath
    AJXExtensionConfig ajxConfig = new AJXExtensionConfig()

    //for aspectj
    String encoding
    String bootClassPath
    String sourceCompatibility
    String targetCompatibility

    AJXCache(Project proj) {
        this.project = proj

        init()
    }
    String INTERMEDIATES = "intermediates";

    private void init() {
        cachePath = project.buildDir.absolutePath + File.separator + INTERMEDIATES + "/ajx"
        extensionConfigPath = cachePath + File.separator + "extensionconfig.json"

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        //extension config
        File extensionConfig = new File(extensionConfigPath)
        if (extensionConfig.exists()) {
            ajxConfig = AJXUtils.optFromJsonString(FileUtils.readFileToString(extensionConfig), AJXExtensionConfig.class)
        }

        if (ajxConfig == null) {
            ajxConfig = new AJXExtensionConfig()
        }
    }

    File getCacheDir() {
        return new File(cachePath)
    }

    File getExtensionConfigFile() {
        return new File(extensionConfigPath)
    }

    void reset() {
        FileUtils.deleteDirectory(cacheDir)

        init()
    }

    void commit() {
        AJXLog.d("putExtensionConfig:${extensionConfigFile}")

        FileUtils.deleteQuietly(extensionConfigFile)

        File parent = extensionConfigFile.parentFile

        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        if (!extensionConfigFile.exists()) {
            extensionConfigFile.createNewFile()
        }

        String jsonString = AJXUtils.optToJsonString(ajxConfig)
        AJXLog.d("${jsonString}")
        FileUtils.write(extensionConfigFile, jsonString, "UTF-8")
    }

    void put(String variantName, VariantCache cache) {
        if (variantName != null && cache != null) {
            variantCacheMap.put(variantName, cache)
        }
    }

    boolean contains(String variantName) {
        if (variantName == null) {
            return false
        }

        return variantCacheMap.containsKey(variantName)
    }

    void putExtensionConfig(AJXExtension extension) {
        if (extension == null) {
            return
        }

        ajxConfig.enabled = extension.enabled
        ajxConfig.logEnabled = extension.logEnabled
        ajxConfig.ajcArgs = extension.ajcArgs
        ajxConfig.includes = extension.includes
        ajxConfig.excludes = extension.excludes
        ajxConfig.forceExcludeFiles = extension.forceExcludeFiles
    }

    boolean isExtensionChanged(AJXExtension extension) {
        if (extension == null) {
            return true
        }
        if (ajxConfig.includes != extension.includes) {
            return true
        }
        if (ajxConfig.excludes != extension.excludes) {
            return true
        }
        if (ajxConfig.forceExcludeFiles != extension.forceExcludeFiles) {
            return true
        }
        return false
    }
}
