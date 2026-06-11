package org.juanro.autumandu.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serial;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.dao.CarDao;
import org.juanro.autumandu.model.dao.FuelTypeDao;
import org.juanro.autumandu.model.dao.OtherCostDao;
import org.juanro.autumandu.model.dao.RefuelingDao;
import org.juanro.autumandu.model.dao.ReminderDao;
import org.juanro.autumandu.model.dao.StationDao;
import org.juanro.autumandu.model.dao.TireDao;
import org.juanro.autumandu.model.dao.TripDao;
import org.juanro.autumandu.model.dao.TripPrefabDao;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelCategory;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Reminder;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.TireUsage;
import org.juanro.autumandu.model.entity.Trip;
import org.juanro.autumandu.model.entity.TripPrefab;
import org.juanro.autumandu.model.entity.helper.SQLTypeConverters;

@Database(
    entities = {Car.class, FuelType.class, Reminder.class, Refueling.class, OtherCost.class, Station.class, TireList.class, TireUsage.class, Trip.class, TripPrefab.class},
    version = AutuManduDatabase.VERSION
)
@TypeConverters({SQLTypeConverters.class})
public abstract class AutuManduDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "data.db";
    public static final int VERSION = 15;

    public abstract CarDao getCarDao();
    public abstract FuelTypeDao getFuelTypeDao();
    public abstract OtherCostDao getOtherCostDao();
    public abstract RefuelingDao getRefuelingDao();
    public abstract ReminderDao getReminderDao();
    public abstract StationDao getStationDao();
    public abstract TireDao getTireDao();
    public abstract TripDao getTripDao();
    public abstract TripPrefabDao getTripPrefabDao();

    public static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    private static final AtomicReference<AutuManduDatabase> sInstance = new AtomicReference<>();
    private static final String LOG_TAG = "AutuManduDatabase";

    /**
     * Get an instance of the persistent Database used by the app.
     * @param context A Context inside the Database should be used.
     * @return The instance of the Database.
     */
    public static AutuManduDatabase getInstance(Context context) {
        AutuManduDatabase db = sInstance.get();
        if (db == null) {
            synchronized (sInstance) {
                db = sInstance.get();
                if (db == null) {
                    var appContext = context.getApplicationContext();
                    var builder = Room.databaseBuilder(
                            appContext, AutuManduDatabase.class, DATABASE_NAME);

                    builder.addMigrations(
                            new AssetFileBasedMigration(appContext, 2),
                            new AssetFileBasedMigration(appContext, 3),
                            new AssetFileBasedMigration(appContext, 4),
                            new AssetFileBasedMigration(appContext, 5),
                            new AssetFileBasedMigration(appContext, 6),
                            new AssetFileBasedMigration(appContext, 7),
                            new AssetFileBasedMigration(appContext, 8),
                            new AssetFileBasedMigration(appContext, 9),
                            new AssetFileBasedMigration(appContext, 10),
                            new AssetFileBasedMigration(appContext, 11),
                            new AssetFileBasedMigration(appContext, 12),
                            new AssetFileBasedMigration(appContext, 13),
                            new AssetFileBasedMigration(appContext, 14),
                            new AssetFileBasedMigration(appContext, 15)
                    );

                    builder.addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // Se ejecuta solo una vez cuando se crea la base de datos por primera vez.
                            DB_EXECUTOR.execute(() -> {
                                var database = getInstance(appContext);
                                database.getFuelTypeDao().insert(new FuelType(
                                        appContext.getString(R.string.default_fuel_type),
                                        FuelCategory.GASOLINE.getKey()
                                ));
                                database.getStationDao().insert(new Station(
                                        appContext.getString(R.string.default_station)
                                ));
                            });
                        }
                    });

                    db = builder.build();

                    // Perform a sanity check to ensure migrations and schema validation are successful.
                    try {
                        db.getOpenHelper().getWritableDatabase();
                    } catch (DatabaseMigrationException e) {
                        Log.e(LOG_TAG, "Critical migration error detected. Attempting recovery...", e);
                        handleMigrationFailure(appContext, e.getVersion());
                        throw e;
                    } catch (IllegalStateException e) {
                        Log.e(LOG_TAG, "Database schema mismatch detected. Attempting recovery...", e);
                        handleMigrationFailure(appContext, VERSION);
                        throw e;
                    }

                    sInstance.set(db);
                }
            }
        }
        return db;
    }

    /**
     * Attempts to recover from a failed migration by backing up the database file
     * and downgrading the database version so that the migration can be retried after fixes.
     */
    private static void handleMigrationFailure(Context context, int failedVersion) {
        try {
            var dbPath = context.getDatabasePath(DATABASE_NAME);
            if (dbPath.exists()) {
                // 1. Auto-export/backup the database before rollback to allow manual recovery
                exportFailedDatabase(context, dbPath, failedVersion);

                // 2. Perform rollback
                try (var db = android.database.sqlite.SQLiteDatabase.openDatabase(
                        dbPath.getAbsolutePath(), null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)) {
                    int currentVersion = db.getVersion();
                    int targetVersion = failedVersion - 1;
                    if (currentVersion >= failedVersion) {
                        Log.w(LOG_TAG, String.format(Locale.US, "Rolling back database version from %d to %d to allow retry.", currentVersion, targetVersion));
                        db.setVersion(targetVersion);

                        // Version specific cleanup if needed
                        if (failedVersion == 15) {
                            db.execSQL("DROP TABLE IF EXISTS trip");
                            db.execSQL("DROP TABLE IF EXISTS trip_prefab");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to perform migration rollback", e);
        }
    }

    /**
     * Copies the database file to a location accessible by the user for recovery purposes.
     * Uses MediaStore on Android 10+ for better visibility.
     */
    private static void exportFailedDatabase(Context context, File source, int version) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = String.format(Locale.US, "data_failed_migration_v%d_%s.db", version, timeStamp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportToDownloads(context, source, fileName);
        } else {
            exportToExternalFiles(context, source, fileName);
        }
    }

    /**
     * Exports the database to the public Downloads folder using MediaStore (Android 10+).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private static void exportToDownloads(Context context, File source, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/x-sqlite3");
        values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/AutuMandu_Recovery");

        Uri externalUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        Uri fileUri = context.getContentResolver().insert(externalUri, values);

        if (fileUri != null) {
            try (InputStream is = new FileInputStream(source);
                 OutputStream os = context.getContentResolver().openOutputStream(fileUri)) {
                if (os != null) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
                    Log.i(LOG_TAG, "Database successfully backed up for recovery at Downloads/AutuMandu_Recovery/" + fileName);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to export database to Downloads", e);
            }
        }
    }

    /**
     * Fallback for Android < 10: Exports to app-specific external files directory.
     */
    private static void exportToExternalFiles(Context context, File source, String fileName) {
        try {
            File externalDir = context.getExternalFilesDir(null);
            if (externalDir != null) {
                File destination = new File(externalDir, fileName);
                try (FileInputStream fis = new FileInputStream(source);
                     FileOutputStream fos = new FileOutputStream(destination);
                     FileChannel sourceChannel = fis.getChannel();
                     FileChannel destChannel = fos.getChannel()) {
                    destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                    Log.i(LOG_TAG, "Database successfully backed up for recovery at: " + destination.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to export database to external files", e);
        }
    }

    public static void resetInstance() {
        synchronized (sInstance) {
            AutuManduDatabase db = sInstance.get();
            if (db != null) {
                db.close();
                sInstance.set(null);
            }
        }
    }

    private static class AssetFileBasedMigration extends Migration {

        private final int newVersion;
        private final Context context;

        AssetFileBasedMigration(Context context, int newVersion) {
            super(newVersion - 1, newVersion);
            this.newVersion = newVersion;
            this.context = context;
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.i(LOG_TAG, String.format(Locale.US, "Migrating database to version %d...", newVersion));
            try (var reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open(String.format(Locale.US, "migrations/%d.sql", newVersion))))) {

                var statement = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    // Remove SQL comments (--)
                    int commentIndex = line.indexOf("--");
                    if (commentIndex != -1) {
                        line = line.substring(0, commentIndex);
                    }

                    var trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        continue;
                    }

                    statement.append(line);
                    if (trimmedLine.endsWith(";")) {
                        try {
                            database.execSQL(statement.toString());
                        } catch (SQLException e) {
                            var message = e.getMessage();
                            if (message != null && message.contains("duplicate column name")) {
                                Log.w(LOG_TAG, "Ignoring duplicate column error during migration: " + message);
                            } else {
                                throw e;
                            }
                        }
                        statement.setLength(0);
                    } else {
                        statement.append(" ");
                    }
                }
                Log.i(LOG_TAG, String.format(Locale.US, "Migration to version %d completed successfully.", newVersion));
            } catch (IOException e) {
                Log.e(LOG_TAG, String.format(Locale.US, "Error during migration to version %d.", newVersion), e);
                throw new DatabaseMigrationException(newVersion, e);
            }
        }
    }

    public static class DatabaseMigrationException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        private final int version;

        public DatabaseMigrationException(int version, Throwable cause) {
            super("Critical error during database migration to version " + version, cause);
            this.version = version;
        }

        public int getVersion() {
            return version;
        }
    }
}
