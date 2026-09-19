package com.livebattle.game;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BattleGameView extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<FloatingText> texts = new ArrayList<>();

    private float leftHp = 1000;
    private float rightHp = 1000;
    private int leftScore = 0;
    private int rightScore = 0;
    private int combo = 0;
    private long lastFrame;

    private String lastEvent = "LIVE BATTLE SIAP";
    private float pulse = 0;

    public BattleGameView(Context context) {
        super(context);
        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        setFocusable(true);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        float w = getWidth();
        float h = getHeight();

        drawBackground(c, w, h);
        drawHeader(c, w);
        drawFighters(c, w, h);
        drawHealthBars(c, w, h);
        drawCenterBattle(c, w, h);
        drawEventTexts(c, w, h);
        drawControls(c, w, h);

        long now = System.currentTimeMillis();
        if (lastFrame == 0) lastFrame = now;

        float dt = Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        pulse += dt;

        updateTexts(dt);
        invalidate();
    }

    private void drawBackground(Canvas c, float w, float h) {
        LinearGradient g = new LinearGradient(
                0, 0, 0, h,
                Color.rgb(10, 10, 20),
                Color.rgb(35, 8, 25),
                Shader.TileMode.CLAMP
        );
        p.setShader(g);
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        p.setColor(Color.argb(30, 255, 255, 255));
        for (int i = 0; i < 8; i++) {
            float y = h * 0.15f + i * h * 0.1f;
            c.drawRect(0, y, w, y + 1, p);
        }
    }

    private void drawHeader(Canvas c, float w) {
        text(c, "⚔", w / 2 - 70, 58, 34, Color.WHITE, Paint.Align.CENTER);
        text(c, "LIVE BATTLE", w / 2, 58, 25, Color.WHITE, Paint.Align.CENTER);
        text(c, "⚔", w / 2 + 70, 58, 34, Color.WHITE, Paint.Align.CENTER);

        text(c, "TIKTOK LIVE GAME", w / 2, 87, 11,
                Color.rgb(180, 180, 190), Paint.Align.CENTER);
    }

    private void drawFighters(Canvas c, float w, float h) {
        float cy = h * 0.39f;
        float radius = Math.min(w * 0.16f, 100);

        drawFighter(c, w * 0.25f, cy, radius, Color.rgb(45, 120, 255), true);
        drawFighter(c, w * 0.75f, cy, radius, Color.rgb(245, 55, 75), false);

        text(c, "PLAYER A", w * 0.25f, cy + radius + 35, 17,
                Color.WHITE, Paint.Align.CENTER);

        text(c, "PLAYER B", w * 0.75f, cy + radius + 35, 17,
                Color.WHITE, Paint.Align.CENTER);
    }

    private void drawFighter(Canvas c, float x, float y, float r, int color, boolean left) {
        float punch = (float)Math.sin(pulse * 8) * 3;

        p.setColor(Color.argb(45, 255, 255, 255));
        c.drawCircle(x, y, r + 15, p);

        p.setColor(color);
        c.drawCircle(x, y, r, p);

        p.setColor(Color.WHITE);
        c.drawCircle(x - r * .28f, y - r * .2f, r * .1f, p);
        c.drawCircle(x + r * .28f, y - r * .2f, r * .1f, p);

        p.setColor(Color.BLACK);
        c.drawCircle(x - r * .28f, y - r * .2f, r * .04f, p);
        c.drawCircle(x + r * .28f, y - r * .2f, r * .04f, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(6);
        c.drawArc(x - r * .35f, y, r * .7f, r * .4f, 20, 140, false, p);
        p.setStyle(Paint.Style.FILL);

        float armX = left ? x + r + punch : x - r - punch;
        p.setStrokeWidth(18);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x, y + r * .1f, armX, y + r * .1f, p);
    }

    private void drawHealthBars(Canvas c, float w, float h) {
        float barW = w * .37f;
        float barH = 18;
        float y = h * .54f;

        drawBar(c, w * .06f, y, barW, barH, leftHp / 1000f, Color.rgb(40, 125, 255));
        drawBar(c, w * .57f, y, barW, barH, rightHp / 1000f, Color.rgb(245, 55, 75));

        text(c, String.format(Locale.US, "%d", (int)leftHp),
                w * .06f, y - 8, 14, Color.WHITE, Paint.Align.LEFT);

        text(c, String.format(Locale.US, "%d", (int)rightHp),
                w * .94f, y - 8, 14, Color.WHITE, Paint.Align.RIGHT);
    }

    private void drawBar(Canvas c, float x, float y, float width, float height,
                         float value, int color) {
        p.setColor(Color.argb(80, 255, 255, 255));
        c.drawRoundRect(x, y, x + width, y + height, 12, 12, p);

        p.setColor(color);
        c.drawRoundRect(x, y, x + width * Math.max(0, Math.min(1, value)),
                y + height, 12, 12, p);
    }

    private void drawCenterBattle(Canvas c, float w, float h) {
        text(c, "VS", w / 2, h * .56f, 25, Color.WHITE, Paint.Align.CENTER);

        if (combo > 1) {
            text(c, "COMBO ×" + combo, w / 2, h * .61f, 20,
                    Color.YELLOW, Paint.Align.CENTER);
        }

        text(c, lastEvent, w / 2, h * .67f, 15,
                Color.rgb(230, 230, 235), Paint.Align.CENTER);
    }

    private void drawEventTexts(Canvas c, float w, float h) {
        float y = h * .72f;

        for (FloatingText t : texts) {
            float alpha = Math.max(0, Math.min(255, (int)(255 * (t.life / 1.5f))));
            text(c, t.value, t.x, y + t.offset, 18,
                    Color.argb((int)alpha, 255, 220, 70), Paint.Align.CENTER);
        }
    }

    private void drawControls(Canvas c, float w, float h) {
        float top = h * .78f;
        float gap = 12;
        float bw = (w - 48 - gap * 2) / 3;
        float bh = 62;

        button(c, 16, top, bw, bh, "🎁 GIFT", Color.rgb(35, 105, 210));
        button(c, 16 + bw + gap, top, bw, bh, "⚡ SKILL", Color.rgb(150, 45, 90));
        button(c, 16 + (bw + gap) * 2, top, bw, bh, "💥 ULT", Color.rgb(190, 75, 35));

        text(c, "Gift event siap dihubungkan ke event provider",
                w / 2, h - 28, 11, Color.rgb(150, 150, 160), Paint.Align.CENTER);
    }

    private void button(Canvas c, float x, float y, float width, float height,
                        String label, int color) {
        p.setColor(color);
        c.drawRoundRect(x, y, x + width, y + height, 18, 18, p);
        text(c, label, x + width / 2, y + height / 2 + 7,
                15, Color.WHITE, Paint.Align.CENTER);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_UP) return true;

        float w = getWidth();
        float h = getHeight();

        float top = h * .78f;
        float gap = 12;
        float bw = (w - 48 - gap * 2) / 3;

        if (e.getY() >= top && e.getY() <= top + 62) {
            if (e.getX() < 16 + bw) {
                gift("Rose", 10);
            } else if (e.getX() < 16 + (bw + gap) * 2) {
                skill();
            } else {
                ultimate();
            }
        }

        return true;
    }

    private void gift(String name, int damage) {
        combo++;
        rightHp -= damage + combo * 2;
        leftScore += damage;

        lastEvent = "🎁 " + name + " → PLAYER A menyerang!";
        texts.add(new FloatingText(
                "🎁 " + name + "  -" + (damage + combo * 2),
                getWidth() * .5f
        ));

        if (rightHp <= 0) knockout();
    }

    private void skill() {
        combo += 2;
        rightHp -= 90;
        leftScore += 90;
        lastEvent = "⚡ SKILL! PLAYER A menyerang!";
        texts.add(new FloatingText("⚡ SKILL -90", getWidth() * .5f));

        if (rightHp <= 0) knockout();
    }

    private void ultimate() {
        combo += 5;
        rightHp -= 220;
        leftScore += 220;
        lastEvent = "💥 ULTIMATE! SERANGAN BESAR!";
        texts.add(new FloatingText("💥 ULTIMATE -220", getWidth() * .5f));

        if (rightHp <= 0) knockout();
    }

    private void knockout() {
        lastEvent = "💥 KNOCKOUT — PLAYER A MENANG!";
        texts.add(new FloatingText("🏆 KNOCKOUT!", getWidth() * .5f));

        postDelayed(() -> {
            leftHp = 1000;
            rightHp = 1000;
            combo = 0;
            lastEvent = "RONDE BARU!";
        }, 1800);
    }

    private void updateTexts(float dt) {
        for (int i = texts.size() - 1; i >= 0; i--) {
            FloatingText t = texts.get(i);
            t.life -= dt;
            t.offset -= dt * 25;

            if (t.life <= 0) {
                texts.remove(i);
            }
        }
    }

    private void text(Canvas c, String s, float x, float y, float size,
                      int color, Paint.Align align) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        p.setTextSize(size);
        p.setTextAlign(align);
        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        c.drawText(s, x, y, p);
    }

    private static class FloatingText {
        String value;
        float x;
        float offset;
        float life = 1.5f;

        FloatingText(String value, float x) {
            this.value = value;
            this.x = x;
        }
    }
}
