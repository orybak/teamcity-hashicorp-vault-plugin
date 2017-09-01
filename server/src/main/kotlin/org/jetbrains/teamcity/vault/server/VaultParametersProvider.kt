package org.jetbrains.teamcity.vault.server

import jetbrains.buildServer.agent.Constants
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.oauth.OAuthConstants
import jetbrains.buildServer.serverSide.parameters.AbstractBuildParametersProvider
import org.jetbrains.teamcity.vault.VaultConstants
import org.jetbrains.teamcity.vault.VaultReferencesUtil
import org.jetbrains.teamcity.vault.isShouldSetEnvParameters

class VaultParametersProvider : AbstractBuildParametersProvider() {
    companion object {
        internal fun isFeatureEnabled(build: SBuild): Boolean {
            val buildType = build.buildType ?: return false
            val project = buildType.project

            val projectFeature = project.getAvailableFeaturesOfType(VaultConstants.FeatureSettings.FEATURE_TYPE).firstOrNull()
            if (projectFeature != null) return true

            // It's faster than asking OAuthConectionsManager
            if (project.getAvailableFeaturesOfType(OAuthConstants.FEATURE_TYPE).any {
                VaultConstants.FeatureSettings.FEATURE_TYPE == it.parameters[OAuthConstants.OAUTH_TYPE_PARAM]
            }) return true

            val buildFeature = buildType.getBuildFeaturesOfType(VaultConstants.FeatureSettings.FEATURE_TYPE).firstOrNull()
            return buildFeature != null && buildType.isEnabled(buildFeature.id)
        }

    }

    override fun getParametersAvailableOnAgent(build: SBuild): Collection<String> {
        if (build.isFinished) return emptyList()

        if (!isFeatureEnabled(build)) return emptyList()

        val exposed = HashSet<String>()
        val parameters = build.buildOwnParameters
        if (isShouldSetEnvParameters(parameters)) {
            exposed += Constants.ENV_PREFIX + VaultConstants.AgentEnvironment.VAULT_TOKEN
            exposed += Constants.ENV_PREFIX + VaultConstants.AgentEnvironment.VAULT_ADDR
        }
        VaultReferencesUtil.collect(parameters, exposed)
        return exposed
    }
}

