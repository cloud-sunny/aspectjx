package sun.gradle.plugin.aspectjx.internal

import com.android.build.api.transform.*
import com.google.gson.Gson
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.AJXPlugin
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.slf4j.LoggerFactory

import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-02-01
 */
class AJXUtils {

    static boolean isAspectClass(File classFile) {
        try {
            byte[] bytes = FileUtils.readFileToByteArray(classFile)
            return isAspectClass(bytes)
        } catch (Throwable ignore) {
        }
        return false
    }

    static String transformToClassName(String entryName) {
        String tranName = entryName.replace("/", ".").replace("\\", ".")
        if (tranName.startsWith(".")) {
            tranName = tranName.substring(1)
        }
        return tranName
    }

    static boolean isAspectClass(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false
        }
        try {
            ClassReader classReader = new ClassReader(bytes)
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
            AspectJClassVisitor aspectJClassVisitor = new AspectJClassVisitor(Opcodes.ASM6, classWriter)
            classReader.accept(aspectJClassVisitor, ClassReader.EXPAND_FRAMES)
            return aspectJClassVisitor.isAspectClass
        } catch (Throwable e) {
            AJXLog.d("~AJX_WARN~    check aspect file error: ${e.getMessage()}")
        }
        return false
    }

    static boolean isClassFile(String filePath) {
        return filePath?.toLowerCase()?.endsWith('.class')
    }

    static <T> T optFromJsonString(String json, Type typeOfT) {
        try {
            return new Gson().fromJson(json, typeOfT)
        } catch (Exception var3) {
            LoggerFactory.getLogger(AJXPlugin).warn("optFromJsonString(${json}, ${typeOfT}", var3)
        }
        return null
    }

    static String optToJsonString(Object object) {
        try {
            return new Gson().toJson(object)
        } catch (Throwable var2) {
            LoggerFactory.getLogger(AJXPlugin).warn("optToJsonString(${object}", var2)
        }
        return null
    }

    /**
     * @param transformInvocation
     */
    static void doWorkWithNoAspectj(TransformInvocation transformInvocation) {
        AJXLog.d("Do work with no aspectj")
        if (transformInvocation.incremental) {
            incrementalCopyFiles(transformInvocation)
        } else {
            fullCopyFiles(transformInvocation)
        }
    }

    static void fullCopyFiles(TransformInvocation transformInvocation) {
        transformInvocation.outputProvider.deleteAll()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                File excludeJar = transformInvocation.outputProvider.getContentLocation(
                        "exclude",
                        dirInput.contentTypes,
                        dirInput.scopes,
                        Format.JAR)
                mergeJar(dirInput.file, excludeJar)
            }

            input.jarInputs.each { JarInput jarInput ->
                String jarName = getUnifiedJarName(jarInput)
                File excludeJar = transformInvocation.outputProvider.getContentLocation(
                        jarName,
                        jarInput.contentTypes,
                        jarInput.scopes,
                        Format.JAR)
                FileUtils.copyFile(jarInput.file, excludeJar)
            }
        }
    }

    static void incrementalCopyFiles(TransformInvocation transformInvocation) {
        for (TransformInput input in transformInvocation.inputs) {
            for (DirectoryInput dirInput in input.directoryInputs) {
                if (dirInput.changedFiles.size() == 0) {
                    continue
                }
                File excludeJar = transformInvocation.outputProvider.getContentLocation(
                        "exclude",
                        dirInput.contentTypes,
                        dirInput.scopes,
                        Format.JAR)
                mergeJar(dirInput.file, excludeJar)
            }

            for (JarInput jarInput in input.jarInputs) {
                String jarName = getUnifiedJarName(jarInput)
                File outputJar = transformInvocation.outputProvider.getContentLocation(
                        jarName,
                        jarInput.contentTypes,
                        jarInput.scopes,
                        Format.JAR)
                switch (jarInput.status) {
                    case Status.REMOVED:
                        FileUtils.deleteQuietly(outputJar)
                        break
                    case Status.CHANGED:
                        FileUtils.deleteQuietly(outputJar)
                        FileUtils.copyFile(jarInput.file, outputJar)
                        break
                    case Status.ADDED:
                        FileUtils.copyFile(jarInput.file, outputJar)
                        break
                    default:
                        break
                }
            }
        }
    }

    static String getUnifiedJarName(QualifiedContent jarInput) {
        MessageDigest md = MessageDigest.getInstance("MD5")
        md.update(jarInput.file.absolutePath.getBytes(StandardCharsets.UTF_8))
        return new BigInteger(1, md.digest()).toString(16)
    }

    static int countOfFiles(File file) {
        if (file.isFile()) {
            return 1
        } else {
            File[] files = file.listFiles()
            int total = 0
            for (File f : files) {
                total += countOfFiles(f)
            }
            return total
        }
    }

    static void mergeJar(File sourceDir, File targetJar) {
        if (sourceDir == null) {
            throw new IllegalArgumentException("sourceDir should not be null")
        }

        if (targetJar == null) {
            throw new IllegalArgumentException("targetJar should not be null")
        }

        if (!targetJar.parentFile.exists()) {
            FileUtils.forceMkdir(targetJar.getParentFile())
        }

        FileUtils.deleteQuietly(targetJar)

        JarMerger jarMerger = new JarMerger(targetJar)
        try {
            jarMerger.setFilter(new JarMerger.IZipEntryFilter() {
                @Override
                boolean checkEntry(String archivePath) throws JarMerger.IZipEntryFilter.ZipAbortException {
                    return archivePath.endsWith(".class")
                }
            })
            jarMerger.addFolder(sourceDir)
        } catch (Exception e) {
            LoggerFactory.getLogger(AJXPlugin).warn("mergeJar(${sourceDir}, ${targetJar}", e)
        } finally {
            jarMerger.close()
        }
    }
}

