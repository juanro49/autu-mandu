package org.juanro.autumandu.model;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import org.juanro.autumandu.model.dao.CarDao;
import org.juanro.autumandu.model.dao.FuelTypeDao;
import org.juanro.autumandu.model.dao.OtherCostDao;
import org.juanro.autumandu.model.dao.RefuelingDao;
import org.juanro.autumandu.model.dao.ReminderDao;
import org.juanro.autumandu.model.dao.StationDao;
import org.juanro.autumandu.model.dao.TireDao;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Reminder;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.TireUsage;
import org.juanro.autumandu.model.entity.helper.SQLTypeConverters;

@Database(
    entities = {Car.class, FuelType.class, Reminder.class, Refueling.class, OtherCost.class, Station.class, TireList.class, TireUsage.class},
    version = 14
)
@TypeConverters({SQLTypeConverters.class})
public abstract class AutuManduDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "data.db";

    public abstract CarDao getCarDao();
    public abstract FuelTypeDao getFuelTypeDao();
    public abstract OtherCostDao getOtherCostDao();
    public abstract RefuelingDao getRefuelingDao();
    public abstract ReminderDao getReminderDao();
    public abstract StationDao getStationDao();
    public abstract TireDao getTireDao();

    public static final java.util.concurrent.Executor DB_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();

    private static volatile AutuManduDatabase sInstance;
    private static final String LOG_TAG = "AutuManduDatabase";

    /**
     * Get an instance of the persistent Database used by the app.
     * @param context A Context inside the Database should be used.
     * @return The instance of the Database.
     */
    public static AutuManduDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AutuManduDatabase.class) {
                if (sInstance == null) {
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
                            new AssetFileBasedMigration(appContext, 14)
                    );

                    sInstance = builder.build();
                }
            }
        }
        return sInstance;
    }

    public static void resetInstance() {
        synchronized (AutuManduDatabase.class) {
            if (sInstance != null) {
                sInstance.close();
                sInstance = null;
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
                    var trimmedLine = line.trim();
                    if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                        continue;
                    }

                    statement.append(line);
                    if (trimmedLine.endsWith(";")) {
                        database.execSQL(statement.toString());
                        statement.setLength(0);
                    } else {
                        statement.append(" ");
                    }
                }
                Log.i(LOG_TAG, String.format(Locale.US, "Migration to version %d completed successfully.", newVersion));
            } catch (IOException e) {
                Log.e(LOG_TAG, String.format(Locale.US, "Error during migration to version %d.", newVersion), e);
                throw new RuntimeException("Critical error during database migration", e);
            }
        }
    }
}
