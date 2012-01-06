C   MODULE TESTP
C
      SUBROUTINE TESTP(IARY,MIARY,NTOTM,MSNG,IER)
C
C   THIS SUBROUTINE ESTIMATES THE PREVIOUS 12Z VALUE AT THE START OF
C   A RUN.
C
C   THIS SUBROUTINE WAS ORIGINALLY WRITTEN BY GERALD N. DAY .
C
      INTEGER*2 IARY,MAXT,MINT,INSTT,ITX1,ITI1,ITI,MSNG
C
      LOGICAL*1 KODE,O,E
      LOGICAL LBUG
C
      DIMENSION PTI(3,4),WTI(3,4),DUM(2),SBNAME(2),OLDOPN(2)
      DIMENSION IARY(1),INSTT(4),KODE(10)
C
      INCLUDE 'common/tscrat'
      INCLUDE 'common/pudbug'
      INCLUDE 'common/tloc'
      INCLUDE 'common/tary'
      INCLUDE 'common/where'
      INCLUDE 'common/ionum'
C
      EQUIVALENCE (ARY(42),PTI(1,1)),(ARY(54),WTI(1,1))
C
C    ================================= RCS keyword statements ==========
      CHARACTER*68     RCSKW1,RCSKW2
      DATA             RCSKW1,RCSKW2 /                                 '
     .$Source: /fs/hseb/ob72/rfc/ofs/src/fcst_mat/RCS/testp.f,v $
     . $',                                                             '
     .$Id: testp.f,v 1.1 1995/09/17 19:03:08 dws Exp $
     . $' /
C    ===================================================================
C
C
      DATA TEMP/4HTEMP/,BLANK/4H    /,O/1H /,E/1HE/
      DATA SBNAME/4HTEST,4HP   /,DCODE/4HESTP/
C
      IF(IPTRCE.GE.1) WRITE(IOPDBG,900)
  900 FORMAT(1H0,17H** TESTP ENTERED.)
C
      LBUG=.FALSE.
      IF(IPBUG(DCODE).EQ.1) LBUG=.TRUE.
C
      IOLDOP=IOPNUM
      IOPNUM=-1
      DO 5 I=1,2
      OLDOPN(I)=OPNAME(I)
    5 OPNAME(I)=SBNAME(I)
C
C
      IER=0
C
      IF(.NOT.LBUG) GO TO 10
      WRITE(IOPDBG,910)
  910 FORMAT(1H0,26HBEFORE TESTP, IARY AND ARY)
      WRITE(IOPDBG,920) (IARY(I),I=1,LAST)
  920 FORMAT(5X,20I6)
      WRITE(IOPDBG,930) (ARY(I),I=ISTDAT,IUSE)
  930 FORMAT(5X,10F10.2)
   10 CONTINUE
C
      KODE(1)=O
      KODE(2)=O
C
      LOC=LOCMP
C
C   LOOP THROUGH MAX/MIN POINTER ARRAY . GET MAX/MIN . CHECK 12Z VALUE
C   FOR INST. STATIONS. CALL UPDATE ROUTINE.
C
      DO 800 I=1,NTOTM
      IF(IARY(LOC).EQ.0) GO TO 790
      LMD=I*2-1
      LMEAN=IARY(LOC+1)
      MEANMX=IARY(LMEAN-1+LOCMMX)
      MEANMN=IARY(LMEAN-1+LOCMMN)
C
      MAXT=IARY(LOCMD+LMD-1)
      MINT=IARY(LOCMD+LMD)
C
C   CHECK IF MAX/MIN ARE MISSING. IF TOMORROW'S VALUES
C   ARE MISSING THEY WILL BE NEEDED FOR BLEND. THIS IS
C   RARE, BUT SET MISSING VALUES TO THE MONTHLY MEAN
C   JUST IN CASE.
C
      IF(MAXT.GT.MSNG) GO TO 12
      MAXT=MEANMX
      KODE(1)=E
   12 IF(MINT.GT.MSNG) GO TO 14
      MINT=MEANMN
      KODE(2)=E
C
   14 IF(IARY(LOC+2).GT.0) GO TO 15
C
      NINST=0
      GO TO 500
C
C   STATION HAS INST. DATA
C
   15 LIP=IARY(LOC+2)
C
C   THE TIME INTERVAL IS SIX FOR THE PREVIOUS DAY.
C
      NINST=4
