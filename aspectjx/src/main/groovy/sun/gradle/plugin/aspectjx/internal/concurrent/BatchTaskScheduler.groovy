package sun.gradle.plugin.aspectjx.internal.concurrent

import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-04
 */
class BatchTaskScheduler {
    List<? extends ITask> tasks = Collections.synchronizedList(new ArrayList<>())

    protected boolean parallel

    BatchTaskScheduler(boolean parallel) {
        this.parallel = parallel
    }

    def <T extends ITask> void addTask(T task) {
        tasks << task
    }

    void execute() {
        int taskCount = tasks.size()
        if (taskCount == 0) {
            return
        }
        if (taskCount == 1) {
            tasks.get(0).call()
            return
        }
        if (parallel) {
            int cpuCount = Runtime.getRuntime().availableProcessors()
            def executor = Executors.newFixedThreadPool(Math.min(cpuCount, taskCount))
            List<Future<?>> futures = new ArrayList()
            tasks.each {
                Future<?> future = executor.submit(it)
                futures.add(future)
            }

            try {
                // 这里调一下 get 仅仅是为了将执行中的异常抛出
                futures.each { it.get() }
            } finally {
                executor.shutdownNow()
            }
        } else {
            tasks.each {  it.call() }
        }
        tasks.clear()
    }

    boolean isEmpty() {
        return tasks.isEmpty()
    }
}
