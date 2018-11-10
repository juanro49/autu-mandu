package me.kuehle.carreport.model;

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
import me.kuehle.carreport.model.dao.CarDAO;
import me.kuehle.carreport.model.dao.FuelTypeDAO;
import me.kuehle.carreport.model.dao.OtherCostDAO;
import me.kuehle.carreport.model.dao.RefuelingDAO;
import me.kuehle.carreport.model.dao.ReminderDAO;
import me.kuehle.carreport.model.entity.Car;
import me.kuehle.carreport.model.entity.FuelType;
import me.kuehle.carreport.model.entity.OtherCost;
import me.kuehle.carreport.model.entity.Refueling;
import me.kuehle.carreport.model.entity.Reminder;
import me.kuehle.carreport.model.entity.helper.SQLTypeConverters;

import static me.kuehle.carreport.provider.DataSQLiteOpenHelper.DATABASE_FILE_NAME;

@Database(
    entities = {Car.class, FuelType.class, Reminder.class, Refueling.class, OtherCost.class},
    version = 11
)
@TypeConverters({SQLTypeConverters.class})
public abstract class CarReportDatabase extends RoomDatabase {
    public abstract CarDAO getCarDao();
    public abstract FuelTypeDAO getFuelTypeDao();
    public abstract OtherCostDAO getOtherCostDao();
    public abstract RefuelingDAO getRefuelingDao();
    public abstract ReminderDAO getReminderDao();

    private static CarReportDatabase sInstance;
    private static final String DB_NAME = DATABASE_FILE_NAME;
    private static final String LOG_TAG = "CarReportDatabase";

    public static synchronized CarReportDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = Room.databaseBuilder(context, CarReportDatabase.class, DB_NAME).
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
                        new Migration(10, 11) {
                            @Override
                            public void migrate(@NonNull SupportSQLiteDatabase database) {
                                // Do nothing, just migrate to room.
                                // Empty Migration creates the master table and does a sanity check.
                            }
                        }).
                    build();
        }
        return sInstance;
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
