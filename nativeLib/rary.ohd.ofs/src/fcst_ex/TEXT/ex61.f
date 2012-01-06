C
C                             LAST UPDATE: 6/08/1998 TLS
C
C @PROCESS LVL(77)
C
      SUBROUTINE EX61 (P,HO,HF,RANGE,SBALR1,SBALR2,SBALR3,SBALR4)

C     THIS IS THE EXECUTION ROUTINE FOR THE STAGE REVIEW OPERATION

C     THIS ROUTINE WAS INITIALLY WRITTEN BY DAVE GAREN AND RICK KOEHLER
C     UPDATED AND ADAPTED TO RFS/OFS ENVIRONMENT BY
C                 JOANNE R.SALERNO, NWRFC  -  JAN 1988

C        1         2         3         4         5         6         7
C23456789012345678901234567890123456789012345678901234567890123456789012

C     POSITION     CONTENTS OF P ARRAY
C      1           VERSION NUMBER OF OPERATION
C      2-19        GENERAL NAME OR TITLE
C  INPUT
C     20           TIME INTERVAL FOR OBS/FCST        - ITIME
C     21-23        OBSERVED    STAGE                 - HO (STG)
C     24-26        DW OBS/FCST STAGE                 - HF (STGE)
C  OUTPUT
C     27           TIME INTERVAL FOR OUTPUT          - OTIME
C     TIDAL RANGE/SLICE DEFINITION
C     28-29        RANGE LIMIT SERIES IDENTIFIER     -  RANGE
C     30           RANGE LIMIT & BALANCE TYPE CODE   -  OUTTC
C                  RANGE(1) R1 LOWER LIMIT
C                  RANGE(2) R2 LOWER LIMIT
C                  RANGE(3) R3 LOWER LIMIT
C                  RANGE(4) R4 LOWER LIMIT
C                  RANGE(5) R4 UPPER LIMIT
C
C     OBSERVED PRTO3 AVERAGE  BALANCE TIME SERIES PER RANGE
C     31-32        RANGE1 AVE TIDE BALANCE TIME SERIES ID  -  SBALR1
C     33-34        RANGE2 AVE TIDE BALANCE TIME SERIES ID  -  SBALR2
C     35-36        RANGE3 AVE TIDE BALANCE TIME SERIES ID  -  SBALR3
C     37-38        RANGE4 AVE TIDE BALANCE TIME SERIES ID  -  SBALR4
C
C     39-40        RESERVED

C**********************************************************************

C     THE NUMBER OF ELEMENTS REQUIRED IN THE P ARRAY IS  40

C     POSITION     CONTENTS OF C ARRAY

C     THE NUMBER OF ELEMENTS REQUIRED IN THE C ARRAY IS   0

C        1         2         3         4         5         6         7
C23456789012345678901234567890123456789012345678901234567890123456789012

C       FLOW CHART FOR DWOPER ASTORIA PREPROCESSOR


C       1.      READ IN TIME SERIES

C               HO(K)   -       OBSERVED STAGE (STG)

C               HF(K)   -       DW SIMULATED OBS/FCST STAGE (STGE)

C       2.      IDENTIFY FIRST AND LAST HOURLY TIME SERIES
C               IDENTIFY LAST OBSERVED  HOURLY TIME SERIES
C                ITS            LOTS              LTS
C                  |______________|________________|
C                STARTRUN       T=0              ENDRUN


C       3.      FILL MISSING OBSERVED W/-5.99 . IT IS UNLIKELY THIS STAGE
C               WILL OCCUR AT ANY STATION

C       4.      COMPUTE TABLE OF AVERAGE DIFFERENCES BETWEEN OBSERVED
C               AND COMPUTED STAGES FOR DIFFERENT STAGE RANGES AND DAYS
C                    - DETERMINE STAGE RANGE
C                       - FIND MAX AND MIN COMPUTED STAGE FOR BACKUP PERIOD
C                       - ROUND TO NEAREST TENTH OF A FOOT
C                       - DIVIDE INTO 4 STAGE RANGES
C                    - SEARCH COMPUTED STAGES AND DETERMINE WHICH ONES FALL
C                      INTO WHICH STAGE RANGES FOR EACH DAY OF BACKUP PERIOD
C                    - THROW OUT VALUES THAT ARE MORE THAN TEN FEET DIFFERENT FROM COMPUTED
C                    - COMPUTE AVERAGE BALANCES
C       5.      APPLY BIAS MATRIX (COMMENTED OUT FOR NOW)

C       6.      WRITE AVERAGE RANGE BALANCES AND RANGE/SLICE BOUNDARIES TO DATABASE

C        1         2         3         4         5         6         7
C23456789012345678901234567890123456789012345678901234567890123456789012

