package sun.gradle.plugin.aspectjx.internal


import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.AJXPlugin
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap

/**
 * trace task execute time
 * @author simon* @version 1.0.0* @since 2016-04-20
 */
class TimeTrace implements TaskExecutionListener, BuildListener {

    private clocks = new ConcurrentHashMap()
    private times = []
    private static final DISPLAY_TIME_THRESHOLD = 50

    void buildStarted(Gradle gradle) {
        File logFile = new File(gradle.rootProject.getRootDir(), "ajc.log");
        if (logFile.isFile()) {
            logFile.delete()
        }
        AJXLog.init(logFile.absolutePath)
    }

    @Override
    void settingsEvaluated(Settings settings) {

    }

    @Override
    void projectsLoaded(Gradle gradle) {

    }

    @Override
    void projectsEvaluated(Gradle gradle) {
        buildStarted(gradle)
    }

    @Override
    void buildFinished(BuildResult result) {
        LoggerFactory.getLogger(AJXPlugin).debug("Tasks spend time > ${DISPLAY_TIME_THRESHOLD}ms:")
        AJXLog.destroy()
        times.sort { lhs, rhs -> -(lhs[0] - rhs[0]) }
                .grep { it[0] > DISPLAY_TIME_THRESHOLD }
                .each { time -> printf "%14s   %s\n", formatTime(time[0]), time[1] }
    }

    @Override
    void beforeExecute(Task task) {
        clocks[task.path] = new Clock(System.currentTimeMillis())
    }

    @Override
    void afterExecute(Task task, TaskState state) {
        clocks.remove(task.path)?.with { clock ->
            def ms = clock.timeInMs
            times.add([ms, task.path])
            AJXLog.d("${task.path} spend ${ms}ms")
        }
    }

    static def formatTime(ms) {
        def sec = ms.intdiv(1000)
        def min = sec.intdiv(60)
        sec %= 60
        ms = (ms % 1000).intdiv(10)
        return String.format("%02d:%02d.%02d", min, sec, ms)
    }
}
