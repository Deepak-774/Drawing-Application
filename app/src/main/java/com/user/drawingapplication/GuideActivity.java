package com.user.drawingapplication;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GuideActivity extends AppCompatActivity {

    private static final String[][] SECTIONS = {
            {
                    "Tracing an image",
                    "Tap \"Select new Image for Tracing\" and pick any photo from your phone, "
                            + "or open \"Sample Images\" to try a built-in design.\n\n"
                            + "Point your camera at your drawing book — the photo appears as a "
                            + "faint ghost over the live camera view.\n\n"
                            + "Drag with one finger to move the ghost, pinch with two fingers "
                            + "to resize it."
            },
            {
                    "Camera controls",
                    "Opacity slider — control how visible the ghost image is. "
                            + "Slide it down for a subtle guide, up for a strong one.\n\n"
                            + "Line art button (top-left) — converts the photo into simple "
                            + "black outlines, much easier to trace.\n\n"
                            + "Flashlight button (top-right) — brightens your paper in dim light.\n\n"
                            + "Change image button (bottom-left) — swap the reference without "
                            + "leaving the camera."
            },
            {
                    "Saving your work",
                    "Tap \"Save to Gallery\" — the screen flashes like a camera, the ghost "
                            + "hides for a clean capture, and your drawing is saved together "
                            + "with the reference image.\n\n"
                            + "You are taken to the gallery right after saving."
            },
            {
                    "My Gallery",
                    "Every saved drawing shows the date it was made.\n\n"
                            + "Share button — sends both the reference and your drawing with "
                            + "the message \"Check out my drawing made with Sketch!\".\n\n"
                            + "Cross button — deletes a drawing permanently after asking "
                            + "for confirmation."
            },
            {
                    "Tips",
                    "Keep the phone steady — rest it against books or a small stand.\n\n"
                            + "Use line art mode with a high opacity for the clearest outlines.\n\n"
                            + "Good lighting (or the flashlight) makes tracing much easier."
            }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guide);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), bars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        LinearLayout list = findViewById(R.id.guideList);
        for (String[] section : SECTIONS) {
            list.addView(createSectionCard(section[0], section[1]));
        }
    }

    private LinearLayout createSectionCard(String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.menu_item_bg);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(20), 0, dp(20), dp(12));
        card.setLayoutParams(params);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(17f);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(titleView);

        TextView bodyView = new TextView(this);
        bodyView.setText(body);
        bodyView.setTextColor(0xFFBBBBBB);
        bodyView.setTextSize(14f);
        bodyView.setLineSpacing(dp(4), 1f);

        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(8);
        bodyView.setLayoutParams(bodyParams);
        card.addView(bodyView);

        return card;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
