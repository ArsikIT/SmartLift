package com.smartlift.integrational;

import com.smartlift.support.TestPasswords;
import com.jayway.jsonpath.JsonPath;
import com.smartlift.dto.request.DocumentRequest;
import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentControllerIntegrationTest extends BaseIntegrationTest {

    private record OrgContext(String token, Long orgId, Integer liftId, Long userId) {}

    private OrgContext setupOrgWithLift(String prefix) throws Exception {
        UserResponse admin = registerOrganization(prefix + "_admin", prefix + "@test.com",
                TestPasswords.DEFAULT, prefix + "Org", "MANUFACTURER");
        String token = login(prefix + "_admin", TestPasswords.DEFAULT);

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
    void createDocument_attachedToLift() throws Exception {
        OrgContext ctx = setupOrgWithLift("doccreate");

        DocumentRequest request = new DocumentRequest();
        request.setFileName("manual.pdf");
        request.setFilePath("/docs/manual.pdf");
        request.setContentType("application/pdf");
        request.setLiftId(ctx.liftId.longValue());

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("manual.pdf"))
                .andExpect(jsonPath("$.liftId").value(ctx.liftId));
    }

    @Test
    void createDocument_attachedToMaintenance() throws Exception {
        OrgContext ctx = setupOrgWithLift("docmnt");

        MaintenanceRequest mntReq = new MaintenanceRequest();
        mntReq.setLiftId(ctx.liftId.longValue());
        mntReq.setTitle("Inspection");

        MvcResult mntResult = mockMvc.perform(post("/api/maintenances")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mntReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer mntId = JsonPath.read(mntResult.getResponse().getContentAsString(), "$.id");

        DocumentRequest request = new DocumentRequest();
        request.setFileName("report.pdf");
        request.setFilePath("/docs/report.pdf");
        request.setContentType("application/pdf");
        request.setMaintenanceId(mntId.longValue());

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("report.pdf"))
                .andExpect(jsonPath("$.maintenanceId").value(mntId));
    }

    @Test
    void getAllDocuments_returnsPage() throws Exception {
        OrgContext ctx = setupOrgWithLift("doclist");

        DocumentRequest request = new DocumentRequest();
        request.setFileName("file1.pdf");
        request.setFilePath("/docs/file1.pdf");
        request.setLiftId(ctx.liftId.longValue());

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getDocumentsByLiftId_filtersCorrectly() throws Exception {
        OrgContext ctx = setupOrgWithLift("docfilter");

        DocumentRequest request = new DocumentRequest();
        request.setFileName("filtered.pdf");
        request.setFilePath("/docs/filtered.pdf");
        request.setLiftId(ctx.liftId.longValue());

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents")
                        .param("liftId", ctx.liftId.toString())
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateDocument_returns200() throws Exception {
        OrgContext ctx = setupOrgWithLift("docupd");

        DocumentRequest createReq = new DocumentRequest();
        createReq.setFileName("old_name.pdf");
        createReq.setFilePath("/docs/old.pdf");
        createReq.setLiftId(ctx.liftId.longValue());

        MvcResult createResult = mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer docId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        DocumentRequest updateReq = new DocumentRequest();
        updateReq.setFileName("new_name.pdf");
        updateReq.setFilePath("/docs/new.pdf");
        updateReq.setContentType("application/pdf");
        updateReq.setLiftId(ctx.liftId.longValue());

        mockMvc.perform(put("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("new_name.pdf"));
    }

    @Test
    void deleteDocument_returns204() throws Exception {
        OrgContext ctx = setupOrgWithLift("docdel");

        DocumentRequest request = new DocumentRequest();
        request.setFileName("delete_me.pdf");
        request.setFilePath("/docs/delete.pdf");
        request.setLiftId(ctx.liftId.longValue());

        MvcResult createResult = mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer docId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isNoContent());
    }
}
