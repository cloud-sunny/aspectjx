package sun.gradle.plugin.aspectjx.internal


import com.google.gson.Gson
import sun.gradle.plugin.aspectjx.AJCCallable
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.concurrent.BatchTaskScheduler
import sun.gradle.plugin.aspectjx.internal.model.AjcArgs
import org.aspectj.util.FileUtil

class AJXMain {

    static void main(String[] args) {
        System.setProperty("aspectj.multithreaded", "true")
        // it's another process, so we must init AjxLog again
        if (args.length == 0) {
            return
        }
        if (args.length > 1 && !args[1].isEmpty()) {
            AJXLog.setEnabled(true)
            AJXLog.init(args[1])
        }
        String json = FileUtil.readAsString(new File(args[0]))
        AjcArgs ajcArgs = new Gson().fromJson(json, AjcArgs.class)

        BatchTaskScheduler taskScheduler = new BatchTaskScheduler(true)
        ajcArgs.tasks.forEach {
            taskScheduler.addTask(new AJCCallable(it, new ArrayList<>(ajcArgs.common)))
        }
        long startTime = System.currentTimeMillis()
        taskScheduler.execute()
        AJXLog.d("doAspectjCompile spend ${System.currentTimeMillis() - startTime}ms.")
        AJXLog.destroy()
    }
}