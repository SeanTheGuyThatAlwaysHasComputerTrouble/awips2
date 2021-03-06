C MEMBER PUC21
C  (from old member FCPUC21)
C
C DESC -- THE FUNCTION OF THIS SUBROUTINE IS TO PUNCH OUT THE INPUT
C DESC    DATA FOR THE DWOPER OPERATION.
C                             LAST UPDATE: 02/15/94.08:23:48 BY $WC30JL
C
C
C @PROCESS LVL(77)
C
      SUBROUTINE PUC21(PO,CO,KD,KU,NB,NCM,NCSSS,NCSS1,NJUN,NNYQ,NQL,
     1 NRCM1,NRT1,NT,NWJ,NUMLAD,NWJX,LAD,LQ,NSTR,NST,NDIV,LDIV,KTYP,
     2 IWTI,NQSL)
C
C           THIS SUBROUTINE WAS WRITTEN BY:
C           JANICE LEWIS      HRL   NOVEMBER,1982     VERSION NO. 1
C        MODIFIED BY JANICE LEWIS  HRL  JUNE 1998 VERSION NO. 3
C           TO ADD OPTION TO ADD U/S CONROL PT FOR L&D
C
      COMMON/IONUM/IN,IPR,IPU
      COMMON/FDBUG/IODBUG,ITRACE,IDBALL,NDEBUG,IDEBUG(20)
C
      DIMENSION PO(*),CO(*),KU(*),KD(*),NB(*),NCM(*),NCSSS(*),LDIV(*)
      DIMENSION NJUN(*),NNYQ(*),NQL(*),NRCM1(*),NRT1(*),NT(*),NDIV(*)
      DIMENSION NCSS1(*),NWJ(*),NUMLAD(*),NWJX(*),LAD(*),LQ(*),NSTR(*)
      DIMENSION NST(*),KTYP(*),IWTI(*),NQSL(*),SNAME(2)
C
       integer itemp, itemp2
C    ================================= RCS keyword statements ==========
      CHARACTER*68     RCSKW1,RCSKW2
      DATA             RCSKW1,RCSKW2 /                                 '
     .$Source: /fs/hseb/ob72/rfc/ofs/src/fcinit_puc/RCS/puc21.f,v $
     . $',                                                             '
     .$Id: puc21.f,v 1.6 2002/02/11 14:05:00 michaelo Exp $
     . $' /
C    ===================================================================
C
C
      DATA SNAME/4HPUC2,4H1   /
C
C
      CALL FPRBUG(SNAME,1,21,IBUG)
C
      JN=PO(25)
      NBMAX=PO(102)
      IVER=PO(1)
      WRITE(IPU,8000) JN,NBMAX
C
      KITPR=PO(101)
      WRITE(IPU,8005) PO(22),PO(103),PO(40),KITPR
C
      WRITE(IPU,8010) PO(17),PO(15),PO(16),PO(39),PO(18),PO(45)
C
      NU=PO(35)
      NCT=PO(32)
      ICD=PO(21)
      NYQD=PO(36)
      IF(IVER.EQ.1) NYQD=1
      ITMAX=PO(24)
      NCML=PO(29)
      KTERM=PO(28)
      WRITE(IPU,8020) NU,NCT,ICD,NYQD,ITMAX,NCML,KTERM
C
      NCS  =PO(30)
      NCSS =PO(31)
      NP   =PO(33)
      KPL  =PO(27)
      KPL2 =PO(100)
      JNK  =PO(26)
      NPEND=PO(34)
      WRITE(IPU,8020) NCS,NCSS,NP,KPL,KPL2,JNK,NPEND
C
      LOAFT=PO(47)
      LOCOW=PO(14)
      LOVWD=PO(43)
      LOWGL=PO(44)
