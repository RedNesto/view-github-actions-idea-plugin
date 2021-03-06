package org.github.otanikotani.workflow

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import org.github.otanikotani.api.GitHubWorkflowRun
import org.github.otanikotani.workflow.data.GitHubWorkflowDataLoader
import org.github.otanikotani.workflow.data.GitHubWorkflowRunDataProvider
import org.github.otanikotani.workflow.data.GitHubWorkflowRunListLoader
import org.jetbrains.annotations.CalledInBackground
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.pullrequest.data.GHListLoader
import org.jetbrains.plugins.github.util.GitRemoteUrlCoordinates
import org.jetbrains.plugins.github.util.GithubUrlUtil
import java.io.IOException

@Service
internal class GitHubWorkflowDataContextRepository {
    @CalledInBackground
    @Throws(IOException::class)
    fun getContext(disposable: Disposable,
                   account: GithubAccount,
                   requestExecutor: GithubApiRequestExecutor,
                   gitRemoteCoordinates: GitRemoteUrlCoordinates): GitHubWorkflowRunDataContext {
        LOG.debug("Get GitHubWorkflowRunDataContext")
        LOG.debug("Get User and  repository")
        val fullPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(gitRemoteCoordinates.url)
            ?: throw IllegalArgumentException(
                "Invalid GitHub Repository URL - ${gitRemoteCoordinates.url} is not a GitHub repository")

        val repositoryCoordinates = GitHubRepositoryCoordinates(account.server, fullPath)

        LOG.debug("Create GitHubWorkflowDataLoader")
        val githubWorkflowDataLoader = GitHubWorkflowDataLoader {
            GitHubWorkflowRunDataProvider(ProgressManager.getInstance(), requestExecutor, it)
        }

        requestExecutor.addListener(githubWorkflowDataLoader) {
            githubWorkflowDataLoader.invalidateAllData()
        }

        LOG.debug("Create CollectionListModel<GitHubWorkflowRun>() and loader")
        val listModel = CollectionListModel<GitHubWorkflowRun>()
        val listLoader = GitHubWorkflowRunListLoader(ProgressManager.getInstance(), requestExecutor,
            repositoryCoordinates,
            listModel)

        listLoader.addDataListener(disposable, object : GHListLoader.ListDataListener {
            override fun onDataAdded(startIdx: Int) {
                val loadedData = listLoader.loadedData
                listModel.add(loadedData.subList(startIdx, loadedData.size))
            }
        })

        return GitHubWorkflowRunDataContext(
            repositoryCoordinates,
            listModel,
            githubWorkflowDataLoader,
            listLoader,
            account)
    }

    companion object {
        private val LOG = logger("org.github.otanikotani")

        fun getInstance(project: Project) = project.service<GitHubWorkflowDataContextRepository>()
    }
}