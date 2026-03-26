package com.alvaronolasco.creditcardtracker.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
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
import com.alvaronolasco.creditcardtracker.data.entity.CreditCard;
import java.lang.Class;
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
public final class CreditCardDao_Impl implements CreditCardDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CreditCard> __insertionAdapterOfCreditCard;

  private final EntityDeletionOrUpdateAdapter<CreditCard> __deletionAdapterOfCreditCard;

  private final EntityDeletionOrUpdateAdapter<CreditCard> __updateAdapterOfCreditCard;

  public CreditCardDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCreditCard = new EntityInsertionAdapter<CreditCard>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `credit_cards` (`id`,`name`,`bank`,`lastFourDigits`,`color`,`cutOffDay`,`paymentDueDay`,`creditLimit`,`extraFinancingPayment`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CreditCard entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getBank());
        statement.bindString(4, entity.getLastFourDigits());
        statement.bindLong(5, entity.getColor());
        statement.bindLong(6, entity.getCutOffDay());
        statement.bindLong(7, entity.getPaymentDueDay());
        statement.bindDouble(8, entity.getCreditLimit());
        statement.bindDouble(9, entity.getExtraFinancingPayment());
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfCreditCard = new EntityDeletionOrUpdateAdapter<CreditCard>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `credit_cards` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CreditCard entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfCreditCard = new EntityDeletionOrUpdateAdapter<CreditCard>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `credit_cards` SET `id` = ?,`name` = ?,`bank` = ?,`lastFourDigits` = ?,`color` = ?,`cutOffDay` = ?,`paymentDueDay` = ?,`creditLimit` = ?,`extraFinancingPayment` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CreditCard entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getBank());
        statement.bindString(4, entity.getLastFourDigits());
        statement.bindLong(5, entity.getColor());
        statement.bindLong(6, entity.getCutOffDay());
        statement.bindLong(7, entity.getPaymentDueDay());
        statement.bindDouble(8, entity.getCreditLimit());
        statement.bindDouble(9, entity.getExtraFinancingPayment());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getId());
      }
    };
  }

  @Override
  public Object insertCard(final CreditCard card, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCreditCard.insertAndReturnId(card);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCard(final CreditCard card, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCreditCard.handle(card);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCard(final CreditCard card, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCreditCard.handle(card);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CreditCard>> getAllCards() {
    final String _sql = "SELECT * FROM credit_cards ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"credit_cards"}, new Callable<List<CreditCard>>() {
      @Override
      @NonNull
      public List<CreditCard> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
          final int _cursorIndexOfLastFourDigits = CursorUtil.getColumnIndexOrThrow(_cursor, "lastFourDigits");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCutOffDay = CursorUtil.getColumnIndexOrThrow(_cursor, "cutOffDay");
          final int _cursorIndexOfPaymentDueDay = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentDueDay");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfExtraFinancingPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "extraFinancingPayment");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CreditCard> _result = new ArrayList<CreditCard>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CreditCard _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBank;
            _tmpBank = _cursor.getString(_cursorIndexOfBank);
            final String _tmpLastFourDigits;
            _tmpLastFourDigits = _cursor.getString(_cursorIndexOfLastFourDigits);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final int _tmpCutOffDay;
            _tmpCutOffDay = _cursor.getInt(_cursorIndexOfCutOffDay);
            final int _tmpPaymentDueDay;
            _tmpPaymentDueDay = _cursor.getInt(_cursorIndexOfPaymentDueDay);
            final double _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
            final double _tmpExtraFinancingPayment;
            _tmpExtraFinancingPayment = _cursor.getDouble(_cursorIndexOfExtraFinancingPayment);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CreditCard(_tmpId,_tmpName,_tmpBank,_tmpLastFourDigits,_tmpColor,_tmpCutOffDay,_tmpPaymentDueDay,_tmpCreditLimit,_tmpExtraFinancingPayment,_tmpCreatedAt);
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
  public Object getCardById(final int id, final Continuation<? super CreditCard> $completion) {
    final String _sql = "SELECT * FROM credit_cards WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CreditCard>() {
      @Override
      @Nullable
      public CreditCard call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
          final int _cursorIndexOfLastFourDigits = CursorUtil.getColumnIndexOrThrow(_cursor, "lastFourDigits");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCutOffDay = CursorUtil.getColumnIndexOrThrow(_cursor, "cutOffDay");
          final int _cursorIndexOfPaymentDueDay = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentDueDay");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfExtraFinancingPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "extraFinancingPayment");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final CreditCard _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBank;
            _tmpBank = _cursor.getString(_cursorIndexOfBank);
            final String _tmpLastFourDigits;
            _tmpLastFourDigits = _cursor.getString(_cursorIndexOfLastFourDigits);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final int _tmpCutOffDay;
            _tmpCutOffDay = _cursor.getInt(_cursorIndexOfCutOffDay);
            final int _tmpPaymentDueDay;
            _tmpPaymentDueDay = _cursor.getInt(_cursorIndexOfPaymentDueDay);
            final double _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
            final double _tmpExtraFinancingPayment;
            _tmpExtraFinancingPayment = _cursor.getDouble(_cursorIndexOfExtraFinancingPayment);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new CreditCard(_tmpId,_tmpName,_tmpBank,_tmpLastFourDigits,_tmpColor,_tmpCutOffDay,_tmpPaymentDueDay,_tmpCreditLimit,_tmpExtraFinancingPayment,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
