package sun.gradle.plugin.aspectjx.internal.model

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.FilterMatcher
import org.gradle.api.GradleException
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-13
 */
class AJXExtensionConfig implements Serializable {

    boolean enabled = true

    boolean logEnabled = false

    List<String> ajcArgs = new ArrayList<>()

    List<String> includes = new ArrayList<>()

    List<String> excludes = new ArrayList<>()

    List<String> forceExcludeFiles = new ArrayList<>()

    boolean isAjxInclude(String file) {
        if (includes.isEmpty() && excludes.isEmpty()) {
            return true
        } else if (includes.isEmpty()) {
            return !isMatchFilters(file, excludes)
        } else if (excludes.isEmpty()) {
            return isMatchFilters(file, includes)
        } else {
            throw new GradleException("aspectjx 的配置不能同时添加 include 和 exclude")
        }
    }

    private static boolean isMatchFilters(String str, List<String> filters) {
        if (str == null) {
            return false
        }
        if (filters == null || filters.isEmpty()) {
            return false
        }
        for (String filter : filters) {
            if (FilterMatcher.isMatch(str, filter)) {
                return true
            }
        }
        return false
    }

    boolean isForceExclude(String file) {
        return isMatchFilters(file, forceExcludeFiles)
    }

    boolean filterClassFile(DirectoryInput dirInput, File file, VariantCache variantCache) {
        String subPath = file.absolutePath.substring(dirInput.file.absolutePath.length())
        String transName = AJXUtils.transformToClassName(subPath)
        boolean isInclude = !isForceExclude(transName) && isAjxInclude(transName)
        if (isInclude) {
            AJXLog.d("~INCLUDE~: " + transName)
        } else {
            AJXLog.d("~EXCLUDE~: " + transName)
        }
        String dir = isInclude ? variantCache.includeFilePath : variantCache.excludeFilePath
        variantCache.add(file, new File(dir, subPath))
        return isInclude
    }

    void filterJarFile(JarInput jarInput, VariantCache variantCache) {
        String jarFilePath = jarInput.file.absolutePath
        if (includes.isEmpty() && excludes.isEmpty()) {
            // include all, put in cache
            variantCache.addIncludeJar(jarFilePath)
            AJXLog.d("~INCLUDE~: " + jarFilePath)
            return
        }
        if (includes.isEmpty()) {
            boolean isExclude = false
            if (isMatchFilters(jarFilePath, excludes)) {
                isExclude = true
            } else {
                new JarFile(jarFilePath).withCloseable { jarFile ->
                    Enumeration<JarEntry> entries = jarFile.entries()
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement()
                        String transName = AJXUtils.transformToClassName(jarEntry.getName())
                        if (isMatchFilters(transName, excludes)) {
                            isExclude = true
                            break
                        }
                    }
                }
            }

            if (isExclude) {
                variantCache.removeIncludeJar(jarFilePath)
                AJXLog.d("~EXCLUDE~: " + jarFilePath)
            } else {
                // put in cache
                variantCache.addIncludeJar(jarFilePath)
                AJXLog.d("~INCLUDE~: " + jarFilePath)
            }
        } else if (excludes.isEmpty()) {
            boolean isInclude = false
            if (isMatchFilters(jarFilePath, includes)) {
                isInclude = true
            } else {
                new JarFile(jarFilePath).withCloseable { jarFile ->
                    Enumeration<JarEntry> entries = jarFile.entries()
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement()
                        String transName = AJXUtils.transformToClassName(jarEntry.getName())
                        if (isMatchFilters(transName, includes)) {
                            isInclude = true
                            break
                        }
                    }
                }
            }
            if (isInclude) {
                // put in cache
                variantCache.addIncludeJar(jarFilePath)
                AJXLog.d("~INCLUDE~: " + jarFilePath)
            } else {
                variantCache.removeIncludeJar(jarFilePath)
                AJXLog.d("~EXCLUDE~: " + jarFilePath)
            }
        } else {
            throw new IllegalArgumentException("aspectjx 的配置不能同时添加 include 和 exclude")
        }
    }
}