C
       itemp = nb(1)
       itemp2 = po(locow)
      WRITE(IPU,8025) itemp, itemp2,PO(LOVWD),PO(LOWGL)
      IF(JN.EQ.1) GO TO 70
      DO 60 J=2,JN
      WRITE(IPU,8030) NB(J),NJUN(J),PO(LOAFT+J-1),PO(LOCOW+J-1),
     . PO(LOVWD+J-1),PO(LOWGL+J-1)
   60 CONTINUE
C
   70 WRITE(IPU,8020) (KU(J),J=1,JN)
      WRITE(IPU,8020) (KD(J),J=1,JN)
      IF(PO(38).NE.0.) WRITE(IPU,8060) PO(38)
      WRITE(IPU,8020) (NRT1(J),J=1,JN)
      WRITE(IPU,8020) (NSTR(J),J=1,JN)
      WRITE(IPU,8020) (NQL(J),J=1,JN)
      WRITE(IPU,8020) (NDIV(J),J=1,JN)
      WRITE(IPU,8020) (NWJ(J),J=1,JN)
      DO 220 J=1,JN
      IF(NWJ(J).EQ.0) GO TO 220
      NW=NWJ(J)
      IF(J.GT.1) GO TO 212
      LOHWH=PO(52)-1
      LOWC =PO(68)-1
      LOTFL=PO(66)-1
      LOBBL=PO(48)-1
      LOHFL=PO(50)-1
      LOHML=PO(51)-1
C
  212 LK=LCAT21(1,J,NWJ)
      LKK=LK+NW-1
      WRITE(IPU,8020)(NWJX(K),K=LK,LKK)
      IF(J.GT.1) GO TO 220
      DO 215 K=1,NW
      WRITE(IPU,8040) PO(LOHWH+K),PO(LOWC+K),PO(LOTFL+K),PO(LOBBL+K),
     1 PO(LOHFL+K),PO(LOHML+K)
  215 CONTINUE
C
  220 CONTINUE
      WRITE(IPU,8020) (NUMLAD(J),J=1,JN)
      NLDM=0
      LOPTR=PO(78)-1
      LOCTW=PO(70)-1
      LOGZPL=PO(128)-1
      LK=1
      DO 230 J=1,JN
      NUM=NUMLAD(J)
      IF(NUM.EQ.0) GO TO 230
      DO 228 L=1,NUM
      IF(IVER.GE.3) THEN
        WRITE(IPU,8032) LAD(LK),PO(LOPTR+L),PO(LOCTW+L),PO(LOGZPL+L),
     .    LAD(LK+1)
      ELSE
        LK=LCAT21(L,J,NUMLAD)
        WRITE(IPU,8035) LAD(LK),PO(LOPTR+L),PO(LOCTW+L),PO(LOGZPL+L)
      ENDIF
      IF(LAD(LK).GT.0.AND.PO(LOCTW+L).GT.0.) NLDM=NLDM+1
      IF(IVER.GE.3) LK=LK+2
  228 CONTINUE
      LOPTR=LOPTR+NUM
      LOCTW=LOCTW+NUM
      LOGZPL=LOGZPL+NUM
  230 CONTINUE
C
      WRITE(IPU,8020) ( NNYQ(J),J=1,JN)
C
      WRITE(IPU,8020) (NRCM1(J),J=1,JN)
C
      DO 94 J=1,JN
      NRCM=NRCM1(J)
      LK=LCAT21(1,J,NRCM1)
      LKK=LK+NRCM-1
      WRITE(IPU,8020) (NCM(K),K=LK,LKK)
   94 CONTINUE
C
      LOYCM=PO(97)
      LOCM =PO(89)
C
      DO 240 J=1,JN
      NRCM=NRCM1(J)
      DO 238 K=1,NRCM
      LYCM=LOYCM+NCML-1
      WRITE(IPU,8090) (PO(L),L=LOYCM,LYCM)
      LOYCM=LYCM+1
  238 CONTINUE
  240 CONTINUE
      DO 250 J=1,JN
      NRCM=NRCM1(J)
      DO 248 K=1,NRCM
      LCM=LOCM+NCML-1
      WRITE(IPU,8070) (PO(L),L=LOCM,LCM)
      LOCM=LCM+1
  248 CONTINUE
  250 CONTINUE
