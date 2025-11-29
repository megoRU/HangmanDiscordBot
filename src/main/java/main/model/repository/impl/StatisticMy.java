package main.model.repository.impl;

import java.time.LocalDateTime;

public interface StatisticMy {

    LocalDateTime getGameDate();

    Integer getTOTAL_ZEROS();

    Integer getTOTAL_ONES();
}