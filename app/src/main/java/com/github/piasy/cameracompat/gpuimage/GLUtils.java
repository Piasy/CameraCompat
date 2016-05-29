package com.github.piasy.cameracompat.gpuimage;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import com.github.piasy.cameracompat.utils.Size;
import java.nio.IntBuffer;
import jp.co.cyberagent.android.gpuimage.OpenGlUtils;

/**
 * Created by Piasy{github.com/Piasy} on 5/24/16.
 */
public class GLUtils {
    public static final int NO_TEXTURE = -1;

    public static int loadTexture(final Bitmap img, final int usedTexId) {
        return OpenGlUtils.loadTexture(img, usedTexId, true);
    }

    public static int loadTexture(final IntBuffer data, final Size size, final int usedTexId) {
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, size.getWidth(),
                    size.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, size.getWidth(), size.getHeight(),
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
            textures[0] = usedTexId;
        }
        return textures[0];
    }

    public static int loadTextureAsBitmap(final IntBuffer data, final Size size,
            final int usedTexId) {
        Bitmap bitmap = Bitmap.createBitmap(data.array(), size.getWidth(), size.getHeight(),
                Bitmap.Config.ARGB_8888);
        return loadTexture(bitmap, usedTexId);
    }

    public static int loadShader(final String strSource, final int iType) {
        return OpenGlUtils.loadShader(strSource, iType);
    }

    public static int loadProgram(final String strVSource, final String strFSource) {
        return OpenGlUtils.loadProgram(strVSource, strFSource);
    }

    public static float rnd(final float min, final float max) {
        return OpenGlUtils.rnd(min, max);
    }
}
