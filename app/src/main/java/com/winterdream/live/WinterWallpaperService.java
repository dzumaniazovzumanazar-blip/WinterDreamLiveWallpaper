package com.winterdream.live;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Random;

public class WinterWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new SnowEngine();
    }

    private final class SnowEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final ArrayList<Flake> flakes = new ArrayList<>();
        private int width = 1;
        private int height = 1;
        private boolean visible;
        private float touchX = -1000f;
        private float touchY = -1000f;
        private long touchUntil;

        private final Runnable drawTask = new Runnable() {
            @Override public void run() {
                drawFrame();
                if (visible) handler.postDelayed(this, 33L);
            }
        };

        SnowEngine() {
            setTouchEventsEnabled(true);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            super.onSurfaceChanged(holder, format, w, h);
            width = Math.max(1, w);
            height = Math.max(1, h);
            createFlakes();
            drawFrame();
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            handler.removeCallbacks(drawTask);
            if (visible) {
                drawFrame();
                handler.post(drawTask);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            handler.removeCallbacks(drawTask);
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            visible = false;
            handler.removeCallbacks(drawTask);
            super.onDestroy();
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {
                touchX = event.getX();
                touchY = event.getY();
                touchUntil = System.currentTimeMillis() + 650L;
                for (Flake flake : flakes) {
                    float dx = flake.x - touchX;
                    float dy = flake.y - touchY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance < 145f) {
                        float force = (145f - distance) / 145f;
                        float safeDistance = Math.max(1f, distance);
                        flake.vx += (dx / safeDistance) * force * 7.5f;
                        flake.vy += (dy / safeDistance) * force * 5.0f - 1.5f;
                    }
                }
                drawFrame();
            }
            super.onTouchEvent(event);
        }

        private void createFlakes() {
            flakes.clear();
            int count = Math.max(65, Math.min(150, (width * height) / 10500));
            for (int i = 0; i < count; i++) {
                flakes.add(new Flake(
                        random.nextFloat() * width,
                        random.nextFloat() * height,
                        1.1f + random.nextFloat() * 3.0f,
                        0.8f + random.nextFloat() * 2.0f,
                        (random.nextFloat() - 0.5f) * 0.9f
                ));
            }
        }

        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;
                width = canvas.getWidth();
                height = canvas.getHeight();
                drawScene(canvas);
            } catch (RuntimeException ignored) {
                // The launcher can recreate the wallpaper surface while changing screens.
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (RuntimeException ignored) {
                        // Surface was already released by the system.
                    }
                }
            }
        }

        private void drawScene(Canvas c) {
            float w = c.getWidth();
            float h = c.getHeight();

            paint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{Color.rgb(8, 20, 43), Color.rgb(24, 48, 76), Color.rgb(67, 91, 113)},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            // Soft winter moon and its halo.
            paint.setShader(new RadialGradient(w * 0.76f, h * 0.22f, Math.min(w, h) * 0.23f,
                    new int[]{0x35D9EDFF, 0x12D9EDFF, 0x00D9EDFF},
                    null, Shader.TileMode.CLAMP));
            c.drawCircle(w * 0.76f, h * 0.22f, Math.min(w, h) * 0.23f, paint);
            paint.setShader(null);
            paint.setColor(0xFFE8F2FF);
            c.drawCircle(w * 0.76f, h * 0.22f, Math.min(w, h) * 0.052f, paint);

            // Tiny stars.
            paint.setColor(0x99EAF4FF);
            for (int i = 0; i < 42; i++) {
                float sx = (i * 83.17f % 997f) / 997f * w;
                float sy = (i * 47.31f % 503f) / 503f * h * 0.58f;
                c.drawCircle(sx, sy, (i % 3 == 0) ? 1.5f : 0.8f, paint);
            }

            // Distant hills.
            paint.setColor(0xFF243B53);
            c.drawOval(-w * 0.25f, h * 0.59f, w * 0.72f, h * 0.94f, paint);
            c.drawOval(w * 0.28f, h * 0.58f, w * 1.2f, h * 0.96f, paint);

            // Snowy foreground.
            paint.setColor(0xFFE4EDF3);
            c.drawOval(-w * 0.18f, h * 0.79f, w * 1.18f, h * 1.16f, paint);

            // Pine silhouettes at both edges.
            drawPine(c, w * 0.08f, h * 0.77f, h * 0.31f);
            drawPine(c, w * 0.91f, h * 0.79f, h * 0.38f);
            drawPine(c, w * 0.18f, h * 0.83f, h * 0.20f);

            // Warm little cabin.
            float cabinX = w * 0.48f;
            float cabinY = h * 0.70f;
            float cabinW = w * 0.28f;
            float cabinH = h * 0.17f;
            paint.setColor(0xFF3B3540);
            c.drawRect(cabinX, cabinY, cabinX + cabinW, cabinY + cabinH, paint);
            paint.setColor(0xFF2A2935);
            android.graphics.Path roof = new android.graphics.Path();
            roof.moveTo(cabinX - cabinW * 0.08f, cabinY + 5);
            roof.lineTo(cabinX + cabinW * 0.5f, cabinY - cabinH * 0.43f);
            roof.lineTo(cabinX + cabinW * 1.08f, cabinY + 5);
            roof.close();
            c.drawPath(roof, paint);
            paint.setColor(0xFFFFD38A);
            c.drawRoundRect(cabinX + cabinW * 0.12f, cabinY + cabinH * 0.22f,
                    cabinX + cabinW * 0.37f, cabinY + cabinH * 0.62f, 5, 5, paint);
            c.drawRoundRect(cabinX + cabinW * 0.62f, cabinY + cabinH * 0.22f,
                    cabinX + cabinW * 0.87f, cabinY + cabinH * 0.62f, 5, 5, paint);
            paint.setColor(0xFF6A4A3C);
            c.drawRect(cabinX + cabinW * 0.43f, cabinY + cabinH * 0.48f,
                    cabinX + cabinW * 0.58f, cabinY + cabinH, paint);

            // Snowfall.
            long now = System.currentTimeMillis();
            for (Flake flake : flakes) {
                flake.update(w, h);
                int alpha = Math.min(235, 115 + (int) (flake.radius * 35));
                paint.setColor(Color.argb(alpha, 245, 250, 255));
                c.drawCircle(flake.x, flake.y, flake.radius, paint);
            }

            // Gentle glow where the screen was touched.
            if (now < touchUntil) {
                float remaining = (touchUntil - now) / 650f;
                paint.setShader(new RadialGradient(touchX, touchY, 95f,
                        new int[]{0x55E7F6FF, 0x20E7F6FF, 0x00E7F6FF},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
                c.drawCircle(touchX, touchY, 95f * (1.1f - remaining * 0.1f), paint);
                paint.setShader(null);
            }
        }

        private void drawPine(Canvas c, float centerX, float baseY, float treeHeight) {
            paint.setColor(0xFF172F3D);
            c.drawRect(centerX - 4, baseY - treeHeight * 0.12f,
                    centerX + 4, baseY + 8, paint);
            paint.setColor(0xFF183B42);
            android.graphics.Path tree = new android.graphics.Path();
            tree.moveTo(centerX, baseY - treeHeight);
            tree.lineTo(centerX - treeHeight * 0.34f, baseY - treeHeight * 0.48f);
            tree.lineTo(centerX - treeHeight * 0.20f, baseY - treeHeight * 0.48f);
            tree.lineTo(centerX - treeHeight * 0.42f, baseY - treeHeight * 0.08f);
            tree.lineTo(centerX + treeHeight * 0.42f, baseY - treeHeight * 0.08f);
            tree.lineTo(centerX + treeHeight * 0.20f, baseY - treeHeight * 0.48f);
            tree.lineTo(centerX + treeHeight * 0.34f, baseY - treeHeight * 0.48f);
            tree.close();
            c.drawPath(tree, paint);
        }

        private final class Flake {
            float x, y, radius, speed, drift, vx, vy;

            Flake(float x, float y, float radius, float speed, float drift) {
                this.x = x;
                this.y = y;
                this.radius = radius;
                this.speed = speed;
                this.drift = drift;
            }

            void update(float w, float h) {
                x += drift + vx;
                y += speed + vy;
                vx *= 0.94f;
                vy *= 0.94f;
                if (x < -10) x = w + 10;
                if (x > w + 10) x = -10;
                if (y > h + 10) {
                    y = -10;
                    x = random.nextFloat() * w;
                    vx = 0;
                    vy = 0;
                }
            }
        }
    }

}
