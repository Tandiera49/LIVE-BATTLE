package com.livebattle.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BattleGameView extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap arenaBitmap;
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();
    private final List<Supporter> supporters = new ArrayList<>();
    private final List<BattleEvent> eventQueue = new ArrayList<>();

    // ===== WAR TERRITORY UNITS =====
    private static final float UNIT_MAX_HP = 100f;
    private static final float UNIT_SPEED = 28f;
    private static final float UNIT_ATTACK_RANGE = 95f;
    private static final long UNIT_ATTACK_COOLDOWN_MS = 900L;
    private static final long UNIT_RESPAWN_MS = 2500L;

    private long lastEventProcess = 0L;
    private long lastFireAccepted = 0L;

    private static final long FIRE_COOLDOWN_MS = 90L;
    private static final long EVENT_INTERVAL_MS = 70L;

    private static final int BLUE = Color.rgb(30, 130, 255);
    private static final int RED = Color.rgb(255, 55, 85);
    private static final int GOLD = Color.rgb(255, 205, 55);
    private static final float FRONTLINE_CENTER = 0.5f;

    private float teamAHp = 1000;
    private float teamBHp = 1000;

    private int teamAAmmo = 40;
    private int teamBAmmo = 40;

    private int teamAGifts = 0;
    private int teamBGifts = 0;

    private int teamALevel = 1;
    private int teamBLevel = 1;

    private int teamASupporters = 0;
    private int teamBSupporters = 0;

    private int combo = 0;
    private long lastTap = 0;
    private long lastFrame = 0;
    private float elapsed = 0;

    private boolean battleStarted = false;
    private boolean finished = false;

    private String lastEvent = "PILIH TIM UNTUK BERGABUNG";
    private String featuredName = "";
    private String featuredGift = "";

    private int selectedTeam = 0;

    private final String[] demoNames = {
            "@BudiGaming",
            "@AndiLive",
            "@Sinta88",
            "@RakaFC",
            "@DimasPro",
            "@NandaBJ",
            "@KevinLive",
            "@Arga17"
    };


    private static final int STATE_LOBBY = 0;
    private static final int STATE_COUNTDOWN = 1;
    private static final int STATE_BATTLE = 2;
    private static final int STATE_FINISHED = 3;

    private int battleState = STATE_LOBBY;
    private float countdownSeconds = 0f;
    private int battleSeconds = 180;
    private long battleStartedAt = 0L;

    private int demoIndex = 0;

    public BattleGameView(Context context) {
        super(context);

        arenaBitmap = BitmapFactory.decodeResource(
                getResources(),
                R.drawable.live_battle_arena_map
        );

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
        drawTeamPanels(c, w);
        drawArena(c, w, h);
        drawFrontline(c, w, h);
        drawWarUnits(c);
        drawCountdownOverlay(c, w, h);
        drawProjectiles(c);
        drawEffects(c);
        drawBattleInfo(c, w, h);
        drawSupporterBanner(c, w, h);
        drawControls(c, w, h);

        long now = System.currentTimeMillis();

        if (lastFrame == 0) {
            lastFrame = now;
        }

        float dt = Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        elapsed += dt;

        updateGame(dt);
        invalidate();
    }

    private void drawBackground(Canvas c, float w, float h) {
        LinearGradient g = new LinearGradient(
                0, 0,
                0, h,
                Color.rgb(7, 9, 18),
                Color.rgb(25, 5, 22),
                Shader.TileMode.CLAMP
        );

        p.setShader(g);
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        p.setColor(Color.argb(18, 255, 255, 255));

        for (int i = 1; i < 10; i++) {
            float y = h * i / 10f;
            c.drawRect(0, y, w, y + 1, p);
        }

        p.setColor(Color.argb(18, Color.red(BLUE), Color.green(BLUE), Color.blue(BLUE)));
        c.drawCircle(w * .12f, h * .42f, w * .35f, p);

        p.setColor(Color.argb(18, Color.red(RED), Color.green(RED), Color.blue(RED)));
        c.drawCircle(w * .88f, h * .42f, w * .35f, p);
    }

    private void drawHeader(Canvas c, float w) {
        text(c, "LIVE-BATTLE", w / 2, 42, 25, Color.WHITE, Paint.Align.CENTER);
        text(c, "TEAM WAR", w / 2, 67, 12,
                Color.rgb(170, 175, 190), Paint.Align.CENTER);

        if (!battleStarted) {
            text(c, "🔥 LIVE WAR LOBBY 🔥",
                    w / 2, 96, 16, GOLD, Paint.Align.CENTER);

            text(c, "PENONTON: PILIH TIMMU!",
                    w / 2, 116, 11, Color.WHITE, Paint.Align.CENTER);
        } else if (!finished) {
            text(c, "⚔️ WAR ON — SUPPORT TIMMU!",
                    w / 2, 96, 13, GOLD, Paint.Align.CENTER);
        }
    }

    private void drawTeamPanels(Canvas c, float w) {
        float top = 112;
        float margin = 12;
        float gap = 8;
        float width = (w - margin * 2 - gap) / 2f;
        float height = 106;

        panel(c, margin, top, width, height, BLUE, "TEAM A");
        panel(c, margin + width + gap, top, width, height, RED, "TEAM B");

        text(c, teamASupporters + " SUPPORTER",
                margin + width / 2, top + 40, 13,
                Color.WHITE, Paint.Align.CENTER);

        text(c, teamAAmmo + " AMMO",
                margin + width / 2, top + 62, 17,
                GOLD, Paint.Align.CENTER);

        text(c, "WEAPON LV." + teamALevel,
                margin + width / 2, top + 84, 11,
                Color.WHITE, Paint.Align.CENTER);

        text(c, teamBSupporters + " SUPPORTER",
                margin + width + gap + width / 2, top + 40, 13,
                Color.WHITE, Paint.Align.CENTER);

        text(c, teamBAmmo + " AMMO",
                margin + width + gap + width / 2, top + 62, 17,
                GOLD, Paint.Align.CENTER);

        text(c, "WEAPON LV." + teamBLevel,
                margin + width + gap + width / 2, top + 84, 11,
                Color.WHITE, Paint.Align.CENTER);
    }

    private void panel(Canvas c, float x, float y, float width,
                       float height, int color, String title) {
        p.setColor(Color.argb(90, color >> 16 & 255,
                color >> 8 & 255, color & 255));
        c.drawRoundRect(x, y, x + width, y + height, 18, 18, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        p.setColor(Color.argb(160, color >> 16 & 255,
                color >> 8 & 255, color & 255));
        c.drawRoundRect(x, y, x + width, y + height, 18, 18, p);
        p.setStyle(Paint.Style.FILL);

        text(c, title, x + width / 2, y + 25, 18,
                Color.WHITE, Paint.Align.CENTER);
    }

    private void drawArena(Canvas c, float w, float h) {
        float top = h * .28f;
        float bottom = h * .64f;
        float left = 12f;
        float right = w - 12f;

        // Arena background baru.
        if (arenaBitmap != null) {
            float dstW = right - left;
            float dstH = bottom - top;

            float srcW = arenaBitmap.getWidth();
            float srcH = arenaBitmap.getHeight();

            float scale = Math.max(dstW / srcW, dstH / srcH);

            float drawW = srcW * scale;
            float drawH = srcH * scale;

            float dx = left + (dstW - drawW) / 2f;
            float dy = top + (dstH - drawH) / 2f;

            c.save();
            c.clipRect(left, top, right, bottom);

            p.setAlpha(255);
            p.setFilterBitmap(true);

            c.drawBitmap(
                    arenaBitmap,
                    dx,
                    dy,
                    p
            );

            c.restore();
        } else {
            p.setColor(Color.rgb(15, 25, 30));
            c.drawRoundRect(
                    left,
                    top,
                    right,
                    bottom,
                    24,
                    24,
                    p
            );
        }

        // Lapisan tipis agar unit/gameplay tetap mudah terlihat.
        p.setColor(Color.argb(22, 0, 0, 0));
        c.drawRoundRect(
                left,
                top,
                right,
                bottom,
                24,
                24,
                p
        );

        float mid = w / 2f;

        // Status arena dinamis tetap berasal dari game.
        if (battleState == STATE_BATTLE) {
            text(
                    c,
                    "TAP ARENA = FIRE",
                    mid,
                    top + 58,
                    13,
                    GOLD,
                    Paint.Align.CENTER
            );
        } else if (battleState == STATE_COUNTDOWN) {
            text(
                    c,
                    "BERSIAP...",
                    mid,
                    top + 58,
                    13,
                    GOLD,
                    Paint.Align.CENTER
            );
        } else if (battleState == STATE_FINISHED) {
            text(
                    c,
                    "WAR SELESAI",
                    mid,
                    top + 58,
                    13,
                    GOLD,
                    Paint.Align.CENTER
            );
        } else {
            text(
                    c,
                    "PILIH TIM DULU",
                    mid,
                    top + 58,
                    13,
                    Color.WHITE,
                    Paint.Align.CENTER
            );
        }

        if (battleState == STATE_BATTLE) {
            text(
                    c,
                    "● LIVE WAR",
                    mid,
                    bottom - 12,
                    10,
                    Color.rgb(255, 80, 95),
                    Paint.Align.CENTER
            );
        }

        if (combo >= 3) {
            text(
                    c,
                    "COMBO x" + combo,
                    mid,
                    bottom - 27,
                    19,
                    GOLD,
                    Paint.Align.CENTER
            );
        }
    }

    private void drawCountdownOverlay(Canvas c, float w, float h) {
        if (battleState != STATE_COUNTDOWN) {
            return;
        }

        float top = h * .28f;
        float bottom = h * .64f;

        p.setColor(Color.argb(175, 0, 0, 0));
        c.drawRoundRect(
                12, top,
                w - 12, bottom,
                24, 24, p
        );

        int number = Math.max(
                1,
                (int) Math.ceil(countdownSeconds)
        );

        text(c,
                String.valueOf(number),
                w / 2,
                (top + bottom) / 2f + 24,
                76,
                GOLD,
                Paint.Align.CENTER);

        text(c,
                "BERSIAP UNTUK WAR!",
                w / 2,
                (top + bottom) / 2f + 58,
                15,
                Color.WHITE,
                Paint.Align.CENTER);
    }

    private void drawBunker(Canvas c, float x, float y, int color) {
        p.setColor(Color.argb(110, color >> 16 & 255,
                color >> 8 & 255, color & 255));
        c.drawRoundRect(x - 22, y - 12, x + 22, y + 12, 7, 7, p);

        p.setColor(Color.rgb(48, 48, 58));
        c.drawRect(x - 13, y - 8, x + 13, y + 8, p);

        p.setColor(color);
        c.drawCircle(x, y - 10, 5, p);
    }

    private void drawBase(Canvas c, float x, float y, int color, String label) {
        p.setColor(Color.argb(80, color >> 16 & 255,
                color >> 8 & 255, color & 255));
        c.drawCircle(x, y, 42, p);

        p.setColor(color);
        c.drawRect(x - 25, y - 22, x + 25, y + 22, p);

        p.setColor(Color.rgb(45, 45, 55));
        c.drawRect(x - 17, y - 12, x + 17, y + 18, p);

        text(c, "BASE " + label, x, y + 5, 10,
                Color.WHITE, Paint.Align.CENTER);
    }

    private float getFrontlineX() {
        float w = getWidth();

        float totalA = 0f;
        float totalB = 0f;
        int countA = 0;
        int countB = 0;

        for (Supporter unit : supporters) {
            if (!unit.alive) {
                continue;
            }

            if (unit.team == 1) {
                totalA += unit.x;
                countA++;
            } else {
                totalB += unit.x;
                countB++;
            }
        }

        if (countA == 0 || countB == 0) {
            return w * FRONTLINE_CENTER;
        }

        float avgA = totalA / countA;
        float avgB = totalB / countB;

        return (avgA + avgB) / 2f;
    }

    private void drawFrontline(Canvas c, float w, float h) {
        if (battleState != STATE_BATTLE) {
            return;
        }

        float top = h * .31f;
        float bottom = h * .61f;
        float x = getFrontlineX();

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3);
        p.setColor(Color.argb(190, 255, 205, 55));

        c.drawLine(x, top + 8, x, bottom - 8, p);

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(35, 255, 205, 55));
        c.drawRect(x - 12, top, x + 12, bottom, p);

        text(
                c,
                "FRONTLINE",
                x,
                top + 14,
                8,
                GOLD,
                Paint.Align.CENTER
        );
    }

    private void drawWarUnits(Canvas c) {
        for (Supporter unit : supporters) {
            if (!unit.alive) {
                continue;
            }

            int teamColor = unit.team == 1 ? BLUE : RED;

            // Unit body
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(220, 12, 18, 25));
            c.drawCircle(unit.x, unit.y, 15, p);

            p.setColor(teamColor);
            c.drawCircle(unit.x, unit.y, 11, p);

            // Unit direction / weapon marker
            p.setColor(Color.WHITE);
            c.drawCircle(
                    unit.team == 1 ? unit.x + 5 : unit.x - 5,
                    unit.y - 2,
                    3,
                    p
            );

            // HP bar
            float hpRatio = Math.max(0f, Math.min(1f, unit.hp / UNIT_MAX_HP));
            float barWidth = 38f;

            p.setColor(Color.argb(170, 0, 0, 0));
            c.drawRoundRect(
                    unit.x - barWidth / 2,
                    unit.y - 25,
                    unit.x + barWidth / 2,
                    unit.y - 20,
                    3,
                    3,
                    p
            );

            p.setColor(teamColor);
            c.drawRoundRect(
                    unit.x - barWidth / 2,
                    unit.y - 25,
                    unit.x - barWidth / 2 + barWidth * hpRatio,
                    unit.y - 20,
                    3,
                    3,
                    p
            );

            // Supporter name
            text(
                    c,
                    unit.name,
                    unit.x,
                    unit.y + 30,
                    9,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            // Weapon level
            text(
                    c,
                    "LV" + unit.weaponLevel,
                    unit.x,
                    unit.y + 42,
                    8,
                    GOLD,
                    Paint.Align.CENTER
            );
        }
    }

    private void drawProjectiles(Canvas c) {
        for (Projectile q : projectiles) {
            float x = q.getX();
            float y = q.getY();

            float trail = q.size * 3.5f;

            p.setColor(Color.argb(
                    45,
                    Color.red(q.color),
                    Color.green(q.color),
                    Color.blue(q.color)
            ));
            c.drawCircle(x, y, trail, p);

            p.setColor(Color.argb(
                    90,
                    Color.red(q.color),
                    Color.green(q.color),
                    Color.blue(q.color)
            ));
            c.drawCircle(x, y, q.size * 1.8f, p);

            p.setColor(q.color);
            c.drawCircle(x, y, q.size, p);

            if ("MISSILE".equals(q.weapon)) {
                p.setColor(Color.WHITE);
                c.drawCircle(x - (q.fromA ? 10 : -10), y, 3, p);
            } else if ("AIR STRIKE".equals(q.weapon)) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2);
                p.setColor(Color.argb(180,
                        Color.red(q.color),
                        Color.green(q.color),
                        Color.blue(q.color)));
                c.drawCircle(x, y, q.size * 2.8f, p);
                p.setStyle(Paint.Style.FILL);
            } else if ("ULTIMATE".equals(q.weapon)) {
                p.setColor(GOLD);
                c.drawCircle(x, y, q.size * 2.4f, p);
            }
        }
    }

    private void drawEffects(Canvas c) {
        for (Effect e : effects) {
            float alpha = Math.max(0, Math.min(255,
                    (int)(255 * e.life / e.maxLife)));

            text(c, e.value, e.x, e.y, e.size,
                    Color.argb(alpha, e.r, e.g, e.b),
                    Paint.Align.CENTER);
        }
    }

    private void drawBattleInfo(Canvas c, float w, float h) {
        float y = h * .67f;

        drawHp(c, 18, y, w * .40f, teamAHp, BLUE, "TEAM A");
        drawHp(c, w * .60f, y, w * .40f - 18, teamBHp, RED, "TEAM B");

        text(c, String.format(Locale.US, "%d", (int) teamAHp),
                18, y - 7, 12, Color.WHITE, Paint.Align.LEFT);

        text(c, String.format(Locale.US, "%d", (int) teamBHp),
                w - 18, y - 7, 12, Color.WHITE, Paint.Align.RIGHT);

        text(c, "VS", w / 2, y + 28, 15,
                Color.WHITE, Paint.Align.CENTER);

        int totalSeconds = battleSeconds;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        String timer = String.format(
                Locale.US,
                "%02d:%02d",
                minutes,
                seconds
        );

        if (battleState == STATE_COUNTDOWN) {
            int number = Math.max(
                    1,
                    (int) Math.ceil(countdownSeconds)
            );

            text(c, String.valueOf(number),
                    w / 2, y + 66, 34,
                    GOLD, Paint.Align.CENTER);

        } else if (battleState == STATE_BATTLE) {
            text(c, timer,
                    w / 2, y + 66, 24,
                    GOLD, Paint.Align.CENTER);

            text(c, "TIME",
                    w / 2, y + 82, 8,
                    Color.rgb(170, 175, 190),
                    Paint.Align.CENTER);

        } else if (battleState == STATE_FINISHED) {
            text(c, "00:00",
                    w / 2, y + 66, 24,
                    GOLD, Paint.Align.CENTER);

            text(c, "WAR SELESAI",
                    w / 2, y + 82, 8,
                    Color.rgb(170, 175, 190),
                    Paint.Align.CENTER);
        }
    }

    private void drawHp(Canvas c, float x, float y, float width,
                        float hp, int color, String name) {
        p.setColor(Color.argb(65, 255, 255, 255));
        c.drawRoundRect(x, y, x + width, y + 17, 10, 10, p);

        float ratio = Math.max(0, Math.min(1, hp / 1000f));

        p.setColor(color);
        c.drawRoundRect(x, y, x + width * ratio, y + 17, 10, 10, p);

        text(c, name, x, y + 34, 10, color, Paint.Align.LEFT);
    }

    private void drawSupporterBanner(Canvas c, float w, float h) {
        if (featuredName == null || featuredName.isEmpty()) {
            return;
        }

        float y = h * .75f;

        p.setColor(Color.argb(210, 12, 12, 22));
        c.drawRoundRect(18, y - 32, w - 18, y + 38, 18, 18, p);

        text(c, featuredName, w / 2, y - 5, 17,
                GOLD, Paint.Align.CENTER);

        text(c, featuredGift, w / 2, y + 20, 13,
                Color.WHITE, Paint.Align.CENTER);
    }

    private void drawControls(Canvas c, float w, float h) {
        float y = h * .81f;

        if (battleState == STATE_LOBBY) {
            text(c, "PILIH TIM UNTUK IKUT PERANG",
                    w / 2, y - 18, 13,
                    Color.WHITE, Paint.Align.CENTER);

            button(c, 16, y, w / 2 - 22, 60,
                    "🔵 TEAM A", BLUE);

            button(c, w / 2 + 6, y, w / 2 - 22, 60,
                    "🔴 TEAM B", RED);

            text(c, "Nama kamu akan tampil di LIVE",
                    w / 2, y + 78, 10,
                    Color.rgb(175, 180, 195),
                    Paint.Align.CENTER);

            button(c, 16, y + 92, w - 32, 58,
                    selectedTeam == 0
                            ? "⚔️ PILIH TIM DULU"
                            : "🔥 MULAI PERANG!",
                    selectedTeam == 0
                            ? Color.rgb(70, 70, 80)
                            : Color.rgb(210, 105, 20));

            text(c, "🎁 GIFT = AMUNISI  •  🚀 GIFT BESAR = SENJATA NAIK LEVEL",
                    w / 2, y + 169, 9,
                    Color.rgb(175, 180, 195),
                    Paint.Align.CENTER);
            return;
        }

        if (battleState == STATE_COUNTDOWN) {
            text(c, "⚔️ WAR AKAN DIMULAI",
                    w / 2, y - 18, 13,
                    GOLD, Paint.Align.CENTER);

            text(c, "BERSIAP • JANGAN TAP DULU",
                    w / 2, y + 18, 11,
                    Color.WHITE, Paint.Align.CENTER);

            return;
        }

        if (battleState == STATE_FINISHED) {
            text(c, "🏆 WAR SELESAI",
                    w / 2, y - 18, 15,
                    GOLD, Paint.Align.CENTER);

            button(c, 16, y + 8, w - 32, 58,
                    "🔥 MULAI RONDE BARU",
                    Color.rgb(210, 105, 20));

            return;
        }

        text(c, "👆 TAP = TEMBAK   •   🎁 GIFT = AMUNISI   •   🚀 GIFT BESAR = SENJATA NAIK LEVEL",
                w / 2, y - 18, 9,
                GOLD, Paint.Align.CENTER);

        button(c, 16, y, w - 32, 58,
                "👆 TAP ARENA UNTUK MENEMBAK",
                selectedTeam == 1 ? BLUE : RED);

        text(c, "TEST EVENT / SIMULATOR",
                w / 2, y + 83, 10,
                Color.rgb(150, 155, 170),
                Paint.Align.CENTER);

        button(c, 16, y + 96, (w - 44) / 3,
                48, "🎁 ROSE",
                Color.rgb(35, 105, 210));

        button(c, 22 + (w - 44) / 3, y + 96,
                (w - 44) / 3, 48, "💎 MEGA",
                Color.rgb(155, 45, 100));

        button(c, 28 + ((w - 44) / 3) * 2, y + 96,
                (w - 44) / 3, 48, "🚀 ULT GIFT",
                Color.rgb(205, 75, 30));
    }

    private void button(Canvas c, float x, float y,
                        float width, float height,
                        String label, int color) {
        p.setColor(color);
        c.drawRoundRect(x, y, x + width, y + height, 16, 16, p);

        text(c, label, x + width / 2,
                y + height / 2 + 6,
                13, Color.WHITE, Paint.Align.CENTER);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }

        float x = e.getX();
        float y = e.getY();
        float w = getWidth();
        float h = getHeight();

        if (battleState == STATE_LOBBY) {
            handleLobbyTouch(x, y, w, h);
            return true;
        }

        if (battleState == STATE_COUNTDOWN) {
            return true;
        }

        if (battleState == STATE_FINISHED) {
            float controlsY = h * .81f;

            if (y >= controlsY + 8 && y <= controlsY + 66) {
                resetBattle();
            }

            return true;
        }

        if (y >= h * .28f && y <= h * .64f) {
            fireTap();
            return true;
        }

        float controlsY = h * .81f;

        if (y >= controlsY + 96 && y <= controlsY + 144) {
            float third = (w - 44) / 3f;

            if (x < 16 + third) {
                gift("ROSE", 1, 10);
            } else if (x < 22 + third * 2) {
                gift("MEGA GIFT", 2, 70);
            } else {
                gift("ULTIMATE GIFT", 3, 180);
            }
        }

        return true;
    }

    private void handleLobbyTouch(float x, float y, float w, float h) {
        float controlsY = h * .81f;

        if (y >= controlsY && y <= controlsY + 60) {
            if (x < w / 2) {
                joinTeam(1);
            } else {
                joinTeam(2);
            }
            return;
        }

        if (y >= controlsY + 70 && y <= controlsY + 124) {
            if (selectedTeam == 0) {
                lastEvent = "PILIH TEAM A ATAU TEAM B DULU";
                return;
            }

            startCountdown();
        }
    }

    private void joinTeam(int team) {
        selectedTeam = team;

        String name = demoNames[demoIndex++ % demoNames.length];

        Supporter unit = new Supporter(name, team);
        supporters.add(unit);

        if (team == 1) {
            teamASupporters++;
            lastEvent = name + " bergabung TEAM A";
            featuredName = name;
            featuredGift = "JOINED TEAM A";
        } else {
            teamBSupporters++;
            lastEvent = name + " bergabung TEAM B";
            featuredName = name;
            featuredGift = "JOINED TEAM B";
        }

        spawnSupporter(unit, getWidth(), getHeight());
    }

    private void startCountdown() {
        battleState = STATE_COUNTDOWN;
        battleStarted = false;
        finished = false;
        countdownSeconds = 3f;
        lastEvent = "WAR DIMULAI DALAM 3...";

        addEffect(
                "GET READY!",
                getWidth() / 2f,
                getHeight() * .45f,
                28,
                255,
                210,
                60
        );
    }

    private void beginBattle() {
        battleState = STATE_BATTLE;
        battleStarted = true;
        finished = false;
        battleStartedAt = System.currentTimeMillis();
        battleSeconds = 180;

        for (Supporter unit : supporters) {
            spawnSupporter(unit, getWidth(), getHeight());
        }

        lastEvent = "WAR DIMULAI!";

        addEffect(
                "WAR DIMULAI!",
                getWidth() / 2f,
                getHeight() * .45f,
                28,
                255,
                210,
                60
        );
    }

    private void fireTap() {
        if (selectedTeam == 0 || !battleStarted || finished) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastFireAccepted < FIRE_COOLDOWN_MS) {
            return;
        }

        int team = selectedTeam;

        // Ammo harus diperiksa sebelum cooldown diterima.
        if (team == 1 && teamAAmmo <= 0) {
            lastEvent = "TEAM A KEHABISAN AMMO";
            return;
        }

        if (team == 2 && teamBAmmo <= 0) {
            lastEvent = "TEAM B KEHABISAN AMMO";
            return;
        }

        lastFireAccepted = now;

        if (now - lastTap < 450) {
            combo++;
        } else {
            combo = 1;
        }

        lastTap = now;

        boolean fromA = team == 1;
        int damage = weaponDamage(fromA ? teamALevel : teamBLevel);

        if (fromA) {
            teamAAmmo--;
        } else {
            teamBAmmo--;
        }

        Supporter target = findNearestEnemyForTap(team);

        if (target != null) {
            launchProjectileToUnit(
                    fromA,
                    damage,
                    target
            );
            lastEvent = "TAP → " + target.name;
        } else {
            // Tidak ada unit lawan: tetap arahkan tembakan ke base.
            launchProjectile(fromA, damage);
        }
    }

    private Supporter findNearestEnemyForTap(int team) {
        Supporter nearest = null;
        float nearestDistance = Float.MAX_VALUE;

        float centerX = getWidth() / 2f;
        float centerY = getHeight() * .45f;

        for (Supporter unit : supporters) {
            if (!unit.alive || unit.team == team) {
                continue;
            }

            float dx = unit.x - centerX;
            float dy = unit.y - centerY;
            float distance = dx * dx + dy * dy;

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = unit;
            }
        }

        return nearest;
    }

    private void launchProjectileToUnit(
            boolean fromA,
            int damage,
            Supporter target
    ) {
        float w = getWidth();
        float h = getHeight();

        float startX = fromA ? w * .22f : w * .78f;
        float startY = h * .45f;

        String weapon = weaponName(fromA ? teamALevel : teamBLevel);
        int color = fromA ? BLUE : RED;

        projectiles.add(new Projectile(
                startX,
                startY,
                target.x,
                target.y,
                color,
                weapon,
                fromA,
                damage,
                target
        ));
    }

    private int weaponDamage(int level) {
        switch (level) {
            case 1: return 4;
            case 2: return 12;
            case 3: return 25;
            case 4: return 50;
            default: return 100;
        }
    }

    private void launchProjectile(boolean fromA, int damage) {
        float w = getWidth();
        float h = getHeight();

        float startX = fromA ? w * .22f : w * .78f;
        float targetX = fromA ? w * .78f : w * .22f;

        String weapon = weaponName(fromA ? teamALevel : teamBLevel);

        int lane = (int)(System.currentTimeMillis() % 3);
        float[] lanes = {
                h * .37f,
                h * .45f,
                h * .53f
        };
        float y = lanes[lane];

        int color = fromA ? BLUE : RED;

        projectiles.add(new Projectile(
                startX,
                y,
                targetX,
                y,
                color,
                weapon,
                fromA,
                damage
        ));

        // Damage diberikan saat projectile benar-benar mencapai target.
        // Nilai damage disimpan di projectile agar visual dan gameplay sinkron.

    }

    private String weaponName(int level) {
        switch (level) {
            case 1: return "BULLET";
            case 2: return "CANNON";
            case 3: return "MISSILE";
            case 4: return "AIR STRIKE";
            default: return "ULTIMATE";
        }
    }

    private void enqueueGift(String name, int team, String gift, int levelUp, int ammo) {
        eventQueue.add(BattleEvent.gift(name, team, gift, levelUp, ammo));
        lastEvent = name + " mengirim " + gift;
    }

    private void processEventQueue() {
        long now = System.currentTimeMillis();

        if (!battleStarted || finished) {
            return;
        }

        if (eventQueue.isEmpty() || now - lastEventProcess < EVENT_INTERVAL_MS) {
            return;
        }

        BattleEvent event = eventQueue.remove(0);
        lastEventProcess = now;

        if (event.type == BattleEvent.TYPE_GIFT) {
            applyGift(event.name, event.team, event.gift, event.levelUp, event.ammo);
        }
    }

    private void applyGift(
            String name,
            int team,
            String gift,
            int levelUp,
            int ammo
    ) {
        boolean teamA = team == 1;

        if (teamA) {
            teamAAmmo += ammo;
            teamAGifts++;
            teamALevel = Math.min(5, teamALevel + levelUp);
        } else {
            teamBAmmo += ammo;
            teamBGifts++;
            teamBLevel = Math.min(5, teamBLevel + levelUp);
        }

        // Gift memperkuat unit supporter yang mengirimkannya.
        int teamLevel = teamA ? teamALevel : teamBLevel;

        for (Supporter unit : supporters) {
            if (unit.team == team && unit.name.equals(name)) {
                unit.weaponLevel = Math.max(
                        unit.weaponLevel,
                        Math.min(5, teamLevel)
                );
            }
        }

        featuredName = name;
        featuredGift = gift + " • " +
                (teamA ? "TEAM A" : "TEAM B") +
                " • +" + ammo + " AMMO";

        lastEvent = name + " mengirim " + gift;

        int color = teamA ? BLUE : RED;

        addEffect(
                gift + "!",
                getWidth() / 2f,
                getHeight() * .42f,
                gift.contains("ULT") ? 32 : 25,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );

        if (gift.contains("ULT")) {
            addEffect(
                    "WEAPON UPGRADED → " +
                            weaponName(teamA ? teamALevel : teamBLevel),
                    getWidth() / 2f,
                    getHeight() * .48f,
                    17,
                    255,
                    210,
                    60
            );
        }
    }

    private static final class BattleEvent {
        static final int TYPE_GIFT = 1;

        final int type;
        final String name;
        final int team;
        final String gift;
        final int levelUp;
        final int ammo;

        private BattleEvent(
                int type,
                String name,
                int team,
                String gift,
                int levelUp,
                int ammo
        ) {
            this.type = type;
            this.name = name;
            this.team = team;
            this.gift = gift;
            this.levelUp = levelUp;
            this.ammo = ammo;
        }

        static BattleEvent gift(
                String name,
                int team,
                String gift,
                int levelUp,
                int ammo
        ) {
            return new BattleEvent(
                    TYPE_GIFT,
                    name,
                    team,
                    gift,
                    levelUp,
                    ammo
            );
        }
    }

    private void gift(String gift, int levelUp, int ammo) {
        if (selectedTeam == 0) {
            selectedTeam = 1;
            joinTeam(1);
        }

        String name = demoNames[demoIndex++ % demoNames.length];

        enqueueGift(
                name,
                selectedTeam,
                gift,
                levelUp,
                ammo
        );
    }

    private void checkWinner() {
        if (teamAHp <= 0 || teamBHp <= 0) {
            finished = true;
            battleStarted = false;
            battleState = STATE_FINISHED;

            String winner;

            if (teamAHp > teamBHp) {
                winner = "TEAM A MENANG!";
            } else {
                winner = "TEAM B MENANG!";
            }

            lastEvent = winner;

            addEffect(
                    winner,
                    getWidth() / 2f,
                    getHeight() * .48f,
                    34, 255, 210, 60
            );
        }
    }

    private void resetBattle() {
        teamAHp = 1000;
        teamBHp = 1000;

        teamAAmmo = 40;
        teamBAmmo = 40;

        teamALevel = 1;
        teamBLevel = 1;

        teamAGifts = 0;
        teamBGifts = 0;

        combo = 0;

        projectiles.clear();
        effects.clear();
        eventQueue.clear();

        lastEventProcess = 0L;
        lastFireAccepted = 0L;
        lastTap = 0L;

        finished = false;
        battleStarted = false;
        battleState = STATE_LOBBY;
        countdownSeconds = 0f;
        selectedTeam = 0;
        supporters.clear();
        battleSeconds = 180;
        battleStartedAt = 0L;

        lastEvent = "PILIH TIM UNTUK BATTLE BARU";
        featuredName = "";
        featuredGift = "";
    }

    private void updateBattleState(float dt) {
        if (battleState == STATE_COUNTDOWN) {
            countdownSeconds -= dt;

            if (countdownSeconds <= 0f) {
                beginBattle();
            }
        }

        if (battleState == STATE_BATTLE && battleStartedAt > 0L) {
            long elapsedMs =
                    System.currentTimeMillis() - battleStartedAt;

            battleSeconds = Math.max(
                    0,
                    180 - (int)(elapsedMs / 1000L)
            );

            if (battleSeconds <= 0) {
                finished = true;
                battleStarted = false;
                battleState = STATE_FINISHED;
                lastEvent = "WAKTU HABIS!";

                addEffect(
                        "WAKTU HABIS!",
                        getWidth() / 2f,
                        getHeight() * .48f,
                        30,
                        255,
                        210,
                        60
                );
            }
        }
    }

    private void updateGame(float dt) {
        updateBattleState(dt);
        processEventQueue();
        updateWarUnits(dt);

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile q = projectiles.get(i);

            // Projectile mengikuti posisi unit selama masih hidup.
            if (q.targetUnit != null && q.targetUnit.alive) {
                q.targetX = q.targetUnit.x;
                q.targetY = q.targetUnit.y;
            }

            q.progress += dt * 2.8f;

            if (q.progress >= 1f) {
                float hitX = q.targetX;
                float hitY = q.targetY;

                if (q.targetUnit != null) {
                    Supporter target = q.targetUnit;

                    if (target.alive) {
                        target.hp -= q.damage;

                        lastEvent = q.weapon
                                + " → "
                                + target.name
                                + " -"
                                + q.damage;

                        addEffect(
                                "-" + q.damage,
                                hitX,
                                hitY - 20,
                                "ULTIMATE".equals(q.weapon) ? 28 : 20,
                                255, 90, 90
                        );

                        if ("AIR STRIKE".equals(q.weapon)) {
                            addEffect(
                                    "AIR STRIKE!",
                                    hitX,
                                    hitY - 45,
                                    18,
                                    255, 190, 60
                            );
                        }

                        if ("ULTIMATE".equals(q.weapon)) {
                            addEffect(
                                    "ULTIMATE HIT!",
                                    hitX,
                                    hitY - 55,
                                    25,
                                    255, 210, 60
                            );
                        }

                        if (target.hp <= 0f) {
                            target.die();

                            addEffect(
                                    "KO!",
                                    hitX,
                                    hitY - 55,
                                    22,
                                    255, 210, 60
                            );

                            lastEvent = q.weapon
                                    + " KO "
                                    + target.name;
                        }
                    } else {
                        addEffect(
                                "MISS",
                                hitX,
                                hitY - 20,
                                14,
                                180, 180, 180
                        );
                    }
                } else {
                    // Projectile tanpa target unit tetap menyerang base.
                    if (q.fromA) {
                        teamBHp -= q.damage;
                        lastEvent = q.weapon
                                + " TEAM A → TEAM B -"
                                + q.damage;
                    } else {
                        teamAHp -= q.damage;
                        lastEvent = q.weapon
                                + " TEAM B → TEAM A -"
                                + q.damage;
                    }

                    addEffect(
                            "-" + q.damage,
                            hitX,
                            hitY - 20,
                            "ULTIMATE".equals(q.weapon) ? 28 : 20,
                            255, 90, 90
                    );

                    if ("AIR STRIKE".equals(q.weapon)) {
                        addEffect(
                                "AIR STRIKE!",
                                hitX,
                                hitY - 45,
                                18,
                                255, 190, 60
                        );
                    }

                    if ("ULTIMATE".equals(q.weapon)) {
                        addEffect(
                                "ULTIMATE HIT!",
                                hitX,
                                hitY - 55,
                                25,
                                255, 210, 60
                        );
                    }

                    checkWinner();
                }

                projectiles.remove(i);
            }
        }

        for (int i = effects.size() - 1; i >= 0; i--) {
            Effect e = effects.get(i);
            e.life -= dt;
            e.y -= dt * 24;

            if (e.life <= 0) {
                effects.remove(i);
            }
        }

        if (elapsed > 0 && battleStarted && !finished) {
            if (elapsed > 0.5f && elapsed % 8 < dt) {
                addEffect(
                        "WAR!",
                        getWidth() / 2f,
                        getHeight() * .43f,
                        18, 255, 255, 255
                );
            }
        }
    }

    // ===== WAR TERRITORY ENGINE =====

    private void spawnSupporter(Supporter unit, float w, float h) {
        float top = h * .31f;
        float bottom = h * .61f;

        float minX;
        float maxX;

        if (unit.team == 1) {
            minX = w * .09f;
            maxX = w * .39f;
        } else {
            minX = w * .61f;
            maxX = w * .91f;
        }

        float x = minX + (float)Math.random() * (maxX - minX);
        float y = top + (float)Math.random() * (bottom - top);

        unit.spawn(x, y);
    }

    private void spawnAllSupporters() {
        float w = getWidth();
        float h = getHeight();

        for (Supporter unit : supporters) {
            if (!unit.alive) {
                spawnSupporter(unit, w, h);
            }
        }
    }

    private Supporter findNearestEnemy(Supporter attacker) {
        Supporter nearest = null;
        float nearestDistance = Float.MAX_VALUE;

        for (Supporter unit : supporters) {
            if (!unit.alive || unit.team == attacker.team) {
                continue;
            }

            float dx = unit.x - attacker.x;
            float dy = unit.y - attacker.y;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = unit;
            }
        }

        return nearest;
    }

    private void updateWarUnits(float dt) {
        if (!battleStarted || finished || battleState != STATE_BATTLE) {
            return;
        }

        long now = System.currentTimeMillis();
        float w = getWidth();
        float h = getHeight();

        for (Supporter unit : supporters) {
            if (!unit.alive) {
                if (now >= unit.respawnAt) {
                    spawnSupporter(unit, w, h);
                }
                continue;
            }

            Supporter enemy = findNearestEnemy(unit);

            if (enemy == null) {
                continue;
            }

            float dx = enemy.x - unit.x;
            float dy = enemy.y - unit.y;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);

            if (distance > UNIT_ATTACK_RANGE) {
                if (distance > 0.1f) {
                    unit.x += (dx / distance) * UNIT_SPEED * dt;
                    unit.y += (dy / distance) * UNIT_SPEED * dt;
                }
            } else if (now - unit.lastAttack >= UNIT_ATTACK_COOLDOWN_MS) {
                attackUnit(unit, enemy);
                unit.lastAttack = now;
            }

            float top = h * .31f;
            float bottom = h * .61f;

            unit.y = Math.max(top, Math.min(bottom, unit.y));

            if (unit.team == 1) {
                unit.x = Math.max(w * .07f, Math.min(w * .94f, unit.x));
            } else {
                unit.x = Math.max(w * .06f, Math.min(w * .93f, unit.x));
            }
        }
    }

    private void attackUnit(Supporter attacker, Supporter target) {
        if (!attacker.alive || !target.alive) {
            return;
        }

        int level = Math.max(1, attacker.weaponLevel);
        int damage = weaponDamage(level);

        target.hp -= damage;
        attacker.damage += damage;

        addEffect(
                "-" + damage,
                target.x,
                target.y - 18,
                level >= 4 ? 20 : 15,
                attacker.team == 1 ? 90 : 255,
                attacker.team == 1 ? 170 : 90,
                attacker.team == 1 ? 255 : 90
        );

        addEffect(
                attacker.team == 1 ? "HIT!" : "HIT!",
                target.x,
                target.y - 38,
                11,
                255,
                220,
                90
        );

        lastEvent = attacker.name + " menyerang " + target.name;

        if (target.hp <= 0f) {
            target.die();
            attacker.kills++;

            addEffect(
                    "KO!",
                    target.x,
                    target.y - 55,
                    22,
                    255,
                    210,
                    60
            );

            lastEvent = attacker.name + " mengalahkan " + target.name;
        }
    }

    private void addEffect(String value, float x, float y,
                           float size, int r, int g, int b) {
        effects.add(new Effect(value, x, y, size, r, g, b));
    }

    private void text(Canvas c, String s, float x, float y,
                      float size, int color, Paint.Align align) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        p.setTextSize(size);
        p.setTextAlign(align);
        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        c.drawText(s, x, y, p);
    }

    private static class Projectile {
        float startX;
        float startY;
        float targetX;
        float targetY;
        float progress;
        float size;
        int color;
        String weapon;
        boolean fromA;
        int damage;
        Supporter targetUnit;

        Projectile(float startX, float startY,
                   float targetX, float targetY,
                   int color, String weapon,
                   boolean fromA,
                   int damage) {
            this(
                    startX,
                    startY,
                    targetX,
                    targetY,
                    color,
                    weapon,
                    fromA,
                    damage,
                    null
            );
        }

        Projectile(float startX, float startY,
                   float targetX, float targetY,
                   int color, String weapon,
                   boolean fromA,
                   int damage,
                   Supporter targetUnit) {
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.color = color;
            this.weapon = weapon;
            this.fromA = fromA;
            this.damage = damage;
            this.targetUnit = targetUnit;

            switch (weapon) {
                case "BULLET":
                    this.size = 5;
                    break;
                case "CANNON":
                    this.size = 8;
                    break;
                case "MISSILE":
                    this.size = 9;
                    break;
                case "AIR STRIKE":
                    this.size = 11;
                    break;
                default:
                    this.size = 15;
                    break;
            }
        }

        float getX() {
            return startX + (targetX - startX) * progress;
        }

        float getY() {
            return startY + (targetY - startY) * progress;
        }
    }

    private static class Effect {
        String value;
        float x;
        float y;
        float size;
        float life = 1.2f;
        float maxLife = 1.2f;
        int r;
        int g;
        int b;

        Effect(String value, float x, float y,
               float size, int r, int g, int b) {
            this.value = value;
            this.x = x;
            this.y = y;
            this.size = size;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static class Supporter {
        String name;
        int team;

        float x;
        float y;
        float hp = UNIT_MAX_HP;

        boolean alive = true;
        long respawnAt = 0L;
        long lastAttack = 0L;

        int weaponLevel = 1;
        int kills = 0;
        int damage = 0;

        Supporter(String name, int team) {
            this.name = name;
            this.team = team;
        }

        void spawn(float x, float y) {
            this.x = x;
            this.y = y;
            this.hp = UNIT_MAX_HP;
            this.alive = true;
            this.respawnAt = 0L;
            this.lastAttack = 0L;
        }

        void die() {
            alive = false;
            hp = 0f;
            respawnAt = System.currentTimeMillis() + UNIT_RESPAWN_MS;
        }

        void respawn(float x, float y) {
            spawn(x, y);
        }
    }
}
