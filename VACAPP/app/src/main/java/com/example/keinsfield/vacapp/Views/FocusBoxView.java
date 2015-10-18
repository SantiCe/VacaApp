package com.example.keinsfield.vacapp.Views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.example.keinsfield.vacapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by Keinsfield on 11-Sep-15.
 */
public class FocusBoxView extends View {

    private static final int MIN_FOCUS_BOX_WIDTH = 50;
    private static final int MIN_FOCUS_BOX_HEIGHT = 20;
    private static Point ScrRes;
    private final Paint paint;
    private final int maskColor;
    private final int frameColor;
    private final int cornerColor;
    private Rect box;
    private OnTouchListener touchListener;

    public FocusBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();

        maskColor = resources.getColor(R.color.focus_box_mask);
        frameColor = resources.getColor(R.color.focus_box_frame);
        cornerColor = resources.getColor(R.color.focus_box_corner);

        this.setOnTouchListener(getTouchListener());
    }

    /*
    Finds the best preview size  supported by the camera given the screen
     */
    public static Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {

        List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(parameters.getSupportedPreviewSizes());

        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        return new Point(supportedPreviewSizes.get(0).width, supportedPreviewSizes.get(0).height);

        /*Point bestSize = null;
        float screenAspectRatio = (float) screenResolution.x / (float) screenResolution.y;

        float diff = Float.POSITIVE_INFINITY;
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            Log.d("Resolution", realWidth + " " + realHeight);
            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                return new Point(realWidth, realHeight);
            }
            float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
            float newDiff = Math.abs(aspectRatio - screenAspectRatio);
            if (newDiff < diff) {
                bestSize = new Point(realWidth, realHeight);
                diff = newDiff;
            }
        }

        if (bestSize == null) {
            Camera.Size defaultSize = parameters.getPreviewSize();
            bestSize = new Point(defaultSize.width, defaultSize.height);
        }
        return bestSize;*/
    }

    public static Point getScreenResolution(Context context) {

        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();

        int width = display.getWidth();
        int height = display.getHeight();

        return new Point(width, height);

    }

    public Rect getBoxRect() {

        if (box == null) {

            ScrRes = getScreenResolution(getContext());

            int width = ScrRes.x * 6 / 7;
            int height = ScrRes.y / 9;

            width = width == 0
                    ? MIN_FOCUS_BOX_WIDTH
                    : width < MIN_FOCUS_BOX_WIDTH ? MIN_FOCUS_BOX_WIDTH : width;

            height = height == 0
                    ? MIN_FOCUS_BOX_HEIGHT
                    : height < MIN_FOCUS_BOX_HEIGHT ? MIN_FOCUS_BOX_HEIGHT : height;

            int left = (ScrRes.x - width) / 2;
            int top = (ScrRes.y - height) / 2;

            box = new Rect(left, top, left + width, top + height);
        }

        return box;
    }

    public Rect getBox() {
        return box;
    }

    private void updateBoxRect(int dW, int dH) {

        int newWidth = (box.width() + dW > ScrRes.x - 4 || box.width() + dW < MIN_FOCUS_BOX_WIDTH) ? 0 : box.width() + dW;

        int newHeight = (box.height() + dH > ScrRes.y - 4 || box.height() + dH < MIN_FOCUS_BOX_HEIGHT) ? 0 : box.height() + dH;

        int xCenter = box.centerX();

        int yCenter = box.centerY();

        int leftOffset = (newWidth) / 2;

        int topOffset = (newHeight) / 2;

        if (newWidth < MIN_FOCUS_BOX_WIDTH || newHeight < MIN_FOCUS_BOX_HEIGHT)
            return;

        box = new Rect(xCenter - leftOffset, yCenter - topOffset, xCenter + leftOffset, yCenter + topOffset);
    }

    /*
    The following method manages the drag and resize logic on the box view
     */
    private OnTouchListener getTouchListener() {

        if (touchListener == null)
            touchListener = new OnTouchListener() {

                int lastX = -1;
                int lastY = -1;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = (int) event.getX();
                            lastY = (int) event.getY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            int currentX = (int) event.getX();
                            int currentY = (int) event.getY();
                            try {
                                Rect rect = getBoxRect();
                                final int BUFFER = 50;
                                final int BIG_BUFFER = 60;

                                if (lastX >= 0) {
                                    if (((currentX >= rect.centerX() - BIG_BUFFER
                                            && currentX <= rect.centerX() + BIG_BUFFER)
                                            || (lastX >= rect.centerX() - BIG_BUFFER
                                            && lastX <= rect.centerX() + BIG_BUFFER))
                                            && ((currentY <= rect.centerY() + BIG_BUFFER
                                            && currentY >= rect.centerY() - BIG_BUFFER)
                                            || (lastY <= rect.centerY() + BIG_BUFFER
                                            && lastY >= rect.centerY() - BIG_BUFFER))) { // if the touch was generated at the center: drag the view
                                        int x = currentX - lastX;
                                        int y = currentY - lastY;
                                        box = new Rect(rect.left + x, rect.top + y, rect.right + x, rect.bottom + y);
                                    } else if (((currentX >= rect.left - BIG_BUFFER
                                            && currentX <= rect.left + BIG_BUFFER)
                                            || (lastX >= rect.left - BIG_BUFFER
                                            && lastX <= rect.left + BIG_BUFFER))
                                            && ((currentY <= rect.top + BIG_BUFFER
                                            && currentY >= rect.top - BIG_BUFFER)
                                            || (lastY <= rect.top + BIG_BUFFER
                                            && lastY >= rect.top - BIG_BUFFER))) { //Top left corner: adjust both top and right sides
                                        updateBoxRect(2 * (lastX - currentX),
                                                2 * (lastY - currentY));
                                    } else if (((currentX >= rect.right - BIG_BUFFER
                                            && currentX <= rect.right + BIG_BUFFER)
                                            || (lastX >= rect.right - BIG_BUFFER
                                            && lastX <= rect.right + BIG_BUFFER))
                                            && ((currentY <= rect.top + BIG_BUFFER
                                            && currentY >= rect.top - BIG_BUFFER)
                                            || (lastY <= rect.top + BIG_BUFFER
                                            && lastY >= rect.top - BIG_BUFFER))) {
                                        // Top right corner: adjust both top and right sides
                                        updateBoxRect(2 * (currentX - lastX),
                                                2 * (lastY - currentY));
                                    } else if (((currentX >= rect.left - BIG_BUFFER
                                            && currentX <= rect.left + BIG_BUFFER)
                                            || (lastX >= rect.left - BIG_BUFFER
                                            && lastX <= rect.left + BIG_BUFFER))
                                            && ((currentY <= rect.bottom + BIG_BUFFER
                                            && currentY >= rect.bottom - BIG_BUFFER)
                                            || (lastY <= rect.bottom + BIG_BUFFER
                                            && lastY >= rect.bottom - BIG_BUFFER))) {
                                        // Bottom left corner: adjust both bottom and left sides
                                        updateBoxRect(2 * (lastX - currentX),
                                                2 * (currentY - lastY));
                                    } else if (((currentX >= rect.right - BIG_BUFFER
                                            && currentX <= rect.right + BIG_BUFFER)
                                            || (lastX >= rect.right - BIG_BUFFER
                                            && lastX <= rect.right + BIG_BUFFER))
                                            && ((currentY <= rect.bottom + BIG_BUFFER
                                            && currentY >= rect.bottom - BIG_BUFFER)
                                            || (lastY <= rect.bottom + BIG_BUFFER
                                            && lastY >= rect.bottom - BIG_BUFFER))) {
                                        // Bottom right corner: adjust both bottom and right sides
                                        updateBoxRect(2 * (currentX - lastX),
                                                2 * (currentY - lastY));
                                    } else if (((currentX >= rect.left - BUFFER
                                            && currentX <= rect.left + BUFFER)
                                            || (lastX >= rect.left - BUFFER
                                            && lastX <= rect.left + BUFFER))
                                            && ((currentY <= rect.bottom
                                            && currentY >= rect.top)
                                            || (lastY <= rect.bottom
                                            && lastY >= rect.top))) {
                                        // Adjusting left side: event falls within BUFFER pixels of
                                        // left side, and between top and bottom side limits
                                        updateBoxRect(2 * (lastX - currentX), 0);
                                    } else if (((currentX >= rect.right - BUFFER
                                            && currentX <= rect.right + BUFFER)
                                            || (lastX >= rect.right - BUFFER
                                            && lastX <= rect.right + BUFFER))
                                            && ((currentY <= rect.bottom
                                            && currentY >= rect.top)
                                            || (lastY <= rect.bottom
                                            && lastY >= rect.top))) {
                                        // Adjusting right side: event falls within BUFFER pixels of
                                        // right side, and between top and bottom side limits
                                        updateBoxRect(2 * (currentX - lastX), 0);
                                    } else if (((currentY <= rect.top + BUFFER
                                            && currentY >= rect.top - BUFFER)
                                            || (lastY <= rect.top + BUFFER
                                            && lastY >= rect.top - BUFFER))
                                            && ((currentX <= rect.right
                                            && currentX >= rect.left)
                                            || (lastX <= rect.right
                                            && lastX >= rect.left))) {
                                        // Adjusting top side: event falls within BUFFER pixels of
                                        // top side, and between left and right side limits
                                        updateBoxRect(0, 2 * (lastY - currentY));
                                    } else if (((currentY <= rect.bottom + BUFFER
                                            && currentY >= rect.bottom - BUFFER)
                                            || (lastY <= rect.bottom + BUFFER
                                            && lastY >= rect.bottom - BUFFER))
                                            && ((currentX <= rect.right
                                            && currentX >= rect.left)
                                            || (lastX <= rect.right
                                            && lastX >= rect.left))) {
                                        updateBoxRect(0, 2 * (currentY - lastY));
                                    }
                                }
                            } catch (NullPointerException e) {
                            }
                            v.invalidate();
                            lastX = currentX;
                            lastY = currentY;
                            return true;
                        case MotionEvent.ACTION_UP:
                            lastX = -1;
                            lastY = -1;
                            return true;
                    }
                    return false;
                }
            };

        return touchListener;
    }

    @Override
    public void onDraw(Canvas canvas) {

        Rect frame = getBoxRect();

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        paint.setAlpha(0);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(frameColor);
        canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
        canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
        canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
        canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);

        paint.setColor(cornerColor);
        canvas.drawCircle(frame.left - 32, frame.top - 32, 32, paint);
        canvas.drawCircle(frame.right + 32, frame.top - 32, 32, paint);
        canvas.drawCircle(frame.left - 32, frame.bottom + 32, 32, paint);
        canvas.drawCircle(frame.right + 32, frame.bottom + 32, 32, paint);
    }

}
