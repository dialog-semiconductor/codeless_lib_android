/*
 **********************************************************************************
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * Copyright (c) 2020-2024 Renesas Electronics Corporation and/or its affiliates
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Renesas nor the names of its contributors may be
 *    used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY RENESAS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, NONINFRINGEMENT, AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RENESAS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 **********************************************************************************
 */

package com.diasemi.codelesslib.misc;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.diasemi.codelesslib.CodelessLibLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/**
 * Helper class for requesting runtime permissions.
 * <h2>Usage</h2>
 * Create a permission checker object in your UI code:
 * <blockquote><pre>
 * public void onCreate(Bundle savedInstanceState) {
 *     ...
 *     permissionChecker = new RuntimePermissionChecker(this, savedInstanceState);
 *     ...
 * }</pre></blockquote>
 * Save the permission checker state:
 * <blockquote><pre>
 * protected void onSaveInstanceState(Bundle outState) {
 *     super.onSaveInstanceState(outState);
 *     permissionChecker.saveState(outState);
 * }</pre></blockquote>
 * Pass the permission request result to the permission checker:
 * <blockquote><pre>
 * public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
 *     if (!permissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults))
 *         super.onRequestPermissionsResult(requestCode, permissions, grantResults);
 * }</pre></blockquote>
 * Use one of the {@link #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions} methods
 * to check for runtime permissions. If the permissions are already granted, <code>true</code> is returned, otherwise
 * a permission request is initiated for the missing permissions. You can provide a {@link PermissionRequestCallback callback}
 * to be called when the request is complete.
 * <blockquote><pre>
 * permissionChecker.checkPermission(SCAN_LOCATION_PERMISSION, rationale, (requestCode, permissions, denied) -> {
 *     if (denied == null) {
 *         // The requested permissions were granted by the user
 *     }
 * });</pre></blockquote>
 */
@SuppressWarnings("unused")
public class RuntimePermissionChecker {

    /** Runtime permissions were introduced on Android 6. */
    private static final boolean noRuntimePermissions = Build.VERSION.SDK_INT < 23;

    /** The permission request code used for calling the {@link Activity#requestPermissions(String[], int) requestPermissions()} API. */
    public static final int PERMISSION_REQUEST_CODE = 4317357;
    /** The permission request code used when the user cancels the permission rationale dialog. */
    public static final int PERMISSION_REQUEST_DIALOG_CANCELLED = 4317358;

    // Configuration
    /** Ensure that permission requests are performed on the main thread. */
    private static final boolean CHECK_THREAD_ON_PERMISSION_REQUEST = true;
    /** Ensure that permission requests result processing is performed on the main thread. */
    private static final boolean CHECK_THREAD_ON_PERMISSION_RESULT = false;

    // Log
    /** Log tag used for log messages. */
    public static final String TAG = RuntimePermissionChecker.class.getSimpleName();

    // Runtime permissions
    /** Set of runtime permissions. */
    public static final HashSet<String> runtimePermissions = new HashSet<>();
    /** Set of runtime permission groups. */
    public static final HashSet<String> runtimePermissionGroups = new HashSet<>();
    static {
        runtimePermissions.addAll(Arrays.asList(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.ADD_VOICEMAIL,
                Manifest.permission.USE_SIP,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_WAP_PUSH,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ));

        runtimePermissionGroups.addAll(Arrays.asList(
                Manifest.permission_group.CALENDAR,
                Manifest.permission_group.CAMERA,
                Manifest.permission_group.CONTACTS,
                Manifest.permission_group.LOCATION,
                Manifest.permission_group.MICROPHONE,
                Manifest.permission_group.PHONE,
                Manifest.permission_group.SENSORS,
                Manifest.permission_group.SMS,
                Manifest.permission_group.STORAGE
        ));
    }

    /**
     * Checks if the specified permission is a runtime permission.
     * @param permission the permission to check
     */
    public static boolean isRuntimePermission(String permission) {
        return runtimePermissions.contains(permission);
    }

