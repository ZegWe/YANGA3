package com.yanga.client.ui.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Single-pane Navigation 3 display with a separate drag / commit / cancel animation.
 * SceneState still owns movable entry content, saved state and entry lifecycles.
 * Keeping the stack unchanged until commit finishes prevents the preview from disappearing
 * when Android ends the gesture, and lets a cancelled gesture retain the current page.
 */
@Composable
internal fun <T : Any> PredictivePageNavDisplay(
  backStack: List<T>,
  entryDecorators: List<NavEntryDecorator<T>>,
  transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform,
  popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform,
  onBack: () -> Unit,
  entryProvider: (T) -> NavEntry<T>,
  modifier: Modifier = Modifier,
) {
  val entries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
  val scenes = rememberSceneState(entries, remember { SinglePaneSceneStrategy() }, onBack)
  val scene = scenes.currentScene
  val transitionState = remember { SeekableTransitionState(scene) }
  val transition = rememberTransition(transitionState, label = "Page navigation")
  val scope = rememberCoroutineScope()
  var gesture by remember { mutableStateOf<PageBackGesture<T>?>(null) }
  var settlingJob by remember { mutableStateOf<Job?>(null) }

  // A deep link or another navigation action invalidates the gesture's captured destination.
  LaunchedEffect(scene) {
    settlingJob?.cancel()
    settlingJob = null
    gesture = null
    transitionState.animateTo(scene)
  }

  // Register before the pages: a dialog, keyboard or a WebView's own history gets first refusal.
  PredictiveBackHandler(enabled = backStack.size > 1) { events ->
    if (gesture != null) {
      events.collect { /* Consume repeated back events while commit/cancel is settling. */ }
      return@PredictiveBackHandler
    }
    val origin = scene
    val parent = scenes.previousScenes.lastOrNull() ?: return@PredictiveBackHandler
    var active: PageBackGesture<T>? = null
    try {
      events.collect { event ->
        if (active != null && gesture !== active) return@collect
        val motion = active ?: PageBackGesture(origin, event.touchY).also {
          active = it
          gesture = it
          // Seek into the hold portion of the exit transition to compose both real pages.
          // All visible motion is controlled below, independently of this holding timeline.
          transitionState.snapTo(origin)
          transitionState.seekTo(0.25f, parent)
        }
        motion.progress = event.progress.coerceIn(0f, 1f)
        motion.touchDeltaY = event.touchY - motion.startTouchY
        motion.fromRightEdge = event.swipeEdge == androidx.activity.BackEventCompat.EDGE_RIGHT
      }
      val motion = active
      if (motion == null) {
        // Hardware / three-button back has no predictive progress events.
        onBack()
      } else if (gesture === motion) {
        settlingJob = scope.launch {
          motion.release.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
          transitionState.snapTo(parent)
          onBack()
          // Keep the predictive transform until the new stack has reached composition.
          withFrameNanos { }
          if (gesture === motion) gesture = null
        }
      }
    } catch (_: CancellationException) {
      active?.takeIf { gesture === it }?.let { motion ->
        settlingJob = scope.launch {
          motion.recovery.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
          transitionState.snapTo(origin)
          if (gesture === motion) gesture = null
        }
      }
    }
  }

  Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    transition.AnimatedContent(
      modifier = Modifier.fillMaxSize(),
      contentKey = { it.key },
      transitionSpec = {
        val motion = gesture
        val isPop = targetState.previousEntries.size < initialState.previousEntries.size
        val transform = if (motion != null) {
          // The first half holds the outgoing page fully opaque. We never play the fade:
          // commit/cancel snap to their destination after the separate visible animation.
          EnterTransition.None togetherWith fadeOut(tween(500, delayMillis = 500))
        } else if (isPop) popTransitionSpec() else transitionSpec()
        ContentTransform(
          targetContentEnter = transform.targetContentEnter,
          initialContentExit = transform.initialContentExit,
          targetContentZIndex = targetState.previousEntries.size.toFloat(),
          sizeTransform = null,
        )
      },
    ) { displayedScene ->
      val motion = gesture
      val foreground = motion?.origin?.key == displayedScene.key
      val background = motion != null && !foreground
      val settled = transition.currentState == transition.targetState && motion == null
      val lifecycleOwner = rememberLifecycleOwner(
        maxLifecycle = when {
          background -> Lifecycle.State.CREATED
          settled -> Lifecycle.State.RESUMED
          else -> Lifecycle.State.STARTED
        },
      )
      CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycleOwner,
        LocalNavAnimatedContentScope provides this,
      ) {
        Box(
          Modifier.fillMaxSize()
            .graphicsLayer {
              scaleX = 1f
              scaleY = 1f
              translationX = 0f
              translationY = 0f
              clip = false
              if (foreground) {
                val drag = motion.progress * motion.recovery.value
                val release = motion.release.value
                val geometry = pageBackGeometry(
                  progress = drag,
                  touchDeltaY = motion.touchDeltaY * motion.recovery.value,
                  fromRightEdge = motion.fromRightEdge,
                  width = size.width,
                  height = size.height,
                  density = density,
                  release = release,
                )
                scaleX = geometry.scale
                scaleY = geometry.scale
                translationX = geometry.translationX
                translationY = geometry.translationY
                shape = RoundedCornerShape((28f * drag).dp)
                clip = drag > 0f
              }
            }
            .background(MaterialTheme.colorScheme.background)
            .drawWithContent {
              drawContent()
              if (background) {
                val dim = (0.32f - 0.20f * motion.progress) * (1f - motion.release.value)
                drawRect(Color.Black.copy(alpha = dim))
              }
            }
            .then(if (background) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
          displayedScene.content()
        }
      }
    }
    if (gesture != null) {
      // Previewed content must not accept taps while either page is moving.
      Box(Modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) awaitPointerEvent().changes.forEach { it.consume() }
        }
      })
    }
  }
}

private class PageBackGesture<T : Any>(
  val origin: Scene<T>,
  val startTouchY: Float,
) {
  var progress by mutableFloatStateOf(0f)
  var touchDeltaY by mutableFloatStateOf(0f)
  var fromRightEdge by mutableStateOf(false)
  val release = Animatable(0f)
  val recovery = Animatable(1f)
}

internal data class PageBackGeometry(
  val scale: Float,
  val translationX: Float,
  val translationY: Float,
)

/** Both edges follow the drag inward; a completed return always exits to the right. */
internal fun pageBackGeometry(
  progress: Float,
  touchDeltaY: Float,
  fromRightEdge: Boolean,
  width: Float,
  height: Float,
  density: Float,
  release: Float,
): PageBackGeometry {
  val drag = progress.coerceIn(0f, 1f)
  val finish = release.coerceIn(0f, 1f)
  val scale = 1f - 0.10f * drag
  val direction = if (fromRightEdge) -1f else 1f
  val dragX = minOf(32f * density, width * 0.06f) * drag * direction
  val maxY = minOf(24f * density, height * 0.03f)
  val dragY = (touchDeltaY * 0.12f).coerceIn(-maxY, maxY) * drag
  return PageBackGeometry(
    scale = scale,
    translationX = dragX + (width * 1.05f - dragX) * finish,
    translationY = dragY * (1f - finish),
  )
}
