package com.mguevara.librocontable.Utils;

import java.time.LocalDate;

public final class PeriodoUtils {

    public static String obtenerPeriodo(LocalDate fecha){

        return fecha.getYear()+"-"+String.format("%02d",fecha.getMonthValue());

    }

}