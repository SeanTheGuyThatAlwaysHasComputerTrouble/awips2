C MEMBER SAR51
C---------------------------------------------------------------------
C
C@PROCESS LVL(77)
C
      SUBROUTINE SAR51(WK,IUSEW,LEFTW,NPSAR,NTSAR,NCSAR,OKSAR)
C
C  DESC READS A NON-BACKWATER RESERVOIR INPUT
C  FOR OPERATION 51 PIN ROUTINE.
C----------------------------------------------------------------------
C  ARGS:
C   WK - ARRAY TO HOLD ENCODE INFORMATION
C   IUSEW - NO. OF WORDS ALREADY USED IN WORK ARRAY
C   LEFTW - NO. OF WORDS REMAINING IN WORK ARRAY
C   NPSAR - NO. OF WORDS NEEDED TO HOLD SAR PARM INFO
C   NTSAR - NO. OF WORDS NEEDED TO HOLD SAR TS INFO
C   NCSAR - NO. OF WORDS NEEDED TO HOLD SAR CARRYOVER INFO
C   NRSAR - NO. OF WORDS NEEDED TO HOLD SAR REGULATION CODE INFO
C   OKSAR  - LOGICAL VARIABLE TELLING WHETHER ANY MISTAKES OCCURRED IN
C            NON-BACKWATER SAR SECTION INPUT.
C
C-----------------------------------------------------------------------
C
C  KUANG HSU - HRL - APRIL 1994
C----------------------------------------------------------------
      INCLUDE 'common/ionum'
      INCLUDE 'common/read51'
      INCLUDE 'common/fld51'
      INCLUDE 'common/err51'
      INCLUDE 'common/comn51'
C
      DIMENSION KEYWDS(3,8),LKEYWD(8),LINE(20),IKYUNT(2,4),
     .LKYUNT(4),ENDSAR(2),UNITC(4),DIM(4),WK(*),IKYENG(4),
     . LKYENG(4),IG(4)
C
      LOGICAL OKSAR ,PRINT1,PRINT2,USET
C
C    ================================= RCS keyword statements ==========
      CHARACTER*68     RCSKW1,RCSKW2
      DATA             RCSKW1,RCSKW2 /                                 '
     .$Source: /fs/hseb/ob72/rfc/ofs/src/fcinit_ssarresv/RCS/sar51.f,v $
     . $',                                                             '
     .$Id: sar51.f,v 1.1 1996/03/21 14:35:46 page Exp $
     . $' /
C    ===================================================================
C
C
      DATA KEYWDS/
     .            4HPARM,4HS   ,4H    ,4HP   ,4H    ,4H    ,
     .            4HTIME,4H-SER,4HIES ,4HTS  ,4H    ,4H    ,
     .            4HCARR,4HYOVE,4HR   ,4HCO  ,4H    ,4H    ,
     .            4HRRCO,4HDE  ,4H    ,4HRR  ,4H    ,4H    /
      DATA LKEYWD/2,1,3,1,3,1,2,1/
      DATA NKEYWD/8/
      DATA NDKEY/3/
      DATA BLANK/4H    /
      DATA ENDSAR/4HENDS,4HAR  /
C
      DATA UNITC/4HM   ,4HCMSD,4HCMS ,4HCMSD/
      DATA DIM  /4HL   ,4HL3  ,4HL3/T,4HL3  /
C
C
      DATA IKYUNT/
     .            4HENGL,4HISH ,4HE   ,4H    ,
     .            4HMETR,4HIC  ,4HM   ,4H    /
      DATA LKYUNT/2,1,2,1/
      DATA NKYUNT/4/
      DATA NDUNT/2/
C
      DATA IKYENG/
     .            4HTIMD,4HT   ,
     .            4HACFT,4HA   /
      DATA LKYENG/1,1,1,1/
      DATA NKYENG/4/
      DATA NDENG/1/
C
C  INITIALIZE COUNTERS AND LOCAL VARIABLES
C
      OKSAR  = .TRUE.
      USEDUP = .FALSE.
      USET = .FALSE.
      NPSAR = 0
      NTSAR = 0
      NCSAR = 0
      NRSAR = 0
      NUMERR = 0
      NUMTS = 0
      PRINT1 = .FALSE.
      PRINT2 = .FALSE.
      OKELST = .FALSE.
      IERR = 0
