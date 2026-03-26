package com.alvaronolasco.creditcardtracker.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.alvaronolasco.creditcardtracker.data.dao.CategoryDao;
import com.alvaronolasco.creditcardtracker.data.dao.CategoryDao_Impl;
import com.alvaronolasco.creditcardtracker.data.dao.CreditCardDao;
import com.alvaronolasco.creditcardtracker.data.dao.CreditCardDao_Impl;
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseCategoryDao;
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseCategoryDao_Impl;
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseDao;
import com.alvaronolasco.creditcardtracker.data.dao.ExpenseDao_Impl;
import com.alvaronolasco.creditcardtracker.data.dao.IncomeDao;
import com.alvaronolasco.creditcardtracker.data.dao.IncomeDao_Impl;
import com.alvaronolasco.creditcardtracker.data.dao.NotificationConfigDao;
import com.alvaronolasco.creditcardtracker.data.dao.NotificationConfigDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile CreditCardDao _creditCardDao;

  private volatile CategoryDao _categoryDao;

  private volatile ExpenseDao _expenseDao;

  private volatile ExpenseCategoryDao _expenseCategoryDao;

  private volatile NotificationConfigDao _notificationConfigDao;

  private volatile IncomeDao _incomeDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `credit_cards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `bank` TEXT NOT NULL, `lastFourDigits` TEXT NOT NULL, `color` INTEGER NOT NULL, `cutOffDay` INTEGER NOT NULL, `paymentDueDay` INTEGER NOT NULL, `creditLimit` REAL NOT NULL, `extraFinancingPayment` REAL NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cardId` INTEGER NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `receiptImagePath` TEXT, `ocrRawText` TEXT, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`cardId`) REFERENCES `credit_cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_cardId` ON `expenses` (`cardId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `expense_categories` (`expenseId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, PRIMARY KEY(`expenseId`, `categoryId`), FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_categories_expenseId` ON `expense_categories` (`expenseId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_categories_categoryId` ON `expense_categories` (`categoryId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `notification_configs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cardId` INTEGER NOT NULL, `type` TEXT NOT NULL, `daysBefore` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, FOREIGN KEY(`cardId`) REFERENCES `credit_cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_configs_cardId` ON `notification_configs` (`cardId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `income_profiles` (`id` INTEGER NOT NULL, `employmentType` TEXT NOT NULL, `incomeMode` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `income_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, `amount` REAL NOT NULL, `dayOfMonth` INTEGER NOT NULL, `isRecurring` INTEGER NOT NULL, `type` TEXT NOT NULL, `monthYear` TEXT, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9320632f468be87384bda95a82b8f6bc')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `credit_cards`");
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `expenses`");
        db.execSQL("DROP TABLE IF EXISTS `expense_categories`");
        db.execSQL("DROP TABLE IF EXISTS `notification_configs`");
        db.execSQL("DROP TABLE IF EXISTS `income_profiles`");
        db.execSQL("DROP TABLE IF EXISTS `income_entries`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCreditCards = new HashMap<String, TableInfo.Column>(10);
        _columnsCreditCards.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCreditCards.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCreditCards.put("bank", new TableInfo.Column("bank", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCreditCards.put("lastFourDigits", new TableInfo.Column("lastFourDigits", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCreditCards.put("color", new TableInfo.Column("color", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCreditCards.put("cutOffDay", new TableInfo.Column("cutOffDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCreditCards.put("paymentDueDay", new TableInfo.Column("paymentDueDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCreditCards.put("creditLimit", new TableInfo.Column("creditLimit", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCreditCards.put("extraFinancingPayment", new TableInfo.Column("extraFinancingPayment", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCreditCards.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCreditCards = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCreditCards = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCreditCards = new TableInfo("credit_cards", _columnsCreditCards, _foreignKeysCreditCards, _indicesCreditCards);
        final TableInfo _existingCreditCards = TableInfo.read(db, "credit_cards");
        if (!_infoCreditCards.equals(_existingCreditCards)) {
          return new RoomOpenHelper.ValidationResult(false, "credit_cards(com.alvaronolasco.creditcardtracker.data.entity.CreditCard).\n"
                  + " Expected:\n" + _infoCreditCards + "\n"
                  + " Found:\n" + _existingCreditCards);
        }
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(5);
        _columnsCategories.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("isDefault", new TableInfo.Column("isDefault", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(com.alvaronolasco.creditcardtracker.data.entity.Category).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsExpenses = new HashMap<String, TableInfo.Column>(8);
        _columnsExpenses.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("cardId", new TableInfo.Column("cardId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("receiptImagePath", new TableInfo.Column("receiptImagePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("ocrRawText", new TableInfo.Column("ocrRawText", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExpenses = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysExpenses.add(new TableInfo.ForeignKey("credit_cards", "CASCADE", "NO ACTION", Arrays.asList("cardId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesExpenses = new HashSet<TableInfo.Index>(1);
        _indicesExpenses.add(new TableInfo.Index("index_expenses_cardId", false, Arrays.asList("cardId"), Arrays.asList("ASC")));
        final TableInfo _infoExpenses = new TableInfo("expenses", _columnsExpenses, _foreignKeysExpenses, _indicesExpenses);
        final TableInfo _existingExpenses = TableInfo.read(db, "expenses");
        if (!_infoExpenses.equals(_existingExpenses)) {
          return new RoomOpenHelper.ValidationResult(false, "expenses(com.alvaronolasco.creditcardtracker.data.entity.Expense).\n"
                  + " Expected:\n" + _infoExpenses + "\n"
                  + " Found:\n" + _existingExpenses);
        }
        final HashMap<String, TableInfo.Column> _columnsExpenseCategories = new HashMap<String, TableInfo.Column>(2);
        _columnsExpenseCategories.put("expenseId", new TableInfo.Column("expenseId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenseCategories.put("categoryId", new TableInfo.Column("categoryId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExpenseCategories = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysExpenseCategories.add(new TableInfo.ForeignKey("expenses", "CASCADE", "NO ACTION", Arrays.asList("expenseId"), Arrays.asList("id")));
        _foreignKeysExpenseCategories.add(new TableInfo.ForeignKey("categories", "CASCADE", "NO ACTION", Arrays.asList("categoryId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesExpenseCategories = new HashSet<TableInfo.Index>(2);
        _indicesExpenseCategories.add(new TableInfo.Index("index_expense_categories_expenseId", false, Arrays.asList("expenseId"), Arrays.asList("ASC")));
        _indicesExpenseCategories.add(new TableInfo.Index("index_expense_categories_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        final TableInfo _infoExpenseCategories = new TableInfo("expense_categories", _columnsExpenseCategories, _foreignKeysExpenseCategories, _indicesExpenseCategories);
        final TableInfo _existingExpenseCategories = TableInfo.read(db, "expense_categories");
        if (!_infoExpenseCategories.equals(_existingExpenseCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "expense_categories(com.alvaronolasco.creditcardtracker.data.entity.ExpenseCategory).\n"
                  + " Expected:\n" + _infoExpenseCategories + "\n"
                  + " Found:\n" + _existingExpenseCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsNotificationConfigs = new HashMap<String, TableInfo.Column>(5);
        _columnsNotificationConfigs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationConfigs.put("cardId", new TableInfo.Column("cardId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationConfigs.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationConfigs.put("daysBefore", new TableInfo.Column("daysBefore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationConfigs.put("enabled", new TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotificationConfigs = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysNotificationConfigs.add(new TableInfo.ForeignKey("credit_cards", "CASCADE", "NO ACTION", Arrays.asList("cardId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesNotificationConfigs = new HashSet<TableInfo.Index>(1);
        _indicesNotificationConfigs.add(new TableInfo.Index("index_notification_configs_cardId", false, Arrays.asList("cardId"), Arrays.asList("ASC")));
        final TableInfo _infoNotificationConfigs = new TableInfo("notification_configs", _columnsNotificationConfigs, _foreignKeysNotificationConfigs, _indicesNotificationConfigs);
        final TableInfo _existingNotificationConfigs = TableInfo.read(db, "notification_configs");
        if (!_infoNotificationConfigs.equals(_existingNotificationConfigs)) {
          return new RoomOpenHelper.ValidationResult(false, "notification_configs(com.alvaronolasco.creditcardtracker.data.entity.NotificationConfig).\n"
                  + " Expected:\n" + _infoNotificationConfigs + "\n"
                  + " Found:\n" + _existingNotificationConfigs);
        }
        final HashMap<String, TableInfo.Column> _columnsIncomeProfiles = new HashMap<String, TableInfo.Column>(4);
        _columnsIncomeProfiles.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeProfiles.put("employmentType", new TableInfo.Column("employmentType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeProfiles.put("incomeMode", new TableInfo.Column("incomeMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeProfiles.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysIncomeProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesIncomeProfiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoIncomeProfiles = new TableInfo("income_profiles", _columnsIncomeProfiles, _foreignKeysIncomeProfiles, _indicesIncomeProfiles);
        final TableInfo _existingIncomeProfiles = TableInfo.read(db, "income_profiles");
        if (!_infoIncomeProfiles.equals(_existingIncomeProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "income_profiles(com.alvaronolasco.creditcardtracker.data.entity.IncomeProfile).\n"
                  + " Expected:\n" + _infoIncomeProfiles + "\n"
                  + " Found:\n" + _existingIncomeProfiles);
        }
        final HashMap<String, TableInfo.Column> _columnsIncomeEntries = new HashMap<String, TableInfo.Column>(9);
        _columnsIncomeEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeEntries.put("label", new TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeEntries.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeEntries.put("dayOfMonth", new TableInfo.Column("dayOfMonth", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeEntries.put("isRecurring", new TableInfo.Column("isRecurring", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeEntries.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeEntries.put("monthYear", new TableInfo.Column("monthYear", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeEntries.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIncomeEntries.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysIncomeEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesIncomeEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoIncomeEntries = new TableInfo("income_entries", _columnsIncomeEntries, _foreignKeysIncomeEntries, _indicesIncomeEntries);
        final TableInfo _existingIncomeEntries = TableInfo.read(db, "income_entries");
        if (!_infoIncomeEntries.equals(_existingIncomeEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "income_entries(com.alvaronolasco.creditcardtracker.data.entity.IncomeEntry).\n"
                  + " Expected:\n" + _infoIncomeEntries + "\n"
                  + " Found:\n" + _existingIncomeEntries);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "9320632f468be87384bda95a82b8f6bc", "a0ccd98dc5808ca955d71304aa510ee0");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "credit_cards","categories","expenses","expense_categories","notification_configs","income_profiles","income_entries");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `credit_cards`");
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `expenses`");
      _db.execSQL("DELETE FROM `expense_categories`");
      _db.execSQL("DELETE FROM `notification_configs`");
      _db.execSQL("DELETE FROM `income_profiles`");
      _db.execSQL("DELETE FROM `income_entries`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(CreditCardDao.class, CreditCardDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ExpenseDao.class, ExpenseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ExpenseCategoryDao.class, ExpenseCategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NotificationConfigDao.class, NotificationConfigDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(IncomeDao.class, IncomeDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public CreditCardDao creditCardDao() {
    if (_creditCardDao != null) {
      return _creditCardDao;
    } else {
      synchronized(this) {
        if(_creditCardDao == null) {
          _creditCardDao = new CreditCardDao_Impl(this);
        }
        return _creditCardDao;
      }
    }
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public ExpenseDao expenseDao() {
    if (_expenseDao != null) {
      return _expenseDao;
    } else {
      synchronized(this) {
        if(_expenseDao == null) {
          _expenseDao = new ExpenseDao_Impl(this);
        }
        return _expenseDao;
      }
    }
  }

  @Override
  public ExpenseCategoryDao expenseCategoryDao() {
    if (_expenseCategoryDao != null) {
      return _expenseCategoryDao;
    } else {
      synchronized(this) {
        if(_expenseCategoryDao == null) {
          _expenseCategoryDao = new ExpenseCategoryDao_Impl(this);
        }
        return _expenseCategoryDao;
      }
    }
  }

  @Override
  public NotificationConfigDao notificationConfigDao() {
    if (_notificationConfigDao != null) {
      return _notificationConfigDao;
    } else {
      synchronized(this) {
        if(_notificationConfigDao == null) {
          _notificationConfigDao = new NotificationConfigDao_Impl(this);
        }
        return _notificationConfigDao;
      }
    }
  }

  @Override
  public IncomeDao incomeDao() {
    if (_incomeDao != null) {
      return _incomeDao;
    } else {
      synchronized(this) {
        if(_incomeDao == null) {
          _incomeDao = new IncomeDao_Impl(this);
        }
        return _incomeDao;
      }
    }
  }
}