C     VARIABLE DEFINITIONS
C
C     P(*)        - PARAMETER ARRAY                         INPUT
C     HO(*)       - OBSERVED    TIME SERIES                 INPUT
C     HF(*)       - DW OBS/FCST TIME SERIES                 INPUT
C     OTS(*)      - TIME MARKER                             INTERNAL
C       KKHR(*)    -       TIDE HOUR
C       KKDAY(*)   -       TIDE DAY
C       KKMO(*)    -       TIDE MONTH
C     BAL(*)       - (OBSERVED - PREDICTED)
C     SBAL(*)      - BALANCE ARRAY SEQUENCE FOR RUN PERIOD  OUTPUT
C     IPRTY(*)     - THE ORDER OR PRIORITY OF SBAL ELEMENTS INTERNAL
C                    USED TO ESTIMATE MISSING VALUES
C     ESBAL(*)     - ESTIMATED BALANCE                      INTERNAL
C     RANGE(*)     - TIDE RANGES/SLICES                     OUTPUT

      DIMENSION P(*),HO(*),HF(*),RANGE(*),SBALR1(*),SBALR2(*),
     +          SBALR3(*),SBALR4(*)
      DIMENSION OTS(720)
      COMMON /KTIME/ KKYR(720),KKMO(720),KKDAY(720),KKHR(720),
     ?               KMSAVE(30),KDSAVE(30)

      DATA E/1hE/


C        1         2         3         4         5         6         7
C23456789012345678901234567890123456789012345678901234567890123456789012

C     COMMON BLOCKS

C     DEBUG COMMON
C        IODBUG - UNIT NUMBER TO WRITE OUT ALL DEBUG OUTPUT

       COMMON /FDBUG/IODBUG,ITRACE,IDBALL,NDEBUG,IDEBUG(20)

C     UNIT NUMBERS COMMON
C     ALWAYS USE THE VARIABLES IN IONUM TO SPECIFY UNIT NUMBER

      COMMON /IONUM/IN,IPR,IPU

C     TIMING INFORMATION COMMON

      COMMON /FCTIME/IDARUN,IHRRUN,LDARUN,LHRRUN,LDACPD,LHRCPD,NOW(5),
     +               LOCAL,NOUTZ,NOUTDS,NLSTZ,IDA,IHR,LDA,LHR,IDADAT

C     IDARUN - I* 4 - INITIAL JULIAN DAY OF THE ENTIRE RUN
C     IHRRUN - I* 4 - INITIAL HOUR OF THE ENTIRE RUN
C     LDARUN - I* 4 - JULIAN DAY OF LAST DAY OF THE ENTIRE RUN
C     LHRRUN - I* 4 - LAST HOUR OF ENTIRE RUN
C     LDACPD - I* 4 - JULIAN DAY OF LAST DAY WITH OBSERVED DATA
C     LHRCPD - I* 4 - LAST HOUR WITH OBSERVED DATA
C     NOW    - I* 4 - CURRENT TIME FROM THE COMPUTER'S CLOCK
C                     NOW(1) - MONTH
C                     NOW(2) - DAY
C                     NOW(3) - YEAR (4 DIGIT)
C     LOCAL  - I* 4 - HOUR OFFSET TO LOCAL TIME
C     NOUTZ  - I* 4 - DEFAULT TIME ZONE NUMBER FOR OUTPUT
C     NOUTDS - I* 4 - DEFAULT DAYLIGHT SAVING TIME SWITCH FOR OUTPUT
C                     =0, STANDARD TIME
C                     =1, DAYLIGHT SAVING TIME
C     NLSTZ  - I* 4 - TIME ZONE NUMBER OF LOCAL STANDARD TIME
C     IDA    - I* 4 - JULIAN DATE OF THE FIRST DAY TO BE COMPUTED
C     IHR    - I* 4 - FIRST HOUR TO BE COMPUTED IN THE CURRENT PASS
C     LDA    - I* 4 - JULIAN DATE OF THE LAST DAY TO BE COMPUTED
C     LHR    - I* 4 - LAST HOUR TO BE COMPUTED IN THE CURRENT PASS
C     IDADAT - I* 4 - JULIAN DATE OF THE FIRST DAY OF TIME SERIES DATA

C     CONTROL INFORMATION FOR SAVING CARRYOVER

C     COMMON /FCARY/IFILLC,NCSTOR,ICDAY(20),ICHOUR(20)

