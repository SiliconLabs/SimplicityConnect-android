package com.siliconlabs.bledemo.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.utils.SignalStrength;

public class KeyFobSignalStrength extends ImageView {
    private Paint paintLightGrayStroke;
    private Paint paintMediumGrayFilled;
    private Paint paintMediumGrayStroke;
    private Paint paintDarkGrayStroke;

    private SignalStrength signalStrength = SignalStrength.UNKNOWN;

    public KeyFobSignalStrength(Context context) {
        super(context);
        init();
    }

    public KeyFobSignalStrength(Context context, AttributeSet set) {
        super(context, set);
        init();
    }

    private void init() {
        paintLightGrayStroke = new Paint();
        paintLightGrayStroke.setColor(getResources().getColor(R.color.key_fob_signal_strength_indicator_light_gray));
        paintLightGrayStroke.setStyle(Paint.Style.STROKE);
        paintLightGrayStroke.setStrokeCap(Paint.Cap.ROUND);
        paintLightGrayStroke.setAntiAlias(true);


        paintMediumGrayStroke = new Paint();
        paintMediumGrayStroke.setColor(getResources().getColor(R.color.key_fob_signal_strength_indicator_medium_gray));
        paintMediumGrayStroke.setStyle(Paint.Style.STROKE);
        paintMediumGrayStroke.setAntiAlias(true);

        paintMediumGrayFilled = new Paint();
        paintMediumGrayFilled.setColor(getResources().getColor(R.color.key_fob_signal_strength_indicator_medium_gray));
        paintMediumGrayFilled.setStyle(Paint.Style.FILL);

        paintDarkGrayStroke = new Paint();
        paintDarkGrayStroke.setColor(getResources().getColor(R.color.key_fob_signal_strength_indicator_dark_gray));
        paintDarkGrayStroke.setStyle(Paint.Style.STROKE);
        paintDarkGrayStroke.setStrokeCap(Paint.Cap.ROUND);
        paintDarkGrayStroke.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float canvasShorterEdge = canvas.getWidth() < canvas.getHeight() ? canvas.getWidth() : canvas.getHeight();
        float radiusRoundedCorner = canvasShorterEdge / 30;
        float padding = radiusRoundedCorner / 2;
        RectF rect = new RectF(padding, padding + canvasShorterEdge / 2, -padding + canvasShorterEdge / 2, -padding + canvasShorterEdge);
        paintMediumGrayStroke.setStrokeWidth(radiusRoundedCorner);
        canvas.drawRoundRect(rect, canvasShorterEdge / 10, canvasShorterEdge / 10, paintMediumGrayStroke);

        canvas.drawCircle(.7f * canvasShorterEdge / 2, 1.3f * canvasShorterEdge / 2, .1f * canvasShorterEdge / 2, paintMediumGrayFilled);

        float centerX = .85f * canvasShorterEdge / 2;
        float centerY = 1.15f * canvasShorterEdge / 2;

        float sectorOneRadius = 2.6f * (canvasShorterEdge / 2f) / 5f;
        float sectorTwoRadius = 3.75f * (canvasShorterEdge / 2f) / 5f;
        float sectorThreeRadius = 4.9f * (canvasShorterEdge / 2f) / 5f;

        paintLightGrayStroke.setStrokeWidth(1.25f * radiusRoundedCorner);
        paintDarkGrayStroke.setStrokeWidth(1.25F * radiusRoundedCorner);
        //left, top, right, bottom
        RectF rectSmallSignalBar = new RectF(centerX - sectorOneRadius, centerY - sectorOneRadius, centerX + sectorOneRadius, centerY + sectorOneRadius);
        canvas.drawArc(rectSmallSignalBar, 270f, 90f, false, paintLightGrayStroke);
        RectF rectMedSignalBar = new RectF(centerX - sectorTwoRadius, centerY - sectorTwoRadius, centerX + sectorTwoRadius, centerY + sectorTwoRadius);
        canvas.drawArc(rectMedSignalBar, 270f, 90f, false, paintLightGrayStroke);
        RectF rectLargeSignalBar = new RectF(centerX - sectorThreeRadius, centerY - sectorThreeRadius, centerX + sectorThreeRadius, centerY + sectorThreeRadius);
        canvas.drawArc(rectLargeSignalBar, 270f, 90f, false, paintLightGrayStroke);

        //draw current signal strength
        switch (signalStrength) {
            case WEAK:
                rect = rectSmallSignalBar;
                canvas.drawArc(rect, 270f, 90f, false, paintDarkGrayStroke);
                break;
            case AVERAGE:
                rect = rectSmallSignalBar;
                canvas.drawArc(rect, 270f, 90f, false, paintDarkGrayStroke);
                rect = rectMedSignalBar;
                canvas.drawArc(rect, 270f, 90f, false, paintDarkGrayStroke);
                break;
            case GOOD:
                rect = rectSmallSignalBar;
                canvas.drawArc(rect, 270f, 90f, false, paintDarkGrayStroke);
                rect = rectMedSignalBar;
                canvas.drawArc(rect, 270f, 90f, false, paintDarkGrayStroke);
                rect = rectLargeSignalBar;
                canvas.drawArc(rect, 270f, 90f, false, paintDarkGrayStroke);
                break;
        }
    }


    public void setSignalStrength(SignalStrength signalStrength) {
        this.signalStrength = signalStrength;
        postInvalidate();
    }
}
