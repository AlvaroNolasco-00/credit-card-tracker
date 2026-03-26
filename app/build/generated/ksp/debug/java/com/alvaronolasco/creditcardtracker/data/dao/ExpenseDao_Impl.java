package com.alvaronolasco.creditcardtracker.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.alvaronolasco.creditcardtracker.data.entity.Category;
import com.alvaronolasco.creditcardtracker.data.entity.Expense;
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseWithCategories;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
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
public final class ExpenseDao_Impl implements ExpenseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Expense> __insertionAdapterOfExpense;

  private final EntityDeletionOrUpdateAdapter<Expense> __deletionAdapterOfExpense;

  private final EntityDeletionOrUpdateAdapter<Expense> __updateAdapterOfExpense;

  public ExpenseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfExpense = new EntityInsertionAdapter<Expense>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `expenses` (`id`,`cardId`,`amount`,`description`,`receiptImagePath`,`ocrRawText`,`date`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Expense entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getCardId());
        statement.bindDouble(3, entity.getAmount());
        statement.bindString(4, entity.getDescription());
        if (entity.getReceiptImagePath() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getReceiptImagePath());
        }
        if (entity.getOcrRawText() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getOcrRawText());
        }
        statement.bindLong(7, entity.getDate());
        statement.bindLong(8, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfExpense = new EntityDeletionOrUpdateAdapter<Expense>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `expenses` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Expense entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfExpense = new EntityDeletionOrUpdateAdapter<Expense>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `expenses` SET `id` = ?,`cardId` = ?,`amount` = ?,`description` = ?,`receiptImagePath` = ?,`ocrRawText` = ?,`date` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Expense entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getCardId());
        statement.bindDouble(3, entity.getAmount());
        statement.bindString(4, entity.getDescription());
        if (entity.getReceiptImagePath() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getReceiptImagePath());
        }
        if (entity.getOcrRawText() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getOcrRawText());
        }
        statement.bindLong(7, entity.getDate());
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindLong(9, entity.getId());
      }
    };
  }

  @Override
  public Object insertExpense(final Expense expense, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfExpense.insertAndReturnId(expense);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteExpense(final Expense expense, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfExpense.handle(expense);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateExpense(final Expense expense, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfExpense.handle(expense);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ExpenseWithCategories>> getExpensesWithCategoriesByCard(final int cardId) {
    final String _sql = "SELECT * FROM expenses WHERE cardId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, cardId);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"expense_categories", "categories",
        "expenses"}, new Callable<List<ExpenseWithCategories>>() {
      @Override
      @NonNull
      public List<ExpenseWithCategories> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
            final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
            final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
            final int _cursorIndexOfReceiptImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptImagePath");
            final int _cursorIndexOfOcrRawText = CursorUtil.getColumnIndexOrThrow(_cursor, "ocrRawText");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final LongSparseArray<ArrayList<Category>> _collectionCategories = new LongSparseArray<ArrayList<Category>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionCategories.containsKey(_tmpKey)) {
                _collectionCategories.put(_tmpKey, new ArrayList<Category>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipcategoriesAscomAlvaronolascoCreditcardtrackerDataEntityCategory(_collectionCategories);
            final List<ExpenseWithCategories> _result = new ArrayList<ExpenseWithCategories>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final ExpenseWithCategories _item;
              final Expense _tmpExpense;
              final int _tmpId;
              _tmpId = _cursor.getInt(_cursorIndexOfId);
              final int _tmpCardId;
              _tmpCardId = _cursor.getInt(_cursorIndexOfCardId);
              final double _tmpAmount;
              _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
              final String _tmpDescription;
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
              final String _tmpReceiptImagePath;
              if (_cursor.isNull(_cursorIndexOfReceiptImagePath)) {
                _tmpReceiptImagePath = null;
              } else {
                _tmpReceiptImagePath = _cursor.getString(_cursorIndexOfReceiptImagePath);
              }
              final String _tmpOcrRawText;
              if (_cursor.isNull(_cursorIndexOfOcrRawText)) {
                _tmpOcrRawText = null;
              } else {
                _tmpOcrRawText = _cursor.getString(_cursorIndexOfOcrRawText);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              _tmpExpense = new Expense(_tmpId,_tmpCardId,_tmpAmount,_tmpDescription,_tmpReceiptImagePath,_tmpOcrRawText,_tmpDate,_tmpCreatedAt);
              final ArrayList<Category> _tmpCategoriesCollection;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpCategoriesCollection = _collectionCategories.get(_tmpKey_1);
              _item = new ExpenseWithCategories(_tmpExpense,_tmpCategoriesCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Expense>> getExpensesByCardInPeriod(final int cardId, final long startDate,
      final long endDate) {
    final String _sql = "SELECT * FROM expenses WHERE cardId = ? AND date BETWEEN ? AND ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, cardId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"expenses"}, new Callable<List<Expense>>() {
      @Override
      @NonNull
      public List<Expense> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfReceiptImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptImagePath");
          final int _cursorIndexOfOcrRawText = CursorUtil.getColumnIndexOrThrow(_cursor, "ocrRawText");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Expense> _result = new ArrayList<Expense>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Expense _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpCardId;
            _tmpCardId = _cursor.getInt(_cursorIndexOfCardId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpReceiptImagePath;
            if (_cursor.isNull(_cursorIndexOfReceiptImagePath)) {
              _tmpReceiptImagePath = null;
            } else {
              _tmpReceiptImagePath = _cursor.getString(_cursorIndexOfReceiptImagePath);
            }
            final String _tmpOcrRawText;
            if (_cursor.isNull(_cursorIndexOfOcrRawText)) {
              _tmpOcrRawText = null;
            } else {
              _tmpOcrRawText = _cursor.getString(_cursorIndexOfOcrRawText);
            }
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Expense(_tmpId,_tmpCardId,_tmpAmount,_tmpDescription,_tmpReceiptImagePath,_tmpOcrRawText,_tmpDate,_tmpCreatedAt);
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
  public Flow<Double> getTotalSpentInPeriod(final int cardId, final long startDate,
      final long endDate) {
    final String _sql = "SELECT SUM(amount) FROM expenses WHERE cardId = ? AND date BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, cardId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"expenses"}, new Callable<Double>() {
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

  @Override
  public Object getExpenseWithCategoriesById(final int id,
      final Continuation<? super ExpenseWithCategories> $completion) {
    final String _sql = "SELECT * FROM expenses WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, true, _cancellationSignal, new Callable<ExpenseWithCategories>() {
      @Override
      @Nullable
      public ExpenseWithCategories call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
            final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
            final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
            final int _cursorIndexOfReceiptImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptImagePath");
            final int _cursorIndexOfOcrRawText = CursorUtil.getColumnIndexOrThrow(_cursor, "ocrRawText");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final LongSparseArray<ArrayList<Category>> _collectionCategories = new LongSparseArray<ArrayList<Category>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionCategories.containsKey(_tmpKey)) {
                _collectionCategories.put(_tmpKey, new ArrayList<Category>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipcategoriesAscomAlvaronolascoCreditcardtrackerDataEntityCategory(_collectionCategories);
            final ExpenseWithCategories _result;
            if (_cursor.moveToFirst()) {
              final Expense _tmpExpense;
              final int _tmpId;
              _tmpId = _cursor.getInt(_cursorIndexOfId);
              final int _tmpCardId;
              _tmpCardId = _cursor.getInt(_cursorIndexOfCardId);
              final double _tmpAmount;
              _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
              final String _tmpDescription;
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
              final String _tmpReceiptImagePath;
              if (_cursor.isNull(_cursorIndexOfReceiptImagePath)) {
                _tmpReceiptImagePath = null;
              } else {
                _tmpReceiptImagePath = _cursor.getString(_cursorIndexOfReceiptImagePath);
              }
              final String _tmpOcrRawText;
              if (_cursor.isNull(_cursorIndexOfOcrRawText)) {
                _tmpOcrRawText = null;
              } else {
                _tmpOcrRawText = _cursor.getString(_cursorIndexOfOcrRawText);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              _tmpExpense = new Expense(_tmpId,_tmpCardId,_tmpAmount,_tmpDescription,_tmpReceiptImagePath,_tmpOcrRawText,_tmpDate,_tmpCreatedAt);
              final ArrayList<Category> _tmpCategoriesCollection;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpCategoriesCollection = _collectionCategories.get(_tmpKey_1);
              _result = new ExpenseWithCategories(_tmpExpense,_tmpCategoriesCollection);
            } else {
              _result = null;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
            _statement.release();
          }
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ExpenseWithCategories>> getAllExpensesWithCategoriesInPeriod(
      final long startDate, final long endDate) {
    final String _sql = "SELECT * FROM expenses WHERE date BETWEEN ? AND ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"expense_categories", "categories",
        "expenses"}, new Callable<List<ExpenseWithCategories>>() {
      @Override
      @NonNull
      public List<ExpenseWithCategories> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
            final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
            final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
            final int _cursorIndexOfReceiptImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptImagePath");
            final int _cursorIndexOfOcrRawText = CursorUtil.getColumnIndexOrThrow(_cursor, "ocrRawText");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final LongSparseArray<ArrayList<Category>> _collectionCategories = new LongSparseArray<ArrayList<Category>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionCategories.containsKey(_tmpKey)) {
                _collectionCategories.put(_tmpKey, new ArrayList<Category>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipcategoriesAscomAlvaronolascoCreditcardtrackerDataEntityCategory(_collectionCategories);
            final List<ExpenseWithCategories> _result = new ArrayList<ExpenseWithCategories>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final ExpenseWithCategories _item;
              final Expense _tmpExpense;
              final int _tmpId;
              _tmpId = _cursor.getInt(_cursorIndexOfId);
              final int _tmpCardId;
              _tmpCardId = _cursor.getInt(_cursorIndexOfCardId);
              final double _tmpAmount;
              _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
              final String _tmpDescription;
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
              final String _tmpReceiptImagePath;
              if (_cursor.isNull(_cursorIndexOfReceiptImagePath)) {
                _tmpReceiptImagePath = null;
              } else {
                _tmpReceiptImagePath = _cursor.getString(_cursorIndexOfReceiptImagePath);
              }
              final String _tmpOcrRawText;
              if (_cursor.isNull(_cursorIndexOfOcrRawText)) {
                _tmpOcrRawText = null;
              } else {
                _tmpOcrRawText = _cursor.getString(_cursorIndexOfOcrRawText);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              _tmpExpense = new Expense(_tmpId,_tmpCardId,_tmpAmount,_tmpDescription,_tmpReceiptImagePath,_tmpOcrRawText,_tmpDate,_tmpCreatedAt);
              final ArrayList<Category> _tmpCategoriesCollection;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpCategoriesCollection = _collectionCategories.get(_tmpKey_1);
              _item = new ExpenseWithCategories(_tmpExpense,_tmpCategoriesCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
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

  private void __fetchRelationshipcategoriesAscomAlvaronolascoCreditcardtrackerDataEntityCategory(
      @NonNull final LongSparseArray<ArrayList<Category>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchLongSparseArray(_map, true, (map) -> {
        __fetchRelationshipcategoriesAscomAlvaronolascoCreditcardtrackerDataEntityCategory(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `categories`.`id` AS `id`,`categories`.`name` AS `name`,`categories`.`icon` AS `icon`,`categories`.`isDefault` AS `isDefault`,`categories`.`createdAt` AS `createdAt`,_junction.`expenseId` FROM `expense_categories` AS _junction INNER JOIN `categories` ON (_junction.`categoryId` = `categories`.`id`) WHERE _junction.`expenseId` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      // _junction.expenseId;
      final int _itemKeyIndex = 5;
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfName = 1;
      final int _cursorIndexOfIcon = 2;
      final int _cursorIndexOfIsDefault = 3;
      final int _cursorIndexOfCreatedAt = 4;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        final ArrayList<Category> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final Category _item_1;
          final int _tmpId;
          _tmpId = _cursor.getInt(_cursorIndexOfId);
          final String _tmpName;
          _tmpName = _cursor.getString(_cursorIndexOfName);
          final String _tmpIcon;
          _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
          final boolean _tmpIsDefault;
          final int _tmp;
          _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
          _tmpIsDefault = _tmp != 0;
          final long _tmpCreatedAt;
          _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
          _item_1 = new Category(_tmpId,_tmpName,_tmpIcon,_tmpIsDefault,_tmpCreatedAt);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
