package com.matburt.mobileorg.Gui.Wizard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;

import com.matburt.mobileorg.R;

public class WizardView extends HorizontalScrollView implements
		View.OnTouchListener {
	private static final String TAG = "PageFlipView";
	private static final int SWIPE_THRESHOLD_VELOCITY = 50;

	public static final int FIRST_PAGE = 0;
	public static final int MIDDLE_PAGE = 1;
	public static final int LAST_PAGE = 2;	

	private GestureDetector mGestureDetector;
	private WideLinearLayout container;
	private NextPageButtonListener nextPageButtonListener;
	private PreviousPageButtonListener previousPageButtonListener;
	
	private int maxScrollX;
	private int maxEnabledPage = -1;
	private int currentPage = 0;

	public WizardView(Context context) {
		super(context);
	}

	public WizardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		nextPageButtonListener = new NextPageButtonListener();
		previousPageButtonListener = new PreviousPageButtonListener();
		// see
		// http://blog.velir.com/index.php/2010/11/17/android-snapping-horizontal-scroll/
		// setup page swiping
		mGestureDetector = new GestureDetector(getContext(),
				new PageSwipeDetector());
		setOnTouchListener(this);
	}

	// Make the child views the same size as the screen
	@Override
	protected void onMeasure(int w, int h) {
		int width = MeasureSpec.getSize(w);
		// Tell screen width to our only child
		container.setWidth(width);
		// default to w and h given in XML
		super.onMeasure(w, h);
	}


	@Override
	public void onFinishInflate() {
		container = (WideLinearLayout) findViewById(R.id.wizard_container);
	}

	private static void setButtonState(Button b, boolean state,
			View.OnClickListener e) {
		b.setOnClickListener(e);
		b.setEnabled(state);
	}

	public void enableNextButton(Button b) {
		b.setOnClickListener(nextPageButtonListener);
		b.setEnabled(true);
	}

	// starting from given page, disable all next buttons, and disable
	// swiping to the right.
	public void disableAllNextActions(int page) {
		// first disable next buttons
		if (container != null)
			for (int i = page; i < container.getChildCount(); i++) {
				// get the pageview container
				View pageContainer = (View) container.getChildAt(i);
				// last page doesn't have a next button
				View nextButton = pageContainer
						.findViewById(R.id.wizard_next_button);
				if (nextButton != null)
					nextButton.setEnabled(false);
			}
		// user can't scroll past current page
		maxScrollX = page * getMeasuredWidth();
	}

	// disable/enable all buttons for given page
	public void setNavButtonStateOnPage(int page, boolean state, int page_type) {
		// get the pageview container
		View pageContainer = (View) container.getChildAt(page);
		Button prevButton = (Button) pageContainer
				.findViewById(R.id.wizard_previous_button);
		Button nextButton = (Button) pageContainer
				.findViewById(R.id.wizard_next_button);
		switch (page_type) {
		case FIRST_PAGE:
			setButtonState(nextButton, state, nextPageButtonListener);
			break;
		case MIDDLE_PAGE:
			setButtonState(prevButton, state, previousPageButtonListener);
			setButtonState(nextButton, state, nextPageButtonListener);
			break;
		case LAST_PAGE:
			setButtonState(prevButton, state, previousPageButtonListener);
			// Button done=(Button)
			// pageContainer.findViewById(R.id.wizard_done_button);
			// done.setEnabled(false);
			break;
		}
	}

	// enable prev/next buttons for give page and allow scrolling
	// to next page
	public void enablePage(int page) {
		if (page == -1) {
			maxScrollX = 0;
			maxEnabledPage = -1;
			return;
		}
		// figure out page type
		int page_type = MIDDLE_PAGE;
		if (page == getNumberOfPages() - 1)
			page_type = LAST_PAGE;
		else if (page == 0)
			page_type = 0;
		setNavButtonStateOnPage(page, true, page_type);
		maxScrollX = (page + 1) * getMeasuredWidth();
		maxEnabledPage = (page > maxEnabledPage) ? page : maxEnabledPage;
	}

	public int getNumberOfPages() {
		return container.getChildCount();
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int i) {
		currentPage = i;
	}

	// used for orientation change
	public void restoreLastPage() {
		SharedPreferences prefs = ((Activity) getContext()).getPreferences(0);
		currentPage = prefs.getInt("currentPage", 0);
		maxEnabledPage = prefs.getInt("maxEnabledPage", -1);
		// scroll to last loaded page
		post(new Runnable() {
			@Override
			public void run() {
				enablePage(maxEnabledPage);
				scrollTo(currentPage * getMeasuredWidth(), 0);
			}
		});
	}

	// ditto
	public void saveCurrentPage() {
		SharedPreferences prefs = ((Activity) getContext()).getPreferences(0);
		SharedPreferences.Editor editor = prefs.edit();
		// save current page
		editor.putInt("currentPage", getCurrentPage());
		editor.putInt("maxEnabledPage", maxEnabledPage);
		editor.commit();
	}

	// adds a new page with the given resource id
	public void addPage(int resId) {
		View v = LayoutInflater.from(getContext()).inflate(resId, null);
		container.addView(v);
	}
	
	public void addPage(View view) {
		container.addView(view);
	}

	// delete all pages after and including given page
	public void removePagesAfter(int page) {
		for (int i = container.getChildCount() - 1; i >= page; i--)
			container.removeViewAt(i);
	}

	// START OF CODE TO HANDLE SWIPING/SCROLLING

	// if scroll more than %25 of screen width, then automagically
	// scroll to next page
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		boolean actionUp = false;
		// If the user swipes
		if (mGestureDetector.onTouchEvent(event))
			return true;
		// else
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_CANCEL:
			int scrollX = getScrollX();
			int featureWidth = getMeasuredWidth();
			// if scroll more than %25 of width, then go to next page
			currentPage = ((scrollX + (featureWidth / 4 * 3)) / featureWidth);
			Log.d(TAG, "scrollX: " + scrollX + " featureWidth: "
					+ (featureWidth / 4 * 3) + " page: " + currentPage);
			int scrollTo = currentPage * featureWidth;
			smoothScrollTo(scrollTo, 0);
			// if you don't return true, a child view will interfere
			// with the scrolling
			actionUp = true;
			break;
		default:
			break;
		}
		return actionUp;
	}

	// Basically, keep HorizontalScrollView from scrolling past current
	// page, until it's OK to do so (ex: user puts password)
	@Override
	public void onScrollChanged(int l, int t, int oldl, int oldt) {
		// Log.d(TAG,"scroll: "+l+", "+oldl+" maxscroll: "+maxScrollX);
		if (l > maxScrollX) {
			// if trying to scroll past maximum allowed, then snap back
			scrollTo(oldl, 0);
			// recalculate current page if necessary
			currentPage = (currentPage * getMeasuredWidth() == maxScrollX) ? --currentPage
					: currentPage;
		} else
			super.onScrollChanged(l, t, oldl, oldt);
	}

	class NextPageButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			scrollRight();
		}
	}

	class PreviousPageButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			scrollLeft();
		}
	}

	// hide keyboard if showing
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) ((Activity) getContext())
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getWindowToken(), 0);
	}

	private void scrollRight() {
		hideKeyboard();
		// unfocus login boxes
		View selectedBox = findFocus();
		if (selectedBox != null)
			selectedBox.clearFocus();
		// scroll
		int featureWidth = getMeasuredWidth();
		currentPage = (currentPage < (container.getChildCount() - 1)) ? currentPage + 1
				: container.getChildCount() - 1;
		smoothScrollTo(currentPage * featureWidth, 0);

	}

	private void scrollLeft() {
		hideKeyboard();
		// unfocus login boxes
		View selectedBox = findFocus();
		if (selectedBox != null)
			selectedBox.clearFocus();
		// scroll
		int featureWidth = getMeasuredWidth();
		currentPage = (currentPage > 0) ? currentPage - 1 : 0;
		smoothScrollTo(currentPage * featureWidth, 0);
	}

	private class PageSwipeDetector extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (velocityX < 0 && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				scrollRight();
				return true;
			}
			// left to right
			else if (velocityX > 0
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				scrollLeft();
				return true;
			}
			return false;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
	}
}
