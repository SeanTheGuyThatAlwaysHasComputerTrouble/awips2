C MEMBER TAB26
C  (from old member FCTAB26)
C
      SUBROUTINE TAB26(TO,LEFT,IUSET,NXT,LPO,PO,LCO,TS,
     1MTS,NWORK,LWORK,IDT)
C.......................................
C     THIS IS THE OPERATIONS TABLE ENTRY SUBROUTINE FOR THE
C      SINGLE RESERVOIR SIMULATION MODEL OPERATION--
C      'RES-SNGL'.
C.......................................
C     INITIALLY WRITTEN BY -- ERIC ANDERSON, HRL--DECEMBER 1983.
C.......................................
      INTEGER TO(1)
      DIMENSION TS(MTS),PO(1)
      DIMENSION SNAME(2)
C
C     COMMON BLOCK
      COMMON/FDBUG/IODBUG,ITRACE,IDBALL,NDEBUG,IDEBUG(20)
C
C    ================================= RCS keyword statements ==========
      CHARACTER*68     RCSKW1,RCSKW2
      DATA             RCSKW1,RCSKW2 /                                 '
     .$Source: /fs/hseb/ob72/rfc/ofs/src/fcinit_pntb/RCS/tab26.f,v $
     . $',                                                             '
     .$Id: tab26.f,v 1.1 1995/09/17 18:53:11 dws Exp $
     . $' /
C    ===================================================================
C
C
C     DATA STATEMENT
      DATA SNAME/4HTAB2,4H6   /
      DATA BLANK/4H    /
C.......................................
C     TRACE LEVEL=1,DEBUG SWITCH=IBUG
      CALL FPRBUG(SNAME,1,26,IBUG)
C.......................................
C     OBTAIN CONTROL VARIABLES
      LOC=PO(11)
      LRC=PO(17)
      NTTS=PO(LOC)
      NRC=0
      IF (LRC.GT.0) NRC=PO(LRC)
      LENGTH=6+NTTS+NRC
C.......................................
C     CHECK TO SEE IF ENOUGH SPACE AVIALABLE IN T( ).
      CALL CHECKT(LENGTH,LEFT,IERR)
      IF(IERR.EQ.0)GO TO 100
      IUSET=0
      IDT=0
      LWORK=0
      RETURN
C.......................................
C     SPACE IS AVIALABLE -- MAKE ETRIES INTO TO( )
 100  TO(1)=26
      TO(2)=NXT+LENGTH
      TO(3)=LPO
      TO(4)=LCO
      TO(5)=NWORK
      TO(6)=NRC
      IF(NRC.EQ.0) GO TO 104
      DO 101 I=1,NRC
 101  TO(6+I)=LPO+LRC+(I-1)*2
 104  K=6+NRC
      L=LOC+1
C
C     CHECK ALL TIME SERIES.
      DO 110 I=1,NTTS
      IF((PO(L).NE.BLANK).OR.(PO(L+1).NE.BLANK)) GO TO 105
C
C     TIME SERIES NOT USED.
      TO(K+I)=0
      L=L+2
      GO TO 110
C
C     TIME SERIES USED.
 105  IOUT=PO(L+4)
      IT=PO(L+3)
      IF(IOUT.EQ.1)GO TO 107
C     INPUT TIME SERIES
      CALL CKINPT(PO(L),PO(L+2),IT,LD,TS,MTS,IERR)
      GO TO 109
C     OUTPUT TIME SERIES
 107  CALL FINDTS(PO(L),PO(L+2),IT,LD,LTS,DIM)
      IF(LTS.GT.0) TS(LTS+8)=1.01
 109  TO(K+I)=LD
      L=L+5
 110  CONTINUE
C
C     GET REMAINING VALUES
      IDT=24
      LWORK=PO(16)
      IUSET=LENGTH
C     ALL ENTRIES INTO TO( ) COMPLETE.
C.......................................
C     DEBUG OUTPUT
      IF(IBUG.EQ.0) GO TO 199
      WRITE(IODBUG,900)IDT,LWORK
 900  FORMAT(1H0,34HTAB26 DEBUG--CONTENTS OF TO ARRAY.,5X,
     1 17HCOMPUTATIONAL DT=,I3,5X,6HLWORK=,I6)
      WRITE(IODBUG,901) (TO(I),I=1,IUSET)
 901  FORMAT(1H ,20I6)
C.......................................
C     CONTENTS OF THE TO ARRAY ARE AS FOLLOWS.
C
C        POSITION                   CONTENTS
C
C           1.            I.D. NUMBER FOR THE OPERATION=26
C           2.         LOCATION OF NEXT OPERATION IN THE T ARRAY.
C           3.         LOCATION OF PARAMETERS IN THE P ARRAY.
C           4.         LOCATION OF CARRYOVER IN THE C ARRAY.
C           5.         LOCATION OF WORK SPACE IN THE D ARRAY
C           6.         NUMBER OF RATING CURVES USED(=0 IF NONE)
C         7 TO         LOCATION OF EACH RATING CURVE
C        6+NRC         IDENTIFIER IN THE P ARRAY.
C        7+NRC TO      LOCATION OF EACH TIME SERIES IN THE
C        6+NRC+NTTS    D ARRAY(=0 IF NOT USED)
C.......................................
 199  CONTINUE
      RETURN
      END
