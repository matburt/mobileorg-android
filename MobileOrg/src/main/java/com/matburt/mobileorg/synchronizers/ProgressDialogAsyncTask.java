package com.matburt.mobileorg.synchronizers;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

abstract public class ProgressDialogAsyncTask<Params, Void, Result> extends AsyncTask<Params, Void, Result> {
    protected Context context;
    private ProgressDialog progress;

    public ProgressDialogAsyncTask(Context context) {
        this.context = context;
    }

    protected abstract void _onPreExecute();

    protected abstract void _onPostExecute(Result result);

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        _onPreExecute();
//        progress = new ProgressDialog(context);
//        progress.setMessage(context.getString(R.string.please_wait));
//        progress.setTitle(context.getString(R.string.signing_in));
//        progress.show();
    }

    @Override
    protected void onPostExecute(Result result) {
//        progress.dismiss();
        _onPostExecute(result);
    }
}