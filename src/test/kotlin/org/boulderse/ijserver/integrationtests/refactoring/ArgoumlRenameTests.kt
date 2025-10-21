package org.boulderse.ijserver.integrationtests.refactoring

import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

class ArgoumlRenameTests: RenameTestsBase() {

    val argoUmlProject = GitHubProject.fromGithub(
        repoRelativeUrl = "argouml-tigris-org/argouml",
        branchName = "master"
    )

    @ParameterizedTest(name = "argouml rename case {index}: {0}")
    @MethodSource("renameCases")
    fun argoUMLRenameTestParameterized(case: RenameCase) {
        Starter.newContext(testName = "Suite to trigger renames in argoUML - ${case.commitHash}",
            TestCase(
                IdeProductProvider.IC,
                projectInfo = argoUmlProject.onCommit(case.commitHash)
            )
                .withVersion("2025.2")).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(5.minutes)
            renameTest(case)
        }
    }

    companion object {
        @JvmStatic
        fun renameCases(): List<RenameCase> {
            return listOf(
                RenameCase(
                    commitHash = "8154894d806e29d83bcdd34655ed2417a24d4ffc",
                    filePath = "src_new/org/argouml/uml/cognitive/critics/ClAttributeCompartment.java",
                    oldName = "fig",
                    newName = "attributesCompartmentFig",
                    lineNum = 56,
                    codeElementType = "field"
                ),
                RenameCase(
                    commitHash = "982c953f978c3238fa7bde44e2a1147197a7d1bb",
                    filePath = "src_new/org/argouml/uml/diagram/ui/ActionAddAllClassesFromModel.java",
                    oldName = "myTabName",
                    newName = "TabName",
                    lineNum = 55,
                    codeElementType = "parameter"
                ),
                RenameCase(
                    commitHash = "1ab4e8046393cac62d61eb0e3c5e82eb5e8bb921",
                    filePath = "src_new/org/argouml/uml/diagram/activity/ui/SelectionActionState.java",
                    oldName = "cls",
                    newName = "existingNode",
                    lineNum = 237,
                    codeElementType = "variable"
                ),
                RenameCase(
                    commitHash = "804aaf9d327fa004acd673467bc2e72256dc8f26",
                    filePath = "src/uci/uml/ui/Actions.java",
                    oldName = "e",
                    newName = "ae",
                    lineNum = 629,
                    codeElementType = "parameter"
                ),
                RenameCase(
                    commitHash = "54d2a049e5f6cd5fc8baccf25c58f888deb47a90",
                    filePath = "src_new/org/argouml/uml/ui/UMLModelElementListModel2.java",
                    oldName = "target",
                    newName = "theNewTarget",
                    lineNum = 250,
                    codeElementType = "parameter"
                ),
                RenameCase(
                    commitHash = "956c16de8e38b8090012db4c4c2b90a9b6a7e3db",
                    filePath = "src/uci/uml/critics/ClClassName.java",
                    oldName = "fc",
                    newName = "fnme",
                    lineNum = 51,
                    codeElementType = "variable"
                ),

                RenameCase(
                    commitHash = "956c16de8e38b8090012db4c4c2b90a9b6a7e3db",
                    filePath = "src/uci/uml/critics/ClClassName.java",
                    oldName = "ClClassName",
                    newName = "ClClassName22",
                    lineNum = 36,
                    codeElementType = "class"
                ),

                RenameCase(
                    commitHash = "3a89da0fec36336116decff9a81fc66551c1ef4d",
                    filePath = "src/uci/gef/ModeCreateEdge.java",
                    oldName = "snapX",
                    newName = "mySnapX",
                    lineNum = 85,
                    codeElementType = "parameter"
                ),

                RenameCase(
                    commitHash = "1ab4e8046393cac62d61eb0e3c5e82eb5e8bb921",
                    filePath = "src_new/org/argouml/uml/diagram/activity/ui/SelectionActionState.java",
                    oldName = "cls",
                    newName = "existingNode",
                    lineNum = 237,
                    codeElementType = "variable"
                ),


                )
        }
    }


}