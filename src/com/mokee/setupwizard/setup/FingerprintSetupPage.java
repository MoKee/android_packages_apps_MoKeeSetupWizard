/*
 * Copyright (C) 2013-2016 The MoKee Open Source Project
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

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.mokee.setupwizard.R;
import com.mokee.setupwizard.SetupWizardApp;
import com.mokee.setupwizard.ui.LoadingFragment;

public class FingerprintSetupPage extends SetupPage {

    private static final String TAG = "FingerprintSetupPage";

    private LoadingFragment mLoadingFragment;

    public FingerprintSetupPage(Context context, SetupDataCallbacks callbacks) {
        super(context, callbacks);
    }

    @Override
    public Fragment getFragment(FragmentManager fragmentManager, int action) {
        mLoadingFragment = (LoadingFragment)fragmentManager.findFragmentByTag(getKey());
        if (mLoadingFragment == null) {
            Bundle args = new Bundle();
            args.putString(Page.KEY_PAGE_ARGUMENT, getKey());
            args.putInt(Page.KEY_PAGE_ACTION, action);
            mLoadingFragment = new LoadingFragment();
            mLoadingFragment.setArguments(args);
        }
        return mLoadingFragment;
    }

    @Override
    public int getNextButtonTitleResId() {
        return R.string.skip;
    }

    @Override
    public String getKey() {
        return TAG;
    }

    @Override
    public int getTitleResId() {
        return R.string.loading;
    }

    @Override
    public void doLoadAction(FragmentManager fragmentManager, int action) {
        super.doLoadAction(fragmentManager, action);
        launchFingerprintSetup();
    }

    private void launchFingerprintSetup() {
        Intent intent = new Intent(SetupWizardApp.ACTION_SETUP_FINGERPRINT);
        intent.putExtra(SetupWizardApp.EXTRA_USE_IMMERSIVE, true);
        intent.putExtra(SetupWizardApp.EXTRA_THEME, SetupWizardApp.EXTRA_MATERIAL_LIGHT);
        ActivityOptions options =
                ActivityOptions.makeCustomAnimation(mContext,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out);
        mLoadingFragment.startActivityForResult(intent,
                SetupWizardApp.REQUEST_CODE_SETUP_FINGERPRINT, options.toBundle());
    }
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (SetupWizardApp.REQUEST_CODE_SETUP_FINGERPRINT == requestCode) {
            if (resultCode == Activity.RESULT_CANCELED) {
                getCallbacks().onPreviousPage();
            } else if (resultCode == Activity.RESULT_OK) {
                getCallbacks().onNextPage();
            } else {
                getCallbacks().onNextPage();
            }
        }
        return true;
    }
}
