package com.abrantix.roundedvideo;

import android.graphics.Point;
import android.graphics.RectF;
import android.support.annotation.NonNull;

/**
 * Created by fabrantes on 03/04/15.
 */
public class GLRoundedGeometry {

    // The key points of the geometry
    private float[] mLeftTop = new float[2];
    private float[] mLeftBottom = new float[2];
    private float[] mTopLeft = new float[2];
    private float[] mTopRight = new float[2];
    private float[] mRightTop = new float[2];
    private float[] mRightBottom = new float[2];
    private float[] mBottomLeft = new float[2];
    private float[] mBottomRight = new float[2];

    private float[] mInnerTopLeft = new float[2];
    private float[] mInnerTopRight = new float[2];
    private float[] mInnerBottomRight = new float[2];
    private float[] mInnerBottomLeft = new float[2];

    private float[] mTopLeftRadius = new float[2];
    private float[] mTopRightRadius = new float[2];
    private float[] mBottomRightRadius = new float[2];
    private float[] mBottomLeftRadius = new float[2];

    public float[] generateVertexData(@NonNull RectF radii, @NonNull RectF viewPortGLBounds,
                                      @NonNull Point viewPortPxSize) {
        return generateVertexData(radii, viewPortGLBounds, viewPortPxSize, 0f);
    }

    public float[] generateVertexData(@NonNull RectF radii, @NonNull RectF viewPortGLBounds,
                                      @NonNull Point viewPortPxSize, float z) {
        final float x0 = viewPortGLBounds.left;
        final float x1 = viewPortGLBounds.right;
        final float y0 = viewPortGLBounds.bottom;
        final float y1 = viewPortGLBounds.top;

        final float leftTopRadius = radii.left;
        final float rightTopRadius = radii.top;
        final float rightBottomRadius = radii.right;
        final float leftBottomRadius = radii.bottom;

        mTopLeftRadius[0] = leftTopRadius / viewPortPxSize.x * viewPortGLBounds.width();
        mTopLeftRadius[1] = leftTopRadius / viewPortPxSize.y * -viewPortGLBounds.height();
        mTopRightRadius[0] = rightTopRadius / viewPortPxSize.x * viewPortGLBounds.width();
        mTopRightRadius[1] = rightTopRadius / viewPortPxSize.y * -viewPortGLBounds.height();
        mBottomRightRadius[0] = rightBottomRadius / viewPortPxSize.x * viewPortGLBounds.width();
        mBottomRightRadius[1] = rightBottomRadius / viewPortPxSize.y * -viewPortGLBounds.height();
        mBottomLeftRadius[0] = leftBottomRadius / viewPortPxSize.x * viewPortGLBounds.width();
        mBottomLeftRadius[1] = leftBottomRadius / viewPortPxSize.y * -viewPortGLBounds.height();

        mLeftTop[0] = x0;
        mLeftTop[1] = y1 - mTopLeftRadius[1];
        mLeftBottom[0] = x0;
        mLeftBottom[1] = y0 + mBottomLeftRadius[1];
        mTopLeft[0] = x0 + mTopLeftRadius[0];
        mTopLeft[1] = y1;
        mTopRight[0] = x1 - mTopRightRadius[0];
        mTopRight[1] = y1;
        mRightTop[0] = x1;
        mRightTop[1] = y1 - mTopRightRadius[1];
        mRightBottom[0] = x1;
        mRightBottom[1] = y0 + mBottomRightRadius[1];
        mBottomLeft[0] = x0 + mBottomLeftRadius[0];
        mBottomLeft[1] = y0;
        mBottomRight[0] = x1 - mBottomRightRadius[0];
        mBottomRight[1] = y0;

        mInnerTopLeft[0] = mTopLeft[0];
        mInnerTopLeft[1] = mLeftTop[1];
        mInnerTopRight[0] = mTopRight[0];
        mInnerTopRight[1] = mRightTop[1];
        mInnerBottomLeft[0] = mBottomLeft[0];
        mInnerBottomLeft[1] = mLeftBottom[1];
        mInnerBottomRight[0] = mBottomRight[0];
        mInnerBottomRight[1] = mRightBottom[1];

        // Each vertex has 5 floats (xyz + uv)
        // 5 squares (each has 4 vertices)
        // 4 rounded corners (each has X triangles, each triangle has 3 vertices)
        final int trianglesPerCorner = 5;
        final int floatsPerTriangle = 3 * 5;
        final int floatsPerSquare = 4 * 5;
        final int vertexCount = 5 * floatsPerSquare + 4 * trianglesPerCorner * floatsPerTriangle;
        final float[] vertices = new float[vertexCount];
        int offset = 0;

        // Inner center rect
        addRect(vertices, offset, new float[][]{
                        mInnerTopLeft, mInnerTopRight, mInnerBottomLeft, mInnerBottomRight},
                viewPortGLBounds, z);
        offset += floatsPerSquare;

        // Left rect
        addRect(vertices, offset, new float[][]{
                        mLeftTop, mInnerTopLeft, mLeftBottom, mInnerBottomLeft},
                viewPortGLBounds, z);
        offset += floatsPerSquare;

        // Right rect
        addRect(vertices, offset, new float[][]{
                        mInnerTopRight, mRightTop, mInnerBottomRight, mRightBottom},
                viewPortGLBounds, z);
        offset += floatsPerSquare;

        // Top rect
        addRect(vertices, offset, new float[][]{
                        mTopLeft, mInnerTopLeft, mTopRight, mInnerTopRight},
                viewPortGLBounds, z);
        offset += floatsPerSquare;

        // Bottom rect
        addRect(vertices, offset, new float[][]{
                        mInnerBottomLeft, mBottomLeft, mInnerBottomRight, mBottomRight},
                viewPortGLBounds, z);
        offset += floatsPerSquare;

        // These assume uniform corners (i.e. same radius on both axis)
        // Top left corner
        addRoundedCorner(vertices, offset, mInnerTopLeft, mTopLeftRadius, (float) Math.PI,
                (float) (Math.PI / 2.0), trianglesPerCorner, viewPortGLBounds, z);
        offset += trianglesPerCorner * floatsPerTriangle;

        // Top right corner
        addRoundedCorner(vertices, offset, mInnerTopRight, mTopRightRadius,
                (float) (Math.PI / 2), 0f, trianglesPerCorner, viewPortGLBounds, z);
        offset += trianglesPerCorner * floatsPerTriangle;

        // Bottom right corner
        addRoundedCorner(vertices, offset, mInnerBottomRight,
                mBottomRightRadius, (float) (Math.PI * 3.0 / 2.0), (float) Math.PI * 2,
                trianglesPerCorner, viewPortGLBounds, z);
        offset += trianglesPerCorner * floatsPerTriangle;

        // Bottom left corner
        addRoundedCorner(vertices, offset, mInnerBottomLeft,
                mBottomLeftRadius, (float) Math.PI, (float) (Math.PI * 3.0 / 2.0),
                trianglesPerCorner, viewPortGLBounds, z);

        return vertices;
    }

