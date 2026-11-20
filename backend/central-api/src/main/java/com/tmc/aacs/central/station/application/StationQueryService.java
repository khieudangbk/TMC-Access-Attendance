package com.tmc.aacs.central.station.application;

import java.util.List;

import com.tmc.aacs.central.station.application.port.in.StationQuery;
import com.tmc.aacs.central.station.application.port.out.StationRepository;
import com.tmc.aacs.central.station.domain.GateStation;
import com.tmc.aacs.central.station.domain.StationNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StationQueryService implements StationQuery {

    private final StationRepository stationRepository;

    public StationQueryService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    public List<GateStation> findAll() {
        return stationRepository.findAll();
    }

    @Override
    public GateStation getById(String stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new StationNotFoundException(stationId));
    }
}
