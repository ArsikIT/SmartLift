package com.smartlift.integrational;

import com.jayway.jsonpath.JsonPath;
import com.smartlift.dto.request.LiftEventRequest;
import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.model.enums.LiftEventType;
import com.smartlift.model.enums.LiftStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LiftStateMachineIntegrationTest extends BaseIntegrationTest {

    private record OrgContext(String token, Long orgId, Integer liftId) {}

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
        return new OrgContext(token, admin.getOrganization().getId(), liftId);
    }

    private void createEvent(OrgContext ctx, LiftEventType type, String description) throws Exception {
        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setType(type);
        request.setDescription(description);

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void assertLiftStatus(OrgContext ctx, String expectedStatus) throws Exception {
        mockMvc.perform(get("/api/lifts/" + ctx.liftId)
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }

    private void expectEventRejected(OrgContext ctx, LiftEventType type) throws Exception {
        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setType(type);
        request.setDescription("Should be rejected");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // === Full lifecycle ===

    @Test
    void fullLifecycle_CREATED_to_INSTALLED_to_ACTIVE() throws Exception {
        OrgContext ctx = setupOrgWithLift("smfull");
        assertLiftStatus(ctx, "CREATED");

        createEvent(ctx, LiftEventType.INSTALLED, "Installed");
        assertLiftStatus(ctx, "INSTALLED");

        createEvent(ctx, LiftEventType.ACTIVATED, "Activated");
        assertLiftStatus(ctx, "ACTIVE");
    }

    @Test
    void faultAndRepairCycle() throws Exception {
        OrgContext ctx = setupOrgWithLift("smfault");

        createEvent(ctx, LiftEventType.INSTALLED, "Installed");
        createEvent(ctx, LiftEventType.ACTIVATED, "Activated");
        assertLiftStatus(ctx, "ACTIVE");

        createEvent(ctx, LiftEventType.FAULT, "Motor failure");
        assertLiftStatus(ctx, "FAULTY");

        createEvent(ctx, LiftEventType.REPAIR, "Motor replaced");
        assertLiftStatus(ctx, "IN_REPAIR");

        createEvent(ctx, LiftEventType.ACTIVATED, "Back online");
        assertLiftStatus(ctx, "ACTIVE");
    }

    @Test
    void decommission_fromAnyState() throws Exception {
        OrgContext ctx = setupOrgWithLift("smdecom1");
        assertLiftStatus(ctx, "CREATED");

        createEvent(ctx, LiftEventType.DECOMMISSIONED, "End of life");
        assertLiftStatus(ctx, "DECOMMISSIONED");
    }

    @Test
    void decommission_fromActive() throws Exception {
        OrgContext ctx = setupOrgWithLift("smdecom2");

        createEvent(ctx, LiftEventType.INSTALLED, "Installed");
        createEvent(ctx, LiftEventType.ACTIVATED, "Activated");
        assertLiftStatus(ctx, "ACTIVE");

        createEvent(ctx, LiftEventType.DECOMMISSIONED, "Decommissioned");
        assertLiftStatus(ctx, "DECOMMISSIONED");
    }

    @Test
    void decommission_fromFaulty() throws Exception {
        OrgContext ctx = setupOrgWithLift("smdecom3");

        createEvent(ctx, LiftEventType.INSTALLED, "Installed");
        createEvent(ctx, LiftEventType.ACTIVATED, "Activated");
        createEvent(ctx, LiftEventType.FAULT, "Critical failure");
        assertLiftStatus(ctx, "FAULTY");

        createEvent(ctx, LiftEventType.DECOMMISSIONED, "Unsalvageable");
        assertLiftStatus(ctx, "DECOMMISSIONED");
    }

    // === Invalid transitions ===

    @Test
    void decommissioned_isTerminal() throws Exception {
        OrgContext ctx = setupOrgWithLift("smterm");

        createEvent(ctx, LiftEventType.DECOMMISSIONED, "End of life");
        assertLiftStatus(ctx, "DECOMMISSIONED");

        expectEventRejected(ctx, LiftEventType.INSTALLED);
        expectEventRejected(ctx, LiftEventType.ACTIVATED);
        expectEventRejected(ctx, LiftEventType.FAULT);
        expectEventRejected(ctx, LiftEventType.REPAIR);
        expectEventRejected(ctx, LiftEventType.CREATED);
    }

    @Test
    void created_cannotSkipToActive() throws Exception {
        OrgContext ctx = setupOrgWithLift("smskip1");
        expectEventRejected(ctx, LiftEventType.ACTIVATED);
    }

    @Test
    void created_cannotFault() throws Exception {
        OrgContext ctx = setupOrgWithLift("smskip2");
        expectEventRejected(ctx, LiftEventType.FAULT);
    }

    @Test
    void created_cannotRepair() throws Exception {
        OrgContext ctx = setupOrgWithLift("smskip3");
        expectEventRejected(ctx, LiftEventType.REPAIR);
    }

    @Test
    void installed_cannotFault() throws Exception {
        OrgContext ctx = setupOrgWithLift("smskip4");
        createEvent(ctx, LiftEventType.INSTALLED, "Installed");

        expectEventRejected(ctx, LiftEventType.FAULT);
    }

    @Test
    void active_cannotRepair() throws Exception {
        OrgContext ctx = setupOrgWithLift("smskip5");
        createEvent(ctx, LiftEventType.INSTALLED, "Installed");
        createEvent(ctx, LiftEventType.ACTIVATED, "Activated");

        expectEventRejected(ctx, LiftEventType.REPAIR);
    }

    @Test
    void faulty_cannotActivateDirectly() throws Exception {
        OrgContext ctx = setupOrgWithLift("smskip6");
        createEvent(ctx, LiftEventType.INSTALLED, "Installed");
        createEvent(ctx, LiftEventType.ACTIVATED, "Activated");
        createEvent(ctx, LiftEventType.FAULT, "Fault");

        expectEventRejected(ctx, LiftEventType.ACTIVATED);
    }

    // === Direct status change blocked ===

    @Test
    void directStatusChange_isBlocked() throws Exception {
        OrgContext ctx = setupOrgWithLift("smdirect");

        LiftRequest updateReq = new LiftRequest();
        updateReq.setSerialNumber("smdirect-SERIAL");
        updateReq.setModel("TestModel");
        updateReq.setStatus(LiftStatus.ACTIVE);
        updateReq.setManufacturerOrganizationId(ctx.orgId);

        mockMvc.perform(put("/api/lifts/" + ctx.liftId)
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest());
    }

    // === Same-status event is allowed (no-op) ===

    @Test
    void sameStatusEvent_isAllowed() throws Exception {
        OrgContext ctx = setupOrgWithLift("smsame");

        createEvent(ctx, LiftEventType.CREATED, "Re-registered");
        assertLiftStatus(ctx, "CREATED");
    }
}