C
      LID=IARY(LOCIP+LIP+1)
C
C   SET KODE TO OBSERVED SYMBOL (BLANK)
C
      KODE(2+NINST)=O
C
C   CHECK IF 12Z VALUE WAS MISSING
C
      IF(IARY(LOCID+LID-2+NINST).GT.MSNG) GO TO 220
C
C   VALUE IS MISSING - MUST ESTIMATE
C
C   READ TEMPERATURE PARAMETERS
C
      IPREC=IARY(LOC)
      DUM(1)=BLANK
      DUM(2)=BLANK
      LIMIT=ISTDAT-1
      NPREC=-IPREC
      CALL RPPREC(DUM,TEMP,NPREC,LIMIT,ARY,NFILL,PTRNXT,ISTAT)
      IF(ISTAT.EQ.0) GO TO 20
C
      CALL PSTRDC(ISTAT,TEMP,DUM,IPREC,LIMIT,NFILL)
C
      IER=1
      GO TO 999
C
   20 IF(LBUG) CALL PDUMPA(NFILL,ARY,TEMP,DUM,1)
C
C   CHECK IF NETWORK HAS BEEN RUN FOR THIS STATION.
C
      INETWK=ARY(16)
      IF(INETWK.EQ.0) GO TO 790
C
      ITIME1=NINST-1
      L2=NINST-1
      DO 100 L=1,L2
      IF(IARY(LOCID+LID-2+ITIME1).GT.MSNG) GO TO 110
      ITIME1=ITIME1-1
  100 CONTINUE
C
C   VALUE CAN NOT BE ESTIMATED
C
      INSTT(NINST)=MSNG
      GO TO 500
C
  110 ITX1=IARY(LOCID+LID-2+ITIME1)
C
      SUMD=0.
      SUMN=0.
      DO 200 J=1,4
      DO 190 K=1,3
      LIPE=PTI(K,J)
      IF(LIPE.LE.0) GO TO 190
      LIDE=IARY(LOCIP+LIPE+1)
C
C   SEE IF EST. STA. HAS DATA AT T (NINST)
C
      IF(IARY(LOCID+LIDE-2+NINST).LE.MSNG) GO TO 190
      ITI=IARY(LOCID+LIDE-2+NINST)
C
C   SEE IF EST. STA. HAS DATA AT T (ITIME1)
C
      IF(IARY(LOCID+LIDE-2+ITIME1).LE.MSNG) GO TO 190
      ITI1=IARY(LOCID+LIDE-2+ITIME1)
C
C   ESTIMATOR FOUND
C
      W=WTI(K,J)
      Q=(ITI+ITX1-ITI1)*W
      SUMN=SUMN+Q
      SUMD=SUMD+W
      GO TO 200
  190 CONTINUE
  200 CONTINUE
C
      IF(SUMD.GT.5.0E-08) GO TO 210
C
C   NO ESTIMATOR FOUND
C
      INSTT(NINST)=MSNG
      GO TO 500
C
C   MISSING VALUE IS ESTIMATED
C
  210 INSTT(NINST)=SUMN/SUMD
      KODE(2+NINST)=E
      GO TO 500
C
C   VALUE IS AVAILABLE
C
  220 INSTT(NINST)=IARY(LOCID+LID-2+NINST)
C
  500 CALL TUPDAT(MAXT,MINT,INSTT,NINST,IARY,LOC,0,KODE,MSNG)
C
  790 LOC=LOC+5
C
      IF(LOC.LE.MIARY) GO TO 800
      WRITE(IPR,620) MIARY
  620 FORMAT(1H0,10X,45H**ERROR** ATTEMPT TO EXCEED THE LENGTH OF THE,
     1 11H IARY ARRAY,I6)
      CALL ERROR
      IER=1
      GO TO 999
C
  800 CONTINUE
C
      IF(.NOT.LBUG) GO TO 999
      WRITE(IOPDBG,940)
  940 FORMAT(1H0,25HAFTER TESTP, IARY AND ARY)
      WRITE(IOPDBG,920) (IARY(I),I=1,LAST)
      WRITE(IOPDBG,930) (ARY(I),I=ISTDAT,IUSE)
C
  999 CONTINUE
C
      IOPNUM=IOLDOP
      OPNAME(1)=OLDOPN(1)
      OPNAME(2)=OLDOPN(2)
      RETURN
      END
