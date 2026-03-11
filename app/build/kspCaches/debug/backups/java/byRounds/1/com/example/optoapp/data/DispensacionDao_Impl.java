package com.example.optoapp.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
public final class DispensacionDao_Impl implements DispensacionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DispensacionOptica> __insertionAdapterOfDispensacionOptica;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public DispensacionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDispensacionOptica = new EntityInsertionAdapter<DispensacionOptica>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `dispensaciones` (`id`,`pacienteId`,`fecha`,`tipoMontura`,`materialMontura`,`tipoLente`,`materialLente`,`tratamientos`,`colorLente`,`notasDiseno`,`origenMontura`,`tipoAro`,`descripcionMontura`,`montoTotal`,`metodoPago`,`montoPagado`,`estadoEntrega`,`fechaVencimientoGarantia`,`distanciaLente`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DispensacionOptica entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPacienteId());
        statement.bindLong(3, entity.getFecha());
        statement.bindString(4, entity.getTipoMontura());
        statement.bindString(5, entity.getMaterialMontura());
        statement.bindString(6, entity.getTipoLente());
        statement.bindString(7, entity.getMaterialLente());
        final String _tmp = __converters.fromStringList(entity.getTratamientos());
        statement.bindString(8, _tmp);
        statement.bindString(9, entity.getColorLente());
        statement.bindString(10, entity.getNotasDiseno());
        statement.bindString(11, entity.getOrigenMontura());
        statement.bindString(12, entity.getTipoAro());
        statement.bindString(13, entity.getDescripcionMontura());
        statement.bindDouble(14, entity.getMontoTotal());
        statement.bindString(15, entity.getMetodoPago());
        statement.bindDouble(16, entity.getMontoPagado());
        statement.bindString(17, entity.getEstadoEntrega());
        if (entity.getFechaVencimientoGarantia() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getFechaVencimientoGarantia());
        }
        statement.bindString(19, entity.getDistanciaLente());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM dispensaciones";
        return _query;
      }
    };
  }

  @Override
  public Object insertDispensacion(final DispensacionOptica dispensacion,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDispensacionOptica.insert(dispensacion);
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
  public Flow<List<DispensacionOptica>> getDispensacionesByPaciente(final String pacienteId) {
    final String _sql = "SELECT * FROM dispensaciones WHERE pacienteId = ? ORDER BY fecha DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pacienteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"dispensaciones"}, new Callable<List<DispensacionOptica>>() {
      @Override
      @NonNull
      public List<DispensacionOptica> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfTipoMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoMontura");
          final int _cursorIndexOfMaterialMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "materialMontura");
          final int _cursorIndexOfTipoLente = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoLente");
          final int _cursorIndexOfMaterialLente = CursorUtil.getColumnIndexOrThrow(_cursor, "materialLente");
          final int _cursorIndexOfTratamientos = CursorUtil.getColumnIndexOrThrow(_cursor, "tratamientos");
          final int _cursorIndexOfColorLente = CursorUtil.getColumnIndexOrThrow(_cursor, "colorLente");
          final int _cursorIndexOfNotasDiseno = CursorUtil.getColumnIndexOrThrow(_cursor, "notasDiseno");
          final int _cursorIndexOfOrigenMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "origenMontura");
          final int _cursorIndexOfTipoAro = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoAro");
          final int _cursorIndexOfDescripcionMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "descripcionMontura");
          final int _cursorIndexOfMontoTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "montoTotal");
          final int _cursorIndexOfMetodoPago = CursorUtil.getColumnIndexOrThrow(_cursor, "metodoPago");
          final int _cursorIndexOfMontoPagado = CursorUtil.getColumnIndexOrThrow(_cursor, "montoPagado");
          final int _cursorIndexOfEstadoEntrega = CursorUtil.getColumnIndexOrThrow(_cursor, "estadoEntrega");
          final int _cursorIndexOfFechaVencimientoGarantia = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaVencimientoGarantia");
          final int _cursorIndexOfDistanciaLente = CursorUtil.getColumnIndexOrThrow(_cursor, "distanciaLente");
          final List<DispensacionOptica> _result = new ArrayList<DispensacionOptica>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DispensacionOptica _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPacienteId;
            _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpTipoMontura;
            _tmpTipoMontura = _cursor.getString(_cursorIndexOfTipoMontura);
            final String _tmpMaterialMontura;
            _tmpMaterialMontura = _cursor.getString(_cursorIndexOfMaterialMontura);
            final String _tmpTipoLente;
            _tmpTipoLente = _cursor.getString(_cursorIndexOfTipoLente);
            final String _tmpMaterialLente;
            _tmpMaterialLente = _cursor.getString(_cursorIndexOfMaterialLente);
            final List<String> _tmpTratamientos;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTratamientos);
            _tmpTratamientos = __converters.toStringList(_tmp);
            final String _tmpColorLente;
            _tmpColorLente = _cursor.getString(_cursorIndexOfColorLente);
            final String _tmpNotasDiseno;
            _tmpNotasDiseno = _cursor.getString(_cursorIndexOfNotasDiseno);
            final String _tmpOrigenMontura;
            _tmpOrigenMontura = _cursor.getString(_cursorIndexOfOrigenMontura);
            final String _tmpTipoAro;
            _tmpTipoAro = _cursor.getString(_cursorIndexOfTipoAro);
            final String _tmpDescripcionMontura;
            _tmpDescripcionMontura = _cursor.getString(_cursorIndexOfDescripcionMontura);
            final double _tmpMontoTotal;
            _tmpMontoTotal = _cursor.getDouble(_cursorIndexOfMontoTotal);
            final String _tmpMetodoPago;
            _tmpMetodoPago = _cursor.getString(_cursorIndexOfMetodoPago);
            final double _tmpMontoPagado;
            _tmpMontoPagado = _cursor.getDouble(_cursorIndexOfMontoPagado);
            final String _tmpEstadoEntrega;
            _tmpEstadoEntrega = _cursor.getString(_cursorIndexOfEstadoEntrega);
            final String _tmpFechaVencimientoGarantia;
            if (_cursor.isNull(_cursorIndexOfFechaVencimientoGarantia)) {
              _tmpFechaVencimientoGarantia = null;
            } else {
              _tmpFechaVencimientoGarantia = _cursor.getString(_cursorIndexOfFechaVencimientoGarantia);
            }
            final String _tmpDistanciaLente;
            _tmpDistanciaLente = _cursor.getString(_cursorIndexOfDistanciaLente);
            _item = new DispensacionOptica(_tmpId,_tmpPacienteId,_tmpFecha,_tmpTipoMontura,_tmpMaterialMontura,_tmpTipoLente,_tmpMaterialLente,_tmpTratamientos,_tmpColorLente,_tmpNotasDiseno,_tmpOrigenMontura,_tmpTipoAro,_tmpDescripcionMontura,_tmpMontoTotal,_tmpMetodoPago,_tmpMontoPagado,_tmpEstadoEntrega,_tmpFechaVencimientoGarantia,_tmpDistanciaLente);
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
  public Flow<List<DispensacionOptica>> getAllDispensaciones() {
    final String _sql = "SELECT * FROM dispensaciones";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"dispensaciones"}, new Callable<List<DispensacionOptica>>() {
      @Override
      @NonNull
      public List<DispensacionOptica> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfTipoMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoMontura");
          final int _cursorIndexOfMaterialMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "materialMontura");
          final int _cursorIndexOfTipoLente = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoLente");
          final int _cursorIndexOfMaterialLente = CursorUtil.getColumnIndexOrThrow(_cursor, "materialLente");
          final int _cursorIndexOfTratamientos = CursorUtil.getColumnIndexOrThrow(_cursor, "tratamientos");
          final int _cursorIndexOfColorLente = CursorUtil.getColumnIndexOrThrow(_cursor, "colorLente");
          final int _cursorIndexOfNotasDiseno = CursorUtil.getColumnIndexOrThrow(_cursor, "notasDiseno");
          final int _cursorIndexOfOrigenMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "origenMontura");
          final int _cursorIndexOfTipoAro = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoAro");
          final int _cursorIndexOfDescripcionMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "descripcionMontura");
          final int _cursorIndexOfMontoTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "montoTotal");
          final int _cursorIndexOfMetodoPago = CursorUtil.getColumnIndexOrThrow(_cursor, "metodoPago");
          final int _cursorIndexOfMontoPagado = CursorUtil.getColumnIndexOrThrow(_cursor, "montoPagado");
          final int _cursorIndexOfEstadoEntrega = CursorUtil.getColumnIndexOrThrow(_cursor, "estadoEntrega");
          final int _cursorIndexOfFechaVencimientoGarantia = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaVencimientoGarantia");
          final int _cursorIndexOfDistanciaLente = CursorUtil.getColumnIndexOrThrow(_cursor, "distanciaLente");
          final List<DispensacionOptica> _result = new ArrayList<DispensacionOptica>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DispensacionOptica _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPacienteId;
            _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpTipoMontura;
            _tmpTipoMontura = _cursor.getString(_cursorIndexOfTipoMontura);
            final String _tmpMaterialMontura;
            _tmpMaterialMontura = _cursor.getString(_cursorIndexOfMaterialMontura);
            final String _tmpTipoLente;
            _tmpTipoLente = _cursor.getString(_cursorIndexOfTipoLente);
            final String _tmpMaterialLente;
            _tmpMaterialLente = _cursor.getString(_cursorIndexOfMaterialLente);
            final List<String> _tmpTratamientos;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTratamientos);
            _tmpTratamientos = __converters.toStringList(_tmp);
            final String _tmpColorLente;
            _tmpColorLente = _cursor.getString(_cursorIndexOfColorLente);
            final String _tmpNotasDiseno;
            _tmpNotasDiseno = _cursor.getString(_cursorIndexOfNotasDiseno);
            final String _tmpOrigenMontura;
            _tmpOrigenMontura = _cursor.getString(_cursorIndexOfOrigenMontura);
            final String _tmpTipoAro;
            _tmpTipoAro = _cursor.getString(_cursorIndexOfTipoAro);
            final String _tmpDescripcionMontura;
            _tmpDescripcionMontura = _cursor.getString(_cursorIndexOfDescripcionMontura);
            final double _tmpMontoTotal;
            _tmpMontoTotal = _cursor.getDouble(_cursorIndexOfMontoTotal);
            final String _tmpMetodoPago;
            _tmpMetodoPago = _cursor.getString(_cursorIndexOfMetodoPago);
            final double _tmpMontoPagado;
            _tmpMontoPagado = _cursor.getDouble(_cursorIndexOfMontoPagado);
            final String _tmpEstadoEntrega;
            _tmpEstadoEntrega = _cursor.getString(_cursorIndexOfEstadoEntrega);
            final String _tmpFechaVencimientoGarantia;
            if (_cursor.isNull(_cursorIndexOfFechaVencimientoGarantia)) {
              _tmpFechaVencimientoGarantia = null;
            } else {
              _tmpFechaVencimientoGarantia = _cursor.getString(_cursorIndexOfFechaVencimientoGarantia);
            }
            final String _tmpDistanciaLente;
            _tmpDistanciaLente = _cursor.getString(_cursorIndexOfDistanciaLente);
            _item = new DispensacionOptica(_tmpId,_tmpPacienteId,_tmpFecha,_tmpTipoMontura,_tmpMaterialMontura,_tmpTipoLente,_tmpMaterialLente,_tmpTratamientos,_tmpColorLente,_tmpNotasDiseno,_tmpOrigenMontura,_tmpTipoAro,_tmpDescripcionMontura,_tmpMontoTotal,_tmpMetodoPago,_tmpMontoPagado,_tmpEstadoEntrega,_tmpFechaVencimientoGarantia,_tmpDistanciaLente);
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
  public Object getDispensacionById(final String id,
      final Continuation<? super DispensacionOptica> $completion) {
    final String _sql = "SELECT * FROM dispensaciones WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DispensacionOptica>() {
      @Override
      @Nullable
      public DispensacionOptica call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfTipoMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoMontura");
          final int _cursorIndexOfMaterialMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "materialMontura");
          final int _cursorIndexOfTipoLente = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoLente");
          final int _cursorIndexOfMaterialLente = CursorUtil.getColumnIndexOrThrow(_cursor, "materialLente");
          final int _cursorIndexOfTratamientos = CursorUtil.getColumnIndexOrThrow(_cursor, "tratamientos");
          final int _cursorIndexOfColorLente = CursorUtil.getColumnIndexOrThrow(_cursor, "colorLente");
          final int _cursorIndexOfNotasDiseno = CursorUtil.getColumnIndexOrThrow(_cursor, "notasDiseno");
          final int _cursorIndexOfOrigenMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "origenMontura");
          final int _cursorIndexOfTipoAro = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoAro");
          final int _cursorIndexOfDescripcionMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "descripcionMontura");
          final int _cursorIndexOfMontoTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "montoTotal");
          final int _cursorIndexOfMetodoPago = CursorUtil.getColumnIndexOrThrow(_cursor, "metodoPago");
          final int _cursorIndexOfMontoPagado = CursorUtil.getColumnIndexOrThrow(_cursor, "montoPagado");
          final int _cursorIndexOfEstadoEntrega = CursorUtil.getColumnIndexOrThrow(_cursor, "estadoEntrega");
          final int _cursorIndexOfFechaVencimientoGarantia = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaVencimientoGarantia");
          final int _cursorIndexOfDistanciaLente = CursorUtil.getColumnIndexOrThrow(_cursor, "distanciaLente");
          final DispensacionOptica _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPacienteId;
            _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpTipoMontura;
            _tmpTipoMontura = _cursor.getString(_cursorIndexOfTipoMontura);
            final String _tmpMaterialMontura;
            _tmpMaterialMontura = _cursor.getString(_cursorIndexOfMaterialMontura);
            final String _tmpTipoLente;
            _tmpTipoLente = _cursor.getString(_cursorIndexOfTipoLente);
            final String _tmpMaterialLente;
            _tmpMaterialLente = _cursor.getString(_cursorIndexOfMaterialLente);
            final List<String> _tmpTratamientos;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTratamientos);
            _tmpTratamientos = __converters.toStringList(_tmp);
            final String _tmpColorLente;
            _tmpColorLente = _cursor.getString(_cursorIndexOfColorLente);
            final String _tmpNotasDiseno;
            _tmpNotasDiseno = _cursor.getString(_cursorIndexOfNotasDiseno);
            final String _tmpOrigenMontura;
            _tmpOrigenMontura = _cursor.getString(_cursorIndexOfOrigenMontura);
            final String _tmpTipoAro;
            _tmpTipoAro = _cursor.getString(_cursorIndexOfTipoAro);
            final String _tmpDescripcionMontura;
            _tmpDescripcionMontura = _cursor.getString(_cursorIndexOfDescripcionMontura);
            final double _tmpMontoTotal;
            _tmpMontoTotal = _cursor.getDouble(_cursorIndexOfMontoTotal);
            final String _tmpMetodoPago;
            _tmpMetodoPago = _cursor.getString(_cursorIndexOfMetodoPago);
            final double _tmpMontoPagado;
            _tmpMontoPagado = _cursor.getDouble(_cursorIndexOfMontoPagado);
            final String _tmpEstadoEntrega;
            _tmpEstadoEntrega = _cursor.getString(_cursorIndexOfEstadoEntrega);
            final String _tmpFechaVencimientoGarantia;
            if (_cursor.isNull(_cursorIndexOfFechaVencimientoGarantia)) {
              _tmpFechaVencimientoGarantia = null;
            } else {
              _tmpFechaVencimientoGarantia = _cursor.getString(_cursorIndexOfFechaVencimientoGarantia);
            }
            final String _tmpDistanciaLente;
            _tmpDistanciaLente = _cursor.getString(_cursorIndexOfDistanciaLente);
            _result = new DispensacionOptica(_tmpId,_tmpPacienteId,_tmpFecha,_tmpTipoMontura,_tmpMaterialMontura,_tmpTipoLente,_tmpMaterialLente,_tmpTratamientos,_tmpColorLente,_tmpNotasDiseno,_tmpOrigenMontura,_tmpTipoAro,_tmpDescripcionMontura,_tmpMontoTotal,_tmpMetodoPago,_tmpMontoPagado,_tmpEstadoEntrega,_tmpFechaVencimientoGarantia,_tmpDistanciaLente);
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

  @Override
  public Object getAllDispensacionesList(
      final Continuation<? super List<DispensacionOptica>> $completion) {
    final String _sql = "SELECT * FROM dispensaciones";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DispensacionOptica>>() {
      @Override
      @NonNull
      public List<DispensacionOptica> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfTipoMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoMontura");
          final int _cursorIndexOfMaterialMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "materialMontura");
          final int _cursorIndexOfTipoLente = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoLente");
          final int _cursorIndexOfMaterialLente = CursorUtil.getColumnIndexOrThrow(_cursor, "materialLente");
          final int _cursorIndexOfTratamientos = CursorUtil.getColumnIndexOrThrow(_cursor, "tratamientos");
          final int _cursorIndexOfColorLente = CursorUtil.getColumnIndexOrThrow(_cursor, "colorLente");
          final int _cursorIndexOfNotasDiseno = CursorUtil.getColumnIndexOrThrow(_cursor, "notasDiseno");
          final int _cursorIndexOfOrigenMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "origenMontura");
          final int _cursorIndexOfTipoAro = CursorUtil.getColumnIndexOrThrow(_cursor, "tipoAro");
          final int _cursorIndexOfDescripcionMontura = CursorUtil.getColumnIndexOrThrow(_cursor, "descripcionMontura");
          final int _cursorIndexOfMontoTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "montoTotal");
          final int _cursorIndexOfMetodoPago = CursorUtil.getColumnIndexOrThrow(_cursor, "metodoPago");
          final int _cursorIndexOfMontoPagado = CursorUtil.getColumnIndexOrThrow(_cursor, "montoPagado");
          final int _cursorIndexOfEstadoEntrega = CursorUtil.getColumnIndexOrThrow(_cursor, "estadoEntrega");
          final int _cursorIndexOfFechaVencimientoGarantia = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaVencimientoGarantia");
          final int _cursorIndexOfDistanciaLente = CursorUtil.getColumnIndexOrThrow(_cursor, "distanciaLente");
          final List<DispensacionOptica> _result = new ArrayList<DispensacionOptica>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DispensacionOptica _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPacienteId;
            _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpTipoMontura;
            _tmpTipoMontura = _cursor.getString(_cursorIndexOfTipoMontura);
            final String _tmpMaterialMontura;
            _tmpMaterialMontura = _cursor.getString(_cursorIndexOfMaterialMontura);
            final String _tmpTipoLente;
            _tmpTipoLente = _cursor.getString(_cursorIndexOfTipoLente);
            final String _tmpMaterialLente;
            _tmpMaterialLente = _cursor.getString(_cursorIndexOfMaterialLente);
            final List<String> _tmpTratamientos;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTratamientos);
            _tmpTratamientos = __converters.toStringList(_tmp);
            final String _tmpColorLente;
            _tmpColorLente = _cursor.getString(_cursorIndexOfColorLente);
            final String _tmpNotasDiseno;
            _tmpNotasDiseno = _cursor.getString(_cursorIndexOfNotasDiseno);
            final String _tmpOrigenMontura;
            _tmpOrigenMontura = _cursor.getString(_cursorIndexOfOrigenMontura);
            final String _tmpTipoAro;
            _tmpTipoAro = _cursor.getString(_cursorIndexOfTipoAro);
            final String _tmpDescripcionMontura;
            _tmpDescripcionMontura = _cursor.getString(_cursorIndexOfDescripcionMontura);
            final double _tmpMontoTotal;
            _tmpMontoTotal = _cursor.getDouble(_cursorIndexOfMontoTotal);
            final String _tmpMetodoPago;
            _tmpMetodoPago = _cursor.getString(_cursorIndexOfMetodoPago);
            final double _tmpMontoPagado;
            _tmpMontoPagado = _cursor.getDouble(_cursorIndexOfMontoPagado);
            final String _tmpEstadoEntrega;
            _tmpEstadoEntrega = _cursor.getString(_cursorIndexOfEstadoEntrega);
            final String _tmpFechaVencimientoGarantia;
            if (_cursor.isNull(_cursorIndexOfFechaVencimientoGarantia)) {
              _tmpFechaVencimientoGarantia = null;
            } else {
              _tmpFechaVencimientoGarantia = _cursor.getString(_cursorIndexOfFechaVencimientoGarantia);
            }
            final String _tmpDistanciaLente;
            _tmpDistanciaLente = _cursor.getString(_cursorIndexOfDistanciaLente);
            _item = new DispensacionOptica(_tmpId,_tmpPacienteId,_tmpFecha,_tmpTipoMontura,_tmpMaterialMontura,_tmpTipoLente,_tmpMaterialLente,_tmpTratamientos,_tmpColorLente,_tmpNotasDiseno,_tmpOrigenMontura,_tmpTipoAro,_tmpDescripcionMontura,_tmpMontoTotal,_tmpMetodoPago,_tmpMontoPagado,_tmpEstadoEntrega,_tmpFechaVencimientoGarantia,_tmpDistanciaLente);
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
