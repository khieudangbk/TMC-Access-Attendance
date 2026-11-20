package com.tmc.aacs.central.station;

import java.util.List;

import com.tmc.aacs.central.station.adapter.in.web.StationController;
import com.tmc.aacs.central.station.adapter.in.web.StationExceptionHandler;
import com.tmc.aacs.central.station.application.port.in.StationQuery;
import com.tmc.aacs.central.station.domain.Direction;
import com.tmc.aacs.central.station.domain.GateStation;
import com.tmc.aacs.central.station.domain.StationNotFoundException;
import com.tmc.aacs.central.station.domain.StationStatus;
import com.tmc.aacs.central.shared.web.RequestIdFilter;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StationControllerTest {

    private static final GateStation STATION = new GateStation(
            "BD-IN-01",
            "TMC-BD",
            "Nhà máy Bình Dương",
            "Cổng vào 1",
            Direction.IN,
            "BD-TS-01",
            "BD-KSK-01",
            StationStatus.ACTIVE,
            1);

    private final StationQuery stationQuery = mock(StationQuery.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new StationController(stationQuery))
            .setControllerAdvice(new StationExceptionHandler())
            .addFilters(new RequestIdFilter())
            .build();

    @Test
    void listsStations() throws Exception {
        when(stationQuery.findAll()).thenReturn(List.of(STATION));

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].stationId").value("BD-IN-01"))
                .andExpect(jsonPath("$.items[0].direction").value("IN"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void returnsStationById() throws Exception {
        when(stationQuery.getById("BD-IN-01")).thenReturn(STATION);

        mockMvc.perform(get("/api/v1/stations/BD-IN-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factoryName").value("Nhà máy Bình Dương"))
                .andExpect(jsonPath("$.gateId").value("BD-TS-01"));
    }

    @Test
    void returns404ForUnknownStation() throws Exception {
        when(stationQuery.getById("UNKNOWN")).thenThrow(new StationNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/v1/stations/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STATION_NOT_FOUND"));
    }
}
