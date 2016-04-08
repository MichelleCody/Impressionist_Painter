package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.text.MessageFormat;
import java.util.Random;
import java.util.UUID;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;
    private float prevX;
    private float prevY;
    private boolean previous_point_flag = false;
    private Bitmap _myBitmap = null;
    private int left = 0, right = 0;
    private int top = 0, bottom = 0;
    private String rotation = "NORMAL";

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    public void setBitmap(Bitmap b) {
        _myBitmap = b;
        Rect r = getBitmapPositionInsideImageView(_imageView);
        _myBitmap = Bitmap.createScaledBitmap(b, r.width(), r.height(), true);
        top = r.top;
        left = r.left;
        bottom = r.bottom;
        right = r.right;
        //Toast.makeText(getContext(), "top: " + top + ", left: " + left + ", bottom: " + bottom + ", right: " + right + ", width: " + _myBitmap.getWidth(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    public void flip() {
        if (rotation == "NORMAL") {
            rotation = "MIRROR";
            Toast.makeText(getContext(), "Image now drawing mirror", Toast.LENGTH_SHORT).show();
        } else if (rotation == "MIRROR"){
            rotation = "UPSIDEDOWN MIRROR";
            Toast.makeText(getContext(), "Image now drawing upside down and mirror", Toast.LENGTH_SHORT).show();
        } else if (rotation == "UPSIDEDOWN MIRROR") {
            rotation = "UPSIDEDOWN";
            Toast.makeText(getContext(), "Image now drawing upside down", Toast.LENGTH_SHORT).show();
        } else {
            rotation = "NORMAL";
            Toast.makeText(getContext(), "Image now drawing normal", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //TODO
        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();
        float changingRadius = 10.0f;

        switch(motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                previous_point_flag = false;
            case MotionEvent.ACTION_MOVE:
                int historySize = motionEvent.getHistorySize();
                for (int i = 0; i < historySize; i++) {

                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    //check bounds here
                    if (touchX < left || touchX > right || touchY < top || touchY > bottom || (right - (int) touchX) >= _myBitmap.getWidth()  || (bottom - (int) touchY >= _myBitmap.getHeight())) {

                    } else {
                        //setting brush size based on velocity
                        if (previous_point_flag) {
                            float distance = (float) Math.sqrt((curTouchX - prevX) * (curTouchX - prevX) + (curTouchY - prevY) * (curTouchY - prevY));

                            int height = _offScreenBitmap.getHeight();
                            int width = _offScreenBitmap.getWidth();

                            float max_dist = (float) Math.sqrt(height * height + width * width) / 4;
                            float percent = distance / max_dist;

                            changingRadius = (int) (percent * 250);

                        }

                        //_paint.setStrokeWidth(100);
                        int pixel;
                        if (rotation == "MIRROR") {
                            pixel = _myBitmap.getPixel(right - (int) touchX, (int) touchY - top);
                        } else if (rotation == "NORMAL"){
                            pixel = _myBitmap.getPixel((int) touchX - left, (int) touchY - top);
                        } else if (rotation == "UPSIDEDOWN MIRROR"){
                            pixel = _myBitmap.getPixel(right - (int) touchX, bottom - (int) touchY);
                        } else {
                            pixel = _myBitmap.getPixel((int) touchX - left, bottom - (int) touchY);
                        }
                        _paint.setColor(pixel);

                        previous_point_flag = true;

                        // TODO: draw to the offscreen bitmap for historical x,y points
                        if (_brushType == BrushType.SquareSplatter) {
                            _offScreenCanvas.drawRect(touchX, touchY, touchX + changingRadius, touchY + changingRadius, _paint);
                        } else if (_brushType == BrushType.CircleSplatter) {
                            _offScreenCanvas.drawCircle(touchX, touchY, changingRadius, _paint);
                        } else if (_brushType == BrushType.Square) {
                            _offScreenCanvas.drawRect(touchX, touchY, touchX + 20.0f, touchY + 20.0f, _paint);
                        } else if (_brushType == BrushType.Circle){
                            _offScreenCanvas.drawCircle(touchX, touchY, 20.0f, _paint);
                        } else {
                            _offScreenCanvas.drawLine(touchX, touchY, touchX + 20.0f, touchY + 20.0f, _paint);
                        }
                    }
                    prevX = curTouchX;
                    prevY = curTouchY;
                }
                if (curTouchX < left || curTouchX > right || curTouchY < top || curTouchY > bottom || (right - (int) curTouchX) >= _myBitmap.getWidth() || (bottom - (int) curTouchY >= _myBitmap.getHeight())) {

                } else {
                    int pixel;
                    if (rotation == "MIRROR") {
                        pixel = _myBitmap.getPixel(right - (int) curTouchX, (int) curTouchY - top);
                    } else if (rotation == "NORMAL"){
                        pixel = _myBitmap.getPixel((int) curTouchX - left, (int) curTouchY - top);
                    } else if (rotation == "UPSIDEDOWN MIRROR"){
                        pixel = _myBitmap.getPixel(right - (int) curTouchX, bottom - (int) curTouchY);
                    } else {
                        pixel = _myBitmap.getPixel((int) curTouchX - left, bottom - (int) curTouchY);
                    }
                    _paint.setColor(pixel);
                    // TODO: draw to the offscreen bitmap for current x,y point.
                    _offScreenCanvas.drawRect(curTouchX, curTouchY, curTouchX + 10.0f, curTouchY + 10.0f, _paint);
                }
        }
        invalidate();
        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

