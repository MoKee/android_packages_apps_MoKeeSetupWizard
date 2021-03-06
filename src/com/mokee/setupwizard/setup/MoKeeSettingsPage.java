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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ThemeUtils;
import android.content.res.ThemeConfig;
import android.content.res.ThemeManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerPolicyControl;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;

import com.mokee.setupwizard.R;
import com.mokee.setupwizard.ui.SetupPageFragment;
import com.mokee.setupwizard.ui.WebViewDialogFragment;
import com.mokee.setupwizard.util.SetupWizardUtils;

import mokee.providers.MKSettings;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import mokee.hardware.MKHardwareManager;

public class MoKeeSettingsPage extends SetupPage {

    public static final String TAG = "MoKeeSettingsPage";

    public static final String KEY_ENABLE_NAV_KEYS = "enable_nav_keys";
    public static final String KEY_APPLY_DEFAULT_THEME = "apply_default_theme";

    public MoKeeSettingsPage(Context context, SetupDataCallbacks callbacks) {
        super(context, callbacks);
    }

    @Override
    public Fragment getFragment(FragmentManager fragmentManager, int action) {
        Fragment fragment = fragmentManager.findFragmentByTag(getKey());
        if (fragment == null) {
            Bundle args = new Bundle();
            args.putString(Page.KEY_PAGE_ARGUMENT, getKey());
            args.putInt(Page.KEY_PAGE_ACTION, action);
            fragment = new MoKeeSettingsFragment();
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public String getKey() {
        return TAG;
    }

    @Override
    public int getTitleResId() {
        return R.string.setup_personalization;
    }

    private static void writeDisableNavkeysOption(Context context, boolean enabled) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int defaultBrightness = context.getResources().getInteger(
                com.android.internal.R.integer.config_buttonBrightnessSettingDefault);

        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.DEV_FORCE_SHOW_NAVBAR, enabled ? 1 : 0);
        final MKHardwareManager hardware = MKHardwareManager.getInstance(context);
        hardware.set(MKHardwareManager.FEATURE_KEY_DISABLE, enabled);

        /* Save/restore button timeouts to disable them in softkey mode */
        SharedPreferences.Editor editor = prefs.edit();

        if (enabled) {
            int currentBrightness = MKSettings.Secure.getInt(context.getContentResolver(),
                    MKSettings.Secure.BUTTON_BRIGHTNESS, defaultBrightness);
            if (!prefs.contains("pre_navbar_button_backlight")) {
                editor.putInt("pre_navbar_button_backlight", currentBrightness);
            }
            MKSettings.Secure.putInt(context.getContentResolver(),
                    MKSettings.Secure.BUTTON_BRIGHTNESS, 0);
        } else {
            int oldBright = prefs.getInt("pre_navbar_button_backlight", -1);
            if (oldBright != -1) {
                MKSettings.Secure.putInt(context.getContentResolver(),
                        MKSettings.Secure.BUTTON_BRIGHTNESS, oldBright);
                editor.remove("pre_navbar_button_backlight");
            }
        }
        editor.commit();
    }

    @Override
    public void onFinishSetup() {
        getCallbacks().addFinishRunnable(new Runnable() {
            @Override
            public void run() {
                if (getData().containsKey(KEY_ENABLE_NAV_KEYS)) {
                    writeDisableNavkeysOption(mContext, getData().getBoolean(KEY_ENABLE_NAV_KEYS));
                }
            }
        });
        handleDefaultThemeSetup();
        handleDefaultLockscreenSetup();
    }

    private void handleDefaultThemeSetup() {
        Bundle privacyData = getData();
        if (!ThemeUtils.getDefaultThemePackageName(mContext).equals(ThemeConfig.SYSTEM_DEFAULT) &&
                privacyData != null && privacyData.getBoolean(KEY_APPLY_DEFAULT_THEME)) {
            Log.i(TAG, "Applying default theme");
            final ThemeManager tm = (ThemeManager) mContext.getSystemService(Context.THEME_SERVICE);
            tm.applyDefaultTheme();

        } else {
            getCallbacks().finishSetup();
        }
    }

    private void handleDefaultLockscreenSetup() {
        String defaultLockscreenComponent = mContext.getResources().getString(
                R.string.default_custom_lockscreen_component);
        if (!TextUtils.isEmpty(defaultLockscreenComponent)) {
            ComponentName cn =
                    ComponentName.unflattenFromString(defaultLockscreenComponent);
            if (cn != null) {
                try {
                    Log.i(TAG, "Applying default lockscreen");
                    LockPatternUtils util = new LockPatternUtils(mContext);
                    util.setThirdPartyKeyguard(cn);
                    disableImmersivePromptForLockscreen(cn);
                } catch (PackageManager.NameNotFoundException | SecurityException e) {
                    Log.w(TAG, "Error setting default lockscreen: " + cn, e);
                }
            }
        }
    }

