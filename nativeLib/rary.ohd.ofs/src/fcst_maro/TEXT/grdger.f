C MEMBER GRDGER
C  (from old member PPGRDGER)
C
      SUBROUTINE GRDGER(IRETRN, TYPE, MAXVAL, IACT)
C
C.....THIS SUBROUTINE IS CALLED WHEN SUBROUTINE RPDGRD RETURNS AN
C.....ERROR CODE, OR WHEN THERE IS NOT ENOUGH DATA SPACES AVAILABLE ON
C.....THE PREPROCESSOR DATA BASE.
C
C.....HERE ARE THE ARGUMENTS:
C
C.....IRETRN - RETURN CODE FROM SUBROUTINE RPDGRD.
C.....TYPE   - DATA TYPE.
C.....MAXVAL - MAXIMUM NUMBER OF VALUES THAT CAN BE ACCOMODATE ON THE
C.....         PREPROCESSOR DATA BASE.
C.....IACT   - ACTION CODE TO DETERMINE WHAT MESSAGE TO PRINT OUT.
C.....           = 1  RPDGRD ERROR. WRITE MESSAGE GIVING ERROR.
C.....           = 2  INSUFFICIENT DATA BASE SPACE. WRITE MESSAGE.
C
C.....ORIGINALLY WRITTEN BY:
C
C.....JERRY M. NUNN       WGRFC FT. WORTH, TEXAS       DECEMBER 1986
C
      DIMENSION SNAME(2)
C
      INCLUDE 'common/ionum'
      INCLUDE 'gcommon/gsize'
      INCLUDE 'common/where'
      INCLUDE 'common/pudbug'
      INCLUDE 'common/errdat'
C
C    ================================= RCS keyword statements ==========
      CHARACTER*68     RCSKW1,RCSKW2
      DATA             RCSKW1,RCSKW2 /                                 '
     .$Source: /fs/hseb/ob72/rfc/ofs/src/fcst_maro/RCS/grdger.f,v $
     . $',                                                             '
     .$Id: grdger.f,v 1.1 1995/09/17 19:02:27 dws Exp $
     . $' /
C    ===================================================================
C
C
      DATA SNAME /4hGRDG, 4hER  /
C
  901 FORMAT(1H0, '*** ERROR ***  SUBROUTINE RPDGRD RETURNED A STATUS CO
     *DE OF ', I4, ' FOR DATA TYPE ', A4, '.', /, 1X, 'DATA TYPE ', A4,
     * ' CANNOT BE WRITTEN TO THE PREPROCESSOR DATA BASE. PROCESSING WIL
     *L CONTINUE.')
  902 FORMAT(1H0, '*** ERROR ***  THERE ARE ONLY ', I6, ' VALUES THAT CA
     *N BE STORED ON THE PREPROCESSOR DATA BASE FOR DATA TYPE ', A4,
     * '.', /, 1X, 'DATA TYE ', A4, ' REQUIRES ', I6, ' VALUES...AND CAN
     *NOT BE WRITTEN TO THE PREPROCESSOR DATA BASE.', /, 1X, 'PROCESSING
     * CONTINUES.')
  903 FORMAT(1H0, '*** SUBROUTINE GRDGER ENTERED -- TYPE = ', A4,
     * '...MAXIMUM NO. OF VALUES ON PPDB = ', I4, ' ***')
  904 FORMAT(1H0, '*** EXIT SUBROUTINE GRDGER ***')
      INCLUDE 'gcommon/setwhere'
C
      IF(IPTRCE .GT. 0) WRITE(IOPDBG,903) TYPE, MAXVAL
C
C.....TEST THE ACTION CODES FOR THE TYPE OF OUTPUT TO PRODUCE.
C
      IF(IACT .EQ. 1) GOTO 100
      IF(IACT .EQ. 2) GOTO 200
      GOTO 999
C
  100 WRITE(IOERR,901) IRETRN, TYPE, TYPE
      CALL ERROR
      GOTO 999
C
  200 WRITE(IOERR,902) MAXVAL, TYPE, TYPE, NGRID
      CALL ERROR
C
  999 IF(IPTRCE .GT. 0) WRITE(IOPDBG,904)
C
      RETURN
      END
