package com.alvaronolasco.creditcardtracker.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.alvaronolasco.creditcardtracker.data.entity.IncomeEntry;
import com.alvaronolasco.creditcardtracker.data.entity.IncomeProfile;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class IncomeDao_Impl implements IncomeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<IncomeProfile> __insertionAdapterOfIncomeProfile;

  private final EntityInsertionAdapter<IncomeEntry> __insertionAdapterOfIncomeEntry;

  private final EntityDeletionOrUpdateAdapter<IncomeEntry> __deletionAdapterOfIncomeEntry;

  private final EntityDeletionOrUpdateAdapter<IncomeEntry> __updateAdapterOfIncomeEntry;

  public IncomeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfIncomeProfile = new EntityInsertionAdapter<IncomeProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `income_profiles` (`id`,`employmentType`,`incomeMode`,`createdAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IncomeProfile entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getEmploymentType());
        statement.bindString(3, entity.getIncomeMode());
        statement.bindLong(4, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfIncomeEntry = new EntityInsertionAdapter<IncomeEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `income_entries` (`id`,`label`,`amount`,`dayOfMonth`,`isRecurring`,`type`,`monthYear`,`isActive`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IncomeEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getLabel());
        statement.bindDouble(3, entity.getAmount());
        statement.bindLong(4, entity.getDayOfMonth());
        final int _tmp = entity.isRecurring() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getType());
        if (entity.getMonthYear() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getMonthYear());
        }
        final int _tmp_1 = entity.isActive() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfIncomeEntry = new EntityDeletionOrUpdateAdapter<IncomeEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `income_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IncomeEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfIncomeEntry = new EntityDeletionOrUpdateAdapter<IncomeEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `income_entries` SET `id` = ?,`label` = ?,`amount` = ?,`dayOfMonth` = ?,`isRecurring` = ?,`type` = ?,`monthYear` = ?,`isActive` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IncomeEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getLabel());
        statement.bindDouble(3, entity.getAmount());
        statement.bindLong(4, entity.getDayOfMonth());
        final int _tmp = entity.isRecurring() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getType());
        if (entity.getMonthYear() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getMonthYear());
        }
        final int _tmp_1 = entity.isActive() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getId());
      }
    };
  }

  @Override
  public Object upsertProfile(final IncomeProfile profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfIncomeProfile.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertEntry(final IncomeEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfIncomeEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteEntry(final IncomeEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfIncomeEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateEntry(final IncomeEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfIncomeEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<IncomeProfile> getProfile() {
    final String _sql = "SELECT * FROM income_profiles WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"income_profiles"}, new Callable<IncomeProfile>() {
      @Override
      @Nullable
      public IncomeProfile call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmploymentType = CursorUtil.getColumnIndexOrThrow(_cursor, "employmentType");
          final int _cursorIndexOfIncomeMode = CursorUtil.getColumnIndexOrThrow(_cursor, "incomeMode");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final IncomeProfile _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpEmploymentType;
            _tmpEmploymentType = _cursor.getString(_cursorIndexOfEmploymentType);
            final String _tmpIncomeMode;
            _tmpIncomeMode = _cursor.getString(_cursorIndexOfIncomeMode);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new IncomeProfile(_tmpId,_tmpEmploymentType,_tmpIncomeMode,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<IncomeEntry>> getAllActiveEntries() {
    final String _sql = "SELECT * FROM income_entries WHERE isActive = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"income_entries"}, new Callable<List<IncomeEntry>>() {
      @Override
      @NonNull
      public List<IncomeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfDayOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfMonth");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfMonthYear = CursorUtil.getColumnIndexOrThrow(_cursor, "monthYear");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<IncomeEntry> _result = new ArrayList<IncomeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IncomeEntry _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final int _tmpDayOfMonth;
            _tmpDayOfMonth = _cursor.getInt(_cursorIndexOfDayOfMonth);
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpMonthYear;
            if (_cursor.isNull(_cursorIndexOfMonthYear)) {
              _tmpMonthYear = null;
            } else {
              _tmpMonthYear = _cursor.getString(_cursorIndexOfMonthYear);
            }
            final boolean _tmpIsActive;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_1 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new IncomeEntry(_tmpId,_tmpLabel,_tmpAmount,_tmpDayOfMonth,_tmpIsRecurring,_tmpType,_tmpMonthYear,_tmpIsActive,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<IncomeEntry>> getRecurringEntries() {
    final String _sql = "SELECT * FROM income_entries WHERE isActive = 1 AND isRecurring = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"income_entries"}, new Callable<List<IncomeEntry>>() {
      @Override
      @NonNull
      public List<IncomeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfDayOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfMonth");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfMonthYear = CursorUtil.getColumnIndexOrThrow(_cursor, "monthYear");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<IncomeEntry> _result = new ArrayList<IncomeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IncomeEntry _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final int _tmpDayOfMonth;
            _tmpDayOfMonth = _cursor.getInt(_cursorIndexOfDayOfMonth);
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpMonthYear;
            if (_cursor.isNull(_cursorIndexOfMonthYear)) {
              _tmpMonthYear = null;
            } else {
              _tmpMonthYear = _cursor.getString(_cursorIndexOfMonthYear);
            }
            final boolean _tmpIsActive;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_1 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new IncomeEntry(_tmpId,_tmpLabel,_tmpAmount,_tmpDayOfMonth,_tmpIsRecurring,_tmpType,_tmpMonthYear,_tmpIsActive,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<IncomeEntry>> getEntriesForMonth(final String monthYear) {
    final String _sql = "SELECT * FROM income_entries WHERE isActive = 1 AND (isRecurring = 1 OR monthYear = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, monthYear);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"income_entries"}, new Callable<List<IncomeEntry>>() {
      @Override
      @NonNull
      public List<IncomeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfDayOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfMonth");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfMonthYear = CursorUtil.getColumnIndexOrThrow(_cursor, "monthYear");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<IncomeEntry> _result = new ArrayList<IncomeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IncomeEntry _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final int _tmpDayOfMonth;
            _tmpDayOfMonth = _cursor.getInt(_cursorIndexOfDayOfMonth);
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpMonthYear;
            if (_cursor.isNull(_cursorIndexOfMonthYear)) {
              _tmpMonthYear = null;
            } else {
              _tmpMonthYear = _cursor.getString(_cursorIndexOfMonthYear);
            }
            final boolean _tmpIsActive;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_1 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new IncomeEntry(_tmpId,_tmpLabel,_tmpAmount,_tmpDayOfMonth,_tmpIsRecurring,_tmpType,_tmpMonthYear,_tmpIsActive,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Double> getTotalIncomeForMonth(final String monthYear) {
    final String _sql = "SELECT SUM(amount) FROM income_entries WHERE isActive = 1 AND (isRecurring = 1 OR monthYear = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, monthYear);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"income_entries"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
