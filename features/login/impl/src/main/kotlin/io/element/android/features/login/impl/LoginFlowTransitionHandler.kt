/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.navigation.transition.ModifierTransitionHandler
import com.bumble.appyx.core.navigation.transition.TransitionDescriptor
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.transitionhandler.rememberBackstackFader
import com.bumble.appyx.navmodel.backstack.transitionhandler.rememberBackstackSlider

/**
 * A TransitionHandler that uses fade transition when CheckClassic is being removed,
 * and slide transition for all other cases.
 */
class LoginFlowTransitionHandler(
    private val backstack: BackStack<LoginFlowNode.NavTarget>,
    private val slider: ModifierTransitionHandler<LoginFlowNode.NavTarget, BackStack.State>,
    private val fader: ModifierTransitionHandler<LoginFlowNode.NavTarget, BackStack.State>,
) : ModifierTransitionHandler<LoginFlowNode.NavTarget, BackStack.State>() {
    override fun createModifier(
        modifier: Modifier,
        transition: Transition<BackStack.State>,
        descriptor: TransitionDescriptor<LoginFlowNode.NavTarget, BackStack.State>
    ): Modifier {
        val isCheckClassicBeingRemoved = backstack.elements.value.any { element ->
            element.key.navTarget == LoginFlowNode.NavTarget.CheckClassic &&
                element.targetState != BackStack.State.ACTIVE
        }
        val handler = if (isCheckClassicBeingRemoved) fader else slider
        return handler.createModifier(modifier, transition, descriptor)
    }
}

@Composable
fun rememberLoginFlowTransitionHandler(
    backstack: BackStack<LoginFlowNode.NavTarget>,
): ModifierTransitionHandler<LoginFlowNode.NavTarget, BackStack.State> {
    val slider = rememberBackstackSlider<LoginFlowNode.NavTarget>(
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
    )
    val fader = rememberBackstackFader<LoginFlowNode.NavTarget>(
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
    )
    return remember(backstack, slider, fader) {
        LoginFlowTransitionHandler(backstack, slider, fader)
    }
}
