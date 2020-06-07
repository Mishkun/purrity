package io.github.mishkun.purrity

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.canBeUsedForConstVal
import org.jetbrains.kotlin.descriptors.impl.referencedProperty
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

internal class PureFunctionsLocalTool : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                if (function.annotationEntries.all { "Pure" !in it.text }) return
                function.accept(object : KtTreeVisitorVoid() {
                    override fun visitCallExpression(expression: KtCallExpression) {
                        super.visitCallExpression(expression)
                        val childOfType = PsiTreeUtil.getChildOfType(expression, KtReferenceExpression::class.java)
                        val context = expression.analyze(BodyResolveMode.FULL)
                        val declarationDescriptor = context.get(BindingContext.REFERENCE_TARGET, childOfType)
                        if (declarationDescriptor?.annotations?.any { "Pure" in it.fqName.toString() } == false) {
                            holder.registerProblem(expression, "Impure function call inside pure")
                        }
                    }

                    override fun visitReferenceExpression(expression: KtReferenceExpression) {
                        super.visitReferenceExpression(expression)
                        val context = expression.analyze(BodyResolveMode.FULL)
                        val declarationDesc = context.get(BindingContext.REFERENCE_TARGET, expression)
                        val prop = declarationDesc?.referencedProperty ?: return
                        if (prop.isVar) {
                            holder.registerProblem(expression, "Var field access inside pure function")
                        }
                        val typeDescriptor = prop.returnType ?: return
                        val classDescriptor = DescriptorUtils.getClassDescriptorForType(typeDescriptor)
                        if (!(typeDescriptor.canBeUsedForConstVal() || classDescriptor.isData)) {
                            if (classDescriptor.annotations.all { "EffectivelyImmutable" !in it.fqName.toString()}) {
                                holder.registerProblem(expression, "Referenced mutable type")
                            }
                        }
                        if (classDescriptor.annotations.any { "Mutable" in it.fqName.toString()}) {
                            holder.registerProblem(expression, "Referenced mutable type")
                        }
                    }
                })
            }
        }
    }
}
