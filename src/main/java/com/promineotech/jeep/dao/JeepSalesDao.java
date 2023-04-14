/**
 * 
 */
package com.promineotech.jeep.dao;

import java.util.List;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;

/**
 * @author Adam
 *
 */


public interface JeepSalesDao {

  /**
   * @param model
   * @param trim
   * @return
   */
  public List<Jeep> fetchJeeps(JeepModel model, String trim);

}
