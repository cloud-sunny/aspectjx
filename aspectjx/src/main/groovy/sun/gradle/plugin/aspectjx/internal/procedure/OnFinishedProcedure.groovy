package sun.gradle.plugin.aspectjx.internal.procedure

import com.android.build.api.transform.TransformInvocation
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import org.gradle.api.Project

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class OnFinishedProcedure extends AbsProcedure {
    OnFinishedProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        ajxCache.commit()
        AJXLog.d("Finished")
        return true
    }
}
