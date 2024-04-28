package com.example.imageemojiconvertor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.widget.Toast
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face

object Emojifier {

    private const val SMILING_PROB_THRESHOLD = 0.15
    private const val EYE_OPEN_PROB_THRESHOLD = 0.5
    private const val EMOJI_SCALE_FACTOR = 0.9f

    fun detectFacesandOverlayEmoji(context: Context, picture: Bitmap): Bitmap {
        val detector = com.google.android.gms.vision.face.FaceDetector.Builder(context)
            .setTrackingEnabled(false)
            .setClassificationType(com.google.android.gms.vision.face.FaceDetector.ALL_CLASSIFICATIONS)
            .build()

        val frame = Frame.Builder().setBitmap(picture).build()
        val faces = detector.detect(frame)

        var resultBitmap = picture

        if (faces.size() == 0) {
            Toast.makeText(context, R.string.no_faces_message, Toast.LENGTH_SHORT).show()
        } else {
            for (i in 0 until faces.size()) {
                val face = faces.valueAt(i)
                val emojiBitmap = when (whichEmoji(face)) {
                    Emoji.SMILE -> BitmapFactory.decodeResource(context.resources, R.drawable.smile)
                    Emoji.FROWN -> BitmapFactory.decodeResource(context.resources, R.drawable.frown)
                    Emoji.LEFT_WINK -> BitmapFactory.decodeResource(context.resources, R.drawable.leftwink)
                    Emoji.RIGHT_WINK -> BitmapFactory.decodeResource(context.resources, R.drawable.rightwink)
                    Emoji.LEFT_WINK_FROWN -> BitmapFactory.decodeResource(context.resources, R.drawable.leftwinkfrown)
                    Emoji.RIGHT_WINK_FROWN -> BitmapFactory.decodeResource(context.resources, R.drawable.rightwinkfrown)
                    Emoji.CLOSED_EYE_SMILE -> BitmapFactory.decodeResource(context.resources, R.drawable.closed_smile)
                    Emoji.CLOSED_EYE_FROWN -> BitmapFactory.decodeResource(context.resources, R.drawable.closed_frown)
//                    Emoji.ANGRY -> BitmapFactory.decodeResource(context.resources, R.drawable.angry)
//                    Emoji.LAUGHING -> BitmapFactory.decodeResource(context.resources, R.drawable.laughing)
//                    Emoji.SURPRISED -> BitmapFactory.decodeResource(context.resources, R.drawable.surprised)
                    else -> {
                        Toast.makeText(context, R.string.no_emoji, Toast.LENGTH_SHORT).show()
                        null
                    }
                }
                resultBitmap = addBitmapToFace(resultBitmap, emojiBitmap, face)
            }
        }
        detector.release()
        return resultBitmap
    }

    private fun whichEmoji(face: Face): Emoji {
        val smiling = face.isSmilingProbability > SMILING_PROB_THRESHOLD
        val leftEyeClosed = face.isLeftEyeOpenProbability < EYE_OPEN_PROB_THRESHOLD
        val rightEyeClosed = face.isRightEyeOpenProbability < EYE_OPEN_PROB_THRESHOLD

        return if (smiling) {
            when {
                leftEyeClosed && !rightEyeClosed -> Emoji.LEFT_WINK
                rightEyeClosed && !leftEyeClosed -> Emoji.RIGHT_WINK
                leftEyeClosed -> Emoji.CLOSED_EYE_SMILE
                else -> Emoji.SMILE
            }
        } else {
            when {
                leftEyeClosed && !rightEyeClosed -> Emoji.LEFT_WINK_FROWN
                rightEyeClosed && !leftEyeClosed -> Emoji.RIGHT_WINK_FROWN
                leftEyeClosed -> Emoji.CLOSED_EYE_FROWN
                else -> Emoji.FROWN
            }
        }
    }

    private fun addBitmapToFace(backgroundBitmap: Bitmap, emojiBitmap: Bitmap?, face: Face): Bitmap {
        var resultBitmap = Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, backgroundBitmap.config)

        val scaleFactor = EMOJI_SCALE_FACTOR
        val newEmojiWidth = (face.width * scaleFactor).toInt()
        val newEmojiHeight = (emojiBitmap!!.height * newEmojiWidth / emojiBitmap.width * scaleFactor).toInt()

        val emojiPositionX = (face.position.x + face.width / 2) - emojiBitmap.width / 2
        val emojiPositionY = (face.position.y + face.height / 2) - emojiBitmap.height / 3

        resultBitmap = Bitmap.createScaledBitmap(resultBitmap, newEmojiWidth, newEmojiHeight, false)

        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null)
        return resultBitmap
    }

    private enum class Emoji {
        SMILE,
        FROWN,
        LEFT_WINK,
        RIGHT_WINK,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_SMILE,
        CLOSED_EYE_FROWN
//        ANGRY,
//        LAUGHING,
//        SURPRISED
    }
}
