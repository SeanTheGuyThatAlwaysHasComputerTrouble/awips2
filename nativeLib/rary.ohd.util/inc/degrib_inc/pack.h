#ifndef PACK_H
#define PACK_H

#include "degrib_inc/meta.h"
#include "degrib_inc/degrib2.h"

int WriteGrib2Record (grib_MetaData *meta, double *Grib_Data,
                      long int grib_DataLen, IS_dataType *is, sChar f_unit,
                      char ** cPack, long int *c_len, uChar f_stdout);

#endif
