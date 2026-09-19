package com.livebattle.game;

import android.content.Context;
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
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();
    private final List<Supporter> supporters = new ArrayList<>();

    private static final int BLUE = Color.rgb(30, 130, 255);
    private static final int RED = Color.rgb(255, 55, 85);
    private static final int GOLD = Color.rgb(255, 205, 55);

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

    private int demoIndex = 0;

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
        drawTeamPanels(c, w);
        drawArena(c, w, h);
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

        p.setColor(Color.argb(18, BLUE));
        c.drawCircle(w * .12f, h * .42f, w * .35f, p);

        p.setColor(Color.argb(18, RED));
        c.drawCircle(w * .88f, h * .42f, w * .35f, p);
    }

    private void drawHeader(Canvas c, float w) {
        text(c, "LIVE-BATTLE", w / 2, 42, 25, Color.WHITE, Paint.Align.CENTER);
        text(c, "TEAM WAR", w / 2, 67, 12,
                Color.rgb(170, 175, 190), Paint.Align.CENTER);

        if (!battleStarted) {
            text(c, "PILIH TIM • KUMPULKAN SUPPORTER • MULAI PERANG",
                    w / 2, 96, 11, GOLD, Paint.Align.CENTER);
        } else if (!finished) {
            text(c, "WAR IN PROGRESS", w / 2, 96, 12,
                    Color.rgb(255, 90, 110), Paint.Align.CENTER);
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

        p.setColor(Color.argb(25, 255, 255, 255));
        c.drawRoundRect(12, top, w - 12, bottom, 24, 24, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        p.setColor(Color.argb(80, 255, 255, 255));
        c.drawRoundRect(12, top, w - 12, bottom, 24, 24, p);
        p.setStyle(Paint.Style.FILL);

        float baseY = bottom - 38;

        drawBase(c, w * .18f, baseY, BLUE, "A");
        drawBase(c, w * .82f, baseY, RED, "B");

        text(c, battleStarted ? "TAP ARENA = FIRE" : "PILIH TIM DULU",
                w / 2, top + 32, 14,
                battleStarted ? GOLD : Color.WHITE,
                Paint.Align.CENTER);

        if (combo >= 3) {
            text(c, "COMBO x" + combo,
                    w / 2, bottom - 14, 20,
                    GOLD, Paint.Align.CENTER);
        }
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

    private void drawProjectiles(Canvas c) {
        for (Projectile q : projectiles) {
            p.setColor(q.color);
            c.drawCircle(q.getX(), q.getY(), q.size, p);

            p.setColor(Color.argb(90,
                    Color.red(q.color),
                    Color.green(q.color),
                    Color.blue(q.color)));

            c.drawCircle(q.getX(), q.getY(), q.size * 2.2f, p);
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

        if (!battleStarted) {
            button(c, 16, y, w / 2 - 22, 60,
                    "JOIN TEAM A", BLUE);

            button(c, w / 2 + 6, y, w / 2 - 22, 60,
                    "JOIN TEAM B", RED);

            button(c, 16, y + 70, w - 32, 54,
                    "START WAR", Color.rgb(210, 105, 20));

            return;
        }

        button(c, 16, y, w - 32, 58,
                "TAP ANYWHERE IN ARENA TO FIRE",
                selectedTeam == 1 ? BLUE : RED);

        text(c, "GIFT SIMULATOR", w / 2, y + 83, 10,
                Color.rgb(150, 155, 170), Paint.Align.CENTER);

        button(c, 16, y + 96, (w - 44) / 3,
                48, "ROSE", Color.rgb(35, 105, 210));

        button(c, 22 + (w - 44) / 3, y + 96,
                (w - 44) / 3, 48, "MEGA", Color.rgb(155, 45, 100));

        button(c, 28 + ((w - 44) / 3) * 2, y + 96,
                (w - 44) / 3, 48, "ULT GIFT",
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

        if (!battleStarted) {
            handleLobbyTouch(x, y, w, h);
            return true;
        }

        if (finished) {
            if (y > h * .80f) {
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

            battleStarted = true;
            lastEvent = "WAR DIMULAI!";
            addEffect("WAR DIMULAI!", w / 2, getHeight() * .45f,
                    28, 255, 210, 60);
        }
    }

    private void joinTeam(int team) {
        selectedTeam = team;

        String name = demoNames[demoIndex++ % demoNames.length];

        if (team == 1) {
            teamASupporters++;
            supporters.add(new Supporter(name, 1));
            lastEvent = name + " bergabung TEAM A";
            featuredName = name;
            featuredGift = "JOINED TEAM A";
        } else {
            teamBSupporters++;
            supporters.add(new Supporter(name, 2));
            lastEvent = name + " bergabung TEAM B";
            featuredName = name;
            featuredGift = "JOINED TEAM B";
        }
    }

    private void fireTap() {
        if (selectedTeam == 0) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastTap < 450) {
            combo++;
        } else {
            combo = 1;
        }

        lastTap = now;

        int team = selectedTeam;

        if (team == 1 && teamAAmmo <= 0) {
            lastEvent = "TEAM A KEHABISAN AMMO";
            return;
        }

        if (team == 2 && teamBAmmo <= 0) {
            lastEvent = "TEAM B KEHABISAN AMMO";
            return;
        }

        if (team == 1) {
            teamAAmmo--;
            launchProjectile(true, weaponDamage(teamALevel));
        } else {
            teamBAmmo--;
            launchProjectile(false, weaponDamage(teamBLevel));
        }
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
        float y = h * .45f;

        int color = fromA ? BLUE : RED;

        String weapon = weaponName(fromA ? teamALevel : teamBLevel);

        projectiles.add(new Projectile(
                startX,
                y,
                targetX,
                y,
                color,
                weapon
        ));

        if (fromA) {
            teamBHp -= damage;
            lastEvent = weapon + " TEAM A → TEAM B -" + damage;
        } else {
            teamAHp -= damage;
            lastEvent = weapon + " TEAM B → TEAM A -" + damage;
        }

        addEffect("-" + damage, targetX, y - 20,
                20, 255, 90, 90);

        checkWinner();
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

    private void gift(String gift, int levelUp, int ammo) {
        String name = demoNames[demoIndex++ % demoNames.length];

        if (selectedTeam == 0) {
            selectedTeam = 1;
        }

        boolean teamA = selectedTeam == 1;

        if (teamA) {
            teamAAmmo += ammo;
            teamAGifts++;
            teamALevel = Math.min(5, teamALevel + levelUp);
        } else {
            teamBAmmo += ammo;
            teamBGifts++;
            teamBLevel = Math.min(5, teamBLevel + levelUp);
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
                    17, 255, 210, 60
            );
        }
    }

    private void checkWinner() {
        if (teamAHp <= 0 || teamBHp <= 0) {
            finished = true;

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

        finished = false;
        battleStarted = false;

        lastEvent = "PILIH TIM UNTUK BATTLE BARU";
        featuredName = "";
        featuredGift = "";
    }

    private void updateGame(float dt) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile q = projectiles.get(i);

            q.progress += dt * 2.8f;

            if (q.progress >= 1f) {
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
        float size = 7;
        int color;
        String weapon;

        Projectile(float startX, float startY,
                   float targetX, float targetY,
                   int color, String weapon) {
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.color = color;
            this.weapon = weapon;
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

        Supporter(String name, int team) {
            this.name = name;
            this.team = team;
        }
    }
}
