package com.example.poc2104301453.pinpadservice.commands;

import android.os.Bundle;

import com.example.poc2104301453.pinpadlibrary.ABECS;
import com.example.poc2104301453.pinpadservice.PinpadAbstractionLayer;

import java.util.concurrent.Semaphore;

import br.com.verifone.bibliotecapinpad.AcessoFuncoesPinpad;

public class GIN {
    private static final String TAG_LOGCAT = GIN.class.getSimpleName();

    public static Bundle gin(Bundle input)
            throws Exception {
        AcessoFuncoesPinpad pinpad = PinpadAbstractionLayer.getInstance().getPinpad();
        String CMD_ID  = input.getString(ABECS.CMD_ID);

        final Bundle[] output = { new Bundle() };
        final Semaphore[] semaphore = { new Semaphore(0, true) };

        pinpad.getInfo(saidaComandoGetInfo -> {
            output[0].putString(ABECS.RSP_ID, CMD_ID);
            output[0].putInt   (ABECS.RSP_STAT, 0);

            String GIN_MNAME    = saidaComandoGetInfo.obtemFabricantePinpad();
            String GIN_MODEL    = saidaComandoGetInfo.obtemModeloPinpad();
            String GIN_CTLSSUP  = (saidaComandoGetInfo.obtemCapacidadesPinpad().suportaContactless()) ? "C" : " ";
            String GIN_SOVER    = saidaComandoGetInfo.obtemVersaoSistemaOperacionalPinpad();
            String GIN_SPECVER  = saidaComandoGetInfo.obtemVersaoEspecificacao();
            String GIN_MANVER   = saidaComandoGetInfo.obtemVersaoAplicacaoGerenciadora();
            String GIN_SERNUM   = saidaComandoGetInfo.obtemNumeroSeriePinpad();

            String GIN_ACQNAM   = saidaComandoGetInfo.obtemNomeAdquirente();
                   GIN_ACQNAM   = (GIN_ACQNAM != null) ? GIN_ACQNAM : "ABECS";

            String GIN_KRNLVER  = saidaComandoGetInfo.obtemVersaoKernelEMV();
            String GIN_APPVERS  = saidaComandoGetInfo.obtemVersaoAplicacaoAbecs();
            String GIN_CTLSVER  = saidaComandoGetInfo.obtemVersaoKernelCtls();

            String GIN_MCTLSVER = saidaComandoGetInfo.obtemVersaoKernelCtlsMasterPayPass();
                   GIN_MCTLSVER = (GIN_MCTLSVER != null) ? GIN_MCTLSVER : "   ";

            String GIN_VCTLSVER = saidaComandoGetInfo.obtemVersaoKernelCtlsVisaPayWave();
                   GIN_VCTLSVER = (GIN_VCTLSVER != null) ? GIN_VCTLSVER : "   ";

            output[0].putString(ABECS.RSP_ID, CMD_ID);
            output[0]   .putInt(ABECS.RSP_STAT, 0);

            int GIN_ACQIDX;

            GIN_ACQIDX = input.getInt(ABECS.GIN_ACQIDX, -1);

            if (GIN_ACQIDX == -1) {
                GIN_ACQIDX = (int) input.getLong(ABECS.GIN_ACQIDX);
            }

            switch (GIN_ACQIDX) {
                case 0:
                    output[0].putString(ABECS.GIN_MNAME, GIN_MNAME);
                    output[0].putString(ABECS.GIN_MODEL, GIN_MODEL);
                    output[0].putString(ABECS.GIN_CTLSSUP, GIN_CTLSSUP);
                    output[0].putString(ABECS.GIN_SOVER, GIN_SOVER);
                    output[0].putString(ABECS.GIN_MANVER, GIN_MANVER);
                    output[0].putString(ABECS.GIN_SERNUM, GIN_SERNUM);
                    break;

                case 2:
                    output[0].putString(ABECS.GIN_ACQNAM, GIN_ACQNAM);
                    output[0].putString(ABECS.GIN_KRNLVER, GIN_KRNLVER);
                    output[0].putString(ABECS.GIN_APPVERS, GIN_APPVERS);
                    break;

                case 3:
                    output[0].putString(ABECS.GIN_ACQNAM, GIN_ACQNAM);
                    output[0].putString(ABECS.GIN_KRNLVER, GIN_KRNLVER);
                    output[0].putString(ABECS.GIN_CTLSVER, GIN_CTLSVER);
                    output[0].putString(ABECS.GIN_MCTLSVER, GIN_MCTLSVER);
                    output[0].putString(ABECS.GIN_VCTLSVER, GIN_VCTLSVER);
                    output[0].putString(ABECS.GIN_APPVERS, GIN_APPVERS);
                    output[0].putString(ABECS.GIN_DUKPT, " ");
                    break;

                default:
                    output[0].putString(ABECS.GIN_ACQNAM, GIN_ACQNAM);
                    output[0].putString(ABECS.GIN_APPVERS, GIN_APPVERS);
                    break;
            }

            output[0].putString(ABECS.GIN_SPECVER, GIN_SPECVER);

            semaphore[0].release();
        });

        semaphore[0].acquireUninterruptibly();

        return output[0];
    }
}