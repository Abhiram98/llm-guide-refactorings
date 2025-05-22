package com.intellij.ml.llm.template.refactoringobjects.inspection

import com.intellij.analysis.AnalysisScope
import com.intellij.analysis.BaseAnalysisActionDialog
import com.intellij.application.options.schemes.SchemesCombo
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.InspectionsBundle
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.actions.CodeInspectionAction
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.codeInspection.ui.InspectionTreeNode
import com.intellij.codeInspection.ui.ProblemDescriptionNode
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.ui.content.ContentManager
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import javax.swing.JComponent
import javax.swing.SwingUtilities.invokeAndWait

open class CustomCodeInspectionAction : CodeInspectionAction {
    private var myRunId = 0
    var myGlobalInspectionContext: CustomGlobalInspectionContext? = null
    protected var myExternalProfile: InspectionProfileImpl? = null
    val problems: MutableList<IdeInspection.MyProblem> = mutableListOf()
    val problemDescriptors: MutableList<ProblemDescriptionNode> = mutableListOf()


    class CustomGlobalInspectionContext(project: Project,
                                                contentManager: NotNullLazyValue<out ContentManager>
    ) : GlobalInspectionContextImpl(project, contentManager) {

        var completed = false;
        override fun cleanup() {
            super.cleanup()
        }

        override fun notifyInspectionsFinished(scope: AnalysisScope) {
            completed = true
            super.notifyInspectionsFinished(scope)
        }
    }

    constructor() : super(
        InspectionsBundle.messagePointer("inspection.action.title").get(),
        InspectionsBundle.messagePointer("inspection.action.noun").get()
    )

    fun cleanup(){
        myGlobalInspectionContext?.cleanup()
    }

    constructor(title: @DialogTitle String?, analysisNoun: @Nls String?) : super(title, analysisNoun)

    fun doAnalysis(project: Project, scope: AnalysisScope){
        try{
            runInspections(project, scope)
        } catch (e: Exception){
            print("Failed to run inspections! ;/")
            e.printStackTrace()
            myGlobalInspectionContext?.cleanup()
            myGlobalInspectionContext?.completed=true
            throw Exception("Failed to run code inspection - $e")
        }
    }

    fun waitForCompletion(){
        var sleepTime = 0
        Thread.sleep(5000)
        if (myGlobalInspectionContext == null)
            return
        while(!myGlobalInspectionContext!!.completed && sleepTime < 20){
            Thread.sleep(1000)
            sleepTime += 1
        }
        if (myGlobalInspectionContext!!.view==null){
            return // inspection found nothing
        }
        if (!myGlobalInspectionContext!!.completed) {
            myGlobalInspectionContext!!.cleanup()
            return
        }

        val root = myGlobalInspectionContext!!.view.tree.root
        val errors = getAllProblemChildren(root).filter {
            val level = it.javaClass.getDeclaredField("myLevel")
            level.isAccessible = true
            (level.get(it) as HighlightDisplayLevel) == HighlightDisplayLevel.ERROR
        }
        problemDescriptors.addAll(errors)
        val descriptions = errors.map{
            val desc = (it.descriptor as? ProblemDescriptorBase)
            IdeInspection.MyProblem(desc?.lineNumber?.plus(1) ?: 0, it.toString())
        }
        problems.addAll(descriptions)
        myGlobalInspectionContext!!.cleanup()
    }
    private fun getAllProblemChildren(root: InspectionTreeNode): List<ProblemDescriptionNode>{
        val problemNodes = mutableListOf<ProblemDescriptionNode>()
        for (c in root.children){
            val problemDescriptionNode = c as? ProblemDescriptionNode
            if (problemDescriptionNode !=null)
                problemNodes.add(problemDescriptionNode)
            problemNodes.addAll(getAllProblemChildren(c))
        }
        return problemNodes
    }

    override fun analyze(project: Project, scope: AnalysisScope) {
        try {
            runInspections(project, scope)
        } finally {
            myGlobalInspectionContext = null
            myExternalProfile = null
        }
    }