    /**
     * Adds the vertices of a rectangle defined by 4 corner points. The array of vertices passed
     * in must have the required length to add the geometry points (5 floats for each vertex). Also
     * the coordinates of the rect corners should already be in the view port space.
     *
     * @param vertices the array in which vertex data will be inserted.
     * @param offset the offset in the array where to add the vertex data.
     * @param rectPoints an array of corner points defining the rectangle. index 0 is the x
     *                   coordinate and index 1 the y coordinate.
     * @param viewPort the bounds of the current GL viewport, this is used to calculate the texture
     *                 mapping.
     * @param z the z coordinate.
     */
    private void addRect(float[] vertices, int offset, float[][] rectPoints,
                         @NonNull RectF viewPort, float z) {
        int rectPointIdx = 0;
        for (final float[] rectPoint : rectPoints) {
             // 5 values [xyzuv] per vertex
            final int currentOffset = offset + rectPointIdx * 5;

            // XYZ (vertex space coordinates
            vertices[currentOffset + 0] = rectPoint[0];
            vertices[currentOffset + 1] = rectPoint[1];
            vertices[currentOffset + 2] = z;

            // UV (texture mapping)
            vertices[currentOffset + 3] = (rectPoint[0] - viewPort.left) / viewPort.width();
            vertices[currentOffset + 4] = (rectPoint[1] - viewPort.bottom) / -viewPort.height();

            rectPointIdx++;
        }
    }

    /**
     * Adds the vertices of a number of triangles to form a rounded corner. The triangles start at
     * some center point and will sweep from a given initial angle up to a final one. The size of
     * the triangles is defined by the radius.
     *
     * The array of vertices passed in must have the required length to add the geometry points
     * (5 floats for each vertex). Also the coordinates of the rect corners should already be in
     * the view port space.
     *
     * @param vertices the array in which vertex data will be inserted.
     * @param offset the offset in the array where to add the vertex data.
     * @param center the center point where all triangles will start.
     * @param radius the desired radius in the x and y axis, in viewport dimensions.
     * @param rads0 the initial angle.
     * @param rads1 the final angle.
     * @param triangles the amount of triangles to create.
     * @param viewPort the bounds of the current GL viewport, this is used to calculate the texture
     *                 mapping.
     * @param z the z coordinate.
     */
    private void addRoundedCorner(@NonNull float[] vertices,
                                  int offset,
                                  @NonNull float[] center,
                                  float[] radius,
                                  float rads0,
                                  float rads1,
                                  int triangles,
                                  @NonNull RectF viewPort,
                                  float z) {
        for (int i = 0; i < triangles; i++) {
            final int currentOffset = offset + i * 15 /* each triangle is 3 * xyzuv */;
            final float rads = rads0 + (rads1 - rads0) * (i / (float) triangles);
            final float radsNext = rads0 + (rads1 - rads0) * ((i + 1) / (float) triangles);

            // XYZUV - center point
            vertices[currentOffset + 0] = center[0];
            vertices[currentOffset + 1] = center[1];
            vertices[currentOffset + 2] = z;
            vertices[currentOffset + 3] =
                    (vertices[currentOffset + 0] - viewPort.left) / viewPort.width();
            vertices[currentOffset + 4] =
                    (vertices[currentOffset + 1] - viewPort.bottom) / -viewPort.height();

            // XYZUV - triangle edge 1
            vertices[currentOffset + 5] = center[0] + radius[0] * (float) Math.cos(rads);
            vertices[currentOffset + 6] = center[1] + radius[1] * (float) Math.sin(rads);
            vertices[currentOffset + 7] = z;
            vertices[currentOffset + 8] =
                    (vertices[currentOffset + 5] - viewPort.left) / viewPort.width();
            vertices[currentOffset + 9] =
                    (vertices[currentOffset + 6] - viewPort.bottom) / -viewPort.height();

            // XYZUV - triangle edge 2
            vertices[currentOffset + 10] = center[0] + radius[0] * (float) Math.cos(radsNext);
            vertices[currentOffset + 11] = center[1] + radius[1] * (float) Math.sin(radsNext);
            vertices[currentOffset + 12] = z;
            vertices[currentOffset + 13] =
                    (vertices[currentOffset + 10] - viewPort.left) / viewPort.width();
            vertices[currentOffset + 14] =
                    (vertices[currentOffset + 11] - viewPort.bottom) / -viewPort.height();
        }
    }
}
