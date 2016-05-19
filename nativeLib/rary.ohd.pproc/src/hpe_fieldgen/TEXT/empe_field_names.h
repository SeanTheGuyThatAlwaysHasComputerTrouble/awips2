/*******************************************************************************
* FILENAME:            mpe_field_names.h
* DESCRIPTION:         Contains the ColorUse enumeration which 
*                      lists all of the MPE products being generated.
*                      Also contains the prototype of the get_mpe_field_names
*                      routine.
*
* ORIGINAL AUTHOR:     Bryon Lawrence
* CREATION DATE:       November 15, 2004
* ORGANIZATION:        HSEB/OHD
* MACHINE:             Linux
* MODIFICATION HISTORY:
*   MODULE #        DATE         PROGRAMMER        DESCRIPTION/REASON
*          1        11/15/2004   Bryon Lawrence    Original Coding         
********************************************************************************
*/

#ifndef MPE_FIELD_NAMES_H
#define MPE_FIELD_NAMES_H

/* Note that there should be a one to one correspondence between the
   ColorUse enumeration and the DisplayFieldData enumeration.  The 
   DisplayFieldData enumeration will contain one extra element, 
   display_subValue, which must appear before the display_missing element
   at the end of the enumeration. */

typedef enum ColorUse
{  DHR_MOSAIC_USE,
   BDHR_MOSAIC_USE,
   RADAR_MOSAIC_USE,
   AVG_RADAR_MOSAIC_USE,
   MAX_RADAR_MOSAIC_USE,
   FIELD_BIAS_MOSAIC_USE,
   LOCAL_BIAS_MOSAIC_USE,
   GAGE_ONLY_ANALYSIS,
   SATELLITE_PRECIP,
   LOCAL_BIAS_SATELLITE_PRECIP,
   MULTI_SENSOR_MOSAIC,
   LOCAL_BIAS_MULTI_MOSAIC,
   P3_LOCAL_BIAS_MOSAIC,
   BEST_ESTIMATE_QPE,
   MULTI_HOUR_QPE,
   LOCAL_SPAN,
   LOCAL_BIAS,
   HEIGHT_FIELD,
   RADAR_COVERAGE_FIELD,
   PRISM,
   MAX_TEMP_PRISM,
   MIN_TEMP_PRISM,
   RFC_QPE_MOSAIC,
   NUM_COLORUSE_ITEMS
} ColorUse ;

enum DisplayFieldData { display_dhrMosaic, 
                        display_bdhrMosaic,
                        display_erMosaic,
                        display_avgerMosaic,
                        display_maxerMosaic,
                        display_ebMosaic, 
                        display_lMosaic,
                        display_gageOnly, 
                        display_satPrecip,
                        display_lsatPrecip, 
                        display_mMosaic, 
                        display_mlMosaic, 
                        display_p3Mosaic,
                        display_Xmrg,
                        display_multiHour, 
                        display_Locspan,
                        display_Locbias, 
                        display_Height, 
                        display_Index, 
                        display_Prism, 
                        display_maxtempPrism,
                        display_mintempPrism,
                        display_rfcMosaic,
                        display_subValue,
                        display_missing } ;

const char ** get_mpe_field_names ( ) ;

#endif /*ifndef MPE_FIELD_NAMES_H */
