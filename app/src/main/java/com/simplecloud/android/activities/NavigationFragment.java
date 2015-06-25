package com.simplecloud.android.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.simplecloud.android.R;
import com.simplecloud.android.adapters.FileSystemObjectAdapter;
import com.simplecloud.android.background.CopyManager;
import com.simplecloud.android.background.CopyManager.CopyState;
import com.simplecloud.android.background.CopyManager.CopyType;
import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.common.TaskCode;
import com.simplecloud.android.common.ui.NoOverscrollSwipeRefreshLayout;
import com.simplecloud.android.common.ui.NoOverscrollSwipeRefreshLayout.OnRefreshListener;
import com.simplecloud.android.common.utils.DateUtils;
import com.simplecloud.android.common.utils.FileUtils;
import com.simplecloud.android.common.utils.SharedPrefUtils;
import com.simplecloud.android.common.utils.ZipUtils;
import com.simplecloud.android.database.FileSystemObjectDAO;
import com.simplecloud.android.models.ObjectFactory;
import com.simplecloud.android.models.accounts.Account.AccountType;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.LocalDirectory;
import com.simplecloud.android.models.files.LocalFile;


/**
 * @author ngu
 *
 */
public abstract class NavigationFragment extends ListFragment 
        implements Handler.Callback, OnRefreshListener {
    
    protected static final String LOGCAT = NavigationFragment.class.getSimpleName();
    public static final String ACCOUNT_ID = "account_id";
    
    protected FileSystemObjectAdapter mAdapter;
    
    protected int mAccId;
    protected DirectoryObject mDirectory; // current directory. This directory should be instantiated only once
    
    protected List<FileSystemObject> mChildren = new ArrayList<FileSystemObject>();
    protected Stack<DirectoryObject> mPrevDirectories = new Stack<DirectoryObject>();
    
    protected NoOverscrollSwipeRefreshLayout mSwipeRefreshLayout;
    protected Handler mHandler;
    protected OnHomeFragmentListener mHomeFragmentListener; // Communicate with the home fragment
    
    protected static ProgressDialog mProgressDialog; // Only 1 dialog for all fragments
    protected static CopyManager mCopyProcess = CopyManager.getCopyManager();
    
    protected FileSystemObjectDAO mFileDAO;
    
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        mHomeFragmentListener = (OnHomeFragmentListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAccId = getArguments().getInt(ACCOUNT_ID);
        mHandler = new Handler(this);
        mFileDAO = new FileSystemObjectDAO(getActivity());
        mAdapter = new FileSystemObjectAdapter(getActivity(), mChildren);
        
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        setRetainInstance(true);
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.navigation_fragment, container, false);
        mSwipeRefreshLayout = (NoOverscrollSwipeRefreshLayout) rootView
            .findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setEnabled(false);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOGCAT, "onActivityCreated() " + mAccId);
        
        initFileListView();
        setListAdapter(mAdapter);
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if ((mProgressDialog != null) && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        mProgressDialog = null;
    }
    
    @Override
    public void onRefresh() {
        // No implementation, disable swipe to refresh by default 
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_menu_overflow, menu);
        
        menu.findItem(R.id.item_paste).setVisible(
            (mCopyProcess.getState() == CopyState.PREPARE) ? true : false);

        menu.findItem(R.id.item_download_dir).setVisible(
            (this instanceof HomeFragment) ? true : false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_paste:
                pasteItemsDialog();
                break;

            case R.id.item_new_folder:
                createNewItemDialog(false);
                break;

            case R.id.item_new_file:
                createNewItemDialog(true);
                break;
                
            case R.id.item_properties:
                viewPropertiesDialog(mDirectory);
                break;

            case R.id.item_bookmark:
                bookmarkFileOrDirectoryObject(mDirectory);
                break;
                
            case R.id.item_download_dir:
                SharedPrefUtils.setDownloadDirectory(
                    getActivity(), ((LocalDirectory)mDirectory).getFile());
                showMessage("All data will be downloaded to this folder using the 'Download' button");
                break;
                
            default:
                break;                
        }

        return false;
    }

    /**
     * Populate the listview with files and/or subdirectories of the current directory
     */
    private void initFileListView() {
        final ListView explorerListView = getListView();
        explorerListView.setAdapter(mAdapter);
        explorerListView.setTextFilterEnabled(true);
        
        explorerListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentUId, View view, int position, long id) {
                FileSystemObject f = (FileSystemObject)explorerListView.getAdapter().getItem(position);

                if (mCopyProcess.getState() == CopyState.IN_PROGRESS
                        && f.getUId().equals(CopyManager.FILE_IN_PROGRESS_UID)) {
                    return;
                }
                
                if (f instanceof DirectoryObject) {
                    onDirectoryObjectClick((DirectoryObject) f);
                } else {
                    onFileObjectClick((FileObject) f);
                }
            }
        });
        
        explorerListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        explorerListView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            private int selected = 0;
            private Menu _menu;

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mAdapter.clearSelection();
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                
                getActivity().getMenuInflater().inflate(R.menu.action_menu_contextual, menu);
                menu.findItem(R.id.item_download).setVisible(
                    (NavigationFragment.this instanceof CloudStorageFragment) ? true : false);
                selected = 0;
                _menu = menu;
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                List<FileSystemObject> files = new ArrayList<FileSystemObject>();
                for (int position : mAdapter.getCurrentCheckedPosition()) {
                    files.add(mAdapter.getItem(position));
                }

                switch (item.getItemId()) {

                    case R.id.item_delete:
                        deleteItemsDialog(files);
                        break;
                        
                    case R.id.item_download:
                        onDownloadMenuItemSelected(files);
                        break;                        

                    case R.id.item_copy:
                        onCopyMenuItemSelected(files, false);
                        break;

                    case R.id.item_move:
                        onCopyMenuItemSelected(files, true);
                        break;
                        
                    case R.id.item_rename:
                        renameItemDialog(files.get(0));
                        break;
 
                    case R.id.item_bookmark:
                        for (FileSystemObject f : files) {
                            bookmarkFileOrDirectoryObject(f);
                        }
                        showMessage("The bookmark was added successfully");
                        break;
                        
                    case R.id.item_properties:
                        viewPropertiesDialog(files.get(0));
                        break;
                        
                    case R.id.item_select_all:
                        for (int i = 0; i < mAdapter.getCount(); i++) {
                            if (!mAdapter.isPositionChecked(i))
                                // simulate click action on all items on the list
                                explorerListView.performItemClick(
                                    explorerListView.getChildAt(i), i, explorerListView.getItemIdAtPosition(i));
                        }

                        return true;
                        
                    case R.id.item_share:
                        shareFileOrDirectoryObject(files.get(0));
                        break;
    
                    case R.id.item_zip:
                        zipItemsDialog(files);
                        break;
                        
                    default:
                        break;
                }
 
                
                mAdapter.clearSelection();
                selected = 0;
                mode.finish();

                return false;
            }
            
            private void onCopyMenuItemSelected(List<FileSystemObject> files, boolean isMove) {
                if (mCopyProcess.getState() == CopyState.IN_PROGRESS) {
                    showMessage("Please wait until the current copy process finish");
                    return;
                }

                mCopyProcess.prepare(mDirectory, files, null, isMove);
                
                showMessage("Select " + selected + " item(s) to " + ((isMove == true) ? "move" : "copy"));
                getActivity().invalidateOptionsMenu();
            }
            
            private void onDownloadMenuItemSelected(List<FileSystemObject> files) {
                if (mCopyProcess.getState() == CopyState.IN_PROGRESS) {
                    showMessage("Please wait until the current copy process finish");
                    return;
                }
                
                LocalDirectory downloadDir = new LocalDirectory(
                    new File(SharedPrefUtils.getDownloadDirectory(getActivity())));
                
                mCopyProcess.prepare(mDirectory, files, downloadDir, false);

                pasteObjects();
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if (checked) {
                    selected++;
                } else {
                    selected--;
                }
                
                mAdapter.switchSelection(position);
                
                // Some menu items (properties, rename) are only available for single selection only
                boolean isVisible = _menu.findItem(R.id.item_properties).isVisible();
                
                if (isVisible && selected > 1) {
                    _menu.findItem(R.id.item_properties).setVisible(false);
                    _menu.findItem(R.id.item_rename).setVisible(false);
                    _menu.findItem(R.id.item_share).setVisible(false);
                    
                    getActivity().invalidateOptionsMenu();
                } else if (!isVisible && selected == 1) {
                    _menu.findItem(R.id.item_properties).setVisible(true);
                    _menu.findItem(R.id.item_rename).setVisible(true);
                    _menu.findItem(R.id.item_share).setVisible(true);

                    getActivity().invalidateOptionsMenu();
                }
                
            }
        });
        
        registerForContextMenu(explorerListView);
    }

    /** 
     * Generate breadcrumbs navigation on action bar.
     * The following action needs the navigation to be regenerated:
     * <p><ul>
     *  <li> Go to a child directory
     *  <li> Go to the previous directory (back pressed)
     *  <li> Go to a directory in the breadcrumbs view
     *  <li> Update the current directory by swipe down.
     *  </ul></p>
     */
    public void generateActionBarBreadcrumbsNavigation() {
        // Make sure the the fragment is already attached to the activity
        if (getActivity() == null) return;
        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setCustomView(R.layout.actionbar_view);

        final HorizontalScrollView hScroll = (HorizontalScrollView) actionBar
            .getCustomView().findViewById(R.id.actionbar_hscroll);
        
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.post(new Runnable() {
            @Override
            public void run() {
                hScroll.fullScroll(View.FOCUS_RIGHT);
            }
        });
        
        LinearLayout actionBarLayout = (LinearLayout) hScroll.findViewById(R.id.actionbar_hscroll_layout);
        LayoutInflater inflator = (LayoutInflater) getActivity()
            .getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        for (final BreadcrumbsItem item : getBreadcrumbsItems()) {

            final Button button = (Button) inflator.inflate(R.layout.actionbar_item, actionBarLayout, false);
            actionBarLayout.addView(button);
            inflator.inflate( R.layout.actionbar_separator, actionBarLayout, true);
            
            button.setText(item.getName());
            button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    onActionBarBreadcrumbsItemClick(item);
                }
            });
        }
        
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
            | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
    }
    
    /**
     * Open a directory. Previous directory is saved to stack
     * @param dir the directory to open
     */
    public void updateView(DirectoryObject dir) {
        mPrevDirectories.push((DirectoryObject)ObjectFactory.clone(mDirectory));
        mDirectory.setAll(dir);
        refreshView();
        generateActionBarBreadcrumbsNavigation();
    }

    /**
     * Refresh current directory's view
     */
    public void refreshView() {
        mChildren.clear();
        mChildren.addAll(mDirectory.getChildren());
        
        if (mCopyProcess.getState() == CopyState.IN_PROGRESS
                && mDirectory.equals(mCopyProcess.getDestination())) {
            
            FileSystemObject f = mCopyProcess.getFileInProgress();
            boolean exist = false;
            for (FileSystemObject c : mChildren) {
                // TODO: google files can have same names
                if (c.getName().equalsIgnoreCase(f.getName())) {
                    c.setUId(CopyManager.FILE_IN_PROGRESS_UID);
                    c.setSize(f.getSize());
                    exist = true;
                    break;
                }
            }

            // create a dummy file if file is not already partially copied
            if (!exist)
                mChildren.add(f);
        }
        
        mAdapter.sort(new FileSystemObjectAdapter.FileSystemObjectComparator());
    }
    
    /**
     * Attempt to go back to the previously opened directory
     * @return <b>true</b> if successful
     */
    public boolean onBackPressed() {
        DirectoryObject prev = null;
        while (prev == null || prev.getChildren() == null) {
            
            if (mPrevDirectories.empty()) 
                return false;
            
            prev = mPrevDirectories.pop();
            if (prev.equals(mDirectory))
                prev = null;
        }
        
        mDirectory.setAll(prev);
        generateActionBarBreadcrumbsNavigation();
        return true;
    }

    /** 
     * View and select list of items to paste
     * List of items was already saved in ActionUtils
     */
    private void pasteItemsDialog() {
        if (mCopyProcess.getSource().equals(mDirectory)) {
            showMessage("Please choose another directory");
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        final List<FileSystemObject> items = new ArrayList<FileSystemObject>();
        items.addAll(mCopyProcess.getSourceFiles());
        
        final CharSequence[] itemNames = new CharSequence[items.size()];
        boolean[] checked = new boolean[items.size()];
    
        final List<FileSystemObject> existedItems = new ArrayList<FileSystemObject>(); 
        
        // Unchecked files to be overwritten by default
        for (FileSystemObject f : CopyManager.getCopyManager().getSourceFiles()) {
            for (FileSystemObject child : mDirectory.getChildren()) {
                if (f.getName().equals(child.getName())) {
                    existedItems.add(f);
                }
            }
        }
        
        for (int i = 0; i < items.size(); i++) {
            itemNames[i] = StringUtils.abbreviateMiddle(items.get(i).getName(), "...", 25);
            checked[i] = true;
            
            for (FileSystemObject existedItem : existedItems) {
                if (existedItem.getName().equals(items.get(i).getName())) {
                    checked[i] = false;
                }
            }
        }
        
        items.removeAll(existedItems);
        
        builder
            .setMultiChoiceItems(itemNames, checked,
                new DialogInterface.OnMultiChoiceClickListener() {
    
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            items.add(mCopyProcess.getSourceFiles().get(which));
                        } else {
                            items.remove(mCopyProcess.getSourceFiles().get(which));
                        }
                    }
                    
                })
            .setPositiveButton(R.string.action_paste, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    if (items.size() > 0) {
                        //TODO: allow option to overwrite, rename or skip
                        mCopyProcess.prepare(mCopyProcess.getSource(), items, mDirectory, mCopyProcess.isMove());
                        
                        if (mCopyProcess.getCopyType() == CopyType.NOT_SUPPORTED) {
                            showMessage("Action is currently not supported");
                            mCopyProcess.cancel();
                            return;
                        }
                        
                        pasteObjects();
                        
                    } else {
                        showMessage("Please choose at least one item to paste");
                    }
                }
            })
            .setNegativeButton(R.string.action_clear_clipboard, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    mCopyProcess.cancel();
                    getActivity().invalidateOptionsMenu();
                }
            });
    
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /** 
     * Create a new file or directory
     * @param isCreateFile  whether to create a new file or a folder
     */
    private void createNewItemDialog(final boolean isCreateFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText input = new EditText(getActivity());
        input.setText(isCreateFile ? "file.txt" : "Folder");
        showSoftKeyboard(input);
        
        builder
            .setTitle("Enter " + (isCreateFile ? "file" : "folder") + " name")
            .setView(input)
            .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    new CreateFileAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                
                class CreateFileAsync extends AsyncTask<Void, Void, Integer> {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        mProgressDialog = new ProgressDialog(getActivity());
                        mProgressDialog.setMessage("Creating ...");
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.setCanceledOnTouchOutside(false);
                        mProgressDialog.setOnCancelListener(new OnCancelListener() {
                            
                            /*
                             * Cancel the task when this progess dialog is dismissed
                             */
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                CreateFileAsync.this.cancel(true);
                            }
                        });
                        mProgressDialog.show();
                    }

                    @Override
                    protected Integer doInBackground(Void... params) {
                        String name = input.getText().toString();
                        if (isCreateFile) {
                            return createNewFileObject(name);
                        } else {
                            return createNewDirectoryObject(name);
                        }
                    }

                    @Override
                    protected void onPostExecute(Integer err) {
                        if ((mProgressDialog != null) && mProgressDialog.isShowing()) { 
                            mProgressDialog.dismiss();
                        }
                        if (err == ErrorCode._NO_ERROR) 
                            refreshView();
                        else
                            showMessage(ErrorCode.getErrorMessage(err));
                    }

                    @Override
                    protected void onCancelled(Integer err) {
                        if (err == ErrorCode._NO_ERROR) 
                            refreshView();
                        else
                            showMessage(ErrorCode.getErrorMessage(err));
                    }
                    
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });

        builder.show();
    }
    
    /**
     * Dialog to rename a file or a directory
     * @param file  the file or directory to rename
     */
    private void renameItemDialog(final FileSystemObject file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText input = new EditText(getActivity());
        input.setText(file.getName());
        showSoftKeyboard(input);
        
        builder
            .setTitle("Enter new name")
            .setView(input)
            .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int whichButton) {
                    new RenameFileAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                
                class RenameFileAsync extends AsyncTask<Void, Void, Integer> {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        mProgressDialog = new ProgressDialog(getActivity());
                        mProgressDialog.setMessage("Renaming ...");
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.setCanceledOnTouchOutside(false);
                        mProgressDialog.setOnCancelListener(new OnCancelListener() {
                            
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                RenameFileAsync.this.cancel(true);
                            }
                        });
                        mProgressDialog.show();
                    }

                    @Override
                    protected Integer doInBackground(Void... params) {
                        String newName = input.getText().toString();

                        for (FileSystemObject child : mDirectory.getChildren()) {
                            if (child.getName().equalsIgnoreCase(newName)) {
                                if (child instanceof DirectoryObject)
                                    return ErrorCode._DIRECTORY_ALREADY_EXIST;
                                else 
                                    return ErrorCode._FILE_ALREADY_EXIST;
                            }
                        }

                        int err = ErrorCode._NO_ERROR;
                        if (file instanceof FileObject) {
                            err = renameFileObject((FileObject) file, newName);
                        } else {
                            err = renameDirectoryObject((DirectoryObject) file, newName);
                        }

                        if (err == ErrorCode._NO_ERROR)
                            file.setName(newName);
                        
                        return err;
                    }

                    @Override
                    protected void onPostExecute(Integer err) {
                        if ((mProgressDialog != null) && mProgressDialog.isShowing()) { 
                            mProgressDialog.dismiss();
                        }
                        
                        if (err == ErrorCode._NO_ERROR) 
                            refreshView();
                        else
                            showMessage(ErrorCode.getErrorMessage(err));
                    }
                    
                    @Override
                    protected void onCancelled(Integer err) {
                        if (err == ErrorCode._NO_ERROR) 
                            refreshView();
                        else
                            showMessage(ErrorCode.getErrorMessage(err));
                    }
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    
                }
            });

        builder.show();
    }
    
    private void zipItemsDialog(final List<FileSystemObject> files) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText input = new EditText(getActivity());
        input.setText("file.zip");
        showSoftKeyboard(input);
        
        builder
            .setTitle("Enter zip file name")
            .setView(input)
            .setPositiveButton("Zip", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    new ZipFilesAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                
                class ZipFilesAsync extends AsyncTask<Void, Void, Integer> {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        mProgressDialog = new ProgressDialog(getActivity());
                        mProgressDialog.setMessage("Creating ...");
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.setCanceledOnTouchOutside(false);
                        mProgressDialog.setOnCancelListener(new OnCancelListener() {
                            
                            /*
                             * Cancel the task when this progess dialog is dismissed
                             */
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                ZipFilesAsync.this.cancel(true);
                            }
                        });
                        mProgressDialog.show();
                    }

                    @Override
                    protected Integer doInBackground(Void... params) {
                        String name = input.getText().toString();
                        zipFileOrDirectoryObjects(files, name);
                            return ErrorCode._NO_ERROR;
                    }

                    @Override
                    protected void onPostExecute(Integer err) {
                        if ((mProgressDialog != null) && mProgressDialog.isShowing()) { 
                            mProgressDialog.dismiss();
                        }
                        if (err == ErrorCode._NO_ERROR) 
                            refreshView();
                        else
                            showMessage(ErrorCode.getErrorMessage(err));
                    }

                    @Override
                    protected void onCancelled(Integer err) {
                        if (err == ErrorCode._NO_ERROR) 
                            refreshView();
                        else
                            showMessage(ErrorCode.getErrorMessage(err));
                    }
                    
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });

        builder.show();
    }
    
    /**
     * Delete the selected files or directories
     * @param files  the list of files/directories to delete
     */
    private void deleteItemsDialog(final List<FileSystemObject> files) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder
            .setTitle("Delete")
            .setMessage("Do you want to delete all seletected items ?")
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int whichButton) {
                    new DeleteFileAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                
                class DeleteFileAsync extends AsyncTask<Void, Void, Integer> {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        mProgressDialog = new ProgressDialog(getActivity());
                        mProgressDialog.setMessage("Deleting ...");
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.setCanceledOnTouchOutside(false);
                        mProgressDialog.setOnCancelListener(new OnCancelListener() {
                            
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                DeleteFileAsync.this.cancel(true);
                            }
                        });
                        mProgressDialog.show();
                    }

                    @Override
                    protected Integer doInBackground(Void... params) {
                        int err = ErrorCode._NO_ERROR;
                        
                        for (FileSystemObject f : files) {

                            if (f instanceof FileObject) {
                                err = deleteFileObject((FileObject) f);
                            } else {
                                err = deleteDirectoryObject((DirectoryObject) f);
                            }

                            // Stop if there is an error
                            if (err != ErrorCode._NO_ERROR)
                                return err;

                            mDirectory.getChildren().remove(f);
                            
                            if (isCancelled())
                                return 0;
                        }

                        return err;
                    }

                    @Override
                    protected void onPostExecute(Integer err) {
                        if ((mProgressDialog != null) && mProgressDialog.isShowing()) { 
                            mProgressDialog.dismiss();
                        }
                        
                        if (err == ErrorCode._NO_ERROR)
                            refreshView();
                        else
                            showMessage(ErrorCode.getErrorMessage(err));
                    }
                    
                    @Override
                    protected void onCancelled(Integer err) {
                        if (err == ErrorCode._NO_ERROR) 
                            refreshView();
                        else
                            showMessage(ErrorCode.getErrorMessage(err));
                    }

                    @Override
                    protected void onCancelled() {
                        refreshView();
                    }

                    private int deleteDirectoryObject(DirectoryObject dir) {
                        int err = ErrorCode._NO_ERROR;
                        for (FileSystemObject child : dir.getChildren()) {
                            
                            if (child instanceof DirectoryObject) {
                                err = deleteDirectoryObject((DirectoryObject) child);
                            } else {
                                err = deleteFileObject((FileObject) child);
                            }
                            
                            if (isCancelled())
                                return 0;
                        }
                        
                        err = deleteEmptyDirectoryObject(dir);
                        return err;
                    }
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    
                }
            });

        builder.show();
    }

    private void viewPropertiesDialog(FileSystemObject file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Properties p = getObjectProperties(file);
        
        builder.setTitle("Information").setMessage(p.toSpannedText());
        AlertDialog dialog = builder.show();
        
        final TextView message = (TextView) dialog.findViewById(android.R.id.message);
        message.setTextSize(15);
        message.setTypeface(Typeface.SANS_SERIF);
        message.setLineSpacing(1, 1.2f);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                    // Size is calculated in the background. Wait to update the size 
                    while (p.getSize() < 0) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            
                        }
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            message.setText(p.toSpannedText());
                        }
                   });
            }
        });
        t.start();
    }
    
    /**
     * Open a local file in another app
     * @param file  the local file to open
     */
    protected void openFile(File file) {
        if (file == null || !file.exists()) 
            return;
        
        Uri uri = Uri.fromFile(file);
        
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            MimeTypeMap.getFileExtensionFromUrl(uri.toString().toLowerCase()));
    
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, type);
    
        PackageManager packageManager = getActivity().getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        boolean isIntentSafe = activities.size() > 0;
    
        if (isIntentSafe) {
            startActivity(intent);
        } else {
            showMessage("No application available to open this file.");
        }
    }

    protected AccountType getAccountType() {
        if (this instanceof GoogleDriveFragment) {
            return AccountType.GOOGLEDRIVE;
        } else if (this instanceof DropboxFragment) {
            return AccountType.DROPBOX;
        } else
            return null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case TaskCode._REFRESH_VIEW:
                refreshView();
                break;
                
            case TaskCode._REFRESH_COPY_DESTINATION_VIEW:
                if (mCopyProcess.getDestination().equals(mDirectory))
                    refreshView();
                break;
                
            case TaskCode._REFRESH_DOWNLOAD_DIRECTORY_VIEW:
                mHomeFragmentListener.onRefreshDownloadDirectoryView();
                //getListView().smoothScrollToPosition(mAdapter.getCopiedFilePosition());
                //TODO: error IllegalStateException: content view not yet created
                break;
                
            case TaskCode._SHOW_MESSAGE_COPY_FINISHED:
                showMessage("Copied " + FileUtils.readableFileSize(mCopyProcess.getSize()) + " successfully");
                break;

            case ErrorCode._CAN_NOT_COPY_FOLDER_INTO_ITSELF:
                showMessage("Cannot copy a folder into itself. Skip 1 folder");
                break;
                
            default:
                break;
        }
    
        return false;
    }

    public void showMessage(String message) {
        if (getActivity() != null)
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
    
    public void showLongMessage(String message) {
        if (getActivity() != null)
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }
    
    public void showSoftKeyboard(final View view) {

        if (view.requestFocus()) {
            view.postDelayed(new Runnable() {
                
                @Override
                public void run() {
                    InputMethodManager imm = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 100);
        }
    }
    
    public void cancelAllBackgroundTasks() {
        mCopyProcess.cancel();
    }

    public DirectoryObject getCurrentDirectory() {
        return mDirectory;
    }

    protected abstract void onDirectoryObjectClick(DirectoryObject directory);

    protected abstract void onFileObjectClick(FileObject file);

    /** 
     * Paste objects from the clipboard to the destination directory.
     * All information (sources, destination ...) is stored in CopyProcess.getInstance()
     */
    protected abstract void pasteObjects();

    protected abstract int deleteEmptyDirectoryObject(DirectoryObject directory);
    
    protected abstract int deleteFileObject(FileObject file);
    
    protected abstract int createNewDirectoryObject(String newdirName);

    protected abstract int createNewFileObject(String newFileName);

    protected abstract int renameDirectoryObject(DirectoryObject directory, String newName);

    protected abstract int renameFileObject(FileObject file, String newName);
    
    protected abstract void shareFileOrDirectoryObject(FileSystemObject file);
    
    protected abstract void bookmarkFileOrDirectoryObject(FileSystemObject file);
    
    protected abstract void zipFileOrDirectoryObjects(List<FileSystemObject> files, String newName);

    /**
     * Get list of breadcrumbs items of the current directory from root.
     * E.g: /root/dir1/dir2/dir3 => [root, dir1, dir2, dir3]
     * @return the list of breadcrumbs items
     */
    protected abstract List<BreadcrumbsItem> getBreadcrumbsItems();

    /**
     * Action taken when changing directory using action bar navigation
     * @param item the directory to change to
     */
    protected abstract void onActionBarBreadcrumbsItemClick(BreadcrumbsItem item);
    
    protected abstract Properties getObjectProperties(FileSystemObject object);
    
    
    /**
     * Represent a single item in the file navigation menu (action bar)
     */
    protected class BreadcrumbsItem {
        
        private int accId;
        private String name;
        private String uid;
        
        public BreadcrumbsItem(DirectoryObject d) {
            this(d.getAccId(), d.getUId(), d.getName());
        }

        public BreadcrumbsItem(int accId, String uid, String name) {
            this.accId = accId;
            this.uid = uid;
            this.name = name.equals("") ? "/" : name;
        }
        
        public int getAccId() {
            return accId;
        }
        
        public String getName() {
            return name;
        }

        public String getUId() {
            return uid;
        }
    }
    
    
    /**
     * Properties of a file/directory to display on the screen 
     */
    protected class Properties {
        
        private String name;
        private long size;
        private long lastModified;

        public Properties(String name, long size, long lastModified) {
            this.name = name;
            this.size = size;
            this.lastModified = lastModified;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getSize() {
            return size;
        }

        public Spanned toSpannedText() {
            String lastModifiedStr = DateUtils.getDateCurrentTimeZone(lastModified);
            String readableSize = (size < 0) ? "Calculating ..." : FileUtils.readableFileSize(size);
            
            return Html.fromHtml((
                "<b>" + "Name : " + "</b>" + name + "<br>" +
                "<b>" + "Last modified : " + "</b>" + lastModifiedStr + "<br>" +
                "<b>" + "Size : " + "</b>" + readableSize + "<br>").replace(" ", "&nbsp;"));
        }
    }


    /**
     * Communicate with the home fragment
     * @author ngu
     */
    public interface OnHomeFragmentListener {
        
        /**
         * Refresh the download directory
         */
        void onRefreshDownloadDirectoryView();
    }
    
}