    /**
     * Checks if the specified permission group is a runtime permission group.
     * @param group the permission group to check
     */
    public static boolean isRuntimePermissionGroup(String group) {
        return runtimePermissionGroups.contains(group);
    }

    /**
     * Checks if the specified permission is marked as dangerous.
     * @param context the application context
     * @param permission the permission to check
     */
    public static boolean isDangerousPermission(Context context, String permission) {
        PackageManager pm = context.getPackageManager();
        PermissionInfo permInfo;
        try {
            permInfo = pm.getPermissionInfo(permission, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return (permInfo.flags & PermissionInfo.PROTECTION_DANGEROUS) != 0;
    }

    /**
     * Checks if the specified permission is marked as dangerous.
     * @param permission the permission to check
     */
    public boolean isDangerousPermission(String permission) {
        return isDangerousPermission(activity, permission);
    }

    // Permission request and callback
    /** Callback interface for receiving the permission request result. */
    public interface PermissionRequestCallback {
        /**
         * Called when the permission request result is received.
         * @param requestCode   the request code (identifier)
         * @param permissions   the requested permissions
         * @param denied        the permissions that were denied, <code>null</code> if all were granted
         */
        void onPermissionRequestResult(int requestCode, String[] permissions, String[] denied);
    }

    /**
     * {@link PermissionRequestCallback PermissionRequestCallback} interface implementation for registered callbacks.
     * <p> Calls the callback that was registered with a specific request code.
     */
    private class RegisteredCallback implements PermissionRequestCallback {

        private int requestCode;

        RegisteredCallback(int requestCode) {
            this.requestCode = requestCode;
        }

        public int getRequestCode() {
            return requestCode;
        }

        @Override
        public void onPermissionRequestResult(int requestCode, String[] permissions, String[] denied) {
            PermissionRequestCallback callback = registeredCallbacks.get(this.requestCode);
            if (callback != null)
                callback.onPermissionRequestResult(this.requestCode, permissions, denied);
        }
    }

    /**
     * Registers a permission request callback.
     * <p>
     * Can be used in activity's <code>onCreate()</code> to register a callback with a specific request code.
     * If the app UI is recreated while the permission request is in progress, the registered callback
     * will be restored and called, while unregistered callbacks will not be called.
     * @param requestCode   the request code (identifier)
     * @param callback      the callback to register
     */
    public void registerPermissionRequestCallback(int requestCode, PermissionRequestCallback callback) {
        if (noRuntimePermissions)
            return;
        registeredCallbacks.put(requestCode, callback);
    }

    /**
     * Unregisters a previously registered permission request callback.
     * @param requestCode the request code (identifier)
     */
    public void unregisterPermissionRequestCallback(int requestCode) {
        if (noRuntimePermissions)
            return;
        registeredCallbacks.remove(requestCode);
    }

    /**
     * Pending permission request.
     * <p> Information can be saved and restored during UI recreation, except for the permission request callback.
     */
    private static class PermissionRequest implements Parcelable {
        int code;
        String[] permissions;
        String rationale;
        PermissionRequestCallback callback;

        public PermissionRequest(int code, String[] permissions, String rationale, PermissionRequestCallback callback) {
            this.code = code;
            this.permissions = permissions;
            this.rationale = rationale;
            this.callback = callback;
        }

        protected PermissionRequest(Parcel in) {
            code = in.readInt();
            permissions = in.createStringArray();
            rationale = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(code);
            dest.writeStringArray(permissions);
            dest.writeString(rationale);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<PermissionRequest> CREATOR = new Creator<PermissionRequest>() {
            @Override
            public PermissionRequest createFromParcel(Parcel in) {
                return new PermissionRequest(in);
            }

            @Override
            public PermissionRequest[] newArray(int size) {
                return new PermissionRequest[size];
            }
        };
    }

    // Rationale dialog
    /** The permission rationale dialog that may be presented to the user prior to the permission request. */
    public static class RationaleDialog extends DialogFragment {

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onCancel(@NonNull DialogInterface dialog) {
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Permission rationale dialog cancelled");
            getActivity().onRequestPermissionsResult(PERMISSION_REQUEST_DIALOG_CANCELLED, new String[0], new int[0]);
            super.onCancel(dialog);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            setCancelable(args.getBoolean("cancellable"));
            return createRationaleDialog(getActivity(), args);
        }
    }

    /** The permission rationale dialog that may be presented to the user prior to the permission request. */
    public static class RationaleDialogOld extends android.app.DialogFragment {

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onCancel(DialogInterface dialog) {
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Permission rationale dialog cancelled");
            getActivity().onRequestPermissionsResult(PERMISSION_REQUEST_DIALOG_CANCELLED, new String[0], new int[0]);
            super.onCancel(dialog);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            setCancelable(args.getBoolean("cancellable"));
            return createRationaleDialog(getActivity(), args);
        }
    }

    private static Dialog createRationaleDialog(Activity activity, Bundle args) {
        String rationale = args.getString("rationale");
        final String[] permissions = args.getStringArray("permissions");
        String title = args.getString("title");
        int iconID = args.getInt("iconID");
        int layoutID = args.getInt("layoutID");

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(title != null ? title : "Permission Request");
        if (iconID != 0)
            builder.setIcon(iconID);
        if (layoutID == 0) {
            builder.setMessage(rationale);
        } else {
            View view = activity.getLayoutInflater().inflate(layoutID, null);
            TextView rationaleView = view.findViewWithTag("rationale");
            rationaleView.setText(rationale);
            builder.setView(view);
        }

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @TargetApi(23)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Request permissions
                if (CodelessLibLog.PERMISSION)
                    Log.d(TAG, "Permission request: " + Arrays.toString(permissions));
                activity.requestPermissions(permissions, RuntimePermissionChecker.PERMISSION_REQUEST_CODE);
            }
        });

        return builder.create();
    }