C     IFILLC - I* 4 - CONTROLS UPDATE OF C ARRAY AND STORING CARRYOVER
C                     =0, NO OPERATION CAN MODIFY THE C ARRAY; NO
C                         CARRYOVER STORED
C                     =1, C ARRAY SHOULD BE MODIFIED
C     NCSTOR - I* 4 - NUMBER OF CARRYOVER DATES SAVED TO BE SAVED
C                     IGNORED IF IFILLC=0
C     ICDAY  - I* 4 - JULIAN DAYS TO STORE CARRYOVER
C     ICHOUR - I* 4 - HOURS TO STORE CARRYOVER

C    ================================= RCS keyword statements ==========
      CHARACTER*68     RCSKW1,RCSKW2
      DATA             RCSKW1,RCSKW2 /                                 '
     .$Source: /fs/hseb/ob72/rfc/ofs/src/fcst_ex/RCS/ex61.f,v $
     . $',                                                             '
     .$Id: ex61.f,v 1.2 1998/10/14 13:45:55 page Exp $
     . $' /
C    ===================================================================

C     CHECK THE TRACE LEVEL AND WHETHER DEBUG OUTPUT IS NEEDED
      CALL FPRBUG ('EX61    ',1,61,IBUG)

C        1         2         3         4         5         6         7
C23456789012345678901234567890123456789012345678901234567890123456789012

C  --- LOCATE FIRST DATA VALUE IN THE 'HOURLY' TIME SERIES DATA ARRAYS
C
      IBUG = 0
      KDA = IDA
      KHR = IHR
      ITS = (KDA-IDADAT) * 24 + KHR


C  PRINT P ARRAY

      IF (IBUG.EQ.1) WRITE(IODBUG,1001) (P(J),J=20,38)

 1001 FORMAT(' EX61: P20,21-26,27,28-38: ',I2,6A8,1X,I2,3A8,8A8)

C  DEBUG OUTPUT FOR FIRST DATA VALUE LOCATION

      IF (IBUG.EQ.1) WRITE(IODBUG,1002) IDARUN,IHRRUN,LDARUN,LHRRUN,
     +                           LDACPD,LHRCPD,NOW(1),NOW(2),NOW(3),
     +                           NOW(4),NOW(5),LOCAL,NOUTZ,NOUTDS,
     +                           NLSTZ,IDA,IHR,LDA,LHR,IDADAT
 1002 FORMAT(' EX61: FCTIME CONTENTS: ',4(5I8/))

C  --- LOCATE LAST DATA VALUE IN THE 'HOURLY' TIME SERIES DATA ARRAYS
C
      LTS = (LDA-(IDADAT-1)) * 24 + KHR - 1


C  --- LOCATE LATEST OBS  VALUE IN 'HOURLY' TIME SERIES DATA
C
      LOTS = (LDACPD-IDADAT) * 24 + LHR

C  DEBUG OUTPUT FOR LAST DATA VALUE LOCATION

      IF (IBUG.EQ.1) WRITE(IODBUG,1003) ITS,LOTS,LTS
 1003 FORMAT(' EX61: ITS,LOTS,LTS: ',3I8)

C        1         2         3         4         5         6         7
C23456789012345678901234567890123456789012345678901234567890123456789012

C  --- DETERMINE HR, DAY, MONTH AND TIME MARKER FOR TIME SERIES
C
        IX=0
      DO 100 I=ITS,LTS+24
        OTS(I)=I
        CALL MDYH1(KDA,KHR,ILM,ILD,ILY,ILH,NOUTZ,NOUTDS,NTZCD)
      IF (IBUG.EQ.1) WRITE(IODBUG,1111) KDA,KHR,ILM,ILD,ILY,
     +    ILH,NOUTZ,NOUTDS,NTZCD
 1111 FORMAT(' EX61: KDA,KHR,ILM,ILD,ILY,ILH,NOUTZ,NOUTDS,NTZCD: ',8I8,
     +               A6)

        IF (KHR.EQ.1) THEN
          IX=IX+1
          KDSAVE(IX)=ILD
          KMSAVE(IX)=ILM
        ENDIF
        KHR=KHR+1
        IF (KHR.GT.24) THEN
          KHR=1
          KDA=KDA+1
        ENDIF
        KKHR(I)=ILH
        KKMO(I)=ILM
        KKDAY(I)=ILD
      IF (IBUG.EQ.1) WRITE(IODBUG,1004) KHR,IX,KDSAVE(IX),KMSAVE(IX)
 1004 FORMAT(' EX61: KHR,IX,KDSAVE,KMSAVE: ',4I8)
  100 CONTINUE

C
C  --- FIND BALANCES IN FUTURE
C

      CALL SBAL61 (P,HO,HF,RANGE,SBALR1,SBALR2,SBALR3,SBALR4,IDA,
     +             IHR,IDADAT,ITS,LOTS,LTS)
      RETURN
      END