C
      LOX  =PO(84)
      LOFKC=PO(71)
C
      DO 190 J=1,JN
      N=NB(J)
      LX=LOX+N-1
      LFKC=LOFKC+N-1
      WRITE(IPU,8040) (PO(K),K=LOX,LX)
      WRITE(IPU,8040) (PO(K),K=LOFKC,LFKC)
      LOX=LX+1
      LOFKC=LFKC+1
  190 CONTINUE
C
      LOBS=PO(87)
      LOHS=PO(90)
      LOAS=PO(85)
      LBS=LOBS+NCS-1
      LHS=LOHS+NCS-1
C
      DO 160 J=1,JN
      N=NB(J)
      DO 152 I=1,N
      WRITE(IPU,8040) (PO(K),K=LOBS,LBS)
      LOBS=LBS+1
      LBS=LOBS+NCS-1
  152 CONTINUE
      DO 154 I=1,N
      WRITE(IPU,8040) (PO(K),K=LOHS,LHS)
      LOHS=LHS+1
      LHS=LOHS+NCS-1
  154 CONTINUE
      WRITE(IPU,8040) (PO(LOAS+(K-1)*NCS),K=1,N)
      LOAS=LOAS+NCS*N
  160 CONTINUE
C
      IF(NCSS.EQ.0) GO TO 175
C
      WRITE(IPU,8020) (NCSS1(J),J=1,JN)
C
      LONSS=PO(75)
      DO 30 J=1,JN
      IF(NCSS1(J).EQ.0) GO TO 30
      NCSJ=NCSS1(J)
      LK=LCAT21(1,J,NCSS1)
      LKK=LK+NCSJ-1
      WRITE(IPU,8020) (NCSSS(K),K=LK,LKK)
   30 CONTINUE
C
      LOBSS=PO(88)
      LOHSS=PO(91)
      LOASS=PO(86)
      LBSS=LOBSS+NCSS-1
      LHSS=LOHSS+NCSS-1
C
      DO 170 J=1,JN
      N=NB(J)
      NCSJ=NCSS1(J)
      IF(NCSJ.EQ.0) GO TO 170
      L=1
      DO 168 I=1,N
  162 IF(I-NCSSS(LCAT21(L,J,NCSS1))) 168,166,164
  164 IF(L.GE.NCSJ) GO TO 168
      L=L+1
      GO TO 162
  166 WRITE(IPU,8040) (PO(K),K=LOBSS,LBSS)
      LOBSS=LBSS+1
      LBSS=LBSS+NCSS
      WRITE(IPU,8040) (PO(K),K=LOHSS,LHSS)
      LOHSS=LHSS+1
      LHSS=LHSS+NCSS
      WRITE(IPU,8040) PO(LOASS)
      LOASS=LOASS+NCSS
  168 CONTINUE
  170 CONTINUE
C
  175 LOFD=PO(65)-1
      LOGZ=PO(72)-1
      LOSTT=PO(96)
C
      DO 118 J=1,JN
      LONT=LCAT21(1,J,NRT1)-1
      NRT=NRT1(J)
      IF(NRT.EQ.0) GO TO 118
      DO 116 K=1,NRT
      LSTT=LOSTT+2
      IF(KPL.EQ.1.OR.KPL.EQ.3) WRITE(IPU,8050) (PO(L),L=LOSTT,LSTT),
     1 NT(LONT+K),PO(LOFD+K),PO(LOGZ+K)
      IF(KPL.EQ.2) WRITE(IPU,8050) (PO(L),L=LOSTT,LSTT),NT(LONT+K),
     1 PO(LOFD+K)
      LOSTT=LOSTT+3
  116 CONTINUE
