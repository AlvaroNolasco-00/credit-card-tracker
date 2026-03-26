package com.alvaronolasco.creditcardtracker.data.dao;

import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.SharedSQLiteStatement;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.alvaronolasco.creditcardtracker.data.entity.ExpenseCategory;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ExpenseCategoryDao_Impl implements ExpenseCategoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ExpenseCategory> __insertionAdapterOfExpenseCategory;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByExpenseId;

  public ExpenseCategoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfExpenseCategory = new EntityInsertionAdapter<ExpenseCategory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `expense_categories` (`expenseId`,`categoryId`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExpenseCategory entity) {
        statement.bindLong(1, entity.getExpenseId());
        statement.bindLong(2, entity.getCategoryId());
      }
    };
    this.__preparedStmtOfDeleteByExpenseId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM expense_categories WHERE expenseId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ExpenseCategory ec, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExpenseCategory.insert(ec);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object replaceExpenseCategories(final int expenseId, final List<Integer> categoryIds,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> ExpenseCategoryDao.DefaultImpls.replaceExpenseCategories(ExpenseCategoryDao_Impl.this, expenseId, categoryIds, __cont), $completion);
  }

  @Override
  public Object deleteByExpenseId(final int expenseId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByExpenseId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, expenseId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByExpenseId.release(_stmt);
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
