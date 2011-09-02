package com.matburt.mobileorg.Settings;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Display;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import com.matburt.mobileorg.R;

public class WizardActivity extends Activity
    implements RadioGroup.OnCheckedChangeListener {

    int syncWebDav, syncDropBox, syncSdCard;
    RadioGroup syncGroup; 

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard);

	//setup horizontal scrollview
	Display display = getWindowManager().getDefaultDisplay(); 
	int screenWidth = display.getWidth();
	int screenHeight = display.getHeight();
	//set width of all setup screens
	// LinearLayout wizardLayout = (LinearLayout) findViewById(R.id.wizard_layout);
	// ScrollView wizardPage;
        // for (int i=0; i<wizardLayout.getChildCount(); i++) {
	//     wizardPage = (ScrollView) wizardLayout.getChildAt(i);
	//     wizardPage.setLayoutParams(new 
	// 			       ViewGroup.LayoutParams(
	// 						      screenWidth, 
	// 						      screenHeight));
	// }

	//get ids and pointers to sync radio buttons
	syncGroup = (RadioGroup) findViewById(R.id.sync_group);
	syncWebDav = ( (RadioButton) 
			findViewById(R.id.sync_webdav) ).getId();
	syncDropBox = ( (RadioButton) 
			findViewById(R.id.sync_dropbox) ).getId();
	syncSdCard = ( (RadioButton) 
			findViewById(R.id.sync_sdcard) ).getId();
	//setup click listener for sync radio group
	syncGroup.setOnCheckedChangeListener(this);
    }

    @Override
    	public void onCheckedChanged(RadioGroup arg, int checkedId) {
    	SharedPreferences appSettings = 
    	    PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	SharedPreferences.Editor editor = appSettings.edit();
    	if ( checkedId == syncWebDav )
    	    editor.putString("syncSource", "webdav");
    	else if ( checkedId == syncDropBox )
    	    editor.putString("syncSource", "dropbox");
    	else if ( checkedId == syncSdCard)
    	    editor.putString("syncSource", "sdcard");
    	editor.commit();
    }

    public class PageView extends View {

	public LabelView(Context context) {
	    super(context);
	    initLabelView();
	}

    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public LabelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLabelView();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.LabelView);

        CharSequence s = a.getString(R.styleable.LabelView_text);
        if (s != null) {
            setText(s.toString());
        }

        // Retrieve the color(s) to be used for this view and apply them.
        // Note, if you only care about supporting a single color, that you
        // can instead call a.getColor() and pass that to setTextColor().
        setTextColor(a.getColor(R.styleable.LabelView_textColor, 0xFF000000));

        int textSize = a.getDimensionPixelOffset(R.styleable.LabelView_textSize, 0);
        if (textSize > 0) {
            setTextSize(textSize);
        }

        a.recycle();
    }

    private final void initLabelView() {
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(16);
        mTextPaint.setColor(0xFF000000);
        setPadding(3, 3, 3, 3);
    }

    /**
     * Sets the text to display in this label
     * @param text The text to display. This will be drawn as one line.
     */
    public void setText(String text) {
        mText = text;
        requestLayout();
        invalidate();
    }

    /**
     * Sets the text size for this label
     * @param size Font size
     */
    public void setTextSize(int size) {
        mTextPaint.setTextSize(size);
        requestLayout();
        invalidate();
    }

    /**
     * Sets the text color for this label.
     * @param color ARGB value for the text
     */
    public void setTextColor(int color) {
        mTextPaint.setColor(color);
        invalidate();
    }

    /**
     * @see android.view.View#measure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = (int) mTextPaint.measureText(mText) + getPaddingLeft()
                    + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mAscent = (int) mTextPaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = (int) (-mAscent + mTextPaint.descent()) + getPaddingTop()
                    + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * Render the text
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(mText, getPaddingLeft(), getPaddingTop() - mAscent, mTextPaint);
    }
}
}

class TestMine {
    
}