C
      LOFD=LOFD+NRT
      LOGZ=LOGZ+NRT
  118 CONTINUE
C
      LOSTR=PO(105)
      LOGZO=PO(120)-1
C
      DO 125 J=1,JN
      LONST=LCAT21(1,J,NSTR)-1
      NSR=NSTR(J)
      IF(NSR.EQ.0) GO TO 125
      DO 124 K=1,NSR
      LSTR=LOSTR+2
      IF(KTYP(LONST+K).EQ.1) WRITE(IPU,8050) (PO(L),L=LOSTR,LSTR),
     * NST(LONST+K),PO(LOGZO+K)
      IF(KTYP(LONST+K).GT.1) WRITE(IPU,8050) (PO(L),L=LOSTR,LSTR),
     * NST(LONST+K)
      LOSTR=LSTR+1
  124 CONTINUE
      LOGZO=LOGZO+NSR
  125 CONTINUE
C
      LOQL=PO(95)
      DO 210 J=1,JN
      IF(NQL(J)) 210,210,202
  202 NQL1=NQL(J)
      LK=LCAT21(1,J,NQL)-1
      DO 208 I=1,NQL1
      LQL=LOQL+2
      WRITE(IPU,8050) (PO(K),K=LOQL,LQL),LQ(LK+I)
      LOQL=LQL+1
  208 CONTINUE
  210 CONTINUE
C
      LODIV=PO(111)
      DO 219 J=1,JN
      IF(NDIV(J)) 219,219,213
  213 NDV1=NDIV(J)
      LK=LCAT21(1,J,NDIV)-1
      DO 218 I=1,NDV1
      LDV=LODIV+2
      WRITE(IPU,8050) (PO(K),K=LODIV,LDV),LDIV(LK+I)
      LODIV=LDV+1
  218 CONTINUE
  219 CONTINUE
C
      LOPLT=PO(94)
      LOIWT=PO(92)
      LOCTW=PO(70)-1
C
      LK=1
      LK1=1
      DO 305 J=1,JN
      IF(NUMLAD(J).EQ.0) GO TO 305
      NUM=NUMLAD(J)
C
      DO 306 K=1,NUM
      IF(IVER.LT.3) LK=LCAT21(K,J,NUMLAD)
      IF(LAD(LK).LT.0) GO TO 304
      IF(PO(K+LOCTW).LT.0.) GO TO 304
      LPLT=LOPLT+2
      WRITE(IPU,8055) (PO(L),L=LOPLT,LPLT)
      LOPLT=LPLT+1
 304  IF(IVER.GE.3) LK=LK+2
 306  CONTINUE
C
      LK=LK1
      DO 310 K=1,NUM
      IF(IVER.LT.3) LK=LCAT21(K,J,NUMLAD)
      IF(LAD(LK).LT.0) GO TO 310
      IF(PO(K+LOCTW).LT.0.) GO TO 310
      LIWT=LOIWT+2
      WRITE(IPU,8050) (PO(L),L=LOIWT,LIWT)
      LOIWT=LIWT+1
  309 IF(IVER.GE.3) LK=LK+2  
  310 CONTINUE
      LOCTW=LOCTW+NUM
      IF(IVER.GE.3) LK1=LK1+NUM*2
  305 CONTINUE
C
      IF(NU.EQ.0) GO TO 130
C
      LOST1=PO(79)
      LOSTM=PO(114)
      LOGZ1=PO(49)
      DO 128 J=1,JN
      LST1=LOST1+2
      IF(KU(J).EQ.1) WRITE(IPU,8056) (PO(L),L=LOST1,LST1),PO(LOSTM),
     *PO(LOGZ1)
      IF(KU(J).EQ.2) WRITE(IPU,8056) (PO(L),L=LOST1,LST1),PO(LOSTM)
      IF(KU(J).EQ.1) LOGZ1=LOGZ1+1
      LOSTM=LOSTM+1
      LOST1=LOST1+3
  128 CONTINUE
