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
public final class PacienteDao_Impl implements PacienteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Paciente> __insertionAdapterOfPaciente;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Paciente> __deletionAdapterOfPaciente;

  private final EntityDeletionOrUpdateAdapter<Paciente> __updateAdapterOfPaciente;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public PacienteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPaciente = new EntityInsertionAdapter<Paciente>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pacientes` (`id`,`nombreCompleto`,`edad`,`telefono`,`fechaCreacion`,`dni`,`fechaNacimiento`,`sexo`,`email`,`direccion`,`distrito`,`ocupacion`,`acompanante`,`hobbies`,`ultimasEtiquetas`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Paciente entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getNombreCompleto());
        statement.bindLong(3, entity.getEdad());
        statement.bindString(4, entity.getTelefono());
        statement.bindLong(5, entity.getFechaCreacion());
        if (entity.getDni() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDni());
        }
        if (entity.getFechaNacimiento() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getFechaNacimiento());
        }
        if (entity.getSexo() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getSexo());
        }
        if (entity.getEmail() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getEmail());
        }
        if (entity.getDireccion() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getDireccion());
        }
        if (entity.getDistrito() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getDistrito());
        }
        if (entity.getOcupacion() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getOcupacion());
        }
        if (entity.getAcompanante() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getAcompanante());
        }
        if (entity.getHobbies() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getHobbies());
        }
        final String _tmp = __converters.fromStringList(entity.getUltimasEtiquetas());
        statement.bindString(15, _tmp);
      }
    };
    this.__deletionAdapterOfPaciente = new EntityDeletionOrUpdateAdapter<Paciente>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `pacientes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Paciente entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfPaciente = new EntityDeletionOrUpdateAdapter<Paciente>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `pacientes` SET `id` = ?,`nombreCompleto` = ?,`edad` = ?,`telefono` = ?,`fechaCreacion` = ?,`dni` = ?,`fechaNacimiento` = ?,`sexo` = ?,`email` = ?,`direccion` = ?,`distrito` = ?,`ocupacion` = ?,`acompanante` = ?,`hobbies` = ?,`ultimasEtiquetas` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Paciente entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getNombreCompleto());
        statement.bindLong(3, entity.getEdad());
        statement.bindString(4, entity.getTelefono());
        statement.bindLong(5, entity.getFechaCreacion());
        if (entity.getDni() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDni());
        }
        if (entity.getFechaNacimiento() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getFechaNacimiento());
        }
        if (entity.getSexo() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getSexo());
        }
        if (entity.getEmail() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getEmail());
        }
        if (entity.getDireccion() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getDireccion());
        }
        if (entity.getDistrito() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getDistrito());
        }
        if (entity.getOcupacion() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getOcupacion());
        }
        if (entity.getAcompanante() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getAcompanante());
        }
        if (entity.getHobbies() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getHobbies());
        }
        final String _tmp = __converters.fromStringList(entity.getUltimasEtiquetas());
        statement.bindString(15, _tmp);
        statement.bindString(16, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pacientes";
        return _query;
      }
    };
  }

  @Override
  public Object insertPaciente(final Paciente paciente,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPaciente.insert(paciente);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePaciente(final Paciente paciente,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPaciente.handle(paciente);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePaciente(final Paciente paciente,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPaciente.handle(paciente);
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
  public Flow<List<Paciente>> getAllPacientes() {
    final String _sql = "SELECT * FROM pacientes ORDER BY nombreCompleto ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pacientes"}, new Callable<List<Paciente>>() {
      @Override
      @NonNull
      public List<Paciente> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNombreCompleto = CursorUtil.getColumnIndexOrThrow(_cursor, "nombreCompleto");
          final int _cursorIndexOfEdad = CursorUtil.getColumnIndexOrThrow(_cursor, "edad");
          final int _cursorIndexOfTelefono = CursorUtil.getColumnIndexOrThrow(_cursor, "telefono");
          final int _cursorIndexOfFechaCreacion = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaCreacion");
          final int _cursorIndexOfDni = CursorUtil.getColumnIndexOrThrow(_cursor, "dni");
          final int _cursorIndexOfFechaNacimiento = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaNacimiento");
          final int _cursorIndexOfSexo = CursorUtil.getColumnIndexOrThrow(_cursor, "sexo");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfDireccion = CursorUtil.getColumnIndexOrThrow(_cursor, "direccion");
          final int _cursorIndexOfDistrito = CursorUtil.getColumnIndexOrThrow(_cursor, "distrito");
          final int _cursorIndexOfOcupacion = CursorUtil.getColumnIndexOrThrow(_cursor, "ocupacion");
          final int _cursorIndexOfAcompanante = CursorUtil.getColumnIndexOrThrow(_cursor, "acompanante");
          final int _cursorIndexOfHobbies = CursorUtil.getColumnIndexOrThrow(_cursor, "hobbies");
          final int _cursorIndexOfUltimasEtiquetas = CursorUtil.getColumnIndexOrThrow(_cursor, "ultimasEtiquetas");
          final List<Paciente> _result = new ArrayList<Paciente>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Paciente _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNombreCompleto;
            _tmpNombreCompleto = _cursor.getString(_cursorIndexOfNombreCompleto);
            final int _tmpEdad;
            _tmpEdad = _cursor.getInt(_cursorIndexOfEdad);
            final String _tmpTelefono;
            _tmpTelefono = _cursor.getString(_cursorIndexOfTelefono);
            final long _tmpFechaCreacion;
            _tmpFechaCreacion = _cursor.getLong(_cursorIndexOfFechaCreacion);
            final String _tmpDni;
            if (_cursor.isNull(_cursorIndexOfDni)) {
              _tmpDni = null;
            } else {
              _tmpDni = _cursor.getString(_cursorIndexOfDni);
            }
            final String _tmpFechaNacimiento;
            if (_cursor.isNull(_cursorIndexOfFechaNacimiento)) {
              _tmpFechaNacimiento = null;
            } else {
              _tmpFechaNacimiento = _cursor.getString(_cursorIndexOfFechaNacimiento);
            }
            final String _tmpSexo;
            if (_cursor.isNull(_cursorIndexOfSexo)) {
              _tmpSexo = null;
            } else {
              _tmpSexo = _cursor.getString(_cursorIndexOfSexo);
            }
            final String _tmpEmail;
            if (_cursor.isNull(_cursorIndexOfEmail)) {
              _tmpEmail = null;
            } else {
              _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            }
            final String _tmpDireccion;
            if (_cursor.isNull(_cursorIndexOfDireccion)) {
              _tmpDireccion = null;
            } else {
              _tmpDireccion = _cursor.getString(_cursorIndexOfDireccion);
            }
            final String _tmpDistrito;
            if (_cursor.isNull(_cursorIndexOfDistrito)) {
              _tmpDistrito = null;
            } else {
              _tmpDistrito = _cursor.getString(_cursorIndexOfDistrito);
            }
            final String _tmpOcupacion;
            if (_cursor.isNull(_cursorIndexOfOcupacion)) {
              _tmpOcupacion = null;
            } else {
              _tmpOcupacion = _cursor.getString(_cursorIndexOfOcupacion);
            }
            final String _tmpAcompanante;
            if (_cursor.isNull(_cursorIndexOfAcompanante)) {
              _tmpAcompanante = null;
            } else {
              _tmpAcompanante = _cursor.getString(_cursorIndexOfAcompanante);
            }
            final String _tmpHobbies;
            if (_cursor.isNull(_cursorIndexOfHobbies)) {
              _tmpHobbies = null;
            } else {
              _tmpHobbies = _cursor.getString(_cursorIndexOfHobbies);
            }
            final List<String> _tmpUltimasEtiquetas;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfUltimasEtiquetas);
            _tmpUltimasEtiquetas = __converters.toStringList(_tmp);
            _item = new Paciente(_tmpId,_tmpNombreCompleto,_tmpEdad,_tmpTelefono,_tmpFechaCreacion,_tmpDni,_tmpFechaNacimiento,_tmpSexo,_tmpEmail,_tmpDireccion,_tmpDistrito,_tmpOcupacion,_tmpAcompanante,_tmpHobbies,_tmpUltimasEtiquetas);
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
  public Object getPacienteById(final String id, final Continuation<? super Paciente> $completion) {
    final String _sql = "SELECT * FROM pacientes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Paciente>() {
      @Override
      @Nullable
      public Paciente call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNombreCompleto = CursorUtil.getColumnIndexOrThrow(_cursor, "nombreCompleto");
          final int _cursorIndexOfEdad = CursorUtil.getColumnIndexOrThrow(_cursor, "edad");
          final int _cursorIndexOfTelefono = CursorUtil.getColumnIndexOrThrow(_cursor, "telefono");
          final int _cursorIndexOfFechaCreacion = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaCreacion");
          final int _cursorIndexOfDni = CursorUtil.getColumnIndexOrThrow(_cursor, "dni");
          final int _cursorIndexOfFechaNacimiento = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaNacimiento");
          final int _cursorIndexOfSexo = CursorUtil.getColumnIndexOrThrow(_cursor, "sexo");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfDireccion = CursorUtil.getColumnIndexOrThrow(_cursor, "direccion");
          final int _cursorIndexOfDistrito = CursorUtil.getColumnIndexOrThrow(_cursor, "distrito");
          final int _cursorIndexOfOcupacion = CursorUtil.getColumnIndexOrThrow(_cursor, "ocupacion");
          final int _cursorIndexOfAcompanante = CursorUtil.getColumnIndexOrThrow(_cursor, "acompanante");
          final int _cursorIndexOfHobbies = CursorUtil.getColumnIndexOrThrow(_cursor, "hobbies");
          final int _cursorIndexOfUltimasEtiquetas = CursorUtil.getColumnIndexOrThrow(_cursor, "ultimasEtiquetas");
          final Paciente _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNombreCompleto;
            _tmpNombreCompleto = _cursor.getString(_cursorIndexOfNombreCompleto);
            final int _tmpEdad;
            _tmpEdad = _cursor.getInt(_cursorIndexOfEdad);
            final String _tmpTelefono;
            _tmpTelefono = _cursor.getString(_cursorIndexOfTelefono);
            final long _tmpFechaCreacion;
            _tmpFechaCreacion = _cursor.getLong(_cursorIndexOfFechaCreacion);
            final String _tmpDni;
            if (_cursor.isNull(_cursorIndexOfDni)) {
              _tmpDni = null;
            } else {
              _tmpDni = _cursor.getString(_cursorIndexOfDni);
            }
            final String _tmpFechaNacimiento;
            if (_cursor.isNull(_cursorIndexOfFechaNacimiento)) {
              _tmpFechaNacimiento = null;
            } else {
              _tmpFechaNacimiento = _cursor.getString(_cursorIndexOfFechaNacimiento);
            }
            final String _tmpSexo;
            if (_cursor.isNull(_cursorIndexOfSexo)) {
              _tmpSexo = null;
            } else {
              _tmpSexo = _cursor.getString(_cursorIndexOfSexo);
            }
            final String _tmpEmail;
            if (_cursor.isNull(_cursorIndexOfEmail)) {
              _tmpEmail = null;
            } else {
              _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            }
            final String _tmpDireccion;
            if (_cursor.isNull(_cursorIndexOfDireccion)) {
              _tmpDireccion = null;
            } else {
              _tmpDireccion = _cursor.getString(_cursorIndexOfDireccion);
            }
            final String _tmpDistrito;
            if (_cursor.isNull(_cursorIndexOfDistrito)) {
              _tmpDistrito = null;
            } else {
              _tmpDistrito = _cursor.getString(_cursorIndexOfDistrito);
            }
            final String _tmpOcupacion;
            if (_cursor.isNull(_cursorIndexOfOcupacion)) {
              _tmpOcupacion = null;
            } else {
              _tmpOcupacion = _cursor.getString(_cursorIndexOfOcupacion);
            }
            final String _tmpAcompanante;
            if (_cursor.isNull(_cursorIndexOfAcompanante)) {
              _tmpAcompanante = null;
            } else {
              _tmpAcompanante = _cursor.getString(_cursorIndexOfAcompanante);
            }
            final String _tmpHobbies;
            if (_cursor.isNull(_cursorIndexOfHobbies)) {
              _tmpHobbies = null;
            } else {
              _tmpHobbies = _cursor.getString(_cursorIndexOfHobbies);
            }
            final List<String> _tmpUltimasEtiquetas;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfUltimasEtiquetas);
            _tmpUltimasEtiquetas = __converters.toStringList(_tmp);
            _result = new Paciente(_tmpId,_tmpNombreCompleto,_tmpEdad,_tmpTelefono,_tmpFechaCreacion,_tmpDni,_tmpFechaNacimiento,_tmpSexo,_tmpEmail,_tmpDireccion,_tmpDistrito,_tmpOcupacion,_tmpAcompanante,_tmpHobbies,_tmpUltimasEtiquetas);
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
  public Flow<List<Paciente>> searchPacientes(final String query) {
    final String _sql = "SELECT * FROM pacientes WHERE nombreCompleto LIKE '%' || ? || '%' OR id LIKE '%' || ? || '%' OR telefono LIKE '%' || ? || '%'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    _argIndex = 3;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pacientes"}, new Callable<List<Paciente>>() {
      @Override
      @NonNull
      public List<Paciente> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNombreCompleto = CursorUtil.getColumnIndexOrThrow(_cursor, "nombreCompleto");
          final int _cursorIndexOfEdad = CursorUtil.getColumnIndexOrThrow(_cursor, "edad");
          final int _cursorIndexOfTelefono = CursorUtil.getColumnIndexOrThrow(_cursor, "telefono");
          final int _cursorIndexOfFechaCreacion = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaCreacion");
          final int _cursorIndexOfDni = CursorUtil.getColumnIndexOrThrow(_cursor, "dni");
          final int _cursorIndexOfFechaNacimiento = CursorUtil.getColumnIndexOrThrow(_cursor, "fechaNacimiento");
          final int _cursorIndexOfSexo = CursorUtil.getColumnIndexOrThrow(_cursor, "sexo");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfDireccion = CursorUtil.getColumnIndexOrThrow(_cursor, "direccion");
          final int _cursorIndexOfDistrito = CursorUtil.getColumnIndexOrThrow(_cursor, "distrito");
          final int _cursorIndexOfOcupacion = CursorUtil.getColumnIndexOrThrow(_cursor, "ocupacion");
          final int _cursorIndexOfAcompanante = CursorUtil.getColumnIndexOrThrow(_cursor, "acompanante");
          final int _cursorIndexOfHobbies = CursorUtil.getColumnIndexOrThrow(_cursor, "hobbies");
          final int _cursorIndexOfUltimasEtiquetas = CursorUtil.getColumnIndexOrThrow(_cursor, "ultimasEtiquetas");
          final List<Paciente> _result = new ArrayList<Paciente>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Paciente _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNombreCompleto;
            _tmpNombreCompleto = _cursor.getString(_cursorIndexOfNombreCompleto);
            final int _tmpEdad;
            _tmpEdad = _cursor.getInt(_cursorIndexOfEdad);
            final String _tmpTelefono;
            _tmpTelefono = _cursor.getString(_cursorIndexOfTelefono);
            final long _tmpFechaCreacion;
            _tmpFechaCreacion = _cursor.getLong(_cursorIndexOfFechaCreacion);
            final String _tmpDni;
            if (_cursor.isNull(_cursorIndexOfDni)) {
              _tmpDni = null;
            } else {
              _tmpDni = _cursor.getString(_cursorIndexOfDni);
            }
            final String _tmpFechaNacimiento;
            if (_cursor.isNull(_cursorIndexOfFechaNacimiento)) {
              _tmpFechaNacimiento = null;
            } else {
              _tmpFechaNacimiento = _cursor.getString(_cursorIndexOfFechaNacimiento);
            }
            final String _tmpSexo;
            if (_cursor.isNull(_cursorIndexOfSexo)) {
              _tmpSexo = null;
            } else {
              _tmpSexo = _cursor.getString(_cursorIndexOfSexo);
            }
            final String _tmpEmail;
            if (_cursor.isNull(_cursorIndexOfEmail)) {
              _tmpEmail = null;
            } else {
              _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            }
            final String _tmpDireccion;
            if (_cursor.isNull(_cursorIndexOfDireccion)) {
              _tmpDireccion = null;
            } else {
              _tmpDireccion = _cursor.getString(_cursorIndexOfDireccion);
            }
            final String _tmpDistrito;
            if (_cursor.isNull(_cursorIndexOfDistrito)) {
              _tmpDistrito = null;
            } else {
              _tmpDistrito = _cursor.getString(_cursorIndexOfDistrito);
            }
            final String _tmpOcupacion;
            if (_cursor.isNull(_cursorIndexOfOcupacion)) {
              _tmpOcupacion = null;
            } else {
              _tmpOcupacion = _cursor.getString(_cursorIndexOfOcupacion);
            }
            final String _tmpAcompanante;
            if (_cursor.isNull(_cursorIndexOfAcompanante)) {
              _tmpAcompanante = null;
            } else {
              _tmpAcompanante = _cursor.getString(_cursorIndexOfAcompanante);
            }
            final String _tmpHobbies;
            if (_cursor.isNull(_cursorIndexOfHobbies)) {
              _tmpHobbies = null;
            } else {
              _tmpHobbies = _cursor.getString(_cursorIndexOfHobbies);
            }
            final List<String> _tmpUltimasEtiquetas;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfUltimasEtiquetas);
            _tmpUltimasEtiquetas = __converters.toStringList(_tmp);
            _item = new Paciente(_tmpId,_tmpNombreCompleto,_tmpEdad,_tmpTelefono,_tmpFechaCreacion,_tmpDni,_tmpFechaNacimiento,_tmpSexo,_tmpEmail,_tmpDireccion,_tmpDistrito,_tmpOcupacion,_tmpAcompanante,_tmpHobbies,_tmpUltimasEtiquetas);
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
