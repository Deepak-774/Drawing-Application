package com.user.drawingapplication;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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
            btnRemovePair.setOnClickListener(v -> confirmDelete((File) v.getTag()));

            View btnSharePair = row.findViewById(R.id.btnSharePair);
            btnSharePair.setTag(pair);
            btnSharePair.setOnClickListener(v -> shareDrawing((File) v.getTag()));

            android.widget.TextView tvDate = row.findViewById(R.id.tvDate);
            tvDate.setText(formatPairDate(pair));

            list.addView(row);
        }

        if (list.getChildCount() > 0) {
            findViewById(R.id.emptyState).setVisibility(View.GONE);
            findViewById(R.id.scrollGallery).setVisibility(View.VISIBLE);
        }
    }

    private void shareDrawing(File pairDir) {
        ArrayList<File> files = new ArrayList<>();
        File result = new File(pairDir, "result.jpg");
        File reference = new File(pairDir, "reference.jpg");
        if (result.exists()) {
            files.add(result);
        }
        if (reference.exists()) {
            files.add(reference);
        }

        if (files.isEmpty()) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Uri> uris = new ArrayList<>();
        ClipData clipData = null;
        for (File file : files) {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            uris.add(uri);
            if (clipData == null) {
                clipData = ClipData.newRawUri(null, uri);
            } else {
                clipData.addItem(new ClipData.Item(uri));
            }
        }

        Intent share;
        if (uris.size() == 1) {
            share = new Intent(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            share = new Intent(Intent.ACTION_SEND_MULTIPLE);
            share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        share.setType("image/jpeg");
        share.setClipData(clipData);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.putExtra(Intent.EXTRA_TEXT,
                "Check out my drawing made with " + getString(R.string.app_name) + "!");

        Intent chooser = Intent.createChooser(share, "Share drawing");
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(chooser);
    }

    private String formatPairDate(File pairDir) {
        try {
            long timestamp = Long.parseLong(pairDir.getName());
            return new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
                    .format(new Date(timestamp));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private void confirmDelete(File pairDir) {
        new AlertDialog.Builder(this)
                .setTitle("Delete drawing?")
                .setMessage("This will remove the reference and your drawing permanently from the app.")
                .setPositiveButton("Delete", (dialog, which) -> deletePairAndRefresh(pairDir))
                .setNegativeButton("Cancel", null)
                .show();
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