    /**
     * Sets the permission rationale dialog title.
     * @param rationaleTitle the rationale dialog title
     */
    public void setRationaleTitle(String rationaleTitle) {
        this.rationaleTitle = rationaleTitle;
    }

    /**
     * Sets the permission rationale dialog title.
     * @param rationaleTitleID the rationale dialog title resource ID
     */
    public void setRationaleTitle(int rationaleTitleID) {
        this.rationaleTitle = activity.getString(rationaleTitleID);
    }

    /**
     * Sets the permission rationale dialog icon.
     * @param rationaleIconID the rationale dialog icon resource ID
     */
    public void setRationaleIcon(int rationaleIconID) {
        this.rationaleIconID = rationaleIconID;
    }

    /**
     * Sets the permission rationale dialog view.
     * @param rationaleLayoutID the rationale dialog layout resource ID
     */
    public void setRationaleView(int rationaleLayoutID) {
        this.rationaleLayoutID = rationaleLayoutID;
    }

    /**
     * Configures whether the permission rationale dialog is cancellable.
     * @param rationaleCancellable <code>true</code> to make the rationale dialog cancellable
     */
    public void setRationaleCancellable(boolean rationaleCancellable) {
        this.rationaleCancellable = rationaleCancellable;
    }

    /**
     * Configures whether the permission rationale dialog will always be shown.
     * <p> The default behavior is to call {@link Activity#shouldShowRequestPermissionRationale(String)} to decide.
     * @param alwaysShowRationale <code>true</code> to always show the rationale dialog
     */
    public void setAlwaysShowRationale(boolean alwaysShowRationale) {
        this.alwaysShowRationale = alwaysShowRationale;
    }

    /**
     * Sets a permission rationale text that will be shown only once to the user.
     * <p>
     * May be used to provide a rationale for all required permissions, before requesting each one as needed.
     * The one-time rationale is always shown if a permission request is initiated.
     * @param oneTimeRationale the rationale that will be shown once
     */
    public void setOneTimeRationale(String oneTimeRationale) {
        this.oneTimeRationale = oneTimeRationale;
    }