    override fun runInspections(
        project: Project,
        scope: AnalysisScope
    ) {
        val runId = ++myRunId
        scope.setSearchInLibraries(false)
//        FileDocumentManager.getInstance().saveAllDocuments()

        val externalProfile = myExternalProfile
        val inspectionContext = getGlobalInspectionContext(project)
        inspectionContext.setRerunAction {
            DumbService.getInstance(project).smartInvokeLater(Runnable {
                //someone called the runInspections before us, we cannot restore the state
                if (runId != myRunId) return@Runnable
                if (project.isDisposed) return@Runnable
                if (!scope.isValid) return@Runnable

                //restore current state
                myExternalProfile = externalProfile
                myGlobalInspectionContext = inspectionContext

//                FileDocumentManager.getInstance().saveAllDocuments()
                analyze(project, scope)
            })
        }

        inspectionContext.setExternalProfile(externalProfile)
        inspectionContext.setCurrentScope(scope)
        inspectionContext.doInspections(scope)
    }


    private fun getGlobalInspectionContext(project: Project): CustomGlobalInspectionContext {
        if (myGlobalInspectionContext == null) {
            val inspectionManagerEx = (InspectionManager.getInstance(project) as InspectionManagerEx)
//            myGlobalInspectionContext =
//                inspectionManagerEx.createNewGlobalContext()
            myGlobalInspectionContext = CustomGlobalInspectionContext(project, inspectionManagerEx.contentManager)
            inspectionManagerEx.runningContexts.add(myGlobalInspectionContext)

        }
        return myGlobalInspectionContext!!
    }

    override fun getHelpTopic(): @NonNls String? {
        return "reference.dialogs.inspection.scope"
    }

    override fun canceled() {
        super.canceled()
        myGlobalInspectionContext = null
    }

    override fun getAdditionalActionSettings(project: Project, dialog: BaseAnalysisActionDialog): JComponent? {
        return null
//        dialog.setShowInspectInjectedCode(true)
//        val ui = CodeInspectionAdditionalUi()
//        val manager = InspectionManager.getInstance(project) as InspectionManagerEx
//        val profiles: SchemesCombo<InspectionProfileImpl?> = ui.browseProfilesCombo
//        val profileManager = InspectionProfileManager.getInstance()
//        val projectProfileManager = ProjectInspectionProfileManager.getInstance(project)
//        ui.link.addActionListener { `__`: ActionEvent? ->
//            val errorConfigurable =
//                createConfigurable(projectProfileManager, profiles)
//            val editor = MySingleConfigurableEditor(project, errorConfigurable, manager)
//            if (editor.showAndGet()) {
//                reloadProfiles(profiles, profileManager, projectProfileManager, project)
//                if (errorConfigurable.mySelectedName != null) {
//                    val profile =
//                        (if (errorConfigurable.mySelectedIsProjectProfile) projectProfileManager else profileManager)
//                            .getProfile(errorConfigurable.mySelectedName!!)
//                    profiles.selectScheme(profile)
//                }
//            } else {
//                //if profile was disabled and cancel after apply was pressed
//                val profile: InspectionProfile? = profiles.selectedScheme
//                val canExecute = profile != null && profile.isExecutable(project)
//                dialog.isOKActionEnabled = canExecute
//            }
//        }
//        profiles.addActionListener { `__`: ActionEvent? ->
//            myExternalProfile = profiles.selectedScheme
//            val canExecute = myExternalProfile != null && myExternalProfile!!.isExecutable(project)
//            dialog.isOKActionEnabled = canExecute
//            if (canExecute) {
//                PropertiesComponent.getInstance(project).setValue(
//                    LAST_SELECTED_PROFILE_PROP,
//                    (if (myExternalProfile!!.isProjectLevel) 'p' else 'a').toString() + myExternalProfile!!.name
//                )
//                manager.setProfile(myExternalProfile!!.name)
//            }
//        }
//        reloadProfiles(profiles, profileManager, projectProfileManager, project)
//        if (hasEnabledInspectionsOnInjectableCode(project)) {
//            dialog.isAnalyzeInjectedCode = false
//        }
//        return ui.panel
    }

//    private fun hasEnabledInspectionsOnInjectableCode(project: Project): Boolean {
//        if (myExternalProfile != null) {
//            return ContainerUtil.exists(
//                myExternalProfile!!.getAllEnabledInspectionTools(project)
//            ) { tool: Tools ->
//                Language.findLanguageByID(
//                    tool.tool.language
//                ) is InjectableLanguage
//            }
//        }
//        return false
//    }

