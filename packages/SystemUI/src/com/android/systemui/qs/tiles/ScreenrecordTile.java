/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2012-2015 The CyanogenMod Project
 * Copyright (C) 2014-2015 The Euphoria-OS Project
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

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: Screenrecord **/
public class ScreenrecordTile extends QSTile<QSTile.BooleanState> {

    private boolean mListening;

    public ScreenrecordTile(Host host) {
        super(host);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }

    @Override
    public void handleClick() {
        mHost.collapsePanels();
        /* wait for the panel to close */
        try {
             Thread.sleep(2000);
        } catch (InterruptedException ie) {
             // Do nothing
        }
        Intent intent = new Intent(Intent.ACTION_SCREENRECORD);
        mContext.sendBroadcast(intent);
    }

    @Override
    protected void handleSecondaryClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.gallery3d",
            "com.android.gallery3d.app.GalleryActivity");
        mHost.startSettingsActivity(intent);
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.gallery3d",
            "com.android.gallery3d.app.GalleryActivity");
        mHost.startSettingsActivity(intent);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_screenrecord);
        state.icon = ResourceIcon.get(R.drawable.ic_qs_screenrecord);
    }
}
