package org.juanro.autumandu.model;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import org.juanro.autumandu.model.dao.CarDAO;
import org.juanro.autumandu.model.dao.FuelTypeDAO;
import org.juanro.autumandu.model.dao.OtherCostDAO;
import org.juanro.autumandu.model.dao.RefuelingDAO;
import org.juanro.autumandu.model.dao.ReminderDAO;
import org.juanro.autumandu.model.dao.StationDAO;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Reminder;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.model.entity.helper.SQLTypeConverters;
import org.juanro.autumandu.provider.DataSQLiteOpenHelper;

import static org.juanro.autumandu.provider.DataSQLiteOpenHelper.DATABASE_FILE_NAME;

@Database(
    entities = {Car.class, FuelType.class, Reminder.class, Refueling.class, OtherCost.class, Station.class},
    version = 12
)
@TypeConverters({SQLTypeConverters.class})
public abstract class AutuManduDatabase extends RoomDatabase {
    public abstract CarDAO getCarDao();
    public abstract FuelTypeDAO getFuelTypeDao();
    public abstract OtherCostDAO getOtherCostDao();
    public abstract RefuelingDAO getRefuelingDao();
    public abstract ReminderDAO getReminderDao();
    public abstract StationDAO getStationDao();

    private static AutuManduDatabase sInstance;
    private static final String DB_NAME = DATABASE_FILE_NAME;
    private static final String LOG_TAG = "AutuManduDatabase";

    /**
     * Get an instance of the persistent Database used by the app.
     * @param context A Context inside the Database should be used.
     * @return The instance of the Database.
     */
    public static synchronized AutuManduDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = Room.databaseBuilder(context, AutuManduDatabase.class, DB_NAME).
                    allowMainThreadQueries().
                    addMigrations(
                        new AssetFileBasedMigration(context, 2),
                        new AssetFileBasedMigration(context, 3),
                        new AssetFileBasedMigration(context, 4),
                        new AssetFileBasedMigration(context, 5),
                        new AssetFileBasedMigration(context, 6),
                        new AssetFileBasedMigration(context, 7),
                        new AssetFileBasedMigration(context, 8),
                        new AssetFileBasedMigration(context, 9),
                        new AssetFileBasedMigration(context, 10),
                        new AssetFileBasedMigration(context, 11),
                        new Migration(11, 12) {
                            @Override
                            public void migrate(@NonNull SupportSQLiteDatabase database) {
                                // Do nothing, just migrate to room.
                                // Empty Migration creates the master table and does a sanity check.
                                Log.i(LOG_TAG, String.format(Locale.US,
                                    "Migrating to new version 12."));
                                new AssetFileBasedMigration(context, 11).migrate(database);
                                Log.i(LOG_TAG, String.format(Locale.US,
                                    "Migrated to new version 12."));
                            }
                        }).
                    build();
        }
        return sInstance;
    }

    public static void resetInstance() {
        // We need to call back legacy.
        DataSQLiteOpenHelper.resetInstance();
        if (sInstance != null) {
            sInstance.close();
            sInstance = null;
        }
    }

    private static class AssetFileBasedMigration extends Migration {

        private int mNewVersion;
        private Context mContext;

        AssetFileBasedMigration(Context context, int newVersion) {
            super(newVersion - 1, newVersion);
            this.mNewVersion = newVersion;
            this.mContext = context;
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.beginTransaction();
            try {
                InputStream migrationInput = mContext.getAssets().open(String.format(Locale.US,
                        "migrations/%d.sql", mNewVersion));
                byte[] binaryMigration = new byte[migrationInput.available()];
                migrationInput.read(binaryMigration);
                migrationInput.close();

                for (String preparedStatement: prepareSqlStatements(new String(binaryMigration))) {
                    database.execSQL(preparedStatement);
                }
                database.setTransactionSuccessful();

                Log.i(LOG_TAG, String.format(Locale.US,
                    "Migrated to new version %d.", mNewVersion));
            } catch (IOException e) {
                Log.e(LOG_TAG, String.format(Locale.US,
                        "File based Migration failed for new version %d.", mNewVersion));
                e.printStackTrace();
            }
            database.endTransaction();
        }

        private static List<String> prepareSqlStatements(String rawSql) {
            rawSql = rawSql.replaceAll("[\r\n]", " ");
            String[] rawCommands = rawSql.split(";");

            List<String> commands = new LinkedList<>();
            for (int i = 0; i < rawCommands.length; i++) {
                rawCommands[i] = rawCommands[i].trim();
                if (rawCommands[i].length() > 0) {
                    commands.add(rawCommands[i]);
                }
            }
            return commands;
        }
    }
}
