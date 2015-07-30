package com.coyhumphrey.android.assignment5;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by coy on 5/30/2015.
 */
public class DrainView extends View{

    private static final String TAG = "DrainView";
    // Bounds of the screen
    private int top, bottom, left, right;
    // Width and height of usable area
    private int width, height;
    // Time values for calculating time between calls
    private long lastTime, now;
    // The tomato, drain and plate objects
    private Tomato tomato;
    private Drain drain;
    private ArrayList<Plate> plates;
    // Used to draw the tomato drain and plates
    private Paint tomatoPaint, drainPaint, platePaint;

    // Used in onDraw to determine if we need to initialize the values of top,bottom,..., width, height
    // For some reason these values default to 0 until onDraw is called, so they can't be initialized earlier
    private boolean firstUpdate;

    public DrainView (Context context) {
        super(context);
        initDrain();
    }

    public DrainView (Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initDrain();
    }

    public DrainView (Context context, AttributeSet attributeSet, int defaultStyle) {
        super(context, attributeSet, defaultStyle);
        initDrain();
    }

    private void initDrain() {
        setFocusable(true);

        Resources r = this.getResources();

        // Initialize the tomato painter to paint filled in circles of tomato color
        tomatoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tomatoPaint.setColor(r.getColor(R.color.tomato_color));
        tomatoPaint.setStrokeWidth(1);
        tomatoPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        // Initialize the drain painter to paint filled in circles of drain color
        drainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        drainPaint.setColor(r.getColor(R.color.drain_color));
        drainPaint.setStrokeWidth(1);
        drainPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        // Initialize the plate painter to paint lines of plate color
        platePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        platePaint.setColor(r.getColor(R.color.plate_color));
        platePaint.setStrokeWidth(4);
        platePaint.setStyle(Paint.Style.STROKE);

        // Initialize our arraylist of plates
        plates = new ArrayList<Plate>();

        // firstUpdate should be true because we have not called onDraw yet
        firstUpdate = true;
        // Initialize our time values
        lastTime = AnimationUtils.currentAnimationTimeMillis();
        now = lastTime;
    }


    private class Tomato {
        // Tomato keeps track of its position, radius and velocities
        private float x, y;
        private float rad;
        private float vx, vy;
        // These values can be adjusted to change game physics
        // Alpha is percentage of velocity kept each update
        // Beta is multiplied by accelerometer reading to get acceleration
        // Gamma is bounciness of the tomato, the amount of velocity kept when bouncing off of walls
        private final float alpha = .5f, beta = 100, gamma = .7f;

        public Tomato () {
            x = width/2f;
            y = height/20f;
            rad = Math.min (width/20f, height/20f);
        }

        public Tomato (float _x, float _y, float r) {
            this.x = _x;
            this.y = _y;
            this.rad = r;
            this.vx = this.vy = 0;
            Log.i(TAG, "x = " + x + " y = " + y + " r = " + r);
        }

        public void update() {
            // initialize values
            float xprev = x, yprev = y;
            // Time in milliseconds, so divide by 1000 to get seconds
            float delta = (now - lastTime) / 1000f;
            delta = (float) Math.min (delta, .02);
            float ix = beta * MainActivity.accelx;
            float iy = beta * MainActivity.accely;

            // Distance calculations
            // X = x0 + v * t + a*t^2/2
            x = x + vx * delta + (ix * delta * delta) / 2;
            y = y + vy * delta + (iy * delta * delta) / 2;
            // Velocity calculations
            // V = v0 + at (- some percentage of v0 to emulate friction)
            vx = vx - vx * (1 - alpha) * delta + ix * delta;
            vy = vy - vy * (1 - alpha) * delta + iy * delta;

            // Collision detection
            for (Plate p : plates) {
                // Collision with horizontal plate
                if (p.startx <= x && x <= p.endx && Math.abs(y - p.endy) < rad) {
                    // Move the tomato slightly to avoid getting stuck in the plate
                    // If we're moving down, we're colliding with top of the plate so we should move
                    // slightly up, the opposite is true if we're moving up
                    y = vy < 0 ? p.endy + rad + 1 : p.endy - rad - 1;
                    // Reverse the velocity and multiply it by gamma (probably < 1)
                    vy *= -gamma;
                }
                // Collision with endpoint of plate
                // We rely a bit on the short circuit or to simplify code
                if ((p.endx < x && Math.sqrt((p.endx-x)*(p.endx-x) + (p.endy-y)*(p.endy-y)) < rad) ||
                        (p.startx > x && Math.sqrt((p.startx-x) * (p.startx-x) + (p.starty-y)*(p.starty-y)) < rad)) {
                    // In the first  case, we're colliding with the right side of a plate, so we
                    // use the end point. In the second case we colide with the left side so we
                    // use the start point
                    float px, py;
                    if (Math.abs (x - p.endx) < Math.abs (x - p.startx)) {
                        px = p.endx; py = p.endy;
                    }
                    else {
                        px = p.startx; py = p.starty;
                    }
                    // Here we calculate our vectors, their magnitude, and the dot product of the two
                    double vectorx = x - px;
                    double vectory = y - py;
                    double lenDistanceVector = Math.sqrt (vectorx * vectorx + vectory * vectory);
                    double dotProduct = vectorx * vx + vectory * vy;
                    // Compose velocity onto distance vector
                    double scalar = dotProduct/(lenDistanceVector * lenDistanceVector);
                    // vd is parallel component vp is perpendicular component
                    double vdx = scalar * vectorx;
                    double vdy = scalar * vectory;
                    double vpx = vectorx - vdx;
                    double vpy = vectory - vdy;
                    // V is vp - vd
                    vx = gamma * (float)(vpx - vdx);
                    vy = gamma * (float)(vpy - vdy);
                }
            }
            // Collisions with walls
            // Same logic as in collision with horizontal plates
            if (x + rad >= right) {x = right - rad - 1; vx *= -gamma;}
            if (x - rad <= left) {x = left + rad + 1; vx *= -gamma;}
            if (y + rad >= bottom) {y = bottom - rad - 1; vy *= -gamma;}
            if (y - rad <= top) {y = top + rad + 1; vy *= -gamma;}
        }

