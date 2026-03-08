package com.example.optoapp.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
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
public final class PagoDao_Impl implements PagoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Pago> __insertionAdapterOfPago;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public PagoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPago = new EntityInsertionAdapter<Pago>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pagos` (`id`,`dispensacionId`,`fecha`,`tipo`,`monto`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Pago entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getDispensacionId());
        statement.bindLong(3, entity.getFecha());
        statement.bindString(4, entity.getTipo());
        statement.bindDouble(5, entity.getMonto());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pagos";
        return _query;
      }
    };
  }

  @Override
  public Object insertPago(final Pago pago, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPago.insert(pago);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Pago>> getPagosByDispensacion(final String dispensacionId) {
    final String _sql = "SELECT * FROM pagos WHERE dispensacionId = ? ORDER BY fecha DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, dispensacionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pagos"}, new Callable<List<Pago>>() {
      @Override
      @NonNull
      public List<Pago> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDispensacionId = CursorUtil.getColumnIndexOrThrow(_cursor, "dispensacionId");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfTipo = CursorUtil.getColumnIndexOrThrow(_cursor, "tipo");
          final int _cursorIndexOfMonto = CursorUtil.getColumnIndexOrThrow(_cursor, "monto");
          final List<Pago> _result = new ArrayList<Pago>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Pago _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDispensacionId;
            _tmpDispensacionId = _cursor.getString(_cursorIndexOfDispensacionId);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpTipo;
            _tmpTipo = _cursor.getString(_cursorIndexOfTipo);
            final double _tmpMonto;
            _tmpMonto = _cursor.getDouble(_cursorIndexOfMonto);
            _item = new Pago(_tmpId,_tmpDispensacionId,_tmpFecha,_tmpTipo,_tmpMonto);
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
  public Object getAllPagos(final Continuation<? super List<Pago>> $completion) {
    final String _sql = "SELECT * FROM pagos";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Pago>>() {
      @Override
      @NonNull
      public List<Pago> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDispensacionId = CursorUtil.getColumnIndexOrThrow(_cursor, "dispensacionId");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfTipo = CursorUtil.getColumnIndexOrThrow(_cursor, "tipo");
          final int _cursorIndexOfMonto = CursorUtil.getColumnIndexOrThrow(_cursor, "monto");
          final List<Pago> _result = new ArrayList<Pago>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Pago _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDispensacionId;
            _tmpDispensacionId = _cursor.getString(_cursorIndexOfDispensacionId);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpTipo;
            _tmpTipo = _cursor.getString(_cursorIndexOfTipo);
            final double _tmpMonto;
            _tmpMonto = _cursor.getDouble(_cursorIndexOfMonto);
            _item = new Pago(_tmpId,_tmpDispensacionId,_tmpFecha,_tmpTipo,_tmpMonto);
            _result.add(_item);
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
