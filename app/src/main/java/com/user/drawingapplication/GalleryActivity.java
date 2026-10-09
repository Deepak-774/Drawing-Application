package com.user.drawingapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

public class GalleryActivity extends AppCompatActivity {

    private static final int MAX_IMAGE_DIMENSION = 1080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gallery);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadGallery();
    }

    private void loadGallery() {
        LinearLayout list = findViewById(R.id.galleryList);
        list.removeAllViews();
        findViewById(R.id.scrollGallery).setVisibility(View.GONE);
        findViewById(R.id.emptyState).setVisibility(View.VISIBLE);

        File drawingsDir = new File(getFilesDir(), "drawings");
        File[] pairs = drawingsDir.listFiles(File::isDirectory);

        if (pairs == null || pairs.length == 0) {
            return;
        }

        java.util.Arrays.sort(pairs, (a, b) -> b.getName().compareTo(a.getName()));

        LayoutInflater inflater = LayoutInflater.from(this);

        for (File pair : pairs) {
            File reference = new File(pair, "reference.jpg");
            File result = new File(pair, "result.jpg");
            if (!reference.exists() && !result.exists()) {
                continue;
            }

            View row = inflater.inflate(R.layout.item_gallery_pair, list, false);

            View sideReference = row.findViewById(R.id.sideReference);
            View sideDrawing = row.findViewById(R.id.sideDrawing);

            if (reference.exists()) {
                ((ImageView) row.findViewById(R.id.imgReference))
                        .setImageBitmap(decodeSampled(reference));
            } else {
                sideReference.setVisibility(View.GONE);
            }

            if (result.exists()) {
                ((ImageView) row.findViewById(R.id.imgResult))
                        .setImageBitmap(decodeSampled(result));
            } else {
                sideDrawing.setVisibility(View.GONE);
            }

            View btnRemovePair = row.findViewById(R.id.btnRemovePair);
            btnRemovePair.setTag(pair);
            btnRemovePair.setOnClickListener(v -> deletePairAndRefresh((File) v.getTag()));

            list.addView(row);
        }

        if (list.getChildCount() > 0) {
            findViewById(R.id.emptyState).setVisibility(View.GONE);
            findViewById(R.id.scrollGallery).setVisibility(View.VISIBLE);
        }
    }

    private void deletePairAndRefresh(File pairDir) {
        if (pairDir != null && pairDir.isDirectory()) {
            File reference = new File(pairDir, "reference.jpg");
            File result = new File(pairDir, "result.jpg");
            if (reference.exists()) {
                reference.delete();
            }
            if (result.exists()) {
                result.delete();
            }
            String[] remaining = pairDir.list();
            if (remaining == null || remaining.length == 0) {
                pairDir.delete();
            }
        }
        loadGallery();
    }

    private Bitmap decodeSampled(File file) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);

        int sampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_IMAGE_DIMENSION) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
}
