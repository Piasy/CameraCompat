/**
 * @author wysaid
 * @mail admin@wysaid.org
 */

package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;
import com.github.guikunzhi.beautify.BasicBeautifyShaders;

public class BasicBeautyFilter extends GPUImageTwoPassTextureSamplingFilter {

    private float mDistanceNormalizationFactor = 1f;
    private float mTexelSpacingMultiplier = 1f;

    public BasicBeautyFilter(float distanceNormalizationFactor) {
        super(BasicBeautifyShaders.VERTEX_SHADER, BasicBeautifyShaders.FRAGMENT_SHADER,
                BasicBeautifyShaders.VERTEX_SHADER, BasicBeautifyShaders.FRAGMENT_SHADER);
        this.mDistanceNormalizationFactor = distanceNormalizationFactor;
        this.mTexelSpacingMultiplier = 4.0f;
    }

    @Override
    public void onInit() {
        super.onInit();
        initTexelOffsets();
    }

    protected void initTexelOffsets() {
        float ratio = getHorizontalTexelOffsetRatio();
        GPUImageFilter filter = mFilters.get(0);
        int distanceNormalizationFactor = GLES20.glGetUniformLocation(filter.getProgram(),
                "distanceNormalizationFactor");
        filter.setFloat(distanceNormalizationFactor, this.mDistanceNormalizationFactor);

        int texelWidthOffsetLocation = GLES20.glGetUniformLocation(filter.getProgram(),
                "texelWidthOffset");
        int texelHeightOffsetLocation = GLES20.glGetUniformLocation(filter.getProgram(),
                "texelHeightOffset");
        filter.setFloat(texelWidthOffsetLocation, ratio / mOutputWidth);
        filter.setFloat(texelHeightOffsetLocation, 0);

        ratio = getVerticalTexelOffsetRatio();
        filter = mFilters.get(1);
        distanceNormalizationFactor = GLES20.glGetUniformLocation(filter.getProgram(),
                "distanceNormalizationFactor");
        filter.setFloat(distanceNormalizationFactor, this.mDistanceNormalizationFactor);

        texelWidthOffsetLocation = GLES20.glGetUniformLocation(filter.getProgram(),
                "texelWidthOffset");
        texelHeightOffsetLocation = GLES20.glGetUniformLocation(filter.getProgram(),
                "texelHeightOffset");

        filter.setFloat(texelWidthOffsetLocation, 0);
        filter.setFloat(texelHeightOffsetLocation, ratio / mOutputHeight);
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
        initTexelOffsets();
    }

    public float getVerticalTexelOffsetRatio() {
        return mTexelSpacingMultiplier;
    }

    public float getHorizontalTexelOffsetRatio() {
        return mTexelSpacingMultiplier;
    }

    /**
     * A normalization factor for the distance between central color and sample color.
     */
    public void setDistanceNormalizationFactor(float distanceNormalizationFactor) {
        this.mDistanceNormalizationFactor = distanceNormalizationFactor;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                initTexelOffsets();
            }
        });
    }

    /**
     * A scaling for the size of the applied blur, default of 4.0
     */
    public void setTexelSpacingMultiplier(float texelSpacingMultiplier) {
        this.mTexelSpacingMultiplier = texelSpacingMultiplier;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                initTexelOffsets();
            }
        });
    }
}
