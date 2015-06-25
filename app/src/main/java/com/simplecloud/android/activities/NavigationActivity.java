package com.simplecloud.android.activities;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.simplecloud.android.R;
import com.simplecloud.android.activities.BookmarkFragment.OnBookmarkItemClickListener;
import com.simplecloud.android.activities.NavigationFragment.OnHomeFragmentListener;
import com.simplecloud.android.adapters.NavigationDrawerItemAdapter;
import com.simplecloud.android.adapters.NavigationDrawerItemAdapter.OnDrawerBookmarkIconCLickListener;
import com.simplecloud.android.background.CopyFilesAsync.OnDrawerProgressBarUpdateListener;
import com.simplecloud.android.background.CopyManager;
import com.simplecloud.android.background.CopyManager.CopyState;
import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.common.utils.FileUtils;
import com.simplecloud.android.common.utils.SharedPrefUtils;
import com.simplecloud.android.database.AccountDAO;
import com.simplecloud.android.models.accounts.Account;
import com.simplecloud.android.models.accounts.Account.AccountType;
import com.simplecloud.android.models.accounts.DropboxAccount;
import com.simplecloud.android.models.accounts.GoogleDriveAccount;
import com.simplecloud.android.models.drawers.DrawerItem;
import com.simplecloud.android.models.drawers.DropboxDrawerItem;
import com.simplecloud.android.models.drawers.ExitAppDrawerItem;
import com.simplecloud.android.models.drawers.GoogleDrawerItem;
import com.simplecloud.android.models.drawers.HomeDrawerItem;
import com.simplecloud.android.models.drawers.SettingsDrawerItem;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;


