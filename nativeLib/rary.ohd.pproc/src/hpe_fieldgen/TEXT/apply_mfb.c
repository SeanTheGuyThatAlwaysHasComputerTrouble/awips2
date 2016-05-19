/*******************************************************************************
* FILENAME:            apply_mfb.c
*
* Purpose:
* This function is converted from FORTRAN code: apply_mfb.f.
* This function applies the mean field bias values
* to the RMOSAIC field to generate the BMOSAIC field.
*
* calling function: runEBMosaic
* functions called: none.
*
* input variables
*
* mfbias  - array of mean field bias values.
* rowSize - dimension of site in y direction.
* colSize - dimension of site in x direction.
* ID      - array showing which radar covers each grid point.
* RMosaic - raw radar mosaic.
*
* output variables
*
* BMosaic - mfb adjusted radar mosaic.
*
*
* MODIFICATION HISTORY:
*   DATE         PROGRAMMER        DESCRIPTION/REASON
*   April 2005   Guoxian Zhou      finish conversion to C Language 
*
*********************************************************************************/

#include "empe_fieldgen.h"

void apply_mfb(const double * mfbias ,
			const int rowSize ,
			const int colSize ,
			int ** ID ,
			double ** RMosaic ,
			double ** BMosaic)
{
	int i, j, k ;
	for(i = 0; i < rowSize; i ++)
	{
		for(j = 0; j < colSize; j ++)
		{
			k = ID[i][j] ;
			if(k > 0)
			{
				/*
				 * Add a check for RMosaic.
				 * Apply mean bias value to RMosaic to make BMosaic
				 * only when the RMosaic is not default missing value.
				 * Otherwise the BMosaic is default to missing value.
				 * Added by guoxian zhou May 2005
				 */

				if( RMosaic[i][j] >= 0.0 )
					BMosaic[i][j] = mfbias[k-1] * RMosaic[i][j] ;
			}
		}
	}
}
