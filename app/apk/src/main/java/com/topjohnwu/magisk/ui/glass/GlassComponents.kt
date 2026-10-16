package com.topjohnwu.magisk.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize

/**
 * New-MagiskUI · 仿 iOS Liquid Glass 手动实现（阶段 1：零依赖）
 *
 * 设计原则：
 *  - 不引入任何第三方库，纯 Compose 实现
 *  - API 31+ 使用真实 blur；低版本降级为半透明材质
 *  - 与 Material3 组件共存：M3 管结构，玻璃管材质
 */

/** 玻璃材质默认参数 */
object GlassDefaults {
    /** 玻璃层透明度（浅色 / 深色） */
    const val LightAlpha = 0.55f
    const val DarkAlpha = 0.38f

    /** 边框高光透明度 */
    const val BorderAlpha = 0.35f

    /** 顶部高光渐变透明度 */
    const val HighlightAlpha = 0.22f

    /** 模糊半径 */
    val BlurRadius: Dp = 18.dp

    /** 圆角 */
    val CornerRadius: Dp = 24.dp
}

/** 系统是否支持真实模糊（Android 12 / API 31+） */
private fun supportsBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * 玻璃材质修饰符：模糊 + 半透明底 + 高光边框 + 柔和阴影
 *
 * 用法：
 * ```
 * Box(
 *     Modifier
 *         .glassSurface()
 *         .padding(16.dp)
 * ) { ... }
 * ```
 *
 * @param cornerRadius 圆角半径
 * @param alphaOverride 手动指定玻璃透明度（不传则按主题自动）
 * @param blurRadius 模糊半径（低版本自动忽略）
 * @param elevated 是否带阴影
 */
@Composable
fun Modifier.glassSurface(
    cornerRadius: Dp = GlassDefaults.CornerRadius,
    alphaOverride: Float? = null,
    blurRadius: Dp = GlassDefaults.BlurRadius,
    elevated: Boolean = true,
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseAlpha = alphaOverride ?: if (isDark) GlassDefaults.DarkAlpha else GlassDefaults.LightAlpha

    val shape = RoundedCornerShape(cornerRadius)
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline

    // 玻璃底色：半透明 surface
    val glassColor = surface.copy(alpha = baseAlpha)

    var m: Modifier = this

    // 1) 阴影（营造悬浮感）
    if (elevated) {
        m = m.shadow(
            elevation = 8.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.12f),
            spotColor = Color.Black.copy(alpha = 0.18f),
        )
    }

    // 2) 裁剪 + 半透明底
    m = m
        .clip(shape)
        .background(glassColor)

    // 3) 真实模糊（仅 API 31+）
    if (supportsBlur()) {
        m = m.blur(blurRadius)
    }

    // 4) 顶部高光渐变 + 细边框
    m = m
        .background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = GlassDefaults.HighlightAlpha),
                0.5f to Color.Transparent,
                1f to Color.White.copy(alpha = GlassDefaults.HighlightAlpha * 0.35f),
            )
        )
        .border(
            width = 1.dp,
            color = outline.copy(alpha = GlassDefaults.BorderAlpha),
            shape = shape,
        )

    return m
}

/**
 * 玻璃卡片：替代普通 Card，视觉上为悬浮玻璃材质
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassDefaults.CornerRadius,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val glass = modifier.glassSurface(cornerRadius = cornerRadius)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = glass,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Card(
            modifier = glass,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(Modifier.padding(contentPadding)) { content() }
        }
    }
}

/**
 * 玻璃面板：一个带玻璃材质的 Box 容器，适合做背景承托层
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassDefaults.CornerRadius,
    alphaOverride: Float? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.glassSurface(
            cornerRadius = cornerRadius,
            alphaOverride = alphaOverride,
        ),
        content = content,
    )
}

/**
 * 玻璃高光叠加层：给已有内容盖一层折射高光，用于沉浸式背景
 */
@Composable
fun GlassOverlay(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(GlassDefaults.CornerRadius),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                    ),
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
    )
}

/** 颜色亮度（0=黑, 1=白），用于判断深浅主题 */
private fun Color.luminance(): Float =
    0.2126f * this.red + 0.7152f * this.green + 0.0722f * this.blue
