/*
 * Copyright (C) 2015 The MoKee OpenSource Project
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

package com.mokee.setupwizard.setup;

import java.util.List;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Context;
import android.mokee.utils.MoKeeUtils;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.mokee.setupwizard.R;
import com.mokee.setupwizard.ui.SetupPageFragment;
import com.mokee.setupwizard.util.InputMethodItem;

public class FinishPage extends SetupPage {

    public static final String TAG = "FinishPage";

    private FinishFragment mFinishFragment;

    public FinishPage(Context context, SetupDataCallbacks callbacks) {
        super(context, callbacks);
    }

    @Override
    public Fragment getFragment(FragmentManager fragmentManager, int action) {
        mFinishFragment = (FinishFragment)fragmentManager.findFragmentByTag(getKey());
        if (mFinishFragment == null) {
            Bundle args = new Bundle();
            args.putString(Page.KEY_PAGE_ARGUMENT, getKey());
            args.putInt(Page.KEY_PAGE_ACTION, action);
            mFinishFragment = new FinishFragment();
            mFinishFragment.setArguments(args);
        }
        return mFinishFragment;
    }

    @Override
    public String getKey() {
        return TAG;
    }

    @Override
    public int getTitleResId() {
        return R.string.setup_complete;
    }

    @Override
    public boolean doNextAction() {
        getCallbacks().onFinish();
        return true;
    }

    @Override
    public int getNextButtonTitleResId() {
        return R.string.start;
    }

    public static class FinishFragment extends SetupPageFragment {

        @Override
        protected void initializePage() {}

        @Override
        protected int getLayoutResource() {
            return R.layout.setup_finished_page;
        }
    }

    @Override
    public void onFinishSetup() {
        if (MoKeeUtils.isSupportLanguage(true)) {
            InputMethodManager manager = (InputMethodManager) 
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> infoList = manager.getInputMethodList();
            ContentResolver mContentResolver = mContext.getContentResolver();
            String mEnabledIM = Settings.Secure.getString(mContentResolver,
                    Settings.Secure.ENABLED_INPUT_METHODS);
            int total = infoList == null ? 0 : infoList.size();
            for (int index = 0; index < total; index++) {
                InputMethodItem mInputMethodItem = new InputMethodItem(mContext, infoList.get(index));
                String mDefaultIM = mInputMethodItem.getImPackage();
                if (mDefaultIM.contains("com.iflytek.inputmethod")) {
                    Settings.Secure.putString(mContentResolver,
                            Settings.Secure.DEFAULT_INPUT_METHOD, mDefaultIM);
                    if (!mEnabledIM.contains(mDefaultIM)) {
                        Settings.Secure.putString(mContentResolver,
                                Settings.Secure.ENABLED_INPUT_METHODS, mEnabledIM + ":" + mDefaultIM);
                    }
                    break;
                }
            }
        }
    }
}
