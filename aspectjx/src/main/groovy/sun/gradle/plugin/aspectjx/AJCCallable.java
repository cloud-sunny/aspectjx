package sun.gradle.plugin.aspectjx;

import sun.gradle.plugin.aspectjx.AJXMessageHandler;
import sun.gradle.plugin.aspectjx.internal.AJXTask;
import sun.gradle.plugin.aspectjx.internal.AJXUtils;
import sun.gradle.plugin.aspectjx.internal.concurrent.ITask;

import org.aspectj.tools.ajc.Main;

import java.io.File;
import java.util.List;

/**
 * @author WingHawk
 */
public class AJCCallable implements ITask {

    private final AJXTask task;
    private final String[] ajcArgs;

    public AJCCallable(AJXTask task, List<String> commonArgs) {
        this.task = task;
        commonArgs.addAll(task.getIoArgs());
        ajcArgs = commonArgs.toArray(new String[]{});
    }

    @Override
    public Object call() {
        new Main().run(ajcArgs, new AJXMessageHandler());
        if (task.getOutputDir() != null) {
            AJXUtils.mergeJar(new File(task.getOutputDir()), new File(task.getOutputJar()));
        }
        return null;
    }
}
