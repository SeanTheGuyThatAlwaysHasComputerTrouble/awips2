/**
 * 
 */
package gov.noaa.nws.ncep.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.measure.unit.Unit;

import com.raytheon.uf.common.dataplugin.IDecoderGettable.Amount;

/**
 * @author archana
 *
 */
 public class UncondProbabilityOfTstorms06Hour extends AbstractMetParameter implements
		Dimensionless {

	 public UncondProbabilityOfTstorms06Hour() {
		 super( UNIT );
	}
	 
 }