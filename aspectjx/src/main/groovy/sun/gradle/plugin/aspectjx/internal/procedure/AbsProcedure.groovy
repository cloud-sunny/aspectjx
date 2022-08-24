package sun.gradle.plugin.aspectjx.internal.procedure

import com.android.build.api.transform.TransformInvocation
import org.gradle.api.Project
import sun.gradle.plugin.aspectjx.internal.cache.AJXCache
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import sun.gradle.plugin.aspectjx.internal.model.AJXExtensionConfig

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
abstract class AbsProcedure {

    List<? extends AbsProcedure> procedures = new ArrayList<>()
    Project project
    AJXCache ajxCache
    AJXExtensionConfig ajxConfig
    VariantCache variantCache
    TransformInvocation transformInvocation

    AbsProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        this.project = project
        if (transformInvocation != null) {
            this.transformInvocation = transformInvocation
        }

        if (variantCache != null) {
            this.variantCache = variantCache
            this.ajxCache = variantCache.ajxCache
            this.ajxConfig = ajxCache.ajxConfig
        }
    }

    public <T extends AbsProcedure> AbsProcedure with(T procedure) {
        if (procedure != null) {
            procedures << procedure
        }

        return this
    }

    public void clearProcedures() {
        procedures.clear()
    }

    boolean doWorkContinuously() {
        for (AbsProcedure procedure : procedures) {
            if (!procedure.doWorkContinuously()) {
                break
            }
        }
        return true
    }
}
