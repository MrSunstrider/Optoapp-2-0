package com.example.optoapp.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
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
public final class ServicioExtraDao_Impl implements ServicioExtraDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ServicioExtra> __insertionAdapterOfServicioExtra;

  private final EntityDeletionOrUpdateAdapter<ServicioExtra> __deletionAdapterOfServicioExtra;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ServicioExtraDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfServicioExtra = new EntityInsertionAdapter<ServicioExtra>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `servicios_extra` (`id`,`ot`,`descripcion`,`montoTotal`,`aCuenta`,`estado`,`fecha`,`pacienteId`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServicioExtra entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getOt());
        statement.bindString(3, entity.getDescripcion());
        statement.bindDouble(4, entity.getMontoTotal());
        statement.bindDouble(5, entity.getACuenta());
        statement.bindString(6, entity.getEstado());
        statement.bindLong(7, entity.getFecha());
        if (entity.getPacienteId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getPacienteId());
        }
      }
    };
    this.__deletionAdapterOfServicioExtra = new EntityDeletionOrUpdateAdapter<ServicioExtra>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `servicios_extra` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServicioExtra entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM servicios_extra";
        return _query;
      }
    };
  }

  @Override
  public Object insertServicio(final ServicioExtra servicio,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfServicioExtra.insert(servicio);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteServicio(final ServicioExtra servicio,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfServicioExtra.handle(servicio);
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
  public Flow<List<ServicioExtra>> getAllServicios() {
    final String _sql = "SELECT * FROM servicios_extra ORDER BY fecha DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"servicios_extra"}, new Callable<List<ServicioExtra>>() {
      @Override
      @NonNull
      public List<ServicioExtra> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfOt = CursorUtil.getColumnIndexOrThrow(_cursor, "ot");
          final int _cursorIndexOfDescripcion = CursorUtil.getColumnIndexOrThrow(_cursor, "descripcion");
          final int _cursorIndexOfMontoTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "montoTotal");
          final int _cursorIndexOfACuenta = CursorUtil.getColumnIndexOrThrow(_cursor, "aCuenta");
          final int _cursorIndexOfEstado = CursorUtil.getColumnIndexOrThrow(_cursor, "estado");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final List<ServicioExtra> _result = new ArrayList<ServicioExtra>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServicioExtra _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpOt;
            _tmpOt = _cursor.getString(_cursorIndexOfOt);
            final String _tmpDescripcion;
            _tmpDescripcion = _cursor.getString(_cursorIndexOfDescripcion);
            final double _tmpMontoTotal;
            _tmpMontoTotal = _cursor.getDouble(_cursorIndexOfMontoTotal);
            final double _tmpACuenta;
            _tmpACuenta = _cursor.getDouble(_cursorIndexOfACuenta);
            final String _tmpEstado;
            _tmpEstado = _cursor.getString(_cursorIndexOfEstado);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpPacienteId;
            if (_cursor.isNull(_cursorIndexOfPacienteId)) {
              _tmpPacienteId = null;
            } else {
              _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            }
            _item = new ServicioExtra(_tmpId,_tmpOt,_tmpDescripcion,_tmpMontoTotal,_tmpACuenta,_tmpEstado,_tmpFecha,_tmpPacienteId);
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
  public Flow<List<ServicioExtra>> getServiciosByPaciente(final String pacienteId) {
    final String _sql = "SELECT * FROM servicios_extra WHERE pacienteId = ? ORDER BY fecha DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pacienteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"servicios_extra"}, new Callable<List<ServicioExtra>>() {
      @Override
      @NonNull
      public List<ServicioExtra> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfOt = CursorUtil.getColumnIndexOrThrow(_cursor, "ot");
          final int _cursorIndexOfDescripcion = CursorUtil.getColumnIndexOrThrow(_cursor, "descripcion");
          final int _cursorIndexOfMontoTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "montoTotal");
          final int _cursorIndexOfACuenta = CursorUtil.getColumnIndexOrThrow(_cursor, "aCuenta");
          final int _cursorIndexOfEstado = CursorUtil.getColumnIndexOrThrow(_cursor, "estado");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final List<ServicioExtra> _result = new ArrayList<ServicioExtra>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServicioExtra _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpOt;
            _tmpOt = _cursor.getString(_cursorIndexOfOt);
            final String _tmpDescripcion;
            _tmpDescripcion = _cursor.getString(_cursorIndexOfDescripcion);
            final double _tmpMontoTotal;
            _tmpMontoTotal = _cursor.getDouble(_cursorIndexOfMontoTotal);
            final double _tmpACuenta;
            _tmpACuenta = _cursor.getDouble(_cursorIndexOfACuenta);
            final String _tmpEstado;
            _tmpEstado = _cursor.getString(_cursorIndexOfEstado);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpPacienteId;
            if (_cursor.isNull(_cursorIndexOfPacienteId)) {
              _tmpPacienteId = null;
            } else {
              _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            }
            _item = new ServicioExtra(_tmpId,_tmpOt,_tmpDescripcion,_tmpMontoTotal,_tmpACuenta,_tmpEstado,_tmpFecha,_tmpPacienteId);
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
  public Object getServicioById(final String id,
      final Continuation<? super ServicioExtra> $completion) {
    final String _sql = "SELECT * FROM servicios_extra WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ServicioExtra>() {
      @Override
      @Nullable
      public ServicioExtra call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfOt = CursorUtil.getColumnIndexOrThrow(_cursor, "ot");
          final int _cursorIndexOfDescripcion = CursorUtil.getColumnIndexOrThrow(_cursor, "descripcion");
          final int _cursorIndexOfMontoTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "montoTotal");
          final int _cursorIndexOfACuenta = CursorUtil.getColumnIndexOrThrow(_cursor, "aCuenta");
          final int _cursorIndexOfEstado = CursorUtil.getColumnIndexOrThrow(_cursor, "estado");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final ServicioExtra _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpOt;
            _tmpOt = _cursor.getString(_cursorIndexOfOt);
            final String _tmpDescripcion;
            _tmpDescripcion = _cursor.getString(_cursorIndexOfDescripcion);
            final double _tmpMontoTotal;
            _tmpMontoTotal = _cursor.getDouble(_cursorIndexOfMontoTotal);
            final double _tmpACuenta;
            _tmpACuenta = _cursor.getDouble(_cursorIndexOfACuenta);
            final String _tmpEstado;
            _tmpEstado = _cursor.getString(_cursorIndexOfEstado);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpPacienteId;
            if (_cursor.isNull(_cursorIndexOfPacienteId)) {
              _tmpPacienteId = null;
            } else {
              _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            }
            _result = new ServicioExtra(_tmpId,_tmpOt,_tmpDescripcion,_tmpMontoTotal,_tmpACuenta,_tmpEstado,_tmpFecha,_tmpPacienteId);
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
