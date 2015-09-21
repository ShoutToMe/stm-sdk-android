package me.shoutto.sdk;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class RecordingGraphicView extends View {

    private final static int GROWTH_SMOOTHING_FACTOR = 100;
    private ShapeDrawable shapeDrawable;
    private int size;

    public RecordingGraphicView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.RecordingGraphicView,
                0, 0
        );

        try {

            int color = a.getColor(R.styleable.RecordingGraphicView_circlecolor, 0xff000000);
            size = a.getInteger(R.styleable.RecordingGraphicView_circlesize, 300);
            shapeDrawable = new ShapeDrawable(new OvalShape());
            shapeDrawable.getPaint().setColor(color);

        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle();
        }
    }

    public RecordingGraphicView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Log.d("Drawable", String.valueOf(canvas.getHeight()) + " " + String.valueOf(canvas.getWidth()));

        int x = canvas.getWidth() / 2 - (size / 2);
        int y = canvas.getHeight() / 2 - (size / 2);

        shapeDrawable.setBounds(x, y, x + size, y + size);
        shapeDrawable.draw(canvas);
    }

    public void setSize(int size) {
        if (Math.abs(size - this.size) > GROWTH_SMOOTHING_FACTOR) {
            this.size += size > this.size ? GROWTH_SMOOTHING_FACTOR : -GROWTH_SMOOTHING_FACTOR;
        } else {
            this.size = size;
        }
        this.invalidate();
    }
}
