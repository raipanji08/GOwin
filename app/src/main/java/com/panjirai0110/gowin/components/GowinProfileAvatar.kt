package com.panjirai0110.gowin.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.panjirai0110.shared.model.UserGender
import com.panjirai0110.shared.ui.theme.GowinBlue

/** A local, non-uploadable avatar used for the selected profile gender. */
@androidx.compose.runtime.Composable
fun GowinProfileAvatar(
    gender: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFEAF2FF))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val skin = Color(0xFFF1BD8F)
            val hair = if (gender == UserGender.Female) {
                Color(0xFF4E342E)
            } else {
                Color(0xFF25364D)
            }
            val shirt = if (gender == UserGender.Female) {
                Color(0xFFE3658C)
            } else {
                GowinBlue
            }

            if (gender == UserGender.Female) {
                drawCircle(
                    color = hair,
                    radius = width * 0.275f,
                    center = Offset(centerX, height * 0.42f)
                )
            }

            drawRoundRect(
                color = shirt,
                topLeft = Offset(width * 0.12f, height * 0.72f),
                size = androidx.compose.ui.geometry.Size(width * 0.76f, height * 0.38f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.25f)
            )
            drawCircle(
                color = skin,
                radius = width * 0.205f,
                center = Offset(centerX, height * 0.40f)
            )

            if (gender == UserGender.Female) {
                drawRoundRect(
                    color = hair,
                    topLeft = Offset(width * 0.24f, height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(width * 0.52f, height * 0.13f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.1f)
                )
                drawRoundRect(
                    color = hair,
                    topLeft = Offset(width * 0.22f, height * 0.34f),
                    size = androidx.compose.ui.geometry.Size(width * 0.09f, height * 0.25f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.06f)
                )
                drawRoundRect(
                    color = hair,
                    topLeft = Offset(width * 0.69f, height * 0.34f),
                    size = androidx.compose.ui.geometry.Size(width * 0.09f, height * 0.25f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.06f)
                )
            } else {
                val hairPath = Path().apply {
                    moveTo(width * 0.29f, height * 0.34f)
                    quadraticBezierTo(
                        centerX,
                        height * 0.16f,
                        width * 0.73f,
                        height * 0.34f
                    )
                    lineTo(width * 0.69f, height * 0.40f)
                    quadraticBezierTo(
                        centerX,
                        height * 0.30f,
                        width * 0.33f,
                        height * 0.41f
                    )
                    close()
                }
                drawPath(color = hair, path = hairPath)
            }

            val eyeY = height * 0.41f
            drawCircle(Color(0xFF25364D), radius = width * 0.018f, center = Offset(width * 0.43f, eyeY))
            drawCircle(Color(0xFF25364D), radius = width * 0.018f, center = Offset(width * 0.57f, eyeY))
            drawArc(
                color = Color(0xFFB56A5D),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(width * 0.42f, height * 0.46f),
                size = androidx.compose.ui.geometry.Size(width * 0.16f, height * 0.10f),
                style = Stroke(width * 0.014f)
            )
        }
    }
}
