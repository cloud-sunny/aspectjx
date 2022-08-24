package sun.gradle.plugin.aspectjx

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import org.gradle.api.Project
import sun.gradle.plugin.aspectjx.internal.cache.VariantCache
import sun.gradle.plugin.aspectjx.internal.procedure.AJXProcedure
import sun.gradle.plugin.aspectjx.internal.procedure.CacheAspectFilesProcedure
import sun.gradle.plugin.aspectjx.internal.procedure.CacheInputFilesProcedure
import sun.gradle.plugin.aspectjx.internal.procedure.CheckAspectJXEnableProcedure
import sun.gradle.plugin.aspectjx.internal.procedure.DoAspectWorkProcedure
import sun.gradle.plugin.aspectjx.internal.procedure.OnFinishedProcedure
import sun.gradle.plugin.aspectjx.internal.procedure.UpdateAspectFilesProcedure
import sun.gradle.plugin.aspectjx.internal.procedure.UpdateAspectOutputProcedure
import sun.gradle.plugin.aspectjx.internal.procedure.UpdateInputFilesProcedure

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-03-12
 */
class AJXTransform extends Transform {

    private AJXProcedure ajxProcedure
    private Project project

    AJXTransform(Project project) {
        this.project = project
        this.ajxProcedure = new AJXProcedure(project)
    }

    @Override
    String getName() {
        return "ajx"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return ImmutableSet.<QualifiedContent.ContentType> of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        //是否支持增量编译
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        AJXLog.d("AJXTransform start")
        long startTime = System.nanoTime()
        VariantCache variantCache = new VariantCache(project, ajxProcedure.ajxCache, transformInvocation.context.variantName)
        ajxProcedure.clearProcedures()
        ajxProcedure.with(new CheckAspectJXEnableProcedure(project, variantCache, transformInvocation))

        if (transformInvocation.incremental) {
            //incremental build
            ajxProcedure.with(new UpdateAspectFilesProcedure(project, variantCache, transformInvocation))
            ajxProcedure.with(new UpdateInputFilesProcedure(project, variantCache, transformInvocation))
            ajxProcedure.with(new UpdateAspectOutputProcedure(project, variantCache, transformInvocation))
        } else {
            //delete output and cache before full build
            transformInvocation.outputProvider.deleteAll()
            variantCache.reset()

            ajxProcedure.with(new CacheAspectFilesProcedure(project, variantCache, transformInvocation))
            ajxProcedure.with(new CacheInputFilesProcedure(project, variantCache, transformInvocation))
            ajxProcedure.with(new DoAspectWorkProcedure(project, variantCache, transformInvocation))
        }
        ajxProcedure.with(new OnFinishedProcedure(project, variantCache, transformInvocation))
        ajxProcedure.doWorkContinuously()
        AJXLog.d("Ajx transform spend ${((System.nanoTime() - startTime) / 1_000_000) as int}ms")
    }
}