C
C  SET OFFSET FOR START OF INFO IN SAR SECTION
C
      NGOFF = IUSEW
C
      DO 3 I =1,4
           IG(I) = 0
    3 CONTINUE
C
C--------------------------------------------------------------------
C  NOW PROCESS INPUT UP TO 'ENDSAR ', LOOKING FOR, IN ORDER, KEYWORDS
C  FOR PARMS, TIME-SERIES, CARRYOVER AND RRCODE.
C
C  AT THIS POINT, WE CAN FILL THE POINTERS AND LENGTHS OF THE PARM, TS,
C  CO, AND RR INFO IN THE WORK SECTION. EVERYTHING WILL BE A DUMMY VALUE
C  AND WILL BE REFILLED LATER WHEN THE REAL VALUES CAN BE DETERMINED.
C
      DO 390 J=1,8
      CALL FLWK51(WK,IUSEW,LEFTW,0.01,501)
  390 CONTINUE
C
C
C  EACH OF THESE HAVE A PRIMARY AND ABBREVIATED KEYWORD.
C
C
      IGSTRT = LSAR +1
      NSAR2 = NSAR  - 1
      CALL POSN26(MUNI51,IGSTRT)
      LASAR  = NSAR2 - 1
      NCARD = 0
C
  100 IF (NCARD .GE. LASAR ) GO TO 900
      NUMFLD = 0
      CALL UFLD51(NUMFLD,IERF)
      IF (IERF.GT.0) GO TO 9000
C
C  LOOK FOR MATCH OF PROPER KEYWORD
C
      NUMWD = (LEN -1)/4 + 1
      IDEST = IKEY26(CHAR,NUMWD,KEYWDS,LKEYWD,NKEYWD,NDKEY)
      IF (IDEST .GT. 0) GO TO 150
C
C  NO VALID KEYWORD FOUND
C
      CALL STER51(1,1)
      GO TO 100
C
C  NOW SEND TO CONTROL TO LOCATION TO PROCESS PROPER KEYWORD
C
  150 CONTINUE
      LOCPTS = 0
      GO TO (400,400,500,500,600,600,700,700) , IDEST
C
C--------------------------------------------------------------------
C  PARMS EXPECTED, IF NOT FOUND SINAL ERROR. IF FOUND, GO GET PARMS
C   NEEDED FOR OPERATION.
C
  400 CONTINUE
      IG(1) = IG(1) + 1
      IF (IG(1).GT.1) CALL STER51(39,1)
C
C  SET POINTER FOR START OF SAR PARM INFO AND REFILL POSITION IN
C  WORK ARRAY WITH THIS VALUE.
C
      PSARL = IUSEW + 1.01
      LOCX = NGOFF + 1
      CALL RFIL51(WK,LOCX,PSARL)
C
      CALL SARP51(WK,IUSEW,LEFTW,NPSAR)
C
C  NOW REFILL THE WORD IN WORK WITH THE NUMBER OF WORDS NEEDED TO STORE
C  THE SAR PARM INFO.
C
      PSARN = NPSAR + 0.01
      LOCX = NGOFF + 2
      CALL RFIL51(WK,LOCX,PSARN)
C
      GO TO 100
C
C-----------------------------------------------------------------------
C  TIME-SERIES INFORMATION EXPECTED NEXT. IF NOT FOUND, SIGNAL ERROR.
C  IF FOUND, CALL ROUTINE TO INPUT ALL REQUIRED AND OPTIONAL TIME-SERIES
C
  500 CONTINUE
      IG(2) = IG(2) + 1
      IF (IG(2).GT.1) CALL STER51(39,1)
C
C  SET POINTER FOR START OF TIME SERIES INFO AND REFILL POSITION IN WORK
C  WITH THIS VALUE
C
      TSARL = IUSEW + 1.01
      LOCX = NGOFF + 3
      CALL RFIL51(WK,LOCX,TSARL)
C
      CALL SART51(WK,IUSEW,LEFTW,NTSAR)
C
C  SET THE NUMBER OF WORDS NEEDED TO STORE THE TIME SERIES INFO
C
      TSARN = NTSAR + 0.01
      LOCX = NGOFF + 4
      CALL RFIL51(WK,LOCX,TSARN)
      GO TO 100