    override fun createConfigurable(
        projectProfileManager: ProjectInspectionProfileManager,
        profilesCombo: SchemesCombo<InspectionProfileImpl?>
    ): MyExternalProfilesComboboxAwareInspectionToolsConfigurable {
        return MyExternalProfilesComboboxAwareInspectionToolsConfigurable(projectProfileManager, profilesCombo)
    }

    protected class MyExternalProfilesComboboxAwareInspectionToolsConfigurable(
        projectProfileManager: ProjectInspectionProfileManager,
        private val myProfilesCombo: SchemesCombo<InspectionProfileImpl?>
    ) :
        ExternalProfilesComboboxAwareInspectionToolsConfigurable(projectProfileManager, myProfilesCombo) {
        var mySelectedName: String? = null
        var mySelectedIsProjectProfile: Boolean = false

        override fun getCurrentProfile(): InspectionProfileImpl {
            return myProfilesCombo.selectedScheme!!
        }

        override fun applyRootProfile(name: String, isProjectLevel: Boolean) {
            mySelectedName = name
            mySelectedIsProjectProfile = isProjectLevel
        }
    }

//    private fun reloadProfiles(
//        profilesCombo: SchemesCombo<InspectionProfileImpl?>,
//        appProfileManager: InspectionProfileManager,
//        projectProfileManager: InspectionProjectProfileManager,
//        project: Project
//    ) {
//        val selectedProfile = getProfileToUse(project, appProfileManager, projectProfileManager)
//        profilesCombo.resetSchemes(
//            InspectionProfileSchemesModel.getSortedProfiles(
//                appProfileManager,
//                projectProfileManager
//            )
//        )
//        profilesCombo.selectScheme(selectedProfile)
//    }

//    private fun getProfileToUse(
//        project: Project,
//        appProfileManager: InspectionProfileManager,
//        projectProfileManager: InspectionProjectProfileManager
//    ): InspectionProfileImpl {
//        val lastSelectedProfile = PropertiesComponent.getInstance(project).getValue(
//            LAST_SELECTED_PROFILE_PROP
//        )
//        if (lastSelectedProfile != null) {
//            val type = lastSelectedProfile[0]
//            val lastSelectedProfileName = lastSelectedProfile.substring(1)
//            if (type == 'a') {
//                val profile = appProfileManager.getProfile(lastSelectedProfileName, false)
//                if (profile != null) return profile
//            } else {
//                LOG.assertTrue(
//                    type == 'p',
//                    "Unexpected last selected profile: '$lastSelectedProfile'"
//                )
//                val profile = projectProfileManager.getProfile(lastSelectedProfileName, false)
//                if (profile != null && profile.isProjectLevel) return profile
//            }
//        }
//        return getGlobalInspectionContext(project).currentProfile
//    }
//
//    private class MySingleConfigurableEditor(
//        project: Project?,
//        configurable: ErrorsConfigurable?,
//        private val myManager: InspectionManagerEx
//    ) :
//        SingleConfigurableEditor(project, configurable, createDimensionKey(configurable)) {
//        override fun doOKAction() {
//            val o = (configurable as ErrorsConfigurable).selectedObject
//            if (o is InspectionProfile) {
//                myManager.setProfile(o.name)
//            }
//            super.doOKAction()
//        }
//    }

    companion object {
//        private val LOG = Logger.getInstance(
//            CodeInspectionAction::class.java
//        )
//        private const val LAST_SELECTED_PROFILE_PROP = "run.code.analysis.last.selected.profile"
    }
}