      SUBROUTINE W3FI68 (ID, PDS)
C$$$  SUBPROGRAM DOCUMENTATION BLOCK
C                .      .    .                                       .
C SUBPROGRAM:    W3FI68      CONVERT 25 WORD ARRAY TO GRIB PDS
C   PRGMMR: R.E.JONES        ORG: W/NMC42    DATE: 91-05-14
C
C ABSTRACT: CONVERTS AN ARRAY OF 25, OR 27 INTEGER WORDS INTO A
C   GRIB PRODUCT DEFINITION SECTION (PDS) OF 28 BYTES , OR 30 BYTES.
C   IF PDS BYTES > 30, THEY ARE SET TO ZERO.
C
C PROGRAM HISTORY LOG:
C   91-05-08  R.E.JONES
C   92-09-25  R.E.JONES   CHANGE TO 25 WORDS OF INPUT, LEVEL
C                         CAN BE IN TWO WORDS. (10,11)
C   93-01-08  R.E.JONES   CHANGE FOR TIME RANGE INDICATOR IF 10,
C                         STORE TIME P1 IN PDS BYTES 19-20.
C   93-01-26  R.E.JONES   CORRECTION FOR FIXED HEIGHT ABOVE
C                         GROUND LEVEL
C   93-03-29  R.E.JONES   ADD SAVE STATEMENT
C   93-06-24  CAVANOUGH   MODIFIED PROGRAM TO ALLOW FOR GENERATION
C                         OF PDS GREATER THAN 28 BYTES (THE DESIRED
C                         PDS SIZE IS IN ID(1).
C   93-09-30  FARLEY      CHANGE TO ALLOW FOR SUBCENTER ID; PUT
C                         ID(24) INTO PDS(26).
C   93-10-12  R.E.JONES   CHANGES FOR ON388 REV. OCT 9,1993, NEW
C                         LEVELS 125, 200, 201.
C   94-02-23  R.E.JONES   TAKE OUT SBYTES, REPLACE WITH DO LOOP
C   94-04-14  R.E.JONES   CHANGES FOR ON388 REV. MAR 24,1994, NEW
C                         LEVELS 115,116.
C   94-12-04  R.E.JONES   CHANGE TO ADD ID WORDS 26, 27 FOR PDS
C                         BYTES 29 AND 30.
C   95-09-07  R.E.JONES   CHANGE FOR NEW LEVEL 117, 119.
C   95-10-31  IREDELL     REMOVED SAVES AND PRINTS
C   98-06-30  EBISUZAKI   LINUX PORT
C 2001-06-05  GILBERT     Changed fortran intrinsic function OR() to
C                         f90 standard intrinsic IOR().
C
C USAGE:    CALL W3FI68 (ID, PDS)
C   INPUT ARGUMENT LIST:
C     ID       - 25, 27 WORD INTEGER ARRAY
C   OUTPUT ARGUMENT LIST:
C     PDS      - 28 30,  OR GREATER CHARACTER PDS FOR EDITION 1
C
C REMARKS: LAYOUT OF 'ID' ARRAY:
C     ID(1)  = NUMBER OF BYTES IN PRODUCT DEFINITION SECTION (PDS)
C     ID(2)  = PARAMETER TABLE VERSION NUMBER
C     ID(3)  = IDENTIFICATION OF ORIGINATING CENTER
C     ID(4)  = MODEL IDENTIFICATION (ALLOCATED BY ORIGINATING CENTER)
C     ID(5)  = GRID IDENTIFICATION
C     ID(6)  = 0 IF NO GDS SECTION, 1 IF GDS SECTION IS INCLUDED
C     ID(7)  = 0 IF NO BMS SECTION, 1 IF BMS SECTION IS INCLUDED
C     ID(8)  = INDICATOR OF PARAMETER AND UNITS (TABLE 2)
C     ID(9)  = INDICATOR OF TYPE OF LEVEL       (TABLE 3)
C     ID(10) = VALUE 1 OF LEVEL  (0 FOR 1-100,102,103,105,107
C              109,111,113,115,117,119,125,160,200,201
C              LEVEL IS IN ID WORD 11)
C     ID(11) = VALUE 2 OF LEVEL
C     ID(12) = YEAR OF CENTURY
C     ID(13) = MONTH OF YEAR
C     ID(14) = DAY OF MONTH
C     ID(15) = HOUR OF DAY
C     ID(16) = MINUTE OF HOUR   (IN MOST CASES SET TO 0)
C     ID(17) = FCST TIME UNIT
C     ID(18) = P1 PERIOD OF TIME
C     ID(19) = P2 PERIOD OF TIME
C     ID(20) = TIME RANGE INDICATOR
C     ID(21) = NUMBER INCLUDED IN AVERAGE
C     ID(22) = NUMBER MISSING FROM AVERAGES
C     ID(23) = CENTURY  (20, CHANGE TO 21 ON JAN. 1, 2001)
C     ID(24) = SUBCENTER IDENTIFICATION
C     ID(25) = SCALING POWER OF 10
C     ID(26) = FLAG BYTE, 8 ON/OFF FLAGS
C              BIT NUMBER  VALUE  ID(26)   DEFINITION
C              1           0      0      FULL FCST FIELD
C                          1      128    FCST ERROR FIELD
C              2           0      0      ORIGINAL FCST FIELD
C                          1      64     BIAS CORRECTED FCST FIELD
C              3           0      0      ORIGINAL RESOLUTION RETAINED
C                          1      32     SMOOTHED FIELD
C              NOTE: ID(26) CAN BE THE SUM OF BITS 1, 2, 3.
C              BITS 4-8 NOT USED, SET TO ZERO
C              IF ID(1) IS 28, YOU DO NOT NEED ID(26) AND ID(27).
C     ID(27) = UNUSED, SET TO 0 SO PDS BYTE 30 IS SET TO ZERO.
C
C   SUBPROGRAM CAN BE CALLED FROM A MULTIPROCESSING ENVIRONMENT.
C
C ATTRIBUTES:
C   LANGUAGE: SiliconGraphics 3.5 FORTRAN 77
C   MACHINE:  SiliconGraphics IRIS-4D/25, 35, INDIGO, Indy
C   LANGUAGE: CRAY CFT77 FORTRAN
C   MACHINE:  CRAY C916/256, J916/2048
C
C$$$
C
      INTEGER        ID(*)
C
      CHARACTER * 1  PDS(*)
C
C    ================================= RCS keyword statements ==========
      CHARACTER*68     RCSKW1,RCSKW2
      DATA             RCSKW1,RCSKW2 /                                 '
     .$Source: /fs/hseb/ob72/wfo_rfc/precip_proc/source/gribit/src/RCS/w3fi68.f,v $
     . $',                                                             '
     .$Id: w3fi68.f,v 1.1 2006/05/03 13:44:00 gsood Exp $
     . $' /
C    ===================================================================
C
C
        PDS(1)  = CHAR(MOD(ID(1)/65536,256))
        PDS(2)  = CHAR(MOD(ID(1)/256,256))
        PDS(3)  = CHAR(MOD(ID(1),256))
        PDS(4)  = CHAR(ID(2))
        PDS(5)  = CHAR(ID(3))
        PDS(6)  = CHAR(ID(4))
        PDS(7)  = CHAR(ID(5))
	i = 0
	if (ID(6).ne.0) i = i + 128
	if (ID(7).ne.0) i = i + 64
	PDS(8) = char(i)

        PDS(9)  = CHAR(ID(8))
        PDS(10) = CHAR(ID(9))
        I9      = ID(9)
C
C       TEST TYPE OF LEVEL TO SEE IF LEVEL IS IN TWO
C       WORDS OR ONE
C
        IF ((I9.GE.1.AND.I9.LE.100).OR.I9.EQ.102.OR.
     &       I9.EQ.103.OR.I9.EQ.105.OR.I9.EQ.107.OR.
     &       I9.EQ.109.OR.I9.EQ.111.OR.I9.EQ.113.OR.
     &       I9.EQ.115.OR.I9.EQ.117.OR.I9.EQ.119.OR.
     &       I9.EQ.125.OR.I9.EQ.160.OR.I9.EQ.200.OR.
     &       I9.EQ.201) THEN
          LEVEL   = ID(11)
          IF (LEVEL.LT.0) THEN
            LEVEL = - LEVEL
            LEVEL = IOR(LEVEL,32768)
          END IF
          PDS(11) = CHAR(MOD(LEVEL/256,256))
          PDS(12) = CHAR(MOD(LEVEL,256))
        ELSE
          PDS(11) = CHAR(ID(10))
          PDS(12) = CHAR(ID(11))
        END IF
        PDS(13) = CHAR(ID(12))
        PDS(14) = CHAR(ID(13))
        PDS(15) = CHAR(ID(14))
        PDS(16) = CHAR(ID(15))
        PDS(17) = CHAR(ID(16))
        PDS(18) = CHAR(ID(17))
C
C       TEST TIME RANGE INDICATOR (PDS BYTE 21) FOR 10
C       IF SO PUT TIME P1 IN PDS BYTES 19-20.
C
        IF (ID(20).EQ.10) THEN
          PDS(19) = CHAR(MOD(ID(18)/256,256))
          PDS(20) = CHAR(MOD(ID(18),256))
        ELSE
          PDS(19) = CHAR(ID(18))
          PDS(20) = CHAR(ID(19))
        END IF
        PDS(21) = CHAR(ID(20))
        PDS(22) = CHAR(MOD(ID(21)/256,256))
        PDS(23) = CHAR(MOD(ID(21),256))
        PDS(24) = CHAR(ID(22))
        PDS(25) = CHAR(ID(23))
        PDS(26) = CHAR(ID(24))
        ISCALE  = ID(25)
        IF (ISCALE.LT.0) THEN
          ISCALE = -ISCALE
          ISCALE =  IOR(ISCALE,32768)
        END IF
        PDS(27) = CHAR(MOD(ISCALE/256,256))
        PDS(28) = CHAR(MOD(ISCALE    ,256))
        IF (ID(1).GT.28) THEN
          PDS(29) = CHAR(ID(26))
          PDS(30) = CHAR(ID(27))
        END IF
C
C       SET PDS 31-?? TO ZERO
C
        IF (ID(1).GT.30) THEN
          K = ID(1)
          DO I = 31,K
            PDS(I) = CHAR(0)
          END DO
        END IF
C
      RETURN
      END
