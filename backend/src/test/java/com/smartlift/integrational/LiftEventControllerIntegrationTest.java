package com.smartlift.integrational;

import com.jayway.jsonpath.JsonPath;
import com.smartlift.dto.request.LiftEventRequest;
import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.model.enums.LiftEventType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LiftEventControllerIntegrationTest extends BaseIntegrationTest {

    private record OrgContext(String token, Long orgId, Integer liftId, Long userId) {}

    private OrgContext setupOrgWithLift(String prefix) throws Exception {
        UserResponse admin = registerOrganization(prefix + "_admin", prefix + "@test.com",
                "password123", prefix + "Org", "MANUFACTURER");
        String token = login(prefix + "_admin", "password123");

        LiftRequest liftReq = new LiftRequest();
        liftReq.setSerialNumber(prefix + "-SERIAL");
        liftReq.setModel("TestModel");
        liftReq.setManufacturer("Mfg");
        liftReq.setManufacturerOrganizationId(admin.getOrganization().getId());

        MvcResult liftResult = mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(liftReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer liftId = JsonPath.read(liftResult.getResponse().getContentAsString(), "$.id");
        return new OrgContext(token, admin.getOrganization().getId(), liftId, admin.getId());
    }

    @Test
    void createEvent_returns201() throws Exception {
        OrgContext ctx = setupOrgWithLift("evtcreate");

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setType(LiftEventType.CREATED);
        request.setDescription("Lift created and registered");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CREATED"))
                .andExpect(jsonPath("$.description").value("Lift created and registered"));
    }

    @Test
    void getAllEvents_returnsEvents() throws Exception {
        OrgContext ctx = setupOrgWithLift("evtlist");

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setType(LiftEventType.INSTALLED);
        request.setDescription("Installed at building");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getEventsByLiftId_filtersCorrectly() throws Exception {
        OrgContext ctx = setupOrgWithLift("evtfilter");

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setType(LiftEventType.INSTALLED);
        request.setDescription("Installed at building");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events")
                        .param("liftId", ctx.liftId.toString())
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/events")
                        .param("liftId", "999999")
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEvent_returns200() throws Exception {
        OrgContext ctx = setupOrgWithLift("evtupd");

        LiftEventRequest createReq = new LiftEventRequest();
        createReq.setLiftId(ctx.liftId.longValue());
        createReq.setType(LiftEventType.INSTALLED);
        createReq.setDescription("Original description");

        MvcResult createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer eventId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        LiftEventRequest updateReq = new LiftEventRequest();
        updateReq.setLiftId(ctx.liftId.longValue());
        updateReq.setType(LiftEventType.INSTALLED);
        updateReq.setDescription("Updated description");

        mockMvc.perform(put("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void deleteEvent_returns204() throws Exception {
        OrgContext ctx = setupOrgWithLift("evtdel");

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setType(LiftEventType.CREATED);
        request.setDescription("To be deleted");

        MvcResult createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer eventId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isNoContent());
    }
}
