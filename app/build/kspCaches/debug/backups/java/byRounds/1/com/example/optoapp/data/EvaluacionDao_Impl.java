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
public final class EvaluacionDao_Impl implements EvaluacionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EvaluacionClinica> __insertionAdapterOfEvaluacionClinica;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public EvaluacionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEvaluacionClinica = new EntityInsertionAdapter<EvaluacionClinica>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `evaluaciones` (`id`,`pacienteId`,`fecha`,`motivoConsulta`,`sintomas`,`antecedentesPersonalesOculares`,`antecedentesPersonalesSistemicos`,`antecedentesFamiliaresOculares`,`antecedentesFamiliaresSistemicos`,`medicacion`,`alergias`,`necesidadVisual`,`avScOdLejos`,`avScOiLejos`,`avScOdCerca`,`avScOiCerca`,`avCcOdLejos`,`avCcOiLejos`,`avCcOdCerca`,`avCcOiCerca`,`phOd`,`phOi`,`kappaOd`,`kappaOi`,`coverTest6m`,`coverTest40cm`,`coverTest10cm`,`ppcOr`,`ppcLuz`,`ppcFrl`,`reflejoFotomotor`,`reflejoConsensual`,`reflejoAcomodativo`,`k1Od`,`k2Od`,`k1Oi`,`k2Oi`,`objOdEsf`,`objOdCil`,`objOdEje`,`objOiEsf`,`objOiCil`,`objOiEje`,`subjOdEsf`,`subjOdCil`,`subjOdEje`,`subjOiEsf`,`subjOiCil`,`subjOiEje`,`recetaOdEsf`,`recetaOdCil`,`recetaOdEje`,`recetaOiEsf`,`recetaOiCil`,`recetaOiEje`,`addCercaOd`,`addCercaOi`,`addIntermediaOd`,`addIntermediaOi`,`dipLejos`,`dipCerca`,`dipIntermedio`,`prismaOdValor`,`prismaOdBase`,`prismaOiValor`,`prismaOiBase`,`diagnostico`,`planTratamiento`,`observaciones`,`proximaFechaControl`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EvaluacionClinica entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPacienteId());
        statement.bindLong(3, entity.getFecha());
        statement.bindString(4, entity.getMotivoConsulta());
        statement.bindString(5, entity.getSintomas());
        statement.bindString(6, entity.getAntecedentesPersonalesOculares());
        statement.bindString(7, entity.getAntecedentesPersonalesSistemicos());
        statement.bindString(8, entity.getAntecedentesFamiliaresOculares());
        statement.bindString(9, entity.getAntecedentesFamiliaresSistemicos());
        statement.bindString(10, entity.getMedicacion());
        statement.bindString(11, entity.getAlergias());
        final String _tmp = __converters.fromStringList(entity.getNecesidadVisual());
        statement.bindString(12, _tmp);
        statement.bindString(13, entity.getAvScOdLejos());
        statement.bindString(14, entity.getAvScOiLejos());
        statement.bindString(15, entity.getAvScOdCerca());
        statement.bindString(16, entity.getAvScOiCerca());
        statement.bindString(17, entity.getAvCcOdLejos());
        statement.bindString(18, entity.getAvCcOiLejos());
        statement.bindString(19, entity.getAvCcOdCerca());
        statement.bindString(20, entity.getAvCcOiCerca());
        statement.bindString(21, entity.getPhOd());
        statement.bindString(22, entity.getPhOi());
        statement.bindString(23, entity.getKappaOd());
        statement.bindString(24, entity.getKappaOi());
        statement.bindString(25, entity.getCoverTest6m());
        statement.bindString(26, entity.getCoverTest40cm());
        statement.bindString(27, entity.getCoverTest10cm());
        statement.bindString(28, entity.getPpcOr());
        statement.bindString(29, entity.getPpcLuz());
        statement.bindString(30, entity.getPpcFrl());
        statement.bindString(31, entity.getReflejoFotomotor());
        statement.bindString(32, entity.getReflejoConsensual());
        statement.bindString(33, entity.getReflejoAcomodativo());
        statement.bindString(34, entity.getK1Od());
        statement.bindString(35, entity.getK2Od());
        statement.bindString(36, entity.getK1Oi());
        statement.bindString(37, entity.getK2Oi());
        statement.bindString(38, entity.getObjOdEsf());
        statement.bindString(39, entity.getObjOdCil());
        statement.bindString(40, entity.getObjOdEje());
        statement.bindString(41, entity.getObjOiEsf());
        statement.bindString(42, entity.getObjOiCil());
        statement.bindString(43, entity.getObjOiEje());
        statement.bindString(44, entity.getSubjOdEsf());
        statement.bindString(45, entity.getSubjOdCil());
        statement.bindString(46, entity.getSubjOdEje());
        statement.bindString(47, entity.getSubjOiEsf());
        statement.bindString(48, entity.getSubjOiCil());
        statement.bindString(49, entity.getSubjOiEje());
        statement.bindString(50, entity.getRecetaOdEsf());
        statement.bindString(51, entity.getRecetaOdCil());
        statement.bindString(52, entity.getRecetaOdEje());
        statement.bindString(53, entity.getRecetaOiEsf());
        statement.bindString(54, entity.getRecetaOiCil());
        statement.bindString(55, entity.getRecetaOiEje());
        statement.bindString(56, entity.getAddCercaOd());
        statement.bindString(57, entity.getAddCercaOi());
        statement.bindString(58, entity.getAddIntermediaOd());
        statement.bindString(59, entity.getAddIntermediaOi());
        statement.bindString(60, entity.getDipLejos());
        statement.bindString(61, entity.getDipCerca());
        statement.bindString(62, entity.getDipIntermedio());
        statement.bindString(63, entity.getPrismaOdValor());
        statement.bindString(64, entity.getPrismaOdBase());
        statement.bindString(65, entity.getPrismaOiValor());
        statement.bindString(66, entity.getPrismaOiBase());
        statement.bindString(67, entity.getDiagnostico());
        statement.bindString(68, entity.getPlanTratamiento());
        statement.bindString(69, entity.getObservaciones());
        statement.bindString(70, entity.getProximaFechaControl());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM evaluaciones";
        return _query;
      }
    };
  }

  @Override
  public Object insertEvaluacion(final EvaluacionClinica evaluacion,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEvaluacionClinica.insert(evaluacion);
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
  public Flow<List<EvaluacionClinica>> getEvaluacionesByPaciente(final String pacienteId) {
    final String _sql = "SELECT * FROM evaluaciones WHERE pacienteId = ? ORDER BY fecha DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pacienteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"evaluaciones"}, new Callable<List<EvaluacionClinica>>() {
      @Override
      @NonNull
      public List<EvaluacionClinica> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfMotivoConsulta = CursorUtil.getColumnIndexOrThrow(_cursor, "motivoConsulta");
          final int _cursorIndexOfSintomas = CursorUtil.getColumnIndexOrThrow(_cursor, "sintomas");
          final int _cursorIndexOfAntecedentesPersonalesOculares = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesPersonalesOculares");
          final int _cursorIndexOfAntecedentesPersonalesSistemicos = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesPersonalesSistemicos");
          final int _cursorIndexOfAntecedentesFamiliaresOculares = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesFamiliaresOculares");
          final int _cursorIndexOfAntecedentesFamiliaresSistemicos = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesFamiliaresSistemicos");
          final int _cursorIndexOfMedicacion = CursorUtil.getColumnIndexOrThrow(_cursor, "medicacion");
          final int _cursorIndexOfAlergias = CursorUtil.getColumnIndexOrThrow(_cursor, "alergias");
          final int _cursorIndexOfNecesidadVisual = CursorUtil.getColumnIndexOrThrow(_cursor, "necesidadVisual");
          final int _cursorIndexOfAvScOdLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOdLejos");
          final int _cursorIndexOfAvScOiLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOiLejos");
          final int _cursorIndexOfAvScOdCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOdCerca");
          final int _cursorIndexOfAvScOiCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOiCerca");
          final int _cursorIndexOfAvCcOdLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOdLejos");
          final int _cursorIndexOfAvCcOiLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOiLejos");
          final int _cursorIndexOfAvCcOdCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOdCerca");
          final int _cursorIndexOfAvCcOiCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOiCerca");
          final int _cursorIndexOfPhOd = CursorUtil.getColumnIndexOrThrow(_cursor, "phOd");
          final int _cursorIndexOfPhOi = CursorUtil.getColumnIndexOrThrow(_cursor, "phOi");
          final int _cursorIndexOfKappaOd = CursorUtil.getColumnIndexOrThrow(_cursor, "kappaOd");
          final int _cursorIndexOfKappaOi = CursorUtil.getColumnIndexOrThrow(_cursor, "kappaOi");
          final int _cursorIndexOfCoverTest6m = CursorUtil.getColumnIndexOrThrow(_cursor, "coverTest6m");
          final int _cursorIndexOfCoverTest40cm = CursorUtil.getColumnIndexOrThrow(_cursor, "coverTest40cm");
          final int _cursorIndexOfCoverTest10cm = CursorUtil.getColumnIndexOrThrow(_cursor, "coverTest10cm");
          final int _cursorIndexOfPpcOr = CursorUtil.getColumnIndexOrThrow(_cursor, "ppcOr");
          final int _cursorIndexOfPpcLuz = CursorUtil.getColumnIndexOrThrow(_cursor, "ppcLuz");
          final int _cursorIndexOfPpcFrl = CursorUtil.getColumnIndexOrThrow(_cursor, "ppcFrl");
          final int _cursorIndexOfReflejoFotomotor = CursorUtil.getColumnIndexOrThrow(_cursor, "reflejoFotomotor");
          final int _cursorIndexOfReflejoConsensual = CursorUtil.getColumnIndexOrThrow(_cursor, "reflejoConsensual");
          final int _cursorIndexOfReflejoAcomodativo = CursorUtil.getColumnIndexOrThrow(_cursor, "reflejoAcomodativo");
          final int _cursorIndexOfK1Od = CursorUtil.getColumnIndexOrThrow(_cursor, "k1Od");
          final int _cursorIndexOfK2Od = CursorUtil.getColumnIndexOrThrow(_cursor, "k2Od");
          final int _cursorIndexOfK1Oi = CursorUtil.getColumnIndexOrThrow(_cursor, "k1Oi");
          final int _cursorIndexOfK2Oi = CursorUtil.getColumnIndexOrThrow(_cursor, "k2Oi");
          final int _cursorIndexOfObjOdEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "objOdEsf");
          final int _cursorIndexOfObjOdCil = CursorUtil.getColumnIndexOrThrow(_cursor, "objOdCil");
          final int _cursorIndexOfObjOdEje = CursorUtil.getColumnIndexOrThrow(_cursor, "objOdEje");
          final int _cursorIndexOfObjOiEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "objOiEsf");
          final int _cursorIndexOfObjOiCil = CursorUtil.getColumnIndexOrThrow(_cursor, "objOiCil");
          final int _cursorIndexOfObjOiEje = CursorUtil.getColumnIndexOrThrow(_cursor, "objOiEje");
          final int _cursorIndexOfSubjOdEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOdEsf");
          final int _cursorIndexOfSubjOdCil = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOdCil");
          final int _cursorIndexOfSubjOdEje = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOdEje");
          final int _cursorIndexOfSubjOiEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOiEsf");
          final int _cursorIndexOfSubjOiCil = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOiCil");
          final int _cursorIndexOfSubjOiEje = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOiEje");
          final int _cursorIndexOfRecetaOdEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOdEsf");
          final int _cursorIndexOfRecetaOdCil = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOdCil");
          final int _cursorIndexOfRecetaOdEje = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOdEje");
          final int _cursorIndexOfRecetaOiEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOiEsf");
          final int _cursorIndexOfRecetaOiCil = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOiCil");
          final int _cursorIndexOfRecetaOiEje = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOiEje");
          final int _cursorIndexOfAddCercaOd = CursorUtil.getColumnIndexOrThrow(_cursor, "addCercaOd");
          final int _cursorIndexOfAddCercaOi = CursorUtil.getColumnIndexOrThrow(_cursor, "addCercaOi");
          final int _cursorIndexOfAddIntermediaOd = CursorUtil.getColumnIndexOrThrow(_cursor, "addIntermediaOd");
          final int _cursorIndexOfAddIntermediaOi = CursorUtil.getColumnIndexOrThrow(_cursor, "addIntermediaOi");
          final int _cursorIndexOfDipLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "dipLejos");
          final int _cursorIndexOfDipCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "dipCerca");
          final int _cursorIndexOfDipIntermedio = CursorUtil.getColumnIndexOrThrow(_cursor, "dipIntermedio");
          final int _cursorIndexOfPrismaOdValor = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOdValor");
          final int _cursorIndexOfPrismaOdBase = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOdBase");
          final int _cursorIndexOfPrismaOiValor = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOiValor");
          final int _cursorIndexOfPrismaOiBase = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOiBase");
          final int _cursorIndexOfDiagnostico = CursorUtil.getColumnIndexOrThrow(_cursor, "diagnostico");
          final int _cursorIndexOfPlanTratamiento = CursorUtil.getColumnIndexOrThrow(_cursor, "planTratamiento");
          final int _cursorIndexOfObservaciones = CursorUtil.getColumnIndexOrThrow(_cursor, "observaciones");
          final int _cursorIndexOfProximaFechaControl = CursorUtil.getColumnIndexOrThrow(_cursor, "proximaFechaControl");
          final List<EvaluacionClinica> _result = new ArrayList<EvaluacionClinica>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EvaluacionClinica _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPacienteId;
            _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpMotivoConsulta;
            _tmpMotivoConsulta = _cursor.getString(_cursorIndexOfMotivoConsulta);
            final String _tmpSintomas;
            _tmpSintomas = _cursor.getString(_cursorIndexOfSintomas);
            final String _tmpAntecedentesPersonalesOculares;
            _tmpAntecedentesPersonalesOculares = _cursor.getString(_cursorIndexOfAntecedentesPersonalesOculares);
            final String _tmpAntecedentesPersonalesSistemicos;
            _tmpAntecedentesPersonalesSistemicos = _cursor.getString(_cursorIndexOfAntecedentesPersonalesSistemicos);
            final String _tmpAntecedentesFamiliaresOculares;
            _tmpAntecedentesFamiliaresOculares = _cursor.getString(_cursorIndexOfAntecedentesFamiliaresOculares);
            final String _tmpAntecedentesFamiliaresSistemicos;
            _tmpAntecedentesFamiliaresSistemicos = _cursor.getString(_cursorIndexOfAntecedentesFamiliaresSistemicos);
            final String _tmpMedicacion;
            _tmpMedicacion = _cursor.getString(_cursorIndexOfMedicacion);
            final String _tmpAlergias;
            _tmpAlergias = _cursor.getString(_cursorIndexOfAlergias);
            final List<String> _tmpNecesidadVisual;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfNecesidadVisual);
            _tmpNecesidadVisual = __converters.toStringList(_tmp);
            final String _tmpAvScOdLejos;
            _tmpAvScOdLejos = _cursor.getString(_cursorIndexOfAvScOdLejos);
            final String _tmpAvScOiLejos;
            _tmpAvScOiLejos = _cursor.getString(_cursorIndexOfAvScOiLejos);
            final String _tmpAvScOdCerca;
            _tmpAvScOdCerca = _cursor.getString(_cursorIndexOfAvScOdCerca);
            final String _tmpAvScOiCerca;
            _tmpAvScOiCerca = _cursor.getString(_cursorIndexOfAvScOiCerca);
            final String _tmpAvCcOdLejos;
            _tmpAvCcOdLejos = _cursor.getString(_cursorIndexOfAvCcOdLejos);
            final String _tmpAvCcOiLejos;
            _tmpAvCcOiLejos = _cursor.getString(_cursorIndexOfAvCcOiLejos);
            final String _tmpAvCcOdCerca;
            _tmpAvCcOdCerca = _cursor.getString(_cursorIndexOfAvCcOdCerca);
            final String _tmpAvCcOiCerca;
            _tmpAvCcOiCerca = _cursor.getString(_cursorIndexOfAvCcOiCerca);
            final String _tmpPhOd;
            _tmpPhOd = _cursor.getString(_cursorIndexOfPhOd);
            final String _tmpPhOi;
            _tmpPhOi = _cursor.getString(_cursorIndexOfPhOi);
            final String _tmpKappaOd;
            _tmpKappaOd = _cursor.getString(_cursorIndexOfKappaOd);
            final String _tmpKappaOi;
            _tmpKappaOi = _cursor.getString(_cursorIndexOfKappaOi);
            final String _tmpCoverTest6m;
            _tmpCoverTest6m = _cursor.getString(_cursorIndexOfCoverTest6m);
            final String _tmpCoverTest40cm;
            _tmpCoverTest40cm = _cursor.getString(_cursorIndexOfCoverTest40cm);
            final String _tmpCoverTest10cm;
            _tmpCoverTest10cm = _cursor.getString(_cursorIndexOfCoverTest10cm);
            final String _tmpPpcOr;
            _tmpPpcOr = _cursor.getString(_cursorIndexOfPpcOr);
            final String _tmpPpcLuz;
            _tmpPpcLuz = _cursor.getString(_cursorIndexOfPpcLuz);
            final String _tmpPpcFrl;
            _tmpPpcFrl = _cursor.getString(_cursorIndexOfPpcFrl);
            final String _tmpReflejoFotomotor;
            _tmpReflejoFotomotor = _cursor.getString(_cursorIndexOfReflejoFotomotor);
            final String _tmpReflejoConsensual;
            _tmpReflejoConsensual = _cursor.getString(_cursorIndexOfReflejoConsensual);
            final String _tmpReflejoAcomodativo;
            _tmpReflejoAcomodativo = _cursor.getString(_cursorIndexOfReflejoAcomodativo);
            final String _tmpK1Od;
            _tmpK1Od = _cursor.getString(_cursorIndexOfK1Od);
            final String _tmpK2Od;
            _tmpK2Od = _cursor.getString(_cursorIndexOfK2Od);
            final String _tmpK1Oi;
            _tmpK1Oi = _cursor.getString(_cursorIndexOfK1Oi);
            final String _tmpK2Oi;
            _tmpK2Oi = _cursor.getString(_cursorIndexOfK2Oi);
            final String _tmpObjOdEsf;
            _tmpObjOdEsf = _cursor.getString(_cursorIndexOfObjOdEsf);
            final String _tmpObjOdCil;
            _tmpObjOdCil = _cursor.getString(_cursorIndexOfObjOdCil);
            final String _tmpObjOdEje;
            _tmpObjOdEje = _cursor.getString(_cursorIndexOfObjOdEje);
            final String _tmpObjOiEsf;
            _tmpObjOiEsf = _cursor.getString(_cursorIndexOfObjOiEsf);
            final String _tmpObjOiCil;
            _tmpObjOiCil = _cursor.getString(_cursorIndexOfObjOiCil);
            final String _tmpObjOiEje;
            _tmpObjOiEje = _cursor.getString(_cursorIndexOfObjOiEje);
            final String _tmpSubjOdEsf;
            _tmpSubjOdEsf = _cursor.getString(_cursorIndexOfSubjOdEsf);
            final String _tmpSubjOdCil;
            _tmpSubjOdCil = _cursor.getString(_cursorIndexOfSubjOdCil);
            final String _tmpSubjOdEje;
            _tmpSubjOdEje = _cursor.getString(_cursorIndexOfSubjOdEje);
            final String _tmpSubjOiEsf;
            _tmpSubjOiEsf = _cursor.getString(_cursorIndexOfSubjOiEsf);
            final String _tmpSubjOiCil;
            _tmpSubjOiCil = _cursor.getString(_cursorIndexOfSubjOiCil);
            final String _tmpSubjOiEje;
            _tmpSubjOiEje = _cursor.getString(_cursorIndexOfSubjOiEje);
            final String _tmpRecetaOdEsf;
            _tmpRecetaOdEsf = _cursor.getString(_cursorIndexOfRecetaOdEsf);
            final String _tmpRecetaOdCil;
            _tmpRecetaOdCil = _cursor.getString(_cursorIndexOfRecetaOdCil);
            final String _tmpRecetaOdEje;
            _tmpRecetaOdEje = _cursor.getString(_cursorIndexOfRecetaOdEje);
            final String _tmpRecetaOiEsf;
            _tmpRecetaOiEsf = _cursor.getString(_cursorIndexOfRecetaOiEsf);
            final String _tmpRecetaOiCil;
            _tmpRecetaOiCil = _cursor.getString(_cursorIndexOfRecetaOiCil);
            final String _tmpRecetaOiEje;
            _tmpRecetaOiEje = _cursor.getString(_cursorIndexOfRecetaOiEje);
            final String _tmpAddCercaOd;
            _tmpAddCercaOd = _cursor.getString(_cursorIndexOfAddCercaOd);
            final String _tmpAddCercaOi;
            _tmpAddCercaOi = _cursor.getString(_cursorIndexOfAddCercaOi);
            final String _tmpAddIntermediaOd;
            _tmpAddIntermediaOd = _cursor.getString(_cursorIndexOfAddIntermediaOd);
            final String _tmpAddIntermediaOi;
            _tmpAddIntermediaOi = _cursor.getString(_cursorIndexOfAddIntermediaOi);
            final String _tmpDipLejos;
            _tmpDipLejos = _cursor.getString(_cursorIndexOfDipLejos);
            final String _tmpDipCerca;
            _tmpDipCerca = _cursor.getString(_cursorIndexOfDipCerca);
            final String _tmpDipIntermedio;
            _tmpDipIntermedio = _cursor.getString(_cursorIndexOfDipIntermedio);
            final String _tmpPrismaOdValor;
            _tmpPrismaOdValor = _cursor.getString(_cursorIndexOfPrismaOdValor);
            final String _tmpPrismaOdBase;
            _tmpPrismaOdBase = _cursor.getString(_cursorIndexOfPrismaOdBase);
            final String _tmpPrismaOiValor;
            _tmpPrismaOiValor = _cursor.getString(_cursorIndexOfPrismaOiValor);
            final String _tmpPrismaOiBase;
            _tmpPrismaOiBase = _cursor.getString(_cursorIndexOfPrismaOiBase);
            final String _tmpDiagnostico;
            _tmpDiagnostico = _cursor.getString(_cursorIndexOfDiagnostico);
            final String _tmpPlanTratamiento;
            _tmpPlanTratamiento = _cursor.getString(_cursorIndexOfPlanTratamiento);
            final String _tmpObservaciones;
            _tmpObservaciones = _cursor.getString(_cursorIndexOfObservaciones);
            final String _tmpProximaFechaControl;
            _tmpProximaFechaControl = _cursor.getString(_cursorIndexOfProximaFechaControl);
            _item = new EvaluacionClinica(_tmpId,_tmpPacienteId,_tmpFecha,_tmpMotivoConsulta,_tmpSintomas,_tmpAntecedentesPersonalesOculares,_tmpAntecedentesPersonalesSistemicos,_tmpAntecedentesFamiliaresOculares,_tmpAntecedentesFamiliaresSistemicos,_tmpMedicacion,_tmpAlergias,_tmpNecesidadVisual,_tmpAvScOdLejos,_tmpAvScOiLejos,_tmpAvScOdCerca,_tmpAvScOiCerca,_tmpAvCcOdLejos,_tmpAvCcOiLejos,_tmpAvCcOdCerca,_tmpAvCcOiCerca,_tmpPhOd,_tmpPhOi,_tmpKappaOd,_tmpKappaOi,_tmpCoverTest6m,_tmpCoverTest40cm,_tmpCoverTest10cm,_tmpPpcOr,_tmpPpcLuz,_tmpPpcFrl,_tmpReflejoFotomotor,_tmpReflejoConsensual,_tmpReflejoAcomodativo,_tmpK1Od,_tmpK2Od,_tmpK1Oi,_tmpK2Oi,_tmpObjOdEsf,_tmpObjOdCil,_tmpObjOdEje,_tmpObjOiEsf,_tmpObjOiCil,_tmpObjOiEje,_tmpSubjOdEsf,_tmpSubjOdCil,_tmpSubjOdEje,_tmpSubjOiEsf,_tmpSubjOiCil,_tmpSubjOiEje,_tmpRecetaOdEsf,_tmpRecetaOdCil,_tmpRecetaOdEje,_tmpRecetaOiEsf,_tmpRecetaOiCil,_tmpRecetaOiEje,_tmpAddCercaOd,_tmpAddCercaOi,_tmpAddIntermediaOd,_tmpAddIntermediaOi,_tmpDipLejos,_tmpDipCerca,_tmpDipIntermedio,_tmpPrismaOdValor,_tmpPrismaOdBase,_tmpPrismaOiValor,_tmpPrismaOiBase,_tmpDiagnostico,_tmpPlanTratamiento,_tmpObservaciones,_tmpProximaFechaControl);
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
  public Object getEvaluacionById(final String id,
      final Continuation<? super EvaluacionClinica> $completion) {
    final String _sql = "SELECT * FROM evaluaciones WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EvaluacionClinica>() {
      @Override
      @Nullable
      public EvaluacionClinica call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfMotivoConsulta = CursorUtil.getColumnIndexOrThrow(_cursor, "motivoConsulta");
          final int _cursorIndexOfSintomas = CursorUtil.getColumnIndexOrThrow(_cursor, "sintomas");
          final int _cursorIndexOfAntecedentesPersonalesOculares = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesPersonalesOculares");
          final int _cursorIndexOfAntecedentesPersonalesSistemicos = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesPersonalesSistemicos");
          final int _cursorIndexOfAntecedentesFamiliaresOculares = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesFamiliaresOculares");
          final int _cursorIndexOfAntecedentesFamiliaresSistemicos = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesFamiliaresSistemicos");
          final int _cursorIndexOfMedicacion = CursorUtil.getColumnIndexOrThrow(_cursor, "medicacion");
          final int _cursorIndexOfAlergias = CursorUtil.getColumnIndexOrThrow(_cursor, "alergias");
          final int _cursorIndexOfNecesidadVisual = CursorUtil.getColumnIndexOrThrow(_cursor, "necesidadVisual");
          final int _cursorIndexOfAvScOdLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOdLejos");
          final int _cursorIndexOfAvScOiLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOiLejos");
          final int _cursorIndexOfAvScOdCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOdCerca");
          final int _cursorIndexOfAvScOiCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOiCerca");
          final int _cursorIndexOfAvCcOdLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOdLejos");
          final int _cursorIndexOfAvCcOiLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOiLejos");
          final int _cursorIndexOfAvCcOdCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOdCerca");
          final int _cursorIndexOfAvCcOiCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOiCerca");
          final int _cursorIndexOfPhOd = CursorUtil.getColumnIndexOrThrow(_cursor, "phOd");
          final int _cursorIndexOfPhOi = CursorUtil.getColumnIndexOrThrow(_cursor, "phOi");
          final int _cursorIndexOfKappaOd = CursorUtil.getColumnIndexOrThrow(_cursor, "kappaOd");
          final int _cursorIndexOfKappaOi = CursorUtil.getColumnIndexOrThrow(_cursor, "kappaOi");
          final int _cursorIndexOfCoverTest6m = CursorUtil.getColumnIndexOrThrow(_cursor, "coverTest6m");
          final int _cursorIndexOfCoverTest40cm = CursorUtil.getColumnIndexOrThrow(_cursor, "coverTest40cm");
          final int _cursorIndexOfCoverTest10cm = CursorUtil.getColumnIndexOrThrow(_cursor, "coverTest10cm");
          final int _cursorIndexOfPpcOr = CursorUtil.getColumnIndexOrThrow(_cursor, "ppcOr");
          final int _cursorIndexOfPpcLuz = CursorUtil.getColumnIndexOrThrow(_cursor, "ppcLuz");
          final int _cursorIndexOfPpcFrl = CursorUtil.getColumnIndexOrThrow(_cursor, "ppcFrl");
          final int _cursorIndexOfReflejoFotomotor = CursorUtil.getColumnIndexOrThrow(_cursor, "reflejoFotomotor");
          final int _cursorIndexOfReflejoConsensual = CursorUtil.getColumnIndexOrThrow(_cursor, "reflejoConsensual");
          final int _cursorIndexOfReflejoAcomodativo = CursorUtil.getColumnIndexOrThrow(_cursor, "reflejoAcomodativo");
          final int _cursorIndexOfK1Od = CursorUtil.getColumnIndexOrThrow(_cursor, "k1Od");
          final int _cursorIndexOfK2Od = CursorUtil.getColumnIndexOrThrow(_cursor, "k2Od");
          final int _cursorIndexOfK1Oi = CursorUtil.getColumnIndexOrThrow(_cursor, "k1Oi");
          final int _cursorIndexOfK2Oi = CursorUtil.getColumnIndexOrThrow(_cursor, "k2Oi");
          final int _cursorIndexOfObjOdEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "objOdEsf");
          final int _cursorIndexOfObjOdCil = CursorUtil.getColumnIndexOrThrow(_cursor, "objOdCil");
          final int _cursorIndexOfObjOdEje = CursorUtil.getColumnIndexOrThrow(_cursor, "objOdEje");
          final int _cursorIndexOfObjOiEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "objOiEsf");
          final int _cursorIndexOfObjOiCil = CursorUtil.getColumnIndexOrThrow(_cursor, "objOiCil");
          final int _cursorIndexOfObjOiEje = CursorUtil.getColumnIndexOrThrow(_cursor, "objOiEje");
          final int _cursorIndexOfSubjOdEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOdEsf");
          final int _cursorIndexOfSubjOdCil = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOdCil");
          final int _cursorIndexOfSubjOdEje = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOdEje");
          final int _cursorIndexOfSubjOiEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOiEsf");
          final int _cursorIndexOfSubjOiCil = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOiCil");
          final int _cursorIndexOfSubjOiEje = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOiEje");
          final int _cursorIndexOfRecetaOdEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOdEsf");
          final int _cursorIndexOfRecetaOdCil = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOdCil");
          final int _cursorIndexOfRecetaOdEje = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOdEje");
          final int _cursorIndexOfRecetaOiEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOiEsf");
          final int _cursorIndexOfRecetaOiCil = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOiCil");
          final int _cursorIndexOfRecetaOiEje = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOiEje");
          final int _cursorIndexOfAddCercaOd = CursorUtil.getColumnIndexOrThrow(_cursor, "addCercaOd");
          final int _cursorIndexOfAddCercaOi = CursorUtil.getColumnIndexOrThrow(_cursor, "addCercaOi");
          final int _cursorIndexOfAddIntermediaOd = CursorUtil.getColumnIndexOrThrow(_cursor, "addIntermediaOd");
          final int _cursorIndexOfAddIntermediaOi = CursorUtil.getColumnIndexOrThrow(_cursor, "addIntermediaOi");
          final int _cursorIndexOfDipLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "dipLejos");
          final int _cursorIndexOfDipCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "dipCerca");
          final int _cursorIndexOfDipIntermedio = CursorUtil.getColumnIndexOrThrow(_cursor, "dipIntermedio");
          final int _cursorIndexOfPrismaOdValor = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOdValor");
          final int _cursorIndexOfPrismaOdBase = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOdBase");
          final int _cursorIndexOfPrismaOiValor = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOiValor");
          final int _cursorIndexOfPrismaOiBase = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOiBase");
          final int _cursorIndexOfDiagnostico = CursorUtil.getColumnIndexOrThrow(_cursor, "diagnostico");
          final int _cursorIndexOfPlanTratamiento = CursorUtil.getColumnIndexOrThrow(_cursor, "planTratamiento");
          final int _cursorIndexOfObservaciones = CursorUtil.getColumnIndexOrThrow(_cursor, "observaciones");
          final int _cursorIndexOfProximaFechaControl = CursorUtil.getColumnIndexOrThrow(_cursor, "proximaFechaControl");
          final EvaluacionClinica _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPacienteId;
            _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpMotivoConsulta;
            _tmpMotivoConsulta = _cursor.getString(_cursorIndexOfMotivoConsulta);
            final String _tmpSintomas;
            _tmpSintomas = _cursor.getString(_cursorIndexOfSintomas);
            final String _tmpAntecedentesPersonalesOculares;
            _tmpAntecedentesPersonalesOculares = _cursor.getString(_cursorIndexOfAntecedentesPersonalesOculares);
            final String _tmpAntecedentesPersonalesSistemicos;
            _tmpAntecedentesPersonalesSistemicos = _cursor.getString(_cursorIndexOfAntecedentesPersonalesSistemicos);
            final String _tmpAntecedentesFamiliaresOculares;
            _tmpAntecedentesFamiliaresOculares = _cursor.getString(_cursorIndexOfAntecedentesFamiliaresOculares);
            final String _tmpAntecedentesFamiliaresSistemicos;
            _tmpAntecedentesFamiliaresSistemicos = _cursor.getString(_cursorIndexOfAntecedentesFamiliaresSistemicos);
            final String _tmpMedicacion;
            _tmpMedicacion = _cursor.getString(_cursorIndexOfMedicacion);
            final String _tmpAlergias;
            _tmpAlergias = _cursor.getString(_cursorIndexOfAlergias);
            final List<String> _tmpNecesidadVisual;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfNecesidadVisual);
            _tmpNecesidadVisual = __converters.toStringList(_tmp);
            final String _tmpAvScOdLejos;
            _tmpAvScOdLejos = _cursor.getString(_cursorIndexOfAvScOdLejos);
            final String _tmpAvScOiLejos;
            _tmpAvScOiLejos = _cursor.getString(_cursorIndexOfAvScOiLejos);
            final String _tmpAvScOdCerca;
            _tmpAvScOdCerca = _cursor.getString(_cursorIndexOfAvScOdCerca);
            final String _tmpAvScOiCerca;
            _tmpAvScOiCerca = _cursor.getString(_cursorIndexOfAvScOiCerca);
            final String _tmpAvCcOdLejos;
            _tmpAvCcOdLejos = _cursor.getString(_cursorIndexOfAvCcOdLejos);
            final String _tmpAvCcOiLejos;
            _tmpAvCcOiLejos = _cursor.getString(_cursorIndexOfAvCcOiLejos);
            final String _tmpAvCcOdCerca;
            _tmpAvCcOdCerca = _cursor.getString(_cursorIndexOfAvCcOdCerca);
            final String _tmpAvCcOiCerca;
            _tmpAvCcOiCerca = _cursor.getString(_cursorIndexOfAvCcOiCerca);
            final String _tmpPhOd;
            _tmpPhOd = _cursor.getString(_cursorIndexOfPhOd);
            final String _tmpPhOi;
            _tmpPhOi = _cursor.getString(_cursorIndexOfPhOi);
            final String _tmpKappaOd;
            _tmpKappaOd = _cursor.getString(_cursorIndexOfKappaOd);
            final String _tmpKappaOi;
            _tmpKappaOi = _cursor.getString(_cursorIndexOfKappaOi);
            final String _tmpCoverTest6m;
            _tmpCoverTest6m = _cursor.getString(_cursorIndexOfCoverTest6m);
            final String _tmpCoverTest40cm;
            _tmpCoverTest40cm = _cursor.getString(_cursorIndexOfCoverTest40cm);
            final String _tmpCoverTest10cm;
            _tmpCoverTest10cm = _cursor.getString(_cursorIndexOfCoverTest10cm);
            final String _tmpPpcOr;
            _tmpPpcOr = _cursor.getString(_cursorIndexOfPpcOr);
            final String _tmpPpcLuz;
            _tmpPpcLuz = _cursor.getString(_cursorIndexOfPpcLuz);
            final String _tmpPpcFrl;
            _tmpPpcFrl = _cursor.getString(_cursorIndexOfPpcFrl);
            final String _tmpReflejoFotomotor;
            _tmpReflejoFotomotor = _cursor.getString(_cursorIndexOfReflejoFotomotor);
            final String _tmpReflejoConsensual;
            _tmpReflejoConsensual = _cursor.getString(_cursorIndexOfReflejoConsensual);
            final String _tmpReflejoAcomodativo;
            _tmpReflejoAcomodativo = _cursor.getString(_cursorIndexOfReflejoAcomodativo);
            final String _tmpK1Od;
            _tmpK1Od = _cursor.getString(_cursorIndexOfK1Od);
            final String _tmpK2Od;
            _tmpK2Od = _cursor.getString(_cursorIndexOfK2Od);
            final String _tmpK1Oi;
            _tmpK1Oi = _cursor.getString(_cursorIndexOfK1Oi);
            final String _tmpK2Oi;
            _tmpK2Oi = _cursor.getString(_cursorIndexOfK2Oi);
            final String _tmpObjOdEsf;
            _tmpObjOdEsf = _cursor.getString(_cursorIndexOfObjOdEsf);
            final String _tmpObjOdCil;
            _tmpObjOdCil = _cursor.getString(_cursorIndexOfObjOdCil);
            final String _tmpObjOdEje;
            _tmpObjOdEje = _cursor.getString(_cursorIndexOfObjOdEje);
            final String _tmpObjOiEsf;
            _tmpObjOiEsf = _cursor.getString(_cursorIndexOfObjOiEsf);
            final String _tmpObjOiCil;
            _tmpObjOiCil = _cursor.getString(_cursorIndexOfObjOiCil);
            final String _tmpObjOiEje;
            _tmpObjOiEje = _cursor.getString(_cursorIndexOfObjOiEje);
            final String _tmpSubjOdEsf;
            _tmpSubjOdEsf = _cursor.getString(_cursorIndexOfSubjOdEsf);
            final String _tmpSubjOdCil;
            _tmpSubjOdCil = _cursor.getString(_cursorIndexOfSubjOdCil);
            final String _tmpSubjOdEje;
            _tmpSubjOdEje = _cursor.getString(_cursorIndexOfSubjOdEje);
            final String _tmpSubjOiEsf;
            _tmpSubjOiEsf = _cursor.getString(_cursorIndexOfSubjOiEsf);
            final String _tmpSubjOiCil;
            _tmpSubjOiCil = _cursor.getString(_cursorIndexOfSubjOiCil);
            final String _tmpSubjOiEje;
            _tmpSubjOiEje = _cursor.getString(_cursorIndexOfSubjOiEje);
            final String _tmpRecetaOdEsf;
            _tmpRecetaOdEsf = _cursor.getString(_cursorIndexOfRecetaOdEsf);
            final String _tmpRecetaOdCil;
            _tmpRecetaOdCil = _cursor.getString(_cursorIndexOfRecetaOdCil);
            final String _tmpRecetaOdEje;
            _tmpRecetaOdEje = _cursor.getString(_cursorIndexOfRecetaOdEje);
            final String _tmpRecetaOiEsf;
            _tmpRecetaOiEsf = _cursor.getString(_cursorIndexOfRecetaOiEsf);
            final String _tmpRecetaOiCil;
            _tmpRecetaOiCil = _cursor.getString(_cursorIndexOfRecetaOiCil);
            final String _tmpRecetaOiEje;
            _tmpRecetaOiEje = _cursor.getString(_cursorIndexOfRecetaOiEje);
            final String _tmpAddCercaOd;
            _tmpAddCercaOd = _cursor.getString(_cursorIndexOfAddCercaOd);
            final String _tmpAddCercaOi;
            _tmpAddCercaOi = _cursor.getString(_cursorIndexOfAddCercaOi);
            final String _tmpAddIntermediaOd;
            _tmpAddIntermediaOd = _cursor.getString(_cursorIndexOfAddIntermediaOd);
            final String _tmpAddIntermediaOi;
            _tmpAddIntermediaOi = _cursor.getString(_cursorIndexOfAddIntermediaOi);
            final String _tmpDipLejos;
            _tmpDipLejos = _cursor.getString(_cursorIndexOfDipLejos);
            final String _tmpDipCerca;
            _tmpDipCerca = _cursor.getString(_cursorIndexOfDipCerca);
            final String _tmpDipIntermedio;
            _tmpDipIntermedio = _cursor.getString(_cursorIndexOfDipIntermedio);
            final String _tmpPrismaOdValor;
            _tmpPrismaOdValor = _cursor.getString(_cursorIndexOfPrismaOdValor);
            final String _tmpPrismaOdBase;
            _tmpPrismaOdBase = _cursor.getString(_cursorIndexOfPrismaOdBase);
            final String _tmpPrismaOiValor;
            _tmpPrismaOiValor = _cursor.getString(_cursorIndexOfPrismaOiValor);
            final String _tmpPrismaOiBase;
            _tmpPrismaOiBase = _cursor.getString(_cursorIndexOfPrismaOiBase);
            final String _tmpDiagnostico;
            _tmpDiagnostico = _cursor.getString(_cursorIndexOfDiagnostico);
            final String _tmpPlanTratamiento;
            _tmpPlanTratamiento = _cursor.getString(_cursorIndexOfPlanTratamiento);
            final String _tmpObservaciones;
            _tmpObservaciones = _cursor.getString(_cursorIndexOfObservaciones);
            final String _tmpProximaFechaControl;
            _tmpProximaFechaControl = _cursor.getString(_cursorIndexOfProximaFechaControl);
            _result = new EvaluacionClinica(_tmpId,_tmpPacienteId,_tmpFecha,_tmpMotivoConsulta,_tmpSintomas,_tmpAntecedentesPersonalesOculares,_tmpAntecedentesPersonalesSistemicos,_tmpAntecedentesFamiliaresOculares,_tmpAntecedentesFamiliaresSistemicos,_tmpMedicacion,_tmpAlergias,_tmpNecesidadVisual,_tmpAvScOdLejos,_tmpAvScOiLejos,_tmpAvScOdCerca,_tmpAvScOiCerca,_tmpAvCcOdLejos,_tmpAvCcOiLejos,_tmpAvCcOdCerca,_tmpAvCcOiCerca,_tmpPhOd,_tmpPhOi,_tmpKappaOd,_tmpKappaOi,_tmpCoverTest6m,_tmpCoverTest40cm,_tmpCoverTest10cm,_tmpPpcOr,_tmpPpcLuz,_tmpPpcFrl,_tmpReflejoFotomotor,_tmpReflejoConsensual,_tmpReflejoAcomodativo,_tmpK1Od,_tmpK2Od,_tmpK1Oi,_tmpK2Oi,_tmpObjOdEsf,_tmpObjOdCil,_tmpObjOdEje,_tmpObjOiEsf,_tmpObjOiCil,_tmpObjOiEje,_tmpSubjOdEsf,_tmpSubjOdCil,_tmpSubjOdEje,_tmpSubjOiEsf,_tmpSubjOiCil,_tmpSubjOiEje,_tmpRecetaOdEsf,_tmpRecetaOdCil,_tmpRecetaOdEje,_tmpRecetaOiEsf,_tmpRecetaOiCil,_tmpRecetaOiEje,_tmpAddCercaOd,_tmpAddCercaOi,_tmpAddIntermediaOd,_tmpAddIntermediaOi,_tmpDipLejos,_tmpDipCerca,_tmpDipIntermedio,_tmpPrismaOdValor,_tmpPrismaOdBase,_tmpPrismaOiValor,_tmpPrismaOiBase,_tmpDiagnostico,_tmpPlanTratamiento,_tmpObservaciones,_tmpProximaFechaControl);
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
  public Object getAllEvaluaciones(
      final Continuation<? super List<EvaluacionClinica>> $completion) {
    final String _sql = "SELECT * FROM evaluaciones";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<EvaluacionClinica>>() {
      @Override
      @NonNull
      public List<EvaluacionClinica> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPacienteId = CursorUtil.getColumnIndexOrThrow(_cursor, "pacienteId");
          final int _cursorIndexOfFecha = CursorUtil.getColumnIndexOrThrow(_cursor, "fecha");
          final int _cursorIndexOfMotivoConsulta = CursorUtil.getColumnIndexOrThrow(_cursor, "motivoConsulta");
          final int _cursorIndexOfSintomas = CursorUtil.getColumnIndexOrThrow(_cursor, "sintomas");
          final int _cursorIndexOfAntecedentesPersonalesOculares = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesPersonalesOculares");
          final int _cursorIndexOfAntecedentesPersonalesSistemicos = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesPersonalesSistemicos");
          final int _cursorIndexOfAntecedentesFamiliaresOculares = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesFamiliaresOculares");
          final int _cursorIndexOfAntecedentesFamiliaresSistemicos = CursorUtil.getColumnIndexOrThrow(_cursor, "antecedentesFamiliaresSistemicos");
          final int _cursorIndexOfMedicacion = CursorUtil.getColumnIndexOrThrow(_cursor, "medicacion");
          final int _cursorIndexOfAlergias = CursorUtil.getColumnIndexOrThrow(_cursor, "alergias");
          final int _cursorIndexOfNecesidadVisual = CursorUtil.getColumnIndexOrThrow(_cursor, "necesidadVisual");
          final int _cursorIndexOfAvScOdLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOdLejos");
          final int _cursorIndexOfAvScOiLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOiLejos");
          final int _cursorIndexOfAvScOdCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOdCerca");
          final int _cursorIndexOfAvScOiCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avScOiCerca");
          final int _cursorIndexOfAvCcOdLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOdLejos");
          final int _cursorIndexOfAvCcOiLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOiLejos");
          final int _cursorIndexOfAvCcOdCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOdCerca");
          final int _cursorIndexOfAvCcOiCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "avCcOiCerca");
          final int _cursorIndexOfPhOd = CursorUtil.getColumnIndexOrThrow(_cursor, "phOd");
          final int _cursorIndexOfPhOi = CursorUtil.getColumnIndexOrThrow(_cursor, "phOi");
          final int _cursorIndexOfKappaOd = CursorUtil.getColumnIndexOrThrow(_cursor, "kappaOd");
          final int _cursorIndexOfKappaOi = CursorUtil.getColumnIndexOrThrow(_cursor, "kappaOi");
          final int _cursorIndexOfCoverTest6m = CursorUtil.getColumnIndexOrThrow(_cursor, "coverTest6m");
          final int _cursorIndexOfCoverTest40cm = CursorUtil.getColumnIndexOrThrow(_cursor, "coverTest40cm");
          final int _cursorIndexOfCoverTest10cm = CursorUtil.getColumnIndexOrThrow(_cursor, "coverTest10cm");
          final int _cursorIndexOfPpcOr = CursorUtil.getColumnIndexOrThrow(_cursor, "ppcOr");
          final int _cursorIndexOfPpcLuz = CursorUtil.getColumnIndexOrThrow(_cursor, "ppcLuz");
          final int _cursorIndexOfPpcFrl = CursorUtil.getColumnIndexOrThrow(_cursor, "ppcFrl");
          final int _cursorIndexOfReflejoFotomotor = CursorUtil.getColumnIndexOrThrow(_cursor, "reflejoFotomotor");
          final int _cursorIndexOfReflejoConsensual = CursorUtil.getColumnIndexOrThrow(_cursor, "reflejoConsensual");
          final int _cursorIndexOfReflejoAcomodativo = CursorUtil.getColumnIndexOrThrow(_cursor, "reflejoAcomodativo");
          final int _cursorIndexOfK1Od = CursorUtil.getColumnIndexOrThrow(_cursor, "k1Od");
          final int _cursorIndexOfK2Od = CursorUtil.getColumnIndexOrThrow(_cursor, "k2Od");
          final int _cursorIndexOfK1Oi = CursorUtil.getColumnIndexOrThrow(_cursor, "k1Oi");
          final int _cursorIndexOfK2Oi = CursorUtil.getColumnIndexOrThrow(_cursor, "k2Oi");
          final int _cursorIndexOfObjOdEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "objOdEsf");
          final int _cursorIndexOfObjOdCil = CursorUtil.getColumnIndexOrThrow(_cursor, "objOdCil");
          final int _cursorIndexOfObjOdEje = CursorUtil.getColumnIndexOrThrow(_cursor, "objOdEje");
          final int _cursorIndexOfObjOiEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "objOiEsf");
          final int _cursorIndexOfObjOiCil = CursorUtil.getColumnIndexOrThrow(_cursor, "objOiCil");
          final int _cursorIndexOfObjOiEje = CursorUtil.getColumnIndexOrThrow(_cursor, "objOiEje");
          final int _cursorIndexOfSubjOdEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOdEsf");
          final int _cursorIndexOfSubjOdCil = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOdCil");
          final int _cursorIndexOfSubjOdEje = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOdEje");
          final int _cursorIndexOfSubjOiEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOiEsf");
          final int _cursorIndexOfSubjOiCil = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOiCil");
          final int _cursorIndexOfSubjOiEje = CursorUtil.getColumnIndexOrThrow(_cursor, "subjOiEje");
          final int _cursorIndexOfRecetaOdEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOdEsf");
          final int _cursorIndexOfRecetaOdCil = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOdCil");
          final int _cursorIndexOfRecetaOdEje = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOdEje");
          final int _cursorIndexOfRecetaOiEsf = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOiEsf");
          final int _cursorIndexOfRecetaOiCil = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOiCil");
          final int _cursorIndexOfRecetaOiEje = CursorUtil.getColumnIndexOrThrow(_cursor, "recetaOiEje");
          final int _cursorIndexOfAddCercaOd = CursorUtil.getColumnIndexOrThrow(_cursor, "addCercaOd");
          final int _cursorIndexOfAddCercaOi = CursorUtil.getColumnIndexOrThrow(_cursor, "addCercaOi");
          final int _cursorIndexOfAddIntermediaOd = CursorUtil.getColumnIndexOrThrow(_cursor, "addIntermediaOd");
          final int _cursorIndexOfAddIntermediaOi = CursorUtil.getColumnIndexOrThrow(_cursor, "addIntermediaOi");
          final int _cursorIndexOfDipLejos = CursorUtil.getColumnIndexOrThrow(_cursor, "dipLejos");
          final int _cursorIndexOfDipCerca = CursorUtil.getColumnIndexOrThrow(_cursor, "dipCerca");
          final int _cursorIndexOfDipIntermedio = CursorUtil.getColumnIndexOrThrow(_cursor, "dipIntermedio");
          final int _cursorIndexOfPrismaOdValor = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOdValor");
          final int _cursorIndexOfPrismaOdBase = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOdBase");
          final int _cursorIndexOfPrismaOiValor = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOiValor");
          final int _cursorIndexOfPrismaOiBase = CursorUtil.getColumnIndexOrThrow(_cursor, "prismaOiBase");
          final int _cursorIndexOfDiagnostico = CursorUtil.getColumnIndexOrThrow(_cursor, "diagnostico");
          final int _cursorIndexOfPlanTratamiento = CursorUtil.getColumnIndexOrThrow(_cursor, "planTratamiento");
          final int _cursorIndexOfObservaciones = CursorUtil.getColumnIndexOrThrow(_cursor, "observaciones");
          final int _cursorIndexOfProximaFechaControl = CursorUtil.getColumnIndexOrThrow(_cursor, "proximaFechaControl");
          final List<EvaluacionClinica> _result = new ArrayList<EvaluacionClinica>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EvaluacionClinica _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPacienteId;
            _tmpPacienteId = _cursor.getString(_cursorIndexOfPacienteId);
            final long _tmpFecha;
            _tmpFecha = _cursor.getLong(_cursorIndexOfFecha);
            final String _tmpMotivoConsulta;
            _tmpMotivoConsulta = _cursor.getString(_cursorIndexOfMotivoConsulta);
            final String _tmpSintomas;
            _tmpSintomas = _cursor.getString(_cursorIndexOfSintomas);
            final String _tmpAntecedentesPersonalesOculares;
            _tmpAntecedentesPersonalesOculares = _cursor.getString(_cursorIndexOfAntecedentesPersonalesOculares);
            final String _tmpAntecedentesPersonalesSistemicos;
            _tmpAntecedentesPersonalesSistemicos = _cursor.getString(_cursorIndexOfAntecedentesPersonalesSistemicos);
            final String _tmpAntecedentesFamiliaresOculares;
            _tmpAntecedentesFamiliaresOculares = _cursor.getString(_cursorIndexOfAntecedentesFamiliaresOculares);
            final String _tmpAntecedentesFamiliaresSistemicos;
            _tmpAntecedentesFamiliaresSistemicos = _cursor.getString(_cursorIndexOfAntecedentesFamiliaresSistemicos);
            final String _tmpMedicacion;
            _tmpMedicacion = _cursor.getString(_cursorIndexOfMedicacion);
            final String _tmpAlergias;
            _tmpAlergias = _cursor.getString(_cursorIndexOfAlergias);
            final List<String> _tmpNecesidadVisual;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfNecesidadVisual);
            _tmpNecesidadVisual = __converters.toStringList(_tmp);
            final String _tmpAvScOdLejos;
            _tmpAvScOdLejos = _cursor.getString(_cursorIndexOfAvScOdLejos);
            final String _tmpAvScOiLejos;
            _tmpAvScOiLejos = _cursor.getString(_cursorIndexOfAvScOiLejos);
            final String _tmpAvScOdCerca;
            _tmpAvScOdCerca = _cursor.getString(_cursorIndexOfAvScOdCerca);
            final String _tmpAvScOiCerca;
            _tmpAvScOiCerca = _cursor.getString(_cursorIndexOfAvScOiCerca);
            final String _tmpAvCcOdLejos;
            _tmpAvCcOdLejos = _cursor.getString(_cursorIndexOfAvCcOdLejos);
            final String _tmpAvCcOiLejos;
            _tmpAvCcOiLejos = _cursor.getString(_cursorIndexOfAvCcOiLejos);
            final String _tmpAvCcOdCerca;
            _tmpAvCcOdCerca = _cursor.getString(_cursorIndexOfAvCcOdCerca);
            final String _tmpAvCcOiCerca;
            _tmpAvCcOiCerca = _cursor.getString(_cursorIndexOfAvCcOiCerca);
            final String _tmpPhOd;
            _tmpPhOd = _cursor.getString(_cursorIndexOfPhOd);
            final String _tmpPhOi;
            _tmpPhOi = _cursor.getString(_cursorIndexOfPhOi);
            final String _tmpKappaOd;
            _tmpKappaOd = _cursor.getString(_cursorIndexOfKappaOd);
            final String _tmpKappaOi;
            _tmpKappaOi = _cursor.getString(_cursorIndexOfKappaOi);
            final String _tmpCoverTest6m;
            _tmpCoverTest6m = _cursor.getString(_cursorIndexOfCoverTest6m);
            final String _tmpCoverTest40cm;
            _tmpCoverTest40cm = _cursor.getString(_cursorIndexOfCoverTest40cm);
            final String _tmpCoverTest10cm;
            _tmpCoverTest10cm = _cursor.getString(_cursorIndexOfCoverTest10cm);
            final String _tmpPpcOr;
            _tmpPpcOr = _cursor.getString(_cursorIndexOfPpcOr);
            final String _tmpPpcLuz;
            _tmpPpcLuz = _cursor.getString(_cursorIndexOfPpcLuz);
            final String _tmpPpcFrl;
            _tmpPpcFrl = _cursor.getString(_cursorIndexOfPpcFrl);
            final String _tmpReflejoFotomotor;
            _tmpReflejoFotomotor = _cursor.getString(_cursorIndexOfReflejoFotomotor);
            final String _tmpReflejoConsensual;
            _tmpReflejoConsensual = _cursor.getString(_cursorIndexOfReflejoConsensual);
            final String _tmpReflejoAcomodativo;
            _tmpReflejoAcomodativo = _cursor.getString(_cursorIndexOfReflejoAcomodativo);
            final String _tmpK1Od;
            _tmpK1Od = _cursor.getString(_cursorIndexOfK1Od);
            final String _tmpK2Od;
            _tmpK2Od = _cursor.getString(_cursorIndexOfK2Od);
            final String _tmpK1Oi;
            _tmpK1Oi = _cursor.getString(_cursorIndexOfK1Oi);
            final String _tmpK2Oi;
            _tmpK2Oi = _cursor.getString(_cursorIndexOfK2Oi);
            final String _tmpObjOdEsf;
            _tmpObjOdEsf = _cursor.getString(_cursorIndexOfObjOdEsf);
            final String _tmpObjOdCil;
            _tmpObjOdCil = _cursor.getString(_cursorIndexOfObjOdCil);
            final String _tmpObjOdEje;
            _tmpObjOdEje = _cursor.getString(_cursorIndexOfObjOdEje);
            final String _tmpObjOiEsf;
            _tmpObjOiEsf = _cursor.getString(_cursorIndexOfObjOiEsf);
            final String _tmpObjOiCil;
            _tmpObjOiCil = _cursor.getString(_cursorIndexOfObjOiCil);
            final String _tmpObjOiEje;
            _tmpObjOiEje = _cursor.getString(_cursorIndexOfObjOiEje);
            final String _tmpSubjOdEsf;
            _tmpSubjOdEsf = _cursor.getString(_cursorIndexOfSubjOdEsf);
            final String _tmpSubjOdCil;
            _tmpSubjOdCil = _cursor.getString(_cursorIndexOfSubjOdCil);
            final String _tmpSubjOdEje;
            _tmpSubjOdEje = _cursor.getString(_cursorIndexOfSubjOdEje);
            final String _tmpSubjOiEsf;
            _tmpSubjOiEsf = _cursor.getString(_cursorIndexOfSubjOiEsf);
            final String _tmpSubjOiCil;
            _tmpSubjOiCil = _cursor.getString(_cursorIndexOfSubjOiCil);
            final String _tmpSubjOiEje;
            _tmpSubjOiEje = _cursor.getString(_cursorIndexOfSubjOiEje);
            final String _tmpRecetaOdEsf;
            _tmpRecetaOdEsf = _cursor.getString(_cursorIndexOfRecetaOdEsf);
            final String _tmpRecetaOdCil;
            _tmpRecetaOdCil = _cursor.getString(_cursorIndexOfRecetaOdCil);
            final String _tmpRecetaOdEje;
            _tmpRecetaOdEje = _cursor.getString(_cursorIndexOfRecetaOdEje);
            final String _tmpRecetaOiEsf;
            _tmpRecetaOiEsf = _cursor.getString(_cursorIndexOfRecetaOiEsf);
            final String _tmpRecetaOiCil;
            _tmpRecetaOiCil = _cursor.getString(_cursorIndexOfRecetaOiCil);
            final String _tmpRecetaOiEje;
            _tmpRecetaOiEje = _cursor.getString(_cursorIndexOfRecetaOiEje);
            final String _tmpAddCercaOd;
            _tmpAddCercaOd = _cursor.getString(_cursorIndexOfAddCercaOd);
            final String _tmpAddCercaOi;
            _tmpAddCercaOi = _cursor.getString(_cursorIndexOfAddCercaOi);
            final String _tmpAddIntermediaOd;
            _tmpAddIntermediaOd = _cursor.getString(_cursorIndexOfAddIntermediaOd);
            final String _tmpAddIntermediaOi;
            _tmpAddIntermediaOi = _cursor.getString(_cursorIndexOfAddIntermediaOi);
            final String _tmpDipLejos;
            _tmpDipLejos = _cursor.getString(_cursorIndexOfDipLejos);
            final String _tmpDipCerca;
            _tmpDipCerca = _cursor.getString(_cursorIndexOfDipCerca);
            final String _tmpDipIntermedio;
            _tmpDipIntermedio = _cursor.getString(_cursorIndexOfDipIntermedio);
            final String _tmpPrismaOdValor;
            _tmpPrismaOdValor = _cursor.getString(_cursorIndexOfPrismaOdValor);
            final String _tmpPrismaOdBase;
            _tmpPrismaOdBase = _cursor.getString(_cursorIndexOfPrismaOdBase);
            final String _tmpPrismaOiValor;
            _tmpPrismaOiValor = _cursor.getString(_cursorIndexOfPrismaOiValor);
            final String _tmpPrismaOiBase;
            _tmpPrismaOiBase = _cursor.getString(_cursorIndexOfPrismaOiBase);
            final String _tmpDiagnostico;
            _tmpDiagnostico = _cursor.getString(_cursorIndexOfDiagnostico);
            final String _tmpPlanTratamiento;
            _tmpPlanTratamiento = _cursor.getString(_cursorIndexOfPlanTratamiento);
            final String _tmpObservaciones;
            _tmpObservaciones = _cursor.getString(_cursorIndexOfObservaciones);
            final String _tmpProximaFechaControl;
            _tmpProximaFechaControl = _cursor.getString(_cursorIndexOfProximaFechaControl);
            _item = new EvaluacionClinica(_tmpId,_tmpPacienteId,_tmpFecha,_tmpMotivoConsulta,_tmpSintomas,_tmpAntecedentesPersonalesOculares,_tmpAntecedentesPersonalesSistemicos,_tmpAntecedentesFamiliaresOculares,_tmpAntecedentesFamiliaresSistemicos,_tmpMedicacion,_tmpAlergias,_tmpNecesidadVisual,_tmpAvScOdLejos,_tmpAvScOiLejos,_tmpAvScOdCerca,_tmpAvScOiCerca,_tmpAvCcOdLejos,_tmpAvCcOiLejos,_tmpAvCcOdCerca,_tmpAvCcOiCerca,_tmpPhOd,_tmpPhOi,_tmpKappaOd,_tmpKappaOi,_tmpCoverTest6m,_tmpCoverTest40cm,_tmpCoverTest10cm,_tmpPpcOr,_tmpPpcLuz,_tmpPpcFrl,_tmpReflejoFotomotor,_tmpReflejoConsensual,_tmpReflejoAcomodativo,_tmpK1Od,_tmpK2Od,_tmpK1Oi,_tmpK2Oi,_tmpObjOdEsf,_tmpObjOdCil,_tmpObjOdEje,_tmpObjOiEsf,_tmpObjOiCil,_tmpObjOiEje,_tmpSubjOdEsf,_tmpSubjOdCil,_tmpSubjOdEje,_tmpSubjOiEsf,_tmpSubjOiCil,_tmpSubjOiEje,_tmpRecetaOdEsf,_tmpRecetaOdCil,_tmpRecetaOdEje,_tmpRecetaOiEsf,_tmpRecetaOiCil,_tmpRecetaOiEje,_tmpAddCercaOd,_tmpAddCercaOi,_tmpAddIntermediaOd,_tmpAddIntermediaOi,_tmpDipLejos,_tmpDipCerca,_tmpDipIntermedio,_tmpPrismaOdValor,_tmpPrismaOdBase,_tmpPrismaOiValor,_tmpPrismaOiBase,_tmpDiagnostico,_tmpPlanTratamiento,_tmpObservaciones,_tmpProximaFechaControl);
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
