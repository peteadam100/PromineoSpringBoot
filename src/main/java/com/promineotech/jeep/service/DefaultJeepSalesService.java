/**
 * 
 */
package com.promineotech.jeep.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.promineotech.jeep.entity.Jeep;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Adam
 *
 */

@Service
@Slf4j
public class DefaultJeepSalesService implements JeepSalesService {

  @Override
  public List<Jeep> fetchJeeps(String model, String trim) {
    log.info("The fetchJeeps method in service layer was called with model={} and trim={}", model, trim);
    return null;
  }

}