C
  130 IF(KD(1)-2) 135,135,136
C
  135 LOSTN=PO(23)
      LSTN=LOSTN+2
      IF(KD(1).EQ.2) WRITE(IPU,8050) (PO(L),L=LOSTN,LSTN)
      IF(KD(1).LE.1) WRITE(IPU,8055) (PO(L),L=LOSTN,LSTN),PO(20)
      IF(KD(1).EQ.0) THEN
        LONOS=PO(138)
        LNOS=LONOS+3
        WRITE(IPU,8065) (PO(L),L=LONOS,LNOS)
        LOTID=PO(141)
        LTID=LOTID+2
        WRITE(IPU,8065) (PO(L),L=LOTID,LTID)
      ENDIF
C
  136 IF(KPL2.LE.1) GO TO 137
      LOSTE=PO(149)
      DO 134 J=1,JN
      NRT=NRT1(J)
      IF(NRT.EQ.NB(J)) NRT=NRT-1
      DO 132 I=1,NRT
      LSTE=LOSTE+2
      WRITE(IPU,8050) (PO(L),L=LOSTE,LSTE)
      LOSTE=LOSTE+3
 132  CONTINUE
      IF(J.EQ.1.AND.NRT.NE.NRT1(J)) LOSTE=LOSTE+3
 134  CONTINUE

 137  LORC=PO(69)
C
      IF(NYQD.EQ.0) GO TO 139
      DO 138 K=1,NYQD
      WRITE(IPU,8050) PO(LORC),PO(LORC+1)
      LORC=LORC+2
  138 CONTINUE
C
  139 IF(NU.EQ.0) WRITE(IPU,8040) PO(41),PO(37),PO(19),PO(46)
C

      IF(KPL2.LE.1) GO TO 180
      LSLIC=PO(144)-1
      LFRMS=PO(145)-1
      LFBIAS=PO(146)-1
      LRRMS=PO(147)-1
      LRBIAS=PO(148)-1
      NSLICE=PO(151)

      IF(KPL2.EQ.3) THEN
        WRITE(IPU,8000) NSLICE
        WRITE(IPU,8020) (NQSL(J),J=1,JN)
      ENDIF
      DO 1085 J=1,JN
        NRT=NRT1(J)
        LONT=LCAT21(NRT,J,NRT1)
        IF(NT(LONT).EQ.NB(J)) NRT=NRT-1
        DO 1080 I=1,NRT
          IF(KPL2.EQ.3) THEN
            IF(NQSL(J).EQ.0) WRITE(IPU,8040) (PO(LSLIC+K),K=1,NSLICE)
            IF(NQSL(J).EQ.1) WRITE(IPU,8075) (PO(LSLIC+K),K=1,NSLICE)
            IUSE=IUSE+NSLICE
          ENDIF
          WRITE(IPU,8070) (PO(LFRMS+K),K=1,NSLICE)
          WRITE(IPU,8070) (PO(LFBIAS+K),K=1,NSLICE)
          WRITE(IPU,8070) (PO(LRRMS+K),K=1,NSLICE)
          WRITE(IPU,8070) (PO(LRBIAS+K),K=1,NSLICE)
          LSLIC=LSLIC+NSLICE
          LFRMS=LFRMS+NSLICE
          LFBIAS=LFBIAS+NSLICE
          LRRMS=LRRMS+NSLICE
 1080   CONTINUE
 1085 CONTINUE

 180  KUSE=1
      DO 232 J=1,JN
      N=NB(J)
      LUSE=KUSE+N-1
      WRITE(IPU,8040) (CO(K),K=KUSE,LUSE)
      KUSE=LUSE+1
  232 CONTINUE
