package com.user.drawingapplication;

import android.animation.ValueAnimator;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GuideActivity extends AppCompatActivity {

    private static final int[] SECTION_ICONS = {
            R.drawable.ic_image,
            R.drawable.ic_flashlight_on,
            R.drawable.ic_save,
            R.drawable.ic_photo_library,
            R.drawable.ic_lightbulb
    };

    private static final String[] SECTION_TITLES = {
            "Tracing an image",
            "Camera controls",
            "Saving your work",
            "My Gallery",
            "Tips"
    };

    private static final String[] SECTION_BODIES = {
            "Tap \"Select new Image for Tracing\" and pick any photo from your phone, "
                    + "or open \"Sample Images\" to try a built-in design.\n\n"
                    + "Point your camera at your drawing book — the photo appears as a "
                    + "faint ghost over the live camera view.\n\n"
                    + "Drag with one finger to move the ghost, pinch with two fingers "
                    + "to resize it, and double-tap it to reset back to the center.",

            "Back button (top-left) — return to the menu.\n\n"
                    + "Flashlight (top-right) — brightens your paper in dim light.\n\n"
                    + "Bottom bar — change image, opacity slider, line art toggle "
                    + "(turns the photo into simple black outlines), and the purple "
                    + "save button.",

            "Tap the purple save button in the bottom bar — the screen flashes like "
                    + "a camera, the ghost hides for a clean capture, and your drawing "
                    + "is saved together with the reference image.\n\n"
                    + "You are taken to the gallery right after saving.",

            "Every saved drawing shows the date it was made.\n\n"
                    + "Share button — sends both the reference and your drawing with "
                    + "the message \"Check out my drawing made with Sketch!\".\n\n"
                    + "Cross button — deletes a drawing permanently after asking "
                    + "for confirmation.",

            "Keep the phone steady — rest it against books or a small stand.\n\n"
                    + "Use line art mode with a high opacity for the clearest outlines.\n\n"
                    + "Good lighting (or the flashlight) makes tracing much easier."
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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        LinearLayout list = findViewById(R.id.guideList);
        for (int i = 0; i < SECTION_TITLES.length; i++) {
            list.addView(createSection(i, i == 0));
        }
    }

    private LinearLayout createSection(int index, boolean expanded) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.menu_item_bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(dp(20), 0, dp(20), dp(10));
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(18), dp(16));

        ImageView icon = new ImageView(this);
        icon.setImageResource(SECTION_ICONS[index]);
        icon.setColorFilter(0xFFBC6FF1);
        header.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView title = new TextView(this);
        title.setText(SECTION_TITLES[index]);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16f);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(dp(14), 0, 0, 0);
        header.addView(title, titleParams);

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron_down);
        chevron.setColorFilter(0xFF9E9E9E);
        header.addView(chevron, new LinearLayout.LayoutParams(dp(22), dp(22)));

        card.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(this);
        body.setText(SECTION_BODIES[index]);
        body.setTextColor(0xFFBBBBBB);
        body.setTextSize(14f);
        body.setLineSpacing(dp(4), 1f);
        body.setPadding(dp(18), 0, dp(18), dp(18));
        card.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (expanded) {
            chevron.setRotation(180f);
        } else {
            body.setVisibility(View.GONE);
        }

        header.setOnClickListener(v -> toggleSection(body, chevron));
        return card;
    }

    private void toggleSection(TextView body, ImageView chevron) {
        boolean opening = body.getVisibility() != View.VISIBLE;
        chevron.animate().rotation(opening ? 180f : 0f).setDuration(200).start();

        if (opening) {
            body.setVisibility(View.VISIBLE);
            int parentWidth = body.getParent() instanceof View && ((View) body.getParent()).getWidth() > 0
                    ? ((View) body.getParent()).getWidth()
                    : getResources().getDisplayMetrics().widthPixels - dp(76);
            body.measure(View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            animateHeight(body, 0, body.getMeasuredHeight(), false);
        } else {
            animateHeight(body, body.getHeight(), 0, true);
        }
    }

    private void animateHeight(View view, int from, int to, boolean hideAtEnd) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(220);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a -> {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = (int) a.getAnimatedValue();
            view.requestLayout();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (hideAtEnd) {
                    view.setVisibility(View.GONE);
                } else {
                    ViewGroup.LayoutParams params = view.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    view.requestLayout();
                }
            }
        });
        animator.start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