    /**
     * Resets one-time rationale shown status, so that is may be shown again.
     * @see #setOneTimeRationale(String)
     */
    public void resetOneTimeRationale() {
        showedOneTimeRationale = false;
    }

    /** Called when the permission rationale dialog is cancelled by the user. */
    private void permissionRationaleDialogCancelled() {
        resumePendingPermissionRequest();
    }

    // Data
    private static HashSet<String> grantedPermissions = new HashSet<>();
    private Activity activity;
    private Handler handler;
    private PermissionRequest currentRequest;
    private LinkedList<PermissionRequest> pendingRequests = new LinkedList<>();
    private HashMap<Integer, PermissionRequestCallback> registeredCallbacks = new HashMap<>();
    private String rationaleTitle;
    private int rationaleIconID;
    private int rationaleLayoutID;
    private boolean rationaleCancellable;
    private boolean alwaysShowRationale;
    private String oneTimeRationale;
    private boolean showedOneTimeRationale;

    /**
     * Creates a runtime permission checker.
     * @param activity the activity that will be used to request permissions and get results
     */
    public RuntimePermissionChecker(Activity activity) {
        this.activity = activity;
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Creates a runtime permission checker.
     * @param activity  the activity that will be used to request permissions and get results
     * @param state     the activity's saved instance state
     * @see #saveState(Bundle)
     */
    public RuntimePermissionChecker(Activity activity, Bundle state) {
        this(activity);
        restoreState(state);
    }

    // Pending permission requests
    /** Checks if there is an active permission request. */
    public boolean permissionRequestPending() {
        return currentRequest != null;
    }

    /** Starts the next permission request. */
    private void resumePendingPermissionRequest() {
        currentRequest = null;
        while (currentRequest == null && !pendingRequests.isEmpty()) {
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Resume pending permission request");
            PermissionRequest req = pendingRequests.poll();
            if (checkPermissions(req.permissions, req.rationale, req.callback, req.code) && req.callback != null)
                req.callback.onPermissionRequestResult(req.code, req.permissions, null);
        }
    }

    /** Cancels all pending permissions requests. */
    public void cancelPendingPermissionRequests() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest != null)
                    currentRequest.callback = null;
                pendingRequests.clear();
            }
        });
    }

    // Activity lifecycle
    /**
     * Saves the information about any pending permission requests.
     * <p> Must be called from the associated activity's <code>onSaveInstanceState()</code>.
     * @param state the activity's saved instance state
     */
    public void saveState(Bundle state) {
        if (noRuntimePermissions)
            return;

        String prefix = getClass().getName() + "#";
        state.putBoolean(prefix + "showedOneTimeRationale", showedOneTimeRationale);
        if (currentRequest != null)
            state.putParcelable(prefix + "currentRequest", currentRequest);
        if (!pendingRequests.isEmpty())
            state.putParcelableArray(prefix + "pendingRequests", pendingRequests.toArray(new PermissionRequest[pendingRequests.size()]));
    }

    /**
     * Restores the information about any pending permission requests.
     * @param state the activity's saved instance state
     */
    public void restoreState(Bundle state) {
        if (noRuntimePermissions)
            return;

        if (state == null)
            return;
        String prefix = getClass().getName() + "#";
        showedOneTimeRationale = state.getBoolean(prefix + "showedOneTimeRationale");

        // WARNING: Unregistered callbacks are lost.

        currentRequest = state.getParcelable(prefix + "currentRequest");
        if (currentRequest != null) {
            currentRequest.callback = currentRequest.code != PERMISSION_REQUEST_CODE ? new RegisteredCallback(currentRequest.code) : null;
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Restored current request");
        }

        Parcelable[] pending = state.getParcelableArray(prefix + "pendingRequests");
        if (pending != null) {
            for (Parcelable parcel : pending) {
                PermissionRequest req = (PermissionRequest) parcel;
                req.callback = req.code != PERMISSION_REQUEST_CODE ? new RegisteredCallback(req.code): null;
                pendingRequests.add(req);
            }
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Restored pending requests");
        }
    }

    // Check and request permissions
    /**
     * Checks if a permission is granted using the {@link Context#checkSelfPermission(String)} API.
     * @param permission the permission to check
     * @return <code>true</code> if the permission is granted
     */
    @TargetApi(23)
    public boolean checkPermissionGranted(String permission) {
        return noRuntimePermissions || activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if a permission is granted, using the internal set of {@link #getGrantedPermissions() granted permissions}.
     * @param permission the permission to check
     * @return <code>true</code> if the permission is granted
     */
    public boolean permissionGranted(String permission) {
        return grantedPermissions.contains(permission);
    }

    /**
     * Checks if permissions are granted, using the internal set of {@link #getGrantedPermissions() granted permissions}.
     * @param permissions the permissions to check
     * @return <code>true</code> if all the permissions are granted
     */
    public boolean permissionsGranted(String[] permissions) {
        return grantedPermissions.containsAll(Arrays.asList(permissions));
    }

    /**
     * Returns the permissions that have been requested by this checker and were granted.
     * <p> This set is updated on each permission check that is performed by this checker.
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public HashSet<String> getGrantedPermissions() {
        return grantedPermissions;
    }

    /**
     * Requests the specified runtime permission.
     * @param permission        the permission to check/request
     * @param rationaleResID    the permission rationale resource ID
     * @param requestCode       the request code (identifier)
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public boolean checkPermission(String permission, int rationaleResID, int requestCode) {
        return noRuntimePermissions || checkPermissions(new String[] { permission }, activity.getString(rationaleResID), null, requestCode);
    }

    /**
     * Requests the specified runtime permission.
     * @param permission    the permission to check/request
     * @param rationale     the permission rationale (optional)
     * @param requestCode   the request code (identifier)
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public boolean checkPermission(String permission, String rationale, int requestCode) {
        return noRuntimePermissions || checkPermissions(new String[] { permission }, rationale, null, requestCode);
    }

    /**
     * Requests the specified runtime permissions.
     * @param permissions       the permissions to check/request
     * @param rationaleResID    the permission rationale resource ID
     * @param requestCode       the request code (identifier)
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public boolean checkPermissions(String[] permissions, int rationaleResID, int requestCode) {
        return noRuntimePermissions || checkPermissions(permissions, activity.getString(rationaleResID), null, requestCode);
    }

    /**
     * Requests the specified runtime permissions.
     * @param permissions   the permissions to check/request
     * @param rationale     the permission rationale (optional)
     * @param requestCode   the request code (identifier)
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public boolean checkPermissions(String[] permissions, String rationale, int requestCode) {
        return noRuntimePermissions || checkPermissions(permissions, rationale, null, requestCode);
    }

    /**
     * Requests the specified runtime permission.
     * @param permission        the permission to check/request
     * @param rationaleResID    the permission rationale resource ID
     * @param callback          the permission request callback (optional)
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public boolean checkPermission(String permission, int rationaleResID, PermissionRequestCallback callback) {
        return noRuntimePermissions || checkPermissions(new String[] { permission }, activity.getString(rationaleResID), callback, PERMISSION_REQUEST_CODE);
    }

    /**
     * Requests the specified runtime permission.
     * @param permission    the permission to check/request
     * @param rationale     the permission rationale (optional)
     * @param callback      the permission request callback (optional)
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public boolean checkPermission(String permission, String rationale, PermissionRequestCallback callback) {
        return noRuntimePermissions || checkPermissions(new String[] { permission }, rationale, callback, PERMISSION_REQUEST_CODE);
    }

    /**
     * Requests the specified runtime permissions.
     * @param permissions   the permissions to check/request
     * @param rationaleResID    the permission rationale resource ID
     * @param callback          the permission request callback (optional)
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public boolean checkPermissions(String[] permissions, int rationaleResID, PermissionRequestCallback callback) {
        return noRuntimePermissions || checkPermissions(permissions, activity.getString(rationaleResID), callback, PERMISSION_REQUEST_CODE);
    }

    /**
     * Requests the specified runtime permissions.
     * @param permissions   the permissions to check/request
     * @param rationale     the permission rationale (optional)
     * @param callback      the permission request callback (optional)
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public boolean checkPermissions(String[] permissions, String rationale, PermissionRequestCallback callback) {
        return noRuntimePermissions || checkPermissions(permissions, rationale, callback, PERMISSION_REQUEST_CODE);
    }

    /**
     * Requests the specified runtime permissions.
     * <p>
     * Returns <code>true</code> if all the requested permissions are granted, otherwise it requests the missing permissions.
     * A permission rationale dialog may be shown to the user prior to the permission request.
     * If another permission request is pending, the new request is delayed until the current one is complete.
     * <p>
     * The request code value is independent of the actual value that is used when calling the request API.
     * If is is different than {@link #PERMISSION_REQUEST_CODE}, it is used as an identifier for any registered callbacks and
     * it is passed to the {@link PermissionRequestCallback#onPermissionRequestResult(int, String[], String[]) callback}.
     * <p>
     * For a registered callback, only the corresponding registered request code should be used.
     * @param permissions   the permissions to check/request
     * @param rationale     the permission rationale (optional)
     * @param callback      the permission request callback (optional)
     * @param requestCode   the request code (identifier)
     * @return <code>true</code> if all requested permissions are already granted
     * @see #onRequestPermissionsResult(int, String[], int[]) onRequestPermissionsResult()
     */
    @TargetApi(23)
    public boolean checkPermissions(final String[] permissions, final String rationale, PermissionRequestCallback callback, final int requestCode) {
        if (noRuntimePermissions)
            return true;

        // Ensure permission request runs on the main thread
        if (CHECK_THREAD_ON_PERMISSION_REQUEST && Thread.currentThread() != handler.getLooper().getThread()) {
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Permission check not on main thread");
            final PermissionRequestCallback requestCallback = callback;
            final PermissionRequestCallback successCallback = callback != null || requestCode == PERMISSION_REQUEST_CODE ? callback : new RegisteredCallback(requestCode);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (CodelessLibLog.PERMISSION)
                        Log.d(TAG, "Permission check moved to main thread");
                    if (checkPermissions(permissions, rationale, requestCallback, requestCode) && successCallback != null)
                        successCallback.onPermissionRequestResult(requestCode, permissions, null);
                }
            });
            return false;
        }

        if (CodelessLibLog.PERMISSION)
            Log.d(TAG, "Check permissions: " + Arrays.toString(permissions));

        // Check if already granted
        boolean grantedAll = true;
        for (String perm : permissions) {
            if (!grantedPermissions.contains(perm)) {
                grantedAll = false;
                break;
            }
        }
        if (grantedAll) {
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Permissions already granted");
            return true;
        }

        List<String> permList = new ArrayList<>(Arrays.asList(permissions));

        // Remove granted permissions
        for (String perm : permissions) {
            if (grantedPermissions.contains(perm) || activity.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
                if (CodelessLibLog.PERMISSION)
                    Log.d(TAG, "Permission granted: " + perm);
                grantedPermissions.add(perm);
                permList.remove(perm);
            }
         }

        // Recheck if already granted
        if (permList.isEmpty()) {
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Permissions already granted");
            return true;
        }

        // Check for pending request
        if (callback == null && requestCode != PERMISSION_REQUEST_CODE)
            callback = new RegisteredCallback(requestCode);
        if (currentRequest != null) {
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Permission request pending");
            pendingRequests.add(new PermissionRequest(requestCode, permissions, rationale, callback));
            return false;
        }
        currentRequest = new PermissionRequest(requestCode, permissions, rationale, callback);

        String[] permArray = permList.toArray(new String[permList.size()]);

        // Show rationale
        boolean showOnce = !showedOneTimeRationale && oneTimeRationale != null;
        if (rationale != null || showOnce) {
            for (String perm : permList) {
                boolean show = rationale != null && (alwaysShowRationale || activity.shouldShowRequestPermissionRationale(perm));
                if (show || showOnce) {
                    showedOneTimeRationale = true;
                    if (CodelessLibLog.PERMISSION)
                        Log.d(TAG, "Showing permission rationale for: " + perm);
                    Bundle args = new Bundle(6);
                    args.putString("rationale", show ? rationale : oneTimeRationale);
                    args.putStringArray("permissions", permArray);
                    args.putString("title", rationaleTitle);
                    args.putInt("iconID", rationaleIconID);
                    args.putInt("layoutID", rationaleLayoutID);
                    args.putBoolean("cancellable", rationaleCancellable);
                    if (activity instanceof FragmentActivity) {
                        FragmentActivity fragmentActivity = (FragmentActivity) activity;
                        RationaleDialog dialog = new RationaleDialog();
                        dialog.setArguments(args);
                        if (fragmentActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                            dialog.show(fragmentActivity.getSupportFragmentManager(), "permission rationale dialog");
                        } else {
                            fragmentActivity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                                @Override
                                public void onStart(@NonNull LifecycleOwner owner) {
                                    fragmentActivity.getLifecycle().removeObserver(this);
                                    dialog.show(((FragmentActivity) activity).getSupportFragmentManager(), "permission rationale dialog");
                                }
                            });
                        }
                    } else {
                        RationaleDialogOld dialog = new RationaleDialogOld();
                        dialog.setArguments(args);
                        dialog.show(activity.getFragmentManager(), "permission rationale dialog");
                    }
                    return false;
                }
            }
        }

        // Request permissions
        if (CodelessLibLog.PERMISSION)
            Log.d(TAG, "Permission request: " + permList);
        activity.requestPermissions(permArray, PERMISSION_REQUEST_CODE);
        return false;
    }

    /**
     * Processes the active permission request result.
     * <p> Must be called from the associated activity's {@link Activity#onRequestPermissionsResult(int, String[], int[]) onRequestPermissionsResult()}.
     * <p> If there are other pending permission requests, it initiates the next request.
     * @param requestCode   the request code (identifier)
     * @param permissions   the requested permissions
     * @param grantResults  the permission request result
     * @return <code>true</code> if the permission request was initiated by this checker
     */
    public boolean onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE && requestCode != PERMISSION_REQUEST_DIALOG_CANCELLED)
            return false;

        // Ensure we are on the main thread (probably not needed)
        if (CHECK_THREAD_ON_PERMISSION_RESULT && Thread.currentThread() != handler.getLooper().getThread()) {
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Permission request result not on main thread");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (CodelessLibLog.PERMISSION)
                        Log.d(TAG, "Permission request result moved to main thread");
                    onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
            });
            return true;
        }

        if (requestCode == PERMISSION_REQUEST_DIALOG_CANCELLED) {
            permissionRationaleDialogCancelled();
            return true;
        }

        // Check request results
        ArrayList<String> denied = new ArrayList<>();
        for (int i = 0; i < permissions.length; ++i) {
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            if (CodelessLibLog.PERMISSION)
                Log.d(TAG, "Permission " + (granted ? "granted: " : "denied: ") + permissions[i]);
            if (granted)
                grantedPermissions.add(permissions[i]);
            else
                denied.add(permissions[i]);
        }

        if (currentRequest.callback != null)
            currentRequest.callback.onPermissionRequestResult(currentRequest.code, currentRequest.permissions, denied.isEmpty() ? null : denied.toArray(new String[denied.size()]));
        resumePendingPermissionRequest();
        return true;
    }

    // Request all runtime permissions for app
    /** Returns all the runtime permissions that are declared in the application manifest. */
    private List<String> getAllRuntimePermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null)
                for (String perm : info.requestedPermissions)
                    if (isRuntimePermission(perm))
                        permissions.add(perm);
        } catch (PackageManager.NameNotFoundException ignored) {}
        return permissions;
    }

    /**
     * Requests all the runtime permissions that are declared in the application manifest.
     * @param requestCode the request code (identifier)
     * @see #requestAllRuntimePermissions(String, PermissionRequestCallback, int) requestAllRuntimePermissions()
     */
    public boolean requestAllRuntimePermissions(int requestCode) {
        return noRuntimePermissions || requestAllRuntimePermissions(null, null, requestCode);
    }

    /**
     * Requests all the runtime permissions that are declared in the application manifest.
     * @param rationaleResID    the permission rationale resource ID
     * @param requestCode       the request code (identifier)
     * @see #requestAllRuntimePermissions(String, PermissionRequestCallback, int) requestAllRuntimePermissions()
     */
    public boolean requestAllRuntimePermissions(int rationaleResID, int requestCode) {
        return noRuntimePermissions || requestAllRuntimePermissions(activity.getString(rationaleResID), null, requestCode);
    }

    /**
     * Requests all the runtime permissions that are declared in the application manifest.
     * @param rationale     the permission rationale (optional)
     * @param requestCode   the request code (identifier)
     * @see #requestAllRuntimePermissions(String, PermissionRequestCallback, int) requestAllRuntimePermissions()
     */
    public boolean requestAllRuntimePermissions(String rationale, int requestCode) {
        return noRuntimePermissions || requestAllRuntimePermissions(rationale, null, requestCode);
    }

    /**
     * Requests all the runtime permissions that are declared in the application manifest.
     * @param callback the permission request collback (optional)
     * @see #requestAllRuntimePermissions(String, PermissionRequestCallback, int) requestAllRuntimePermissions()
     */
    public boolean requestAllRuntimePermissions(PermissionRequestCallback callback) {
        return noRuntimePermissions || requestAllRuntimePermissions(null, callback, PERMISSION_REQUEST_CODE);
    }

    /**
     * Requests all the runtime permissions that are declared in the application manifest.
     * @param rationaleResID    the permission rationale resource ID
     * @param callback          the permission request collback (optional)
     * @see #requestAllRuntimePermissions(String, PermissionRequestCallback, int) requestAllRuntimePermissions()
     */
    public boolean requestAllRuntimePermissions(int rationaleResID, PermissionRequestCallback callback) {
        return noRuntimePermissions || requestAllRuntimePermissions(activity.getString(rationaleResID), callback, PERMISSION_REQUEST_CODE);
    }

    /**
     * Requests all the runtime permissions that are declared in the application manifest.
     * @param rationale     the permission rationale (optional)
     * @param callback      the permission request collback (optional)
     * @see #requestAllRuntimePermissions(String, PermissionRequestCallback, int) requestAllRuntimePermissions()
     */
    public boolean requestAllRuntimePermissions(String rationale, PermissionRequestCallback callback) {
        return noRuntimePermissions || requestAllRuntimePermissions(rationale, callback, PERMISSION_REQUEST_CODE);
    }

    /**
     * Requests all the runtime permissions that are declared in the application manifest.
     * <p> Returns <code>true</code> if all declared runtime permissions are granted, otherwise it requests the missing permissions.
     * @param rationale     the permission rationale (optional)
     * @param callback      the permission request collback (optional)
     * @param requestCode   the request code (identifier)
     * @return <code>true</code> if all declared runtime permissions are already granted
     * @see #checkPermissions(String[], String, PermissionRequestCallback, int) checkPermissions()
     */
    public boolean requestAllRuntimePermissions(String rationale, PermissionRequestCallback callback, int requestCode) {
        if (noRuntimePermissions)
            return true;
        List<String> permList = getAllRuntimePermissions();
        if (CodelessLibLog.PERMISSION)
            Log.d(TAG, !permList.isEmpty() ? "Request all runtime permissions" : "No runtime permissions found");
        return permList.isEmpty() || checkPermissions(permList.toArray(new String[permList.size()]), rationale, callback, requestCode);
    }
}