C
C------------------------------------------------------------------
C  ' CARRYOVER' EXPECTED HERE. IF NOT FOUND, SIGNAL ERROR. IF FOUND
C  CALL ROUTINE TO GET CARRYOVER VALUES FOR THE OPERATION
C
  600 CONTINUE
      IG(3) = IG(3) + 1
C
      IF (IG(1).GT.0) GO TO 605
      WRITE(IPR,606)
606   FORMAT(/10X,'**ERROR** IN SAR51 NO PARMS DEFINED SO CANNOT',
     $ ' PROCESS CARRYOVER. MAY SEE MANY INVALID',
     $ ' KEYWORD ERRORS.')
      CALL ERROR
      OKSAR =.FALSE.
      GO TO 100
C
605   CONTINUE
      IF (IG(3).GT.1) CALL STER51(39,1)
C
C  SET START OF CARRYOVER INFO IN WORK ARRAY
C
      CSARL = IUSEW + 1.01
      LOCX = NGOFF + 5
      CALL RFIL51(WK,LOCX,CSARL)
C
      CALL SARC51(WK,IUSEW,LEFTW,NCSAR)
C
C  RESET THE NUMBER OF WORDS TO STORE CARRYOVER
C
      CSARN = NCSAR + 0.01
      LOCX = NGOFF + 6
      CALL RFIL51(WK,LOCX,CSARN)
C
      GO TO 100
C
C--------------------------------------------------------------------
C  RRCODE EXPECTED, IF NOT FOUND SINAL ERROR. IF FOUND, GO GET
C  REGULATION CODE INFORMATION NEEDED FOR OPERATION.
C
  700 CONTINUE
      IG(4) = IG(4) + 1
      IF (IG(4).GT.1) CALL STER51(39,1)
C
C  SET POINTER FOR START OF SAR REGULATION CODE INFO AND REFILL
C  POSITION IN WORK ARRAY WITH THIS VALUE.
C
      RSARL = IUSEW + 1.01
      LOCX = NGOFF + 7
      CALL RFIL51(WK,LOCX,RSARL)
C
      CALL SARP51(WK,IUSEW,LEFTW,NRSAR)
C
C  NOW REFILL THE WORD IN WORK WITH THE NUMBER OF WORDS NEEDED TO STORE
C  THE SAR REGULATION CODE INFO.
C
      RSARN = NRSAR + 0.01
      LOCX = NGOFF + 8
      CALL RFIL51(WK,LOCX,RSARN)
C
      GO TO 100
C
C--------------------------------------------------------------
C  SUMMARY
C---------------------------------------------------------------
C  PM, TS, CO ARE REQUIRED.
C  RR IS OPTIONAL, DEFAULT TO FREEFLOW IF NOT USED.
C
C
  900 CONTINUE
      DO 910 I = 1,3
          IERN = 34 + I
          IF (IG(I).EQ.0) CALL STER51(IERN,1)
  910 CONTINUE
 1000 CONTINUE
      IF (NUMERR.EQ.0) GO TO 9999
      OKSAR  = .FALSE.
C
C  POSITION TO START OF SAR SECTION INPUT AND PRINT INPUT WITH
C  LINE NUMBERS.
C
        WRITE(IPR,771)
 771    FORMAT(1H1,' *** SAR SECTION INPUT FOR THIS OPERATION ',
     .  'DEFINITION ***'//)
C
      IGSTRT = LSAR +1
      CALL POSN26(MUNI51,IGSTRT)
C
      NSAR2 = NSAR  - 1
      DO 30 I=1,NSAR2
      READ(MUNI51,891) LINE
      WRITE(IPR,892) I,LINE
   30 CONTINUE
C
  891 FORMAT(20A4)
  892 FORMAT(1H ,'(',I3,')',1X,20A4)
      CALL EROT51
      GO TO 9999
C
C  ERROR FOUND IN READING THE NEXT FIELD, SET ERROR AND GET NEXT CARD
C
 9000 CONTINUE
      IF (IERF.EQ.1) CALL STER51(19,1)
      IF (IERF.EQ.2) CALL STER51(20,1)
      IF (IERF.EQ.3) CALL STER51(21,1)
      IF (IERF.EQ.4) CALL STER51(1,1)
      IF (IERF.NE.3) GO TO 100
      USEDUP = .TRUE.
      GO TO 1000
C
 9999 CONTINUE
      RETURN
      END
