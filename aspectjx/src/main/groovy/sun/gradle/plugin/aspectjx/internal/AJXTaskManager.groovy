package sun.gradle.plugin.aspectjx.internal

import com.google.gson.Gson
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import sun.gradle.plugin.aspectjx.internal.model.AjcArgs
import org.aspectj.bridge.context.CompilationAndWeavingContext
import org.aspectj.lang.annotation.Aspect
import org.aspectj.org.eclipse.jdt.internal.compiler.batch.ClasspathJar
import org.aspectj.util.FileUtil
import org.gradle.api.artifacts.Configuration
import org.gradle.process.ExecResult
/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-03-14
 */
class AJXTaskManager {
    VariantCache variantCache
    List<AJXTask> tasks = Collections.synchronizedList(new ArrayList<AJXTask>())
    ArrayList<File> aspectPath = new ArrayList<>()
    ArrayList<File> classPath = new ArrayList<>()
    List<String> ajcArgs = new ArrayList<>()
    String encoding
    String bootClassPath
    String sourceCompatibility
    String targetCompatibility

    AJXTaskManager() {
    }

    void addTask(AJXTask task) {
        tasks << task
    }

    void batchExecute() {
        if (tasks.isEmpty()) {
            return
        }
        runAjcIndependent()
    }

    def runAjcIndependent() {
        def project = variantCache.project
        AjcArgs ajcArgs = new AjcArgs()
        ajcArgs.common = commonArgs
        ajcArgs.tasks = tasks
        String argsJson = new Gson().toJson(ajcArgs)
        if (!project.buildDir.exists()) {
            project.buildDir.mkdirs()
        }
        File argsFile = new File(variantCache.cachePath, "ajcargs.json")
        FileUtil.writeAsString(argsFile, argsJson)

        Configuration configuration = project.configurations["aspectjx"]
        String mainPath = AJXMain.class.protectionDomain.codeSource.location.path
        String aspectjRuntimeClassPath = Aspect.class.protectionDomain.codeSource.location.path
        String aspectjToolsClassPath = ClasspathJar.class.protectionDomain.codeSource.location.path
        String aspectjWeaverClassPath = CompilationAndWeavingContext.class.protectionDomain.codeSource.location.path
        def classPath = [mainPath, configuration, aspectjRuntimeClassPath, aspectjToolsClassPath, aspectjWeaverClassPath]
        String logPath = AJXLog.enabled ? AJXLog.outFile.absolutePath : ""
        def arguments = [argsFile.path, logPath]
        ExecResult result = project.javaexec {
            classpath classPath
            main = AJXMain.class.name
            args = arguments
        }
        result.rethrowFailure()
    }

    List<String> getCommonArgs() {
        def args = [
                "-showWeaveInfo",
                "-encoding", encoding,
                "-source", sourceCompatibility,
                "-target", targetCompatibility,
                "-classpath", classPath.join(File.pathSeparator),
                "-bootclasspath", bootClassPath
        ]
        if (!aspectPath.isEmpty()) {
            args << '-aspectpath'
            args << aspectPath.join(File.pathSeparator)
        }
        if (ajcArgs != null && !ajcArgs.isEmpty()) {
            if (!ajcArgs.contains('-Xlint')) {
                args.add('-Xlint:ignore')
            }
            if (!ajcArgs.contains('-warn')) {
                args.add('-warn:none')
            }
            args.addAll(ajcArgs)
        } else {
            args.add('-Xlint:ignore')
            args.add('-warn:none')
        }
        return args
    }

    boolean isEmpty() {
        return tasks.isEmpty()
    }
}
