package sun.gradle.plugin.aspectjx.internal.procedure

import com.android.build.api.transform.TransformInvocation
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import sun.gradle.plugin.aspectjx.AJXLog
import sun.gradle.plugin.aspectjx.internal.AJXUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class CheckAspectJXEnableProcedure extends AbsProcedure {

    CheckAspectJXEnableProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        AJXLog.d("Check aspectjx enable")
        if (!ajxConfig.enabled) {
            AJXUtils.doWorkWithNoAspectj(transformInvocation)
            return false
        }

        return true
    }
}
