package io.github.mishkun.purrity

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.canBeUsedForConstVal
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens.VAR_KEYWORD
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class CheckImmutableDataClassLocal : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtTreeVisitorVoid() {
            override fun visitClass(ktClass: KtClass) {
                super.visitClass(ktClass)
                if (!ktClass.isData()) return
                if (ktClass.annotationEntries.any { it.text.toString() == "@Mutable" }) return
                val constructor = ktClass.primaryConstructor ?: return
                val context = constructor.analyze(BodyResolveMode.FULL)
                for (parameter in constructor.valueParameters) {
                    if (parameter?.hasValOrVar() == true) {
                        val valOrVarKeyword = parameter.valOrVarKeyword
                        if (valOrVarKeyword?.text == VAR_KEYWORD.value) {
                            holder.registerProblem(valOrVarKeyword, "Data class should only use var or be @Mutable!")
                        }
                    }
                    val typeReference = parameter.typeReference ?: continue
                    val typeDescriptor = context.get(BindingContext.TYPE, typeReference) ?: continue
                    val classDescriptor = DescriptorUtils.getClassDescriptorForType(typeDescriptor)
                    val fqName = classDescriptor.let { DescriptorUtils.getFqName(it) }
                    if (!(typeDescriptor.canBeUsedForConstVal() || classDescriptor.isData)) {
                        if (classDescriptor.annotations.all { "EffectivelyImmutable" !in it.fqName.toString()}) {
                            holder.registerProblem(typeReference, "Data class parameter is not data class itself, but $fqName!")
                        }
                    }
                    if (classDescriptor.annotations.any { "Mutable" in it.fqName.toString()}) {
                        holder.registerProblem(typeReference, "Data class parameter ${parameter.name} is Mutable data class!")
                    }
                }
            }
        }
    }
}