public class NavigationActivity extends Activity implements OnDrawerProgressBarUpdateListener, 
        OnDrawerBookmarkIconCLickListener, OnBookmarkItemClickListener, OnHomeFragmentListener {

    protected static final String LOGCAT = NavigationActivity.class.getSimpleName();
    private final String NAV_FRAGMENT_TAG = "NAV_FRAGMENT_ID_";
    private final String BM_FRAGMENT_TAG = "BM_FRAGMENT";
    private final String NAV_DRAWER_PROGRESS_BAR = "drawer_progress_bar";
    private final String NAV_DRAWER_PROGRESS = "drawer_progress";

    private DrawerLayout mDrawerLayout;
    private LinearLayout mDrawerLinear;
    private ListView mDrawerList;
    private NavigationDrawerItemAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    
    private static RelativeLayout mProgressBarLayout;
    private static ProgressBar mProgressBar; // Must be static to retain progress on screen rotation
    private static TextView mProgressMessage;
    private float copyProgress;
    
    private AccountDAO mAccountDAO;
    private List<Account> mAccounts;
    private static int mAccId; // 0 for Home
    
    private SparseArray<NavigationFragment> mNavFragments = new  SparseArray<NavigationFragment>();
    
    private NavigationFragment mNavFragment;
    private BookmarkFragment mBmFragment;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);
        
        FileUtils.setDefaultApplicationDirectory();
        
        mAccountDAO = new AccountDAO(this);
        mAccounts = mAccountDAO.getAllAccounts();

        initNavigationDrawer();
   
        if (savedInstanceState != null) {
            retrieveFragments();
        }
        
        // Make sure the HomeFragment is always initialized first
        if (mNavFragments.get(0) == null)
            mAccId = 0;
        
        Log.d(LOGCAT, "onCreate() " + mAccId);
        
        if (mBmFragment == null)
            displayNavFragment(mAccId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (mNavFragment != null) {
            boolean isBacked = mNavFragment.onBackPressed();
            if (!isBacked) {
                NavigationActivity.this.finish();
            }
        }
    }

    /**
     * Return from SettingsActivity onActionBarHomeButtonClicked
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Account list might change in case of Add/remove account
        mAccounts = mAccountDAO.getAllAccounts(); 
        initNavigationDrawer();
        displayNavFragment(mAccId);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        
        CopyManager copySMgr = CopyManager.getCopyManager();
        
        if (mProgressBarLayout != null && mProgressBarLayout.getVisibility() == View.VISIBLE) {
            float progress = 0f;
            if (!copySMgr.isIndeterminate()) {
                progress = ((float) mProgressBar.getProgress()) / mProgressBar.getMax();
            }

            savedInstanceState.putFloat(NAV_DRAWER_PROGRESS, progress);
            savedInstanceState.putBoolean(NAV_DRAWER_PROGRESS_BAR, true );
        } 
        
        super.onSaveInstanceState(savedInstanceState);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        
        // Rebuild the drawer progress bar in case of screen rotation
        if (savedInstanceState.getBoolean(NAV_DRAWER_PROGRESS_BAR)) {
            
            CopyManager copySMgr = CopyManager.getCopyManager();
            onPrepareProgressBar();
            
            if (copySMgr.getSize() >= 0) {
                copyProgress = savedInstanceState.getFloat(NAV_DRAWER_PROGRESS);
                onStartProgressBar();
            }
            
            if (copySMgr.getState() == CopyState.IDLE) {
                onFinishedProgress(copySMgr.getError());
            }
        }
        
    }
    
    /**
     * Setup the navigation drawer, including the item lists, onClick() actions
     */
    private void initNavigationDrawer() {
        // Generate the list of items to show in the drawer
        List<DrawerItem> drawerItems = new ArrayList<DrawerItem>();
        
        drawerItems.add(new DrawerItem("Local"));
        drawerItems.add(new HomeDrawerItem());
        
        if (mAccounts.size() > 0) {
            drawerItems.add(new DrawerItem("Accounts"));
            for (Account acc : mAccounts) {
                if (acc instanceof GoogleDriveAccount) {
                    drawerItems.add(new GoogleDrawerItem(acc.getId(), acc.getDisplayName()));
                } else if (acc instanceof DropboxAccount) {
                    drawerItems.add(new DropboxDrawerItem(acc.getId(), acc.getDisplayName()));
                }
            }
        }
        
        drawerItems.add(new DrawerItem("Options"));
        drawerItems.add(new SettingsDrawerItem());
        drawerItems.add(new ExitAppDrawerItem());

        // 
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLinear = (LinearLayout) findViewById(R.id.left_drawer);
        mDrawerList = (ListView) findViewById(R.id.left_drawer_list);
        //mDrawerList.setVerticalScrollBarEnabled(false); 
        
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        
        mDrawerAdapter = new NavigationDrawerItemAdapter(this, drawerItems);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {
    
            @Override
            public void onItemClick(AdapterView<?> parentUId, View view, int position, long id) {
                displayView(position);
            }
        });
    
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        
        mDrawerToggle = new ActionBarDrawerToggle(this, 
                mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                //TODO: action bar show when rotating with navigation drawer
                
                if (getActionBar() != null && getActionBar().getCustomView() != null)
                    getActionBar().getCustomView().setVisibility(View.VISIBLE);
            }
    
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (getActionBar() != null && getActionBar().getCustomView() != null)
                    getActionBar().getCustomView().setVisibility(View.GONE);
            }
        };
    
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }
    
    public static int getAccountId() {
        return mAccId;
    }
    
    /**
     * Display the view based on the selected drawer item
     * @param position  the position of the item in the drawer list
     */
    private void displayView(int position) {
        DrawerItem item = (DrawerItem) mDrawerList.getAdapter().getItem(position);
        if (item.getTitle() != null) 
            return;
        
        if (item instanceof ExitAppDrawerItem) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                .setTitle("Exit app")
                .setMessage("All running tasks (copy, refresh ...) will be cancelled")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    
                    public void onClick(DialogInterface dialog, int whichButton) {
                        for (int i = 0; i < mNavFragments.size(); i++) {
                            mNavFragments.valueAt(i).cancelAllBackgroundTasks();
                            
                        }
                        NavigationActivity.this.finish();
                    }
 
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        
                    }
                });

            AlertDialog alert = builder.create();
            alert.show();
            alert.getWindow().getAttributes();

            TextView textView = (TextView) alert.findViewById(android.R.id.message);
            textView.setTextSize(14);
            textView.setTypeface(Typeface.DEFAULT_BOLD, Typeface.ITALIC);
            
        } else if (item instanceof SettingsDrawerItem) {
            Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivityForResult(intent, 0);
            
        } else {
            displayNavFragment(item.getId());
        }
    }

    /**
     * Diplay the selected navigation fragment (or account) 
     * @param accId  the id of the selected account
     */
    private void displayNavFragment(int accId) {

        Account acc = null;
        for (Account a : mAccounts) {
            if (a.getId() == accId) {
                acc = a;
                break;
            }
        }
        
        // Make sure accId is correct
        if (accId > 0 && acc == null) {
            accId = 0;
        }
        
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        
        if (mNavFragments.get(accId) == null) {
            
            if (acc == null) {
                mNavFragment = new HomeFragment();
            } else if (acc.getAccountType() == AccountType.GOOGLEDRIVE) {
                mNavFragment = new GoogleDriveFragment();
            } else if (acc.getAccountType() == AccountType.DROPBOX) {
                mNavFragment = new DropboxFragment();
            }
            
            mNavFragments.put(accId, mNavFragment);
            Bundle bundle = new Bundle();
            bundle.putInt(NavigationFragment.ACCOUNT_ID, accId);
            mNavFragment.setArguments(bundle);
            transaction.add(R.id.content_frame, mNavFragment, NAV_FRAGMENT_TAG + accId);
        }
        
        if (mBmFragment != null) {
            transaction.hide(mBmFragment);
        }
        
        for (int i = 0; i < mNavFragments.size(); i++) {
            int key = mNavFragments.keyAt(i);
            if (key == accId) {
                mAccId = accId;
                mNavFragment = mNavFragments.get(key);
                transaction.show(mNavFragment);
            } else {
                transaction.hide(mNavFragments.get(key));
            }
        }
        
        transaction.commit();
        
        int position = getPositionByAccId(accId);
        
        DrawerItem item = (DrawerItem) mDrawerList.getAdapter().getItem(position);
        
        mNavFragment.generateActionBarBreadcrumbsNavigation();
        getActionBar().setIcon(item.getIcon());
        
        mDrawerList.setItemChecked(position, true);
        mDrawerList.setSelection(position);
        mDrawerLayout.closeDrawer(mDrawerLinear);
    }

    /** 
     * Retrieve the navigation fragment(s) when activity is recreated (screen rotation)
     */
    private void retrieveFragments() {
        FragmentManager fm = getFragmentManager();
        
        for (int i = 0; i < mDrawerList.getAdapter().getCount(); i++) {
            int itemId = ((DrawerItem) mDrawerList.getAdapter().getItem(i)).getId();
            if (itemId >= 0) {
                NavigationFragment f = (NavigationFragment)fm.findFragmentByTag(NAV_FRAGMENT_TAG + itemId);
                if (f != null) {
                    mNavFragments.put(itemId, f);
                }
            }
        }
        mBmFragment = (BookmarkFragment)fm.findFragmentByTag(BM_FRAGMENT_TAG);
    }

    private int getPositionByAccId(int accId) {
        for (int i = 0; i < mDrawerList.getAdapter().getCount(); i++) {
            DrawerItem item = (DrawerItem) mDrawerList.getAdapter().getItem(i);
            if (item.getId() == accId) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onBookmarkIconClick(DrawerItem item) {

        // Change text size and font of selected position by notifying the adapter
        if ( mAccId != item.getId()) {
            mAccId = item.getId();
            mDrawerAdapter.notifyDataSetChanged();
        }
        
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        
        if (mBmFragment == null) {
            mBmFragment = new BookmarkFragment();
            transaction.add(R.id.content_frame, mBmFragment, BM_FRAGMENT_TAG);
        }
        mBmFragment.populateBookmarkList(this, mAccId);
        
        // Make sure the associated navigation fragment (with this bookmark) is generated
        if (mNavFragments.get(mAccId) == null) {
            displayNavFragment(mAccId);
        }
        
        transaction.show(mBmFragment).hide(mNavFragment);
        transaction.commit();
        
        getActionBar().setIcon(item.getIcon());
        getActionBar().setDisplayShowCustomEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setTitle(R.string.bookmarks);
        
        mDrawerLayout.closeDrawer(mDrawerLinear);

    }
    
    public void onPrepareProgressBar() {
        Log.d(LOGCAT, "onPrepareProgressBar()");
        mProgressBar = (ProgressBar) findViewById(R.id.drawer_progressbar);
        mProgressBar.setIndeterminate(true);
        copyProgress = 0;

        // Show the progress bar
        mProgressBarLayout = (RelativeLayout) mProgressBar.getParent();
        mProgressBarLayout.setVisibility(View.VISIBLE);
        
        mDrawerLayout.openDrawer(mDrawerLinear);
        
        CopyManager copySmgr = CopyManager.getCopyManager();
        
        DirectoryObject dstDir = copySmgr.getDestination();
        final int accId = dstDir.getAccId();
        final String destUId = dstDir.getUId();
        
        // Top text view contains the destination information
        TextView tv1 = (TextView)findViewById(R.id.drawer_progress_destination);
        
        // Add an Image to the text view
        int position = getPositionByAccId(accId);
        DrawerItem drawer = (DrawerItem) mDrawerList.getAdapter().getItem(position);
        
        int px = (int)tv1.getTextSize();
        Drawable img = getBaseContext().getResources().getDrawable(drawer.getIcon());
        img.setBounds(0, 0, px, px);
        tv1.setCompoundDrawables(img, null, null, null);
        
        int margin = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
        tv1.setCompoundDrawablePadding(margin);
        tv1.setText("/ ... /" + dstDir.getName());
        
        // Bottom text view contains the copy information (size, number of files)
        mProgressMessage = (TextView)findViewById(R.id.drawer_progress_message);
        mProgressMessage.setText("Calculating ...");
        
        // Left button: go to destination directory
        Button bt1 = (Button) findViewById(R.id.drawer_progress_button_left);
        bt1.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                displayNavFragment(accId);
                DirectoryObject d = new DirectoryObject();
                d.setUId(destUId);
                mNavFragment.onDirectoryObjectClick(d);//TODO: smooth scroll
            }
        });
        
        // Right button: cancel the copy process
        Button bt2 = (Button) findViewById(R.id.drawer_progress_button_right);
        bt2.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                final ViewGroup.LayoutParams lp = mProgressBarLayout.getLayoutParams();
                final int originalHeight = mProgressBarLayout.getHeight();
                long mAnimationTime = NavigationActivity.this
                    .getResources().getInteger(android.R.integer.config_shortAnimTime);
                ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

                animator.addListener(new AnimatorListenerAdapter() {
                    
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressBarLayout.setVisibility(View.GONE);
                        CopyManager.getCopyManager().cancel();
                        
                        // Reset view presentation
                        mProgressBarLayout.setAlpha(1f);
                        mProgressBarLayout.setTranslationX(0);
                        lp.height = originalHeight;
                        mProgressBarLayout.setLayoutParams(lp);
                    }
                });

                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        lp.height = (Integer) valueAnimator.getAnimatedValue();
                        mProgressBarLayout.setLayoutParams(lp);
                    }
                });

                animator.start();
            }
        });
 
    }

    @Override
    public void onStartProgressBar() {
        Log.d(LOGCAT, "onStartProgressBar()");
        CopyManager copySmgr = CopyManager.getCopyManager();
        
        DirectoryObject dstDir = copySmgr.getDestination();
        final int accId = dstDir.getAccId();
        int numOfFiles = copySmgr.getSourceFiles().size();
        
        long max = copySmgr.getSize();
        
        // Top text view contains the destination information
        TextView tv1 = (TextView)findViewById(R.id.drawer_progress_destination);
        
        // Add an Image to the text view
        int position = getPositionByAccId(accId);
        DrawerItem drawer = (DrawerItem) mDrawerList.getAdapter().getItem(position);
        
        int px = (int)tv1.getTextSize();
        Drawable img = getBaseContext().getResources().getDrawable(drawer.getIcon());
        img.setBounds(0, 0, px, px);
        tv1.setCompoundDrawables(img, null, null, null);
        
        int margin = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
        tv1.setCompoundDrawablePadding(margin);
        tv1.setText("/ ... /" + dstDir.getName());
        
        // Bottom text view contains the copy information (size, number of files)
        mProgressMessage.setText("Copying " + numOfFiles + 
            " item" + (numOfFiles > 1 ? "s" : "") + " (" + FileUtils.readableFileSize(max) + ")");
        
        if (!copySmgr.isIndeterminate()) {
            // Workaround: progress bar only accept int value < ~2GB (2^31 - 1)
            if (max > Integer.MAX_VALUE) {
                max = max/100; 
            }
            mProgressBar.setIndeterminate(false);
            mProgressBar.setMax((int)max);
            mProgressBar.setProgress((int)(copyProgress * max));
        }
        
    }
    
    @Override
    public synchronized void onIncrementProgress(float rate) {
        mProgressBar.incrementProgressBy((int) (mProgressBar.getMax()*rate));
    }

    @Override
    public synchronized void onFinishedProgress(int err) {
        mProgressBar.setIndeterminate(false);
        mProgressBar.setProgress(mProgressBar.getMax());
        Log.d(LOGCAT, "onFinishedProgress()");
        
        String message = null;
        
        if (err == ErrorCode._NO_ERROR) {
            message = mProgressMessage.getText().toString().replace(
                "Copying", "Copied").concat(" successfully");
        } else {
            message = "Error: " + ErrorCode.getErrorMessage(err);
        }
        
        mProgressMessage.setText(message);
    }

    @Override
    public void onBookmarkItemClick(FileSystemObject file) {
        if (file instanceof DirectoryObject) {
            displayNavFragment(mAccId); // View the nav fragment associated with this directory
            mNavFragment.onDirectoryObjectClick((DirectoryObject)file);
        } else {
            mNavFragment.onFileObjectClick((FileObject)file);
        }
    }

    @Override
    public void onRefreshDownloadDirectoryView() {
        HomeFragment hF = (HomeFragment)mNavFragments.get(0);
        
        if (hF.getCurrentDirectory().getUId()
                .equals(SharedPrefUtils.getDownloadDirectory(this))) {
            hF.refreshView();
        }
    }

}
