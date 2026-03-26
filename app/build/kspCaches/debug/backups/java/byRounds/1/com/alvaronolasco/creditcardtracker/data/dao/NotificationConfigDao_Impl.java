package com.alvaronolasco.creditcardtracker.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.alvaronolasco.creditcardtracker.data.entity.NotificationConfig;
import java.lang.Class;
import java.lang.Exception;
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
public final class NotificationConfigDao_Impl implements NotificationConfigDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<NotificationConfig> __insertionAdapterOfNotificationConfig;

  private final EntityDeletionOrUpdateAdapter<NotificationConfig> __deletionAdapterOfNotificationConfig;

  private final EntityDeletionOrUpdateAdapter<NotificationConfig> __updateAdapterOfNotificationConfig;

  public NotificationConfigDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfNotificationConfig = new EntityInsertionAdapter<NotificationConfig>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `notification_configs` (`id`,`cardId`,`type`,`daysBefore`,`enabled`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NotificationConfig entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getCardId());
        statement.bindString(3, entity.getType());
        statement.bindLong(4, entity.getDaysBefore());
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
      }
    };
    this.__deletionAdapterOfNotificationConfig = new EntityDeletionOrUpdateAdapter<NotificationConfig>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `notification_configs` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NotificationConfig entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfNotificationConfig = new EntityDeletionOrUpdateAdapter<NotificationConfig>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `notification_configs` SET `id` = ?,`cardId` = ?,`type` = ?,`daysBefore` = ?,`enabled` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NotificationConfig entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getCardId());
        statement.bindString(3, entity.getType());
        statement.bindLong(4, entity.getDaysBefore());
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getId());
      }
    };
  }

  @Override
  public Object insertConfigs(final List<NotificationConfig> configs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfNotificationConfig.insert(configs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteConfig(final NotificationConfig config,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfNotificationConfig.handle(config);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateConfig(final NotificationConfig config,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfNotificationConfig.handle(config);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<NotificationConfig>> getConfigsByCard(final int cardId) {
    final String _sql = "SELECT * FROM notification_configs WHERE cardId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, cardId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notification_configs"}, new Callable<List<NotificationConfig>>() {
      @Override
      @NonNull
      public List<NotificationConfig> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfDaysBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "daysBefore");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final List<NotificationConfig> _result = new ArrayList<NotificationConfig>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NotificationConfig _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpCardId;
            _tmpCardId = _cursor.getInt(_cursorIndexOfCardId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final int _tmpDaysBefore;
            _tmpDaysBefore = _cursor.getInt(_cursorIndexOfDaysBefore);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            _item = new NotificationConfig(_tmpId,_tmpCardId,_tmpType,_tmpDaysBefore,_tmpEnabled);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
