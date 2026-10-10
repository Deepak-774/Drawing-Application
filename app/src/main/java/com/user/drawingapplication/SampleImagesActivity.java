package com.user.drawingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SampleImagesActivity extends AppCompatActivity {

    private static final int[] SAMPLE_IMAGES = {
            R.drawable.sample_1,
            R.drawable.sample_2,
            R.drawable.sample_3,
            R.drawable.sample_4,
            R.drawable.sample_5,
            R.drawable.sample_6,
            R.drawable.sample_7,
            R.drawable.sample_8,
            R.drawable.sample_9
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sample_images);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), bars.bottom);
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        GridLayout grid = findViewById(R.id.sampleGrid);
        for (int sampleId : SAMPLE_IMAGES) {
            grid.addView(createCell(sampleId));
        }
    }

    private ImageView createCell(int sampleId) {
        ImageView cell = new ImageView(this);
        cell.setImageResource(sampleId);
        cell.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cell.setBackgroundColor(0xFF2A2A2A);
        cell.setPadding(dp(4), dp(4), dp(4), dp(4));
        cell.setContentDescription("Sample image");

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(180);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        params.setGravity(Gravity.FILL);
        cell.setLayoutParams(params);

        cell.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraTraceActivity.class);
            intent.putExtra("sampleResId", sampleId);
            startActivity(intent);
        });

        return cell;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
