package org.boulderse.ijserver.refactoringobjects.extractfunction

import com.intellij.lang.Language
import org.boulderse.ijserver.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.kotlin.idea.base.psi.getLineCount
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression

class EFCandidateFactory {
    fun buildCandidates(efSuggestion: EFSuggestion, editor: Editor, file: PsiFile): HashSet<EFCandidate> {
        val candidates = HashSet<EFCandidate>()

        if (!isValid(efSuggestion, file)) {
            buildInvalidCandidate(efSuggestion).let { candidates.add(it) }
            return candidates
        }

        buildCandidateAsIs(efSuggestion, editor, file)?.let { candidates.add(it) }
        buildCandidateWithAdjustment(efSuggestion, editor, file)?.let { candidates.add(it) }

        return candidates
    }

    fun buildCandidates(efSuggestions: List<EFSuggestion>, editor: Editor, file: PsiFile): HashSet<EFCandidate> {
        val candidates = HashSet<EFCandidate>()

        efSuggestions.forEach {
            candidates.apply { addAll(buildCandidates(it, editor, file)) }
        }

        return candidates
    }

    private fun buildCandidateAsIs(efSuggestion: EFSuggestion, editor: Editor, file: PsiFile): EFCandidate? {
        val psiElementStart = PsiUtils.getLeftmostPsiElement(efSuggestion.lineStart - 1, editor, file)
        var psiElementEnd = PsiUtils.getLeftmostPsiElement(efSuggestion.lineEnd - 1, editor, file)

        if (psiElementStart == null || psiElementEnd == null) {
            return null
        }

        return EFCandidate(
            functionName = efSuggestion.functionName,
            offsetStart = psiElementStart.startOffset,
            offsetEnd = psiElementEnd.endOffset,
            lineStart = editor.document.getLineNumber(psiElementStart.startOffset) + 1,
            lineEnd = editor.document.getLineNumber(psiElementEnd.endOffset) + 1,
        ).also {
            it.efSuggestion = efSuggestion
            it.type = EfCandidateType.AS_IS
        }
    }

    private fun buildCandidateWithAdjustment(efSuggestion: EFSuggestion, editor: Editor, file: PsiFile): EFCandidate? {
        val psiElementStart = PsiUtils.getLeftmostPsiElement(efSuggestion.lineStart - 1, editor, file)
        val psiElementEnd = PsiUtils.getLeftmostPsiElement(efSuggestion.lineEnd - 1, editor, file)

        if (psiElementStart == null || psiElementEnd == null) {
            return null
        }

        val adjustedRegion = adjustRegion(psiElementStart, psiElementEnd, file.language) ?: return null
        return EFCandidate(
            functionName = efSuggestion.functionName,
            offsetStart = adjustedRegion.first.startOffset,
            offsetEnd = adjustedRegion.second.endOffset,
            lineStart = editor.document.getLineNumber(adjustedRegion.first.startOffset) + 1,
            lineEnd = editor.document.getLineNumber(adjustedRegion.second.endOffset) + 1,
        ).also {
            it.efSuggestion = efSuggestion
            it.type = EfCandidateType.ADJUSTED
        }
    }


    /**
     * Selected region is adjusted by enlarging it to the top-most PsiElement, either start or end
     *
     * Example:
     * 10. if (x == 0) {
     * 11.  println(x)
     * 12.  some more statements
     * 13.  more statements
     * 14. }
     *
     * If the selection has lines 10-13 (included), then it will enlarge the selection to lines 10-14 (included)
     */
    private fun adjustRegion(
        psiElementStart: PsiElement,
        psiElementEnd: PsiElement,
        language: Language
    ): Pair<PsiElement, PsiElement>? {
        var start = psiElementStart
        var end = psiElementEnd

        // both start and end should belong to the same function
        if (!PsiUtils.elementsBelongToSameFunction(start, end, language)) return null

        // if the start/end are inside the argument list, then bubble up to function call
        if (PsiUtils.isElementArgumentOrArgumentList(start, language)) {
            start = PsiUtils.getParentFunctionCallOrNull(start, language) ?: start
        }
        if (PsiUtils.isElementArgumentOrArgumentList(end, language)) {
            end = PsiUtils.getParentFunctionCallOrNull(end, language) ?: end
        }

        // if the start/end are inside the parameter list, then bubble up to function declaration
        if (PsiUtils.isElementParameterOrParameterList(start, language)) {
            start = PsiUtils.getParentFunctionOrNull(start, language) ?: start
        }
        if (PsiUtils.isElementParameterOrParameterList(end, language)) {
            end = PsiUtils.getParentFunctionOrNull(end, language) ?: end
        }

        // if start is parent of end, then assign end to start
        if (PsiTreeUtil.isAncestor(start, end, true)) {
            end = start
        } else if (PsiTreeUtil.isAncestor(end, start, true)) {
            start = end
        }

        // shift right to the first non-brace and non-white space element on the same line
        while ((start.node.elementType == KtTokens.LBRACE || start.node.elementType == JavaTokenType.LBRACE || start is PsiWhiteSpace) &&
            (start.getLineNumber(false) == start.nextSibling?.getLineNumber(false))
        ) {
            start = start.nextSibling
        }

        var commonParent = PsiTreeUtil.findCommonParent(start, end) ?: return null

        if (commonParent == start || commonParent == end || (commonParent !is PsiBlockStatement && commonParent !is PsiCodeBlock)) {
            start = commonParent
            end = commonParent
        }

        if (start == end) {
            commonParent = start.parent
        }

        if (commonParent == null) {
            return null
        }

        if (commonParent.lastChild == end) {
            start = commonParent.firstChild
        } else if (commonParent.firstChild == start) {
            end = commonParent.lastChild
        } else {
            start = bubbleUp(start, commonParent)
            end = bubbleUp(end, commonParent)
        }

        if (start.node.elementType == KtTokens.LBRACE || start.node.elementType == JavaTokenType.LBRACE) {
            var temp: PsiElement? = start
            // bubble up the start node to its parent statement
            while (true) {
                if (temp == null) break
                if (temp is PsiStatement && temp !is PsiBlockStatement) break
                if (temp is KtExpression && temp !is KtBlockExpression) break
                temp = temp.parent
            }
            if (temp != null) {
                start = temp
            }
        }

        return start to end
    }


    private fun bubbleUp(element: PsiElement, stopElement: PsiElement): PsiElement {
        var result = element
        while (result.parent != stopElement && result.parent != null) {
            result = result.parent
        }

        return result
    }

    private fun isValid(efSuggestion: EFSuggestion, file: PsiFile): Boolean {
        return (efSuggestion.lineStart in (1 until file.getLineCount())) && (efSuggestion.lineEnd in (1 until file.getLineCount()))
    }

    private fun buildInvalidCandidate(efSuggestion: EFSuggestion): EFCandidate {
        return EFCandidate(
            functionName = efSuggestion.functionName,
            lineStart = 0,
            lineEnd = 0,
            offsetStart = -1,
            offsetEnd = -1,
        ).also {
            it.efSuggestion = efSuggestion
            it.type = EfCandidateType.INVALID
        }
    }
}