    private void disableImmersivePromptForLockscreen(ComponentName cn) {
        // prevents the immersive screen hint for the lockscreen
        WindowManagerPolicyControl policy = new WindowManagerPolicyControl();
        policy.reloadFromSetting(mContext);
        policy.addToPreconfirmWhiteList(cn.getPackageName());
        policy.saveToSettings(mContext);
    }

    private static boolean hideKeyDisabler(Context ctx) {
        final MKHardwareManager hardware = MKHardwareManager.getInstance(ctx);
        return !hardware.isSupported(MKHardwareManager.FEATURE_KEY_DISABLE);
    }

    private static boolean isKeyDisablerActive(Context ctx) {
        final MKHardwareManager hardware = MKHardwareManager.getInstance(ctx);
        return hardware.get(MKHardwareManager.FEATURE_KEY_DISABLE);
    }

    private static boolean hideThemeSwitch(Context context) {
        return ThemeUtils.getDefaultThemePackageName(context).equals(ThemeConfig.SYSTEM_DEFAULT);
    }

    public static class MoKeeSettingsFragment extends SetupPageFragment {

        private View mDefaultThemeRow;
        private View mNavKeysRow;
        private CheckBox mDefaultTheme;
        private CheckBox mNavKeys;
        private boolean mHideNavKeysRow = false;
        private boolean mHideThemeRow = false;


        private View.OnClickListener mDefaultThemeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = !mDefaultTheme.isChecked();
                mDefaultTheme.setChecked(checked);
                mPage.getData().putBoolean(KEY_APPLY_DEFAULT_THEME, checked);
            }
        };

        private View.OnClickListener mNavKeysClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = !mNavKeys.isChecked();
                mNavKeys.setChecked(checked);
                mPage.getData().putBoolean(KEY_ENABLE_NAV_KEYS, checked);
            }
        };

        @Override
        protected void initializePage() {

            mDefaultThemeRow = mRootView.findViewById(R.id.theme);
            mHideThemeRow = hideThemeSwitch(getActivity());
            if (mHideThemeRow) {
                mDefaultThemeRow.setVisibility(View.GONE);
            } else {
                mDefaultThemeRow.setOnClickListener(mDefaultThemeClickListener);
                String defaultTheme =
                        getString(R.string.services_apply_theme,
                                getString(R.string.default_theme_name));
                String defaultThemeSummary = getString(R.string.services_apply_theme_label,
                        defaultTheme);
                final SpannableStringBuilder themeSpan =
                        new SpannableStringBuilder(defaultThemeSummary);
                themeSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        0, defaultTheme.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView theme = (TextView) mRootView.findViewById(R.id.enable_theme_summary);
                theme.setText(themeSpan);
                mDefaultTheme = (CheckBox) mRootView.findViewById(R.id.enable_theme_checkbox);
            }

            mNavKeysRow = mRootView.findViewById(R.id.nav_keys);
            mNavKeysRow.setOnClickListener(mNavKeysClickListener);
            mNavKeys = (CheckBox) mRootView.findViewById(R.id.nav_keys_checkbox);
            boolean needsNavBar = true;
            try {
                IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
                needsNavBar = windowManager.needsNavigationBar();
            } catch (RemoteException e) {
            }
            mHideNavKeysRow = hideKeyDisabler(getActivity());
            if (mHideNavKeysRow || needsNavBar) {
                mNavKeysRow.setVisibility(View.GONE);
            } else {
                boolean navKeysDisabled =
                        isKeyDisablerActive(getActivity());
                mNavKeys.setChecked(navKeysDisabled);
            }
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.setup_mokee_services;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateDisableNavkeysOption();
            updateThemeOption();
        }

        private void updateThemeOption() {
            if (!mHideThemeRow) {
                final Bundle myPageBundle = mPage.getData();
                boolean themesChecked;
                if (myPageBundle.containsKey(KEY_APPLY_DEFAULT_THEME)) {
                    themesChecked = myPageBundle.getBoolean(KEY_APPLY_DEFAULT_THEME);
                } else {
                    themesChecked = getActivity().getResources().getBoolean(
                            R.bool.check_custom_theme_by_default);
                }
                mDefaultTheme.setChecked(themesChecked);
                myPageBundle.putBoolean(KEY_APPLY_DEFAULT_THEME, themesChecked);
            }
        }

        private void updateDisableNavkeysOption() {
            if (!mHideNavKeysRow) {
                final Bundle myPageBundle = mPage.getData();
                boolean enabled = Settings.Secure.getInt(getActivity().getContentResolver(),
                        Settings.Secure.DEV_FORCE_SHOW_NAVBAR, 0) != 0;
                boolean checked = myPageBundle.containsKey(KEY_ENABLE_NAV_KEYS) ?
                        myPageBundle.getBoolean(KEY_ENABLE_NAV_KEYS) :
                        enabled;
                mNavKeys.setChecked(checked);
                myPageBundle.putBoolean(KEY_ENABLE_NAV_KEYS, checked);
            }
        }

    }
}
