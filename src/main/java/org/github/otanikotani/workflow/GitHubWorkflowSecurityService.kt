package org.github.otanikotani.workflow

import org.jetbrains.plugins.github.api.data.GHUser
import org.jetbrains.plugins.github.api.data.GithubUser

interface GitHubWorkflowSecurityService {
    val currentUser: GHUser

    fun isCurrentUser(user: GithubUser): Boolean
}