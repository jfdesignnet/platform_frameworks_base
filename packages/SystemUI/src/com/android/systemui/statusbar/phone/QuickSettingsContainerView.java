/*
 * Copyright (C) 2012 The Android Open Source Project
 * This code has been modified. Portions copyright (C) 2013, ParanoidAndroid Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.systemui.R;

import java.util.ArrayList;

/**
 *
 */
public class QuickSettingsContainerView extends FrameLayout {

    // The number of columns in the QuickSettings grid
    private int mNumColumns;
    private int mNumFinalColumns;

    // Duplicate number of columns in the QuickSettings grid on landscape view
    private boolean mDuplicateColumnsLandscape;

    // The gap between tiles in the QuickSettings grid
    private float mCellGap;

    private Context mContext;
    private Resources mResources;

    // Default layout transition
    private LayoutTransition mLayoutTransition;

    // Edit mode status
    private boolean mEditModeEnabled;

    // Edit mode changed listener
    private EditModeChangedListener mEditModeChangedListener;

    public interface EditModeChangedListener {
        public abstract void onEditModeChanged(boolean enabled);
    }

    public QuickSettingsContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mResources = getContext().getResources();

        updateResources();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mLayoutTransition = getLayoutTransition();
        mLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
    }

    public void updateResources() {
        mCellGap = mResources.getDimension(R.dimen.quick_settings_cell_gap);
        mNumColumns = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW, 3, UserHandle.USER_CURRENT);
        // do not allow duplication on tablets or any device which do not have
        // flipsettings
        mDuplicateColumnsLandscape = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE,
                1, UserHandle.USER_CURRENT) == 1
                        && mResources.getBoolean(R.bool.config_hasFlipSettingsPanel);
        requestLayout();
    }

    public void updateSpan() {
        Resources r = getContext().getResources();
        for(int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if(v instanceof QuickSettingsTileView) {
                QuickSettingsTileView qs = (QuickSettingsTileView) v;
                if(i < 3) { // Modify span of the first three childs
                    int span = r.getInteger(R.integer.quick_settings_user_time_settings_tile_span);
                    qs.setColumnSpan(span);
                } else {
                    qs.setColumnSpan(1); // One column item
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mDuplicateColumnsLandscape && isLandscape()) {
            mNumFinalColumns = mNumColumns * 2;
        } else {
            mNumFinalColumns = mNumColumns;
        }
        // Calculate the cell width dynamically
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int availableWidth = (int) (width - getPaddingLeft() - getPaddingRight() -
                (mNumFinalColumns - 1) * mCellGap);
        float cellWidth = (float) Math.ceil(((float) availableWidth) / mNumFinalColumns);

        // Update each of the children's widths accordingly to the cell width
        int N = getChildCount();
        int cellHeight = 0;
        int cursor = 0;
        for (int i = 0; i < N; ++i) {
            // Update the child's width
            QuickSettingsTileView v = (QuickSettingsTileView) getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                int colSpan = v.getColumnSpan();
                lp.width = (int) ((colSpan * cellWidth) + (colSpan - 1) * mCellGap);

                if (mNumFinalColumns > 3 && !isLandscape()) {
                    lp.height = (lp.width * mNumFinalColumns - 1) / mNumFinalColumns;
                }

                // Measure the child
                int newWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
                int newHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                v.measure(newWidthSpec, newHeightSpec);

                // Save the cell height
                if (cellHeight <= 0) {
                    cellHeight = v.getMeasuredHeight();
                }
                cursor += colSpan;
            }
        }

        // Set the measured dimensions.  We always fill the tray width, but wrap to the height of
        // all the tiles.
        int numRows = (int) Math.ceil((float) cursor / mNumFinalColumns);
        int newHeight = (int) ((numRows * cellHeight) + ((numRows - 1) * mCellGap)) +
                getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, newHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int N = getChildCount();
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int cursor = 0;

        if (mDuplicateColumnsLandscape && isLandscape()) {
            mNumFinalColumns = mNumColumns * 2;
        } else {
            mNumFinalColumns = mNumColumns;
        }

        for (int i = 0; i < N; ++i) {
            QuickSettingsTileView v = (QuickSettingsTileView) getChildAt(i);
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (v.getVisibility() != GONE) {
                int col = cursor % mNumFinalColumns;
                int colSpan = v.getColumnSpan();
                int row = cursor / mNumFinalColumns;

                // Push the item to the next row if it can't fit on this one
                if ((col + colSpan) > mNumFinalColumns) {
                    x = getPaddingLeft();
                    y += lp.height + mCellGap;
                    row++;
                }

                // Layout the container
                v.layout(x, y, x + lp.width, y + lp.height);

                // Offset the position by the cell gap or reset the position and cursor when we
                // reach the end of the row
                cursor += v.getColumnSpan();
                if (cursor < (((row + 1) * mNumFinalColumns))) {
                    x += lp.width + mCellGap;
                } else {
                    x = getPaddingLeft();
                    y += lp.height + mCellGap;
                }
            }
        }
    }

    private boolean isLandscape() {
        final boolean isLandscape =
            Resources.getSystem().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;
        return isLandscape;
    }

    public int getTileTextSize() {
        // get tile text size based on column count
        switch (mNumColumns) {
            case 5:
                return mResources.getDimensionPixelSize(R.dimen.qs_5_column_text_size);
            case 4:
                return mResources.getDimensionPixelSize(R.dimen.qs_4_column_text_size);
            case 3:
            default:
                return mResources.getDimensionPixelSize(R.dimen.qs_3_column_text_size);
        }
    }

    public int getTileTextPadding() {
        // get tile text padding based on column count
        switch (mNumColumns) {
            case 5:
                return mResources.getDimensionPixelSize(R.dimen.qs_5_column_text_padding);
            case 4:
                return mResources.getDimensionPixelSize(R.dimen.qs_4_column_text_padding);
            case 3:
            default:
                return mResources.getDimensionPixelSize(R.dimen.qs_tile_margin_below_icon);
        }
    }

    public void setOnEditModeChangedListener(EditModeChangedListener listener) {
        mEditModeChangedListener = listener;
    }

    public void enableLayoutTransitions() {
        setLayoutTransition(mLayoutTransition);
    }

    public boolean isEditModeEnabled() {
        return mEditModeEnabled;
    }

    public void setEditModeEnabled(boolean enabled) {
        mEditModeEnabled = enabled;
        mEditModeChangedListener.onEditModeChanged(enabled);
        ArrayList<String> tiles = new ArrayList<String>();
        for(int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if(v instanceof QuickSettingsTileView) {
                QuickSettingsTileView qs = (QuickSettingsTileView) v;
                qs.setEditMode(enabled);

                // Add to provider string
                if(!enabled && qs.getVisibility() == View.VISIBLE
                        && !qs.isTemporary()) {
                    tiles.add(qs.getTileId().toString());
                }
            }
        }

        if(!enabled) { // Store modifications
            ContentResolver resolver = getContext().getContentResolver();
            if(!tiles.isEmpty()) {
                Settings.System.putString(resolver,
                        Settings.System.QUICK_SETTINGS_TILES,
                                TextUtils.join(QuickSettings.DELIMITER, tiles));
            } else { // No tiles
                Settings.System.putString(resolver,
                        Settings.System.QUICK_SETTINGS_TILES, QuickSettings.NO_TILES);
            }
            updateSpan();
        }
    }
}