        // If distance between center of tomato and drain is less than difference in radiuses
        // then the tomato is within the drain
        public boolean withinDrain () {
            return (Math.sqrt((drain.x-x)*(drain.x-x) + (drain.y-y)*(drain.y-y)) < drain.rad - rad);
        }
    }

    private class Plate {
        // Class just encapsulates plates start and end coordinates
        private float startx, endx, starty, endy;

        public Plate (float sx, float sy, float ex, float ey) {
            startx = sx;
            endx = ex;
            starty = sy;
            endy = ey;
        }
    }

    private class Drain {
        // Class just encapsulates drains coordinates and radius
        private float x, y, rad;

        public Drain (float _x, float _y, float r) {
            x = _x;
            y = _y;
            rad = r;
        }
    }

    private Runnable animator = new Runnable() {
        @Override
        public void run() {
            // Update time values
            lastTime = now;
            now = AnimationUtils.currentAnimationTimeMillis();

            // Update tomato
            tomato.update();
            // Check for win condition and display toast + restart game if won
            if (tomato.withinDrain()) {
                tomato = new Tomato();
                Context context = getContext();
                CharSequence text = "You win!";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }

            // Trigger a redraw
            invalidate();
            // Queue another call to this method for 20ms in the future
            removeCallbacks(animator);
            postDelayed(animator, 20);
        }
    };

    protected void onDraw (Canvas canvas) {
        // For some reason, getWidth/height/padding/etc seem to only return 0 unless they are called
        // from within onDraw. So, we're initializing these values in onDraw.
        if (firstUpdate) {
            // Initialize top, left... taking padding into account
            top = getPaddingTop();
            left = getPaddingLeft();
            right = getWidth() - getPaddingRight();
            bottom = getHeight() - getPaddingBottom();
            Log.i(TAG, "top = " + top + "left = " + left + "right = " + right + " bottom = " + bottom);
            // Initialize width and height taking the padding in top,left... into account
            width = right - left;
            height = bottom - top;
            Log.i(TAG, "width = " + width + " height = " + height);
            // Initialize objects now that we have valid height and width numbers
            tomato = new Tomato ();
            drain = new Drain (left + width/2f, bottom-tomato.rad * 2f + 1, tomato.rad * 2f);
            plates.add (new Plate (left, top + height/3f, left + width * 2/3f, top + height/3f));
            plates.add (new Plate (left + width/3f, top + height*2/3f, right, top + height*2/3f));
            // Start the animator
            removeCallbacks(animator);
            post(animator);
            // We now have valid values for top,left..., width, height
            firstUpdate = false;
        }
        // Draw our objects
        canvas.drawCircle(tomato.x, tomato.y, tomato.rad, tomatoPaint);
        canvas.drawCircle(drain.x, drain.y, drain.rad, drainPaint);
        for (Plate p : plates) {
            canvas.drawLine(p.startx, p.starty, p.endx, p.endy, platePaint);
        }
    }
}
