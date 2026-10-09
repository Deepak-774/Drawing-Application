package com.user.drawingapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class CameraTraceActivity extends AppCompatActivity {

    private static final float DEFAULT_OPACITY = 0.3f;

    private PreviewView previewView;
    private ImageView ghostImage;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isScaling;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to trace", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadGhostImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_trace);

        previewView = findViewById(R.id.cameraPreview);
        ghostImage = findViewById(R.id.ghostImage);

        applyInsets();
        setupGhostTouch();
        setupControls();

        Uri imageUri = getIntent().getData();
        if (imageUri != null) {
            loadGhostImage(imageUri);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void applyInsets() {
        float density = getResources().getDisplayMetrics().density;
        int pad12 = (int) (12 * density);
        int pad14 = (int) (14 * density);

        TextView hint = findViewById(R.id.traceHint);
        ViewCompat.setOnApplyWindowInsetsListener(hint, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top + pad12, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        android.view.View controlBar = findViewById(R.id.controlBar);
        ViewCompat.setOnApplyWindowInsetsListener(controlBar, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), pad14 + bars.bottom);
            return insets;
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Unable to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupControls() {
        ghostImage.setAlpha(DEFAULT_OPACITY);

        SeekBar opacitySeekBar = findViewById(R.id.opacitySeekBar);
        opacitySeekBar.setProgress((int) (DEFAULT_OPACITY * 100));
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ghostImage.setAlpha(progress / 100f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        findViewById(R.id.changeImageBtn).setOnClickListener(v -> imagePicker.launch("image/*"));
    }

    private void loadGhostImage(Uri uri) {
        Bitmap bitmap = decodeScaledBitmap(uri);
        if (bitmap != null) {
            ghostImage.setImageBitmap(bitmap);
            ghostImage.setScaleX(1f);
            ghostImage.setScaleY(1f);
            ghostImage.setTranslationX(0f);
            ghostImage.setTranslationY(0f);
        } else {
            Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeScaledBitmap(Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }

            int sampleSize = 1;
            while (bounds.outWidth / (sampleSize * 2) >= 1024
                    && bounds.outHeight / (sampleSize * 2) >= 1024) {
                sampleSize *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, options);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private void setupGhostTouch() {
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(@NonNull ScaleGestureDetector detector) {
                        float factor = detector.getScaleFactor();
                        ghostImage.setScaleX(ghostImage.getScaleX() * factor);
                        ghostImage.setScaleY(ghostImage.getScaleY() * factor);
                        return true;
                    }

                    @Override
                    public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                        isScaling = true;
                        return true;
                    }

                    @Override
                    public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
                        isScaling = false;
                    }
                });

        ghostImage.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!isScaling && event.getPointerCount() == 1) {
                        float dx = event.getRawX() - lastTouchX;
                        float dy = event.getRawY() - lastTouchY;
                        ghostImage.setTranslationX(ghostImage.getTranslationX() + dx);
                        ghostImage.setTranslationY(ghostImage.getTranslationY() + dy);
                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                    }
                    break;
            }
            return true;
        });
    }
}
