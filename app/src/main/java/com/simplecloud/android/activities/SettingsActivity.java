package com.simplecloud.android.activities;

import java.io.File;
import java.util.List;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.simplecloud.android.R;
import com.simplecloud.android.common.utils.FileUtils;
import com.simplecloud.android.common.utils.NetworkUtils;
import com.simplecloud.android.database.AccountDAO;
import com.simplecloud.android.models.accounts.Account;
import com.simplecloud.android.models.accounts.DropboxAccount;
import com.simplecloud.android.models.accounts.GoogleDriveAccount;
import com.simplecloud.android.services.AbstractServiceManager;
import com.simplecloud.android.services.dropbox.DropboxServiceManager;
import com.simplecloud.android.services.googledrive.GoogleDriveServiceManager;


public class SettingsActivity extends PreferenceActivity {

    private static final String LOGCAT = SettingsActivity.class.getSimpleName();
    public static final String PREF_FRAGMENT_TAG = "pref_fragment_tag";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        getActionBar().setIcon(R.drawable.settings);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new PrefsFragment(), PREF_FRAGMENT_TAG).commit();
    }


    public static class PrefsFragment extends PreferenceFragment {

        private final String PREF_ADD_ACCOUNT = "prefAddAccount";
        private final String PREF_DELETE_ACCOUNT = "prefDeleteAccount";
        private final String PREF_SEND_FEEDBACK = "prefSendFeedback";
        private final String PREF_ABOUT = "prefAbout";
        
        private Preference addAccount;
        private Preference deleteAccount;
        private Preference sendFeedback;
        private Preference about;

        private AbstractServiceManager cloudSMgr;
        private AccountDAO mAccountDAO;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            mAccountDAO = new AccountDAO(getActivity());

            addPreferencesFromResource(R.xml.preferences);

            addAccount = findPreference(PREF_ADD_ACCOUNT);
            addAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    
                    final String GOOGLE_DRIVE = "Google Drive";
                    final String DROPBOX = "Dropbox";
                    
                    final String[] accountTypes = new String[]{GOOGLE_DRIVE, DROPBOX};
                    
                    ListAdapter adapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.select_dialog_item, android.R.id.text1, accountTypes) {
                        
                            @Override
                            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                                View v = super.getView(position, convertView, parent);
                                TextView tv = (TextView)v.findViewById(android.R.id.text1);
                                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                                tv.setTypeface(Typeface.DEFAULT_BOLD);
    
                                //Put the image on the TextView
                                int accountImage = 0;
                                if (accountTypes[position].equals(GOOGLE_DRIVE)) {
                                    accountImage = R.drawable.googledrive;
                                } else if (accountTypes[position].equals(DROPBOX)) {
                                    accountImage = R.drawable.dropbox;
                                }
                                
                                tv.setCompoundDrawablesWithIntrinsicBounds(accountImage, 0, 0, 0);
    
                                //Add margin between image and text (support various screen densities)
                                int margin = (int) (10 * getResources().getDisplayMetrics().density + 0.5f);
                                tv.setCompoundDrawablePadding(margin);
    
                                return v;
                            }
                        
                    };
                    
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    
                    builder
                        .setTitle("Add account")
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (accountTypes[item].equals(GOOGLE_DRIVE)) {
                                    cloudSMgr = new GoogleDriveServiceManager(getActivity());
                                } else if (accountTypes[item].equals(DROPBOX)) {
                                    cloudSMgr = new DropboxServiceManager(getActivity());
                                }
                                cloudSMgr.login();
                            }
                            
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                
                            }
                        });
                    
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return false;
                }
            });
            
            deleteAccount = findPreference(PREF_DELETE_ACCOUNT);
            deleteAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    
                    final List<Account> accounts = mAccountDAO.getAllAccounts();
                    
                    if (accounts.size() <= 0) 
                        return false;
                    
                    ListAdapter adapter = new ArrayAdapter<Account>(getActivity(),
                        android.R.layout.select_dialog_item, android.R.id.text1, accounts) {
                        
                            @Override
                            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                                View v = super.getView(position, convertView, parent);
                                TextView tv = (TextView)v.findViewById(android.R.id.text1);
                                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                                tv.setText(accounts.get(position).getDisplayName());
                                
                                //Put the image on the TextView
                                int accountImage = 0;
                                if (accounts.get(position) instanceof GoogleDriveAccount) {
                                    accountImage = R.drawable.googledrive;
                                    
                                } else if (accounts.get(position) instanceof DropboxAccount) {
                                    accountImage = R.drawable.dropbox;
                                }
                                
                                tv.setCompoundDrawablesWithIntrinsicBounds(accountImage, 0, 0, 0);
    
                                //Add margin between image and text (support various screen densities)
                                int margin = (int) (10 * getResources().getDisplayMetrics().density + 0.5f);
                                tv.setCompoundDrawablePadding(margin);
    
                                return v;
                            }
                        
                    };
                    
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    
                    builder
                        .setTitle("Remove account")
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                mAccountDAO.removeAccount(accounts.get(item).getId());
                                FileUtils.deleteCacheDirByAccount(accounts.get(item).getId());
                            }
                            
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                
                            }
                        });
                    
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    return false;
                }
            });
            
            about = findPreference(PREF_ABOUT);
            about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder
                        .setTitle("SimpleCloud v1.0.0")
                        .setMessage(
                            "\n======= Contribution ======="
                            + "\n"
                            + "\nDang Tran Ngu - Project Lead"
                            + "\nDao Minh Hai - Developer"
                            + "\nNguyen Xuan Chien - Developer"
                            + "\n"
                            + "\n========================"
                            + "\n"
                            + "\nVersion 1.0.0"
                            + "\n-------------"
                            + "\n* Support Dropbox, GoogleDrive"
                            + "\n  - Copy/Move/Download/Upload"
                            + "\n  - Multiple selection"
                            + "\n  - Cache view"
                            + "\n  - Bookmarks"
                            + "\n  - Breadcrumb browsing"
                            + "\n  - Add/Edit/Delete Account"
                            + "\n")

                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }
                        });

                    AlertDialog dialog = builder.show();
                    TextView message = (TextView) dialog.findViewById(android.R.id.message);
                    message.setTextSize(14);
                    return false;
                    
                }
            });
            
            sendFeedback = findPreference(PREF_SEND_FEEDBACK);
            sendFeedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent i = new Intent(android.content.Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[] { "ngu.dangtran@gmail.com" });
                    i.putExtra(Intent.EXTRA_SUBJECT, "SimpleCloud feedback");
                    i.putExtra(Intent.EXTRA_TEXT,
                        "Hi Ngu,"
                            + "\n"
                            + "\n"
                            + "\n"
                            + "\nDEVICE INFORMATION:"
                            + "\n* MODEL: " + android.os.Build.MODEL
                            + "\n* DEVICE: " + android.os.Build.DEVICE
                            + "\n* BRAND: " + android.os.Build.BRAND
                            + "\n* HOST: " + android.os.Build.HOST
                            + "\n* SDK VERSION: " + android.os.Build.VERSION.SDK_INT
                            + "\n"
                            + "\n"
                            + "Best regards,");
                    
                    File log = FileUtils.writeLog();
                    if (log != null && log.exists()) {
                        Uri path = Uri.fromFile(log);
                        i.putExtra(Intent.EXTRA_STREAM, path);
                    }
                    
                    startActivity(i);
                    return false;
                    
                }
            });
        }


        /**
         * Handle result from user's picked google account
         */
        @Override
        public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
            switch (requestCode) {
                case GoogleDriveServiceManager.REQUEST_ACCOUNT_PICKER:
                    if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                        String accName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                        if (accName != null) {
                            if (!NetworkUtils.isConnected(getActivity())){
                                return;
                            }
                            cloudSMgr.addAccount(accName);
                        }
                    }
                    break;

                case GoogleDriveServiceManager.REQUEST_AUTHORIZATION:
                    if (resultCode != RESULT_OK) {
                        cloudSMgr.login();
                    }
                    break;
            }
        }

        /**
         * Handle result from Dropbox authentication page
         */
        @Override
        public void onResume() {
            super.onResume();
            if (cloudSMgr != null && cloudSMgr instanceof DropboxServiceManager)
                cloudSMgr.addAccount(null); // accName will be chosen in the Dropbox auth. page
        }

    }

}