C
      KUSE=PO(121)
      DO 236 J=1,JN
      N=NB(J)
      LUSE=KUSE+N-1
      WRITE(IPU,8090) (CO(K),K=KUSE,LUSE)
      KUSE=LUSE+1
  236 CONTINUE
C
      NTQL=PO(12)
      IF(NTQL.EQ.0) GO TO 245
      KUSE=PO(122)
      DO 242 J=1,JN
      N=NQL(J)
      IF(N.EQ.0) GO TO 242
      LUSE=KUSE+N-1
      WRITE(IPU,8090) (CO(K),K=KUSE,LUSE)
      KUSE=LUSE+1
  242 CONTINUE
C
  245 NTDV=PO(113)
      IF(NTDV.EQ.0) GO TO 255
      KUSE=PO(123)
      DO 252 J=1,JN
      N=NDIV(J)
      IF(N.EQ.0) GO TO 252
      LUSE=KUSE+N-1
      WRITE(IPU,8090) (CO(K),K=KUSE,LUSE)
      KUSE=LUSE+1
  252 CONTINUE
C
  255 NTLD=PO(6)
      IF(NTLD.EQ.0) GO TO 270
      KUSE=PO(124)
      LCTW=PO(70)-1
      DO 260 J=1,JN
      N=NUMLAD(J)
      IF(N.EQ.0) GO TO 260
      I1=LCAT21(1,J,NUMLAD)-1
      LOCK=0
      DO 256 L=1,N
      IF(LAD(I1+L).GT.0.AND.PO(LCTW+L).GT.0) LOCK=LOCK+1
  256 CONTINUE
      LCTW=LCTW+N
      IF(LOCK.EQ.0) GO TO 260
      LUSE=KUSE+LOCK-1
      WRITE(IPU,8040) (CO(K),K=KUSE,LUSE)
      KUSE=LUSE+1
  260 CONTINUE
C
      IF(NLDM.EQ.0) GO TO  270
      LCTW=PO(70)-1
CC      LIWT=PO(125)-1
      LIWT=0
      DO 265 J=1,JN
      N=NUMLAD(J)
      IF(N.EQ.0) GO TO 265
      I1=LCAT21(1,J,NUMLAD)-1
      LOCK=0
      DO 262 L=1,N
      IF(LAD(I1+L).GT.0.AND.PO(LCTW+L).GT.0) LOCK=LOCK+1
  262 CONTINUE
      IF(LOCK.GT.0) WRITE(IPU,8020) (IWTI(I+LIWT),I=1,LOCK)
      LIWT=LIWT+LOCK
      LCTW=LCTW+N
  265 CONTINUE
  270 LODNE=PO(55)
      LDNE=LODNE+17
C
      WRITE(IPU,8080) (PO(K),K=LODNE,LDNE)
C
 8000 FORMAT(2I5)
 8005 FORMAT(2F10.0,F10.2,I10)
 8010 FORMAT(F10.4,5F10.2)
 8020 FORMAT(7I10)
 8025 FORMAT(2I10,20X,3F10.2)
 8030 FORMAT(2I10,5F10.2)
 8032 FORMAT(I10,3F10.2,I10)
 8035 FORMAT(I10,3F10.2)
 8040 FORMAT(7F10.2)
 8045 FORMAT(F10.2,I10)
 8050 FORMAT(2A4,2X,A4,1X,I5,2F10.2)
 8055 FORMAT(2A4,2X,A4,6X,2F10.2)
 8056 FORMAT(2A4,2X,A4,6X,F10.0,F10.2)
 8060 FORMAT(F10.6)
 8065 FORMAT(2A4,2X,A4,1X,A4)
 8070 FORMAT(7F10.4)
 8075 FORMAT(7F10.0)
 8080 FORMAT(18A4)
 8090 FORMAT(7F10.0)
C
      IF(ITRACE.EQ.1) WRITE(IODBUG,9000) SNAME
 9000 FORMAT(1H0,2H**,1X,2A4,8H EXITED.)
      RETURN
      END

