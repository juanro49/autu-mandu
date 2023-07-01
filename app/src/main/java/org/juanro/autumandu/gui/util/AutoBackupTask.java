package org.juanro.autumandu.gui.util;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import org.juanro.autumandu.R;
import org.juanro.autumandu.util.backup.Backup;


public class AutoBackupTask extends AsyncTask<Void, Void, Boolean> {
    private WeakReference<Context> mContext;

    public AutoBackupTask(Context context) {
        mContext = new WeakReference<>(context);
    }

    @Override
    protected Boolean doInBackground(Void... anything) {
        Context context = mContext.get();
        if (context != null) {
            Backup backup = new Backup(context);
            return backup.autoBackup();
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Context context = mContext.get();
        if (result != null && context != null) {
            Toast.makeText(context, (result ? R.string.toast_auto_backup_succeeded :
                R.string.toast_auto_backup_failed), Toast.LENGTH_SHORT).show();
        }
    }
}
