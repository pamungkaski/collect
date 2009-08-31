/*
 * Copyright (C) 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.odk.collect.android;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Activity to upload completed forms.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * 
 */
public class InstanceUploaderActivity extends Activity implements InstanceUploaderListener {

    private final static int PROGRESS_DIALOG = 1;
    private final static String KEY_TOTALCOUNT = "totalcount";
    private ProgressDialog mProgressDialog;

    private InstanceUploaderTask mInstanceUploaderTask;
    private int totalCount = -1;

    // TODO: don't hard code this
    private String mUrl = "http://opendatakit.appspot.com/submission";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get instances to upload
        Intent i = getIntent();
        ArrayList<String> instances = i.getStringArrayListExtra(SharedConstants.KEY_INSTANCES);
        if (instances == null) {
            // nothing to upload
            return;
        }

        mInstanceUploaderTask = (InstanceUploaderTask) getLastNonConfigurationInstance();
        if (mInstanceUploaderTask == null) {

            // setup dialog and upload task
            showDialog(PROGRESS_DIALOG);
            mInstanceUploaderTask = new InstanceUploaderTask();
            mInstanceUploaderTask.setUploadServer(mUrl);
            totalCount = instances.size();

            // convert array list to an array
            String[] sa = instances.toArray(new String[totalCount]);
            mInstanceUploaderTask.execute(sa);
        }
    }


    // TODO: if uploadingComplete() when activity backgrounded, won't work.
    public void uploadingComplete(ArrayList<String> result) {

        int resultSize = result.size();
        if (resultSize == totalCount) {
            Toast.makeText(this, getString(R.string.upload_all_successful, totalCount),
                    Toast.LENGTH_LONG).show();
        } else {
            String s = totalCount - resultSize + " of " + totalCount;
            Toast.makeText(this, getString(R.string.upload_some_failed, s), Toast.LENGTH_LONG)
                    .show();
        }

        // for each path, update the status
        FileDbAdapter fda = new FileDbAdapter(this);
        for (int i = 0; i < resultSize; i++) {
            fda.open();
            //fda.updateFile(result.get(i), FileDbAdapter.STATUS_SUBMITTED);
            fda.close();
        }

        finish();
    }


    public void progressUpdate(int progress, int total) {
        mProgressDialog.setMax(total);
        mProgressDialog.setProgress(progress);
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                mInstanceUploaderTask.setUploaderListener(null);
                                finish();
                            }
                        };
                mProgressDialog.setTitle(getString(R.string.uploading_data));
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setMax(0);
                mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                return mProgressDialog;
        }
        return null;
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        totalCount = savedInstanceState.getInt(KEY_TOTALCOUNT);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TOTALCOUNT, totalCount);
    }


    @Override
    public Object onRetainNonConfigurationInstance() {
        return mInstanceUploaderTask;
    }


    @Override
    protected void onDestroy() {
        mInstanceUploaderTask.setUploaderListener(null);
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        if (mInstanceUploaderTask != null) {
            mInstanceUploaderTask.setUploaderListener(this);
        }
        super.onResume();
    }

}