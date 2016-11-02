/**
 * @author wysaid
 * @mail admin@wysaid.org
 */

package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;
import com.github.guikunzhi.beautify.BasicBeautifyShaders;

public class BasicBeautyFilter extends GPUImageTwoPassTextureSamplingFilter {

    private float distanceNormalizationFactor = 1f;
    private float texelSpacingMultiplier = 1f;

    public BasicBeautyFilter(float distanceNormalizationFactor) {
        super(BasicBeautifyShaders.VERTEX_SHADER, BasicBeautifyShaders.FRAGMENT_SHADER,
                BasicBeautifyShaders.VERTEX_SHADER, BasicBeautifyShaders.FRAGMENT_SHADER);
        this.distanceNormalizationFactor = distanceNormalizationFactor;
        this.texelSpacingMultiplier = 4.0f;
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
        filter.setFloat(distanceNormalizationFactor, this.distanceNormalizationFactor);

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
        filter.setFloat(distanceNormalizationFactor, this.distanceNormalizationFactor);

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
        return texelSpacingMultiplier;
    }

    public float getHorizontalTexelOffsetRatio() {
        return texelSpacingMultiplier;
    }

    /**
     * A normalization factor for the distance between central color and sample color.
     */
    public void setDistanceNormalizationFactor(float distanceNormalizationFactor) {
        this.distanceNormalizationFactor = distanceNormalizationFactor;
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
        this.texelSpacingMultiplier = texelSpacingMultiplier;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                initTexelOffsets();
            }
        });
    }
}
