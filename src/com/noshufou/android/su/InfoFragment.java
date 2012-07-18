package com.noshufou.android.su;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.noshufou.android.su.preferences.Preferences;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.util.Util.MenuId;
import com.noshufou.android.su.util.Util.VersionInfo;
import com.noshufou.android.su.widget.ChangeLog;

public class InfoFragment extends Fragment 
        implements OnClickListener, OnCheckedChangeListener {
    private static final String TAG = "Su.InfoFragment";
    
    private TextView mSuperuserVersion;
    private TextView mEliteInstalled;
    private TextView mGetEliteLabel;
    private TextView mSuVersion;
    private View mSuDetailsRow;
    private TextView mSuDetailsMode;
    private TextView mSuDetailsOwner;
    private TextView mSuDetailsFile;
    private TextView mSuWarning;
    private CheckBox mOutdatedNotification;
    private CheckBox mTempUnroot;
    private CheckBox mOtaSurvival;

    private View mGetElite;
    private View mBinaryUpdater;
    
    private SharedPreferences mPrefs;
    private boolean mDualPane = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        
        mSuperuserVersion = (TextView) view.findViewById(R.id.superuser_version);
        mEliteInstalled = (TextView) view.findViewById(R.id.elite_installed);
        mGetEliteLabel = (TextView) view.findViewById(R.id.get_elite_label);
        mSuVersion = (TextView) view.findViewById(R.id.su_version);
        mSuDetailsRow = view.findViewById(R.id.su_details_row);
        mSuDetailsMode = (TextView) view.findViewById(R.id.su_details_mode);
        mSuDetailsOwner = (TextView) view.findViewById(R.id.su_details_owner);
        mSuDetailsFile = (TextView) view.findViewById(R.id.su_details_file);
        mSuWarning = (TextView) view.findViewById(R.id.su_warning);
        mOutdatedNotification = (CheckBox) view.findViewById(R.id.outdated_notification);
        mOutdatedNotification.setOnCheckedChangeListener(this);
        mTempUnroot = (CheckBox) view.findViewById(R.id.temp_unroot);
        mTempUnroot.setOnClickListener(this);
        mOtaSurvival = (CheckBox) view.findViewById(R.id.ota_survival);
        mOtaSurvival.setOnClickListener(this);
        
        view.findViewById(R.id.display_changelog).setOnClickListener(this);
        mGetElite = view.findViewById(R.id.get_elite);
        mGetElite.setOnClickListener(this);
        mBinaryUpdater = view.findViewById(R.id.binary_updater);
        mBinaryUpdater.setOnClickListener(this);
        
        new UpdateInfo().execute();
        
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mDualPane = ((HomeActivity)getActivity()).isDualPane();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mDualPane) {
            menu.add(Menu.NONE, MenuId.LOG, MenuId.LOG,
                    R.string.page_label_log)
                    .setIcon(R.drawable.ic_action_log)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MenuId.LOG:
                ((HomeActivity)getActivity()).showLog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.display_changelog:
                ChangeLog cl = new ChangeLog(getActivity());
                cl.getFullLogDialog().show();
                break;
            case R.id.get_elite:
                final Intent eliteIntent = new Intent(Intent.ACTION_VIEW);
                eliteIntent.setData(Uri.parse("market://details?id=com.noshufou.android.su.elite"));
                startActivity(eliteIntent);
                break;
            case R.id.binary_updater:
                final Intent updaterIntent = new Intent(getActivity(), UpdaterActivity.class);
                startActivity(updaterIntent);
                break;
            case R.id.temp_unroot:
                new ToggleSuOption(Preferences.TEMP_UNROOT).execute();
                break;
            case R.id.ota_survival:
                new ToggleSuOption(Preferences.OTA_SURVIVE).execute();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.outdated_notification:
                mPrefs.edit().putBoolean(Preferences.OUTDATED_NOTIFICATION, isChecked).commit();
                break;
        }
    }

    private class UpdateInfo extends AsyncTask<Void, Object, Void> {

        @Override
        protected void onPreExecute() {
            getActivity().setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Void doInBackground(Void... arg) {
            publishProgress(0, Util.getSuperuserVersionInfo(getActivity()));
            publishProgress(1, Util.elitePresent(getActivity(), false, 0));

            String suPath = Util.whichSu();
            if (suPath != null) {
                publishProgress(2, Util.getSuVersionInfo());
                String suTools = Util.ensureSuTools(getActivity());
                try {
                    Process process = new ProcessBuilder(suTools, "ls", "-l", suPath)
                    .redirectErrorStream(true).start();
                    BufferedReader is = new BufferedReader(new InputStreamReader(
                            new DataInputStream(process.getInputStream())), 64);
                    String inLine = is.readLine();
                    String bits[] = inLine.split("\\s+");
                    publishProgress(3, bits[0], String.format("%s %s", bits[1], bits[2]), suPath);
                } catch (IOException e) {
                    Log.w(TAG, "Binary information could not be read");
                }
            } else {
                publishProgress(2, null);
            }
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            publishProgress(4,
                    prefs.getBoolean(Preferences.OUTDATED_NOTIFICATION, true),
                    prefs.getBoolean(Preferences.TEMP_UNROOT, false),
                    prefs.getBoolean(Preferences.OTA_SURVIVE, false));
            return null;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            switch ((Integer) values[0]) {
                case 0:
                    VersionInfo superuserVersion = (VersionInfo) values[1];
                    mSuperuserVersion.setText(getActivity().getString(
                            R.string.info_version,
                            superuserVersion.version,
                            superuserVersion.versionCode));
                    break;
                case 1:
                    boolean eliteInstalled = (Boolean) values[1];
                    mEliteInstalled.setText(eliteInstalled ? 
                            R.string.info_elite_installed : R.string.info_elite_not_installed);
                    mGetEliteLabel.setVisibility(eliteInstalled ? View.GONE : View.VISIBLE);
                    mGetElite.setClickable(!eliteInstalled);
                    break;
                case 2:
                    VersionInfo suVersion = (VersionInfo) values[1];
                    if (suVersion != null) {
                        mSuVersion.setText(getActivity().getString(
                                R.string.info_bin_version,
                                suVersion.version,
                                suVersion.versionCode));
                        mSuDetailsRow.setVisibility(View.VISIBLE);
                        mSuWarning.setVisibility(View.VISIBLE);
                        mBinaryUpdater.setClickable(true);
                    } else {
                        mSuVersion.setText(R.string.info_bin_version_not_found);
                        mSuDetailsRow.setVisibility(View.GONE);
                        mBinaryUpdater.setClickable(false);
                        mSuWarning.setVisibility(View.GONE);
                    }
                    break;
                case 3:
                    String mode = (String) values[1];
                    String owner = (String) values[2];
                    String file = (String) values[3];
                    boolean goodMode = mode.equals("-rwsr-sr-x");
                    boolean goodOwner = owner.equals("root root");
                    boolean goodFile = !file.equals("/sbin/su");
                    mSuDetailsMode.setText(mode);
                    mSuDetailsMode.setTextColor(goodMode ? Color.GREEN : Color.RED);
                    mSuDetailsOwner.setText(owner);
                    mSuDetailsOwner.setTextColor(goodOwner ? Color.GREEN : Color.RED);
                    mSuDetailsFile.setText(file);
                    mSuDetailsFile.setTextColor(goodFile ? Color.GREEN : Color.RED);
                    if (!goodFile) {
                        mBinaryUpdater.setClickable(false);
                        mSuWarning.setText("note: your su binary cannot be updated due to it's location");
                        mSuWarning.setVisibility(View.VISIBLE);
                    }
                    break;
                case 4:
                    boolean outdatedNotification = (Boolean) values[1];
                    boolean tempUnroot = (Boolean) values[2];
                    boolean otaSurvival = (Boolean) values[3];
                    mOutdatedNotification.setChecked(outdatedNotification);
                    mTempUnroot.setChecked(tempUnroot);
                    mTempUnroot.setEnabled(true);
                    mOtaSurvival.setChecked(otaSurvival);
                    mOtaSurvival.setEnabled(!tempUnroot);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            getActivity().setProgressBarIndeterminateVisibility(false);
        }
    }

    private class ToggleSuOption extends AsyncTask<Void, Void, Boolean> {
        private String mKey;
        
        public ToggleSuOption(String key) {
            mKey = key;
        }

        @Override
        protected void onPreExecute() {
            getActivity().setProgressBarIndeterminateVisibility(true);
            mTempUnroot.setEnabled(false);
            mOtaSurvival.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean status = false;
            boolean startState = mPrefs.getBoolean(mKey, false);
            if (startState) {
                status = Util.restoreSu(getActivity(), true, mKey);
            } else {
                status = Util.backupSu(getActivity(), mKey.equals(Preferences.TEMP_UNROOT));
            }

            if (status) {
                mPrefs.edit().putBoolean(mKey, !startState).commit();
            }
            
            return status;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            getActivity().setProgressBarIndeterminateVisibility(false);
            new UpdateInfo().execute();
        }
    }
}
