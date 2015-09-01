/*
 * Copyright (C) 2015-2016 The MoKee Open Source Project
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

import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.mokee.setupwizard.SetupWizardApp;
import com.mokee.setupwizard.util.SetupWizardUtils;

public class FinishSetupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SetupWizardUtils.frpEnabled(context)) {
            return;
        }
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 1);
        ((StatusBarManager)context.getSystemService(Context.STATUS_BAR_SERVICE)).disable(
                StatusBarManager.DISABLE_NONE);
        Settings.Global.putInt(context.getContentResolver(),
                SetupWizardApp.KEY_DETECT_CAPTIVE_PORTAL, 1);
        mokee.providers.MKSettings.Secure.putInt(context.getContentResolver(),
                mokee.providers.MKSettings.Secure.MK_SETUP_WIZARD_COMPLETED, 1);
        SetupWizardUtils.disableGMSSetupWizard(context);
        SetupWizardUtils.disableSetupWizard(context);
    }
}
