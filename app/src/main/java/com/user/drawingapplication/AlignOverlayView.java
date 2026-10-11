package com.user.drawingapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

public class AlignOverlayView extends View {

    private static final int MESH = 16;

    private final float density = getResources().getDisplayMetrics().density;
    private final float handleRadius = 13f * density;
    private final float hitRadius = 40f * density;

    private final float[] handles = new float[8];
    private final float[] verts = new float[(MESH + 1) * (MESH + 1) * 2];
    private final float[] homography = new float[9];
    private final float[] sourceCorners = new float[8];
    private final Path quadPath = new Path();
    private final SparseArray<Integer> pointerGrab = new SparseArray<>();

    private final Paint meshPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint quadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handleFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handleStroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean active;
    private boolean initialized;
    private boolean vertsDirty = true;
    private Bitmap source;
    private float ghostAlpha = 0.3f;

    public AlignOverlayView(Context context) {
        this(context, null);
    }

    public AlignOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        quadPaint.setStyle(Paint.Style.STROKE);
        quadPaint.setStrokeWidth(2f * density);
        quadPaint.setColor(0xFFBC6FF1);
        quadPaint.setPathEffect(new DashPathEffect(new float[]{10f * density, 8f * density}, 0f));

        handleFill.setStyle(Paint.Style.FILL);
        handleFill.setColor(0xFFFFFFFF);
        handleStroke.setStyle(Paint.Style.STROKE);
        handleStroke.setStrokeWidth(3f * density);
        handleStroke.setColor(0xFFBC6FF1);
    }

    public void setActive(boolean active) {
        this.active = active;
        vertsDirty = true;
        setVisibility(active ? VISIBLE : GONE);
        invalidate();
    }

    public void setSource(Bitmap bitmap) {
        source = bitmap;
        vertsDirty = true;
        invalidate();
    }

    public void setGhostAlpha(float alpha) {
        ghostAlpha = alpha;
        invalidate();
    }

    public float[] getPoints() {
        return handles.clone();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!initialized && w > 0 && h > 0) {
            float dx = w * 0.12f;
            float dy = h * 0.16f;
            handles[0] = dx;
            handles[1] = dy;
            handles[2] = w - dx;
            handles[3] = dy;
            handles[4] = w - dx;
            handles[5] = h - dy;
            handles[6] = dx;
            handles[7] = h - dy;
            initialized = true;
            vertsDirty = true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!active || !initialized) {
            return;
        }

        if (source != null) {
            if (vertsDirty) {
                recomputeVerts();
            }
            meshPaint.setAlpha(Math.round(Math.max(0f, Math.min(1f, ghostAlpha)) * 255));
            canvas.drawBitmapMesh(source, MESH, MESH, verts, 0, null, 0, meshPaint);
        }

        quadPath.reset();
        quadPath.moveTo(handles[0], handles[1]);
        quadPath.lineTo(handles[2], handles[3]);
        quadPath.lineTo(handles[4], handles[5]);
        quadPath.lineTo(handles[6], handles[7]);
        quadPath.close();
        canvas.drawPath(quadPath, quadPaint);

        for (int i = 0; i < 4; i++) {
            canvas.drawCircle(handles[i * 2], handles[i * 2 + 1], handleRadius, handleFill);
            canvas.drawCircle(handles[i * 2], handles[i * 2 + 1], handleRadius, handleStroke);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!active || !initialized) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = event.getActionIndex();
                int handle = handleAt(event.getX(index), event.getY(index));
                if (handle < 0) {
                    return false;
                }
                pointerGrab.put(event.getPointerId(index), handle);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                boolean handled = false;
                for (int i = 0; i < event.getPointerCount(); i++) {
                    Integer handle = pointerGrab.get(event.getPointerId(i));
                    if (handle != null) {
                        handles[handle * 2] = clamp(event.getX(i), 0, getWidth());
                        handles[handle * 2 + 1] = clamp(event.getY(i), 0, getHeight());
                        vertsDirty = true;
                        invalidate();
                        handled = true;
                    }
                }
                return handled || pointerGrab.size() > 0;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                pointerGrab.remove(event.getPointerId(event.getActionIndex()));
                return true;
            case MotionEvent.ACTION_CANCEL:
                pointerGrab.clear();
                return true;
            default:
                return false;
        }
    }

    private int handleAt(float x, float y) {
        for (int i = 0; i < 4; i++) {
            float dx = x - handles[i * 2];
            float dy = y - handles[i * 2 + 1];
            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                return i;
            }
        }
        return -1;
    }

    private void recomputeVerts() {
        vertsDirty = false;
        if (source == null || !initialized) {
            return;
        }

        float bw = source.getWidth();
        float bh = source.getHeight();
        sourceCorners[0] = 0;
        sourceCorners[1] = 0;
        sourceCorners[2] = bw;
        sourceCorners[3] = 0;
        sourceCorners[4] = bw;
        sourceCorners[5] = bh;
        sourceCorners[6] = 0;
        sourceCorners[7] = bh;

        if (!computeHomography(sourceCorners, handles, homography)) {
            return;
        }

        int idx = 0;
        for (int j = 0; j <= MESH; j++) {
            float sy = bh * j / MESH;
            for (int i = 0; i <= MESH; i++) {
                float sx = bw * i / MESH;
                float w = homography[6] * sx + homography[7] * sy + 1f;
                verts[idx++] = (homography[0] * sx + homography[1] * sy + homography[2]) / w;
                verts[idx++] = (homography[3] * sx + homography[4] * sy + homography[5]) / w;
            }
        }
    }

    private static boolean computeHomography(float[] src, float[] dst, float[] h) {
        float[][] a = new float[8][9];
        for (int i = 0; i < 4; i++) {
            float x = src[i * 2];
            float y = src[i * 2 + 1];
            float u = dst[i * 2];
            float v = dst[i * 2 + 1];
            a[i * 2] = new float[]{x, y, 1, 0, 0, 0, -u * x, -u * y, u};
            a[i * 2 + 1] = new float[]{0, 0, 0, x, y, 1, -v * x, -v * y, v};
        }

        for (int col = 0; col < 8; col++) {
            int pivot = col;
            for (int row = col + 1; row < 8; row++) {
                if (Math.abs(a[row][col]) > Math.abs(a[pivot][col])) {
                    pivot = row;
                }
            }
            if (Math.abs(a[pivot][col]) < 1e-6f) {
                return false;
            }
            if (pivot != col) {
                float[] tmp = a[col];
                a[col] = a[pivot];
                a[pivot] = tmp;
            }
            float div = a[col][col];
            for (int c = col; c < 9; c++) {
                a[col][c] /= div;
            }
            for (int row = 0; row < 8; row++) {
                if (row == col) {
                    continue;
                }
                float factor = a[row][col];
                if (factor == 0f) {
                    continue;
                }
                for (int c = col; c < 9; c++) {
                    a[row][c] -= factor * a[col][c];
                }
            }
        }

        for (int i = 0; i < 8; i++) {
            h[i] = a[i][8];
        }
        h[8] = 1f;
        return true;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
